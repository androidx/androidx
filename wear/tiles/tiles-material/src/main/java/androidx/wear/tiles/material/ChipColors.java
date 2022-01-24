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

import static androidx.wear.tiles.ColorBuilders.argb;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.wear.tiles.ColorBuilders.ColorProp;

/**
 * Represents the background and content colors used in a chip Tiles component.
 *
 * <p>See {@link ChipDefaults#PRIMARY} for the default colors used in a primary styled {@link Chip}.
 * See {@link ChipDefaults#SECONDARY} for the default colors used in a secondary styled {@link
 * Chip}.
 */
public class ChipColors {
    @NonNull private final ColorProp mBackgroundColor;
    @NonNull private final ColorProp mIconTintColor;
    @NonNull private final ColorProp mContentColor;
    @NonNull private final ColorProp mSecondaryContentColor;

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for a chip Tiles component. Should be
     *     in ARGB format.
     * @param iconTintColor The tint color to be used for an icon in a chip Tiles component. Should
     *     be in ARGB format.
     * @param contentColor The text color to be used for a main text in a chip Tiles component.
     *     Should be in ARGB format.
     * @param secondaryContentColor The text color to be used for a label text in a chip Tiles
     *     component. Should be in ARGB format.
     */
    public ChipColors(
            @ColorInt int backgroundColor,
            @ColorInt int iconTintColor,
            @ColorInt int contentColor,
            @ColorInt int secondaryContentColor) {
        mBackgroundColor = argb(backgroundColor);
        mIconTintColor = argb(iconTintColor);
        mContentColor = argb(contentColor);
        mSecondaryContentColor = argb(secondaryContentColor);
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
        mBackgroundColor = argb(backgroundColor);
        mIconTintColor = argb(contentColor);
        mContentColor = argb(contentColor);
        mSecondaryContentColor = argb(contentColor);
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for a chip Tiles component.
     * @param iconTintColor The tint color to be used for an icon in a chip Tiles component.
     * @param contentColor The text color to be used for a main text in a chip Tiles component.
     * @param secondaryContentColor The text color to be used for a label text in a chip Tiles
     *     component.
     */
    public ChipColors(
            @NonNull ColorProp backgroundColor,
            @NonNull ColorProp iconTintColor,
            @NonNull ColorProp contentColor,
            @NonNull ColorProp secondaryContentColor) {
        mBackgroundColor = backgroundColor;
        mIconTintColor = iconTintColor;
        mContentColor = contentColor;
        mSecondaryContentColor = secondaryContentColor;
    }

    /**
     * Constructor for the {@link ChipColors} object.
     *
     * @param backgroundColor The background color to be used for a chip Tiles component.
     * @param contentColor The content color to be used for all items inside a chip Tiles component.
     */
    public ChipColors(@NonNull ColorProp backgroundColor, @NonNull ColorProp contentColor) {
        mBackgroundColor = backgroundColor;
        mIconTintColor = contentColor;
        mContentColor = contentColor;
        mSecondaryContentColor = contentColor;
    }

    /** The background color to be used on a chip Tiles components. */
    @NonNull
    public ColorProp getBackgroundColor() {
        return mBackgroundColor;
    }

    /** The icon tint color to be used on a chip Tiles components. */
    @NonNull
    public ColorProp getIconTintColor() {
        return mIconTintColor;
    }

    /** The main text color to be used on a chip Tiles components. */
    @NonNull
    public ColorProp getContentColor() {
        return mContentColor;
    }

    /** The label text color to be used on a chip Tiles components. */
    @NonNull
    public ColorProp getSecondaryContentColor() {
        return mSecondaryContentColor;
    }
}
