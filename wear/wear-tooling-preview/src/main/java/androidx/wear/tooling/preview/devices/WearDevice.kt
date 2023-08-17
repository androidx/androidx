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

package androidx.wear.tooling.preview.devices

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/**
 * List with the pre-defined devices available to be used in previews.
 */
object WearDevices {
    // Make sure to update any @StringDefs that reference this object.
    /** Round device with 227x227dp (454x454px) dimensions, 1.39" size and xhdpi density. */
    const val LARGE_ROUND = "id:wearos_large_round"
    /** Round device with 192x192dp (384x384px) dimensions, 1.2" size and xhdpi density. */
    const val SMALL_ROUND = "id:wearos_small_round"
    /** Square device with 180x180dp (360x360px) dimensions, 1.2" size and xhdpi density. If
     * you are targeting Wear 3 or later, it is recommended to use [LARGE_ROUND] or [SMALL_ROUND]
     * instead. */
    const val SQUARE = "id:wearos_square"
    /** Rectangular device with 201x238dp (402x476px) dimensions, 1.2" size and xhdpi density. If
     * you are targeting Wear 3 or later, it is recommended to use [LARGE_ROUND] or [SMALL_ROUND]
     * instead. */
    const val RECT = "id:wearos_rect"
}

/**
 * Annotation for defining the device to use.
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    open = true,
    value = [
        WearDevices.LARGE_ROUND,
        WearDevices.SMALL_ROUND,
    ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class WearDevice
