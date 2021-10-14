/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CamcorderProfileProvider;
import androidx.camera.core.impl.CamcorderProfileProxy;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.core.util.Preconditions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VideoCapabilities is used to query video recording capabilities on the device.
 *
 * <p>Calling {@link #from(CameraInfo)} to obtain the VideoCapabilities.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(Scope.LIBRARY)
public final class VideoCapabilities {
    private static final String TAG = "VideoCapabilities";

    /**
     * The supported @link CamcorderProfileProxy} map from quality to CamcorderProfileProxy. The
     * order is from size large to small.
     */
    private final Map<Integer, CamcorderProfileProxy> mSupportedProfileMap = new LinkedHashMap<>();
    private final CamcorderProfileProxy mHighestProfile;
    private final CamcorderProfileProxy mLowestProfile;

    /**
     * Creates a VideoCapabilities.
     *
     * @param cameraInfoInternal the cameraInfo
     * @throws IllegalArgumentException if unable to get the capability information from the
     * CameraInfo.
     */
    VideoCapabilities(@NonNull CameraInfoInternal cameraInfoInternal) {
        CamcorderProfileProvider camcorderProfileProvider =
                cameraInfoInternal.getCamcorderProfileProvider();

        // Construct supported profile map
        for (@QualitySelector.VideoQuality int quality : QualitySelector.getSortedQualities()) {
            // SortedQualities is from size large to small

            // Get CamcorderProfile
            if (!camcorderProfileProvider.hasProfile(quality)) {
                continue;
            }
            CamcorderProfileProxy profile = camcorderProfileProvider.get(quality);
            Logger.d(TAG, "profile = " + profile);
            mSupportedProfileMap.put(quality, profile);
        }
        if (mSupportedProfileMap.isEmpty()) {
            Logger.e(TAG, "No supported CamcorderProfile");
            mLowestProfile = null;
            mHighestProfile = null;
        } else {
            Deque<CamcorderProfileProxy> profileQueue = new ArrayDeque<>(
                    mSupportedProfileMap.values());
            mHighestProfile = profileQueue.peekFirst();
            mLowestProfile = profileQueue.peekLast();
        }
    }

    /** Gets VideoCapabilities by the {@link CameraInfo} */
    @NonNull
    public static VideoCapabilities from(@NonNull CameraInfo cameraInfo) {
        return new VideoCapabilities((CameraInfoInternal) cameraInfo);
    }

    /**
     * Gets all supported qualities on the device.
     *
     * <p>The returned list is sorted by quality size from large to small. For the qualities in
     * the returned list, calling {@link #getProfile(int)} with these qualities will return a
     * non-null result.
     *
     * <p>Note: Constants {@link QualitySelector#QUALITY_HIGHEST} and
     * {@link QualitySelector#QUALITY_LOWEST} are not included.
     */
    @NonNull
    public List<Integer> getSupportedQualities() {
        return new ArrayList<>(mSupportedProfileMap.keySet());
    }

    /**
     * Checks if the quality is supported.
     *
     * @param quality one of the quality constants. Possible values include
     * {@link QualitySelector#QUALITY_LOWEST}, {@link QualitySelector#QUALITY_HIGHEST},
     * {@link QualitySelector#QUALITY_SD}, {@link QualitySelector#QUALITY_HD},
     * {@link QualitySelector#QUALITY_FHD}, or {@link QualitySelector#QUALITY_UHD}.
     * @return {@code true} if the quality is supported; {@code false} otherwise.
     * @throws IllegalArgumentException if not a quality constant.
     */
    public boolean isQualitySupported(@QualitySelector.VideoQuality int quality) {
        checkQualityConstantsOrThrow(quality);
        return getProfile(quality) != null;
    }

    /**
     * Gets the corresponding {@link CamcorderProfileProxy} of the input quality.
     *
     * @param quality one of the quality constants. Possible values include
     * {@link QualitySelector#QUALITY_LOWEST}, {@link QualitySelector#QUALITY_HIGHEST},
     * {@link QualitySelector#QUALITY_SD}, {@link QualitySelector#QUALITY_HD},
     * {@link QualitySelector#QUALITY_FHD}, or {@link QualitySelector#QUALITY_UHD}.
     * @return the CamcorderProfileProxy
     * @throws IllegalArgumentException if not a quality constant
     */
    @Nullable
    public CamcorderProfileProxy getProfile(@QualitySelector.VideoQuality int quality) {
        checkQualityConstantsOrThrow(quality);
        if (quality == QualitySelector.QUALITY_HIGHEST) {
            return mHighestProfile;
        } else if (quality == QualitySelector.QUALITY_LOWEST) {
            return mLowestProfile;
        }
        return mSupportedProfileMap.get(quality);
    }

    private static void checkQualityConstantsOrThrow(@QualitySelector.VideoQuality int quality) {
        Preconditions.checkArgument(QualitySelector.containsQuality(quality),
                "Unknown quality: " + quality);
    }
}
