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

package androidx.xr.arcore.apps.whitebox.mobile.hittest

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.HitResult
import androidx.xr.arcore.Plane
import androidx.xr.arcore.apps.whitebox.mobile.common.ArCoreVerificationHelper
import androidx.xr.arcore.apps.whitebox.mobile.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.mobile.common.SessionLifecycleHelper
import androidx.xr.arcore.hitTest
import androidx.xr.arcore.playservices.UnsupportedArCoreCompatApi
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Ray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Activity to test the hit test APIs. */
class HitTestActivity : ComponentActivity() {
    companion object {
        private const val TAG = "HitTestActivity"
        private const val MAX_HIT_TEST_RESULTS = 10
    }

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var updatePlanesJob: Job
    private val foundPlanes = mutableStateListOf<Plane>()
    private var foundHits = MutableStateFlow<List<HitResult>>(mutableStateListOf<HitResult>())
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

    @OptIn(UnsupportedArCoreCompatApi::class)
    override fun onResume() {
        super.onResume()
        if (::session.isInitialized) {
            val supervisorJob = SupervisorJob()
            val scope = CoroutineScope(supervisorJob + lifecycleScope.coroutineContext)
            updatePlanesJob = scope.launch { Plane.subscribe(session).collect { updatePlanes(it) } }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::updatePlanesJob.isInitialized) {
            updatePlanesJob.cancel()
        }
    }

    private fun updatePlanes(planes: Collection<Plane>) {
        foundPlanes.clear()
        foundPlanes.addAll(planes)
    }

    @OptIn(UnsupportedArCoreCompatApi::class)
    private fun getHits() {
        if (lifecycle.currentStateFlow.value == Lifecycle.State.RESUMED) {
            val cameraState = session.state.value.cameraState
            if (cameraState == null || cameraState.displayOrientedPose == null) {
                return
            }
            val pose = cameraState.displayOrientedPose!!
            val ray = Ray(pose.translation, pose.forward)
            val hitResults = hitTest(session, ray)
            if (hitResults.isNotEmpty()) {
                val newHits = mutableStateListOf<HitResult>()
                newHits.addAll(hitResults)
                while (newHits.size > MAX_HIT_TEST_RESULTS) {
                    newHits.removeAt(0)
                }
                foundHits.value = newHits
            }
        }
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
                        text = "Hit Test",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = { getHits() }) { Text(text = "Hit Test", fontSize = 30.sp) }
                }
                for (plane in foundPlanes) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                "Tracking Plane: ${plane.hashCode()} - ${plane.state.value.trackingState} - ${plane.type} - ${plane.state.value.label}",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                for (hitResult in foundHits.value) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                "Hit Result: ${hitResult.trackable.hashCode()} - ${hitResult.hitPose.translation}",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
