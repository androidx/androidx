/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.view;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.view.PreviewView.ScaleType;

/**
 * Implement the MeteringPointFactory for PreviewView by DisplayOrientedMeteringPointFactory.
 *
 * <p>The width / height in DisplayOrientedMeteringPointFactory defines area (0, 0) - (width,
 * height) which represents the full preview surface.  The (x, y) passed to createPoint()
 * must be in this coordinate system.
 *
 * However, in PreviewView, the preview could be cropped or letterbox/pillarbox depending on
 * the scaleType. Thus we need to adjust two things for DisplayOrientedMeteringPointFactory.
 * (1) Calculate the new width/height of factory based on the FIT/FULL scaleType to make it
 * represent the full preview
 * (2) Add offset to the (x, y) in convertPoint based on the BEGIN/CENTER/END scaleType.
 *
 */
class PreviewViewMeteringPointFactory extends MeteringPointFactory {
    private DisplayOrientedMeteringPointFactory mDisplayOrientedMeteringPointFactory;
    private final float mViewWidth;
    private final float mViewHeight;
    private float mFactoryWidth;
    private float mFactoryHeight;
    private final ScaleType mScaleType;
    private final boolean mIsValid;

    // TODO(b/150916047): Use Previewview scaleType transform to implement instead.
    PreviewViewMeteringPointFactory(@NonNull Display display,
            @NonNull CameraSelector cameraSelector, @Nullable Size resolution,
            @NonNull ScaleType scaleType, int viewWidth, int viewHeight) {
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
        mScaleType = scaleType;

        // invalid condition
        if (resolution == null || mViewWidth <= 0 || mViewHeight <= 0) {
            mIsValid = false;
            return;
        }
        mIsValid = true;

        boolean needReverse = false;

        if (isNaturalPortrait(display)) {
            if (display.getRotation() == Surface.ROTATION_0
                    || display.getRotation() == Surface.ROTATION_180) {
                needReverse = true;
            }
        } else {
            if (display.getRotation() == Surface.ROTATION_90
                    || display.getRotation() == Surface.ROTATION_270) {
                needReverse = true;
            }
        }

        int bufferRotatedWidth;
        int bufferRotatedHeight;
        if (needReverse) {
            bufferRotatedWidth = resolution.getHeight();
            bufferRotatedHeight = resolution.getWidth();
        } else {
            bufferRotatedWidth = resolution.getWidth();
            bufferRotatedHeight = resolution.getHeight();
        }

        final float scale;
        if (mScaleType == ScaleType.FILL_CENTER
                || mScaleType == ScaleType.FILL_START
                || mScaleType == ScaleType.FILL_END) {
            scale = Math.max((float) viewWidth / bufferRotatedWidth,
                    (float) viewHeight / bufferRotatedHeight);
        } else if (mScaleType == ScaleType.FIT_START
                || mScaleType == ScaleType.FIT_CENTER
                || mScaleType == ScaleType.FIT_END) {
            scale = Math.min((float) viewWidth / bufferRotatedWidth,
                    (float) viewHeight / bufferRotatedHeight);
        } else {
            throw new IllegalArgumentException("Unknown scale type " + scaleType);
        }
        mFactoryWidth = bufferRotatedWidth * scale;
        mFactoryHeight = bufferRotatedHeight * scale;
        mDisplayOrientedMeteringPointFactory =
                new DisplayOrientedMeteringPointFactory(display, cameraSelector, mFactoryWidth,
                        mFactoryHeight);
    }

    @NonNull
    @Override
    protected PointF convertPoint(float x, float y) {
        if (!mIsValid) {
            // Returns a invalid point whose value is out of range [0..1]
            return new PointF(2.0f, 2.0f);
        }
        float offsetX = 0f;
        float offsetY = 0f;

        if (mScaleType == ScaleType.FILL_START
                || mScaleType == ScaleType.FIT_START) {
            offsetX = 0;
            offsetY = 0;
        } else if (mScaleType == ScaleType.FILL_CENTER
                || mScaleType == ScaleType.FIT_CENTER) {
            offsetX = (mFactoryWidth - mViewWidth) / 2;
            offsetY = (mFactoryHeight - mViewHeight) / 2;
        } else if (mScaleType == ScaleType.FILL_END
                || mScaleType == ScaleType.FIT_END) {
            offsetX = (mFactoryWidth - mViewWidth);
            offsetY = (mFactoryHeight - mViewHeight);
        }

        float adjustedX = x + offsetX;
        float adjustedY = y + offsetY;

        // DisplayOrientedMeteringPointFactory#convertPoint is not public, thus we cannot use
        // it to convert the point. A alternative approach is using createPoint() to create a
        // MeteringPoint and get X, Y from it.
        MeteringPoint pt = mDisplayOrientedMeteringPointFactory.createPoint(adjustedX,
                adjustedY);
        return new PointF(pt.getX(), pt.getY());
    }


    private boolean isNaturalPortrait(Display display) {
        final Point deviceSize = new Point();
        display.getRealSize(deviceSize);
        int rotation = display.getRotation();

        final int width = deviceSize.x;
        final int height = deviceSize.y;
        return ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
                && width < height) || (
                (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
                        && width >= height);
    }
}
