/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.xr.arcore.AugmentedImage
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
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

internal class AugmentedImageRenderer {

    private var _imageModel: GltfModel? = null
    private val _renderedImages: MutableStateFlow<List<AugmentedImage>> =
        MutableStateFlow(mutableListOf<AugmentedImage>())
    private val _runningJobs = Collections.synchronizedMap(HashMap<AugmentedImage, Job>())

    private lateinit var _coroutineScope: CoroutineScope
    private lateinit var _session: Session
    private lateinit var _supervisorJob: Job

    val renderedImages: StateFlow<Collection<AugmentedImage>> = _renderedImages.asStateFlow()

    fun startRendering(session: Session, coroutineScope: CoroutineScope) {
        _session = session

        _supervisorJob = SupervisorJob()
        _coroutineScope = CoroutineScope(coroutineScope.coroutineContext + _supervisorJob)

        _coroutineScope.launch {
            AugmentedImage.subscribe(_session).collect { updateImageModels(it) }
        }
    }

    fun stopRendering() {
        check(::_session.isInitialized) { "_session is not initialized" }
        check(::_supervisorJob.isInitialized) { "_supervisorJob is not initialized" }

        // cancel all rendering jobs
        _runningJobs.clear()
        _supervisorJob.cancel()

        // emit an empty list to and flow subscribers
        _renderedImages.value = emptyList()
    }

    private fun addImageModel(obj: AugmentedImage, imagesToRender: MutableList<AugmentedImage>) {
        // Make the render job a child of the update job so it completes when the parent completes.
        _coroutineScope.launch { updateAndRenderImage(obj) }

        imagesToRender.add(obj)
    }

    private suspend fun loadModel(): GltfModelEntity {
        if (_imageModel == null) {
            _imageModel = GltfModel.create(_session, Paths.get("models", DEFAULT_IMAGE_MODEL))
        }

        val entity = GltfModelEntity.create(_session, _imageModel!!)
        entity.setEnabled(false)
        return entity
    }

    private fun scaledExtents(extents: FloatSize2d): Vector3 {
        return Vector3(
            extents.width * MODEL_SCALING_FACTOR,
            MODEL_DEPTH,
            extents.height * MODEL_SCALING_FACTOR,
        )
    }

    private suspend fun updateAndRenderImage(augmentedImage: AugmentedImage) {
        val modelEntity = loadModel()
        // The counter starts at max to trigger the resize on the first update loop.
        var counter = PANEL_RESIZE_UPDATE_COUNT
        try {
            augmentedImage.state.collect { state ->
                // update the model entity based on the current tracking state,
                // pose, and extents.
                when (state.trackingState) {
                    TrackingState.TRACKING -> {
                        modelEntity.setEnabled(true)
                        modelEntity.setAlpha(.5f)
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
            modelEntity.parent = null
        }
    }

    private fun updateImageModels(images: Collection<AugmentedImage>) {
        val imagesToRender = _renderedImages.value.toMutableList()
        // create renderers for new images
        for (image in images) {
            if (_renderedImages.value.none { it.hashCode() == image.hashCode() }) {
                addImageModel(image, imagesToRender)
            }
        }

        // stop rendering dropped images
        for (renderedImage in imagesToRender) {
            if (images.none { it.hashCode() == renderedImage.hashCode() }) {
                imagesToRender.remove(renderedImage)
                _runningJobs.remove(renderedImage)?.cancel()
            }
        }

        // emit to notify collectors that the collection has been updated.
        _renderedImages.value = imagesToRender
    }

    private companion object {
        private const val PANEL_RESIZE_UPDATE_COUNT = 50
        private const val MODEL_SCALING_FACTOR = 1f / 1.7f / 2f
        private const val MODEL_DEPTH = .001f
        private const val DEFAULT_IMAGE_MODEL = "BoundingBoxGreen.glb"
        private const val PAUSED_ALPHA = 0.25f
    }
}
