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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.graphics.PorterDuff.Mode;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarColorConstraints;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents an icon to be used in a car app.
 *
 * <p>Car icons wrap a backing {@link IconCompat}, and add additional attributes optimized for the
 * car such as a {@link CarColor} tint.
 *
 * <h4>Car Screen Pixel Densities</h4>
 *
 * <p>Similar to Android devices, car screens cover a wide range of sizes and densities. To ensure
 * that icons and images render well across all car screens, use vector assets whenever possible to
 * avoid scaling issues. If your app relies on bitmaps or other non-vector assets, you should ensure
 * that you have resources that address multiple pixel density buckets using configuration
 * qualifiers in your resource folders (e.g. "mdpi", "hdpi", etc). See {@link
 * androidx.car.app.CarContext} for more details.
 *
 * <h4>Themed Drawables</h4>
 *
 * Vector drawables can contain references to attributes declared in a theme. For example:
 *
 * <pre>{@code
 * <vector ...
 *   <path
 *     android:pathData="..."
 *     android:fillColor="?myIconColor"/>
 * </vector>
 * }</pre>
 *
 * The theme must be defined in the app's manifest metadata, by declaring them in a theme and
 * referencing it from the <code>androidx.car.app.theme</code> metadata.
 *
 * <p>In <code>AndroidManifest.xml</code>, under the <code>application</code> element corresponding
 * to the car app:
 *
 * <pre>{@code
 * <meta-data
 *   android:name="androidx.car.app.theme"
 *   android:resource="@style/CarAppTheme"/>
 * }</pre>
 *
 * The <code>CarAppTheme</code> style is defined as any other themes in a resource file:
 *
 * <pre>{@code
 * <resources>
 *   <style name="CarAppTheme">
 *     <item name="myIconColor">@color/my_icon_color</item>
 *     ...
 *   </style>
 * </resources>
 * }</pre>
 */
@CarProtocol
@KeepFields
public final class CarIcon {
    /** Matches with {@link android.graphics.drawable.Icon#TYPE_RESOURCE} */
    private static final int TYPE_RESOURCE = 2;

    /** Matches with {@link android.graphics.drawable.Icon#TYPE_URI} */
    private static final int TYPE_URI = 4;

