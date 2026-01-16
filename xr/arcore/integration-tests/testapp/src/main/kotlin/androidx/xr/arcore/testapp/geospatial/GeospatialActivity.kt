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

package androidx.xr.arcore.testapp.geospatial

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseNotTracking
import androidx.xr.arcore.CreateGeospatialPoseFromPoseResult
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.Plane
import androidx.xr.arcore.hitTest
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.helloar.rendering.PlaneRenderer
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.runtime.Config
import androidx.xr.runtime.Log
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.VpsAvailabilityAvailable
import androidx.xr.runtime.VpsAvailabilityErrorInternal
import androidx.xr.runtime.VpsAvailabilityNetworkError
import androidx.xr.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.runtime.VpsAvailabilityResourceExhausted
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GeospatialActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var planeRenderer: PlaneRenderer
    private val anchors = mutableListOf<Anchor>()
    private val anchorEntities = mutableListOf<GltfModelEntity>()
    private lateinit var anchorModel: GltfModel
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mainPanelEntity: PanelEntity
    private val mainPanelOffset = Pose(Vector3(0f, 0f, -0.7f))

    companion object {
        private const val SAVED_ANCHORS_KEY = "geospatial_anchors"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getPreferences(MODE_PRIVATE)

        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(
                    deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN,
                    planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                ),
                onSessionAvailable = { session ->
                    this.session = session
                    if (session.config.geospatial == Config.GeospatialMode.DISABLED) {
                        if (Config.GeospatialMode.VPS_AND_GPS.isSupported(session)) {
                            val newConfig =
                                session.config.copy(geospatial = Config.GeospatialMode.VPS_AND_GPS)
                            sessionHelper.tryUpdateConfig(newConfig)
                            return@SessionLifecycleHelper
                        } else {
                            Toast.makeText(
                                    this,
                                    "Geospatial not supported on this device.",
                                    Toast.LENGTH_LONG,
                                )
                                .show()
                            finish()
                            return@SessionLifecycleHelper
                        }
                    }

                    lifecycleScope.launch {
                        anchorModel =
                            GltfModel.create(session, Paths.get("models", "geospatial_marker.glb"))

                        // Wait for Geospatial to be running before loading anchors.
                        val geospatial = Geospatial.getInstance(session)
                        geospatial.state.first { it == Geospatial.State.RUNNING }
                        loadAnchorsFromSharedPreferences()
                    }

                    planeRenderer = PlaneRenderer(session, lifecycleScope)
                    lifecycle.addObserver(planeRenderer)
                    attachTapHandlerToPlanes()
                    setContent { MainPanel(session) }
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        for (entity in anchorEntities) {
            entity.dispose()
        }
    }

    @Composable
    private fun MainPanel(session: Session) {
        val geospatial = Geospatial.getInstance(session)
        val geospatialState by geospatial.state.collectAsStateWithLifecycle()
        var vpsAvailability by remember { mutableStateOf<VpsAvailabilityResult?>(null) }
        var localizationStatusText by remember { mutableStateOf("") }
        val arDevice = ArDevice.getInstance(session)
        val arDeviceState by arDevice.state.collectAsStateWithLifecycle()

        Scaffold(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(0.dp).background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "Geospatial",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.padding(innerPadding).background(color = Color.White).fillMaxSize()
            ) {
                Text("Geospatial State: $geospatialState")
                Text("VPS Availability: ${vpsAvailabilityToString(vpsAvailability)}")
                Text(localizationStatusText)
                if (geospatialState == Geospatial.State.RUNNING) {
                    Text("Tap on a plane to create an anchor.")
                }
                Button(onClick = { clearAnchors() }) { Text("Clear Anchors") }
                Text("Anchor count: ${anchors.size}")
            }
        }

        LaunchedEffect(geospatialState) {
            if (geospatialState == Geospatial.State.RUNNING) {
                val poseResult =
                    snapshotFlow { arDeviceState }
                        .map { geospatial.createGeospatialPoseFromPose(it.devicePose) }
                        .filterIsInstance<CreateGeospatialPoseFromPoseSuccess>()
                        .first()

                try {
                    vpsAvailability =
                        geospatial.checkVpsAvailability(
                            poseResult.pose.latitude,
                            poseResult.pose.longitude,
                        )
                } catch (e: Exception) {
                    Log.warn(e) { "checkVpsAvailability failed: $e" }
                    vpsAvailability = null
                }
            }
        }

        LaunchedEffect(geospatialState) {
            while (true) {
                if (geospatialState == Geospatial.State.RUNNING) {
                    try {
                        val result =
                            geospatial.createGeospatialPoseFromPose(arDeviceState.devicePose)
                        localizationStatusText = localizationTextFromResult(result)
                    } catch (e: Exception) {
                        Log.warn(e) { "createGeospatialPoseFromPose failed: $e" }
                        localizationStatusText = "Error creating geospatial pose"
                    }
                } else {
                    localizationStatusText = "Geospatial not RUNNING"
                }
                delay(1.seconds)
            }
        }
    }

    private fun attachTapHandlerToPlanes() {
        lifecycleScope.launch {
            planeRenderer.renderedPlanes.collect { planes ->
                for (plane in planes) {
                    if (plane.modelEntity.getComponents().isEmpty()) {
                        plane.modelEntity.addComponent(
                            InteractableComponent.create(session, mainExecutor) { event ->
                                if (event.action == InputEvent.Action.DOWN) {
                                    val arDevice = ArDevice.getInstance(session)
                                    val headScenePose =
                                        session.scene.perceptionSpace
                                            .getScenePoseFromPerceptionPose(
                                                arDevice.state.value.devicePose
                                            )
                                    val up = headScenePose.activitySpacePose.up
                                    val perceptionRayPose =
                                        session.scene.activitySpace.transformPoseTo(
                                            Pose(
                                                event.origin,
                                                Quaternion.fromLookTowards(event.direction, up),
                                            ),
                                            session.scene.perceptionSpace,
                                        )
                                    val perceptionRay =
                                        Ray(
                                            perceptionRayPose.translation,
                                            perceptionRayPose.backward,
                                        )
                                    hitTest(session, perceptionRay)
                                        .firstOrNull {
                                            (it.trackable as? Plane)?.state?.value?.label !=
                                                Plane.Label.UNKNOWN
                                        }
                                        ?.let { hitResult -> createAnchor(hitResult.hitPose) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun createAnchor(pose: Pose) {
        val geospatial = Geospatial.getInstance(session)
        if (geospatial.state.value != Geospatial.State.RUNNING) {
            Log.warn { "Geospatial not running, cannot create anchor." }
            return
        }

        // Get the geospatial pose from the tap pose, then create an anchor from
        // that pose.
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
                            renderAnchor(it.anchor)
                            saveAnchorToSharedPreferences(geospatialPose)
                        } else {
                            Log.error { "Failed to create anchor: $it" }
                        }
                    }
            }
            is CreateGeospatialPoseFromPoseNotTracking -> {
                Log.warn { "Not tracking, cannot create anchor." }
            }
        }
    }

    private fun saveAnchorToSharedPreferences(geospatialPose: GeospatialPose) {
        val newAnchorInfo =
            "${geospatialPose.latitude},${geospatialPose.longitude},${geospatialPose.altitude}," +
                "${geospatialPose.eastUpSouthQuaternion.x},${geospatialPose.eastUpSouthQuaternion.y}," +
                "${geospatialPose.eastUpSouthQuaternion.z},${geospatialPose.eastUpSouthQuaternion.w}"
        val savedAnchors =
            sharedPreferences.getStringSet(SAVED_ANCHORS_KEY, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        savedAnchors.add(newAnchorInfo)
        sharedPreferences.edit().putStringSet(SAVED_ANCHORS_KEY, savedAnchors).apply()
    }

    private fun loadAnchorsFromSharedPreferences() {
        val savedAnchors = sharedPreferences.getStringSet(SAVED_ANCHORS_KEY, null) ?: return
        lifecycleScope.launch {
            for (anchorInfo in savedAnchors) {
                val parts = anchorInfo.split(',').map { it.toDouble() }.toDoubleArray()
                if (parts.size == 7) {
                    val latitude = parts[0]
                    val longitude = parts[1]
                    val altitude = parts[2]
                    val qx = parts[3].toFloat()
                    val qy = parts[4].toFloat()
                    val qz = parts[5].toFloat()
                    val qw = parts[6].toFloat()
                    val quaternion = Quaternion(qx, qy, qz, qw)

                    val geospatial = Geospatial.getInstance(session)
                    geospatial.createAnchor(latitude, longitude, altitude, quaternion).let {
                        if (it is AnchorCreateSuccess) {
                            anchors.add(it.anchor)
                            renderAnchor(it.anchor)
                        }
                    }
                }
            }
        }
    }

    private fun renderAnchor(anchor: Anchor) {
        val entity =
            GltfModelEntity.create(session, anchorModel).also {
                it.setScale(0.3f)
                it.setEnabled(false)
            }
        anchorEntities.add(entity)

        lifecycleScope.launch {
            anchor.state.collect { state ->
                if (state.trackingState == TrackingState.TRACKING) {
                    if (!entity.isEnabled(false)) {
                        entity.setEnabled(true)
                    }
                    val activityPose =
                        session.scene.perceptionSpace.transformPoseTo(
                            state.pose,
                            session.scene.activitySpace,
                        )
                    entity.setPose(activityPose)
                } else {
                    if (entity.isEnabled(false)) {
                        entity.setEnabled(false)
                    }
                }
            }
        }
    }

    private fun clearAnchors() {
        for (anchor in anchors) {
            anchor.detach()
        }
        anchors.clear()
        for (entity in anchorEntities) {
            entity.dispose()
        }
        anchorEntities.clear()
        sharedPreferences.edit().remove(SAVED_ANCHORS_KEY).apply()
    }

    private fun vpsAvailabilityToString(vpsAvailability: VpsAvailabilityResult?): String {
        return when (vpsAvailability) {
            is VpsAvailabilityAvailable -> "Available"
            is VpsAvailabilityErrorInternal -> "ErrorInternal"
            is VpsAvailabilityNetworkError -> "NetworkError"
            is VpsAvailabilityNotAuthorized -> "NotAuthorized"
            is VpsAvailabilityResourceExhausted -> "ResourceExhausted"
            is VpsAvailabilityUnavailable -> "Unavailable"
            null -> "Checking..."
        }
    }

    private fun localizationTextFromResult(result: CreateGeospatialPoseFromPoseResult): String {
        return when (result) {
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
            is CreateGeospatialPoseFromPoseNotTracking -> "Localization Status: Not tracking"
        }
    }
}
