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

package androidx.xr.scenecore.testapp.fieldofviewvisibility

import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.CameraView
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ScenePose
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.DebugTextLinearView
import androidx.xr.scenecore.testapp.common.managers.SurfaceEntityManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow

/** Manage the UI for the Panel Entity. */
class PerceivedResolutionManager(
    private val session: Session,
    private val activity: FieldOfViewVisibilityActivity,
    private val surfaceEntityManager: SurfaceEntityManager,
    private val panelEntityManager: PanelEntityManager,
) {
    private val _mPanelEntityFlow = MutableStateFlow<PanelEntity?>(null)
    var mPanelEntity: PanelEntity?
        get() = _mPanelEntityFlow.value
        set(value) {
            _mPanelEntityFlow.value = value
        }

    private val _mTextViewFlow = MutableStateFlow<DebugTextLinearView?>(null)
    var mTextView: DebugTextLinearView?
        get() = _mTextViewFlow.value
        set(value) {
            _mTextViewFlow.value = value
        }

    private var mMovableComponent: MovableComponent? = null

    // Handler for scheduling periodic updates
    private val mHandler = Handler(Looper.getMainLooper())
    private var mStopLoop = false

    // Runnable to update the perceived resolution display
    private val mUpdatePerceivedResolutionRunnable =
        object : Runnable {
            override fun run() {

                // Update the Text
                val leftEye = session.scene.spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
                val leftEyeFov = leftEye?.fov
                val fovString =
                    if (leftEyeFov != null) {
                        "FOV L:%.2f, R:%.2f, U:%.2f, D:%.2f (rad)"
                            .format(
                                leftEyeFov.angleLeft,
                                leftEyeFov.angleRight,
                                leftEyeFov.angleUp,
                                leftEyeFov.angleDown,
                            )
                    } else {
                        "Unavailable"
                    }
                mTextView?.setLine("Left Eye Field Of View", fovString)

                mTextView?.setLine(
                    "Main Panel distance to Camera",
                    distanceToCamera(leftEye, session.scene.mainPanelEntity),
                )
                mTextView?.setLine(
                    "Main Panel Perceived Resolution",
                    session.scene.mainPanelEntity.getPerceivedResolution().toString(),
                )

                mTextView?.setLine(
                    "Panel Entity distance to Camera",
                    distanceToCamera(leftEye, panelEntityManager.panelEntity),
                )
                if (panelEntityManager.panelEntity != null) {
                    val panelWidthInActivitySpace: Float =
                        panelEntityManager.panelEntity!!.size.width *
                            panelEntityManager.panelEntity!!.getScale(Space.ACTIVITY)
                    val panelHeightInActivitySpace: Float =
                        panelEntityManager.panelEntity!!.size.height *
                            panelEntityManager.panelEntity!!.getScale(Space.ACTIVITY)
                    mTextView?.setLine(
                        "Panel Entity dimensions",
                        "Width: $panelWidthInActivitySpace x Height: $panelHeightInActivitySpace",
                    )
                    mTextView?.setLine(
                        "Panel Entity Perceived Resolution",
                        panelEntityManager.panelEntity!!.getPerceivedResolution().toString(),
                    )
                } else {
                    mTextView?.setLine("Panel Entity dimensions", "Can't Retrieve it")
                    mTextView?.setLine(
                        "Panel Entity Perceived Resolution",
                        "Create Panel Entity for resolution",
                    )
                }

                mTextView?.setLine(
                    "Surface Entity distance to Camera",
                    distanceToCamera(leftEye, surfaceEntityManager.surfaceEntity),
                )
                if (surfaceEntityManager.surfaceEntity != null) {
                    val dimensionsInLocalUnits: FloatSize3d =
                        surfaceEntityManager.surfaceEntity!!.dimensions
                    val activitySpaceScale: Float =
                        surfaceEntityManager.surfaceEntity!!.getScale(Space.ACTIVITY)
                    val dimensionsInActivitySpace: FloatSize3d =
                        FloatSize3d(
                            dimensionsInLocalUnits.width * activitySpaceScale,
                            dimensionsInLocalUnits.height * activitySpaceScale,
                            dimensionsInLocalUnits.depth * activitySpaceScale,
                        )
                    mTextView?.setLine("Surface Entity dimensions", "$dimensionsInActivitySpace")
                    mTextView?.setLine(
                        "Surface Entity Perceived Resolution",
                        surfaceEntityManager.surfaceEntity?.getPerceivedResolution().toString(),
                    )
                } else {
                    mTextView?.setLine("Surface Entity dimensions", "Can't Retrieve it")
                    mTextView?.setLine(
                        "Surface Entity Perceived Resolution",
                        "Create Surface Entity for resolution",
                    )
                }

                // Schedule the runnable to run again after 1 second
                if (!mStopLoop) {
                    mHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(1))
                }
            }
        }
    private val buttonCreatePerceivedResolutionPanel =
        activity.findViewById<Button>(R.id.button_create_perceived_resolution_panel)
    private val buttonDestroyPerceivedResolutionPanel =
        activity.findViewById<Button>(R.id.button_destroy_perceived_resolution_panel)

    init {
        buttonCreatePerceivedResolutionPanel.setOnClickListener { createPanelEntity() }

        buttonDestroyPerceivedResolutionPanel.setOnClickListener {
            destroyPerceivedResolutionPanel()
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        buttonCreatePerceivedResolutionPanel.isEnabled = (mPanelEntity == null)
        buttonDestroyPerceivedResolutionPanel.isEnabled = (mPanelEntity != null)
    }

    private fun createPanelEntity() {
        mTextView = DebugTextLinearView(context = session.activity)
        mTextView?.setName("Perceived Resolution")
        // Create PanelEntity and Components if they don't exist.
        if (mPanelEntity == null) {
            mPanelEntity =
                PanelEntity.create(
                    session = session,
                    view = mTextView!!,
                    pixelDimensions = IntSize2d(1000, 500),
                    name = "perceivedResolutionPanel",
                    pose = Pose(Vector3(0.5f, 0f, 0.1f)),
                )

            mMovableComponent = MovableComponent.createSystemMovable(session)
            mPanelEntity!!.addComponent(mMovableComponent!!)

            // Start the periodic update for perceived resolution
            mStopLoop = false
            mHandler.post(mUpdatePerceivedResolutionRunnable)
        }
    }

    private fun destroyPerceivedResolutionPanel() {
        mStopLoop = true
        mPanelEntity?.dispose()
        mPanelEntity = null
    }

    private fun distanceToCamera(cameraView: CameraView?, pose: ScenePose?): String {
        val distance =
            if (cameraView != null && pose != null)
                Vector3.distance(
                        cameraView.activitySpacePose.translation,
                        pose.activitySpacePose.translation,
                    )
                    .toString()
            else "Can't retrieve distance to Camera"
        return distance
    }
}
