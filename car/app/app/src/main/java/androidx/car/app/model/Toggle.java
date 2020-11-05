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
import androidx.car.app.IOnCheckedChangeListener;
import androidx.car.app.IOnDoneCallback;
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
    private final IOnCheckedChangeListener mOnCheckedChangeListener;
    @Keep
    private final boolean mIsChecked;

    /**
     * Constructs a new builder of {@link Toggle}.
     *
     * @throws NullPointerException if {@code onCheckedChangeListener} is {@code null}.
     */
    @NonNull
    @SuppressLint("ExecutorRegistration") // this listener is for transport to the host only.
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
     * Returns the {@link OnCheckedChangeListener} that is called when the checked state of the
     * {@link Toggle}is changed.
     *
     * @hide
     */
    // TODO(shiufai): re-surface this API with a wrapper around the AIDL class.
    @RestrictTo(LIBRARY)
    @NonNull
    public IOnCheckedChangeListener getOnCheckedChangeListener() {
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
        private IOnCheckedChangeListener mOnCheckedChangeListener;
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
         * {@link Toggle}
         * is changed.
         *
         * @throws NullPointerException if {@code onCheckedChangeListener} is {@code null}.
         */
        @NonNull
        // TODO(shiufai): remove MissingGetterMatchingBuilder once listener is properly exposed.
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public Builder setCheckedChangeListener(
                @NonNull OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener =
                    new OnCheckedChangeListenerStub(requireNonNull(onCheckedChangeListener));
            return this;
        }

        private Builder(OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener = new OnCheckedChangeListenerStub(
                    onCheckedChangeListener);
        }

        /** Constructs the {@link Toggle} defined by this builder. */
        @NonNull
        public Toggle build() {
            return new Toggle(this);
        }
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
