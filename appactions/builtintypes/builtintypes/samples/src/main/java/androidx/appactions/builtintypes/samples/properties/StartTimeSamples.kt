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
package androidx.appactions.builtintypes.samples.properties

import androidx.`annotation`.Sampled
import androidx.appactions.builtintypes.properties.StartTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.String

@Sampled
public fun startTimeMapWhenUsage(startTime: StartTime) =
  startTime.mapWhen(
    object : StartTime.Mapper<String> {
      public override fun time(instance: LocalTime): String = """Got LocalTime: $instance"""

      public override fun localDateTime(instance: LocalDateTime): String =
        """Got a local DateTime: $instance"""

      public override fun instant(instance: Instant): String =
        """Got an absolute DateTime: $instance"""

      public override fun orElse(): String = """Got some unrecognized variant: $startTime"""
    }
  )
