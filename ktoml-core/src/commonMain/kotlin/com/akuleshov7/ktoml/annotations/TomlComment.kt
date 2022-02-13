package com.akuleshov7.ktoml.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPE

/**
 * Instructs the encoder to insert comments around a field or table.
 *
 * @property lines The comment lines to insert above the field or table.
 * @property inline A comment to insert at the end of the line. If left empty,
 * no comment will be inserted.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(
    CLASS,
    PROPERTY,
    TYPE
)
public annotation class TomlComment(
    vararg val lines: String,
    val inline: String = ""
)
