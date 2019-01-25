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

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ImageViewUtils {
    private static final String TAG = "ImageViewUtils";

    private static Method sAnimateTransformMethod;
    private static boolean sAnimateTransformMethodFetched;

    private static Field sDrawMatrixField;
    private static boolean sDrawMatrixFieldFetched;

    /**
     * Sets the matrix to animate the content of the image view.
     */
    static void animateTransform(ImageView view, Matrix matrix) {
        if (matrix == null) {
            // There is a bug in ImageView.animateTransform() prior to the current development
            // version of Android so paddings are ignored when matrix is null.
            Drawable drawable = view.getDrawable();
            if (drawable != null) {
                int vwidth = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
                int vheight = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
                drawable.setBounds(0, 0, vwidth, vheight);
                view.invalidate();
            }
        } else if (Build.VERSION.SDK_INT >= 21) {
            fetchAnimateTransformMethod();
            if (sAnimateTransformMethod != null) {
                try {
                    sAnimateTransformMethod.invoke(view, matrix);
                } catch (IllegalAccessException e) {
                    // Do nothing
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
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

    private static void fetchAnimateTransformMethod() {
        if (!sAnimateTransformMethodFetched) {
            try {
                sAnimateTransformMethod = ImageView.class.getDeclaredMethod("animateTransform",
                        Matrix.class);
                sAnimateTransformMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve animateTransform method", e);
            }
            sAnimateTransformMethodFetched = true;
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
