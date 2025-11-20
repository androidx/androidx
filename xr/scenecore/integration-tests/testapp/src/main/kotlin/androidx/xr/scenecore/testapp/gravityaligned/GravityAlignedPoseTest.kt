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

package androidx.xr.scenecore.testapp.gravityaligned

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import kotlinx.coroutines.flow.MutableStateFlow

class GravityAlignedPoseTest : AppCompatActivity() {
    companion object {
        private const val TAG = "GravityAlignedPoseTest"
    }

    private val moveListener =
        object : EntityMoveListener {
            override fun onMoveUpdate(
                entity: Entity,
                currentInputRay: Ray,
                currentPose: Pose,
                currentScale: Float,
            ) {
                updatePoseAndScale(entity, currentPose, currentScale)
            }

            override fun onMoveEnd(
                entity: Entity,
                finalInputRay: Ray,
                finalPose: Pose,
                finalScale: Float,
                updatedParent: Entity?,
            ) {
                Log.i(TAG, "$entity $finalInputRay $finalPose $finalScale $updatedParent")
                updatePoseAndScale(entity, finalPose, finalScale)
            }
        }

    private var session: Session? = null
    private val _surfaceEntityFlow = MutableStateFlow<SurfaceEntity?>(null)
    var surfaceEntity: SurfaceEntity?
        get() = _surfaceEntityFlow.value
        set(value) {
            _surfaceEntityFlow.value = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gravity_aligned_pose_test)

