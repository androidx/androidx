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

package androidx.xr.arcore.projected.testapp.tiltgesture

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.ExperimentalGesturesApi
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalGesturesApi::class, ExperimentalProjectedApi::class)
class TiltGestureHostActivity : ComponentActivity() {

    private val viewModel: TiltGestureViewModel by viewModels()
    private lateinit var tiltGestureController: TiltGestureController
    private var projectedActivity: TiltGestureProjectedActivity? = null

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is TiltGestureProjectedActivity) {
                    projectedActivity = activity
                    activity.viewModel = this@TiltGestureHostActivity.viewModel
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

        val projectedContext = ProjectedContext.createProjectedDeviceContext(this)
        val intent = Intent(this, TiltGestureProjectedActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(
            intent,
            ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
        )

        tiltGestureController =
            TiltGestureController(viewModel = viewModel, coroutineScope = lifecycleScope)
        lifecycleScope.launch {
            tiltGestureController.onCreate(projectedContext, this@TiltGestureHostActivity)
        }
        setContent { HostView(viewModel) }
    }

    override fun onDestroy() {
        super.onDestroy()
        tiltGestureController.onDestroy()
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        projectedActivity?.finish()
    }

    @Composable
    private fun HostView(viewModel: TiltGestureViewModel) {
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.Start,
                ) {
                    TextEntry(text = "Tilt: ${viewModel.tilt.collectAsState().value}")
                    TextEntry(text = "Progress: ${viewModel.progress.collectAsState().value}")
                    TextEntry(text = "Info: ${viewModel.message.collectAsState().value}")
                }
            }
        }
    }

    @Composable
    private fun TextEntry(text: String) {
        Text(text = text, fontSize = 32.sp, textAlign = TextAlign.Left)
    }
}
