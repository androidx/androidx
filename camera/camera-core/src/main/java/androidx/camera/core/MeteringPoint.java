/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.util.Rational;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A MeteringPoint used to specify a region in sensor coordinates for focusing and metering
 * Purpose.
 *
 * <p>To create a {@link MeteringPoint}, apps have to use
 * {@link DisplayOrientedMeteringPointFactory} or {@link SensorOrientedMeteringPointFactory}.
 * The X/Y insides a MeteringPoint represents the normalized X/Y inside current crop region. If no
 * crop region is set, the the whole sensor area is used. AreaSize represents the width and the
 * height of the metering area and Weight can also be specified. By default, a MeteringPoint is
 * mapped to the sensor coordinates using Preview aspect ratio.  A custom FOV aspect ratio can be
 * set if apps want to use aspect ratio other than Preview.
 */
public class MeteringPoint {
    private float mNormalizedCropRegionX;
    private float mNormalizedCropRegionY;
    private float mSize;
    private float mWeight;
    @Nullable
    private Rational mFovAspectRatio; // null for preview aspect ratio.

    /**
     * Constructor is restricted for use within library.
     *
     * @param normalizedCropRegionX normalized X (ranging from 0 to 1) in current crop region.
     * @param normalizedCropRegionY normalized Y (ranging from 0 to 1) in current crop region.
     * @param size            size of the MeteringPoint(ranging from 0 to 1). The value
     *                       represents the percentage of current crop region width/height.
     * @param weight                weight of this metering point ranging from 0 to 1.
     * @param fovAspectRatio        if specified, use this aspect ratio. Otherwise use Preview's
     *                              aspect
     *                              ratio.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public MeteringPoint(float normalizedCropRegionX, float normalizedCropRegionY, float size,
            float weight, @Nullable Rational fovAspectRatio) {
        mNormalizedCropRegionX = normalizedCropRegionX;
        mNormalizedCropRegionY = normalizedCropRegionY;
        mSize = size;
        mWeight = weight;
        mFovAspectRatio = fovAspectRatio;
    }

    /**
     * Normalized crop region X (Ranging from 0 to 1)
     */
    public float getNormalizedCropRegionX() {
        return mNormalizedCropRegionX;
    }

    /**
     * Normalized crop region Y (Ranging from 0 to 1)
     */
    public float getNormalizedCropRegionY() {
        return mNormalizedCropRegionY;
    }

    /**
     * Size of the MeteringPoint(ranging from 0 to 1). The value represents the percentage of
     * current crop region width/height
     */
    public float getSize() {
        return mSize;
    }

    /**
     * Weight of the MeteringPoint (Ranging from 0 to 1)
     */
    public float getWeight() {
        return mWeight;
    }

    /**
     * Sets the size of MeteringPoint (ranging from 0 to 1). It is the percentage of the sensor
     * width/height (or cropRegion width/height if crop region is set)
     *
     * <p><pre>Metering Area width = size * cropRegion.width
     * Metering Area height = size * cropRegion.height
     * </pre>
     *
     */
    public void setSize(float size) {
        mSize = size;
    }

    /**
     * Sets weight of this metering point (ranging from 0 to 1)
     */
    public void setWeight(float weight) {
        mWeight = weight;
    }

    /**
     * Set custom aspect ratio to be adjusted for final sensor coordinates.
     */
    public void setFovAspectRatio(@Nullable Rational fovAspectRatio) {
        mFovAspectRatio = fovAspectRatio;
    }


    /**
     * Get custom aspect ratio to be adjusted for final sensor coordinates.
     */
    @Nullable
    public Rational getFovAspectRatio() {
        return mFovAspectRatio;
    }
}
