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

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.DebugTextLinearView
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.function.Consumer
import kotlinx.coroutines.flow.MutableStateFlow

class FieldOfViewVisibilityActivity : AppCompatActivity() {
    private val TAG = "FieldOfViewVisibility"
    private var session: Session? = null
    private lateinit var mGltfManager: GltfManager
    private lateinit var mSurfaceEntityManager: SurfaceEntityManager
    private lateinit var mSpatialEnvironmentManager: SpatialEnvironmentManager
    private lateinit var mHeadLockedUIManager: HeadLockedUIManager
    private lateinit var mPanelEntityManager: PanelEntityManager
    private lateinit var mPerceivedResolutionManager: PerceivedResolutionManager
    private lateinit var mHeadLockedPanelView: DebugTextLinearView
    private val _mSpatialVisibilityFlow =
        MutableStateFlow(SpatialVisibility(SpatialVisibility.UNKNOWN))
    var mSpatialVisibility: SpatialVisibility
        get() = _mSpatialVisibilityFlow.value
        set(value) {
            _mSpatialVisibilityFlow.value = value
        }

    private val _mPerceivedResolutionFlow = MutableStateFlow(IntSize2d(0, 0))
    var mPerceivedResolution: IntSize2d
        get() = _mPerceivedResolutionFlow.value
        set(value) {
            _mPerceivedResolutionFlow.value = value
        }

    private val mPerceivedResolutionListener: Consumer<IntSize2d> = Consumer {
        mPerceivedResolution = it
        Log.i(TAG, "Perceived Resolution listener called $mPerceivedResolution")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_of_view_visibility)

        session = createSession(this)
        if (session == null) this.finish()
        session!!.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))

        // toolbar
        findViewById<Toolbar>(R.id.top_app_bar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this@FieldOfViewVisibilityActivity.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@FieldOfViewVisibilityActivity) }
        }

        createHeadLockedPanel()
        setupMainPanel()
    }

    override fun onDestroy() {
        super.onDestroy()
        session!!.scene.clearSpatialVisibilityChangedListener()
        session!!.scene.removePerceivedResolutionChangedListener(mPerceivedResolutionListener)
    }

    private fun createHeadLockedPanel() {
        mHeadLockedPanelView = DebugTextLinearView(context = this)
        mHeadLockedPanelView.setName("Spatial Visibility")
        mHeadLockedPanelView.setLine("State", "UNKNOWN")
        this.mHeadLockedUIManager = HeadLockedUIManager(session!!, this, mHeadLockedPanelView)

        session!!.scene.setSpatialVisibilityChangedListener { visibility: SpatialVisibility ->
            mSpatialVisibility = visibility
            Log.i(TAG, "Spatial visibility changed listener called $visibility")
            mHeadLockedPanelView.setLine("State", "$visibility")
            updateTextViews()
        }
        session!!.scene.addPerceivedResolutionChangedListener(mPerceivedResolutionListener)
    }

    private fun updateTextViews() {
        findViewById<TextView>(R.id.fov_textview1).also {
            it.text = "SpatialVisibility: $mSpatialVisibility"
        }

        findViewById<TextView>(R.id.fov_textview2).also {
            it.text = "Perceived Resolution (HSM): $mPerceivedResolution"
        }

        findViewById<TextView>(R.id.fov_textview3).also {
            it.text =
                "To turn on bounding boxes, use these ADB Commands:\n" +
                    "adb root\n" +
                    "adb shell setprop persist.spaceflinger.fov.visualize_bounds 1\n" +
                    "adb shell setprop persist.ix.sysui.editor_enabled 1\n" +
                    "adb reboot"
        }
    }

    private fun setupMainPanel() {
        updateTextViews()

        // Request FSM
        findViewById<Button>(R.id.button_request_fsm).also {
            it.setOnClickListener { session!!.scene.requestFullSpaceMode() }
        }

        // Request HSM
        findViewById<Button>(R.id.button_request_hsm).also {
            it.setOnClickListener { session!!.scene.requestHomeSpaceMode() }
        }

        // Make the main panel movable.
        val movableComponent = MovableComponent.createSystemMovable(session!!, scaleInZ = false)
        session!!.scene.mainPanelEntity.addComponent(movableComponent)

        // Create the UI component managers.
        mSpatialEnvironmentManager = SpatialEnvironmentManager(session!!, this)
        mSurfaceEntityManager = SurfaceEntityManager(session!!, this)
        mGltfManager = GltfManager(session!!, this)
        mPanelEntityManager = PanelEntityManager(session!!, this)
        mPerceivedResolutionManager =
            PerceivedResolutionManager(session!!, this, mSurfaceEntityManager, mPanelEntityManager)
    }
}
