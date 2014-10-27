/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v4.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import java.util.List;
import java.util.Map;

/**
 * Listener provided in
 * {@link FragmentActivity#setEnterSharedElementCallback(SharedElementCallback)} and
 * {@link FragmentActivity#setExitSharedElementCallback(SharedElementCallback)}
 * to monitor the Activity transitions. The events can be used to customize Activity
 * Transition behavior.
 */
public abstract class SharedElementCallback {
    private Matrix mTempMatrix;
    private static int MAX_IMAGE_SIZE = (1024 * 1024);
    private static final String BUNDLE_SNAPSHOT_BITMAP = "sharedElement:snapshot:bitmap";
    private static final String BUNDLE_SNAPSHOT_IMAGE_SCALETYPE = "sharedElement:snapshot:imageScaleType";
    private static final String BUNDLE_SNAPSHOT_IMAGE_MATRIX = "sharedElement:snapshot:imageMatrix";

    /**
     * Called immediately after the start state is set for the shared element.
     * The shared element will start at the size and position of the shared element
     * in the launching Activity or Fragment.
     *
     * @param sharedElementNames The names of the shared elements that were accepted into
     *                           the View hierarchy.
     * @param sharedElements The shared elements that are part of the View hierarchy.
     * @param sharedElementSnapshots The Views containing snap shots of the shared element
     *                               from the launching Window. These elements will not
     *                               be part of the scene, but will be positioned relative
     *                               to the Window decor View. This list is null for Fragment
     *                               Transitions.
     */
    public void onSharedElementStart(List<String> sharedElementNames,
            List<View> sharedElements, List<View> sharedElementSnapshots) {}

    /**
     * Called after the end state is set for the shared element, but before the end state
     * is captured by the shared element transition.
     * <p>
     *     Any customization done in
     *     {@link #onSharedElementStart(java.util.List, java.util.List, java.util.List)}
     *     may need to be modified to the final state of the shared element if it is not
     *     automatically corrected by layout. For example, rotation or scale will not
     *     be affected by layout and if changed in {@link #onSharedElementStart(java.util.List,
     *     java.util.List, java.util.List)}, it will also have to be set here again to correct
     *     the end state.
     * </p>
     *
     * @param sharedElementNames The names of the shared elements that were accepted into
     *                           the View hierarchy.
     * @param sharedElements The shared elements that are part of the View hierarchy.
     * @param sharedElementSnapshots The Views containing snap shots of the shared element
     *                               from the launching Window. These elements will not
     *                               be part of the scene, but will be positioned relative
     *                               to the Window decor View. This list will be null for
     *                               Fragment Transitions.
     */
    public void onSharedElementEnd(List<String> sharedElementNames,
            List<View> sharedElements, List<View> sharedElementSnapshots) {}

    /**
     * Called after {@link #onMapSharedElements(java.util.List, java.util.Map)} when
     * transferring shared elements in. Any shared elements that have no mapping will be in
     * <var>rejectedSharedElements</var>. The elements remaining in
     * <var>rejectedSharedElements</var> will be transitioned out of the Scene. If a
     * View is removed from <var>rejectedSharedElements</var>, it must be handled by the
     * <code>SharedElementListener</code>.
     * <p>
     * Views in rejectedSharedElements will have their position and size set to the
     * position of the calling shared element, relative to the Window decor View and contain
     * snapshots of the View from the calling Activity or Fragment. This
     * view may be safely added to the decor View's overlay to remain in position.
     * </p>
     * <p>This method is not called for Fragment Transitions. All rejected shared elements
     * will be handled by the exit transition.</p>
     *
     * @param rejectedSharedElements Views containing visual information of shared elements
     *                               that are not part of the entering scene. These Views
     *                               are positioned relative to the Window decor View. A
     *                               View removed from this list will not be transitioned
     *                               automatically.
     */
    public void onRejectSharedElements(List<View> rejectedSharedElements) {}

    /**
     * Lets the SharedElementCallback adjust the mapping of shared element names to
     * Views.
     *
     * @param names The names of all shared elements transferred from the calling Activity
     *              or Fragment in the order they were provided.
     * @param sharedElements The mapping of shared element names to Views. The best guess
     *                       will be filled into sharedElements based on the transitionNames.
     */
    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {}


