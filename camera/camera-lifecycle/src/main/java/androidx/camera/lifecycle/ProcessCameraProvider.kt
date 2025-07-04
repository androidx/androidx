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

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider.Companion.configureInstance
import androidx.camera.lifecycle.ProcessCameraProvider.Companion.getInstance
import androidx.core.util.Preconditions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.tracing.trace
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
@OptIn(ExperimentalCameraProviderConfiguration::class)
@SuppressLint("NullAnnotationGroup")
public class ProcessCameraProvider
private constructor(private val lifecycleCameraProvider: LifecycleCameraProviderImpl) :
    CameraProvider {

    /**
     * Returns `true` if this [UseCase] is bound to a lifecycle or included in a bound
     * [SessionConfig]. Otherwise returns `false`.
     *
     * After binding a use case, use cases remain bound until the lifecycle reaches a
     * [Lifecycle.State.DESTROYED] state or if is unbound by calls to [unbind] or [unbindAll].
     */
    public fun isBound(useCase: UseCase): Boolean {
        return lifecycleCameraProvider.isBound(useCase)
    }

    /**
     * Returns `true` if the [SessionConfig] is bound to a lifecycle. Otherwise returns `false`.
     *
     * After binding a [SessionConfig], this [SessionConfig] remains bound until the lifecycle
     * reaches a [Lifecycle.State.DESTROYED] state or if is unbound by calls to [unbind] or
     * [unbindAll].
     */
    @ExperimentalSessionConfig
    public fun isBound(sessionConfig: SessionConfig): Boolean {
        return lifecycleCameraProvider.isBound(sessionConfig)
    }

    /**
     * Unbinds all specified use cases from the lifecycle provider.
     *
     * This will initiate a close of every open camera which has zero [UseCase] associated with it
     * at the end of this call.
     *
     * If a use case in the argument list is not bound, then it is simply ignored.
     *
     * After unbinding a UseCase, the UseCase can be bound to another [Lifecycle] however listeners
     * and settings should be reset by the application.
     *
     * @param useCases The collection of use cases to remove.
     * @throws IllegalStateException If not called on main thread.
     * @throws UnsupportedOperationException If called in concurrent mode.
     */
    @MainThread
    public fun unbind(vararg useCases: UseCase?) {
        return lifecycleCameraProvider.unbind(*useCases)
    }

    /**
     * Unbinds the [SessionConfig] from the lifecycle provider.
     *
     * This [SessionConfig] contains the [UseCase]s to be detached from the camera. This will
     * initiate a close of every open camera which has zero [UseCase] associated with it at the end
     * of this call.
     *
     * After unbinding the [SessionConfig], another [SessionConfig] can be bound again and its
     * [UseCase]s can be bound to another [Lifecycle].
     *
     * @param sessionConfig The sessionConfig that contains the collection of use cases to remove.
     * @throws IllegalStateException If not called on main thread.
     * @throws UnsupportedOperationException If called in concurrent mode.
     */
    @ExperimentalSessionConfig
    public fun unbind(sessionConfig: SessionConfig) {
        return lifecycleCameraProvider.unbind(sessionConfig)
    }

    /**
     * Unbinds all use cases from the lifecycle provider and removes them from CameraX.
     *
     * This will initiate a close of every currently open camera.
     *
     * @throws IllegalStateException If not called on main thread.
     */
    @MainThread
    public fun unbindAll() {
        return lifecycleCameraProvider.unbindAll()
    }

    /**
     * Binds the collection of [UseCase] to a [LifecycleOwner].
     *
     * The state of the lifecycle will determine when the cameras are open, started, stopped and
     * closed. When started, the use cases receive camera data.
     *
     * Binding to a lifecycleOwner in state currently in [Lifecycle.State.STARTED] or greater will
     * also initialize and start data capture. If the camera was already running this may cause a
     * new initialization to occur temporarily stopping data from the camera before restarting it.
     *
     * Multiple use cases can be bound via adding them all to a single bindToLifecycle call, or by
     * using multiple bindToLifecycle calls. Using a single call that includes all the use cases
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
     * A UseCase should only be bound to a single lifecycle and camera selector a time. Attempting
     * to bind a use case to a lifecycle when it is already bound to another lifecycle is an error,
     * and the use case binding will not change. Attempting to bind the same use case to multiple
     * camera selectors is also an error and will not change the binding.
     *
     * Binding different use cases to the same lifecycle with different camera selectors that
     * resolve to distinct cameras is an error, resulting in an exception.
     *
     * The [Camera] returned is determined by the given camera selector, plus other internal
     * requirements, possibly from use case configurations. The camera returned from bindToLifecycle
     * may differ from the camera determined solely by a camera selector. If the camera selector
     * can't resolve a valid camera under the requirements, an IllegalArgumentException will be
     * thrown.
     *
     * Only [UseCase] bound to latest active [Lifecycle] can keep alive. [UseCase] bound to other
     * [Lifecycle] will be stopped.
     *
     * @param lifecycleOwner The lifecycleOwner which controls the lifecycle transitions of the use
     *   cases.
     * @param cameraSelector The camera selector which determines the camera to use for set of use
     *   cases.
     * @param useCases The use cases to bind to a lifecycle.
     * @return The [Camera] instance which is determined by the camera selector and internal
     *   requirements.
     * @throws IllegalStateException If the use case has already been bound to another lifecycle or
     *   method is not called on main thread.
     * @throws IllegalArgumentException If the provided camera selector is unable to resolve a
     *   camera to be used for the given use cases.
     * @throws UnsupportedOperationException If the camera is configured in concurrent mode.
     */
    @MainThread
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        vararg useCases: UseCase?,
    ): Camera {
        return lifecycleCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases)
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
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        useCaseGroup: UseCaseGroup,
    ): Camera {
        return lifecycleCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
    }

    /**
     * Binds a [SessionConfig] to a [LifecycleOwner].
     *
     * A [SessionConfig] encapsulates the configuration required for a camera session. This
     * includes:
     * - A collection of [UseCase] instances defining the desired camera functionality.
     * - Session parameters to be applied to the camera.
     * - Common properties such as the field-of-view defined by [androidx.camera.core.ViewPort].
     * - [androidx.camera.core.CameraEffect]s to be applied for image processing.
     *
     * The state of the lifecycle will determine when the cameras are open, started, stopped and
     * closed. When started, the use cases contained in the given [SessionConfig] receive camera
     * data and the parameters of [SessionConfig] are used for configuring the camera including
     * common field of view, effects and the session parameters.
     *
     * Binding to a lifecycleOwner in state currently in [Lifecycle.State.STARTED] or greater will
     * also initialize and start data capture. If the camera was already running this may cause a
     * new initialization to occur temporarily stopping data from the camera before restarting it.
     *
     * Updates the [SessionConfig] for a given [LifecycleOwner] by invoking [bindToLifecycle] again
     * with the new [SessionConfig]. There is no need to call [unbind] or [unbindAll]; the previous
     * [SessionConfig] and its associated [UseCase]s will be implicitly unbound. This behavior also
     * applies when rebinding to the same [LifecycleOwner] with a different [CameraSelector], such
     * as when switching the camera's lens facing.
     *
     * **Important Restrictions:**
     * - You cannot bind a [SessionConfig] to a [LifecycleOwner] that already has individual
     *   [UseCase]s or a [UseCaseGroup] bound to it.
     * - A [SessionConfig] bound to a [LifecycleOwner] cannot contain [UseCase]s that are already
     *   bound to a different [LifecycleOwner].
     *
     * Violating these restrictions will result in an [IllegalStateException].
     *
     * The [Camera] returned is determined by the given camera selector, plus other internal
     * requirements, possibly from use case configurations. The camera returned from bindToLifecycle
     * may differ from the camera determined solely by a camera selector. If the camera selector
     * can't resolve a valid camera under the requirements, an IllegalArgumentException will be
     * thrown.
     *
     * The following code example shows various aspects of binding a session config.
     *
     * @sample androidx.camera.lifecycle.samples.bindSessionConfigToLifecycle
     *
     * The following code snippet demonstrates binding a session config with feature groups.
     *
     * @sample androidx.camera.lifecycle.samples.bindSessionConfigWithFeatureGroupsToLifecycle
     * @throws UnsupportedOperationException If the camera is configured in concurrent mode. For
     *   example, if a list of [SingleCameraConfig]s was bound to the lifecycle already.
     * @throws IllegalStateException if either of the following conditions is met:
     * - A [UseCase] or [SessionConfig] is already bound to the same [LifecycleOwner].
     * - A [UseCase] contained within the [SessionConfig] is already bound to a different
     *   [LifecycleOwner].
     */
    @ExperimentalSessionConfig
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        sessionConfig: SessionConfig,
    ): Camera {
        return lifecycleCameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            sessionConfig,
        )
    }

    /**
     * Binds list of [SingleCameraConfig]s to [LifecycleOwner].
     *
     * The concurrent camera is only supporting two cameras currently. If the input list of
     * [SingleCameraConfig]s have less or more than two [SingleCameraConfig]s,
     * [IllegalArgumentException] will be thrown. If cameras are already used by other [UseCase]s,
     * [UnsupportedOperationException] will be thrown.
     *
     * A logical camera is a grouping of two or more of those physical cameras. See
     * [Multi-camera API](https://developer.android.com/media/camera/camera2/multi-camera)
     *
     * If we want to open concurrent logical cameras, which are one front camera and one back
     * camera, the device needs to support [PackageManager.FEATURE_CAMERA_CONCURRENT]. To set up
     * concurrent logical camera, call [availableConcurrentCameraInfos] to get the list of available
     * combinations of concurrent cameras. Each sub-list contains the [CameraInfo]s for a
     * combination of cameras that can be operated concurrently. Each logical camera can have its
     * own [UseCase]s and [LifecycleOwner]. See
     * [CameraX lifecycles]({@docRoot}training/camerax/architecture#lifecycles)
     *
     * If the concurrent logical cameras are binding the same preview and video capture use cases,
     * the concurrent cameras video recording will be supported. The concurrent camera preview
     * stream will be shared with video capture and record the concurrent cameras streams as a
     * composited stream. The [CompositionSettings] can be used to configure the position of each
     * camera stream and different layouts can be built. See [CompositionSettings] for more details.
     *
     * If we want to open concurrent physical cameras, which are two front cameras or two back
     * cameras, the device needs to support physical cameras and the capability could be checked via
     * [CameraInfo.isLogicalMultiCameraSupported]. Each physical cameras can have its own [UseCase]s
     * but needs to have the same [LifecycleOwner], otherwise [IllegalArgumentException] will be
     * thrown.
     *
     * If we want to open one physical camera, for example ultra wide, we just need to set physical
     * camera id in [CameraSelector] and bind to lifecycle. All CameraX features will work normally
     * when only a single physical camera is used.
     *
     * If we want to open multiple physical cameras, we need to have multiple [CameraSelector]s,
     * each in one [SingleCameraConfig] and set physical camera id, then bind to lifecycle with the
     * [SingleCameraConfig]s. Internally each physical camera id will be set on [UseCase], for
     * example, [Preview] and call
     * [android.hardware.camera2.params.OutputConfiguration.setPhysicalCameraId].
     *
     * Currently only two physical cameras for the same logical camera id are allowed and the device
     * needs to support physical cameras by checking [CameraInfo.isLogicalMultiCameraSupported]. In
     * addition, there is no guarantee or API to query whether the device supports multiple physical
     * camera opening or not. Internally the library checks
     * [android.hardware.camera2.CameraDevice.isSessionConfigurationSupported], if the device does
     * not support the multiple physical camera configuration, [IllegalArgumentException] will be
     * thrown.
     *
     * @param singleCameraConfigs Input list of [SingleCameraConfig]s.
     * @return Output [ConcurrentCamera] instance.
     * @throws IllegalArgumentException If less or more than two camera configs are provided.
     * @throws UnsupportedOperationException If device is not supporting concurrent camera or
     *   cameras are already used by other [UseCase]s.
     * @see ConcurrentCamera
     * @see availableConcurrentCameraInfos
     * @see CameraInfo.isLogicalMultiCameraSupported
     * @see CameraInfo.getPhysicalCameraInfos
     */
    @MainThread
    public fun bindToLifecycle(singleCameraConfigs: List<SingleCameraConfig?>): ConcurrentCamera {
        return lifecycleCameraProvider.bindToLifecycle(singleCameraConfigs)
    }

    override val availableCameraInfos: List<CameraInfo>
        get() = lifecycleCameraProvider.availableCameraInfos

    override val availableConcurrentCameraInfos: List<List<CameraInfo>>
        get() = lifecycleCameraProvider.availableConcurrentCameraInfos

    override val isConcurrentCameraModeOn: Boolean
        @MainThread get() = lifecycleCameraProvider.isConcurrentCameraModeOn

    override val configImplType: Int
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = lifecycleCameraProvider.configImplType

    @Throws(CameraInfoUnavailableException::class)
    override fun hasCamera(cameraSelector: CameraSelector): Boolean {
        return lifecycleCameraProvider.hasCamera(cameraSelector)
    }

    override fun getCameraInfo(cameraSelector: CameraSelector): CameraInfo {
        return lifecycleCameraProvider.getCameraInfo(cameraSelector)
    }

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
        return lifecycleCameraProvider.shutdownAsync()
    }

    private fun initAsync(context: Context): ListenableFuture<Void> {
        return lifecycleCameraProvider.initAsync(context, null)
    }

    private fun configure(cameraXConfig: CameraXConfig) {
        return lifecycleCameraProvider.configure(cameraXConfig)
    }

    public companion object {
        private val sAppInstance = ProcessCameraProvider(LifecycleCameraProviderImpl())

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
                sAppInstance.initAsync(context),
                { sAppInstance },
                CameraXExecutors.directExecutor(),
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
         * @param cameraXConfig The configuration options for the singleton process camera provider
         *   instance.
         * @throws IllegalStateException If the camera provider has already been configured by a
         *   previous call to `configureInstance()` or [getInstance].
         */
        @JvmStatic
        @ExperimentalCameraProviderConfiguration
        public fun configureInstance(cameraXConfig: CameraXConfig): Unit =
            trace("CX:configureInstance") { sAppInstance.configure(cameraXConfig) }

        @JvmStatic
        @VisibleForTesting
        @ExperimentalCameraProviderConfiguration
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun clearConfiguration(timeout: Duration = 10.seconds) {
            sAppInstance.shutdownAsync().get(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        }

        /**
         * Shuts down the ProcessCameraProvider asynchronously.
         *
         * This will release all resources held by the provider. The returned [ListenableFuture]
         * will complete once the shutdown is complete.
         *
         * @return A [ListenableFuture] which will complete when the provider has been shut down.
         */
        @JvmStatic
        @VisibleForTesting
        @ExperimentalCameraProviderConfiguration
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun shutdown(): ListenableFuture<Void> {
            return sAppInstance.shutdownAsync()
        }
    }
}
