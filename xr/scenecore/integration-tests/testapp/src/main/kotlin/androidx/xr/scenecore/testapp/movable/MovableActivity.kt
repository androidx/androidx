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

package androidx.xr.scenecore.testapp.movable

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorPlacement
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

/**
 * A simple activity that creates a panel and attaches a movable component to it.
 *
 * <p>The movable panel has switched that can be toggled to enable and disable all the options of
 * the movable component. The panel can also be parented to a stationary panel that is offset from
 * the activity space. When system movable is disabled, the panel will still be moved but the
 * position will be set manually based on the results of the move event.
 *
 * <p>The purpose of this app is to enable easy internal testing of the movable component. Unlike
 * the InputMoveResize sample app this is not intended to be tested by QA at this time.
 */
@SuppressLint("SetTextI18n", "RestrictedApi")
class MovableActivity : AppCompatActivity() {
    private var session: Session? = null

    private enum class MovableType {
        ANCHORABLE,
        SYSTEM,
        CUSTOM,
    }

    private var movableType = MovableType.ANCHORABLE
    private var scaleInZ = false

    private lateinit var scaleInZSwitch: MaterialSwitch
    private lateinit var anchorableSection: View
    private lateinit var customBehaviorSection: View
    private lateinit var customBehaviorGroup: RadioGroup

    private var movableComponent: MovableComponent? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var planeOrientationFilter: MutableSet<Int> = mutableSetOf()
    private var planeSemanticFilter: MutableSet<Int> = mutableSetOf()

    companion object {
        private const val TAG = "MovableActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.common_test_panel)

        if (!setupSession()) return
        initializeUI()
        val stationaryPanelEntity = createStationaryPanel()
        setupMovablePanel(stationaryPanelEntity)
        createAnchorableGltfEntity()
    }

