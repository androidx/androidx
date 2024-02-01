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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import android.media.CamcorderProfile;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class representing video quality constraints that will be used by {@link QualitySelector} to
 * choose video resolution and appropriate encoding parameters.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Quality {

    // Restrict access to sealed class
    private Quality() {
    }

    /**
     * Standard Definition (SD) 480p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 720 x 480 or 640 x 480 (480p)
     * pixels.
     */
    public static final Quality SD = ConstantQuality.of(CamcorderProfile.QUALITY_480P, "SD",
            unmodifiableList(asList(new Size(720, 480), new Size(640, 480))));

    /**
     * High Definition (HD) video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1280 x 720 (720p) pixels.
     */
    public static final Quality HD = ConstantQuality.of(CamcorderProfile.QUALITY_720P, "HD",
            singletonList(new Size(1280, 720)));

    /**
     * Full High Definition (FHD) 1080p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1920 x 1080 (1080p) pixels.
     */
    public static final Quality FHD = ConstantQuality.of(CamcorderProfile.QUALITY_1080P, "FHD",
            singletonList(new Size(1920, 1080)));

    /**
     * Ultra High Definition (UHD) 2160p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 3840 x 2160 (2160p) pixels.
     */
    public static final Quality UHD = ConstantQuality.of(CamcorderProfile.QUALITY_2160P, "UHD",
            singletonList(new Size(3840, 2160)));

    /**
     * The lowest video quality supported by the video frame producer.
     */
    public static final Quality LOWEST = ConstantQuality.of(CamcorderProfile.QUALITY_LOW, "LOWEST",
            emptyList());

    /**
     * The highest video quality supported by the video frame producer.
     */
    public static final Quality HIGHEST = ConstantQuality.of(CamcorderProfile.QUALITY_HIGH,
            "HIGHEST", emptyList());

    static final Quality NONE = ConstantQuality.of(-1, "NONE", emptyList());

    /** All quality constants. */
    private static final Set<Quality> QUALITIES =
            new HashSet<>(asList(LOWEST, HIGHEST, SD, HD, FHD, UHD));

    /** Quality constants with size from large to small. */
    private static final List<Quality> QUALITIES_ORDER_BY_SIZE = asList(UHD, FHD, HD, SD);

    static boolean containsQuality(@NonNull Quality quality) {
        return QUALITIES.contains(quality);
    }

    /**
     * Gets all video quality constants with clearly defined size sorted from largest to smallest.
     *
     * <p>{@link #HIGHEST} and {@link #LOWEST} are not included.
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public static List<Quality> getSortedQualities() {
        return new ArrayList<>(QUALITIES_ORDER_BY_SIZE);
    }

    @RequiresApi(21)
    @RestrictTo(Scope.LIBRARY)
    @AutoValue
    public abstract static class ConstantQuality extends Quality {
        @NonNull
        static ConstantQuality of(int value, @NonNull String name,
                @NonNull List<Size> typicalSizes) {
            return new AutoValue_Quality_ConstantQuality(value, name, typicalSizes);
        }

        /** Gets the quality value corresponding to CamcorderProfile quality constant. */
        public abstract int getValue();

        /** Gets the quality name. */
        @NonNull
        public abstract String getName();

        /** Gets the typical sizes of the quality. */
        @SuppressWarnings("AutoValueImmutableFields")
        @NonNull
        public abstract List<Size> getTypicalSizes();
    }
}
