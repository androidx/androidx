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
package androidx.appactions.builtintypes.properties

import androidx.appactions.builtintypes.serializers.InstantAsEpochMilliSerializer
import androidx.appactions.builtintypes.serializers.LocalDateTimeAsUtcEpochSecondSerializer
import androidx.appactions.builtintypes.serializers.LocalTimeAsNanoOfDaySerializer
import androidx.appsearch.`annotation`.Document
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * The startTime of something.
 *
 * For a reserved event or service (e.g. `FoodEstablishmentReservation`), the time that it is
 * expected to start. For actions that span a period of time, when the action was performed. E.g.
 * John wrote a book from *January* to December. For media, including audio and video, it's the time
 * offset of the start of a clip within a larger file.
 *
 * See https://schema.org/startTime for context.
 *
 * Holds one of:
 * * Time i.e. [LocalTime]
 * * [LocalDateTime]
 * * [Instant]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:StartTime")
public class StartTime
internal constructor(
  /** The [LocalTime] variant, or null if constructed using a different variant. */
  @get:JvmName("asTime")
  @get:Document.LongProperty(serializer = LocalTimeAsNanoOfDaySerializer::class)
  public val asTime: LocalTime? = null,
  /** The [LocalDateTime] variant, or null if constructed using a different variant. */
  @get:JvmName("asLocalDateTime")
  @get:Document.LongProperty(serializer = LocalDateTimeAsUtcEpochSecondSerializer::class)
  public val asLocalDateTime: LocalDateTime? = null,
  /** The [Instant] variant, or null if constructed using a different variant. */
  @get:JvmName("asInstant")
  @get:Document.LongProperty(serializer = InstantAsEpochMilliSerializer::class)
  public val asInstant: Instant? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
  /** Constructor for the [LocalTime] variant. */
  public constructor(time: LocalTime) : this(asTime = time)

  /** Constructor for the [LocalDateTime] variant. */
  public constructor(localDateTime: LocalDateTime) : this(asLocalDateTime = localDateTime)

  /** Constructor for the [Instant] variant. */
  public constructor(instant: Instant) : this(asInstant = instant)

  /**
   * Maps each of the possible underlying variants to some [R].
   *
   * A visitor can be provided to handle the possible variants. A catch-all default case must be
   * provided in case a new type is added in a future release of this library.
   *
   * @sample [androidx.appactions.builtintypes.samples.properties.startTimeMapWhenUsage]
   */
  public fun <R> mapWhen(mapper: Mapper<R>): R =
    when {
      asTime != null -> mapper.time(asTime)
      asLocalDateTime != null -> mapper.localDateTime(asLocalDateTime)
      asInstant != null -> mapper.instant(asInstant)
      else -> error("No variant present in StartTime")
    }

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asTime != null ->
        if (includeWrapperName) {
          """StartTime($asTime)"""
        } else {
          asTime.toString()
        }
      asLocalDateTime != null ->
        if (includeWrapperName) {
          """StartTime($asLocalDateTime)"""
        } else {
          asLocalDateTime.toString()
        }
      asInstant != null ->
        if (includeWrapperName) {
          """StartTime($asInstant)"""
        } else {
          asInstant.toString()
        }
      else -> error("No variant present in StartTime")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is StartTime) return false
    if (asTime != other.asTime) return false
    if (asLocalDateTime != other.asLocalDateTime) return false
    if (asInstant != other.asInstant) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asTime, asLocalDateTime, asInstant)

  /** Maps each of the possible variants of [StartTime] to some [R]. */
  public interface Mapper<R> {
    /** Returns some [R] when the [StartTime] holds some [LocalTime] instance. */
    public fun time(instance: LocalTime): R = orElse()

    /** Returns some [R] when the [StartTime] holds some [LocalDateTime] instance. */
    public fun localDateTime(instance: LocalDateTime): R = orElse()

    /** Returns some [R] when the [StartTime] holds some [Instant] instance. */
    public fun instant(instance: Instant): R = orElse()

    /** The catch-all handler that is invoked when a particular variant isn't explicitly handled. */
    public fun orElse(): R
  }
}
