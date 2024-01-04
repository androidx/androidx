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

import androidx.appactions.builtintypes.serializers.DurationAsNanosSerializer
import androidx.appsearch.`annotation`.Document
import java.time.Duration
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * Defines the frequency at which `Event`s will occur according to a schedule `Schedule`. The
 * intervals between events should be defined as a `Duration` of time.
 *
 * See https://schema.org/repeatFrequency for context.
 *
 * Holds one of:
 * * [Duration]
 * * Text i.e. [String]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:RepeatFrequency")
public class RepeatFrequency
internal constructor(
  /** The [Duration] variant, or null if constructed using a different variant. */
  @get:JvmName("asDuration")
  @get:Document.LongProperty(serializer = DurationAsNanosSerializer::class)
  public val asDuration: Duration? = null,
  /** The [String] variant, or null if constructed using a different variant. */
  @get:JvmName("asText") @get:Document.StringProperty public val asText: String? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
  /** Constructor for the [Duration] variant. */
  public constructor(duration: Duration) : this(asDuration = duration)

  /** Constructor for the [String] variant. */
  public constructor(text: String) : this(asText = text)

  /**
   * Maps each of the possible underlying variants to some [R].
   *
   * A visitor can be provided to handle the possible variants. A catch-all default case must be
   * provided in case a new type is added in a future release of this library.
   *
   * @sample [androidx.appactions.builtintypes.samples.properties.repeatFrequencyMapWhenUsage]
   */
  public fun <R> mapWhen(mapper: Mapper<R>): R =
    when {
      asDuration != null -> mapper.duration(asDuration)
      asText != null -> mapper.text(asText)
      else -> error("No variant present in RepeatFrequency")
    }

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asDuration != null ->
        if (includeWrapperName) {
          """RepeatFrequency($asDuration)"""
        } else {
          asDuration.toString()
        }
      asText != null ->
        if (includeWrapperName) {
          """RepeatFrequency($asText)"""
        } else {
          asText
        }
      else -> error("No variant present in RepeatFrequency")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RepeatFrequency) return false
    if (asDuration != other.asDuration) return false
    if (asText != other.asText) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asDuration, asText)

  /** Maps each of the possible variants of [RepeatFrequency] to some [R]. */
  public interface Mapper<R> {
    /** Returns some [R] when the [RepeatFrequency] holds some [Duration] instance. */
    public fun duration(instance: Duration): R = orElse()

    /** Returns some [R] when the [RepeatFrequency] holds some [String] instance. */
    public fun text(instance: String): R = orElse()

    /** The catch-all handler that is invoked when a particular variant isn't explicitly handled. */
    public fun orElse(): R
  }
}
