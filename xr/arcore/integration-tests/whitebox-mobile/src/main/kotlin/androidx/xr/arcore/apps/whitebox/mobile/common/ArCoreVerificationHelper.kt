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

package androidx.xr.arcore.apps.whitebox.mobile.common

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateResult
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Observer class to manage the device ARCore verification on the lifecycle owner (activity) before
 * the session is created.
 */
class ArCoreVerificationHelper(
    val activity: ComponentActivity,
    val onArCoreVerified: () -> Unit = {},
    private val arCoreApkInstance: ArCoreApk = ArCoreApk.getInstance(),
) : DefaultLifecycleObserver {

    private var installRequested: Boolean = false
    private var queryRequested: Boolean = false
    private var requestBehavior: ArCoreApk.InstallBehavior = ArCoreApk.InstallBehavior.OPTIONAL
    private var requestMessageType: ArCoreApk.UserMessageType = ArCoreApk.UserMessageType.FEATURE

    override fun onResume(owner: LifecycleOwner) {
        if (installRequested) {
            requestInstallLogic()
        }
    }

    public fun getArCoreAPKAvailability(): ArCoreApk.Availability {
        return arCoreApkInstance.checkAvailability(activity)
    }

    public fun installArCore(
        behavior: ArCoreApk.InstallBehavior = ArCoreApk.InstallBehavior.OPTIONAL,
        messageType: ArCoreApk.UserMessageType = ArCoreApk.UserMessageType.FEATURE,
    ) {
        if (installRequested) {
            return
        }
        requestBehavior = behavior
        requestMessageType = messageType
        requestInstallLogic()
    }

    public fun queryArCoreAvailability() {
        if (queryRequested) {
            return
        }
        queryRequested = true
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) { remoteQueryArCoreAvailability() }
            } catch (e: Exception) {
                showErrorMessage(activity, "ARCore verification failed: ${e.message}")
            }
        }
    }

    public fun handleSessionCreateActionRequired(result: SessionCreateResult) {
        when (result) {
            is SessionCreateApkRequired -> {
                if (result.requiredApk == ARCORE_PACKAGE_NAME) {
                    when (getArCoreAPKAvailability()) {
                        ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                            onArCoreVerified()
                        }
                        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                            installArCore()
                        }
                        ArCoreApk.Availability.UNKNOWN_CHECKING,
                        ArCoreApk.Availability.UNKNOWN_ERROR,
                        ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                            queryArCoreAvailability()
                        }
                        else -> {
                            showErrorMessage(
                                activity,
                                "Failed to create session: ${ARCORE_PACKAGE_NAME} availability error.",
                            )
                        }
                    }
                } else {
                    showErrorMessage(
                        activity,
                        "Session Create failed because of missing required APK: ${result.requiredApk}",
                    )
                    activity.finish()
                }
            }
            else -> {
                showErrorMessage(activity, "Session Create due to: ${result}")
                activity.finish()
            }
        }
    }

    private fun requestInstallLogic() {
        try {
            // Request ARCore installation or update if needed.
            when (
                arCoreApkInstance.requestInstall(
                    activity,
                    !installRequested,
                    requestBehavior,
                    requestMessageType,
                )
            ) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    Log.i(TAG, "ARCore installation requested.")
                    installRequested = true
                    return
                }
                ArCoreApk.InstallStatus.INSTALLED -> {
                    installRequested = false
                    onArCoreVerified()
                    return
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            throw RuntimeException("ARCore not installed and user declined to install.", e)
        }
    }

    suspend fun remoteQueryArCoreAvailability() {
        var attempt = 0
        while (queryRequested) {
            // TODO: b/414854001 - Implement exponential backoff.
            delay(REQUEST_DELAY_BETWEEN_ATTEMPTS_MS)
            when (arCoreApkInstance.checkAvailability(activity)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    queryRequested = false
                    onArCoreVerified()
                }
                ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                    attempt++
                    if (attempt >= REQUEST_MAX_ATTEMPTS) {
                        continue
                    } else {
                        queryRequested = false
                        throw RuntimeException(
                            "Unable to determine ARCore availability due to timeout after $REQUEST_MAX_ATTEMPTS attempts."
                        )
                    }
                }
                else -> {
                    queryRequested = false
                    throw RuntimeException("Unable to determine ARCore availability.")
                }
            }
        }
    }

    companion object {
        private val TAG = this::class.simpleName

        private fun <F> showErrorMessage(activity: ComponentActivity, error: F) {
            Log.e(TAG, error.toString())
            Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show()
        }

        const val REQUEST_DELAY_BETWEEN_ATTEMPTS_MS: Long = 200L
        const val REQUEST_MAX_ATTEMPTS: Int = 5
        const private val ARCORE_PACKAGE_NAME = "com.google.ar.core"
    }
}
