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

package androidx.xr.scenecore.testapp.transformation

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.DebugTextLinearView
import androidx.xr.scenecore.testapp.common.DebugTextPanel
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.nio.file.Paths
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class TransformationActivity : AppCompatActivity() {
    private var session: Session? = null
    private var pauseAnimation = MutableStateFlow(false)
    private var movableActive = MutableStateFlow(false)
    private lateinit var solarSystemEntityModel: GltfModel
    private lateinit var staticEntityModel: GltfModel
    private var anchor: AnchorEntity? = null
    private lateinit var sunEntity: GltfModelEntity
    private lateinit var planetEntity: GltfModelEntity
    private lateinit var moonEntity: GltfModelEntity
    private lateinit var anchorDebugPanel: DebugTextPanel
    private lateinit var activitySpaceDebugPanel: DebugTextPanel
    private val mainActivityDebugView by lazy {
        findViewById<DebugTextLinearView>(R.id.mainDebugTextPanel).also { it.setName("Main Panel") }
    }
    private var debugTextPanelsToUpdate = mutableListOf<DebugTextPanel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.transformation_activity_panel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Create session
        session = createSession(this)
        session!!.configure(
            Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
        )

        // toolbar
        findViewById<Toolbar>(R.id.topAppBar).also { toolbar ->
            setSupportActionBar(toolbar)
            toolbar.setNavigationOnClickListener { this@TransformationActivity.finish() }
            toolbar.setTitle(R.string.cuj_transformation_test)
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@TransformationActivity) }
        }

        // handle switches
        findViewById<Switch>(R.id.switch_pause_animation).setOnCheckedChangeListener { _, isOn ->
            pauseAnimation.value = isOn
        }
        findViewById<Switch>(R.id.switch_allow_panel_movement).setOnCheckedChangeListener { _, isOn
            ->
            switchMainPanelMovement(isOn)
        }

        lifecycleScope.launch {
            // Entity solar system
            loadModels()
            entitySolarSystem()

            // Anchor
            createAnchor()

            // Activity space debug panel
            createActivitySpaceDebugPanel()

            while (true) {

                // Update debug panels
                val anchorState = anchor!!.state
                for (panel in debugTextPanelsToUpdate) {
                    if (panel == anchorDebugPanel) {
                        anchorDebugPanel.view.setLine(
                            "Anchor State",
                            anchorStateToString(anchorState),
                        )
                        if (anchorState != AnchorEntity.State.ANCHORED) {
                            continue
                        }
                    }
                    updateDebugTextPanel(
                        panel.view,
                        panel.trackedEntity!!,
                        AnchorEntity.State.ANCHORED,
                    )
                }
                // Update main panel debug data
                updateDebugTextPanel(
                    mainActivityDebugView,
                    session!!.scene.mainPanelEntity,
                    anchorState,
                )

                delay(100L)
            }
        }
    }

    private fun createAnchor() {
        anchor =
            AnchorEntity.create(
                session!!,
                FloatSize2d(0.1f, 0.1f),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
            )
        GltfModelEntity.create(session!!, staticEntityModel, Pose.Identity).also {
            it.setScale(1f)
            anchor!!.addChild(it)
        }
        anchorDebugPanel =
            createDebugPanelAndLabel("Anchor", anchor!!).also {
                it.view.setLine(
                    "onAnchorSpaceUpdatedCount",
                    (++onAnchorSpaceUpdatedCount).toString(),
                )
                anchor!!.setOnSpaceUpdatedListener({
                    it.view.setLine(
                        "onAnchorSpaceUpdatedCount",
                        (++onAnchorSpaceUpdatedCount).toString(),
                    )
                })
            }
    }

    private fun createActivitySpaceDebugPanel() {
        activitySpaceDebugPanel =
            createDebugPanelAndLabel("ActivitySpace", session!!.scene.activitySpace).also {
                it.view.setLine(
                    "onActivitySpaceUpdatedCount",
                    (++onActivitySpaceUpdatedCount).toString(),
                )
                session!!.scene.activitySpace.addOnSpaceUpdatedListener {
                    it.view.setLine(
                        "onActivitySpaceUpdatedCount",
                        (++onActivitySpaceUpdatedCount).toString(),
                    )
                }
            }
    }

    private fun updateDebugTextPanel(
        view: DebugTextLinearView,
        trackedEntity: Entity,
        anchorState: Int,
    ) {
        // Need to handle unsupported exception from the anchorEntity's getPose
        val localPose =
            try {
                trackedEntity.getPose().toFormattedString()
            } catch (e: UnsupportedOperationException) {
                "getPose is not enabled on this Entity ${e.cause}"
            }
        view.setLine("localPose", localPose)

        view.setLine("worldSpacePose", trackedEntity.activitySpacePose.toFormattedString())
        view.setLine("worldSpaceScale", trackedEntity.getScale().toString())
        if (
            trackedEntity == sunEntity ||
                trackedEntity == planetEntity ||
                trackedEntity == moonEntity
        ) {
            view.setLine("local scale", trackedEntity.getScale(Space.PARENT).toString())
        }

        val activitySpacePose =
            trackedEntity.transformPoseTo(Pose.Identity, session!!.scene.activitySpace)
        view.setLine("ActivitySpacePose", activitySpacePose.toFormattedString())

        val mainPanelSpacePose =
            trackedEntity.transformPoseTo(Pose.Identity, session!!.scene.mainPanelEntity)
        view.setLine("MainPanelSpacePose", mainPanelSpacePose.toFormattedString())

        // Pose in Anchor Space is only retrieved if anchor is anchored
        if (anchorState != AnchorEntity.State.ANCHORED) {
            view.setLine("AnchorSpacePose", "N/A")
            view.setLine("Distance to Anchor", "N/A")
        } else {
            val anchorSpacePose = trackedEntity.transformPoseTo(Pose.Identity, anchor!!)
            view.setLine("AnchorSpacePose", anchorSpacePose.toFormattedString())
            view.setLine("Distance to Anchor", length(anchorSpacePose.translation).toString())
        }

        view.setLine("Distance to ActivitySpace", length(activitySpacePose.translation).toString())
        view.setLine("Distance to Main Panel", length(mainPanelSpacePose.translation).toString())
        when (trackedEntity) {
            is PanelEntity -> {
                view.setLine("Panel size", trackedEntity.size.toString())
                view.setLine("Panel scale", trackedEntity.getScale().toString())
            }
        }
    }

    private fun switchMainPanelMovement(switchState: Boolean) {
        val movableComponent = MovableComponent.createSystemMovable(session!!, scaleInZ = false)
        if (switchState) {
            movableActive.value = session!!.scene.mainPanelEntity.addComponent(movableComponent)
        } else {
            movableActive.let { session!!.scene.mainPanelEntity.removeAllComponents() }
        }
    }

    private suspend fun loadModels() {
        solarSystemEntityModel =
            GltfModel.create(session!!, Paths.get("models", "Dragon_Evolved.gltf"))
        staticEntityModel = GltfModel.create(session!!, Paths.get("models", "xyzArrows.glb"))
    }

    private fun entitySolarSystem() {
        sunEntity =
            GltfModelEntity.create(session!!, solarSystemEntityModel, Pose.Identity).also {
                it.setScale(3f)
                it.setPose(Pose(Vector3(-0.5f, 3f, -9f)))
                it.parent = session!!.scene.activitySpace
            }
        planetEntity =
            GltfModelEntity.create(session!!, solarSystemEntityModel, Pose.Identity).also {
                it.setScale(0.5f)
                it.setPose(Pose(Vector3(-1f, 3f, -9f)))
                it.parent = sunEntity
            }
        moonEntity =
            GltfModelEntity.create(session!!, solarSystemEntityModel, Pose.Identity).also {
                it.setScale(0.5f)
                it.setPose(Pose(Vector3(-1.5f, 3f, -9f)))
                it.parent = planetEntity
            }
        orbitModelAroundParent(planetEntity, 4f, 0f, 20000f)
        orbitModelAroundParent(moonEntity, 2f, 1.67f, 5000f)

        val largeLabelDimensions = FloatSize3d(700f, 200f)
        createDebugPanelAndLabel("sunEntity", sunEntity, largeLabelDimensions)
        createDebugPanelAndLabel("planetEntity", planetEntity, largeLabelDimensions)
        createDebugPanelAndLabel("moonEntity", moonEntity, largeLabelDimensions)
    }

    private fun orbitModelAroundParent(
        modelEntity: GltfModelEntity,
        radius: Float,
        startAngle: Float,
        rotateTimeMs: Float,
    ) {
        lifecycleScope.launch {
            val pi = 3.14159F
            val timeSource = TimeSource.Monotonic
            val startTime = timeSource.markNow()

            while (true) {
                while (pauseAnimation.value) {
                    delay(16L)
                }
                delay(16L)
                val deltaAngle =
                    (2 * pi) * ((timeSource.markNow() - startTime).inWholeMilliseconds) /
                        rotateTimeMs

                val angle = startAngle + deltaAngle
                val pos = Vector3(radius * cos(angle), 0F, radius * sin(angle))
                modelEntity.setPose(Pose(pos, Quaternion.Identity))
            }
        }
    }

    private fun createDebugPanelAndLabel(
        name: String,
        trackedEntity: Entity,
        labelDimensions: FloatSize3d = FloatSize3d(140f, 50f),
    ): DebugTextPanel {
        // Set position of the panel to be next to other panels created previously
        val panelPose = Pose(Vector3(-1.0f + debugTextPanelsToUpdate.size * 0.6f, -0.4f, 0.1f))

        // Create the debug panel with info on the tracked entity
        val debugPanel =
            DebugTextPanel(
                this,
                session!!,
                session!!.scene.activitySpace,
                name = name,
                pose = panelPose,
            )
        debugPanel.trackedEntity = trackedEntity
        debugTextPanelsToUpdate.add(debugPanel)

        // Create a label to follow the tracked entity
        val unused =
            DebugTextPanel(
                this,
                session!!,
                trackedEntity,
                pixelDimensions =
                    IntSize2d(
                        (labelDimensions.width * trackedEntity.getScale(Space.REAL_WORLD)).toInt(),
                        (labelDimensions.height * trackedEntity.getScale(Space.REAL_WORLD)).toInt(),
                    ),
                name = name,
            )
        return debugPanel
    }

    private fun Pose.toFormattedString(): String {
        val position =
            "Vector3 [%f, %f, %f]"
                .format(this.translation.x, this.translation.y, this.translation.z)
        val rotation =
            "Rotation [%f, %f, %f, %f]"
                .format(this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w)
        return "$position, $rotation"
    }

    private fun length(position: Vector3): Float {
        return sqrt(position.x * position.x + position.y * position.y + position.z * position.z)
    }

    companion object {
        var onActivitySpaceUpdatedCount = 0
        var onAnchorSpaceUpdatedCount = 0
    }

    private fun anchorStateToString(state: Int): String {
        return when (state) {
            0 -> "ANCHORED"
            1 -> "UNANCHORED"
            2 -> "TIMEOUT"
            3 -> "ERROR"
            else -> "Unknown"
        }
    }
}
