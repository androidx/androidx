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

import androidx.appactions.builtintypes.serializers.DayOfWeekAsCanonicalUrlSerializer
import androidx.appactions.builtintypes.types.DayOfWeek
import androidx.appsearch.`annotation`.Document
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * Defines the day(s) of the week on which a recurring Event takes place.
 *
 * See https://schema.org/byDay for context.
 *
 * Holds one of:
 * * Text i.e. [String]
 * * [DayOfWeek]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:ByDay")
public class ByDay
internal constructor(
  /** The [String] variant, or null if constructed using a different variant. */
  @get:JvmName("asText") @get:Document.StringProperty public val asText: String? = null,
  /** The [DayOfWeek] variant, or null if constructed using a different variant. */
  @get:JvmName("asDayOfWeek")
  @get:Document.StringProperty(serializer = DayOfWeekAsCanonicalUrlSerializer::class)
  public val asDayOfWeek: DayOfWeek? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
  /** Constructor for the [String] variant. */
  public constructor(text: String) : this(asText = text)

  /** Constructor for the [DayOfWeek] variant. */
  public constructor(dayOfWeek: DayOfWeek) : this(asDayOfWeek = dayOfWeek)

  /**
   * Maps each of the possible underlying variants to some [R].
   *
   * A visitor can be provided to handle the possible variants. A catch-all default case must be
   * provided in case a new type is added in a future release of this library.
   *
   * @sample [androidx.appactions.builtintypes.samples.properties.byDayMapWhenUsage]
   */
  public fun <R> mapWhen(mapper: Mapper<R>): R =
    when {
      asText != null -> mapper.text(asText)
      asDayOfWeek != null -> mapper.dayOfWeek(asDayOfWeek)
      else -> error("No variant present in ByDay")
    }

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asText != null ->
        if (includeWrapperName) {
          """ByDay($asText)"""
        } else {
          asText
        }
      asDayOfWeek != null ->
        if (includeWrapperName) {
          """ByDay($asDayOfWeek)"""
        } else {
          asDayOfWeek.toString()
        }
      else -> error("No variant present in ByDay")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ByDay) return false
    if (asText != other.asText) return false
    if (asDayOfWeek != other.asDayOfWeek) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asText, asDayOfWeek)

  /** Maps each of the possible variants of [ByDay] to some [R]. */
  public interface Mapper<R> {
    /** Returns some [R] when the [ByDay] holds some [String] instance. */
    public fun text(instance: String): R = orElse()

    /** Returns some [R] when the [ByDay] holds some [DayOfWeek] instance. */
    public fun dayOfWeek(instance: DayOfWeek): R = orElse()

    /** The catch-all handler that is invoked when a particular variant isn't explicitly handled. */
    public fun orElse(): R
  }
}