    /** The type of car icon represented by the {@link CarIcon} instance. */
    @RestrictTo(LIBRARY)
    @SuppressLint("UniqueConstants") // TYPE_APP will be removed in a follow-up change.
    @IntDef(
            value = {
                TYPE_CUSTOM,
                TYPE_BACK,
                TYPE_ALERT,
                TYPE_APP_ICON,
                TYPE_ERROR,
                TYPE_PAN,
                TYPE_COMPOSE_MESSAGE,
                TYPE_MEDIA_PLAYBACK,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CarIconType {}

    /** A custom, non-standard, app-defined icon. */
    public static final int TYPE_CUSTOM = 1;

    /**
     * An icon representing a "back" action.
     *
     * @see #BACK
     */
    public static final int TYPE_BACK = 3;

    /**
     * An alert icon.
     *
     * @see #ALERT
     */
    public static final int TYPE_ALERT = 4;

    /**
     * The app's icon.
     *
     * @see #APP_ICON
     */
    public static final int TYPE_APP_ICON = 5;

    /**
     * An error icon.
     *
     * @see #ERROR
     */
    public static final int TYPE_ERROR = 6;

    /**
     * A pan icon.
     *
     * @see #PAN
     */
    public static final int TYPE_PAN = 7;

    /**
     * A message icon.
     *
     * @see #COMPOSE_MESSAGE
     */
    public static final int TYPE_COMPOSE_MESSAGE = 8;

    /**
     * A media playback icon.
     *
     * @see #MEDIA_PLAYBACK
     */
    public static final int TYPE_MEDIA_PLAYBACK = 9;

    /**
     * Represents the app's icon, as defined in the app's manifest by the {@code android:icon}
     * attribute of the {@code application} element.
     */
    public static final @NonNull CarIcon APP_ICON = CarIcon.forStandardType(TYPE_APP_ICON);

    /** An icon representing a "back" action. */
    public static final @NonNull CarIcon BACK = CarIcon.forStandardType(TYPE_BACK);

    /** An icon representing an alert. */
    public static final @NonNull CarIcon ALERT = CarIcon.forStandardType(TYPE_ALERT);

    /** An icon representing an error. */
    public static final @NonNull CarIcon ERROR = CarIcon.forStandardType(TYPE_ERROR);

    /** An icon representing a pan action (for example, in a map surface). */
    @RequiresCarApi(2)
    public static final @NonNull CarIcon PAN = CarIcon.forStandardType(TYPE_PAN);

    /** An icon that represents the user's intent to send a message. */
    @RequiresCarApi(7)
    public static final @NonNull CarIcon COMPOSE_MESSAGE =
            CarIcon.forStandardType(TYPE_COMPOSE_MESSAGE);

    /**
     * An icon that represents a playable media item. Note: This is specifically used for category
     * MEDIA apps. Used in conjunction with {@link Action#MEDIA_PLAYBACK}
     */
    @RequiresCarApi(8)
    public static final @NonNull CarIcon MEDIA_PLAYBACK =
            CarIcon.forStandardType(TYPE_MEDIA_PLAYBACK);

    @CarIconType private final int mType;
    @Nullable private final IconCompat mIcon;

    /**
     * @deprecated Moved to {@link CarIconStyle#getTint()}. Field retained for backwards
     *     compatibility with legacy hosts.
     */
    @Deprecated @Nullable private final CarColor mTint;

    @Nullable private final CarIconStyle mStyle;

    CarIcon(@Nullable IconCompat icon, @Nullable CarColor tint, @CarIconType int type) {
        mType = type;
        mIcon = icon;
        mTint = tint;
        if (tint != null) {
            mStyle = new CarIconStyle.Builder(CarIconStyle.TINTED).setTint(tint).build();
        } else {
            mStyle = null;
        }
    }

    CarIcon(Builder builder) {
        mType = builder.mType;
        mIcon = builder.mIcon;
        mStyle = builder.mStyle;

        if (mStyle != null) {
            mTint = mStyle.getTint();
        } else {
            mTint = null;
        }
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarIcon() {
        mType = TYPE_CUSTOM;
        mIcon = null;
        mTint = null;
        mStyle = null;
    }

    /**
     * Creates a tinted {@link CarIcon}.
     *
     * <p>By default, using this builder allows the host vehicle system to automatically tint the
     * icon based on the active template theme. To apply a custom tint instead, use {@link
     * Builder#Builder(IconCompat, CarIconStyle)} or {@link Builder#setStyle(CarIconStyle)} with
     * {@link CarIconStyle.Builder#setTint(CarColor)}.
     *
     * @param icon The base icon graphic.
     * @return A styled {@link CarIcon} instance configured with {@link CarIconStyle#TINTED}.
     */
    @NonNull
    public static CarIcon createTintedIcon(@NonNull IconCompat icon) {
        return new Builder(icon, CarIconStyle.TINTED).build();
    }

    /**
     * Creates an icon that retains its original colors without allowing any host-side tinting on
     * it.
     *
     * @param icon The base icon graphic.
     * @return A styled {@link CarIcon} instance configured with {@link CarIconStyle#ORIGINAL}.
     */
    @NonNull
    public static CarIcon createOriginalIcon(@NonNull IconCompat icon) {
        return new Builder(icon, CarIconStyle.ORIGINAL).build();
    }

    /**
     * Returns the {@link IconCompat} instance backing by this car icon or {@code null} if one isn't
     * set.
     *
     * @see Builder#Builder(IconCompat)
     */
    public @Nullable IconCompat getIcon() {
        return mIcon;
    }

    /**
     * Returns the tint of the icon or {@code null} if not set.
     *
     * @see Builder#setTint(CarColor)
     * @deprecated use {@link CarIconStyle#getTint} instead
     */
    @Deprecated
    public @Nullable CarColor getTint() {
        if (mStyle == null) {
            return mTint;
        }
        return mStyle.getTint();
    }

    /**
     * Returns the style of the icon or {@code null} if not set.
     *
     * @see Builder#setStyle(CarIconStyle)
     */
    public @Nullable CarIconStyle getStyle() {
        // If style is present return it
        if (mStyle != null) {
            return mStyle;
        }

        // If style is null, but tint is provided, we return the style with according tint
        if (mTint != null) {
            return new CarIconStyle.Builder(CarIconStyle.TINTED).setTint(mTint).build();
        }

        return null;
    }

    /** Returns the type of car icon for this instance. */
    @CarIconType
    public int getType() {
        return mType;
    }

    @Override
    public @NonNull String toString() {
        return "[type: " + typeToString(mType) + ", tint: " + mTint + ", style: " + mStyle + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mTint, iconCompatHash(), mStyle);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarIcon)) {
            return false;
        }
        CarIcon otherIcon = (CarIcon) other;

        return mType == otherIcon.mType
                && Objects.equals(mTint, otherIcon.mTint)
                && iconCompatEquals(otherIcon.mIcon)
                && Objects.equals(mStyle, otherIcon.mStyle);
    }

