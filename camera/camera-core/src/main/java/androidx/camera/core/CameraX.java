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
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraRepository;
import androidx.camera.core.impl.CameraThreadConfig;
import androidx.camera.core.impl.CameraValidator;
import androidx.camera.core.impl.MetadataHolderService;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.ContextUtil;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.HandlerCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Main interface for accessing CameraX library.
 *
 * <p>This is a singleton class responsible for managing the set of camera instances.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@MainThread
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraX {
    private static final String TAG = "CameraX";
    private static final String RETRY_TOKEN = "retry_token";
    private static final long WAIT_INITIALIZED_TIMEOUT_MILLIS = 3000L;
    private static final long RETRY_SLEEP_MILLIS = 500L;

    static final Object INSTANCE_LOCK = new Object();

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("INSTANCE_LOCK")
    static CameraX sInstance = null;

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
    private final ListenableFuture<Void> mInitInternalFuture;

    @GuardedBy("mInitializeLock")
    private InternalInitState mInitState = InternalInitState.UNINITIALIZED;
    @GuardedBy("mInitializeLock")
    private ListenableFuture<Void> mShutdownInternalFuture = Futures.immediateFuture(null);
    private final Integer mMinLogLevel;

    private static final Object MIN_LOG_LEVEL_LOCK = new Object();
    @GuardedBy("MIN_LOG_LEVEL_LOCK")
    private static final SparseArray<Integer> sMinLogLevelReferenceCountMap = new SparseArray<>();

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraX(@NonNull Context context, @Nullable CameraXConfig.Provider configProvider) {
        if (configProvider != null) {
            mCameraXConfig = configProvider.getCameraXConfig();
        } else {
            CameraXConfig.Provider provider =
                    getConfigProvider(context);

            if (provider == null) {
                throw new IllegalStateException("CameraX is not configured properly. The most "
                        + "likely cause is you did not include a default implementation in your "
                        + "build such as 'camera-camera2'.");
            }

            mCameraXConfig = provider.getCameraXConfig();
        }

        Executor executor = mCameraXConfig.getCameraExecutor(null);
        Handler schedulerHandler = mCameraXConfig.getSchedulerHandler(null);
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

        // Retrieves the mini log level setting from config provider
        mMinLogLevel = mCameraXConfig.retrieveOption(CameraXConfig.OPTION_MIN_LOGGING_LEVEL, null);
        increaseMinLogLevelReference(mMinLogLevel);

        mInitInternalFuture = initInternal(context);
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
            initializeInstanceLocked(context, () -> cameraXConfig);
            return sInitializeFuture;
        }
    }

    @GuardedBy("INSTANCE_LOCK")
    private static void initializeInstanceLocked(@NonNull Context context,
            @Nullable CameraXConfig.Provider configProvider) {
        Preconditions.checkNotNull(context);
        Preconditions.checkState(sInstance == null, "CameraX already initialized.");
        CameraX cameraX = new CameraX(context, configProvider);
        sInstance = cameraX;
        sInitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
            synchronized (INSTANCE_LOCK) {
                // The sShutdownFuture should always be successful, otherwise it will not
                // propagate to transformAsync() due to the behavior of FutureChain.
                ListenableFuture<Void> future = FutureChain.from(sShutdownFuture)
                        .transformAsync(input -> cameraX.getInitializeFuture(),
                                CameraXExecutors.directExecutor());

                Futures.addCallback(future, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        completer.set(null);
                    }

                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void onFailure(Throwable t) {
                        Logger.w(TAG, "CameraX initialize() failed", t);
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
        sShutdownFuture = Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    synchronized (INSTANCE_LOCK) {
                        // Wait initialize complete
                        sInitializeFuture.addListener(() -> {
                            // Wait shutdownInternal complete
                            Futures.propagate(cameraX.getShutdownFuture(), completer);
                        }, CameraXExecutors.directExecutor());
                        return "CameraX shutdown";
                    }
                }));
        return sShutdownFuture;
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
     * Returns a future which contains a CameraX instance after initialization is complete.
     *
     * @param configProvider If this is non-null, it will be used to run the initialization
     *                      process. Otherwise, the initialization process will attempt to
     *                      retrieve the config provider from Application or meta-data.
     * @hide
     */
    @SuppressWarnings("FutureReturnValueIgnored") // shutdownLocked() should always succeed.
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static ListenableFuture<CameraX> getOrCreateInstance(@NonNull Context context,
            @Nullable CameraXConfig.Provider configProvider) {
        Preconditions.checkNotNull(context, "Context must not be null.");
        synchronized (INSTANCE_LOCK) {
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
                initializeInstanceLocked(context, configProvider);
                instanceFuture = getInstanceLocked();
            }

            return instanceFuture;
        }
    }

    @Nullable
    private static CameraXConfig.Provider getConfigProvider(@NonNull Context context) {
        CameraXConfig.Provider configProvider = null;
        Application application = ContextUtil.getApplicationFromContext(context);
        if (application instanceof CameraXConfig.Provider) {
            // Application is a CameraXConfig.Provider, use this directly
            configProvider = (CameraXConfig.Provider) application;
        } else {
            // Try to retrieve the CameraXConfig.Provider through meta-data provided by
            // implementation library.
            try {
                Context appContext = ContextUtil.getApplicationContext(context);
                ServiceInfo serviceInfo = appContext.getPackageManager().getServiceInfo(
                        new ComponentName(appContext, MetadataHolderService.class),
                        PackageManager.GET_META_DATA | PackageManager.MATCH_DISABLED_COMPONENTS);

                String defaultProviderClassName = null;
                if (serviceInfo.metaData != null) {
                    defaultProviderClassName = serviceInfo.metaData.getString(
                            "androidx.camera.core.impl.MetadataHolderService"
                                    + ".DEFAULT_CONFIG_PROVIDER");
                }
                if (defaultProviderClassName == null) {
                    Logger.e(TAG,
                            "No default CameraXConfig.Provider specified in meta-data. The most "
                                    + "likely cause is you did not include a default "
                                    + "implementation in your build such as 'camera-camera2'.");
                    return null;
                }
                Class<?> providerClass =
                        Class.forName(defaultProviderClassName);
                configProvider = (CameraXConfig.Provider) providerClass
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (PackageManager.NameNotFoundException
                    | ClassNotFoundException
                    | InstantiationException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | NullPointerException e) {
                Logger.e(TAG, "Failed to retrieve default CameraXConfig.Provider from "
                        + "meta-data", e);
            }
        }

        return configProvider;
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
     * Returns the {@link CameraDeviceSurfaceManager} instance.
     *
     * @throws IllegalStateException if the {@link CameraDeviceSurfaceManager} has not been set, due
     *                               to being uninitialized.
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
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public CameraRepository getCameraRepository() {
        return mCameraRepository;
    }

    /**
     * Returns the {@link UseCaseConfigFactory} instance.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public UseCaseConfigFactory getDefaultConfigFactory() {
        if (mDefaultConfigFactory == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mDefaultConfigFactory;
    }

    /**
     * Returns the initialize future.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public ListenableFuture<Void> getInitializeFuture() {
        return mInitInternalFuture;
    }

    /**
     * Returns the shutdown future.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public ListenableFuture<Void> getShutdownFuture() {
        return shutdownInternal();
    }

    private ListenableFuture<Void> initInternal(@NonNull Context context) {
        synchronized (mInitializeLock) {
            Preconditions.checkState(mInitState == InternalInitState.UNINITIALIZED,
                    "CameraX.initInternal() should only be called once per instance");
            mInitState = InternalInitState.INITIALIZING;
            return CallbackToFutureAdapter.getFuture(
                    completer -> {
                        initAndRetryRecursively(mCameraExecutor, SystemClock.elapsedRealtime(),
                                context, completer);
                        return "CameraX initInternal";
                    });
        }
    }

    /**
     * Initializes camera stack on the given thread and retry recursively until timeout.
     */
    private void initAndRetryRecursively(
            @NonNull Executor cameraExecutor,
            long startMs,
            @NonNull Context context,
            @NonNull CallbackToFutureAdapter.Completer<Void> completer) {
        cameraExecutor.execute(() -> {
            try {
                // TODO(b/161302102): Remove the stored context. Only make use of
                //  the context within the called method.
                mAppContext = ContextUtil.getApplicationFromContext(context);
                if (mAppContext == null) {
                    mAppContext = ContextUtil.getApplicationContext(context);
                }
                CameraFactory.Provider cameraFactoryProvider =
                        mCameraXConfig.getCameraFactoryProvider(null);
                if (cameraFactoryProvider == null) {
                    throw new InitializationException(new IllegalArgumentException(
                            "Invalid app configuration provided. Missing "
                                    + "CameraFactory."));
                }

                CameraThreadConfig cameraThreadConfig = CameraThreadConfig.create(mCameraExecutor,
                        mSchedulerHandler);

                CameraSelector availableCamerasLimiter =
                        mCameraXConfig.getAvailableCamerasLimiter(null);
                mCameraFactory = cameraFactoryProvider.newInstance(mAppContext,
                        cameraThreadConfig, availableCamerasLimiter);
                CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                        mCameraXConfig.getDeviceSurfaceManagerProvider(null);
                if (surfaceManagerProvider == null) {
                    throw new InitializationException(new IllegalArgumentException(
                            "Invalid app configuration provided. Missing "
                                    + "CameraDeviceSurfaceManager."));
                }
                mSurfaceManager = surfaceManagerProvider.newInstance(mAppContext,
                        mCameraFactory.getCameraManager(),
                        mCameraFactory.getAvailableCameraIds());

                UseCaseConfigFactory.Provider configFactoryProvider =
                        mCameraXConfig.getUseCaseConfigFactoryProvider(null);
                if (configFactoryProvider == null) {
                    throw new InitializationException(new IllegalArgumentException(
                            "Invalid app configuration provided. Missing "
                                    + "UseCaseConfigFactory."));
                }
                mDefaultConfigFactory = configFactoryProvider.newInstance(mAppContext);

                if (cameraExecutor instanceof CameraExecutor) {
                    CameraExecutor executor = (CameraExecutor) cameraExecutor;
                    executor.init(mCameraFactory);
                }

                mCameraRepository.init(mCameraFactory);

                // Please ensure only validate the camera at the last of the initialization.
                CameraValidator.validateCameras(mAppContext, mCameraRepository,
                        availableCamerasLimiter);

                // Set completer to null if the init was successful.
                setStateToInitialized();
                completer.set(null);
            } catch (CameraValidator.CameraIdListIncorrectException | InitializationException
                    | RuntimeException e) {
                if (SystemClock.elapsedRealtime() - startMs
                        < WAIT_INITIALIZED_TIMEOUT_MILLIS - RETRY_SLEEP_MILLIS) {
                    Logger.w(TAG, "Retry init. Start time " + startMs + " current time "
                            + SystemClock.elapsedRealtime(), e);
                    HandlerCompat.postDelayed(mSchedulerHandler, () -> initAndRetryRecursively(
                            cameraExecutor, startMs, mAppContext, completer), RETRY_TOKEN,
                            RETRY_SLEEP_MILLIS);

                } else {
                    // Set the state to initialized so it can be shut down properly.
                    setStateToInitialized();
                    if (e instanceof CameraValidator.CameraIdListIncorrectException) {
                        // Ignore the camera validation failure if it reaches the maximum retry
                        // time. Set complete.
                        Logger.e(TAG, "The device might underreport the amount of the cameras. "
                                + "Finish the initialize task since we are already reaching the "
                                + "maximum number of retries.");
                        completer.set(null);
                    } else if (e instanceof InitializationException) {
                        completer.setException(e);
                    } else {
                        // For any unexpected RuntimeException, catch it instead of crashing.
                        completer.setException(new InitializationException(e));
                    }
                }
            }
        });
    }

    private void setStateToInitialized() {
        synchronized (mInitializeLock) {
            mInitState = InternalInitState.INITIALIZED;
        }
    }

    @NonNull
    private ListenableFuture<Void> shutdownInternal() {
        synchronized (mInitializeLock) {
            mSchedulerHandler.removeCallbacksAndMessages(RETRY_TOKEN);
            switch (mInitState) {
                case UNINITIALIZED:
                    mInitState = InternalInitState.SHUTDOWN;
                    return Futures.immediateFuture(null);

                case INITIALIZING:
                    throw new IllegalStateException(
                            "CameraX could not be shutdown when it is initializing.");

                case INITIALIZED:
                    mInitState = InternalInitState.SHUTDOWN;
                    decreaseMinLogLevelReference(mMinLogLevel);
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

    private static void increaseMinLogLevelReference(@Nullable Integer minLogLevel) {
        synchronized (MIN_LOG_LEVEL_LOCK) {
            if (minLogLevel == null) {
                return;
            }

            Preconditions.checkArgumentInRange(minLogLevel, Log.DEBUG, Log.ERROR, "minLogLevel");

            int refCount = 1;
            // Retrieves the value from the map and plus one if there has been some other
            // instance refers to the same minimum log level.
            if (sMinLogLevelReferenceCountMap.get(minLogLevel) != null) {
                refCount = sMinLogLevelReferenceCountMap.get(minLogLevel) + 1;
            }
            sMinLogLevelReferenceCountMap.put(minLogLevel, refCount);
            updateOrResetMinLogLevel();
        }
    }

    private static void decreaseMinLogLevelReference(@Nullable Integer minLogLevel) {
        synchronized (MIN_LOG_LEVEL_LOCK) {
            if (minLogLevel == null) {
                return;
            }

            int refCount = sMinLogLevelReferenceCountMap.get(minLogLevel) - 1;

            if (refCount == 0) {
                // Removes the entry if reference count becomes zero.
                sMinLogLevelReferenceCountMap.remove(minLogLevel);
            } else {
                // Update the value if it is still referred by other instance.
                sMinLogLevelReferenceCountMap.put(minLogLevel, refCount);
            }
            updateOrResetMinLogLevel();
        }
    }

    @GuardedBy("MIN_LOG_LEVEL_LOCK")
    private static void updateOrResetMinLogLevel() {
        // Resets the minimum log level if there has been no instances refer to any minimum
        // log level setting.
        if (sMinLogLevelReferenceCountMap.size() == 0) {
            Logger.resetMinLogLevel();
            return;
        }

        // If the HashMap is not empty, find the minimum log level from the map and update it
        // to Logger.
        if (sMinLogLevelReferenceCountMap.get(Log.DEBUG) != null) {
            Logger.setMinLogLevel(Log.DEBUG);
        } else if (sMinLogLevelReferenceCountMap.get(Log.INFO) != null) {
            Logger.setMinLogLevel(Log.INFO);
        } else if (sMinLogLevelReferenceCountMap.get(Log.WARN) != null) {
            Logger.setMinLogLevel(Log.WARN);
        } else if (sMinLogLevelReferenceCountMap.get(Log.ERROR) != null) {
            Logger.setMinLogLevel(Log.ERROR);
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
