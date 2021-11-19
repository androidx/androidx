/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.performance

import android.content.Context
import android.os.Build

/**
 * Reports the media performance class of the device.
 * @param context ApplicationContext
 */
class PerformanceClass(private val context: Context) {

    /**
     * The media performance class of the device or 0 if none.
     * <p>
     * If this value is not <code>0</code>, the device conforms to the media performance class
     * definition of the SDK version of this value. This value never changes while a device is
     * booted, but it may increase when the hardware manufacturer provides an OTA update.
     * <p>
     * Possible non-zero values are defined in {@link Build.VERSION_CODES} starting with
     * {@link Build.VERSION_CODES#R}.
     * <p>
     * Defaults to {@link Build.MEDIA_PERFORMANCE_CLASS}
     */
    fun getMediaPerformanceClass(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Build.VERSION.MEDIA_PERFORMANCE_CLASS
        }
        return 0
    }
}
