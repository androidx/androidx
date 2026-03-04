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

package androidx.xr.scenecore.testapp.headlockedui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ScenePose
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.DebugTextPanel
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.MutableStateFlow

@SuppressLint("SetTextI18n", "RestrictedApi")
class HeadLockedUiActivity : AppCompatActivity() {
    private val TAG = "HeadLockedUiActivity"
    private var session: Session? = null
    private lateinit var device: ArDevice
    private var cameraLeft: RenderViewpoint? = null
    private var cameraRight: RenderViewpoint? = null
    private var mUserForward = MutableStateFlow(Pose(Vector3(0f, 0.00f, -1.0f)))
    private lateinit var mHeadLockedPanel: PanelEntity
    private lateinit var mHeadLockedPanelView: View
    private lateinit var mDebugPanel: DebugTextPanel
    private var mProjectionSource: ProjectionSource = ProjectionSource.Head
    private var mIsDebugPanelEnabled: Boolean = true

    private var sliderPositionZ: Float = -1.0f
    private var sliderPositionY: Float = 0.0f
    private var sliderPositionX: Float = 0.0f

    private val animationRunnable: Runnable = Runnable { updateHeadLockedPose() }

    enum class ProjectionSource {
        Head,
        CameraLeft,
        CameraRight,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_head_locked_ui)

        // Create session
        session = SessionManager(this).createSession()
        if (session == null) this.finish()
        session!!.configure(
            Config(
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN,
            )
        )
        session?.scene?.keyEntity = session?.scene?.mainPanelEntity
        device = ArDevice.getInstance(session!!)
        cameraLeft = RenderViewpoint.left(session!!)
        cameraRight = RenderViewpoint.right(session!!)

        // Toolbar action
        findViewById<Toolbar>(R.id.top_app_bar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener {
                val owningActivity = this.getActivity()
                owningActivity?.let { activity ->
                    ActivityCompat.recreate(activity)
                    Log.i(TAG, "Activity ${activity.componentName} will be recreated")
                } ?: Log.e(TAG, "Could not retrieve activity to recreate for button")
            }
        }

        // Hide debug panel
        findViewById<MaterialButton>(R.id.toggle_debug_panel).setOnClickListener {
            mDebugPanel.panelEntity.let { it.setEnabled(!it.isEnabled()) }
        }

        // X Slider Setup
        val xSliderView = findViewById<Slider>(R.id.x_slider)
        xSliderView.valueFrom = -1f
        xSliderView.valueTo = 1f
        xSliderView.value = sliderPositionX
        xSliderView.addOnChangeListener { _, value, _ ->
            run {
                sliderPositionX = value
                setProjectionVector(sliderPositionX, sliderPositionY, sliderPositionZ)
            }
        }

        // Y Slider Setup
        val ySliderView = findViewById<Slider>(R.id.y_slider)
        ySliderView.valueFrom = -1f
        ySliderView.valueTo = 1f
        ySliderView.value = sliderPositionY
        ySliderView.addOnChangeListener { _, value, _ ->
            run {
                sliderPositionY = value
                setProjectionVector(sliderPositionX, sliderPositionY, sliderPositionZ)
            }
        }

        // Z Slider Setup
        val zSliderView = findViewById<Slider>(R.id.z_slider)
        zSliderView.valueFrom = -5f
        zSliderView.valueTo = 5f
        zSliderView.value = sliderPositionZ
        zSliderView.addOnChangeListener { _, value, _ ->
            run {
                sliderPositionZ = value
                setProjectionVector(sliderPositionX, sliderPositionY, sliderPositionZ)
            }
        }

