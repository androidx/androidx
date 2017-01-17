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

package android.support.transition;

import android.graphics.Matrix;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Property;
import android.view.View;
import android.view.ViewParent;

/**
 * Compatibility utilities for platform features of {@link View}.
 */
class ViewUtils {

    private static final ViewUtilsImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 19) {
            IMPL = new ViewUtilsApi19();
        } else if (Build.VERSION.SDK_INT >= 18) {
            IMPL = new ViewUtilsApi18();
        } else {
            IMPL = new ViewUtilsApi14();
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

    /**
     * Backward-compatible {@link View#getOverlay()}.
     */
    static ViewOverlayImpl getOverlay(@NonNull View view) {
        return IMPL.getOverlay(view);
    }

    /**
     * Backward-compatible {@link View#getWindowId()}.
     */
    static WindowIdImpl getWindowId(@NonNull View view) {
        return IMPL.getWindowId(view);
    }

    static void setTransitionAlpha(@NonNull View view, float alpha) {
        IMPL.setTransitionAlpha(view, alpha);
    }

    static float getTransitionAlpha(@NonNull View view) {
        return IMPL.getTransitionAlpha(view);
    }

    /**
     * Modifies the input matrix such that it maps view-local coordinates to
     * on-screen coordinates.
     *
     * @param view target view
     * @param matrix input matrix to modify
     */
    static void transformMatrixToGlobal(@NonNull View view, @NonNull Matrix matrix) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View) {
            final View vp = (View) parent;
            transformMatrixToGlobal(vp, matrix);
            matrix.preTranslate(-vp.getScrollX(), -vp.getScrollY());
        }
        matrix.preTranslate(view.getLeft(), view.getTop());
        final Matrix vm = view.getMatrix();
        if (!vm.isIdentity()) {
            matrix.preConcat(vm);
        }
    }

    /**
     * Modifies the input matrix such that it maps on-screen coordinates to
     * view-local coordinates.
     *
     * @param view target view
     * @param matrix input matrix to modify
     */
    static void transformMatrixToLocal(@NonNull View view, @NonNull Matrix matrix) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View) {
            final View vp = (View) parent;
            transformMatrixToLocal(vp, matrix);
            matrix.postTranslate(vp.getScrollX(), vp.getScrollY());
        }
        matrix.postTranslate(view.getLeft(), view.getTop());
        final Matrix vm = view.getMatrix();
        if (!vm.isIdentity()) {
            final Matrix inverted = new Matrix();
            if (vm.invert(inverted)) {
                matrix.postConcat(inverted);
            }
        }
    }

}
