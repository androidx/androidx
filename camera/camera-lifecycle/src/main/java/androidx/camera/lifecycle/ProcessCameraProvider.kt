/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.lifecycle

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.ExperimentalCameraInfo
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_SINGLE
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED
import androidx.camera.core.concurrent.CameraCoordinator.CameraOperatingMode
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.RestrictedCameraInfo
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.impl.utils.Threads
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.FutureChain
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.lifecycle.ProcessCameraProvider.Companion.getInstance
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.util.Preconditions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.tracing.trace
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.Objects.requireNonNull

/**
 * A singleton which can be used to bind the lifecycle of cameras to any [LifecycleOwner] within an
 * application's process.
 *
 * Only a single process camera provider can exist within a process, and it can be retrieved with
 * [getInstance].
 *
 * Heavyweight resources, such as open and running camera devices, will be scoped to the lifecycle
 * provided to [bindToLifecycle]. Other lightweight resources, such as static camera
 * characteristics, may be retrieved and cached upon first retrieval of this provider with
 * [getInstance], and will persist for the lifetime of the process.
 *
 * This is the standard provider for applications to use.
 */
public class ProcessCameraProvider private constructor() : LifecycleCameraProvider {
    private val mLock = Any()

    @GuardedBy("mLock") private var mCameraXConfigProvider: CameraXConfig.Provider? = null

    @GuardedBy("mLock") private var mCameraXInitializeFuture: ListenableFuture<CameraX>? = null

    @GuardedBy("mLock") private var mCameraXShutdownFuture = Futures.immediateFuture<Void>(null)

    private val mLifecycleCameraRepository = LifecycleCameraRepository()
    private var mCameraX: CameraX? = null
    private var mContext: Context? = null

    @GuardedBy("mLock")
    private val mCameraInfoMap: MutableMap<CameraUseCaseAdapter.CameraId, RestrictedCameraInfo> =
        HashMap()

    /**
     * Allows shutting down this ProcessCameraProvider instance so a new instance can be retrieved
     * by [getInstance].
     *
     * Once shutdownAsync is invoked, a new instance can be retrieved with [getInstance].
     *
     * This method should be used for testing purposes only. Along with [configureInstance], this
     * allows the process camera provider to be used in test suites which may need to initialize
     * CameraX in different ways in between tests.
     *
     * @return A [ListenableFuture] representing the shutdown status. Cancellation of this future is
     *   a no-op.
     */
    @VisibleForTesting
    public fun shutdownAsync(): ListenableFuture<Void> {
        Threads.runOnMainSync {
            unbindAll()
            mLifecycleCameraRepository.clear()
        }

        if (mCameraX != null) {
            mCameraX!!.cameraFactory.cameraCoordinator.shutdown()
        }

        val shutdownFuture =
            if (mCameraX != null) mCameraX!!.shutdown() else Futures.immediateFuture<Void>(null)

        synchronized(mLock) {
            mCameraXConfigProvider = null
            mCameraXInitializeFuture = null
            mCameraXShutdownFuture = shutdownFuture
            mCameraInfoMap.clear()
        }
        mCameraX = null
        mContext = null
        return shutdownFuture
    }

