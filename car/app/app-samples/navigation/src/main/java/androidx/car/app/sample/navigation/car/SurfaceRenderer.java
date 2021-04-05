/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.navigation.car;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/** A very simple implementation of a renderer for the app's background surface. */
public final class SurfaceRenderer implements DefaultLifecycleObserver {
    private static final String TAG = "SurfaceRenderer";

    @Nullable Surface mSurface;
    @Nullable Rect mVisibleArea;
    @Nullable Rect mStableArea;

    private final CarContext mCarContext;
    private final Paint mLeftInsetPaint = new Paint();
    private final Paint mRightInsetPaint = new Paint();
    private final Paint mCenterPaint = new Paint();
    private final Paint mMarkerPaint = new Paint();

    private boolean mShowMarkers;
    private int mNumMarkers;
    private int mActiveMarker;
    private String mLocationString = "unknown";

    private final SurfaceCallback mSurfaceCallback =
            new SurfaceCallback() {
                @Override
                public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
                    synchronized (SurfaceRenderer.this) {
                        Log.i(TAG, String.format("Surface available %s", surfaceContainer));
                        mSurface = surfaceContainer.getSurface();
                        renderFrame();
                    }
                }

                @Override
                public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
                    synchronized (SurfaceRenderer.this) {
                        Log.i(
                                TAG,
                                String.format(
                                        "Visible area changed %s. stableArea:%s visibleArea:%s",
                                        mSurface, mStableArea, visibleArea));
                        mVisibleArea = visibleArea;
                        renderFrame();
                    }
                }

                @Override
                public void onStableAreaChanged(@NonNull Rect stableArea) {
                    synchronized (SurfaceRenderer.this) {
                        Log.i(
                                TAG,
                                String.format(
                                        "Stable area changed %s. stable:%s inset:%s",
                                        mSurface, stableArea, mVisibleArea));
                        mStableArea = stableArea;
                        renderFrame();
                    }
                }

                @Override
                public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
                    synchronized (SurfaceRenderer.this) {
                        Log.i(TAG, "Surface destroyed");
                        mSurface = null;
                    }
                }
            };

    public SurfaceRenderer(@NonNull CarContext carContext, @NonNull Lifecycle lifecycle) {
        mCarContext = carContext;

        mLeftInsetPaint.setColor(Color.RED);
        mLeftInsetPaint.setAntiAlias(true);
        mLeftInsetPaint.setStyle(Style.STROKE);

        mRightInsetPaint.setColor(Color.RED);
        mRightInsetPaint.setAntiAlias(true);
        mRightInsetPaint.setStyle(Style.STROKE);
        mRightInsetPaint.setTextAlign(Align.RIGHT);

        mCenterPaint.setColor(Color.BLUE);
        mCenterPaint.setAntiAlias(true);
        mCenterPaint.setStyle(Style.STROKE);

        mMarkerPaint.setColor(Color.GREEN);
        mMarkerPaint.setAntiAlias(true);
        mMarkerPaint.setStyle(Style.STROKE);
        mMarkerPaint.setStrokeWidth(3);

        lifecycle.addObserver(this);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Log.i(TAG, "SurfaceRenderer created");
        mCarContext.getCarService(AppManager.class).setSurfaceCallback(mSurfaceCallback);
    }

    /** Callback called when the car configuration changes. */
    public void onCarConfigurationChanged() {
        renderFrame();
    }

    /** Updates the markers drawn on the surface. */
    public void updateMarkerVisibility(boolean showMarkers, int numMarkers, int activeMarker) {
        mShowMarkers = showMarkers;
        mNumMarkers = numMarkers;
        mActiveMarker = activeMarker;
        renderFrame();
    }

    /** Updates the location coordinate string drawn on the surface. */
    public void updateLocationString(@NonNull String locationString) {
        mLocationString = locationString;
        renderFrame();
    }

    void renderFrame() {
        if (mSurface == null || !mSurface.isValid()) {
            // Surface is not available, or has been destroyed, skip this frame.
            return;
        }
        Canvas canvas = mSurface.lockCanvas(null);

        // Clear the background.
        canvas.drawColor(mCarContext.isDarkMode() ? Color.DKGRAY : Color.LTGRAY);

        final int horizontalTextMargin = 10;
        final int verticalTextMarginFromTop = 20;
        final int verticalTextMarginFromBottom = 10;

        // Draw a rectangle showing the inset.
        Rect visibleArea = mVisibleArea;
        if (visibleArea != null) {
            if (visibleArea.isEmpty()) {
                // No inset set. The entire area is considered safe to draw.
                visibleArea.set(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
            }

            canvas.drawRect(visibleArea, mLeftInsetPaint);
            canvas.drawLine(
                    visibleArea.left,
                    visibleArea.top,
                    visibleArea.right,
                    visibleArea.bottom,
                    mLeftInsetPaint);
            canvas.drawLine(
                    visibleArea.right,
                    visibleArea.top,
                    visibleArea.left,
                    visibleArea.bottom,
                    mLeftInsetPaint);
            canvas.drawText(
                    "(" + visibleArea.left + " , " + visibleArea.top + ")",
                    visibleArea.left + horizontalTextMargin,
                    visibleArea.top + verticalTextMarginFromTop,
                    mLeftInsetPaint);
            canvas.drawText(
                    "(" + visibleArea.right + " , " + visibleArea.bottom + ")",
                    visibleArea.right - horizontalTextMargin,
                    visibleArea.bottom - verticalTextMarginFromBottom,
                    mRightInsetPaint);

            // Draw location on the top right corner of the screen.
            canvas.drawText(
                    "(" + mLocationString + ")",
                    visibleArea.right - horizontalTextMargin,
                    visibleArea.top + verticalTextMarginFromTop,
                    mRightInsetPaint);
        } else {
            Log.d(TAG, "Visible area not available.");
        }

        if (mStableArea != null) {
            // Draw a cross-hairs at the stable center.
            final int lengthPx = 15;
            int centerX = mStableArea.centerX();
            int centerY = mStableArea.centerY();
            canvas.drawLine(centerX - lengthPx, centerY, centerX + lengthPx, centerY, mCenterPaint);
            canvas.drawLine(centerX, centerY - lengthPx, centerX, centerY + lengthPx, mCenterPaint);
            canvas.drawText(
                    "(" + centerX + ", " + centerY + ")",
                    centerX + horizontalTextMargin,
                    centerY,
                    mCenterPaint);
        } else {
            Log.d(TAG, "Stable area not available.");
        }

        if (mShowMarkers) {
            // Show a set number of markers centered around the midpoint of the stable area. If no
            // stable area, then use visible area or canvas dimensions. If an active marker is set
            // draw
            // a line from the center to that marker.
            Rect markerArea =
                    mStableArea != null
                            ? mStableArea
                            : (mVisibleArea != null
                                    ? mVisibleArea
                                    : new Rect(0, 0, canvas.getWidth() - 1, canvas.getHeight()));
            int centerX = markerArea.centerX();
            int centerY = markerArea.centerY();
            double radius = Math.min(centerX / 2, centerY / 2);

            double circleAngle = 2.0d * Math.PI;
            double markerpiece = circleAngle / mNumMarkers;
            for (int i = 0; i < mNumMarkers; i++) {
                int markerX = centerX + (int) (radius * Math.cos(markerpiece * i));
                int markerY = centerY + (int) (radius * Math.sin(markerpiece * i));
                canvas.drawCircle(markerX, markerY, 5, mMarkerPaint);
                if (i == mActiveMarker) {
                    canvas.drawLine(centerX, centerY, markerX, markerY, mMarkerPaint);
                }
            }
        }

        mSurface.unlockCanvasAndPost(canvas);
    }
}
