package com.akuleshov7.ktoml.annotations

import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Instructs the encoder to serialize a [String] or [CharSequence] as a multiline
 * string. Has no effect on deserialization.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(PROPERTY)
public annotation class TomlMultilineString
