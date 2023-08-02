/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.DeviceTypes

val DEVICE_TYPE_STRING_TO_INT_MAP =
    mapOf(
        DeviceTypes.UNKNOWN to Device.TYPE_UNKNOWN,
        DeviceTypes.CHEST_STRAP to Device.TYPE_CHEST_STRAP,
        DeviceTypes.FITNESS_BAND to Device.TYPE_FITNESS_BAND,
        DeviceTypes.HEAD_MOUNTED to Device.TYPE_HEAD_MOUNTED,
        DeviceTypes.PHONE to Device.TYPE_PHONE,
        DeviceTypes.RING to Device.TYPE_RING,
        DeviceTypes.SCALE to Device.TYPE_SCALE,
        DeviceTypes.SMART_DISPLAY to Device.TYPE_SMART_DISPLAY,
        DeviceTypes.WATCH to Device.TYPE_WATCH
    )

val DEVICE_TYPE_INT_TO_STRING_MAP: Map<Int, String> =
    DEVICE_TYPE_STRING_TO_INT_MAP.entries.associate { it.value to it.key }