        session = createSession(this)
        if (session == null) this.finish()
        session!!.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))

        // Toolbar action
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this.finish() }
        }

        val parentPxSize = IntSize2d(720, 1000)

        val parentPanelView = layoutInflater.inflate(R.layout.gravity_aligned_parent_panel, null)
        val parentPanelPose =
            Pose(Vector3(-0.1f, 0f, 0.05f), Quaternion.fromAxisAngle(Vector3.Forward, 15f))
        val parentPanelEntity =
            PanelEntity.create(session!!, parentPanelView, parentPxSize, "Parent Panel")
        parentPanelEntity.setPose(parentPanelPose, Space.PARENT)

        val movablePanelComponent =
            MovableComponent.createCustomMovable(session!!, true, mainExecutor, moveListener)
        parentPanelEntity.addComponent(movablePanelComponent)

        // Panel pose relative to PARENT.
        val childPanelPoseParent =
            Pose(Vector3(0.4f, 0f, 0.1f), Quaternion.fromAxisAngle(Vector3.Forward, 15f))
        // Panel pose relative to ACTIVITY.
        val childPanelPoseActivity =
            Pose(Vector3(0.4f, 0f, 0.1f), Quaternion.fromAxisAngle(Vector3.Forward, 15f))
        // Panel pose relative to REAL_WORLD.
        var childPanelPoseWorld: Pose? = null

        val childPanel = createChildPanel(session!!, parentPanelEntity, childPanelPoseParent)

        parentPanelView.findViewById<Button>(R.id.default_pose_button).setOnClickListener {
            // Get the initial pose of childPanel in REAL_WORLD space.
            childPanelPoseWorld = getPanelInitialPoseInRealWorld(childPanel, childPanelPoseWorld)

            childPanel.setPose(childPanelPoseParent, Space.PARENT)
        }

        parentPanelView.findViewById<Button>(R.id.gravity_aligned_pose_button).setOnClickListener {
            // Get the initial pose of childPanel in REAL_WORLD space.
            childPanelPoseWorld = getPanelInitialPoseInRealWorld(childPanel, childPanelPoseWorld)

            val childPanelGravityAlignedPoseParent =
                childPanel.getGravityAlignedPose(childPanelPoseParent)

            childPanel.setPose(childPanelGravityAlignedPoseParent)
        }

        parentPanelView.findViewById<Button>(R.id.default_pose_button_activity).setOnClickListener {
            // Get the initial pose of childPanel in REAL_WORLD space.
            childPanelPoseWorld = getPanelInitialPoseInRealWorld(childPanel, childPanelPoseWorld)

            childPanel.setPose(childPanelPoseActivity, Space.ACTIVITY)
        }

        parentPanelView
            .findViewById<Button>(R.id.gravity_aligned_pose_button_activity)
            .setOnClickListener {
                // Get the initial pose of childPanel in REAL_WORLD space.
                childPanelPoseWorld =
                    getPanelInitialPoseInRealWorld(childPanel, childPanelPoseWorld)

                val parentPoseInActivitySpace = childPanel.parent!!.getPose(Space.ACTIVITY)
                val inverseParentPose = parentPoseInActivitySpace.inverse
                val childPanelPoseParent = inverseParentPose.compose(childPanelPoseActivity)
                val childPanelGravityAlignedPoseParent =
                    childPanel.getGravityAlignedPose(childPanelPoseParent)

                childPanel.setPose(childPanelGravityAlignedPoseParent)
            }

        parentPanelView.findViewById<Button>(R.id.default_pose_button_world).setOnClickListener {
            // Get the initial pose of childPanel in REAL_WORLD space.
            childPanelPoseWorld = getPanelInitialPoseInRealWorld(childPanel, childPanelPoseWorld)

            childPanel.setPose(childPanelPoseWorld!!, Space.REAL_WORLD)
        }

        parentPanelView
            .findViewById<Button>(R.id.gravity_aligned_pose_button_world)
            .setOnClickListener {
                // Get the initial pose of childPanel in REAL_WORLD space.
                childPanelPoseWorld =
                    getPanelInitialPoseInRealWorld(childPanel, childPanelPoseWorld)

                childPanel.setPose(childPanelPoseWorld, Space.REAL_WORLD)
                val childPanelPose = childPanel.getPose()
                val childPanelGravityAlignedPose = childPanel.getGravityAlignedPose(childPanelPose)
                childPanel.setPose(childPanelGravityAlignedPose)
            }

        parentPanelView.findViewById<Button>(R.id.create_surface_entity_button).setOnClickListener {
            session!!
                .scene
                .spatialUser
                .head
                ?.transformPoseTo(Pose(Vector3(0f, 0f, -1f)), session!!.scene.activitySpace)
                .let {
                    if (surfaceEntity == null) {
                        surfaceEntity =
                            SurfaceEntity.create(
                                session = session!!,
                                stereoMode = SurfaceEntity.StereoMode.MONO,
                                pose = it!!,
                                shape = SurfaceEntity.Shape.Quad(FloatSize2d(0.5f, 0.5f)),
                            )
                        val gravityAlignedSurfacePose = surfaceEntity!!.getGravityAlignedPose(it)
                        surfaceEntity!!.setPose(gravityAlignedSurfacePose)
                    }
                }
        }

        parentPanelView
            .findViewById<Button>(R.id.destroy_surface_entity_button)
            .setOnClickListener {
                surfaceEntity?.dispose()
                surfaceEntity = null
            }
    }

    private fun getPanelInitialPoseInRealWorld(panel: PanelEntity, pose: Pose?): Pose {
        return pose ?: panel.getPose(Space.REAL_WORLD)
    }

    private fun createChildPanel(
        session: Session,
        parentPanelEntity: PanelEntity,
        pose: Pose,
    ): PanelEntity {
        val childPanelView = layoutInflater.inflate(R.layout.gravity_aligned_child_panel, null)
        val childPanelEntity =
            PanelEntity.create(session, childPanelView, IntSize2d(360, 240), name = "Child Panel")
        childPanelEntity.parent = parentPanelEntity

        childPanelEntity.setPose(pose)

        return childPanelEntity
    }

    private fun updatePoseAndScale(entity: Entity, pose: Pose, scale: Float) {
        entity.setPose(pose)
        entity.setScale(scale)
    }
}
