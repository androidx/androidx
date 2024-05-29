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

import static androidx.camera.core.CameraUnavailableException.CAMERA_ERROR;
import static androidx.camera.core.impl.CameraValidator.CameraIdListIncorrectException;
import static androidx.camera.core.impl.CameraValidator.validateCameras;

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
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraProviderExecutionState;
import androidx.camera.core.impl.CameraRepository;
import androidx.camera.core.impl.CameraThreadConfig;
import androidx.camera.core.impl.MetadataHolderService;
import androidx.camera.core.impl.QuirkSettings;
import androidx.camera.core.impl.QuirkSettingsHolder;
import androidx.camera.core.impl.QuirkSettingsLoader;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.ContextUtil;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.HandlerCompat;
import androidx.core.util.Preconditions;
import androidx.tracing.Trace;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;

/**
 * Main interface for accessing CameraX library.
 *
 * <p>This is a singleton class responsible for managing the set of camera instances.
 *
 */
@OptIn(markerClass = ExperimentalRetryPolicy.class)
@MainThread
@RestrictTo(Scope.LIBRARY_GROUP)
public final class CameraX {
    private static final String TAG = "CameraX";
    private static final String RETRY_TOKEN = "retry_token";

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
    private final RetryPolicy mRetryPolicy;
    private final ListenableFuture<Void> mInitInternalFuture;

    @GuardedBy("mInitializeLock")
    private InternalInitState mInitState = InternalInitState.UNINITIALIZED;
    @GuardedBy("mInitializeLock")
    private ListenableFuture<Void> mShutdownInternalFuture = Futures.immediateFuture(null);
    private final Integer mMinLogLevel;

    private static final Object MIN_LOG_LEVEL_LOCK = new Object();
    @GuardedBy("MIN_LOG_LEVEL_LOCK")
    private static final SparseArray<Integer> sMinLogLevelReferenceCountMap = new SparseArray<>();

    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraX(@NonNull Context context, @Nullable CameraXConfig.Provider configProvider) {
        this(context, configProvider, new QuirkSettingsLoader());
    }

    @VisibleForTesting
    CameraX(@NonNull Context context, @Nullable CameraXConfig.Provider configProvider,
            @NonNull Function<Context, QuirkSettings> quirkSettingsLoader) {
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
        // Update quirks settings as early as possible since device quirks are loaded statically.
        updateQuirkSettings(context, mCameraXConfig.getQuirkSettings(), quirkSettingsLoader);

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

        mRetryPolicy = new RetryPolicy.Builder(
                mCameraXConfig.getCameraProviderInitRetryPolicy()).build();
        mInitInternalFuture = initInternal(context);
    }

    /**
     * Returns the {@link CameraFactory} instance.
     *
     * @throws IllegalStateException if the {@link CameraFactory} has not been set, due to being
     *                               uninitialized.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraFactory getCameraFactory() {
        if (mCameraFactory == null) {
            throw new IllegalStateException("CameraX not initialized yet.");
        }

        return mCameraFactory;
    }

    @Nullable
    @SuppressWarnings("deprecation")
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

    /**
     * Updates the global {@link QuirkSettings} instance based on provided settings or
     * application metadata.
     *
     * <p>This method determines the source of the quirk settings to be used:
     *
     * <ol>
     *   <li>If `cameraXConfigQuirkSettings` is not null, those settings are used directly.</li>
     *   <li>Otherwise, if the application's meta-data contains quirk settings, those are loaded
     *       using {@link QuirkSettingsLoader}.</li>
     *   <li>If neither of the above is available, the default {@link QuirkSettings} are used.</li>
     * </ol>
     *
     * <p>The determined quirk settings are then set as the global instance in
     * {@link QuirkSettingsHolder}.
     *
     * @param context                    The context used for loading quirk settings from app
     *                                   metadata.
     * @param cameraXConfigQuirkSettings Quirk settings provided through the CameraX configuration,
     *                                   or null if not available.
     * @param quirkSettingsLoader        Typically a {@link QuirkSettingsLoader} instance to load
     *                                   settings from context. In unit tests, this may be an
     *                                   fake implementation.
     */
    private static void updateQuirkSettings(@NonNull Context context,
            @Nullable QuirkSettings cameraXConfigQuirkSettings,
            @NonNull Function<Context, QuirkSettings> quirkSettingsLoader) {
        QuirkSettings quirkSettings;
        if (cameraXConfigQuirkSettings != null) {
            quirkSettings = cameraXConfigQuirkSettings;
            Logger.d(TAG, "QuirkSettings from CameraXConfig: " + quirkSettings);
        } else {
            quirkSettings = quirkSettingsLoader.apply(context);
            Logger.d(TAG, "QuirkSettings from app metadata: " + quirkSettings);
        }
        if (quirkSettings == null) {
            quirkSettings = QuirkSettingsHolder.DEFAULT;
            Logger.d(TAG, "QuirkSettings by default: " + quirkSettings);
        }
        QuirkSettingsHolder.instance().set(quirkSettings);
    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} instance.
     *
     * @throws IllegalStateException if the {@link CameraDeviceSurfaceManager} has not been set, due
     *                               to being uninitialized.
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
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public CameraRepository getCameraRepository() {
        return mCameraRepository;
    }

