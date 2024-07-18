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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.core.util.Preconditions.checkNotNull;

import static java.util.Arrays.stream;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.annotation.StyleableRes;
import androidx.collection.ArrayMap;
import androidx.wear.protolayout.renderer.ProtoLayoutTheme;
import androidx.wear.protolayout.renderer.R;

import com.google.common.collect.ImmutableSet;

import java.util.Map;

/** Theme customization for ProtoLayout texts, which includes Font types and variants. */
public class ProtoLayoutThemeImpl implements ProtoLayoutTheme {

    /** Holder for different weights of the same font variant. */
    public static class FontSetImpl implements FontSet {
        final Typeface mNormalFont;
        final Typeface mMediumFont;
        final Typeface mBoldFont;

        FontSetImpl(@NonNull Theme theme, @StyleRes int style) {
            TypedArray a = theme.obtainStyledAttributes(style, R.styleable.ProtoLayoutFontSet);
            this.mNormalFont =
                    loadTypeface(a, R.styleable.ProtoLayoutFontSet_protoLayoutNormalFont);
            this.mMediumFont =
                    loadTypeface(a, R.styleable.ProtoLayoutFontSet_protoLayoutMediumFont);
            this.mBoldFont = loadTypeface(a, R.styleable.ProtoLayoutFontSet_protoLayoutBoldFont);
            a.recycle();
        }

        private static Typeface loadTypeface(TypedArray array, @StyleableRes int styleableResId) {
            // Resources are a little nasty; we can't just check if resType =
            // TypedValue.TYPE_REFERENCE, because it never is (if you use @font/foo inside of
            // styles.xml, the value will be a string of the form res/font/foo.ttf). Instead, see if
            // there's a resource ID at all, and use that, otherwise assume it's a well known font
            // family.
            int resType = array.getType(styleableResId);

            if (array.getResourceId(styleableResId, -1) != -1
                    && array.getFont(styleableResId) != null) {
                return checkNotNull(array.getFont(styleableResId));
            } else if (resType == TypedValue.TYPE_STRING
                    && array.getString(styleableResId) != null) {
                // Load the normal typeface; we customise this into BOLD/ITALIC later in
                // ProtoLayoutRenderer.
                return Typeface.create(
                        checkNotNull(array.getString(styleableResId)), Typeface.NORMAL);
            } else {
                throw new IllegalArgumentException("Unknown resource value type " + resType);
            }
        }

        @Override
        @NonNull
        public Typeface getNormalFont() {
            return mNormalFont;
        }

        @Override
        @NonNull
        public Typeface getMediumFont() {
            return mMediumFont;
        }

        @Override
        @NonNull
        public Typeface getBoldFont() {
            return mBoldFont;
        }
    }

    /**
     * Creates a ProtoLayoutTheme for the default theme, based on R.style.ProtoLayoutBaseTheme and
     * R.attr.protoLayoutFallbackAppearance from the local package.
     */
    @NonNull
    public static ProtoLayoutTheme defaultTheme(@NonNull Context context) {
        return new ProtoLayoutThemeImpl(context.getResources(), R.style.ProtoLayoutBaseTheme);
    }

    private final Map<String, FontSet> mFontFamilyToFontSet = new ArrayMap<>();
    private final Theme mTheme;
    @AttrRes private final int mFallbackTextAppearanceAttrId;

    /** Constructor with default fallbackTextAppearanceAttrId. */
    public ProtoLayoutThemeImpl(@NonNull Context context, @StyleRes int themeResId) {
        this(context.getResources(), themeResId, R.attr.protoLayoutFallbackTextAppearance);
    }

    /** Constructor with default fallbackTextAppearanceAttrId. */
    public ProtoLayoutThemeImpl(@NonNull Resources resources, @StyleRes int themeResId) {
        this(resources, themeResId, R.attr.protoLayoutFallbackTextAppearance);
    }

