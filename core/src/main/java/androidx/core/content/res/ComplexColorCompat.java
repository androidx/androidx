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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Represents a color which is one of either:
 *
 * <ol>
 *  <li>A Gradient; as represented by a {@link Shader}.</li>
 *  <li>A {@link ColorStateList}</li>
 *  <li>A simple color represented by an {@code int}</li>
 * </ol>
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public final class ComplexColorCompat {
    private static final String LOG_TAG = "ComplexColorCompat";

    private final Shader mShader;
    private final ColorStateList mColorStateList;
    private int mColor; // mutable for animation/state changes

    private ComplexColorCompat(Shader shader, ColorStateList colorStateList, @ColorInt int color) {
        mShader = shader;
        mColorStateList = colorStateList;
        mColor = color;
    }

    static ComplexColorCompat from(@NonNull Shader shader) {
        return new ComplexColorCompat(shader, null, TRANSPARENT);
    }

    static ComplexColorCompat from(@NonNull ColorStateList colorStateList) {
        return new ComplexColorCompat(null, colorStateList, colorStateList.getDefaultColor());
    }

    static ComplexColorCompat from(@ColorInt int color) {
        return new ComplexColorCompat(null, null, color);
    }

    @Nullable
    public Shader getShader() {
        return mShader;
    }

    @ColorInt
    public int getColor() {
        return mColor;
    }

    public void setColor(@ColorInt int color) {
        mColor = color;
    }

    public boolean isGradient() {
        return mShader != null;
    }

    public boolean isStateful() {
        return mShader == null && mColorStateList != null && mColorStateList.isStateful();
    }

    /**
     * @return {@code true} if the given state causes this color to change, otherwise
     * {@code false}. If the color has changed, it can be retrieved via {@link #getColor}.
     * @see #isStateful()
     * @see #getColor()
     */
    public boolean onStateChanged(int[] stateSet) {
        boolean changed = false;
        if (isStateful()) {
            final int colorForState = mColorStateList.getColorForState(stateSet,
                    mColorStateList.getDefaultColor());
            if (colorForState != mColor) {
                changed = true;
                mColor = colorForState;
            }
        }
        return changed;
    }

    /**
     * @return {@code true} if the this color will draw.
     */
    public boolean willDraw() {
        return isGradient() || mColor != TRANSPARENT;
    }

    /**
     * Creates a ComplexColorCompat from an XML document using given a set of
     * {@link Resources} and a {@link Resources.Theme}.
     *
     * @param resources Resources against which the ComplexColorCompat should be inflated.
     * @param resId     the resource identifier of the ColorStateList of GradientColor to retrieve.
     * @param theme     Optional theme to apply to the color, may be {@code null}.
     * @return A new color.
     */
    @Nullable
    public static ComplexColorCompat inflate(@NonNull Resources resources, @ColorRes int resId,
            @Nullable Resources.Theme theme) {
        try {
            return createFromXml(resources, resId, theme);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to inflate ComplexColor.", e);
        }
        return null;
    }

    @NonNull
    private static ComplexColorCompat createFromXml(@NonNull Resources resources,
            @ColorRes int resId, @Nullable Resources.Theme theme)
            throws IOException, XmlPullParserException {
        @SuppressLint("ResourceType")
        XmlPullParser parser = resources.getXml(resId);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        final String name = parser.getName();
        switch (name) {
            case "selector":
                return ComplexColorCompat.from(ColorStateListInflaterCompat.createFromXmlInner(
                        resources, parser, attrs, theme));
            case "gradient":
                return ComplexColorCompat.from(GradientColorInflaterCompat.createFromXmlInner(
                        resources, parser, attrs, theme));
            default:
                throw new XmlPullParserException(parser.getPositionDescription()
                        + ": unsupported complex color tag " + name);
        }
    }
}
