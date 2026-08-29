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

package androidx.xr.projected.testapp.permissions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/** Activity running on projected display to display permissions status (read-only). */
@OptIn(ExperimentalProjectedApi::class)
class PermissionsProjectedActivity : ComponentActivity() {

    internal lateinit var viewModel: PermissionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PermissionsProjectedScreen(viewModel) }
    }

    @Composable
    private fun PermissionsProjectedScreen(viewModel: PermissionsViewModel) {
        val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
        val permissionsStatus by viewModel.permissionsStatus.collectAsStateWithLifecycle()
        val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Permissions Test (Projected)", color = Color.White, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Connection: ${if (isConnected) "Connected" else "Not Connected"}",
                color = if (isConnected) Color.Green else Color.Red,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Permission Statuses:", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            for ((permName, isGranted) in permissionsStatus) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = permName, color = Color.White, fontSize = 16.sp)
                    Text(
                        text = if (isGranted) "GRANTED" else "DENIED",
                        color = if (isGranted) Color.Green else Color(0xFFFF4444),
                        fontSize = 16.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Status: $statusMessage", color = Color(0xFFFFCC00), fontSize = 16.sp)
        }
    }
}
