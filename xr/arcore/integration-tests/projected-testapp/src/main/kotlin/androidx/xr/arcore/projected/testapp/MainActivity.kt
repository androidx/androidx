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

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.xr.runtime.XrLog

@OptIn(ExperimentalProjectedApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        XrLog.isEnabled = true
        XrLog.level = XrLog.Level.VERBOSE

        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                    HorizontalDivider(color = Color.Gray)
                    TestActivityRow(
                        "TiltGesture test",
                        TiltGestureTrackingActivity::class.java,
                        this@MainActivity,
                    )
                    TestActivityRow(
                        "Geospatial/Tracking test",
                        ProjectedTestAppActivity::class.java,
                        this@MainActivity,
                    )
                }
            }
        }
    }

    @Composable
    private fun TestActivityRow(name: String, activityClass: Class<*>, context: Context) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontSize = 18.sp)
            Button(onClick = { launchProjectedActivity(activityClass, context) }) {
                Text("Run Test", fontSize = 18.sp)
            }
        }
        HorizontalDivider(color = Color.Gray)
    }

    private fun launchProjectedActivity(activityClass: Class<*>, context: Context) {
        val projectedContext =
            try {
                ProjectedContext.createProjectedDeviceContext(context)
            } catch (e: IllegalStateException) {
                XrLog.warn(e) { "Error creating projected device" }
                return
            }
        val intent = Intent(context, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(
            intent,
            ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
        )
    }
}
