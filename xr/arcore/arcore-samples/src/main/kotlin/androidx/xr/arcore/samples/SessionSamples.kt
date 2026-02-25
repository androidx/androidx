/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.samples

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Sampled
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.RequiredCalibrationType
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureCalibrationRequired
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateTimedOut
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @param activity the [ComponentActivity] to create the session in
 * @param userRequestedInstall whether the user has requested to install the ARCore APK
 */
@Sampled
fun callSessionCreate(activity: ComponentActivity, userRequestedInstall: Boolean = false) {
    // Note: registerForActivityResult must be called before the Activity is STARTED.
    // Ensure this function is called during initialization or onCreate.
    val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
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

    val config = generateConfig()

    // Launch a coroutine bound to the Activity's lifecycle
    activity.lifecycleScope.launch {
        try {
            // Switch to the IO Dispatcher for the heavy creation call
            val result = withContext(Dispatchers.IO) { Session.create(activity) }

            // Handle the result back on the initial Thread
            when (result) {
                // Having successfully created the session, we now need to configure it.
                is SessionCreateSuccess -> {
                    val session = result.session
                    try {
                        // Configure is also a potentially heavy operation, offload it as well.
                        val configResult = withContext(Dispatchers.IO) { session.configure(config) }

                        when (configResult) {
                            is SessionConfigureCalibrationRequired -> {
                                // Certain runtimes require user calibration in order to operate
                                // effectively. Either launch the calibration activity, or inform
                                // the user they must do so.
                                handleSessionCalibration(configResult.calibrationType)
                            }
                            is SessionConfigureSuccess -> {
                                // Our Session was successfully configured, so now we can continue
                                // initializing the rest of our application.
                                handleConfiguredSession(session)
                            }
                            else -> {
                                Toast.makeText(
                                        activity,
                                        "Unexpected session configuration result, closing activity. ",
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                                activity.finish()
                            }
                        }
                    } catch (e: SecurityException) {
                        // Session configuration failed due to missing permission. Try asking the
                        // user for those permissions and then relaunching the app (which will then
                        // try and create the session again).
                        permissionLauncher.launch(yourPermissionsForConfig(config).toTypedArray())
                    } catch (e: UnsupportedOperationException) {
                        // Session configuration is not supported. This means we likely won't have
                        // the required feature support, so we probably just need to report an error
                        // and then close.
                        Toast.makeText(
                                activity,
                                "Unable to configure ARCore session, closing activity. ",
                                Toast.LENGTH_LONG,
                            )
                            .show()
                        activity.finish()
                    }
                }

                is SessionCreateApkRequired -> {
                    // Certain runtimes (notably, ARCore for Play Services) require additional APKs
                    // in order to function.
                    when (
                        val result =
                            ArCoreApk.getInstance().requestInstall(activity, !userRequestedInstall)
                    ) {
                        ArCoreApk.InstallStatus.INSTALLED -> {
                            // ARCore has been successfully installed, so try running this creation
                            // function again.
                        }
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            // The current application will be paused to offer to install the ARCore
                            // apk. When it resumes, this creation function should be called again,
                            // but userRequestedInstall should be explicitly set to true.
                        }
                    }
                }
                is SessionCreateUnsupportedDevice -> {
                    // Session creation is not supported on this device, so we just need to report
                    // an error and then close.
                    Toast.makeText(
                            activity,
                            "Unable to create ARCore session; device unsupported, closing activity. ",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    activity.finish()
                }

                is SessionCreateTimedOut -> {
                    Toast.makeText(activity, "Session creation timed out.", Toast.LENGTH_LONG)
                }
                is SessionCreateUnknownError -> {
                    Toast.makeText(activity, result.errorMessage, Toast.LENGTH_LONG)
                }
            }
        } catch (e: SecurityException) {
            // Session creation failed due to missing permission. Try asking the user for those
            // permissions and then relaunching the app (which will then try and create the session
            // again).
            permissionLauncher.launch(yourPermissionsForConfig(config).toTypedArray())
        }
    }
}

private fun generateConfig(): Config = Config()

private fun handleConfiguredSession(session: Session) {}

private fun handleSessionCalibration(type: RequiredCalibrationType) {}

private fun yourPermissionsForConfig(config: Config): List<String> = emptyList()
