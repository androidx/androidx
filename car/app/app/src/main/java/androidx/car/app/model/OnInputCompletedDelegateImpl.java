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
import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.utils.RemoteUtils;

/**
 * Implementation class for {@link OnInputCompletedDelegate} to allow IPC for text-input-related
 * events.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
@ExperimentalCarApi
public class OnInputCompletedDelegateImpl implements OnInputCompletedDelegate {

    @Keep
    @Nullable
    private final IOnInputCompletedListener mListener;

    @Override
    public void sendInputCompleted(@NonNull String text, @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mListener).onInputCompleted(text,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /** Creates an instance of {@link OnInputCompletedDelegateImpl}. */
    // This mirrors the AIDL class and is not supposed to support an executor as an input.
    @SuppressLint("ExecutorRegistration")
    @NonNull
    public static OnInputCompletedDelegate create(@NonNull OnInputCompletedListener listener) {
        return new OnInputCompletedDelegateImpl(requireNonNull(listener));
    }

    private OnInputCompletedDelegateImpl(@NonNull OnInputCompletedListener listener) {
        mListener = new OnInputCompletedStub(listener);
    }

    /** For serialization. */
    private OnInputCompletedDelegateImpl() {
        mListener = null;
    }

    @Keep // We need to keep these stub for Bundler serialization logic.
    private static class OnInputCompletedStub extends IOnInputCompletedListener.Stub {
        private final OnInputCompletedListener mListener;

        OnInputCompletedStub(OnInputCompletedListener listener) {
            mListener = listener;
        }

        @Override
        public void onInputCompleted(String value, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(callback, "onInputCompleted",
                    () -> {
                        mListener.onInputCompleted(value);
                        return null;

                    });
        }
    }
}
