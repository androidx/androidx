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

@file:Suppress("DEPRECATION")

package androidx.camera.camera2.impl

import android.content.Context
import android.media.MediaCodec
import androidx.annotation.GuardedBy
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.adapter.ZslControl
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.config.UseCaseCameraComponent
import androidx.camera.camera2.config.UseCaseCameraConfig
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.ResolvedStreamInfo
import androidx.camera.core.impl.SessionConfig.ValidatingBuilder
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SurfaceConfig
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CompletableDeferred
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
    private val builder: Provider<UseCaseCameraComponent.Builder>,
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
    private val supportedSurfaceCombination: SupportedSurfaceCombination,
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

    @GuardedBy("lock") private var deferredUseCaseCameraConfig: UseCaseCameraConfig? = null

    @GuardedBy("lock") private var isPrimary = true

    @GuardedBy("lock") private var isZslDisabledByDetach = false

    @GuardedBy("lock")
    private val pendingUseCasesToNotifyCameraControlReady = mutableSetOf<UseCase>()

    @GuardedBy("lock") private var enabledUseCases: Set<UseCase>? = null

    @GuardedBy("lock") private var sessionInteropConfig: Config? = null

    @GuardedBy("lock") private var pendingModeSwitch: CompletableDeferred<Unit>? = null

    @GuardedBy("lock") private var lastRepeatingUpdateJob: Job? = null

    private val meteringRepeating =
        MeteringRepeating.Builder(cameraProperties, displayInfoManager).build()

    private val defaultCameraGraphFactory: (CameraGraph.Config) -> CameraGraph = { config ->
        cameraPipe.createCameraGraph(config)
    }

    @Volatile private var _activeComponent: UseCaseCameraComponent? = null
    public val camera: UseCaseCamera?
        get() = _activeComponent?.getUseCaseCamera()

    private val closingCameraJobs = mutableListOf<Job>()

    private val allControls = controls.toMutableSet().apply { add(camera2CameraControl) }

    internal fun setCameraGraphCreationMode(createImmediately: Boolean) =
        synchronized(lock) {
            shouldCreateCameraGraphImmediately = createImmediately
            if (shouldCreateCameraGraphImmediately) {
                // Clear the UseCaseCameraConfig that haven't been "resumed" when we return
                // to single camera operating mode early.
                deferredUseCaseCameraConfig = null
            }
        }

    internal fun getDeferredCameraGraphConfig() =
        synchronized(lock) { deferredUseCaseCameraConfig?.cameraGraphConfig }

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

            val unattachedUseCases = useCases.filter { useCase ->
                !attachedUseCases.contains(useCase)
            }

            // Notify session start to use cases
            for (useCase in unattachedUseCases) {
                useCase.onSessionStart()
            }

            if (attachedUseCases.addAll(useCases)) {
                if (!addOrRemoveRepeatingUseCase(getRunningUseCases())) {
                    updateZslDisabledByUseCaseConfigStatus()
                    lowLightBoostControl.onSessionConfigChanged(attachedUseCases.toList())
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
                // When all enabled use cases are detached (e.g. Lifecycle ON_STOP when no other
                // LifecycleCamera has attached its use cases yet), reset the enabled subset to
                // null per CameraInternal.setEnabledUseCases contract. Checking `none { it in
                // attachedUseCases }` preserves a newly active LifecycleCamera's enabled subset
                // during overlapping bind/suspend transitions.
                if (enabledUseCases?.none { it in attachedUseCases } == true) {
                    enabledUseCases = null
                }
                if (attachedUseCases.isEmpty()) {
                    sessionInteropConfig = null
                }
                if (addOrRemoveRepeatingUseCase(getRunningUseCases())) {
                    return
                }

                if (attachedUseCases.isEmpty()) {
                    zslControl.setZslDisabledByUserCaseConfig(false)
                    lowLightBoostControl.onSessionConfigChanged(emptyList())
                } else {
                    updateZslDisabledByUseCaseConfigStatus()
                    lowLightBoostControl.onSessionConfigChanged(attachedUseCases.toList())
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

    public fun setSessionInteropConfig(config: Config?): Unit =
        synchronized(lock) { sessionInteropConfig = config }

    /**
     * Updates the active subset of attached [UseCase]s enabled for streaming without reconfiguring
     * the capture session, or sets the initial enabled subset prior to [attach]. Pass `null` to
     * enable all attached [UseCase]s.
     *
     * @return A [Deferred] that completes when the first frame of the updated [UseCase]s is
     *   produced, or null if the camera is not currently active.
     * @throws IllegalArgumentException if [useCases] is empty or requires an internal repeating
     *   stream that is not supported alongside all attached [UseCase]s.
     */
    public fun setEnabledUseCases(useCases: Collection<UseCase>?): Deferred<Unit>? =
        synchronized(lock) {
            if (useCases == null) {
                pendingModeSwitch?.completeExceptionally(
                    CameraControl.OperationCanceledException("Dynamic UseCase switching disabled.")
                )
                pendingModeSwitch = null
                if (enabledUseCases != null) {
                    enabledUseCases = null
                    if (attachedUseCases.isNotEmpty()) {
                        refreshRunningUseCases()
                    }
                }
                return CompletableDeferred(Unit)
            }
            require(useCases.isNotEmpty()) { "useCases cannot be empty" }
            // CameraUseCaseAdapter updates the enabled subset before attaching newly added UseCases
            // during initial bind or partial re-bind; record the subset so the subsequent attach()
            // configures the capture session with it from the start.
            if (attachedUseCases.isEmpty() || !attachedUseCases.containsAll(useCases)) {
                pendingModeSwitch?.completeExceptionally(
                    CameraControl.OperationCanceledException(
                        "Superseded by setEnabledUseCases($useCases)"
                    )
                )
                pendingModeSwitch = null
                enabledUseCases = useCases.toSet()
                return CompletableDeferred(Unit)
            }
            val attachedWithoutMetering = attachedUseCases.filter { it != meteringRepeating }
            if (attachedWithoutMetering.isNotEmpty() && cameraXConfig.isRepeatingStreamForced) {
                require(
                    !useCases.shouldForceRepeatingStream() ||
                        attachedUseCases.contains(meteringRepeating) ||
                        attachedWithoutMetering.isMeteringCombinationSupported(
                            forEnabledSubset = true
                        )
                ) {
                    "The enabled UseCases ($useCases) require a repeating stream, which is not " +
                        "supported in combination with all bound SessionConfig UseCases."
                }
            }
            if (camera == null) {
                return null
            }
            pendingModeSwitch?.completeExceptionally(
                CameraControl.OperationCanceledException(
                    "Superseded by setEnabledUseCasesAsync($useCases)"
                )
            )
            pendingModeSwitch = null
            val previousEnabledUseCases = enabledUseCases
            enabledUseCases = useCases.toSet()
            val addedRepeating =
                if (shouldAddRepeatingUseCase(attachedUseCases.intersect(activeUseCases))) {
                    addRepeatingUseCase()
                    true
                } else {
                    false
                }
            val useCaseCamera = camera
            if (useCaseCamera == null) {
                enabledUseCases = previousEnabledUseCases
                return null
            }
            Camera2Logger.debug { "Updating enabled use cases to $useCases on $useCaseCamera" }
            val repeatingUpdate =
                if (addedRepeating) {
                    lastRepeatingUpdateJob
                } else {
                    updateRunningUseCases(getRunningUseCases())
                }
            val switchDeferred = CompletableDeferred<Unit>()
            pendingModeSwitch = switchDeferred
            if (repeatingUpdate == null) {
                pendingModeSwitch = null
                switchDeferred.complete(Unit)
            } else {
                // Map coroutine Job cancellation to CameraControl.OperationCanceledException so
                // ListenableFuture callers receive OperationCanceledException rather than
                // CancellationException.
                repeatingUpdate.invokeOnCompletion { cause ->
                    if (cause != null) {
                        switchDeferred.completeExceptionally(
                            (cause as? CameraControl.OperationCanceledException)
                                ?: (cause.cause as? CameraControl.OperationCanceledException)
                                ?: CameraControl.OperationCanceledException(
                                    "setEnabledUseCasesAsync($useCases) failed: $cause",
                                    cause,
                                )
                        )
                    } else {
                        switchDeferred.complete(Unit)
                    }
                }
            }
            return switchDeferred
        }

    public suspend fun close() {
        val closingJobs =
            synchronized(lock) {
                closeCurrentUseCases()
                cameraGraphConfigProvider.onUseCasesEmpty()
                meteringRepeating.onUnbind()
                closingCameraJobs.toList()
            }
        closingJobs.joinAll()
    }

    override fun toString(): String = "UseCaseManager<${cameraGraphConfigProvider}>"

    @GuardedBy("lock")
    private fun refreshRunningUseCases() {
        // If there are no attached UseCases, the camera is either closed or being closed
        // by refreshAttachedUseCases(). There is no need to update the running state or
        // repeating request.
        if (attachedUseCases.isEmpty()) {
            return
        }

        val runningUseCases = getRunningUseCases()
        when {
            shouldAddRepeatingUseCase(runningUseCases) -> addRepeatingUseCase()
            shouldRemoveRepeatingUseCase(runningUseCases) -> removeRepeatingUseCase()
            else -> updateRunningUseCases(runningUseCases)
        }
    }

    @GuardedBy("lock")
    private fun updateRunningUseCases(runningUseCases: Set<UseCase>): Job? {
        val job = camera?.let {
            val job = it.updateRepeatingRequestAsync(isPrimary, runningUseCases)
            for (control in allControls) {
                if (control is RunningUseCasesChangeListener) {
                    control.onRunningUseCasesChanged(runningUseCases)
                }
            }
            job
        }
        lastRepeatingUpdateJob = job
        return job
    }

    @GuardedBy("lock")
    private fun refreshAttachedUseCases(newUseCases: Set<UseCase>) {
        closeCurrentUseCases(hasAttachedUseCases = newUseCases.isNotEmpty())

        val useCases = newUseCases.toList()

        // Update list of active useCases
        if (useCases.isEmpty()) {
            cameraGraphConfigProvider.onUseCasesEmpty()
            for (control in allControls) {
                control.requestControl = null
                control.reset()
            }
            return
        }
        cameraGraphConfigProvider.onUseCasesAttached()

        if (!shouldCreateCameraGraphImmediately) {
            // We will need to set the UseCaseCamera to null since the new UseCaseCamera along with
            // its respective CameraGraph configurations won't be ready until:
            //
            // - And/or, the UseCaseManager is ready to be resumed under concurrent camera settings.
            for (control in allControls) {
                control.requestControl = null
            }
        }

        if (newUseCases.contains(meteringRepeating)) {
            newUseCases
                .filter { it != meteringRepeating }
                .getMeteringRepeatingDynamicRange()
                ?.let { meteringDynamicRange ->
                    if (
                        meteringRepeating.attachedStreamSpec?.dynamicRange != meteringDynamicRange
                    ) {
                        meteringRepeating.setupSession(meteringDynamicRange)
                    }
                }
        }

        val extensionMode =
            sessionProcessor?.implementationType?.let { implType ->
                if (implType.first == SessionProcessor.TYPE_CAMERA2_EXTENSION) {
                    implType.second
                } else {
                    null
                }
            }
        // Enables extensions with the Camera2 Extensions approach if extension mode is requested.
        if (extensionMode != null) {
            Camera2Logger.debug { "Setting up UseCaseManager with OperatingMode.EXTENSION" }
            sessionProcessor!!.initSession(cameraInfoInternal.get(), null)
        }
        tryResumeUseCaseManager(
            createUseCaseCameraConfig(
                sessionConfigAdapter =
                    SessionConfigAdapter(
                        useCases,
                        isPrimary = isPrimary,
                        sessionInteropConfig = sessionInteropConfig,
                        allowEmptySurfaceStreamUseCase = enabledUseCases != null,
                    ),
                extensionMode = extensionMode,
            )
        )
    }

    @VisibleForTesting
    internal fun createUseCaseCameraConfig(
        sessionConfigAdapter: SessionConfigAdapter,
        extensionMode: Int?,
    ): UseCaseCameraConfig {
        return UseCaseCameraConfig.create(
            cameraGraphConfigProvider = cameraGraphConfigProvider,
            sessionConfigAdapter = sessionConfigAdapter,
            cameraGraphFactory = defaultCameraGraphFactory,
            cameraStateAdapter = cameraStateAdapter,
            sessionProcessor = sessionProcessor,
            extensionMode = extensionMode,
        )
    }

    @GuardedBy("lock")
    private fun closeCurrentUseCases(hasAttachedUseCases: Boolean = false) {
        pendingModeSwitch?.completeExceptionally(
            CameraControl.OperationCanceledException("Camera is closing.")
        )
        pendingModeSwitch = null
        lastRepeatingUpdateJob = null
        // Drain any buffered ZSL frames from the closing session so GraphicBuffers and
        // TotalCaptureResults are not retained while idle or across session recreation.
        isZslDisabledByDetach = !hasAttachedUseCases || attachedUseCases.isEmpty()
        zslControl.setZslDisabledByDetach(isZslDisabledByDetach)
        zslControl.clearRingBuffer()
        if (!hasAttachedUseCases && attachedUseCases.isNotEmpty()) {
            zslControl.clearZslConfig()
        }

        // Close prior camera graph
        camera.let { useCaseCamera ->
            _activeComponent = null
            cameraCoordinator.removePendingCameraInfo(cameraInfoInternal.get())
            useCaseCamera?.close()?.let { closingJob ->
                closingCameraJobs.add(closingJob)
                closingJob.invokeOnCompletion {
                    synchronized(lock) {
                        closingCameraJobs.remove(closingJob)
                        if (isZslDisabledByDetach || attachedUseCases.isEmpty()) {
                            zslControl.setZslDisabledByDetach(true)
                            zslControl.clearRingBuffer()
                        }
                    }
                }
            }
        }
        sessionProcessor?.deInitSession()
        deferredUseCaseCameraConfig = null
    }

    @GuardedBy("lock")
    private fun tryResumeUseCaseManager(useCaseCameraConfig: UseCaseCameraConfig) {
        if (!shouldCreateCameraGraphImmediately) {
            deferredUseCaseCameraConfig = useCaseCameraConfig
            cameraCoordinator.addPendingCameraInfo(cameraInfoInternal.get())
            return
        }

        beginComponentCreation(useCaseCameraConfig)
    }

    internal fun resumeDeferredComponentCreation(cameraGraph: CameraGraph) =
        synchronized(lock) {
            val originalConfig = checkNotNull(deferredUseCaseCameraConfig)

            val resumedConfig = originalConfig.copy(cameraGraphFactory = { _ -> cameraGraph })

            beginComponentCreation(resumedConfig)
        }

    @GuardedBy("lock")
    private fun beginComponentCreation(useCaseCameraConfig: UseCaseCameraConfig) {
        // Create and configure the new camera component.
        _activeComponent = builder.get().config(useCaseCameraConfig).build()

        val newUseCaseCamera = checkNotNull(camera)
        newUseCaseCamera.start()

        for (control in allControls) {
            control.requestControl = newUseCaseCamera.requestControl
        }

        newUseCaseCamera.setActiveResumeMode(activeResumeEnabled)

        updateRunningUseCases(getRunningUseCases())

        Camera2Logger.debug {
            "Notifying $pendingUseCasesToNotifyCameraControlReady camera control ready"
        }
        for (useCase in pendingUseCasesToNotifyCameraControlReady) {
            useCase.onCameraControlReady()
        }
        pendingUseCasesToNotifyCameraControlReady.clear()
    }

    @GuardedBy("lock")
    private fun getRunningUseCases(): Set<UseCase> {
        val running = attachedUseCases.intersect(activeUseCases)
        val enabled = enabledUseCases ?: return running
        val filtered = running.filterTo(mutableSetOf()) { it in enabled }
        if (
            cameraXConfig.isRepeatingStreamForced &&
                filtered.any { it.sessionConfig.surfaces.isNotEmpty() } &&
                filtered.shouldForceRepeatingStream()
        ) {
            if (meteringRepeating in running) {
                filtered.add(meteringRepeating)
            } else {
                // If MeteringRepeating cannot be added due to hardware stream combination limits,
                // keep one configured repeating UseCase (preferring Preview) so the capture session
                // retains a repeating request for 3A convergence.
                val fallbackCandidates = running.filter { useCase ->
                    useCase != meteringRepeating &&
                        useCase !in filtered &&
                        useCase.sessionConfig.repeatingCaptureConfig.surfaces.any {
                            it.containerClass != MediaCodec::class.java
                        }
                }
                (fallbackCandidates.firstOrNull { it is Preview }
                        ?: fallbackCandidates.firstOrNull())
                    ?.let {
                        Camera2Logger.warn {
                            "Keeping $it in the repeating request because MeteringRepeating " +
                                "cannot be added alongside $attachedUseCases"
                        }
                        filtered.add(it)
                    }
            }
        }
        return filtered
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

        val attachedWithoutMetering = attachedUseCases.filter { it != meteringRepeating }

        if (attachedWithoutMetering.isEmpty()) {
            return false
        }

        if (enabledUseCases != null) {
            return attachedWithoutMetering.any { listOf(it).shouldForceRepeatingStream() } &&
                attachedWithoutMetering.isMeteringCombinationSupported()
        }

        val hasActiveSurfaces = runningUseCases.any {
            it != meteringRepeating && it.sessionConfig.surfaces.isNotEmpty()
        }
        if (!hasActiveSurfaces) {
            return false
        }

        return attachedWithoutMetering.shouldForceRepeatingStream() &&
            attachedWithoutMetering.isMeteringCombinationSupported()
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
        if (!cameraXConfig.isRepeatingStreamForced) {
            return false
        }

        val isMeteringEnabled = attachedUseCases.contains(meteringRepeating)
        return !isMeteringEnabled && isMeteringRepeatingRequired(runningUseCases)
    }

    @GuardedBy("lock")
    private fun shouldRemoveRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        if (!attachedUseCases.contains(meteringRepeating)) {
            return false
        }
        return activeUseCases.contains(meteringRepeating) &&
            !isMeteringRepeatingRequired(runningUseCases)
    }

    @GuardedBy("lock")
    private fun addRepeatingUseCase() {
        val meteringDynamicRange =
            attachedUseCases.filter { it != meteringRepeating }.getMeteringRepeatingDynamicRange()
                ?: DynamicRange.SDR
        meteringRepeating.bindToCamera(cameraInternal.get(), null, null, null)
        meteringRepeating.setupSession(meteringDynamicRange)
        activeUseCases.add(meteringRepeating)
        attach(listOf(meteringRepeating))
    }

    @GuardedBy("lock")
    private fun removeRepeatingUseCase() {
        // Remove directly from activeUseCases instead of calling deactivate() to avoid a
        // redundant refreshRunningUseCases() before detach() tears down the session.
        activeUseCases.remove(meteringRepeating)
        detach(listOf(meteringRepeating))
        meteringRepeating.unbindFromCamera(cameraInternal.get())
    }

    @GuardedBy("lock")
    private fun Collection<UseCase>.isMeteringCombinationSupported(
        forEnabledSubset: Boolean = enabledUseCases != null
    ): Boolean =
        meteringRepeating.resolveStreamInfo(
            toResolvedStreamInfos(),
            supportedSurfaceCombination,
            getCameraMode(),
            sessionSurfaceConfigsProvider =
                if (!forEnabledSubset) {
                    { featureSettings -> getSessionSurfacesConfigs(featureSettings) }
                } else {
                    null
                },
        ) != null

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

    private fun Collection<UseCase>.getMeteringRepeatingDynamicRange(): DynamicRange? =
        meteringRepeating.resolveDynamicRange(
            toResolvedStreamInfos(),
            supportedSurfaceCombination,
            getCameraMode(),
        )

    private fun Collection<UseCase>.toResolvedStreamInfos(): List<ResolvedStreamInfo> = buildList {
        for (useCase in this@toResolvedStreamInfos) {
            val surfaceResolution = useCase.attachedSurfaceResolution
            val streamSpec = useCase.attachedStreamSpec
            // When collecting the info, the UseCases might be unbound to make these info become
            // null. Abort and return emptyList() so MeteringRepeating is never validated against an
            // incomplete subset of UseCases.
            if (surfaceResolution == null || streamSpec == null) {
                Camera2Logger.warn { "Invalid surface resolution or stream spec is found." }
                return emptyList()
            }
            add(
                ResolvedStreamInfo(
                    useCase.currentConfig,
                    streamSpec.toBuilder().setResolution(surfaceResolution).build(),
                )
            )
        }
    }

    private fun Collection<UseCase>.getSessionSurfacesConfigs(
        featureSettings: SupportedSurfaceCombination.FeatureSettings
    ): List<SurfaceConfig> =
        mutableListOf<SurfaceConfig>().apply {
            this@getSessionSurfacesConfigs.forEach { useCase ->
                val inputFormats = useCase.inputFormats
                useCase.sessionConfig.surfaces.forEachIndexed { index, deferrableSurface ->
                    val format = inputFormats.getOrElse(index) { inputFormats.first() }
                    add(
                        supportedSurfaceCombination.transformSurfaceConfig(
                            getCameraMode(),
                            format,
                            deferrableSurface.prescribedSize,
                            useCase.currentConfig.streamUseCase,
                            featureSettings,
                        )
                    )
                }
            }
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
}
