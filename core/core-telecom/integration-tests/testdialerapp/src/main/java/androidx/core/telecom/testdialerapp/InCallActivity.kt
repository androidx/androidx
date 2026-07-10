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

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val callState by CallManager.callState.collectAsState()
                InCallScreen(callState = callState, onHangup = { CallManager.hangup() })
            }
        }
    }

    // When the call is disconnected, finish this activity
    override fun onResume() {
        super.onResume()
        if (CallManager.call.value == null) {
            finish()
        }
    }
}

@Composable
fun InCallScreen(callState: Int?, onHangup: () -> Unit) {
    val stateText =
        when (callState) {
            Call.STATE_ACTIVE -> "Active"
            Call.STATE_DIALING -> "Dialing..."
            Call.STATE_RINGING -> "Ringing..."
            Call.STATE_CONNECTING -> "Connecting..."
            Call.STATE_DISCONNECTED -> "Disconnected"
            Call.STATE_HOLDING -> "On Hold"
            else -> "..."
        }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1B1F)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Calling...", fontSize = 24.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stateText, fontSize = 32.sp, color = Color.White)
            Spacer(modifier = Modifier.height(150.dp))
            FloatingActionButton(
                onClick = onHangup,
                containerColor = Color.Red,
                contentColor = Color.White,
                modifier = Modifier.size(72.dp),
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Hang Up")
            }
        }

        // If the call is disconnected, automatically finish the activity
        if (callState == Call.STATE_DISCONNECTED) {
            val activity = (androidx.compose.ui.platform.LocalContext.current as? ComponentActivity)
            activity?.finish()
        }
    }
}
