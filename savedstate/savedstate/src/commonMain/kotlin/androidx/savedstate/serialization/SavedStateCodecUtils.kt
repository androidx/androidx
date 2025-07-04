/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.savedstate.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.BooleanArraySerializer
import kotlinx.serialization.builtins.CharArraySerializer
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer

internal val intListDescriptor = ListSerializer(Int.serializer()).descriptor
internal val stringListDescriptor = ListSerializer(String.serializer()).descriptor
internal val booleanArrayDescriptor = BooleanArraySerializer().descriptor
internal val charArrayDescriptor = CharArraySerializer().descriptor
internal val doubleArrayDescriptor = DoubleArraySerializer().descriptor
internal val floatArrayDescriptor = FloatArraySerializer().descriptor
internal val intArrayDescriptor = IntArraySerializer().descriptor
internal val longArrayDescriptor = LongArraySerializer().descriptor
@OptIn(ExperimentalSerializationApi::class)
internal val stringArrayDescriptor = ArraySerializer(String.serializer()).descriptor
