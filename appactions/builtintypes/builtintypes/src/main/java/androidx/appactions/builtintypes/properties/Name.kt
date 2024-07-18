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

import androidx.appsearch.`annotation`.Document
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
 * See https://schema.org/name for context.
 *
 * Holds one of:
 * * Text i.e. [String]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:Name")
public class Name
internal constructor(
  /** The [String] variant, or null if constructed using a different variant. */
  @get:JvmName("asText") @get:Document.StringProperty public val asText: String? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
  /** Constructor for the [String] variant. */
  public constructor(text: String) : this(asText = text)

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asText != null ->
        if (includeWrapperName) {
          """Name($asText)"""
        } else {
          asText
        }
      else -> error("No variant present in Name")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Name) return false
    if (asText != other.asText) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asText)
}
