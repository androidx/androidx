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
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link android.graphics.drawable.Drawable}.
 */
public final class DrawableCompat {
    private static final String TAG = "DrawableCompat";

    private static Method sSetLayoutDirectionMethod;
    private static boolean sSetLayoutDirectionMethodFetched;

    private static Method sGetLayoutDirectionMethod;
    private static boolean sGetLayoutDirectionMethodFetched;

    /**
     * Call {@link Drawable#jumpToCurrentState() Drawable.jumpToCurrentState()}.
     *
     * @param drawable The Drawable against which to invoke the method.
     *
     * @deprecated Use {@link Drawable#jumpToCurrentState()} directly.
     */
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
     */
    public static void setAutoMirrored(@NonNull Drawable drawable, boolean mirrored) {
        if (Build.VERSION.SDK_INT >= 19) {
            drawable.setAutoMirrored(mirrored);
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            return drawable.isAutoMirrored();
        } else {
            return false;
        }
    }

    /**
     * Specifies the hotspot's location within the drawable.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param x The X coordinate of the center of the hotspot
     * @param y The Y coordinate of the center of the hotspot
     */
    public static void setHotspot(@NonNull Drawable drawable, float x, float y) {
        if (Build.VERSION.SDK_INT >= 21) {
            drawable.setHotspot(x, y);
        }
    }

    /**
     * Sets the bounds to which the hotspot is constrained, if they should be
     * different from the drawable bounds.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static void setHotspotBounds(@NonNull Drawable drawable, int left, int top,
            int right, int bottom) {
        if (Build.VERSION.SDK_INT >= 21) {
            drawable.setHotspotBounds(left, top, right, bottom);
        }
    }

    /**
     * Specifies a tint for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color to use for tinting this drawable
     */
    public static void setTint(@NonNull Drawable drawable, @ColorInt int tint) {
        if (Build.VERSION.SDK_INT >= 21) {
            drawable.setTint(tint);
        } else if (drawable instanceof TintAwareDrawable) {
            ((TintAwareDrawable) drawable).setTint(tint);
        }
    }

    /**
     * Specifies a tint for {@code drawable} as a color state list.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tint     Color state list to use for tinting this drawable, or null to clear the tint
     */
    public static void setTintList(@NonNull Drawable drawable, @Nullable ColorStateList tint) {
        if (Build.VERSION.SDK_INT >= 21) {
            drawable.setTintList(tint);
        } else if (drawable instanceof TintAwareDrawable) {
            ((TintAwareDrawable) drawable).setTintList(tint);
        }
    }

    /**
     * Specifies a tint blending mode for {@code drawable}.
     *
     * @param drawable The Drawable against which to invoke the method.
     * @param tintMode A Porter-Duff blending mode
     */
    public static void setTintMode(@NonNull Drawable drawable, @NonNull PorterDuff.Mode tintMode) {
        if (Build.VERSION.SDK_INT >= 21) {
            drawable.setTintMode(tintMode);
        } else if (drawable instanceof TintAwareDrawable) {
            ((TintAwareDrawable) drawable).setTintMode(tintMode);
        }
    }

