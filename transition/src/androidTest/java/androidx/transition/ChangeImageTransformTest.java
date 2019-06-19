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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.transition.test.R;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nullable;

@LargeTest
public class ChangeImageTransformTest extends BaseTransitionTest {

    private ChangeImageTransform mChangeImageTransform;
    private Matrix mStartMatrix;
    private Matrix mEndMatrix;
    private Drawable mImage;
    private ImageView mImageView;

    @Override
    Transition createTransition() {
        mChangeImageTransform = new CaptureMatrix();
        mChangeImageTransform.setDuration(100);
        mTransition = mChangeImageTransform;
        resetListener();
        return mChangeImageTransform;
    }

    @Test
    public void testCenterToFitXY() throws Throwable {
        transformImage(ImageView.ScaleType.CENTER, ImageView.ScaleType.FIT_XY);
        verifyMatrixMatches(centerMatrix(), mStartMatrix);
        verifyMatrixMatches(fitXYMatrix(), mEndMatrix);
    }

    @Test
    public void testCenterCropToFitCenter() throws Throwable {
        transformImage(ImageView.ScaleType.CENTER_CROP, ImageView.ScaleType.FIT_CENTER);
        verifyMatrixMatches(centerCropMatrix(), mStartMatrix);
        verifyMatrixMatches(fitCenterMatrix(), mEndMatrix);
    }

    @Test
    public void testCenterInsideToFitEnd() throws Throwable {
        transformImage(ImageView.ScaleType.CENTER_INSIDE, ImageView.ScaleType.FIT_END);
        // CENTER_INSIDE and CENTER are the same when the image is smaller than the View
        verifyMatrixMatches(centerMatrix(), mStartMatrix);
        verifyMatrixMatches(fitEndMatrix(), mEndMatrix);
    }

    @Test
    public void testFitStartToCenter() throws Throwable {
        transformImage(ImageView.ScaleType.FIT_START, ImageView.ScaleType.CENTER);
        verifyMatrixMatches(fitStartMatrix(), mStartMatrix);
        verifyMatrixMatches(centerMatrix(), mEndMatrix);
    }

    @Test
    public void testNoChange() throws Throwable {
        transformImage(ImageView.ScaleType.CENTER, ImageView.ScaleType.CENTER);
        assertNull(mStartMatrix);
        assertNull(mEndMatrix);
    }

    @Test
    public void testNoAnimationForDrawableWithoutSize() throws Throwable {
        transformImage(ImageView.ScaleType.FIT_XY,
                ImageView.ScaleType.CENTER_CROP,
                new ColorDrawable(Color.WHITE),
                false,
                false,
                true);
        assertNull(mStartMatrix);
        assertNull(mEndMatrix);
    }

    @Test
    public void testNullAnimatorKeepsImagePadding() throws Throwable {
        transformImage(ImageView.ScaleType.FIT_XY, ImageView.ScaleType.FIT_XY,
                new ColorDrawable(Color.WHITE), true, true, false);
        assertEquals(mImage.getBounds().width(), mImageView.getWidth()
                - mImageView.getPaddingLeft() - mImageView.getPaddingRight());
        assertEquals(mImage.getBounds().height(), mImageView.getHeight()
                - mImageView.getPaddingTop() - mImageView.getPaddingBottom());
    }

