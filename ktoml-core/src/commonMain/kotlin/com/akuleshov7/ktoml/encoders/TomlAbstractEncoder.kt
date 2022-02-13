package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.annotations.TomlComment
import com.akuleshov7.ktoml.annotations.TomlInline
import com.akuleshov7.ktoml.annotations.TomlInteger
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.TomlBasicString
import com.akuleshov7.ktoml.tree.TomlBoolean
import com.akuleshov7.ktoml.tree.TomlDouble
import com.akuleshov7.ktoml.tree.TomlKey
import com.akuleshov7.ktoml.tree.TomlLiteralString
import com.akuleshov7.ktoml.tree.TomlLong
import com.akuleshov7.ktoml.tree.TomlNull
import com.akuleshov7.ktoml.tree.TomlTable
import com.akuleshov7.ktoml.tree.TomlValue
import com.akuleshov7.ktoml.writers.IntegerRepresentation.DECIMAL
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind.CLASS
import kotlinx.serialization.descriptors.StructureKind.LIST
import kotlinx.serialization.descriptors.StructureKind.MAP
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.Encoder
import com.akuleshov7.ktoml.annotations.TomlLiteralString as TomlLiteral
import com.akuleshov7.ktoml.annotations.TomlMultilineString as TomlMultiline

/**
 * Abstract Encoder for TOML format that is inherited by each and every encoder in this project.
 * It serves one aim: to define encoders for primitive types that are allowed in TOML.
 *
 * Todo: Support polymorphic types
 * @param startElementIndex The element index to start from. The next element will
 * be this `+ 1`.
 */
@ExperimentalSerializationApi
@Suppress("CLASS_SHOULD_NOT_BE_ABSTRACT")  // The currentKey property is abstract.
public abstract class TomlAbstractEncoder(
    protected val config: TomlConfig,
    startElementIndex: Int = -1
) : AbstractEncoder() {
    internal abstract val currentKey: TomlKey
    protected var elementIndex: Int = startElementIndex
        private set

    private var isStringLiteral = false
    private var isStringMultiline = false
    private var integerRepresentation = DECIMAL

    private var isInlineTable = false

    private var comments: List<String>? = null
    private var inlineComment: String? = null

    protected fun nextElementIndex(): Int = ++elementIndex

    // Invalid Toml primitive types, we will simply throw an error for them
    override fun encodeByte(value: Byte): Nothing = invalidType("Byte", "Long", value)
    override fun encodeShort(value: Short): Nothing = invalidType("Short", "Long", value)
    override fun encodeInt(value: Int): Nothing = invalidType("Int", "Long", value)
    override fun encodeFloat(value: Float): Nothing = invalidType("Float", "Double", value)
    override fun encodeChar(value: Char): Nothing = invalidType("Char", "String", value)

    private fun invalidType(
        typeName: String,
        requiredType: String,
        value: Any): Nothing {
        val key = currentKey.content

        // Todo: This is a decoding exception. Make it shared or make a separate one?
        throw IllegalTypeException(
            "<$typeName> type is not allowed by toml specification," +
                    " use <$requiredType> instead" +
                    " (field = $key; value = $value)",
            elementIndex
        )
    }

    override fun encodeBoolean(value: Boolean): Unit = encodeValue(TomlBoolean(value, elementIndex))

    override fun encodeDouble(value: Double): Unit = encodeValue(TomlDouble(value, elementIndex))

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit =
        encodeString(enumDescriptor.getElementName(index))

    override fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder = this

    override fun encodeLong(value: Long) {
        val representation = integerRepresentation

        if (integerRepresentation !== DECIMAL)
            integerRepresentation = DECIMAL

        if (representation !== DECIMAL)
            throw UnsupportedEncodingFeatureException(
                "Non-decimal integers are not yet supported."
            )

        encodeValue(TomlLong(value, elementIndex))
    }

    override fun encodeNull(): Unit = encodeValue(TomlNull(elementIndex))

    override fun encodeString(value: String) {
        // Record and reset the "literal" flag
        val literal = if (isStringLiteral) {
            isStringLiteral = false

            true
        }
        else false

        // Record and reset the "multiline" flag
        val multiline = if (isStringMultiline) {
            isStringMultiline = false

            true
        }
        else false

        if (multiline)
            throw UnsupportedEncodingFeatureException(
                "Encoding multiline strings is not yet supported."
            )

        encodeValue(
            if (literal)
                TomlLiteralString(value as Any, elementIndex)
            else TomlBasicString(value as Any, elementIndex)
        )
    }

    protected abstract fun encodeValue(value: TomlValue)
    protected abstract fun encodeTable(table: TomlTable)

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        nextElementIndex()

        // Find annotations

        val typeAnnotations = descriptor.annotations

        val elementAnnotations = when (val kind = descriptor.kind) {
            CLASS -> descriptor.getElementAnnotations(index)
            MAP -> descriptor.getElementAnnotations(1)
            LIST -> descriptor.getElementAnnotations(0)
            is PolymorphicKind -> throw UnsupportedEncodingFeatureException(
                "Polymorphic types are not yet supported"
            )
            else -> throw InternalEncodingException("Unknown parent kind $kind")
        }

        setFlags(   typeAnnotations)
        setFlags(elementAnnotations)

        return true
    }

    private fun setFlags(annotations: Iterable<Annotation>) =
        annotations.forEach {
            when (it) {
                is TomlLiteral -> isStringLiteral = true
                is TomlMultiline -> isStringMultiline = true
                is TomlInteger -> integerRepresentation = it.representation
                is TomlComment -> {
                    comments = it.lines.asList()
                    inlineComment = it.inline
                }
                is TomlInline -> isInlineTable = true
                else -> { }
            }
        }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        if (encodeElement(descriptor, index)) {
            when (val kind = descriptor.getElementDescriptor(index).kind) {
                LIST -> {
                    val enc = TomlArrayEncoder(descriptor, index, elementIndex, config)

                    serializer.serialize(enc, value)

                    if (enc.isTableArray())
                        encodeTable(enc.tableArray)
                    else encodeValue(enc.valueArray)
                }
                CLASS, MAP -> encodeTableLike(descriptor, index, serializer, value)
                is PolymorphicKind -> throw UnsupportedEncodingFeatureException(
                    "Polymorphic types are not yet supported."
                )
                else -> throw InternalEncodingException("Unknown parent kind $kind")
            }
        }
    }

    protected abstract fun <T> encodeTableLike(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ): TomlAbstractEncoder
}
