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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * The ProjectedContextActivity will create a projected context and launch a projected activity, The
 * connected state of the projected context will be tracked.
 */
@OptIn(ExperimentalProjectedApi::class)
class ProjectedContextActivity : ComponentActivity() {

    var deviceName: String? = null
    var connectedFlow: Flow<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initProjectedDevice()
        setContent { DisplayProjectedState() }
    }

    private fun initProjectedDevice() {
        var projectedContext: Context? = null
        try {
            projectedContext = ProjectedContext.createProjectedDeviceContext(this)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Error creating projected device: $e")
        }
        if (projectedContext == null) {
            return
        }
        deviceName = ProjectedContext.getProjectedDeviceName(projectedContext)
        connectedFlow =
            ProjectedContext.isProjectedDeviceConnected(projectedContext, Dispatchers.Default)
        startActivity(
            Intent(this, ProjectedActivity::class.java),
            ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
        )
    }

    @Composable
    private fun DisplayProjectedState() {
        val currentState = remember { mutableStateOf("Not Connected") }
        val projectedStateUpdates = rememberSaveable { mutableListOf<String>() }

        LaunchedEffect(Unit) {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                connectedFlow?.collect { connected ->
                    if (connected) {
                        projectedStateUpdates += "Connected"
                        currentState.value = "Connected"
                        Log.i(TAG, "Projected Device Connected")
                    } else {
                        projectedStateUpdates += "Disconnected"
                        currentState.value = "Disconnected"
                        Log.i(TAG, "Projected Device Not Connected")
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(text = "Projected Device Name: ${deviceName?:"N/A"}")
            Text(text = "Current State: ${currentState.value}")
            for (state in projectedStateUpdates) {
                Text(text = "Projected State: $state")
            }
        }
    }

    private companion object {
        const val TAG = "ProjectedContextActivity"
    }
}
