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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;

/** Represents a toggle that can have either a checked or unchecked state. */
@CarProtocol
public final class Toggle {
    /** A listener for handling checked state change events. */
    public interface OnCheckedChangeListener {
        /** Notifies that the checked state has changed. */
        void onCheckedChange(boolean isChecked);
    }

    @Keep
    @Nullable
    private final OnCheckedChangeDelegate mOnCheckedChangeDelegate;
    @Keep
    private final boolean mIsChecked;

    /**
     * Returns {@code true} if the toggle is checked.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Returns the {@link OnCheckedChangeDelegate} that is called when the checked state of
     * the {@link Toggle} is changed.
     */
    @NonNull
    public OnCheckedChangeDelegate getOnCheckedChangeDelegate() {
        return requireNonNull(mOnCheckedChangeDelegate);
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

    Toggle(Builder builder) {
        mIsChecked = builder.mIsChecked;
        mOnCheckedChangeDelegate = builder.mOnCheckedChangeDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Toggle() {
        mOnCheckedChangeDelegate = null;
        mIsChecked = false;
    }

    /** A builder of {@link Toggle}. */
    public static final class Builder {
        OnCheckedChangeDelegate mOnCheckedChangeDelegate;
        boolean mIsChecked;

        /**
         * Sets the initial checked state for {@link Toggle}.
         *
         * <p>The default state of a {@link Toggle} is unchecked.
         */
        @NonNull
        public Builder setChecked(boolean checked) {
            mIsChecked = checked;
            return this;
        }

        /** Constructs the {@link Toggle} defined by this builder. */
        @NonNull
        public Toggle build() {
            return new Toggle(this);
        }

        /**
         * Returns a new instance of a {@link Builder} with the given
         * {@link OnCheckedChangeListener}.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         *
         * @throws NullPointerException if {@code onCheckedChangeListener} is {@code null}
         */
        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull OnCheckedChangeListener onCheckedChangeListener) {
            mOnCheckedChangeDelegate = OnCheckedChangeDelegateImpl.create(onCheckedChangeListener);
        }
    }
}
