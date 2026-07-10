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

package androidx.camera.video.internal.workaround;

import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.internal.compat.quirk.SurfaceProcessingQuirk;
import androidx.camera.video.Quality;
import androidx.camera.video.internal.compat.quirk.VideoQualityQuirk;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An implementation that provides the {@link EncoderProfilesProxy} only when the quality is
 * capable of video recording
 *
 * @see VideoQualityQuirk
 */
public class QualityValidatedEncoderProfilesProvider implements EncoderProfilesProvider {

    private final @NonNull EncoderProfilesProvider mProvider;
    private final @NonNull CameraInfoInternal mCameraInfo;
    private final @NonNull Quirks mQuirks;

    public QualityValidatedEncoderProfilesProvider(@NonNull EncoderProfilesProvider provider,
            @NonNull CameraInfoInternal cameraInfo, @NonNull Quirks quirks) {
        mProvider = provider;
        mCameraInfo = cameraInfo;
        mQuirks = quirks;
    }

    @Override
    public boolean hasProfile(int quality) {
        return mProvider.hasProfile(quality) && isDeviceValidQuality(quality);
    }

    @Override
    public @Nullable EncoderProfilesProxy getAll(int quality) {
        if (!hasProfile(quality)) {
            return null;
        }

        return mProvider.getAll(quality);
    }

    private boolean isDeviceValidQuality(int quality) {
        Quality videoQuality = Quality.castOrNull(quality);

        // Check if the quality is not problematic or can be workaround.
        if (videoQuality != null) {
            for (VideoQualityQuirk quirk : mQuirks.getAll(VideoQualityQuirk.class)) {
                // All quirks must be able to be workaround, then it can be considered valid.
                if (quirk != null && quirk.isProblematicVideoQuality(mCameraInfo, videoQuality)) {
                    if (!workaroundBySurfaceProcessing(quirk)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean workaroundBySurfaceProcessing(@NonNull Quirk quirk) {
        return quirk instanceof SurfaceProcessingQuirk
                && ((SurfaceProcessingQuirk) quirk).workaroundBySurfaceProcessing();
    }
}
