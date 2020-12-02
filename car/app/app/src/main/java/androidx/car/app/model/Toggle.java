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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.IOnCheckedChangeListener;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnCheckedChangeListenerWrapper;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.WrappedRuntimeException;
import androidx.car.app.utils.RemoteUtils;

/** Represents a toggle that can have either a checked or unchecked state. */
public class Toggle {
    /** A listener for handling checked state change events. */
    public interface OnCheckedChangeListener {
        /** Notifies that the checked state has changed. */
        void onCheckedChange(boolean isChecked);
    }

    @Keep
    @Nullable
    private final OnCheckedChangeListenerWrapper mOnCheckedChangeListener;
    @Keep
    private final boolean mIsChecked;

    /**
     * Constructs a new builder of {@link Toggle} with the given {@link OnCheckedChangeListener}.
     *
     * <p>Note that the listener relates to UI events and will be executed on the main thread
     * using {@link Looper#getMainLooper()}.z
     *
     * @throws NullPointerException if {@code onCheckedChangeListener} is {@code null}.
     */
    @NonNull
    @SuppressLint("ExecutorRegistration")
    public static Builder builder(@NonNull OnCheckedChangeListener onCheckedChangeListener) {
        return new Builder(requireNonNull(onCheckedChangeListener));
    }

    /**
     * Returns {@code true} if the toggle is checked.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Returns the {@link OnCheckedChangeListenerWrapper} that is called when the checked state of
     * the {@link Toggle} is changed.
     */
    @NonNull
    public OnCheckedChangeListenerWrapper getOnCheckedChangeListener() {
        return requireNonNull(mOnCheckedChangeListener);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ isChecked: " + mIsChecked + "]";
    }

    @Override
    public int hashCode() {
        return Boolean.valueOf(mIsChecked).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Toggle)) {
            return false;
        }
        Toggle otherToggle = (Toggle) other;

        // Don't compare listener.
        return mIsChecked == otherToggle.mIsChecked;
    }

    private Toggle(Builder builder) {
        mIsChecked = builder.mIsChecked;
        mOnCheckedChangeListener = builder.mOnCheckedChangeListener;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Toggle() {
        mOnCheckedChangeListener = null;
        mIsChecked = false;
    }

    /** A builder of {@link Toggle}. */
    public static final class Builder {
        private OnCheckedChangeListenerWrapper mOnCheckedChangeListener;
        private boolean mIsChecked;

        /**
         * Sets the initial checked state for {@link Toggle}.
         *
         * <p>The default state of a {@link Toggle} is unchecked.
         */
        @NonNull
        public Builder setChecked(boolean checked) {
            this.mIsChecked = checked;
            return this;
        }

        /**
         * Sets the {@link OnCheckedChangeListener} to call when the checked state of the
         * {@link Toggle} is changed.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         *
         * @throws NullPointerException if {@code onCheckedChangeListener} is {@code null}.
         */
        @NonNull
        @SuppressLint({"ExecutorRegistration"})
        public Builder setOnCheckedChangeListener(
                @NonNull OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener =
                    createOnCheckedChangeListener(onCheckedChangeListener);
            return this;
        }

        private Builder(@NonNull OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener =
                    createOnCheckedChangeListener(onCheckedChangeListener);
        }

        /** Constructs the {@link Toggle} defined by this builder. */
        @NonNull
        public Toggle build() {
            return new Toggle(this);
        }
    }

    private static OnCheckedChangeListenerWrapper createOnCheckedChangeListener(
            @NonNull OnCheckedChangeListener listener) {
        return new OnCheckedChangeListenerWrapper() {
            private final IOnCheckedChangeListener mOnCheckedChangeListener =
                    new OnCheckedChangeListenerStub(listener);

            @Override
            public void onCheckedChange(boolean isChecked, @NonNull OnDoneCallback callback) {
                try {
                    mOnCheckedChangeListener.onCheckedChange(isChecked,
                            RemoteUtils.createOnDoneCallbackStub(callback));
                } catch (RemoteException e) {
                    throw new WrappedRuntimeException(e);
                }
            }
        };
    }

    @Keep // We need to keep these stub for Bundler serialization logic.
    private static class OnCheckedChangeListenerStub extends IOnCheckedChangeListener.Stub {
        private final OnCheckedChangeListener mOnCheckedChangeListener;

        private OnCheckedChangeListenerStub(OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener = onCheckedChangeListener;
        }

        @Override
        public void onCheckedChange(boolean isChecked, IOnDoneCallback callback) {
            RemoteUtils.dispatchHostCall(
                    () -> mOnCheckedChangeListener.onCheckedChange(isChecked), callback,
                    "onCheckedChange");
        }
    }
}
