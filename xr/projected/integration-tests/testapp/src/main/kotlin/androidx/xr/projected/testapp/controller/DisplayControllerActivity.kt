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

package androidx.xr.projected.testapp.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * The DisplayControllerActivity launches the [DisplayControllerProjectedActivity] and sends intents
 * to it to update the setting to keep the screen on.
 */
@OptIn(ExperimentalProjectedApi::class)
class DisplayControllerActivity : ComponentActivity() {
    lateinit var connectedFlow: Flow<Boolean>
    var statusMessage: String = "Initializing"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "Creating DisplayControllerActivity")
        super.onCreate(savedInstanceState)
        connectedFlow = ProjectedContext.isProjectedDeviceConnected(this, Dispatchers.Default)
        setContent { CreateUi() }
    }

    @Composable
    private fun CreateUi() {
        val screenOnState = remember { mutableStateOf(false) }
        val projectedActivityReady = remember { mutableStateOf(false) }
        UpdateConnectedState(projectedActivityReady)
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            if (!projectedActivityReady.value) {
                Text(statusMessage)
                return
            }
            Text("Keep Screen On: ${screenOnState.value}")
            Button(
                onClick = {
                    screenOnState.value = !screenOnState.value
                    toggleScreenOnState(screenOnState.value)
                }
            ) {
                Text(getScreenOnButtonText(screenOnState.value))
            }
            Button(onClick = { playSound() }) { Text("Play Display Status Sound") }
        }
    }

    private fun launchProjectedActivity(projectedActivityReady: MutableState<Boolean>) {
        startActivity(
            Intent(this, DisplayControllerProjectedActivity::class.java),
            ProjectedContext.createProjectedActivityOptions(this).toBundle(),
        )
        projectedActivityReady.value = true
    }

    @Composable
    private fun UpdateConnectedState(projectedActivityReady: MutableState<Boolean>) {
        LaunchedEffect(Unit) {
            connectedFlow.collect { connected ->
                if (connected) {
                    launchProjectedActivity(projectedActivityReady)
                } else {
                    statusMessage = "Projected device is not connected."
                    Log.w(TAG, "Projected device is not connected")
                }
            }
        }
    }

    private fun toggleScreenOnState(screenOnState: Boolean) {
        val intent = Intent(this, DisplayControllerProjectedActivity::class.java)
        intent.putExtra("KEEP_SCREEN_ON", screenOnState)
        startActivity(intent, ProjectedContext.createProjectedActivityOptions(this).toBundle())
    }

    private fun playSound() {
        val intent = Intent(this, DisplayControllerProjectedActivity::class.java)
        intent.putExtra("PLAY_SOUND", true)
        startActivity(intent, ProjectedContext.createProjectedActivityOptions(this).toBundle())
    }

    private fun getScreenOnButtonText(screenOnState: Boolean): String {
        return if (screenOnState) {
            "Disable \"Keep Screen On\""
        } else {
            "Enable \"Keep Screen On\""
        }
    }

    private companion object {
        const val TAG = "DisplayControllerActivity"
    }
}