    /**
     * Constructor.
     *
     * @param resources Resources reference containing the styles.
     * @param themeResId a Style resource id for ProtoLayoutTheme that can be read from the
     *     specified resources.
     * @param fallbackTextAppearanceAttrId a attribute id for the fallbackTextAppearance that can be
     *     read from the specified resources
     */
    public ProtoLayoutThemeImpl(
            @NonNull Resources resources,
            @StyleRes int themeResId,
            @AttrRes int fallbackTextAppearanceAttrId) {
        mTheme = resources.newTheme();
        mTheme.applyStyle(themeResId, true);
        mFallbackTextAppearanceAttrId = fallbackTextAppearanceAttrId;

        TypedArray a = mTheme.obtainStyledAttributes(R.styleable.ProtoLayoutTheme);

        int defaultBodyFontResourceId =
                a.getResourceId(R.styleable.ProtoLayoutTheme_protoLayoutBodyFont, -1);

        // Font families. Default to body font in case theme doesn't have set attribute.
        mFontFamilyToFontSet.put(
                FONT_NAME_DEFAULT,
                new FontSetImpl(
                        mTheme,
                        a.getResourceId(
                                R.styleable.ProtoLayoutTheme_protoLayoutDefaultSystemFont,
                                defaultBodyFontResourceId)));
        mFontFamilyToFontSet.put(
                FONT_NAME_ROBOTO,
                new FontSetImpl(
                        mTheme,
                        a.getResourceId(
                                R.styleable.ProtoLayoutTheme_protoLayoutRobotoFont,
                                defaultBodyFontResourceId)));
        mFontFamilyToFontSet.put(
                FONT_NAME_ROBOTO_FLEX,
                new FontSetImpl(
                        mTheme,
                        a.getResourceId(
                                R.styleable.ProtoLayoutTheme_protoLayoutRobotoFlexFont,
                                defaultBodyFontResourceId)));

        // Legacy variants
        mFontFamilyToFontSet.put(
                FONT_NAME_LEGACY_VARIANT_TITLE,
                new FontSetImpl(
                        mTheme,
                        a.getResourceId(R.styleable.ProtoLayoutTheme_protoLayoutTitleFont, -1)));
        mFontFamilyToFontSet.put(
                FONT_NAME_LEGACY_VARIANT_BODY, new FontSetImpl(mTheme, defaultBodyFontResourceId));

        a.recycle();
    }

    private static final ImmutableSet<String> SUPPORTED_FONT_FAMILIES =
            ImmutableSet.of(
                    FONT_NAME_DEFAULT,
                    FONT_NAME_ROBOTO,
                    FONT_NAME_ROBOTO_FLEX,
                    FONT_NAME_LEGACY_VARIANT_TITLE,
                    FONT_NAME_LEGACY_VARIANT_BODY);

    /**
     * Returns the {@link FontSet} for the first font family name that is supported. If none are
     * supported, defaults to the system font.
     *
     * <p>It's theme's responsibility to define which font families are supported by returning the
     * corresponding {@link FontSet}. The default one should be system font and always supported.
     * The Roboto Flex variable font from {@link
     * androidx.wear.protolayout.LayoutElementBuilders.FontStyle#ROBOTO_FLEX_FONT} and
     * standard Roboto font from {@link
     * androidx.wear.protolayout.LayoutElementBuilders.FontStyle#ROBOTO_FONT} should be
     * supported on renderers supporting versions 1.4 and above.
     *
     * @param preferredFontFamilies the prioritized list of String values representing the preferred
     *     font families that should be used.
     */
    @NonNull
    @Override
    public FontSet getFontSet(@NonNull String... preferredFontFamilies) {
        String acceptedFontFamily =
                stream(preferredFontFamilies)
                        .filter(SUPPORTED_FONT_FAMILIES::contains)
                        .findFirst()
                        .orElse(FONT_NAME_DEFAULT);
        // Default font name would always be available.
        FontSet defaultFontSet = checkNotNull(mFontFamilyToFontSet.get(FONT_NAME_DEFAULT));
        return mFontFamilyToFontSet.getOrDefault(acceptedFontFamily, defaultFontSet);
    }

    /** Gets an Android Theme object styled with TextAppearance attributes. */
    @Override
    @NonNull
    public Theme getTheme() {
        return mTheme;
    }

    /**
     * Gets a Attribute resource Id for a fallback TextAppearance. The resource with this id should
     * be present in the Android Theme returned by {@link ProtoLayoutTheme#getTheme()}.
     */
    @Override
    @AttrRes
    public int getFallbackTextAppearanceResId() {
        return mFallbackTextAppearanceAttrId;
    }

    @Override
    @DrawableRes
    public int getRippleResId() {
        return 0;
    }
}