    /**
     * Get the alpha value of the {@code drawable}.
     * 0 means fully transparent, 255 means fully opaque.
     *
     * @param drawable The Drawable against which to invoke the method.
     */
    public static int getAlpha(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 19) {
            return drawable.getAlpha();
        } else {
            return 0;
        }
    }

    /**
     * Applies the specified theme to this Drawable and its children.
     */
    public static void applyTheme(@NonNull Drawable drawable, @NonNull Resources.Theme theme) {
        if (Build.VERSION.SDK_INT >= 21) {
            drawable.applyTheme(theme);
        }
    }

    /**
     * Whether a theme can be applied to this Drawable and its children.
     */
    public static boolean canApplyTheme(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 21) {
            return drawable.canApplyTheme();
        } else {
            return false;
        }
    }

    /**
     * Returns the current color filter, or {@code null} if none set.
     *
     * @return the current color filter, or {@code null} if none set
     */
    public static ColorFilter getColorFilter(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 21) {
            return drawable.getColorFilter();
        } else {
            return null;
        }
    }

    /**
     * Removes the color filter from the given drawable.
     */
    public static void clearColorFilter(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 23) {
            // We can use clearColorFilter() safely on M+
            drawable.clearColorFilter();
        } else if (Build.VERSION.SDK_INT >= 21) {
            drawable.clearColorFilter();

            // API 21 + 22 have an issue where clearing a color filter on a DrawableContainer
            // will not propagate to all of its children. To workaround this we unwrap the drawable
            // to find any DrawableContainers, and then unwrap those to clear the filter on its
            // children manually
            if (drawable instanceof InsetDrawable) {
                clearColorFilter(((InsetDrawable) drawable).getDrawable());
            } else if (drawable instanceof WrappedDrawable) {
                clearColorFilter(((WrappedDrawable) drawable).getWrappedDrawable());
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
        } else {
            drawable.clearColorFilter();
        }
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
    public static void inflate(@NonNull Drawable drawable, @NonNull Resources res,
            @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
            @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        if (Build.VERSION.SDK_INT >= 21) {
            drawable.inflate(res, parser, attrs, theme);
        } else {
            drawable.inflate(res, parser, attrs);
        }
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
    public static Drawable wrap(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 23) {
            return drawable;
        } else if (Build.VERSION.SDK_INT >= 21) {
            if (!(drawable instanceof TintAwareDrawable)) {
                return new WrappedDrawableApi21(drawable);
            }
            return drawable;
        } else {
            if (!(drawable instanceof TintAwareDrawable)) {
                return new WrappedDrawableApi14(drawable);
            }
            return drawable;
        }
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
    @SuppressWarnings("TypeParameterUnusedInFormals")
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
     * @param layoutDirection the resolved layout direction for the drawable,
     *                        either {@link ViewCompat#LAYOUT_DIRECTION_LTR}
     *                        or {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @return {@code true} if the layout direction change has caused the
     *         appearance of the drawable to change such that it needs to be
     *         re-drawn, {@code false} otherwise
     * @see #getLayoutDirection(Drawable)
     */
    public static boolean setLayoutDirection(@NonNull Drawable drawable, int layoutDirection) {
        if (Build.VERSION.SDK_INT >= 23) {
            return drawable.setLayoutDirection(layoutDirection);
        } else if (Build.VERSION.SDK_INT >= 17) {
            if (!sSetLayoutDirectionMethodFetched) {
                try {
                    sSetLayoutDirectionMethod =
                            Drawable.class.getDeclaredMethod("setLayoutDirection", int.class);
                    sSetLayoutDirectionMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    Log.i(TAG, "Failed to retrieve setLayoutDirection(int) method", e);
                }
                sSetLayoutDirectionMethodFetched = true;
            }

            if (sSetLayoutDirectionMethod != null) {
                try {
                    sSetLayoutDirectionMethod.invoke(drawable, layoutDirection);
                    return true;
                } catch (Exception e) {
                    Log.i(TAG, "Failed to invoke setLayoutDirection(int) via reflection", e);
                    sSetLayoutDirectionMethod = null;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * Returns the resolved layout direction for this Drawable.
     *
     * @return One of {@link ViewCompat#LAYOUT_DIRECTION_LTR},
     *         {@link ViewCompat#LAYOUT_DIRECTION_RTL}
     * @see #setLayoutDirection(Drawable, int)
     */
    public static int getLayoutDirection(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 23) {
            return drawable.getLayoutDirection();
        } else if (Build.VERSION.SDK_INT >= 17) {
            if (!sGetLayoutDirectionMethodFetched) {
                try {
                    sGetLayoutDirectionMethod =
                            Drawable.class.getDeclaredMethod("getLayoutDirection");
                    sGetLayoutDirectionMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    Log.i(TAG, "Failed to retrieve getLayoutDirection() method", e);
                }
                sGetLayoutDirectionMethodFetched = true;
            }

            if (sGetLayoutDirectionMethod != null) {
                try {
                    return (int) sGetLayoutDirectionMethod.invoke(drawable);
                } catch (Exception e) {
                    Log.i(TAG, "Failed to invoke getLayoutDirection() via reflection", e);
                    sGetLayoutDirectionMethod = null;
                }
            }
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        } else {
            return ViewCompat.LAYOUT_DIRECTION_LTR;
        }
    }

    private DrawableCompat() {}
}
