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
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * This Transition captures an ImageView's matrix before and after the
 * scene change and animates it during the transition.
 *
 * <p>In combination with ChangeBounds, ChangeImageTransform allows ImageViews
 * that change size, shape, or {@link android.widget.ImageView.ScaleType} to animate contents
 * smoothly.</p>
 */
public class ChangeImageTransform extends Transition {

    private static final String PROPNAME_MATRIX = "android:changeImageTransform:matrix";
    private static final String PROPNAME_BOUNDS = "android:changeImageTransform:bounds";

    private static final String[] sTransitionProperties = {
            PROPNAME_MATRIX,
            PROPNAME_BOUNDS,
    };

    private static final TypeEvaluator<Matrix> NULL_MATRIX_EVALUATOR = new TypeEvaluator<Matrix>() {
        @Override
        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            return null;
        }
    };

    private static final Property<ImageView, Matrix> ANIMATED_TRANSFORM_PROPERTY =
            new Property<ImageView, Matrix>(Matrix.class, "animatedTransform") {
                @Override
                public void set(ImageView view, Matrix matrix) {
                    ImageViewUtils.animateTransform(view, matrix);
                }

                @Override
                public Matrix get(ImageView object) {
                    return null;
                }
            };

    public ChangeImageTransform() {
    }

    public ChangeImageTransform(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean isSeekingSupported() {
        return true;
    }

    private void captureValues(TransitionValues transitionValues, boolean useIntermediate) {
        View view = transitionValues.view;
        if (!(view instanceof ImageView) || view.getVisibility() != View.VISIBLE) {
            return;
        }
        ImageView imageView = (ImageView) view;
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) {
            return;
        }
        Map<String, Object> values = transitionValues.values;

        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();

        Rect bounds = new Rect(left, top, right, bottom);
        values.put(PROPNAME_BOUNDS, bounds);
        Matrix matrix = null;
        if (useIntermediate) {
            matrix = (Matrix) imageView.getTag(R.id.transition_image_transform);
        }
        if (matrix == null) {
            matrix = copyImageMatrix(imageView);
        }
        values.put(PROPNAME_MATRIX, matrix);
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues, true);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues, false);
    }

    @Override
    @NonNull
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    /**
     * Creates an Animator for ImageViews moving, changing dimensions, and/or changing
     * {@link android.widget.ImageView.ScaleType}.
     *
     * @param sceneRoot   The root of the transition hierarchy.
     * @param startValues The values for a specific target in the start scene.
     * @param endValues   The values for the target in the end scene.
     * @return An Animator to move an ImageView or null if the View is not an ImageView,
     * the Drawable changed, the View is not VISIBLE, or there was no change.
     */
    @Nullable
    @Override
    public Animator createAnimator(@NonNull ViewGroup sceneRoot,
            @Nullable TransitionValues startValues,
            final @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Rect startBounds = (Rect) startValues.values.get(PROPNAME_BOUNDS);
        Rect endBounds = (Rect) endValues.values.get(PROPNAME_BOUNDS);
        if (startBounds == null || endBounds == null) {
            return null;
        }

        Matrix startMatrix = (Matrix) startValues.values.get(PROPNAME_MATRIX);
        Matrix endMatrix = (Matrix) endValues.values.get(PROPNAME_MATRIX);

        boolean matricesEqual = (startMatrix == null && endMatrix == null)
                || (startMatrix != null && startMatrix.equals(endMatrix));

        if (startBounds.equals(endBounds) && matricesEqual) {
            return null;
        }

        final ImageView imageView = (ImageView) endValues.view;
        Drawable drawable = imageView.getDrawable();
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        ObjectAnimator animator;
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            animator = createNullAnimator(imageView);
        } else {
            if (startMatrix == null) {
                startMatrix = MatrixUtils.IDENTITY_MATRIX;
            }
            if (endMatrix == null) {
                endMatrix = MatrixUtils.IDENTITY_MATRIX;
            }
            ANIMATED_TRANSFORM_PROPERTY.set(imageView, startMatrix);
            animator = createMatrixAnimator(imageView, startMatrix, endMatrix);
            Listener listener = new Listener(imageView, startMatrix, endMatrix);
            animator.addListener(listener);
            AnimatorUtils.addPauseListener(animator, listener);
            addListener(listener);
        }

        return animator;
    }

    @NonNull
    private ObjectAnimator createNullAnimator(@NonNull ImageView imageView) {
        return ObjectAnimator.ofObject(imageView, ANIMATED_TRANSFORM_PROPERTY,
                NULL_MATRIX_EVALUATOR, MatrixUtils.IDENTITY_MATRIX, MatrixUtils.IDENTITY_MATRIX);
    }

    private ObjectAnimator createMatrixAnimator(final ImageView imageView, Matrix startMatrix,
            final Matrix endMatrix) {
        return ObjectAnimator.ofObject(imageView, ANIMATED_TRANSFORM_PROPERTY,
                new TransitionUtils.MatrixEvaluator(), startMatrix, endMatrix);
    }

    @NonNull
    private static Matrix copyImageMatrix(@NonNull ImageView view) {
        final Drawable image = view.getDrawable();
        if (image.getIntrinsicWidth() > 0 && image.getIntrinsicHeight() > 0) {
            switch (view.getScaleType()) {
                case FIT_XY:
                    return fitXYMatrix(view);
                case CENTER_CROP:
                    return centerCropMatrix(view);
                default:
                    return new Matrix(view.getImageMatrix());
            }
        }
        return new Matrix(view.getImageMatrix());
    }

    /**
     * Calculates the image transformation matrix for an ImageView with ScaleType FIT_XY. This
     * needs to be manually calculated as the platform does not give us the value for this case.
     */
    private static Matrix fitXYMatrix(ImageView view) {
        final Drawable image = view.getDrawable();
        final Matrix matrix = new Matrix();
        matrix.postScale(
                ((float) view.getWidth()) / image.getIntrinsicWidth(),
                ((float) view.getHeight()) / image.getIntrinsicHeight());
        return matrix;
    }

    /**
     * Calculates the image transformation matrix for an ImageView with ScaleType CENTER_CROP. This
     * needs to be manually calculated for consistent behavior across all the API levels.
     */
    private static Matrix centerCropMatrix(ImageView view) {
        final Drawable image = view.getDrawable();
        final int imageWidth = image.getIntrinsicWidth();
        final int imageViewWidth = view.getWidth();
        final float scaleX = ((float) imageViewWidth) / imageWidth;

        final int imageHeight = image.getIntrinsicHeight();
        final int imageViewHeight = view.getHeight();
        final float scaleY = ((float) imageViewHeight) / imageHeight;

        final float maxScale = Math.max(scaleX, scaleY);

        final float width = imageWidth * maxScale;
        final float height = imageHeight * maxScale;
        final int tx = Math.round((imageViewWidth - width) / 2f);
        final int ty = Math.round((imageViewHeight - height) / 2f);

        final Matrix matrix = new Matrix();
        matrix.postScale(maxScale, maxScale);
        matrix.postTranslate(tx, ty);
        return matrix;
    }

    private static class Listener extends AnimatorListenerAdapter implements TransitionListener {
        private final ImageView mImageView;
        private final Matrix mStartMatrix;
        private final Matrix mEndMatrix;
        private boolean mIsBeforeAnimator = true;

        Listener(ImageView imageView, Matrix startMatrix, Matrix endMatrix) {
            mImageView = imageView;
            mStartMatrix = startMatrix;
            mEndMatrix = endMatrix;
        }

        @Override
        public void onTransitionStart(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionCancel(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionPause(@NonNull Transition transition) {
            if (mIsBeforeAnimator) {
                saveMatrix(mStartMatrix);
            }
        }

        @Override
        public void onTransitionResume(@NonNull Transition transition) {
            restoreMatrix();
        }

        @Override
        public void onAnimationStart(@NonNull Animator animation, boolean isReverse) {
            mIsBeforeAnimator = false;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mIsBeforeAnimator = false;
        }

        @Override
        public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
            mIsBeforeAnimator = isReverse;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mIsBeforeAnimator = false;
        }

        @Override
        public void onAnimationPause(Animator animation) {
            Matrix pauseMatrix = (Matrix) ((ObjectAnimator) animation).getAnimatedValue();
            saveMatrix(pauseMatrix);
        }

        @Override
        public void onAnimationResume(Animator animation) {
            restoreMatrix();
        }

        private void restoreMatrix() {
            Matrix pauseMatrix = (Matrix) mImageView.getTag(R.id.transition_image_transform);
            if (pauseMatrix != null) {
                ImageViewUtils.animateTransform(mImageView, pauseMatrix);
                mImageView.setTag(R.id.transition_image_transform, null);
            }
        }

        private void saveMatrix(Matrix pauseMatrix) {
            mImageView.setTag(R.id.transition_image_transform, pauseMatrix);
            ImageViewUtils.animateTransform(mImageView, mEndMatrix);
        }
    }
}
