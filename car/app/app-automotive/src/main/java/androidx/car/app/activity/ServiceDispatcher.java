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

package androidx.car.app.activity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.activity.renderer.IRendererService;
import androidx.car.app.serialization.BundlerException;

/**
 * {@link IRendererService} messages dispatcher, responsible for IPC error handling.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ServiceDispatcher {

    /** An interface for monitoring the binding state of a service connection. */
    public interface OnBindingListener {
        /** Returns true if the service connection is bound. */
        boolean isBound();
    }

    private final ErrorHandler mErrorHandler;
    private OnBindingListener mOnBindingListener;

    /** A one way call to the service */
    public interface OneWayCall {
        /** Remote invocation to execute */
        void invoke() throws RemoteException, BundlerException;
    }

    /**
     * A call to fetch a value from the service. This call will block the thread until the value
     * is received
     *
     * @param <T> Type of value to be returned
     */
    // TODO(b/184697399): Remove blocking return callbacks.
    public interface ReturnCall<T> {
        /** Remote invocation to execute */
        @Nullable
        T invoke() throws RemoteException, BundlerException;
    }

    public ServiceDispatcher(@NonNull ErrorHandler errorHandler,
            @NonNull OnBindingListener onBindingListener) {
        mErrorHandler = errorHandler;
        mOnBindingListener = onBindingListener;
    }

    @VisibleForTesting
    public void setOnBindingListener(@NonNull OnBindingListener onBindingListener) {
        mOnBindingListener = onBindingListener;
    }

    /** Dispatches the given {@link OneWayCall}. This is a non-blocking call. */
    public void dispatch(@NonNull OneWayCall call) {
        fetch(null, (ReturnCall<Void>) () -> {
            call.invoke();
            return null;
        });
    }

    /**
     * Retrieves a value from the service. This is a blocking call.
     *
     * @param call code to execute to retrieve the value
     * @param fallbackValue value to return in case the call is unsuccessful
     * @param <T> type of value to retrieve
     * @return the value retrieved
     */
    // TODO(b/184697399): Remove two-way calls as these are blocking.
    @Nullable
    public <T> T fetch(@Nullable T fallbackValue, @NonNull ReturnCall<T> call) {
        if (!mOnBindingListener.isBound()) {
            // Avoid dispatching messages if we are not bound to the service
            return fallbackValue;
        }
        try {
            // TODO(b/184697267): Implement ANR (application not responding) checks
            return call.invoke();
        } catch (RemoteException e) {
            mErrorHandler.onError(ErrorHandler.ErrorType.HOST_ERROR, e);
        } catch (BundlerException e) {
            mErrorHandler.onError(ErrorHandler.ErrorType.CLIENT_SIDE_ERROR, e);
        } catch (RuntimeException e) {
            mErrorHandler.onError(ErrorHandler.ErrorType.UNKNOWN_ERROR, e);
        }
        return fallbackValue;
    }
}
