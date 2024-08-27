/*
 * Copyright 2019 The Android Open Source Project
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
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.concurrent.futures.await
import androidx.core.util.Preconditions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture

/**
 * Provides access to a camera which has its opening and closing controlled by a [LifecycleOwner].
 */
// TODO: Remove the annotation when LifecycleCameraProvider is ready to be public.
@RestrictTo(Scope.LIBRARY_GROUP)
public interface LifecycleCameraProvider : CameraProvider {
    /**
     * Returns `true` if the [UseCase] is bound to a lifecycle. Otherwise returns `false`.
     *
     * After binding a use case, use cases remain bound until the lifecycle reaches a
     * [Lifecycle.State.DESTROYED] state or if is unbound by calls to [unbind] or [unbindAll].
     */
    public fun isBound(useCase: UseCase): Boolean

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
    public fun unbind(vararg useCases: UseCase?): Unit

    /**
     * Unbinds all use cases from the lifecycle provider and removes them from CameraX.
     *
     * This will initiate a close of every currently open camera.
     *
     * @throws IllegalStateException If not called on main thread.
     */
    public fun unbindAll(): Unit

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
     * If different use cases are bound to different camera selectors that resolve to distinct
     * cameras, but the same lifecycle, only one of the cameras will operate at a time. The
     * non-operating camera will not become active until it is the only camera with use cases bound.
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
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        vararg useCases: UseCase?
    ): Camera

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
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        useCaseGroup: UseCaseGroup
    ): Camera

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
    public fun bindToLifecycle(singleCameraConfigs: List<SingleCameraConfig?>): ConcurrentCamera

    public companion object {
        /**
         * Creates a lifecycle camera provider instance.
         *
         * @param context The Application context.
         * @param cameraXConfig The configuration options to configure the lifecycle camera
         *   provider. If not set, the default configuration will be used.
         * @return The lifecycle camera provider instance.
         */
        @JvmOverloads
        @JvmStatic
        public suspend fun createInstance(
            context: Context,
            cameraXConfig: CameraXConfig? = null,
        ): LifecycleCameraProvider {
            return createInstanceAsync(context, cameraXConfig).await()
        }

        /**
         * Creates a lifecycle camera provider instance asynchronously.
         *
         * @param context The Application context.
         * @param cameraXConfig The configuration options to configure the lifecycle camera
         *   provider. If not set, the default configuration will be used.
         * @return A [ListenableFuture] that will be completed when the lifecycle camera provider
         *   instance is initialized.
         */
        @JvmOverloads
        @JvmStatic
        public fun createInstanceAsync(
            context: Context,
            cameraXConfig: CameraXConfig? = null,
        ): ListenableFuture<LifecycleCameraProvider> {
            Preconditions.checkNotNull(context)
            val lifecycleCameraProvider = LifecycleCameraProviderImpl()
            return Futures.transform(
                lifecycleCameraProvider.initAsync(context, cameraXConfig),
                { lifecycleCameraProvider },
                CameraXExecutors.directExecutor()
            )
        }
    }
}
