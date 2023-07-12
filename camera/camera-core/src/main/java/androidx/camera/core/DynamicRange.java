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

package androidx.camera.core;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** A representation of the dynamic range of an image. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DynamicRange {
    /**
     * An unspecified dynamic range format which allows the device to determine the underlying
     * dynamic range format.
     */
    public static final int FORMAT_UNSPECIFIED = 0;

    /** Standard Dynamic Range (SDR) format. */
    public static final int FORMAT_SDR = 1;

    //------------------------------------------------------------------------------//
    //                            HDR Formats                                       //
    //------------------------------------------------------------------------------//
    /**
     * An unspecified dynamic range format which allows the device to determine the
     * underlying dynamic range format, limited to High Dynamic Range (HDR) encodings.
     */
    public static final int FORMAT_HDR_UNSPECIFIED = 2;
    /** Hybrid Log Gamma (HLG) dynamic range format. */
    public static final int FORMAT_HLG = FORMAT_HDR_UNSPECIFIED + 1;
    /** HDR10 dynamic range format. */
    public static final int FORMAT_HDR10 = FORMAT_HDR_UNSPECIFIED + 2;
    /** HDR10+ dynamic range format. */
    public static final int FORMAT_HDR10_PLUS = FORMAT_HDR_UNSPECIFIED + 3;
    /** Dolby Vision dynamic range format. */
    public static final int FORMAT_DOLBY_VISION = FORMAT_HDR_UNSPECIFIED + 4;
    //------------------------------------------------------------------------------//

    /** Bit depth is unspecified and may be determined automatically by the device. */
    public static final int BIT_DEPTH_UNSPECIFIED = 0;
    /** Eight-bit bit depth. */
    public static final int BIT_DEPTH_8_BIT = 8;
    /** Ten-bit bit depth. */
    public static final int BIT_DEPTH_10_BIT = 10;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({FORMAT_UNSPECIFIED, FORMAT_SDR, FORMAT_HDR_UNSPECIFIED, FORMAT_HLG, FORMAT_HDR10,
            FORMAT_HDR10_PLUS, FORMAT_DOLBY_VISION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DynamicRangeFormat {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({BIT_DEPTH_UNSPECIFIED, BIT_DEPTH_8_BIT, BIT_DEPTH_10_BIT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BitDepth {
    }

    /**
     * A dynamic range with unspecified format and bit depth
     *
     * <p>The dynamic range is unspecified and may defer to device defaults when used to select a
     * dynamic range.
     */
    @NonNull
    public static final DynamicRange UNSPECIFIED = new DynamicRange(FORMAT_UNSPECIFIED,
            BIT_DEPTH_UNSPECIFIED);

    /** A dynamic range representing 8-bit standard dynamic range (SDR). */
    @NonNull
    public static final DynamicRange SDR = new DynamicRange(FORMAT_SDR, BIT_DEPTH_8_BIT);

    /**
     * A dynamic range representing 10-bit high dynamic range (HDR) with unspecified format.
     *
     * <p>The HDR format is unspecified, and may defer to device defaults
     * when used to select a dynamic range. In this case, the dynamic range will be limited to
     * 10-bit high dynamic ranges.
     */
    @NonNull
    public static final DynamicRange HDR_UNSPECIFIED_10_BIT =
            new DynamicRange(FORMAT_HDR_UNSPECIFIED, BIT_DEPTH_10_BIT);

    private final @DynamicRangeFormat int mFormat;
    private final @BitDepth int mBitDepth;

    /**
     * Creates a dynamic range representation from a format and bit depth.
     *
     * <p>This constructor is left public for testing purposes. It does not do any verification that
     * the provided arguments are a valid combination of format and bit depth.
     *
     * @param format   The dynamic range format.
     * @param bitDepth The bit depth.
     */
    public DynamicRange(
            @DynamicRangeFormat int format,
            @BitDepth int bitDepth) {
        mFormat = format;
        mBitDepth = bitDepth;
    }

    /**
     * Returns the dynamic range format.
     *
     * @return The dynamic range format. Possible values are {@link #FORMAT_SDR},
     * {@link #FORMAT_HLG}, {@link #FORMAT_HDR10}, {@link #FORMAT_HDR10_PLUS}, or
     * {@link #FORMAT_DOLBY_VISION}.
     */
    @DynamicRangeFormat
    public int getFormat() {
        return mFormat;
    }

    /**
     * Returns the bit depth used by this dynamic range configuration.
     *
     * <p>Common values are {@link #BIT_DEPTH_8_BIT}, such as for {@link #FORMAT_SDR} or
     * {@link #BIT_DEPTH_10_BIT}, such as for {@link #FORMAT_HDR10}.
     *
     * @return The bit depth. Possible values are {@link #BIT_DEPTH_8_BIT},
     * {@link #BIT_DEPTH_10_BIT}, or {@link #BIT_DEPTH_UNSPECIFIED}.
     */
    @BitDepth
    public int getBitDepth() {
        return mBitDepth;
    }

    @NonNull
    @Override
    public String toString() {
        return "DynamicRange@" + Integer.toHexString(System.identityHashCode(this)) + "{"
                + "format=" + getFormatLabel(mFormat) + ", "
                + "bitDepth=" + mBitDepth
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DynamicRange) {
            DynamicRange that = (DynamicRange) o;
            return this.mFormat == that.getFormat()
                    && this.mBitDepth == that.getBitDepth();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode *= 1000003;
        hashCode ^= mFormat;
        hashCode *= 1000003;
        hashCode ^= mBitDepth;
        return hashCode;
    }

    @NonNull
    private static String getFormatLabel(@DynamicRangeFormat int format) {
        switch (format) {
            case FORMAT_UNSPECIFIED: return "FORMAT_UNSPECIFIED";
            case FORMAT_SDR: return "FORMAT_SDR";
            case FORMAT_HDR_UNSPECIFIED: return "FORMAT_HDR_UNSPECIFIED";
            case FORMAT_HLG: return "FORMAT_HLG";
            case FORMAT_HDR10: return "FORMAT_HDR10";
            case FORMAT_HDR10_PLUS: return "FORMAT_HDR10_PLUS";
            case FORMAT_DOLBY_VISION: return "FORMAT_DOLBY_VISION";
        }

        return "<Unknown>";
    }
}
