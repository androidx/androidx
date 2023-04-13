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

package androidx.camera.camera2.internal.compat.quirk;

import android.media.EncoderProfiles;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Quirk denoting the video profile list returns by {@link EncoderProfiles} is invalid.
 *
 * <p>QuirkSummary
 *     Bug Id: 267727595
 *     Description: When using {@link EncoderProfiles} on TP1A or TD1A builds of Android API 33,
 *                  {@link EncoderProfiles#getVideoProfiles()} returns a list with size one, but
 *                  the single value in the list is null. This is not the expected behavior, and
 *                  makes {@link EncoderProfiles} lack of video information.
 *     Device(s): Pixel 4 and above pixel devices with TP1A or TD1A builds (API 33).
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class InvalidVideoProfilesQuirk implements Quirk {

    static final List<String> AFFECTED_MODELS = Arrays.asList(
            "pixel 4",
            "pixel 4a",
            "pixel 4a (5g)",
            "pixel 4 xl",
            "pixel 5",
            "pixel 5a",
            "pixel 6",
            "pixel 6a",
            "pixel 6 pro",
            "pixel 7",
            "pixel 7 pro"
    );

    static boolean load() {
        return isAffectedModel() && isAffectedBuild();
    }

    private static boolean isAffectedModel() {
        return AFFECTED_MODELS.contains(Build.MODEL.toLowerCase(Locale.US));
    }

    private static boolean isAffectedBuild() {
        return isTp1aBuild() || isTd1aBuild();
    }

    private static boolean isTp1aBuild() {
        return Build.ID.startsWith("TP1A");
    }

    private static boolean isTd1aBuild() {
        return Build.ID.startsWith("TD1A");
    }
}
