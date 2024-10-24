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

import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.utils.RemoteUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Implementation class for {@link OnContentRefreshListener}.
 *
 */
@RestrictTo(LIBRARY)
@CarProtocol
@KeepFields
public class OnContentRefreshDelegateImpl implements OnContentRefreshDelegate {
    private final @Nullable IOnContentRefreshListener mListener;

    @Override
    public void sendContentRefreshRequested(@NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mListener)
                    .onContentRefreshRequested(RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link OnContentRefreshDelegate} for host-side callbacks to the input listener.
     */
    // This listener relates to UI event and is expected to be triggered on the main thread.
    @SuppressLint("ExecutorRegistration")
    public static @NonNull OnContentRefreshDelegate create(
            @NonNull OnContentRefreshListener listener) {
        return new OnContentRefreshDelegateImpl(listener);
    }

    private OnContentRefreshDelegateImpl(@NonNull OnContentRefreshListener listener) {
        mListener = new OnContentRefreshListenerStub(listener);
    }

    /** For serialization. */
    private OnContentRefreshDelegateImpl() {
        mListener = null;
    }

    @CarProtocol
    @KeepFields // We need to keep these stub for Bundler serialization logic.
    private static class OnContentRefreshListenerStub extends
            IOnContentRefreshListener.Stub {
        private final OnContentRefreshListener mOnContentRefreshListener;

        OnContentRefreshListenerStub(OnContentRefreshListener onContentRefreshListener) {
            mOnContentRefreshListener = onContentRefreshListener;
        }

        @Override
        public void onContentRefreshRequested(IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(callback, "onClick", () -> {
                mOnContentRefreshListener.onContentRefreshRequested();
                return null;
            });
        }
    }
}
