/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfoInternal;
import androidx.camera.core.CameraOrientationUtil;
import androidx.camera.core.ImageOutputConfig.RotationValue;
import androidx.camera.core.LensFacing;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Information for a fake camera.
 *
 * <p>This camera info can be constructed with fake values.
 */
public final class FakeCameraInfoInternal implements CameraInfoInternal {

    private final int mSensorRotation;
    private final LensFacing mLensFacing;
    private MutableLiveData<Boolean> mFlashAvailability;

    public FakeCameraInfoInternal() {
        this(/*sensorRotation=*/ 0, /*lensFacing=*/ LensFacing.BACK);
        mFlashAvailability = new MutableLiveData<>(Boolean.TRUE);
    }

    public FakeCameraInfoInternal(int sensorRotation, @NonNull LensFacing lensFacing) {
        mSensorRotation = sensorRotation;
        mLensFacing = lensFacing;
        mFlashAvailability = new MutableLiveData<>(Boolean.TRUE);
    }

    @Nullable
    @Override
    public LensFacing getLensFacing() {
        return mLensFacing;
    }

    @Override
    public int getSensorRotationDegrees(@RotationValue int relativeRotation) {
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(relativeRotation);
        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        boolean isOppositeFacingScreen = LensFacing.BACK.equals(getLensFacing());
        return CameraOrientationUtil.getRelativeImageRotation(
                relativeRotationDegrees,
                mSensorRotation,
                isOppositeFacingScreen);
    }

    @Override
    public int getSensorRotationDegrees() {
        return getSensorRotationDegrees(Surface.ROTATION_0);
    }

    @NonNull
    @Override
    public LiveData<Boolean> isFlashAvailable() {
        return mFlashAvailability;
    }
}