    @Test
    public void testInterruptionKeepsCorrectScaleType() throws Throwable {
        final ImageView imageView = enterImageViewScene(ImageView.ScaleType.CENTER_INSIDE,
                null, false);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mChangeImageTransform);
                imageView.setScaleType(ImageView.ScaleType.FIT_END);
            }
        });
        waitForStart();

        // reset the transition with the listener
        createTransition();
        // start the new transition which will interrupt the previous one
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mChangeImageTransform);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        });
        waitForEnd();

        assertEquals(ImageView.ScaleType.CENTER_CROP, imageView.getScaleType());
    }

    private Matrix centerMatrix() {
        int imageWidth = mImage.getIntrinsicWidth();
        int imageViewWidth = mImageView.getWidth();
        float tx = Math.round((imageViewWidth - imageWidth) / 2f);

        int imageHeight = mImage.getIntrinsicHeight();
        int imageViewHeight = mImageView.getHeight();
        float ty = Math.round((imageViewHeight - imageHeight) / 2f);

        Matrix matrix = new Matrix();
        matrix.postTranslate(tx, ty);
        return matrix;
    }

    private Matrix fitXYMatrix() {
        int imageWidth = mImage.getIntrinsicWidth();
        int imageViewWidth = mImageView.getWidth();
        float scaleX = ((float) imageViewWidth) / imageWidth;

        int imageHeight = mImage.getIntrinsicHeight();
        int imageViewHeight = mImageView.getHeight();
        float scaleY = ((float) imageViewHeight) / imageHeight;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        return matrix;
    }

    private Matrix centerCropMatrix() {
        int imageWidth = mImage.getIntrinsicWidth();
        int imageViewWidth = mImageView.getWidth();
        float scaleX = ((float) imageViewWidth) / imageWidth;

        int imageHeight = mImage.getIntrinsicHeight();
        int imageViewHeight = mImageView.getHeight();
        float scaleY = ((float) imageViewHeight) / imageHeight;

        float maxScale = Math.max(scaleX, scaleY);

        float width = imageWidth * maxScale;
        float height = imageHeight * maxScale;
        int tx = Math.round((imageViewWidth - width) / 2f);
        int ty = Math.round((imageViewHeight - height) / 2f);

        Matrix matrix = new Matrix();
        matrix.postScale(maxScale, maxScale);
        matrix.postTranslate(tx, ty);
        return matrix;
    }

    private Matrix fitCenterMatrix() {
        int imageWidth = mImage.getIntrinsicWidth();
        int imageViewWidth = mImageView.getWidth();
        float scaleX = ((float) imageViewWidth) / imageWidth;

        int imageHeight = mImage.getIntrinsicHeight();
        int imageViewHeight = mImageView.getHeight();
        float scaleY = ((float) imageViewHeight) / imageHeight;

        float minScale = Math.min(scaleX, scaleY);

        float width = imageWidth * minScale;
        float height = imageHeight * minScale;
        float tx = (imageViewWidth - width) / 2f;
        float ty = (imageViewHeight - height) / 2f;

        Matrix matrix = new Matrix();
        matrix.postScale(minScale, minScale);
        matrix.postTranslate(tx, ty);
        return matrix;
    }

    private Matrix fitStartMatrix() {
        int imageWidth = mImage.getIntrinsicWidth();
        int imageViewWidth = mImageView.getWidth();
        float scaleX = ((float) imageViewWidth) / imageWidth;

        int imageHeight = mImage.getIntrinsicHeight();
        int imageViewHeight = mImageView.getHeight();
        float scaleY = ((float) imageViewHeight) / imageHeight;

        float minScale = Math.min(scaleX, scaleY);

        Matrix matrix = new Matrix();
        matrix.postScale(minScale, minScale);
        return matrix;
    }

    private Matrix fitEndMatrix() {
        int imageWidth = mImage.getIntrinsicWidth();
        int imageViewWidth = mImageView.getWidth();
        float scaleX = ((float) imageViewWidth) / imageWidth;

        int imageHeight = mImage.getIntrinsicHeight();
        int imageViewHeight = mImageView.getHeight();
        float scaleY = ((float) imageViewHeight) / imageHeight;

        float minScale = Math.min(scaleX, scaleY);

        float width = imageWidth * minScale;
        float height = imageHeight * minScale;
        float tx = imageViewWidth - width;
        float ty = imageViewHeight - height;

        Matrix matrix = new Matrix();
        matrix.postScale(minScale, minScale);
        matrix.postTranslate(tx, ty);
        return matrix;
    }

    private void verifyMatrixMatches(Matrix expected, Matrix matrix) {
        if (expected == null) {
            assertNull(matrix);
            return;
        }
        assertNotNull(matrix);
        float[] expectedValues = new float[9];
        expected.getValues(expectedValues);

        float[] values = new float[9];
        matrix.getValues(values);

        for (int i = 0; i < values.length; i++) {
            final float expectedValue = expectedValues[i];
            final float value = values[i];
            assertEquals("Value [" + i + "]", expectedValue, value, 0.01f);
        }
    }

    private void transformImage(final ImageView.ScaleType startScale,
            final ImageView.ScaleType endScale) throws Throwable {
        transformImage(startScale, endScale, null, false, false, startScale == endScale);
    }

    private void transformImage(final ImageView.ScaleType startScale,
            final ImageView.ScaleType endScale,
            @Nullable final Drawable customImage,
            final boolean applyPadding,
            final boolean withChangingSize,
            final boolean noMatrixChangeExpected) throws Throwable {
        final ImageView imageView = enterImageViewScene(startScale, customImage, applyPadding);
        rule.runOnUiThread(() -> {
            TransitionManager.beginDelayedTransition(mRoot, mChangeImageTransform);
            if (withChangingSize) {
                imageView.getLayoutParams().height /= 2;
                imageView.requestLayout();
            }
            imageView.setScaleType(endScale);
        });
        if (noMatrixChangeExpected) {
            verify(mListener, never()).onTransitionStart(any(Transition.class));
        } else {
            waitForStart();
            waitForEnd();
        }
    }

    private ImageView enterImageViewScene(final ImageView.ScaleType scaleType,
            @Nullable final Drawable customImage,
            final boolean withPadding) throws Throwable {
        enterScene(R.layout.scene4);
        final ViewGroup container = rule.getActivity().findViewById(R.id.holder);
        final ImageView[] imageViews = new ImageView[1];
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView = new ImageView(rule.getActivity());
                if (customImage != null) {
                    mImage = customImage;
                } else {
                    mImage = ActivityCompat.getDrawable(rule.getActivity(),
                            android.R.drawable.ic_media_play);
                }
                mImageView.setImageDrawable(mImage);
                mImageView.setScaleType(scaleType);
                imageViews[0] = mImageView;
                container.addView(mImageView);
                ViewGroup.LayoutParams layoutParams = mImageView.getLayoutParams();
                DisplayMetrics metrics = rule.getActivity().getResources().getDisplayMetrics();
                float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, metrics);
                layoutParams.width = Math.round(size);
                layoutParams.height = Math.round(size * 2);
                mImageView.setLayoutParams(layoutParams);
                if (withPadding) {
                    int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                            metrics);
                    mImageView.setPadding(padding, padding, padding, padding);
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return imageViews[0];
    }

    private class CaptureMatrix extends ChangeImageTransform {

        @Override
        public Animator createAnimator(@NonNull ViewGroup sceneRoot, TransitionValues startValues,
                TransitionValues endValues) {
            Animator animator = super.createAnimator(sceneRoot, startValues, endValues);
            assertNotNull(animator);
            animator.addListener(new CaptureMatrixListener((ImageView) endValues.view));
            return animator;
        }

    }

    private class CaptureMatrixListener extends AnimatorListenerAdapter {

        private final ImageView mImageView;

        CaptureMatrixListener(ImageView view) {
            mImageView = view;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mStartMatrix = copyMatrix();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mEndMatrix = copyMatrix();
        }

        private Matrix copyMatrix() {
            Matrix matrix = getDrawMatrixCompat(mImageView);
            if (matrix != null) {
                matrix = new Matrix(matrix);
            }
            return matrix;
        }

    }

    private Matrix getDrawMatrixCompat(ImageView imageView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return imageView.getImageMatrix();
        } else {
            Canvas canvas = mock(Canvas.class);
            imageView.draw(canvas);
            ArgumentCaptor<Matrix> matrixCaptor = ArgumentCaptor.forClass(Matrix.class);
            verify(canvas, atMost(1)).concat(matrixCaptor.capture());
            return !matrixCaptor.getAllValues().isEmpty() ? matrixCaptor.getValue() : new Matrix();
        }
    }

}
