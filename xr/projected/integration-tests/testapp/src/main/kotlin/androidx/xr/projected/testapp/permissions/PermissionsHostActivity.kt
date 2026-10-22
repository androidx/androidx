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

package androidx.xr.projected.testapp.permissions

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Activity displaying individual status of each permission and controls to request permissions. */
@OptIn(ExperimentalProjectedApi::class)
class PermissionsHostActivity : ComponentActivity() {

    private val viewModel: PermissionsViewModel by viewModels()
    private lateinit var controller: PermissionsController
    private var projectedActivity: PermissionsProjectedActivity? = null

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is PermissionsProjectedActivity) {
                    projectedActivity = activity
                    activity.viewModel = this@PermissionsHostActivity.viewModel
                    activity.onPermissionsResult = { controller.refreshPermissionsStatus() }
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity == projectedActivity) {
                    projectedActivity = null
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        controller = PermissionsController(this, viewModel, lifecycleScope)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PermissionsScreen(viewModel, controller)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        controller.refreshPermissionsStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.close()
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        projectedActivity?.finish()
    }

    /**
     * Requests the permissions for the projected device. The request must be made from the
     * projected activity, and the result is delivered to its `onRequestPermissionsResult`.
     */
    private fun requestProjectedPermissions() {
        val activity = projectedActivity
        if (activity == null) {
            viewModel.setStatusMessage("Projected activity is not running.")
            return
        }
        val missingPermissions =
            arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
                .filter {
                    ContextCompat.checkSelfPermission(activity, it) !=
                        PackageManager.PERMISSION_GRANTED
                }
        if (missingPermissions.isEmpty()) {
            controller.refreshPermissionsStatus()
            return
        }
        // ProjectedActivityCompat.requestPermissions blocks, so it must not run on the main thread.
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                ProjectedActivityCompat.requestPermissions(
                    activity,
                    missingPermissions.toTypedArray(),
                    PermissionsProjectedActivity.PERMISSION_REQUEST_CODE,
                )
            } catch (e: Exception) {
                viewModel.setStatusMessage("Permission request failed: ${e.message}")
            }
        }
    }

    @Composable
    private fun PermissionsScreen(
        viewModel: PermissionsViewModel,
        controller: PermissionsController,
    ) {
        val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
        val permissionsStatus by viewModel.permissionsStatus.collectAsStateWithLifecycle()
        val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Permissions Test",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connection: ${if (isConnected) "Connected" else "Not Connected"}",
                fontSize = 18.sp,
                color =
                    if (isConnected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Individual Permission Status:",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            for ((permName, isGranted) in permissionsStatus) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = permName,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (isGranted) "GRANTED" else "DENIED",
                        fontSize = 16.sp,
                        color =
                            if (isGranted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { requestProjectedPermissions() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Request Permissions on Phone")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Status: $statusMessage",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
