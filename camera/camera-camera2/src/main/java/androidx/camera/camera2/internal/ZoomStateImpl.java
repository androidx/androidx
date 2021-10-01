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

package androidx.camera.camera2.internal;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ZoomState;
import androidx.core.math.MathUtils;

/** An implementation of {@link ZoomState} where the values can be set. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ZoomStateImpl implements ZoomState {
    private float mZoomRatio;
    private final float mMaxZoomRatio;
    private final float mMinZoomRatio;
    private float mLinearZoom;

    ZoomStateImpl(float maxZoomRatio, float minZoomRatio) {
        mMaxZoomRatio = maxZoomRatio;
        mMinZoomRatio = minZoomRatio;
    }

    void setZoomRatio(float zoomRatio) throws IllegalArgumentException {
        if (zoomRatio > mMaxZoomRatio || zoomRatio < mMinZoomRatio) {
            String outOfRangeDesc = "Requested zoomRatio " + zoomRatio + " is not within valid "
                    + "range [" + mMinZoomRatio + " , "
                    + mMaxZoomRatio + "]";

            throw new IllegalArgumentException(outOfRangeDesc);
        }
        mZoomRatio = zoomRatio;
        mLinearZoom = getPercentageByRatio(mZoomRatio);
    }

    void setLinearZoom(float linearZoom) throws IllegalArgumentException {
        if (linearZoom > 1.0f || linearZoom < 0f) {
            String outOfRangeDesc = "Requested linearZoom " + linearZoom + " is not within"
                    + " valid range [0..1]";
            throw new IllegalArgumentException(outOfRangeDesc);
        }
        mLinearZoom = linearZoom;
        mZoomRatio = getRatioByPercentage(mLinearZoom);
    }

    @Override
    public float getZoomRatio() {
        return mZoomRatio;
    }

    @Override
    public float getMaxZoomRatio() {
        return mMaxZoomRatio;
    }

    @Override
    public float getMinZoomRatio() {
        return mMinZoomRatio;
    }

    @Override
    public float getLinearZoom() {
        return mLinearZoom;
    }

    private float getRatioByPercentage(float percentage) {
        // Make sure 1.0f and 0.0 return exactly the same max/min ratio.
        if (percentage == 1.0f) {
            return mMaxZoomRatio;
        } else if (percentage == 0f) {
            return mMinZoomRatio;
        }
        // This crop width is proportional to the real crop width.
        // The real crop with = sensorWidth/ zoomRatio,  but we need the ratio only so we can
        // assume sensorWidth as 1.0f.
        double cropWidthInMaxZoom = 1.0f / mMaxZoomRatio;
        double cropWidthInMinZoom = 1.0f / mMinZoomRatio;

        double cropWidth = cropWidthInMinZoom + (cropWidthInMaxZoom - cropWidthInMinZoom)
                * percentage;

        double ratio = 1.0 / cropWidth;

        return (float) MathUtils.clamp(ratio, mMinZoomRatio, mMaxZoomRatio);
    }

    private float getPercentageByRatio(float ratio) {
        // if zoom is not supported, return 0
        if (mMaxZoomRatio == mMinZoomRatio) {
            return 0f;
        }

        // To make the min/max same value when doing conversion between ratio / percentage.
        // We return the max/min value directly.
        if (ratio == mMaxZoomRatio) {
            return 1f;
        } else if (ratio == mMinZoomRatio) {
            return 0f;
        }

        float cropWidth = 1.0f / ratio;
        float cropWidthInMaxZoom = 1.0f / mMaxZoomRatio;
        float cropWidthInMinZoom = 1.0f / mMinZoomRatio;

        return (cropWidth - cropWidthInMinZoom) / (cropWidthInMaxZoom - cropWidthInMinZoom);
    }
}
