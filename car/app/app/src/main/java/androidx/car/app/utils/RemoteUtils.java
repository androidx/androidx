/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.utils.LogTags.TAG;
import static androidx.lifecycle.Lifecycle.State.CREATED;

import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.FailureResponse;
import androidx.car.app.HostException;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.ISurfaceCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.lifecycle.Lifecycle;

/**
 * Assorted utilities to deal with serialization of remote calls.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class RemoteUtils {
    /** An interface that defines a remote call to be made. */
    public interface RemoteCall<ReturnT> {
        /** Performs the remote call. */
        @Nullable
        ReturnT call() throws RemoteException;
    }

    /**
     * A method that the host dispatched to be run on the main thread and notify the host of
     * success/failure.
     */
    public interface HostCall {
        /**
         * Dispatches the call and returns its outcome if any.
         *
         * @return the response from the app for the host call, or {@code null} if there is
         * nothing to return
         */
        @Nullable
        Object dispatch() throws BundlerException;
    }

    /**
     * Performs the remote call to the host and handles exceptions thrown by the host.
     *
     * @return the value that the host returns for the IPC
     * @throws RemoteException   if the host is unresponsive
     * @throws SecurityException as a pass through from the host
     * @throws HostException     if the remote call fails with any other exception
     */
    @Nullable
    public static <ReturnT> ReturnT dispatchCallToHostForResult(@NonNull String callName,
            @NonNull RemoteCall<ReturnT> remoteCall) throws RemoteException {
        try {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Dispatching call " + callName + " to host");
            }
            return remoteCall.call();
        } catch (SecurityException e) {
            // SecurityException is treated specially where we allow it to flow through since
            // this is specific to not having permissions to perform an API.
            throw e;
        } catch (RuntimeException e) {
            throw new HostException("Remote " + callName + " call failed", e);
        }
    }

    /**
     * Performs the remote call to the host and handles exceptions thrown by the host.
     *
     * @throws SecurityException as a pass through from the host
     * @throws HostException     if the remote call fails with any other exception
     */
    public static void dispatchCallToHost(@NonNull String callName,
            @NonNull RemoteCall<?> remoteCall) {
        try {
            dispatchCallToHostForResult(callName, remoteCall);
        } catch (RemoteException e) {
            // The host is dead, don't crash the app, just log.
            Log.e(LogTags.TAG_DISPATCH, "Host unresponsive when dispatching call " + callName, e);
        }
    }

    /**
     * Returns an {@link ISurfaceCallback} stub that invokes the input {@link SurfaceCallback}
     * if it is not {@code null}, or {@code null} if the input {@link SurfaceCallback} is {@code
     * null}
     *
     * @param lifecycle       the lifecycle of the session to be used to not dispatch calls out of
     *                        lifecycle.
     * @param surfaceCallback the callback to wrap in an {@link ISurfaceCallback}
     */
    @Nullable
    public static ISurfaceCallback stubSurfaceCallback(@NonNull Lifecycle lifecycle,
            @Nullable SurfaceCallback surfaceCallback) {
        if (surfaceCallback == null) {
            return null;
        }

        return new SurfaceCallbackStub(lifecycle, surfaceCallback);
    }

    /**
     * Dispatches the given {@link HostCall} to the client in the main thread, and notifies the host
     * of outcome.
     *
     * <p>If the app processes the response, will call {@link IOnDoneCallback#onSuccess} with a
     * {@code null}.
     *
     * <p>If the app throws an exception, will call {@link IOnDoneCallback#onFailure} with a {@link
     * FailureResponse} including information from the caught exception.
     */
    public static void dispatchCallFromHost(
            @NonNull IOnDoneCallback callback, @NonNull String callName,
            @NonNull HostCall hostCall) {
        // TODO(b/180530156): Move callers that should be lifecycle aware once we can put a
        //  lifecycle into a Template and propagate it to the models.
        ThreadUtils.runOnMain(
                () -> {
                    try {
                        sendSuccessResponseToHost(callback, callName, hostCall.dispatch());
                    } catch (RuntimeException e) {
                        // Catch exceptions, notify the host of it, then rethrow it.
                        // This allows the host to log, and show an error to the user.
                        sendFailureResponseToHost(callback, callName, e);
                        throw new RuntimeException(e);
                    } catch (BundlerException e) {
                        sendFailureResponseToHost(callback, callName, e);
                    }
                });
    }

    /**
     * Dispatches the given {@link HostCall} to the client in the main thread, but only if the
     * provided {@link Lifecycle} has a state of at least created, and notifies the host of outcome.
     *
     * <p>If the app processes the response, will call {@link IOnDoneCallback#onSuccess} with a
     * {@code null}.
     *
     * <p>If the app throws an exception, will call {@link IOnDoneCallback#onFailure} with a {@link
     * FailureResponse} including information from the caught exception.
     *
     * <p>If the {@code lifecycle} provided is {@code null} or not at least created, will call
     * {@link IOnDoneCallback#onFailure} with a {@link FailureResponse}.
     */
    public static void dispatchCallFromHost(
            @Nullable Lifecycle lifecycle, @NonNull IOnDoneCallback callback,
            @NonNull String callName, @NonNull HostCall hostCall) {
        ThreadUtils.runOnMain(
                () -> {
                    if (lifecycle == null || !lifecycle.getCurrentState().isAtLeast(CREATED)) {
                        sendFailureResponseToHost(callback, callName, new IllegalStateException(
                                "Lifecycle is not at least created when dispatching " + hostCall));
                        return;
                    }

                    dispatchCallFromHost(callback, callName, hostCall);
                });
    }

    /**
     * Dispatches the given {@link HostCall} to the client in the main thread.
     */
    public static void dispatchCallFromHost(@Nullable Lifecycle lifecycle,
            @NonNull String callName, @NonNull HostCall hostCall) {
        ThreadUtils.runOnMain(
                () -> {
                    try {
                        if (lifecycle == null || !lifecycle.getCurrentState().isAtLeast(CREATED)) {
                            Log.w(LogTags.TAG_DISPATCH,
                                    "Lifecycle is not at least created when dispatching "
                                            + hostCall);
                            return;
                        }

                        hostCall.dispatch();
                    } catch (BundlerException e) {
                        // Not possible, but catching since BundlerException is not runtime.
                        Log.e(LogTags.TAG_DISPATCH,
                                "Serialization failure in " + callName, e);
                    }
                });
    }

    /**
     * Invoke onSuccess on the given {@code callback} instance with the given {@code response}.
     */
    // TODO(b/178748627): the nullable annotation from the AIDL file is not being considered.
    @SuppressWarnings("NullAway")
    public static void sendSuccessResponseToHost(
            @NonNull IOnDoneCallback callback, @NonNull String callName,
            @Nullable Object response) {
        dispatchCallToHost(callName + " onSuccess", () -> {
            try {
                callback.onSuccess(response == null ? null : Bundleable.create(response));
            } catch (BundlerException e) {
                sendFailureResponseToHost(callback, callName, e);
            }
            return null;
        });
    }

    /**
     * Invoke onFailure on the given {@code callback} instance with the given {@link Throwable}.
     */
    public static void sendFailureResponseToHost(@NonNull IOnDoneCallback callback,
            @NonNull String callName,
            @NonNull Throwable e) {
        dispatchCallToHost(callName + " onFailure", () -> {
            try {
                callback.onFailure(Bundleable.create(new FailureResponse(e)));
            } catch (BundlerException bundlerException) {
                // Not possible, but catching since BundlerException is not runtime.
                Log.e(LogTags.TAG_DISPATCH,
                        "Serialization failure in " + callName, bundlerException);
            }
            return null;
        });
    }

    /**
     * Provides a {@link IOnDoneCallback} that forwards success and failure callbacks to a
     * {@link OnDoneCallback}.
     */
    @NonNull
    public static IOnDoneCallback createOnDoneCallbackStub(@NonNull OnDoneCallback callback) {
        return new IOnDoneCallback.Stub() {
            @Override
            public void onSuccess(Bundleable response) {
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(Bundleable failureResponse) {
                callback.onFailure(failureResponse);
            }
        };
    }

    private static class SurfaceCallbackStub extends ISurfaceCallback.Stub {
        private final Lifecycle mLifecycle;
        private final SurfaceCallback mSurfaceCallback;

        SurfaceCallbackStub(Lifecycle lifecycle, SurfaceCallback surfaceCallback) {
            mLifecycle = lifecycle;
            mSurfaceCallback = surfaceCallback;
        }

        @Override
        public void onSurfaceAvailable(Bundleable surfaceContainer, IOnDoneCallback callback) {
            dispatchCallFromHost(
                    mLifecycle,
                    callback,
                    "onSurfaceAvailable",
                    () -> {
                        mSurfaceCallback.onSurfaceAvailable(
                                (SurfaceContainer) surfaceContainer.get());
                        return null;
                    });
        }

        @Override
        public void onVisibleAreaChanged(Rect visibleArea, IOnDoneCallback callback) {
            dispatchCallFromHost(
                    mLifecycle,
                    callback,
                    "onVisibleAreaChanged",
                    () -> {
                        mSurfaceCallback.onVisibleAreaChanged(visibleArea);
                        return null;
                    });
        }

        @Override
        public void onStableAreaChanged(Rect stableArea, IOnDoneCallback callback) {
            dispatchCallFromHost(
                    mLifecycle, callback,
                    "onStableAreaChanged", () -> {
                        mSurfaceCallback.onStableAreaChanged(stableArea);
                        return null;
                    });
        }

        @Override
        public void onSurfaceDestroyed(Bundleable surfaceContainer, IOnDoneCallback callback) {
            dispatchCallFromHost(
                    mLifecycle,
                    callback,
                    "onSurfaceDestroyed",
                    () -> {
                        mSurfaceCallback.onSurfaceDestroyed(
                                (SurfaceContainer) surfaceContainer.get());
                        return null;
                    });
        }
        @RequiresCarApi(2)
        @Override
        public void onScroll(float distanceX, float distanceY) {
            dispatchCallFromHost(mLifecycle, "onScroll", () -> {
                mSurfaceCallback.onScroll(distanceX, distanceY);
                return null;
            });
        }
        @RequiresCarApi(2)
        @Override
        public void onFling(float velocityX, float velocityY) {
            dispatchCallFromHost(mLifecycle, "onFling", () -> {
                mSurfaceCallback.onFling(velocityX, velocityY);
                return null;
            });
        }
        @RequiresCarApi(2)
        @Override
        public void onScale(float focusX, float focusY, float scaleFactor) {
            dispatchCallFromHost(mLifecycle, "onScale", () -> {
                mSurfaceCallback.onScale(focusX, focusY, scaleFactor);
                return null;
            });
        }
    }

    private RemoteUtils() {
    }
}
