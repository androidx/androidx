/*
 * Copyright 2020 The Android Open Source Project
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
@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.media.MediaCodec
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.EncoderProfilesProviderAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CloseCameraDeviceOnCameraGraphCloseQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.CloseCaptureSessionOnVideoQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraComponent
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.DynamicRange
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.ValidatingBuilder
import androidx.camera.core.impl.stabilization.StabilizationMode
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll

/**
 * This class keeps track of the currently attached and active [UseCase]'s for a specific camera.
 * A [UseCase] during its lifetime, can be:
 *
 *   - Attached: This happens when a use case is bound to a CameraX Lifecycle, and signals that the
 *       camera should be opened, and a camera capture session should be created to include the
 *       stream corresponding to the use case. In the integration layer here, we'll recreate a
 *       CameraGraph when a use case is attached.
 *   - Detached: This happens when a use case is unbound from a CameraX Lifecycle, and signals that
 *       we no longer need this specific use case and therefore its corresponding stream in our
 *       current capture session. In the integration layer, we'll also recreate a CameraGraph when
 *       a use case is detached, though it might not be strictly necessary.
 *   - Active: This happens when the use case is considered "ready", meaning that the use case is
 *       ready to have frames delivered to it. In the case of the integration layer, this means we
 *       can start submitting the capture requests corresponding to the use case. An important note
 *       here is that a use case can actually become "active" before it is "attached", and thus we
 *       should only take action when a use case is both "attached" and "active".
 *   - Inactive: This happens when use case no longer needs frames delivered to it. This is can be
 *       seen as an optimization signal, as we technically are allowed to continue submitting
 *       capture requests, but we no longer need to. An example of this is when you clear the
 *       analyzer during ImageAnalysis.
 *
 *  In this class, we also define a new term - "Running". A use case is considered running when it's
 *  both "attached" and "active". This means we should have a camera opened, a capture session with
 *  the streams created and have capture requests submitting.
 */
