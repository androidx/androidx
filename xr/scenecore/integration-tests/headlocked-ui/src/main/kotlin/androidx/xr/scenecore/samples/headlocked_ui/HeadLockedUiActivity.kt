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

package androidx.xr.scenecore.samples.headlocked_ui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.CameraView
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ScenePose
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.samples.commontestview.DebugTextPanel
import androidx.xr.scenecore.scene

class HeadLockedUiActivity : AppCompatActivity() {

    private val TAG = "HeadLockedUiActivity"

    // For now, we want to keep this path around to test the non-split engine path.
    // To test the environment in SE, use the SplitEngineTestActivity.
    private val mSession by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private var mUserForward: Pose by mutableStateOf(Pose(Vector3(0f, 0.00f, -1.0f)))
    private lateinit var mHeadLockedPanel: PanelEntity
    private lateinit var mHeadLockedPanelView: View
    private lateinit var mDebugPanel: DebugTextPanel
    private var mProjectionSource: ScenePose? = null
    private var mIsDebugPanelEnabled: Boolean = false

    @Suppress("UNUSED_VARIABLE")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mActivity = this
        mSession.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))

        // Set the main panel size and make the main panel movable.
        mSession.scene.mainPanelEntity.sizeInPixels = IntSize2d(width = 1500, height = 1100)
        val movableComponent = MovableComponent.createSystemMovable(mSession, scaleInZ = false)
        val unused = mSession.scene.mainPanelEntity.addComponent(movableComponent)

        // Create the debug panel with info on the tracked entity
        mDebugPanel =
            DebugTextPanel(
                context = this,
                session = mSession,
                parent = mSession.scene.activitySpace,
                pixelDimensions = IntSize2d(1500, 1000),
                name = "DebugPanel",
                pose = Pose(Vector3(0f, -0.6f, -0.05f)),
            )

        // Create the image panel.
        @SuppressLint("InflateParams")
        this.mHeadLockedPanelView = layoutInflater.inflate(R.layout.image, null, false)
        this.mHeadLockedPanelView.postOnAnimation(this::updateHeadLockedPose)
        this.mHeadLockedPanel =
            PanelEntity.create(
                session = mSession,
                view = mHeadLockedPanelView,
                pixelDimensions = IntSize2d(360, 180),
                name = "headLockedPanel",
                pose = Pose(Vector3(0f, 0f, 0f)),
            )
        this.mHeadLockedPanel.parent = mSession.scene.activitySpace

        setContent { HeadLockParameterOptions(mSession, mActivity) }
    }

    private fun updateHeadLockedPose() {
        if (this.mProjectionSource != null) {
            // Since the panel is parented by the activitySpace, we need to inverse its scale
            // so that the panel stays at a fixed size in the view even when ActivitySpace scales.
            this.mHeadLockedPanel.setScale(
                0.5f / mSession.scene.activitySpace.getScale(Space.REAL_WORLD)
            )
            this.mProjectionSource
                ?.transformPoseTo(mUserForward, mSession.scene.activitySpace)
                ?.let {
                    this.mHeadLockedPanel.setPose(it)
                    if (mIsDebugPanelEnabled) updateDebugPanel(it)
                }
        }
        mHeadLockedPanelView.postOnAnimation(this::updateHeadLockedPose)
    }

    private fun updateDebugPanel(projectedPose: Pose) {
        mDebugPanel.view.setLine(
            "ActivitySpace ActivityPose",
            mSession.scene.activitySpace.activitySpacePose.toString(),
        )
        mDebugPanel.view.setLine(
            "ActivitySpace WorldScale",
            mSession.scene.activitySpace.getScale(Space.REAL_WORLD).toString(),
        )
        mDebugPanel.view.setLine(
            "Head Locked Panel WorldScale",
            this.mHeadLockedPanel.getScale(Space.REAL_WORLD).toString(),
        )
        mDebugPanel.view.setLine(
            "Head ActivityPose",
            mSession.scene.spatialUser.head?.activitySpacePose.toString(),
        )
        mDebugPanel.view.setLine(
            "Left Eye ActivityPose",
            mSession.scene.spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]!!
                .activitySpacePose
                .toString(),
        )
        mDebugPanel.view.setLine(
            "Right Eye ActivityPose",
            mSession.scene.spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]!!
                .activitySpacePose
                .toString(),
        )
        mDebugPanel.view.setLine(
            "Projection Source ActivityPose",
            this.mProjectionSource?.activitySpacePose.toString(),
        )
        mDebugPanel.view.setLine("Head locked Pose ActivitySpace", projectedPose.toString())
        mDebugPanel.view.setLine(
            "Head lock Projection Direction (meters)",
            mUserForward.translation.toString(),
        )
    }

    @Composable
    fun HeadLockParameterOptions(session: Session, activity: Activity) {
        LaunchedEffect(Unit) {
            activity.setContentView(
                createButtonViewUsingCompose(activity = activity, session = session)
            )
        }
    }

    private fun createButtonViewUsingCompose(activity: Activity, session: Session): View {
        val view =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { ModifyProjectionParameters(session) }
            }
        view.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        view.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
        return view
    }

    private fun setProjectionVector(x: Float, y: Float, z: Float) {
        mUserForward = Pose(Vector3(x, y, z))
    }

    private fun setProjectionSource(source: String) {
        when (source) {
            "LeftEye" ->
                mProjectionSource =
                    mSession.scene.spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
            "RightEye" ->
                mProjectionSource =
                    mSession.scene.spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]
            "Head" -> mProjectionSource = mSession.scene.spatialUser.head!!
            else -> Log.e(TAG, "Unknown projection source: $source")
        }
    }

    @Composable
    fun ModifyProjectionParameters(session: Session) {
        var sliderPositionZ by remember { mutableFloatStateOf(-1.0f) }
        var sliderPositionY by remember { mutableFloatStateOf(0.0f) }
        var sliderPositionX by remember { mutableFloatStateOf(0.0f) }

        val radioOptions = listOf("LeftEye", "RightEye", "Head")
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }

        // Trigger the default action (e.g., when the composable is first composed)
        LaunchedEffect(Unit) { setProjectionSource(radioOptions[0]) }

        Column(verticalArrangement = Arrangement.Top) {
            Text(text = "User Forward Z ${sliderPositionZ}%", fontSize = 30.sp)
            Slider(
                value = sliderPositionZ,
                onValueChange = {
                    sliderPositionZ = it
                    setProjectionVector(sliderPositionX, sliderPositionY, sliderPositionZ)
                },
                valueRange = -5f..5f,
                steps = 101,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
            Text(text = "User Forward Y ${sliderPositionY}%", fontSize = 30.sp)
            Slider(
                value = sliderPositionY,
                onValueChange = {
                    sliderPositionY = it
                    setProjectionVector(sliderPositionX, sliderPositionY, sliderPositionZ)
                },
                valueRange = -1f..1f,
                steps = 50,
                modifier = Modifier.fillMaxWidth(0.8f),
            )
            Text(text = "User Forward X ${sliderPositionX}%", fontSize = 30.sp)
            Slider(
                value = sliderPositionX,
                onValueChange = {
                    sliderPositionX = it
                    setProjectionVector(sliderPositionX, sliderPositionY, sliderPositionZ)
                },
                valueRange = -1f..1f,
                steps = 50,
                modifier = Modifier.fillMaxWidth(0.8f),
            )

            for (text in radioOptions) {
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = {
                        onOptionSelected(text)
                        setProjectionSource(text)
                    },
                )
                Text(text = text, modifier = Modifier.padding(start = 8.dp))
            }

            Button(onClick = { session.scene.requestFullSpaceMode() }) {
                Text(text = "Request FSM", fontSize = 30.sp)
            }
            Button(onClick = { session.scene.requestHomeSpaceMode() }) {
                Text(text = "Request HSM", fontSize = 30.sp)
            }
            Button(onClick = { mIsDebugPanelEnabled = !mIsDebugPanelEnabled }) {
                Text(text = "Toggle Debug Panel", fontSize = 30.sp)
            }
        }
    }
}
