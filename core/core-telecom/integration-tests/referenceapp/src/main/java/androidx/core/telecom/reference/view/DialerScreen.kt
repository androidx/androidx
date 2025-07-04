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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.reference.CallRepository
import androidx.core.telecom.reference.viewModel.DialerViewModel

/**
 * Composable function that displays the main Dialer UI.
 *
 * This screen allows the user to input a display name and phone number, select call options (video,
 * hold support), and initiate an outgoing call. It observes the UI state from the provided
 * [DialerViewModel] and delegates user actions back to the ViewModel or navigation callbacks.
 *
 * @param dialerViewModel The ViewModel instance that holds the state for the dialer and handles
 *   business logic.
 * @param onNavigateToSettings Lambda function to be invoked when the settings icon is clicked.
 * @param onStartCall Lambda function to be invoked when the call button is clicked, typically used
 *   for navigating to the ongoing call screen.
 */
@Composable
fun DialerScreen(
    dialerViewModel: DialerViewModel,
    onNavigateToSettings: () -> Unit = {},
    onStartCall: () -> Unit = {},
) {
    val uiStateState = dialerViewModel.uiState.collectAsState()
    val uiState = uiStateState.value

    // Main layout column
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top row containing the Settings icon button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onNavigateToSettings) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        // Input field for Display Name
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = { dialerViewModel.updateDisplayName(it) },
            label = { Text("Display Name") },
            textStyle = TextStyle(color = Color.Black),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )

        // Input field for Phone Number
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = { dialerViewModel.updatePhoneNumber(it) },
            label = { Text("Phone Number") },
            textStyle = TextStyle(color = Color.Black),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )

        // Row for the Video Call switch
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Video Call")
            Switch(
                checked = uiState.isVideoCall,
                onCheckedChange = { dialerViewModel.updateIsVideoCall(it) },
            )
        }

        // Row for the Supports Hold switch
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Supports Hold")
            Switch(
                checked = uiState.canHold,
                onCheckedChange = { dialerViewModel.updateCanHold(it) },
            )
        }

        // Call button
        Button(
            onClick = {
                dialerViewModel.initiateOutgoingCall()
                onStartCall()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.phoneNumber.isNotBlank(),
        ) {
            Icon(Icons.Filled.Call, contentDescription = "Call")
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text("Call")
        }

        // --- Pre-Call Endpoint List ---
        if (uiState.isFetchingEndpoints) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        } else if (uiState.availableEndpoints.isNotEmpty()) {
            Text(
                "Select Audio Output:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(bottom = 8.dp)
            ) {
                items(items = uiState.availableEndpoints, key = { it.identifier }) { endpoint ->
                    EndpointItem(
                        endpoint = endpoint,
                        isSelected = endpoint == uiState.selectedEndpoint,
                        onSelect = { dialerViewModel.selectEndpoint(endpoint) },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Text("No audio endpoints available.", modifier = Modifier.padding(bottom = 16.dp))
        }
    } // End of Main Column
}

@Preview(showBackground = true, name = "Dialer Screen with Settings")
@Composable
fun DialerScreenPreview() {
    val previewContext = LocalContext.current

    MaterialTheme {
        DialerScreen(
            dialerViewModel = DialerViewModel(previewContext, CallRepository()),
            onNavigateToSettings = { println("Preview: Settings icon clicked!") },
            onStartCall = { println("Preview: Call button clicked!") },
        )
    }
}

@Composable
fun EndpointItem(endpoint: CallEndpointCompat, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = endpoint.name.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = endpointTypeToString(endpoint.type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
