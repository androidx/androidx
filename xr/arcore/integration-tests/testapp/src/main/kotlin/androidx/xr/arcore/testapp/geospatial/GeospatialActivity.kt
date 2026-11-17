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
import android.util.Log.w
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseInternalError
import androidx.xr.arcore.CreateGeospatialPoseFromPoseNotTracking
import androidx.xr.arcore.CreateGeospatialPoseFromPoseResult
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.GeospatialSurface
import androidx.xr.arcore.Plane
import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.VpsAvailabilityAvailable
import androidx.xr.arcore.VpsAvailabilityErrorInternal
import androidx.xr.arcore.VpsAvailabilityNetworkError
import androidx.xr.arcore.VpsAvailabilityNotAuthorized
import androidx.xr.arcore.VpsAvailabilityResourceExhausted
import androidx.xr.arcore.VpsAvailabilityResult
import androidx.xr.arcore.VpsAvailabilityUnavailable
import androidx.xr.arcore.hitTest
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.helloar.rendering.PlaneRenderer
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.XrDevice
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GeospatialActivity : ComponentActivity() {

    private enum class AnchorType {
        STANDARD,
        TERRAIN,
        ROOFTOP,
    }

    /** An anchor saved to [SharedPreferences] by a previous run of the activity. */
    private class SavedAnchor(
        val type: AnchorType,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val eastUpSouthQuaternion: Quaternion,
    )

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var planeRenderer: PlaneRenderer
    private val anchors = mutableListOf<Anchor>()
    private val anchorEntities = mutableListOf<GltfModelEntity>()
    // Jobs that create, restore, or render anchors, which are cancelled when anchors are cleared.
    private val anchorJobs = mutableListOf<Job>()
    private var anchorModel: GltfModel? = null
    private var surfaceAnchorModel: GltfModel? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mainPanelEntity: PanelEntity
    private val mainPanelOffset = Pose(Vector3(0f, 0f, -0.7f))
    private var selectedAnchorType = AnchorType.STANDARD
    private val configBuilder = Config.Builder()
    private val localizationTracker = GeospatialLocalizationTracker()
    private val localizationState = MutableStateFlow(LocalizationState.NOT_TRACKING)
    private var localizationStatusText by mutableStateOf("")
    // Anchors saved by previous runs, which are restored once the Geospatial accuracy converges.
    private val pendingSavedAnchors = mutableStateListOf<String>()

    companion object {
        private const val SAVED_ANCHORS_KEY = "geospatial_anchors"
        private val LOCALIZATION_UPDATE_INTERVAL = 500.milliseconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getPreferences(MODE_PRIVATE)
        pendingSavedAnchors.addAll(
            sharedPreferences.getStringSet(SAVED_ANCHORS_KEY, null).orEmpty()
        )

        configBuilder
            .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            .setDeviceTracking(DeviceTrackingMode.SPATIAL)

        sessionHelper =
            SessionLifecycleHelper(
                this,
                configBuilder.build(),
                onSessionAvailable = { session ->
                    this.session = session
                    if (session.config.geospatial == GeospatialMode.DISABLED) {
                        if (
                            XrDevice.getCurrentDevice(this)
                                .isGeospatialModeSupported(GeospatialMode.SPATIAL)
                        ) {
                            val newConfig =
                                configBuilder.setGeospatial(GeospatialMode.SPATIAL).build()
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

                    monitorLocalization(session)
                    lifecycleScope.launch {
                        anchorModel =
                            GltfModel.create(session, Paths.get("models", "geospatial_marker.glb"))
                        surfaceAnchorModel =
                            GltfModel.create(
                                session,
                                Paths.get("models", "geospatial_marker_yellow.glb"),
                            )

                        // Wait for the Geospatial accuracy to converge before restoring anchors, so
                        // that they are restored close to where they were created.
                        localizationState.first { it == LocalizationState.LOCALIZED }
                        restoreSavedAnchors()
                    }

                    planeRenderer = PlaneRenderer(session)
                    lifecycle.addObserver(planeRenderer)
                    attachTapHandlerToPlanes()
                    setContent { MainPanel(session) }
                },
            )
        sessionHelper.tryCreateSession()
    }

    @Composable
    private fun MainPanel(session: Session) {
        val geospatial = Geospatial.getInstance(session)
        val geospatialState by geospatial.state.collectAsStateWithLifecycle()
        val localization by localizationState.collectAsStateWithLifecycle()
        var vpsAvailability by remember { mutableStateOf<VpsAvailabilityResult?>(null) }
        val arDevice = ArDevice.getInstance(session)
        val arDeviceState by arDevice.state.collectAsStateWithLifecycle()
        var selectedAnchorTypeState by remember { mutableStateOf(selectedAnchorType) }

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
                Text(
                    "Geospatial geospatialTrackingState: ${geospatialState.geospatialTrackingState}"
                )
                Text("VPS Availability: ${vpsAvailabilityToString(vpsAvailability)}")
                Text(localizationStatusText)
                Text(localizationStateToString(localization))
                Text("Anchor type:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (type in AnchorType.values()) {
                        Button(
                            onClick = {
                                selectedAnchorType = type
                                selectedAnchorTypeState = type
                            },
                            colors =
                                if (selectedAnchorTypeState == type) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                                },
                        ) {
                            Text(type.name)
                        }
                    }
                }
                Button(onClick = { clearAnchors() }) { Text("Clear Anchors") }
                Text("Anchor count: ${anchors.size}")
                if (pendingSavedAnchors.isNotEmpty()) {
                    Text("Saved anchors waiting to be restored: ${pendingSavedAnchors.size}")
                }
            }
        }

        // Keyed on the tracking state because the full state changes with every localization
        // update, which would restart the effect and cancel the VPS availability check.
        LaunchedEffect(geospatialState.geospatialTrackingState) {
            if (
                geospatialState.geospatialTrackingState ==
                    Geospatial.GeospatialTrackingState.RUNNING
            ) {
                val poseResult = snapshotFlow {
                    arDeviceState
                }
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
                    w("JetpackXR", "checkVpsAvailability failed: $e", e)
                    vpsAvailability = null
                }
            }
        }
    }

    /** Periodically updates [localizationState] from the accuracy of the device geospatial pose. */
    private fun monitorLocalization(session: Session) {
        val geospatial = Geospatial.getInstance(session)
        val arDevice = ArDevice.getInstance(session)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    while (true) {
                        updateLocalization(geospatial, arDevice)
                        delay(LOCALIZATION_UPDATE_INTERVAL)
                    }
                } finally {
                    // Localization has to converge again after the activity resumes.
                    localizationState.value = localizationTracker.onNotTracking()
                }
            }
        }
    }

    private fun updateLocalization(geospatial: Geospatial, arDevice: ArDevice) {
        if (
            geospatial.state.value.geospatialTrackingState !=
                Geospatial.GeospatialTrackingState.RUNNING
        ) {
            localizationStatusText = "Geospatial not RUNNING"
            localizationState.value = localizationTracker.onNotTracking()
            return
        }
        val result =
            try {
                geospatial.createGeospatialPoseFromPose(arDevice.state.value.devicePose)
            } catch (e: Exception) {
                w("JetpackXR", "createGeospatialPoseFromPose failed: $e", e)
                localizationStatusText = "Error creating geospatial pose"
                localizationState.value = localizationTracker.onNotTracking()
                return
            }
        localizationStatusText = localizationTextFromResult(result)
        localizationState.value =
            if (result is CreateGeospatialPoseFromPoseSuccess) {
                localizationTracker.onAccuracySample(
                    result.horizontalAccuracy,
                    result.orientationYawAccuracy,
                )
            } else {
                localizationTracker.onNotTracking()
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
                                    val up = headScenePose.poseInActivitySpace.up
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
                                                PlaneLabel.UNKNOWN
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
        if (anchors.size >= 10) {
            Toast.makeText(this, "Anchor limit of 10 reached.", Toast.LENGTH_LONG).show()
            return
        }
        if (anchorModel == null || surfaceAnchorModel == null) {
            logAndShowToast("Anchor models are still loading, cannot create anchor.")
            return
        }
        val geospatial = Geospatial.getInstance(session)
        if (
            geospatial.state.value.geospatialTrackingState !=
                Geospatial.GeospatialTrackingState.RUNNING
        ) {
            logAndShowToast("Geospatial not running, cannot create anchor.")
            return
        }
        if (localizationState.value != LocalizationState.LOCALIZED) {
            logAndShowToast("Geospatial accuracy has not converged yet, cannot create anchor.")
            return
        }

        // Get the geospatial pose from the tap pose, then create an anchor from
        // that pose.
        val geospatialPoseResult = geospatial.createGeospatialPoseFromPose(pose)
        when (geospatialPoseResult) {
            is CreateGeospatialPoseFromPoseSuccess -> {
                // The accuracy at the tapped location also accounts for its distance from the
                // device, so check it before saving the geospatial pose of the anchor.
                if (
                    !localizationTracker.isWithinThresholds(
                        geospatialPoseResult.horizontalAccuracy,
                        geospatialPoseResult.orientationYawAccuracy,
                    )
                ) {
                    logAndShowToast(
                        "Accuracy at the tapped location is too low (horizontal: " +
                            "${"%.1f".format(geospatialPoseResult.horizontalAccuracy)} m, yaw: " +
                            "${"%.1f".format(geospatialPoseResult.orientationYawAccuracy)} " +
                            "degrees), cannot create anchor."
                    )
                    return
                }
                val geospatialPose = geospatialPoseResult.pose
                val anchorType = selectedAnchorType
                val job = lifecycleScope.launch {
                    val anchor =
                        createGeospatialAnchor(
                            anchorType,
                            geospatialPose.latitude,
                            geospatialPose.longitude,
                            geospatialPose.altitude,
                            geospatialPose.eastUpSouthQuaternion,
                        ) ?: return@launch
                    anchors.add(anchor)
                    renderAnchor(anchor, anchorType)
                    saveAnchorToSharedPreferences(geospatialPose, anchorType)
                }
                anchorJobs.add(job)
            }
            is CreateGeospatialPoseFromPoseNotTracking -> {
                logAndShowToast("Not tracking, cannot create anchor.")
            }

            is CreateGeospatialPoseFromPoseInternalError -> {
                logAndShowToast(geospatialPoseResult.error)
            }
            else -> {
                logAndShowToast("Failed to create anchor: Unknown error.")
            }
        }
    }

    /** Creates a Geospatial anchor of the given [type], or returns null if creation failed. */
    private suspend fun createGeospatialAnchor(
        type: AnchorType,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        eastUpSouthQuaternion: Quaternion,
    ): Anchor? {
        val geospatial = Geospatial.getInstance(session)
        val result =
            try {
                when (type) {
                    AnchorType.STANDARD ->
                        geospatial.createAnchor(
                            latitude,
                            longitude,
                            altitude,
                            eastUpSouthQuaternion,
                        )
                    AnchorType.TERRAIN ->
                        geospatial.createAnchorOnSurface(
                            latitude,
                            longitude,
                            0.0,
                            eastUpSouthQuaternion,
                            GeospatialSurface.TERRAIN,
                        )
                    AnchorType.ROOFTOP ->
                        geospatial.createAnchorOnSurface(
                            latitude,
                            longitude,
                            0.0,
                            eastUpSouthQuaternion,
                            GeospatialSurface.ROOFTOP,
                        )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                // The runtime throws when it fails to create the anchor, e.g. for an unsupported
                // location, which shouldn't crash the app or stop other anchors from being created.
                logAndShowToast("Failed to create ${type.name} anchor: $e", e)
                return null
            }
        if (result is AnchorCreateSuccess) {
            return result.anchor
        }
        logAndShowToast("Failed to create ${type.name} anchor: $result")
        return null
    }

    private fun saveAnchorToSharedPreferences(geospatialPose: GeospatialPose, type: AnchorType) {
        val newAnchorInfo =
            "${type.name},${geospatialPose.latitude},${geospatialPose.longitude},${geospatialPose.altitude}," +
                "${geospatialPose.eastUpSouthQuaternion.x},${geospatialPose.eastUpSouthQuaternion.y}," +
                "${geospatialPose.eastUpSouthQuaternion.z},${geospatialPose.eastUpSouthQuaternion.w}"
        val savedAnchors =
            sharedPreferences.getStringSet(SAVED_ANCHORS_KEY, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        savedAnchors.add(newAnchorInfo)
        sharedPreferences.edit().putStringSet(SAVED_ANCHORS_KEY, savedAnchors).apply()
    }

    /**
     * Restores the anchors saved by previous runs. Only call this once the Geospatial accuracy has
     * converged, so that the anchors are restored close to where they were created.
     */
    private fun restoreSavedAnchors() {
        val savedAnchors = pendingSavedAnchors.toList()
        pendingSavedAnchors.clear()
        if (savedAnchors.isEmpty()) {
            return
        }
        val job = lifecycleScope.launch {
            var restoredCount = 0
            for (anchorInfo in savedAnchors) {
                val savedAnchor = parseSavedAnchor(anchorInfo)
                if (savedAnchor == null) {
                    w("JetpackXR", "Skipping malformed saved anchor: $anchorInfo")
                    continue
                }
                val anchor =
                    createGeospatialAnchor(
                        savedAnchor.type,
                        savedAnchor.latitude,
                        savedAnchor.longitude,
                        savedAnchor.altitude,
                        savedAnchor.eastUpSouthQuaternion,
                    ) ?: continue
                anchors.add(anchor)
                renderAnchor(anchor, savedAnchor.type)
                restoredCount++
            }
            logAndShowToast("Restored $restoredCount of ${savedAnchors.size} saved anchors.")
        }
        anchorJobs.add(job)
    }

    private fun parseSavedAnchor(anchorInfo: String): SavedAnchor? {
        val parts = anchorInfo.split(',')
        if (parts.size != 8) {
            return null
        }
        val type = AnchorType.entries.firstOrNull { it.name == parts[0] } ?: return null
        val latitude = parts[1].toDoubleOrNull() ?: return null
        val longitude = parts[2].toDoubleOrNull() ?: return null
        val altitude = parts[3].toDoubleOrNull() ?: return null
        val qx = parts[4].toFloatOrNull() ?: return null
        val qy = parts[5].toFloatOrNull() ?: return null
        val qz = parts[6].toFloatOrNull() ?: return null
        val qw = parts[7].toFloatOrNull() ?: return null
        return SavedAnchor(type, latitude, longitude, altitude, Quaternion(qx, qy, qz, qw))
    }

    private fun renderAnchor(anchor: Anchor, type: AnchorType) {
        val model =
            when (type) {
                AnchorType.STANDARD -> anchorModel
                AnchorType.TERRAIN,
                AnchorType.ROOFTOP -> surfaceAnchorModel
            }
        if (model == null) {
            w("JetpackXR", "Anchor models are not loaded, cannot render ${type.name} anchor.")
            return
        }
        val entity =
            GltfModelEntity.create(session, model, parent = session.scene.activitySpace).also {
                it.setScale(0.3f)
                it.setEnabled(false)
            }
        anchorEntities.add(entity)

        // Only show the anchor while the Geospatial accuracy has converged, so that it reappears
        // close to where it was created rather than where an unconverged localization places it.
        val renderJob = lifecycleScope.launch {
            combine(anchor.state, localizationState) { state, localization ->
                    state to localization
                }
                .collect { (state, localization) ->
                    if (
                        state.trackingState == TrackingState.TRACKING &&
                            localization == LocalizationState.LOCALIZED
                    ) {
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
        anchorJobs.add(renderJob)
    }

    private fun clearAnchors() {
        pendingSavedAnchors.clear()
        // Also cancels anchors that are still being created or restored, so that they aren't
        // added, rendered, or saved after the anchors have been cleared.
        for (job in anchorJobs) {
            job.cancel()
        }
        anchorJobs.clear()
        for (anchor in anchors) {
            anchor.detach()
        }
        anchors.clear()
        for (entity in anchorEntities) {
            entity.parent = null
        }
        anchorEntities.clear()
        sharedPreferences.edit().remove(SAVED_ANCHORS_KEY).apply()
    }

    private fun logAndShowToast(message: String, throwable: Throwable? = null) {
        w("JetpackXR", message, throwable)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
            else -> "Unknown"
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
            is CreateGeospatialPoseFromPoseInternalError -> "Localization Status: ${result.error}"
            else -> "Localization Status: Unknown"
        }
    }

    private fun localizationStateToString(state: LocalizationState): String {
        return when (state) {
            LocalizationState.NOT_TRACKING -> "Localization: Not tracking"
            LocalizationState.LOCALIZING ->
                "Localization: Converging. Anchors can be created, and saved anchors are " +
                    "restored, once horizontal accuracy <= " +
                    "${"%.1f".format(localizationTracker.horizontalAccuracyThresholdMeters)} m " +
                    "and yaw accuracy <= " +
                    "${"%.1f".format(localizationTracker.yawAccuracyThresholdDegrees)} degrees. " +
                    "Face nearby buildings to help VPS localize."
            LocalizationState.LOCALIZED ->
                "Localization: Converged. Tap on a plane to create an anchor."
        }
    }
}
