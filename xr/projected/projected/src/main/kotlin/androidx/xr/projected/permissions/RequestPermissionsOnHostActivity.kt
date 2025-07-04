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

package androidx.xr.projected.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ResultReceiver
import androidx.activity.compose.setContent
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.projected.R

/**
 * An Activity to display rationale and request permissions on the host display. See
 * [ProjectedPermissionsResultContract] for details.
 */
@Suppress("ForbiddenSuperClass")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RequestPermissionsOnHostActivity : AppCompatActivity() {

    private data class PermissionRequest(
        val permissions: List<String>,
        val rationale: String?,
        val deviceId: Int,
    )

    private lateinit var requests: List<PermissionRequest>
    private lateinit var resultReceiver: ResultReceiver
    private lateinit var permissionResults: Bundle
    private var nextRequestIndex = 0
    private var pendingUserAction = false
    private var resultSent = false
    private var isInitialized = false

    /**
     * Holds the current request that requires a rationale to be shown. When this is non-null, the
     * [RequestPermissionScreen] is displayed.
     */
    private var rationaleRequest by mutableStateOf<PermissionRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(GoToHostProjectedActivity.EXTRA_SHOULD_FINISH, false)) {
            finish()
            return
        }
        initialize(savedInstanceState)
    }

    private fun initialize(savedInstanceState: Bundle?) {
        rationaleRequest = null

        resultSent = false
        nextRequestIndex =
            savedInstanceState?.getInt(INSTANCE_STATE_NEXT_REQUEST_INDEX_KEY, nextRequestIndex) ?: 0

        permissionResults =
            savedInstanceState?.getBundle(INSTANCE_STATE_PERMISSION_RESULTS_KEY) ?: Bundle()
        pendingUserAction =
            savedInstanceState?.getBoolean(
                INSTANCE_STATE_PENDING_USER_ACTION_KEY,
                pendingUserAction,
            ) == true

        val resultReceiverNullable =
            intent.getParcelableExtra(
                GoToHostProjectedActivity.EXTRA_RESULT_RECEIVER,
                ResultReceiver::class.java,
            )
        if (resultReceiverNullable == null) {
            finish()
            return
        }
        resultReceiver = resultReceiverNullable

        val deviceId =
            intent.getIntExtra(
                ProjectedPermissionsResultContract.EXTRA_DEVICE_ID,
                DEVICE_ID_INVALID,
            )
        if (deviceId == DEVICE_ID_INVALID) {
            sendResultsAndFinish() // send empty results to finish the Projected activity
            return
        }
        val requestDataBundles =
            intent.getParcelableArrayExtra(
                ProjectedPermissionsResultContract.EXTRA_PERMISSION_REQUEST_DATA,
                Bundle::class.java,
            )
        if (requestDataBundles == null || requestDataBundles.size == 0) {
            sendResultsAndFinish() // send empty results to finish the Projected activity
            return
        }
        requests = preprocessRequestDataBundles(requestDataBundles, deviceId)

        setContent {
            // The rationale UI is only composed if there is a request that needs it.
            rationaleRequest?.let { request ->
                RequestPermissionScreen(
                    rationaleText = request.rationale ?: "",
                    onContinueClick = { onContinue() },
                    onCancelClick = { onCancel() },
                )
            }
        }

        nextRequestIndex =
            savedInstanceState?.getInt(INSTANCE_STATE_NEXT_REQUEST_INDEX_KEY, nextRequestIndex)
                ?: nextRequestIndex
        permissionResults =
            savedInstanceState?.getBundle(INSTANCE_STATE_PERMISSION_RESULTS_KEY) ?: Bundle()
        pendingUserAction =
            savedInstanceState?.getBoolean(
                INSTANCE_STATE_PENDING_USER_ACTION_KEY,
                pendingUserAction,
            ) ?: pendingUserAction
        if (!pendingUserAction) {
            handleNextRequest()
        }
        isInitialized = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        deviceId: Int,
    ) {
        pendingUserAction = false
        recordPermissionResult(requestCode, permissions, grantResults)
        handleNextRequest()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putInt(INSTANCE_STATE_NEXT_REQUEST_INDEX_KEY, nextRequestIndex)
            putBundle(INSTANCE_STATE_PERMISSION_RESULTS_KEY, permissionResults)
            putBoolean(INSTANCE_STATE_PENDING_USER_ACTION_KEY, pendingUserAction)
        }
    }

    @VisibleForTesting
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(GoToHostProjectedActivity.EXTRA_SHOULD_FINISH, false)) {
            finish()
            return
        }
        // The user launched another activity that requests permissions. Send the results for the
        // previous request intent.
        sendResults()
        setIntent(intent)
        initialize(savedInstanceState = null)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isInitialized && isFinishing && !resultSent) {
            // pendingUserAction means the user is presented with the system's permission dialog.
            // Although nextRequestIndex has already advanced, the user has not acted on this
            // request, so we label it as rejected.
            val currentRequestIndex =
                if (pendingUserAction) (nextRequestIndex - 1) else nextRequestIndex
            for (i in currentRequestIndex until requests.size) {
                recordRejection(i)
            }
            sendResults()
        }
    }

    private fun handleNextRequest() {
        if (nextRequestIndex >= requests.size) {
            sendResultsAndFinish()
            return
        }
        val request = requests[nextRequestIndex]
        if (request.rationale == null) {
            requestPermissions(
                request.permissions.toTypedArray(),
                nextRequestIndex,
                request.deviceId,
            )
            pendingUserAction = true
            nextRequestIndex++
        } else {
            rationaleRequest = request
        }
    }

    private fun sendResultsAndFinish() {
        sendResults()
        finish()
    }

    private fun sendResults() {
        // the resultCode is unused
        resultReceiver.send(/* resultCode= */ 0, permissionResults)
        resultSent = true
    }

    private fun onContinue() {
        val request = requests[nextRequestIndex]
        // Hide the rationale UI before showing the system permission dialog.
        rationaleRequest = null
        requestPermissions(request.permissions.toTypedArray(), nextRequestIndex, request.deviceId)
        pendingUserAction = true
        nextRequestIndex++
    }

    /**
     * Handles the case where the user taps the cancel button on the rationale page. Rejects all
     * permissions in the current and following requests until the next request has a rationale.
     * This prevents a confusing user experience where the user is immediately prompted for a
     * permission after hitting cancel in the rationale UI.
     */
    private fun onCancel() {
        // Hide the rationale UI.
        rationaleRequest = null
        recordRejection(nextRequestIndex)
        nextRequestIndex++
        while (nextRequestIndex < requests.size && requests[nextRequestIndex].rationale == null) {
            recordRejection(nextRequestIndex)
            nextRequestIndex++
        }
        handleNextRequest()
    }

    private fun recordRejection(requestIndex: Int) {
        val request = requests[requestIndex]
        recordPermissionResult(
            requestIndex,
            request.permissions.toTypedArray(),
            IntArray(request.permissions.size) { PackageManager.PERMISSION_DENIED },
        )
    }

    private fun recordPermissionResult(
        requestIndex: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (permissions.isEmpty()) {
            // Empty means the request is cancelled, treat as denied.
            for (permission in requests[requestIndex].permissions) {
                permissionResults.putBoolean(permission, false)
            }
        } else {
            for (i in 0 until permissions.size) {
                if (
                    permissions[i] in DEVICE_SCOPED_PERMISSIONS &&
                        !permissionResults.getBoolean(permissions[i], true)
                ) {
                    // We will receive two results for device-scoped permissions, one for the host
                    // device and one for the Projected device. If any of these two is denied, we
                    // should report denied. This `continue` prevents the second result from
                    // overwriting a denied first result.
                    continue
                }
                permissionResults.putBoolean(
                    permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED,
                )
            }
        }
    }

    @Composable
    private fun RequestPermissionScreen(
        rationaleText: String,
        onContinueClick: () -> Unit,
        onCancelClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp), // Add some horizontal padding to the whole screen
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                // Adds space between the buttons.
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Button(onClick = onContinueClick, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.continue_button))
                }

                Button(onClick = onCancelClick, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.cancel_button))
                }
            }

            // The Text composable for the rationale.
            Text(
                text = rationaleText,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 30.dp),
            )
        }
    }

    private companion object {
        private const val INSTANCE_STATE_NEXT_REQUEST_INDEX_KEY = "nextRequestIndex"
        private const val INSTANCE_STATE_PERMISSION_RESULTS_KEY = "permissionResults"
        private const val INSTANCE_STATE_PENDING_USER_ACTION_KEY = "pendingUserAction"
        private val DEVICE_SCOPED_PERMISSIONS =
            setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        /**
         * Preprocesses the request data bundles into a list of {@code PermissionRequest}s.
         * Device-scoped permissions are split into separate requests for the host device and the
         * Projected device because they are both required in Android V. We expect this behavior to
         * change in a future Android release.
         */
        private fun preprocessRequestDataBundles(
            requestDataBundles: Array<Bundle>,
            deviceId: Int,
        ): List<PermissionRequest> {
            val requests = mutableListOf<PermissionRequest>()
            for (requestDataBundle in requestDataBundles) {
                var rationale =
                    requestDataBundle.getString(
                        ProjectedPermissionsResultContract.BUNDLE_KEY_RATIONALE
                    )
                val permissions =
                    requestDataBundle.getStringArrayList(
                        ProjectedPermissionsResultContract.BUNDLE_KEY_PERMISSIONS
                    )
                if (permissions == null || permissions.isEmpty()) {
                    continue
                }
                val permissionsForNextRequest = mutableListOf<String>()
                for (permission in permissions) {
                    permissionsForNextRequest.add(permission)
                    if (!DEVICE_SCOPED_PERMISSIONS.contains(permission)) {
                        continue
                    }
                    requests.add(
                        PermissionRequest(
                            permissionsForNextRequest.toList(),
                            rationale,
                            DEVICE_ID_DEFAULT,
                        )
                    )
                    permissionsForNextRequest.clear()
                    // Only the first request from the same requestDataBundle needs a rationale.
                    // Otherwise, the user will be presented with the same rationale multiple times.
                    rationale = null
                    // Create request from the current permission with the virtual device's ID.
                    requests.add(PermissionRequest(listOf(permission), rationale, deviceId))
                }
                if (permissionsForNextRequest.isNotEmpty()) {
                    // Any non-device-scoped permissions can only be requested for the host device.
                    requests.add(
                        PermissionRequest(
                            permissionsForNextRequest.toList(),
                            rationale,
                            DEVICE_ID_DEFAULT,
                        )
                    )
                }
            }
            return requests
        }
    }
}
