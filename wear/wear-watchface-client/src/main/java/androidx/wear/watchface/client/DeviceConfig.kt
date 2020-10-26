/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.client

import androidx.annotation.IntDef

/** @hide */
@IntDef(
    value = [
        ScreenShape.ROUND,
        ScreenShape.RECTANGULAR
    ]
)
public annotation class ScreenShape {
    public companion object {
        /** The watch screen has a circular shape. */
        public const val ROUND: Int = androidx.wear.watchface.data.DeviceConfig.SCREEN_SHAPE_ROUND

        /** The watch screen has a rectangular or square shape. */
        public const val RECTANGULAR: Int =
            androidx.wear.watchface.data.DeviceConfig.SCREEN_SHAPE_RECTANGULAR
    }
}

/** Describes the hardware configuration of the device the watch face is running on. */
public class DeviceConfig(
    /** Whether or not the watch hardware supports low bit ambient support. */
    @get:JvmName("hasLowBitAmbient")
    public var hasLowBitAmbient: Boolean,

    /** Whether or not the watch hardware supports burn in protection. */
    @get:JvmName("hasBurnInProtection")
    public var hasBurnInProtection: Boolean,

    /** Describes the shape of the screen of the device the watch face is running on.*/
    @ScreenShape
    public var screenShape: Int
)
