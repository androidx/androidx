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

package androidx.xr.projected.testapp.projectedcontext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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

/** Activity projected onto the projected display (read-only UI). */
@OptIn(ExperimentalProjectedApi::class)
class ProjectedActivity : ComponentActivity() {

    internal lateinit var viewModel: ProjectedContextViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProjectedScreen(viewModel) }
    }

    @Composable
    private fun ProjectedScreen(viewModel: ProjectedContextViewModel) {
        val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
        val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
        val pkgName by viewModel.packageName.collectAsStateWithLifecycle()
        val relaunchCount by viewModel.relaunchCount.collectAsStateWithLifecycle()
        val restartCount by viewModel.restartCount.collectAsStateWithLifecycle()
        val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "XR Projected Context Activity", color = Color.White, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Connection: ${if (isConnected) "Connected" else "Not Connected"}",
                color = if (isConnected) Color.Green else Color.Red,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device: ${deviceName ?: "Projected Display"}",
                color = Color(0xFFFFCC00),
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Package: ${pkgName.ifEmpty { "Default" }}",
                color = Color(0xFF00FFCC),
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total Launches: ${relaunchCount + restartCount}",
                color = Color.White,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Status: $statusMessage", color = Color.Gray, fontSize = 14.sp)
        }
    }

    companion object {
        const val EXTRA_LAUNCH_COUNT = "EXTRA_LAUNCH_COUNT"
        const val EXTRA_LAUNCH_TYPE = "EXTRA_LAUNCH_TYPE"
        private const val TAG = "ProjectedActivity"
    }
}
