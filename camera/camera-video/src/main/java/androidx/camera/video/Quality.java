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

import static android.media.CamcorderProfile.QUALITY_1080P;
import static android.media.CamcorderProfile.QUALITY_2160P;
import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_720P;
import static android.media.CamcorderProfile.QUALITY_HIGH;
import static android.media.CamcorderProfile.QUALITY_HIGH_SPEED_1080P;
import static android.media.CamcorderProfile.QUALITY_HIGH_SPEED_2160P;
import static android.media.CamcorderProfile.QUALITY_HIGH_SPEED_480P;
import static android.media.CamcorderProfile.QUALITY_HIGH_SPEED_720P;
import static android.media.CamcorderProfile.QUALITY_HIGH_SPEED_HIGH;
import static android.media.CamcorderProfile.QUALITY_HIGH_SPEED_LOW;
import static android.media.CamcorderProfile.QUALITY_LOW;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import android.util.Size;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class representing video quality constraints that will be used by {@link QualitySelector} to
 * choose video resolution and appropriate encoding parameters.
 */
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
    @NonNull
    public static final Quality SD = ConstantQuality.of(
            QUALITY_480P,
            QUALITY_HIGH_SPEED_480P,
            "SD",
            unmodifiableList(asList(new Size(720, 480), new Size(640, 480))
            ));

    /**
     * High Definition (HD) video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1280 x 720 (720p) pixels.
     */
    @NonNull
    public static final Quality HD = ConstantQuality.of(
            QUALITY_720P,
            QUALITY_HIGH_SPEED_720P,
            "HD",
            singletonList(new Size(1280, 720)
            ));

    /**
     * Full High Definition (FHD) 1080p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1920 x 1080 (1080p) pixels.
     */
    @NonNull
    public static final Quality FHD = ConstantQuality.of(
            QUALITY_1080P,
            QUALITY_HIGH_SPEED_1080P,
            "FHD",
            singletonList(new Size(1920, 1080)
            ));

    /**
     * Ultra High Definition (UHD) 2160p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 3840 x 2160 (2160p) pixels.
     */
    @NonNull
    public static final Quality UHD = ConstantQuality.of(
            QUALITY_2160P,
            QUALITY_HIGH_SPEED_2160P,
            "UHD",
            singletonList(new Size(3840, 2160)
            ));

    /**
     * The lowest video quality supported by the video frame producer.
     */
    @NonNull
    public static final Quality LOWEST = ConstantQuality.of(
            QUALITY_LOW,
            QUALITY_HIGH_SPEED_LOW,
            "LOWEST",
            emptyList()
    );

    /**
     * The highest video quality supported by the video frame producer.
     */
    @NonNull
    public static final Quality HIGHEST = ConstantQuality.of(
            QUALITY_HIGH,
            QUALITY_HIGH_SPEED_HIGH,
            "HIGHEST",
            emptyList()
    );

    static final Quality NONE = ConstantQuality.of(-1, -1, "NONE", emptyList());

    /** All quality constants. */
    private static final Set<Quality> QUALITIES =
            new HashSet<>(asList(LOWEST, HIGHEST, SD, HD, FHD, UHD));

    /** Quality constants with size from large to small. */
    private static final List<Quality> QUALITIES_ORDER_BY_SIZE = asList(UHD, FHD, HD, SD);

    static boolean containsQuality(@NonNull Quality quality) {
        return QUALITIES.contains(quality);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int QUALITY_SOURCE_REGULAR = 1;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int QUALITY_SOURCE_HIGH_SPEED = 2;

    @IntDef({QUALITY_SOURCE_REGULAR, QUALITY_SOURCE_HIGH_SPEED})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface QualitySource {
    }

    /**
     * Gets all video quality constants with clearly defined size sorted from largest to smallest.
     *
     * <p>{@link #HIGHEST} and {@link #LOWEST} are not included.
     */
    @RestrictTo(Scope.LIBRARY)
    public static @NonNull List<Quality> getSortedQualities() {
        return new ArrayList<>(QUALITIES_ORDER_BY_SIZE);
    }

    @RestrictTo(Scope.LIBRARY)
    @AutoValue
    public abstract static class ConstantQuality extends Quality {
        static ConstantQuality of(int value, int highSpeedValue, @NonNull String name,
                @NonNull List<Size> typicalSizes) {
            return new AutoValue_Quality_ConstantQuality(value, highSpeedValue, name, typicalSizes);
        }

        /** Gets the quality value corresponding to CamcorderProfile quality constant. */
        abstract int getValue();

        /** Gets the quality value corresponding to CamcorderProfile high speed quality constant. */
        abstract int getHighSpeedValue();

        /** Gets the quality name. */
        public abstract @NonNull String getName();

        /** Gets the typical sizes of the quality. */
        @SuppressWarnings("AutoValueImmutableFields")
        public abstract @NonNull List<Size> getTypicalSizes();

        /** Gets the quality value for the given quality source. */
        public int getQualityValue(@QualitySource int qualitySource) {
            switch (qualitySource) {
                case QUALITY_SOURCE_REGULAR:
                    return getValue();
                case QUALITY_SOURCE_HIGH_SPEED:
                    return getHighSpeedValue();
                default:
                    throw new AssertionError("Unknown quality source: " + qualitySource);

            }
        }
    }
}
