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
package androidx.health.data.client.metadata

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/** List of supported device types on Health Platform. */
public object DeviceTypes {
    const val UNKNOWN = "UNKNOWN"
    const val WATCH = "WATCH"
    const val PHONE = "PHONE"
    const val SCALE = "SCALE"
    const val RING = "RING"
    const val HEAD_MOUNTED = "HEAD_MOUNTED"
    const val FITNESS_BAND = "FITNESS_BAND"
    const val CHEST_STRAP = "CHEST_STRAP"
    const val SMART_DISPLAY = "SMART_DISPLAY"
}

/**
 * List of supported device types on Health Platform.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            DeviceTypes.UNKNOWN,
            DeviceTypes.WATCH,
            DeviceTypes.PHONE,
            DeviceTypes.SCALE,
            DeviceTypes.RING,
            DeviceTypes.HEAD_MOUNTED,
            DeviceTypes.FITNESS_BAND,
            DeviceTypes.CHEST_STRAP,
            DeviceTypes.SMART_DISPLAY,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class DeviceType