    /**
     * Creates a snapshot of a shared element to be used by the remote Activity and reconstituted
     * with {@link #onCreateSnapshotView(android.content.Context, android.os.Parcelable)}. A
     * null return value will mean that the remote Activity will have a null snapshot View in
     * {@link #onSharedElementStart(java.util.List, java.util.List, java.util.List)} and
     * {@link #onSharedElementEnd(java.util.List, java.util.List, java.util.List)}.
     *
     * <p>This is not called for Fragment Transitions.</p>
     *
     * @param sharedElement The shared element View to create a snapshot for.
     * @param viewToGlobalMatrix A matrix containing a transform from the view to the screen
     *                           coordinates.
     * @param screenBounds The bounds of shared element in screen coordinate space. This is
     *                     the bounds of the view with the viewToGlobalMatrix applied.
     * @return A snapshot to send to the remote Activity to be reconstituted with
     * {@link #onCreateSnapshotView(android.content.Context, android.os.Parcelable)} and passed
     * into {@link #onSharedElementStart(java.util.List, java.util.List, java.util.List)} and
     * {@link #onSharedElementEnd(java.util.List, java.util.List, java.util.List)}.
     */
    public Parcelable onCaptureSharedElementSnapshot(View sharedElement, Matrix viewToGlobalMatrix,
            RectF screenBounds) {
        if (sharedElement instanceof ImageView) {
            ImageView imageView = ((ImageView) sharedElement);
            Drawable d = imageView.getDrawable();
            Drawable bg = imageView.getBackground();
            if (d != null && bg == null) {
                Bitmap bitmap = createDrawableBitmap(d);
                if (bitmap != null) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(BUNDLE_SNAPSHOT_BITMAP, bitmap);
                    bundle.putString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE,
                            imageView.getScaleType().toString());
                    if (imageView.getScaleType() == ScaleType.MATRIX) {
                        Matrix matrix = imageView.getImageMatrix();
                        float[] values = new float[9];
                        matrix.getValues(values);
                        bundle.putFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX, values);
                    }
                    return bundle;
                }
            }
        }
        int bitmapWidth = Math.round(screenBounds.width());
        int bitmapHeight = Math.round(screenBounds.height());
        Bitmap bitmap = null;
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            float scale = Math.min(1f, ((float)MAX_IMAGE_SIZE) / (bitmapWidth * bitmapHeight));
            bitmapWidth *= scale;
            bitmapHeight *= scale;
            if (mTempMatrix == null) {
                mTempMatrix = new Matrix();
            }
            mTempMatrix.set(viewToGlobalMatrix);
            mTempMatrix.postTranslate(-screenBounds.left, -screenBounds.top);
            mTempMatrix.postScale(scale, scale);
            bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.concat(mTempMatrix);
            sharedElement.draw(canvas);
        }
        return bitmap;
    }

    /**
     * Get a copy of bitmap of given drawable.
     */
    private static Bitmap createDrawableBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        float scale = Math.min(1f, ((float)MAX_IMAGE_SIZE) / (width * height));
        if (drawable instanceof BitmapDrawable && scale == 1f) {
            // return same bitmap if scale down not needed
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int bitmapWidth = (int) (width * scale);
        int bitmapHeight = (int) (height * scale);
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Rect existingBounds = drawable.getBounds();
        int left = existingBounds.left;
        int top = existingBounds.top;
        int right = existingBounds.right;
        int bottom = existingBounds.bottom;
        drawable.setBounds(0, 0, bitmapWidth, bitmapHeight);
        drawable.draw(canvas);
        drawable.setBounds(left, top, right, bottom);
        return bitmap;
    }

    /**
     * Reconstitutes a snapshot View from a Parcelable returned in
     * {@link #onCaptureSharedElementSnapshot(android.view.View, android.graphics.Matrix,
     * android.graphics.RectF)} to be used in {@link #onSharedElementStart(java.util.List,
     * java.util.List, java.util.List)} and {@link #onSharedElementEnd(java.util.List,
     * java.util.List, java.util.List)}. The returned View will be sized and positioned after
     * this call so that it is ready to be added to the decor View's overlay.
     *
     * <p>This is not called for Fragment Transitions.</p>
     *
     * @param context The Context used to create the snapshot View.
     * @param snapshot The Parcelable returned by {@link #onCaptureSharedElementSnapshot(
     * android.view.View, android.graphics.Matrix, android.graphics.RectF)}.
     * @return A View to be sent in {@link #onSharedElementStart(java.util.List, java.util.List,
     * java.util.List)} and {@link #onSharedElementEnd(java.util.List, java.util.List,
     * java.util.List)}. A null value will produce a null snapshot value for those two methods.
     */
    public View onCreateSnapshotView(Context context, Parcelable snapshot) {
        ImageView view = null;
        if (snapshot instanceof Bundle) {
            Bundle bundle = (Bundle) snapshot;
            Bitmap bitmap = (Bitmap) bundle.getParcelable(BUNDLE_SNAPSHOT_BITMAP);
            if (bitmap == null) {
                return null;
            }
            ImageView imageView = new ImageView(context);
            view = imageView;
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(
                    ScaleType.valueOf(bundle.getString(BUNDLE_SNAPSHOT_IMAGE_SCALETYPE)));
            if (imageView.getScaleType() == ScaleType.MATRIX) {
                float[] values = bundle.getFloatArray(BUNDLE_SNAPSHOT_IMAGE_MATRIX);
                Matrix matrix = new Matrix();
                matrix.setValues(values);
                imageView.setImageMatrix(matrix);
            }
        } else if (snapshot instanceof Bitmap) {
            Bitmap bitmap = (Bitmap) snapshot;
            view = new ImageView(context);
            view.setImageBitmap(bitmap);
        }
        return view;
    }

}
