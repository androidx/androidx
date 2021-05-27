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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.RemoteException;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.utils.RemoteUtils;

/**
 * Implementation class for {@link InputCallbackDelegate} to allow IPC for text-input-related
 * events.
 *
 * @hide
 */
@OptIn(markerClass = ExperimentalCarApi.class)
@RestrictTo(LIBRARY)
public class InputCallbackDelegateImpl implements InputCallbackDelegate {
    @Keep
    @Nullable
    private final IInputCallback mCallback;

    @Override
    public void sendInputSubmitted(@NonNull String text, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mCallback).onInputSubmitted(text,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendInputTextChanged(@NonNull String text, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mCallback).onInputTextChanged(text,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /** Creates an instance of {@link InputCallbackDelegate}. */
    // This mirrors the AIDL class and is not supposed to support an executor as an input.
    @SuppressLint("ExecutorRegistration")
    @NonNull
    public static InputCallbackDelegate create(@NonNull InputCallback callback) {
        return new InputCallbackDelegateImpl(requireNonNull(callback));
    }

    private InputCallbackDelegateImpl(@NonNull InputCallback callback) {
        mCallback = new OnInputCallbackStub(callback);
    }

    /** For serialization. */
    private InputCallbackDelegateImpl() {
        mCallback = null;
    }

    @Keep // We need to keep these stub for Bundler serialization logic.
    private static class OnInputCallbackStub extends IInputCallback.Stub {
        private final InputCallback mCallback;

        OnInputCallbackStub(InputCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onInputSubmitted(String value, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(callback, "onInputSubmitted",
                    () -> {
                        mCallback.onInputSubmitted(value);
                        return null;

                    });
        }

        @Override
        public void onInputTextChanged(String value, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(callback, "onInputTextChanged",
                    () -> {
                        mCallback.onInputTextChanged(value);
                        return null;

                    });
        }
    }
}