    private fun setupSession(): Boolean {
        session = SessionManager(this).createSession()
        if (session == null) {
            Log.e(TAG, "Failed to create a session. Finishing activity.")
            finish()
            return false
        }
        session!!.configure(Config(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        session!!.scene.keyEntity = session!!.scene.mainPanelEntity

        // Enable passthrough by default to allow interaction with the real world,
        // which is necessary for testing anchoring functionality.
        session!!.scene.spatialEnvironment.preferredPassthroughOpacity = 1.0f
        return true
    }

    private fun initializeUI() {
        // Toolbar action
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this.finish() }
            it.setTitle(R.string.cuj_movable_test)
        }
        findViewById<Button>(R.id.spawn_activity_panel_button).visibility = View.GONE

        // Recreate activity
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@MovableActivity) }
        }
    }

    private fun createStationaryPanel(): PanelEntity {
        @SuppressLint("InflateParams")
        val stationaryPanelContentView = layoutInflater.inflate(R.layout.activity_panel, null)
        stationaryPanelContentView.findViewById<Toolbar>(R.id.activity_panel_tool_bar).also {
            setSupportActionBar(it)
            it.setTitle(R.string.stationary_panel_text)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        val stationaryPanelEntity =
            PanelEntity.create(
                session!!,
                stationaryPanelContentView,
                IntSize2d(640, 550),
                "stationaryPanel",
                Pose(Vector3(0.9f, 0f, 0f)),
            )
        stationaryPanelEntity.parent = session!!.scene.keyEntity
        return stationaryPanelEntity
    }

    private fun updateUiForMovableType() {
        scaleInZSwitch.visibility =
            if (movableType != MovableType.ANCHORABLE) View.VISIBLE else View.GONE
        customBehaviorSection.visibility =
            if (movableType == MovableType.CUSTOM) View.VISIBLE else View.GONE
        anchorableSection.visibility =
            if (movableType == MovableType.ANCHORABLE) View.VISIBLE else View.GONE
    }

    private fun setupMovablePanel(stationaryPanelEntity: Entity) {
        // Create a single panel with text
        @SuppressLint("InflateParams")
        val movablePanelContentView = layoutInflater.inflate(R.layout.panel_movable, null)
        val movablePanelEntity =
            PanelEntity.create(
                session!!,
                movablePanelContentView,
                IntSize2d(750, 1200),
                "panel",
                Pose(Vector3(0f, 0f, 0.1f)),
            )
        movablePanelEntity.parent = session!!.scene.keyEntity
        val enableMovableSwitch =
            movablePanelContentView.findViewById<MaterialSwitch>(R.id.enable_movable_switch)
        val movableOptionsContainer =
            movablePanelContentView.findViewById<LinearLayout>(R.id.movable_options_container)
        enableMovableSwitch.setOnCheckedChangeListener { _, isChecked ->
            movableOptionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                if (movableComponent == null) {
                    replaceMovableComponent(movablePanelEntity)
                }
            } else {
                movableComponent?.let { movablePanelEntity.removeComponent(it) }
                movableComponent = null
            }
            updateUiForMovableType()
        }
        val movableTypeGroup =
            movablePanelContentView.findViewById<RadioGroup>(R.id.movable_type_group)
        movableTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            movableType =
                when (checkedId) {
                    R.id.radio_anchorable -> MovableType.ANCHORABLE
                    R.id.radio_custom -> MovableType.CUSTOM
                    R.id.radio_system -> MovableType.SYSTEM
                    else -> MovableType.ANCHORABLE // Should not happen, but fallback
                }
            updateUiForMovableType()
            replaceMovableComponent(movablePanelEntity)
        }

        scaleInZSwitch =
            movablePanelContentView.findViewById<MaterialSwitch>(R.id.scale_in_z_switch)
        scaleInZSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            scaleInZ = isChecked
            replaceMovableComponent(movablePanelEntity)
        }

        anchorableSection = movablePanelContentView.findViewById(R.id.anchorable_section)
        customBehaviorSection = movablePanelContentView.findViewById(R.id.custom_behavior_section)

        val parentSwitch = movablePanelContentView.findViewById<MaterialSwitch>(R.id.parent_switch)
        parentSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            when (isChecked) {
                true -> movablePanelEntity.parent = stationaryPanelEntity
                false -> movablePanelEntity.parent = session!!.scene.keyEntity
            }
            movablePanelEntity.setPose(Pose(Vector3(0f, 0f, 0.1f)))
        }

        setupAnchorPlacementCheckboxes(movablePanelContentView, movablePanelEntity)

        customBehaviorGroup =
            movablePanelContentView.findViewById<RadioGroup>(R.id.custom_behavior_group)
    }

    private fun setupAnchorPlacementCheckboxes(view: View, movablePanelEntity: Entity) {
        val planeOrientationCheckboxMap =
            mapOf(
                view.findViewById<CheckBox>(R.id.planetype_any_checkbox) to PlaneOrientation.ANY,
                view.findViewById<CheckBox>(R.id.planetype_horizontal_checkbox) to
                    PlaneOrientation.HORIZONTAL,
                view.findViewById<CheckBox>(R.id.planetype_vertical_checkbox) to
                    PlaneOrientation.VERTICAL,
            )
        val planeSemanticCheckboxMap =
            mapOf(
                view.findViewById<CheckBox>(R.id.planesemantic_any_checkbox) to
                    PlaneSemanticType.ANY,
                view.findViewById<CheckBox>(R.id.planesemantic_wall_checkbox) to
                    PlaneSemanticType.WALL,
                view.findViewById<CheckBox>(R.id.planesemantic_ceiling_checkbox) to
                    PlaneSemanticType.CEILING,
                view.findViewById<CheckBox>(R.id.planesemantic_table_checkbox) to
                    PlaneSemanticType.TABLE,
                view.findViewById<CheckBox>(R.id.planesemantic_floor_checkbox) to
                    PlaneSemanticType.FLOOR,
            )

        for ((planeView, planeOrientation) in planeOrientationCheckboxMap) {
            if (planeView.isChecked) {
                planeOrientationFilter.add(planeOrientation)
            }
            planeView.setOnCheckedChangeListener { _, isChecked: Boolean ->
                when (isChecked) {
                    true -> planeOrientationFilter.add(planeOrientation)
                    false -> planeOrientationFilter.remove(planeOrientation)
                }
                replaceMovableComponent(movablePanelEntity)
            }
        }

        for ((planeView, planeSemantic) in planeSemanticCheckboxMap) {
            if (planeView.isChecked) {
                planeSemanticFilter.add(planeSemantic)
            }
            planeView.setOnCheckedChangeListener { _, isChecked: Boolean ->
                when (isChecked) {
                    true -> planeSemanticFilter.add(planeSemantic)
                    false -> planeSemanticFilter.remove(planeSemantic)
                }
                replaceMovableComponent(movablePanelEntity)
            }
        }
    }

    private fun replaceMovableComponent(movablePanelEntity: Entity) {
        movableComponent?.let { movablePanelEntity.removeComponent(it) }

        if (movableType == MovableType.ANCHORABLE) {
            val anchorPlacementSet: MutableSet<AnchorPlacement> = mutableSetOf()
            anchorPlacementSet.add(
                AnchorPlacement.createForPlanes(planeOrientationFilter, planeSemanticFilter)
            )
            movableComponent = MovableComponent.createAnchorable(session!!, anchorPlacementSet)
        } else if (movableType == MovableType.SYSTEM) {
            movableComponent = MovableComponent.createSystemMovable(session!!, scaleInZ)
        } else {
            movableComponent =
                MovableComponent.createCustomMovable(
                    session!!,
                    scaleInZ,
                    executor,
                    object : EntityMoveListener {
                        override fun onMoveUpdate(
                            entity: Entity,
                            currentInputRay: Ray,
                            currentPose: Pose,
                            currentScale: Float,
                        ) {
                            val newPose =
                                when (customBehaviorGroup.checkedRadioButtonId) {
                                    R.id.radio_translation_only ->
                                        Pose(currentPose.translation, entity.getPose().rotation)
                                    R.id.radio_rotation_only ->
                                        Pose(entity.getPose().translation, currentPose.rotation)
                                    else -> currentPose
                                }

                            entity.setPose(newPose)
                            entity.setScale(currentScale)
                        }

                        override fun onMoveEnd(
                            entity: Entity,
                            finalInputRay: Ray,
                            finalPose: Pose,
                            finalScale: Float,
                            updatedParent: Entity?,
                        ) {
                            if (updatedParent != null) {
                                Log.i(TAG, "Panel parent is updated to: $updatedParent")
                            }
                        }
                    },
                )
        }
        movablePanelEntity.addComponent(movableComponent!!)
    }

    @SuppressLint("ExceptionMessage")
    private fun createAnchorableGltfEntity() {
        lifecycleScope.launch {
            val gltfModel =
                GltfModel.create(
                    session = checkNotNull(session),
                    path = Paths.get("models", "Dragon_Evolved.gltf"),
                )

            val anchorPlacementSet: Set<AnchorPlacement> =
                setOf(
                    AnchorPlacement.createForPlanes(
                        anchorablePlaneOrientations = setOf(PlaneOrientation.ANY),
                        anchorablePlaneSemanticTypes = setOf(PlaneSemanticType.ANY),
                    )
                )
            val movableComponent =
                MovableComponent.createAnchorable(
                    session = checkNotNull(session),
                    anchorPlacement = anchorPlacementSet,
                    disposeParentOnReAnchor = true,
                )

            val gltfModelEntity =
                GltfModelEntity.create(
                    session = checkNotNull(session),
                    model = gltfModel,
                    pose = Pose(Vector3(-2f, -1.5f, -2f)),
                )

            gltfModelEntity.setScale(0.5f)
            gltfModelEntity.addComponent(component = movableComponent)
        }
    }
}
