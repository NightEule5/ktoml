package com.akuleshov7.ktoml.annotations

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPE
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Instructs the encoder to serialize a table-like value (class instance or [Map])
 * to an inline table. Has no effect on deserialization. If table-like collection
 * element types are annotated, the collection will be serialized as an array with
 * inline table elements instead of an array of tables; annotating the collection
 * type or the property is also valid.
 *
 * ```kotlin
 * // Examples
 *
 * @TomlInlineTable
 * class MyTable
 *
 * @TomlInlineTable
 * val myTable: MyTable
 *
 * val myTable: @TomlInlineTable MyTable
 *
 *
 * @TomlInlineTable
 * val myTables: List<MyTable>
 *
 * val myTables: @TomlInlineTable List<MyTable>
 *
 * val myTables: List<@TomlInlineTable MyTable>
 * ```
 *
 * When [Map] value types are annotated, the map will be serialized as a table with
 * inline table fields.
 *
 * Todo: Inline tables are not supported yet. This has no effect currently.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(
    CLASS,
    PROPERTY,
    TYPE
)
public annotation class TomlInlineTable
