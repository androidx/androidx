/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video;

import android.util.Size;

import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.CameraInfoInternal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * RecorderVideoCapabilities is used to query video recording capabilities related to Recorder.
 *
 * @see Recorder#getVideoCapabilities(CameraInfo)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RecorderVideoCapabilities implements VideoCapabilities {
    private final EncoderProfilesResolver mEncoderProfilesResolver;
    private final boolean mIsStabilizationSupported;

    /**
     * Creates a RecorderVideoCapabilities.
     *
     * @param encoderProfilesResolver the encoder profiles resolver.
     * @param cameraInfo              the cameraInfo.
     * @throws IllegalArgumentException if unable to get the capability information from the
     *                                  CameraInfo or the videoCapabilitiesSource is not supported.
     */
    RecorderVideoCapabilities(
            @NonNull EncoderProfilesResolver encoderProfilesResolver,
            @NonNull CameraInfoInternal cameraInfo
    ) {
        mEncoderProfilesResolver = encoderProfilesResolver;
        mIsStabilizationSupported = cameraInfo.isVideoStabilizationSupported();
    }

    @Override
    public @NonNull Set<DynamicRange> getSupportedDynamicRanges() {
        return mEncoderProfilesResolver.getSupportedDynamicRanges();
    }

    @Override
    public @NonNull List<Quality> getSupportedQualities(@NonNull DynamicRange dynamicRange) {
        return mEncoderProfilesResolver.getSupportedQualities(dynamicRange);
    }

    @Override
    public boolean isQualitySupported(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        return mEncoderProfilesResolver.isQualitySupported(quality, dynamicRange);
    }

    @Override
    public boolean isStabilizationSupported() {
        return mIsStabilizationSupported;
    }

    @Override
    public @Nullable Size getResolution(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        return mEncoderProfilesResolver.getResolution(quality, dynamicRange);
    }
}
