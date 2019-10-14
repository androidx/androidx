/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.content.res;

import static android.graphics.Color.TRANSPARENT;
import static android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT;
import static android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT;
import static android.graphics.drawable.GradientDrawable.SWEEP_GRADIENT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Xml;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
final class GradientColorInflaterCompat {

    @IntDef({TILE_MODE_CLAMP, TILE_MODE_REPEAT, TILE_MODE_MIRROR})
    @Retention(RetentionPolicy.SOURCE)
    private @interface GradientTileMode {
    }

    private static final int TILE_MODE_CLAMP = 0;
    private static final int TILE_MODE_REPEAT = 1;
    private static final int TILE_MODE_MIRROR = 2;

    private GradientColorInflaterCompat() {
    }

    static Shader createFromXml(@NonNull Resources resources, @NonNull XmlPullParser parser,
            @Nullable Resources.Theme theme) throws XmlPullParserException, IOException {
        final AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Seek parser to start tag.
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        return createFromXmlInner(resources, parser, attrs, theme);
    }

    static Shader createFromXmlInner(@NonNull Resources resources,
            @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
            @Nullable Resources.Theme theme)
            throws IOException, XmlPullParserException {
        final String name = parser.getName();
        if (!name.equals("gradient")) {
            throw new XmlPullParserException(
                    parser.getPositionDescription() + ": invalid gradient color tag " + name);
        }

        final TypedArray a = TypedArrayUtils.obtainAttributes(resources, theme, attrs,
                R.styleable.GradientColor);
        final float startX = TypedArrayUtils.getNamedFloat(a, parser, "startX",
                R.styleable.GradientColor_android_startX, 0f);
        final float startY = TypedArrayUtils.getNamedFloat(a, parser, "startY",
                R.styleable.GradientColor_android_startY, 0f);
        final float endX = TypedArrayUtils.getNamedFloat(a, parser, "endX",
                R.styleable.GradientColor_android_endX, 0f);
        final float endY = TypedArrayUtils.getNamedFloat(a, parser, "endY",
                R.styleable.GradientColor_android_endY, 0f);
        final float centerX = TypedArrayUtils.getNamedFloat(a, parser, "centerX",
                R.styleable.GradientColor_android_centerX, 0f);
        final float centerY = TypedArrayUtils.getNamedFloat(a, parser, "centerY",
                R.styleable.GradientColor_android_centerY, 0f);
        final int type = TypedArrayUtils.getNamedInt(a, parser, "type",
                R.styleable.GradientColor_android_type, LINEAR_GRADIENT);
        final int startColor = TypedArrayUtils.getNamedColor(a, parser, "startColor",
                R.styleable.GradientColor_android_startColor, TRANSPARENT);
        final boolean hasCenterColor = TypedArrayUtils.hasAttribute(parser, "centerColor");
        final int centerColor = TypedArrayUtils.getNamedColor(a, parser, "centerColor",
                R.styleable.GradientColor_android_centerColor, TRANSPARENT);
        final int endColor = TypedArrayUtils.getNamedColor(a, parser, "endColor",
                R.styleable.GradientColor_android_endColor, TRANSPARENT);
        final int tileMode = TypedArrayUtils.getNamedInt(a, parser, "tileMode",
                R.styleable.GradientColor_android_tileMode, TILE_MODE_CLAMP);
        final float gradientRadius = TypedArrayUtils.getNamedFloat(a, parser, "gradientRadius",
                R.styleable.GradientColor_android_gradientRadius, 0f);
        a.recycle();

        ColorStops colorStops = inflateChildElements(resources, parser, attrs, theme);
        colorStops = checkColors(colorStops, startColor, endColor, hasCenterColor, centerColor);

        switch (type) {
            case RADIAL_GRADIENT:
                if (gradientRadius <= 0f) {
                    throw new XmlPullParserException(
                            "<gradient> tag requires 'gradientRadius' attribute with radial type");
                }
                return new RadialGradient(centerX, centerY, gradientRadius, colorStops.mColors,
                        colorStops.mOffsets, parseTileMode(tileMode));
            case SWEEP_GRADIENT:
                return new SweepGradient(centerX, centerY, colorStops.mColors,
                        colorStops.mOffsets);
            case LINEAR_GRADIENT:
            default:
                return new LinearGradient(startX, startY, endX, endY, colorStops.mColors,
                        colorStops.mOffsets, parseTileMode(tileMode));
        }
    }

    private static ColorStops inflateChildElements(@NonNull Resources resources,
            @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
            @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        final int innerDepth = parser.getDepth() + 1;
        int type;
        int depth;

        List<Float> offsets = new ArrayList<>(20);
        List<Integer> colors = new ArrayList<>(20);

        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth
                || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            if (depth > innerDepth || !parser.getName().equals("item")) {
                continue;
            }

            final TypedArray a = TypedArrayUtils.obtainAttributes(resources, theme, attrs,
                    R.styleable.GradientColorItem);
            final boolean hasColor = a.hasValue(R.styleable.GradientColorItem_android_color);
            final boolean hasOffset = a.hasValue(R.styleable.GradientColorItem_android_offset);
            if (!hasColor || !hasOffset) {
                throw new XmlPullParserException(
                        parser.getPositionDescription()
                                + ": <item> tag requires a 'color' attribute and a 'offset' "
                                + "attribute!");
            }

            final int color = a.getColor(R.styleable.GradientColorItem_android_color, TRANSPARENT);
            final float offset = a.getFloat(R.styleable.GradientColorItem_android_offset, 0f);
            a.recycle();

            colors.add(color);
            offsets.add(offset);
        }
        if (colors.size() > 0) return new ColorStops(colors, offsets);
        return null;
    }

    private static ColorStops checkColors(@Nullable ColorStops colorItems, @ColorInt int startColor,
            @ColorInt int endColor, boolean hasCenterColor, @ColorInt int centerColor) {
        // prefer child color items if any, otherwise use the start, (center), end colors
        if (colorItems != null) {
            return colorItems;
        } else if (hasCenterColor) {
            return new ColorStops(startColor, centerColor, endColor);
        } else {
            return new ColorStops(startColor, endColor);
        }
    }

    private static Shader.TileMode parseTileMode(@GradientTileMode int tileMode) {
        switch (tileMode) {
            case TILE_MODE_REPEAT:
                return Shader.TileMode.REPEAT;
            case TILE_MODE_MIRROR:
                return Shader.TileMode.MIRROR;
            case TILE_MODE_CLAMP:
            default:
                return Shader.TileMode.CLAMP;
        }
    }

    static final class ColorStops {
        final int[] mColors;
        final float[] mOffsets;

        ColorStops(@NonNull List<Integer> colorsList, @NonNull List<Float> offsetsList) {
            final int size = colorsList.size();
            mColors = new int[size];
            mOffsets = new float[size];
            for (int i = 0; i < size; i++) {
                mColors[i] = colorsList.get(i);
                mOffsets[i] = offsetsList.get(i);
            }
        }

        ColorStops(@ColorInt int startColor, @ColorInt int endColor) {
            mColors = new int[]{startColor, endColor};
            mOffsets = new float[]{0f, 1f};
        }

        ColorStops(@ColorInt int startColor, @ColorInt int centerColor, @ColorInt int endColor) {
            mColors = new int[]{startColor, centerColor, endColor};
            mOffsets = new float[]{0f, 0.5f, 1f};
        }
    }
}
