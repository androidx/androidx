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

package androidx.xr.scenecore.samples.transformationtests

import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
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
import androidx.xr.scenecore.samples.commontestview.DebugTextLinearView
import androidx.xr.scenecore.samples.commontestview.DebugTextPanel
import androidx.xr.scenecore.scene
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.lang.UnsupportedOperationException
import java.nio.file.Paths
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TransformationTestsActivity : AppCompatActivity() {

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    private var anchor: AnchorEntity? = null
    private lateinit var sunDragon: GltfModelEntity
    private lateinit var planetDragon: GltfModelEntity
    private lateinit var moonDragon: GltfModelEntity
    private var moveableActive = false
    private var debugTextPanelsToUpdate = mutableListOf<DebugTextPanel>()
    private val pauseSwitch by lazy { findViewById<Switch>(R.id.switchPause) }
    private val mainActivityDebugView by lazy {
        findViewById<DebugTextLinearView>(R.id.mainDebugTextPanel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transformationtests_activity)
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        setupMovableMainPanel()

        // Create a transform widget model and assign it to an Anchor
        lifecycleScope.launch {
            val axesModel = GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
            setupAnchorAndDebugPanelUi(axesModel)
        }

        // Create multiple orbiting dragon models
        lifecycleScope.launch {
            val dragonModel = GltfModel.create(session, Paths.get("models", "Dragon_Evolved.gltf"))
            createModelSolarSystem(session, dragonModel)
        }
    }

    // Called once the transformWidgetModel is ready
    private fun setupAnchorAndDebugPanelUi(anchorModel: GltfModel) {
        val anchoredTransformWidgetEntity =
            GltfModelEntity.create(session, anchorModel, Pose.Identity)

        anchor =
            AnchorEntity.create(
                session,
                FloatSize2d(0.1f, 0.1f),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
            )
        anchor!!.addChild(anchoredTransformWidgetEntity)
        anchoredTransformWidgetEntity.setPose(Pose.Identity)

        // Create debug panels for the activitySpace and the Anchor
        val activitySpaceDebugPanel =
            createDebugPanelAndLabel("ActivitySpace", session.scene.activitySpace)
        val anchorDebugPanel = createDebugPanelAndLabel("Anchor", anchor!!)

        // Set callbacks for the Activity Space and anchor's underlying space updating
        onActivitySpaceUpdatedCount = 0
        // Print it once, since we probably have missed the first update already
        activitySpaceDebugPanel.view.setLine(
            "onActivitySpaceUpdatedCount",
            (++onActivitySpaceUpdatedCount).toString(),
        )
        session.scene.activitySpace.addOnSpaceUpdatedListener {
            // Use lifecycleScope to update the UI view in the same thread it was created in
            lifecycleScope.launch {
                activitySpaceDebugPanel.view.setLine(
                    "onActivitySpaceUpdatedCount",
                    (++onActivitySpaceUpdatedCount).toString(),
                )
            }
        }
        onAnchorSpaceUpdatedCount = 0
        anchor!!.setOnSpaceUpdatedListener({
            // Use lifecycleScope to update the UI view in the same thread it was created in
            lifecycleScope.launch {
                anchorDebugPanel.view.setLine(
                    "onAnchorSpaceUpdatedCount",
                    (++onAnchorSpaceUpdatedCount).toString(),
                )
            }
        })

        // Update the debug text panels every few milliseconds
        lifecycleScope.launch {
            while (true) {
                delay(16L)
                val anchorState = anchor!!.state
                for (panel in debugTextPanelsToUpdate) {
                    // If the anchor is not anchored, then skip updating its panel
                    if (panel == anchorDebugPanel) {
                        anchorDebugPanel.view.setLine(
                            "Anchor State",
                            anchorStateToString(anchorState),
                        )
                        if (anchorState != AnchorEntity.State.ANCHORED) {
                            continue
                        }
                    }
                    updateDebugTextPanel(panel.view, panel.trackedEntity!!, anchorState)
                }
                // Also update the main activity panel
                updateDebugTextPanel(
                    mainActivityDebugView,
                    session.scene.mainPanelEntity,
                    anchorState,
                )
            }
        }
    }

    private fun setupMovableMainPanel() {
        mainActivityDebugView.setName("Main Panel")
        val movableComponent = MovableComponent.createSystemMovable(session, scaleInZ = false)
        val movablePanelSwitch = findViewById<Switch>(R.id.switchMovePanel)
        movablePanelSwitch.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> {
                    moveableActive = session.scene.mainPanelEntity.addComponent(movableComponent)
                }
                false ->
                    moveableActive.let {
                        session.scene.mainPanelEntity.removeComponent(movableComponent)
                    }
            }
        }
    }

    /**
     * Creates a debug panel to be updated with pose info and a label to follow the tracked entity.
     */
    @CanIgnoreReturnValue
    @Suppress("UNUSED_VARIABLE")
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
                session,
                session.scene.activitySpace,
                name = name,
                pose = panelPose,
            )
        debugPanel.trackedEntity = trackedEntity
        debugTextPanelsToUpdate.add(debugPanel)

        // Create a label to follow the tracked entity
        val unused =
            DebugTextPanel(
                this,
                session,
                trackedEntity,
                name = name,
                pixelDimensions =
                    IntSize2d(
                        (labelDimensions.width * trackedEntity.getScale(Space.REAL_WORLD)).toInt(),
                        (labelDimensions.height * trackedEntity.getScale(Space.REAL_WORLD)).toInt(),
                    ),
            )
        return debugPanel
    }

    /** Updates the debug text panel with pose info on the tracked entity. */
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
                "getPose is not enabled on this Entity"
            }
        view.setLine("localPose", localPose)

        view.setLine("worldSpacePose", trackedEntity.activitySpacePose.toFormattedString())
        view.setLine("worldSpaceScale", trackedEntity.getScale(Space.REAL_WORLD).toString())
        if (
            trackedEntity == sunDragon ||
                trackedEntity == planetDragon ||
                trackedEntity == moonDragon
        ) {
            view.setLine("local scale", trackedEntity.getScale(Space.PARENT).toString())
        }

        val activitySpacePose =
            trackedEntity.transformPoseTo(Pose.Identity, session.scene.activitySpace)
        view.setLine("ActivitySpacePose", activitySpacePose.toFormattedString())

        val mainPanelSpacePose =
            trackedEntity.transformPoseTo(Pose.Identity, session.scene.mainPanelEntity)
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

    private fun createModelSolarSystem(session: Session, model: GltfModel) {
        sunDragon = GltfModelEntity.create(session, model, Pose(Vector3(-0.5f, 3f, -9f)))
        sunDragon.setScale(3f)
        sunDragon.parent = session.scene.activitySpace

        planetDragon = GltfModelEntity.create(session, model, Pose(Vector3(-1f, 3f, -9f)))
        planetDragon.setScale(0.5f)
        planetDragon.parent = sunDragon

        moonDragon = GltfModelEntity.create(session, model, Pose(Vector3(-1.5f, 3f, -9f)))
        moonDragon.setScale(0.5f)
        moonDragon.parent = planetDragon

        // Create debug panels for the sun, planet, and moon
        val largeLabelDimensions = FloatSize3d(700f, 200f)
        createDebugPanelAndLabel("sunDragon", sunDragon, largeLabelDimensions)
        createDebugPanelAndLabel("planetDragon", planetDragon, largeLabelDimensions)
        createDebugPanelAndLabel("moonDragon", moonDragon, largeLabelDimensions)

        orbitModelAroundParent(planetDragon, 4f, 0f, 20000f)
        orbitModelAroundParent(moonDragon, 2f, 1.67f, 5000f)
    }

    // TODO: b/339450306 - Simply update parent's rotation once math library is added to jxrCore
    @CanIgnoreReturnValue
    private fun orbitModelAroundParent(
        modelEntity: GltfModelEntity,
        radius: Float,
        startAngle: Float,
        rotateTimeMs: Float,
    ) =
        lifecycleScope.launch {
            val pi = 3.14159F
            val timeSource = TimeSource.Monotonic
            val startTime = timeSource.markNow()

            while (true) {
                while (pauseSwitch.isChecked) {
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
