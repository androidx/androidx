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

package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.media.MediaCodec
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode
import androidx.camera.camera2.pipe.CameraGraph.RepeatingRequestRequirementsBeforeCapture.CompletionBehavior.AT_LEAST
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.InputStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.compat.CameraPipeKeys
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.EncoderProfilesProviderAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.pipe.integration.adapter.ZslControl
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CaptureSessionStuckQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.CloseCameraDeviceOnCameraGraphCloseQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.CloseCaptureSessionOnDisconnectQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.CloseCaptureSessionOnVideoQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.ConfigureSurfaceToSecondarySessionFailQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.DisableAbortCapturesOnStopWithSessionProcessorQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.FinalizeSessionOnCloseQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.PreviewOrientationIncorrectQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.QuickSuccessiveImageCaptureFailsRepeatingRequestQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.TextureViewIsClosedQuirk
import androidx.camera.camera2.pipe.integration.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraComponent
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.internal.DynamicRangeResolver
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.OutputConfig.SURFACE_GROUP_ID_NONE
import androidx.camera.core.impl.SessionConfig.ValidatingBuilder
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.core.streamsharing.StreamSharingConfig
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly

/**
 * This class keeps track of the currently attached and active [UseCase]'s for a specific camera. A
 * [UseCase] during its lifetime, can be:
 * - Attached: This happens when a use case is bound to a CameraX Lifecycle, and signals that the
 *   camera should be opened, and a camera capture session should be created to include the stream
 *   corresponding to the use case. In the integration layer here, we'll recreate a CameraGraph when
 *   a use case is attached.
 * - Detached: This happens when a use case is unbound from a CameraX Lifecycle, and signals that we
 *   no longer need this specific use case and therefore its corresponding stream in our current
 *   capture session. In the integration layer, we'll also recreate a CameraGraph when a use case is
 *   detached, though it might not be strictly necessary.
 * - Active: This happens when the use case is considered "ready", meaning that the use case is
 *   ready to have frames delivered to it. In the case of the integration layer, this means we can
 *   start submitting the capture requests corresponding to the use case. An important note here is
 *   that a use case can actually become "active" before it is "attached", and thus we should only
 *   take action when a use case is both "attached" and "active".
 * - Inactive: This happens when use case no longer needs frames delivered to it. This is can be
 *   seen as an optimization signal, as we technically are allowed to continue submitting capture
 *   requests, but we no longer need to. An example of this is when you clear the analyzer during
 *   ImageAnalysis.
 *
 * In this class, we also define a new term - "Running". A use case is considered running when it's
 * both "attached" and "active". This means we should have a camera opened, a capture session with
 * the streams created and have capture requests submitting.
 */
