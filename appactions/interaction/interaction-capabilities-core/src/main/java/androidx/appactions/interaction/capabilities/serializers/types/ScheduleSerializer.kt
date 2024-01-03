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

package androidx.appactions.interaction.capabilities.serializers.types

import androidx.appactions.builtintypes.types.Schedule
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpecBuilder
import androidx.appactions.interaction.capabilities.serializers.properties.BY_DAY_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.END_DATE_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.END_TIME_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.EXCEPT_DATE_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.NAME_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.REPEAT_FREQUENCY_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.START_DATE_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.START_TIME_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.TEXT_ONLY_DISAMBIGUATING_DESCRIPTION_TYPE_SPEC

val SCHEDULE_TYPE_SPEC: TypeSpec<Schedule> = TypeSpecBuilder.newBuilder(
  "Schedule",
  Schedule::Builder,
  Schedule.Builder<*>::build
).bindRepeatedSpecField(
  "byDays",
  { it.byDays },
  Schedule.Builder<*>::addByDays,
  BY_DAY_TYPE_SPEC
).bindRepeatedSpecField(
  "byMonths",
  { it.byMonths },
  Schedule.Builder<*>::addByMonths,
  TypeSpec.LONG_TYPE_SPEC
).bindRepeatedSpecField(
  "byMonthDays",
  { it.byMonthDays },
  Schedule.Builder<*>::addByMonthDays,
  TypeSpec.LONG_TYPE_SPEC
).bindRepeatedSpecField(
  "byMonthWeeks",
  { it.byMonthWeeks },
  Schedule.Builder<*>::addByMonthWeeks,
  TypeSpec.LONG_TYPE_SPEC
).bindSpecField(
  "endDate",
  { it.endDate },
  Schedule.Builder<*>::setEndDate,
  END_DATE_TYPE_SPEC
).bindSpecField(
  "endTime",
  { it.endTime },
  Schedule.Builder<*>::setEndTime,
  END_TIME_TYPE_SPEC
).bindSpecField(
  "exceptDate",
  { it.exceptDate },
  Schedule.Builder<*>::setExceptDate,
  EXCEPT_DATE_TYPE_SPEC
).bindSpecField(
  "repeatCount",
  { it.repeatCount },
  Schedule.Builder<*>::setRepeatCount,
  TypeSpec.LONG_TYPE_SPEC
).bindSpecField(
  "repeatFrequency",
  { it.repeatFrequency },
  Schedule.Builder<*>::setRepeatFrequency,
  REPEAT_FREQUENCY_TYPE_SPEC
).bindStringField(
  "scheduleTimeZone",
  { it.scheduleTimezone },
  Schedule.Builder<*>::setScheduleTimezone
).bindSpecField(
  "startDate",
  { it.startDate },
  Schedule.Builder<*>::setStartDate,
  START_DATE_TYPE_SPEC
).bindSpecField(
  "startTime",
  { it.startTime },
  Schedule.Builder<*>::setStartTime,
  START_TIME_TYPE_SPEC
).bindSpecField(
  "disambiguatingDescription",
  { it.disambiguatingDescription },
  Schedule.Builder<*>::setDisambiguatingDescription,
  TEXT_ONLY_DISAMBIGUATING_DESCRIPTION_TYPE_SPEC
).bindSpecField(
  "name",
  { it.name },
  Schedule.Builder<*>::setName,
  NAME_TYPE_SPEC
).bindStringField(
  "identifier",
  { it.identifier.ifEmpty { null } },
  Schedule.Builder<*>::setIdentifier
).bindIdentifier {
  it.identifier.ifEmpty { null }
}.build()
