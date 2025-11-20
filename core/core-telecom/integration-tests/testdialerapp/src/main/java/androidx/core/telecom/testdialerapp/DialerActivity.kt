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
package androidx.core.telecom.testdialerapp

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class DialerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "DialerActivity"
    }

    private val viewModel: CallLogViewModel by viewModels()
    private val REQUIRED_PERMISSIONS =
        arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE)

    private var uiState by mutableStateOf<UiState>(UiState.RequiresPermissions)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            Log.d(TAG, "Permission launcher result received.")
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.i(TAG, "All permissions granted by user.")
            } else {
                Log.w(TAG, "Some or all permissions were denied.")
            }
            checkPermissionsAndRoles(launchRequests = false)
        }

    private val setDefaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Set default dialer launcher result received.")
            if (result.resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "User successfully set the app as default dialer.")
                Toast.makeText(this, "Default dialer set!", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "User cancelled or failed to set the app as default dialer.")
                Toast.makeText(this, "App must be default dialer to function.", Toast.LENGTH_LONG)
                    .show()
            }
            checkPermissionsAndRoles(launchRequests = false)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity starting.")

        // Run the check once on startup to see if we are already good to go.
        checkPermissionsAndRoles(launchRequests = false)

        setContent {
            AppTheme {
                val callLogs by viewModel.callLogs.collectAsState()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (uiState) {
                        UiState.RequiresPermissions -> {
                            PermissionRationaleScreen {
                                Log.d(
                                    TAG,
                                    "Rationale button clicked. Re-checking with launch intent.",
                                )
                                checkPermissionsAndRoles(launchRequests = true)
                            }
                        }

                        UiState.Ready -> {
                            CallLogScreen(
                                logs = callLogs,
                                onCall = { uri -> placeVoipCall(uri) },
                                onReload = { loadCallLogs() },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity has come to the foreground.")
        // Only refresh if the app is fully set up and ready.
        if (uiState == UiState.Ready) {
            loadCallLogs()
        }
    }

    private fun checkPermissionsAndRoles(launchRequests: Boolean) {
        Log.d(TAG, "checkPermissionsAndRoles: Starting check... (launchRequests = $launchRequests)")
        val hasPermissions = hasPermissions(this, REQUIRED_PERMISSIONS)
        val isDefaultDialer = isDefaultDialer()

        Log.i(
            TAG,
            "checkPermissionsAndRoles: Has Permissions = $hasPermissions, Is Default Dialer = $isDefaultDialer",
        )

        if (hasPermissions && isDefaultDialer) {
            Log.i(TAG, "checkPermissionsAndRoles: All conditions met. Setting UI state to Ready.")
            uiState = UiState.Ready
            loadCallLogs()
        } else {
            Log.w(
                TAG,
                "checkPermissionsAndRoles: Conditions not met. Setting UI state to RequiresPermissions.",
            )
            uiState = UiState.RequiresPermissions

            // Only launch the system dialogs if explicitly told to
            if (launchRequests) {
                if (!hasPermissions) {
                    Log.d(
                        TAG,
                        "checkPermissionsAndRoles: Runtime permissions are missing. Launching permission request.",
                    )
                    permissionLauncher.launch(REQUIRED_PERMISSIONS)
                } else if (!isDefaultDialer) {
                    Log.d(
                        TAG,
                        "checkPermissionsAndRoles: App is not the default dialer. Launching role request.",
                    )
                    val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                    setDefaultDialerLauncher.launch(intent)
                }
            }
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun isDefaultDialer(): Boolean {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        return telecomManager.defaultDialerPackage == packageName
    }

    private fun loadCallLogs() {
        Log.d(TAG, "loadCallLogs: Loading call logs")
        viewModel.loadVoipCallLogs()
    }

    private fun placeVoipCall(callUri: Uri) {
        Log.d(TAG, "placeVoipCall: Attempting to place call with URI: $callUri")
        try {
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            telecomManager.placeCall(callUri, Bundle())
            Log.i(TAG, "placeVoipCall: placeCall intent sent successfully.")
        } catch (e: SecurityException) {
            Log.e(TAG, "placeVoipCall: SecurityException! Does the app still have permissions?", e)
            Toast.makeText(this, "Permission denied to place call.", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Log.e(TAG, "placeVoipCall: An unexpected error occurred.", t)
        }
    }
}

sealed interface UiState {
    object RequiresPermissions : UiState

    object Ready : UiState
}

// --- Composable UI ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    logs: List<CallLogEntry>,
    onCall: (Uri) -> Unit,
    onReload: () -> Unit, // NEW PARAMETER
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoIP Call Logs") },
                actions = {
                    IconButton(onClick = onReload) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Call Logs",
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        if (logs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues) // Apply padding from Scaffold
                        .padding(16.dp),
            ) {
                Text("No VoIP call logs found.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize().padding(paddingValues) // Apply padding from Scaffold
            ) {
                items(logs) { log ->
                    CallLogItem(entry = log, onCall = onCall)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PermissionRationaleScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                "This app needs to be the default phone app and requires call permissions to display your VoIP call history.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("Grant Permissions") }
    }
}

@Composable
fun CallLogItem(entry: CallLogEntry, onCall: (Uri) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.displayName, fontWeight = FontWeight.Bold)
            Text(text = entry.number)
            Text(
                text =
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(entry.date))
            )
        }
        Button(onClick = { onCall(entry.uri) }) { Text("Call Back") }
    }
}
