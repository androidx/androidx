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

package androidx.camera.view.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;

/**
 * A quirk where the preview buffer is stretched.
 *
 * <p> This is similar to the SamsungPreviewTargetAspectRatioQuirk in camera-camera2 artifact.
 * The difference is, the other quirk can be fixed by choosing a different resolution,
 * while for this one the preview is always stretched no matter what resolution is selected.
 */
public class PreviewStretchedQuirk implements Quirk {

    private static final String SAMSUNG_A3_2017 = "A3Y17LTE"; // b/180121821

    private static final List<String> KNOWN_AFFECTED_DEVICES = Arrays.asList(SAMSUNG_A3_2017);

    static boolean load() {
        return KNOWN_AFFECTED_DEVICES.contains(Build.DEVICE.toUpperCase());
    }

    /**
     * The mount that the crop rect needs to be scaled in x.
     */
    public float getCropRectScaleX() {
        if (SAMSUNG_A3_2017.equals(Build.DEVICE.toUpperCase())) {
            // For Samsung A3 2017, the symptom seems to be that the preview's FOV is always 1/3
            // wider than it's supposed to be. For example, if the preview Surface is 800x600, it's
            // actually has a FOV of 1066x600, but stretched to fit the 800x600 buffer. To correct
            // the preview, we need to crop out the extra 25% FOV.
            return 0.75f;
        }
        // No scale.
        return 1;
    }

    /**
     * The mount that the crop rect needs to be scaled in y.
     */
    public float getCropRectScaleY() {
        // No scale.
        return 1;
    }
}
