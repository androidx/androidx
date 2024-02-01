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
import android.animation.AnimatorSet;
import android.animation.TypeEvaluator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.RectF;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.ImageView;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

class TransitionUtils {

    private static final int MAX_IMAGE_SIZE = 1024 * 1024;
    private static final boolean HAS_PICTURE_BITMAP =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;

    /**
     * Creates a View using the bitmap copy of <code>view</code>. If <code>view</code> is large,
     * the copy will use a scaled bitmap of the given view.
     *
     * @param sceneRoot The ViewGroup in which the view copy will be displayed.
     * @param view      The view to create a copy of.
     * @param parent    The parent of view.
     */
    static View copyViewImage(ViewGroup sceneRoot, View view, View parent) {
        Matrix matrix = new Matrix();
        matrix.setTranslate(-parent.getScrollX(), -parent.getScrollY());
        ViewUtils.transformMatrixToGlobal(view, matrix);
        ViewUtils.transformMatrixToLocal(sceneRoot, matrix);
        RectF bounds = new RectF(0, 0, view.getWidth(), view.getHeight());
        matrix.mapRect(bounds);
        int left = Math.round(bounds.left);
        int top = Math.round(bounds.top);
        int right = Math.round(bounds.right);
        int bottom = Math.round(bounds.bottom);

        ImageView copy = new ImageView(view.getContext());
        copy.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bitmap = createViewBitmap(view, matrix, bounds, sceneRoot);
        if (bitmap != null) {
            copy.setImageBitmap(bitmap);
        }
        int widthSpec = View.MeasureSpec.makeMeasureSpec(right - left, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(bottom - top, View.MeasureSpec.EXACTLY);
        copy.measure(widthSpec, heightSpec);
        copy.layout(left, top, right, bottom);
        return copy;
    }

    /**
     * Creates a Bitmap of the given view, using the Matrix matrix to transform to the local
     * coordinates. <code>matrix</code> will be modified during the bitmap creation.
     *
     * <p>If the bitmap is large, it will be scaled uniformly down to at most 1MB size.</p>
     *
     * @param view   The view to create a bitmap for.
     * @param matrix The matrix converting the view local coordinates to the coordinates that
     *               the bitmap will be displayed in. <code>matrix</code> will be modified before
     *               returning.
     * @param bounds The bounds of the bitmap in the destination coordinate system (where the
     *               view should be presented. Typically, this is matrix.mapRect(viewBounds);
     * @return A bitmap of the given view or null if bounds has no width or height.
     */
    private static Bitmap createViewBitmap(View view, Matrix matrix, RectF bounds,
            ViewGroup sceneRoot) {
        final boolean addToOverlay;
        final boolean sceneRootIsAttached;
        addToOverlay = !view.isAttachedToWindow();
        sceneRootIsAttached = sceneRoot != null && sceneRoot.isAttachedToWindow();
        ViewGroup parent = null;
        int indexInParent = 0;
        if (addToOverlay) {
            if (!sceneRootIsAttached) {
                return null;
            }
            parent = (ViewGroup) view.getParent();
            indexInParent = parent.indexOfChild(view);
            ViewGroupOverlay result = sceneRoot.getOverlay();
            result.add(view);
        }
        Bitmap bitmap = null;
        int bitmapWidth = Math.round(bounds.width());
        int bitmapHeight = Math.round(bounds.height());
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            float scale = Math.min(1f, ((float) MAX_IMAGE_SIZE) / (bitmapWidth * bitmapHeight));
            bitmapWidth = Math.round(bitmapWidth * scale);
            bitmapHeight = Math.round(bitmapHeight * scale);
            matrix.postTranslate(-bounds.left, -bounds.top);
            matrix.postScale(scale, scale);

            if (HAS_PICTURE_BITMAP) {
                // Hardware rendering
                final Picture picture = new Picture();
                final Canvas canvas = picture.beginRecording(bitmapWidth, bitmapHeight);
                canvas.concat(matrix);
                view.draw(canvas);
                picture.endRecording();
                bitmap = Api28Impl.createBitmap(picture);
            } else {
                // Software rendering
                bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.concat(matrix);
                view.draw(canvas);
            }
        }
        if (addToOverlay) {
            ViewGroupOverlay result = sceneRoot.getOverlay();
            result.remove(view);
            parent.addView(view, indexInParent);
        }
        return bitmap;
    }

    static Animator mergeAnimators(Animator animator1, Animator animator2) {
        if (animator1 == null) {
            return animator2;
        } else if (animator2 == null) {
            return animator1;
        } else {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animator1, animator2);
            return animatorSet;
        }
    }

    static class MatrixEvaluator implements TypeEvaluator<Matrix> {

        final float[] mTempStartValues = new float[9];

        final float[] mTempEndValues = new float[9];

        final Matrix mTempMatrix = new Matrix();

        @Override
        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            startValue.getValues(mTempStartValues);
            endValue.getValues(mTempEndValues);
            for (int i = 0; i < 9; i++) {
                float diff = mTempEndValues[i] - mTempStartValues[i];
                mTempEndValues[i] = mTempStartValues[i] + (fraction * diff);
            }
            mTempMatrix.setValues(mTempEndValues);
            return mTempMatrix;
        }

    }

    private TransitionUtils() { }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Bitmap createBitmap(Picture source) {
            return Bitmap.createBitmap(source);
        }

    }
}
