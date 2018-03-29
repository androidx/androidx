/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.transition;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.Property;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import java.lang.reflect.Field;

/**
 * Compatibility utilities for platform features of {@link View}.
 */
class ViewUtils {

    private static final ViewUtilsBase IMPL;
    private static final String TAG = "ViewUtils";

    private static Field sViewFlagsField;
    private static boolean sViewFlagsFieldFetched;
    private static final int VISIBILITY_MASK = 0x0000000C;

    static {
        if (Build.VERSION.SDK_INT >= 22) {
            IMPL = new ViewUtilsApi22();
        } else if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new ViewUtilsApi21();
        } else if (Build.VERSION.SDK_INT >= 19) {
            IMPL = new ViewUtilsApi19();
        } else {
            IMPL = new ViewUtilsBase();
        }
    }

    /**
     * A {@link Property} for animating transitionAlpha value of a View.
     */
    static final Property<View, Float> TRANSITION_ALPHA =
            new Property<View, Float>(Float.class, "translationAlpha") {

                @Override
                public Float get(View view) {
                    return getTransitionAlpha(view);
                }

                @Override
                public void set(View view, Float alpha) {
                    setTransitionAlpha(view, alpha);
                }

            };

    static final Property<View, Rect> CLIP_BOUNDS =
            new Property<View, Rect>(Rect.class, "clipBounds") {

                @Override
                public Rect get(View view) {
                    return ViewCompat.getClipBounds(view);
                }

                @Override
                public void set(View view, Rect clipBounds) {
                    ViewCompat.setClipBounds(view, clipBounds);
                }

            };

    /**
     * Backward-compatible {@link View#getOverlay()}.
     */
    static ViewOverlayImpl getOverlay(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 18) {
            return new ViewOverlayApi18(view);
        }
        return ViewOverlayApi14.createFrom(view);
    }

    /**
     * Backward-compatible {@link View#getWindowId()}.
     */
    static WindowIdImpl getWindowId(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 18) {
            return new WindowIdApi18(view);
        }
        return new WindowIdApi14(view.getWindowToken());
    }

    static void setTransitionAlpha(@NonNull View view, float alpha) {
        IMPL.setTransitionAlpha(view, alpha);
    }

    static float getTransitionAlpha(@NonNull View view) {
        return IMPL.getTransitionAlpha(view);
    }

    /**
     * This method needs to be called before an animation using {@link #setTransitionAlpha(View,
     * float)} in order to make its behavior backward-compatible.
     */
    static void saveNonTransitionAlpha(@NonNull View view) {
        IMPL.saveNonTransitionAlpha(view);
    }

    /**
     * This method needs to be called after an animation using
     * {@link #setTransitionAlpha(View, float)} if {@link #saveNonTransitionAlpha(View)} has been
     * called.
     */
    static void clearNonTransitionAlpha(@NonNull View view) {
        IMPL.clearNonTransitionAlpha(view);
    }

    /**
     * Copy of a hidden platform method, View#setTransitionVisibility.
     *
     * <p>Change the visibility of the View without triggering any other changes. This is
     * important for transitions, where visibility changes should not adjust focus or
     * trigger a new layout. This is only used when the visibility has already been changed
     * and we need a transient value during an animation. When the animation completes,
     * the original visibility value is always restored.</p>
     *
     * @param view       The target view.
     * @param visibility One of {@link View#VISIBLE}, {@link View#INVISIBLE}, or
     *                   {@link View#GONE}.
     */
    static void setTransitionVisibility(@NonNull View view, int visibility) {
        fetchViewFlagsField();
        if (sViewFlagsField != null) {
            try {
                int viewFlags = sViewFlagsField.getInt(view);
                sViewFlagsField.setInt(view, (viewFlags & ~VISIBILITY_MASK) | visibility);
            } catch (IllegalAccessException e) {
                // Do nothing
            }
        }
    }

    /**
     * Modifies the input matrix such that it maps view-local coordinates to
     * on-screen coordinates.
     *
     * <p>On API Level 21 and above, this includes transformation matrix applied to {@code
     * ViewRootImpl}, but not on older platforms. This difference is balanced out by the
     * implementation difference in other related platform APIs and their backport, such as
     * GhostView.</p>
     *
     * @param view   target view
     * @param matrix input matrix to modify
     */
    static void transformMatrixToGlobal(@NonNull View view, @NonNull Matrix matrix) {
        IMPL.transformMatrixToGlobal(view, matrix);
    }

    /**
     * Modifies the input matrix such that it maps on-screen coordinates to
     * view-local coordinates.
     *
     * <p>On API Level 21 and above, this includes transformation matrix applied to {@code
     * ViewRootImpl}, but not on older platforms. This difference is balanced out by the
     * implementation difference in other related platform APIs and their backport, such as
     * GhostView.</p>
     *
     * @param view   target view
     * @param matrix input matrix to modify
     */
    static void transformMatrixToLocal(@NonNull View view, @NonNull Matrix matrix) {
        IMPL.transformMatrixToLocal(view, matrix);
    }

    /**
     * Sets the transformation matrix for animation.
     *
     * @param v The view
     * @param m The matrix
     */
    static void setAnimationMatrix(@NonNull View v, @Nullable Matrix m) {
        IMPL.setAnimationMatrix(v, m);
    }

    /**
     * Assign a size and position to this view.
     *
     * @param left   Left position, relative to parent
     * @param top    Top position, relative to parent
     * @param right  Right position, relative to parent
     * @param bottom Bottom position, relative to parent
     */
    static void setLeftTopRightBottom(@NonNull View v, int left, int top, int right, int bottom) {
        IMPL.setLeftTopRightBottom(v, left, top, right, bottom);
    }

    private static void fetchViewFlagsField() {
        if (!sViewFlagsFieldFetched) {
            try {
                sViewFlagsField = View.class.getDeclaredField("mViewFlags");
                sViewFlagsField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.i(TAG, "fetchViewFlagsField: ");
            }
            sViewFlagsFieldFetched = true;
        }
    }

    private ViewUtils() {
    }
}
