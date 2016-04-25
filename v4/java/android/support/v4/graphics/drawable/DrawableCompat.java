/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Drawable}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class DrawableCompat {
    /**
     * Interface for the full API.
     */
    interface DrawableImpl {
        void jumpToCurrentState(Drawable drawable);
        void setAutoMirrored(Drawable drawable, boolean mirrored);
        boolean isAutoMirrored(Drawable drawable);
        void setHotspot(Drawable drawable, float x, float y);
        void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom);
        void setTint(Drawable drawable, int tint);
        void setTintList(Drawable drawable, ColorStateList tint);
        void setTintMode(Drawable drawable, PorterDuff.Mode tintMode);
        Drawable wrap(Drawable drawable);
        boolean setLayoutDirection(Drawable drawable, int layoutDirection);
        int getLayoutDirection(Drawable drawable);
        int getAlpha(Drawable drawable);
        void applyTheme(Drawable drawable, Resources.Theme t);
        boolean canApplyTheme(Drawable drawable);
        ColorFilter getColorFilter(Drawable drawable);
        void inflate(Drawable drawable, Resources res, XmlPullParser parser, AttributeSet attrs,
                     Resources.Theme t) throws IOException, XmlPullParserException;
    }

    /**
     * Interface implementation that doesn't use anything about v4 APIs.
     */
    static class BaseDrawableImpl implements DrawableImpl {
        @Override
        public void jumpToCurrentState(Drawable drawable) {
        }

        @Override
        public void setAutoMirrored(Drawable drawable, boolean mirrored) {
        }

        @Override
        public boolean isAutoMirrored(Drawable drawable) {
            return false;
        }

        @Override
        public void setHotspot(Drawable drawable, float x, float y) {
        }

        @Override
        public void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom) {
        }

        @Override
        public void setTint(Drawable drawable, int tint) {
            DrawableCompatBase.setTint(drawable, tint);
        }

        @Override
        public void setTintList(Drawable drawable, ColorStateList tint) {
            DrawableCompatBase.setTintList(drawable, tint);
        }

        @Override
        public void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
            DrawableCompatBase.setTintMode(drawable, tintMode);
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            return DrawableCompatBase.wrapForTinting(drawable);
        }

        @Override
        public boolean setLayoutDirection(Drawable drawable, int layoutDirection) {
            // No op for API < 23
            return false;
        }

        @Override
        public int getLayoutDirection(Drawable drawable) {
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        }

        @Override
        public int getAlpha(Drawable drawable) {
            return 0;
        }

        @Override
        public void applyTheme(Drawable drawable, Resources.Theme t) {
        }

        @Override
        public boolean canApplyTheme(Drawable drawable) {
            return false;
        }

        @Override
        public ColorFilter getColorFilter(Drawable drawable) {
            return null;
        }

        @Override
        public void inflate(Drawable drawable, Resources res, XmlPullParser parser,
                            AttributeSet attrs, Resources.Theme t)
                throws IOException, XmlPullParserException {
            DrawableCompatBase.inflate(drawable, res, parser, attrs, t);
        }
    }

    /**
     * Interface implementation for devices with at least v5 APIs.
     */
    static class EclairDrawableImpl extends BaseDrawableImpl {
        @Override
        public Drawable wrap(Drawable drawable) {
            return DrawableCompatEclair.wrapForTinting(drawable);
        }
    }

    /**
     * Interface implementation for devices with at least v11 APIs.
     */
    static class HoneycombDrawableImpl extends EclairDrawableImpl {
        @Override
        public void jumpToCurrentState(Drawable drawable) {
            DrawableCompatHoneycomb.jumpToCurrentState(drawable);
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            return DrawableCompatHoneycomb.wrapForTinting(drawable);
        }
    }

    static class JellybeanMr1DrawableImpl extends HoneycombDrawableImpl {
        @Override
        public boolean setLayoutDirection(Drawable drawable, int layoutDirection) {
            return DrawableCompatJellybeanMr1.setLayoutDirection(drawable, layoutDirection);
        }

        @Override
        public int getLayoutDirection(Drawable drawable) {
            final int dir = DrawableCompatJellybeanMr1.getLayoutDirection(drawable);
            return dir >= 0 ? dir : ViewCompat.LAYOUT_DIRECTION_LTR;
        }
    }

    /**
     * Interface implementation for devices with at least KitKat APIs.
     */
    static class KitKatDrawableImpl extends JellybeanMr1DrawableImpl {
        @Override
        public void setAutoMirrored(Drawable drawable, boolean mirrored) {
            DrawableCompatKitKat.setAutoMirrored(drawable, mirrored);
        }

        @Override
        public boolean isAutoMirrored(Drawable drawable) {
            return DrawableCompatKitKat.isAutoMirrored(drawable);
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            return DrawableCompatKitKat.wrapForTinting(drawable);
        }

        @Override
        public int getAlpha(Drawable drawable) {
            return DrawableCompatKitKat.getAlpha(drawable);
        }
    }

    /**
     * Interface implementation for devices with at least L APIs.
     */
    static class LollipopDrawableImpl extends KitKatDrawableImpl {
        @Override
        public void setHotspot(Drawable drawable, float x, float y) {
            DrawableCompatLollipop.setHotspot(drawable, x, y);
        }

        @Override
        public void setHotspotBounds(Drawable drawable, int left, int top, int right, int bottom) {
            DrawableCompatLollipop.setHotspotBounds(drawable, left, top, right, bottom);
        }

        @Override
        public void setTint(Drawable drawable, int tint) {
            DrawableCompatLollipop.setTint(drawable, tint);
        }

        @Override
        public void setTintList(Drawable drawable, ColorStateList tint) {
            DrawableCompatLollipop.setTintList(drawable, tint);
        }

        @Override
        public void setTintMode(Drawable drawable, PorterDuff.Mode tintMode) {
            DrawableCompatLollipop.setTintMode(drawable, tintMode);
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            return DrawableCompatLollipop.wrapForTinting(drawable);
        }

        @Override
        public void applyTheme(Drawable drawable, Resources.Theme t) {
            DrawableCompatLollipop.applyTheme(drawable, t);
        }

        @Override
        public boolean canApplyTheme(Drawable drawable) {
            return DrawableCompatLollipop.canApplyTheme(drawable);
        }

        @Override
        public ColorFilter getColorFilter(Drawable drawable) {
            return DrawableCompatLollipop.getColorFilter(drawable);
        }

        @Override
        public void inflate(Drawable drawable, Resources res, XmlPullParser parser,
                            AttributeSet attrs, Resources.Theme t)
                throws IOException, XmlPullParserException {
            DrawableCompatLollipop.inflate(drawable, res, parser, attrs, t);
        }
    }

    /**
     * Interface implementation for devices with at least M APIs.
     */
    static class MDrawableImpl extends LollipopDrawableImpl {
        @Override
        public boolean setLayoutDirection(Drawable drawable, int layoutDirection) {
            return DrawableCompatApi23.setLayoutDirection(drawable, layoutDirection);
        }

        @Override
        public int getLayoutDirection(Drawable drawable) {
            return DrawableCompatApi23.getLayoutDirection(drawable);
        }

        @Override
        public Drawable wrap(Drawable drawable) {
            // No need to wrap on M+
            return drawable;
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final DrawableImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 23) {
            IMPL = new MDrawableImpl();
        } else if (version >= 21) {
            IMPL = new LollipopDrawableImpl();
        } else if (version >= 19) {
            IMPL = new KitKatDrawableImpl();
        } else if (version >= 17) {
            IMPL = new JellybeanMr1DrawableImpl();
        } else if (version >= 11) {
            IMPL = new HoneycombDrawableImpl();
        } else if (version >= 5) {
            IMPL = new EclairDrawableImpl();
        } else {
            IMPL = new BaseDrawableImpl();
        }
    }

    /**
     * Call {@link Drawable#jumpToCurrentState() Drawable.jumpToCurrentState()}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#HONEYCOMB}
     * device this method does nothing.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static void jumpToCurrentState(@NonNull Drawable drawable) {
        IMPL.jumpToCurrentState(drawable);
    }

    /**
     * Set whether this Drawable is automatically mirrored when its layout
     * direction is RTL (right-to left). See
     * {@link android.util.LayoutDirection}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#KITKAT} device
     * this method does nothing.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param mirrored Set to true if the Drawable should be mirrored, false if
     *            not.
     */
    public static void setAutoMirrored(@NonNull Drawable drawable, boolean mirrored) {
        IMPL.setAutoMirrored(drawable, mirrored);
    }

    /**
     * Tells if this Drawable will be automatically mirrored when its layout
     * direction is RTL right-to-left. See {@link android.util.LayoutDirection}.
     * <p>
     * If running on a pre-{@link android.os.Build.VERSION_CODES#KITKAT} device
     * this method returns false.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @return boolean Returns true if this Drawable will be automatically
     *         mirrored.
     */
    public static boolean isAutoMirrored(@NonNull Drawable drawable) {
        return IMPL.isAutoMirrored(drawable);
    }

    /**
     * Specifies the hotspot's location within the drawable.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param x The X coordinate of the center of the hotspot
     * @param y The Y coordinate of the center of the hotspot
     */
    public static void setHotspot(@NonNull Drawable drawable, float x, float y) {
        IMPL.setHotspot(drawable, x, y);
    }

    /**
     * Sets the bounds to which the hotspot is constrained, if they should be
     * different from the drawable bounds.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static void setHotspotBounds(@NonNull Drawable drawable, int left, int top,
            int right, int bottom) {
        IMPL.setHotspotBounds(drawable, left, top, right, bottom);
    }

    /**
     * Specifies a tint for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color to use for tinting this drawable
     */
    public static void setTint(@NonNull Drawable drawable, @ColorInt int tint) {
        IMPL.setTint(drawable, tint);
    }

    /**
     * Specifies a tint for {@code drawable} as a color state list.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color state list to use for tinting this drawable, or null to clear the tint
     */
    public static void setTintList(@NonNull Drawable drawable, @Nullable ColorStateList tint) {
        IMPL.setTintList(drawable, tint);
    }

    /**
     * Specifies a tint blending mode for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tintMode A Porter-Duff blending mode
     */
    public static void setTintMode(@NonNull Drawable drawable, @Nullable PorterDuff.Mode tintMode) {
        IMPL.setTintMode(drawable, tintMode);
    }

    /**
     * Get the alpha value of the {@code drawable}.
     * 0 means fully transparent, 255 means fully opaque.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static int getAlpha(@NonNull Drawable drawable) {
        return IMPL.getAlpha(drawable);
    }

    /**
     * Applies the specified theme to this Drawable and its children.
     */
    public static void applyTheme(Drawable drawable, Resources.Theme t) {
        IMPL.applyTheme(drawable, t);
    }

    /**
     * Whether a theme can be applied to this Drawable and its children.
     */
    public static boolean canApplyTheme(Drawable drawable) {
        return IMPL.canApplyTheme(drawable);
    }

    /**
     * Returns the current color filter, or {@code null} if none set.
     *
     * @return the current color filter, or {@code null} if none set
     */
    public static ColorFilter getColorFilter(Drawable drawable) {
        return IMPL.getColorFilter(drawable);
    }

    /**
     * Inflate this Drawable from an XML resource optionally styled by a theme.
     *
     * @param res Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this Drawable
     * @param attrs Base set of attribute values
     * @param theme Theme to apply, may be null
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static void inflate(Drawable drawable, Resources res, XmlPullParser parser,
                               AttributeSet attrs, Resources.Theme theme)
            throws XmlPullParserException, IOException {
        IMPL.inflate(drawable, res, parser, attrs, theme);
    }

    /**
     * Potentially wrap {@code drawable} so that it may be used for tinting across the
     * different API levels, via the tinting methods in this class.
     *
     * <p>You must use the result of this call. If the given drawable is being used by a view
     * (as it's background for instance), you must replace the original drawable with
     * the result of this call:</p>
     *
     * <pre>
     * Drawable bg = DrawableCompat.wrap(view.getBackground());
     * // Need to set the background with the wrapped drawable
     * view.setBackground(bg);
     *
     * // You can now tint the drawable
     * DrawableCompat.setTint(bg, ...);
     * </pre>
     *
     * <p>If you need to get hold of the original {@link android.graphics.drawable.Drawable} again,
     * you can use the value returned from {@link #unwrap(Drawable)}.</p>
     *
     * @param drawable The Drawable to process
     * @return A drawable capable of being tinted across all API levels.
     *
     * @see #setTint(Drawable, int)
     * @see #setTintList(Drawable, ColorStateList)
     * @see #setTintMode(Drawable, PorterDuff.Mode)
     * @see #unwrap(Drawable)
     */
    public static Drawable wrap(@NonNull Drawable drawable) {
        return IMPL.wrap(drawable);
    }

    /**
     * Unwrap {@code drawable} if it is the result of a call to {@link #wrap(Drawable)}. If
     * the {@code drawable} is not the result of a call to {@link #wrap(Drawable)} then
     * {@code drawable} is returned as-is.
     *
     * @param drawable The drawable to unwrap
     * @return the unwrapped {@link Drawable} or {@code drawable} if it hasn't been wrapped.
     *
     * @see #wrap(Drawable)
     */
    public static <T extends Drawable> T unwrap(@NonNull Drawable drawable) {
        if (drawable instanceof DrawableWrapper) {
            return (T) ((DrawableWrapper) drawable).getWrappedDrawable();
        }
        return (T) drawable;
    }

    /**
     * Set the layout direction for this drawable. Should be a resolved
     * layout direction, as the Drawable has no capacity to do the resolution on
     * its own.
     *
     * @param layoutDirection the resolved layout direction for the drawable,
     *                        either {@link ViewCompat#LAYOUT_DIRECTION_LTR}
     *                        or {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @return {@code true} if the layout direction change has caused the
     *         appearance of the drawable to change such that it needs to be
     *         re-drawn, {@code false} otherwise
     * @see #getLayoutDirection(Drawable)
     */
    public static boolean setLayoutDirection(@NonNull Drawable drawable, int layoutDirection) {
        return IMPL.setLayoutDirection(drawable, layoutDirection);
    }

    /**
     * Returns the resolved layout direction for this Drawable.
     *
     * @return One of {@link ViewCompat#LAYOUT_DIRECTION_LTR},
     *         {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @see #setLayoutDirection(Drawable, int)
     */
    public static int getLayoutDirection(@NonNull Drawable drawable) {
        return IMPL.getLayoutDirection(drawable);
    }

    private DrawableCompat() {}
}
