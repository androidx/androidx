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

package androidx.appactions.interaction.capabilities.serializers.properties

import androidx.appactions.builtintypes.properties.RepeatFrequency
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec

/** Note: The order of [bindMemberType] calls matters here. */
val REPEAT_FREQUENCY_TYPE_SPEC = UnionTypeSpec.Builder<RepeatFrequency>().bindMemberType(
  memberGetter = RepeatFrequency::asDuration,
  ctor = { RepeatFrequency(it) },
  typeSpec = TypeSpec.DURATION_TYPE_SPEC
).bindMemberType(
  memberGetter = RepeatFrequency::asText,
  ctor = { RepeatFrequency(it) },
  typeSpec = TypeSpec.STRING_TYPE_SPEC
).build()
