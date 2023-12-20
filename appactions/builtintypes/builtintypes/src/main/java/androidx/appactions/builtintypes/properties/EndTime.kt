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

import androidx.appactions.builtintypes.serializers.LocalTimeAsNanoOfDaySerializer
import androidx.appsearch.`annotation`.Document
import java.time.LocalTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.error
import kotlin.jvm.JvmName

/**
 * The endTime of something.
 *
 * For a reserved event or service (e.g. `FoodEstablishmentReservation`), the time that it is
 * expected to end. For actions that span a period of time, when the action was performed. E.g. John
 * wrote a book from January to *December*. For media, including audio and video, it's the time
 * offset of the end of a clip within a larger file.
 *
 * See https://schema.org/endTime for context.
 *
 * Holds one of:
 * * Time i.e. [LocalTime]
 *
 * May hold more types over time.
 */
@Document(name = "bitprop:EndTime")
public class EndTime
internal constructor(
  /** The [LocalTime] variant, or null if constructed using a different variant. */
  @get:JvmName("asTime")
  @get:Document.LongProperty(serializer = LocalTimeAsNanoOfDaySerializer::class)
  public val asTime: LocalTime? = null,
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Id @get:JvmName("getIdentifier") internal val identifier: String = "",
  /** Required ctor param for the AppSearch compiler. */
  @get:Document.Namespace @get:JvmName("getNamespace") internal val namespace: String = "",
) {
  /** Constructor for the [LocalTime] variant. */
  public constructor(time: LocalTime) : this(asTime = time)

  public override fun toString(): String = toString(includeWrapperName = true)

  internal fun toString(includeWrapperName: Boolean): String =
    when {
      asTime != null ->
        if (includeWrapperName) {
          """EndTime($asTime)"""
        } else {
          asTime.toString()
        }
      else -> error("No variant present in EndTime")
    }

  public override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is EndTime) return false
    if (asTime != other.asTime) return false
    return true
  }

  public override fun hashCode(): Int = Objects.hash(asTime)
}
