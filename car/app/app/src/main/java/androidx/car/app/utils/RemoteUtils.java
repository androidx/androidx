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
import static androidx.car.app.utils.CommonUtils.TAG;

import android.annotation.SuppressLint;
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
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

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
        void dispatch() throws BundlerException;
    }

    /**
     * Performs the remote call and handles exceptions thrown by the host.
     *
     * @throws SecurityException as a pass through from the host
     * @throws HostException     if the remote call fails with any other exception
     */
    @SuppressLint("LambdaLast")
    @Nullable
    public static <ReturnT> ReturnT call(@NonNull RemoteCall<ReturnT> remoteCall,
            @NonNull String callName) {
        try {
            Log.d(TAG, "Dispatching call " + callName + " to host");
            return remoteCall.call();
        } catch (SecurityException e) {
            // SecurityException is treated specially where we allow it to flow through since
            // this is specific to not having permissions to perform an API.
            throw e;
        } catch (RemoteException | RuntimeException e) {
            throw new HostException("Remote " + callName + " call failed", e);
        }
    }

    /**
     * Returns an {@link ISurfaceCallback} stub that invokes the input {@link SurfaceCallback}
     * if it is not {@code null}, or {@code null} if the input {@link SurfaceCallback} is {@code
     * null}
     */
    @Nullable
    public static ISurfaceCallback stubSurfaceCallback(@Nullable SurfaceCallback surfaceCallback) {
        if (surfaceCallback == null) {
            return null;
        }

        return new SurfaceCallbackStub(surfaceCallback);
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
    @SuppressLint("LambdaLast")
    public static void dispatchHostCall(
            @NonNull HostCall hostCall, @NonNull IOnDoneCallback callback,
            @NonNull String callName) {
        ThreadUtils.runOnMain(
                () -> {
                    try {
                        hostCall.dispatch();
                    } catch (BundlerException e) {
                        sendFailureResponse(callback, callName, e);
                        throw new HostException("Serialization failure in " + callName, e);
                    } catch (RuntimeException e) {
                        sendFailureResponse(callback, callName, e);
                        throw new RuntimeException(e);
                    }
                    sendSuccessResponse(callback, callName, null);
                });
    }

    public static void sendSuccessResponse(
            @NonNull IOnDoneCallback callback, @NonNull String callName,
            @Nullable Object response) {
        call(() -> {
            try {
                callback.onSuccess(response == null ? null : Bundleable.create(response));
            } catch (BundlerException e) {
                sendFailureResponse(callback, callName, e);
                throw new IllegalStateException("Serialization failure in " + callName, e);
            }
            return null;
        }, callName + " onSuccess");
    }

    public static void sendFailureResponse(@NonNull IOnDoneCallback callback,
            @NonNull String callName,
            @NonNull Throwable e) {
        call(() -> {
            try {
                callback.onFailure(Bundleable.create(new FailureResponse(e)));
            } catch (BundlerException bundlerException) {
                // Not possible, but catching since BundlerException is not runtime.
                throw new IllegalStateException(
                        "Serialization failure in " + callName, bundlerException);
            }
            return null;
        }, callName + " onFailure");
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
        private final SurfaceCallback mSurfaceCallback;

        SurfaceCallbackStub(SurfaceCallback surfaceCallback) {
            mSurfaceCallback = surfaceCallback;
        }

        @Override
        public void onSurfaceAvailable(Bundleable surfaceContainer, IOnDoneCallback callback) {
            dispatchHostCall(
                    () -> mSurfaceCallback.onSurfaceAvailable(
                            (SurfaceContainer) surfaceContainer.get()),
                    callback,
                    "onSurfaceAvailable");
        }

        @Override
        public void onVisibleAreaChanged(Rect visibleArea, IOnDoneCallback callback) {
            dispatchHostCall(
                    () -> mSurfaceCallback.onVisibleAreaChanged(visibleArea),
                    callback,
                    "onVisibleAreaChanged");
        }

        @Override
        public void onStableAreaChanged(Rect stableArea, IOnDoneCallback callback) {
            dispatchHostCall(
                    () -> mSurfaceCallback.onStableAreaChanged(stableArea), callback,
                    "onStableAreaChanged");
        }

        @Override
        public void onSurfaceDestroyed(Bundleable surfaceContainer, IOnDoneCallback callback) {
            dispatchHostCall(
                    () -> mSurfaceCallback.onSurfaceDestroyed(
                            (SurfaceContainer) surfaceContainer.get()),
                    callback,
                    "onSurfaceDestroyed");
        }
    }

    private RemoteUtils() {
    }
}
