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

package androidx.camera.core.internal.utils;

import static androidx.camera.core.internal.utils.SizeUtil.getArea;

import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.Comparator;

/**
 * Utility class for aspect ratio related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AspectRatioUtil {
    private static final int ALIGN16 = 16;

    private AspectRatioUtil() {}

    /**
     * Returns true if the supplied resolution is a mod16 matching with the supplied aspect ratio.
     */
    public static boolean hasMatchingAspectRatio(@NonNull Size resolution,
            @Nullable Rational aspectRatio) {
        boolean isMatch = false;
        if (aspectRatio == null) {
            isMatch = false;
        } else if (aspectRatio.equals(
                new Rational(resolution.getWidth(), resolution.getHeight()))) {
            isMatch = true;
        } else if (getArea(resolution) >= getArea(SizeUtil.VGA_SIZE)) {
            // Only do mod 16 calculation if the size is equal to or larger than 640x480. It is
            // because the aspect ratio will be affected critically by mod 16 calculation if the
            // size is small. It may result in unexpected outcome such like 256x144 will be
            // considered as 18.5:9.
            isMatch = isPossibleMod16FromAspectRatio(resolution,
                    aspectRatio);
        }
        return isMatch;
    }

    /**
     * For codec performance improvement, OEMs may make the supported sizes to be mod16 alignment
     * . It means that the width or height of the supported size will be multiple of 16. The
     * result number after applying mod16 alignment can be the larger or smaller number that is
     * multiple of 16 and is closest to the original number. For example, a standard 16:9
     * supported size is 1920x1080. It may become 1920x1088 on some devices because 1088 is
     * multiple of 16. This function uses the target aspect ratio to calculate the possible
     * original width or height inversely. And then, checks whether the possibly original width or
     * height is in the range that the mod16 aligned height or width can support.
     */
    private static boolean isPossibleMod16FromAspectRatio(@NonNull Size resolution,
            @NonNull Rational aspectRatio) {
        int width = resolution.getWidth();
        int height = resolution.getHeight();

        Rational invAspectRatio = new Rational(/* numerator= */aspectRatio.getDenominator(),
                /* denominator= */aspectRatio.getNumerator());
        if (width % 16 == 0 && height % 16 == 0) {
            return ratioIntersectsMod16Segment(Math.max(0, height - ALIGN16), width, aspectRatio)
                    || ratioIntersectsMod16Segment(Math.max(0, width - ALIGN16), height,
                    invAspectRatio);
        } else if (width % 16 == 0) {
            return ratioIntersectsMod16Segment(height, width, aspectRatio);
        } else if (height % 16 == 0) {
            return ratioIntersectsMod16Segment(width, height, invAspectRatio);
        }
        return false;
    }


    private static boolean ratioIntersectsMod16Segment(int height, int mod16Width,
            Rational aspectRatio) {
        Preconditions.checkArgument(mod16Width % 16 == 0);
        double aspectRatioWidth =
                height * aspectRatio.getNumerator() / (double) aspectRatio.getDenominator();
        return aspectRatioWidth > Math.max(0, mod16Width - ALIGN16) && aspectRatioWidth < (
                mod16Width + ALIGN16);
    }

    /** Comparator based on how close they are to the target aspect ratio. */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class CompareAspectRatiosByDistanceToTargetRatio implements
            Comparator<Rational> {
        private Rational mTargetRatio;

        public CompareAspectRatiosByDistanceToTargetRatio(@NonNull Rational targetRatio) {
            mTargetRatio = targetRatio;
        }

        @Override
        public int compare(Rational lhs, Rational rhs) {
            if (lhs.equals(rhs)) {
                return 0;
            }

            final Float lhsRatioDelta = Math.abs(lhs.floatValue() - mTargetRatio.floatValue());
            final Float rhsRatioDelta = Math.abs(rhs.floatValue() - mTargetRatio.floatValue());

            int result = (int) Math.signum(lhsRatioDelta - rhsRatioDelta);
            return result;
        }
    }
}
