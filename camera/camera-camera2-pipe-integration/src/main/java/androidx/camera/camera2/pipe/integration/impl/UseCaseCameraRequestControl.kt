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
 * Provides methods to update the configuration and parameters of the camera. It also stores the
 * repeating request parameters associated with the configured [UseCaseCamera]. When parameters are
 * updated, it triggers changes in the [UseCaseCameraState].
 *
 * Parameters can be stored and managed according to different configuration types. Each type can be
 * modified or overridden independently without affecting other types.
 */
@JvmDefaultWithCompatibility
public interface UseCaseCameraRequestControl {
    /** Defines the types or categories of configuration parameters. */
    public enum class Type {
        /** Parameters related to the overall session configuration. */
        SESSION_CONFIG,
        /** General, default parameters. */
        DEFAULT,
        /** Parameters specifically for interoperability with Camera2. */
        CAMERA2_CAMERA_CONTROL
    }

    // Repeating Request Parameters
    /**
     * Asynchronously sets or updates parameters for the repeating capture request.
     *
     * New values will overwrite any existing parameters with the same key for the given [type]. If
     * no [type] is specified, it defaults to [Type.DEFAULT].
     *
     * @param type The category of parameters being set (default: [Type.DEFAULT]).
     * @param values A map of [CaptureRequest.Key] to their new values.
     * @param optionPriority The priority for resolving conflicts if the same parameter is set
     *   multiple times.
     * @return A [Deferred] object representing the asynchronous operation.
     */
    public fun setParametersAsync(
        type: Type = Type.DEFAULT,
        values: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
        optionPriority: Config.OptionPriority = defaultOptionPriority,
    ): Deferred<Unit>

    /**
     * Asynchronously updates the repeating request with a new configuration.
     *
     * This method replaces any existing configuration, tags, and listeners associated with the
     * specified [type]. The repeating request is then rebuilt by merging all configurations, tags,
     * and listeners from all defined types.
     *
     * @param type The category of the configuration being updated (e.g., SESSION_CONFIG, DEFAULT).
     * @param config The new configuration values to apply. If null, the existing configuration for
     *   this type is cleared.
     * @param tags Optional tags to append to the repeating request, similar to
     *   [CaptureRequest.Builder.setTag].
     * @param streams The specific streams to update. If empty, all previously specified streams are
     *   updated. The update only proceeds if at least one valid stream is specified.
     * @param template The [RequestTemplate] to use for the requests. If null, the previously
     *   specified template is used.
     * @param listeners Listeners to receive capture results.
     * @param sessionConfig Optional [SessionConfig] to update if applicable to the configuration
     *   type.
     * @return A [Deferred] representing the asynchronous update operation.
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
    /**
     * Asynchronously sets the torch (flashlight) to ON state.
     *
     * @return A [Deferred] representing the asynchronous operation and its result ([Result3A]).
     */
    public suspend fun setTorchOnAsync(): Deferred<Result3A>

    /**
     * Asynchronously sets the torch (flashlight) state to OFF state.
     *
     * @param aeMode The [AeMode] to set while setting the torch value. See
     *   [CameraGraph.Session.setTorchOff] for details.
     * @return A [Deferred] representing the asynchronous operation and its result ([Result3A]).
     */
    public suspend fun setTorchOffAsync(aeMode: AeMode): Deferred<Result3A>

    /**
     * Asynchronously starts a 3A (Auto Exposure, Auto Focus, Auto White Balance) operation with the
     * specified regions and locking behaviors.
     *
     * @param aeRegions The auto-exposure regions.
     * @param afRegions The auto-focus regions.
     * @param awbRegions The auto-white balance regions.
     * @param aeLockBehavior The behavior for locking auto-exposure.
     * @param afLockBehavior The behavior for locking auto-focus.
     * @param awbLockBehavior The behavior for locking auto-white balance.
     * @param afTriggerStartAeMode The AE mode to use when triggering AF.
     * @param timeLimitNs The time limit for the 3A operation in nanoseconds. Defaults to
     *   [CameraGraph.Constants3A.DEFAULT_TIME_LIMIT_NS].
     * @return A [Deferred] representing the asynchronous operation and its result ([Result3A]).
     */
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

    /**
     * Asynchronously cancels any ongoing focus and metering operations.
     *
     * @return A [Deferred] representing the asynchronous operation and its result ([Result3A]).
     */
    public suspend fun cancelFocusAndMeteringAsync(): Deferred<Result3A>

    // Capture
    /**
     * Asynchronously issues a single capture request.
     *
     * @param captureSequence A list of [CaptureConfig] objects defining the capture settings.
     * @param captureMode The capture mode (from [ImageCapture.CaptureMode]).
     * @param flashType The flash type (from [ImageCapture.FlashType]).
     * @param flashMode The flash mode (from [ImageCapture.FlashMode]).
     * @return A list of [Deferred] objects, one for each capture in the sequence.
     */
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
    private val threads: UseCaseThreads,
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

    override fun setParametersAsync(
        type: UseCaseCameraRequestControl.Type,
        values: Map<CaptureRequest.Key<*>, Any>,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> =
        runIfNotClosed {
            synchronized(lock) {
                    debug { "[$type] Add request option: $values" }
                    infoBundleMap
                        .getOrPut(type) { InfoBundle() }
                        .options
                        .addAllCaptureRequestOptionsWithPriority(values, optionPriority)
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

    override suspend fun setTorchOnAsync(): Deferred<Result3A> =
        runIfNotClosed { useGraphSessionOrFailed { it.setTorchOn() } } ?: submitFailedResult

    override suspend fun setTorchOffAsync(aeMode: AeMode): Deferred<Result3A> =
        runIfNotClosed {
            useGraphSessionOrFailed {
                it.setTorchOff(
                    aeMode = aeMode,
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
                    aeRegions = aeRegions ?: METERING_REGIONS_DEFAULT.asList(),
                    afRegions = afRegions ?: METERING_REGIONS_DEFAULT.asList(),
                    awbRegions = awbRegions ?: METERING_REGIONS_DEFAULT.asList()
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
