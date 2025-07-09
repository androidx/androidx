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

package androidx.xr.arcore.apps.whitebox.helloar.rendering

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.AugmentedObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import kotlin.enums.enumEntries
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AugmentedObjectRenderer(val session: Session, val coroutineScope: CoroutineScope) :
    DefaultLifecycleObserver {
    private val _renderedObjects: MutableStateFlow<List<AugmentedObjectModel>> =
        MutableStateFlow(mutableListOf<AugmentedObjectModel>())

    internal val renderedObjects: StateFlow<Collection<AugmentedObjectModel>> =
        _renderedObjects.asStateFlow()

    private lateinit var updateJob: CompletableJob

    override fun onResume(owner: LifecycleOwner) {
        updateJob =
            SupervisorJob(
                coroutineScope.launch {
                    AugmentedObject.subscribe(session).collect { updateObjectModels(it) }
                }
            )
    }

    override fun onPause(owner: LifecycleOwner) {
        updateJob.complete()
        _renderedObjects.value = emptyList<AugmentedObjectModel>()
    }

    private fun updateObjectModels(objects: Collection<AugmentedObject>) {
        val objectsToRender = _renderedObjects.value.toMutableList()
        // create renderers for new objects
        for (obj in objects) {
            if (_renderedObjects.value.none { it.id == obj.hashCode() }) {
                addObjectModel(obj, objectsToRender)
            }
        }

        // stop rendering dropped objects
        for (renderedObject in objectsToRender) {
            if (objects.none { it.hashCode() == renderedObject.id }) {
                removeObjectModel(renderedObject, objectsToRender)
            }
        }

        // emit to notify collectors that the collection has been updated.
        _renderedObjects.value = objectsToRender
    }

    private fun addObjectModel(
        obj: AugmentedObject,
        objectsToRender: MutableList<AugmentedObjectModel>,
    ) {
        val entity = CubeEntity(session, obj.state.value.centerPose, obj.state.value.extents)
        // The counter starts at max to trigger the resize on the first update loop since emulators
        // only
        // update their static planes once.
        var counter = PANEL_RESIZE_UPDATE_COUNT
        // Make the render job a child of the update job so it completes when the parent completes.
        val renderJob =
            coroutineScope.launch(updateJob) {
                obj.state.collect { state ->
                    when (state.trackingState) {
                        TrackingState.TRACKING -> {
                            if (state.category == AugmentedObjectCategory.UNKNOWN) {
                                entity.setEnabled(false)
                            } else {
                                entity.setEnabled(true)
                                entity.setAlpha(
                                    TRACKED_ALPHA_VALUES[state.category.toString()]
                                        ?: TRACKED_ALPHA_DEFAULT
                                )
                                counter++
                                val newPose =
                                    session.scene.perceptionSpace.transformPoseTo(
                                        state.centerPose,
                                        session.scene.activitySpace,
                                    )
                                entity.update(newPose, state)

                                if (counter > PANEL_RESIZE_UPDATE_COUNT) {
                                    entity.resize(state.extents)
                                    counter = 0
                                }
                            }
                        }
                        TrackingState.PAUSED -> entity.setAlpha(PAUSED_ALPHA)
                        TrackingState.STOPPED -> entity.setEnabled(false)
                    }
                }
            }

        objectsToRender.add(
            AugmentedObjectModel(
                obj.hashCode(),
                obj.state.value.category,
                obj.state,
                entity,
                renderJob,
            )
        )
    }

    private fun removeObjectModel(
        objectModel: AugmentedObjectModel,
        objsToRender: MutableList<AugmentedObjectModel>,
    ) {
        objectModel.renderJob?.cancel()
        objectModel.cubeEntity.dispose()
        objsToRender.remove(objectModel)
    }

    private companion object {
        private val PX_PER_METER = Resources.getSystem().displayMetrics.density * 1111.11f
        private const val PANEL_RESIZE_UPDATE_COUNT = 50

        @SuppressLint("PrimitiveInCollection")
        private val TRACKED_ALPHA_VALUES =
            mapOf("Keyboard" to .05f, "Mouse" to .25f, "Laptop" to .1f)
        private const val TRACKED_ALPHA_DEFAULT = .5f
        private const val PAUSED_ALPHA = 0.25f

        // TODO: b/428037016 - remove this scaling factor once this bug is fixed
        private const val PANEL_SCALING_FACTOR = 1f / 1.7f
    }

    internal class CubeEntity(
        private val session: Session,
        public var centerPose: Pose,
        public var extents: Vector3,
    ) {
        enum class Face {
            PosX,
            PosY,
            PosZ,
            NegX,
            NegY,
            NegZ;

            override fun toString(): String {
                return when (this) {
                    PosX -> "+X"
                    PosY -> "+Y"
                    PosZ -> "+Z"
                    NegX -> "-X"
                    NegY -> "-Y"
                    NegZ -> "-Z"
                }
            }
        }

        private val _views: Array<TextView> =
            Array(6) { createFaceView(enumEntries<Face>()[it], session.activity) }

        private val _faces: Array<PanelEntity> =
            Array(6) { createFacePanel(enumEntries<Face>()[it]) }

        public fun dispose() {
            for (face in enumEntries<Face>()) {
                _faces[face.ordinal].dispose()
            }
        }

        public fun resize(newExtents: Vector3) {
            extents = newExtents
            for (face in enumEntries<Face>()) {
                _faces[face.ordinal].size = dimensionsForFace(face)
            }
        }

        public fun setAlpha(alpha: Float) {
            for (face in enumEntries<Face>()) {
                _faces[face.ordinal].setAlpha(alpha)
            }
        }

        public fun setEnabled(enabled: Boolean) {
            for (face in enumEntries<Face>()) {
                _faces[face.ordinal].setEnabled(enabled)
            }
        }

        public fun update(newPose: Pose, state: AugmentedObject.State) {
            centerPose = newPose
            for (face in enumEntries<Face>()) {
                _views[face.ordinal].setBackgroundColor(
                    convertObjectCategoryToColor(state.category)
                )
                _faces[face.ordinal].setPose(poseForFace(face))
            }
        }

        private fun convertObjectCategoryToColor(category: AugmentedObjectCategory): Int =
            when (category) {
                AugmentedObjectCategory.KEYBOARD -> Color.GREEN
                AugmentedObjectCategory.MOUSE -> Color.BLUE
                AugmentedObjectCategory.LAPTOP -> Color.YELLOW
                // Planes with Unknown Label are currently not rendered.
                else -> Color.RED
            }

        private fun convertMetersToPixels(input: Vector3): Vector3 = input * PX_PER_METER

        private fun createFaceView(face: Face, activity: Activity): TextView {
            val view = TextView(activity.applicationContext)
            view.text = "$face"
            view.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            view.setBackgroundColor(Color.WHITE)
            return view
        }

        private fun createFacePanel(face: Face): PanelEntity {
            val view = createFaceView(face, session.activity)
            return PanelEntity.create(
                session,
                _views[face.ordinal],
                dimensionsForFace(face),
                "$face",
                poseForFace(face),
            )
        }

        private fun dimensionsForFace(face: Face): FloatSize2d {
            val scaledExtents = extents * PANEL_SCALING_FACTOR
            return when (face) {
                Face.PosX,
                Face.NegX -> {
                    FloatSize2d(scaledExtents.z, scaledExtents.y)
                }
                Face.PosY,
                Face.NegY -> {
                    FloatSize2d(scaledExtents.x, scaledExtents.z)
                }
                Face.PosZ,
                Face.NegZ -> {
                    FloatSize2d(scaledExtents.x, scaledExtents.y)
                }
            }
        }

        private fun poseForFace(face: Face): Pose {
            val scaledExtents = (extents * PANEL_SCALING_FACTOR) / 2f
            return when (face) {
                Face.PosX -> {
                    centerPose.compose(
                        Pose(
                            Vector3(scaledExtents.x, 0f, 0f),
                            Quaternion.fromEulerAngles(0f, 90f, 0f),
                        )
                    )
                }
                Face.PosY -> {
                    centerPose.compose(
                        Pose(
                            Vector3(0f, scaledExtents.y, 0f),
                            Quaternion.fromEulerAngles(-90f, 0f, 0f),
                        )
                    )
                }
                Face.PosZ -> {
                    centerPose.compose(Pose(Vector3(0f, 0f, scaledExtents.z), Quaternion.Identity))
                }
                Face.NegX -> {
                    centerPose.compose(
                        Pose(
                            Vector3(-scaledExtents.x, 0f, 0f),
                            Quaternion.fromEulerAngles(0f, -90f, 0f),
                        )
                    )
                }
                Face.NegY -> {
                    centerPose.compose(
                        Pose(
                            Vector3(0f, -scaledExtents.y, 0f),
                            Quaternion.fromEulerAngles(90f, 0f, 0f),
                        )
                    )
                }
                Face.NegZ -> {
                    centerPose.compose(
                        Pose(
                            Vector3(0f, 0f, -scaledExtents.z),
                            Quaternion.fromEulerAngles(0f, 180f, 0f),
                        )
                    )
                }
            }
        }
    }
}
