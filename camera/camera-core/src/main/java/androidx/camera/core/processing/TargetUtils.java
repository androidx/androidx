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

package androidx.camera.core.processing;

import static androidx.camera.core.CameraEffect.IMAGE_CAPTURE;
import static androidx.camera.core.CameraEffect.PREVIEW;
import static androidx.camera.core.CameraEffect.VIDEO_CAPTURE;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for {@link androidx.camera.core.CameraEffect.Targets}.
 */
public class TargetUtils {

    private TargetUtils() {
    }

    /**
     * Returns the number of targets in the given target mask by counting the number of 1s.
     */
    public static int getNumberOfTargets(int targets) {
        int count = 0;
        while (targets != 0) {
            count += (targets & 1);
            targets >>= 1;
        }
        return count;
    }

    /**
     * Returns true if subset ⊆ superset.
     */
    public static boolean isSuperset(int superset, int subset) {
        return (superset & subset) == subset;
    }

    /**
     * Checks if the list contains the target and throws a human-readable exception if it doesn't.
     */
    public static void checkSupportedTargets(@NonNull Collection<Integer> supportedTargets,
            int targets) {
        Preconditions.checkArgument(supportedTargets.contains(targets), String.format(Locale.US,
                "Effects target %s is not in the supported list %s.",
                getHumanReadableName(targets),
                getHumanReadableNames(supportedTargets)));
    }

    @NonNull
    private static String getHumanReadableNames(@NonNull Collection<Integer> targets) {
        List<String> targetNameList = new ArrayList<>();
        for (Integer target : targets) {
            targetNameList.add(getHumanReadableName(target));
        }
        return "[" + String.join(", ", targetNameList) + "]";
    }

    /**
     * Returns a human-readable name for the target.
     */
    @NonNull
    public static String getHumanReadableName(int target) {
        List<String> names = new ArrayList<>();
        if ((target & IMAGE_CAPTURE) != 0) {
            names.add("IMAGE_CAPTURE");
        }
        if ((target & PREVIEW) != 0) {
            names.add("PREVIEW");
        }
        if ((target & VIDEO_CAPTURE) != 0) {
            names.add("VIDEO_CAPTURE");
        }
        return String.join("|", names);
    }
}