@OptIn(ExperimentalCamera2Interop::class)
@CameraScope
public class UseCaseManager
@Inject
constructor(
    private val cameraPipe: CameraPipe,
    private val cameraDevices: CameraDevices,
    @GuardedBy("lock") private val cameraCoordinator: CameraCoordinator,
    private val callbackMap: CameraCallbackMap,
    private val requestListener: ComboRequestListener,
    private val cameraConfig: CameraConfig,
    private val builder: UseCaseCameraComponent.Builder,
    private val zslControl: ZslControl,
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // Java version required for Dagger
    private val controls: java.util.Set<UseCaseCameraControl>,
    private val camera2CameraControl: Camera2CameraControl,
    private val cameraStateAdapter: CameraStateAdapter,
    private val cameraQuirks: CameraQuirks,
    private val cameraInternal: Provider<CameraInternal>,
    private val useCaseThreads: Provider<UseCaseThreads>,
    private val cameraInfoInternal: Provider<CameraInfoInternal>,
    private val templateParamsOverride: TemplateParamsOverride,
    context: Context,
    cameraProperties: CameraProperties,
    displayInfoManager: DisplayInfoManager,
) {
    private val lock = Any()

    internal var sessionProcessor: SessionProcessor? = null
        get() =
            synchronized(lock) {
                return field
            }
        set(value) = synchronized(lock) { field = value }

    @GuardedBy("lock") private var sessionProcessorManager: SessionProcessorManager? = null

    @GuardedBy("lock") private val attachedUseCases = mutableSetOf<UseCase>()

    @GuardedBy("lock") private val activeUseCases = mutableSetOf<UseCase>()

    @GuardedBy("lock") private var activeResumeEnabled = false

    @GuardedBy("lock") private var shouldCreateCameraGraphImmediately = true

    @GuardedBy("lock") private var deferredUseCaseManagerConfig: UseCaseManagerConfig? = null

    @GuardedBy("lock") private var pendingSessionProcessorInitialization = false

    @GuardedBy("lock") private var isPrimary = true

    @GuardedBy("lock")
    private val pendingUseCasesToNotifyCameraControlReady = mutableSetOf<UseCase>()

    private val meteringRepeating by lazy {
        MeteringRepeating.Builder(cameraProperties, displayInfoManager).build()
    }

    private val supportedSurfaceCombination by lazy {
        SupportedSurfaceCombination(
            context,
            cameraProperties.metadata,
            EncoderProfilesProviderAdapter(cameraConfig.cameraId.value, cameraQuirks.quirks)
        )
    }

    private val dynamicRangeResolver = DynamicRangeResolver(cameraProperties.metadata)

    @Volatile private var _activeComponent: UseCaseCameraComponent? = null
    public val camera: UseCaseCamera?
        get() = _activeComponent?.getUseCaseCamera()

    public val useCaseGraphConfig: UseCaseGraphConfig?
        get() = _activeComponent?.getUseCaseGraphConfig()

    private val closingCameraJobs = mutableListOf<Job>()

    private val allControls = controls.toMutableSet().apply { add(camera2CameraControl) }

    internal fun setCameraGraphCreationMode(createImmediately: Boolean) =
        synchronized(lock) {
            shouldCreateCameraGraphImmediately = createImmediately
            if (shouldCreateCameraGraphImmediately) {
                // Clear the UseCaseManager configuration that haven't been "resumed" when we return
                // to single camera operating mode early.
                deferredUseCaseManagerConfig = null
            }
        }

    internal fun getDeferredCameraGraphConfig() =
        synchronized(lock) { deferredUseCaseManagerConfig?.cameraGraphConfig }

    /**
     * This attaches the specified [useCases] to the current set of attached use cases. When any
     * changes are identified (i.e., a new use case is added), the subsequent actions would trigger
     * a recreation of the current CameraGraph if there is one.
     */
    public fun attach(useCases: List<UseCase>): Unit =
        synchronized(lock) {
            if (useCases.isEmpty()) {
                Log.warn { "Attach [] from $this (Ignored)" }
                return
            }
            Log.debug { "Attaching $useCases from $this" }

            val unattachedUseCases =
                useCases.filter { useCase -> !attachedUseCases.contains(useCase) }

            // Notify state attached to use cases
            for (useCase in unattachedUseCases) {
                useCase.onStateAttached()
            }

            if (attachedUseCases.addAll(useCases)) {
                if (!addOrRemoveRepeatingUseCase(getRunningUseCases())) {
                    updateZslDisabledByUseCaseConfigStatus()
                    refreshAttachedUseCases(attachedUseCases)
                }
            }

            if (sessionProcessor != null || !shouldCreateCameraGraphImmediately) {
                pendingUseCasesToNotifyCameraControlReady.addAll(unattachedUseCases)
            } else {
                unattachedUseCases.forEach { useCase ->
                    // Notify CameraControl is ready after the UseCaseCamera is created
                    useCase.onCameraControlReady()
                }
            }
        }

    /**
     * This detaches the specified [useCases] from the current set of attached use cases. When any
     * changes are identified (i.e., an existing use case is removed), the subsequent actions would
     * trigger a recreation of the current CameraGraph.
     */
    public fun detach(useCases: List<UseCase>): Unit =
        synchronized(lock) {
            if (useCases.isEmpty()) {
                Log.warn { "Detaching [] from $this (Ignored)" }
                return
            }
            Log.debug { "Detaching $useCases from $this" }

            // When use cases are detached, they should be considered inactive as well. Also note
            // that
            // we remove the use cases from our set directly because the subsequent cleanup actions
            // from
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

                if (attachedUseCases.isEmpty()) {
                    zslControl.setZslDisabledByUserCaseConfig(false)
                } else {
                    updateZslDisabledByUseCaseConfigStatus()
                }
                refreshAttachedUseCases(attachedUseCases)
            }
            pendingUseCasesToNotifyCameraControlReady.removeAll(useCases)
        }

    /**
     * This marks the specified [useCase] as active ("activate"). This refreshes the current set of
     * active use cases, and if any changes are identified, we update [UseCaseCamera] with the
     * latest set of "running" (attached and active) use cases, which will in turn trigger actions
     * for SessionConfig updates.
     */
    public fun activate(useCase: UseCase): Unit =
        synchronized(lock) {
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
    public fun deactivate(useCase: UseCase): Unit =
        synchronized(lock) {
            if (activeUseCases.remove(useCase)) {
                refreshRunningUseCases()
            }
        }

    public fun update(useCase: UseCase): Unit =
        synchronized(lock) {
            if (attachedUseCases.contains(useCase)) {
                refreshRunningUseCases()
            }
        }

    public fun reset(useCase: UseCase): Unit =
        synchronized(lock) {
            if (attachedUseCases.contains(useCase)) {
                refreshAttachedUseCases(attachedUseCases)
            }
        }

    public fun setPrimary(isPrimary: Boolean) {
        synchronized(lock) { this.isPrimary = isPrimary }
    }

    public fun setActiveResumeMode(enabled: Boolean): Unit? =
        synchronized(lock) {
            activeResumeEnabled = enabled
            camera?.setActiveResumeMode(enabled)
        }

    public suspend fun close() {
        val closingJobs =
            synchronized(lock) {
                if (attachedUseCases.isNotEmpty()) {
                    detach(attachedUseCases.toList())
                }
                meteringRepeating.onUnbind()
                closingCameraJobs.toList()
            }
        closingJobs.joinAll()
        cameraDevices.disconnectAll()
    }

    override fun toString(): String = "UseCaseManager<${cameraConfig.cameraId}>"

    @GuardedBy("lock")
    private fun refreshRunningUseCases() {
        // refreshRunningUseCases() is called after we activate, deactivate, update or have finished
        // attaching use cases. If the SessionProcessor is still being initialized, we cannot
        // refresh the current set of running use cases (we don't have a UseCaseCamera), but we
        // can safely abort here, because once the SessionProcessor is initialized, we'll resume
        // the process of creating UseCaseCamera components, finish attaching use cases and finally
        // invoke refreshingRunningUseCases().
        if (pendingSessionProcessorInitialization) return
        val runningUseCases = getRunningUseCases()
        when {
            shouldAddRepeatingUseCase(runningUseCases) -> addRepeatingUseCase()
            shouldRemoveRepeatingUseCase(runningUseCases) -> removeRepeatingUseCase()
            else -> {
                camera?.let {
                    it.updateRepeatingRequests(isPrimary, runningUseCases)
                    for (control in allControls) {
                        if (control is RunningUseCasesChangeListener) {
                            control.onRunningUseCasesChanged(runningUseCases)
                        }
                    }
                }
            }
        }
    }

    private fun UseCaseCamera.updateRepeatingRequests(
        isPrimary: Boolean,
        runningUseCases: Set<UseCase>
    ) {
        // Note: This may be called with the same set of values that was previously set. This
        // is used as a signal to indicate the properties of the UseCase may have changed.
        SessionConfigAdapter(runningUseCases, isPrimary = isPrimary)
            .getValidSessionConfigOrNull()
            ?.let { requestControl.setSessionConfigAsync(it) }
            ?: run {
                Log.debug { "Unable to reset the session due to invalid config" }
                requestControl.setSessionConfigAsync(
                    SessionConfig.Builder().apply { setTemplateType(defaultTemplate) }.build()
                )
            }
    }

    private fun UseCaseCameraRequestControl.setSessionConfigAsync(
        sessionConfig: SessionConfig
    ): Deferred<Unit> =
        setConfigAsync(
            type = UseCaseCameraRequestControl.Type.SESSION_CONFIG,
            config = sessionConfig.implementationOptions,
            tags = sessionConfig.repeatingCaptureConfig.tagBundle.toMap(),
            listeners =
                setOf(
                    CameraCallbackMap.createFor(
                        sessionConfig.repeatingCameraCaptureCallbacks,
                        useCaseThreads.get().backgroundExecutor
                    )
                ),
            template = RequestTemplate(sessionConfig.repeatingCaptureConfig.templateType),
            streams =
                useCaseGraphConfig?.getStreamIdsFromSurfaces(
                    sessionConfig.repeatingCaptureConfig.surfaces
                ),
            sessionConfig = sessionConfig,
        )

    @GuardedBy("lock")
    private fun refreshAttachedUseCases(newUseCases: Set<UseCase>) {
        val useCases = newUseCases.toList()

        // Close prior camera graph
        camera.let { useCaseCamera ->
            _activeComponent = null
            useCaseCamera?.close()?.let { closingJob ->
                if (sessionProcessorManager != null) {
                    // If the current session was created for extensions. We need to make sure
                    // the closing procedures are done. This is needed because the same
                    // SessionProcessor instance may be reused in the next extensions session, and
                    // we need to make sure we de-initialize the current SessionProcessor session.
                    runBlocking { closingJob.join() }
                } else {
                    closingCameraJobs.add(closingJob)
                    closingJob.invokeOnCompletion {
                        synchronized(lock) { closingCameraJobs.remove(closingJob) }
                    }
                }
            }
        }
        sessionProcessorManager?.let {
            it.close()
            sessionProcessorManager = null
            pendingSessionProcessorInitialization = false
        }

        // Update list of active useCases
        if (useCases.isEmpty()) {
            for (control in allControls) {
                control.requestControl = null
                control.reset()
            }
            return
        }

        if (sessionProcessor != null || !shouldCreateCameraGraphImmediately) {
            // We will need to set the UseCaseCamera to null since the new UseCaseCamera along with
            // its respective CameraGraph configurations won't be ready until:
            //
            // - SessionProcessorManager finishes the initialization, _acquires the lock_, and
            //    resume UseCaseManager successfully
            // - And/or, the UseCaseManager is ready to be resumed under concurrent camera settings.
            for (control in allControls) {
                control.requestControl = null
            }
        }

        if (sessionProcessor != null) {
            Log.debug { "Setting up UseCaseManager with SessionProcessorManager" }
            sessionProcessorManager =
                SessionProcessorManager(
                        sessionProcessor!!,
                        cameraInfoInternal.get(),
                        useCaseThreads.get().scope,
                    )
                    .also { manager ->
                        pendingSessionProcessorInitialization = true
                        manager.initialize(this, useCases) { config ->
                            synchronized(lock) {
                                if (manager.isClosed()) {
                                    // We've been cancelled by other use case transactions. This
                                    // means the
                                    // attached set of use cases have been updated in the meantime,
                                    // and the
                                    // UseCaseManagerConfig we have here is obsolete, so we can
                                    // simply abort
                                    // here.
                                    return@initialize
                                }
                                if (config == null) {
                                    Log.error { "Failed to initialize SessionProcessor" }
                                    manager.close()
                                    sessionProcessorManager = null
                                    return@initialize
                                }
                                pendingSessionProcessorInitialization = false
                                this@UseCaseManager.tryResumeUseCaseManager(config)
                            }
                        }
                    }
            return
        } else {
            val sessionConfigAdapter = SessionConfigAdapter(useCases, isPrimary = isPrimary)
            val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()
            val graphConfig = createCameraGraphConfig(sessionConfigAdapter, streamConfigMap)

            val useCaseManagerConfig =
                UseCaseManagerConfig(useCases, sessionConfigAdapter, graphConfig, streamConfigMap)
            this.tryResumeUseCaseManager(useCaseManagerConfig)
        }
    }

    @VisibleForTesting
    @GuardedBy("lock")
    internal fun tryResumeUseCaseManager(useCaseManagerConfig: UseCaseManagerConfig) {
        if (!shouldCreateCameraGraphImmediately) {
            deferredUseCaseManagerConfig = useCaseManagerConfig
            return
        }
        val cameraGraph = cameraPipe.create(useCaseManagerConfig.cameraGraphConfig)
        beginComponentCreation(useCaseManagerConfig, cameraGraph)
    }

    internal fun resumeDeferredComponentCreation(cameraGraph: CameraGraph) =
        synchronized(lock) {
            beginComponentCreation(checkNotNull(deferredUseCaseManagerConfig), cameraGraph)
        }

    @GuardedBy("lock")
    private fun beginComponentCreation(
        useCaseManagerConfig: UseCaseManagerConfig,
        cameraGraph: CameraGraph
    ) {
        val sessionProcessorEnabled =
            useCaseManagerConfig.sessionConfigAdapter.isSessionProcessorEnabled
        with(useCaseManagerConfig) {
            if (sessionProcessorEnabled) {
                for ((streamConfig, deferrableSurface) in streamConfigMap) {
                    cameraGraph.streams[streamConfig]?.let {
                        cameraGraph.setSurface(it.id, deferrableSurface.surface.get())
                    }
                }
            }

            // Create and configure the new camera component.
            _activeComponent =
                builder
                    .config(
                        UseCaseCameraConfig(
                            useCases,
                            sessionConfigAdapter,
                            cameraStateAdapter,
                            cameraGraph,
                            streamConfigMap,
                            sessionProcessorManager,
                        )
                    )
                    .build()

            for (control in allControls) {
                control.requestControl = camera?.requestControl
            }

            camera?.setActiveResumeMode(activeResumeEnabled)

            refreshRunningUseCases()
        }

        Log.debug { "Notifying $pendingUseCasesToNotifyCameraControlReady camera control ready" }
        for (useCase in pendingUseCasesToNotifyCameraControlReady) {
            useCase.onCameraControlReady()
        }
        pendingUseCasesToNotifyCameraControlReady.clear()
    }

    @GuardedBy("lock")
    private fun getRunningUseCases(): Set<UseCase> {
        return attachedUseCases.intersect(activeUseCases)
    }

    @TestOnly
    @VisibleForTesting
    public fun getRunningUseCasesForTest(): Set<UseCase> =
        synchronized(lock) {
            return getRunningUseCases()
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
            return activeSurfaces > 0 &&
                with(attachedUseCases.withoutMetering()) {
                    (onlyVideoCapture() || requireMeteringRepeating()) &&
                        isMeteringCombinationSupported()
                }
        }
        return false
    }

    @GuardedBy("lock")
    private fun addRepeatingUseCase() {
        meteringRepeating.bindToCamera(cameraInternal.get(), null, null, null)
        meteringRepeating.setupSession()
        attach(listOf(meteringRepeating))
        activate(meteringRepeating)
    }

    @GuardedBy("lock")
    private fun shouldRemoveRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        val meteringRepeatingEnabled = runningUseCases.contains(meteringRepeating)
        if (meteringRepeatingEnabled) {
            val activeSurfaces = runningUseCases.withoutMetering().surfaceCount()
            return activeSurfaces == 0 ||
                with(attachedUseCases.withoutMetering()) {
                    !(onlyVideoCapture() || requireMeteringRepeating()) ||
                        !isMeteringCombinationSupported()
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

    internal fun createCameraGraphConfig(
        sessionConfigAdapter: SessionConfigAdapter,
        streamConfigMap: MutableMap<CameraStream.Config, DeferrableSurface>,
        isExtensions: Boolean = false,
    ): CameraGraph.Config {
        return Companion.createCameraGraphConfig(
            sessionConfigAdapter,
            streamConfigMap,
            callbackMap,
            requestListener,
            cameraConfig,
            cameraQuirks,
            zslControl,
            templateParamsOverride,
            isExtensions,
        )
    }

    private fun Collection<UseCase>.onlyVideoCapture(): Boolean {
        return isNotEmpty() &&
            checkSurfaces { _, sessionSurfaces ->
                sessionSurfaces.isNotEmpty() &&
                    sessionSurfaces.all { it.containerClass == MediaCodec::class.java }
            }
    }

    private fun Collection<UseCase>.isMeteringCombinationSupported(): Boolean {
        if (meteringRepeating.attachedSurfaceResolution == null) {
            meteringRepeating.setupSession()
        }

        val attachedSurfaceInfoList = getAttachedSurfaceInfoList()

        if (attachedSurfaceInfoList.isEmpty()) {
            return false
        }

        val sessionSurfacesConfigs = getSessionSurfacesConfigs()

        return supportedSurfaceCombination
            .checkSupported(
                SupportedSurfaceCombination.FeatureSettings(
                    getCameraMode(),
                    getRequiredMaxBitDepth(attachedSurfaceInfoList),
                    isPreviewStabilizationOn(),
                    isUltraHdrOn()
                ),
                mutableListOf<SurfaceConfig>().apply {
                    addAll(sessionSurfacesConfigs)
                    add(createMeteringRepeatingSurfaceConfig())
                }
            )
            .also {
                Log.debug {
                    "Combination of $sessionSurfacesConfigs + $meteringRepeating is supported: $it"
                }
            }
    }

    private fun getCameraMode(): Int {
        synchronized(lock) {
            if (
                cameraCoordinator.cameraOperatingMode ==
                    CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
            ) {
                return CameraMode.CONCURRENT_CAMERA
            }
        }

        return CameraMode.DEFAULT
    }

    private fun getRequiredMaxBitDepth(attachedSurfaceInfoList: List<AttachedSurfaceInfo>): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dynamicRangeResolver
                .resolveAndValidateDynamicRanges(
                    attachedSurfaceInfoList,
                    listOf(meteringRepeating.currentConfig),
                    listOf(0)
                )
                .forEach { (_, u) ->
                    if (u.bitDepth == DynamicRange.BIT_DEPTH_10_BIT) {
                        return DynamicRange.BIT_DEPTH_10_BIT
                    }
                }
        }

        return DynamicRange.BIT_DEPTH_8_BIT
    }

    private fun Collection<UseCase>.getAttachedSurfaceInfoList(): List<AttachedSurfaceInfo> =
        mutableListOf<AttachedSurfaceInfo>().apply {
            this@getAttachedSurfaceInfoList.forEach { useCase ->
                val surfaceResolution = useCase.attachedSurfaceResolution
                val streamSpec = useCase.attachedStreamSpec

                // When collecting the info, the UseCases might be unbound to make these info
                // become null.
                if (surfaceResolution == null || streamSpec == null) {
                    Log.warn { "Invalid surface resolution or stream spec is found." }
                    clear()
                    return@apply
                }

                val surfaceConfig =
                    supportedSurfaceCombination.transformSurfaceConfig(
                        getCameraMode(),
                        useCase.currentConfig.inputFormat,
                        surfaceResolution
                    )
                add(
                    AttachedSurfaceInfo.create(
                        surfaceConfig,
                        useCase.currentConfig.inputFormat,
                        surfaceResolution,
                        streamSpec.dynamicRange,
                        useCase.getCaptureTypes(),
                        streamSpec.implementationOptions ?: MutableOptionsBundle.create(),
                        useCase.currentConfig.getTargetFrameRate(null)
                    )
                )
            }
        }

    private fun UseCase.getCaptureTypes() =
        if (this is StreamSharing) {
            (currentConfig as StreamSharingConfig).captureTypes
        } else {
            listOf(currentConfig.captureType)
        }

    private fun Collection<UseCase>.isPreviewStabilizationOn() =
        filterIsInstance<Preview>().firstOrNull()?.currentConfig?.previewStabilizationMode ==
            StabilizationMode.ON

    private fun Collection<UseCase>.isUltraHdrOn() =
        filterIsInstance<ImageCapture>().firstOrNull()?.currentConfig?.inputFormat ==
            ImageFormat.JPEG_R

    private fun Collection<UseCase>.getSessionSurfacesConfigs(): List<SurfaceConfig> =
        mutableListOf<SurfaceConfig>().apply {
            this@getSessionSurfacesConfigs.forEach { useCase ->
                useCase.sessionConfig.surfaces.forEach { deferrableSurface ->
                    add(
                        supportedSurfaceCombination.transformSurfaceConfig(
                            getCameraMode(),
                            useCase.currentConfig.inputFormat,
                            deferrableSurface.prescribedSize
                        )
                    )
                }
            }
        }

    private fun createMeteringRepeatingSurfaceConfig() =
        supportedSurfaceCombination.transformSurfaceConfig(
            getCameraMode(),
            meteringRepeating.imageFormat,
            meteringRepeating.attachedSurfaceResolution!!
        )

    private fun Collection<UseCase>.surfaceCount(): Int =
        ValidatingBuilder().let { validatingBuilder ->
            forEach { useCase -> validatingBuilder.add(useCase.sessionConfig) }
            return validatingBuilder.build().surfaces.size
        }

    private fun Collection<UseCase>.withoutMetering(): Collection<UseCase> = filterNot {
        it is MeteringRepeating
    }

    private fun Collection<UseCase>.requireMeteringRepeating(): Boolean {
        return isNotEmpty() &&
            checkSurfaces { repeatingSurfaces, sessionSurfaces ->
                // There is no repeating UseCases
                sessionSurfaces.isNotEmpty() && repeatingSurfaces.isEmpty()
            }
    }

    private fun Collection<UseCase>.checkSurfaces(
        predicate:
            (
                repeatingSurfaces: List<DeferrableSurface>, sessionSurfaces: List<DeferrableSurface>
            ) -> Boolean
    ): Boolean =
        ValidatingBuilder().let { validatingBuilder ->
            forEach { useCase -> validatingBuilder.add(useCase.sessionConfig) }
            val sessionConfig = validatingBuilder.build()
            val captureConfig = sessionConfig.repeatingCaptureConfig
            return predicate(captureConfig.surfaces, sessionConfig.surfaces)
        }

    private fun updateZslDisabledByUseCaseConfigStatus() {
        val disableZsl = attachedUseCases.any { it.currentConfig.isZslDisabled(false) }
        zslControl.setZslDisabledByUserCaseConfig(disableZsl)
    }

    /**
     * This interface defines a listener that is notified when the set of running UseCases changes.
     *
     * A "running" UseCase is one that is both attached and active, meaning it's bound to the
     * lifecycle and ready to receive camera frames.
     *
     * Classes implementing this interface can take action when the active UseCase configuration
     * changes.
     */
    public interface RunningUseCasesChangeListener {

        /**
         * Invoked when the set of running UseCases has been modified (added, removed, or updated).
         *
         * @param runningUseCases The updated set of UseCases that are currently running.
         */
        public fun onRunningUseCasesChanged(runningUseCases: Set<UseCase>)
    }

    public companion object {
        internal data class UseCaseManagerConfig(
            val useCases: List<UseCase>,
            val sessionConfigAdapter: SessionConfigAdapter,
            val cameraGraphConfig: CameraGraph.Config,
            val streamConfigMap: MutableMap<CameraStream.Config, DeferrableSurface>
        )

        public fun SessionConfig.toCamera2ImplConfig(): Camera2ImplConfig {
            return Camera2ImplConfig(implementationOptions)
        }

        public fun createCameraGraphConfig(
            sessionConfigAdapter: SessionConfigAdapter,
            streamConfigMap: MutableMap<CameraStream.Config, DeferrableSurface>,
            callbackMap: CameraCallbackMap,
            requestListener: ComboRequestListener,
            cameraConfig: CameraConfig,
            cameraQuirks: CameraQuirks,
            zslControl: ZslControl,
            templateParamsOverride: TemplateParamsOverride,
            isExtensions: Boolean = false,
        ): CameraGraph.Config {
            var containsVideo = false
            var operatingMode = OperatingMode.NORMAL
            val streamGroupMap = mutableMapOf<Int, MutableList<CameraStream.Config>>()
            val inputStreams = mutableListOf<InputStream.Config>()
            var sessionTemplate = RequestTemplate(TEMPLATE_PREVIEW)
            val sessionParameters: MutableMap<CaptureRequest.Key<*>, Any> = mutableMapOf()
            sessionConfigAdapter.getValidSessionConfigOrNull()?.let { sessionConfig ->
                operatingMode =
                    when (sessionConfig.sessionType) {
                        SESSION_REGULAR -> OperatingMode.NORMAL
                        SESSION_HIGH_SPEED -> OperatingMode.HIGH_SPEED
                        else -> OperatingMode.custom(sessionConfig.sessionType)
                    }

                if (sessionConfig.templateType != CaptureConfig.TEMPLATE_TYPE_NONE) {
                    sessionTemplate = RequestTemplate(sessionConfig.templateType)
                }
                sessionParameters.putAll(templateParamsOverride.getOverrideParams(sessionTemplate))
                sessionParameters.putAll(sessionConfig.implementationOptions.toParameters())

                val physicalCameraIdForAllStreams =
                    sessionConfig.toCamera2ImplConfig().getPhysicalCameraId(null)
                var zslStream: CameraStream.Config? = null
                for (outputConfig in sessionConfig.outputConfigs) {
                    val deferrableSurface = outputConfig.surface
                    val physicalCameraId =
                        physicalCameraIdForAllStreams ?: outputConfig.physicalCameraId
                    val mirrorMode = outputConfig.mirrorMode
                    val outputStreamConfig =
                        OutputStream.Config.create(
                            size = deferrableSurface.prescribedSize,
                            format = StreamFormat(deferrableSurface.prescribedStreamFormat),
                            camera =
                                if (physicalCameraId == null) {
                                    null
                                } else {
                                    CameraId.fromCamera2Id(physicalCameraId)
                                },
                            // No need to map MIRROR_MODE_ON_FRONT_ONLY to MIRROR_MODE_AUTO
                            // since its default value in framework
                            mirrorMode =
                                when (mirrorMode) {
                                    MirrorMode.MIRROR_MODE_OFF ->
                                        OutputStream.MirrorMode(
                                            OutputConfiguration.MIRROR_MODE_NONE
                                        )
                                    MirrorMode.MIRROR_MODE_ON ->
                                        OutputStream.MirrorMode(OutputConfiguration.MIRROR_MODE_H)
                                    else -> null
                                },
                            streamUseCase =
                                getStreamUseCase(
                                    deferrableSurface,
                                    sessionConfigAdapter.surfaceToStreamUseCaseMap
                                ),
                            streamUseHint =
                                getStreamUseHint(
                                    deferrableSurface,
                                    sessionConfigAdapter.surfaceToStreamUseHintMap
                                ),
                        )
                    val surfaces = outputConfig.sharedSurfaces + deferrableSurface
                    for (surface in surfaces) {
                        val stream = CameraStream.Config.create(outputStreamConfig)
                        streamConfigMap[stream] = surface
                        if (outputConfig.surfaceGroupId != SURFACE_GROUP_ID_NONE) {
                            val streamList = streamGroupMap[outputConfig.surfaceGroupId]
                            if (streamList == null) {
                                streamGroupMap[outputConfig.surfaceGroupId] = mutableListOf(stream)
                            } else {
                                streamList.add(stream)
                            }
                        }
                        if (surface.containerClass == MediaCodec::class.java) {
                            containsVideo = true
                        }
                        if (surface != deferrableSurface) continue
                        if (zslControl.isZslSurface(surface, sessionConfig)) {
                            zslStream = stream
                        }
                    }
                }
                if (sessionConfig.inputConfiguration != null) {
                    zslStream?.let {
                        inputStreams.add(
                            InputStream.Config(
                                stream = it,
                                maxImages = 1,
                                streamFormat = it.outputs.single().format,
                            )
                        )
                    }
                }
            }

            val combinedFlags = createCameraGraphFlags(cameraQuirks, containsVideo, isExtensions)

            // Set video stabilization mode to capture request
            var videoStabilizationMode = CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            if (sessionConfigAdapter.getValidSessionConfigOrNull() != null) {
                val config =
                    sessionConfigAdapter.getValidSessionConfigOrNull()!!.repeatingCaptureConfig
                val isPreviewStabilizationMode = config.previewStabilizationMode
                val isVideoStabilizationMode = config.videoStabilizationMode

                if (
                    isPreviewStabilizationMode == StabilizationMode.OFF ||
                        isVideoStabilizationMode == StabilizationMode.OFF
                ) {
                    videoStabilizationMode = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                } else if (isPreviewStabilizationMode == StabilizationMode.ON) {
                    videoStabilizationMode =
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                } else if (isVideoStabilizationMode == StabilizationMode.ON) {
                    videoStabilizationMode = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                }
            }

            // Set fps range to capture request
            val targetFpsRange = sessionConfigAdapter.getExpectedFrameRateRange()
            val defaultParameters =
                buildMap<Any, Any?> {
                    if (isExtensions) {
                        set(CameraPipeKeys.ignore3ARequiredParameters, true)
                    }
                    set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, videoStabilizationMode)
                    set(
                        CameraPipeKeys.camera2CaptureRequestTag,
                        "android.hardware.camera2.CaptureRequest.setTag.CX"
                    )
                    targetFpsRange?.let {
                        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetFpsRange)
                    }
                }
            targetFpsRange?.let {
                sessionParameters[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] = targetFpsRange
            }

            // TODO: b/327517884 - Add a quirk to not abort captures on stop for certain OEMs during
            //   extension sessions.

            // Build up a config (using TEMPLATE_PREVIEW by default)
            return CameraGraph.Config(
                camera = cameraConfig.cameraId,
                streams = streamConfigMap.keys.toList(),
                exclusiveStreamGroups = streamGroupMap.values.toList(),
                input = if (inputStreams.isEmpty()) null else inputStreams,
                sessionTemplate = sessionTemplate,
                sessionParameters = sessionParameters,
                sessionMode = operatingMode,
                defaultListeners = listOf(callbackMap, requestListener),
                defaultParameters = defaultParameters,
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

        private fun createCameraGraphFlags(
            cameraQuirks: CameraQuirks,
            containsVideo: Boolean,
            isExtensions: Boolean,
        ): CameraGraph.Flags {
            if (cameraQuirks.quirks.contains(CaptureSessionStuckQuirk::class.java)) {
                Log.debug { "CameraPipe should be enabling CaptureSessionStuckQuirk by default" }
            }
            // TODO(b/276354253): Set quirkWaitForRepeatingRequestOnDisconnect flag for overrides.

            // TODO(b/277310425): When creating a CameraGraph, this flag should be turned OFF when
            //  this behavior is not needed based on the use case interaction and the device on
            //  which the test is running.
            val shouldFinalizeSessionOnCloseBehavior = FinalizeSessionOnCloseQuirk.getBehavior()

            val shouldCloseCaptureSessionOnDisconnect =
                when {
                    isExtensions -> true
                    // If we can release Surfaces immediately, we'll finalize the session when the
                    // camera graph is closed (through FinalizeSessionOnCloseQuirk), and thus we
                    // won't need to explicitly close the capture session.
                    CameraQuirks.isImmediateSurfaceReleaseAllowed() -> false
                    cameraQuirks.quirks.contains(CloseCaptureSessionOnVideoQuirk::class.java) &&
                        containsVideo -> true
                    DeviceQuirks[CloseCaptureSessionOnDisconnectQuirk::class.java] != null -> true
                    else -> false
                }

            val shouldCloseCameraDeviceOnClose =
                DeviceQuirks[CloseCameraDeviceOnCameraGraphCloseQuirk::class.java] != null

            val shouldAbortCapturesOnStop =
                when {
                    isExtensions &&
                        DeviceQuirks[
                            DisableAbortCapturesOnStopWithSessionProcessorQuirk::class.java] !=
                            null -> false
                    /** @see [CameraGraph.Flags.abortCapturesOnStop] */
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> true
                    else -> false
                }

            val repeatingRequestsToCompleteBeforeNonRepeatingCapture =
                if (
                    cameraQuirks.quirks.contains(
                        QuickSuccessiveImageCaptureFailsRepeatingRequestQuirk::class.java
                    )
                ) {
                    1u
                } else {
                    0u
                }

            val shouldDisableGraphLevelSurfaceTracking =
                cameraQuirks.quirks.let {
                    it.contains(ConfigureSurfaceToSecondarySessionFailQuirk::class.java) ||
                        it.contains(PreviewOrientationIncorrectQuirk::class.java) ||
                        it.contains(TextureViewIsClosedQuirk::class.java)
                }

            return CameraGraph.Flags(
                abortCapturesOnStop = shouldAbortCapturesOnStop,
                awaitRepeatingRequestBeforeCapture =
                    CameraGraph.RepeatingRequestRequirementsBeforeCapture(
                        repeatingFramesToComplete =
                            repeatingRequestsToCompleteBeforeNonRepeatingCapture,
                        // TODO: b/364491700 - use CompletionBehavior.EXACT to disable CameraPipe
                        //  internal workaround when not required. See
                        //  Camera2Quirks.getRepeatingRequestFrameCountForCapture for details.
                        completionBehavior = AT_LEAST,
                    ),
                closeCaptureSessionOnDisconnect = shouldCloseCaptureSessionOnDisconnect,
                closeCameraDeviceOnClose = shouldCloseCameraDeviceOnClose,
                finalizeSessionOnCloseBehavior = shouldFinalizeSessionOnCloseBehavior,
                disableGraphLevelSurfaceTracking = shouldDisableGraphLevelSurfaceTracking,
                enableRestartDelays = true,
            )
        }
    }
}
