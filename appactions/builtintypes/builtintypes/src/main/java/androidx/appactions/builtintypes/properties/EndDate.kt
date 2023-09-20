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

import androidx.appactions.builtintypes.serializers.LocalDateAsEpochDaySerializer
import androidx.appsearch.`annotation`.Document
import java.time.LocalDate
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * The end date and time of the item.
 *
 * See https://schema.org/endDate for context.
 *
 * Holds one of:
 * * Date i.e. [LocalDate]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:EndDate")
public class EndDate
internal constructor(
  /** The [LocalDate] variant, or null if constructed using a different variant. */
  @get:JvmName("asDate")
  @get:Document.LongProperty(serializer = LocalDateAsEpochDaySerializer::class)
  public val asDate: LocalDate? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
  /** Constructor for the [LocalDate] variant. */
  public constructor(date: LocalDate) : this(asDate = date)

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asDate != null ->
        if (includeWrapperName) {
          """EndDate($asDate)"""
        } else {
          asDate.toString()
        }
      else -> error("No variant present in EndDate")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is EndDate) return false
    if (asDate != other.asDate) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asDate)
}
