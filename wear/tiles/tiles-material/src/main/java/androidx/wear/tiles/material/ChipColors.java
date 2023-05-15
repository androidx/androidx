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

package androidx.wear.tiles.material;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Represents the background and content colors used in a chip Tiles component.
 *
 * <p>See {@link ChipDefaults#PRIMARY_COLORS} for the default colors used in a primary styled {@link
 * Chip}. See {@link ChipDefaults#SECONDARY_COLORS} for the default colors used in a secondary
 * styled {@link Chip}.
 *
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.ChipColors} which
 *     provides the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class ChipColors {
    @NonNull private final androidx.wear.tiles.ColorBuilders.ColorProp mBackgroundColor;
    @NonNull private final androidx.wear.tiles.ColorBuilders.ColorProp mIconColor;
    @NonNull private final androidx.wear.tiles.ColorBuilders.ColorProp mContentColor;
    @NonNull private final androidx.wear.tiles.ColorBuilders.ColorProp mSecondaryContentColor;

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for a chip Tiles component. Should be
     *     in ARGB format.
     * @param iconColor The color to be used for an icon in a chip Tiles component. Should be in
     *     ARGB format.
     * @param contentColor The text color to be used for a main text in a chip Tiles component.
     *     Should be in ARGB format.
     * @param secondaryContentColor The text color to be used for a label text in a chip Tiles
     *     component. Should be in ARGB format.
     */
    public ChipColors(
            @ColorInt int backgroundColor,
            @ColorInt int iconColor,
            @ColorInt int contentColor,
            @ColorInt int secondaryContentColor) {
        mBackgroundColor = androidx.wear.tiles.ColorBuilders.argb(backgroundColor);
        mIconColor = androidx.wear.tiles.ColorBuilders.argb(iconColor);
        mContentColor = androidx.wear.tiles.ColorBuilders.argb(contentColor);
        mSecondaryContentColor = androidx.wear.tiles.ColorBuilders.argb(secondaryContentColor);
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for a chip Tiles component. Should be
     *     in ARGB format.
     * @param contentColor The content color to be used for all items inside a chip Tiles component.
     *     Should be in ARGB format.
     */
    public ChipColors(@ColorInt int backgroundColor, @ColorInt int contentColor) {
        mBackgroundColor = androidx.wear.tiles.ColorBuilders.argb(backgroundColor);
        mIconColor = androidx.wear.tiles.ColorBuilders.argb(contentColor);
        mContentColor = androidx.wear.tiles.ColorBuilders.argb(contentColor);
        mSecondaryContentColor = androidx.wear.tiles.ColorBuilders.argb(contentColor);
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for a chip Tiles component.
     * @param iconColor The color to be used for an icon in a chip Tiles component.
     * @param contentColor The text color to be used for a main text in a chip Tiles component.
     * @param secondaryContentColor The text color to be used for a label text in a chip Tiles
     *     component.
     */
    public ChipColors(
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp backgroundColor,
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp iconColor,
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp contentColor,
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp secondaryContentColor) {
        mBackgroundColor = backgroundColor;
        mIconColor = iconColor;
        mContentColor = contentColor;
        mSecondaryContentColor = secondaryContentColor;
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for a chip Tiles component.
     * @param contentColor The content color to be used for all items inside a chip Tiles component.
     */
    public ChipColors(
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp backgroundColor,
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp contentColor) {
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

    /** The background color to be used on a chip Tiles components. */
    @NonNull
    public androidx.wear.tiles.ColorBuilders.ColorProp getBackgroundColor() {
        return mBackgroundColor;
    }

    /** The icon color to be used on a chip Tiles components. */
    @NonNull
    public androidx.wear.tiles.ColorBuilders.ColorProp getIconColor() {
        return mIconColor;
    }

    /** The main text color to be used on a chip Tiles components. */
    @NonNull
    public androidx.wear.tiles.ColorBuilders.ColorProp getContentColor() {
        return mContentColor;
    }

    /** The label text color to be used on a chip Tiles components. */
    @NonNull
    public androidx.wear.tiles.ColorBuilders.ColorProp getSecondaryContentColor() {
        return mSecondaryContentColor;
    }
}
