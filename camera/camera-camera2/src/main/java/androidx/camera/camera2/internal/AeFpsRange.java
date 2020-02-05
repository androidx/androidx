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
import android.hardware.camera2.CaptureRequest;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;

/**
 * Specifies proper CONTROL_AE_TARGET_FPS_RANGE to ensure exposure is good on devices.
 */
final class AeFpsRange {
    private Range<Integer> mAeTargetFpsRange = null;

    AeFpsRange(CameraCharacteristics cameraCharacteristics) {
        // Only specify AE Target FPS range on legacy devices because these devices always set
        // target fps range to [30,30] which might cause under-exposed issue.
        Integer hardwareLevel =
                cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (hardwareLevel != null
                && hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            Range<Integer>[] ranges =
                    cameraCharacteristics.get(
                            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            mAeTargetFpsRange = pickSuitableFpsRange(ranges);
        }
    }

    /**
     * Add CONTROL_AE_TARGET_FPS_RANGE option to the {@link Camera2ImplConfig.Builder}.
     */
    public void addAeFpsRangeOptions(@NonNull Camera2ImplConfig.Builder configBuilder) {
        if (mAeTargetFpsRange != null) {
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    mAeTargetFpsRange);
        }
    }

    /*
       On android 5.0/5.1,  CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES returns wrong range whose
       value was multiplied by 1000. So we need to convert it to the correct value.
     */
    private static Range<Integer> getCorrectedFpsRange(@NonNull Range<Integer> fpsRange) {
        int newUpper = fpsRange.getUpper();
        int newLower = fpsRange.getLower();
        if (fpsRange.getUpper() >= 1000) {
            newUpper = fpsRange.getUpper() / 1000;
        }

        if (fpsRange.getLower() >= 1000) {
            newLower = fpsRange.getLower() / 1000;
        }

        return new Range<>(newLower, newUpper);

    }

    /*
       Pick the fps range whose upper is 30 and whose lower is the smallest.  Return null if no
       range has 30 upper.  The rational is
       (1) range upper is always 30 so that a smooth frame rate is guaranteed.
       (2) range lower contains the smallest supported value so that it can adapt for low
           light condition as much as possible.
     */
    private static Range<Integer> pickSuitableFpsRange(Range<Integer>[] availableFpsRanges) {
        if (availableFpsRanges == null || availableFpsRanges.length == 0) {
            return null;
        }

        Range<Integer> pickedRange = null;
        for (Range<Integer> fpsRange : availableFpsRanges) {
            fpsRange = getCorrectedFpsRange(fpsRange);
            if (fpsRange.getUpper() != 30) {
                continue;
            }

            if (pickedRange == null) {
                pickedRange = fpsRange;
            } else {
                if (fpsRange.getLower() < pickedRange.getLower()) {
                    pickedRange = fpsRange;
                }
            }
        }

        return pickedRange;
    }
}
