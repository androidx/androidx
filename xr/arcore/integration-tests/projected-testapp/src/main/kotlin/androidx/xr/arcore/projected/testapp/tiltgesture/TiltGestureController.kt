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

package androidx.xr.arcore.projected.testapp.tiltgesture

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.ExperimentalGesturesApi
import androidx.xr.arcore.TiltGesture
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.ExperimentalInertialTrackingApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalGesturesApi::class, ExperimentalInertialTrackingApi::class)
internal class TiltGestureController(
    private val viewModel: TiltGestureViewModel,
    private val coroutineScope: CoroutineScope,
) {
    private lateinit var session: Session
    private var detectionJob: Job? = null

    internal suspend fun onCreate(context: Context, lifecycleOwner: LifecycleOwner) {
        val message: String
        when (val result = Session.create(context = context, lifecycleOwner = lifecycleOwner)) {
            is SessionCreateSuccess -> {
                session = result.session
                message = "Session created successfully."
                configureSession()
            }
            is SessionCreateApkRequired -> {
                message = "Session creation failed: ${result.requiredApk} is required."
            }
            is SessionCreateUnsupportedDevice -> {
                message = "Session creation failed: Device is not supported."
            }
            is SessionCreateUnknownError -> {
                message = "Session creation failed: ${result.errorMessage}"
            }
            else -> {
                message = "Session creation failed: ${result::class.simpleName}"
            }
        }
        viewModel.setMessage(message)
    }

    internal fun onDestroy() {
        detectionJob?.cancel()
    }

    private fun configureSession() {
        val message: String
        when (
            val configResult =
                session.configure(
                    Config.Builder().setDeviceTracking(DeviceTrackingMode.INERTIAL).build()
                )
        ) {
            is SessionConfigureSuccess -> {
                message = "Session configured successfully."
                startTracking()
            }
            else -> {
                message = "Session configuration failed: ${configResult::class.simpleName}"
            }
        }
        viewModel.setMessage(message)
    }

    private fun startTracking() {
        detectionJob = coroutineScope.launch {
            viewModel.setMessage("Tracking started.")
            TiltGesture.detect(session).collect {
                viewModel.setTiltGestureState(it.tilt, it.progress)
            }
        }
    }
}
