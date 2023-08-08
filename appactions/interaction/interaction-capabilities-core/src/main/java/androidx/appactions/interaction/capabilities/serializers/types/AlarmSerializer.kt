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

import androidx.appactions.builtintypes.properties.DisambiguatingDescription
import androidx.appactions.builtintypes.types.Alarm
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.serializers.properties.NAME_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.createDisambiguatingDescriptionTypeSpec

private val supportedDisambiguatingDescriptionValues = listOf(
  Alarm.DisambiguatingDescriptionValue.FAMILY_BELL
)

val ALARM_DISAMBIGUATING_DESCRIPTION_VALUE_TYPE_SPEC = TypeSpec.createStringBasedTypeSpec<
  DisambiguatingDescription.CanonicalValue
>(
  toString = DisambiguatingDescription.CanonicalValue::textValue,
  fromString = {
    str ->
    supportedDisambiguatingDescriptionValues.find {
      it.textValue == str
    } ?: throw StructConversionException(
      "failed to deserialize Alarm.DisambiguatingDescriptionValue with textValue=$str"
    )
  }
)

val ALARM_TYPE_SPEC: TypeSpec<Alarm> = TypeSpecBuilder.newBuilder(
  "Alarm",
  Alarm::Builder,
  Alarm.Builder<*>::build
).bindSpecField(
  "alarmSchedule",
  { it.alarmSchedule },
  Alarm.Builder<*>::setAlarmSchedule,
  SCHEDULE_TYPE_SPEC
).bindSpecField(
  "disambiguatingDescription",
  { it.disambiguatingDescription },
  Alarm.Builder<*>::setDisambiguatingDescription,
  createDisambiguatingDescriptionTypeSpec(ALARM_DISAMBIGUATING_DESCRIPTION_VALUE_TYPE_SPEC)
).bindSpecField(
  "name",
  { it.name },
  Alarm.Builder<*>::setName,
  NAME_TYPE_SPEC
).bindStringField(
  "identifier",
  { it.identifier.ifEmpty { null } },
  Alarm.Builder<*>::setIdentifier
).bindIdentifier {
  it.identifier.ifEmpty { null }
}.build()
