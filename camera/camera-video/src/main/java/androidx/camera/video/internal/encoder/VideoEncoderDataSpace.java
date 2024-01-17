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

package androidx.camera.video.internal.encoder;

import android.media.MediaFormat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/**
 * Defines the three components of colors used by an encoder.
 *
 * <p>This is the encoder equivalent of {@link android.hardware.DataSpace}, and should be used to
 * communicate the {@link MediaFormat} keys needed by the encoder.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class VideoEncoderDataSpace {

    /** Standard characteristics that are unknown or specified by the device defaults. */
    public static final int VIDEO_COLOR_STANDARD_UNSPECIFIED = 0;

    /** Color transfer function that is unknown or specified by the device defaults. */
    public static final int VIDEO_COLOR_TRANSFER_UNSPECIFIED = 0;

    /** Range characteristics that are unknown or specified by the device defaults. */
    public static final int VIDEO_COLOR_RANGE_UNSPECIFIED = 0;

    /** A data space where all components are unspecified. */
    public static final VideoEncoderDataSpace ENCODER_DATA_SPACE_UNSPECIFIED =
            create(VIDEO_COLOR_STANDARD_UNSPECIFIED,
                    VIDEO_COLOR_TRANSFER_UNSPECIFIED,
                    VIDEO_COLOR_RANGE_UNSPECIFIED);

    /**
     * Color standard BT.709 with SDR video transfer function.
     *
     * <p>This mirrors the data space from {@link android.hardware.DataSpace#DATASPACE_BT709}.
     */
    public static final VideoEncoderDataSpace ENCODER_DATA_SPACE_BT709 =
            create(MediaFormat.COLOR_STANDARD_BT709,
                    MediaFormat.COLOR_TRANSFER_SDR_VIDEO,
                    MediaFormat.COLOR_RANGE_LIMITED);

    /**
     * Color standard BT.2020 with HLG transfer function.
     *
     * <p>This mirrors the data space from {@link android.hardware.DataSpace#DATASPACE_BT2020_HLG}.
     */
    public static final VideoEncoderDataSpace ENCODER_DATA_SPACE_BT2020_HLG =
            create(MediaFormat.COLOR_STANDARD_BT2020,
                    MediaFormat.COLOR_TRANSFER_HLG,
                    MediaFormat.COLOR_RANGE_FULL);

    /**
     * Color standard BT.2020 with PQ (ST2084) transfer function.
     *
     * <p>This mirrors the data space from {@link android.hardware.DataSpace#DATASPACE_BT2020_PQ}.
     */
    public static final VideoEncoderDataSpace ENCODER_DATA_SPACE_BT2020_PQ =
            create(MediaFormat.COLOR_STANDARD_BT2020,
                    MediaFormat.COLOR_TRANSFER_ST2084,
                    MediaFormat.COLOR_RANGE_FULL);

    // Restrict constructor to same package
    VideoEncoderDataSpace() {
    }

    /** Creates a data space from the three primaries. */
    @NonNull
    public static VideoEncoderDataSpace create(int standard, int transfer, int range) {
        return new AutoValue_VideoEncoderDataSpace(standard, transfer, range);
    }

    /**
     * Returns the color standard.
     *
     * <p>This will be one of {@link #VIDEO_COLOR_STANDARD_UNSPECIFIED} or one of the color
     * standard constants defined in {@link MediaFormat}, such as
     * {@link MediaFormat#COLOR_STANDARD_BT2020}.
     */
    public abstract int getStandard();

    /**
     * Returns the color transfer function.
     *
     * <p>This will be one of {@link #VIDEO_COLOR_TRANSFER_UNSPECIFIED} or one of the color
     * transfer function constants defined in {@link MediaFormat}, such as
     * {@link MediaFormat#COLOR_TRANSFER_HLG}.
     */
    public abstract int getTransfer();

    /**
     * Returns the color range.
     *
     * <p>This will be one of {@link #VIDEO_COLOR_RANGE_UNSPECIFIED} or one of the color
     * range constants defined in {@link MediaFormat}, such as
     * {@link MediaFormat#COLOR_RANGE_LIMITED}.
     */
    public abstract int getRange();
}
