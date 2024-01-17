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

@JvmDefaultWithCompatibility
/**
 * Reports the media performance class of the device.
 *
 * Create an instance of DevicePerformance in your [android.app.Application.onCreate] and use
 * the [mediaPerformanceClass] value any time it is needed.
 * @sample androidx.core.performance.samples.usage
 *
 */
interface DevicePerformance {

    /**
     * The media performance class of the device or 0 if none.
     *
     * If this value is not <code>0</code>, the device conforms to the media performance class
     * definition of the SDK version of this value. This value is stable for the duration of
     * the process.
     *
     * Possible non-zero values are defined in
     * [Build.VERSION_CODES][android.os.Build.VERSION_CODES] starting with
     * [VERSION_CODES.R][android.os.Build.VERSION_CODES.R].
     *
     * Defaults to
     * [VERSION.MEDIA_PERFORMANCE_CLASS][android.os.Build.VERSION.MEDIA_PERFORMANCE_CLASS]
     *
     */
    val mediaPerformanceClass: Int
}
