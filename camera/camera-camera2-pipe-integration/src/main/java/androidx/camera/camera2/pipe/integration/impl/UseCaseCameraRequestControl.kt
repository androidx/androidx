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
import androidx.annotation.AnyThread
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.Constants3A.METERING_REGIONS_DEFAULT
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.interop.configureWithUnchecked
import androidx.camera.camera2.pipe.integration.interop.getCamera2CaptureRequestConfigurator
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.CaptureConfig.TEMPLATE_TYPE_NONE
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.MutableTagBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.TagBundle
import dagger.Binds
import dagger.Module
import java.util.concurrent.Executor
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
 *
 * This class should be used as the entry point for submitting requests to the [UseCaseCameraScope]
 * layer. This ensures that thread confinement are properly applied at a single place for the whole
 * [UseCaseCameraScope] and reduces concurrency issues.
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
        CAMERA2_CAMERA_CONTROL,
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
    @AnyThread
    public fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        type: Type = Type.DEFAULT,
        optionPriority: Config.OptionPriority = defaultOptionPriority,
    ): Deferred<Unit>

    /**
     * Asynchronously sets parameters for the repeating capture request by invoking a factory
     * function.
     *
     * This overload is an optimization. The provided [valuesFactory] lambda will be invoked on the
     * internal camera executor, not on the calling thread. This allows the caller to avoid
     * expensive map creation or computation on their current thread, which is especially useful
     * when calling from within a `synchronized` block.
     *
     * @param valuesFactory A lambda function that produces the parameter map. This will be executed
     *   on the camera executor thread.
     * @param type The [Type] of the parameters to set.
     * @param optionPriority The priority of these options.
     * @return A [Deferred] that completes when the parameters have been submitted.
     */
    @AnyThread
    public fun setParametersAsync(
        valuesFactory: () -> Map<CaptureRequest.Key<*>, Any>,
        type: Type = Type.DEFAULT,
        optionPriority: Config.OptionPriority = defaultOptionPriority,
    ): Deferred<Unit>

    /**
     * Asynchronously removes parameters for the repeating capture request.
     *
     * This method clears the parameters with the specified [CaptureRequest.Key]s if the parameters
     * with the same keys have been set to this control previously.
     *
     * This method doesn't clear the parameters with the specified [CaptureRequest.Key] if the
     * parameters with the same keys are set by other controls.
     *
     * @param keys A list of [CaptureRequest.Key] to be removed.
     * @param type The category of parameters being set (default: [Type.DEFAULT]).
     * @return A [Deferred] object representing the asynchronous operation.
     */
    public fun removeParametersAsync(
        keys: List<CaptureRequest.Key<*>>,
        type: Type = Type.DEFAULT,
    ): Deferred<Unit>

    /**
     * Asynchronously builds a SessionConfig from the running UseCases and updates the repeating
     * request.
     *
     * This performs all work, including the potentially slow SessionConfig creation, within a
     * single sequential task to guarantee ordering.
     *
     * @param isPrimary Whether the camera is the primary camera.
     * @param runningUseCases The collection of active UseCases.
     * @return A [Deferred] representing the asynchronous update operation.
     */
    @AnyThread
    public fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Deferred<Unit>

    /**
     * Asynchronously updates the repeating request with Camera2 interop configurations.
     *
     * This replaces any existing configuration for the [Type.CAMERA2_CAMERA_CONTROL] type. The
     * repeating request is then rebuilt by merging all configurations.
     *
     * @param config The new configuration values to apply.
     * @param tags Optional tags to append to the repeating request.
     * @return A [Deferred] representing the asynchronous update operation.
     */
    @AnyThread
    public fun updateCamera2ConfigAsync(
        config: Config,
        tags: Map<String, Any> = emptyMap(),
    ): Deferred<Unit>

    // 3A
    /**
     * Asynchronously sets the torch (flashlight) to ON state.
     *
     * @return A [Deferred] representing the asynchronous operation and its result ([Result3A]).
     */
    @AnyThread public fun setTorchOnAsync(): Deferred<Result3A>

    /**
     * Asynchronously sets the torch (flashlight) state to OFF state.
     *
     * @param aeMode The [AeMode] to set while setting the torch value. See
     *   [androidx.camera.camera2.pipe.CameraControls3A.setTorchOff] for details.
     * @return A [Deferred] representing the asynchronous operation and its result ([Result3A]).
     */
    @AnyThread public fun setTorchOffAsync(aeMode: AeMode): Deferred<Result3A>

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
    @AnyThread
    public fun startFocusAndMeteringAsync(
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
    @AnyThread public fun cancelFocusAndMeteringAsync(): Deferred<Result3A>

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
    @AnyThread
    public fun issueSingleCaptureAsync(
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
     * @see [androidx.camera.camera2.pipe.CameraControls3A.update3A]
     */
    @AnyThread
    public fun update3aRegions(
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
    ): Deferred<Result3A>

    /**
     * Waits for any ongoing surface setup to be completed and returns a boolean value to indicate
     * if a successful setup exists.
     *
     * @see UseCaseSurfaceManager.awaitSetupCompletion
     */
    public suspend fun awaitSurfaceSetup(): Boolean

    public fun close()
}

@UseCaseCameraScope
public class UseCaseCameraRequestControlImpl
@Inject
constructor(
    private val capturePipeline: CapturePipeline,
    private val state: UseCaseCameraState,
    private val useCaseGraphConfig: UseCaseGraphConfig,
    private val useCaseSurfaceManager: UseCaseSurfaceManager,
    private val threads: UseCaseThreads,
    private val cameraXConfig: CameraXConfig? = null,
) : UseCaseCameraRequestControl {
    private val graph = useCaseGraphConfig.graph

    @Volatile private var closed = false

    private data class InfoBundle(
        val options: Camera2ImplConfig.Builder = Camera2ImplConfig.Builder(),
        val tags: MutableMap<String, Any> = mutableMapOf(),
        val listeners: MutableSet<Request.Listener> = mutableSetOf(),
        var template: RequestTemplate? = null,
    )

    /**
     * Creates a new [InfoBundle] by copying the current one and adding new parameters to its
     * options.
     */
    private fun InfoBundle.withParameters(
        values: Map<CaptureRequest.Key<*>, Any>,
        optionPriority: Config.OptionPriority,
    ): InfoBundle {
        val newOptionsBuilder =
            Camera2ImplConfig.Builder().apply {
                insertAllOptions(this@withParameters.options.mutableConfig)

                addAllCaptureRequestOptionsWithPriority(values, optionPriority)
            }

        return this.copy(
            options = newOptionsBuilder,
            tags = this.tags.toMutableMap(),
            listeners = this.listeners.toMutableSet(),
        )
    }

    /**
     * Creates a new [InfoBundle] by copying the current one and removing specified parameters from
     * its options.
     */
    private fun InfoBundle.withoutParameters(keys: List<CaptureRequest.Key<*>>): InfoBundle {
        val newOptionsBuilder =
            Camera2ImplConfig.Builder().apply {
                insertAllOptions(this@withoutParameters.options.mutableConfig)
                removeCaptureRequestOptions(keys)
            }

        return this.copy(
            options = newOptionsBuilder,
            tags = this.tags.toMutableMap(),
            listeners = this.listeners.toMutableSet(),
        )
    }

    private val infoBundleMap = mutableMapOf<UseCaseCameraRequestControl.Type, InfoBundle>()

    override fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        type: UseCaseCameraRequestControl.Type,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> {
        return runIfNotClosed {
            threads.confineDeferredSuspend { setParametersInternal(type, values, optionPriority) }
        } ?: canceledResult
    }

    override fun setParametersAsync(
        valuesFactory: () -> Map<CaptureRequest.Key<*>, Any>,
        type: UseCaseCameraRequestControl.Type,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> {
        return runIfNotClosed {
            threads.confineDeferredSuspend {
                val values = valuesFactory()
                setParametersInternal(type, values, optionPriority)
            }
        } ?: canceledResult
    }

    private suspend fun setParametersInternal(
        type: UseCaseCameraRequestControl.Type,
        values: Map<CaptureRequest.Key<*>, Any>,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> {
        Camera2Logger.debug {
            "UseCaseCameraRequestControlImpl#setParametersAsync: [$type] values = $values" +
                ", optionPriority = $optionPriority"
        }
        val currentBundle = infoBundleMap.getOrPut(type) { InfoBundle() }
        infoBundleMap[type] = currentBundle.withParameters(values, optionPriority)
        return infoBundleMap.merge().updateCameraStateAsync()
    }

    override fun removeParametersAsync(
        keys: List<CaptureRequest.Key<*>>,
        type: UseCaseCameraRequestControl.Type,
    ): Deferred<Unit> =
        runIfNotClosed {
            threads.confineDeferredSuspend {
                Camera2Logger.debug {
                    "UseCaseCameraRequestControlImpl#removeParametersAsync: [$type] keys = $keys"
                }
                val currentBundle = infoBundleMap.getOrPut(type) { InfoBundle() }
                infoBundleMap[type] = currentBundle.withoutParameters(keys)
                infoBundleMap.merge().updateCameraStateAsync()
            }
        } ?: canceledResult

    override fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Deferred<Unit> =
        runIfNotClosed {
            threads.confineDeferredSuspend {
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl: Building SessionConfig..." }

                val sessionConfigAdapter = SessionConfigAdapter(runningUseCases, isPrimary)
                val sessionConfig =
                    sessionConfigAdapter.getValidSessionConfigOrNull()
                        ?: run {
                            Camera2Logger.debug { "Using default SessionConfig" }
                            SessionConfig.Builder()
                                .apply { setTemplateType(DEFAULT_REQUEST_TEMPLATE) }
                                .build()
                        }

                Camera2Logger.debug {
                    "UseCaseCameraRequestControlImpl: SessionConfig built. Updating state..."
                }

                infoBundleMap[UseCaseCameraRequestControl.Type.SESSION_CONFIG] =
                    sessionConfig.toInfoBundle(threads.sequentialExecutor)

                val streams =
                    useCaseGraphConfig.getStreamIdsFromSurfaces(
                        sessionConfig.repeatingCaptureConfig.surfaces
                    )
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl: State update processing." }
                infoBundleMap.merge().updateCameraStateAsync(streams = streams)
            }
        } ?: canceledResult

    override fun updateCamera2ConfigAsync(config: Config, tags: Map<String, Any>): Deferred<Unit> =
        runIfNotClosed {
            threads.confineDeferredSuspend {
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl#updateCamera2ConfigAsync" }
                infoBundleMap[UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL] =
                    InfoBundle(
                        options = config.extractCamera2ImplConfigBuilder(),
                        tags = tags.toMutableMap(),
                    )
                infoBundleMap.merge().updateCameraStateAsync()
            }
        } ?: canceledResult

    override fun setTorchOnAsync(): Deferred<Result3A> =
        runIfNotClosed {
            threads.confineDeferredSuspend {
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl#setTorchOnAsync" }
                useGraphSessionOrFailed { it.setTorchOn() }
            }
        } ?: submitFailedResult

    override fun setTorchOffAsync(aeMode: AeMode): Deferred<Result3A> =
        runIfNotClosed {
            threads.confineDeferredSuspend {
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl#setTorchOffAsync" }
                useGraphSessionOrFailed { it.setTorchOff(aeMode = aeMode) }
            }
        } ?: submitFailedResult

    override fun startFocusAndMeteringAsync(
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
            threads.confineDeferredSuspend {
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl#startFocusAndMeteringAsync" }
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
                        lockedTimeLimitNs = timeLimitNs,
                    )
                }
            }
        } ?: submitFailedResult

    override fun cancelFocusAndMeteringAsync(): Deferred<Result3A> =
        runIfNotClosed {
            threads.confineDeferredSuspend {
                Camera2Logger.debug {
                    "UseCaseCameraRequestControlImpl#cancelFocusAndMeteringAsync"
                }

                useGraphSessionOrFailed { it.unlock3A(ae = true, af = true, awb = true) }.await()

                useGraphSessionOrFailed {
                    it.update3A(
                        aeRegions = METERING_REGIONS_DEFAULT.asList(),
                        afRegions = METERING_REGIONS_DEFAULT.asList(),
                        awbRegions = METERING_REGIONS_DEFAULT.asList(),
                    )
                }
            }
        } ?: submitFailedResult

    override fun issueSingleCaptureAsync(
        captureSequence: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
        @ImageCapture.FlashMode flashMode: Int,
    ): List<Deferred<Void?>> =
        runIfNotClosed {
            threads.confineDeferredListSuspend(captureSequence.size) {
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl#issueSingleCaptureAsync" }

                if (captureSequence.hasInvalidSurface()) {
                    failedResults(
                        captureSequence.size,
                        "Capture request failed due to invalid surface",
                    )
                }

                infoBundleMap.merge().let { infoBundle ->
                    Camera2Logger.debug {
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
        }
            ?: failedResults(
                captureSequence.size,
                "Capture request is cancelled on closed CameraGraph",
            )

    override fun update3aRegions(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
    ): Deferred<Result3A> =
        runIfNotClosed {
            threads.confineDeferredSuspend {
                Camera2Logger.debug { "UseCaseCameraRequestControlImpl#update3aRegions" }
                useGraphSessionOrFailed {
                    it.update3A(
                        aeRegions = aeRegions ?: METERING_REGIONS_DEFAULT.asList(),
                        afRegions = afRegions ?: METERING_REGIONS_DEFAULT.asList(),
                        awbRegions = awbRegions ?: METERING_REGIONS_DEFAULT.asList(),
                    )
                }
            }
        } ?: submitFailedResult

    override suspend fun awaitSurfaceSetup(): Boolean = useCaseSurfaceManager.awaitSetupCompletion()

    override fun close() {
        closed = true
        Camera2Logger.debug { "UseCaseCameraRequestControl: closed" }
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
    private fun Map<UseCaseCameraRequestControl.Type, InfoBundle>.merge(): InfoBundle {
        val mergedBundle = InfoBundle(template = RequestTemplate(DEFAULT_REQUEST_TEMPLATE))
        UseCaseCameraRequestControl.Type.entries.forEach { type ->
            val bundleForType = this@merge[type]

            if (bundleForType != null) {
                mergedBundle.options.insertAllOptions(bundleForType.options.mutableConfig)
                mergedBundle.tags.putAll(bundleForType.tags)
                mergedBundle.listeners.addAll(bundleForType.listeners)
                bundleForType.template?.let { mergedBundle.template = it }
            }
        }
        return mergedBundle
    }

    private fun InfoBundle.toTagBundle(): TagBundle =
        MutableTagBundle.create().also { tagBundle ->
            tags.forEach { (tagKey, tagValue) -> tagBundle.putTag(tagKey, tagValue) }
        }

    private suspend fun InfoBundle.updateCameraStateAsync(
        streams: Set<StreamId>? = null
    ): Deferred<Unit> =
        runIfNotClosed {
            cameraXConfig
                ?.getCamera2CaptureRequestConfigurator()
                ?.configureWithUnchecked(options.build().toParameters().toMap())

            capturePipeline.template =
                if (template!!.value != TEMPLATE_TYPE_NONE) {
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
            Camera2Logger.debug(e) { "Cannot acquire the CameraGraph.Session" }
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

        public fun SessionConfig.extractCamera2ImplConfigBuilder(): Camera2ImplConfig.Builder {
            val updatedConfig = Camera2ImplConfig.Builder()
            if (expectedFrameRateRange != FRAME_RATE_RANGE_UNSPECIFIED) {
                updatedConfig.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    expectedFrameRateRange,
                )
            }
            updatedConfig.insertAllOptions(implementationOptions)
            return updatedConfig
        }

        public fun SessionConfig.extractTags(): MutableMap<String, Any> =
            repeatingCaptureConfig.tagBundle.toMap().toMutableMap()

        public fun SessionConfig.extractListeners(
            callbackExecutor: Executor
        ): MutableSet<Request.Listener> =
            mutableSetOf(
                CameraCallbackMap.createFor(repeatingCameraCaptureCallbacks, callbackExecutor)
            )

        public fun SessionConfig.extractTemplate(): RequestTemplate = RequestTemplate(templateType)

        private fun SessionConfig.toInfoBundle(callbackExecutor: Executor): InfoBundle {
            return InfoBundle(
                options = extractCamera2ImplConfigBuilder(),
                tags = extractTags(),
                listeners = extractListeners(callbackExecutor),
                template = extractTemplate(),
            )
        }

        private fun Config.extractCamera2ImplConfigBuilder(): Camera2ImplConfig.Builder {
            val updatedConfig = Camera2ImplConfig.Builder()
            updatedConfig.insertAllOptions(this)
            return updatedConfig
        }
    }
}

public fun TagBundle.toMap(): Map<String, Any> =
    mutableMapOf<String, Any>().also {
        listKeys().forEach { tagKey -> it[tagKey] = getTag(tagKey) as Any }
    }
