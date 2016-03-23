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

package android.support.v7.widget;

import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** @hide */
public class DrawableUtils {

    private static final String TAG = "DrawableUtils";

    public static final Rect INSETS_NONE = new Rect();
    private static Class<?> sInsetsClazz;

    private static final String VECTOR_DRAWABLE_CLAZZ_NAME
            = "android.graphics.drawable.VectorDrawable";

    static {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                sInsetsClazz = Class.forName("android.graphics.Insets");
            } catch (ClassNotFoundException e) {
                // Oh well...
            }
        }
    }

    private DrawableUtils() {}

    /**
     * Allows us to get the optical insets for a {@link Drawable}. Since this is hidden we need to
     * use reflection. Since the {@code Insets} class is hidden also, we return a Rect instead.
     */
    public static Rect getOpticalBounds(Drawable drawable) {
        if (sInsetsClazz != null) {
            try {
                // If the Drawable is wrapped, we need to manually unwrap it and process
                // the wrapped drawable.
                drawable = DrawableCompat.unwrap(drawable);

                final Method getOpticalInsetsMethod = drawable.getClass()
                        .getMethod("getOpticalInsets");
                final Object insets = getOpticalInsetsMethod.invoke(drawable);

                if (insets != null) {
                    // If the drawable has some optical insets, let's copy them into a Rect
                    final Rect result = new Rect();

                    for (Field field : sInsetsClazz.getFields()) {
                        switch (field.getName()) {
                            case "left":
                               result.left = field.getInt(insets);
                                break;
                            case "top":
                                result.top = field.getInt(insets);
                                break;
                            case "right":
                                result.right = field.getInt(insets);
                                break;
                            case "bottom":
                                result.bottom = field.getInt(insets);
                                break;
                        }
                    }
                    return result;
                }
            } catch (Exception e) {
                // Eugh, we hit some kind of reflection issue...
                Log.e(TAG, "Couldn't obtain the optical insets. Ignoring.");
            }
        }

        // If we reach here, either we're running on a device pre-v18, the Drawable didn't have
        // any optical insets, or a reflection issue, so we'll just return an empty rect
        return INSETS_NONE;
    }

    /**
     * Attempt the fix any issues in the given drawable, usually caused by platform bugs in the
     * implementation. This method should be call after retrieval from
     * {@link android.content.res.Resources} or a {@link android.content.res.TypedArray}.
     */
    static void fixDrawable(@NonNull final Drawable drawable) {
        if (Build.VERSION.SDK_INT == 21
                && VECTOR_DRAWABLE_CLAZZ_NAME.equals(drawable.getClass().getName())) {
            fixVectorDrawableTinting(drawable);
        }
    }

    /**
     * Some drawable implementations have problems with mutation. This method returns false if
     * there is a known issue in the given drawable's implementation.
     */
    public static boolean canSafelyMutateDrawable(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT < 8 && drawable instanceof StateListDrawable) {
            // StateListDrawable has a bug in mutate() on API 7
            return false;
        } else if (Build.VERSION.SDK_INT < 15 && drawable instanceof InsetDrawable) {
            return false;
        }  else if (Build.VERSION.SDK_INT < 15 && drawable instanceof GradientDrawable) {
            // GradientDrawable has a bug pre-ICS which results in mutate() resulting
            // in loss of color
            return false;
        } else if (Build.VERSION.SDK_INT < 17 && drawable instanceof LayerDrawable) {
            return false;
        }

        if (drawable instanceof DrawableContainer) {
            // If we have a DrawableContainer, let's traverse it's child array
            final Drawable.ConstantState state = drawable.getConstantState();
            if (state instanceof DrawableContainer.DrawableContainerState) {
                final DrawableContainer.DrawableContainerState containerState =
                        (DrawableContainer.DrawableContainerState) state;
                for (final Drawable child : containerState.getChildren()) {
                    if (!canSafelyMutateDrawable(child)) {
                        return false;
                    }
                }
            }
        } else if (drawable instanceof android.support.v4.graphics.drawable.DrawableWrapper) {
            return canSafelyMutateDrawable(
                    ((android.support.v4.graphics.drawable.DrawableWrapper) drawable)
                            .getWrappedDrawable());
        } else if (drawable instanceof android.support.v7.graphics.drawable.DrawableWrapper) {
            return canSafelyMutateDrawable(
                    ((android.support.v7.graphics.drawable.DrawableWrapper) drawable)
                            .getWrappedDrawable());
        } else if (drawable instanceof ScaleDrawable) {
            return canSafelyMutateDrawable(((ScaleDrawable) drawable).getDrawable());
        }

        return true;
    }

    /**
     * VectorDrawable has an issue on API 21 where it sometimes doesn't create its tint filter.
     * Fixed by toggling it's state to force a filter creation.
     */
    private static void fixVectorDrawableTinting(final Drawable drawable) {
        final int[] originalState = drawable.getState();
        if (originalState == null || originalState.length == 0) {
            // The drawable doesn't have a state, so set it to be checked
            drawable.setState(ThemeUtils.CHECKED_STATE_SET);
        } else {
            // Else the drawable does have a state, so clear it
            drawable.setState(ThemeUtils.EMPTY_STATE_SET);
        }
        // Now set the original state
        drawable.setState(originalState);
    }

    static PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
        switch (value) {
            case 3: return PorterDuff.Mode.SRC_OVER;
            case 5: return PorterDuff.Mode.SRC_IN;
            case 9: return PorterDuff.Mode.SRC_ATOP;
            case 14: return PorterDuff.Mode.MULTIPLY;
            case 15: return PorterDuff.Mode.SCREEN;
            case 16: return Build.VERSION.SDK_INT >= 11
                    ? PorterDuff.Mode.valueOf("ADD")
                    : defaultMode;
            default: return defaultMode;
        }
    }

}
