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
 * Represents the background and content colors used in a button Tiles component.
 *
 * <p>See {@link ButtonDefaults#PRIMARY_COLORS} for the default colors used in a primary styled
 * {@link Button}. See {@link ButtonDefaults#SECONDARY_COLORS} for the default colors used in a
 * secondary styled {@link Button}.
 *
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.ButtonColors} which
 *     provides the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class ButtonColors {
    @NonNull private final androidx.wear.tiles.ColorBuilders.ColorProp mBackgroundColor;
    @NonNull private final androidx.wear.tiles.ColorBuilders.ColorProp mContentColor;

    /**
     * Constructor for {@link ButtonColors} object.
     *
     * @param backgroundColor The background color to be used for a button Tiles component. Should
     *     be in ARGB format.
     * @param contentColor The content color or tint color to be used for a button Tiles component.
     *     Should be in ARGB format.
     */
    public ButtonColors(@ColorInt int backgroundColor, @ColorInt int contentColor) {
        mBackgroundColor = androidx.wear.tiles.ColorBuilders.argb(backgroundColor);
        mContentColor = androidx.wear.tiles.ColorBuilders.argb(contentColor);
    }

    /**
     * Constructor for {@link ButtonColors} object.
     *
     * @param backgroundColor The background color to be used for a button.
     * @param contentColor The content color or tint color to be used for a button.
     */
    public ButtonColors(
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp backgroundColor,
            @NonNull androidx.wear.tiles.ColorBuilders.ColorProp contentColor) {
        mBackgroundColor = backgroundColor;
        mContentColor = contentColor;
    }

    /**
     * Returns a {@link ButtonColors} object, using the current Primary colors from the given {@link
     * Colors}.
     */
    @NonNull
    public static ButtonColors primaryButtonColors(@NonNull Colors colors) {
        return new ButtonColors(colors.getPrimary(), colors.getOnPrimary());
    }

    /**
     * Returns a {@link ButtonColors} object, using the current Surface colors from the given {@link
     * Colors}.
     */
    @NonNull
    public static ButtonColors secondaryButtonColors(@NonNull Colors colors) {
        return new ButtonColors(colors.getSurface(), colors.getOnSurface());
    }

    /** The background color to be used on a button Tiles components. */
    @NonNull
    public androidx.wear.tiles.ColorBuilders.ColorProp getBackgroundColor() {
        return mBackgroundColor;
    }

    /** The content or tint color to be used on a button Tiles components. */
    @NonNull
    public androidx.wear.tiles.ColorBuilders.ColorProp getContentColor() {
        return mContentColor;
    }
}
