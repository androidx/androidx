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

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.annotation.Sampled
import androidx.core.performance.DevicePerformance
import androidx.core.performance.play.services.PlayServicesDevicePerformance

@Sampled
fun usage() {

    class MyApplication : Application() {
        lateinit var devicePerformance: DevicePerformance

        override fun onCreate() {
            // use a DevicePerformance derived class
            devicePerformance = PlayServicesDevicePerformance(applicationContext)
        }
    }

    class MyActivity : Activity() {
        private lateinit var devicePerformance: DevicePerformance
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Production applications should use a dependency framework.
            // See https://developer.android.com/training/dependency-injection for more information.
            devicePerformance = (application as MyApplication).devicePerformance
        }

        override fun onResume() {
            super.onResume()
            when {
                devicePerformance.mediaPerformanceClass >= Build.VERSION_CODES.TIRAMISU -> {
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
