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

package androidx.xr.scenecore.testapp.panelroundedcorner

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SpatialCapabilities
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.activitypanel.ActivityPanel
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch

const val TAG = "PanelRoundedCornerActivity"

private const val DEFAULT_CORNER_RADIUS = 28f

@SuppressLint("SetTextI18n", "RestrictedApi")
class PanelRoundedCornerActivity : AppCompatActivity() {
    private var activityPanelEntity: ActivityPanelEntity? = null
    private var panelEntity: PanelEntity? = null
    private var session: Session? = null
    private var activityPanelCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create session
        session = createSession(this)
        if (session == null) this.finish()
        session!!.scene.addSpatialCapabilitiesChangedListener { capabilities ->
            tryToCreateActivityPanel(capabilities)
        }
        tryToCreateActivityPanel(session!!.scene.spatialCapabilities)

        @SuppressLint("InflateParams")
        val panelEntityView = layoutInflater.inflate(R.layout.rounded_corner_panel_entity, null)

        if (panelEntityView == null) {
            Log.e(ACTIVITY_NAME, "Failed to inflate corner_rounded_panel_entity")
        }

        panelEntity =
            PanelEntity.create(
                session!!,
                panelEntityView,
                IntSize2d(640, 480),
                "panel_entity",
                Pose(Vector3(0.1f, -0.5f, 0.1f)),
            )

        val mainPanelSwitch = panelEntityView.findViewById<MaterialSwitch>(R.id.main_panel_switch)
        mainPanelSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                session!!.scene.mainPanelEntity.cornerRadius = 0.0f
            } else {
                session!!.scene.mainPanelEntity.cornerRadius =
                    calculateCornerRadiusInMeters(session!!.scene.mainPanelEntity, 32f)
            }
            session!!.scene.mainPanelEntity.setPose(session!!.scene.mainPanelEntity.getPose())
        }
        val activityPanelSwitch =
            panelEntityView.findViewById<MaterialSwitch>(R.id.activity_panel_switch)
        activityPanelSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (activityPanelEntity == null) {
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                activityPanelEntity?.cornerRadius = 0.0f
            } else {
                activityPanelEntity?.cornerRadius =
                    calculateCornerRadiusInMeters(activityPanelEntity!!, 32f)
            }
            activityPanelEntity?.setPose(activityPanelEntity!!.getPose())
        }
        val panelEntitySwitch =
            panelEntityView.findViewById<MaterialSwitch>(R.id.panel_entity_switch)
        panelEntitySwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                panelEntity!!.cornerRadius = 0.0f
            } else {
                panelEntity!!.cornerRadius = calculateCornerRadiusInMeters(panelEntity!!, 32f)
            }
            panelEntity!!.setPose(panelEntity!!.getPose())
        }

        // Set main panel dimensions
        setContentView(R.layout.common_test_panel)
        session!!.scene.mainPanelEntity.setPose(Pose(Vector3(-0.1f, 0.1f, 0.0f)))

        // Set toolbar
        val toolbar: Toolbar = findViewById(R.id.top_app_bar_activity_panel)
        setSupportActionBar(toolbar)
        toolbar.setTitle(R.string.cuj_panel_rounded_corner)
        toolbar.setNavigationOnClickListener { this.finish() }

        // Hide the center button
        findViewById<Button>(R.id.spawn_activity_panel_button).visibility = View.GONE

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@PanelRoundedCornerActivity) }
        }
    }

    fun calculateCornerRadiusInMeters(entity: PanelEntity, cornerRadiusDp: Float): Float {
        val pixelDensity = entity.sizeInPixels.width.toFloat() / entity.size.width
        val radiusPixels =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                cornerRadiusDp,
                Resources.getSystem().displayMetrics,
            )
        return radiusPixels / pixelDensity
    }

    override fun onDestroy() {
        super.onDestroy()
        activityPanelEntity?.parent = null
        activityPanelEntity?.dispose()
        panelEntity?.parent = null
        panelEntity?.dispose()
    }

    override fun onStop() {
        super.onStop()
        activityPanelEntity?.parent = null
        activityPanelEntity?.dispose()
        panelEntity?.parent = null
        panelEntity?.dispose()
    }

    fun tryToCreateActivityPanel(capabilities: SpatialCapabilities) {
        if (
            capabilities.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY) &&
                !activityPanelCreated
        ) {
            activityPanelEntity =
                ActivityPanelEntity.create(session!!, IntSize2d(640, 480), "activity_panel")
            val intent = Intent(this, ActivityPanel::class.java)
            intent.putExtra("NAV_ICON", false)
            activityPanelEntity!!.launchActivity(intent)
            activityPanelEntity!!.setPose(Pose(Vector3(0.75f, 0.0f, 0.0f)))
            activityPanelCreated = true
        }
    }

    companion object {
        const val ACTIVITY_NAME = TAG
    }
}
