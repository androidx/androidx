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
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpatialPointerComponent
import androidx.xr.scenecore.SpatialPointerIcon
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.SpatialMode
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class VisibilityActivity : AppCompatActivity() {
    private val viewModel: VisibilityViewModel by viewModels()

    private var session: Session? = null

    private var parentGltfEntity: GltfModelEntity? = null
    private var childGltfEntity1: GltfModelEntity? = null
    private var childGltfEntity2: GltfModelEntity? = null

    private var parentPanelEntity: PanelEntity? = null
    private var childPanelEntity1: PanelEntity? = null
    private var childPanelEntity2: PanelEntity? = null

    private lateinit var model: GltfModel
    private var childPanel1PointerComponent: SpatialPointerComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_visibility)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()

        lifecycleScope.launch {
            session = SessionManager(this@VisibilityActivity).createSession()
            if (session == null) {
                finish()
                return@launch
            }
            session
                ?.scene
                ?.mainPanelEntity
                ?.addComponent(MovableComponent.createSystemMovable(session!!))
            // Disable default scale overrides on key entity from Spatial Mode events
            session?.scene?.setSpaceChangedListener { event ->
                session?.scene?.keyEntity?.setPose(event.recommendedPose, Space.ACTIVITY)
            }
            session!!.scene.activitySpace.addBoundsChangedListener { dimensions ->
                val mode =
                    if (dimensions.width == Float.POSITIVE_INFINITY) SpatialMode.FSM
                    else SpatialMode.HSM
                viewModel.setSpatialMode(mode)
            }
            session?.scene?.keyEntity = session?.scene?.mainPanelEntity

            // Async get gltf model and create entities
            model = GltfModel.create(session!!, MODEL_PATH)
            createGltfEntities()
            createActivityPanels()

            // Observe ViewModel state and synchronize UI and entity visibility
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> syncState(state) }
            }
        }
    }

    private fun setupViews() {
        // Toolbar action
        findViewById<Toolbar>(R.id.visibility_top_app_bar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { finish() }
            it.setTitle(R.string.cuj_visibility_test)
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this) }
        }

        // fsm/hsm toggle
        findViewById<Button>(R.id.visibility_toggle_fsm_hsm).also {
            it.setOnClickListener { toggleMode() }
        }

        // Temporary 3-second hide space
        findViewById<Button>(R.id.visibility_hide_activity_space).also {
            it.setOnClickListener { viewModel.hideActivitySpaceTemporarily() }
        }

        // Temporary 3-second hide main panel
        findViewById<Button>(R.id.visibility_hide_main_panel).also {
            it.setOnClickListener { viewModel.hideMainPanelTemporarily() }
        }

        // Hide all switch
        findViewById<SwitchMaterial>(R.id.visibility_hide_all_entities)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                viewModel.setHideAllChecked(isChecked)
            }

        // Hide gltf entities switches
        findViewById<SwitchMaterial>(R.id.visibility_hide_parent_gltf).setOnCheckedChangeListener {
            _,
            isChecked: Boolean ->
            viewModel.setParentGltfHidden(isChecked)
        }
        findViewById<SwitchMaterial>(R.id.visibility_hide_first_child_gltf)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                viewModel.setChildGltf1Hidden(isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_second_child_gltf)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                viewModel.setChildGltf2Hidden(isChecked)
            }

        // Hide panels switches
        findViewById<SwitchMaterial>(R.id.visibility_hide_parent_panel)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                viewModel.setParentPanelHidden(isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_first_child_panel)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                viewModel.setChildPanel1Hidden(isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_second_child_panel)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                viewModel.setChildPanel2Hidden(isChecked)
            }
        findViewById<SwitchMaterial>(R.id.visibility_hide_panel1_pointer)
            .setOnCheckedChangeListener { _, isChecked: Boolean ->
                viewModel.setPanel1PointerHidden(isChecked)
            }

        // Move gltf entities by moving the parent entity
        findViewById<Button>(R.id.visibility_move_parent_gltf).setOnClickListener { _ ->
            parentGltfEntity?.let { entity ->
                val original = entity.getPose().translation
                val newPose =
                    entity
                        .getPose()
                        .copy(Vector3(original.x + 0.25f, original.y + 0.5f, original.z))
                entity.setPose(newPose)
            }
        }
    }

    private fun syncState(state: VisibilityUiState) {
        val currentSession = session ?: return

        // 1. Sync temporary space & main panel hides
        currentSession.scene.activitySpace.setEnabled(!state.isActivitySpaceTemporarilyHidden)
        currentSession.scene.mainPanelEntity.setEnabled(!state.isMainPanelTemporarilyHidden)

        // 2. Hide All overrides scene entities without altering individual switch states in
        // ViewModel
        val hideAll = state.isHideAllChecked

        parentGltfEntity?.setEnabled(!hideAll && !state.isParentGltfHidden)
        childGltfEntity1?.setEnabled(!hideAll && !state.isChildGltf1Hidden)
        childGltfEntity2?.setEnabled(!hideAll && !state.isChildGltf2Hidden)

        parentPanelEntity?.setEnabled(!hideAll && !state.isParentPanelHidden)
        childPanelEntity1?.setEnabled(!hideAll && !state.isChildPanel1Hidden)
        childPanelEntity2?.setEnabled(!hideAll && !state.isChildPanel2Hidden)

        childPanel1PointerComponent?.spatialPointerIcon =
            if (hideAll || state.isPanel1PointerHidden) SpatialPointerIcon.NONE
            else SpatialPointerIcon.DEFAULT

        // 3. Sync UI switches
        updateSwitch(R.id.visibility_hide_all_entities, state.isHideAllChecked)
        updateSwitch(R.id.visibility_hide_parent_gltf, state.isParentGltfHidden)
        updateSwitch(R.id.visibility_hide_first_child_gltf, state.isChildGltf1Hidden)
        updateSwitch(R.id.visibility_hide_second_child_gltf, state.isChildGltf2Hidden)
        updateSwitch(R.id.visibility_hide_parent_panel, state.isParentPanelHidden)
        updateSwitch(R.id.visibility_hide_first_child_panel, state.isChildPanel1Hidden)
        updateSwitch(R.id.visibility_hide_second_child_panel, state.isChildPanel2Hidden)
        updateSwitch(R.id.visibility_hide_panel1_pointer, state.isPanel1PointerHidden)
    }

    private fun updateSwitch(id: Int, isChecked: Boolean) {
        val switch = findViewById<SwitchMaterial>(id)
        if (switch.isChecked != isChecked) {
            switch.isChecked = isChecked
        }
    }

    private fun toggleMode() {
        val currentSession = session ?: return
        when (viewModel.uiState.value.spatialMode) {
            SpatialMode.FSM -> {
                currentSession.scene.requestHomeSpace()
                viewModel.setSpatialMode(SpatialMode.HSM)
            }

            SpatialMode.HSM -> {
                currentSession.scene.requestFullSpace()
                viewModel.setSpatialMode(SpatialMode.FSM)
            }
        }
    }

    private fun createActivityPanels() {
        parentPanelEntity =
            createPanel(
                "Parent Panel",
                session!!.scene.mainPanelEntity,
                Pose(Vector3(-0.5f, -0.65f, 0.1f)),
            )
        childPanelEntity1 =
            createPanel("Child Panel 1", parentPanelEntity, Pose(Vector3(0.5f, 0f, 0f)))
        childPanelEntity2 =
            createPanel("Child Panel 2", childPanelEntity1, Pose(Vector3(0.5f, 0f, 0f)))

        childPanel1PointerComponent = SpatialPointerComponent.create(session!!)
        if (!childPanelEntity1!!.addComponent(childPanel1PointerComponent!!)) {
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
            PanelEntity.create(
                session!!,
                panelContentView,
                IntSize2d(640, 480),
                panelName,
                pose,
                parent = parent,
            )

        val movableComponent = MovableComponent.createSystemMovable(session!!)
        panelEntity.addComponent(movableComponent)
        movableComponent.size = getSizeInLocalSpace(panelEntity).to3d()

        panelContentView.findViewById<Toolbar>(R.id.activity_panel_tool_bar).setTitle(panelName)
        return panelEntity
    }

    private fun createGltfEntities() {
        parentGltfEntity =
            GltfModelEntity.create(
                    session!!,
                    model,
                    Pose(Vector3(2f, 0f, 0f)),
                    parent = session!!.scene.activitySpace,
                )
                .also {
                    it.setScale(0.5f)
                    it.parent = session!!.scene.activitySpace
                }

        childGltfEntity1 =
            GltfModelEntity.create(
                    session!!,
                    model,
                    Pose(Vector3(0.7f, -0.3f, 0f)),
                    parent = session!!.scene.activitySpace,
                )
                .also {
                    it.setScale(0.5f)
                    it.parent = parentGltfEntity
                }

        childGltfEntity2 =
            GltfModelEntity.create(
                    session!!,
                    model,
                    Pose(Vector3(0.7f, -0.6f, 0f)),
                    parent = session!!.scene.activitySpace,
                )
                .also {
                    it.setScale(0.5f)
                    it.parent = childGltfEntity1
                }
    }

    private fun getSizeInLocalSpace(panel: PanelEntity): FloatSize2d {
        val scaledSize = panel.size
        val spaceScale = panel.getScale()
        return FloatSize2d(scaledSize.width / spaceScale, scaledSize.height / spaceScale)
    }

    companion object {
        const val ACTIVITY_NAME: String = "visibilityActivity"
        val MODEL_PATH: Path = Paths.get("models", "Dragon_Evolved.gltf")
    }
}
