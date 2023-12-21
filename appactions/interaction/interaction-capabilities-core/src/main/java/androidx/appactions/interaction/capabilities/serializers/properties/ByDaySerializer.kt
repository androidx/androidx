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

import androidx.appactions.builtintypes.properties.ByDay
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec
import androidx.appactions.interaction.capabilities.serializers.types.DAY_OF_WEEK_TYPE_SPEC

val BY_DAY_TYPE_SPEC = UnionTypeSpec.Builder<ByDay>().bindMemberType(
  memberGetter = ByDay::asDayOfWeek,
  ctor = { ByDay(it) },
  typeSpec = DAY_OF_WEEK_TYPE_SPEC,
).bindMemberType(
  memberGetter = ByDay::asText,
  ctor = { ByDay(it) },
  typeSpec = TypeSpec.STRING_TYPE_SPEC,
).build()
