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
import static androidx.car.app.model.constraints.CarColorConstraints.UNCONSTRAINED;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.text.TextUtils;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
@CarProtocol
@KeepFields
public final class Action {
    /**
     * The type of action represented by the {@link Action} instance.
     */
    @OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
    @RestrictTo(LIBRARY)
    @IntDef(
            value = {
                    TYPE_CUSTOM,
                    TYPE_APP_ICON,
                    TYPE_BACK,
                    TYPE_PAN,
                    TYPE_COMPOSE_MESSAGE,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
    }

    /**
     * The flag of action represented by the {@link Action} instance.
     */
    @RestrictTo(LIBRARY)
    @IntDef(
            flag = true,
            value = {
                    FLAG_PRIMARY,
                    FLAG_IS_PERSISTENT,
                    FLAG_DEFAULT
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionFlag {
    }

    static final int TYPE_STANDARD = 1 << 16;

    /**
     * An app-defined custom action type.
     */
    public static final int TYPE_CUSTOM = 1;

    /**
     * An action representing an app's icon.
     *
     * @see #APP_ICON
     */
    public static final int TYPE_APP_ICON = 2 | TYPE_STANDARD;

    /**
     * An action to navigate back in the user interface.
     *
     * @see #BACK
     */
    public static final int TYPE_BACK = 3 | TYPE_STANDARD;

    /**
     * An action to toggle the pan mode in a map-based template.
     */
    public static final int TYPE_PAN = 4 | TYPE_STANDARD;

    /**
     * An action to allow user compose a message.
     */
    @ExperimentalCarApi
    @RequiresCarApi(7)
    public static final int TYPE_COMPOSE_MESSAGE = 5 | TYPE_STANDARD;

    /**
     * Indicates that this action is the most important one, out of a set of other actions.
     *
     * <p>The action with this flag may be treated differently by the host depending on where they
     * are used. For example, it may be colored or ordered differently to align with the vehicle's
     * look and feel. See the documentation on where the {@link Action} is added for more details on
     * any restriction(s) that might apply.
     */
    @RequiresCarApi(4)
    public static final int FLAG_PRIMARY = 1 << 0;

    /**
     * Indicates that this action will not fade in/out inside an {@link ActionStrip}.
     */
    @RequiresCarApi(5)
    public static final int FLAG_IS_PERSISTENT = 1 << 1;

    /**
     * Indicates that this action is the default action out of a set of other actions.
     *
     * <p>The action with this flag may be treated differently by the host depending on where
     * they are used. For example, it may be set as the default action to be triggered when the
     * Alerter times out in the AlertCard. The first action with the FLAG_DEFAULT in an action
     * list will be treated as the Default Action. See the documentation on where the
     * {@link Action} is added for more details on any restriction(s) that might apply.
     */
    @RequiresCarApi(5)
    public static final int FLAG_DEFAULT = 1 << 2;

    /**
     * A standard action to show the app's icon.
     *
     * <p>This action is non-interactive.
     */
    public static final @NonNull Action APP_ICON = new Action(TYPE_APP_ICON);

    /**
     * A standard action to show the message compose button
     *
     * <p>This action is interactive.
     */
    @ExperimentalCarApi
    @RequiresCarApi(7)
    public static final @NonNull Action COMPOSE_MESSAGE = new Action(TYPE_COMPOSE_MESSAGE);

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
    public static final @NonNull Action BACK = new Action(TYPE_BACK);

    /**
     * A standard action to toggle the pan mode in a map-based template.
     *
     * <p>If the app does not provide a custom icon, a default pan icon will be used.
     *
     * <p>You can set a custom icon in a pan action with the following code:
     *
     * <pre>{@code
     * Action panAction = new Action.Builder(Action.PAN).setIcon(customIcon).build();
     * }</pre>
     */
    public static final @NonNull Action PAN = new Action(TYPE_PAN);

    private final boolean mIsEnabled;
    private final @Nullable CarText mTitle;
    private final @Nullable CarIcon mIcon;
    private final CarColor mBackgroundColor;
    private final @Nullable OnClickDelegate mOnClickDelegate;
    @ActionType
    private final int mType;
    @ActionFlag
    private final int mFlags;

    /**
     * Returns the title displayed in the action or {@code null} if the action does not have a
     * title.
     *
     * @see Builder#setTitle(CharSequence)
     */
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link CarIcon} to display in the action or {@code null} if the action does
     * not have an icon.
     *
     * @see Builder#setIcon(CarIcon)
     */
    public @Nullable CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the {@link CarColor} used for the background color of the action.
     *
     * @see Builder#setBackgroundColor(CarColor)
     */
    public @Nullable CarColor getBackgroundColor() {
        return mBackgroundColor;
    }

    /** Returns the type of the action. */
    @ActionType
    public int getType() {
        return mType;
    }

    /** Returns flags affecting how this action should be treated */
    @RequiresCarApi(4)
    @ActionFlag
    public int getFlags() {
        return mFlags;
    }

    /** Returns whether the action is a standard action such as {@link #BACK}. */
    public boolean isStandard() {
        return isStandardActionType(mType);
    }

    /**
     * Returns the {@link OnClickDelegate} that should be used for this action.
     */
    public @Nullable OnClickDelegate getOnClickDelegate() {
        return mOnClickDelegate;
    }

    /**
     * Returns {@code true} if the action is enabled.
     */
    @RequiresCarApi(5)
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public @NonNull String toString() {
        return "[type: " + typeToString(mType) + ", icon: " + mIcon
                + ", bkg: " + mBackgroundColor + ", isEnabled: " + mIsEnabled + "]";
    }

    /**
     * Converts the given {@code type} into a string representation.
     */
    @OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
    public static @NonNull String typeToString(@ActionType int type) {
        switch (type) {
            case TYPE_CUSTOM:
                return "CUSTOM";
            case TYPE_APP_ICON:
                return "APP_ICON";
            case TYPE_BACK:
                return "BACK";
            case TYPE_PAN:
                return "PAN";
            case TYPE_COMPOSE_MESSAGE:
                return "COMPOSE_MESSAGE";
            default:
                return "<unknown>";
        }
    }

    /** Convenience constructor for standard action singletons. */
    private Action(@ActionType int type) {
        if (type == TYPE_CUSTOM) {
            throw new IllegalArgumentException(
                    "Standard action constructor used with non standard type");
        }

        mTitle = null;
        mIcon = null;
        mBackgroundColor = DEFAULT;
        mOnClickDelegate = null;
        mType = type;
        mFlags = 0;
        mIsEnabled = true;
    }

    Action(Builder builder) {
        mTitle = builder.mTitle;
        mIcon = builder.mIcon;
        mBackgroundColor = builder.mBackgroundColor;
        mOnClickDelegate = builder.mOnClickDelegate;
        mType = builder.mType;
        mFlags = builder.mFlags;
        mIsEnabled = builder.mIsEnabled;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Action() {
        mTitle = null;
        mIcon = null;
        mBackgroundColor = DEFAULT;
        mOnClickDelegate = null;
        mType = TYPE_CUSTOM;
        mFlags = 0;
        mIsEnabled = true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mType, mOnClickDelegate == null, mIcon == null, mIsEnabled);
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
                && Objects.equals(mOnClickDelegate == null, otherAction.mOnClickDelegate == null)
                && Objects.equals(mFlags, otherAction.mFlags)
                && mIsEnabled == otherAction.mIsEnabled;
    }

    static boolean isStandardActionType(@ActionType int type) {
        return 0 != (type & TYPE_STANDARD);
    }

    /** A builder of {@link Action}. */
    public static final class Builder {
        boolean mIsEnabled = true;
        @Nullable CarText mTitle;
        @Nullable CarIcon mIcon;
        @Nullable OnClickDelegate mOnClickDelegate;
        CarColor mBackgroundColor = DEFAULT;
        @ActionType
        int mType = TYPE_CUSTOM;
        @ActionFlag
        int mFlags = 0;

        /**
         * Sets the title to display in the action.
         *
         * <p>Support for text spans depends on where the action is used. See the documentation
         * of the specific APIs taking an {@link Action} for details.
         *
         * @throws NullPointerException if {@code title} is {@code null}
         */
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /**
         * Sets the title to display in the action, with support for multiple length variants.
         *
         * <p>Support for text spans depends on where the action is used. For example,
         * most templates taking an action support {@link ForegroundCarColorSpan}, but this may
         * vary. See the documentation of the specific APIs taking an {@link Action} for details.
         *
         * @throws NullPointerException if {@code title} is {@code null}
         * @see CarText
         */
        public @NonNull Builder setTitle(@NonNull CarText title) {
            mTitle = requireNonNull(title);
            return this;
        }

        /**
         * Sets the icon to display in the action.
         *
         * <p>Unless set with this method, the action will not have an icon.
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
         * Sets the {@link OnClickListener} to call when the action is clicked.
         *
         * <p>Unless set with this method, the action will not have a click listener.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         *
         * @throws NullPointerException if {@code listener} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public @NonNull Builder setOnClickListener(@NonNull OnClickListener listener) {
            mOnClickDelegate = OnClickDelegateImpl.create(listener);
            return this;
        }

        /**
         * Sets the background color to be used for the action.
         *
         * <h4>Requirements</h4>
         *
         * <p>Depending on contrast requirements, capabilities of the vehicle screens, or other
         * factors, the color may be ignored by the host or overridden by the vehicle system. See
         * the documentation on where the {@link Action} is added for more details on any other
         * restriction(s) that might apply.
         *
         * @param backgroundColor the {@link CarColor} to set as background. Use {@link
         *                        CarColor#DEFAULT} to let the host pick a default
         * @throws NullPointerException if {@code backgroundColor} is {@code null}
         */
        public @NonNull Builder setBackgroundColor(@NonNull CarColor backgroundColor) {
            UNCONSTRAINED.validateOrThrow(requireNonNull(backgroundColor));
            mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the initial enabled state for {@link Action}.
         *
         * <p>The default state of a {@link Action} is enabled.
         */
        @RequiresCarApi(5)
        public @NonNull Builder setEnabled(boolean enabled) {
            mIsEnabled = enabled;
            return this;
        }

        /** Sets flags affecting how this action should be treated. */
        @RequiresCarApi(4)
        public @NonNull Builder setFlags(@ActionFlag int flags) {
            mFlags |= flags;
            return this;
        }

        /**
         * Constructs the {@link Action} defined by this builder.
         *
         * @throws IllegalStateException if the action is not a standard action and does not have an
         *                               icon or a title, if a listener is set on either
         *                               {@link #APP_ICON} or {@link #BACK}, or if an icon or
         *                               title is set on either {@link #APP_ICON} or {@link #BACK}
         */
        @OptIn(markerClass = androidx.car.app.annotations.ExperimentalCarApi.class)
        public @NonNull Action build() {
            boolean isStandard = isStandardActionType(mType);
            if (!isStandard && mIcon == null && (mTitle == null || TextUtils.isEmpty(
                    mTitle.toString()))) {
                throw new IllegalStateException("An action must have either an icon or a title");
            }

            if (mType == TYPE_APP_ICON || mType == TYPE_BACK) {
                if (mOnClickDelegate != null) {
                    throw new IllegalStateException(String.format(
                            "An on-click listener can't be set on an action of type %s", mType));
                }

                if (mIcon != null || (mTitle != null && !TextUtils.isEmpty(mTitle.toString()))) {
                    throw new IllegalStateException(
                            "An icon or title can't be set on the standard back or app-icon "
                                    + "action");
                }
            }

            if (mType == TYPE_PAN) {
                if (mOnClickDelegate != null) {
                    throw new IllegalStateException(
                            "An on-click listener can't be set on the pan mode action");
                }
            }

            if (mType == TYPE_COMPOSE_MESSAGE) {
                if (mOnClickDelegate != null) {
                    throw new IllegalStateException(
                            "An on-click listener can't be set on the compose action");
                }

                if (mTitle != null && !TextUtils.isEmpty(mTitle.toString())) {
                    throw new IllegalStateException(
                            "A title can't be set on the standard compose action");
                }
            }

            return new Action(this);
        }

        /** Creates an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Returns a {@link Builder} instance configured with the same data as the given
         * {@link Action} instance.
         *
         * @throws NullPointerException if {@code action} is {@code null}
         */
        @RequiresCarApi(2)
        public Builder(@NonNull Action action) {
            requireNonNull(action);
            mType = action.getType();
            mIcon = action.getIcon();
            mTitle = action.getTitle();
            mOnClickDelegate = action.getOnClickDelegate();
            CarColor backgroundColor = action.getBackgroundColor();
            mBackgroundColor = backgroundColor == null ? DEFAULT : backgroundColor;
            mFlags = action.getFlags();
            mIsEnabled = action.isEnabled();
        }
    }
}
