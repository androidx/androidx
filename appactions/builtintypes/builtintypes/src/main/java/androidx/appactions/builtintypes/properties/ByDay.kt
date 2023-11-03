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
 * * [DayOfWeek]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:ByDay")
public class ByDay
internal constructor(
  /** The [DayOfWeek] variant, or null if constructed using a different variant. */
  @get:JvmName("asDayOfWeek")
  @get:Document.StringProperty(serializer = DayOfWeekAsCanonicalUrlSerializer::class)
  public val asDayOfWeek: DayOfWeek? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
  /** Constructor for the [DayOfWeek] variant. */
  public constructor(dayOfWeek: DayOfWeek) : this(asDayOfWeek = dayOfWeek)

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
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
    if (asDayOfWeek != other.asDayOfWeek) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asDayOfWeek)
}