@OptIn(ExperimentalCamera2Interop::class)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class UseCaseManager @Inject constructor(
    private val cameraPipe: CameraPipe,
    private val callbackMap: CameraCallbackMap,
    private val requestListener: ComboRequestListener,
    private val cameraConfig: CameraConfig,
    private val builder: UseCaseCameraComponent.Builder,
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // Java version required for Dagger
    private val controls: java.util.Set<UseCaseCameraControl>,
    private val camera2CameraControl: Camera2CameraControl,
    private val cameraStateAdapter: CameraStateAdapter,
    private val cameraQuirks: CameraQuirks,
    private val cameraGraphFlags: CameraGraph.Flags,
    private val cameraInternal: Provider<CameraInternal>,
    context: Context,
    cameraProperties: CameraProperties,
    displayInfoManager: DisplayInfoManager,
) {
    private val lock = Any()

    @GuardedBy("lock")
    private val attachedUseCases = mutableSetOf<UseCase>()

    @GuardedBy("lock")
    private val activeUseCases = mutableSetOf<UseCase>()

    @GuardedBy("lock")
    private var activeResumeEnabled = false

    @GuardedBy("lock")
    private var shouldCreateCameraGraphImmediately = true

    @GuardedBy("lock")
    private var deferredUseCaseManagerConfig: UseCaseManagerConfig? = null

    private val meteringRepeating by lazy {
        MeteringRepeating.Builder(
            cameraProperties,
            displayInfoManager
        ).build()
    }

    private val supportedSurfaceCombination by lazy {
        SupportedSurfaceCombination(
            context,
            cameraProperties.metadata,
            EncoderProfilesProviderAdapter(cameraConfig.cameraId.value)
        )
    }

    @Volatile
    private var _activeComponent: UseCaseCameraComponent? = null
    val camera: UseCaseCamera?
        get() = _activeComponent?.getUseCaseCamera()

    private val closingCameraJobs = mutableListOf<Job>()

    private val allControls = controls.toMutableSet().apply { add(camera2CameraControl) }

    internal fun setCameraGraphCreationMode(createImmediately: Boolean) = synchronized(lock) {
        shouldCreateCameraGraphImmediately = createImmediately
        if (shouldCreateCameraGraphImmediately) {
            // Clear the UseCaseManager configuration that haven't been "resumed" when we return
            // to single camera operating mode early.
            deferredUseCaseManagerConfig = null
        }
    }

    internal fun getDeferredCameraGraphConfig() = synchronized(lock) {
        deferredUseCaseManagerConfig?.cameraGraphConfig
    }

    /**
     * This attaches the specified [useCases] to the current set of attached use cases. When any
     * changes are identified (i.e., a new use case is added), the subsequent actions would trigger
     * a recreation of the current CameraGraph if there is one.
     */
    fun attach(useCases: List<UseCase>) = synchronized(lock) {
        if (useCases.isEmpty()) {
            Log.warn { "Attach [] from $this (Ignored)" }
            return
        }
        Log.debug { "Attaching $useCases from $this" }

        val unattachedUseCases = useCases.filter { useCase ->
            !attachedUseCases.contains(useCase)
        }

        // Notify state attached to use cases
        for (useCase in unattachedUseCases) {
            useCase.onStateAttached()
        }

        if (attachedUseCases.addAll(useCases)) {
            if (!addOrRemoveRepeatingUseCase(getRunningUseCases())) {
                refreshAttachedUseCases(attachedUseCases)
            }
        }

        unattachedUseCases.forEach { useCase ->
            // Notify CameraControl is ready after the UseCaseCamera is created
            useCase.onCameraControlReady()
        }
    }

    /**
     * This detaches the specified [useCases] from the current set of attached use cases. When any
     * changes are identified (i.e., an existing use case is removed), the subsequent actions would
     * trigger a recreation of the current CameraGraph.
     */
    fun detach(useCases: List<UseCase>) = synchronized(lock) {
        if (useCases.isEmpty()) {
            Log.warn { "Detaching [] from $this (Ignored)" }
            return
        }
        Log.debug { "Detaching $useCases from $this" }

        // When use cases are detached, they should be considered inactive as well. Also note that
        // we remove the use cases from our set directly because the subsequent cleanup actions from
        // detaching the use cases should suffice here.
        activeUseCases.removeAll(useCases)

        // Notify state detached to use cases
        for (useCase in useCases) {
            if (attachedUseCases.contains(useCase)) {
                useCase.onStateDetached()
            }
        }

        // TODO: We might only want to tear down when the number of attached use cases goes to
        //  zero. If a single UseCase is removed, we could deactivate it?
        if (attachedUseCases.removeAll(useCases)) {
            if (addOrRemoveRepeatingUseCase(getRunningUseCases())) {
                return
            }
            refreshAttachedUseCases(attachedUseCases)
        }
    }

    /**
     * This marks the specified [useCase] as active ("activate"). This refreshes the current set of
     * active use cases, and if any changes are identified, we update [UseCaseCamera] with the
     * latest set of "running" (attached and active) use cases, which will in turn trigger actions
     * for SessionConfig updates.
     */
    fun activate(useCase: UseCase) = synchronized(lock) {
        if (activeUseCases.add(useCase)) {
            refreshRunningUseCases()
        }
    }

    /**
     * This marks the specified [useCase] as inactive ("deactivate"). This refreshes the current set
     * of active use cases, and if any changes are identified, we update [UseCaseCamera] with the
     * latest set of "running" (attached and active) use cases, which will in turn trigger actions
     * for SessionConfig updates.
     */
    fun deactivate(useCase: UseCase) = synchronized(lock) {
        if (activeUseCases.remove(useCase)) {
            refreshRunningUseCases()
        }
    }

    fun update(useCase: UseCase) = synchronized(lock) {
        if (attachedUseCases.contains(useCase)) {
            refreshRunningUseCases()
        }
    }

    fun reset(useCase: UseCase) = synchronized(lock) {
        if (attachedUseCases.contains(useCase)) {
            refreshAttachedUseCases(attachedUseCases)
        }
    }

    fun setActiveResumeMode(enabled: Boolean) = synchronized(lock) {
        activeResumeEnabled = enabled
        camera?.setActiveResumeMode(enabled)
    }

    suspend fun close() {
        val closingJobs = synchronized(lock) {
            if (attachedUseCases.isNotEmpty()) {
                detach(attachedUseCases.toList())
            }
            meteringRepeating.onUnbind()
            closingCameraJobs.toList()
        }
        closingJobs.joinAll()
    }

    override fun toString(): String = "UseCaseManager<${cameraConfig.cameraId}>"

    @GuardedBy("lock")
    private fun refreshRunningUseCases() {
        val runningUseCases = getRunningUseCases()
        when {
            shouldAddRepeatingUseCase(runningUseCases) -> addRepeatingUseCase()
            shouldRemoveRepeatingUseCase(runningUseCases) -> removeRepeatingUseCase()
            else -> camera?.runningUseCases = runningUseCases
        }
    }

    @GuardedBy("lock")
    private fun refreshAttachedUseCases(newUseCases: Set<UseCase>) {
        val useCases = newUseCases.toList()

        // Close prior camera graph
        camera.let {
            _activeComponent = null
            it?.close()?.let { closingJob ->
                closingCameraJobs.add(closingJob)
                closingJob.invokeOnCompletion {
                    synchronized(lock) {
                        closingCameraJobs.remove(closingJob)
                    }
                }
            }
        }

        // Update list of active useCases
        if (useCases.isEmpty()) {
            for (control in allControls) {
                control.useCaseCamera = null
                control.reset()
            }
            return
        }

        val sessionConfigAdapter = SessionConfigAdapter(useCases)
        val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()

        val graphConfig = createCameraGraphConfig(
            sessionConfigAdapter, streamConfigMap, callbackMap,
            requestListener, cameraConfig, cameraQuirks, cameraGraphFlags
        )

        val useCaseManagerConfig = UseCaseManagerConfig(
            useCases,
            sessionConfigAdapter,
            graphConfig,
            streamConfigMap
        )
        if (!shouldCreateCameraGraphImmediately) {
            deferredUseCaseManagerConfig = useCaseManagerConfig
            return
        }
        val cameraGraph = cameraPipe.create(useCaseManagerConfig.cameraGraphConfig)
        beginComponentCreation(useCaseManagerConfig, cameraGraph)
    }

    internal fun resumeDeferredComponentCreation(cameraGraph: CameraGraph) {
        val config = synchronized(lock) { deferredUseCaseManagerConfig }
        checkNotNull(config)
        beginComponentCreation(config, cameraGraph)
    }

    private fun beginComponentCreation(
        useCaseManagerConfig: UseCaseManagerConfig,
        cameraGraph: CameraGraph
    ) {
        with(useCaseManagerConfig) {
            // Create and configure the new camera component.
            _activeComponent =
                builder.config(
                    UseCaseCameraConfig(
                        useCases,
                        sessionConfigAdapter,
                        cameraStateAdapter,
                        cameraGraph,
                        streamConfigMap
                    )
                )
                    .build()
            for (control in allControls) {
                control.useCaseCamera = camera
            }
            camera?.setActiveResumeMode(activeResumeEnabled)

            refreshRunningUseCases()
        }
    }

    @GuardedBy("lock")
    private fun getRunningUseCases(): Set<UseCase> {
        return attachedUseCases.intersect(activeUseCases)
    }

    /**
     * Adds or removes repeating use case if needed.
     *
     * @param runningUseCases the set of currently running use cases
     * @return true if repeating use cases is added or removed, false otherwise
     */
    @GuardedBy("lock")
    private fun addOrRemoveRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        if (shouldAddRepeatingUseCase(runningUseCases)) {
            addRepeatingUseCase()
            return true
        }
        if (shouldRemoveRepeatingUseCase(runningUseCases)) {
            removeRepeatingUseCase()
            return true
        }
        return false
    }

    @GuardedBy("lock")
    private fun shouldAddRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        val meteringRepeatingEnabled = attachedUseCases.contains(meteringRepeating)
        if (!meteringRepeatingEnabled) {
            val activeSurfaces = runningUseCases.withoutMetering().surfaceCount()
            return activeSurfaces > 0 && with(attachedUseCases.withoutMetering()) {
                (onlyVideoCapture() || requireMeteringRepeating()) && supportMeteringCombination()
            }
        }
        return false
    }

    @GuardedBy("lock")
    private fun addRepeatingUseCase() {
        meteringRepeating.bindToCamera(cameraInternal.get(), null, null)
        meteringRepeating.setupSession()
        attach(listOf(meteringRepeating))
        activate(meteringRepeating)
    }

    @GuardedBy("lock")
    private fun shouldRemoveRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        val meteringRepeatingEnabled = runningUseCases.contains(meteringRepeating)
        if (meteringRepeatingEnabled) {
            val activeSurfaces = runningUseCases.withoutMetering().surfaceCount()
            return activeSurfaces == 0 || with(attachedUseCases.withoutMetering()) {
                !(onlyVideoCapture() || requireMeteringRepeating()) || !supportMeteringCombination()
            }
        }
        return false
    }

    @GuardedBy("lock")
    private fun removeRepeatingUseCase() {
        deactivate(meteringRepeating)
        detach(listOf(meteringRepeating))
        meteringRepeating.unbindFromCamera(cameraInternal.get())
    }

    private fun Collection<UseCase>.onlyVideoCapture(): Boolean {
        return isNotEmpty() && checkSurfaces { _, sessionSurfaces ->
            sessionSurfaces.isNotEmpty() && sessionSurfaces.all {
                it.containerClass == MediaCodec::class.java
            }
        }
    }

    private fun Collection<UseCase>.supportMeteringCombination(): Boolean {
        val useCases = this.toMutableList().apply { add(meteringRepeating) }
        if (meteringRepeating.attachedSurfaceResolution == null) {
            meteringRepeating.setupSession()
        }
        return isCombinationSupported(useCases).also {
            Log.debug { "Combination of $useCases is supported: $it" }
        }
    }

    private fun isCombinationSupported(currentUseCases: Collection<UseCase>): Boolean {
        val surfaceConfigs = currentUseCases.map { useCase ->
            // TODO: Test with correct Camera Mode when concurrent mode / ultra high resolution is
            //  implemented.
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                useCase.imageFormat,
                useCase.attachedSurfaceResolution!!
            )
        }

        var isPreviewStabilizationOn = false
        for (useCase in currentUseCases) {
            if (useCase.currentConfig is PreviewConfig) {
                isPreviewStabilizationOn =
                    useCase.currentConfig.previewStabilizationMode == StabilizationMode.ON
            }
        }

        return supportedSurfaceCombination.checkSupported(
            SupportedSurfaceCombination.FeatureSettings(
                CameraMode.DEFAULT,
                DynamicRange.BIT_DEPTH_8_BIT,
                isPreviewStabilizationOn
            ), surfaceConfigs
        )
    }

    private fun Collection<UseCase>.surfaceCount(): Int =
        ValidatingBuilder().let { validatingBuilder ->
            forEach { useCase -> validatingBuilder.add(useCase.sessionConfig) }
            return validatingBuilder.build().surfaces.size
        }

    private fun Collection<UseCase>.withoutMetering(): Collection<UseCase> =
        filterNot { it is MeteringRepeating }

    private fun Collection<UseCase>.requireMeteringRepeating(): Boolean {
        return isNotEmpty() && checkSurfaces { repeatingSurfaces, sessionSurfaces ->
            // There is no repeating UseCases
            sessionSurfaces.isNotEmpty() && repeatingSurfaces.isEmpty()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun Collection<UseCase>.checkSurfaces(
        predicate: (
            repeatingSurfaces: List<DeferrableSurface>,
            sessionSurfaces: List<DeferrableSurface>
        ) -> Boolean
    ): Boolean = ValidatingBuilder().let { validatingBuilder ->
        forEach { useCase -> validatingBuilder.add(useCase.sessionConfig) }
        val sessionConfig = validatingBuilder.build()
        val captureConfig = sessionConfig.repeatingCaptureConfig
        return predicate(captureConfig.surfaces, sessionConfig.surfaces)
    }

    companion object {
        internal data class UseCaseManagerConfig(
            val useCases: List<UseCase>,
            val sessionConfigAdapter: SessionConfigAdapter,
            val cameraGraphConfig: CameraGraph.Config,
            val streamConfigMap: MutableMap<CameraStream.Config, DeferrableSurface>
        )

        fun SessionConfig.toCamera2ImplConfig(): Camera2ImplConfig {
            return Camera2ImplConfig(implementationOptions)
        }

        fun createCameraGraphConfig(
            sessionConfigAdapter: SessionConfigAdapter,
            streamConfigMap: MutableMap<CameraStream.Config, DeferrableSurface>,
            callbackMap: CameraCallbackMap,
            requestListener: ComboRequestListener,
            cameraConfig: CameraConfig,
            cameraQuirks: CameraQuirks,
            cameraGraphFlags: CameraGraph.Flags?,
        ): CameraGraph.Config {
            var containsVideo = false
            // TODO: This may need to combine outputs that are (or will) share the same output
            //  imageReader or surface.
            sessionConfigAdapter.getValidSessionConfigOrNull()?.let { sessionConfig ->
                sessionConfig.surfaces.forEach { deferrableSurface ->
                    val outputConfig = CameraStream.Config.create(
                        streamUseCase = getStreamUseCase(
                            deferrableSurface,
                            sessionConfigAdapter.surfaceToStreamUseCaseMap
                        ),
                        streamUseHint = getStreamUseHint(
                            deferrableSurface,
                            sessionConfigAdapter.surfaceToStreamUseHintMap
                        ),
                        size = deferrableSurface.prescribedSize,
                        format = StreamFormat(deferrableSurface.prescribedStreamFormat),
                        camera = CameraId(
                            sessionConfig.toCamera2ImplConfig().getPhysicalCameraId(
                                cameraConfig.cameraId.value
                            )!!
                        )
                    )
                    streamConfigMap[outputConfig] = deferrableSurface
                    Log.debug {
                        "Prepare config for: $deferrableSurface (" +
                            "${deferrableSurface.prescribedSize}," +
                            " ${deferrableSurface.prescribedStreamFormat})"
                    }
                    if (deferrableSurface.containerClass == MediaCodec::class.java) {
                        containsVideo = true
                    }
                }
            }
            val shouldCloseCaptureSessionOnDisconnect =
                if (CameraQuirks.isImmediateSurfaceReleaseAllowed()) {
                    // If we can release Surfaces immediately, we'll finalize the session when the
                    // camera graph is closed (through FinalizeSessionOnCloseQuirk), and thus we won't
                    // need to explicitly close the capture session.
                    false
                } else {
                    if (cameraQuirks.quirks.contains(CloseCaptureSessionOnVideoQuirk::class.java) &&
                        containsVideo
                    ) {
                        true
                    } else
                    // TODO(b/277675483): From the current test results, older devices (Android
                    //  version <= 8.1.0) seem to have a higher chance of encountering an issue where
                    //  not closing the capture session would lead to CameraDevice.close stalling
                    //  indefinitely. This version check might need to be further fine-turned down the
                    //  line.
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1
                }
            val shouldCloseCameraDeviceOnClose =
                DeviceQuirks[CloseCameraDeviceOnCameraGraphCloseQuirk::class.java] != null

            val combinedFlags = cameraGraphFlags?.copy(
                quirkCloseCaptureSessionOnDisconnect = shouldCloseCaptureSessionOnDisconnect,
                quirkCloseCameraDeviceOnClose = shouldCloseCameraDeviceOnClose,
            )
                ?: CameraGraph.Flags(
                    quirkCloseCaptureSessionOnDisconnect = shouldCloseCaptureSessionOnDisconnect,
                    quirkCloseCameraDeviceOnClose = shouldCloseCameraDeviceOnClose,
                )

            // Set video stabilization mode to capture request
            var videoStabilizationMode = CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            if (sessionConfigAdapter.getValidSessionConfigOrNull() != null) {
                val config = sessionConfigAdapter
                    .getValidSessionConfigOrNull()!!
                    .repeatingCaptureConfig
                val isPreviewStabilizationMode = config.previewStabilizationMode
                val isVideoStabilizationMode = config.videoStabilizationMode

                if (isPreviewStabilizationMode == StabilizationMode.OFF ||
                    isVideoStabilizationMode == StabilizationMode.OFF) {
                    videoStabilizationMode = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                } else if (isPreviewStabilizationMode == StabilizationMode.ON) {
                    videoStabilizationMode =
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                } else if (isVideoStabilizationMode == StabilizationMode.ON) {
                    videoStabilizationMode = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                }
            }

            // Build up a config (using TEMPLATE_PREVIEW by default)
            return CameraGraph.Config(
                camera = cameraConfig.cameraId,
                streams = streamConfigMap.keys.toList(),
                defaultListeners = listOf(callbackMap, requestListener),
                defaultParameters = mapOf(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE to videoStabilizationMode
                ),
                flags = combinedFlags,
            )
        }

        private fun getStreamUseCase(
            deferrableSurface: DeferrableSurface,
            mapping: Map<DeferrableSurface, Long>
        ): OutputStream.StreamUseCase? {
            return mapping[deferrableSurface]?.let { OutputStream.StreamUseCase(it) }
        }

        private fun getStreamUseHint(
            deferrableSurface: DeferrableSurface,
            mapping: Map<DeferrableSurface, Long>
        ): OutputStream.StreamUseHint? {
            return mapping[deferrableSurface]?.let { OutputStream.StreamUseHint(it) }
        }
    }
}
