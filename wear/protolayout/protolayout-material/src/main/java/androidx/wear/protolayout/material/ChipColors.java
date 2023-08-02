/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.protolayout.material;

import static androidx.wear.protolayout.ColorBuilders.argb;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.wear.protolayout.ColorBuilders.ColorProp;

/**
 * Represents the background and content colors used in {@link Chip}.
 *
 * <p>See {@link ChipDefaults#PRIMARY_COLORS} for the default colors used in a primary styled {@link
 * Chip}. See {@link ChipDefaults#SECONDARY_COLORS} for the default colors used in a secondary
 * styled {@link Chip}.
 */
public class ChipColors {
    @NonNull private final ColorProp mBackgroundColor;
    @NonNull private final ColorProp mIconColor;
    @NonNull private final ColorProp mContentColor;
    @NonNull private final ColorProp mSecondaryContentColor;

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for {@link Chip}. Should be in ARGB
     *     format.
     * @param iconColor The color to be used for an icon in {@link Chip}. Should be in ARGB format.
     * @param contentColor The text color to be used for a main text in {@link Chip}. Should be in
     *     ARGB format.
     * @param secondaryContentColor The text color to be used for a label text in {@link Chip}
     *     Should be in ARGB format.
     */
    public ChipColors(
            @ColorInt int backgroundColor,
            @ColorInt int iconColor,
            @ColorInt int contentColor,
            @ColorInt int secondaryContentColor) {
        mBackgroundColor = argb(backgroundColor);
        mIconColor = argb(iconColor);
        mContentColor = argb(contentColor);
        mSecondaryContentColor = argb(secondaryContentColor);
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for {@link Chip}. Should be in ARGB
     *     format.
     * @param contentColor The content color to be used for all items inside {@link Chip} Should be
     *     in ARGB format.
     */
    public ChipColors(@ColorInt int backgroundColor, @ColorInt int contentColor) {
        mBackgroundColor = argb(backgroundColor);
        mIconColor = argb(contentColor);
        mContentColor = argb(contentColor);
        mSecondaryContentColor = argb(contentColor);
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for {@link Chip}.
     * @param iconColor The color to be used for an icon in {@link Chip}.
     * @param contentColor The text color to be used for a main text in {@link Chip}.
     * @param secondaryContentColor The text color to be used for a label text in {@link Chip}.
     */
    public ChipColors(
            @NonNull ColorProp backgroundColor,
            @NonNull ColorProp iconColor,
            @NonNull ColorProp contentColor,
            @NonNull ColorProp secondaryContentColor) {
        mBackgroundColor = backgroundColor;
        mIconColor = iconColor;
        mContentColor = contentColor;
        mSecondaryContentColor = secondaryContentColor;
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for {@link Chip}.
     * @param contentColor The content color to be used for all items inside {@link Chip}.
     */
    public ChipColors(@NonNull ColorProp backgroundColor, @NonNull ColorProp contentColor) {
        mBackgroundColor = backgroundColor;
        mIconColor = contentColor;
        mContentColor = contentColor;
        mSecondaryContentColor = contentColor;
    }

    /**
     * Returns a {@link ChipColors} object, using the current Primary colors from the given {@link
     * Colors}.
     */
    @NonNull
    public static ChipColors primaryChipColors(@NonNull Colors colors) {
        return new ChipColors(colors.getPrimary(), colors.getOnPrimary());
    }

    /**
     * Returns a {@link ChipColors} object, using the current Surface colors from the given {@link
     * Colors}.
     */
    @NonNull
    public static ChipColors secondaryChipColors(@NonNull Colors colors) {
        return new ChipColors(colors.getSurface(), colors.getOnSurface());
    }

    /** The background color to be used on {@link Chip}. */
    @NonNull
    public ColorProp getBackgroundColor() {
        return mBackgroundColor;
    }

    /** The icon color to be used on {@link Chip}. */
    @NonNull
    public ColorProp getIconColor() {
        return mIconColor;
    }

    /** The main text color to be used on {@link Chip}. */
    @NonNull
    public ColorProp getContentColor() {
        return mContentColor;
    }

    /** The label text color to be used on {@link Chip}. */
    @NonNull
    public ColorProp getSecondaryContentColor() {
        return mSecondaryContentColor;
    }
}
