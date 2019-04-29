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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;

class ImageViewUtils {

    /**
     * False when linking of the hidden animateTransform method has previously failed.
     */
    private static boolean sTryHiddenAnimateTransform = true;

    private static Field sDrawMatrixField;
    private static boolean sDrawMatrixFieldFetched;

    /**
     * Sets the matrix to animate the content of the image view.
     */
    @SuppressLint("NewApi") // TODO: Remove this suppression once Q SDK is released.
    static void animateTransform(@NonNull ImageView view, @Nullable Matrix matrix) {
        if (Build.VERSION.SDK_INT >= 29) {
            view.animateTransform(matrix);
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
        } else if (Build.VERSION.SDK_INT >= 21) {
            hiddenAnimateTransform(view, matrix);
        } else {
            Drawable drawable = view.getDrawable();
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight());
                Matrix drawMatrix = null;
                fetchDrawMatrixField();
                if (sDrawMatrixField != null) {
                    try {
                        drawMatrix = (Matrix) sDrawMatrixField.get(view);
                        if (drawMatrix == null) {
                            drawMatrix = new Matrix();
                            sDrawMatrixField.set(view, drawMatrix);
                        }
                    } catch (IllegalAccessException ignore) {
                        // Do nothing
                    }
                }
                if (drawMatrix != null) {
                    drawMatrix.set(matrix);
                }
                view.invalidate();
            }
        }
    }

    @RequiresApi(21)
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    private static void hiddenAnimateTransform(@NonNull ImageView view, @Nullable Matrix matrix) {
        if (sTryHiddenAnimateTransform) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                view.animateTransform(matrix);
            } catch (NoSuchMethodError e) {
                sTryHiddenAnimateTransform = false;
            }
        }
    }

    private static void fetchDrawMatrixField() {
        if (!sDrawMatrixFieldFetched) {
            try {
                sDrawMatrixField = ImageView.class.getDeclaredField("mDrawMatrix");
                sDrawMatrixField.setAccessible(true);
            } catch (NoSuchFieldException ignore) {
                // Do nothing
            }
            sDrawMatrixFieldFetched = true;
        }
    }

    private ImageViewUtils() {
    }
}
