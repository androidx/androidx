/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.testapp.persistentanchors

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorLoadInvalidUuid
import androidx.xr.arcore.ViewCamera
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.AnchorPersistenceMode
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import java.util.UUID
import kotlin.math.atan2
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PersistentAnchorsActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var movableEntity: Entity
    private val movableEntityOffset = Pose(Vector3(0f, 0.75f, -1.3f))
    private val uuids = MutableStateFlow<List<UUID>>(emptyList())
    private var anchorOffset = MutableStateFlow<Float>(0f)
    private lateinit var viewCameras: List<ViewCamera>
    private val panelInViewStatus = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(
                    anchorPersistence = AnchorPersistenceMode.LOCAL,
                    headTracking = HeadTrackingMode.LAST_KNOWN,
                ),
                onSessionAvailable = { session ->
                    this.session = session
                    this.viewCameras = ViewCamera.getAll(session)

                    createTargetPanel()
                    setContent { MainPanel() }
                    lifecycleScope.launch {
                        // First load will fail, so we launch a second load after a delay which
                        // should succeed.
                        uuids.emit(Anchor.getPersistedAnchorUuids(session))
                        delay(2.seconds)
                        uuids.emit(Anchor.getPersistedAnchorUuids(session))
                    }

                    startPanelInViewStatusUpdates()

                    lifecycleScope.launch { session.state.collect { updatePlaneEntity() } }
                },
            )
        sessionHelper.tryCreateSession()
    }

    private fun startPanelInViewStatusUpdates() {
        val cameraStateFlows = viewCameras.map { it.state }

        lifecycleScope.launch {
            combine(cameraStateFlows) { cameraStates ->
                    val mainPanelEntity = session.scene.mainPanelEntity
                    val panelPoseInActivitySpace = mainPanelEntity.getPose()
                    val panelPoseInPerceptionSpace =
                        session.scene.activitySpace.transformPoseTo(
                            panelPoseInActivitySpace,
                            session.scene.perceptionSpace,
                        )
                    val panelSizeInMeters = mainPanelEntity.size
                    val newStatus =
                        cameraStates.mapIndexed { index, cameraState ->
                            val isInView =
                                isPanelInView(
                                    cameraPoseInPerceptionSpace = cameraState.pose,
                                    cameraFov = cameraState.fieldOfView,
                                    panelPoseInPerceptionSpace = panelPoseInPerceptionSpace,
                                    panelSizeInMeters = panelSizeInMeters,
                                )
                            val cameraName =
                                when {
                                    viewCameras.size == 1 -> "ViewCamera"
                                    index == 0 -> "Left Eye ViewCamera"
                                    index == 1 -> "Right Eye ViewCamera"
                                    else -> "ViewCamera ${index + 1}"
                                }
                            cameraName to isInView
                        }
                    panelInViewStatus.value = newStatus
                }
                .collect {}
        }
    }

    private fun createTargetPanel() {
        val composeView = ComposeView(this)
        composeView.setContent { TargetPanel() }
        movableEntity =
            PanelEntity.create(
                session,
                composeView,
                IntSize2d(640, 640),
                "movableEntity",
                movableEntityOffset,
            )
        movableEntity.parent = session.scene.activitySpace
        configureComposeView(composeView, this)
    }

    private fun updatePlaneEntity() {
        session.scene.spatialUser.head?.let {
            movableEntity.setPose(
                it.transformPoseTo(movableEntityOffset, session.scene.activitySpace)
            )
        }
    }

    private fun configureComposeView(composeView: ComposeView, activity: Activity) {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        // TODO: b/413478924 - Use controlPanelEntity.view when the api is available.
        val parentView: View =
            if (composeView.parent != null && composeView.parent is View) composeView.parent as View
            else composeView

        parentView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        parentView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        parentView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
    }

    /**
     * Checks if a rectangular panel is fully visible within the camera's field of view. Assumes the
     * camera looks down -Z axis.
     *
     * @param cameraPoseInPerceptionSpace The position and orientation of the camera in perception
     *   space.
     * @param cameraFov The camera's field of view, defined by four angles.
     * @param panelPoseInPerceptionSpace The position and orientation of the panel in perception
     *   space.
     * @param panelSizeInMeters The width and height of the panel.
     * @return Returns true if all four corners of the panel are within the camera's field of view,
     *   and false otherwise.
     */
    private fun isPanelInView(
        cameraPoseInPerceptionSpace: Pose,
        cameraFov: FieldOfView,
        panelPoseInPerceptionSpace: Pose,
        panelSizeInMeters: FloatSize2d,
    ): Boolean {
        val halfWidth = panelSizeInMeters.width / 2f
        val halfHeight = panelSizeInMeters.height / 2f

        val localCorners =
            listOf(
                Vector3(-halfWidth, halfHeight, 0f), // Top-left
                Vector3(halfWidth, halfHeight, 0f), // Top-right
                Vector3(-halfWidth, -halfHeight, 0f), // Bottom-left
                Vector3(halfWidth, -halfHeight, 0f), // Bottom-right
            )

        // Loop through each corner to see if it's visible.
        for (corner in localCorners) {
            val cornerPositionInPerceptionSpace = panelPoseInPerceptionSpace.transformPoint(corner)
            val vecCameraToCornerPerception =
                cornerPositionInPerceptionSpace - cameraPoseInPerceptionSpace.translation

            val vecCameraToCornerCameraLocal =
                cameraPoseInPerceptionSpace.inverse.transformVector(vecCameraToCornerPerception)

            val x = vecCameraToCornerCameraLocal.x
            val y = vecCameraToCornerCameraLocal.y
            val z = vecCameraToCornerCameraLocal.z

            // Check if the corner is behind the camera.
            // In a -Z forward system, points in front have a negative z.
            // If z is positive, the point is behind the camera and not visible.
            if (z > -0.001f) {
                return false
            }

            // Calculate the horizontal and vertical angles and check if these angles are within the
            // camera's Field of View.
            val horizontalAngle = atan2(x, -z)
            val verticalAngle = atan2(y, -z)

            val inHorizontalFov =
                horizontalAngle >= cameraFov.angleLeft && horizontalAngle <= cameraFov.angleRight

            val inVerticalFov =
                verticalAngle >= cameraFov.angleDown && verticalAngle <= cameraFov.angleUp

            if (!(inHorizontalFov && inVerticalFov)) {
                return false
            }
        }

        // If all four corners passed the checks, the entire panel is considered to be in view.
        return true
    }

    @Composable
    private fun MainPanel() {
        val uuidsState = uuids.collectAsStateWithLifecycle()
        var title = intent.getStringExtra("TITLE")
        if (title == null) title = "Persistent Anchors"
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
                        text = title,
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
                for (uuid in uuidsState.value) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "UUID: $uuid", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Button(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            onClick = { loadAnchor(uuid) },
                        ) {
                            Text("Load anchor")
                        }
                        Button(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            onClick = { unpersistAnchor(uuid) },
                        ) {
                            Text("Unpersist anchor")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TargetPanel() {
        val currentPanelInViewStatus by panelInViewStatus.collectAsStateWithLifecycle()

        Column(
            modifier =
                Modifier.background(color = Color.White)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { addAnchor() }) { Text(text = "Add anchor", fontSize = 38.sp) }

            if (currentPanelInViewStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Main Panel Visibility:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
                currentPanelInViewStatus.forEach { (cameraName, isInView) ->
                    Text(
                        text = "$cameraName: ${if (isInView) "IN VIEW" else "NOT IN VIEW"}",
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    private fun addAnchor() {
        // We need to use a small offset to avoid overlapping with
        // the target panel and future anchors.
        anchorOffset.value += 0.25f
        val anchorPose =
            session.scene.activitySpace.transformPoseTo(
                movableEntity.getPose().translate(Vector3(anchorOffset.value, 0f, 0f)),
                session.scene.perceptionSpace,
            )
        val anchorResult = Anchor.create(session, anchorPose)
        when (anchorResult) {
            is AnchorCreateSuccess -> createAnchorPanel(anchorResult.anchor)
            is AnchorCreateResourcesExhausted -> {
                Log.e(ACTIVITY_NAME, "Failed to create anchor: anchor resources exhausted.")
                Toast.makeText(this, "Anchor limit has been reached.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Log.e(ACTIVITY_NAME, "Failed to create anchor: ${anchorResult::class.simpleName}")
                Toast.makeText(this, "Anchor failed to create.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createAnchorPanel(anchor: Anchor) {
        val composeView = ComposeView(this)
        val anchorEntity = AnchorEntity.create(session, anchor)
        val activity = this

        lifecycleScope.launch {
            anchor.state.collect { anchorState ->
                if (anchorState.trackingState == TrackingState.TRACKING) {
                    val panelEntity =
                        PanelEntity.create(
                            session,
                            composeView,
                            IntSize2d(640, 640),
                            "anchorEntity ${anchor.hashCode()}",
                            Pose(),
                        )
                    panelEntity.parent = anchorEntity
                    composeView.setContent { AnchorPanel(anchor, panelEntity) }
                    configureComposeView(composeView, activity)
                    cancel()
                }
            }
        }
    }

    @Composable
    private fun AnchorPanel(anchor: Anchor, entity: Entity) {
        val anchorState = anchor.state.collectAsStateWithLifecycle()
        Column(
            modifier =
                Modifier.background(color = Color.White)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = "Tracking State: ${anchorState.value.trackingState}",
                fontSize = 32.sp,
            )
            Button(modifier = Modifier.padding(top = 10.dp), onClick = { persistAnchor(anchor) }) {
                Text(text = "Persist anchor", fontSize = 32.sp)
            }
            Button(
                modifier = Modifier.padding(top = 10.dp),
                onClick = { deleteEntity(anchor, entity) },
            ) {
                Text(text = "Delete anchor", fontSize = 32.sp)
            }
        }
    }

    private fun persistAnchor(anchor: Anchor) {
        lifecycleScope.launch {
            try {
                anchor.persist()
                uuids.emit(Anchor.getPersistedAnchorUuids(session))
            } catch (e: RuntimeException) {
                Log.e("ARCore", "Error persisting anchor: ${e.message}")
            }
        }
    }

    private fun deleteEntity(anchor: Anchor, entity: Entity) {
        entity.dispose()
        anchor.detach()
    }

    private fun unpersistAnchor(uuid: UUID) {
        Anchor.unpersist(session, uuid)
        lifecycleScope.launch { uuids.emit(uuids.value - uuid) }
    }

    private fun loadAnchor(uuid: UUID) {
        val anchorResult =
            try {
                Anchor.load(session, uuid)
            } catch (e: IllegalStateException) {
                Log.e(ACTIVITY_NAME, "Failed to create anchor: ${e.message}")
                return
            }

        when (anchorResult) {
            is AnchorCreateSuccess -> {
                lifecycleScope.launch {
                    // We need to wait until the anchor is tracked before querying its pose.
                    delay(1.seconds)
                    createAnchorPanel(anchorResult.anchor)
                }
            }
            is AnchorCreateResourcesExhausted -> {
                Log.e(ACTIVITY_NAME, "Failed to load anchor: anchor resources exhausted.")
                Toast.makeText(this, "Anchor limit has been reached.", Toast.LENGTH_LONG).show()
            }
            is AnchorLoadInvalidUuid -> {
                Log.e(ACTIVITY_NAME, "Failed to load anchor: invalid UUID.")
                Toast.makeText(this, "Invalid UUID.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Log.e(ACTIVITY_NAME, "Failed to load anchor: ${anchorResult::class.simpleName}")
                Toast.makeText(this, "Anchor failed to load.", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val ACTIVITY_NAME = "PersistentAnchorsActivity"
    }
}