    private @Nullable Object iconCompatHash() {
        // Use the same things being compared in iconCompatEquals for hashing.
        if (mIcon == null) {
            return null;
        }

        int type = mIcon.getType();
        if (type == TYPE_RESOURCE) {
            return mIcon.getResPackage() + mIcon.getResId();
        } else if (type == TYPE_URI) {
            return mIcon.getUri();
        }

        return type;
    }

    private boolean iconCompatEquals(@Nullable IconCompat other) {
        if (mIcon == null) {
            return other == null;
        } else if (other == null) {
            return false;
        }

        int type = mIcon.getType();
        int otherType = other.getType();

        if (type != otherType) {
            return false;
        }

        // TODO(b/146175636): Decide how/if we will diff bitmap type IconCompat
        if (type == TYPE_RESOURCE) {
            return Objects.equals(mIcon.getResPackage(), other.getResPackage())
                    && mIcon.getResId() == other.getResId();
        } else if (type == TYPE_URI) {
            return Objects.equals(mIcon.getUri(), other.getUri());
        }

        // Since we support any icon types, we only check for type equality if the type is
        // neither a resource or uri.
        return true;
    }

    private static CarIcon forStandardType(@CarIconType int type) {
        return new CarIcon(null, DEFAULT, type);
    }

    private static String typeToString(@CarIconType int type) {
        switch (type) {
            case TYPE_ALERT:
                return "ALERT";
            case TYPE_APP_ICON:
                return "APP";
            case TYPE_ERROR:
                return "ERROR";
            case TYPE_BACK:
                return "BACK";
            case TYPE_PAN:
                return "PAN";
            case TYPE_COMPOSE_MESSAGE:
                return "COMPOSE_MESSAGE";
            case TYPE_MEDIA_PLAYBACK:
                return "MEDIA_PLAYBACK";
            case TYPE_CUSTOM:
                return "CUSTOM";
            default:
                return "<unknown>";
        }
    }

    /** A builder of {@link CarIcon}. */
    public static final class Builder {
        private final @Nullable IconCompat mIcon;
        @CarIconType private final int mType;
        private @Nullable CarIconStyle mStyle;

        /**
         * Creates a {@link Builder} instance using the given {@link IconCompat}.
         *
         * <p>The following types are supported:
         *
         * <ul>
         *   <li>{@link IconCompat#TYPE_BITMAP}
         *   <li>{@link IconCompat#TYPE_RESOURCE}
         *   <li>{@link IconCompat#TYPE_URI}
         * </ul>
         *
         * <p>{@link IconCompat#TYPE_URI} is only supported in templates that explicitly allow it.
         * In those cases, the appropriate APIs will be documented to indicate this.
         *
         * <p>For {@link IconCompat#TYPE_URI}, the URI's scheme must be {@link
         * ContentResolver#SCHEME_CONTENT}.
         *
         * <p>If the icon image is loaded from URI, it may be cached on the host side. Changing the
         * contents of the URI will result in the host showing a stale image.
         *
         * <p><b>Note:</b> It is recommended to use {@link CarIcon#createTintedIcon(IconCompat)},
         * {@link CarIcon#createOriginalIcon(IconCompat)}, or {@link #Builder(IconCompat,
         * CarIconStyle)} to explicitly specify visual styling behavior. This constructor will be
         * deprecated in a future release.
         *
         * @throws IllegalArgumentException if {@code icon}'s URI scheme is not supported
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        public Builder(@NonNull IconCompat icon) {
            CarIconConstraints.UNCONSTRAINED.checkSupportedIcon(requireNonNull(icon));
            mType = TYPE_CUSTOM;
            mIcon = icon;
            mStyle = null;
        }

        /**
         * Creates a {@link Builder} instance using the given {@link IconCompat} and explicit {@link
         * CarIconStyle}.
         *
         * <p>The following types are supported:
         *
         * <ul>
         *   <li>{@link IconCompat#TYPE_BITMAP}
         *   <li>{@link IconCompat#TYPE_RESOURCE}
         *   <li>{@link IconCompat#TYPE_URI}
         * </ul>
         *
         * <p>{@link IconCompat#TYPE_URI} is only supported in templates that explicitly allow it.
         * In those cases, the appropriate APIs will be documented to indicate this.
         *
         * <p>For {@link IconCompat#TYPE_URI}, the URI's scheme must be {@link
         * ContentResolver#SCHEME_CONTENT}.
         *
         * <p>If the icon image is loaded from URI, it may be cached on the host side. Changing the
         * contents of the URI will result in the host showing a stale image.
         *
         * <p>Select the appropriate style depending on the asset:
         *
         * <ul>
         *   <li>Use {@link CarIconStyle#TINTED} for icons that should be automatically tinted by
         *       the vehicle theme or assigned a custom tint via {@link
         *       CarIconStyle.Builder#setTint(CarColor)}.
         *   <li>Use {@link CarIconStyle#ORIGINAL} for icons that must retain their original colors
         *       without host-side tinting (e.g., user avatars, media album art, photos, or
         *       un-tinted logos).
         * </ul>
         *
         * @param icon The base icon or image graphic.
         * @param style The explicit style contract dictating tinting behavior and optional
         *     geometric shaping.
         * @throws IllegalArgumentException if {@code icon}'s URI scheme is not supported
         * @throws NullPointerException if {@code icon} or {@code style} is {@code null}
         */
        public Builder(@NonNull IconCompat icon, @NonNull CarIconStyle style) {
            CarIconConstraints.UNCONSTRAINED.checkSupportedIcon(requireNonNull(icon));
            this.mType = TYPE_CUSTOM;
            this.mIcon = icon;
            this.mStyle = requireNonNull(style);
        }