    /**
     * Returns the {@link UseCaseConfigFactory} instance.
     *
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
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public ListenableFuture<Void> getInitializeFuture() {
        return mInitInternalFuture;
    }

    /**
     * Returns the shutdown future.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public ListenableFuture<Void> shutdown() {
        return shutdownInternal();
    }

    private ListenableFuture<Void> initInternal(@NonNull Context context) {
        synchronized (mInitializeLock) {
            Preconditions.checkState(mInitState == InternalInitState.UNINITIALIZED,
                    "CameraX.initInternal() should only be called once per instance");
            mInitState = InternalInitState.INITIALIZING;
            return CallbackToFutureAdapter.getFuture(
                    completer -> {
                        initAndRetryRecursively(mCameraExecutor, SystemClock.elapsedRealtime(), 1,
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
            int attemptCount,
            @NonNull Context context,
            @NonNull CallbackToFutureAdapter.Completer<Void> completer) {
        cameraExecutor.execute(() -> {
            Trace.beginSection("CX:initAndRetryRecursively");
            Context appContext = ContextUtil.getApplicationContext(context);
            try {
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
                long cameraOpenRetryMaxTimeoutInMillis =
                        mCameraXConfig.getCameraOpenRetryMaxTimeoutInMillisWhileResuming();
                mCameraFactory = cameraFactoryProvider.newInstance(appContext,
                        cameraThreadConfig,
                        availableCamerasLimiter,
                        cameraOpenRetryMaxTimeoutInMillis);
                CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                        mCameraXConfig.getDeviceSurfaceManagerProvider(null);
                if (surfaceManagerProvider == null) {
                    throw new InitializationException(new IllegalArgumentException(
                            "Invalid app configuration provided. Missing "
                                    + "CameraDeviceSurfaceManager."));
                }
                mSurfaceManager = surfaceManagerProvider.newInstance(appContext,
                        mCameraFactory.getCameraManager(),
                        mCameraFactory.getAvailableCameraIds());

                UseCaseConfigFactory.Provider configFactoryProvider =
                        mCameraXConfig.getUseCaseConfigFactoryProvider(null);
                if (configFactoryProvider == null) {
                    throw new InitializationException(new IllegalArgumentException(
                            "Invalid app configuration provided. Missing "
                                    + "UseCaseConfigFactory."));
                }
                mDefaultConfigFactory = configFactoryProvider.newInstance(appContext);

                if (cameraExecutor instanceof CameraExecutor) {
                    CameraExecutor executor = (CameraExecutor) cameraExecutor;
                    executor.init(mCameraFactory);
                }

                mCameraRepository.init(mCameraFactory);

                // Please ensure only validate the camera at the last of the initialization.
                validateCameras(appContext, mCameraRepository, availableCamerasLimiter);

                // Set completer to null if the init was successful.
                if (attemptCount > 1) {
                    // Reset execution trace status on success
                    traceExecutionState(null);
                }
                setStateToInitialized();
                completer.set(null);
            } catch (CameraIdListIncorrectException | InitializationException
                     | RuntimeException e) {
                RetryPolicy.ExecutionState executionState =
                        new CameraProviderExecutionState(startMs, attemptCount, e);
                RetryPolicy.RetryConfig retryConfig = mRetryPolicy.onRetryDecisionRequested(
                        executionState);
                traceExecutionState(executionState);
                if (retryConfig.shouldRetry() && attemptCount < Integer.MAX_VALUE) {
                    Logger.w(TAG, "Retry init. Start time " + startMs + " current time "
                            + SystemClock.elapsedRealtime(), e);
                    HandlerCompat.postDelayed(mSchedulerHandler, () -> initAndRetryRecursively(
                            cameraExecutor, startMs, attemptCount + 1, appContext,
                            completer), RETRY_TOKEN, retryConfig.getRetryDelayInMillis());

                } else {
                    synchronized (mInitializeLock) {
                        mInitState = InternalInitState.INITIALIZING_ERROR;
                    }
                    if (retryConfig.shouldCompleteWithoutFailure()) {
                        // Ignoring camera failure for compatibility reasons. Initialization will
                        // be marked as complete, but some camera features might be unavailable.
                        setStateToInitialized();
                        completer.set(null);
                    } else if (e instanceof CameraIdListIncorrectException) {
                        String message = "Device reporting less cameras than anticipated. On real"
                                + " devices: Retrying initialization might resolve temporary "
                                + "camera errors. On emulators: Ensure virtual camera "
                                + "configuration matches supported camera features as reported by"
                                + " PackageManager#hasSystemFeature. Available cameras: "
                                + ((CameraIdListIncorrectException) e).getAvailableCameraCount();
                        Logger.e(TAG, message, e);
                        completer.setException(new InitializationException(
                                new CameraUnavailableException(CAMERA_ERROR, message)));
                    } else if (e instanceof InitializationException) {
                        completer.setException(e);
                    } else {
                        // For any unexpected RuntimeException, catch it instead of crashing.
                        completer.setException(new InitializationException(e));
                    }
                }
            } finally {
                Trace.endSection();
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

                case INITIALIZING_ERROR:
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
                                    }
                                    completer.set(null);
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

    /**
     * Returns whether the instance is in InternalInitState.INITIALIZED state.
     */
    boolean isInitialized() {
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

        /** The CameraX instance encounters error when initializing. */
        INITIALIZING_ERROR,

        /** The CameraX instance has been initialized. */
        INITIALIZED,

        /**
         * The CameraX instance has been shutdown.
         *
         * <p>Once the CameraX instance has been shutdown, it can't be used or re-initialized.
         */
        SHUTDOWN
    }

    private void traceExecutionState(@Nullable RetryPolicy.ExecutionState state) {
        if (Trace.isEnabled()) {
            int status = state != null ? state.getStatus() : -1;
            Trace.setCounter("CX:CameraProvider-RetryStatus", status);
        }
    }
}
