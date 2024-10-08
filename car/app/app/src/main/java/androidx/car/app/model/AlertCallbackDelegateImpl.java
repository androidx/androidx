/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.utils.RemoteUtils;

/**
 * Implementation class for {@link AlertCallbackDelegate} to allow IPC for cancel events.
 *
 */
@RestrictTo(LIBRARY)
@CarProtocol
@KeepFields
public class AlertCallbackDelegateImpl implements AlertCallbackDelegate {
    @Nullable
    private final IAlertCallback mCallback;

    @NonNull
    @SuppressLint("ExecutorRegistration")
    public static AlertCallbackDelegate create(@NonNull AlertCallback callback) {
        return new AlertCallbackDelegateImpl(callback);
    }

    private AlertCallbackDelegateImpl(@NonNull AlertCallback callback) {
        mCallback = new AlertCallbackStub(callback);
    }

    /** For serialization. */
    private AlertCallbackDelegateImpl() {
        mCallback = null;
    }

    @Override
    public void sendCancel(@AlertCallback.Reason int reason, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mCallback).onAlertCancelled(reason,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendDismiss(@NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mCallback)
                    .onAlertDismissed(RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @CarProtocol
    @KeepFields // We need to keep these stub for Bundler serialization logic.
    private static class AlertCallbackStub extends IAlertCallback.Stub {
        private final AlertCallback mCallback;

        AlertCallbackStub(AlertCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onAlertCancelled(@AlertCallback.Reason int reason, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(callback, "onCancel", () -> {
                mCallback.onCancel(reason);
                return null;
            });
        }

        @Override
        public void onAlertDismissed(IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(callback, "onDismiss", () -> {
                mCallback.onDismiss();
                return null;
            });
        }
    }
}
