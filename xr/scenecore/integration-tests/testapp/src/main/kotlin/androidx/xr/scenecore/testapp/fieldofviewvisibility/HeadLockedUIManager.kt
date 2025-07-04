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

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.MutableStateFlow

/** Manage the Head Locked UI. */
class HeadLockedUIManager(
    session: Session,
    activity: FieldOfViewVisibilityActivity,
    headLockedPanelView: View,
) {
    private val mSession = session
    private val mHeadLockedPanelView: View = headLockedPanelView
    private lateinit var mHeadLockedPanel: PanelEntity
    private val _mEnableHeadlockFlow = MutableStateFlow(true)
    private var mEnableHeadlock: Boolean
        get() = _mEnableHeadlockFlow.value
        set(value) {
            _mEnableHeadlockFlow.value = value
        }

    private val _mUserForwardFlow = MutableStateFlow(Pose(Vector3(0f, 0.00f, -1.3f)))
    private var mUserForward: Pose
        get() = _mUserForwardFlow.value
        set(value) {
            _mUserForwardFlow.value = value
        }

    private val _sliderPositionAlphaFlow = MutableStateFlow(1.0f)
    private var sliderPositionAlpha: Float
        get() = _sliderPositionAlphaFlow.value
        set(value) {
            _sliderPositionAlphaFlow.value = value
        }

    private val _modelIsEnabledFlow = MutableStateFlow(true)
    private var modelIsEnabled: Boolean
        get() = _modelIsEnabledFlow.value
        set(value) {
            _modelIsEnabledFlow.value = value
        }

    private val sliderLabelAlpha = activity.findViewById<TextView>(R.id.slider_label_alpha)
    private val sliderAlpha = activity.findViewById<Slider>(R.id.slider_alpha)
    private val buttonHidePanel = activity.findViewById<Button>(R.id.button_hide_panel)
    private val buttonDisableHeadlock = activity.findViewById<Button>(R.id.button_disable_headlock)

    init {
        updateUIState()
        createHeadLockedPanel()

        sliderAlpha.addOnChangeListener { _, value, _ ->
            run {
                sliderPositionAlpha = value
                mHeadLockedPanel.setAlpha(sliderPositionAlpha)
            }
            updateUIState()
        }

        buttonHidePanel.setOnClickListener {
            modelIsEnabled = mHeadLockedPanel.isEnabled(true)
            mHeadLockedPanel.setEnabled(!modelIsEnabled)
            modelIsEnabled = !modelIsEnabled
            updateUIState()
        }

        buttonDisableHeadlock.setOnClickListener {
            mEnableHeadlock = !mEnableHeadlock
            updateUIState()
        }
    }

    private fun createHeadLockedPanel() {
        this.mHeadLockedPanelView.postOnAnimation(this::updateHeadLockedPose)
        this.mHeadLockedPanel =
            PanelEntity.create(
                session = mSession,
                view = mHeadLockedPanelView,
                pixelDimensions = IntSize2d(800, 360),
                name = "headLockedPanel",
                pose = Pose(Vector3(0f, 0f, 0f)),
            )
        this.mHeadLockedPanel.parent = mSession.scene.activitySpace
    }

    private fun updateUIState() {
        sliderLabelAlpha.text = "Alpha ${sliderPositionAlpha}%"
        buttonHidePanel.text = (if (modelIsEnabled) "Hide Panel" else "Show Panel")
        buttonDisableHeadlock.text =
            (if (mEnableHeadlock) "Disable Headlock" else "Enable Headlock")
    }

    private fun updateHeadLockedPose() {
        if (mSession.scene.spatialUser.head != null && this.mEnableHeadlock) {
            // Since the panel is parented by the activitySpace, we need to inverse its scale
            // so that the panel stays at a fixed size in the view even when ActivitySpace scales.
            this.mHeadLockedPanel.setScale(
                0.5f / mSession.scene.activitySpace.getScale(Space.REAL_WORLD)
            )
            mSession.scene.spatialUser.head
                ?.transformPoseTo(mUserForward, mSession.scene.activitySpace)
                ?.let { this.mHeadLockedPanel.setPose(it) }
        }
        mHeadLockedPanelView.postOnAnimation(this::updateHeadLockedPose)
    }
}
