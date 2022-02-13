package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.tree.TomlArray
import com.akuleshov7.ktoml.tree.TomlFile
import com.akuleshov7.ktoml.tree.TomlKey
import com.akuleshov7.ktoml.tree.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.TomlNode
import com.akuleshov7.ktoml.tree.TomlTable
import com.akuleshov7.ktoml.tree.TomlValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Main entry point into the encoding process. It can create less common encoders inside, for example:
 * TomlListEncoder, TomlPrimitiveEncoder, etc.
 */
@OptIn(ExperimentalSerializationApi::class)
public class TomlMainEncoder(
    private val rootNode: TomlNode,
    config: TomlConfig,
) : TomlAbstractEncoder(config) {
    override val serializersModule: SerializersModule = EmptySerializersModule

    override lateinit var currentKey: TomlKey
        private set

    private lateinit var currentTable: TomlTable

    override fun encodeValue(value: TomlValue) {
        currentTable.appendChild(
            if (value is TomlArray)
                TomlKeyValueArray(currentKey, value, elementIndex, "")
            else TomlKeyValuePrimitive(currentKey, value, elementIndex, "")
        )
    }

    override fun encodeTable(table: TomlTable) {
        rootNode.insertTableToTree(table)
    }
}
