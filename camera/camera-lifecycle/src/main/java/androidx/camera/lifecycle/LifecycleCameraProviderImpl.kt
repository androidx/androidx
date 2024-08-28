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

import android.content.Context
import android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
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
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.util.Preconditions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.tracing.trace
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.Objects.requireNonNull

/** Implementation of the [LifecycleCameraProvider] interface. */
internal class LifecycleCameraProviderImpl : LifecycleCameraProvider {
    private val lock = Any()
    @GuardedBy("mLock") private var cameraXConfigProvider: CameraXConfig.Provider? = null
    @GuardedBy("mLock") private var cameraXInitializeFuture: ListenableFuture<Void>? = null
    @GuardedBy("mLock") private var cameraXShutdownFuture = Futures.immediateFuture<Void>(null)
    private val lifecycleCameraRepository = LifecycleCameraRepository()
    private var cameraX: CameraX? = null
    private var context: Context? = null
    @GuardedBy("mLock")
    private val cameraInfoMap: MutableMap<CameraUseCaseAdapter.CameraId, RestrictedCameraInfo> =
        HashMap()

    internal fun initAsync(
        context: Context,
        cameraXConfig: CameraXConfig? = null
    ): ListenableFuture<Void> {
        synchronized(lock) {
            if (cameraXInitializeFuture != null) {
                return cameraXInitializeFuture as ListenableFuture<Void>
            }
            cameraXConfig?.let { configure(it) }
            val cameraX = CameraX(context, cameraXConfigProvider)

            cameraXInitializeFuture =
                CallbackToFutureAdapter.getFuture { completer ->
                    synchronized(lock) {
                        val future: ListenableFuture<Void> =
                            FutureChain.from(cameraXShutdownFuture)
                                .transformAsync(
                                    { cameraX.initializeFuture },
                                    CameraXExecutors.directExecutor()
                                )
                        Futures.addCallback(
                            future,
                            object : FutureCallback<Void?> {
                                override fun onSuccess(result: Void?) {
                                    this@LifecycleCameraProviderImpl.cameraX = cameraX
                                    this@LifecycleCameraProviderImpl.context =
                                        ContextUtil.getApplicationContext(context)
                                    completer.set(null)
                                }

                                override fun onFailure(t: Throwable) {
                                    completer.setException(t)
                                }
                            },
                            CameraXExecutors.directExecutor()
                        )
                    }

                    "LifecycleCameraProvider-initialize"
                }

            return cameraXInitializeFuture as ListenableFuture<Void>
        }
    }

    /**
     * Configures the camera provider.
     *
     * The camera provider can only be configured once. Trying to configure it multiple times will
     * throw an [IllegalStateException].
     *
     * @param cameraXConfig The CameraX configuration.
     */
    internal fun configure(cameraXConfig: CameraXConfig) =
        trace("CX:configureInstanceInternal") {
            synchronized(lock) {
                Preconditions.checkNotNull(cameraXConfig)
                Preconditions.checkState(
                    cameraXConfigProvider == null,
                    "CameraX has already been configured. To use a different configuration, " +
                        "shutdown() must be called."
                )
                cameraXConfigProvider = CameraXConfig.Provider { cameraXConfig }
            }
        }

    internal fun shutdownAsync(): ListenableFuture<Void> {
        Threads.runOnMainSync {
            unbindAll()
            lifecycleCameraRepository.clear()
        }

        if (cameraX != null) {
            cameraX!!.cameraFactory.cameraCoordinator.shutdown()
        }

        val shutdownFuture =
            if (cameraX != null) cameraX!!.shutdown() else Futures.immediateFuture<Void>(null)

        synchronized(lock) {
            cameraXConfigProvider = null
            cameraXInitializeFuture = null
            cameraXShutdownFuture = shutdownFuture
            cameraInfoMap.clear()
        }
        cameraX = null
        context = null
        return shutdownFuture
    }

    override fun isBound(useCase: UseCase): Boolean {
        for (lifecycleCamera: LifecycleCamera in lifecycleCameraRepository.lifecycleCameras) {
            if (lifecycleCamera.isBound(useCase)) {
                return true
            }
        }

        return false
    }

    @MainThread
    override fun unbind(vararg useCases: UseCase?): Unit =
        trace("CX:unbind") {
            Threads.checkMainThread()

            if (cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT) {
                throw UnsupportedOperationException(
                    "Unbind usecase is not supported in concurrent camera mode, call unbindAll() first."
                )
            }

            lifecycleCameraRepository.unbind(listOf(*useCases))
        }

    @MainThread
    override fun unbindAll(): Unit =
        trace("CX:unbindAll") {
            Threads.checkMainThread()
            cameraOperatingMode = CAMERA_OPERATING_MODE_UNSPECIFIED
            lifecycleCameraRepository.unbindAll()
        }

    @Throws(CameraInfoUnavailableException::class)
    override fun hasCamera(cameraSelector: CameraSelector): Boolean =
        trace("CX:hasCamera") {
            try {
                cameraSelector.select(cameraX!!.cameraRepository.cameras)
            } catch (e: IllegalArgumentException) {
                return@trace false
            }

            return@trace true
        }

