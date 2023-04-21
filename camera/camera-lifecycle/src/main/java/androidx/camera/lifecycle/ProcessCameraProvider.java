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

import static android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT;

import static androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT;
import static androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_SINGLE;
import static androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED;
import static androidx.camera.core.impl.utils.Threads.runOnMainSync;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ConcurrentCamera;
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.InitializationException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.concurrent.CameraCoordinator.CameraOperatingMode;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore;
import androidx.camera.core.impl.utils.ContextUtil;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ProcessCameraProvider implements LifecycleCameraProvider {

    private static final ProcessCameraProvider sAppInstance = new ProcessCameraProvider();

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private CameraXConfig.Provider mCameraXConfigProvider = null;
    @GuardedBy("mLock")
    private ListenableFuture<CameraX> mCameraXInitializeFuture;
    @GuardedBy("mLock")
    private ListenableFuture<Void> mCameraXShutdownFuture = Futures.immediateFuture(null);

    private final LifecycleCameraRepository mLifecycleCameraRepository =
            new LifecycleCameraRepository();
    private CameraX mCameraX;
    private Context mContext;

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
     *                    .setSchedulerHandler(mySchedulerHandler)
     *                    .build();
     *     }
     *
     *     . . .
     * }
     * </pre>
     * <p>If it isn't possible to subclass the {@link Application} class, such as in library
     * code, then the singleton can be configured via {@link #configureInstance(CameraXConfig)}
     * before the first invocation of {@code getInstance(context)}, as in the following example.
     * <p/>
     * <pre>{@code
     * class MyCustomizedCameraProvider {
     *
     *     private static boolean configured = false;
     *
     *     static ListenableFuture<ProcessCameraProvider> getInstance(Context context) {
     *         synchronized(MyCustomizedCameraProvider.class) {
     *             if (!configured) {
     *                 configured = true;
     *                 ProcessCameraProvider.configureInstance(
     *                     CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
     *                           .setCameraExecutor(myExecutor)
     *                           .setSchedulerHandler(mySchedulerHandler)
     *                           .build());
     *             }
     *         }
     *         return ProcessCameraProvider.getInstance(context);
     *     }
     * }
     * }</pre>
     * <p>If no {@link CameraXConfig.Provider} is implemented by {@link Application}, or if the
     * singleton has not been configured via {@link #configureInstance(CameraXConfig)} a default
     * configuration will be used.
     *
     * @return A future which will contain the {@link ProcessCameraProvider}. Cancellation of
     * this future is a no-op. This future may fail with an {@link InitializationException} and
     * associated cause that can be retrieved by {@link Throwable#getCause()}. The cause will be
     * a {@link androidx.camera.core.CameraUnavailableException} if it fails to access any camera
     * during initialization.
     * @throws IllegalStateException if CameraX fails to initialize via a default provider or a
     *                               CameraXConfig.Provider.
     * @see #configureInstance(CameraXConfig)
     */
    @NonNull
    public static ListenableFuture<ProcessCameraProvider> getInstance(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        return Futures.transform(sAppInstance.getOrCreateCameraXInstance(context),
                cameraX -> {
                    sAppInstance.setCameraX(cameraX);
                    sAppInstance.setContext(ContextUtil.getApplicationContext(context));
                    return sAppInstance;
                }, CameraXExecutors.directExecutor());
    }

    private ListenableFuture<CameraX> getOrCreateCameraXInstance(@NonNull Context context) {
        synchronized (mLock) {
            if (mCameraXInitializeFuture != null) {
                return mCameraXInitializeFuture;
            }

            CameraX cameraX = new CameraX(context, mCameraXConfigProvider);

            mCameraXInitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
                synchronized (mLock) {
                    ListenableFuture<Void> future =
                            FutureChain.from(mCameraXShutdownFuture).transformAsync(
                                    input -> cameraX.getInitializeFuture(),
                                    CameraXExecutors.directExecutor());

                    Futures.addCallback(future, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            completer.set(cameraX);
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            completer.setException(t);
                        }
                    }, CameraXExecutors.directExecutor());
                }

                return "ProcessCameraProvider-initializeCameraX";
            });

            return mCameraXInitializeFuture;
        }
    }

    /**
     * Perform one-time configuration of the {@link ProcessCameraProvider} singleton with the
     * given {@link CameraXConfig}.
     *
     * <p>This method allows configuration of the camera provider via {@link CameraXConfig}. All
     * initialization tasks, such as communicating with the camera service, will be executed
     * on the {@link java.util.concurrent.Executor} set by
     * {@link CameraXConfig.Builder#setCameraExecutor(Executor)}, or by an internally defined
     * executor if none is provided.
     *
     * <p>This method is not required for every application. If the method is not called and
     * {@link CameraXConfig.Provider} is not implemented in {@link Application}, default
     * configuration will be used.
     *
     * <p>Once this method is called, the instance configured by the given {@link CameraXConfig} can
     *  be retrieved with {@link #getInstance(Context)}. {@link CameraXConfig.Provider}
     *  implemented in {@link Application} will be ignored.
     *
     * <p>Configuration can only occur once. Once the ProcessCameraProvider has been configured with
     * {@code configureInstance()} or {@link #getInstance(Context)}, this method will throw
     * an {@link IllegalStateException}. Because configuration can only occur once, <b>usage of this
     * method from library code is not recommended</b> as the application owner should ultimately
     * be in control of singleton configuration.
     *
     * @param cameraXConfig configuration options for the singleton process camera provider
     *                      instance.
     * @throws IllegalStateException if the camera provider has already been configured by a
     *                               previous call to {@code configureInstance()} or
     *                               {@link #getInstance(Context)}.
     */
    @ExperimentalCameraProviderConfiguration
    public static void configureInstance(@NonNull CameraXConfig cameraXConfig) {
        sAppInstance.configureInstanceInternal(cameraXConfig);
    }

    private void configureInstanceInternal(@NonNull CameraXConfig cameraXConfig) {
        synchronized (mLock) {
            Preconditions.checkNotNull(cameraXConfig);
            Preconditions.checkState(mCameraXConfigProvider == null, "CameraX has "
                    + "already been configured. To use a different configuration, shutdown() must"
                    + " be called.");

            mCameraXConfigProvider = () -> cameraXConfig;
        }
    }

    /**
     * Allows shutting down this {@link ProcessCameraProvider} instance so a new instance can be
     * retrieved by {@link #getInstance(Context)}.
     *
     * <p>Once shutdown, a new instance can be retrieved with
     * {@link ProcessCameraProvider#getInstance(Context)}.
     *
     * <p>This method, along with {@link #configureInstance(CameraXConfig)} allows the process
     * camera provider to be used in test suites which may need to initialize CameraX in
     * different ways in between tests.
     *
     * @return A {@link ListenableFuture} representing the shutdown status. Cancellation of this
     * future is a no-op.
     * @hide
     */
    @VisibleForTesting
    @NonNull
    public ListenableFuture<Void> shutdown() {
        runOnMainSync(this::unbindAll);
        mLifecycleCameraRepository.clear();

        ListenableFuture<Void> shutdownFuture = mCameraX != null ? mCameraX.shutdown() :
                Futures.immediateFuture(null);

        synchronized (mLock) {
            mCameraXConfigProvider = null;
            mCameraXInitializeFuture = null;
            mCameraXShutdownFuture = shutdownFuture;
        }
        mCameraX = null;
        mContext = null;
        return shutdownFuture;
    }

    private void setCameraX(CameraX cameraX) {
        mCameraX = cameraX;
    }

    private void setContext(Context context) {
        mContext = context;
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
     * camera selector can't resolve a valid camera under the requirements, an
     * IllegalArgumentException will be thrown.
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
    @SuppressWarnings({"lambdaLast"})
    @MainThread
    @NonNull
    public Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector,
            @NonNull UseCase... useCases) {
        if (getCameraOperatingMode() == CAMERA_OPERATING_MODE_CONCURRENT) {
            throw new UnsupportedOperationException("bindToLifecycle for single camera is not "
                    + "supported in concurrent camera mode, call unbindAll() first");
        }
        setCameraOperatingMode(CAMERA_OPERATING_MODE_SINGLE);
        Camera camera = bindToLifecycle(lifecycleOwner, cameraSelector, null, emptyList(),
                useCases);
        return camera;
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
     */
    @SuppressWarnings({"lambdaLast"})
    @MainThread
    @NonNull
    public Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector,
            @NonNull UseCaseGroup useCaseGroup) {
        if (getCameraOperatingMode() == CAMERA_OPERATING_MODE_CONCURRENT) {
            throw new UnsupportedOperationException("bindToLifecycle for single camera is not "
                    + "supported in concurrent camera mode, call unbindAll() first");
        }
        setCameraOperatingMode(CAMERA_OPERATING_MODE_SINGLE);
        Camera camera = bindToLifecycle(lifecycleOwner, cameraSelector,
                useCaseGroup.getViewPort(), useCaseGroup.getEffects(),
                useCaseGroup.getUseCases().toArray(new UseCase[0]));
        return camera;
    }

    /**
     * Binds list of {@link SingleCameraConfig}s to {@link LifecycleOwner}.
     *
     * <p>The concurrent camera is only supporting two cameras currently. If the input
     * list of {@link SingleCameraConfig}s have less or more than two {@link SingleCameraConfig}s,
     * {@link IllegalArgumentException} will be thrown. If the device is not supporting
     * {@link PackageManager#FEATURE_CAMERA_CONCURRENT} or cameras are already used by other
     * {@link UseCase}s, {@link UnsupportedOperationException} will be thrown.
     *
     * <p>To set up concurrent camera, call {@link #getAvailableConcurrentCameraInfos()} to get
     * the list of available combinations of concurrent cameras. Each sub-list contains the
     * {@link CameraInfo}s for a combination of cameras that can be operated concurrently.
     * Each camera can have its own {@link UseCase}s and {@link LifecycleOwner}. See
     * <a href="{@docRoot}training/camerax/architecture#lifecycles">CameraX lifecycles</a>
     *
     * @param singleCameraConfigs input list of {@link SingleCameraConfig}s.
     * @return output {@link ConcurrentCamera} instance.
     *
     * @throws IllegalArgumentException If less or more than two camera configs are provided.
     * @throws UnsupportedOperationException If device is not supporting concurrent camera or
     * cameras are already used by other {@link UseCase}s.
     *
     * @see ConcurrentCamera
     * @see #getAvailableConcurrentCameraInfos()
     */
    @MainThread
    @NonNull
    public ConcurrentCamera bindToLifecycle(@NonNull List<SingleCameraConfig> singleCameraConfigs) {
        if (!mContext.getPackageManager().hasSystemFeature(FEATURE_CAMERA_CONCURRENT)) {
            throw new UnsupportedOperationException("Concurrent camera is not supported on the "
                    + "device");
        }

        if (getCameraOperatingMode() == CAMERA_OPERATING_MODE_SINGLE) {
            throw new UnsupportedOperationException("Camera is already running, call "
                    + "unbindAll() before binding more cameras");
        }

        if (singleCameraConfigs.size() < 2) {
            throw new IllegalArgumentException("Concurrent camera needs two camera configs");
        }

        if (singleCameraConfigs.size() > 2) {
            throw new IllegalArgumentException("Concurrent camera is only supporting two  "
                    + "cameras at maximum.");
        }

        List<CameraInfo> cameraInfosToBind = new ArrayList<>();
        List<CameraInfo> availableCameraInfos = getAvailableCameraInfos();
        CameraInfo firstCameraInfo = getCameraInfoFromCameraSelector(
                singleCameraConfigs.get(0).getCameraSelector(),
                availableCameraInfos);
        CameraInfo secondCameraInfo = getCameraInfoFromCameraSelector(
                singleCameraConfigs.get(1).getCameraSelector(),
                availableCameraInfos);
        if (firstCameraInfo == null || secondCameraInfo == null) {
            throw new IllegalArgumentException("Invalid camera selectors in camera configs");
        }
        cameraInfosToBind.add(firstCameraInfo);
        cameraInfosToBind.add(secondCameraInfo);
        if (!getActiveConcurrentCameraInfos().isEmpty()
                && !cameraInfosToBind.equals(getActiveConcurrentCameraInfos())) {
            throw new UnsupportedOperationException("Cameras are already running, call "
                    + "unbindAll() before binding more cameras");
        }

        setCameraOperatingMode(CAMERA_OPERATING_MODE_CONCURRENT);
        List<Camera> cameras = new ArrayList<>();
        for (SingleCameraConfig config : singleCameraConfigs) {
            Camera camera = bindToLifecycle(
                    config.getLifecycleOwner(),
                    config.getCameraSelector(),
                    config.getUseCaseGroup().getViewPort(),
                    config.getUseCaseGroup().getEffects(),
                    config.getUseCaseGroup().getUseCases().toArray(new UseCase[0]));
            cameras.add(camera);
        }
        setActiveConcurrentCameraInfos(cameraInfosToBind);
        return new ConcurrentCamera(cameras);
    }

    /**
     * Binds {@link ViewPort} and a collection of {@link UseCase} to a {@link LifecycleOwner}.
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
     * listed in {@link android.hardware.camera2.CameraDevice#createCaptureSession(List,
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
     * @param viewPort       The viewPort which represents the visible camera sensor rect.
     * @param effects        The effects applied to the camera outputs.
     * @param useCases       The use cases to bind to a lifecycle.
     * @return The {@link Camera} instance which is determined by the camera selector and
     * internal requirements.
     * @throws IllegalStateException    If the use case has already been bound to another lifecycle
     *                                  or method is not called on main thread.
     * @throws IllegalArgumentException If the provided camera selector is unable to resolve a
     *                                  camera to be used for the given use cases.
     */
    @SuppressWarnings({"lambdaLast", "unused"})
    @NonNull
    Camera bindToLifecycle(
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector,
            @Nullable ViewPort viewPort,
            @NonNull List<CameraEffect> effects,
            @NonNull UseCase... useCases) {
        Threads.checkMainThread();
        // TODO(b/153096869): override UseCase's target rotation.
        // TODO(b/154939118) The filter appending should be removed after extensions are moved to
        //  the CheckedCameraInternal
        CameraSelector.Builder selectorBuilder =
                CameraSelector.Builder.fromSelector(cameraSelector);
        // Append the camera filter required internally if there's any.
        for (UseCase useCase : useCases) {
            CameraSelector selector = useCase.getCurrentConfig().getCameraSelector(null);
            if (selector != null) {
                for (CameraFilter filter : selector.getCameraFilterSet()) {
                    selectorBuilder.addCameraFilter(filter);
                }
            }
        }

        CameraSelector modifiedSelector = selectorBuilder.build();

        LinkedHashSet<CameraInternal> cameraInternals =
                modifiedSelector.filter(mCameraX.getCameraRepository().getCameras());
        if (cameraInternals.isEmpty()) {
            throw new IllegalArgumentException("Provided camera selector unable to resolve a "
                    + "camera for the given use case");
        }
        CameraUseCaseAdapter.CameraId cameraId =
                CameraUseCaseAdapter.generateCameraId(cameraInternals);

        LifecycleCamera lifecycleCameraToBind =
                mLifecycleCameraRepository.getLifecycleCamera(lifecycleOwner, cameraId);

        Collection<LifecycleCamera> lifecycleCameras =
                mLifecycleCameraRepository.getLifecycleCameras();
        for (UseCase useCase : useCases) {
            for (LifecycleCamera lifecycleCamera : lifecycleCameras) {
                if (lifecycleCamera.isBound(useCase)
                        && lifecycleCamera != lifecycleCameraToBind) {
                    throw new IllegalStateException(
                            String.format(
                                    "Use case %s already bound to a different lifecycle.",
                                    useCase));
                }
            }
        }

        // Try to get the camera before binding to the use case, and throw IllegalArgumentException
        // if the camera not found.
        if (lifecycleCameraToBind == null) {
            lifecycleCameraToBind =
                    mLifecycleCameraRepository.createLifecycleCamera(lifecycleOwner,
                            new CameraUseCaseAdapter(cameraInternals,
                                    mCameraX.getCameraFactory().getCameraCoordinator(),
                                    mCameraX.getCameraDeviceSurfaceManager(),
                                    mCameraX.getDefaultConfigFactory()));
        }

        CameraConfig cameraConfig = null;

        // Retrieves extended camera configs from ExtendedCameraConfigProviderStore
        for (CameraFilter cameraFilter : cameraSelector.getCameraFilterSet()) {
            if (cameraFilter.getIdentifier() != CameraFilter.DEFAULT_ID) {
                CameraConfig extendedCameraConfig =
                        ExtendedCameraConfigProviderStore.getConfigProvider(
                                cameraFilter.getIdentifier()).getConfig(
                                lifecycleCameraToBind.getCameraInfo(), mContext);
                if (extendedCameraConfig == null) { // ignore IDs unrelated to camera configs.
                    continue;
                }

                // Only allows one camera config now.
                if (cameraConfig != null) {
                    throw new IllegalArgumentException(
                            "Cannot apply multiple extended camera configs at the same time.");
                }
                cameraConfig = extendedCameraConfig;
            }
        }

        // Applies extended camera configs to the camera
        lifecycleCameraToBind.setExtendedConfig(cameraConfig);

        if (useCases.length == 0) {
            return lifecycleCameraToBind;
        }

        mLifecycleCameraRepository.bindToLifecycleCamera(
                lifecycleCameraToBind,
                viewPort,
                effects,
                Arrays.asList(useCases),
                mCameraX.getCameraFactory().getCameraCoordinator());

        return lifecycleCameraToBind;
    }

    /**
     * Returns true if the {@link UseCase} is bound to a lifecycle. Otherwise returns false.
     *
     * <p>After binding a use case with {@link #bindToLifecycle}, use cases remain bound until the
     * lifecycle reaches a {@link Lifecycle.State#DESTROYED} state or if is unbound by calls to
     * {@link #unbind(UseCase...)} or {@link #unbindAll()}.
     */
    @Override
    public boolean isBound(@NonNull UseCase useCase) {
        for (LifecycleCamera lifecycleCamera :
                mLifecycleCameraRepository.getLifecycleCameras()) {
            if (lifecycleCamera.isBound(useCase)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Unbinds all specified use cases from the lifecycle.
     *
     * <p>This will initiate a close of every open camera which has zero {@link UseCase}
     * associated with it at the end of this call.
     *
     * <p>If a use case in the argument list is not bound, then it is simply ignored.
     *
     * <p>After unbinding a UseCase, the UseCase can be and bound to another {@link Lifecycle}
     * however listeners and settings should be reset by the application.
     *
     * @param useCases The collection of use cases to remove.
     * @throws IllegalStateException If not called on main thread.
     * @throws UnsupportedOperationException If called in concurrent mode.
     */
    @MainThread
    @Override
    public void unbind(@NonNull UseCase... useCases) {
        Threads.checkMainThread();

        if (getCameraOperatingMode() == CAMERA_OPERATING_MODE_CONCURRENT) {
            throw new UnsupportedOperationException("unbind usecase is not "
                    + "supported in concurrent camera mode, call unbindAll() first");
        }

        mLifecycleCameraRepository.unbind(Arrays.asList(useCases));
    }

    /**
     * Unbinds all use cases from the lifecycle and removes them from CameraX.
     *
     * <p>This will initiate a close of every currently open camera.
     *
     * @throws IllegalStateException If not called on main thread.
     */
    @MainThread
    @Override
    public void unbindAll() {
        Threads.checkMainThread();
        setCameraOperatingMode(CAMERA_OPERATING_MODE_UNSPECIFIED);
        mLifecycleCameraRepository.unbindAll();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasCamera(@NonNull CameraSelector cameraSelector)
            throws CameraInfoUnavailableException {
        try {
            cameraSelector.select(mCameraX.getCameraRepository().getCameras());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * Returns {@link CameraInfo} instances of the available cameras.
     *
     * <p>The available cameras include all the available cameras on the device, or only those
     * selected through
     * {@link androidx.camera.core.CameraXConfig.Builder#setAvailableCamerasLimiter(CameraSelector)}
     *
     * <p>While iterating through all the available {@link CameraInfo}, if one of them meets some
     * predefined requirements, a {@link CameraSelector} that uniquely identifies its camera
     * can be retrieved using {@link CameraInfo#getCameraSelector()}, which can then be used to bind
     * {@linkplain UseCase use cases} to that camera.
     *
     * @return A list of {@link CameraInfo} instances for the available cameras.
     */
    @NonNull
    @Override
    public List<CameraInfo> getAvailableCameraInfos() {
        final List<CameraInfo> availableCameraInfos = new ArrayList<>();
        final Set<CameraInternal> cameras = mCameraX.getCameraRepository().getCameras();
        for (final CameraInternal camera : cameras) {
            availableCameraInfos.add(camera.getCameraInfo());
        }
        return availableCameraInfos;
    }

    /**
     * Returns list of {@link CameraInfo} instances of the available concurrent cameras.
     *
     * <p>The available concurrent cameras include all combinations of cameras which could
     * operate concurrently on the device. Each list maps to one combination of these camera's
     * {@link CameraInfo}.
     *
     * For example, to select a front camera and a back camera and bind to {@link LifecycleOwner}
     * with preview {@link UseCase}, this function could be used with
     * {@link #bindToLifecycle(List)}.
     * <pre><code>
     * Preview previewFront = new Preview.Builder()
     *                 .build();
     * CameraSelector cameraSelectorPrimary = null;
     * CameraSelector cameraSelectorSecondary = null;
     * for (List<CameraInfo> cameraInfoList : cameraProvider.getAvailableConcurrentCameraInfos()) {
     *     for (CameraInfo cameraInfo : cameraInfoList) {
     *         if (cameraInfo.getLensFacing() == CameraSelector.LENS_FACING_FRONT) {
     *             cameraSelectorPrimary = cameraInfo.getCameraSelector();
     *         } else if (cameraInfo.getLensFacing() == CameraSelector.LENS_FACING_BACK) {
     *             cameraSelectorSecondary = cameraInfo.getCameraSelector();
     *         }
     *     }
     * }
     * if (cameraSelectorPrimary == null || cameraSelectorSecondary == null) {
     *     return;
     * }
     * previewFront.setSurfaceProvider(frontPreviewView.getSurfaceProvider());
     * SingleCameraConfig primary = new SingleCameraConfig(
     *         cameraSelectorPrimary,
     *         new UseCaseGroup.Builder()
     *                 .addUseCase(previewFront)
     *                 .build(),
     *         lifecycleOwner);
     * Preview previewBack = new Preview.Builder()
     *         .build();
     * previewBack.setSurfaceProvider(backPreviewView.getSurfaceProvider());
     * SingleCameraConfig secondary = new SingleCameraConfig(
     *         cameraSelectorSecondary,
     *         new UseCaseGroup.Builder()
     *                 .addUseCase(previewBack)
     *                 .build(),
     *         lifecycleOwner);
     * cameraProvider.bindToLifecycle(ImmutableList.of(primary, secondary));
     * </code></pre>
     *
     * @return list of combinations of {@link CameraInfo}.
     *
     */
    @NonNull
    public List<List<CameraInfo>> getAvailableConcurrentCameraInfos() {
        requireNonNull(mCameraX);
        requireNonNull(mCameraX.getCameraFactory().getCameraCoordinator());
        List<List<CameraSelector>> concurrentCameraSelectorLists =
                mCameraX.getCameraFactory().getCameraCoordinator().getConcurrentCameraSelectors();
        List<CameraInfo> availableCameraInfos = getAvailableCameraInfos();

        List<List<CameraInfo>> availableConcurrentCameraInfos = new ArrayList<>();
        for (final List<CameraSelector> cameraSelectors : concurrentCameraSelectorLists) {
            List<CameraInfo> cameraInfos = new ArrayList<>();
            for (CameraSelector cameraSelector : cameraSelectors) {
                CameraInfo cameraInfo = getCameraInfoFromCameraSelector(cameraSelector,
                        availableCameraInfos);
                if (cameraInfo != null) {
                    cameraInfos.add(cameraInfo);
                }
            }
            availableConcurrentCameraInfos.add(cameraInfos);
        }
        return availableConcurrentCameraInfos;
    }

    /**
     * Returns whether there is a {@link ConcurrentCamera} bound.
     *
     * @return true if there is a {@link ConcurrentCamera} bound, otherwise false.
     *
     */
    @MainThread
    public boolean isConcurrentCameraModeOn() {
        return getCameraOperatingMode() == CAMERA_OPERATING_MODE_CONCURRENT;
    }

    @CameraOperatingMode
    private int getCameraOperatingMode() {
        if (mCameraX == null) {
            return CAMERA_OPERATING_MODE_UNSPECIFIED;
        }
        return mCameraX.getCameraFactory().getCameraCoordinator().getCameraOperatingMode();
    }

    private void setCameraOperatingMode(@CameraOperatingMode int cameraOperatingMode) {
        if (mCameraX == null) {
            return;
        }
        mCameraX.getCameraFactory().getCameraCoordinator()
                .setCameraOperatingMode(cameraOperatingMode);
    }

    @NonNull
    private List<CameraInfo> getActiveConcurrentCameraInfos() {
        if (mCameraX == null) {
            return new ArrayList<>();
        }
        return mCameraX.getCameraFactory().getCameraCoordinator()
                .getActiveConcurrentCameraInfos();
    }

    private void setActiveConcurrentCameraInfos(@NonNull List<CameraInfo> cameraInfos) {
        if (mCameraX == null) {
            return;
        }
        mCameraX.getCameraFactory().getCameraCoordinator()
                .setActiveConcurrentCameraInfos(cameraInfos);
    }

    @Nullable
    private CameraInfo getCameraInfoFromCameraSelector(
            @NonNull CameraSelector cameraSelector,
            @NonNull List<CameraInfo> availableCameraInfos) {
        List<CameraInfo> cameraInfos = cameraSelector.filter(availableCameraInfos);
        return cameraInfos.isEmpty() ? null : cameraInfos.get(0);
    }

    private ProcessCameraProvider() {
    }
}
