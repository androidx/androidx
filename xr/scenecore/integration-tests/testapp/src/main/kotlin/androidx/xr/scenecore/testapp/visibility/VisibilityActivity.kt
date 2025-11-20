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

package androidx.xr.scenecore.testapp.visibility

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
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
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.SpatialMode
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class VisibilityActivity : AppCompatActivity() {
    private var session: Session? = null

    private var parentGltfEntity: GltfModelEntity? = null
    private var childGltfEntity1: GltfModelEntity? = null
    private var childGltfEntity2: GltfModelEntity? = null

    private var parentPanelEntity: PanelEntity? = null
    private var childPanelEntity1: PanelEntity? = null
    private var childPanelEntity2: PanelEntity? = null

    private lateinit var model: GltfModel
    private lateinit var childPanel1PointerComponent: SpatialPointerComponent

    private var spatialMode: SpatialMode = SpatialMode.FSM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_visibility)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Create rendering session
        session = createSession(this)
        if (session == null) this.finish()

        // Toolbar action
        findViewById<Toolbar>(R.id.visibility_top_app_bar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this.finish() }
            it.setTitle(R.string.cuj_visibility_test)
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@VisibilityActivity) }
        }

        // fsm/hsm toggle
        findViewById<Button>(R.id.visibility_toggle_fsm_hsm).also {
            it.setOnClickListener { toggleMode() }
        }

        // Hide space
        findViewById<Button>(R.id.visibility_hide_activity_space).also {
            it.setOnClickListener {
                session!!.scene.activitySpace.setEnabled(false)
                Handler(Looper.getMainLooper())
                    .postDelayed(
                        { session!!.scene.activitySpace.setEnabled(true) },
                        DELAY_FOR_3_SEC,
                    )
            }
        }

        // Hide main panel
        findViewById<Button>(R.id.visibility_hide_main_panel).also {
            it.setOnClickListener {
                session!!.scene.mainPanelEntity.setEnabled(false)
                Handler(Looper.getMainLooper())
                    .postDelayed(
                        { session!!.scene.mainPanelEntity.setEnabled(true) },
                        DELAY_FOR_3_SEC,
                    )
            }
        }

        // Hide all
        findViewById<SwitchMaterial>(R.id.visibility_hide_all_entities)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                setDisabledForAllEntities(isChecked)
                updateToggles()
            }

        // Hide gltf entities
        findViewById<SwitchMaterial>(R.id.visibility_hide_parent_gltf).setOnCheckedChangeListener {
            _,
            isChecked: Boolean ->
            parentGltfEntity?.setEnabled(!isChecked)
        }
        findViewById<SwitchMaterial>(R.id.visibility_hide_first_child_gltf)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                childGltfEntity1?.setEnabled(!isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_second_child_gltf)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                childGltfEntity2?.setEnabled(!isChecked)
            }

        // Hide panels
        findViewById<SwitchMaterial>(R.id.visibility_hide_parent_panel)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                parentPanelEntity?.setEnabled(!isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_first_child_panel)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                childPanelEntity1?.setEnabled(!isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_second_child_panel)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                childPanelEntity2?.setEnabled(!isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_panel1_pointer)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                childPanel1PointerComponent.spatialPointerIcon =
                    if (isChecked) SpatialPointerIcon.NONE else SpatialPointerIcon.DEFAULT
            }

        // Move gltf entities by moving the parent entity
        findViewById<Button>(R.id.visibility_move_parent_gltf).setOnClickListener { _ ->
            val original = parentGltfEntity!!.getPose().translation
            val newPose =
                parentGltfEntity!!
                    .getPose()
                    .copy(Vector3(original.x + 0.25f, original.y + 0.5f, original.z))
            parentGltfEntity!!.setPose(newPose)
        }

        lifecycleScope.launch {
            // Async get gltf model
            model = GltfModel.create(session!!, MODEL_PATH)

            // create gltf entities
            createGltfEntities()

            // Create activity panel
            createActivityPanels()
        }
    }

    private fun toggleMode() {
        when (spatialMode) {
            SpatialMode.FSM -> {
                session!!.scene.requestHomeSpaceMode()
                spatialMode = SpatialMode.HSM
            }
            SpatialMode.HSM -> {
                session!!.scene.requestFullSpaceMode()
                spatialMode = SpatialMode.FSM
            }
        }
    }

    private fun createActivityPanels() {
        parentPanelEntity =
            createPanel(
                "Parent Panel",
                session!!.scene.activitySpace,
                Pose(Vector3(-0.5f, -0.65f, 0f)),
            )
        childPanelEntity1 =
            createPanel("Child Panel 1", parentPanelEntity, Pose(Vector3(0.5f, 0f, 0f)))
        childPanelEntity2 =
            createPanel("Child Panel 2", childPanelEntity1, Pose(Vector3(0.5f, 0f, 0f)))

        childPanel1PointerComponent = SpatialPointerComponent.create(session!!)
        if (!childPanelEntity1!!.addComponent(childPanel1PointerComponent)) {
            throw RuntimeException("Failed to add spatial pointer component to child panel 1")
        }
    }

    private fun createPanel(panelName: String, parent: Entity?, pose: Pose): PanelEntity {
        val panelContentView = layoutInflater.inflate(R.layout.activity_panel, null)
        panelContentView.findViewById<Toolbar>(R.id.activity_panel_tool_bar).also {
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        val panelEntity =
            PanelEntity.create(session!!, panelContentView, IntSize2d(640, 480), panelName, pose)
        panelEntity.parent = parent

        val movableComponent = MovableComponent.createSystemMovable(session!!)
        panelEntity.addComponent(movableComponent)
        movableComponent.size = getSizeInLocalSpace(panelEntity).to3d()

        panelContentView.findViewById<Toolbar>(R.id.activity_panel_tool_bar).setTitle(panelName)
        return panelEntity
    }

    private fun createGltfEntities() {
        parentGltfEntity =
            GltfModelEntity.create(session!!, model, Pose(Vector3(0.7f, 0f, 0f))).also {
                it.setScale(0.5f)
                it.parent = session!!.scene.activitySpace
            }

        childGltfEntity1 =
            GltfModelEntity.create(session!!, model, Pose(Vector3(0.7f, -0.3f, 0f))).also {
                it.setScale(0.5f)
                it.parent = parentGltfEntity
            }

        childGltfEntity2 =
            GltfModelEntity.create(session!!, model, Pose(Vector3(0.7f, -0.6f, 0f))).also {
                it.setScale(0.5f)
                it.parent = childGltfEntity1
            }
    }

    private fun getSizeInLocalSpace(panel: PanelEntity): FloatSize2d {
        val scaledSize = panel.size
        val spaceScale = panel.getScale()
        return FloatSize2d(scaledSize.width / spaceScale, scaledSize.height / spaceScale)
    }

    private fun setDisabledForAllEntities(disabled: Boolean) {
        parentGltfEntity?.setEnabled(!disabled)
        childGltfEntity1?.setEnabled(!disabled)
        childGltfEntity2?.setEnabled(!disabled)

        parentPanelEntity?.setEnabled(!disabled)
        childPanelEntity1?.setEnabled(!disabled)
        childPanelEntity2?.setEnabled(!disabled)
    }

    private fun updateToggles() {
        findViewById<SwitchMaterial>(R.id.visibility_hide_parent_gltf).isChecked =
            !parentGltfEntity!!.isEnabled(false)
        findViewById<SwitchMaterial>(R.id.visibility_hide_first_child_gltf).isChecked =
            !childGltfEntity1!!.isEnabled(false)
        findViewById<SwitchMaterial>(R.id.visibility_hide_second_child_gltf).isChecked =
            !childGltfEntity2!!.isEnabled(false)

        findViewById<SwitchMaterial>(R.id.visibility_hide_parent_panel).isChecked =
            !parentPanelEntity!!.isEnabled(false)
        findViewById<SwitchMaterial>(R.id.visibility_hide_first_child_panel).isChecked =
            !childPanelEntity1!!.isEnabled(false)
        findViewById<SwitchMaterial>(R.id.visibility_hide_second_child_panel).isChecked =
            !childPanelEntity2!!.isEnabled(false)
    }

    companion object {
        const val DELAY_FOR_3_SEC: Long = 3000
        const val ACTIVITY_NAME: String = "visibilityActivity"
        val MODEL_PATH: Path = Paths.get("models", "Dragon_Evolved.gltf")
    }
}
