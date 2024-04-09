/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.exifinterface.media;

import android.content.res.Resources;

import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.exifinterface.test.R;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;

/** Expected Exif attributes for test images in the res/raw/ directory. */
final class ExpectedAttributes {

    /** Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_ii}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_II =
            new Builder()
                    .setThumbnailOffsetAndLength(3500, 6265)
                    .setThumbnailSize(512, 288)
                    .setIsThumbnailCompressed(true)
                    .setMake("SAMSUNG")
                    .setMakeOffsetAndLength(160, 8)
                    .setModel("SM-N900S")
                    .setAperture(2.2)
                    .setDateTimeOriginal("2016:01:29 18:32:27")
                    .setExposureTime(1.0 / 30)
                    .setFocalLength("413/100")
                    .setImageSize(640, 480)
                    .setIso("50")
                    .setOrientation(ExifInterface.ORIENTATION_ROTATE_90)
                    .build();

    /**
     * Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_ii} when only the Exif data is
     * read using {@link ExifInterface#STREAM_TYPE_EXIF_DATA_ONLY}.
     */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_II_STANDALONE =
            JPEG_WITH_EXIF_BYTE_ORDER_II
                    .buildUpon()
                    .setThumbnailOffset(JPEG_WITH_EXIF_BYTE_ORDER_II.thumbnailOffset - 6)
                    .setMakeOffset(JPEG_WITH_EXIF_BYTE_ORDER_II.makeOffset - 6)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_mm}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_MM =
            new Builder()
                    .setLatitudeOffsetAndLength(584, 24)
                    .setLatLong(0, 0)
                    .setAltitude(0)
                    .setMake("LGE")
                    .setMakeOffsetAndLength(414, 4)
                    .setModel("Nexus 5")
                    .setAperture(2.4)
                    .setDateTimeOriginal("2016:01:29 15:44:58")
                    .setExposureTime(1.0 / 60)
                    .setFocalLength("3970/1000")
                    .setGpsAltitude("0/1000")
                    .setGpsAltitudeRef("0")
                    .setGpsDatestamp("1970:01:01")
                    .setGpsLatitude("0/1,0/1,0/10000")
                    .setGpsLatitudeRef("N")
                    .setGpsLongitude("0/1,0/1,0/10000")
                    .setGpsLongitudeRef("E")
                    .setGpsProcessingMethod("GPS")
                    .setGpsTimestamp("00:00:00")
                    .setImageSize(144, 176)
                    .setIso("146")
                    .build();

    /**
     * Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_mm} when only the Exif data is
     * read using {@link ExifInterface#STREAM_TYPE_EXIF_DATA_ONLY}.
     */
    public static final ExpectedAttributes JPEG_WITH_EXIF_BYTE_ORDER_MM_STANDALONE =
            JPEG_WITH_EXIF_BYTE_ORDER_MM
                    .buildUpon()
                    .setLatitudeOffset(JPEG_WITH_EXIF_BYTE_ORDER_MM.latitudeOffset - 6)
                    .setMakeOffset(JPEG_WITH_EXIF_BYTE_ORDER_MM.makeOffset - 6)
                    .setImageSize(0, 0)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_exif_invalid_offset}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_INVALID_OFFSET =
            JPEG_WITH_EXIF_BYTE_ORDER_MM
                    .buildUpon()
                    .setAperture(0)
                    .setDateTimeOriginal(null)
                    .setExposureTime(0)
                    .setFocalLength(null)
                    .setIso(null)
                    .build();

    /** Expected attributes for {@link R.raw#dng_with_exif_with_xmp}. */
    public static final ExpectedAttributes DNG_WITH_EXIF_WITH_XMP =
            new Builder()
                    .setThumbnailOffsetAndLength(12570, 15179)
                    .setThumbnailSize(256, 144)
                    .setIsThumbnailCompressed(true)
                    .setLatitudeOffsetAndLength(12486, 24)
                    .setLatLong(53.83450833333334, 10.69585)
                    .setAltitude(0)
                    .setMake("LGE")
                    .setMakeOffsetAndLength(102, 4)
                    .setModel("LG-H815")
                    .setAperture(1.8)
                    .setDateTimeOriginal("2015:11:12 16:46:18")
                    .setExposureTime(0.0040)
                    .setFocalLength("442/100")
                    .setGpsDatestamp("1970:01:17")
                    .setGpsLatitude("53/1,50/1,423/100")
                    .setGpsLatitudeRef("N")
                    .setGpsLongitude("10/1,41/1,4506/100")
                    .setGpsLongitudeRef("E")
                    .setGpsTimestamp("18:08:10")
                    .setImageSize(600, 337)
                    .setIso("800")
                    .setXmpResourceId(R.raw.dng_xmp)
                    .setXmpOffsetAndLength(826, 10067)
                    .build();

    /** Expected attributes for {@link R.raw#jpeg_with_exif_with_xmp}. */
    public static final ExpectedAttributes JPEG_WITH_EXIF_WITH_XMP =
            DNG_WITH_EXIF_WITH_XMP
                    .buildUpon()
                    .clearThumbnail()
                    .setLatitudeOffset(1692)
                    .setMakeOffset(84)
                    .setOrientation(ExifInterface.ORIENTATION_NORMAL)
                    .setXmpResourceId(R.raw.jpeg_xmp)
                    .setXmpOffsetAndLength(1809, 13197)
                    .build();

    /** Expected attributes for {@link R.raw#png_with_exif_byte_order_ii}. */
    public static final ExpectedAttributes PNG_WITH_EXIF_BYTE_ORDER_II =
            JPEG_WITH_EXIF_BYTE_ORDER_II
                    .buildUpon()
                    .setThumbnailOffset(212271)
                    .setMakeOffset(211525)
                    .setFocalLength("41/10")
                    .build();

    /** Expected attributes for {@link R.raw#webp_with_exif}. */
    public static final ExpectedAttributes WEBP_WITH_EXIF =
            JPEG_WITH_EXIF_BYTE_ORDER_II
                    .buildUpon()
                    .setThumbnailOffset(9646)
                    .setMakeOffset(6306)
                    .build();

    /** Expected attributes for {@link R.raw#invalid_webp_with_jpeg_app1_marker}. */
    public static final ExpectedAttributes INVALID_WEBP_WITH_JPEG_APP1_MARKER =
            new Builder().setOrientation(ExifInterface.ORIENTATION_ROTATE_270).build();

    /** Expected attributes for {@link R.raw#heif_with_exif} when read on a device below API 31. */
    public static final ExpectedAttributes HEIF_WITH_EXIF_BELOW_API_31 =
            new Builder()
                    .setMake("LGE")
                    .setMakeOffsetAndLength(3519, 4)
                    .setModel("Nexus 5")
                    .setImageSize(1920, 1080)
                    .setOrientation(ExifInterface.ORIENTATION_NORMAL)
                    .build();

    /**
     * Expected attributes for {@link R.raw#heif_with_exif} when read on a device running API 31 or
     * above.
     */
    public static final ExpectedAttributes HEIF_WITH_EXIF_API_31_AND_ABOVE =
            HEIF_WITH_EXIF_BELOW_API_31
                    .buildUpon()
                    .setXmpResourceId(R.raw.heif_xmp)
                    .setXmpOffsetAndLength(3721, 3020)
                    .build();

    public static class Builder {
        // Thumbnail information.
        private boolean mHasThumbnail;
        private long mThumbnailOffset;
        private long mThumbnailLength;
        private int mThumbnailWidth;
        private int mThumbnailHeight;
        private boolean mIsThumbnailCompressed;

        // GPS information.
        private boolean mHasLatLong;
        private long mLatitudeOffset;
        private long mLatitudeLength;
        private double mLatitude;
        private double mLongitude;
        private double mAltitude;

        // Make information
        private boolean mHasMake;
        private long mMakeOffset;
        private long mMakeLength;
        @Nullable private String mMake;

        // Values.
        @Nullable private String mModel;
        private double mAperture;
        @Nullable private String mDateTimeOriginal;
        private double mExposureTime;
        private double mFlash;
        @Nullable private String mFocalLength;
        @Nullable private String mGpsAltitude;
        @Nullable private String mGpsAltitudeRef;
        @Nullable private String mGpsDatestamp;
        @Nullable private String mGpsLatitude;
        @Nullable private String mGpsLatitudeRef;
        @Nullable private String mGpsLongitude;
        @Nullable private String mGpsLongitudeRef;
        @Nullable private String mGpsProcessingMethod;
        @Nullable private String mGpsTimestamp;
        private int mImageLength;
        private int mImageWidth;
        @Nullable private String mIso;
        private int mOrientation;
        private int mWhiteBalance;

        // XMP information.
        private boolean mHasXmp;
        @Nullable private String mXmp;
        @Nullable private Integer mXmpResourceId;
        private long mXmpOffset;
        private long mXmpLength;

        Builder() {}

        private Builder(ExpectedAttributes attributes) {
            mHasThumbnail = attributes.hasThumbnail;
            mThumbnailOffset = attributes.thumbnailOffset;
            mThumbnailLength = attributes.thumbnailLength;
            mThumbnailWidth = attributes.thumbnailWidth;
            mThumbnailHeight = attributes.thumbnailHeight;
            mIsThumbnailCompressed = attributes.isThumbnailCompressed;
            mHasLatLong = attributes.hasLatLong;
            mLatitude = attributes.latitude;
            mLatitudeOffset = attributes.latitudeOffset;
            mLatitudeLength = attributes.latitudeLength;
            mLongitude = attributes.longitude;
            mAltitude = attributes.altitude;
            mHasMake = attributes.hasMake;
            mMakeOffset = attributes.makeOffset;
            mMakeLength = attributes.makeLength;
            mMake = attributes.make;
            mModel = attributes.model;
            mAperture = attributes.aperture;
            mDateTimeOriginal = attributes.dateTimeOriginal;
            mExposureTime = attributes.exposureTime;
            mFocalLength = attributes.focalLength;
            mGpsAltitude = attributes.gpsAltitude;
            mGpsAltitudeRef = attributes.gpsAltitudeRef;
            mGpsDatestamp = attributes.gpsDatestamp;
            mGpsLatitude = attributes.gpsLatitude;
            mGpsLatitudeRef = attributes.gpsLatitudeRef;
            mGpsLongitude = attributes.gpsLongitude;
            mGpsLongitudeRef = attributes.gpsLongitudeRef;
            mGpsProcessingMethod = attributes.gpsProcessingMethod;
            mGpsTimestamp = attributes.gpsTimestamp;
            mImageLength = attributes.imageLength;
            mImageWidth = attributes.imageWidth;
            mIso = attributes.iso;
            mOrientation = attributes.orientation;
            mHasXmp = attributes.hasXmp;
            mXmp = attributes.mXmp;
            mXmpResourceId = attributes.mXmpResourceId;
            mXmpOffset = attributes.xmpOffset;
            mXmpLength = attributes.xmpLength;
        }

        public Builder setThumbnailSize(int width, int height) {
            mHasThumbnail = true;
            mThumbnailWidth = width;
            mThumbnailHeight = height;
            return this;
        }

        public Builder setIsThumbnailCompressed(boolean isThumbnailCompressed) {
            mHasThumbnail = true;
            mIsThumbnailCompressed = isThumbnailCompressed;
            return this;
        }

        public Builder setThumbnailOffsetAndLength(long offset, long length) {
            mHasThumbnail = true;
            mThumbnailOffset = offset;
            mThumbnailLength = length;
            return this;
        }

        public Builder setThumbnailOffset(long offset) {
            if (!mHasThumbnail) {
                throw new IllegalStateException(
                        "Thumbnail position in the file must first be set with "
                                + "setThumbnailOffsetAndLength(...)");
            }
            mThumbnailOffset = offset;
            return this;
        }

        public Builder clearThumbnail() {
            mHasThumbnail = false;
            mThumbnailWidth = 0;
            mThumbnailHeight = 0;
            mThumbnailOffset = 0;
            mThumbnailLength = 0;
            mIsThumbnailCompressed = false;
            return this;
        }

        public Builder setLatLong(double latitude, double longitude) {
            mHasLatLong = true;
            mLatitude = latitude;
            mLongitude = longitude;
            return this;
        }

        public Builder setLatitudeOffsetAndLength(long offset, long length) {
            mHasLatLong = true;
            mLatitudeOffset = offset;
            mLatitudeLength = length;
            return this;
        }

        public Builder setLatitudeOffset(long offset) {
            if (!mHasLatLong) {
                throw new IllegalStateException(
                        "Latitude position in the file must first be "
                                + "set with setLatitudeOffsetAndLength(...)");
            }
            mLatitudeOffset = offset;
            return this;
        }

        public Builder clearLatLong() {
            mHasLatLong = false;
            mLatitude = 0;
            mLongitude = 0;
            return this;
        }

        public Builder setAltitude(double altitude) {
            mAltitude = altitude;
            return this;
        }

        public Builder setMake(@Nullable String make) {
            if (make == null) {
                mHasMake = false;
                mMakeOffset = 0;
                mMakeLength = 0;
            } else {
                mHasMake = true;
                mMake = make;
            }
            return this;
        }

        // TODO: b/270554381 - consider deriving length automatically from `make.length() + 1`
        //  (since the string is null-terminated in the format).
        public Builder setMakeOffsetAndLength(long offset, long length) {
            mHasMake = true;
            mMakeOffset = offset;
            mMakeLength = length;
            return this;
        }

        public Builder setMakeOffset(long offset) {
            if (!mHasMake) {
                throw new IllegalStateException(
                        "Make position in the file must first be set with"
                                + " setMakeOffsetAndLength(...)");
            }
            mMakeOffset = offset;
            return this;
        }

        public Builder setModel(@Nullable String model) {
            mModel = model;
            return this;
        }

        public Builder setAperture(double aperture) {
            mAperture = aperture;
            return this;
        }

        public Builder setDateTimeOriginal(@Nullable String dateTimeOriginal) {
            mDateTimeOriginal = dateTimeOriginal;
            return this;
        }

        public Builder setExposureTime(double exposureTime) {
            mExposureTime = exposureTime;
            return this;
        }

        public Builder setFlash(double flash) {
            mFlash = flash;
            return this;
        }

        public Builder setFocalLength(@Nullable String focalLength) {
            mFocalLength = focalLength;
            return this;
        }

        public Builder setGpsAltitude(@Nullable String gpsAltitude) {
            mGpsAltitude = gpsAltitude;
            return this;
        }

        public Builder setGpsAltitudeRef(@Nullable String gpsAltitudeRef) {
            mGpsAltitudeRef = gpsAltitudeRef;
            return this;
        }

        public Builder setGpsDatestamp(@Nullable String gpsDatestamp) {
            mGpsDatestamp = gpsDatestamp;
            return this;
        }

        public Builder setGpsLatitude(@Nullable String gpsLatitude) {
            mGpsLatitude = gpsLatitude;
            return this;
        }

        public Builder setGpsLatitudeRef(@Nullable String gpsLatitudeRef) {
            mGpsLatitudeRef = gpsLatitudeRef;
            return this;
        }

        public Builder setGpsLongitude(@Nullable String gpsLongitude) {
            mGpsLongitude = gpsLongitude;
            return this;
        }

        public Builder setGpsLongitudeRef(@Nullable String gpsLongitudeRef) {
            mGpsLongitudeRef = gpsLongitudeRef;
            return this;
        }

        public Builder setGpsProcessingMethod(@Nullable String gpsProcessingMethod) {
            mGpsProcessingMethod = gpsProcessingMethod;
            return this;
        }

        public Builder setGpsTimestamp(@Nullable String gpsTimestamp) {
            mGpsTimestamp = gpsTimestamp;
            return this;
        }

        public Builder setImageSize(int imageWidth, int imageLength) {
            mImageWidth = imageWidth;
            mImageLength = imageLength;
            return this;
        }

        public Builder setIso(@Nullable String iso) {
            mIso = iso;
            return this;
        }

        public Builder setOrientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        public Builder setWhiteBalance(int whiteBalance) {
            mWhiteBalance = whiteBalance;
            return this;
        }

        /**
         * Sets the expected XMP data.
         *
         * <p>Clears any value set by {@link #setXmpResourceId}.
         */
        public Builder setXmp(String xmp) {
            mHasXmp = true;
            mXmp = xmp;
            mXmpResourceId = null;
            return this;
        }

        /**
         * Sets the resource ID of the expected XMP data.
         *
         * <p>Clears any value set by {@link #setXmp}.
         */
        public Builder setXmpResourceId(@RawRes int xmpResourceId) {
            mHasXmp = true;
            mXmp = null;
            mXmpResourceId = xmpResourceId;
            return this;
        }

        public Builder setXmpOffsetAndLength(int offset, int length) {
            mHasXmp = true;
            mXmpOffset = offset;
            mXmpLength = length;
            return this;
        }

        public Builder setXmpOffset(int offset) {
            if (!mHasXmp) {
                throw new IllegalStateException(
                        "XMP position in the file must first be set with"
                                + " setXmpOffsetAndLength(...)");
            }
            mXmpOffset = offset;
            return this;
        }

        public Builder clearXmp() {
            mHasXmp = false;
            mXmp = null;
            mXmpResourceId = null;
            mXmpOffset = 0;
            mXmpLength = 0;
            return this;
        }

        public ExpectedAttributes build() {
            return new ExpectedAttributes(this);
        }
    }

    // TODO: b/270554381 - Add nullability annotations below.

    // Thumbnail information.
    public final boolean hasThumbnail;
    public final int thumbnailWidth;
    public final int thumbnailHeight;
    public final boolean isThumbnailCompressed;
    // TODO: b/270554381 - Merge these offset and length (and others) into long[] arrays, and
    //  move them down to their own section. This may also allow removing some of the hasXXX
    // fields.
    public final long thumbnailOffset;
    public final long thumbnailLength;

    // GPS information.
    public final boolean hasLatLong;
    // TODO: b/270554381 - Merge this and longitude into a double[]
    public final double latitude;
    public final long latitudeOffset;
    public final long latitudeLength;
    public final double longitude;
    public final double altitude;

    // Make information
    public final boolean hasMake;
    public final long makeOffset;
    public final long makeLength;
    public final String make;

    // Values.
    public final String model;
    public final double aperture;
    public final String dateTimeOriginal;
    public final double exposureTime;
    public final String focalLength;
    // TODO: b/270554381 - Rename these to make them clear they're strings, or original values,
    //  and move them closer to the (computed) latitude/longitude/altitude values. Consider
    //  also having a verification check that they are consistent with latitude/longitude (but
    //  not sure how to reconcile that with "don't duplicate business logic in tests").
    public final String gpsAltitude;
    public final String gpsAltitudeRef;
    public final String gpsDatestamp;
    public final String gpsLatitude;
    public final String gpsLatitudeRef;
    public final String gpsLongitude;
    public final String gpsLongitudeRef;
    public final String gpsProcessingMethod;
    public final String gpsTimestamp;
    public final int imageLength;
    public final int imageWidth;
    public final String iso;
    public final int orientation;

    // XMP information.
    @Nullable private final String mXmp;
    @Nullable private final Integer mXmpResourceId;
    @Nullable private String mMemoizedXmp;
    public final boolean hasXmp;
    public final long xmpOffset;
    public final long xmpLength;

    private ExpectedAttributes(Builder builder) {
        // TODO: b/270554381 - Re-order these assignments to match the fields above.
        hasThumbnail = builder.mHasThumbnail;
        thumbnailOffset = builder.mThumbnailOffset;
        thumbnailLength = builder.mThumbnailLength;
        thumbnailWidth = builder.mThumbnailWidth;
        thumbnailHeight = builder.mThumbnailHeight;
        isThumbnailCompressed = builder.mIsThumbnailCompressed;
        hasLatLong = builder.mHasLatLong;
        latitudeOffset = builder.mLatitudeOffset;
        latitudeLength = builder.mLatitudeLength;
        latitude = builder.mLatitude;
        longitude = builder.mLongitude;
        altitude = builder.mAltitude;
        hasMake = builder.mHasMake;
        makeOffset = builder.mMakeOffset;
        makeLength = builder.mMakeLength;
        make = builder.mMake;
        model = builder.mModel;
        aperture = builder.mAperture;
        dateTimeOriginal = builder.mDateTimeOriginal;
        exposureTime = builder.mExposureTime;
        focalLength = builder.mFocalLength;
        gpsAltitude = builder.mGpsAltitude;
        gpsAltitudeRef = builder.mGpsAltitudeRef;
        gpsDatestamp = builder.mGpsDatestamp;
        gpsLatitude = builder.mGpsLatitude;
        gpsLatitudeRef = builder.mGpsLatitudeRef;
        gpsLongitude = builder.mGpsLongitude;
        gpsLongitudeRef = builder.mGpsLongitudeRef;
        gpsProcessingMethod = builder.mGpsProcessingMethod;
        gpsTimestamp = builder.mGpsTimestamp;
        imageLength = builder.mImageLength;
        imageWidth = builder.mImageWidth;
        iso = builder.mIso;
        orientation = builder.mOrientation;
        hasXmp = builder.mHasXmp;
        mXmp = builder.mXmp;
        mXmpResourceId = builder.mXmpResourceId;
        Preconditions.checkArgument(
                mXmp == null || mXmpResourceId == null,
                "At most one of mXmp or mXmpResourceId may be set");
        mMemoizedXmp = mXmp;
        xmpOffset = builder.mXmpOffset;
        xmpLength = builder.mXmpLength;
    }

    /**
     * Returns the expected XMP data set directly with {@link Builder#setXmp} or read from {@code
     * resources} using {@link Builder#setXmpResourceId}.
     *
     * <p>Returns null if no expected XMP data was set.
     */
    @Nullable
    public String getXmp(Resources resources) throws IOException {
        if (mMemoizedXmp == null && mXmpResourceId != null) {
            try (InputStreamReader inputStreamReader =
                    new InputStreamReader(
                            resources.openRawResource(mXmpResourceId), Charsets.UTF_8)) {
                mMemoizedXmp = CharStreams.toString(inputStreamReader);
            }
        }
        return mMemoizedXmp;
    }

    public Builder buildUpon() {
        return new Builder(this);
    }
}
