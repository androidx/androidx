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

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.CaptureConfig.TEMPLATE_TYPE_NONE
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.MutableTagBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.TagBundle
import dagger.Binds
import dagger.Module
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal const val DEFAULT_REQUEST_TEMPLATE = CameraDevice.TEMPLATE_PREVIEW

/**
 * The RequestControl provides a couple of APIs to update the config of the camera, it also stores
 * the (repeating) request parameters of the configured [UseCaseCamera]. Once the parameters are
 * updated, it will trigger the update to the [UseCaseCameraState].
 *
 * The parameters can be stored for the different types of config respectively. Each type of the
 * config can be removed or overridden respectively without interfering with the other types.
 */
public interface UseCaseCameraRequestControl {
    /** The declaration order is the ordering to merge. */
    public enum class Type {
        SESSION_CONFIG,
        DEFAULT,
        CAMERA2_CAMERA_CONTROL,
    }

    // Repeating parameters
    /**
     * Append a new option to update the repeating request.
     *
     * This method will: (1) Stores [values], [tags] and [listeners] by [type] respectively. The new
     * inputs above will append to the values that store as the same [type], the existing values
     * that don't conflict with the new inputs will not be cleared. If the [type] isn't set, it will
     * treat the new inputs as the [Type.DEFAULT] (2) Update the repeating request by merging all
     * the [values], [tags] and [listeners] from all the defined types.
     *
     * @param type the type of the input parameter, the possible value could be one of the [Type]
     * @param values the new [CaptureRequest.Key] and value will be append to the repeating request
     * @param optionPriority is the priority option that would be used to determine whether the new
     *   value can override the existing value or not. This is default to
     *   [Config.OptionPriority.OPTIONAL]
     * @param tags the option tag that could be appended to the repeating request, its effect is
     *   similar to the [CaptureRequest.Builder.setTag].
     * @param listeners to receive the capture results.
     */
    public fun addParametersAsync(
        type: Type = Type.DEFAULT,
        values: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
        optionPriority: Config.OptionPriority = defaultOptionPriority,
        tags: Map<String, Any> = emptyMap(),
        listeners: Set<Request.Listener> = emptySet()
    ): Deferred<Unit>

    /**
     * Use a new [config] to update the repeating request.
     *
     * This method will: (1) Stores [config], [tags] and [listeners] by [type] respectively. The new
     * inputs above will take place of the existing value of the [type]. (2) Update the repeating
     * request by merging all the [config], [tags] and [listeners] from all the defined types.
     *
     * @param type the type of the input [config]
     * @param config the new config values will be used to update the repeating request.
     * @param tags the option tag that could be appended to the repeating request, its effect is
     *   similar to the [CaptureRequest.Builder.setTag].
     * @param streams Specify a list of streams that would be updated. Leave the value in empty will
     *   use the [streams] that is previously specified. The update can only be processed after
     *   specifying 1 or more valid streams.
     * @param template The [RequestTemplate] will be used for the requests. Leave the value in empty
     *   will use the [RequestTemplate] that is previously specified.
     * @param listeners to receive the capture results.
     */
    public fun setConfigAsync(
        type: Type,
        config: Config? = null,
        tags: Map<String, Any> = emptyMap(),
        streams: Set<StreamId>? = null,
        template: RequestTemplate? = null,
        listeners: Set<Request.Listener> = emptySet(),
        sessionConfig: SessionConfig? = null,
    ): Deferred<Unit>

    // 3A
    public suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A>

    public suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
        aeLockBehavior: Lock3ABehavior? = null,
        afLockBehavior: Lock3ABehavior? = null,
        awbLockBehavior: Lock3ABehavior? = null,
        afTriggerStartAeMode: AeMode? = null,
        timeLimitNs: Long = CameraGraph.Constants3A.DEFAULT_TIME_LIMIT_NS,
    ): Deferred<Result3A>

    public suspend fun cancelFocusAndMeteringAsync(): Deferred<Result3A>

    // Capture
    public suspend fun issueSingleCaptureAsync(
        captureSequence: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
        @ImageCapture.FlashMode flashMode: Int,
    ): List<Deferred<Void?>>

    /**
     * Updates the 3A regions and applies to the repeating request.
     *
     * Note that camera-pipe may invalidate the CameraGraph and update the repeating request
     * parameters for this operations.
     *
     * @see [CameraGraph.Session.update3A]
     */
    public suspend fun update3aRegions(
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
    ): Deferred<Result3A>

    public fun close()
}

