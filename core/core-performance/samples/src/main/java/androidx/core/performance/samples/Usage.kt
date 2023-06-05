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

package androidx.core.performance.samples

import android.app.Application
import android.os.Build
import androidx.annotation.Sampled
import androidx.core.performance.DevicePerformance
import androidx.core.performance.StaticDevicePerformanceSupplier

@Sampled
fun usage() {

    class MyApplication : Application() {

        private lateinit var devicePerformance: DevicePerformance

        override fun onCreate() {
            devicePerformance =
                StaticDevicePerformanceSupplier.createDevicePerformance()
        }

        fun doSomeThing() {
            when {
                devicePerformance.mediaPerformanceClass >= Build.VERSION_CODES.S -> {
                    // Provide the most premium experience for highest performing devices
                }
                devicePerformance.mediaPerformanceClass == Build.VERSION_CODES.R -> {
                    // Provide a high quality experience
                }
                else -> {
                    // Remove extras to keep experience functional
                }
            }
        }
    }
}
