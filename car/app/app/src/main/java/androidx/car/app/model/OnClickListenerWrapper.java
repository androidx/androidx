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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.utils.RemoteUtils;

/**
 * Internal state object to pass additional state along with the wrapped {@code IOnClickListener}.
 */
// TODO(shiufai): Replace code tag with the correct AIDL wrapper.
public class OnClickListenerWrapper {

    @Keep
    @Nullable
    private final IOnClickListener mListener;
    @Keep
    private final boolean mIsParkedOnly;

    /**
     * @hide
     */
    // TODO(shiufai): re-surface this API with a wrapper around the AIDL class.
    @RestrictTo(LIBRARY)
    @NonNull
    public IOnClickListener getListener() {
        return requireNonNull(mListener);
    }

    /**
     * Whether the click listener is for parked-only scenarios.
     */
    public boolean isParkedOnly() {
        return mIsParkedOnly;
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(LIBRARY)
    @SuppressLint("ExecutorRegistration") // this listener is for transport to the host only.
    public static OnClickListenerWrapper create(@NonNull OnClickListener listener) {
        return new OnClickListenerWrapper(
                new OnClickListenerStub(listener), listener instanceof ParkedOnlyOnClickListener);
    }

    private OnClickListenerWrapper(IOnClickListener listener, boolean isParkedOnly) {
        this.mListener = listener;
        this.mIsParkedOnly = isParkedOnly;
    }

    /** For serialization. */
    private OnClickListenerWrapper() {
        mListener = null;
        mIsParkedOnly = false;
    }

    @Keep // We need to keep these stub for Bundler serialization logic.
    private static class OnClickListenerStub extends IOnClickListener.Stub {
        private final OnClickListener mOnClickListener;

        private OnClickListenerStub(OnClickListener onClickListener) {
            this.mOnClickListener = onClickListener;
        }

        @Override
        public void onClick(IOnDoneCallback callback) {
            RemoteUtils.dispatchHostCall(mOnClickListener::onClick, callback, "onClick");
        }
    }
}