        // Head eye radio button setup
        findViewById<RadioButton>(R.id.head_radio_button).also {
            it.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) setProjectionSource(buttonView.text.toString())
            }
            it.isChecked = true
        }

        // Left eye radio button setup
        findViewById<RadioButton>(R.id.left_eye_radio_button).setOnCheckedChangeListener {
            buttonView,
            isChecked ->
            if (isChecked) setProjectionSource(buttonView.text.toString())
        }

        // Right eye radio button setup
        findViewById<RadioButton>(R.id.right_eye_radio_button).setOnCheckedChangeListener {
            buttonView,
            isChecked ->
            if (isChecked) setProjectionSource(buttonView.text.toString())
        }

        // Create the debug panel with info on the tracked entity
        mDebugPanel =
            DebugTextPanel(
                context = this,
                session = session!!,
                parent = session!!.scene.activitySpace,
                name = "DebugPanel",
                pose = Pose(Vector3(0f, -0.8f, -0.05f)),
            )
        mDebugPanel.panelEntity.sizeInPixels = IntSize2d(1500, 1000)

        // Create the head locked star image panel.
        createHeadLockedPanel()
    }

    override fun onResume() {
        super.onResume()
        // Register the animation runnable to update the head locked panel.
        this.mHeadLockedPanelView.postOnAnimation(animationRunnable)
    }

    override fun onStop() {
        super.onStop()
        // Unregister the animation runnable when the activity is stopped.
        this.mHeadLockedPanelView.removeCallbacks(animationRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHeadLockedPanel.parent = null
        mHeadLockedPanel.dispose()
    }

    private fun createHeadLockedPanel() {
        this.mHeadLockedPanelView = layoutInflater.inflate(R.layout.headlocked_star, null, false)
        this.mHeadLockedPanel =
            PanelEntity.create(
                session = session!!,
                view = mHeadLockedPanelView,
                pixelDimensions = IntSize2d(640, 480),
                name = "headLockedPanel",
            )
        this.mHeadLockedPanel.setPose(Pose(Vector3(0f, 0f, 0f)))
        this.mHeadLockedPanel.parent = session!!.scene.activitySpace
        this.mHeadLockedPanel.isEnabled(false)
        this.mHeadLockedPanel.setPose(Pose.Identity)
        session!!.scene.mainPanelEntity.setPose(Pose(Vector3(0.1f, 0f, 0f)))
    }

    private fun Context.getActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> null
        }
    }

    private fun getProjectionSource(): ScenePose? {
        return when (mProjectionSource) {
            ProjectionSource.Head ->
                session!!
                    .scene
                    .perceptionSpace
                    .getScenePoseFromPerceptionPose(device.state.value.devicePose)

            ProjectionSource.CameraLeft ->
                cameraLeft?.let {
                    session!!
                        .scene
                        .perceptionSpace
                        .getScenePoseFromPerceptionPose(it.state.value.pose)
                }

            ProjectionSource.CameraRight ->
                cameraRight?.let {
                    session!!
                        .scene
                        .perceptionSpace
                        .getScenePoseFromPerceptionPose(it.state.value.pose)
                }
        }
    }

    private fun updateHeadLockedPose() {
        val projectionSource = getProjectionSource()
        if (projectionSource != null) {
            // Since the panel is parented by the activitySpace, we need to inverse its scale
            // so that the panel stays at a fixed size in the view even when ActivitySpace scales.
            this.mHeadLockedPanel.setScale(
                0.5f / session!!.scene.activitySpace.getScale(Space.REAL_WORLD)
            )
            projectionSource
                .transformPoseTo(mUserForward.value, session!!.scene.activitySpace)
                .let {
                    this.mHeadLockedPanel.setPose(it)
                    if (mIsDebugPanelEnabled) updateDebugPanel(it)
                }
        }
        mHeadLockedPanelView.postOnAnimation(animationRunnable)
    }

    private fun updateDebugPanel(projectedPose: Pose) {
        mDebugPanel.view.setLine(
            "ActivitySpace ActivityPose",
            session!!.scene.activitySpace.poseInActivitySpace.toFormattedString(),
        )
        mDebugPanel.view.setLine(
            "ActivitySpace WorldScale",
            session!!.scene.activitySpace.getScale(Space.REAL_WORLD).toString(),
        )
        val worldScaleValue = this.mHeadLockedPanel.getScale(Space.REAL_WORLD).toString()
        mDebugPanel.view.setLine("Head Locked Panel WorldScale", worldScaleValue)
        mDebugPanel.view.setLine(
            "Head ActivityPose",
            session!!
                .scene
                .perceptionSpace
                .getScenePoseFromPerceptionPose(device.state.value.devicePose)
                .poseInActivitySpace
                .toFormattedString(),
        )
        mDebugPanel.view.setLine(
            "Left Eye ActivityPose",
            session!!
                .scene
                .perceptionSpace
                .getScenePoseFromPerceptionPose(cameraLeft!!.state.value.pose)
                .poseInActivitySpace
                .toFormattedString(),
        )
        mDebugPanel.view.setLine(
            "Right Eye ActivityPose",
            session!!
                .scene
                .perceptionSpace
                .getScenePoseFromPerceptionPose(cameraRight!!.state.value.pose)
                .poseInActivitySpace
                .toFormattedString(),
        )
        mDebugPanel.view.setLine(
            "Projection Source ActivityPose",
            getProjectionSource()?.poseInActivitySpace!!.toFormattedString(),
        )
        mDebugPanel.view.setLine(
            "Head locked Pose ActivitySpace",
            projectedPose.toFormattedString(),
        )
        mDebugPanel.view.setLine(
            "Head lock Projection Direction (meters)",
            mUserForward.value.translation.toString(),
        )
    }

    private fun setProjectionVector(x: Float, y: Float, z: Float) {
        mUserForward.value = Pose(Vector3(x, y, z))
    }

    private fun setProjectionSource(source: String) {
        when (source) {
            "LeftEye" -> mProjectionSource = ProjectionSource.CameraLeft
            "RightEye" -> mProjectionSource = ProjectionSource.CameraRight
            "Head" -> mProjectionSource = ProjectionSource.Head
            else -> Log.e(TAG, "Unknown projection source: $source")
        }
    }

    private fun Pose.toFormattedString(): String {
        val position =
            "Vector3 [%.2f, %.2f, %.2f]"
                .format(this.translation.x, this.translation.y, this.translation.z)
        val rotation =
            "Rotation [%.2f, %.2f, %.2f, %.2f]"
                .format(this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w)
        return "$position, $rotation"
    }
}
