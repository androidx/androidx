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

package androidx.camera.camera2.impl

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.media.MediaCodec
import android.os.Build
import android.util.Pair
import androidx.annotation.GuardedBy
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.adapter.ZslControl
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.config.UseCaseCameraComponent
import androidx.camera.camera2.config.UseCaseCameraConfig
import androidx.camera.camera2.config.UseCaseGraphConfig
import androidx.camera.camera2.internal.DynamicRangeResolver
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsLowLightBoost
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.ValidatingBuilder
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.impl.utils.UseCaseUtil.containsVideoCapture
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.core.streamsharing.StreamSharingConfig
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
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
    @GuardedBy("lock") private val cameraCoordinator: CameraCoordinator,
    private val builder: UseCaseCameraComponent.Builder,
    private val zslControl: ZslControl,
    private val lowLightBoostControl: LowLightBoostControl,
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // Java version required for Dagger
    private val controls: java.util.Set<UseCaseCameraControl>,
    private val camera2CameraControl: Camera2CameraControl,
    private val cameraStateAdapter: CameraStateAdapter,
    private val cameraInternal: Provider<CameraInternal>,
    private val useCaseThreads: Provider<UseCaseThreads>,
    private val cameraInfoInternal: Provider<CameraInfoInternal>,
    private val encoderProfilesProvider: EncoderProfilesProvider,
    private val cameraProperties: CameraProperties,
    private val cameraXConfig: CameraXConfig,
    private val cameraGraphConfigProvider: CameraGraphConfigProvider,
    context: Context,
    displayInfoManager: DisplayInfoManager,
) {
    private val lock = Any()

    internal var sessionProcessor: SessionProcessor? = null
        get() =
            synchronized(lock) {
                return field
            }
        set(value) = synchronized(lock) { field = value }

    @GuardedBy("lock") private val attachedUseCases = mutableSetOf<UseCase>()

    @GuardedBy("lock") private val activeUseCases = mutableSetOf<UseCase>()

    @GuardedBy("lock") private var activeResumeEnabled = false

    @GuardedBy("lock") private var shouldCreateCameraGraphImmediately = true

    @GuardedBy("lock") private var deferredUseCaseManagerConfig: UseCaseManagerConfig? = null

    @GuardedBy("lock") private var isPrimary = true

    @GuardedBy("lock")
    private val pendingUseCasesToNotifyCameraControlReady = mutableSetOf<UseCase>()

    private val meteringRepeating by lazy {
        MeteringRepeating.Builder(cameraProperties, displayInfoManager).build()
    }

    private val supportedSurfaceCombination =
        SupportedSurfaceCombination(
            context,
            cameraProperties.metadata,
            encoderProfilesProvider,
            // TODO: b/406367951 - Create and use a proper impl. of FeatureCombinationQuery in
            //   order to handle MeteringRepeating scenarios
            FeatureCombinationQuery.NO_OP_FEATURE_COMBINATION_QUERY,
        )

    private val dynamicRangeResolver = DynamicRangeResolver(cameraProperties.metadata)

    @Volatile private var _activeComponent: UseCaseCameraComponent? = null
    public val camera: UseCaseCamera?
        get() = _activeComponent?.getUseCaseCamera()

    public val useCaseGraphConfig: UseCaseGraphConfig?
        get() = _activeComponent?.getUseCaseGraphConfig()

    private val closingCameraJobs = mutableListOf<Job>()

    private val allControls = controls.toMutableSet().apply { add(camera2CameraControl) }

    init {
        val outputStream =
            OutputStream.Config.create(
                size = getProperPreviewSize(cameraProperties, displayInfoManager),
                format = StreamFormat.PRIVATE,
            )
        val cameraStream = CameraStream.Config.create(outputStream)
        val noOpCameraGraph =
            cameraPipe.createCameraGraph(
                CameraGraph.Config(
                    camera = cameraProperties.cameraId,
                    streams = listOf(cameraStream),
                )
            )
        noOpCameraGraph.close()
    }

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
                Camera2Logger.warn { "Attach [] from $this (Ignored)" }
                return
            }
            Camera2Logger.debug { "Attaching $useCases from $this" }

            val unattachedUseCases =
                useCases.filter { useCase -> !attachedUseCases.contains(useCase) }

            // Notify session start to use cases
            for (useCase in unattachedUseCases) {
                useCase.onSessionStart()
            }

            if (attachedUseCases.addAll(useCases)) {
                if (!addOrRemoveRepeatingUseCase(getRunningUseCases())) {
                    updateZslDisabledByUseCaseConfigStatus()
                    updateLowLightBoostDisabledByUseCaseSessionConfigStatus()
                    refreshAttachedUseCases(attachedUseCases)
                }
            }

            if (!shouldCreateCameraGraphImmediately) {
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
                Camera2Logger.warn { "Detaching [] from $this (Ignored)" }
                return
            }
            Camera2Logger.debug { "Detaching $useCases from $this" }

            // When use cases are detached, they should be considered inactive as well. Also note
            // that
            // we remove the use cases from our set directly because the subsequent cleanup actions
            // from
            // detaching the use cases should suffice here.
            activeUseCases.removeAll(useCases)

            // Notify state detached to use cases
            for (useCase in useCases) {
                if (attachedUseCases.contains(useCase)) {
                    useCase.onSessionStop()
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
                    lowLightBoostControl.setLowLightBoostDisabledByUseCaseSessionConfig(false)
                } else {
                    updateZslDisabledByUseCaseConfigStatus()
                    updateLowLightBoostDisabledByUseCaseSessionConfigStatus()
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
                closeCurrentUseCases()
                meteringRepeating.onUnbind()
                closingCameraJobs.toList()
            }
        closingJobs.joinAll()
    }

    override fun toString(): String = "UseCaseManager<${cameraGraphConfigProvider}>"

    @GuardedBy("lock")
    private fun refreshRunningUseCases() {
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
        runningUseCases: Set<UseCase>,
    ) {
        // Note: This may be called with the same set of values that was previously set. This
        // is used as a signal to indicate the properties of the UseCase may have changed.
        SessionConfigAdapter(runningUseCases, isPrimary = isPrimary)
            .getValidSessionConfigOrNull()
            ?.let { requestControl.setSessionConfigAsync(it) }
            ?: run {
                Camera2Logger.debug { "Unable to reset the session due to invalid config" }
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
                        useCaseThreads.get().sequentialExecutor,
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
        closeCurrentUseCases()

        val useCases = newUseCases.toList()

        // Update list of active useCases
        if (useCases.isEmpty()) {
            for (control in allControls) {
                control.requestControl = null
                control.reset()
            }
            return
        }

        if (!shouldCreateCameraGraphImmediately) {
            // We will need to set the UseCaseCamera to null since the new UseCaseCamera along with
            // its respective CameraGraph configurations won't be ready until:
            //
            // - And/or, the UseCaseManager is ready to be resumed under concurrent camera settings.
            for (control in allControls) {
                control.requestControl = null
            }
        }

        val graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter)
        val useCamera2Extension =
            sessionProcessor?.implementationType?.first == SessionProcessor.TYPE_CAMERA2_EXTENSION
        val sessionConfigAdapter = SessionConfigAdapter(useCases, isPrimary = isPrimary)

        // Enables extensions with the Camera2 Extensions approach if extension mode is requested.
        if (useCamera2Extension) {
            Camera2Logger.debug { "Setting up UseCaseManager with OperatingMode.EXTENSION" }
            val creationResult =
                cameraGraphConfigProvider.create(
                    operatingMode = OperatingMode.EXTENSION,
                    sessionConfig = sessionConfigAdapter.getValidSessionConfigOrNull(),
                    graphStateToCameraStateAdapter = graphStateToCameraStateAdapter,
                    camera2ExtensionMode = sessionProcessor?.implementationType?.second,
                    isExtensions = true,
                    enableStreamUseCase = false,
                    surfaceToStreamUseCaseMap = sessionConfigAdapter.surfaceToStreamUseCaseMap,
                    surfaceToStreamUseHintMap = sessionConfigAdapter.surfaceToStreamUseHintMap,
                    cameraXConfig = cameraXConfig,
                )

            sessionProcessor!!.initSession(cameraInfoInternal.get(), null)

            val useCaseManagerConfig =
                UseCaseManagerConfig(
                    useCases,
                    sessionConfigAdapter,
                    creationResult.config,
                    creationResult.streamConfigMap,
                    graphStateToCameraStateAdapter,
                )
            tryResumeUseCaseManager(useCaseManagerConfig)
            return
        } else {
            val creationResult =
                createCameraGraphConfig(sessionConfigAdapter, graphStateToCameraStateAdapter)

            val useCaseManagerConfig =
                UseCaseManagerConfig(
                    useCases,
                    sessionConfigAdapter,
                    creationResult.config,
                    creationResult.streamConfigMap,
                    graphStateToCameraStateAdapter,
                )
            tryResumeUseCaseManager(useCaseManagerConfig)
        }
    }

    @GuardedBy("lock")
    private fun closeCurrentUseCases() {
        // Close prior camera graph
        camera.let { useCaseCamera ->
            _activeComponent = null
            cameraCoordinator.removePendingCameraInfo(cameraInfoInternal.get())
            useCaseCamera?.close()?.let { closingJob ->
                closingCameraJobs.add(closingJob)
                closingJob.invokeOnCompletion {
                    synchronized(lock) { closingCameraJobs.remove(closingJob) }
                }
            }
        }
        sessionProcessor?.apply {
            setCaptureSessionRequestProcessor(null)
            deInitSession()
        }
    }

    @VisibleForTesting
    @GuardedBy("lock")
    internal fun tryResumeUseCaseManager(useCaseManagerConfig: UseCaseManagerConfig) {
        if (!shouldCreateCameraGraphImmediately) {
            deferredUseCaseManagerConfig = useCaseManagerConfig
            cameraCoordinator.addPendingCameraInfo(cameraInfoInternal.get())
            return
        }
        val cameraGraph = cameraPipe.createCameraGraph(useCaseManagerConfig.cameraGraphConfig)
        beginComponentCreation(useCaseManagerConfig, cameraGraph)
    }

    internal fun resumeDeferredComponentCreation(cameraGraph: CameraGraph) =
        synchronized(lock) {
            beginComponentCreation(checkNotNull(deferredUseCaseManagerConfig), cameraGraph)
        }

    @GuardedBy("lock")
    private fun beginComponentCreation(
        useCaseManagerConfig: UseCaseManagerConfig,
        cameraGraph: CameraGraph,
    ) {
        with(useCaseManagerConfig) {
            graphStateToCameraStateAdapter.cameraGraph = cameraGraph
            cameraStateAdapter.onGraphUpdated(cameraGraph)
            // Create and configure the new camera component.
            _activeComponent =
                builder
                    .config(
                        UseCaseCameraConfig(
                            useCases,
                            sessionConfigAdapter,
                            cameraGraph,
                            streamConfigMap,
                        )
                    )
                    .build()
                    .also { it.getUseCaseCamera().start() }

            for (control in allControls) {
                control.requestControl = camera?.requestControl
            }

            setCaptureSessionRequestProcessor(sessionConfigAdapter, cameraGraph)

            camera?.setActiveResumeMode(activeResumeEnabled)

            refreshRunningUseCases()
        }

        Camera2Logger.debug {
            "Notifying $pendingUseCasesToNotifyCameraControlReady camera control ready"
        }
        for (useCase in pendingUseCasesToNotifyCameraControlReady) {
            useCase.onCameraControlReady()
        }
        pendingUseCasesToNotifyCameraControlReady.clear()
    }

    private fun setCaptureSessionRequestProcessor(
        sessionConfigAdapter: SessionConfigAdapter,
        cameraGraph: CameraGraph,
    ) {
        val stillCaptureStreamId: StreamId? =
            sessionConfigAdapter.getValidSessionConfigOrNull()?.let { sessionConfig ->
                val repeatingSurfaces = sessionConfig.repeatingCaptureConfig.surfaces
                sessionConfig.surfaces
                    .find { surface ->
                        surface !in repeatingSurfaces
                    } // Find the first non-repeating surface (nullable)
                    ?.let { surface -> // If found...
                        useCaseGraphConfig?.getStreamIdsFromSurfaces(
                            listOf(surface)
                        ) // Get its StreamIds (nullable list)
                    }
                    ?.firstOrNull() // Get the first StreamId or null
            }

        sessionProcessor?.setCaptureSessionRequestProcessor(
            object : SessionProcessor.CaptureSessionRequestProcessor {
                override fun getRealtimeStillCaptureLatency(): Pair<Long, Long>? {
                    val outputLatency =
                        cameraGraph.streams.getOutputLatency(stillCaptureStreamId!!) ?: return null
                    val captureLatencyMs = outputLatency.estimatedCaptureLatencyNs.div(1_000_000)
                    val processingLatencyMs =
                        outputLatency.estimatedProcessingLatencyNs.div(1_000_000)
                    return Pair.create(captureLatencyMs, processingLatencyMs)
                }

                override fun setExtensionStrength(strength: Int) {
                    if (Build.VERSION.SDK_INT >= 34) {
                        camera
                            ?.requestControl
                            ?.setParametersAsync(
                                values = mutableMapOf(CaptureRequest.EXTENSION_STRENGTH to strength)
                            )
                    }
                }
            }
        )
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
    private fun isMeteringRepeatingRequired(runningUseCases: Set<UseCase>): Boolean {
        if (!cameraXConfig.isRepeatingStreamForced) {
            return false
        }

        val hasActiveSurfaces =
            runningUseCases.any {
                it != meteringRepeating && it.sessionConfig.surfaces.isNotEmpty()
            }
        if (!hasActiveSurfaces) {
            return false
        }

        val attachedWithoutMetering = attachedUseCases.filter { it != meteringRepeating }

        if (attachedWithoutMetering.isEmpty()) {
            return false
        }

        return with(attachedWithoutMetering) {
            shouldForceRepeatingStream() && isMeteringCombinationSupported()
        }
    }

    private fun Collection<UseCase>.shouldForceRepeatingStream(): Boolean {
        if (isEmpty()) {
            return false
        }

        val sessionConfig =
            ValidatingBuilder().apply { forEach { useCase -> add(useCase.sessionConfig) } }.build()

        val repeatingSurfaces = sessionConfig.repeatingCaptureConfig.surfaces
        val allSurfaces = sessionConfig.surfaces

        if (allSurfaces.isEmpty()) {
            return false
        }

        val isVideoOnly = allSurfaces.all { it.containerClass == MediaCodec::class.java }
        val hasNoRepeatingUseCases = repeatingSurfaces.isEmpty()

        return isVideoOnly || hasNoRepeatingUseCases
    }

    @GuardedBy("lock")
    private fun shouldAddRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        val isMeteringEnabled = attachedUseCases.contains(meteringRepeating)
        return !isMeteringEnabled && isMeteringRepeatingRequired(runningUseCases)
    }

    @GuardedBy("lock")
    private fun shouldRemoveRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        val isMeteringEnabled = runningUseCases.contains(meteringRepeating)
        return isMeteringEnabled && !isMeteringRepeatingRequired(runningUseCases)
    }

    @GuardedBy("lock")
    private fun addRepeatingUseCase() {
        meteringRepeating.bindToCamera(cameraInternal.get(), null, null, null)
        meteringRepeating.setupSession()
        attach(listOf(meteringRepeating))
        activate(meteringRepeating)
    }

    @GuardedBy("lock")
    private fun removeRepeatingUseCase() {
        deactivate(meteringRepeating)
        detach(listOf(meteringRepeating))
        meteringRepeating.unbindFromCamera(cameraInternal.get())
    }

    internal fun createCameraGraphConfig(
        sessionConfigAdapter: SessionConfigAdapter,
        graphStateToCameraStateAdapter: GraphStateToCameraStateAdapter,
        isExtensions: Boolean = false,
    ): CameraGraphConfigProvider.CameraGraphCreationResult {
        val sessionConfig = sessionConfigAdapter.getValidSessionConfigOrNull()
        val operatingMode =
            sessionConfig?.let {
                when (it.sessionType) {
                    SESSION_REGULAR -> OperatingMode.NORMAL
                    SESSION_HIGH_SPEED -> OperatingMode.HIGH_SPEED
                    else -> OperatingMode.custom(it.sessionType)
                }
            } ?: OperatingMode.NORMAL

        return cameraGraphConfigProvider.create(
            operatingMode = operatingMode,
            sessionConfig = sessionConfig,
            graphStateToCameraStateAdapter = graphStateToCameraStateAdapter,
            camera2ExtensionMode = null,
            isExtensions = isExtensions,
            surfaceToStreamUseCaseMap = sessionConfigAdapter.surfaceToStreamUseCaseMap,
            surfaceToStreamUseHintMap = sessionConfigAdapter.surfaceToStreamUseHintMap,
            cameraXConfig = cameraXConfig,
        )
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

        // TODO: b/406367951 - Properly pass feature combo info for MeteringRepeating
        return supportedSurfaceCombination
            .checkSupported(
                SupportedSurfaceCombination.FeatureSettings(
                    getCameraMode(),
                    getRequiredMaxBitDepth(attachedSurfaceInfoList),
                    hasVideoCapture = containsVideoCapture(),
                    isPreviewStabilizationOn = isPreviewStabilizationOn(),
                    isUltraHdrOn = isUltraHdrOn(),
                ),
                mutableListOf<SurfaceConfig>().apply {
                    addAll(sessionSurfacesConfigs)
                    add(createMeteringRepeatingSurfaceConfig())
                },
            )
            .also {
                Camera2Logger.debug {
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
                    listOf(0),
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
                    Camera2Logger.warn { "Invalid surface resolution or stream spec is found." }
                    clear()
                    return@apply
                }

                val surfaceConfig =
                    supportedSurfaceCombination.transformSurfaceConfig(
                        getCameraMode(),
                        useCase.currentConfig.inputFormat,
                        surfaceResolution,
                        useCase.currentConfig.streamUseCase,
                    )
                add(
                    AttachedSurfaceInfo.create(
                        surfaceConfig,
                        useCase.currentConfig.inputFormat,
                        surfaceResolution,
                        streamSpec.dynamicRange,
                        useCase.getCaptureTypes(),
                        streamSpec.implementationOptions ?: MutableOptionsBundle.create(),
                        streamSpec.sessionType,
                        streamSpec.expectedFrameRateRange,
                        useCase.currentConfig.isStrictFrameRateRequired,
                        useCase.currentConfig.getCustomMaxFrameRate(surfaceResolution),
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
                            deferrableSurface.prescribedSize,
                            useCase.currentConfig.streamUseCase,
                        )
                    )
                }
            }
        }

    private fun createMeteringRepeatingSurfaceConfig() =
        supportedSurfaceCombination.transformSurfaceConfig(
            getCameraMode(),
            meteringRepeating.imageFormat,
            meteringRepeating.attachedSurfaceResolution!!,
            meteringRepeating.currentConfig.streamUseCase,
        )

    private fun updateZslDisabledByUseCaseConfigStatus() {
        val disableZsl = attachedUseCases.any { it.currentConfig.isZslDisabled(false) }
        zslControl.setZslDisabledByUserCaseConfig(disableZsl)
    }

    private fun updateLowLightBoostDisabledByUseCaseSessionConfigStatus() {
        if (!cameraProperties.metadata.supportsLowLightBoost) {
            return
        }

        // Low-light boost should be disabled when expected frame rate range exceeds 30.
        if (attachedUseCases.getSessionConfig().expectedFrameRateRange.upper > 30) {
            lowLightBoostControl.setLowLightBoostDisabledByUseCaseSessionConfig(true)
            return
        }

        lowLightBoostControl.setLowLightBoostDisabledByUseCaseSessionConfig(false)
    }

    private fun Collection<UseCase>.getSessionConfig(): SessionConfig =
        ValidatingBuilder().apply { forEach { useCase -> add(useCase.sessionConfig) } }.build()

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
            val streamConfigMap: Map<CameraStream.Config, DeferrableSurface>,
            val graphStateToCameraStateAdapter: GraphStateToCameraStateAdapter,
        )
    }
}
