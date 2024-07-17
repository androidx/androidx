/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer;

import android.content.res.Resources.Theme;
import android.graphics.Typeface;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/** Theme customization for ProtoLayout texts, which includes Font types and variants. */
@RestrictTo(Scope.LIBRARY)
public interface ProtoLayoutTheme {
    // These map to family names from androidx.wear.protolayout.LayoutElementBuilders.FontStyle
    String FONT_NAME_DEFAULT = "default";
    String FONT_NAME_ROBOTO = "roboto";
    String FONT_NAME_ROBOTO_FLEX = "roboto-flex";
    String FONT_NAME_LEGACY_VARIANT_TITLE = "protolayout-title";
    String FONT_NAME_LEGACY_VARIANT_BODY = "protolayout-body";

    /** Holder for different weights of the same font variant. */
    interface FontSet {

        @NonNull
        Typeface getNormalFont();

        @NonNull
        Typeface getMediumFont();

        @NonNull
        Typeface getBoldFont();
    }

    /**
     * Returns the {@link FontSet} for the first font family name that is supported. If none are
     * supported, defaults to the system font.
     *
     * <p>It's theme's responsibility to define which font family is supported by returning the
     * corresponding {@link FontSet}. The default one should be system font and always supported.
     * The Roboto Flex variable font from {@link
     * androidx.wear.protolayout.LayoutElementBuilders.FontStyle#ROBOTO_FLEX_FONT} and
     * standard Roboto font from {@link
     * androidx.wear.protolayout.LayoutElementBuilders.FontStyle#ROBOTO_FONT} should be
     * supported on renderers supporting versions 1.4 and above.
     *
     * @param preferredFontFamilies the ordered list of String values representing the preferred
     *     font families that should be used.
     */
    @NonNull
    FontSet getFontSet(@NonNull String... preferredFontFamilies);

    /** Gets an Android Theme object styled with TextAppearance attributes. */
    @NonNull
    Theme getTheme();

    /**
     * Gets an Attribute resource Id for a fallback TextAppearance. The resource with this id should
     * be present in the Android Theme returned by {@link ProtoLayoutTheme#getTheme()}.
     */
    @AttrRes
    int getFallbackTextAppearanceResId();

    /**
     * Gets a drawable resource Id for a custom ripple. The resource with this id should be present
     * in the Android Theme returned by {@link ProtoLayoutTheme#getTheme()}. If no custom ripple is
     * set, this method should return zero.
     */
    @DrawableRes
    int getRippleResId();
}
