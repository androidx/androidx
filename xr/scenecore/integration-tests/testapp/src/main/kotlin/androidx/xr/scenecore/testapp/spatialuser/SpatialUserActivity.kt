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

package androidx.xr.scenecore.testapp.spatialuser

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.CameraView
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.tan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class SpatialUserActivity : AppCompatActivity() {
    private var session: Session? = null
    private val poseOffset = Pose(Vector3(0f, 0f, -1f), Quaternion.Identity)
    private var checkVisibility = false
    private lateinit var spatialUserPanel: PanelEntity
    private lateinit var panelContentView: View

    override fun onStart() {
        super.onStart()
        checkVisibility = true
    }

    override fun onPause() {
        super.onPause()
        checkVisibility = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = createSession(this)
        if (session == null) this.finish()

        enableEdgeToEdge()
        setContentView(R.layout.common_test_panel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        session!!.configure(
            Config(
                planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                headTracking = Config.HeadTrackingMode.LAST_KNOWN,
            )
        )

        // toolbar
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setTitle(R.string.cuj_spatial_user_test)
            it.setNavigationOnClickListener { this.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@SpatialUserActivity) }
        }

        // Hide the default button in the layout
        findViewById<Button>(R.id.spawn_activity_panel_button).also { it.visibility = View.GONE }

        createPanel()
    }

    private fun createPanel() {
        panelContentView = layoutInflater.inflate(R.layout.activity_spatial_user, null)
        spatialUserPanel =
            PanelEntity.create(
                session!!,
                panelContentView,
                IntSize2d(640, 480),
                "Spatial User Test Panel",
                Pose(Vector3(0f, 0f, 0.5f)),
            )
        spatialUserPanel.parent = session!!.scene.activitySpace

        val buttonRecenter: Button =
            panelContentView.findViewById(R.id.spatial_user_panel_recenter_button)
        buttonRecenter.setBackgroundColor(getColor(R.color.purple_500))
        buttonRecenter.setTextColor(getColor(R.color.white))
        buttonRecenter.setOnClickListener {
            val pos =
                session!!
                    .scene
                    .spatialUser
                    .head
                    ?.transformPoseTo(poseOffset, session!!.scene.activitySpace)
            if (pos != null) {
                spatialUserPanel.setPose(pos)
            }
        }
        checkVisibility(
            session!!,
            panelContentView.findViewById(R.id.left_eye_textView),
            panelContentView.findViewById(R.id.right_eye_textview),
        )
    }

    private fun checkVisibility(
        session: Session,
        panelLeftView: TextView,
        panelRightView: TextView,
    ) {
        lifecycleScope.launch {
            while (true) {
                delay(16L)
                val leftCamera =
                    session.scene.spatialUser.cameraViews[CameraView.CameraType.LEFT_EYE]
                val rightCamera =
                    session.scene.spatialUser.cameraViews[CameraView.CameraType.RIGHT_EYE]
                val leftVisible =
                    leftCamera?.let { isEntityInView(session.scene.mainPanelEntity, it) } ?: false
                val rightVisible =
                    rightCamera?.let { isEntityInView(session.scene.mainPanelEntity, it) } ?: false
                panelLeftView.text = "$leftVisible"
                panelRightView.text = "$rightVisible"
            }
        }
    }

    private fun isEntityInView(entity: Entity, camera: CameraView): Boolean {
        val cameraToEntity = entity.transformPoseTo(poseOffset, camera).translation

        // If the xValue is negative use the angleLeft to calculate.
        if (cameraToEntity.x < 0) {
            // Calculate the visible xDistance from the camera at the entities distance.
            val xDist = tan(camera.fov.angleLeft) * (-cameraToEntity.z)
            // If the entities distance is greater than the calculated view distance return
            // false
            if (cameraToEntity.x < xDist) {
                return false
            }
        } else if (cameraToEntity.x > 0) {
            val xDist = tan(camera.fov.angleRight) * (-cameraToEntity.z)
            if (cameraToEntity.x > xDist) {
                return false
            }
        }

        // Do the same with the Y values.
        if (cameraToEntity.y < 0) {
            val yDist = tan(camera.fov.angleDown) * (-cameraToEntity.z)
            if (cameraToEntity.y < yDist) {
                return false
            }
        } else if (cameraToEntity.y > 0) {
            val yDist = tan(camera.fov.angleUp) * (-cameraToEntity.z)
            if (cameraToEntity.y > yDist) {
                return false
            }
        }
        return true
    }
}
