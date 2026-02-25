/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.car.app.dialer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.dialer.TelephoneKeypadTemplate.KeypadKey;
import androidx.car.app.model.ITelephoneKeypadCallback;
import androidx.car.app.utils.RemoteUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link TelephoneKeypadCallbackDelegate} that uses the
 * {@link ITelephoneKeypadCallback} binder to send events back to the client. Clients implement
 * the {@link androidx.car.app.dialer.TelephoneKeypadCallback} interface and this class abstracts
 * away the binder transactions and whatever success/failure outcomes.
 */
@SuppressLint("UnsafeOptInUsageError")
@RestrictTo(LIBRARY)
@CarProtocol
@KeepFields
public class TelephoneKeypadCallbackDelegateImpl implements TelephoneKeypadCallbackDelegate {
    private final @Nullable ITelephoneKeypadCallback mBinder;

    /**
     * Creates a new TelephoneKeypadCallbackDelegate.
     *
     * @param callback Callbacks for key presses.
     */
    @NonNull
    public static TelephoneKeypadCallbackDelegate create(
            @NonNull TelephoneKeypadCallback callback) {
        return new TelephoneKeypadCallbackDelegateImpl(callback);
    }

    @SuppressLint("ExecutorRegistration")
    private TelephoneKeypadCallbackDelegateImpl(@NonNull TelephoneKeypadCallback callback) {
        mBinder = new TelephoneKeypadCallbackStub(callback);
    }

    /** For serialization. */
    private TelephoneKeypadCallbackDelegateImpl() {
        mBinder = null;
    }

    @Override
    @SuppressLint("ExecutorRegistration")
    public void sendKeyLongPress(@KeypadKey int key, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mBinder).onKeyLongPress(key,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressLint("ExecutorRegistration")
    public void sendKeyDown(@KeypadKey int key, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mBinder).onKeyDown(key, RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressLint("ExecutorRegistration")
    public void sendKeyUp(@KeypadKey int key, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mBinder).onKeyUp(key, RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Binder implementation of the [ITelephoneKeypadCallback] AIDL interface that
     * dispatches IPC calls to the client on the main thread.
     */
    @CarProtocol
    @KeepFields
    @ExperimentalCarApi
    private static class TelephoneKeypadCallbackStub extends ITelephoneKeypadCallback.Stub {
        private final TelephoneKeypadCallback mTelephoneKeypadCallback;

        TelephoneKeypadCallbackStub(TelephoneKeypadCallback callback) {
            mTelephoneKeypadCallback = callback;
        }

        @Override
        public void onKeyLongPress(@KeypadKey int key, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(
                    callback,
                    "TelephoneKeypadCallback#onKeyLongPress",
                    () -> {
                        mTelephoneKeypadCallback.onKeyLongPress(key);
                        return null;
                    });
        }

        @Override
        public void onKeyDown(@KeypadKey int key, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(
                    callback,
                    "TelephoneKeypadCallback#onKeyDown",
                    () -> {
                        mTelephoneKeypadCallback.onKeyDown(key);
                        return null;
                    });
        }

        @Override
        public void onKeyUp(@KeypadKey int key, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(
                    callback,
                    "TelephoneKeypadCallback#onKeyUp",
                    () -> {
                        mTelephoneKeypadCallback.onKeyUp(key);
                        return null;
                    });
        }

        @Override
        public int getInterfaceVersion() {
            return super.VERSION;
        }
    }
}
