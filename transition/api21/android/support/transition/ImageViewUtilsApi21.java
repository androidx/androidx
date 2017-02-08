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
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.ImageView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RequiresApi(21)
class ImageViewUtilsApi21 implements ImageViewUtilsImpl {

    private static final String TAG = "ImageViewUtilsApi21";

    private static Method sAnimateTransformMethod;
    private static boolean sAnimateTransformMethodFetched;

    @Override
    public void startAnimateTransform(ImageView view) {
        // Do nothing
    }

    @Override
    public void animateTransform(ImageView view, Matrix matrix) {
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

    @Override
    public void reserveEndAnimateTransform(ImageView view, Animator animator) {
        // Do nothing
    }

    private void fetchAnimateTransformMethod() {
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

}