    @MainThread
    public override fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        vararg useCases: UseCase?
    ): Camera =
        trace("CX:bindToLifecycle") {
            if (cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT) {
                throw UnsupportedOperationException(
                    "bindToLifecycle for single camera is not supported in concurrent camera mode, " +
                        "call unbindAll() first"
                )
            }
            cameraOperatingMode = CAMERA_OPERATING_MODE_SINGLE
            val camera =
                bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    null,
                    CompositionSettings.DEFAULT,
                    CompositionSettings.DEFAULT,
                    null,
                    emptyList<CameraEffect>(),
                    *useCases
                )
            return@trace camera
        }

    /**
     * Binds a [UseCaseGroup] to a [LifecycleOwner].
     *
     * Similar to [bindToLifecycle], with the addition that the bound collection of [UseCase] share
     * parameters defined by [UseCaseGroup] such as consistent camera sensor rect across all
     * [UseCase]s.
     *
     * If one [UseCase] is in multiple [UseCaseGroup]s, it will be linked to the [UseCaseGroup] in
     * the latest [bindToLifecycle] call.
     *
     * @throws UnsupportedOperationException If the camera is configured in concurrent mode.
     */
    @MainThread
    public override fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        useCaseGroup: UseCaseGroup
    ): Camera =
        trace("CX:bindToLifecycle-UseCaseGroup") {
            if (cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT) {
                throw UnsupportedOperationException(
                    "bindToLifecycle for single camera is not supported in concurrent camera mode, " +
                        "call unbindAll() first."
                )
            }
            cameraOperatingMode = CAMERA_OPERATING_MODE_SINGLE
            val camera =
                bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    null,
                    CompositionSettings.DEFAULT,
                    CompositionSettings.DEFAULT,
                    useCaseGroup.viewPort,
                    useCaseGroup.effects,
                    *useCaseGroup.useCases.toTypedArray<UseCase>()
                )
            return@trace camera
        }

    @OptIn(ExperimentalCameraInfo::class)
    @MainThread
    public override fun bindToLifecycle(
        singleCameraConfigs: List<SingleCameraConfig?>
    ): ConcurrentCamera =
        trace("CX:bindToLifecycle-Concurrent") {
            if (singleCameraConfigs.size < 2) {
                throw IllegalArgumentException("Concurrent camera needs two camera configs.")
            }

            if (singleCameraConfigs.size > 2) {
                throw IllegalArgumentException(
                    "Concurrent camera is only supporting two cameras at maximum."
                )
            }

            val firstCameraConfig = singleCameraConfigs[0]!!
            val secondCameraConfig = singleCameraConfigs[1]!!

            val cameras: MutableList<Camera> = ArrayList()
            if (
                firstCameraConfig.cameraSelector.lensFacing ==
                    secondCameraConfig.cameraSelector.lensFacing
            ) {
                if (cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT) {
                    throw UnsupportedOperationException(
                        "Camera is already running, call unbindAll() before binding more cameras."
                    )
                }
                if (
                    firstCameraConfig.lifecycleOwner != secondCameraConfig.lifecycleOwner ||
                        firstCameraConfig.useCaseGroup.viewPort !=
                            secondCameraConfig.useCaseGroup.viewPort ||
                        firstCameraConfig.useCaseGroup.effects !=
                            secondCameraConfig.useCaseGroup.effects
                ) {
                    throw IllegalArgumentException(
                        "Two camera configs need to have the same lifecycle owner, view port and " +
                            "effects."
                    )
                }
                val lifecycleOwner = firstCameraConfig.lifecycleOwner
                val cameraSelector = firstCameraConfig.cameraSelector
                val viewPort = firstCameraConfig.useCaseGroup.viewPort
                val effects = firstCameraConfig.useCaseGroup.effects
                val useCases: MutableList<UseCase> = ArrayList()
                for (config: SingleCameraConfig? in singleCameraConfigs) {
                    // Connect physical camera id with use case.
                    for (useCase: UseCase in config!!.useCaseGroup.useCases) {
                        config.cameraSelector.physicalCameraId?.let {
                            useCase.setPhysicalCameraId(it)
                        }
                    }
                    useCases.addAll(config.useCaseGroup.useCases)
                }

                cameraOperatingMode = CAMERA_OPERATING_MODE_SINGLE
                val camera =
                    bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        null,
                        CompositionSettings.DEFAULT,
                        CompositionSettings.DEFAULT,
                        viewPort,
                        effects,
                        *useCases.toTypedArray<UseCase>()
                    )
                cameras.add(camera)
            } else {
                if (!mContext!!.packageManager.hasSystemFeature(FEATURE_CAMERA_CONCURRENT)) {
                    throw UnsupportedOperationException(
                        "Concurrent camera is not supported on the device."
                    )
                }

                if (cameraOperatingMode == CAMERA_OPERATING_MODE_SINGLE) {
                    throw UnsupportedOperationException(
                        "Camera is already running, call unbindAll() before binding more cameras."
                    )
                }

                val cameraInfosToBind: MutableList<CameraInfo> = ArrayList()
                val firstCameraInfo: CameraInfo
                val secondCameraInfo: CameraInfo
                try {
                    firstCameraInfo = getCameraInfo(firstCameraConfig.cameraSelector)
                    secondCameraInfo = getCameraInfo(secondCameraConfig.cameraSelector)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid camera selectors in camera configs.")
                }
                cameraInfosToBind.add(firstCameraInfo)
                cameraInfosToBind.add(secondCameraInfo)
                if (
                    activeConcurrentCameraInfos.isNotEmpty() &&
                        cameraInfosToBind != activeConcurrentCameraInfos
                ) {
                    throw UnsupportedOperationException(
                        "Cameras are already running, call unbindAll() before binding more cameras."
                    )
                }

                cameraOperatingMode = CAMERA_OPERATING_MODE_CONCURRENT

                // For dual camera video capture, we are only supporting two use cases:
                // Preview + VideoCapture. If ImageCapture support is added, the validation logic
                // will be updated accordingly.
                var isDualCameraVideoCapture = false
                if (
                    Objects.equals(
                        firstCameraConfig.useCaseGroup.useCases,
                        secondCameraConfig.useCaseGroup.useCases
                    ) && firstCameraConfig.useCaseGroup.useCases.size == 2
                ) {
                    val useCase0 = firstCameraConfig.useCaseGroup.useCases[0]
                    val useCase1 = firstCameraConfig.useCaseGroup.useCases[1]
                    isDualCameraVideoCapture =
                        (isVideoCapture(useCase0) && isPreview(useCase1)) ||
                            (isPreview(useCase0) && isVideoCapture(useCase1))
                }

                if (isDualCameraVideoCapture) {
                    cameras.add(
                        bindToLifecycle(
                            firstCameraConfig.lifecycleOwner,
                            firstCameraConfig.cameraSelector,
                            secondCameraConfig.cameraSelector,
                            firstCameraConfig.compositionSettings,
                            secondCameraConfig.compositionSettings,
                            firstCameraConfig.useCaseGroup.viewPort,
                            firstCameraConfig.useCaseGroup.effects,
                            *firstCameraConfig.useCaseGroup.useCases.toTypedArray<UseCase>(),
                        )
                    )
                } else {
                    for (config: SingleCameraConfig? in singleCameraConfigs) {
                        val camera =
                            bindToLifecycle(
                                config!!.lifecycleOwner,
                                config.cameraSelector,
                                null,
                                CompositionSettings.DEFAULT,
                                CompositionSettings.DEFAULT,
                                config.useCaseGroup.viewPort,
                                config.useCaseGroup.effects,
                                *config.useCaseGroup.useCases.toTypedArray<UseCase>()
                            )
                        cameras.add(camera)
                    }
                }
                activeConcurrentCameraInfos = cameraInfosToBind
            }
            return@trace ConcurrentCamera(cameras)
        }

    private fun isVideoCapture(useCase: UseCase): Boolean {
        return useCase.currentConfig.containsOption(UseCaseConfig.OPTION_CAPTURE_TYPE) &&
            useCase.currentConfig.captureType == CaptureType.VIDEO_CAPTURE
    }

    private fun isPreview(useCase: UseCase): Boolean {
        return useCase is Preview
    }

    /**
     * Binds [ViewPort] and a collection of [UseCase] to a [LifecycleOwner].
     *
     * The state of the lifecycle will determine when the cameras are open, started, stopped and
     * closed. When started, the use cases receive camera data.
     *
     * Binding to a [LifecycleOwner] in state currently in [Lifecycle.State.STARTED] or greater will
     * also initialize and start data capture. If the camera was already running this may cause a
     * new initialization to occur temporarily stopping data from the camera before restarting it.
     *
     * Multiple use cases can be bound via adding them all to a single [bindToLifecycle] call, or by
     * using multiple [bindToLifecycle] calls. Using a single call that includes all the use cases
     * helps to set up a camera session correctly for all uses cases, such as by allowing
     * determination of resolutions depending on all the use cases bound being bound. If the use
     * cases are bound separately, it will find the supported resolution with the priority depending
     * on the binding sequence. If the use cases are bound with a single call, it will find the
     * supported resolution with the priority in sequence of [ImageCapture], [Preview] and then
     * [ImageAnalysis]. The resolutions that can be supported depends on the camera device hardware
     * level that there are some default guaranteed resolutions listed in
     * [android.hardware.camera2.CameraDevice.createCaptureSession].
     *
     * Currently up to 3 use cases may be bound to a [Lifecycle] at any time. Exceeding capability
     * of target camera device will throw an IllegalArgumentException.
     *
     * A [UseCase] should only be bound to a single lifecycle and camera selector a time. Attempting
     * to bind a use case to a lifecycle when it is already bound to another lifecycle is an error,
     * and the use case binding will not change. Attempting to bind the same use case to multiple
     * camera selectors is also an error and will not change the binding.
     *
     * If different use cases are bound to different camera selectors that resolve to distinct
     * cameras, but the same lifecycle, only one of the cameras will operate at a time. The
     * non-operating camera will not become active until it is the only camera with use cases bound.
     *
     * The [Camera] returned is determined by the given camera selector, plus other internal
     * requirements, possibly from use case configurations. The camera returned from
     * [bindToLifecycle] may differ from the camera determined solely by a camera selector. If the
     * camera selector can't resolve a camera under the requirements, an [IllegalArgumentException]
     * will be thrown.
     *
     * Only [UseCase] bound to latest active [Lifecycle] can keep alive. [UseCase] bound to other
     * [Lifecycle] will be stopped.
     *
     * @param lifecycleOwner The [LifecycleOwner] which controls the lifecycle transitions of the
     *   use cases.
     * @param primaryCameraSelector The primary camera selector which determines the camera to use
     *   for set of use cases.
     * @param secondaryCameraSelector The secondary camera selector in dual camera case.
     * @param primaryCompositionSettings The composition settings for the primary camera.
     * @param secondaryCompositionSettings The composition settings for the secondary camera.
     * @param viewPort The viewPort which represents the visible camera sensor rect.
     * @param effects The effects applied to the camera outputs.
     * @param useCases The use cases to bind to a lifecycle.
     * @return The [Camera] instance which is determined by the camera selector and internal
     *   requirements.
     * @throws IllegalStateException If the use case has already been bound to another lifecycle or
     *   method is not called on main thread.
     * @throws IllegalArgumentException If the provided camera selector is unable to resolve a
     *   camera to be used for the given use cases.
     */
    @Suppress("unused")
    @OptIn(ExperimentalCameraInfo::class)
    internal fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        primaryCameraSelector: CameraSelector,
        secondaryCameraSelector: CameraSelector?,
        primaryCompositionSettings: CompositionSettings,
        secondaryCompositionSettings: CompositionSettings,
        viewPort: ViewPort?,
        effects: List<CameraEffect?>,
        vararg useCases: UseCase?
    ): Camera =
        trace("CX:bindToLifecycle-internal") {
            Threads.checkMainThread()
            // TODO(b/153096869): override UseCase's target rotation.

            // Get the LifecycleCamera if existed.
            val primaryCameraInternal =
                primaryCameraSelector.select(mCameraX!!.cameraRepository.cameras)
            primaryCameraInternal.setPrimary(true)
            val primaryRestrictedCameraInfo =
                getCameraInfo(primaryCameraSelector) as RestrictedCameraInfo

            var secondaryCameraInternal: CameraInternal? = null
            var secondaryRestrictedCameraInfo: RestrictedCameraInfo? = null
            if (secondaryCameraSelector != null) {
                secondaryCameraInternal =
                    secondaryCameraSelector.select(mCameraX!!.cameraRepository.cameras)
                secondaryCameraInternal.setPrimary(false)
                secondaryRestrictedCameraInfo =
                    getCameraInfo(secondaryCameraSelector) as RestrictedCameraInfo
            }

            var lifecycleCameraToBind =
                mLifecycleCameraRepository.getLifecycleCamera(
                    lifecycleOwner,
                    CameraUseCaseAdapter.generateCameraId(
                        primaryRestrictedCameraInfo,
                        secondaryRestrictedCameraInfo
                    )
                )

            // Check if there's another camera that has already been bound.
            val lifecycleCameras = mLifecycleCameraRepository.lifecycleCameras
            useCases.filterNotNull().forEach { useCase ->
                for (lifecycleCamera: LifecycleCamera in lifecycleCameras) {
                    if (
                        lifecycleCamera.isBound(useCase) && lifecycleCamera != lifecycleCameraToBind
                    ) {
                        throw IllegalStateException(
                            String.format(
                                "Use case %s already bound to a different lifecycle.",
                                useCase
                            )
                        )
                    }
                }
            }

            // Create the LifecycleCamera if there's no existing one that can be used.
            if (lifecycleCameraToBind == null) {
                lifecycleCameraToBind =
                    mLifecycleCameraRepository.createLifecycleCamera(
                        lifecycleOwner,
                        CameraUseCaseAdapter(
                            primaryCameraInternal,
                            secondaryCameraInternal,
                            primaryRestrictedCameraInfo,
                            secondaryRestrictedCameraInfo,
                            primaryCompositionSettings,
                            secondaryCompositionSettings,
                            mCameraX!!.cameraFactory.cameraCoordinator,
                            mCameraX!!.cameraDeviceSurfaceManager,
                            mCameraX!!.defaultConfigFactory
                        )
                    )
            }

            if (useCases.isEmpty()) {
                return@trace lifecycleCameraToBind!!
            }

            mLifecycleCameraRepository.bindToLifecycleCamera(
                lifecycleCameraToBind!!,
                viewPort,
                effects,
                listOf(*useCases),
                mCameraX!!.cameraFactory.cameraCoordinator
            )

            return@trace lifecycleCameraToBind
        }

    public override fun isBound(useCase: UseCase): Boolean {
        for (lifecycleCamera: LifecycleCamera in mLifecycleCameraRepository.lifecycleCameras) {
            if (lifecycleCamera.isBound(useCase)) {
                return true
            }
        }

        return false
    }

    /**
     * Unbinds all specified use cases from the lifecycle.
     *
     * This will initiate a close of every open camera which has zero [UseCase] associated with it
     * at the end of this call.
     *
     * If a use case in the argument list is not bound, then it is simply ignored.
     *
     * After unbinding a UseCase, the UseCase can be and bound to another [Lifecycle] however
     * listeners and settings should be reset by the application.
     *
     * @param useCases The collection of use cases to remove.
     * @throws IllegalStateException If not called on main thread.
     * @throws UnsupportedOperationException If called in concurrent mode.
     */
    @MainThread
    public override fun unbind(vararg useCases: UseCase?): Unit =
        trace("CX:unbind") {
            Threads.checkMainThread()

            if (cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT) {
                throw UnsupportedOperationException(
                    "Unbind usecase is not supported in concurrent camera mode, call unbindAll() first."
                )
            }

            mLifecycleCameraRepository.unbind(listOf(*useCases))
        }

    @MainThread
    public override fun unbindAll(): Unit =
        trace("CX:unbindAll") {
            Threads.checkMainThread()
            cameraOperatingMode = CAMERA_OPERATING_MODE_UNSPECIFIED
            mLifecycleCameraRepository.unbindAll()
        }

    @Throws(CameraInfoUnavailableException::class)
    override fun hasCamera(cameraSelector: CameraSelector): Boolean =
        trace("CX:hasCamera") {
            try {
                cameraSelector.select(mCameraX!!.cameraRepository.cameras)
            } catch (e: IllegalArgumentException) {
                return@trace false
            }

            return@trace true
        }

    override val availableCameraInfos: List<CameraInfo>
        get() =
            trace("CX:getAvailableCameraInfos") {
                val availableCameraInfos: MutableList<CameraInfo> = ArrayList()
                val cameras: Set<CameraInternal> = mCameraX!!.cameraRepository.cameras
                for (camera: CameraInternal in cameras) {
                    availableCameraInfos.add(camera.cameraInfo)
                }
                return@trace availableCameraInfos
            }

    final override val availableConcurrentCameraInfos: List<List<CameraInfo>>
        @OptIn(ExperimentalCameraInfo::class)
        get() =
            trace("CX:getAvailableConcurrentCameraInfos") {
                requireNonNull(mCameraX)
                requireNonNull(mCameraX!!.cameraFactory.cameraCoordinator)
                val concurrentCameraSelectorLists =
                    mCameraX!!.cameraFactory.cameraCoordinator.concurrentCameraSelectors

                val availableConcurrentCameraInfos: MutableList<List<CameraInfo>> = ArrayList()
                for (cameraSelectors in concurrentCameraSelectorLists) {
                    val cameraInfos: MutableList<CameraInfo> = ArrayList()
                    for (cameraSelector in cameraSelectors) {
                        var cameraInfo: CameraInfo
                        try {
                            cameraInfo = getCameraInfo(cameraSelector)
                        } catch (e: IllegalArgumentException) {
                            continue
                        }
                        cameraInfos.add(cameraInfo)
                    }
                    availableConcurrentCameraInfos.add(cameraInfos)
                }
                return@trace availableConcurrentCameraInfos
            }

    @ExperimentalCameraInfo
    override fun getCameraInfo(cameraSelector: CameraSelector): CameraInfo =
        trace("CX:getCameraInfo") {
            val cameraInfoInternal =
                cameraSelector.select(mCameraX!!.cameraRepository.cameras).cameraInfoInternal
            val cameraConfig = getCameraConfig(cameraSelector, cameraInfoInternal)

            val key =
                CameraUseCaseAdapter.CameraId.create(
                    cameraInfoInternal.cameraId,
                    cameraConfig.compatibilityId
                )
            var restrictedCameraInfo: RestrictedCameraInfo?
            synchronized(mLock) {
                restrictedCameraInfo = mCameraInfoMap[key]
                if (restrictedCameraInfo == null) {
                    restrictedCameraInfo = RestrictedCameraInfo(cameraInfoInternal, cameraConfig)
                    mCameraInfoMap[key] = restrictedCameraInfo!!
                }
            }

            return@trace restrictedCameraInfo!!
        }

    final override val isConcurrentCameraModeOn: Boolean
        @MainThread get() = cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT

    private fun getOrCreateCameraXInstance(context: Context): ListenableFuture<CameraX> {
        synchronized(mLock) {
            if (mCameraXInitializeFuture != null) {
                return mCameraXInitializeFuture as ListenableFuture<CameraX>
            }
            val cameraX = CameraX(context, mCameraXConfigProvider)

            mCameraXInitializeFuture =
                CallbackToFutureAdapter.getFuture { completer ->
                    synchronized(mLock) {
                        val future: ListenableFuture<Void> =
                            FutureChain.from(mCameraXShutdownFuture)
                                .transformAsync(
                                    { cameraX.initializeFuture },
                                    CameraXExecutors.directExecutor()
                                )
                        Futures.addCallback(
                            future,
                            object : FutureCallback<Void?> {
                                override fun onSuccess(result: Void?) {
                                    completer.set(cameraX)
                                }

                                override fun onFailure(t: Throwable) {
                                    completer.setException(t)
                                }
                            },
                            CameraXExecutors.directExecutor()
                        )
                    }

                    "ProcessCameraProvider-initializeCameraX"
                }

            return mCameraXInitializeFuture as ListenableFuture<CameraX>
        }
    }

    private fun configureInstanceInternal(cameraXConfig: CameraXConfig) =
        trace("CX:configureInstanceInternal") {
            synchronized(mLock) {
                Preconditions.checkNotNull(cameraXConfig)
                Preconditions.checkState(
                    mCameraXConfigProvider == null,
                    "CameraX has already been configured. To use a different configuration, " +
                        "shutdown() must be called."
                )
                mCameraXConfigProvider = CameraXConfig.Provider { cameraXConfig }
            }
        }

    private fun getCameraConfig(
        cameraSelector: CameraSelector,
        cameraInfo: CameraInfo
    ): CameraConfig {
        var cameraConfig: CameraConfig? = null
        for (cameraFilter: CameraFilter in cameraSelector.cameraFilterSet) {
            if (cameraFilter.identifier != CameraFilter.DEFAULT_ID) {
                val extendedCameraConfig =
                    ExtendedCameraConfigProviderStore.getConfigProvider(cameraFilter.identifier)
                        .getConfig(cameraInfo, (mContext)!!)
                if (extendedCameraConfig == null) { // ignore IDs unrelated to camera configs.
                    continue
                }

                // Only allows one camera config now.
                if (cameraConfig != null) {
                    throw IllegalArgumentException(
                        "Cannot apply multiple extended camera configs at the same time."
                    )
                }
                cameraConfig = extendedCameraConfig
            }
        }

        if (cameraConfig == null) {
            cameraConfig = CameraConfigs.defaultConfig()
        }
        return cameraConfig
    }

    private fun setCameraX(cameraX: CameraX) {
        mCameraX = cameraX
    }

    private fun setContext(context: Context) {
        mContext = context
    }

    @get:CameraOperatingMode
    private var cameraOperatingMode: Int
        get() {
            if (mCameraX == null) {
                return CAMERA_OPERATING_MODE_UNSPECIFIED
            }
            return mCameraX!!.cameraFactory.cameraCoordinator.cameraOperatingMode
        }
        private set(cameraOperatingMode) {
            if (mCameraX == null) {
                return
            }
            mCameraX!!.cameraFactory.cameraCoordinator.cameraOperatingMode = cameraOperatingMode
        }

    private var activeConcurrentCameraInfos: List<CameraInfo>
        get() {
            if (mCameraX == null) {
                return java.util.ArrayList()
            }
            return mCameraX!!.cameraFactory.cameraCoordinator.activeConcurrentCameraInfos
        }
        private set(cameraInfos) {
            if (mCameraX == null) {
                return
            }
            mCameraX!!.cameraFactory.cameraCoordinator.activeConcurrentCameraInfos = cameraInfos
        }

    public companion object {
        private val sAppInstance = ProcessCameraProvider()

        /**
         * Retrieves the ProcessCameraProvider associated with the current process.
         *
         * The instance returned here can be used to bind use cases to any [LifecycleOwner] with
         * [bindToLifecycle].
         *
         * The instance's configuration may be customized by subclassing the application's
         * [Application] class and implementing [CameraXConfig.Provider]. For example, the sample
         * implements [CameraXConfig.Provider.getCameraXConfig] and initializes this process camera
         * provider with a [Camera2 implementation][androidx.camera.camera2.Camera2Config] from
         * [androidx.camera.camera2], and with a custom executor.
         *
         * @sample androidx.camera.lifecycle.samples.getCameraXConfigSample
         *
         * If it isn't possible to subclass the [Application] class, such as in library code, then
         * the singleton can be configured via [configureInstance] before the first invocation of
         * `getInstance(context)`, the sample implements a customized camera provider that
         * configures the instance before getting it.
         *
         * @sample androidx.camera.lifecycle.samples.configureAndGetInstanceSample
         *
         * If no [CameraXConfig.Provider] is implemented by [Application], or if the singleton has
         * not been configured via [configureInstance] a default configuration will be used.
         *
         * @param context The application context.
         * @return A future which will contain the ProcessCameraProvider. Cancellation of this
         *   future is a no-op. This future may fail with an [InitializationException] and
         *   associated cause that can be retrieved by [Throwable.cause]. The cause will be a
         *   [androidx.camera.core.CameraUnavailableException] if it fails to access any camera
         *   during initialization.
         * @throws IllegalStateException if CameraX fails to initialize via a default provider or a
         *   [CameraXConfig.Provider].
         * @see configureInstance
         */
        @Suppress("AsyncSuffixFuture")
        @JvmStatic
        public fun getInstance(context: Context): ListenableFuture<ProcessCameraProvider> {
            Preconditions.checkNotNull(context)
            return Futures.transform(
                sAppInstance.getOrCreateCameraXInstance(context),
                { cameraX ->
                    sAppInstance.setCameraX(cameraX)
                    sAppInstance.setContext(ContextUtil.getApplicationContext(context))
                    sAppInstance
                },
                CameraXExecutors.directExecutor()
            )
        }

        /**
         * Perform one-time configuration of the ProcessCameraProvider singleton with the given
         * [CameraXConfig].
         *
         * This method allows configuration of the camera provider via [CameraXConfig]. All
         * initialization tasks, such as communicating with the camera service, will be executed on
         * the [java.util.concurrent.Executor] set by [CameraXConfig.Builder.setCameraExecutor], or
         * by an internally defined executor if none is provided.
         *
         * This method is not required for every application. If the method is not called and
         * [CameraXConfig.Provider] is not implemented in [Application], default configuration will
         * be used.
         *
         * Once this method is called, the instance configured by the given [CameraXConfig] can be
         * retrieved with [getInstance]. [CameraXConfig.Provider] implemented in [Application] will
         * be ignored.
         *
         * Configuration can only occur once. Once the ProcessCameraProvider has been configured
         * with `configureInstance()` or [getInstance], this method will throw an
         * [IllegalStateException]. Because configuration can only occur once, **usage of this
         * method from library code is not recommended** as the application owner should ultimately
         * be in control of singleton configuration.
         *
         * @param cameraXConfig configuration options for the singleton process camera provider
         *   instance.
         * @throws IllegalStateException If the camera provider has already been configured by a
         *   previous call to `configureInstance()` or [getInstance].
         */
        @JvmStatic
        @ExperimentalCameraProviderConfiguration
        public fun configureInstance(cameraXConfig: CameraXConfig): Unit =
            trace("CX:configureInstance") { sAppInstance.configureInstanceInternal(cameraXConfig) }
    }
}
