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

package androidx.core.performance.testapp

import android.app.Application
import androidx.core.performance.DefaultDevicePerformance
import androidx.core.performance.DevicePerformance
import androidx.core.performance.testlib.HasDevicePerformance

/** Sample Media Performance Class Application backed by [DefaultDevicePerformance]. */
class MpcApp : Application(), HasDevicePerformance {

    private lateinit var devicePerformance: DevicePerformance

    override fun onCreate() {
        super.onCreate()
        devicePerformance = DefaultDevicePerformance()
    }

    override fun getDevicePerformance(): DevicePerformance {
        return devicePerformance
    }
}
