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

import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * The name of the item.
 *
 * See http://schema.org/name for context.
 *
 * Holds one of:
 * * Text i.e. [String]
 * * [Name.CanonicalValue]
 *
 * May hold more types over time.
 */
public class Name
internal constructor(
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
   * @sample [androidx.appactions.builtintypes.samples.properties.nameMapWhenUsage]
   */
  public fun <R> mapWhen(mapper: Mapper<R>): R =
    when {
      asText != null -> mapper.text(asText)
      asCanonicalValue != null -> mapper.canonicalValue(asCanonicalValue)
      else -> error("No variant present in Name")
    }

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asText != null ->
        if (includeWrapperName) {
          """Name($asText)"""
        } else {
          asText
        }
      asCanonicalValue != null ->
        if (includeWrapperName) {
          """Name($asCanonicalValue)"""
        } else {
          asCanonicalValue.toString()
        }
      else -> error("No variant present in Name")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Name) return false
    if (asText != other.asText) return false
    if (asCanonicalValue != other.asCanonicalValue) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asText, asCanonicalValue)

  /** Maps each of the possible variants of [Name] to some [R]. */
  public interface Mapper<R> {
    /** Returns some [R] when the [Name] holds some [String] instance. */
    public fun text(instance: String): R = orElse()

    /** Returns some [R] when the [Name] holds some [CanonicalValue] instance. */
    public fun canonicalValue(instance: CanonicalValue): R = orElse()

    /** The catch-all handler that is invoked when a particular variant isn't explicitly handled. */
    public fun orElse(): R
  }

  public abstract class CanonicalValue internal constructor() {
    public abstract val textValue: String
  }
}
