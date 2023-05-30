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

import androidx.appactions.builtintypes.properties.ExceptDate
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.converters.UnionTypeSpec

val EXCEPT_DATE_TYPE_SPEC = UnionTypeSpec.Builder<ExceptDate>().bindMemberType(
  memberGetter = ExceptDate::asDate,
  ctor = { ExceptDate(it) },
  typeSpec = TypeSpec.LOCAL_DATE_TYPE_SPEC
).bindMemberType(
  memberGetter = ExceptDate::asLocalDateTime,
  ctor = { ExceptDate(it) },
  typeSpec = TypeSpec.LOCAL_DATE_TIME_TYPE_SPEC
).bindMemberType(
  memberGetter = ExceptDate::asInstant,
  ctor = { ExceptDate(it) },
  typeSpec = TypeSpec.INSTANT_TYPE_SPEC
).build()
