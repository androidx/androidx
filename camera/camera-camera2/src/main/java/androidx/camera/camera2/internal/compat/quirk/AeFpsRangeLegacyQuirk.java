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

package androidx.camera.camera2.internal.compat.quirk;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: b/167425305
 *     Description: Quirk required to maintain good exposure on legacy devices by specifying a
 *                  proper
 *                  {@link android.hardware.camera2.CaptureRequest#CONTROL_AE_TARGET_FPS_RANGE}.
 *                  Legacy devices set the AE target FPS range to [30, 30]. This can potentially
 *                  cause underexposure issues.
 *                  {@link androidx.camera.camera2.internal.compat.workaround.AeFpsRange}
 *                  contains a workaround that is used on legacy devices to set a AE FPS range
 *                  whose upper bound is 30, which guarantees a smooth frame rate, and whose lower
 *                  bound is as small as possible to properly expose frames in low light
 *                  conditions. The default behavior on non legacy devices does not add the AE
 *                  FPS range option.
 *     Device(s): All legacy devices
 *     @see androidx.camera.camera2.internal.compat.workaround.AeFpsRange
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AeFpsRangeLegacyQuirk implements Quirk {

    @Nullable
    private final Range<Integer> mAeFpsRange;

    public AeFpsRangeLegacyQuirk(
            @NonNull final CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        final Range<Integer>[] availableFpsRanges = cameraCharacteristicsCompat.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        mAeFpsRange = pickSuitableFpsRange(availableFpsRanges);
    }

    static boolean load(@NonNull final CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        final Integer level = cameraCharacteristicsCompat.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    /**
     * Returns the fps range whose upper is 30 and whose lower is the smallest, or null if no
     * range has an upper equal to 30.  The rational is:
     * 1. Range upper is always 30 so that a smooth frame rate is guaranteed.
     * 2. Range lower contains the smallest supported value so that it can adapt as much as
     * possible to low light conditions.
     */
    @Nullable
    public Range<Integer> getRange() {
        return mAeFpsRange;
    }

    @Nullable
    private Range<Integer> pickSuitableFpsRange(
            @Nullable final Range<Integer>[] availableFpsRanges) {
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

    /**
     * On android 5.0/5.1, {@link CameraCharacteristics#CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES}
     * returns wrong ranges whose values were multiplied by 1000. So we need to convert them to the
     * correct values.
     */
    @NonNull
    private Range<Integer> getCorrectedFpsRange(@NonNull final Range<Integer> fpsRange) {
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
}
