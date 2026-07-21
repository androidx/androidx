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

package androidx.xr.arcore.projected.testapp

import android.app.Activity
import android.app.Application
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
import androidx.xr.arcore.projected.testapp.tiltgesture.TiltGestureTrackingActivity
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi

@OptIn(ExperimentalProjectedApi::class)
class MainActivity : ComponentActivity() {
    private val activeProjectedActivities = mutableListOf<Activity>()

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity != this@MainActivity) {
                    activeProjectedActivities.add(activity)
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (activity != this@MainActivity) {
                    activeProjectedActivities.remove(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)

        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                    HorizontalDivider(color = Color.Gray)
                    TestActivityRow(
                        "Inertial Tracking test",
                        InertialTrackingActivity::class.java,
                        isProjected = true,
                        this@MainActivity,
                    )
                    TestActivityRow(
                        "TiltGesture test",
                        TiltGestureTrackingActivity::class.java,
                        isProjected = true,
                        this@MainActivity,
                    )
                    TestActivityRow(
                        "Geospatial/Tracking Test",
                        GeospatialProjectedActivity::class.java,
                        isProjected = true,
                        this@MainActivity,
                    )
                    TestActivityRow(
                        "Geospatial/Tracking Remote",
                        GeospatialRemoteSensorActivity::class.java,
                        isProjected = false,
                        this@MainActivity,
                    )
                    TestActivityRow(
                        "Low Power Geospatial Test",
                        LowPowerGeospatialActivity::class.java,
                        isProjected = true,
                        this@MainActivity,
                    )
                    TestActivityRow(
                        "Low Power Geospatial Remote",
                        LowPowerRemoteSensorGeospatialActivity::class.java,
                        isProjected = false,
                        this@MainActivity,
                    )
                    GeospatialActivityRow(
                        "Config Projected: INERTIAL",
                        "INERTIAL",
                        this@MainActivity,
                    )
                    GeospatialActivityRow("Config Projected: SPATIAL", "SPATIAL", this@MainActivity)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        activeProjectedActivities.forEach { activity ->
            if (ProjectedContext.isProjectedDeviceContext(activity)) {
                activity.moveTaskToBack(true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        activeProjectedActivities.forEach { it.finish() }
        activeProjectedActivities.clear()
    }

    @Composable
    private fun TestActivityRow(
        name: String,
        activityClass: Class<*>,
        isProjected: Boolean,
        context: Context,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontSize = 18.sp)
            Button(
                onClick = {
                    activeProjectedActivities.toList().forEach { it.finish() }
                    if (isProjected) {
                        launchProjectedActivity(activityClass, context)
                    } else {
                        val intent = Intent(context, activityClass)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            ) {
                Text("Run", fontSize = 18.sp)
            }
        }
        HorizontalDivider(color = Color.Gray)
    }

    private fun launchProjectedActivity(activityClass: Class<*>, context: Context) {
        val projectedContext =
            try {
                ProjectedContext.createProjectedDeviceContext(context)
            } catch (e: IllegalStateException) {
                Log.w("JetpackXR", "Error creating projected device", e)
                return
            }
        val intent = Intent(context, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(
            intent,
            ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
        )
    }

    @Composable
    private fun GeospatialActivityRow(name: String, mode: String, context: Context) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontSize = 18.sp)
            Button(
                onClick = {
                    activeProjectedActivities.toList().forEach { it.finish() }
                    val targetClass = ConfigProjectedGeospatialActivity::class.java
                    val intent = Intent(context, targetClass)
                    intent.putExtra("GEOSPATIAL_MODE", mode)
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )

                    val projectedContext =
                        try {
                            ProjectedContext.createProjectedDeviceContext(context)
                        } catch (e: IllegalStateException) {
                            return@Button
                        }

                    startActivity(
                        intent,
                        ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
                    )
                }
            ) {
                Text("Run", fontSize = 18.sp)
            }
        }
        HorizontalDivider(color = Color.Gray)
    }
}
