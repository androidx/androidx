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

import android.hardware.camera2.CameraCharacteristics;
import android.util.Range;
import android.util.Rational;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.ExposureState;

/**
 * An implementation of {@link ExposureState} where the values can be set.
 */
class ExposureStateImpl implements ExposureState {

    private final Object mLock = new Object();
    private final CameraCharacteristicsCompat mCameraCharacteristics;
    @GuardedBy("mLock")
    private int mExposureCompensation;

    ExposureStateImpl(CameraCharacteristicsCompat characteristics, int exposureCompensation) {
        mCameraCharacteristics = characteristics;
        mExposureCompensation = exposureCompensation;
    }

    @Override
    public int getExposureCompensationIndex() {
        synchronized (mLock) {
            return mExposureCompensation;
        }
    }

    void setExposureCompensationIndex(int value) {
        synchronized (mLock) {
            mExposureCompensation = value;
        }
    }

    @NonNull
    @Override
    public Range<Integer> getExposureCompensationRange() {
        return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
    }

    @NonNull
    @Override
    public Rational getExposureCompensationStep() {
        if (!isExposureCompensationSupported()) {
            return Rational.ZERO;
        }
        return mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
    }

    @Override
    public boolean isExposureCompensationSupported() {
        Range<Integer> compensationRange =
                mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        return compensationRange != null
                && compensationRange.getLower() != 0
                && compensationRange.getUpper() != 0;
    }
}
