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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.graphics.drawable.DrawableWrapper;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.WrappedDrawable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** @hide */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class DrawableUtils {

    private static final int[] CHECKED_STATE_SET = new int[] { android.R.attr.state_checked };
    private static final int[] EMPTY_STATE_SET = new int[0];

    public static final Rect INSETS_NONE = new Rect();

    private DrawableUtils() {
        // This class is non-instantiable.
    }

    /**
     * Allows us to get the optical insets for a {@link Drawable}. Since this is hidden we need to
     * use reflection. Since the {@code Insets} class is hidden also, we return a Rect instead.
     */
    @NonNull
    public static Rect getOpticalBounds(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 29) {
            final Insets insets = Api29Impl.getOpticalInsets(drawable);
            return new Rect(
                    insets.left,
                    insets.top,
                    insets.right,
                    insets.bottom
            );
        } else if (Build.VERSION.SDK_INT >= 18) {
            return Api18Impl.getOpticalInsets(DrawableCompat.unwrap(drawable));
        }

        // If we reach here, either we're running on a device pre-v18, the Drawable didn't have
        // any optical insets, or a reflection issue, so we'll just return an empty rect.
        return INSETS_NONE;
    }

    /**
     * Attempt the fix any issues in the given drawable, usually caused by platform bugs in the
     * implementation. This method should be call after retrieval from
     * {@link Resources} or a {@link TypedArray}.
     */
    static void fixDrawable(@NonNull Drawable drawable) {
        String className = drawable.getClass().getName();
        if (Build.VERSION.SDK_INT == 21
                && "android.graphics.drawable.VectorDrawable".equals(className)) {
            // VectorDrawable has an issue on API 21 where it sometimes doesn't create its tint
            // filter until a state change event has occurred.
            forceDrawableStateChange(drawable);
        } else if (Build.VERSION.SDK_INT >= 29 && Build.VERSION.SDK_INT < 31
                && "android.graphics.drawable.ColorStateListDrawable".equals(className)) {
            // ColorStateListDrawable has an issue on APIs 29 and 30 where it doesn't set up the
            // default color until a state change event has occurred.
            forceDrawableStateChange(drawable);
        }
    }

    /**
     * Some drawable implementations have problems with mutation. This method returns false if
     * there is a known issue in the given drawable's implementation.
     */
    public static boolean canSafelyMutateDrawable(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT < 15 && drawable instanceof InsetDrawable) {
            return false;
        } else if (Build.VERSION.SDK_INT < 15 && drawable instanceof GradientDrawable) {
            // GradientDrawable has a bug pre-ICS which results in mutate() resulting
            // in loss of color
            return false;
        } else if (Build.VERSION.SDK_INT < 17 && drawable instanceof LayerDrawable) {
            return false;
        }

        if (drawable instanceof DrawableContainer) {
            // If we have a DrawableContainer, let's traverse its child array
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
        } else if (drawable instanceof WrappedDrawable) {
            return canSafelyMutateDrawable(((WrappedDrawable) drawable).getWrappedDrawable());
        } else if (drawable instanceof DrawableWrapper) {
            return canSafelyMutateDrawable(((DrawableWrapper) drawable).getWrappedDrawable());
        } else if (drawable instanceof ScaleDrawable) {
            return canSafelyMutateDrawable(((ScaleDrawable) drawable).getDrawable());
        }

        return true;
    }

    /**
     * Force a drawable state change.
     */
    private static void forceDrawableStateChange(final Drawable drawable) {
        final int[] originalState = drawable.getState();
        if (originalState == null || originalState.length == 0) {
            // The drawable doesn't have a state, so set it to be checked
            drawable.setState(CHECKED_STATE_SET);
        } else {
            // Else the drawable does have a state, so clear it
            drawable.setState(EMPTY_STATE_SET);
        }
        // Now set the original state
        drawable.setState(originalState);
    }

    /**
     * Parses tint mode.
     */
    public static PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
        switch (value) {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return PorterDuff.Mode.ADD;
            default:
                return defaultMode;
        }
    }

    // Only accessible on SDK_INT >= 18 and < 29.
    @RequiresApi(18)
    static class Api18Impl {
        private static final boolean sReflectionSuccessful;
        private static final Method sGetOpticalInsets;
        private static final Field sLeft;
        private static final Field sTop;
        private static final Field sRight;
        private static final Field sBottom;

        static {
            Method getOpticalInsets = null;
            Field left = null;
            Field top = null;
            Field right = null;
            Field bottom = null;
            boolean success = false;

            try {
                Class<?> insets = Class.forName("android.graphics.Insets");
                getOpticalInsets = Drawable.class.getMethod("getOpticalInsets");
                left = insets.getField("left");
                top = insets.getField("top");
                right = insets.getField("right");
                bottom = insets.getField("bottom");
                success = true;
            } catch (NoSuchMethodException e) {
                // Not successful, null everything out.
            } catch (ClassNotFoundException e) {
                // Not successful, null everything out.
            } catch (NoSuchFieldException e) {
                // Not successful, null everything out.
            }

            if (success) {
                sGetOpticalInsets = getOpticalInsets;
                sLeft = left;
                sTop = top;
                sRight = right;
                sBottom = bottom;
                sReflectionSuccessful = true;
            } else {
                sGetOpticalInsets = null;
                sLeft = null;
                sTop = null;
                sRight = null;
                sBottom = null;
                sReflectionSuccessful = false;
            }
        }

        private Api18Impl() {
            // This class is not instantiable.
        }

        @NonNull
        static Rect getOpticalInsets(@NonNull Drawable drawable) {
            // Check the SDK_INT to avoid UncheckedReflection error.
            if (Build.VERSION.SDK_INT < 29 && sReflectionSuccessful) {
                try {
                    Object insets = sGetOpticalInsets.invoke(drawable);
                    if (insets != null) {
                        return new Rect(
                                sLeft.getInt(insets),
                                sTop.getInt(insets),
                                sRight.getInt(insets),
                                sBottom.getInt(insets)
                        );
                    }
                } catch (IllegalAccessException e) {
                    // Ignore, we'll return empty insets.
                } catch (InvocationTargetException e) {
                    // Ignore, we'll return empty insets.
                }
            }
            return DrawableUtils.INSETS_NONE;
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Insets getOpticalInsets(Drawable drawable) {
            return drawable.getOpticalInsets();
        }
    }
}
