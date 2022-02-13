package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.annotations.TomlInline
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.tree.TomlArray
import com.akuleshov7.ktoml.tree.TomlArrayOfTables
import com.akuleshov7.ktoml.tree.TomlTable
import com.akuleshov7.ktoml.tree.TomlValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind.CLASS
import kotlinx.serialization.descriptors.StructureKind.LIST
import kotlinx.serialization.descriptors.StructureKind.MAP
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Encodes a TOML array.
 *
 * Todo: Handle PolymorphicKind
 */
@ExperimentalSerializationApi
public class TomlArrayEncoder private constructor(
    private var isTableArray: Boolean,
    private val startElementIndex: Int,
    config: TomlConfig
) : TomlAbstractEncoder(config, startElementIndex) {
    /**
     * @param parentDescriptor The [SerialDescriptor] passed to [encodeElement].
     * @param index The index passed to [encodeElement].
     * @param startElementIndex The element index to start the array from.
     */
    public constructor(
        parentDescriptor: SerialDescriptor,
        index: Int,
        startElementIndex: Int,
        config: TomlConfig
    ) : this(parentDescriptor.isTableArray(index), startElementIndex, config)

    override val serializersModule: SerializersModule = EmptySerializersModule

    override val currentKey: Nothing get() = throw UnsupportedOperationException()

    internal val lastElementIndex get() = elementIndex

    private lateinit var tables: MutableList<TomlTable>
    private lateinit var values: MutableList<TomlValue>

    internal lateinit var tableArray: TomlArrayOfTables
    internal lateinit var valueArray: TomlArray

    internal fun isTableArray() = isTableArray

    // Structure begin and end

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        fun <T> mutableList(capacity: Int) =
            if (capacity > -1)
                ArrayList<T>(capacity)
            else mutableListOf()

        if (!isTableArray) {
            // Confirm the table array status

            // If the type is table-like, set isTableArray to true.
            when (descriptor.kind) {
                CLASS, MAP ->
                    if (!descriptor.annotations.hasInlineAnnotation())
                        isTableArray = true
                else -> { }
            }
        }

        if (isTableArray)
            tables = mutableList(collectionSize)
        else values = mutableList(collectionSize)

        return this
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        beginCollection(descriptor, collectionSize = -1)

    override fun endStructure(descriptor: SerialDescriptor) {
        if (isTableArray) {
            tableArray = TomlArrayOfTables(
                content = "",
                lineNo = startElementIndex + 1,
                config
            )

            tables.forEach {
                tableArray.insertTableToTree(it, it.type)
            }
        }
        else
            valueArray = TomlArray(
                values,
                rawContent = "",
                lineNo = startElementIndex + 1
            )
    }

    // Elements

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        // Increment the element index, since array elements are on separate lines
        // from their parent nodes.
        nextElementIndex()

        return true
    }

    override fun encodeValue(value: TomlValue) {
        values += value
    }

    override fun encodeTable(table: TomlTable) {
        tables += table
    }

    override fun <T> encodeTableLike(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ): TomlAbstractEncoder {
        TODO("Not yet implemented")
    }

    public companion object
    {
        private fun Iterable<Annotation>.hasInlineAnnotation() = any { it is TomlInline }

        /**
         * Determines whether an element is a table array. An array of inline tables
         * will return false.
         */
        private fun SerialDescriptor.isTableArray(index: Int): Boolean {
            // Check for the annotation on the parent
            if (annotations.hasInlineAnnotation()) return false // Defer for later.

            return when (kind) {
                CLASS -> {
                    // Check for the annotation on the member property.
                    !getElementAnnotations(index).hasInlineAnnotation()
                }
                LIST, MAP -> false // Defer for later.
                else -> throw InternalEncodingException(
                    "The serial kind $kind is incompatible as an array parent."
                )
            }
        }
    }
}