/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Implementation of drawable compatibility that can call L APIs.
 */
class DrawableCompatLollipop {

    public static void setHotspot(Drawable drawable, float x, float y) {
        drawable.setHotspot(x, y);
    }

    public static void setHotspotBounds(Drawable drawable, int left, int top,
            int right, int bottom) {
        drawable.setHotspotBounds( left, top, right, bottom);
    }

    public static void setTint(Drawable drawable, int tint) {
        drawable.setTint(tint);
    }

    public static void setTintList(Drawable drawable, ColorStateList tint) {
        drawable.setTintList(tint);
    }

    public static void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
        drawable.setTintMode(tintMode);
    }

    public static Drawable wrapForTinting(final Drawable drawable) {
        if (!(drawable instanceof TintAwareDrawable)) {
            return new DrawableWrapperLollipop(drawable);
        }
        return drawable;
    }

    public static void applyTheme(Drawable drawable, Resources.Theme t) {
        drawable.applyTheme(t);
    }

    public static boolean canApplyTheme(Drawable drawable) {
        return drawable.canApplyTheme();
    }

    public static ColorFilter getColorFilter(Drawable drawable) {
        return drawable.getColorFilter();
    }

    public static void clearColorFilter(Drawable drawable) {
        drawable.clearColorFilter();

        // API 21 + 22 have an issue where clearing a color filter on a DrawableContainer
        // will not propagate to all of its children. To workaround this we unwrap the drawable
        // to find any DrawableContainers, and then unwrap those to clear the filter on its
        // children manually
        if (drawable instanceof InsetDrawable) {
            clearColorFilter(((InsetDrawable) drawable).getDrawable());
        } else if (drawable instanceof DrawableWrapper) {
            clearColorFilter(((DrawableWrapper) drawable).getWrappedDrawable());
        } else if (drawable instanceof DrawableContainer) {
            final DrawableContainer container = (DrawableContainer) drawable;
            final DrawableContainer.DrawableContainerState state =
                    (DrawableContainer.DrawableContainerState) container.getConstantState();
            if (state != null) {
                Drawable child;
                for (int i = 0, count = state.getChildCount(); i < count; i++) {
                    child = state.getChild(i);
                    if (child != null) {
                        clearColorFilter(child);
                    }
                }
            }
        }
    }

    public static void inflate(Drawable drawable, Resources res, XmlPullParser parser,
                               AttributeSet attrs, Resources.Theme t)
            throws IOException, XmlPullParserException {
        drawable.inflate(res, parser, attrs, t);
    }
}
