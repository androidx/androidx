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
import android.animation.AnimatorListenerAdapter;
import android.graphics.Matrix;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;

@RequiresApi(14)
class ImageViewUtilsApi14 implements ImageViewUtilsImpl {

    @Override
    public void startAnimateTransform(ImageView view) {
        final ImageView.ScaleType scaleType = view.getScaleType();
        view.setTag(R.id.save_scale_type, scaleType);
        if (scaleType == ImageView.ScaleType.MATRIX) {
            view.setTag(R.id.save_image_matrix, view.getImageMatrix());
        } else {
            view.setScaleType(ImageView.ScaleType.MATRIX);
        }
        view.setImageMatrix(MatrixUtils.IDENTITY_MATRIX);
    }

    @Override
    public void animateTransform(ImageView view, Matrix matrix) {
        view.setImageMatrix(matrix);
    }

    @Override
    public void reserveEndAnimateTransform(final ImageView view, Animator animator) {
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
