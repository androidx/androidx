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

package androidx.transition;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

class ImageViewUtils {

    /**
     * False when linking of the hidden animateTransform method has previously failed.
     */
    private static boolean sTryHiddenAnimateTransform = true;

    /**
     * Sets the matrix to animate the content of the image view.
     */
    static void animateTransform(@NonNull ImageView view, @Nullable Matrix matrix) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.animateTransform(view, matrix);
        } else if (matrix == null) {
            // There is a bug in ImageView.animateTransform() prior to Q so paddings are
            // ignored when matrix is null.
            Drawable drawable = view.getDrawable();
            if (drawable != null) {
                int vwidth = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
                int vheight = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
                drawable.setBounds(0, 0, vwidth, vheight);
                view.invalidate();
            }
        } else {
            hiddenAnimateTransform(view, matrix);
        }
    }

    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    private static void hiddenAnimateTransform(@NonNull ImageView view, @Nullable Matrix matrix) {
        if (sTryHiddenAnimateTransform) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                Api29Impl.animateTransform(view, matrix);
            } catch (NoSuchMethodError e) {
                sTryHiddenAnimateTransform = false;
            }
        }
    }

    private ImageViewUtils() { }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static void animateTransform(ImageView imageView, Matrix matrix) {
            imageView.animateTransform(matrix);
        }
    }
}
