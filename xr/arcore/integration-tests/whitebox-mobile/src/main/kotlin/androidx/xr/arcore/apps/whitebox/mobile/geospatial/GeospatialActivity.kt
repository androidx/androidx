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

package androidx.xr.arcore.apps.whitebox.mobile.geospatial

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseNotTracking
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.HitResult
import androidx.xr.arcore.Plane
import androidx.xr.arcore.apps.whitebox.mobile.common.SessionLifecycleHelper
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.hitTest
import androidx.xr.arcore.playservices.UnsupportedArCoreCompatApi
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Log
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** Activity to test the Geospatial API. */
class GeospatialActivity : ComponentActivity(), DefaultLifecycleObserver {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper

    // OpenGL Rendering.
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var geospatialRenderer: GeospatialRenderer
    private lateinit var sampleRender: SampleRender
    private lateinit var renderer: SampleRender.Companion.Renderer

    private val anchors = CopyOnWriteArrayList<Anchor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)

        sessionHelper =
            SessionLifecycleHelper(
                this,
                config =
                    Config(
                        planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                        deviceTracking = DeviceTrackingMode.LAST_KNOWN,
                        geospatial = GeospatialMode.VPS_AND_GPS,
                    ),
                onSessionAvailable = { session ->
                    this.session = session
                    surfaceView = GLSurfaceView(this)
                    renderer = GeospatialRenderer(session, anchors)
                    sampleRender = SampleRender(surfaceView, renderer, assets)
                    setContent { MainPanel(session) }
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onResume(owner: LifecycleOwner) {
        surfaceView.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        surfaceView.onPause()
    }

    @OptIn(UnsupportedArCoreCompatApi::class)
    private fun getHits() {
        if (lifecycle.currentStateFlow.value == Lifecycle.State.RESUMED) {
            val pose: Pose? = session.state.value.cameraState?.displayOrientedPose
            if (pose == null) {
                return
            }

            // Using the camera forward ray for now. Screen space ray conversion should be supported
            // at
            // the system level.
            val ray = Ray(pose.translation, pose.forward)
            for (hit in hitTest(session, ray)) {
                if (shouldCreateAnchor(hit, pose)) {
                    createAnchorAtPose(hit.hitPose)
                    return
                }
            }
        }
    }

    private fun shouldCreateAnchor(hit: HitResult, cameraPose: Pose): Boolean {
        return hit.trackable is Plane
    }

    private fun createAnchorAtPose(pose: Pose) {
        val geospatial = Geospatial.getInstance(session)
        if (geospatial.state.value != Geospatial.State.RUNNING) {
            Log.error { "Failed to create anchor: Geospatial is not running." }
            return
        }

        val geospatialPoseResult = geospatial.createGeospatialPoseFromPose(pose)
        when (geospatialPoseResult) {
            is CreateGeospatialPoseFromPoseSuccess -> {
                val geospatialPose = geospatialPoseResult.pose
                geospatial
                    .createAnchor(
                        geospatialPose.latitude,
                        geospatialPose.longitude,
                        geospatialPose.altitude,
                        geospatialPose.eastUpSouthQuaternion,
                    )
                    .let {
                        if (it is AnchorCreateSuccess) {
                            anchors.add(it.anchor)
                        }
                    }
            }
            is CreateGeospatialPoseFromPoseNotTracking -> {
                Log.error { "Failed to create anchor: Geospatial is not tracking." }
            }
        }
    }

    @Composable
    private fun MainPanel(session: Session) {
        var localizationStatusText by remember { mutableStateOf("") }

        // Create anchors when the user taps on the screen.
        Box(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { getHits() } }
        ) {
            BackgroundImage(surfaceView)

            // Show the localization status text at the top of the screen.
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    localizationStatusText,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                )
            }

            // Anchor controls and status at the bottom of the screen.
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.BottomCenter)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tap to create anchor",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${anchors.size} / $MAX_ANCHOR_COUNT anchors", color = Color.White)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { clearAllAnchors() },
                            contentPadding = PaddingValues(all = 10.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors =
                                ButtonDefaults.buttonColors(containerColor = Color(60, 80, 190)),
                        ) {
                            Text("Clear all anchors")
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                localizationStatusText =
                    localizationTextForGeospatial(Geospatial.getInstance(session))
                delay(1.seconds)
            }
        }
    }

    private fun clearAllAnchors() {
        anchors.forEach { it.detach() }
        anchors.clear()
    }

    private fun localizationTextForGeospatial(geospatial: Geospatial): String {
        return when (geospatial.state.value) {
            Geospatial.State.NOT_RUNNING -> "Enable Config.GeospatialMode to use the Geospatial API"
            Geospatial.State.ERROR_INTERNAL -> "Error: Internal"
            Geospatial.State.PAUSED -> "Paused"
            Geospatial.State.ERROR_NOT_AUTHORIZED ->
                "Error: Not authorized. Check your API key or keyless authorization configuration"
            Geospatial.State.ERROR_RESOURCE_EXHAUSTED -> "Error: ARCore API limit reached."
            Geospatial.State.RUNNING ->
                when (
                    val result =
                        geospatial.createGeospatialPoseFromPose(
                            ArDevice.getInstance(session).state.value.devicePose
                        )
                ) {
                    is CreateGeospatialPoseFromPoseSuccess ->
                        """
                        Localization Status:
                          Lat: ${"%.6f".format(result.pose.latitude)}
                          Lng: ${"%.6f".format(result.pose.longitude)}
                          Alt: ${"%.3f".format(result.pose.altitude)}
                          Horizontal Accuracy: ${"%.3f".format(result.horizontalAccuracy)}
                          Vertical Accuracy: ${"%.3f".format(result.verticalAccuracy)}
                          Yaw Accuracy: ${"%.3f".format(result.orientationYawAccuracy)}
                        """
                            .trimIndent()
                    is CreateGeospatialPoseFromPoseNotTracking ->
                        "Localization Status: Not tracking"
                }
            else -> "Localization Status: Unknown"
        }
    }

    @Composable
    fun BackgroundImage(view: GLSurfaceView) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { _ -> view })
    }

    companion object {
        const val ACTIVITY_NAME = "GeospatialActivity"
        const val MAX_ANCHOR_COUNT: Int = 20
    }
}
