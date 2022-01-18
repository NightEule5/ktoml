package com.akuleshov7.ktoml.annotations

import com.akuleshov7.ktoml.writers.IntegerRepresentation
import com.akuleshov7.ktoml.writers.IntegerRepresentation.DECIMAL

import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPE
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Instructs the encoder to represent an integer differently during serialization.
 *
 * @property representation How the integer should be represented in TOML.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(
    PROPERTY,
    TYPE
)
public annotation class TomlInteger(val representation: IntegerRepresentation = DECIMAL)
