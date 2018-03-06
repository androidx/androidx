/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.content.res;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.util.StateSet;
import android.util.Xml;

import java.io.IOException;

final class AppCompatColorStateListInflater {

    private static final int DEFAULT_COLOR = Color.RED;

    private AppCompatColorStateListInflater() {}

    /**
     * Creates a ColorStateList from an XML document using given a set of
     * {@link Resources} and a {@link Theme}.
     *
     * @param r Resources against which the ColorStateList should be inflated.
     * @param parser Parser for the XML document defining the ColorStateList.
     * @param theme Optional theme to apply to the color state list, may be
     *              {@code null}.
     * @return A new color state list.
     */
    @NonNull
    public static ColorStateList createFromXml(@NonNull Resources r, @NonNull XmlPullParser parser,
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

        return createFromXmlInner(r, parser, attrs, theme);
    }

    /**
     * Create from inside an XML document. Called on a parser positioned at a
     * tag in an XML document, tries to create a ColorStateList from that tag.
     *
     * @throws XmlPullParserException if the current tag is not &lt;selector>
     * @return A new color state list for the current tag.
     */
    @NonNull
    private static ColorStateList createFromXmlInner(@NonNull Resources r,
            @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
            @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        final String name = parser.getName();
        if (!name.equals("selector")) {
            throw new XmlPullParserException(
                    parser.getPositionDescription() + ": invalid color state list tag " + name);
        }

        return inflate(r, parser, attrs, theme);
    }

    /**
     * Fill in this object based on the contents of an XML "selector" element.
     */
    private static ColorStateList inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        final int innerDepth = parser.getDepth() + 1;
        int depth;
        int type;
        int defaultColor = DEFAULT_COLOR;

        int[][] stateSpecList = new int[20][];
        int[] colorList = new int[stateSpecList.length];
        int listSize = 0;

        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG || depth > innerDepth
                    || !parser.getName().equals("item")) {
                continue;
            }

            final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ColorStateListItem);
            final int baseColor = a.getColor(R.styleable.ColorStateListItem_android_color,
                    Color.MAGENTA);

            float alphaMod = 1.0f;
            if (a.hasValue(R.styleable.ColorStateListItem_android_alpha)) {
                alphaMod = a.getFloat(R.styleable.ColorStateListItem_android_alpha, alphaMod);
            } else if (a.hasValue(R.styleable.ColorStateListItem_alpha)) {
                alphaMod = a.getFloat(R.styleable.ColorStateListItem_alpha, alphaMod);
            }

            a.recycle();

            // Parse all unrecognized attributes as state specifiers.
            int j = 0;
            final int numAttrs = attrs.getAttributeCount();
            int[] stateSpec = new int[numAttrs];
            for (int i = 0; i < numAttrs; i++) {
                final int stateResId = attrs.getAttributeNameResource(i);
                if (stateResId != android.R.attr.color && stateResId != android.R.attr.alpha
                        && stateResId != R.attr.alpha) {
                    // Unrecognized attribute, add to state set
                    stateSpec[j++] = attrs.getAttributeBooleanValue(i, false)
                            ? stateResId : -stateResId;
                }
            }
            stateSpec = StateSet.trimStateSet(stateSpec, j);

            // Apply alpha modulation. If we couldn't resolve the color or
            // alpha yet, the default values leave us enough information to
            // modulate again during applyTheme().
            final int color = modulateColorAlpha(baseColor, alphaMod);
            if (listSize == 0 || stateSpec.length == 0) {
                defaultColor = color;
            }

            colorList = GrowingArrayUtils.append(colorList, listSize, color);
            stateSpecList = GrowingArrayUtils.append(stateSpecList, listSize, stateSpec);
            listSize++;
        }

        int[] colors = new int[listSize];
        int[][] stateSpecs = new int[listSize][];
        System.arraycopy(colorList, 0, colors, 0, listSize);
        System.arraycopy(stateSpecList, 0, stateSpecs, 0, listSize);

        return new ColorStateList(stateSpecs, colors);
    }

    private static TypedArray obtainAttributes(Resources res, Resources.Theme theme,
            AttributeSet set, int[] attrs) {
        return theme == null ? res.obtainAttributes(set, attrs)
                : theme.obtainStyledAttributes(set, attrs, 0, 0);
    }

    private static int modulateColorAlpha(int color, float alphaMod) {
        return ColorUtils.setAlphaComponent(color, Math.round(Color.alpha(color) * alphaMod));
    }
}
