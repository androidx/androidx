/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.params;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.DynamicRange;
import androidx.core.util.Preconditions;

import java.util.Set;

/**
 * Helper for accessing features in DynamicRangeProfiles in a backwards compatible fashion.
 */
@RequiresApi(21)
public final class DynamicRangesCompat {

    private final DynamicRangeProfilesCompatImpl mImpl;

    DynamicRangesCompat(@NonNull DynamicRangeProfilesCompatImpl impl) {
        mImpl = impl;
    }

    /**
     * Returns a set of supported {@link DynamicRange} that can be referenced in a single
     * capture request.
     *
     * <p>For example if a particular 10-bit output capable device returns (STANDARD,
     * HLG10, HDR10) as result from calling {@link #getSupportedDynamicRanges()} and
     * getProfileCaptureRequestConstraints(long) returns (STANDARD, HLG10) when given an argument
     * of STANDARD. This means that the corresponding camera device will only accept and process
     * capture requests that reference outputs configured using HDR10 dynamic range or
     * alternatively some combination of STANDARD and HLG10. However trying to queue capture
     * requests to outputs that reference both HDR10 and STANDARD/HLG10 will result in
     * IllegalArgumentException.
     *
     * <p>The list will be empty in case there are no constraints for the given dynamic range.
     *
     * @param dynamicRange The dynamic range that will be checked for constraints
     * @return non-modifiable set of dynamic ranges
     * @throws IllegalArgumentException If the dynamic range argument is not within the set
     * returned by {@link #getSupportedDynamicRanges()}.
     */
    @NonNull
    public Set<DynamicRange> getDynamicRangeCaptureRequestConstraints(
            @NonNull DynamicRange dynamicRange) {
        return mImpl.getDynamicRangeCaptureRequestConstraints(dynamicRange);
    }

    /**
     * Returns a set of supported dynamic ranges.
     *
     * @return a non-modifiable set of dynamic ranges.
     */
    @NonNull
    public Set<DynamicRange> getSupportedDynamicRanges() {
        return mImpl.getSupportedDynamicRanges();
    }

    /**
     * Checks whether a given dynamic range is suitable for latency sensitive use cases.
     *
     * <p>Due to internal lookahead logic, camera outputs configured with some dynamic range
     * profiles may experience additional latency greater than 3 buffers. Using camera outputs
     * with such dynamic ranges for latency sensitive use cases such as camera preview is not
     * recommended. Dynamic ranges that have such extra streaming delay are typically utilized for
     * scenarios such as offscreen video recording.
     *
     * @param dynamicRange The dynamic range to check for extra latency
     * @return {@code true} if the given profile is not suitable for latency sensitive use cases,
     * {@code false} otherwise.
     * @throws IllegalArgumentException If the dynamic range argument is not within the set
     * returned by {@link #getSupportedDynamicRanges()}.
     */
    public boolean isExtraLatencyPresent(@NonNull DynamicRange dynamicRange) {
        return mImpl.isExtraLatencyPresent(dynamicRange);
    }

    /**
     * Returns a {@link DynamicRangesCompat} using the capabilities derived from the provided
     * characteristics.
     *
     * @param characteristics the characteristics used to derive dynamic range information.
     * @return a {@link DynamicRangesCompat} object.
     */
    @NonNull
    public static DynamicRangesCompat fromCameraCharacteristics(
            @NonNull CameraCharacteristicsCompat characteristics) {
        DynamicRangesCompat rangesCompat = null;
        if (Build.VERSION.SDK_INT >= 33) {
            rangesCompat = toDynamicRangesCompat(characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES));
        }

        return (rangesCompat == null) ? DynamicRangesCompatBaseImpl.COMPAT_INSTANCE : rangesCompat;
    }

    /**
     * Creates an instance from a framework android.hardware.camera2.params.DynamicRangeProfiles
     * object.
     *
     * @param dynamicRangeProfiles a {@link android.hardware.camera2.params.DynamicRangeProfiles}.
     * @return an equivalent {@link DynamicRangesCompat} object.
     */
    @Nullable
    @RequiresApi(33)
    public static DynamicRangesCompat toDynamicRangesCompat(
            @Nullable DynamicRangeProfiles dynamicRangeProfiles) {
        if (dynamicRangeProfiles == null) {
            return null;
        }

        Preconditions.checkState(Build.VERSION.SDK_INT >= 33, "DynamicRangeProfiles can only be "
                + "converted to DynamicRangesCompat on API 33 or higher.");

        return new DynamicRangesCompat(new DynamicRangesCompatApi33Impl(dynamicRangeProfiles));
    }

    /**
     * Returns the underlying framework
     * {@link android.hardware.camera2.params.DynamicRangeProfiles}.
     *
     * @return the underlying {@link android.hardware.camera2.params.DynamicRangeProfiles} or
     * {@code null} if the device doesn't support 10 bit dynamic range.
     */
    @Nullable
    @RequiresApi(33)
    public DynamicRangeProfiles toDynamicRangeProfiles() {
        Preconditions.checkState(Build.VERSION.SDK_INT >= 33, "DynamicRangesCompat can only be "
                + "converted to DynamicRangeProfiles on API 33 or higher.");
        return mImpl.unwrap();
    }

    interface DynamicRangeProfilesCompatImpl {
        @NonNull
        Set<DynamicRange> getDynamicRangeCaptureRequestConstraints(
                @NonNull DynamicRange dynamicRange);

        @NonNull
        Set<DynamicRange> getSupportedDynamicRanges();

        boolean isExtraLatencyPresent(@NonNull DynamicRange dynamicRange);

        @Nullable
        DynamicRangeProfiles unwrap();
    }
}
