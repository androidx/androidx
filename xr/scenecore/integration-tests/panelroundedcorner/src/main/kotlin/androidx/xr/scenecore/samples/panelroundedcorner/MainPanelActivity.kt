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

package androidx.xr.scenecore.samples.panelroundedcorner

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SpatialCapabilities
import androidx.xr.scenecore.samples.commontestview.CommonTestView
import androidx.xr.scenecore.scene

const val TAG = "MainPanelActivity"

class MainPanelActivity : AppCompatActivity() {
    private var activityPanelEntity: ActivityPanelEntity? = null
    private lateinit var panelEntity: PanelEntity
    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }
    var activityPanelCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session.scene.addSpatialCapabilitiesChangedListener() { capabilities ->
            tryToCreateActivityPanel(capabilities)
        }
        tryToCreateActivityPanel(session.scene.spatialCapabilities)
        @SuppressLint("InflateParams")
        val panelEntityView = layoutInflater.inflate(R.layout.panel_entity, null)
        panelEntity =
            PanelEntity.create(
                session,
                panelEntityView,
                IntSize2d(640, 880),
                "panel_entity",
                Pose(Vector3(0f, 0f, 0.0f)),
            )
        session.scene.mainPanelEntity.setPose(Pose(Vector3(-0.75f, 0.0f, 0.0f)))

        val mainPanelSwitch = panelEntityView.findViewById<Switch>(R.id.main_panel_switch)
        mainPanelSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                session.scene.mainPanelEntity.cornerRadius = 0.0f
            } else {
                session.scene.mainPanelEntity.cornerRadius =
                    calculateCornerRadiusInMeters(session.scene.mainPanelEntity, 32f)
            }
            session.scene.mainPanelEntity.setPose(session.scene.mainPanelEntity.getPose())
        }
        val activityPanelSwitch = panelEntityView.findViewById<Switch>(R.id.activity_panel_switch)
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
        val panelEntitySwitch = panelEntityView.findViewById<Switch>(R.id.panel_entity_switch)
        panelEntitySwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                panelEntity.cornerRadius = 0.0f
            } else {
                panelEntity.cornerRadius = calculateCornerRadiusInMeters(panelEntity, 32f)
            }
            panelEntity.setPose(panelEntity.getPose())
        }
        setContentView(CommonTestView(this))
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
    }

    fun tryToCreateActivityPanel(capabilities: SpatialCapabilities) {
        if (
            capabilities.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY) &&
                !activityPanelCreated
        ) {
            activityPanelEntity =
                ActivityPanelEntity.create(session, IntSize2d(1280, 800), "activity_panel")

            val intent = Intent(this, ActivityPanelActivity::class.java)
            activityPanelEntity?.launchActivity(intent)
            activityPanelEntity?.setPose(Pose(Vector3(0.75f, 0.0f, 0.0f)))
            activityPanelCreated = true
        }
    }
}
