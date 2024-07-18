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

package androidx.appactions.interaction.capabilities.filters

import java.time.LocalTime

/** Filter class for [LocalTime] values. */
class TimeFilter(
  @get:JvmName("asRangeFilter")
  val asRangeFilter: TimeRangeFilter?,
  @get:JvmName("asExactTime")
  val asExactTime: LocalTime?
) {
  /** Creates a [TimeFilter] instance with a [TimeRangeFilter]. */
  constructor(rangeFilter: TimeRangeFilter) : this(rangeFilter, null)
  /** Creates a [TimeFilter] instance with a [LocalTime]. */
  constructor(exactTime: LocalTime) : this(null, exactTime)
}

/** Matches [LocalTime] values within a range. * If only minTime is non-null, any [LocalTime] at or after minTime satisfies this filter.
 * If only maxTime is non-null, any [LocalTime] at or before maxTime satisfies this filter.
 */
class TimeRangeFilter(
  /** The minimum time to satisfy this filter. (inclusive) */
  val minTime: LocalTime?,
  /** The maximum time to satisfy this filter. (inclusive) */
  val maxTime: LocalTime?
) {
  init {
    require(minTime != null || maxTime != null) {
      "at least one of 'minTime' and 'maxTime' must be non-null"
    }
  }
}
