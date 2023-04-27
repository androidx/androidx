// Copyright 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package androidx.appactions.builtintypes.properties

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * Can be used in cases where more specific properties (e.g. temporalCoverage, dateCreated,
 * dateModified, datePublished) are not known to be appropriate.
 *
 * See http://schema.googleapis.com/temporal for context.
 *
 * Holds one of:
 * * [LocalDateTime]
 * * [ZonedDateTime]
 * * Text i.e. [String]
 * * [Temporal.CanonicalValue]
 *
 * May hold more types over time.
 */
public class Temporal
internal constructor(
  /** The [LocalDateTime] variant, or null if constructed using a different variant. */
  @get:JvmName("asLocalDateTime") public val asLocalDateTime: LocalDateTime? = null,
  /** The [ZonedDateTime] variant, or null if constructed using a different variant. */
  @get:JvmName("asZonedDateTime") public val asZonedDateTime: ZonedDateTime? = null,
  /** The [String] variant, or null if constructed using a different variant. */
  @get:JvmName("asText") public val asText: String? = null,
  /** The [CanonicalValue] variant, or null if constructed using a different variant. */
  @get:JvmName("asCanonicalValue") public val asCanonicalValue: CanonicalValue? = null,
  /**
   * The AppSearch document's identifier.
   *
   * Every AppSearch document needs an identifier. Since property wrappers are only meant to be used
   * at nested levels, this is internal and will always be an empty string.
   */
  internal val identifier: String = "",
) {
  /** Constructor for the [LocalDateTime] variant. */
  public constructor(localDateTime: LocalDateTime) : this(asLocalDateTime = localDateTime)

  /** Constructor for the [ZonedDateTime] variant. */
  public constructor(zonedDateTime: ZonedDateTime) : this(asZonedDateTime = zonedDateTime)

  /** Constructor for the [String] variant. */
  public constructor(text: String) : this(asText = text)

  /** Constructor for the [CanonicalValue] variant. */
  public constructor(canonicalValue: CanonicalValue) : this(asCanonicalValue = canonicalValue)

  /**
   * Maps each of the possible underlying variants to some [R].
   *
   * A visitor can be provided to handle the possible variants. A catch-all default case must be
   * provided in case a new type is added in a future release of this library.
   *
   * @sample [androidx.appactions.builtintypes.samples.properties.temporalMapWhenUsage]
   */
  public fun <R> mapWhen(mapper: Mapper<R>): R =
    when {
      asLocalDateTime != null -> mapper.localDateTime(asLocalDateTime)
      asZonedDateTime != null -> mapper.zonedDateTime(asZonedDateTime)
      asText != null -> mapper.text(asText)
      asCanonicalValue != null -> mapper.canonicalValue(asCanonicalValue)
      else -> error("No variant present in Temporal")
    }

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asLocalDateTime != null ->
        if (includeWrapperName) {
          """Temporal($asLocalDateTime)"""
        } else {
          asLocalDateTime.toString()
        }
      asZonedDateTime != null ->
        if (includeWrapperName) {
          """Temporal($asZonedDateTime)"""
        } else {
          asZonedDateTime.toString()
        }
      asText != null ->
        if (includeWrapperName) {
          """Temporal($asText)"""
        } else {
          asText
        }
      asCanonicalValue != null ->
        if (includeWrapperName) {
          """Temporal($asCanonicalValue)"""
        } else {
          asCanonicalValue.toString()
        }
      else -> error("No variant present in Temporal")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Temporal) return false
    if (asLocalDateTime != other.asLocalDateTime) return false
    if (asZonedDateTime != other.asZonedDateTime) return false
    if (asText != other.asText) return false
    if (asCanonicalValue != other.asCanonicalValue) return false
    return true
  }

  public override fun hashCode(): Int =
    Objects.hash(asLocalDateTime, asZonedDateTime, asText, asCanonicalValue)

  /** Maps each of the possible variants of [Temporal] to some [R]. */
  public interface Mapper<R> {
    /** Returns some [R] when the [Temporal] holds some [LocalDateTime] instance. */
    public fun localDateTime(instance: LocalDateTime): R = orElse()

    /** Returns some [R] when the [Temporal] holds some [ZonedDateTime] instance. */
    public fun zonedDateTime(instance: ZonedDateTime): R = orElse()

    /** Returns some [R] when the [Temporal] holds some [String] instance. */
    public fun text(instance: String): R = orElse()

    /** Returns some [R] when the [Temporal] holds some [CanonicalValue] instance. */
    public fun canonicalValue(instance: CanonicalValue): R = orElse()

    /** The catch-all handler that is invoked when a particular variant isn't explicitly handled. */
    public fun orElse(): R
  }

  public abstract class CanonicalValue internal constructor() {
    public abstract val textValue: String
  }
}
