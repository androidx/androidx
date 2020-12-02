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
import static androidx.car.app.model.CarColor.DEFAULT;
import static androidx.car.app.model.constraints.CarColorConstraints.STANDARD_ONLY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.text.TextUtils;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.lifecycle.LifecycleOwner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents an action with an optional icon and text.
 *
 * <p>Actions may be displayed differently depending on the template or model they are added to. For
 * example, the host may decide to display an action as a floating action button (FAB) when
 * displayed over a map, as a button when displayed in a {@link Pane}, or as a simple icon with no
 * title when displayed within a {@link Row}.
 *
 * <h4>Standard actions</h4>
 *
 * A set of standard, built-in {@link Action} instances is available with a few of the common basic
 * actions car apps may need (for example a {@link #BACK} action).
 *
 * <p>With the exception of {@link #APP_ICON} and {@link #BACK}, an app can provide a custom title
 * and icon for the action. However, depending on the template the action belongs to, the title or
 * icon may be disallowed. If such restrictions apply, the documentation of the APIs that consume
 * the action will note them accordingly.
 */
public final class Action {
    /**
     * The type of action represented by the {@link Action } instance.
     *
     * @hide
     */
    // TODO(shiufai): investigate how to expose IntDefs if needed.
    @RestrictTo(LIBRARY)
    @IntDef(
            value = {
                    TYPE_UNKNOWN,
                    TYPE_CUSTOM,
                    TYPE_APP_ICON,
                    TYPE_BACK,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
    }

    static final int FLAG_STANDARD = 1 << 16;

    /**
     * An unknown action type.
     */
    public static final int TYPE_UNKNOWN = 0;

    /**
     * An app-defined custom action type.
     */
    public static final int TYPE_CUSTOM = 1;

    /**
     * An action representing an app's icon.
     *
     * @see #APP_ICON
     */
    public static final int TYPE_APP_ICON = 2 | FLAG_STANDARD;

    /**
     * An action to navigate back in the user interface.
     *
     * @see #BACK
     */
    public static final int TYPE_BACK = 3 | FLAG_STANDARD;

    /**
     * A standard action to show the app's icon.
     *
     * <p>This action is non-interactive.
     */
    @NonNull
    public static final Action APP_ICON = new Action(TYPE_APP_ICON);

    /**
     * A standard action to navigate back in the user interface.
     *
     * <p>The default behavior for a back press will call
     * {@link androidx.car.app.ScreenManager#pop}.
     *
     * <p>To override the default behavior, register a {@link OnBackPressedCallback} via
     * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}, which
     * you can retrieve from {@link CarContext#getOnBackPressedDispatcher()}.
     */
    @NonNull
    public static final Action BACK = new Action(TYPE_BACK);

    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final CarIcon mIcon;
    @Keep
    private final CarColor mBackgroundColor;
    @Keep
    @Nullable
    private final OnClickListenerWrapper mListener;
    @Keep
    @ActionType
    private final int mType;

    /** Constructs a new builder of {@link Action}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a {@link Builder} instance configured with the same data as this {@link Action}
     * instance.
     */
    @NonNull
    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Returns the title displayed in the action, or {@code null} if the action does not have a
     * title.
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link CarIcon} to displayed in the action, or {@code null} if the action does
     * not
     * have an icon.
     */
    @Nullable
    public CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the {@link CarColor} used for the background color of the action.
     */
    @NonNull
    public CarColor getBackgroundColor() {
        return mBackgroundColor;
    }


    @ActionType
    public int getType() {
        return mType;
    }


    public boolean isStandard() {
        return isStandardActionType(mType);
    }


    @Nullable
    public OnClickListenerWrapper getOnClickListener() {
        return mListener;
    }

    @Override
    @NonNull
    public String toString() {
        return "[type: " + typeToString(mType) + ", icon: " + mIcon + ", bkg: " + mBackgroundColor
                + "]";
    }

    /**
     * Converts the given {@code type} into a string representation.
     */
    @NonNull
    public static String typeToString(@ActionType int type) {
        switch (type) {
            case TYPE_CUSTOM:
                return "CUSTOM";
            case TYPE_APP_ICON:
                return "APP_ICON";
            case TYPE_BACK:
                return "BACK";
            default:
                return "<unknown>";
        }
    }

    /** Convenience constructor for standard action singletons. */
    private Action(@ActionType int type) {
        if (!isStandardActionType(type)) {
            throw new IllegalArgumentException(
                    "Standard action constructor used with non standard type");
        }

        mTitle = null;
        mIcon = null;
        mBackgroundColor = DEFAULT;

        // The listeners can be set, for actions that support it, by copying the standard action
        // instance with the newBuilder and setting it.
        mListener = null;
        this.mType = type;
    }

    private Action(
            @Nullable CarText title,
            @Nullable CarIcon icon,
            CarColor backgroundColor,
            @Nullable OnClickListenerWrapper listener,
            @ActionType int type) {
        this.mTitle = title;
        this.mIcon = icon;
        this.mBackgroundColor = backgroundColor;
        this.mListener = listener;
        this.mType = type;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Action() {
        mTitle = null;
        mIcon = null;
        mBackgroundColor = DEFAULT;
        mListener = null;
        mType = TYPE_UNKNOWN;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mType, mListener == null, mIcon == null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Action)) {
            return false;
        }
        Action otherAction = (Action) other;

        // Don't compare callback, only ensure if it is present in one, it is also present in
        // the other.
        return Objects.equals(mTitle, otherAction.mTitle)
                && mType == otherAction.mType
                && Objects.equals(mIcon, otherAction.mIcon)
                && Objects.equals(mListener == null, otherAction.mListener == null);
    }

    private static boolean isStandardActionType(@ActionType int type) {
        return 0 != (type & FLAG_STANDARD);
    }

    /** A builder of {@link Action}. */
    public static final class Builder {
        @Nullable
        private CarText mTitle;
        @Nullable
        private CarIcon mIcon;
        @Nullable
        private OnClickListenerWrapper mListener;
        private CarColor mBackgroundColor = DEFAULT;
        @ActionType
        private int mType = TYPE_CUSTOM;

        /**
         * Sets the title to display in the action, or {@code null} to not display a title.
         *
         * <p>The title of a standard action can be set with this method. Actions, including
         * standard
         * actions, don't have a title by default.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            this.mTitle = title == null ? null : CarText.create(title);
            return this;
        }

        /**
         * Sets the icon to display in the action, or {@code null} to not display an icon.
         *
         * <p>Icons can't be set in standard actions.
         *
         * <h4>Icon Sizing Guidance</h4>
         *
         * The provided icon should have a maximum size of 36 x 36 dp. If the icon exceeds this
         * maximum
         * size in either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving the aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that
         * work with different car screen pixel densities.
         */
        @NonNull
        public Builder setIcon(@Nullable CarIcon icon) {
            CarIconConstraints.DEFAULT.validateOrThrow(icon);
            this.mIcon = icon;
            return this;
        }

        /**
         * Sets the {@link OnClickListener} to call when the action is clicked.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         */
        @NonNull
        @SuppressLint("ExecutorRegistration")
        public Builder setOnClickListener(@Nullable OnClickListener listener) {
            this.mListener = listener == null ? null : OnClickListenerWrapperImpl.create(listener);
            return this;
        }

        /**
         * Sets the background color to be used for the action.
         *
         * <h4>Requirements</h4>
         *
         * <p>The host may ignore this color and use the default instead if the color does not
         * pass the
         * contrast requirements.
         *
         * @param backgroundColor the {@link CarColor} to set as background. Use {@link
         *                        CarColor#DEFAULT} to let the host pick a default.
         * @throws IllegalArgumentException if {@code backgroundColor} is not a standard color.
         * @throws NullPointerException     if {@code backgroundColor} is {@code null}.
         */
        @NonNull
        public Builder setBackgroundColor(@NonNull CarColor backgroundColor) {
            STANDARD_ONLY.validateOrThrow(requireNonNull(backgroundColor));
            this.mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * Constructs the {@link Action} defined by this builder.
         *
         * @throws IllegalStateException if the action is not a standard action and does not have an
         *                               icon or a title.
         * @throws IllegalStateException if a listener is set on either {@link #APP_ICON} or {@link
         *                               #BACK}.
         * @throws IllegalStateException if an icon or title is set on either {@link #APP_ICON} or
         *                               {@link #BACK}.
         */
        @NonNull
        public Action build() {
            if (mType == TYPE_UNKNOWN) {
                throw new IllegalStateException("Missing action type");
            }
            boolean isStandard = isStandardActionType(mType);
            if (!isStandard && mIcon == null && (mTitle == null || TextUtils.isEmpty(
                    mTitle.getText()))) {
                throw new IllegalStateException("An action must have either an icon or a title");
            }

            if ((mType == TYPE_APP_ICON || mType == TYPE_BACK)) {
                if (mListener != null) {
                    throw new IllegalStateException(
                            "An on-click listener can't be set on the standard back or app-icon "
                                    + "action");
                }

                if (mIcon != null || (mTitle != null && !TextUtils.isEmpty(mTitle.getText()))) {
                    throw new IllegalStateException(
                            "An icon or title can't be set on the standard back or app-icon "
                                    + "action");
                }
            }

            return new Action(mTitle, mIcon, mBackgroundColor, mListener, mType);
        }

        private Builder() {
        }

        private Builder(Action action) {
            mTitle = action.mTitle;
            mIcon = action.mIcon;
            mBackgroundColor = action.mBackgroundColor;
            mListener = action.mListener;
            mType = action.mType;
        }
    }
}
