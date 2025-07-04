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

package androidx.xr.scenecore.samples.visibilitytest

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SpatialPointerComponent
import androidx.xr.scenecore.SpatialPointerIcon
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import kotlinx.coroutines.launch

@Suppress("Deprecation")
// TODO - b/421386891: is/setHidden is deprecated; this activity needs to be updated to use
// is/setEnabled.
class VisibilityTestActivity : AppCompatActivity() {

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    private var parentGltfEntity: GltfModelEntity? = null
    private var childGltfEntity1: GltfModelEntity? = null
    private var childGltfEntity2: GltfModelEntity? = null

    private var parentPanelEntity: PanelEntity? = null
    private var childPanelEntity1: PanelEntity? = null
    private var childPanelEntity2: PanelEntity? = null

    private lateinit var childPanel1PointerComponent: SpatialPointerComponent

    private var isFsm = true // launch in FSM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.visibilitytest_activity)

        parentPanelEntity = createPanelEntity(session, "Parent Panel", session.scene.activitySpace)
        childPanelEntity1 = createPanelEntity(session, "Child Panel 1", parentPanelEntity)
        childPanel1PointerComponent = SpatialPointerComponent.create(session)
        if (!childPanelEntity1!!.addComponent(childPanel1PointerComponent)) {
            throw RuntimeException("Failed to add spatial pointer component to child panel 1")
        }

        childPanelEntity2 = createPanelEntity(session, "Child Panel 2", childPanelEntity1)
        lifecycleScope.launch {
            val dragonModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
            setUpScene(dragonModel)
        }
    }

    private fun setUpScene(dragonModel: GltfModel) {
        createGltfEntities(session, dragonModel)

        findViewById<Button>(R.id.toggle_fsm_hsm).setOnClickListener { _ ->
            if (isFsm) {
                session.scene.requestHomeSpaceMode()
                isFsm = false
            } else {
                session.scene.requestFullSpaceMode()
                isFsm = true
            }
        }

        // Hiding the activitySpace hides everything, so we automatically unhide after 3 seconds
        // because
        // the user cannot see the Switch to unhide.
        findViewById<Button>(R.id.hide_space).setOnClickListener { _ ->
            session.scene.activitySpace.setHidden(true)
            Handler(Looper.getMainLooper())
                .postDelayed({ session.scene.activitySpace.setHidden(false) }, 3000)
        }

        findViewById<Button>(R.id.hide_main_panel).setOnClickListener { _ ->
            session.scene.mainPanelEntity.setHidden(true)
            Handler(Looper.getMainLooper())
                .postDelayed({ session.scene.mainPanelEntity.setHidden(false) }, 3000)
        }

        findViewById<Switch>(R.id.hide_all).setOnCheckedChangeListener { _, isChecked: Boolean ->
            setHiddenForAllEntities(isChecked)
            updateToggles()
        }

        findViewById<Switch>(R.id.hide_gltf0).setOnCheckedChangeListener { _, isChecked: Boolean ->
            parentGltfEntity?.setHidden(isChecked)
        }
        findViewById<Switch>(R.id.hide_gltf1).setOnCheckedChangeListener { _, isChecked: Boolean ->
            childGltfEntity1?.setHidden(isChecked)
        }
        findViewById<Switch>(R.id.hide_gltf2).setOnCheckedChangeListener { _, isChecked: Boolean ->
            childGltfEntity2?.setHidden(isChecked)
        }

        findViewById<Switch>(R.id.hide_panel0).setOnCheckedChangeListener { _, isChecked: Boolean ->
            parentPanelEntity?.setHidden(isChecked)
        }
        findViewById<Switch>(R.id.hide_panel1).setOnCheckedChangeListener { _, isChecked: Boolean ->
            childPanelEntity1?.setHidden(isChecked)
        }
        findViewById<Switch>(R.id.hide_panel1_pointer).setOnCheckedChangeListener {
            _,
            isChecked: Boolean ->
            childPanel1PointerComponent.spatialPointerIcon =
                if (isChecked) SpatialPointerIcon.NONE else SpatialPointerIcon.DEFAULT
        }
        findViewById<Switch>(R.id.hide_panel2).setOnCheckedChangeListener { _, isChecked: Boolean ->
            childPanelEntity2?.setHidden(isChecked)
        }
        findViewById<Button>(R.id.move_gltf0).setOnClickListener { _ ->
            val original = parentGltfEntity!!.getPose().translation
            val newPose =
                parentGltfEntity!!
                    .getPose()
                    .copy(Vector3(original.x + 0.25f, original.y + 0.5f, original.z))
            parentGltfEntity!!.setPose(newPose)
        }
    }

    private fun updateToggles() {
        findViewById<Switch>(R.id.hide_gltf0).isChecked = parentGltfEntity!!.isHidden(false)
        findViewById<Switch>(R.id.hide_gltf1).isChecked = childGltfEntity1!!.isHidden(false)
        findViewById<Switch>(R.id.hide_gltf2).isChecked = childGltfEntity2!!.isHidden(false)

        findViewById<Switch>(R.id.hide_panel0).isChecked = parentPanelEntity!!.isHidden(false)
        findViewById<Switch>(R.id.hide_panel1).isChecked = childPanelEntity1!!.isHidden(false)
        findViewById<Switch>(R.id.hide_panel2).isChecked = childPanelEntity2!!.isHidden(false)
    }

    private fun setHiddenForAllEntities(hidden: Boolean) {
        parentGltfEntity?.setHidden(hidden)
        childGltfEntity1?.setHidden(hidden)
        childGltfEntity2?.setHidden(hidden)

        parentPanelEntity?.setHidden(hidden)
        childPanelEntity1?.setHidden(hidden)
        childPanelEntity2?.setHidden(hidden)
    }

    private fun createPanelEntity(session: Session, name: String, parent: Entity?): PanelEntity {
        @SuppressLint("InflateParams")
        val panelContentView = layoutInflater.inflate(R.layout.panel, null)
        val panelEntity =
            PanelEntity.create(
                session,
                panelContentView,
                IntSize2d(640, 480),
                name,
                Pose(Vector3(-0.5f, -0.1f, 0f)),
            )
        panelEntity.parent = parent
        if (!panelEntity.addComponent(MovableComponent.createSystemMovable(session, false))) {
            throw RuntimeException("Failed to add movable component to panel")
        }
        panelContentView.findViewById<TextView>(R.id.textView).text = name
        return panelEntity
    }

    private fun createGltfEntities(session: Session, model: GltfModel) {
        parentGltfEntity = GltfModelEntity.create(session, model, Pose(Vector3(1.5f, 0f, -2f)))
        parentGltfEntity?.parent = session.scene.activitySpace

        childGltfEntity1 = GltfModelEntity.create(session, model, Pose(Vector3(0.5f, -0.5f, 0f)))
        childGltfEntity1?.parent = parentGltfEntity

        childGltfEntity2 = GltfModelEntity.create(session, model, Pose(Vector3(0.5f, -0.5f, 0f)))
        childGltfEntity2?.parent = childGltfEntity1
    }
}
