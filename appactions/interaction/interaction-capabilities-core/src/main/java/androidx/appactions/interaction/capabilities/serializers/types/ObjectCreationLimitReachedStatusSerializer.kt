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

import androidx.appactions.builtintypes.types.ObjectCreationLimitReachedStatus
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpecBuilder
import androidx.appactions.interaction.capabilities.serializers.properties.NAME_TYPE_SPEC
import androidx.appactions.interaction.capabilities.serializers.properties.TEXT_ONLY_DISAMBIGUATING_DESCRIPTION_TYPE_SPEC

val OBJECT_CREATION_LIMIT_REACHED_STATUS_TYPE_SPEC: TypeSpec<ObjectCreationLimitReachedStatus> =
TypeSpecBuilder.newBuilder(
  "ObjectCreationLimitReachedStatus",
  ObjectCreationLimitReachedStatus::Builder,
  ObjectCreationLimitReachedStatus.Builder<*>::build
).bindSpecField(
  "disambiguatingDescription",
  { it.disambiguatingDescription },
  ObjectCreationLimitReachedStatus.Builder<*>::setDisambiguatingDescription,
  TEXT_ONLY_DISAMBIGUATING_DESCRIPTION_TYPE_SPEC
).bindSpecField(
  "name",
  { it.name },
  ObjectCreationLimitReachedStatus.Builder<*>::setName,
  NAME_TYPE_SPEC
).bindStringField(
  "identifier",
  { it.identifier.ifEmpty { null } },
  ObjectCreationLimitReachedStatus.Builder<*>::setIdentifier
).bindIdentifier {
  it.identifier.ifEmpty { null }
}.build()
