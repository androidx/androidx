/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appactions.interaction.capabilities.core.impl.converters

import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.protobuf.Value
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * TypeSpec is used to convert between native objects in capabilities/values and Value proto.
 */
interface TypeSpec<T> {
    /* Given the object, returns its identifier, which can be null. */
    fun getIdentifier(obj: T): String?

    /** Converts a object into a Value proto. */
    fun toValue(obj: T): Value

    /**
     * Converts a Value into an object.
     *
     * @throws StructConversionException if the Struct is malformed.
     */
    @Throws(StructConversionException::class)
    fun fromValue(value: Value): T

    companion object {
        /** Create a TypeSpec that serializes to/from Value.stringValue. */
        @JvmStatic
        fun <T> createStringBasedTypeSpec(
            toString: (T) -> String,
            fromString: (String) -> T
        ): TypeSpec<T> = object : TypeSpec<T> {
            override fun getIdentifier(obj: T): String? = null
            override fun toValue(obj: T): Value = Value.newBuilder()
                .setStringValue(toString(obj)).build()
            override fun fromValue(value: Value): T = when {
                value.hasStringValue() -> fromString(value.stringValue)
                else -> throw StructConversionException(
                    "cannot convert $value, expected Value.stringValue to be present")
            }
        }

        @JvmField
        val STRING_TYPE_SPEC = createStringBasedTypeSpec<String>(
            toString = { it },
            fromString = { it }
        )

        @JvmField
        val BOOL_TYPE_SPEC = object : TypeSpec<Boolean> {
            override fun getIdentifier(obj: Boolean): String? = null

            override fun toValue(
                obj: Boolean,
            ): Value = Value.newBuilder().setBoolValue(obj).build()

            override fun fromValue(value: Value): Boolean = when {
                value.hasBoolValue() -> value.boolValue
                else -> throw StructConversionException("BOOL_TYPE_SPEC cannot convert $value")
            }
        }

        @JvmField
        val NUMBER_TYPE_SPEC = object : TypeSpec<Double> {
            override fun getIdentifier(obj: Double): String? = null

            override fun toValue(
                obj: Double,
            ): Value = Value.newBuilder().setNumberValue(obj).build()

            override fun fromValue(value: Value): Double = when {
                value.hasNumberValue() -> value.numberValue
                else -> throw StructConversionException("NUMBER_TYPE_SPEC cannot convert $value")
            }
        }

        @JvmField
        val INTEGER_TYPE_SPEC = object : TypeSpec<Int> {
            override fun getIdentifier(obj: Int): String? = null

            override fun toValue(
                obj: Int,
            ): Value = Value.newBuilder().setNumberValue(obj.toDouble()).build()

            override fun fromValue(value: Value): Int = when {
                value.hasNumberValue() -> value.numberValue.toInt()
                else -> throw StructConversionException("INTEGER_TYPE_SPEC cannot convert $value")
            }
        }

        // TODO(remove this when Long is no longer used in BIT library)
        @JvmField
        /** Serialize Long to/from string field due to precision limit with nunmber value. */
        val LONG_TYPE_SPEC = object : TypeSpec<Long> {
            override fun getIdentifier(obj: Long): String? = null

            override fun toValue(
                obj: Long,
                ): Value = Value.newBuilder().setNumberValue(obj.toDouble()).build()

                override fun fromValue(value: Value): Long = when {
                    value.hasNumberValue() -> value.numberValue.toLong()
                    else -> throw StructConversionException("LONG_TYPE_SPEC cannot convert $value")
                }
        }

        @JvmField
        val LOCAL_DATE_TYPE_SPEC = createStringBasedTypeSpec<LocalDate>(
            toString = { it.format(DateTimeFormatter.ISO_LOCAL_DATE) },
            fromString = { try {
                LocalDate.parse(it)
            } catch (e: DateTimeParseException) {
                throw StructConversionException(
                                    "Failed to parse ISO 8601 string to LocalDate", e)
            } }
        )

        @JvmField
        val LOCAL_TIME_TYPE_SPEC = createStringBasedTypeSpec<LocalTime>(
            toString = { it.format(DateTimeFormatter.ISO_LOCAL_TIME) },
            fromString = { try {
                LocalTime.parse(it)
            } catch (e: DateTimeParseException) {
                throw StructConversionException(
                                    "Failed to parse ISO 8601 string to LocalTime", e)
            } }
        )

        @JvmField
        val LOCAL_DATE_TIME_TYPE_SPEC = createStringBasedTypeSpec<LocalDateTime>(
            toString = { it.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) },
            fromString = { try {
                LocalDateTime.parse(it)
            } catch (e: DateTimeParseException) {
                throw StructConversionException(
                                    "Failed to parse ISO 8601 string to LocalDateTime", e)
            } }
        )

        @JvmField
        val ZONED_DATE_TIME_TYPE_SPEC = createStringBasedTypeSpec<ZonedDateTime>(
            toString = { it.toOffsetDateTime().toString() },
            fromString = { try {
                ZonedDateTime.parse(it)
            } catch (e: DateTimeParseException) {
                throw StructConversionException(
                                    "Failed to parse ISO 8601 string to ZonedDateTime", e)
            } }
        )

        @JvmField
        val INSTANT_TYPE_SPEC = createStringBasedTypeSpec<Instant>(
            toString = Instant::toString,
            fromString = { try {
                Instant.parse(it)
            } catch (e: DateTimeParseException) {
                throw StructConversionException(
                                    "Failed to parse ISO 8601 string to Instant", e)
            } }
        )

        @JvmField
        val DURATION_TYPE_SPEC = createStringBasedTypeSpec<Duration>(
            toString = Duration::toString,
            fromString = { try {
                Duration.parse(it)
            } catch (e: DateTimeParseException) {
                throw StructConversionException(
                                    "Failed to parse ISO 8601 string to Duration", e)
            } }
        )

        @JvmField
        val ZONE_ID_TYPE_SPEC = createStringBasedTypeSpec<ZoneId>(
            toString = ZoneId::toString,
            fromString = { try {
                ZoneId.of(it)
            } catch (e: Exception) {
                throw StructConversionException(
                    "Failed to parse string to ZoneId", e)
            } }
        )
    }
}
