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

@file:RequiresApi(24)

package androidx.benchmark

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.os.PowerManager
import androidx.annotation.RequiresApi

internal fun Instrumentation.isSustainedPerformanceModeSupported(): Boolean {
    val powerManager = targetContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isSustainedPerformanceModeSupported
}

internal fun Activity.setSustainedPerformanceMode(enable: Boolean) {
    window.setSustainedPerformanceMode(enable)
}