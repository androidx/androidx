/*
 * Copyright (C) 2022 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.impl.converters.records

import androidx.annotation.RestrictTo
import androidx.health.platform.client.proto.DataProto

/**
 * Provides conversion to proto value objects.
 *
 * @suppress
 */
internal fun longVal(value: Long): DataProto.Value =
    DataProto.Value.newBuilder().setLongVal(value).build()

internal fun doubleVal(value: Double): DataProto.Value =
    DataProto.Value.newBuilder().setDoubleVal(value).build()

internal fun stringVal(value: String): DataProto.Value =
    DataProto.Value.newBuilder().setStringVal(value).build()

internal fun enumVal(value: String): DataProto.Value =
    DataProto.Value.newBuilder().setEnumVal(value).build()

internal fun boolVal(value: Boolean): DataProto.Value =
    DataProto.Value.newBuilder().setBooleanVal(value).build()

internal fun enumValFromInt(value: Int, intToStringMap: Map<Int, String>): DataProto.Value? {
    return intToStringMap[value]?.let(::enumVal)
}
