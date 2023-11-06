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
package androidx.appactions.builtintypes.types

import kotlin.String
import kotlin.collections.List
import kotlin.collections.listOf
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * The day of the week.
 *
 * See https://schema.org/DayOfWeek for context.
 *
 * Represents an open enum. See [Companion] for the different possible variants. More variants may
 * be added over time.
 */
public class DayOfWeek
private constructor(
  public val canonicalUrl: String,
) {
  /**
   * Maps each of the possible variants to some [R].
   *
   * A visitor can be provided to handle the possible variants. A catch-all default case must be
   * provided in case a new variant is added in a future release of this library.
   *
   * @sample [androidx.appactions.builtintypes.samples.types.dayOfWeekMapWhenUsage]
   */
  public fun <R> mapWhen(mapper: Mapper<R>): R =
    when (this) {
      FRIDAY -> mapper.friday()
      MONDAY -> mapper.monday()
      PUBLIC_HOLIDAYS -> mapper.publicHolidays()
      SATURDAY -> mapper.saturday()
      SUNDAY -> mapper.sunday()
      THURSDAY -> mapper.thursday()
      TUESDAY -> mapper.tuesday()
      WEDNESDAY -> mapper.wednesday()
      else -> mapper.orElse()
    }

  public override fun toString(): String = """DayOfWeek($canonicalUrl)"""

  public companion object {
    /** The day of the week between Thursday and Saturday. */
    @JvmField public val FRIDAY: DayOfWeek = DayOfWeek(canonicalUrl = "http://schema.org/Friday")

    /** The day of the week between Sunday and Tuesday. */
    @JvmField public val MONDAY: DayOfWeek = DayOfWeek(canonicalUrl = "http://schema.org/Monday")

    /**
     * This stands for any day that is a public holiday; it is a placeholder for all official public
     * holidays in some particular location. While not technically a "day of the week", it can be
     * used with `OpeningHoursSpecification`. In the context of an opening hours specification it
     * can be used to indicate opening hours on public holidays, overriding general opening hours
     * for the day of the week on which a public holiday occurs.
     */
    @JvmField
    public val PUBLIC_HOLIDAYS: DayOfWeek =
      DayOfWeek(canonicalUrl = "http://schema.org/PublicHolidays")

    /** The day of the week between Friday and Sunday. */
    @JvmField
    public val SATURDAY: DayOfWeek = DayOfWeek(canonicalUrl = "http://schema.org/Saturday")

    /** The day of the week between Saturday and Monday. */
    @JvmField public val SUNDAY: DayOfWeek = DayOfWeek(canonicalUrl = "http://schema.org/Sunday")

    /** The day of the week between Wednesday and Friday. */
    @JvmField
    public val THURSDAY: DayOfWeek = DayOfWeek(canonicalUrl = "http://schema.org/Thursday")

    /** The day of the week between Monday and Wednesday. */
    @JvmField public val TUESDAY: DayOfWeek = DayOfWeek(canonicalUrl = "http://schema.org/Tuesday")

    /** The day of the week between Tuesday and Thursday. */
    @JvmField
    public val WEDNESDAY: DayOfWeek = DayOfWeek(canonicalUrl = "http://schema.org/Wednesday")

    @JvmStatic
    public fun values(): List<DayOfWeek> =
      listOf(FRIDAY, MONDAY, PUBLIC_HOLIDAYS, SATURDAY, SUNDAY, THURSDAY, TUESDAY, WEDNESDAY)
  }

  /** Maps each of the possible variants of [DayOfWeek] to some [R]. */
  public interface Mapper<R> {
    /** Returns some [R] when the [DayOfWeek] is [FRIDAY]. */
    public fun friday(): R = orElse()

    /** Returns some [R] when the [DayOfWeek] is [MONDAY]. */
    public fun monday(): R = orElse()

    /** Returns some [R] when the [DayOfWeek] is [PUBLIC_HOLIDAYS]. */
    public fun publicHolidays(): R = orElse()

    /** Returns some [R] when the [DayOfWeek] is [SATURDAY]. */
    public fun saturday(): R = orElse()

    /** Returns some [R] when the [DayOfWeek] is [SUNDAY]. */
    public fun sunday(): R = orElse()

    /** Returns some [R] when the [DayOfWeek] is [THURSDAY]. */
    public fun thursday(): R = orElse()

    /** Returns some [R] when the [DayOfWeek] is [TUESDAY]. */
    public fun tuesday(): R = orElse()

    /** Returns some [R] when the [DayOfWeek] is [WEDNESDAY]. */
    public fun wednesday(): R = orElse()

    /** The catch-all handler that is invoked when a particular variant isn't explicitly handled. */
    public fun orElse(): R
  }
}
