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

package androidx.camera.video;

import static androidx.camera.core.AspectRatio.RATIO_16_9;
import static androidx.camera.core.AspectRatio.RATIO_4_3;
import static androidx.camera.core.AspectRatio.RATIO_DEFAULT;

import static java.lang.Math.abs;
import static java.util.Objects.requireNonNull;

import android.util.Range;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.internal.utils.SizeUtil;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class saves the mapping from a {@link Quality} + {@code VideoSpec#ASPECT_RATIO_*}
 * combination to a resolution list.
 *
 * <p>The class defines the video height range for each Quality. It classifies the input
 * resolutions by the Quality ranges and aspect ratios. For example, assume the input resolutions
 * are [1920x1080, 1440x1080, 1080x1080, 1280x720, 960x720 864x480, 640x480, 640x360],
 * <pre>{@code
 * SD-4:3 = [640x480]
 * SD-16:9 = [640x360, 864x480]
 * HD-4:3 = [960x720]
 * HD-16:9 = [1280x720]
 * FHD-4:3 = [1440x1080]
 * FHD-16:9 = [1920x1080]
 * }</pre>
 * It ignores resolutions not belong to the supported aspect ratios. It sorts each resolution
 * list based on the smallest area difference to the given video size of CamcorderProfile.
 * It provides {@link #getResolutions(Quality, int)} API to query the result.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class QualityRatioToResolutionsTable {

    // Key: Quality
    // Value: the height range of Quality
    private static final Map<Quality, Range<Integer>> sQualityRangeMap = new HashMap<>();
    static {
        sQualityRangeMap.put(Quality.UHD, Range.create(2160, 4319));
        sQualityRangeMap.put(Quality.FHD, Range.create(1080, 1439));
        sQualityRangeMap.put(Quality.HD, Range.create(720, 1079));
        sQualityRangeMap.put(Quality.SD, Range.create(241, 719));
    }

    // Key: aspect ratio constant
    // Value: aspect ratio rational
    private static final Map<Integer, Rational> sAspectRatioMap = new HashMap<>();
    static {
        sAspectRatioMap.put(RATIO_4_3, AspectRatioUtil.ASPECT_RATIO_4_3);
        sAspectRatioMap.put(RATIO_16_9, AspectRatioUtil.ASPECT_RATIO_16_9);
    }

    // Key: QualityRatio (Quality + AspectRatio)
    // Value: resolutions
    private final Map<QualityRatio, List<Size>> mTable = new HashMap<>();
    {
        for (Quality quality : sQualityRangeMap.keySet()) {
            mTable.put(QualityRatio.of(quality, RATIO_DEFAULT), new ArrayList<>());
            for (Integer aspectRatio : sAspectRatioMap.keySet()) {
                mTable.put(QualityRatio.of(quality, aspectRatio), new ArrayList<>());
            }
        }
    }

    /**
     * Constructs table.
     *
     * @param resolutions             the resolutions to be classified.
     * @param profileQualityToSizeMap the video sizes of CamcorderProfile. It will be used to map
     *                                [quality + {@link AspectRatio#RATIO_DEFAULT}] to the profile
     *                                size, and used to sort each Quality-Ratio row by the
     *                                smallest area difference to the profile size.
     */
    QualityRatioToResolutionsTable(@NonNull List<Size> resolutions,
            @NonNull Map<Quality, Size> profileQualityToSizeMap) {
        addProfileSizesToTable(profileQualityToSizeMap);
        addResolutionsToTable(resolutions);
        sortQualityRatioRow(profileQualityToSizeMap);
    }

    /**
     * Gets the resolutions of the mapped Quality + AspectRatio.
     *
     * <p>Giving {@link AspectRatio#RATIO_DEFAULT} will return the mapped profile size.
     */
    @NonNull
    List<Size> getResolutions(@NonNull Quality quality, @AspectRatio.Ratio int aspectRatio) {
        List<Size> qualityRatioRow = getQualityRatioRow(quality, aspectRatio);
        return qualityRatioRow != null ? new ArrayList<>(qualityRatioRow) : new ArrayList<>(0);
    }

    private void addProfileSizesToTable(@NonNull Map<Quality, Size> profileQualityToSizeMap) {
        for (Map.Entry<Quality, Size> entry : profileQualityToSizeMap.entrySet()) {
            requireNonNull(getQualityRatioRow(entry.getKey(), RATIO_DEFAULT)).add(entry.getValue());
        }
    }

    private void addResolutionsToTable(@NonNull List<Size> resolutions) {
        for (Size resolution : resolutions) {
            Quality quality = findMappedQuality(resolution);
            if (quality == null) {
                continue;
            }
            Integer aspectRatio = findMappedAspectRatio(resolution);
            if (aspectRatio == null) {
                continue;
            }
            List<Size> qualityRatioRow = requireNonNull(getQualityRatioRow(quality, aspectRatio));
            qualityRatioRow.add(resolution);
        }
    }

    private void sortQualityRatioRow(@NonNull Map<Quality, Size> profileQualityToSizeMap) {
        for (Map.Entry<QualityRatio, List<Size>> entry : mTable.entrySet()) {
            Size profileSize = profileQualityToSizeMap.get(entry.getKey().getQuality());
            if (profileSize == null) {
                // Sorting is ignored if the profile doesn't contain the corresponding size.
                continue;
            }
            // Sort by the smallest area difference from the profile size.
            int qualitySizeArea = SizeUtil.getArea(profileSize);
            Collections.sort(entry.getValue(), (s1, s2) -> {
                int s1Diff = abs(SizeUtil.getArea(s1) - qualitySizeArea);
                int s2Diff = abs(SizeUtil.getArea(s2) - qualitySizeArea);
                return s1Diff - s2Diff;
            });
        }
    }

    @Nullable
    private static Quality findMappedQuality(@NonNull Size resolution) {
        for (Map.Entry<Quality, Range<Integer>> entry : sQualityRangeMap.entrySet()) {
            if (entry.getValue().contains(resolution.getHeight())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    private static Integer findMappedAspectRatio(@NonNull Size resolution) {
        for (Map.Entry<Integer, Rational> entry : sAspectRatioMap.entrySet()) {
            if (AspectRatioUtil.hasMatchingAspectRatio(resolution, entry.getValue(),
                    SizeUtil.RESOLUTION_QVGA)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    private List<Size> getQualityRatioRow(@NonNull Quality quality,
            @AspectRatio.Ratio int aspectRatio) {
        return mTable.get(QualityRatio.of(quality, aspectRatio));
    }

    @AutoValue
    abstract static class QualityRatio {

        static QualityRatio of(@NonNull Quality quality, @AspectRatio.Ratio int aspectRatio) {
            return new AutoValue_QualityRatioToResolutionsTable_QualityRatio(quality, aspectRatio);
        }

        @NonNull
        abstract Quality getQuality();

        @SuppressWarnings("unused")
        @AspectRatio.Ratio
        abstract int getAspectRatio();
    }
}
