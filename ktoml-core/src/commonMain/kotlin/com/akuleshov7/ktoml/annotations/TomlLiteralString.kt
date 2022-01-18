package com.akuleshov7.ktoml.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Instructs the encoder to serialize a [String] or [CharSequence] as a literal
 * string. Has no effect on deserialization.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(PROPERTY)
public annotation class TomlLiteralString
