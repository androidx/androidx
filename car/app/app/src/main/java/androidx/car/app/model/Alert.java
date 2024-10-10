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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an alert with an optional icon, subtitle and actions.
 */
@CarProtocol
@RequiresCarApi(5)
@KeepFields
public final class Alert {

    /* Maximum number of actions allowed on the alert. */
    private static final int MAX_ACTION_COUNT = 2;

    /**
     * By setting the alert duration to this value, the progress bar (timer) on the
     * alert will not be shown.
     */
    public static final int DURATION_SHOW_INDEFINITELY = Integer.MAX_VALUE;

    private final int mId;
    private final @Nullable CarIcon mIcon;
    private final @NonNull CarText mTitle;
    private final @Nullable CarText mSubtitle;
    private final @NonNull List<Action> mActions;
    private final long mDuration;
    private final @Nullable AlertCallbackDelegate mCallbackDelegate;

    /** Returns the id of the alert. */
    public int getId() {
        return mId;
    }

    /** Returns the title displayed in the alert. */
    public @NonNull CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the subtitle displayed in the alert or {@code null} if the alert does
     * not have a subtitle.
     *
     * @see Builder#setSubtitle(CarText)
     */
    public @Nullable CarText getSubtitle() {
        return mSubtitle;
    }

    /**
     * Returns the {@link CarIcon} to display in the alert or {@code null} if the alert does
     * not have an icon.
     *
     * @see Builder#setIcon(CarIcon)
     */
    public @Nullable CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the {@link List} of {@link Action}s associated with the alert.
     *
     * @see Builder#addAction(Action)
     */
    public @NonNull List<Action> getActions() {
        return mActions;
    }

    /** Returns the maximum duration in milli seconds for which the alert can be shown. */
    public long getDurationMillis() {
        return mDuration;
    }

    /**
     * Returns the {@link AlertCallbackDelegate} that should be used for this alert.
     *
     * @see Builder#setCallback(AlertCallbackDelegate)
     */
    public @Nullable AlertCallbackDelegate getCallbackDelegate() {
        return mCallbackDelegate;
    }

    @Override
    public @NonNull String toString() {
        return "[id: " + mId + ", title: " + mTitle + ", icon: " + mIcon + "]";
    }

    Alert(Builder builder) {
        mId = builder.mId;
        mTitle = builder.mTitle;
        mSubtitle = builder.mSubtitle;
        mIcon = builder.mIcon;
        mActions = CollectionUtils.unmodifiableCopy(builder.mActions);
        mDuration = builder.mDuration;
        mCallbackDelegate = builder.mCallbackDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Alert() {
        mId = 0;
        mTitle = CarText.create("");
        mSubtitle = null;
        mIcon = null;
        mActions = new ArrayList<>();
        mDuration = 0;
        mCallbackDelegate = null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Alert)) {
            return false;
        }
        Alert otherAlert = (Alert) other;

        // Only compare the alertIds.
        return mId == otherAlert.mId;
    }

    /** A builder of {@link Alert}. */
    public static final class Builder {
        int mId;
        @NonNull CarText mTitle;
        @Nullable CarText mSubtitle;
        @Nullable CarIcon mIcon;
        @NonNull List<Action> mActions;
        long mDuration;
        @Nullable AlertCallbackDelegate mCallbackDelegate;

        /**
         * Creates an {@link Builder} instance.
         *
         * <p>Text spans are supported.
         *
         * @param alertId        The unique identifier used for the alert. Alerts with the same
         *                       id are considered equal.
         * @param title          The title of the alert.
         * @param durationMillis The maximum duration the alert can be shown in milli seconds.
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code duration} is not positive
         * @see CarText
         */
        public Builder(int alertId, @NonNull CarText title, long durationMillis) {
            if (durationMillis <= 0) {
                throw new IllegalArgumentException("Duration should be a positive number.");
            }

            mId = alertId;
            mTitle = requireNonNull(title);
            mDuration = durationMillis;
            mActions = new ArrayList<>(MAX_ACTION_COUNT);
        }

        /**
         * Sets the subtitle to display in the alert, with support for multiple length variants.
         *
         * <p>Text spans are supported.
         *
         * @throws NullPointerException if {@code subtitle} is {@code null}
         * @see CarText
         */
        public @NonNull Builder setSubtitle(@NonNull CarText subtitle) {
            mSubtitle = requireNonNull(subtitle);
            return this;
        }

        /**
         * Sets the icon to display in the alert.
         *
         * <h4>Icon Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * icons targeting a 88 x 88 dp bounding box. If the icon exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        public @NonNull Builder setIcon(@NonNull CarIcon icon) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(icon));
            mIcon = icon;
            return this;
        }

        /**
         * Adds the {@code action} to the list of actions on the alert.
         *
         * <p>An alert can have up to 2 actions.
         *
         * @throws IllegalStateException if more than 2 actions are added.
         */
        public @NonNull Builder addAction(@NonNull Action action) {
            if (mActions.size() >= MAX_ACTION_COUNT) {
                throw new IllegalStateException("Cannot add more than " + MAX_ACTION_COUNT
                        + " actions.");
            }
            mActions.add(action);
            return this;
        }

        /**
         * Sets the {@link AlertCallback} to receive alert related events.
         *
         * @throws NullPointerException if {@code callback} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public @NonNull Builder setCallback(@NonNull AlertCallback callback) {
            mCallbackDelegate = AlertCallbackDelegateImpl.create(requireNonNull(callback));
            return this;
        }

        /** Constructs the {@link Alert} defined by this builder. */
        public @NonNull Alert build() {
            return new Alert(this);
        }
    }
}
