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
import androidx.xr.arcore.QrCode
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

internal class QrCodeRenderer {

    private var _qrCodeModel: GltfModel? = null

    private val _renderedQrCodes: MutableStateFlow<List<QrCode>> =
        MutableStateFlow(mutableListOf<QrCode>())
    private val _runningJobs = Collections.synchronizedMap(HashMap<QrCode, Job>())

    private lateinit var _coroutineScope: CoroutineScope
    private lateinit var _session: Session
    private lateinit var _supervisorJob: Job

    val renderedQrCodes: StateFlow<Collection<QrCode>> = _renderedQrCodes.asStateFlow()

    fun startRendering(session: Session, coroutineScope: CoroutineScope) {
        _session = session

        _supervisorJob = SupervisorJob()
        _coroutineScope = CoroutineScope(coroutineScope.coroutineContext + _supervisorJob)

        _coroutineScope.launch { QrCode.subscribe(_session).collect { updateQrCodeModels(it) } }
    }

    fun stopRendering() {
        check(::_session.isInitialized) { "_session is not initialized" }
        check(::_supervisorJob.isInitialized) { "_supervisorJob is not initialized" }

        // cancel all rendering jobs
        _runningJobs.clear()
        _supervisorJob.cancel()

        // emit an empty list to and flow subscribers
        _renderedQrCodes.value = emptyList()
    }

    private fun addQrCodeModel(qrCode: QrCode, qrCodesToRender: MutableList<QrCode>) {
        // Make the render job a child of the update job so it completes when the parent completes.
        _runningJobs[qrCode] = _coroutineScope.launch { updateAndRenderQrCode(qrCode) }

        qrCodesToRender.add(qrCode)
    }

    private suspend fun loadModel(): GltfModelEntity {
        if (_qrCodeModel == null) {
            _qrCodeModel = GltfModel.create(_session, Paths.get("models", DEFAULT_QR_CODE_MODEL))
        }

        val entity = GltfModelEntity.create(_session, _qrCodeModel!!)
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

    private suspend fun updateAndRenderQrCode(qrCode: QrCode) {
        val modelEntity = loadModel()
        // The counter starts at max to trigger the resize on the first update loop.
        var counter = PANEL_RESIZE_UPDATE_COUNT
        try {
            qrCode.state.collect { state ->
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

    private fun updateQrCodeModels(qrCodes: Collection<QrCode>) {
        val qrCodesToRender = _renderedQrCodes.value.toMutableList()
        // create renderers for new QR codes
        for (qrCode in qrCodes) {
            if (_renderedQrCodes.value.none { it == qrCode }) {
                addQrCodeModel(qrCode, qrCodesToRender)
            }
        }

        // stop rendering dropped QR codes
        qrCodesToRender.removeIf { renderedQrCode ->
            if (qrCodes.none { it == renderedQrCode }) {
                _runningJobs.remove(renderedQrCode)?.cancel()
                true
            } else {
                false
            }
        }

        // emit to notify collectors that the collection has been updated.
        _renderedQrCodes.value = qrCodesToRender
    }

    private companion object {
        private const val PANEL_RESIZE_UPDATE_COUNT = 50
        private const val MODEL_SCALING_FACTOR = 1f / 1.7f / 2f
        private const val MODEL_DEPTH = .001f
        private const val DEFAULT_QR_CODE_MODEL = "BoundingBoxGreen.glb"
        private const val PAUSED_ALPHA = 0.25f
    }
}
