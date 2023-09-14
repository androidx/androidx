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

import androidx.`annotation`.RestrictTo
import androidx.`annotation`.RestrictTo.Scope.LIBRARY_GROUP
import androidx.appsearch.`annotation`.Document
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * A sub property of description. A short description of the item used to disambiguate from other,
 * similar items. Information from other properties (in particular, name) may be necessary for the
 * description to be useful for disambiguation.
 *
 * See https://schema.org/disambiguatingDescription for context.
 *
 * Holds one of:
 * * Text i.e. [String]
 * * [DisambiguatingDescription.CanonicalValue]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:DisambiguatingDescription")
public class DisambiguatingDescription
internal constructor(
  /** The [String] variant, or null if constructed using a different variant. */
  @get:JvmName("asText") @get:Document.StringProperty public val asText: String? = null,
  /** The [CanonicalValue] variant, or null if constructed using a different variant. */
  @get:JvmName("asCanonicalValue")
  @get:Document.DocumentProperty
  public val asCanonicalValue: CanonicalValue? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
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
   * @sample [androidx.appactions.builtintypes.samples.properties.disambiguatingDescriptionMapWhenUsage]
   */
  public fun <R> mapWhen(mapper: Mapper<R>): R =
    when {
      asText != null -> mapper.text(asText)
      asCanonicalValue != null -> mapper.canonicalValue(asCanonicalValue)
      else -> error("No variant present in DisambiguatingDescription")
    }

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asText != null ->
        if (includeWrapperName) {
          """DisambiguatingDescription($asText)"""
        } else {
          asText
        }
      asCanonicalValue != null ->
        if (includeWrapperName) {
          """DisambiguatingDescription($asCanonicalValue)"""
        } else {
          asCanonicalValue.toString()
        }
      else -> error("No variant present in DisambiguatingDescription")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DisambiguatingDescription) return false
    if (asText != other.asText) return false
    if (asCanonicalValue != other.asCanonicalValue) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asText, asCanonicalValue)

  /** Maps each of the possible variants of [DisambiguatingDescription] to some [R]. */
  public interface Mapper<R> {
    /** Returns some [R] when the [DisambiguatingDescription] holds some [String] instance. */
    public fun text(instance: String): R = orElse()

    /**
     * Returns some [R] when the [DisambiguatingDescription] holds some [CanonicalValue] instance.
     */
    public fun canonicalValue(instance: CanonicalValue): R = orElse()

    /** The catch-all handler that is invoked when a particular variant isn't explicitly handled. */
    public fun orElse(): R
  }

  /**
   * Represents a canonical text value for [DisambiguatingDescription].
   *
   * @see androidx.appactions.builtintypes.types.Alarm.DisambiguatingDescriptionValue
   */
  @Document(name = "bitprop:DisambiguatingDescription:CanonicalValue")
  public open class CanonicalValue
  @RestrictTo(LIBRARY_GROUP)
  constructor(
    @get:Document.StringProperty public val textValue: String,
  ) {
    @get:RestrictTo(LIBRARY_GROUP)
    @set:RestrictTo(LIBRARY_GROUP)
    @get:Document.Id
    public var identifier: String = ""

    @get:RestrictTo(LIBRARY_GROUP)
    @set:RestrictTo(LIBRARY_GROUP)
    @get:Document.Namespace
    public var namespace: String = ""

    public override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is CanonicalValue) return false
      return textValue == other.textValue
    }

    public override fun hashCode(): Int = textValue.hashCode()
  }
}
