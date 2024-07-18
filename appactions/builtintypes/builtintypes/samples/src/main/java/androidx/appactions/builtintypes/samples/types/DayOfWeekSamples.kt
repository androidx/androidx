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
package androidx.appactions.builtintypes.samples.types

import androidx.`annotation`.Sampled
import androidx.appactions.builtintypes.types.DayOfWeek
import kotlin.String

@Sampled
public fun dayOfWeekMapWhenUsage(dayOfWeek: DayOfWeek) =
  dayOfWeek.mapWhen(
    object : DayOfWeek.Mapper<String> {
      public override fun friday(): String = "Got Friday"

      public override fun monday(): String = "Got Monday"

      public override fun publicHolidays(): String = "Got PublicHolidays"

      public override fun saturday(): String = "Got Saturday"

      public override fun sunday(): String = "Got Sunday"

      public override fun thursday(): String = "Got Thursday"

      public override fun tuesday(): String = "Got Tuesday"

      public override fun wednesday(): String = "Got Wednesday"

      public override fun orElse(): String = """Got some unrecognized DayOfWeek: $dayOfWeek"""
    }
  )
