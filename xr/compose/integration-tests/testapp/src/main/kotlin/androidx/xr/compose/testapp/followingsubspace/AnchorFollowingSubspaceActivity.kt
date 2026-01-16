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
package androidx.xr.compose.testapp.followingsubspace

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.Plane
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.spatial.FollowingSubspace
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.FollowBehavior
import androidx.xr.compose.subspace.FollowTarget
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Space
import kotlinx.coroutines.delay

/**
 * Main activity for the FollowingSubspace.
 *
 * This activity demonstrates the capability of FollowingSubspace for anchors.
 */
@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalFollowingSubspaceApi::class)
class AnchorFollowingSubspaceActivity : ComponentActivity() {
    lateinit var session: Session

    var planePoses by mutableStateOf(mutableListOf<Pose>())

    companion object {
        private const val TAG = "FollowingSubspaceApp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            session =
                remember(this) { (Session.create(activity = this) as SessionCreateSuccess).session }
            LaunchedEffect(session) {
                session.configure(
                    Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                )
            }

            MainPanelContent()
        }
    }

    @Composable
    fun GetPlanes() {
        var poses by remember { mutableStateOf<List<Pose>>(emptyList()) }
        var selectedPose by remember {
            mutableStateOf<Pose?>(null)
        } // To track which pose is selected
        val currentPose = selectedPose

        LaunchedEffect(session) {
            Plane.subscribe(session).collect { planeUpdate ->
                val newPoses = updatePlaneModels(planeUpdate)
                poses = newPoses
            }
        }

        if (currentPose == null) {
            SetOfAnchorButtons(poses) { pose ->
                selectedPose = pose // Set the selected pose on button click
            }
        } else {
            FollowingSubspaceContent(session, currentPose)
            Button(onClick = { this@AnchorFollowingSubspaceActivity.finish() }) {
                Text("Back to main app")
            }
        }
    }

    @Composable
    fun SetOfAnchorButtons(poses: List<Pose>, onPoseSelected: (Pose) -> Unit) {
        if (poses.isEmpty()) {
            Text("Scanning for planes...")
        } else {
            Column {
                poses.forEachIndexed { index, pose ->
                    SingleAnchorButtonWithPoseListener(
                        text = "Anchor to Plane Position ${index + 1}",
                        pose,
                        onClick = { onPoseSelected(pose) },
                    )
                }
            }
        }
    }

    @Composable
    fun SingleAnchorButtonWithPoseListener(text: String, position: Pose, onClick: () -> Unit) {
        var rootAnchor = remember { mutableStateOf<AnchorEntity?>(null) }
        LaunchedEffect(Unit) {
            val anchorResult = Anchor.create(session, position)

            when (anchorResult) {
                is AnchorCreateSuccess -> {
                    rootAnchor.value = AnchorEntity.create(session, anchor = anchorResult.anchor)
                }

                is AnchorCreateResourcesExhausted -> {
                    Log.e(TAG, "Failed to create anchor: anchor resources exhausted.")
                }

                else -> {
                    Log.e(TAG, "Failed to create anchor: ${anchorResult::class.simpleName}")
                }
            }
        }

        if (rootAnchor.value != null) {
            val currentAnchor = rootAnchor.value!!

            LaunchedEffect(currentAnchor) {
                val initialPose = currentAnchor.getPose(relativeTo = Space.ACTIVITY)
                var counter = 0

                // Loop indefinitely as long as the composable is active
                while (true) {
                    val translationX = if (counter % 2 == 0) 5f else -5f

                    val newPose =
                        Pose(
                            Vector3(
                                initialPose.translation.x + translationX,
                                initialPose.translation.y,
                                initialPose.translation.z,
                            ),
                            Quaternion(
                                initialPose.rotation.x,
                                initialPose.rotation.y,
                                initialPose.rotation.z,
                                initialPose.rotation.w,
                            ),
                        )

                    Log.d(TAG, "Anchor moved to: (${newPose.translation}")

                    // Wait for a few seconds before the next move
                    delay(1000L)
                    counter++
                }
            }

            FollowingSubspace(
                target = FollowTarget.Anchor(currentAnchor),
                behavior = FollowBehavior.Tight,
                modifier = SubspaceModifier.rotate(pitch = -90f, 0f, 0f),
            ) {
                SpatialPanel(modifier = SubspaceModifier.width(400.dp).height(300.dp)) {
                    Box(
                        modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(onClick = onClick) {
                            Text(
                                text,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updatePlaneModels(planes: Collection<Plane>): MutableList<Pose> {
        planePoses.clear()

        val planesToProcess = planes.take(3)
        for (plane in planesToProcess) {
            planePoses.add(plane.state.value.centerPose)
        }
        return planePoses
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class)
    @Composable
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    private fun FollowingSubspaceContent(session: Session, anchorPose: Pose) {
        var rootAnchor = remember { mutableStateOf<AnchorEntity?>(null) }

        val anchorResult = Anchor.create(session, anchorPose)
        when (anchorResult) {
            is AnchorCreateSuccess -> {
                rootAnchor.value = AnchorEntity.create(session, anchor = anchorResult.anchor)
            }

            is AnchorCreateResourcesExhausted -> {
                Log.e(TAG, "Failed to create anchor: anchor resources exhausted.")
            }

            else -> {
                Log.e(TAG, "Failed to create anchor: ${anchorResult::class.simpleName}")
            }
        }

        if (rootAnchor.value != null) {
            FollowingSubspace(
                target = FollowTarget.Anchor(rootAnchor.value!!),
                behavior = FollowBehavior.Tight,
                modifier = SubspaceModifier.rotate(pitch = -90f, 0f, 0f),
            ) {
                SpatialRow {
                    CustomSpatialPanel {
                        Text("Anchored (row)", fontSize = 30.sp, color = Color(16, 156, 11))
                    }
                    SpatialColumn {
                        CustomSpatialPanel {
                            Text("Anchored (column)", fontSize = 30.sp, color = Color(16, 156, 11))
                        }
                    }
                }
            }
        }

        Subspace {
            SpatialRow {
                CustomSpatialPanel {
                    Text("NOT Anchored (row)", fontSize = 30.sp, color = Color.Red)
                }
                CustomSpatialPanel {
                    Text("NOT Anchored (row)", fontSize = 30.sp, color = Color.Red)
                }
            }
        }
    }

    @UiComposable
    @Composable
    private fun CustomSpatialPanel(
        modifier: SubspaceModifier = SubspaceModifier.Companion,
        content: @Composable () -> Unit,
    ) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
        (SpatialPanel(modifier.width(300.dp).height(200.dp)) {
            Box(
                modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        })
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainPanelContent() {
        var startSubscription by remember { mutableStateOf(false) }
        CommonTestScaffold(
            title = "Anchorable Subspace Test case",
            bottomBarText = "Bottom Bar",
            onClickBackArrow = { this@AnchorFollowingSubspaceActivity.finish() },
            showBottomBar = false,
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().background(PurpleGrey80).padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Row {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Anchored Subspace will be created if a button is clicked.",
                            fontSize = 32.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        Button(onClick = { startSubscription = true }) {
                            Text("Show Planes/Buttons", textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        if (startSubscription) GetPlanes()
    }
}
