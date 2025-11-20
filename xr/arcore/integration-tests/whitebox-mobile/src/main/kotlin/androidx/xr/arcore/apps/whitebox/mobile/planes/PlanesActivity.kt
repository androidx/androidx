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

package androidx.xr.arcore.apps.whitebox.mobile.planes

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Plane
import androidx.xr.arcore.apps.whitebox.mobile.common.ArCoreVerificationHelper
import androidx.xr.arcore.apps.whitebox.mobile.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.mobile.common.SessionLifecycleHelper
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Activity to test the Planes APIs. */
class PlanesActivity : ComponentActivity() {
    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var updateJob: CompletableJob

    private val foundPlanes = mutableStateListOf<Plane>()
    private val arCoreVerificationHelper: ArCoreVerificationHelper =
        ArCoreVerificationHelper(this, onArCoreVerified = { sessionHelper.tryCreateSession() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(arCoreVerificationHelper)
        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL),
                onSessionAvailable = { session ->
                    this.session = session
                    setContent { MainPanel() }
                },
                onSessionCreateActionRequired = { result ->
                    arCoreVerificationHelper.handleSessionCreateActionRequired(result)
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onResume() {
        super.onResume()
        if (::session.isInitialized) {
            updateJob =
                SupervisorJob(
                    lifecycleScope.launch { Plane.subscribe(session).collect { updatePlanes(it) } }
                )
        }
    }

    override fun onPause() {
        super.onPause()
        if (::updateJob.isInitialized) {
            updateJob.complete()
        }
    }

    private fun updatePlanes(planes: Collection<Plane>) {
        foundPlanes.clear()
        foundPlanes.addAll(planes)
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    private fun MainPanel() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        textAlign = TextAlign.Center,
                        text = "Planes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.background(color = Color.White)
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
            ) {
                for (plane in foundPlanes) {
                    val planeState = plane.state.value
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                "${plane.hashCode()} - ${planeState.trackingState} - ${plane.type} - ${planeState.label}",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ACTIVITY_NAME = "PlanesActivity"
    }
}
