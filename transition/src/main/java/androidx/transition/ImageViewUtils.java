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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Matrix;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ImageViewUtils {
    private static final String TAG = "ImageViewUtils";

    private static Method sAnimateTransformMethod;
    private static boolean sAnimateTransformMethodFetched;

    /**
     * Starts animating the transformation of the image view. This has to be called before calling
     * {@link #animateTransform(ImageView, Matrix)}.
     */
    static void startAnimateTransform(ImageView view) {
        if (Build.VERSION.SDK_INT < 21) {
            final ImageView.ScaleType scaleType = view.getScaleType();
            view.setTag(R.id.save_scale_type, scaleType);
            if (scaleType == ImageView.ScaleType.MATRIX) {
                view.setTag(R.id.save_image_matrix, view.getImageMatrix());
            } else {
                view.setScaleType(ImageView.ScaleType.MATRIX);
            }
            view.setImageMatrix(MatrixUtils.IDENTITY_MATRIX);
        }
    }

    /**
     * Sets the matrix to animate the content of the image view.
     */
    static void animateTransform(ImageView view, Matrix matrix) {
        if (Build.VERSION.SDK_INT < 21) {
            view.setImageMatrix(matrix);
        } else {
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

    /**
     * Reserves that the caller will stop calling {@link #animateTransform(ImageView, Matrix)} when
     * the specified animator ends.
     */
    static void reserveEndAnimateTransform(final ImageView view, Animator animator) {
        if (Build.VERSION.SDK_INT < 21) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    final ImageView.ScaleType scaleType = (ImageView.ScaleType)
                            view.getTag(R.id.save_scale_type);
                    view.setScaleType(scaleType);
                    view.setTag(R.id.save_scale_type, null);
                    if (scaleType == ImageView.ScaleType.MATRIX) {
                        view.setImageMatrix((Matrix) view.getTag(R.id.save_image_matrix));
                        view.setTag(R.id.save_image_matrix, null);
                    }
                    animation.removeListener(this);
                }
            });
        }
    }

    private ImageViewUtils() {
    }
}
