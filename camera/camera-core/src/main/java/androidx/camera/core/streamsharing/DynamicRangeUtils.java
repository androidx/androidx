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

package androidx.camera.core.streamsharing;

import static androidx.camera.core.DynamicRange.BIT_DEPTH_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.ENCODING_HDR_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.ENCODING_SDR;
import static androidx.camera.core.DynamicRange.ENCODING_UNSPECIFIED;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.UseCaseConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for handling dynamic range.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DynamicRangeUtils {

    private DynamicRangeUtils() {
    }

    /**
     * Resolves dynamic ranges from use case configs.
     *
     * <p>If there is no dynamic range that satisfies all requirements, a null will be returned.
     */
    @Nullable
    public static DynamicRange resolveDynamicRange(@NonNull Set<UseCaseConfig<?>> useCaseConfigs) {
        List<DynamicRange> dynamicRanges = new ArrayList<>();
        for (UseCaseConfig<?> useCaseConfig : useCaseConfigs) {
            dynamicRanges.add(useCaseConfig.getDynamicRange());
        }

        return intersectDynamicRange(dynamicRanges);
    }

    /**
     * Finds the intersection of the input dynamic ranges.
     *
     * <p>Returns the intersection if found, or null if no intersection.
     */
    @Nullable
    private static DynamicRange intersectDynamicRange(@NonNull List<DynamicRange> dynamicRanges) {
        if (dynamicRanges.isEmpty()) {
            return null;
        }

        DynamicRange firstDynamicRange = dynamicRanges.get(0);
        Integer resultEncoding = firstDynamicRange.getEncoding();
        Integer resultBitDepth = firstDynamicRange.getBitDepth();
        for (int i = 1; i < dynamicRanges.size(); i++) {
            DynamicRange childDynamicRange = dynamicRanges.get(i);
            resultEncoding = intersectDynamicRangeEncoding(resultEncoding,
                    childDynamicRange.getEncoding());
            resultBitDepth = intersectDynamicRangeBitDepth(resultBitDepth,
                    childDynamicRange.getBitDepth());

            if (resultEncoding == null || resultBitDepth == null) {
                return null;
            }
        }

        return new DynamicRange(resultEncoding, resultBitDepth);
    }

    @Nullable
    private static Integer intersectDynamicRangeEncoding(@NonNull Integer encoding1,
            @NonNull Integer encoding2) {
        // Handle unspecified.
        if (encoding1.equals(ENCODING_UNSPECIFIED)) {
            return encoding2;
        }
        if (encoding2.equals(ENCODING_UNSPECIFIED)) {
            return encoding1;
        }

        // Handle HDR unspecified.
        if (encoding1.equals(ENCODING_HDR_UNSPECIFIED) && !encoding2.equals(ENCODING_SDR)) {
            return encoding2;
        }
        if (encoding2.equals(ENCODING_HDR_UNSPECIFIED) && !encoding1.equals(ENCODING_SDR)) {
            return encoding1;
        }

        return encoding1.equals(encoding2) ? encoding1 : null;
    }

    @Nullable
    private static Integer intersectDynamicRangeBitDepth(@NonNull Integer bitDepth1,
            @NonNull Integer bitDepth2) {
        // Handle unspecified.
        if (bitDepth1.equals(BIT_DEPTH_UNSPECIFIED)) {
            return bitDepth2;
        }
        if (bitDepth2.equals(BIT_DEPTH_UNSPECIFIED)) {
            return bitDepth1;
        }

        return bitDepth1.equals(bitDepth2) ? bitDepth1 : null;
    }
}
