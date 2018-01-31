/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.animation.Animator;
import android.graphics.Matrix;
import android.os.Build;
import android.widget.ImageView;

class ImageViewUtils {

    private static final ImageViewUtilsImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new ImageViewUtilsApi21();
        } else {
            IMPL = new ImageViewUtilsApi14();
        }
    }

    /**
     * Starts animating the transformation of the image view. This has to be called before calling
     * {@link #animateTransform(ImageView, Matrix)}.
     */
    static void startAnimateTransform(ImageView view) {
        IMPL.startAnimateTransform(view);
    }

    /**
     * Sets the matrix to animate the content of the image view.
     */
    static void animateTransform(ImageView view, Matrix matrix) {
        IMPL.animateTransform(view, matrix);
    }

    /**
     * Reserves that the caller will stop calling {@link #animateTransform(ImageView, Matrix)} when
     * the specified animator ends.
     */
    static void reserveEndAnimateTransform(ImageView view, Animator animator) {
        IMPL.reserveEndAnimateTransform(view, animator);
    }

}
