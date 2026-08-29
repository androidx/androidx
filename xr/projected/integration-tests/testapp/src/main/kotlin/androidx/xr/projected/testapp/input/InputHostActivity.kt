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

package androidx.xr.projected.testapp.input

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/** Activity displaying input events received from projected display and providing controls. */
@OptIn(ExperimentalProjectedApi::class)
class InputHostActivity : ComponentActivity() {

    private val viewModel: InputViewModel by viewModels()
    private lateinit var controller: InputTestController
    private var projectedActivity: ProjectedInputActivity? = null

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ProjectedInputActivity) {
                    projectedActivity = activity
                    activity.viewModel = this@InputHostActivity.viewModel
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
        controller = InputTestController(this, viewModel, lifecycleScope)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    InputScreen(viewModel, controller)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.close()
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        projectedActivity?.finish()
    }

    @Composable
    private fun InputScreen(viewModel: InputViewModel, controller: InputTestController) {
        val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
        val cameraToggleStatus by viewModel.cameraToggleStatus.collectAsStateWithLifecycle()
        val eventLogs by viewModel.eventLogs.collectAsStateWithLifecycle()
        val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Input Test",
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Camera Toggle: $cameraToggleStatus",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { controller.clearLogs() }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear Event Logs")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Recent Input Events (${eventLogs.size}):",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (eventLogs.isEmpty()) {
                    item {
                        Text(
                            text = "No key/input events received yet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(eventLogs.reversed()) { log ->
                        Text(
                            text = log,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: $statusMessage",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
