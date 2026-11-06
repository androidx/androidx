/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.testapp.nativedata

import android.annotation.SuppressLint
import android.content.Context
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/**
 * [NativeDataActivity] for projected OpenXR runtimes (XR glasses driven by a phone).
 *
 * It runs on the phone, started from the main activity like the other test cases, and targets the
 * connected glasses: the Session is created with the projected device context, which is bound to
 * the glasses but is not an Activity, so the Session does not attach a SceneCore Scene (this app
 * only bundles the headset SceneCore runtimes).
 *
 * It injects `XR_EXT_debug_utils`, which projected runtimes advertise but Jetpack XR does not
 * enable by default, calls one of its functions through the native `xrGetInstanceProcAddr` (test
 * 5), and shows the results in the activity window.
 */
// Projected devices need API 34 or newer.
@SuppressLint("NewApi")
@OptIn(ExperimentalProjectedApi::class)
class ProjectedNativeDataActivity : NativeDataActivity() {
    override val extensionToInject: String = "XR_EXT_debug_utils"
    override val verifyInjectedFunctionCall: Boolean = true
    override val showResultsInSpatialPanel: Boolean = false
    override val sessionContext: Context by lazy {
        ProjectedContext.createProjectedDeviceContext(this)
    }
}
