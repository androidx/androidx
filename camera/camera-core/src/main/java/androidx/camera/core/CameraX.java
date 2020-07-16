/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraRepository;
import androidx.camera.core.impl.CameraThreadConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.HandlerCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main interface for accessing CameraX library.
 *
 * <p>This is a singleton class responsible for managing the set of camera instances.
 * @hide
 */
@MainThread
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraX {
    private static final String TAG = "CameraX";
    private static final long WAIT_INITIALIZED_TIMEOUT = 3L;

    static final Object INSTANCE_LOCK = new Object();

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("INSTANCE_LOCK")
    static CameraX sInstance = null;

    @GuardedBy("INSTANCE_LOCK")
    private static CameraXConfig.Provider sConfigProvider = null;

    @GuardedBy("INSTANCE_LOCK")
    private static ListenableFuture<Void> sInitializeFuture =
            Futures.immediateFailedFuture(new IllegalStateException("CameraX is not initialized."));

    @GuardedBy("INSTANCE_LOCK")
    private static ListenableFuture<Void> sShutdownFuture = Futures.immediateFuture(null);

    final CameraRepository mCameraRepository = new CameraRepository();
    private final Object mInitializeLock = new Object();

    private final CameraXConfig mCameraXConfig;

    private final Executor mCameraExecutor;
    private final Handler mSchedulerHandler;
    @Nullable
    private final HandlerThread mSchedulerThread;
    private CameraFactory mCameraFactory;
    private CameraDeviceSurfaceManager mSurfaceManager;
    private UseCaseConfigFactory mDefaultConfigFactory;
    // TODO(b/161302102): Remove the stored context. Only make use of the context within the
    //  called method.
    private Context mAppContext;

    @GuardedBy("mInitializeLock")
    private InternalInitState mInitState = InternalInitState.UNINITIALIZED;
    @GuardedBy("mInitializeLock")
    private ListenableFuture<Void> mShutdownInternalFuture = Futures.immediateFuture(null);

    CameraX(@NonNull CameraXConfig cameraXConfig) {
        mCameraXConfig = Preconditions.checkNotNull(cameraXConfig);

        Executor executor = cameraXConfig.getCameraExecutor(null);
        Handler schedulerHandler = cameraXConfig.getSchedulerHandler(null);
        mCameraExecutor = executor == null ? new CameraExecutor() : executor;
        if (schedulerHandler == null) {
            mSchedulerThread = new HandlerThread(CameraXThreads.TAG + "scheduler",
                    Process.THREAD_PRIORITY_BACKGROUND);
            mSchedulerThread.start();
            mSchedulerHandler = HandlerCompat.createAsync(mSchedulerThread.getLooper());
        } else {
            mSchedulerThread = null;
            mSchedulerHandler = schedulerHandler;
        }
    }

    /**
     * Checks if the device supports at least one camera that meets the requirements from a
     * {@link CameraSelector}.
     *
     * @param cameraSelector the {@link CameraSelector} that filters available cameras.
     * @return true if the device has at least one available camera, otherwise false.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static boolean hasCamera(@NonNull CameraSelector cameraSelector) {
        CameraX cameraX = checkInitialized();

        try {
            cameraSelector.select(cameraX.getCameraRepository().getCameras());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * Returns the camera id for a camera defined by the given {@link CameraSelector}.
     *
     * @param cameraSelector the camera selector
     * @return the camera id if camera exists or {@code null} if no camera can be resolved with
     * the camera selector.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static CameraInternal getCameraWithCameraSelector(
            @NonNull CameraSelector cameraSelector) {
        CameraX cameraX = checkInitialized();

        return cameraSelector.select(cameraX.getCameraRepository().getCameras());
    }

    /**
     * Gets the default lens facing, or throws a {@link IllegalStateException} if there is no
     * available camera.
     *
     * @return The default lens facing.
     * @throws IllegalStateException if unable to find a camera with available lens facing.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @CameraSelector.LensFacing
    public static int getDefaultLensFacing() {
        checkInitialized();

        Integer lensFacingCandidate = null;
        List<Integer> lensFacingList = Arrays.asList(CameraSelector.LENS_FACING_BACK,
                CameraSelector.LENS_FACING_FRONT);
        for (Integer lensFacing : lensFacingList) {
            if (hasCamera(new CameraSelector.Builder().requireLensFacing(lensFacing).build())) {
                lensFacingCandidate = lensFacing;
                break;
            }
        }
        if (lensFacingCandidate == null) {
            throw new IllegalStateException("Unable to get default lens facing.");
        }
        return lensFacingCandidate;
    }

    /**
     * Returns the camera info for the camera with the given camera id.
     *
     * @param cameraId the internal id of the camera
     * @return the camera info if it can be retrieved for the given id.
     * @throws IllegalArgumentException if unable to access cameras, perhaps due to
     *                                  insufficient permissions.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static CameraInfoInternal getCameraInfo(@NonNull String cameraId) {
        CameraX cameraX = checkInitialized();

        return cameraX.getCameraRepository().getCamera(cameraId).getCameraInfoInternal();
    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} which can be used to query for valid surface
     * configurations.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static CameraDeviceSurfaceManager getSurfaceManager() {
        CameraX cameraX = checkInitialized();

        return cameraX.getCameraDeviceSurfaceManager();
    }

    /**
     * Returns the default configuration for the given use case configuration type.
     *
     * <p>The options contained in this configuration serve as fallbacks if they are not included in
     * the user-provided configuration used to create a use case.
     *
     * @param configType the configuration type
     * @param cameraInfo The {@link CameraInfo} of the camera that the default configuration
     *                   will target to, null if it doesn't target to any camera.
     * @return the default configuration for the given configuration type
     * @throws IllegalStateException if CameraX has not yet been initialized.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public static <C extends UseCaseConfig<?>> C getDefaultUseCaseConfig(
            @NonNull Class<C> configType,
            @Nullable CameraInfo cameraInfo) {
        CameraX cameraX = checkInitialized();

        return cameraX.getDefaultConfigFactory().getConfig(configType, cameraInfo);
    }

    /**
     * Initializes CameraX with the given context and application configuration.
     *
     * <p>The context enables CameraX to obtain access to necessary services, including the camera
     * service. For example, the context can be provided by the application.
     *
     * @param context       to attach
     * @param cameraXConfig configuration options for this application session.
     * @return A {@link ListenableFuture} representing the initialization task. This future may
     * fail with an {@link InitializationException} and associated cause that can be retrieved by
     * {@link Throwable#getCause()). The cause will be a {@link CameraUnavailableException} if it
     * fails to access any camera during initialization.
     * @hide
     */
    @RestrictTo(Scope.TESTS)
    @NonNull
    public static ListenableFuture<Void> initialize(@NonNull Context context,
            @NonNull CameraXConfig cameraXConfig) {
        synchronized (INSTANCE_LOCK) {
            Preconditions.checkNotNull(context);
            configureInstanceLocked(() -> cameraXConfig);
            initializeInstanceLocked(context);
            return sInitializeFuture;
        }
    }

    /**
     * Configures the CameraX singleton with the given {@link androidx.camera.core.CameraXConfig}.
     *
     * @param cameraXConfig configuration options for the singleton instance.
     */
    public static void configureInstance(@NonNull CameraXConfig cameraXConfig) {
        synchronized (INSTANCE_LOCK) {
            configureInstanceLocked(() -> cameraXConfig);
        }
    }

    @GuardedBy("INSTANCE_LOCK")
    private static void configureInstanceLocked(@NonNull CameraXConfig.Provider configProvider) {
        Preconditions.checkNotNull(configProvider);
        Preconditions.checkState(sConfigProvider == null, "CameraX has already been configured. "
                + "To use a different configuration, shutdown() must be called.");

        sConfigProvider = configProvider;
    }

    @GuardedBy("INSTANCE_LOCK")
    private static void initializeInstanceLocked(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        Preconditions.checkState(sInstance == null, "CameraX already initialized.");
        Preconditions.checkNotNull(sConfigProvider);
        CameraX cameraX = new CameraX(sConfigProvider.getCameraXConfig());
        sInstance = cameraX;
        sInitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
            synchronized (INSTANCE_LOCK) {
                // The sShutdownFuture should always be successful, otherwise it will not
                // propagate to transformAsync() due to the behavior of FutureChain.
                ListenableFuture<Void> future = FutureChain.from(sShutdownFuture)
                        .transformAsync(input -> cameraX.initInternal(context),
                                CameraXExecutors.directExecutor());

                Futures.addCallback(future, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        completer.set(null);
                    }

                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void onFailure(Throwable t) {
                        Log.w(TAG, "CameraX initialize() failed", t);
                        // Call shutdown() automatically, if initialization fails.
                        synchronized (INSTANCE_LOCK) {
                            // Make sure it is the same instance to prevent reinitialization
                            // during initialization.
                            if (sInstance == cameraX) {
                                shutdownLocked();
                            }
                        }
                        completer.setException(t);
                    }
                }, CameraXExecutors.directExecutor());
                return "CameraX-initialize";
            }
        });
    }

    /**
     * Shutdown CameraX so that it can be initialized again.
     *
     * @return A {@link ListenableFuture} representing the shutdown task.
     */
    @NonNull
    public static ListenableFuture<Void> shutdown() {
        synchronized (INSTANCE_LOCK) {
            sConfigProvider = null;
            return shutdownLocked();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("INSTANCE_LOCK")
    @NonNull
    static ListenableFuture<Void> shutdownLocked() {
        if (sInstance == null) {
            // If it is already or will be shutdown, return the future directly.
            return sShutdownFuture;
        }

        CameraX cameraX = sInstance;
        sInstance = null;

        // Do not use FutureChain to chain the initFuture, because FutureChain.transformAsync()
        // will not propagate if the input initFuture is failed. We want to always
        // shutdown the CameraX instance to ensure that resources are freed.
        sShutdownFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    synchronized (INSTANCE_LOCK) {
                        // Wait initialize complete
                        sInitializeFuture.addListener(() -> {
                            // Wait shutdownInternal complete
                            Futures.propagate(cameraX.shutdownInternal(), completer);
                        }, CameraXExecutors.directExecutor());
                        return "CameraX shutdown";
                    }
                });
        return sShutdownFuture;
    }

    /**
     * Returns the context used for CameraX.
     * @deprecated This method will be removed. New code should not rely on it. See b/161302102.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @Deprecated
    public static Context getContext() {
        CameraX cameraX = checkInitialized();
        return cameraX.mAppContext;
    }

    /**
     * Returns true if CameraX is initialized.
     *
     * @hide
     */
    @RestrictTo(Scope.TESTS)
    public static boolean isInitialized() {
        synchronized (INSTANCE_LOCK) {
            return sInstance != null && sInstance.isInitializedInternal();
        }
    }

    /**
     * Returns the {@link CameraFactory} instance.
     *
     * @throws IllegalStateException if the {@link CameraFactory} has not been set, due to being
     *                               uninitialized.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraFactory getCameraFactory() {
        if (mCameraFactory == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mCameraFactory;
    }

    /**
     * Wait for the initialize or shutdown task finished and then check if it is initialized.
     *
     * @return CameraX instance
     * @throws IllegalStateException if it is not initialized
     */
    @NonNull
    private static CameraX checkInitialized() {
        CameraX cameraX = waitInitialized();
        Preconditions.checkState(cameraX.isInitializedInternal(),
                "Must call CameraX.initialize() first");
        return cameraX;
    }

    /**
     * Returns a future which contains a CameraX instance after initialization is complete.
     *
     * @hide
     */
    @SuppressWarnings("FutureReturnValueIgnored") // shutdownLocked() should always succeed.
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static ListenableFuture<CameraX> getOrCreateInstance(@NonNull Context context) {
        Preconditions.checkNotNull(context, "Context must not be null.");
        synchronized (INSTANCE_LOCK) {
            boolean isConfigured = sConfigProvider != null;
            ListenableFuture<CameraX> instanceFuture = getInstanceLocked();
            if (instanceFuture.isDone()) {
                try {
                    instanceFuture.get();
                } catch (InterruptedException e) {
                    // Should not be possible since future is complete.
                    throw new RuntimeException("Unexpected thread interrupt. Should not be "
                            + "possible since future is already complete.", e);
                } catch (ExecutionException e) {
                    // Either initialization failed or initialize() has not been called, ensure we
                    // can try to reinitialize.
                    shutdownLocked();
                    instanceFuture = null;
                }
            }

            if (instanceFuture == null) {
                if (!isConfigured) {
                    // Attempt initialization through Application or Resources
                    CameraXConfig.Provider configProvider = getConfigProvider(context);
                    if (configProvider == null) {
                        throw new IllegalStateException("CameraX is not configured properly. "
                                + "The most likely cause is you did not include a default "
                                + "implementation in your build such as 'camera-camera2'.");
                    }

                    configureInstanceLocked(configProvider);
                }

                initializeInstanceLocked(context);
                instanceFuture = getInstanceLocked();
            }

            return instanceFuture;
        }
    }

    @Nullable
    private static CameraXConfig.Provider getConfigProvider(@NonNull Context context) {
        CameraXConfig.Provider configProvider = null;
        Application application = getApplicationFromContext(context);
        if (application instanceof CameraXConfig.Provider) {
            // Application is a CameraXConfig.Provider, use this directly
            configProvider = (CameraXConfig.Provider) application;
        } else {
            // Try to retrieve the CameraXConfig.Provider through the application's resources
            try {
                Resources resources = context.getApplicationContext().getResources();
                String defaultProviderClassName =
                        resources.getString(
                                R.string.androidx_camera_default_config_provider);
                Class<?> providerClass =
                        Class.forName(defaultProviderClassName);
                configProvider = (CameraXConfig.Provider) providerClass
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Resources.NotFoundException
                    | ClassNotFoundException
                    | InstantiationException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | NullPointerException e) {
                Log.e(TAG, "Failed to retrieve default CameraXConfig.Provider from "
                        + "resources", e);
            }
        }

        return configProvider;
    }

    /**
     * Attempts to retrieve an {@link Application} object from the provided {@link Context}.
     *
     * <p>Because the contract does not specify that {@code Context.getApplicationContext()} must
     * return an {@code Application} object, this method will attempt to retrieve the
     * {@code Application} by unwrapping the context via {@link ContextWrapper#getBaseContext()} if
     * {@code Context.getApplicationContext()}} does not succeed.
     */
    @Nullable
    private static Application getApplicationFromContext(@NonNull Context context) {
        Application application = null;
        Context appContext = context.getApplicationContext();
        while (appContext instanceof ContextWrapper) {
            if (appContext instanceof Application) {
                application = (Application) appContext;
                break;
            } else {
                appContext = ((ContextWrapper) appContext).getBaseContext();
            }
        }

        return application;
    }

    @NonNull
    private static ListenableFuture<CameraX> getInstance() {
        synchronized (INSTANCE_LOCK) {
            return getInstanceLocked();
        }
    }

    @GuardedBy("INSTANCE_LOCK")
    @NonNull
    private static ListenableFuture<CameraX> getInstanceLocked() {
        CameraX cameraX = sInstance;
        if (cameraX == null) {
            return Futures.immediateFailedFuture(new IllegalStateException("Must "
                    + "call CameraX.initialize() first"));
        }

        return Futures.transform(sInitializeFuture, nullVoid -> cameraX,
                CameraXExecutors.directExecutor());
    }

    /**
     * Wait for the initialize or shutdown task finished.
     *
     * @throws IllegalStateException if the initialization is fail or timeout
     */
    @NonNull
    private static CameraX waitInitialized() {
        ListenableFuture<CameraX> future = getInstance();
        try {
            return future.get(WAIT_INITIALIZED_TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IllegalStateException(e);
        }

    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} instance.
     *
     * @throws IllegalStateException if the {@link CameraDeviceSurfaceManager} has not been set, due
     *                               to being uninitialized.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public CameraDeviceSurfaceManager getCameraDeviceSurfaceManager() {
        if (mSurfaceManager == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mSurfaceManager;
    }

    /**
     * Returns the {@link CameraRepository} instance.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public CameraRepository getCameraRepository() {
        return mCameraRepository;
    }

    private UseCaseConfigFactory getDefaultConfigFactory() {
        if (mDefaultConfigFactory == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mDefaultConfigFactory;
    }

    private ListenableFuture<Void> initInternal(@NonNull Context context) {
        synchronized (mInitializeLock) {
            Preconditions.checkState(mInitState == InternalInitState.UNINITIALIZED,
                    "CameraX.initInternal() should only be called once per instance");
            mInitState = InternalInitState.INITIALIZING;

            final Executor cameraExecutor = mCameraExecutor;
            return CallbackToFutureAdapter.getFuture(
                    completer -> {
                        cameraExecutor.execute(() -> {
                            InitializationException initException = null;
                            try {
                                // TODO(b/161302102): Remove the stored context. Only make use of
                                //  the context within the called method.
                                mAppContext = getApplicationFromContext(context);
                                if (mAppContext == null) {
                                    mAppContext = context.getApplicationContext();
                                }
                                CameraFactory.Provider cameraFactoryProvider =
                                        mCameraXConfig.getCameraFactoryProvider(null);
                                if (cameraFactoryProvider == null) {
                                    throw new InitializationException(new IllegalArgumentException(
                                            "Invalid app configuration provided. Missing "
                                                    + "CameraFactory."));
                                }

                                CameraThreadConfig cameraThreadConfig =
                                        CameraThreadConfig.create(mCameraExecutor,
                                                mSchedulerHandler);

                                mCameraFactory = cameraFactoryProvider.newInstance(context,
                                        cameraThreadConfig);

                                CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                                        mCameraXConfig.getDeviceSurfaceManagerProvider(null);
                                if (surfaceManagerProvider == null) {
                                    throw new InitializationException(new IllegalArgumentException(
                                            "Invalid app configuration provided. Missing "
                                                    + "CameraDeviceSurfaceManager."));
                                }
                                mSurfaceManager = surfaceManagerProvider.newInstance(context);

                                UseCaseConfigFactory.Provider configFactoryProvider =
                                        mCameraXConfig.getUseCaseConfigFactoryProvider(null);
                                if (configFactoryProvider == null) {
                                    throw new InitializationException(new IllegalArgumentException(
                                            "Invalid app configuration provided. Missing "
                                                    + "UseCaseConfigFactory."));
                                }
                                mDefaultConfigFactory = configFactoryProvider.newInstance(context);

                                if (cameraExecutor instanceof CameraExecutor) {
                                    CameraExecutor executor = (CameraExecutor) cameraExecutor;
                                    executor.init(mCameraFactory);
                                }

                                mCameraRepository.init(mCameraFactory);
                            } catch (InitializationException e) {
                                initException = e;
                            } catch (RuntimeException e) {
                                // For any unexpected RuntimeException, catch it instead of
                                // crashing.
                                initException = new InitializationException(e);
                            } finally {
                                synchronized (mInitializeLock) {
                                    mInitState = InternalInitState.INITIALIZED;
                                }
                                if (initException != null) {
                                    completer.setException(initException);
                                } else {
                                    completer.set(null);
                                }
                            }
                        });
                        return "CameraX initInternal";
                    });
        }
    }

    @NonNull
    private ListenableFuture<Void> shutdownInternal() {
        synchronized (mInitializeLock) {
            switch (mInitState) {
                case UNINITIALIZED:
                    mInitState = InternalInitState.SHUTDOWN;
                    return Futures.immediateFuture(null);

                case INITIALIZING:
                    throw new IllegalStateException(
                            "CameraX could not be shutdown when it is initializing.");

                case INITIALIZED:
                    mInitState = InternalInitState.SHUTDOWN;

                    mShutdownInternalFuture = CallbackToFutureAdapter.getFuture(
                            completer -> {
                                ListenableFuture<Void> future = mCameraRepository.deinit();

                                // Deinit camera executor at last to avoid RejectExecutionException.
                                future.addListener(() -> {
                                    if (mSchedulerThread != null) {
                                        // Ensure we shutdown the camera executor before
                                        // exiting the scheduler thread
                                        if (mCameraExecutor instanceof CameraExecutor) {
                                            CameraExecutor executor =
                                                    (CameraExecutor) mCameraExecutor;
                                            executor.deinit();
                                        }
                                        mSchedulerThread.quit();
                                        completer.set(null);
                                    }
                                }, mCameraExecutor);
                                return "CameraX shutdownInternal";
                            }
                    );
                    // Fall through
                case SHUTDOWN:
                    break;
            }
            // Already shutdown. Return the shutdown future.
            return mShutdownInternalFuture;
        }
    }

    private boolean isInitializedInternal() {
        synchronized (mInitializeLock) {
            return mInitState == InternalInitState.INITIALIZED;
        }
    }

    /** Internal initialization state. */
    private enum InternalInitState {
        /** The CameraX instance has not yet been initialized. */
        UNINITIALIZED,

        /** The CameraX instance is initializing. */
        INITIALIZING,

        /** The CameraX instance has been initialized. */
        INITIALIZED,

        /**
         * The CameraX instance has been shutdown.
         *
         * <p>Once the CameraX instance has been shutdown, it can't be used or re-initialized.
         */
        SHUTDOWN
    }
}
