/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.MutableTagBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.TagBundle
import kotlinx.coroutines.Deferred
import javax.inject.Inject

/**
 * The RequestControl provides a couple of APIs to update the config of the camera, it also stores
 * the (repeating) request parameters of the configured [UseCaseCamera].
 * Once the parameters are updated, it will trigger the update to the [UseCaseCameraState].
 *
 * The parameters can be stored for the different types of config respectively. Each
 * type of the config can be removed or overridden respectively without interfering with the
 * other types.
 */
@UseCaseCameraScope
interface UseCaseCameraRequestControl {
    /**
     * The declaration order is the ordering to merge.
     */
    enum class Type {
        SESSION_CONFIG,
        DEFAULT,
        CAMERA2_CAMERA_CONTROL,
    }

    // Repeating parameters
    fun appendParametersAsync(
        type: Type = Type.DEFAULT,
        values: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
        optionPriority: Config.OptionPriority = defaultOptionPriority,
        tags: Map<String, Any> = emptyMap(),
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener> = emptySet()
    ): Deferred<Unit>
    fun setConfigAsync(
        type: Type = Type.DEFAULT,
        config: Config? = null,
        tags: Map<String, Any> = emptyMap(),
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener> = emptySet()
    ): Deferred<Unit>
    fun setSessionConfigAsync(
        sessionConfig: SessionConfig,
    ): Deferred<Unit>

    // 3A
    suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A>
    suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>,
        afRegions: List<MeteringRectangle>,
        awbRegions: List<MeteringRectangle>,
    ): Deferred<Result3A>

    // Capture
    fun issueSingleCapture(captureSequence: List<CaptureConfig>)
}

class UseCaseCameraRequestControlImpl @Inject constructor(
    private val graph: CameraGraph,
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
    private val threads: UseCaseThreads,
) : UseCaseCameraRequestControl {

    private data class InfoBundle(
        val options: Camera2ImplConfig.Builder = Camera2ImplConfig.Builder(),
        val tags: MutableMap<String, Any> = mutableMapOf(),
        val listeners: MutableSet<Request.Listener> = mutableSetOf()
    )

    @GuardedBy("lock")
    private val infoBundleMap = mutableMapOf<UseCaseCameraRequestControl.Type, InfoBundle>()
    private val lock = Any()

    private val state = UseCaseCameraState(graph, threads)
    private val configAdapter = CaptureConfigAdapter(surfaceToStreamMap, threads.backgroundExecutor)

    override fun appendParametersAsync(
        type: UseCaseCameraRequestControl.Type,
        values: Map<CaptureRequest.Key<*>, Any>,
        optionPriority: Config.OptionPriority,
        tags: Map<String, Any>,
        streams: Set<StreamId>?,
        template: RequestTemplate?,
        listeners: Set<Request.Listener>
    ): Deferred<Unit> = synchronized(lock) {
        infoBundleMap.getOrPut(type) { InfoBundle() }.let {
            it.options.addAllCaptureRequestOptionsWithPriority(values, optionPriority)
            it.tags.putAll(tags)
            it.listeners.addAll(listeners)
        }
        infoBundleMap.merge()
    }.updateCameraStateAsync(
        streams = streams,
        template = template,
    )

    override fun setConfigAsync(
        type: UseCaseCameraRequestControl.Type,
        config: Config?,
        tags: Map<String, Any>,
        streams: Set<StreamId>?,
        template: RequestTemplate?,
        listeners: Set<Request.Listener>
    ): Deferred<Unit> = synchronized(lock) {
        infoBundleMap[type] = InfoBundle(
            Camera2ImplConfig.Builder().apply {
                config?.let {
                    insertAllOptions(it)
                }
            },
            tags.toMutableMap(),
            listeners.toMutableSet()
        )
        infoBundleMap.merge()
    }.updateCameraStateAsync(
        streams = streams,
        template = template,
    )

    override fun setSessionConfigAsync(sessionConfig: SessionConfig): Deferred<Unit> {
        val repeatingStreamIds = mutableSetOf<StreamId>()
        val repeatingListeners = CameraCallbackMap()

        sessionConfig.repeatingCaptureConfig.surfaces.forEach {
            surfaceToStreamMap[it]?.let { streamId ->
                repeatingStreamIds.add(streamId)
            }
        }

        sessionConfig.repeatingCameraCaptureCallbacks.forEach { callback ->
            repeatingListeners.addCaptureCallback(
                callback,
                threads.backgroundExecutor
            )
        }

        return setConfigAsync(
            type = UseCaseCameraRequestControl.Type.SESSION_CONFIG,
            config = sessionConfig.implementationOptions,
            listeners = setOf(repeatingListeners),
            streams = repeatingStreamIds,
        )
    }

    override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> =
        graph.acquireSession().use {
            it.setTorch(
                when (enabled) {
                    true -> TorchState.ON
                    false -> TorchState.OFF
                }
            )
        }

    override suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>,
        afRegions: List<MeteringRectangle>,
        awbRegions: List<MeteringRectangle>
    ): Deferred<Result3A> = graph.acquireSession().use {
        it.lock3A(
            aeRegions = aeRegions,
            afRegions = afRegions,
            awbRegions = awbRegions,
            afLockBehavior = Lock3ABehavior.AFTER_NEW_SCAN
        )
    }

    override fun issueSingleCapture(captureSequence: List<CaptureConfig>) {
        val requests = captureSequence.map { configAdapter.mapToRequest(it) }
        // TODO(b/194243796): Need to append repeating parameters to the capture request.
        state.capture(requests)
    }

    /**
     * The merge order is the same as the [UseCaseCameraRequestControl.Type] declaration order.
     *
     * Option merge: The earlier merged option in [Config.OptionPriority.OPTIONAL] could be
     * overridden by later merged options.
     * Tag merge: If there is the same tagKey but tagValue is different, the later merge would
     * override the earlier one.
     * Listener merge: merge the listeners into a set.
     */
    private fun Map<UseCaseCameraRequestControl.Type, InfoBundle>.merge(): InfoBundle =
        InfoBundle().also {
            UseCaseCameraRequestControl.Type.values().forEach { type ->
                getOrElse(type) { InfoBundle() }.also { infoBundleInType ->
                    it.options.insertAllOptions(infoBundleInType.options.mutableConfig)
                    it.tags.putAll(infoBundleInType.tags)
                    it.listeners.addAll(infoBundleInType.listeners)
                }
            }
        }

    private fun InfoBundle.toTagBundle(): TagBundle =
        MutableTagBundle.create().also { tagBundle ->
            tags.forEach { (tagKey, tagValue) ->
                tagBundle.putTag(tagKey, tagValue)
            }
        }

    private fun InfoBundle.updateCameraStateAsync(
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
    ) = state.updateAsync(
        parameters = options.build().toParameters(),
        appendParameters = false,
        internalParameters = mapOf(CAMERAX_TAG_BUNDLE to toTagBundle()),
        appendInternalParameters = false,
        streams = streams,
        template = template,
        listeners = listeners,
    )
}