        /**
         * Returns a {@link Builder} instance configured with the same data as the given {@link
         * CarIcon} instance.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        public Builder(@NonNull CarIcon carIcon) {
            requireNonNull(carIcon);
            mType = carIcon.getType();
            mIcon = carIcon.getIcon();
            mStyle = carIcon.getStyle();

            if (carIcon.getStyle() == null && carIcon.getTint() != null) {
                mStyle =
                        new CarIconStyle.Builder(CarIconStyle.TINTED)
                                .setTint(carIcon.getTint())
                                .build();
            }
        }

        /**
         * Sets the tint of the icon to the given {@link CarColor}.
         *
         * <p>This tint overrides the tint set through {@link IconCompat#setTint(int)} in the
         * backing {@link IconCompat} with a {@link CarColor} tint. The tint set through {@link
         * IconCompat#setTint(int)} is not guaranteed to be applied if the {@link CarIcon} tint is
         * not set.
         *
         * <p>The tint mode used to blend this color is {@link Mode#SRC_IN}.
         *
         * <p>Depending on contrast requirements, capabilities of the vehicle screens, or other
         * factors, the color may be ignored by the host or overridden by the vehicle system.
         *
         * @throws NullPointerException if {@code tint} is {@code null}
         * @see CarColor
         * @see android.graphics.drawable.Drawable#setTintMode(Mode)
         * @deprecated Use {@link CarIcon.Builder#setStyle(CarIconStyle)} and provide a tint via
         *     {@link CarIconStyle.Builder#setTint(CarColor)}, or use {@link
         *     CarIcon#createTintedIcon(IconCompat)} instead.
         */
        @Deprecated
        public @NonNull Builder setTint(@NonNull CarColor tint) {
            CarColorConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(tint));

            // Set the style property
            if (mStyle == null) {
                mStyle = new CarIconStyle.Builder(CarIconStyle.TINTED).setTint(tint).build();
            } else {
                mStyle = new CarIconStyle.Builder(mStyle).setTint(tint).build();
            }

            return this;
        }

        /**
         * Sets the style of the icon to the given {@link CarIconStyle}.
         *
         * <p>Select the appropriate style depending on the asset:
         *
         * <ul>
         *   <li>Use {@link CarIconStyle#TINTED} for icons that should be automatically tinted by
         *       the vehicle theme or assigned a custom tint via {@link
         *       CarIconStyle.Builder#setTint(CarColor)}.
         *   <li>Use {@link CarIconStyle#ORIGINAL} for icons that must retain their original colors
         *       without host-side tinting (e.g., user avatars, media album art, photos, or
         *       un-tinted logos).
         * </ul>
         *
         * @param style The explicit style contract dictating tinting behavior and optional
         *     geometric shaping.
         */
        @NonNull
        public Builder setStyle(@NonNull CarIconStyle style) {
            this.mStyle = requireNonNull(style);
            return this;
        }

        /** Constructs the {@link CarIcon} defined by this builder. */
        @NonNull
        public CarIcon build() {
            return new CarIcon(this);
        }
    }
}
