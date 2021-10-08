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

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Represents a color to be used in a car app.
 *
 * <p>The host chooses the dark or light variant of the color when displaying the user interface,
 * depending where the color is used, to ensure the proper contrast ratio is maintained. For
 * example, the dark variant when may be used as the background of a view with brighter text on it,
 * and the light variant for text on a dark background.
 *
 * <p>Colors provided by the app should meet the contrast requirements defined by the host, and
 * documented by the app quality guidelines.
 *
 * <h4>Standard colors</h4>
 *
 * A set of standard {@link CarColor} instances (for example, {@link #BLUE}) is available in this
 * class. It is recommended to use these standard colors whenever possible as they are guaranteed to
 * adhere to the contrast requirements.
 *
 * <h4>Primary and secondary colors</h4>
 *
 * The app can define two additional {@link CarColor}s in its manifest metadata, through the <code>
 * carColorPrimary</code>, <code>carColorPrimaryDark</code>, <code>
 * carColorSecondary</code>, and <code>carColorSecondaryDark</code> theme attributes, by declaring
 * them in a theme and referencing the theme from the <code>
 * androidx.car.app.theme</code> metadata. Both the light and dark variants must
 * be declared for the primary and secondary colors, otherwise default variants will be used.
 * Wherever primary and secondary colors are used by the app, the host may use a default color
 * instead if the colors do not pass the contrast requirements.
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
 *     <item name="carColorPrimary">@color/my_primary_car_color</item>
 *     <item name="carColorPrimaryDark">@color/my_primary_dark_car_color</item>
 *     <item name="carColorSecondary">@color/my_secondary_car_color</item>
 *     <item name="carColorSecondaryDark">@color/my_secondary_cark_car_color</item>
 *   </style>
 * </resources>
 * }</pre>
 *
 * <h4>Custom Colors</h4>
 *
 * Besides the primary and secondary colors, custom colors can be created at runtime with {@link
 * #createCustom}. Wherever custom colors are used by the app, the host may use a default color
 * instead if the custom color does not pass the contrast requirements.
 */
@CarProtocol
public final class CarColor {
    /**
     * The type of color represented by the {@link CarColor} instance.
     *
     * @hide
     */
    @IntDef(
            value = {
                    TYPE_CUSTOM,
                    TYPE_DEFAULT,
                    TYPE_PRIMARY,
                    TYPE_SECONDARY,
                    TYPE_RED,
                    TYPE_GREEN,
                    TYPE_BLUE,
                    TYPE_YELLOW
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface CarColorType {
    }

    /**
     * A custom, non-standard, app-defined color.
     */
    @CarColorType
    public static final int TYPE_CUSTOM = 0;

    /**
     * A default color, chosen by the host.
     *
     * @see #DEFAULT
     */
    @CarColorType
    public static final int TYPE_DEFAULT = 1;

    /**
     * The primary app color.
     *
     * @see #PRIMARY
     */
    @CarColorType
    public static final int TYPE_PRIMARY = 2;

    /**
     * The secondary app color.
     *
     * @see #SECONDARY
     */
    @CarColorType
    public static final int TYPE_SECONDARY = 3;

    /**
     * The standard red color.
     *
     * @see #RED
     */
    @CarColorType
    public static final int TYPE_RED = 4;

    /**
     * The standard green color.
     *
     * @see #GREEN
     */
    @CarColorType
    public static final int TYPE_GREEN = 5;

    /**
     * The standard blue color.
     *
     * @see #BLUE
     */
    @CarColorType
    public static final int TYPE_BLUE = 6;

    /**
     * The standard yellow color.
     *
     * @see #YELLOW
     */
    @CarColorType
    public static final int TYPE_YELLOW = 7;

    /**
     * Indicates that a default color should be used.
     *
     * <p>This can be used for example to tell the host that the app has no preference for the
     * tint of an icon, and it should use whatever default it finds appropriate.
     */
    @NonNull
    public static final CarColor DEFAULT = create(TYPE_DEFAULT);

    /**
     * Indicates that the app primary color and its dark version should be used, as declared in the
     * app manifest through the {@code carColorPrimary} and {@code carColorPrimaryDark}
     * theme attributes.
     */
    @NonNull
    public static final CarColor PRIMARY = create(TYPE_PRIMARY);

    /**
     * Indicates that the app secondary color and its dark version should be used, as declared in
     * the app manifest through the <code>carColorSecondary</code> and {@code
     * carColorSecondaryDark} theme attributes.
     */
    @NonNull
    public static final CarColor SECONDARY = create(TYPE_SECONDARY);

    /** A standard red color. */
    @NonNull
    public static final CarColor RED = create(TYPE_RED);

    /** A standard green color. */
    @NonNull
    public static final CarColor GREEN = create(TYPE_GREEN);

    /** A standard blue color. */
    @NonNull
    public static final CarColor BLUE = create(TYPE_BLUE);

    /** A standard yellow color. */
    @NonNull
    public static final CarColor YELLOW = create(TYPE_YELLOW);

    @Keep
    @CarColorType
    private final int mType;

    /** A light-variant custom color-int, used when the type is {@link #TYPE_CUSTOM}. */
    @Keep
    @ColorInt
    private final int mColor;

    /** A dark-variant custom color-int, used when the type is {@link #TYPE_CUSTOM}. */
    @Keep
    @ColorInt
    private final int mColorDark;

    /**
     * Returns an instance of {@link CarColor} containing a non-standard color.
     *
     * <p>See the top-level documentation of {@link CarColor} for details about how the host
     * determines which variant is used.
     */
    @NonNull
    public static CarColor createCustom(@ColorInt int color, @ColorInt int colorDark) {
        return new CarColor(TYPE_CUSTOM, color, colorDark);
    }

    /** Returns the type of color for this instance. */
    @CarColorType
    public int getType() {
        return mType;
    }

    /**
     * Returns a packed color int for the light variant of the color, used when the type
     * is {@link #TYPE_CUSTOM}.
     */
    @ColorInt
    public int getColor() {
        return mColor;
    }

    /**
     * Returns a packed color int for the dark variant of the color, used when the type
     * is {@link #TYPE_CUSTOM}.
     */
    @ColorInt
    public int getColorDark() {
        return mColorDark;
    }

    @Override
    public String toString() {
        return "[type: " + typeToString(mType) + ", color: " + mColor + ", dark: " + mColorDark
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mColor, mColorDark);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarColor)) {
            return false;
        }
        CarColor otherColor = (CarColor) other;

        return mColor == otherColor.mColor
                && mColorDark == otherColor.mColorDark
                && mType == otherColor.mType;
    }

    private static CarColor create(@CarColorType int type) {
        return new CarColor(type, 0, 0);
    }

    private static String typeToString(@CarColorType int type) {
        switch (type) {
            case TYPE_BLUE:
                return "BLUE";
            case TYPE_DEFAULT:
                return "DEFAULT";
            case TYPE_PRIMARY:
                return "PRIMARY";
            case TYPE_SECONDARY:
                return "SECONDARY";
            case TYPE_CUSTOM:
                return "CUSTOM";
            case TYPE_GREEN:
                return "GREEN";
            case TYPE_RED:
                return "RED";
            case TYPE_YELLOW:
                return "YELLOW";
            default:
                return "<unknown>";
        }
    }

    private CarColor() {
        mType = TYPE_DEFAULT;
        mColor = 0;
        mColorDark = 0;
    }

    private CarColor(@CarColorType int type, @ColorInt int color, @ColorInt int colorDark) {
        mType = type;
        mColor = color;
        mColorDark = colorDark;
    }
}
