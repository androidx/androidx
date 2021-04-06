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
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.ItemList.OnItemVisibilityChangedListener;
import androidx.car.app.utils.RemoteUtils;

/**
 * Implementation class for {@link OnItemVisibilityChangedDelegate}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
@CarProtocol
public class OnItemVisibilityChangedDelegateImpl implements
        OnItemVisibilityChangedDelegate {

    @Keep
    @Nullable
    private final IOnItemVisibilityChangedListener mStub;

    @Override
    public void sendItemVisibilityChanged(int startIndex, int rightIndex,
            @NonNull OnDoneCallback callback) {
        try {
            requireNonNull(mStub).onItemVisibilityChanged(startIndex, rightIndex,
                    RemoteUtils.createOnDoneCallbackStub(callback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private OnItemVisibilityChangedDelegateImpl(
            @NonNull OnItemVisibilityChangedListener listener) {
        mStub = new OnItemVisibilityChangedListenerStub(listener);
    }

    /** For serialization. */
    private OnItemVisibilityChangedDelegateImpl() {
        mStub = null;
    }

    @NonNull
    // This listener relates to UI event and is expected to be triggered on the main thread.
    @SuppressLint("ExecutorRegistration")
    static OnItemVisibilityChangedDelegate create(
            @NonNull OnItemVisibilityChangedListener listener) {
        return new OnItemVisibilityChangedDelegateImpl(listener);
    }

    /** Stub class for the {@link IOnItemVisibilityChangedListener} interface. */
    @Keep // We need to keep these stub for Bundler serialization logic.
    private static class OnItemVisibilityChangedListenerStub
            extends IOnItemVisibilityChangedListener.Stub {
        private final OnItemVisibilityChangedListener mListener;

        OnItemVisibilityChangedListenerStub(
                OnItemVisibilityChangedListener listener) {
            mListener = listener;
        }

        @Override
        public void onItemVisibilityChanged(
                int startIndexInclusive, int endIndexExclusive, IOnDoneCallback callback) {
            RemoteUtils.dispatchCallFromHost(
                    callback, "onItemVisibilityChanged", () -> {
                        mListener.onItemVisibilityChanged(
                                startIndexInclusive, endIndexExclusive);
                        return null;
                    }
            );
        }
    }
}
