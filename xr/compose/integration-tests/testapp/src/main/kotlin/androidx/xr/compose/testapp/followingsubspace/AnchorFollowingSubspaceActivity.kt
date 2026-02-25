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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import kotlinx.coroutines.delay

/** Represents the different states of the AnchorFollowingSubspaceActivity. */
sealed interface AppState {
    /** The initial state, showing instructions and a button to start scanning planes. */
    object Initial : AppState

    /** The state where the app is scanning for planes and showing buttons on them. */
    object Scanning : AppState

    /** The state where a Pose has been selected for showing anchored content. */
    data class PoseSelected(val pose: Pose) : AppState
}

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
    private var appState by mutableStateOf<AppState>(AppState.Initial)

    companion object {
        private const val TAG = "FollowingSubspaceApp"
        private val ANIMATION_DELTA = Vector3(.5f, 0f, 0f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            session =
                remember(this) { (Session.create(activity = this) as SessionCreateSuccess).session }
            LaunchedEffect(session) {
                session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
            }

            when (val currentState = appState) {
                is AppState.Initial -> {
                    MainPanelContent(currentState) { appState = AppState.Scanning }
                }
                is AppState.Scanning -> {
                    PlaneScanner { pose -> appState = AppState.PoseSelected(pose) }
                }
                is AppState.PoseSelected -> {
                    MainPanelContent(currentState) {}
                    FollowingSubspaceContent(session, currentState.pose)
                }
            }
        }
    }

    @Composable
    fun PlaneScanner(onPoseSelected: (Pose) -> Unit) {
        var poses by remember { mutableStateOf<List<Pose>>(emptyList()) }
        LaunchedEffect(session) {
            Plane.subscribe(session).collect { planeUpdate ->
                val newPoses = updatePlaneModels(planeUpdate)
                poses = newPoses
            }
        }
        SetOfAnchorButtons(poses, onPoseSelected)
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
        var rootAnchor by remember { mutableStateOf<AnchorEntity?>(null) }
        LaunchedEffect(Unit) {
            val anchorResult = Anchor.create(session, position)

            when (anchorResult) {
                is AnchorCreateSuccess -> {
                    rootAnchor = AnchorEntity.create(session, anchor = anchorResult.anchor)
                }

                is AnchorCreateResourcesExhausted -> {
                    Log.e(TAG, "Failed to create anchor: anchor resources exhausted.")
                }

                else -> {
                    Log.e(TAG, "Failed to create anchor: ${anchorResult::class.simpleName}")
                }
            }
        }

        val currentAnchor = rootAnchor
        if (currentAnchor != null) {
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
        var rootAnchor by remember { mutableStateOf<AnchorEntity?>(null) }
        var alternateAnchor by remember { mutableStateOf<AnchorEntity?>(null) }
        var showAlternate by remember { mutableStateOf(false) }
        var isAnimating by remember { mutableStateOf(false) }

        DisposableEffect(anchorPose) {
            val localRoot = createAnchorEntity(session, anchorPose)
            val alternatePose = Pose(anchorPose.translation + ANIMATION_DELTA, anchorPose.rotation)
            val localAlternative = createAnchorEntity(session, alternatePose)
            rootAnchor = localRoot
            alternateAnchor = localAlternative

            onDispose {
                localRoot?.getAnchor()?.detach()
                localAlternative?.getAnchor()?.detach()
            }
        }

        LaunchedEffect(rootAnchor, alternateAnchor, isAnimating) {
            if (rootAnchor != null && alternateAnchor != null) {
                while (isAnimating) {
                    showAlternate = !showAlternate
                    delay(2000L)
                }
            }
        }

        val currentAnchor = if (showAlternate) alternateAnchor else rootAnchor
        currentAnchor?.let { activeAnchor ->
            val buttonText = if (isAnimating) "Stop anchor animation" else "Start anchor animation"

            FollowingSubspace(
                target = FollowTarget.Anchor(activeAnchor),
                behavior = FollowBehavior.Soft(),
                modifier = SubspaceModifier.rotate(pitch = -90f, 0f, 0f),
            ) {
                SpatialRow {
                    CustomSpatialPanel {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Anchored", fontSize = 30.sp, color = Color(16, 156, 11))
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { isAnimating = !isAnimating }) { Text(buttonText) }
                        }
                    }
                }
            }
        }
    }

    private fun createAnchorEntity(session: Session, anchorPose: Pose): AnchorEntity? {
        when (val anchorResult = Anchor.create(session, anchorPose)) {
            is AnchorCreateSuccess -> {
                return AnchorEntity.create(session, anchor = anchorResult.anchor)
            }
            is AnchorCreateResourcesExhausted -> {
                Log.e(TAG, "Failed to create anchor: anchor resources exhausted.")
                return null
            }
            else -> {
                Log.e(TAG, "Failed to create anchor: ${anchorResult::class.simpleName}")
                return null
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
    private fun MainPanelContent(appState: AppState, onShowPlanesClick: () -> Unit) {
        Subspace {
            SpatialPanel(modifier = SubspaceModifier.width(800.dp).height(350.dp)) {
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
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            if (appState == AppState.Initial) {
                                Text(
                                    text =
                                        "Detected planes will be shown with buttons on them. You will then be able to place anchored content at that location.",
                                    fontSize = 20.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = onShowPlanesClick,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                ) {
                                    Text("Show Planes/Buttons")
                                }
                            } else {
                                Text(
                                    text =
                                        "There is now content loaded at the anchor you selected.",
                                    fontSize = 20.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
