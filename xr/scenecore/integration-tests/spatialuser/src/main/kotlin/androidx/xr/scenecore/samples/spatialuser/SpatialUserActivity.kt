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

package androidx.xr.scenecore.samples.spatialuser

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.CameraView
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import kotlin.math.tan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpatialUserActivity : AppCompatActivity() {
    private val TAG = "SpatialUserTag"
    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private val poseOffset = Pose(Vector3(0f, 0f, -1f), Quaternion.Identity)
    private var checkVisibility = false
    var panelString = "Left: Visible \n Right: Invisible"

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
        setContentView(R.layout.spatialuser_activity)
        session.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))

        // Create a single panel with text
        @SuppressLint("InflateParams")
        val panelContentView = layoutInflater.inflate(R.layout.panel, null)
        val panelEntity =
            PanelEntity.create(
                session,
                panelContentView,
                IntSize2d(640, 480),
                "panel",
                Pose(Vector3(0f, 0f, 0.5f)),
            )
        panelEntity.parent = session.scene.activitySpace

        val buttonRecenter: Button = panelContentView.findViewById(R.id.buttonRecenter)
        buttonRecenter.setOnClickListener {
            val pos =
                session.scene.spatialUser.head?.transformPoseTo(
                    poseOffset,
                    session.scene.activitySpace,
                )
            if (pos != null) {
                panelEntity.setPose(pos)
            }
        }
        checkVisibility(session, panelContentView.findViewById(R.id.textView))
    }

    private fun checkVisibility(session: Session, panelView: TextView) {
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
                panelString = "Is Main Panel In View?\nLeft: ${leftVisible}\nRight: ${rightVisible}"
                panelView.text = panelString
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
