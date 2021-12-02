/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.testing;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.InitializationException;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Utility functions for creating and shutting down an CameraX instance.
 *
 * {@link #initialize} or {@link #getOrCreateInstance} can be used to get or create a CameraX
 * instance. {@link #shutdown()} can be used to shutdown the existing instance. The utility class
 * helps to manage that only one CameraX instance exists at a time. If a new CameraX instance
 * with different config is needed, {@link #shutdown()} needs to be called first and then calling
 * {@link #initialize} or {@link #getOrCreateInstance} to create a new one with the desired
 * settings.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraXUtil {
    private static final String TAG = "CameraXUtil";
    private static final CameraXUtil sInstance = new CameraXUtil();

    final Object mLock = new Object();
    @GuardedBy("mLock")
    private CameraX mCameraX = null;
    @GuardedBy("mLock")
    private ListenableFuture<Void> mInitializeFuture =
            Futures.immediateFailedFuture(new IllegalStateException("CameraX is not initialized."));
    @GuardedBy("mLock")
    private ListenableFuture<Void> mShutdownFuture = Futures.immediateFuture(null);

    /**
     * Initializes a CameraX instance with the given context and application configuration.
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
     */
    @NonNull
    public static ListenableFuture<Void> initialize(@NonNull Context context,
            @NonNull CameraXConfig cameraXConfig) {
        return sInstance.initializeInternal(context, () -> cameraXConfig);
    }

    @NonNull
    private ListenableFuture<Void> initializeInternal(@NonNull Context context,
            @Nullable CameraXConfig.Provider configProvider) {
        synchronized (mLock) {
            Preconditions.checkNotNull(context);
            Preconditions.checkState(mCameraX == null, "A CameraX instance has already been "
                    + "initialized.");
            CameraX cameraX = new CameraX(context, configProvider);
            mCameraX = cameraX;
            mInitializeFuture = CallbackToFutureAdapter.getFuture(completer -> {
                synchronized (mLock) {
                    // The sShutdownFuture should always be successful, otherwise it will not
                    // propagate to transformAsync() due to the behavior of FutureChain.
                    ListenableFuture<Void> future = FutureChain.from(mShutdownFuture)
                            .transformAsync(input -> cameraX.getInitializeFuture(),
                                    CameraXExecutors.directExecutor());

                    Futures.addCallback(future, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            completer.set(null);
                        }

                        @SuppressLint("SyntheticAccessor")
                        @SuppressWarnings("FutureReturnValueIgnored")
                        @Override
                        public void onFailure(Throwable t) {
                            Logger.w(TAG, "CameraX initialize failed", t);
                            // Call shutdown() automatically, if initialization fails.
                            synchronized (mLock) {
                                // Make sure it is the same instance to prevent reinitialization
                                // during initialization.
                                if (mCameraX == cameraX) {
                                    shutdownInternal();
                                }
                            }
                            completer.setException(t);
                        }
                    }, CameraXExecutors.directExecutor());
                    return "CameraXUtil-initialize";
                }
            });

            return mInitializeFuture;
        }
    }

    /**
     * Shutdown the created CameraX instance so that a new CameraX instance can be initialized
     * again.
     *
     * @return A {@link ListenableFuture} representing the shutdown task.
     */
    @NonNull
    public static ListenableFuture<Void> shutdown() {
        return sInstance.shutdownInternal();
    }

    @NonNull
    private ListenableFuture<Void> shutdownInternal() {
        synchronized (mLock) {
            if (mCameraX == null) {
                // If it is already or will be shutdown, return the future directly.
                return mShutdownFuture;
            }

            CameraX cameraX = mCameraX;
            mCameraX = null;

            // Do not use FutureChain to chain the initFuture, because FutureChain.transformAsync()
            // will not propagate if the input initFuture is failed. We want to always
            // shutdown the CameraX instance to ensure that resources are freed.
            mShutdownFuture = Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                    completer -> {
                        synchronized (mLock) {
                            // Wait initialize complete
                            mInitializeFuture.addListener(() -> {
                                // Wait shutdownInternal complete
                                Futures.propagate(cameraX.shutdown(), completer);
                            }, CameraXExecutors.directExecutor());
                            return "CameraXUtil shutdown";
                        }
                    }));
            return mShutdownFuture;
        }
    }

    /**
     * Returns a future which contains a CameraX instance after initialization is complete.
     *
     * @param configProvider If this is non-null, it will be used to run the initialization
     *                      process. Otherwise, the initialization process will attempt to
     *                      retrieve the config provider from Application or meta-data.
     */
    @SuppressWarnings("FutureReturnValueIgnored") // shutdownLocked() should always succeed.
    @NonNull
    public static ListenableFuture<CameraX> getOrCreateInstance(@NonNull Context context,
            @Nullable CameraXConfig.Provider configProvider) {
        return sInstance.getOrCreateInstanceInternal(context, configProvider);
    }

    @NonNull
    private ListenableFuture<CameraX> getOrCreateInstanceInternal(@NonNull Context context,
            @Nullable CameraXConfig.Provider configProvider) {
        Preconditions.checkNotNull(context, "Context must not be null.");
        synchronized (mLock) {
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
                    ListenableFuture<Void> shutdownFuture = shutdownInternal();
                    shutdownFuture.addListener(() -> {
                        Logger.w(TAG, "Shutdown due to get or create CameraX instance failed.");
                    }, CameraXExecutors.directExecutor());
                    instanceFuture = null;
                }
            }

            if (instanceFuture == null && initializeInternal(context, configProvider) != null) {
                instanceFuture = getInstanceLocked();
            }

            return instanceFuture;
        }
    }

    @GuardedBy("mLock")
    @NonNull
    private ListenableFuture<CameraX> getInstanceLocked() {
        CameraX cameraX = mCameraX;
        if (cameraX == null) {
            return Futures.immediateFailedFuture(new IllegalStateException("Must "
                    + "call CameraXUtil.initialize() first"));
        }

        return Futures.transform(mInitializeFuture, nullVoid -> cameraX,
                CameraXExecutors.directExecutor());
    }

    private CameraXUtil() {}
}
