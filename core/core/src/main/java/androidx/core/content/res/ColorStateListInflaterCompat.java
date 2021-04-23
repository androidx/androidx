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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StateSet;
import android.util.TypedValue;
import android.util.Xml;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.XmlRes;
import androidx.core.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public final class ColorStateListInflaterCompat {

    private static final ThreadLocal<TypedValue> sTempTypedValue = new ThreadLocal<>();

    private ColorStateListInflaterCompat() {
    }

    /**
     * Creates a ColorStateList from an XML document using given a set of
     * {@link Resources} and a {@link Resources.Theme}.
     *
     * @param resources Resources against which the ColorStateList should be inflated.
     * @param resId     the resource identifier of the ColorStateList to retrieve.
     * @param theme     Optional theme to apply to the color, may be {@code null}.
     * @return A new color state list.
     */
    @Nullable
    public static ColorStateList inflate(@NonNull Resources resources, @XmlRes int resId,
            @Nullable Resources.Theme theme) {
        try {
            XmlPullParser parser = resources.getXml(resId);
            return createFromXml(resources, parser, theme);
        } catch (Exception e) {
            Log.e("CSLCompat", "Failed to inflate ColorStateList.", e);
        }
        return null;
    }

    /**
     * Creates a ColorStateList from an XML document using given a set of
     * {@link Resources} and a {@link android.content.res.Resources.Theme}.
     *
     * @param r      Resources against which the ColorStateList should be inflated.
     * @param parser Parser for the XML document defining the ColorStateList.
     * @param theme  Optional theme to apply to the color state list, may be
     *               {@code null}.
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
     * @return A new color state list for the current tag.
     * @throws XmlPullParserException if the current tag is not &lt;selector>
     */
    @NonNull
    public static ColorStateList createFromXmlInner(@NonNull Resources r,
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
            int resourceId = a.getResourceId(R.styleable.ColorStateListItem_android_color, -1);
            int baseColor;
            if (resourceId != -1 && !isColorInt(r, resourceId)) {
                try {
                    baseColor = createFromXml(r, r.getXml(resourceId), theme).getDefaultColor();
                } catch (Exception e) {
                    baseColor = a.getColor(R.styleable.ColorStateListItem_android_color,
                            Color.MAGENTA);
                }
            } else {
                baseColor = a.getColor(R.styleable.ColorStateListItem_android_color, Color.MAGENTA);
            }

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

    private static boolean isColorInt(@NonNull Resources r, @ColorRes int resId) {
        final TypedValue value = getTypedValue();
        r.getValue(resId, value, true);
        return value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    @NonNull
    private static TypedValue getTypedValue() {
        TypedValue tv = sTempTypedValue.get();
        if (tv == null) {
            tv = new TypedValue();
            sTempTypedValue.set(tv);
        }
        return tv;
    }

    private static TypedArray obtainAttributes(Resources res, Resources.Theme theme,
            AttributeSet set, int[] attrs) {
        return theme == null ? res.obtainAttributes(set, attrs)
                : theme.obtainStyledAttributes(set, attrs, 0, 0);
    }

    @ColorInt
    private static int modulateColorAlpha(@ColorInt int color,
            @FloatRange(from = 0f, to = 1f) float alphaMod) {
        int alpha = Math.round(Color.alpha(color) * alphaMod);
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
