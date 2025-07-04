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

package androidx.core.telecom.reference.view

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.telecom.reference.CallRepository
import androidx.core.telecom.reference.Constants.ACTION_ANSWER_CALL
import androidx.core.telecom.reference.Constants.EXTRA_CALL_ID
import androidx.core.telecom.reference.Constants.EXTRA_REMOTE_USER_NAME
import androidx.core.telecom.reference.Constants.EXTRA_SIMULATED_NUMBER
import androidx.core.telecom.reference.VoipApplication
import androidx.core.telecom.reference.service.TelecomVoipService
import androidx.core.telecom.reference.viewModel.DialerActivityAction
import androidx.core.telecom.reference.viewModel.DialerActivityViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * The main activity for the Dialer application.
 *
 * This activity serves as the entry point and hosts the Jetpack Compose UI.
 */
@RequiresApi(Build.VERSION_CODES.S)
class DialerActivity : ComponentActivity() {
    private val activityViewModel: DialerActivityViewModel by viewModels()
    private val callRepository: CallRepository by lazy {
        (application as VoipApplication).callRepository
    }

    // List of permissions required by the app
    private val requiredPermissions =
        mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH_CONNECT)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .toTypedArray()

    private val requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            // Check if all essential permissions were granted
            val allGranted = requiredPermissions.all { permissions.getOrDefault(it, false) }
            if (allGranted) {
                Log.i(TAG, "All required permissions granted.")
                // Permissions granted, proceed with app initialization that depends on them
                initializeAppLogic()
            } else {
                Log.w(TAG, "Not all required permissions were granted.")
                // Handle the case where permissions are denied.
                // You might want to show a dialog explaining why they are needed
                // and potentially guide the user to settings.
                // Check if rationale should be shown for any denied permission
                val shouldShowRationale =
                    requiredPermissions.any {
                        !permissions.getOrDefault(it, false) &&
                            shouldShowRequestPermissionRationale(it)
                    }
                if (!shouldShowRationale) {
                    // User selected "Don't ask again" or policy prevents asking.
                    // Guide to settings.
                    Log.w(TAG, "Permission(s) permanently denied or policy restricted.")
                    activityViewModel.onPermissionsPermanentlyDenied()
                } else {
                    Log.w(TAG, "Some permissions denied." + " App functionality may be limited.")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("DialerActivity", "onCreate")
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            activityViewModel.activityAction.collect { action ->
                Log.d(TAG, "Collected Activity Action: $action")
                when (action) {
                    is DialerActivityAction.StartServiceToAnswer -> {
                        startServiceToAnswerCall(action.callId, action.number, action.name)
                    }
                    is DialerActivityAction.ConnectServiceIfNeeded -> {
                        callRepository.maybeConnectService(applicationContext)
                    }
                // Handle other actions if added
                }
            }
        }

        setContent {
            val showSettingsDialog by activityViewModel.showSettingsGuidanceDialog.collectAsState()

            AppTheme { // Apply the defined Material theme
                DialerApp(context = applicationContext)

                if (showSettingsDialog) {
                    PermissionDeniedDialog {
                        activityViewModel.onSettingsGuidanceDialogDismissed()
                        // Intent to open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                }
            }
        }
        checkAndRequestPermissions()
        // Handle new incoming call intents etc.
        intent?.let { activityViewModel.processIntent(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        activityViewModel.processIntent(intent)
        Log.d("DialerActivity", "onNewIntent:")
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions =
            requiredPermissions
                .filter {
                    (ContextCompat.checkSelfPermission(this, it) !=
                        PackageManager.PERMISSION_GRANTED)
                }
                .toTypedArray() // Get permissions that are NOT granted

        if (missingPermissions.isNotEmpty()) {
            Log.i(TAG, "Requesting missing permissions:" + " ${missingPermissions.joinToString()}")
            requestMultiplePermissionsLauncher.launch(missingPermissions)
        } else {
            Log.i(TAG, "All required permissions already granted.")
            // All permissions are already granted, proceed with initialization
            initializeAppLogic()
        }
    }

    private fun initializeAppLogic() {
        Log.d(TAG, "Permissions granted, initializing app logic...")
        callRepository.maybeConnectService(applicationContext)
    }

    private fun startServiceToAnswerCall(callId: String, number: String, name: String) {
        val serviceIntent =
            Intent(this, TelecomVoipService::class.java).apply {
                this.action = ACTION_ANSWER_CALL
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_SIMULATED_NUMBER, number)
                putExtra(EXTRA_REMOTE_USER_NAME, name)
            }
        Log.i(TAG, "[$callId] Calling startForegroundService to answer")
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
