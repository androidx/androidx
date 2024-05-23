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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;
import android.util.Pair;

import androidx.camera.core.impl.Quirk;
import androidx.camera.core.internal.compat.workaround.CaptureFailedRetryEnabler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 242683221
 *     Description: Quirk that allows ImageCapture to retry once when encountering capture
 *     failures. On Samsung sm-g981u1, image capture requests might fail when continuously taking
 *     multiple pictures. The capture failure might be related to some problems in the CamX
 *     pipeline. Re-issue the original capture request to retry can workaround the issue.
 *     Device(s): Samsung sm-g981u1
 *     @see CaptureFailedRetryEnabler
 */
public class CaptureFailedRetryQuirk implements Quirk {

    private static final Set<Pair<String, String>> FAILED_RETRY_ALLOW_LIST = new HashSet<>(
            Collections.singletonList(Pair.create("SAMSUNG", "SM-G981U1")));

    static boolean load() {
        String brand = Build.BRAND.toUpperCase(Locale.US);
        String model = Build.MODEL.toUpperCase(Locale.US);

        return FAILED_RETRY_ALLOW_LIST.contains(Pair.create(brand, model));
    }

    /**
     * Returns the count which the image capture request can be retried. Currently returns 1
     * always if the quirk is loaded.
     */
    public int getRetryCount() {
        return 1;
    }
}
