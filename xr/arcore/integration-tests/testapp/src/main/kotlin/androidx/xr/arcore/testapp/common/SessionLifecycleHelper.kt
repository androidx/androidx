/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.testapp.common

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.xr.runtime.Config
import androidx.xr.runtime.Log
import androidx.xr.runtime.RequiredCalibrationType
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureCalibrationRequired
import androidx.xr.runtime.SessionConfigureGooglePlayServicesLocationLibraryNotLinked
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.manifest.EYE_TRACKING_COARSE
import androidx.xr.runtime.manifest.EYE_TRACKING_FINE
import androidx.xr.runtime.manifest.FACE_TRACKING
import androidx.xr.runtime.manifest.HAND_TRACKING
import androidx.xr.runtime.manifest.HEAD_TRACKING
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_FINE

/**
 * Observer class to manage the lifecycle of the JXR Runtime Session based on the lifecycle owner
 * (activity).
 */
class SessionLifecycleHelper(
    val activity: ComponentActivity,
    val config: Config = Config(),
    val onSessionAvailable: (Session) -> Unit = {},
    val onSessionCreateActionRequired: (SessionCreateResult) -> Unit = {},
    val onSessionCalibrationRequired: (RequiredCalibrationType) -> Unit = {},
) {

    /** Accessed through the [onSessionAvailable] callback. */
    private lateinit var session: Session
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    init {
        registerRequestPermissionLauncher(activity)
    }

    private fun registerRequestPermissionLauncher(activity: ComponentActivity) {
        requestPermissionLauncher =
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allPermissionsGranted = permissions.all { it.value }
                if (!allPermissionsGranted) {
                    Toast.makeText(
                            activity,
                            "Required permissions were not granted, closing activity. ",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    activity.finish()
                } else {
                    activity.recreate()
                }
            }
    }

    private fun getRequiredPermissions(config: Config): List<String> {
        val permissions = mutableListOf<String>()
        if (config.planeTracking != Config.PlaneTrackingMode.DISABLED) {
            permissions.add(SCENE_UNDERSTANDING_COARSE)
        }
        if (config.depthEstimation != Config.DepthEstimationMode.DISABLED) {
            permissions.add(SCENE_UNDERSTANDING_FINE)
        }
        if (config.handTracking != Config.HandTrackingMode.DISABLED) {
            permissions.add(HAND_TRACKING)
        }
        if (config.faceTracking != Config.FaceTrackingMode.DISABLED) {
            permissions.add(FACE_TRACKING)
        }
        if (config.deviceTracking != Config.DeviceTrackingMode.DISABLED) {
            permissions.add(HEAD_TRACKING)
        }
        if (config.eyeTracking == Config.EyeTrackingMode.COARSE_TRACKING) {
            permissions.add(EYE_TRACKING_COARSE)
        }
        if (config.eyeTracking == Config.EyeTrackingMode.FINE_TRACKING) {
            permissions.add(EYE_TRACKING_FINE)
        }
        if (config.geospatial == Config.GeospatialMode.VPS_AND_GPS) {
            permissions.add(ACCESS_FINE_LOCATION)
        }
        return permissions
    }

    // TODO: b/442623996 -- this code needs to be reworked to better convey
    // the correct usage pattern.
    internal fun tryCreateSession() {
        try {
            when (val result = Session.create(activity)) {
                is SessionCreateSuccess -> {
                    session = result.session
                    try {
                        when (val configResult = session.configure(config)) {
                            is SessionConfigureGooglePlayServicesLocationLibraryNotLinked -> {
                                Log.error {
                                    "Google Play Services Location Library is not linked, this should not happen."
                                }
                            }
                            is SessionConfigureCalibrationRequired -> {
                                onSessionCalibrationRequired(configResult.calibrationType)
                            }
                            is SessionConfigureSuccess -> {
                                onSessionAvailable(session)
                            }
                        }
                    } catch (e: SecurityException) {
                        requestPermissionLauncher.launch(
                            getRequiredPermissions(config).toTypedArray()
                        )
                    } catch (e: UnsupportedOperationException) {
                        showErrorMessage("Session configuration not supported.")
                        activity.finish()
                    }
                }
                is SessionCreateApkRequired -> {
                    onSessionCreateActionRequired(result)
                }
                is SessionCreateUnsupportedDevice -> {
                    showErrorMessage("Session could not be created, device is Unsupported.")
                    activity.finish()
                }
            }
        } catch (e: SecurityException) {
            requestPermissionLauncher.launch(getRequiredPermissions(config).toTypedArray())
        }
    }

    internal fun tryUpdateConfig(config: Config) {
        if (!::session.isInitialized) {
            Log.error { "Can't update config, session has not been initialized" }
            return
        }
        try {
            when (val result = session.configure(config)) {
                is SessionConfigureGooglePlayServicesLocationLibraryNotLinked -> {
                    Log.error {
                        "Google Play Services Location Library is not linked, this should not happen."
                    }
                }
                is SessionConfigureCalibrationRequired -> {
                    onSessionCalibrationRequired(result.calibrationType)
                }
                is SessionConfigureSuccess -> {
                    onSessionAvailable(session)
                }
            }
        } catch (e: SecurityException) {
            requestPermissionLauncher.launch(getRequiredPermissions(config).toTypedArray())
        } catch (e: UnsupportedOperationException) {
            showErrorMessage("Session configuration not supported.")
            activity.finish()
        }
    }

    companion object {
        private val TAG = this::class.simpleName
    }

    private fun <F> showErrorMessage(error: F) {
        Log.error { error.toString() }
        Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show()
    }
}