    @MainThread
    override fun bindToLifecycle(
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

    @MainThread
    override fun bindToLifecycle(singleCameraConfigs: List<SingleCameraConfig?>): ConcurrentCamera =
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
                if (!context!!.packageManager.hasSystemFeature(FEATURE_CAMERA_CONCURRENT)) {
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

    override val availableCameraInfos: List<CameraInfo>
        get() =
            trace("CX:getAvailableCameraInfos") {
                val availableCameraInfos: MutableList<CameraInfo> = ArrayList()
                val cameras: Set<CameraInternal> = cameraX!!.cameraRepository.cameras
                for (camera: CameraInternal in cameras) {
                    availableCameraInfos.add(camera.cameraInfo)
                }
                return@trace availableCameraInfos
            }

    override val availableConcurrentCameraInfos: List<List<CameraInfo>>
        get() =
            trace("CX:getAvailableConcurrentCameraInfos") {
                requireNonNull(cameraX)
                requireNonNull(cameraX!!.cameraFactory.cameraCoordinator)
                val concurrentCameraSelectorLists =
                    cameraX!!.cameraFactory.cameraCoordinator.concurrentCameraSelectors

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

    override val isConcurrentCameraModeOn: Boolean
        /**
         * Returns whether there is a [ConcurrentCamera] bound.
         *
         * @return `true` if there is a [ConcurrentCamera] bound, otherwise `false`.
         */
        @MainThread get() = cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT

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
    private fun bindToLifecycle(
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
                primaryCameraSelector.select(cameraX!!.cameraRepository.cameras)
            primaryCameraInternal.setPrimary(true)
            val primaryRestrictedCameraInfo =
                getCameraInfo(primaryCameraSelector) as RestrictedCameraInfo

            var secondaryCameraInternal: CameraInternal? = null
            var secondaryRestrictedCameraInfo: RestrictedCameraInfo? = null
            if (secondaryCameraSelector != null) {
                secondaryCameraInternal =
                    secondaryCameraSelector.select(cameraX!!.cameraRepository.cameras)
                secondaryCameraInternal.setPrimary(false)
                secondaryRestrictedCameraInfo =
                    getCameraInfo(secondaryCameraSelector) as RestrictedCameraInfo
            }

            var lifecycleCameraToBind =
                lifecycleCameraRepository.getLifecycleCamera(
                    lifecycleOwner,
                    CameraUseCaseAdapter.generateCameraId(
                        primaryRestrictedCameraInfo,
                        secondaryRestrictedCameraInfo
                    )
                )

            // Check if there's another camera that has already been bound.
            val lifecycleCameras = lifecycleCameraRepository.lifecycleCameras
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
                    lifecycleCameraRepository.createLifecycleCamera(
                        lifecycleOwner,
                        CameraUseCaseAdapter(
                            primaryCameraInternal,
                            secondaryCameraInternal,
                            primaryRestrictedCameraInfo,
                            secondaryRestrictedCameraInfo,
                            primaryCompositionSettings,
                            secondaryCompositionSettings,
                            cameraX!!.cameraFactory.cameraCoordinator,
                            cameraX!!.cameraDeviceSurfaceManager,
                            cameraX!!.defaultConfigFactory
                        )
                    )
            }

            if (useCases.isEmpty()) {
                return@trace lifecycleCameraToBind!!
            }

            lifecycleCameraRepository.bindToLifecycleCamera(
                lifecycleCameraToBind!!,
                viewPort,
                effects,
                listOf(*useCases),
                cameraX!!.cameraFactory.cameraCoordinator
            )

            return@trace lifecycleCameraToBind
        }

    override fun getCameraInfo(cameraSelector: CameraSelector): CameraInfo =
        trace("CX:getCameraInfo") {
            val cameraInfoInternal =
                cameraSelector.select(cameraX!!.cameraRepository.cameras).cameraInfoInternal
            val cameraConfig = getCameraConfig(cameraSelector, cameraInfoInternal)

            val key =
                CameraUseCaseAdapter.CameraId.create(
                    cameraInfoInternal.cameraId,
                    cameraConfig.compatibilityId
                )
            var restrictedCameraInfo: RestrictedCameraInfo?
            synchronized(lock) {
                restrictedCameraInfo = cameraInfoMap[key]
                if (restrictedCameraInfo == null) {
                    restrictedCameraInfo = RestrictedCameraInfo(cameraInfoInternal, cameraConfig)
                    cameraInfoMap[key] = restrictedCameraInfo!!
                }
            }

            return@trace restrictedCameraInfo!!
        }

    private fun isVideoCapture(useCase: UseCase): Boolean {
        return useCase.currentConfig.containsOption(UseCaseConfig.OPTION_CAPTURE_TYPE) &&
            useCase.currentConfig.captureType == CaptureType.VIDEO_CAPTURE
    }

    private fun isPreview(useCase: UseCase): Boolean {
        return useCase is Preview
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
                        .getConfig(cameraInfo, (context)!!)
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

    @get:CameraOperatingMode
    private var cameraOperatingMode: Int
        get() {
            if (cameraX == null) {
                return CAMERA_OPERATING_MODE_UNSPECIFIED
            }
            return cameraX!!.cameraFactory.cameraCoordinator.cameraOperatingMode
        }
        private set(cameraOperatingMode) {
            if (cameraX == null) {
                return
            }
            cameraX!!.cameraFactory.cameraCoordinator.cameraOperatingMode = cameraOperatingMode
        }

    private var activeConcurrentCameraInfos: List<CameraInfo>
        get() {
            if (cameraX == null) {
                return java.util.ArrayList()
            }
            return cameraX!!.cameraFactory.cameraCoordinator.activeConcurrentCameraInfos
        }
        private set(cameraInfos) {
            if (cameraX == null) {
                return
            }
            cameraX!!.cameraFactory.cameraCoordinator.activeConcurrentCameraInfos = cameraInfos
        }

    companion object {
        private const val TAG = "LifecycleCameraProvider"
    }
}
