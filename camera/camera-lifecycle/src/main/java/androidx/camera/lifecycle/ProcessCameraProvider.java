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

package androidx.camera.lifecycle;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.InitializationException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Preconditions;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * A singleton which can be used to bind the lifecycle of cameras to any {@link LifecycleOwner}
 * within an application's process.
 *
 * <p>Only a single process camera provider can exist within a process, and it can be retrieved
 * with {@link #getInstance(Context)}.
 *
 * <p>Heavyweight resources, such as open and running camera devices, will be scoped to the
 * lifecycle provided to {@link #bindToLifecycle(LifecycleOwner, CameraSelector, UseCase...)}.
 * Other lightweight resources, such as static camera characteristics, may be retrieved and
 * cached upon first retrieval of this provider with {@link #getInstance(Context)}, and will
 * persist for the lifetime of the process.
 *
 * <p>This is the standard provider for applications to use.
 */
public final class ProcessCameraProvider implements LifecycleCameraProvider {

    private static final ProcessCameraProvider sAppInstance = new ProcessCameraProvider();


    /**
     * Retrieves the {@link ProcessCameraProvider} associated with the current process.
     *
     * <p>The instance returned here can be used to bind use cases to any
     * {@link LifecycleOwner} with
     * {@link #bindToLifecycle(LifecycleOwner, CameraSelector, UseCase...)}.
     * <p>The instance's configuration may be customized by subclassing the application's
     * {@link Application} class and implementing {@link CameraXConfig.Provider}.  For example, the
     * following will initialize this process camera provider with a
     * {@linkplain androidx.camera.camera2.Camera2Config Camera2 implementation} from
     * {@link androidx.camera.camera2}, and with a custom executor.
     * <p/>
     * <pre>
     * public class MyApplication extends Application implements CameraXConfig.Provider {
     *     {@literal @}Override
     *     public CameraXConfig getCameraXConfig() {
     *         return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
     *                    .setCameraExecutor(myExecutor)
     *                    .build();
     *     }
     *
     *     . . .
     * }
     * </pre>
     * If no {@link CameraXConfig.Provider} is implemented, a default configuration will be used.
     *
     * @return A future which will contain the {@link ProcessCameraProvider}. Cancellation of
     * this future is a no-op. This future may fail with an {@link InitializationException} and
     * associated cause that can be retrieved by {@link Throwable#getCause()). The cause will be
     * a {@link CameraUnavailableException} if it fails to access any camera during initialization.
     * @throws IllegalStateException if CameraX fails to initialize via a default provider or a
     *                               CameraXConfig.Provider.
     */
    @NonNull
    public static ListenableFuture<ProcessCameraProvider> getInstance(
            @NonNull Context context) {
        Preconditions.checkNotNull(context);
        return Futures.transform(CameraX.getOrCreateInstance(context), cameraX -> sAppInstance,
                CameraXExecutors.directExecutor());
    }

    /**
     * Initializes the {@link ProcessCameraProvider} with the given context and config.
     *
     * <p>The context enables CameraX to obtain access to necessary services, including the camera
     * service. For example, the context can be provided by the {@link Application}.
     *
     * <p>This method, along with {@link #shutdown()} allows the process
     * camera provider to be used in test suites which may need to initialize CameraX in
     * different ways in between tests.
     *
     * <p>Once this method is called, the instance can be retrieved with
     * {@link #getInstance(Context)} without the need for implementing
     * {@link CameraXConfig.Provider}
     * in the test suite's {@link Application}.
     *
     * @param context       for retrieving access to the camera service.
     * @param cameraXConfig configuration options for the singleton process camera provider
     *                      instance.
     * @hide
     */
    @RestrictTo(Scope.TESTS)
    public static void initializeInstance(@NonNull Context context,
            @NonNull CameraXConfig cameraXConfig) {
        ListenableFuture<Void> future = CameraX.initialize(context, cameraXConfig);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                // Throw the unexpected error.
                throw new RuntimeException(t);
            }
        }, CameraXExecutors.directExecutor());
    }

    /**
     * Allows shutting down this {@link ProcessCameraProvider} instance so a new instance can be
     * retrieved by {@link #getInstance(Context)}.
     *
     * <p>Once shutdown, a new instance can be retrieved with
     * {@link ProcessCameraProvider#getInstance(Context)}.
     *
     * <p>This method, along with {@link #initializeInstance(Context, CameraXConfig)} allows the
     * process
     * camera provider to be used in test suites which may need to initialize CameraX in
     * different ways in between tests.
     *
     * @return A {@link ListenableFuture} representing the shutdown status. Cancellation of this
     * future is a no-op.
     * @hide
     */
    @RestrictTo(Scope.TESTS)
    @NonNull
    public ListenableFuture<Void> shutdown() {
        return CameraX.shutdown();
    }

    /**
     * Binds the collection of {@link UseCase} to a {@link LifecycleOwner}.
     *
     * <p>The state of the lifecycle will determine when the cameras are open, started, stopped
     * and closed.  When started, the use cases receive camera data.
     *
     * <p>Binding to a lifecycleOwner in state currently in {@link Lifecycle.State#STARTED} or
     * greater will also initialize and start data capture. If the camera was already running
     * this may cause a new initialization to occur temporarily stopping data from the camera
     * before restarting it.
     *
     * <p>Multiple use cases can be bound via adding them all to a single bindToLifecycle call, or
     * by using multiple bindToLifecycle calls.  Using a single call that includes all the use
     * cases helps to set up a camera session correctly for all uses cases, such as by allowing
     * determination of resolutions depending on all the use cases bound being bound.
     * If the use cases are bound separately, it will find the supported resolution with the
     * priority depending on the binding sequence. If the use cases are bound with a single call,
     * it will find the supported resolution with the priority in sequence of {@link ImageCapture},
     * {@link Preview} and then {@link ImageAnalysis}. The resolutions that can be supported depends
     * on the camera device hardware level that there are some default guaranteed resolutions
     * listed in
     * {@link android.hardware.camera2.CameraDevice#createCaptureSession(List,
     * android.hardware.camera2.CameraCaptureSession.StateCallback, Handler)}.
     *
     * <p>Currently up to 3 use cases may be bound to a {@link Lifecycle} at any time. Exceeding
     * capability of target camera device will throw an IllegalArgumentException.
     *
     * <p>A UseCase should only be bound to a single lifecycle and camera selector a time.
     * Attempting to bind a use case to a lifecycle when it is already bound to another lifecycle
     * is an error, and the use case binding will not change. Attempting to bind the same use case
     * to multiple camera selectors is also an error and will not change the binding.
     *
     * <p>If different use cases are bound to different camera selectors that resolve to distinct
     * cameras, but the same lifecycle, only one of the cameras will operate at a time. The
     * non-operating camera will not become active until it is the only camera with use cases bound.
     *
     * <p>The {@link Camera} returned is determined by the given camera selector, plus other
     * internal requirements, possibly from use case configurations. The camera returned from
     * bindToLifecycle may differ from the camera determined solely by a camera selector. If the
     * camera selector can't resolve a camera under the requirements, an IllegalArgumentException
     * will be thrown.
     *
     * <p>Only {@link UseCase} bound to latest active {@link Lifecycle} can keep alive.
     * {@link UseCase} bound to other {@link Lifecycle} will be stopped.
     *
     * @param lifecycleOwner The lifecycleOwner which controls the lifecycle transitions of the use
     *                       cases.
     * @param cameraSelector The camera selector which determines the camera to use for set of
     *                       use cases.
     * @param useCases       The use cases to bind to a lifecycle.
     * @return The {@link Camera} instance which is determined by the camera selector and
     * internal requirements.
     * @throws IllegalStateException    If the use case has already been bound to another lifecycle
     *                                  or method is not called on main thread.
     * @throws IllegalArgumentException If the provided camera selector is unable to resolve a
     *                                  camera to be used for the given use cases.
     */
    @SuppressWarnings("lambdaLast")
    @MainThread
    @NonNull
    public Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector,
            @NonNull UseCase... useCases) {
        return CameraX.bindToLifecycle(lifecycleOwner, cameraSelector, null, useCases);
    }

    /**
     * Binds a {@link UseCaseGroup} to a {@link LifecycleOwner}.
     *
     * <p> Similar to {@link #bindToLifecycle(LifecycleOwner, CameraSelector, UseCase[])},
     * with the addition that the bound collection of {@link UseCase} share parameters
     * defined by {@link UseCaseGroup} such as consistent camera sensor rect across all
     * {@link UseCase}s.
     *
     * <p> If one {@link UseCase} is in multiple {@link UseCaseGroup}s, it will be linked to
     * the {@link UseCaseGroup} in the latest
     * {@link #bindToLifecycle(LifecycleOwner, CameraSelector, UseCaseGroup)} call.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("lambdaLast")
    @MainThread
    @NonNull
    public Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector,
            @NonNull UseCaseGroup useCaseGroup) {
        return CameraX.bindToLifecycle(lifecycleOwner, cameraSelector,
                useCaseGroup.getViewPort(), useCaseGroup.getUseCases());
    }

    @Override
    public boolean isBound(@NonNull UseCase useCase) {
        return CameraX.isBound(useCase);
    }

    @MainThread
    @Override
    public void unbind(@NonNull UseCase... useCases) {
        CameraX.unbind(useCases);
    }

    @MainThread
    @Override
    public void unbindAll() {
        CameraX.unbindAll();
    }

    @Override
    public boolean hasCamera(@NonNull CameraSelector cameraSelector)
            throws CameraInfoUnavailableException {
        return CameraX.hasCamera(cameraSelector);
    }

    private ProcessCameraProvider() {
    }
}
