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

package androidx.xr.projected

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * An Activity to request permissions on the host display. It finishes itself when it receives a
 * result from a permission request or when the target projected device disconnects.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalProjectedApi::class)
internal class TrampolineRequestPermissionsOnHostActivity : Activity() {

    private val activityScope = CoroutineScope(coroutineContext + Job())
    private var resultReceiver: ResultReceiver? = null
    private var hasSentResult = false
    // VirtualDevice ID for which the system permission dialog was launched, persisted across
    // configuration changes so we can detect if that specific device disconnects or is replaced.
    private var inFlightDeviceId: Int = Context.DEVICE_ID_INVALID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultReceiver =
            intent.getParcelableExtra(
                ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER,
                ResultReceiver::class.java,
            )

        if (savedInstanceState != null) {
            hasSentResult = savedInstanceState.getBoolean(KEY_HAS_SENT_RESULT, false)
            inFlightDeviceId =
                savedInstanceState.getInt(KEY_IN_FLIGHT_DEVICE_ID, Context.DEVICE_ID_INVALID)
        }

        // Finish immediately if required extras are missing, if a result has already been sent, or
        // if this instance was recreated by the FLAG_ACTIVITY_CLEAR_TOP self-intent after the
        // system evicted the original trampoline instance while it was stopped behind
        // GrantPermissionsActivity.
        if (
            resultReceiver == null ||
                hasSentResult ||
                intent.getBooleanExtra(EXTRA_DISMISS_ON_DISCONNECT, false)
        ) {
            sendCanceledAndFinish()
            return
        }

        val permissions =
            intent.getStringArrayExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS)

        if (permissions.isNullOrEmpty()) {
            sendCanceledAndFinish()
            return
        }

        // Only launch the system permission dialog on initial creation so configuration changes do
        // not spawn duplicate permission dialogs.
        if (savedInstanceState == null) {
            try {
                val targetDeviceId = ProjectedContext.createProjectedDeviceContext(this).deviceId
                requestPermissions(permissions, PERMISSION_REQUEST_CODE, targetDeviceId)
                inFlightDeviceId = targetDeviceId
            } catch (e: RuntimeException) {
                when (e) {
                    // The projected device disconnected before or during the requestPermissions
                    // call (e.g., IllegalStateException when resolving the projected device context
                    // or IllegalArgumentException/SecurityException if the deviceId became
                    // invalid).
                    is IllegalStateException,
                    is IllegalArgumentException,
                    is SecurityException -> {
                        sendCanceledAndFinish()
                        return
                    }
                    else -> throw e
                }
            }
        } else if (!isTargetProjectedDeviceConnected(this, inFlightDeviceId)) {
            onTargetProjectedDeviceDisconnected()
            return
        }

        // Monitor the target projected device on API 36+ (where
        // ProjectedContext.isProjectedDeviceConnected is available) so an in-flight host permission
        // dialog is dismissed if the projected device disconnects while the dialog is showing.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                inFlightDeviceId != Context.DEVICE_ID_INVALID &&
                !hasSentResult
        ) {
            observeProjectedDeviceDisconnect(inFlightDeviceId)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HAS_SENT_RESULT, hasSentResult)
        outState.putInt(KEY_IN_FLIGHT_DEVICE_ID, inFlightDeviceId)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Once the FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP self-intent arrives here,
        // WindowManager has already finished the GrantPermissionsActivity stacked above this
        // trampoline, so we can now finish the trampoline itself.
        if (intent.getBooleanExtra(EXTRA_DISMISS_ON_DISCONNECT, false)) {
            sendCanceledAndFinish()
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
        if (!isChangingConfigurations) {
            sendCanceledResultIfNeeded()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }
        // GrantPermissionsActivity delivers empty arrays when the permission request is canceled
        // (e.g., via back navigation or when cleared by FLAG_ACTIVITY_CLEAR_TOP on disconnect).
        if (hasSentResult || permissions.isEmpty()) {
            sendCanceledAndFinish()
            return
        }
        activityScope.cancel()
        resultReceiver?.send(
            RESULT_OK,
            Bundle().apply {
                putStringArray(ProjectedPermissionsConstants.RESULT_DATA_PERMISSIONS, permissions)
                putIntArray(ProjectedPermissionsConstants.RESULT_DATA_GRANT_RESULTS, grantResults)
            },
        )
        hasSentResult = true
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun observeProjectedDeviceDisconnect(targetDeviceId: Int) {
        val appContext = applicationContext
        // Observe projected device connectivity and query VirtualDeviceManager/DisplayManager on
        // the configured CoroutineContext, then hop back to the main thread to dismiss the dialog.
        activityScope.launch {
            ProjectedContext.isProjectedDeviceConnected(appContext, this.coroutineContext).first {
                isConnected ->
                !isConnected || !isTargetProjectedDeviceConnected(appContext, targetDeviceId)
            }
            if (isActive) {
                runOnUiThread { onTargetProjectedDeviceDisconnected() }
            }
        }
    }

    // Verifies that a projected device is connected and that its deviceId still matches the
    // targetDeviceId for which the dialog was launched.
    private fun isTargetProjectedDeviceConnected(
        context: Context,
        targetDeviceId: Int,
    ): Boolean {
        if (targetDeviceId == Context.DEVICE_ID_INVALID) {
            return false
        }
        return try {
            ProjectedContext.createProjectedDeviceContext(context).deviceId == targetDeviceId
        } catch (e: IllegalStateException) {
            false
        }
    }

    private fun onTargetProjectedDeviceDisconnected() {
        if (hasSentResult || isFinishing || isDestroyed || isChangingConfigurations) {
            return
        }
        sendCanceledResultIfNeeded()
        intent.putExtra(EXTRA_DISMISS_ON_DISCONNECT, true)
        // Simply calling finish() on this trampoline activity does not dismiss the system
        // GrantPermissionsActivity stacked above it in the task. Launching a self-intent with
        // FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP instructs WindowManager to finish
        // all activities above this instance (dismissing the permission dialog) and deliver the
        // intent to onNewIntent(), where this trampoline finishes itself.
        startActivity(
            Intent(this, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_DISMISS_ON_DISCONNECT, true)
            }
        )
    }

    private fun sendCanceledAndFinish() {
        sendCanceledResultIfNeeded()
        finish()
    }

    private fun sendCanceledResultIfNeeded() {
        activityScope.cancel()
        if (!hasSentResult) {
            resultReceiver?.send(RESULT_CANCELED, Bundle.EMPTY)
            hasSentResult = true
        }
    }

    internal companion object {
        @VisibleForTesting const val PERMISSION_REQUEST_CODE = 105
        @VisibleForTesting const val EXTRA_DISMISS_ON_DISCONNECT = "dismiss_on_disconnect"
        private const val KEY_HAS_SENT_RESULT = "has_sent_result"
        private const val KEY_IN_FLIGHT_DEVICE_ID = "in_flight_device_id"
        @VisibleForTesting var coroutineContext: CoroutineContext = EmptyCoroutineContext
    }
}
