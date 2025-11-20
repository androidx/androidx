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

package androidx.xr.projected.testapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testapp.audio.AudioActivity
import androidx.xr.projected.testapp.camera.CameraActivity
import androidx.xr.projected.testapp.componentpermissions.PermissionsComponentActivity
import androidx.xr.projected.testapp.controller.DisplayControllerActivity
import androidx.xr.projected.testapp.input.ProjectedInputActivity
import androidx.xr.projected.testapp.permissions.PermissionsActivity
import androidx.xr.projected.testapp.projectedcontext.ProjectedContextActivity

/** The MainActivity is used to launch the various projected test activities. */
@OptIn(ExperimentalProjectedApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ActivityList(this) }
    }

    @Composable
    fun ActivityList(context: Context) {
        Column(
            modifier = Modifier.padding(top = 100.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Top,
        ) {
            HorizontalDivider(modifier = Modifier, thickness = 1.dp, color = Color.Gray)
            // Add all activities that launch to the phone.
            for (activity in ACTIVITY_MAP) {
                CreateTestActivityRow(
                    activity.key,
                    activity.value,
                    context,
                    launchProjected = false,
                )
            }
            // Add all activities that are launched on a projected device.
            for (activity in PROJECTED_ACTIVITY_MAP) {
                CreateTestActivityRow(activity.key, activity.value, context, launchProjected = true)
            }
        }
    }

    @Composable
    private fun CreateTestActivityRow(
        name: String,
        activityClass: Class<*>,
        context: Context,
        launchProjected: Boolean,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(all = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontSize = 18.sp)
            Button(onClick = { launchActivity(activityClass, context, launchProjected) }) {
                Text("Run Test", fontSize = 18.sp)
            }
        }
        HorizontalDivider(modifier = Modifier, thickness = 1.dp, color = Color.Gray)
    }

    private fun launchActivity(
        activityClass: Class<*>,
        context: Context,
        launchProjected: Boolean,
    ) {
        // If it is not a projected activity, launch the app using a host device context.
        if (!launchProjected) {
            startActivity(Intent(context, activityClass))
            return
        }
        var projectedContext: Context? = null
        try {
            projectedContext = ProjectedContext.createProjectedDeviceContext(this)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Error creating projected device: $e")
        }
        if (projectedContext == null) {
            return
        }
        startActivity(
            Intent(this, activityClass),
            ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
        )
    }

    companion object {
        const val TAG = "MainActivity"

        // A list of activities that are launchable from the main activity.
        val ACTIVITY_MAP: Map<String, Class<*>> =
            mapOf(
                "Projected Context" to ProjectedContextActivity::class.java,
                "Audio" to AudioActivity::class.java,
                "Display Controller" to DisplayControllerActivity::class.java,
                "Camera" to CameraActivity::class.java,
            )

        // A list of projected activities that are launchable from the main activity.
        val PROJECTED_ACTIVITY_MAP: Map<String, Class<*>> =
            mapOf(
                "Permission" to PermissionsActivity::class.java,
                "Component Permission" to PermissionsComponentActivity::class.java,
                "Input" to ProjectedInputActivity::class.java,
            )
    }
}
