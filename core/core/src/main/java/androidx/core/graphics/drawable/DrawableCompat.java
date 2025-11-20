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

package androidx.core.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Drawable}.
 */
public final class DrawableCompat {

    /**
     * Call {@link Drawable#jumpToCurrentState() Drawable.jumpToCurrentState()}.
     *
     * @param drawable The Drawable against which to invoke the method.
     *
     * @deprecated Use {@link Drawable#jumpToCurrentState()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "drawable.jumpToCurrentState()")
    @Deprecated
    public static void jumpToCurrentState(@NonNull Drawable drawable) {
        drawable.jumpToCurrentState();
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
     * @deprecated Call {@link Drawable#setAutoMirrored()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "drawable.setAutoMirrored(mirrored)")
    public static void setAutoMirrored(@NonNull Drawable drawable, boolean mirrored) {
        drawable.setAutoMirrored(mirrored);
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
     * @deprecated Call {@link Drawable#isAutoMirrored()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "drawable.isAutoMirrored()")
    public static boolean isAutoMirrored(@NonNull Drawable drawable) {
        return drawable.isAutoMirrored();
    }

    /**
     * Specifies the hotspot's location within the drawable.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param x The X coordinate of the center of the hotspot
     * @param y The Y coordinate of the center of the hotspot
     */
    public static void setHotspot(@NonNull Drawable drawable, float x, float y) {
        drawable.setHotspot(x, y);
    }

    /**
     * Sets the bounds to which the hotspot is constrained, if they should be
     * different from the drawable bounds.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param left position in pixels of the left bound
     * @param top position in pixels of the top bound
     * @param right position in pixels of the right bound
     * @param bottom position in pixels of the bottom bound
     */
    public static void setHotspotBounds(@NonNull Drawable drawable, int left, int top,
            int right, int bottom) {
        drawable.setHotspotBounds(left, top, right, bottom);
    }

    /**
     * Specifies a tint for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color to use for tinting this drawable
     */
    public static void setTint(@NonNull Drawable drawable, @ColorInt int tint) {
        drawable.setTint(tint);
    }

    /**
     * Specifies a tint for {@code drawable} as a color state list.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color state list to use for tinting this drawable, or null to clear the tint
     */
    public static void setTintList(@NonNull Drawable drawable, @Nullable ColorStateList tint) {
        drawable.setTintList(tint);
    }

    /**
     * Specifies a tint blending mode for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tintMode A Porter-Duff blending mode
     */
    public static void setTintMode(@NonNull Drawable drawable, PorterDuff.@Nullable Mode tintMode) {
        drawable.setTintMode(tintMode);
    }

    /**
     * Get the alpha value of the {@code drawable}.
     * 0 means fully transparent, 255 means fully opaque.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @deprecated Call {@link Drawable#getAlpha()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "drawable.getAlpha()")
    @SuppressWarnings("unused")
    public static int getAlpha(@NonNull Drawable drawable) {
        return drawable.getAlpha();
    }

    /**
     * Applies the specified theme to this Drawable and its children.
     */
    @SuppressWarnings("unused")
    public static void applyTheme(@NonNull Drawable drawable, Resources.@NonNull Theme theme) {
        drawable.applyTheme(theme);
    }

    /**
     * Whether a theme can be applied to this Drawable and its children.
     */
    @SuppressWarnings("unused")
    public static boolean canApplyTheme(@NonNull Drawable drawable) {
        return drawable.canApplyTheme();
    }

    /**
     * Returns the current color filter, or {@code null} if none set.
     *
     * @return the current color filter, or {@code null} if none set
     */
    @SuppressWarnings("unused")
    public static @Nullable ColorFilter getColorFilter(@NonNull Drawable drawable) {
        return drawable.getColorFilter();
    }

    /**
     * Removes the color filter from the given drawable.
     */
    @SuppressWarnings("unused")
    public static void clearColorFilter(@NonNull Drawable drawable) {
        drawable.clearColorFilter();
    }

    /**
     * Inflate this Drawable from an XML resource optionally styled by a theme.
     *
     * @param drawable drawable to inflate.
     * @param res Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this Drawable
     * @param attrs Base set of attribute values
     * @param theme Theme to apply, may be null
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static void inflate(@NonNull Drawable drawable, @NonNull Resources res,
            @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
            Resources.@Nullable Theme theme)
            throws XmlPullParserException, IOException {
        drawable.inflate(res, parser, attrs, theme);
    }

    /**
     * Potentially wrap {@code drawable} so that it may be used for tinting across the
     * different API levels, via the tinting methods in this class.
     *
     * <p>If the given drawable is wrapped, we will copy over certain state over to the wrapped
     * drawable, such as its bounds, level, visibility and state.</p>
     *
     * <p>You must use the result of this call. If the given drawable is being used by a view
     * (as its background for instance), you must replace the original drawable with
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
    public static @NonNull Drawable wrap(@NonNull Drawable drawable) {
        return drawable;
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
    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    public static <T extends Drawable> T unwrap(@NonNull Drawable drawable) {
        if (drawable instanceof WrappedDrawable) {
            return (T) ((WrappedDrawable) drawable).getWrappedDrawable();
        }
        return (T) drawable;
    }

    /**
     * Set the layout direction for this drawable. Should be a resolved
     * layout direction, as the Drawable has no capacity to do the resolution on
     * its own.
     *
     * @param drawable drawable for which to set the layout direction.
     * @param layoutDirection the resolved layout direction for the drawable,
     *                        either {@link ViewCompat#LAYOUT_DIRECTION_LTR}
     *                        or {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @return {@code true} if the layout direction change has caused the
     *         appearance of the drawable to change such that it needs to be
     *         re-drawn, {@code false} otherwise
     * @see Drawable#getLayoutDirection()
     */
    public static boolean setLayoutDirection(@NonNull Drawable drawable, int layoutDirection) {
        return drawable.setLayoutDirection(layoutDirection);
    }

    /**
     * Returns the resolved layout direction for this Drawable.
     *
     * @return One of {@link ViewCompat#LAYOUT_DIRECTION_LTR},
     *         {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @see Drawable#setLayoutDirection(int)
     */
    public static int getLayoutDirection(@NonNull Drawable drawable) {
        return drawable.getLayoutDirection();
    }

    private DrawableCompat() {
    }
}