@UseCaseCameraScope
public class UseCaseCameraRequestControlImpl
@Inject
constructor(
    private val capturePipeline: CapturePipeline,
    private val state: UseCaseCameraState,
    private val useCaseGraphConfig: UseCaseGraphConfig,
) : UseCaseCameraRequestControl {
    private val graph = useCaseGraphConfig.graph

    @Volatile private var closed = false

    private data class InfoBundle(
        val options: Camera2ImplConfig.Builder = Camera2ImplConfig.Builder(),
        val tags: MutableMap<String, Any> = mutableMapOf(),
        val listeners: MutableSet<Request.Listener> = mutableSetOf(),
        var template: RequestTemplate? = null,
    )

    @GuardedBy("lock")
    private val infoBundleMap = mutableMapOf<UseCaseCameraRequestControl.Type, InfoBundle>()
    private val lock = Any()

    override fun addParametersAsync(
        type: UseCaseCameraRequestControl.Type,
        values: Map<CaptureRequest.Key<*>, Any>,
        optionPriority: Config.OptionPriority,
        tags: Map<String, Any>,
        listeners: Set<Request.Listener>
    ): Deferred<Unit> =
        runIfNotClosed {
            synchronized(lock) {
                    debug { "[$type] Add request option: $values" }
                    infoBundleMap
                        .getOrPut(type) { InfoBundle() }
                        .let {
                            it.options.addAllCaptureRequestOptionsWithPriority(
                                values,
                                optionPriority
                            )
                            it.tags.putAll(tags)
                            it.listeners.addAll(listeners)
                        }
                    infoBundleMap.merge()
                }
                .updateCameraStateAsync()
        } ?: canceledResult

    override fun setConfigAsync(
        type: UseCaseCameraRequestControl.Type,
        config: Config?,
        tags: Map<String, Any>,
        streams: Set<StreamId>?,
        template: RequestTemplate?,
        listeners: Set<Request.Listener>,
        sessionConfig: SessionConfig?,
    ): Deferred<Unit> =
        runIfNotClosed {
            synchronized(lock) {
                    debug { "[$type] Set config: ${config?.toParameters()}" }
                    infoBundleMap[type] =
                        InfoBundle(
                            Camera2ImplConfig.Builder().apply {
                                config?.let { insertAllOptions(it) }
                            },
                            tags.toMutableMap(),
                            listeners.toMutableSet(),
                            template,
                        )
                    infoBundleMap.merge()
                }
                .updateCameraStateAsync(
                    streams = streams,
                    sessionConfig = sessionConfig,
                )
        } ?: canceledResult

    override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> =
        runIfNotClosed {
            useGraphSessionOrFailed {
                it.setTorch(
                    when (enabled) {
                        true -> TorchState.ON
                        false -> TorchState.OFF
                    }
                )
            }
        } ?: submitFailedResult

    override suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        afTriggerStartAeMode: AeMode?,
        timeLimitNs: Long,
    ): Deferred<Result3A> =
        runIfNotClosed {
            useGraphSessionOrFailed {
                it.lock3A(
                    aeRegions = aeRegions,
                    afRegions = afRegions,
                    awbRegions = awbRegions,
                    aeLockBehavior = aeLockBehavior,
                    afLockBehavior = afLockBehavior,
                    awbLockBehavior = awbLockBehavior,
                    afTriggerStartAeMode = afTriggerStartAeMode,
                    convergedTimeLimitNs = timeLimitNs,
                    lockedTimeLimitNs = timeLimitNs
                )
            }
        } ?: submitFailedResult

    override suspend fun cancelFocusAndMeteringAsync(): Deferred<Result3A> =
        runIfNotClosed {
            useGraphSessionOrFailed { it.unlock3A(ae = true, af = true, awb = true) }.await()

            useGraphSessionOrFailed {
                it.update3A(
                    aeRegions = METERING_REGIONS_DEFAULT.asList(),
                    afRegions = METERING_REGIONS_DEFAULT.asList(),
                    awbRegions = METERING_REGIONS_DEFAULT.asList()
                )
            }
        } ?: submitFailedResult

    override suspend fun issueSingleCaptureAsync(
        captureSequence: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
        @ImageCapture.FlashMode flashMode: Int,
    ): List<Deferred<Void?>> =
        runIfNotClosed {
            if (captureSequence.hasInvalidSurface()) {
                failedResults(captureSequence.size, "Capture request failed due to invalid surface")
            }

            synchronized(lock) { infoBundleMap.merge() }
                .let { infoBundle ->
                    debug {
                        "UseCaseCameraRequestControl: Submitting still captures to capture pipeline"
                    }
                    capturePipeline.submitStillCaptures(
                        configs = captureSequence,
                        requestTemplate = infoBundle.template!!,
                        sessionConfigOptions = infoBundle.options.build(),
                        captureMode = captureMode,
                        flashType = flashType,
                        flashMode = flashMode,
                    )
                }
        }
            ?: failedResults(
                captureSequence.size,
                "Capture request is cancelled on closed CameraGraph"
            )

    override suspend fun update3aRegions(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> =
        runIfNotClosed {
            useGraphSessionOrFailed {
                it.update3A(
                    aeRegions = METERING_REGIONS_DEFAULT.asList(),
                    afRegions = METERING_REGIONS_DEFAULT.asList(),
                    awbRegions = METERING_REGIONS_DEFAULT.asList()
                )
            }
        } ?: submitFailedResult

    override fun close() {
        closed = true
        debug { "UseCaseCameraRequestControl: closed" }
    }

    private fun failedResults(count: Int, message: String): List<Deferred<Void?>> =
        List(count) {
            CompletableDeferred<Void>().apply {
                completeExceptionally(
                    ImageCaptureException(ImageCapture.ERROR_CAPTURE_FAILED, message, null)
                )
            }
        }

    private fun List<CaptureConfig>.hasInvalidSurface(): Boolean {
        forEach { captureConfig ->
            if (captureConfig.surfaces.isEmpty()) {
                return true
            }
            captureConfig.surfaces.forEach {
                if (useCaseGraphConfig.surfaceToStreamMap[it] == null) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * The merge order is the same as the [UseCaseCameraRequestControl.Type] declaration order.
     *
     * Option merge: The earlier merged option in [Config.OptionPriority.OPTIONAL] could be
     * overridden by later merged options. Tag merge: If there is the same tagKey but tagValue is
     * different, the later merge would override the earlier one. Listener merge: merge the
     * listeners into a set.
     */
    private fun Map<UseCaseCameraRequestControl.Type, InfoBundle>.merge(): InfoBundle =
        InfoBundle(template = RequestTemplate(DEFAULT_REQUEST_TEMPLATE)).apply {
            UseCaseCameraRequestControl.Type.values().forEach { type ->
                getOrElse(type) { InfoBundle() }
                    .let { infoBundleInType ->
                        options.insertAllOptions(infoBundleInType.options.mutableConfig)
                        tags.putAll(infoBundleInType.tags)
                        listeners.addAll(infoBundleInType.listeners)
                        infoBundleInType.template?.let { template = it }
                    }
            }
        }

    private fun InfoBundle.toTagBundle(): TagBundle =
        MutableTagBundle.create().also { tagBundle ->
            tags.forEach { (tagKey, tagValue) -> tagBundle.putTag(tagKey, tagValue) }
        }

    private fun InfoBundle.updateCameraStateAsync(
        streams: Set<StreamId>? = null,
        sessionConfig: SessionConfig? = null,
    ): Deferred<Unit> =
        runIfNotClosed {
            capturePipeline.template =
                if (template != null && template!!.value != TEMPLATE_TYPE_NONE) {
                    template!!.value
                } else {
                    DEFAULT_REQUEST_TEMPLATE
                }

            state.updateAsync(
                parameters = options.build().toParameters(),
                appendParameters = false,
                internalParameters = mapOf(CAMERAX_TAG_BUNDLE to toTagBundle()),
                appendInternalParameters = false,
                streams = streams,
                template = template,
                listeners = listeners,
                sessionConfig = sessionConfig,
            )
        } ?: canceledResult

    private inline fun <R> runIfNotClosed(block: () -> R): R? {
        return if (!closed) block() else null
    }

    private suspend inline fun useGraphSessionOrFailed(
        crossinline block: suspend (CameraGraph.Session) -> Deferred<Result3A>
    ): Deferred<Result3A> =
        try {
            graph.acquireSession().use { block(it) }
        } catch (e: CancellationException) {
            debug(e) { "Cannot acquire the CameraGraph.Session" }
            submitFailedResult
        }

    @Module
    public abstract class Bindings {
        @UseCaseCameraScope
        @Binds
        public abstract fun provideRequestControls(
            requestControl: UseCaseCameraRequestControlImpl
        ): UseCaseCameraRequestControl
    }

    public companion object {
        private val submitFailedResult =
            CompletableDeferred(Result3A(Result3A.Status.SUBMIT_FAILED))
        private val canceledResult = CompletableDeferred<Unit>().apply { cancel() }
    }
}

public fun TagBundle.toMap(): Map<String, Any> =
    mutableMapOf<String, Any>().also {
        listKeys().forEach { tagKey -> it[tagKey] = getTag(tagKey) as Any }
    }
