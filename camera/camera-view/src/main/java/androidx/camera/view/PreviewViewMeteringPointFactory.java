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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
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
 */
class PreviewViewMeteringPointFactory extends MeteringPointFactory {
    @GuardedBy("mLock")
    private DisplayOrientedMeteringPointFactory mDisplayOrientedMeteringPointFactory;

    @GuardedBy("mLock")
    private float mViewWidth;

    @GuardedBy("mLock")
    private float mViewHeight;

    @GuardedBy("mLock")
    private float mFactoryWidth;

    @GuardedBy("mLock")
    private float mFactoryHeight;

    @Nullable
    @GuardedBy("mLock")
    private Size mResolution;

    @GuardedBy("mLock")
    private Display mDisplay;

    @GuardedBy("mLock")
    private CameraInfo mCameraInfo;

    @GuardedBy("mLock")
    private ScaleType mScaleType = ScaleType.FILL_CENTER;

    @GuardedBy("mLock")
    private boolean mIsValid;

    @GuardedBy("mLock")
    private boolean mIsCalculationStale = true;

    // Synchronize access to all the parameters since they can be updated by the main thread at
    // any time due to layout changes while the CameraInfo does not have a guaranteed thread it
    // will be called on.
    // In addition the metering point factory convert can be called on any thread.
    private final Object mLock = new Object();

    // TODO(b/150916047): Use Previewview scaleType transform to implement instead.
    PreviewViewMeteringPointFactory() {
        mIsValid = false;
    }

    /**
     * Initialize metering point factory with parameters.
     *
     * @param display the display on which the {@link PreviewView} is display
     * @param cameraInfo the information of the {@link Camera} that the PreviewView is attached to
     * @param resolution the resolution of the {@link PreviewViewImplementation} that is used by
     *                   the PreviewView
     * @param scaleType the scale type of the PreviewView
     * @param viewWidth the width of the PreviewView
     * @param viewHeight the height of the PreviewView
     */
    PreviewViewMeteringPointFactory(@Nullable Display display,
            @Nullable CameraInfo cameraInfo, @Nullable Size resolution,
            @Nullable ScaleType scaleType, int viewWidth, int viewHeight) {
        mDisplay = display;
        mCameraInfo = cameraInfo;
        mResolution = resolution;
        mScaleType = scaleType;
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
        recalculate();
    }

    void recalculate() {
        synchronized (mLock) {
            mIsCalculationStale = false;

            // invalid condition
            if (mResolution == null
                    || mViewWidth <= 0
                    || mViewHeight <= 0
                    || mDisplay == null
                    || mCameraInfo == null) {
                mIsValid = false;
                return;
            }
            mIsValid = true;

            boolean needReverse = false;

            if (isNaturalPortrait(mDisplay)) {
                if (mDisplay.getRotation() == Surface.ROTATION_0
                        || mDisplay.getRotation() == Surface.ROTATION_180) {
                    needReverse = true;
                }
            } else {
                if (mDisplay.getRotation() == Surface.ROTATION_90
                        || mDisplay.getRotation() == Surface.ROTATION_270) {
                    needReverse = true;
                }
            }

            int bufferRotatedWidth;
            int bufferRotatedHeight;
            if (needReverse) {
                bufferRotatedWidth = mResolution.getHeight();
                bufferRotatedHeight = mResolution.getWidth();
            } else {
                bufferRotatedWidth = mResolution.getWidth();
                bufferRotatedHeight = mResolution.getHeight();
            }

            final float scale;
            if (mScaleType == ScaleType.FILL_CENTER
                    || mScaleType == ScaleType.FILL_START
                    || mScaleType == ScaleType.FILL_END) {
                scale = Math.max((float) mViewWidth / bufferRotatedWidth,
                        (float) mViewHeight / bufferRotatedHeight);
            } else if (mScaleType == ScaleType.FIT_START
                    || mScaleType == ScaleType.FIT_CENTER
                    || mScaleType == ScaleType.FIT_END) {
                scale = Math.min((float) mViewWidth / bufferRotatedWidth,
                        (float) mViewHeight / bufferRotatedHeight);
            } else {
                throw new IllegalArgumentException("Unknown scale type " + mScaleType);
            }
            mFactoryWidth = bufferRotatedWidth * scale;
            mFactoryHeight = bufferRotatedHeight * scale;
            mDisplayOrientedMeteringPointFactory =
                    new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo, mFactoryWidth,
                            mFactoryHeight);
        }
    }

    void setScaleType(@Nullable ScaleType scaleType) {
        synchronized (mLock) {
            if (mScaleType == null || mScaleType != scaleType) {
                mScaleType = scaleType;
                mIsCalculationStale = true;
            }
        }
    }

    void setViewSize(int viewWidth, int viewHeight) {
        synchronized (mLock) {
            if (mViewWidth != viewWidth || mViewHeight != viewHeight) {
                mViewWidth = viewWidth;
                mViewHeight = viewHeight;
                mIsCalculationStale = true;
            }
        }
    }

    void setViewImplementationResolution(@Nullable Size resolution) {
        synchronized (mLock) {
            if (mResolution == null || !mResolution.equals(resolution)) {
                mResolution = resolution;
                mIsCalculationStale = true;
            }
        }
    }

    void setDisplay(@Nullable Display display) {
        synchronized (mLock) {
            if (mDisplay == null || mDisplay != display) {
                mDisplay = display;
                mIsCalculationStale = true;
            }
        }
    }

    void setCameraInfo(@Nullable CameraInfo cameraInfo) {
        synchronized (mLock) {
            if (mCameraInfo == null || mCameraInfo != cameraInfo) {
                mCameraInfo = cameraInfo;
                mIsCalculationStale = true;
            }
        }
    }

    @NonNull
    @Override
    protected PointF convertPoint(float x, float y) {
        synchronized (mLock) {
            if (mIsCalculationStale) {
                recalculate();
            }

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
