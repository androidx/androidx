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
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarColorConstraints;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.core.graphics.drawable.IconCompat;

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
 * <p>Similar to Android devices, car screens cover a wide range of pixel densities. To ensure that
 * icons and images render well across all car screens, use vector assets whenever possible to avoid
 * scaling issues. If you use a bitmap instead, ensure that you have resources that address multiple
 * pixel density buckets.
 *
 * <p>In order to support all car screen sizes and pixel density, you can use configuration
 * qualifiers in your resource files (e.g. "mdpi", "hdpi", etc). See
 * {@link androidx.car.app.CarContext} for more details.
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
public final class CarIcon {
    /** Matches with {@link android.graphics.drawable.Icon#TYPE_RESOURCE} */
    private static final int TYPE_RESOURCE = 2;

    /** Matches with {@link android.graphics.drawable.Icon#TYPE_URI} */
    private static final int TYPE_URI = 4;

    /**
     * The type of car icon represented by the {@link CarIcon} instance.
     *
     * @hide
     */
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
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CarIconType {
    }

    /**
     * A custom, non-standard, app-defined icon.
     */
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
     * Represents the app's icon, as defined in the app's manifest by the {@code android:icon}
     * attribute of the {@code application} element.
     */
    @NonNull
    public static final CarIcon APP_ICON = CarIcon.forStandardType(TYPE_APP_ICON);

    /**
     * An icon representing a "back" action.
     */
    @NonNull
    public static final CarIcon BACK = CarIcon.forStandardType(TYPE_BACK);

    /**
     * An icon representing an alert.
     */
    @NonNull
    public static final CarIcon ALERT = CarIcon.forStandardType(TYPE_ALERT);

    /**
     * An icon representing an error.
     */
    @NonNull
    public static final CarIcon ERROR = CarIcon.forStandardType(TYPE_ERROR);

    /**
     * An icon representing a pan action (for example, in a map surface).
     */
    @RequiresCarApi(2)
    @NonNull
    public static final CarIcon PAN = CarIcon.forStandardType(TYPE_PAN);

    @Keep
    @CarIconType
    private final int mType;
    @Keep
    @Nullable
    private final IconCompat mIcon;
    @Keep
    @Nullable
    private final CarColor mTint;

    /**
     * Returns the {@link IconCompat} instance backing by this car icon or {@code null} if one
     * isn't set.
     *
     * @see Builder#Builder(IconCompat)
     */
    @Nullable
    public IconCompat getIcon() {
        return mIcon;
    }

    /**
     * Returns the tint of the icon or {@code null} if not set.
     *
     * @see Builder#setTint(CarColor)
     */
    @Nullable
    public CarColor getTint() {
        return mTint;
    }

    /**
     * Returns the type of car icon for this instance.
     */
    @CarIconType
    public int getType() {
        return mType;
    }

    @Override
    public String toString() {
        return "[type: " + typeToString(mType) + ", tint: " + mTint + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mTint, iconCompatHash());
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
                && iconCompatEquals(otherIcon.mIcon);
    }

    @Nullable
    private Object iconCompatHash() {
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
        return forStandardType(type, DEFAULT);
    }

    private static CarIcon forStandardType(@CarIconType int type, @Nullable CarColor tint) {
        return new CarIcon(null, tint, type);
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
            case TYPE_CUSTOM:
                return "CUSTOM";
            default:
                return "<unknown>";
        }
    }

    CarIcon(@Nullable IconCompat icon, @Nullable CarColor tint, @CarIconType int type) {
        mType = type;
        mIcon = icon;
        mTint = tint;
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarIcon() {
        mType = TYPE_CUSTOM;
        mIcon = null;
        mTint = null;
    }

    /** A builder of {@link CarIcon}. */
    public static final class Builder {
        @Nullable
        private IconCompat mIcon;
        @Nullable
        private CarColor mTint;
        @CarIconType
        private int mType;

        /**
         * Sets the tint of the icon to the given {@link CarColor}.
         *
         * <p>This tint overrides the tint set through {@link IconCompat#setTint(int)} in the
         * backing {@link IconCompat} with a {@link CarColor} tint. The tint set through {@link
         * IconCompat#setTint(int)} is not guaranteed to be applied if the {@link CarIcon} tint
         * is not set.
         *
         * <p>The tint mode used to blend this color is {@link Mode#SRC_IN}.
         *
         * <p>Depending on contrast requirements, capabilities of the vehicle screens, or other
         *  factors, the color may be ignored by the host or overridden by the vehicle system.
         *
         * @throws NullPointerException if {@code tin} is {@code null}
         * @see CarColor
         * @see android.graphics.drawable.Drawable#setTintMode(Mode)
         */
        @NonNull
        public Builder setTint(@NonNull CarColor tint) {
            CarColorConstraints.UNCONSTRAINED.validateOrThrow(requireNonNull(tint));
            mTint = tint;
            return this;
        }

        /** Constructs the {@link CarIcon} defined by this builder. */
        @NonNull
        public CarIcon build() {
            return new CarIcon(mIcon, mTint, mType);
        }

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
         * @throws IllegalArgumentException if {@code icon}'s URI scheme is not supported
         * @throws NullPointerException     if {@code icon} is {@code null}
         */
        public Builder(@NonNull IconCompat icon) {
            CarIconConstraints.UNCONSTRAINED.checkSupportedIcon(requireNonNull(icon));
            mType = TYPE_CUSTOM;
            mIcon = icon;
            mTint = null;
        }

        /**
         * Returns a {@link Builder} instance configured with the same data as the given
         * {@link CarIcon} instance.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        public Builder(@NonNull CarIcon carIcon) {
            requireNonNull(carIcon);
            mType = carIcon.getType();
            mIcon = carIcon.getIcon();
            mTint = carIcon.getTint();
        }
    }
}
