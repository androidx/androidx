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

package androidx.xr.arcore.testapp.helloar.rendering

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.xr.arcore.AugmentedObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AugmentedObjectRenderer {
    private val _cubeModelMap = Collections.synchronizedMap(HashMap<String, GltfModel?>())
    private val _renderedObjects: MutableStateFlow<List<AugmentedObject>> =
        MutableStateFlow(mutableListOf<AugmentedObject>())
    private val _runningJobs = Collections.synchronizedMap(HashMap<AugmentedObject, Job>())

    private lateinit var _coroutineScope: CoroutineScope
    private lateinit var _session: Session
    private lateinit var _supervisorJob: Job

    val renderedObjects: StateFlow<Collection<AugmentedObject>> = _renderedObjects.asStateFlow()

    fun startRendering(session: Session, coroutineScope: CoroutineScope) {
        _session = session

        _supervisorJob = SupervisorJob()
        _coroutineScope = CoroutineScope(coroutineScope.coroutineContext + _supervisorJob)

        _coroutineScope.launch {
            XrLog.debug { "listening to AugmentedObject flow" }
            AugmentedObject.subscribe(_session).collect { updateObjectModels(it) }
        }
    }

    fun stopRendering() {
        check(::_session.isInitialized) { "_session is not initialized" }
        check(::_supervisorJob.isInitialized) { "_supervisorJob is not initialized" }

        // cancel all rendering jobs
        _runningJobs.clear()
        _supervisorJob.cancel()

        // emit an empty list to and flow subscribers
        _renderedObjects.value = emptyList()
    }

    private fun addObjectModel(
        obj: AugmentedObject,
        objectsToRender: MutableList<AugmentedObject>,
    ) {
        // Make the render job a child of the update job so it completes when the parent completes.
        _coroutineScope.launch { updateAndRenderObject(obj) }

        objectsToRender.add(obj)
    }

    private suspend fun loadModelForCategory(category: AugmentedObjectCategory): GltfModelEntity {
        val key = category.toString()

        val asset =
            if (SUPPORTED_OBJECT_MODELS.containsKey(key)) {
                SUPPORTED_OBJECT_MODELS[key]
            } else {
                DEFAULT_OBJECT_MODEL
            }

        if (!_cubeModelMap.containsKey(key)) {
            _cubeModelMap[key] = GltfModel.create(_session, Paths.get("models", asset))
        }

        val entity = GltfModelEntity.create(_session, _cubeModelMap[key]!!)
        entity.setEnabled(false)
        return entity
    }

    private fun scaledExtents(extents: FloatSize3d): Vector3 {
        return Vector3(
            extents.width * MODEL_SCALING_FACTOR,
            extents.height * MODEL_SCALING_FACTOR,
            extents.depth * MODEL_SCALING_FACTOR,
        )
    }

    private suspend fun updateAndRenderObject(augmentedObject: AugmentedObject) {
        var currentCategory = augmentedObject.state.value.category
        var modelEntity = loadModelForCategory(currentCategory)
        // The counter starts at max to trigger the resize on the first update loop.
        var counter = PANEL_RESIZE_UPDATE_COUNT
        try {
            XrLog.debug { "Starting render job for $augmentedObject ($currentCategory)" }
            augmentedObject.state.collect { state ->
                // update the model entity based on the current tracking state,
                // pose, and extents.
                when (state.trackingState) {
                    TrackingState.TRACKING -> {
                        // update our model if our category changes, but ignore changes
                        // _into_ the unknown state.
                        if (
                            state.category != currentCategory &&
                                state.category != AugmentedObjectCategory.UNKNOWN
                        ) {
                            modelEntity.dispose()
                            modelEntity = loadModelForCategory(state.category)
                            currentCategory = state.category
                        }

                        if (currentCategory == AugmentedObjectCategory.UNKNOWN) {
                            modelEntity.setEnabled(false)
                            // early return; wait for the next update.
                            return@collect
                        }

                        modelEntity.setEnabled(true)
                        modelEntity.setAlpha(
                            TRACKED_ALPHA_VALUES[state.category.toString()] ?: TRACKED_ALPHA_DEFAULT
                        )
                        counter++

                        val newPose =
                            _session.scene.perceptionSpace.transformPoseTo(
                                state.centerPose,
                                _session.scene.activitySpace,
                            )
                        modelEntity.setPose(newPose)

                        if (counter > PANEL_RESIZE_UPDATE_COUNT) {
                            @SuppressLint("RestrictedApiAndroidX")
                            modelEntity.setScale(scaledExtents(state.extents))
                            counter = 0
                        }
                    }
                    TrackingState.PAUSED -> modelEntity.setAlpha(PAUSED_ALPHA)
                    TrackingState.STOPPED -> modelEntity.setEnabled(false)
                }
            }
        } finally {
            XrLog.debug { "Stopping render job for $augmentedObject" }
            // we've been cancelled, so dispose of the model entity.
            modelEntity.dispose()
        }
    }

    private fun updateObjectModels(objects: Collection<AugmentedObject>) {
        val objectsToRender = _renderedObjects.value.toMutableList()
        // create renderers for new objects
        for (obj in objects) {
            if (_renderedObjects.value.none { it.hashCode() == obj.hashCode() }) {
                addObjectModel(obj, objectsToRender)
            }
        }

        // stop rendering dropped objects
        for (renderedObject in objectsToRender) {
            if (objects.none { it.hashCode() == renderedObject.hashCode() }) {
                objectsToRender.remove(renderedObject)
                _runningJobs.remove(renderedObject)?.cancel()
            }
        }

        // emit to notify collectors that the collection has been updated.
        _renderedObjects.value = objectsToRender
    }

    private companion object {
        private val PX_PER_METER = Resources.getSystem().displayMetrics.density * 1111.11f
        private const val PANEL_RESIZE_UPDATE_COUNT = 50

        @SuppressLint("PrimitiveInCollection")
        private val TRACKED_ALPHA_VALUES =
            mapOf("Keyboard" to .05f, "Mouse" to .25f, "Laptop" to .1f)
        private val SUPPORTED_OBJECT_MODELS =
            mapOf(
                "Keyboard" to "BoundingBoxGreen.glb",
                "Mouse" to "BoundingBoxBlue.glb",
                "Laptop" to "BoundingBoxYellow.glb",
            )
        private const val DEFAULT_OBJECT_MODEL = "BoundingBoxGreen.glb"
        private const val TRACKED_ALPHA_DEFAULT = .5f
        private const val PAUSED_ALPHA = 0.25f

        private const val MODEL_SCALING_FACTOR = 1f / 1.7f / 2f
    }
}
