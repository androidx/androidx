/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.StrictMode;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link ExifInterface}.
 */
// TODO: Add NEF test file from CTS after reducing file size in order to test uncompressed thumbnail
// image.
@RunWith(AndroidJUnit4.class)
public class ExifInterfaceTest {
    private static final String TAG = ExifInterface.class.getSimpleName();
    private static final boolean VERBOSE = false;  // lots of logging
    private static final double DIFFERENCE_TOLERANCE = .001;
    private static final boolean ENABLE_STRICT_MODE_FOR_UNBUFFERED_IO = true;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final String WEBP_WITHOUT_EXIF_WITH_ANIM_DATA =
            "webp_with_anim_without_exif.webp";

    private static final double DELTA = 1e-8;
    // We translate double to rational in a 1/10000 precision.
    private static final double RATIONAL_DELTA = 0.0001;
    private static final int TEST_LAT_LONG_VALUES_ARRAY_LENGTH = 8;
    private static final double[] TEST_LATITUDE_VALID_VALUES = new double[]
            {0, 45, 90, -60, 0.00000001, -89.999999999, 14.2465923626, -68.3434534737};
    private static final double[] TEST_LONGITUDE_VALID_VALUES = new double[]
            {0, -45, 90, -120, 180, 0.00000001, -179.99999999999, -58.57834236352};
    private static final double[] TEST_LATITUDE_INVALID_VALUES = new double[]
            {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 90.0000000001,
                    263.34763236326, -1e5, 347.32525, -176.346347754};
    private static final double[] TEST_LONGITUDE_INVALID_VALUES = new double[]
            {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 180.0000000001,
                    263.34763236326, -1e10, 347.325252623, -4000.346323236};
    private static final double[] TEST_ALTITUDE_VALUES = new double[]
            {0, -2000, 10000, -355.99999999999, 18.02038};
    private static final int[][] TEST_ROTATION_STATE_MACHINE = {
            {ExifInterface.ORIENTATION_UNDEFINED, -90, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_UNDEFINED, 0, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_UNDEFINED, 90, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_UNDEFINED, 180, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_UNDEFINED, 270, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_UNDEFINED, 540, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_NORMAL, -90, ExifInterface.ORIENTATION_ROTATE_270},
            {ExifInterface.ORIENTATION_NORMAL, 0, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_NORMAL, 90, ExifInterface.ORIENTATION_ROTATE_90},
            {ExifInterface.ORIENTATION_NORMAL, 180, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_NORMAL, 270, ExifInterface.ORIENTATION_ROTATE_270},
            {ExifInterface.ORIENTATION_NORMAL, 540, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_ROTATE_90, -90, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_ROTATE_90, 0, ExifInterface.ORIENTATION_ROTATE_90},
            {ExifInterface.ORIENTATION_ROTATE_90, 90, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_ROTATE_90, 180 , ExifInterface.ORIENTATION_ROTATE_270},
            {ExifInterface.ORIENTATION_ROTATE_90, 270, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_ROTATE_90, 540, ExifInterface.ORIENTATION_ROTATE_270},
            {ExifInterface.ORIENTATION_ROTATE_180, -90, ExifInterface.ORIENTATION_ROTATE_90},
            {ExifInterface.ORIENTATION_ROTATE_180, 0, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_ROTATE_180, 90, ExifInterface.ORIENTATION_ROTATE_270},
            {ExifInterface.ORIENTATION_ROTATE_180, 180, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_ROTATE_180, 270, ExifInterface.ORIENTATION_ROTATE_90},
            {ExifInterface.ORIENTATION_ROTATE_180, 540, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_ROTATE_270, -90, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_ROTATE_270, 0, ExifInterface.ORIENTATION_ROTATE_270},
            {ExifInterface.ORIENTATION_ROTATE_270, 90, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_ROTATE_270, 180, ExifInterface.ORIENTATION_ROTATE_90},
            {ExifInterface.ORIENTATION_ROTATE_270, 270, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_ROTATE_270, 540, ExifInterface.ORIENTATION_ROTATE_90},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, -90, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, 0, ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, 90, ExifInterface.ORIENTATION_TRANSPOSE},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, 180,
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, 270, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, 540,
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, -90, ExifInterface.ORIENTATION_TRANSPOSE},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 0,
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 90, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 180,
                    ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 270, ExifInterface.ORIENTATION_TRANSPOSE},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 540,
                    ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_TRANSPOSE, -90, ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_TRANSPOSE, 0, ExifInterface.ORIENTATION_TRANSPOSE},
            {ExifInterface.ORIENTATION_TRANSPOSE, 90, ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_TRANSPOSE, 180, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_TRANSPOSE, 270, ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_TRANSPOSE, 540, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_TRANSVERSE, -90, ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_TRANSVERSE, 0, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_TRANSVERSE, 90, ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_TRANSVERSE, 180, ExifInterface.ORIENTATION_TRANSPOSE},
            {ExifInterface.ORIENTATION_TRANSVERSE, 270, ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_TRANSVERSE, 540, ExifInterface.ORIENTATION_TRANSPOSE},
    };
    private static final int[][] TEST_FLIP_VERTICALLY_STATE_MACHINE = {
            {ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSPOSE},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_ROTATE_270},
            {ExifInterface.ORIENTATION_TRANSVERSE, ExifInterface.ORIENTATION_ROTATE_90}
    };
    private static final int[][] TEST_FLIP_HORIZONTALLY_STATE_MACHINE = {
            {ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_UNDEFINED},
            {ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_FLIP_HORIZONTAL},
            {ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE},
            {ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL},
            {ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE},
            {ExifInterface.ORIENTATION_FLIP_VERTICAL, ExifInterface.ORIENTATION_ROTATE_180},
            {ExifInterface.ORIENTATION_FLIP_HORIZONTAL, ExifInterface.ORIENTATION_NORMAL},
            {ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_ROTATE_90},
            {ExifInterface.ORIENTATION_TRANSVERSE, ExifInterface.ORIENTATION_ROTATE_270}
    };
    private static final HashMap<Integer, Pair<Boolean, Integer>> FLIP_STATE_AND_ROTATION_DEGREES =
            new HashMap<>();
    static {
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_UNDEFINED, new Pair<>(false, 0));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_NORMAL, new Pair<>(false, 0));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_ROTATE_90, new Pair<>(false, 90));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_ROTATE_180, new Pair<>(false, 180));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_ROTATE_270, new Pair<>(false, 270));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL, new Pair<>(true, 0));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_TRANSVERSE, new Pair<>(true, 90));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_FLIP_VERTICAL, new Pair<>(true, 180));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_TRANSPOSE, new Pair<>(true, 270));
    }

    private static final String[] EXIF_TAGS = {
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_IMAGE_LENGTH,
            ExifInterface.TAG_IMAGE_WIDTH,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_WHITE_BALANCE
    };

    // TODO: b/270554381 - Rename this to ExpectedAttributes, make it final, and move it to the
    //  bottom of the file.
    private static class ExpectedValue {

        /** Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_ii}. */
        public static final ExpectedValue JPEG_WITH_EXIF_BYTE_ORDER_II =
                new Builder()
                        .setThumbnailOffsetAndLength(3500, 6265)
                        .setThumbnailSize(512, 288)
                        .setIsThumbnailCompressed(true)
                        .setMake("SAMSUNG")
                        .setMakeOffsetAndLength(160, 8)
                        .setModel("SM-N900S")
                        .setAperture(2.2f)
                        .setDateTimeOriginal("2016:01:29 18:32:27")
                        .setExposureTime(0.033f)
                        .setFocalLength("413/100")
                        .setImageSize(640, 480)
                        .setIso("50")
                        .setOrientation(ExifInterface.ORIENTATION_ROTATE_90)
                        .build();

        /**
         * Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_ii} when only the Exif
         * data is read using {@link ExifInterface#STREAM_TYPE_EXIF_DATA_ONLY}.
         */
        public static final ExpectedValue JPEG_WITH_EXIF_BYTE_ORDER_II_STANDALONE =
                JPEG_WITH_EXIF_BYTE_ORDER_II
                        .buildUpon()
                        .setThumbnailOffset(JPEG_WITH_EXIF_BYTE_ORDER_II.thumbnailOffset - 6)
                        .setMakeOffset(JPEG_WITH_EXIF_BYTE_ORDER_II.makeOffset - 6)
                        .build();

        /** Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_mm}. */
        public static final ExpectedValue JPEG_WITH_EXIF_BYTE_ORDER_MM =
                new Builder()
                        .setLatitudeOffsetAndLength(584, 24)
                        .setLatLong(0, 0)
                        .setAltitude(0)
                        .setMake("LGE")
                        .setMakeOffsetAndLength(414, 4)
                        .setModel("Nexus 5")
                        .setAperture(2.4f)
                        .setDateTimeOriginal("2016:01:29 15:44:58")
                        .setExposureTime(0.017f)
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
         * Expected attributes for {@link R.raw#jpeg_with_exif_byte_order_mm} when only the Exif
         * data is read using {@link ExifInterface#STREAM_TYPE_EXIF_DATA_ONLY}.
         */
        public static final ExpectedValue JPEG_WITH_EXIF_BYTE_ORDER_MM_STANDALONE =
                JPEG_WITH_EXIF_BYTE_ORDER_MM
                        .buildUpon()
                        .setLatitudeOffset(JPEG_WITH_EXIF_BYTE_ORDER_MM.latitudeOffset - 6)
                        .setMakeOffset(JPEG_WITH_EXIF_BYTE_ORDER_MM.makeOffset - 6)
                        .setImageSize(0, 0)
                        .build();

        /** Expected attributes for {@link R.raw#jpeg_with_exif_invalid_offset}. */
        public static final ExpectedValue JPEG_WITH_EXIF_INVALID_OFFSET =
                JPEG_WITH_EXIF_BYTE_ORDER_MM
                        .buildUpon()
                        .setAperture(0)
                        .setDateTimeOriginal(null)
                        .setExposureTime(0)
                        .setFocalLength(null)
                        .setIso(null)
                        .build();

        /** Expected attributes for {@link R.raw#dng_with_exif_with_xmp}. */
        public static final ExpectedValue DNG_WITH_EXIF_WITH_XMP =
                new Builder()
                        .setThumbnailOffsetAndLength(12570, 15179)
                        .setThumbnailSize(256, 144)
                        .setIsThumbnailCompressed(true)
                        .setLatitudeOffsetAndLength(12486, 24)
                        .setLatLong(53.834507f, 10.69585f)
                        .setAltitude(0)
                        .setMake("LGE")
                        .setMakeOffsetAndLength(102, 4)
                        .setModel("LG-H815")
                        .setAperture(1.8f)
                        .setDateTimeOriginal("2015:11:12 16:46:18")
                        .setExposureTime(0.0040f)
                        .setFocalLength("442/100")
                        .setGpsDatestamp("1970:01:17")
                        .setGpsLatitude("53/1,50/1,423/100")
                        .setGpsLatitudeRef("N")
                        .setGpsLongitude("10/1,41/1,4506/100")
                        .setGpsLongitudeRef("E")
                        .setGpsTimestamp("18:08:10")
                        .setImageSize(600, 337)
                        .setIso("800")
                        .setXmpOffsetAndLength(826, 10067)
                        .build();

        /** Expected attributes for {@link R.raw#jpeg_with_exif_with_xmp}. */
        public static final ExpectedValue JPEG_WITH_EXIF_WITH_XMP =
                DNG_WITH_EXIF_WITH_XMP
                        .buildUpon()
                        .clearThumbnail()
                        .setLatitudeOffset(1692)
                        .setMakeOffset(84)
                        .setOrientation(ExifInterface.ORIENTATION_NORMAL)
                        .setXmpOffsetAndLength(1809, 13197)
                        .build();

        /** Expected attributes for {@link R.raw#png_with_exif_byte_order_ii}. */
        public static final ExpectedValue PNG_WITH_EXIF_BYTE_ORDER_II =
                JPEG_WITH_EXIF_BYTE_ORDER_II
                        .buildUpon()
                        .setThumbnailOffset(212271)
                        .setMakeOffset(211525)
                        .setFocalLength("41/10")
                        .build();

        /** Expected attributes for {@link R.raw#webp_with_exif}. */
        public static final ExpectedValue WEBP_WITH_EXIF =
                JPEG_WITH_EXIF_BYTE_ORDER_II
                        .buildUpon()
                        .setThumbnailOffset(9646)
                        .setMakeOffset(6306)
                        .build();

        /** Expected attributes for {@link R.raw#invalid_webp_with_jpeg_app1_marker}. */
        public static final ExpectedValue INVALID_WEBP_WITH_JPEG_APP1_MARKER =
                new Builder().setOrientation(ExifInterface.ORIENTATION_ROTATE_270).build();

        /**
         * Expected attributes for {@link R.raw#heif_with_exif} when read on a device below API 31.
         */
        public static final ExpectedValue HEIF_WITH_EXIF_BELOW_API_31 =
                new Builder()
                        .setMake("LGE")
                        .setMakeOffsetAndLength(3519, 4)
                        .setModel("Nexus 5")
                        .setImageSize(1920, 1080)
                        .setOrientation(ExifInterface.ORIENTATION_NORMAL)
                        .build();

        /**
         * Expected attributes for {@link R.raw#heif_with_exif} when read on a device running API 31
         * or above.
         */
        public static final ExpectedValue HEIF_WITH_EXIF_API_31_AND_ABOVE =
                HEIF_WITH_EXIF_BELOW_API_31.buildUpon().setXmpOffsetAndLength(3721, 3020).build();

        public static class Builder {
            // Thumbnail information.
            private boolean mHasThumbnail;
            private int mThumbnailOffset;
            private int mThumbnailLength;
            private int mThumbnailWidth;
            private int mThumbnailHeight;
            private boolean mIsThumbnailCompressed;

            // GPS information.
            private boolean mHasLatLong;
            private int mLatitudeOffset;
            private int mLatitudeLength;
            private float mLatitude;
            private float mLongitude;
            private float mAltitude;

            // Make information
            private boolean mHasMake;
            private int mMakeOffset;
            private int mMakeLength;
            @Nullable private String mMake;

            // Values.
            @Nullable private String mModel;
            private float mAperture;
            @Nullable private String mDateTimeOriginal;
            private float mExposureTime;
            private float mFlash;
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
            private int mXmpOffset;
            private int mXmpLength;

            Builder() {}

            private Builder(ExpectedValue attributes) {
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

            public Builder setThumbnailOffsetAndLength(int offset, int length) {
                mHasThumbnail = true;
                mThumbnailOffset = offset;
                mThumbnailLength = length;
                return this;
            }

            public Builder setThumbnailOffset(int offset) {
                if (!mHasThumbnail) {
                    throw new IllegalStateException(
                            "Thumbnail position in the file must first be set with "
                                    + "setThumbnailOffsetAndLength(int, int)");
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

            public Builder setLatLong(float latitude, float longitude) {
                mHasLatLong = true;
                mLatitude = latitude;
                mLongitude = longitude;
                return this;
            }

            public Builder setLatitudeOffsetAndLength(int offset, int length) {
                mHasLatLong = true;
                mLatitudeOffset = offset;
                mLatitudeLength = length;
                return this;
            }

            public Builder setLatitudeOffset(int offset) {
                if (!mHasLatLong) {
                    throw new IllegalStateException(
                            "Latitude position in the file must first be "
                                    + "set with setLatitudeOffsetAndLength(int, int)");
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

            public Builder setAltitude(float altitude) {
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
            public Builder setMakeOffsetAndLength(int offset, int length) {
                mHasMake = true;
                mMakeOffset = offset;
                mMakeLength = length;
                return this;
            }

            public Builder setMakeOffset(int offset) {
                if (!mHasMake) {
                    throw new IllegalStateException(
                            "Make position in the file must first be set with"
                                    + " setMakeOffsetAndLength(int, int)");
                }
                mMakeOffset = offset;
                return this;
            }

            public Builder setModel(@Nullable String model) {
                mModel = model;
                return this;
            }

            public Builder setAperture(float aperture) {
                mAperture = aperture;
                return this;
            }

            public Builder setDateTimeOriginal(@Nullable String dateTimeOriginal) {
                mDateTimeOriginal = dateTimeOriginal;
                return this;
            }

            public Builder setExposureTime(float exposureTime) {
                mExposureTime = exposureTime;
                return this;
            }

            public Builder setFlash(float flash) {
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
                                    + " setXmpOffsetAndLength(int, int)");
                }
                mXmpOffset = offset;
                return this;
            }

            public Builder clearXmp() {
                mHasXmp = false;
                mXmpOffset = 0;
                mXmpLength = 0;
                return this;
            }

            public ExpectedValue build() {
                return new ExpectedValue(this);
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
        public final int thumbnailOffset;
        public final int thumbnailLength;

        // GPS information.
        public final boolean hasLatLong;
        // TODO: b/270554381 - Merge this and longitude into a double[]
        public final float latitude;
        public final int latitudeOffset;
        public final int latitudeLength;
        public final float longitude;
        public final float altitude;

        // Make information
        public final boolean hasMake;
        public final int makeOffset;
        public final int makeLength;
        public final String make;

        // Values.
        public final String model;
        public final float aperture;
        public final String dateTimeOriginal;
        public final float exposureTime;
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
        public final boolean hasXmp;
        public final int xmpOffset;
        public final int xmpLength;

        private ExpectedValue(Builder builder) {
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
            xmpOffset = builder.mXmpOffset;
            xmpLength = builder.mXmpLength;
        }

        public Builder buildUpon() {
            return new Builder(this);
        }
    }

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        if (ENABLE_STRICT_MODE_FOR_UNBUFFERED_IO && Build.VERSION.SDK_INT >= 26) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectUnbufferedIo()
                    .penaltyDeath()
                    .build());
        }
    }

    @Test
    @LargeTest
    public void testJpegWithExifIntelByteOrder() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_ii, "jpeg_with_exif_byte_order_ii.jpg");
        readFromFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_BYTE_ORDER_II);
        writeToFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_BYTE_ORDER_II);
    }

    @Test
    @LargeTest
    public void testJpegWithExifMotorolaByteOrder() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_mm, "jpeg_with_exif_byte_order_mm.jpg");
        readFromFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_BYTE_ORDER_MM);
        writeToFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_BYTE_ORDER_MM);
    }

    @Test
    @LargeTest
    public void testJpegWithExifAndXmp() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_with_xmp, "jpeg_with_exif_with_xmp.jpg");
        readFromFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_WITH_XMP);
        writeToFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_WITH_XMP);
    }

    // https://issuetracker.google.com/264729367
    @Test
    @LargeTest
    public void testJpegWithInvalidOffset() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_invalid_offset, "jpeg_with_exif_invalid_offset.jpg");
        readFromFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_INVALID_OFFSET);
        writeToFilesWithExif(imageFile, ExpectedValue.JPEG_WITH_EXIF_INVALID_OFFSET);
    }

    // https://issuetracker.google.com/263747161
    @Test
    @LargeTest
    public void testJpegWithFullApp1Segment() throws Throwable {
        File srcFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_full_app1_segment,
                        "jpeg_with_exif_full_app1_segment.jpg");
        File imageFile = clone(srcFile);
        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        // Add a really long string that makes the Exif data too large for the JPEG APP1 segment.
        char[] longStringChars = new char[500];
        Arrays.fill(longStringChars, 'a');
        String longString = new String(longStringChars);
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, longString);

        IOException expected = assertThrows(IOException.class,
                exifInterface::saveAttributes);
        assertThat(expected)
                .hasCauseThat()
                .hasMessageThat()
                .contains("exceeds the max size of a JPEG APP1 segment");
        assertBitmapsEquivalent(srcFile, imageFile);
    }

    @Test
    @LargeTest
    public void testDngWithExifAndXmp() throws Throwable {
        File imageFile =
                copyFromResourceToFile(R.raw.dng_with_exif_with_xmp, "dng_with_exif_with_xmp.dng");
        readFromFilesWithExif(imageFile, ExpectedValue.DNG_WITH_EXIF_WITH_XMP);
    }

    @Test
    @LargeTest
    public void testPngWithExif() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.png_with_exif_byte_order_ii, "png_with_exif_byte_order_ii.png");
        readFromFilesWithExif(imageFile, ExpectedValue.PNG_WITH_EXIF_BYTE_ORDER_II);
        writeToFilesWithExif(imageFile, ExpectedValue.PNG_WITH_EXIF_BYTE_ORDER_II);
    }

    @Test
    @LargeTest
    public void testPngWithoutExif() throws Throwable {
        File imageFile =
                copyFromResourceToFile(R.raw.png_with_exif_byte_order_ii, "png_without_exif.png");
        writeToFilesWithoutExif(imageFile);
    }

    @Test
    @LargeTest
    public void testStandaloneData_jpegIntelByteOrder() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_ii, "jpeg_with_exif_byte_order_ii.jpg");
        readFromStandaloneDataWithExif(
                imageFile, ExpectedValue.JPEG_WITH_EXIF_BYTE_ORDER_II_STANDALONE);
    }

    @Test
    @LargeTest
    public void testStandaloneData_jpegMotorolaByteOrder() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_mm, "jpeg_with_exif_byte_order_mm.jpg");
        readFromStandaloneDataWithExif(
                imageFile, ExpectedValue.JPEG_WITH_EXIF_BYTE_ORDER_MM_STANDALONE);
    }

    @Test
    @LargeTest
    public void testWebpWithExif() throws Throwable {
        File imageFile = copyFromResourceToFile(R.raw.webp_with_exif, "webp_with_exif.jpg");
        readFromFilesWithExif(imageFile, ExpectedValue.WEBP_WITH_EXIF);
        writeToFilesWithExif(imageFile, ExpectedValue.WEBP_WITH_EXIF);
    }

    // https://issuetracker.google.com/281638358
    @Test
    @LargeTest
    public void testWebpWithExifApp1() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.invalid_webp_with_jpeg_app1_marker,
                        "invalid_webp_with_jpeg_app1_marker.webp");
        readFromFilesWithExif(imageFile, ExpectedValue.INVALID_WEBP_WITH_JPEG_APP1_MARKER);
        writeToFilesWithExif(imageFile, ExpectedValue.INVALID_WEBP_WITH_JPEG_APP1_MARKER);
    }

    @Test
    @LargeTest
    public void testWebpWithoutExif() throws Throwable {
        File imageFile = copyFromResourceToFile(R.raw.webp_without_exif, "webp_without_exif.webp");
        writeToFilesWithoutExif(imageFile);
    }

    @Test
    @LargeTest
    public void testWebpWithoutExifWithAnimData() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.webp_with_anim_without_exif, WEBP_WITHOUT_EXIF_WITH_ANIM_DATA);
        writeToFilesWithoutExif(imageFile);
    }
    @Test
    @LargeTest
    public void testWebpWithoutExifWithLosslessEncoding() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.webp_lossless_without_exif, "webp_lossless_without_exif.webp");
        writeToFilesWithoutExif(imageFile);
    }

    @Test
    @LargeTest
    public void testWebpWithoutExifWithLosslessEncodingAndAlpha() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.webp_lossless_alpha_without_exif,
                        "webp_lossless_alpha_without_exif.webp");
        writeToFilesWithoutExif(imageFile);
    }

    /**
     * Support for retrieving EXIF from HEIF was added in SDK 28.
     */
    @Test
    @LargeTest
    public void testHeifFile() throws Throwable {
        File imageFile = copyFromResourceToFile(R.raw.heif_with_exif, "heif_with_exif.heic");
        if (Build.VERSION.SDK_INT >= 28) {
            // Reading XMP data from HEIF was added in SDK 31.
            readFromFilesWithExif(
                    imageFile,
                    Build.VERSION.SDK_INT >= 31
                            ? ExpectedValue.HEIF_WITH_EXIF_API_31_AND_ABOVE
                            : ExpectedValue.HEIF_WITH_EXIF_BELOW_API_31);
        } else {
            // Make sure that an exception is not thrown and that image length/width tag values
            // return default values, not the actual values.
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            String defaultTagValue = "0";
            assertEquals(defaultTagValue, exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            assertEquals(defaultTagValue, exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
        }
    }

    @Test
    @SmallTest
    public void testDoNotFailOnCorruptedImage() throws Throwable {
        Random random = new Random(/* seed= */ 0);
        byte[] bytes = new byte[8096];
        random.nextBytes(bytes);
        // Overwrite the start of the random bytes with some JPEG-like data, so it starts like a
        // plausible image with EXIF data.
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.put(ExifInterface.JPEG_SIGNATURE);
        buffer.put(ExifInterface.MARKER_APP1);
        buffer.putShort((short) 350);
        buffer.put(ExifInterface.IDENTIFIER_EXIF_APP1);
        buffer.putShort(ExifInterface.BYTE_ALIGN_MM);
        buffer.put((byte) 0);
        buffer.put(ExifInterface.START_CODE);
        buffer.putInt(8);
        // Number of primary tag directories
        buffer.putShort((short) 1);
        // Corruption starts here

        ExifInterface exifInterface = new ExifInterface(new ByteArrayInputStream(bytes));
        exifInterface.getAttribute(ExifInterface.TAG_ARTIST);
        // Test will fail if the ExifInterface constructor or getter throw an exception.
    }

    @Test
    @SmallTest
    public void testSetGpsInfo() throws IOException {
        final String provider = "ExifInterfaceTest";
        final long timestamp = 1689328448000L; // 2023-07-14T09:54:32.000Z
        final float speedInMeterPerSec = 36.627533f;
        Location location = new Location(provider);
        location.setLatitude(TEST_LATITUDE_VALID_VALUES[TEST_LATITUDE_VALID_VALUES.length - 1]);
        location.setLongitude(TEST_LONGITUDE_VALID_VALUES[TEST_LONGITUDE_VALID_VALUES.length - 1]);
        location.setAltitude(TEST_ALTITUDE_VALUES[TEST_ALTITUDE_VALUES.length - 1]);
        location.setSpeed(speedInMeterPerSec);
        location.setTime(timestamp);
        ExifInterface exif = createTestExifInterface();
        exif.setGpsInfo(location);

        double[] latLong = exif.getLatLong();
        assertNotNull(latLong);
        assertEquals(TEST_LATITUDE_VALID_VALUES[TEST_LATITUDE_VALID_VALUES.length - 1],
                latLong[0], DELTA);
        assertEquals(TEST_LONGITUDE_VALID_VALUES[TEST_LONGITUDE_VALID_VALUES.length - 1],
                latLong[1], DELTA);
        assertEquals(TEST_ALTITUDE_VALUES[TEST_ALTITUDE_VALUES.length - 1], exif.getAltitude(0),
                RATIONAL_DELTA);
        assertEquals("K", exif.getAttribute(ExifInterface.TAG_GPS_SPEED_REF));
        assertEquals(speedInMeterPerSec, exif.getAttributeDouble(ExifInterface.TAG_GPS_SPEED, 0.0)
                * 1000 / TimeUnit.HOURS.toSeconds(1), RATIONAL_DELTA);
        assertEquals(provider, exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD));
        // GPS time's precision is secs.
        assertEquals(TimeUnit.MILLISECONDS.toSeconds(timestamp),
                TimeUnit.MILLISECONDS.toSeconds(exif.getGpsDateTime()));
    }

    @Test
    @SmallTest
    public void testSetLatLong_withValidValues() throws IOException {
        for (int i = 0; i < TEST_LAT_LONG_VALUES_ARRAY_LENGTH; i++) {
            ExifInterface exif = createTestExifInterface();
            exif.setLatLong(TEST_LATITUDE_VALID_VALUES[i], TEST_LONGITUDE_VALID_VALUES[i]);

            double[] latLong = exif.getLatLong();
            assertNotNull(latLong);
            assertEquals(TEST_LATITUDE_VALID_VALUES[i], latLong[0], DELTA);
            assertEquals(TEST_LONGITUDE_VALID_VALUES[i], latLong[1], DELTA);
        }
    }

    @Test
    @SmallTest
    public void testSetLatLong_withInvalidLatitude() throws IOException {
        for (int i = 0; i < TEST_LAT_LONG_VALUES_ARRAY_LENGTH; i++) {
            ExifInterface exif = createTestExifInterface();
            try {
                exif.setLatLong(TEST_LATITUDE_INVALID_VALUES[i], TEST_LONGITUDE_VALID_VALUES[i]);
                fail();
            } catch (IllegalArgumentException e) {
                // expected
            }
            assertNull(exif.getLatLong());
            assertLatLongValuesAreNotSet(exif);
        }
    }

    @Test
    @SmallTest
    public void testSetLatLong_withInvalidLongitude() throws IOException {
        for (int i = 0; i < TEST_LAT_LONG_VALUES_ARRAY_LENGTH; i++) {
            ExifInterface exif = createTestExifInterface();
            try {
                exif.setLatLong(TEST_LATITUDE_VALID_VALUES[i], TEST_LONGITUDE_INVALID_VALUES[i]);
                fail();
            } catch (IllegalArgumentException e) {
                // expected
            }
            assertNull(exif.getLatLong());
            assertLatLongValuesAreNotSet(exif);
        }
    }

    @Test
    @SmallTest
    public void testSetAltitude() throws IOException {
        for (int i = 0; i < TEST_ALTITUDE_VALUES.length; i++) {
            ExifInterface exif = createTestExifInterface();
            exif.setAltitude(TEST_ALTITUDE_VALUES[i]);
            assertEquals(TEST_ALTITUDE_VALUES[i], exif.getAltitude(Double.NaN), RATIONAL_DELTA);
        }
    }

    /**
     * JPEG_WITH_DATETIME_TAG_PRIMARY_FORMAT contains the following tags:
     *   TAG_DATETIME, TAG_DATETIME_ORIGINAL, TAG_DATETIME_DIGITIZED = "2016:01:29 18:32:27"
     *   TAG_OFFSET_TIME, TAG_OFFSET_TIME_ORIGINAL, TAG_OFFSET_TIME_DIGITIZED = "100000"
     *   TAG_DATETIME, TAG_DATETIME_ORIGINAL, TAG_DATETIME_DIGITIZED = "+09:00"
     */
    @Test
    @SmallTest
    public void testGetSetDateTime() throws IOException {
        final long expectedGetDatetimeValue =
                1454027547000L /* TAG_DATETIME value ("2016:01:29 18:32:27") converted to msec */
                + 100L /* TAG_SUBSEC_TIME value ("100000") converted to msec */
                + 32400000L /* TAG_OFFSET_TIME value ("+09:00") converted to msec */;
        // GPS datetime does not support subsec precision
        final long expectedGetGpsDatetimeValue =
                1454027547000L /* TAG_DATETIME value ("2016:01:29 18:32:27") converted to msec */
                + 32400000L /* TAG_OFFSET_TIME value ("+09:00") converted to msec */;
        final String expectedDatetimeOffsetStringValue = "+09:00";

        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_datetime_tag_primary_format,
                        "jpeg_with_datetime_tag_primary_format.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        // Test getting datetime values
        assertEquals(expectedGetDatetimeValue, (long) exif.getDateTime());
        assertEquals(expectedGetDatetimeValue, (long) exif.getDateTimeOriginal());
        assertEquals(expectedGetDatetimeValue, (long) exif.getDateTimeDigitized());
        assertEquals(expectedGetGpsDatetimeValue, (long) exif.getGpsDateTime());
        assertEquals(expectedDatetimeOffsetStringValue,
                exif.getAttribute(ExifInterface.TAG_OFFSET_TIME));
        assertEquals(expectedDatetimeOffsetStringValue,
                exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL));
        assertEquals(expectedDatetimeOffsetStringValue,
                exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED));

        // Test setting datetime values
        final long newTimestamp = 1689328448000L; // 2023-07-14T09:54:32.000Z
        final long expectedDatetimeOffsetLongValue = 32400000L;
        exif.setDateTime(newTimestamp);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(newTimestamp - expectedDatetimeOffsetLongValue, (long) exif.getDateTime());

        // Test that setting null throws NPE
        try {
            exif.setDateTime(null);
            fail();
        } catch (NullPointerException e) {
            // Expected
        }

        // Test that setting negative value throws IAE
        try {
            exif.setDateTime(-1L);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /**
     * Test whether ExifInterface can correctly get and set datetime value for a secondary format:
     * Primary format example: 2020:01:01 00:00:00
     * Secondary format example: 2020-01-01 00:00:00
     *
     * Getting a datetime tag value with the secondary format should work for both
     * {@link ExifInterface#getAttribute(String)} and {@link ExifInterface#getDateTime()}.
     * Setting a datetime tag value with the secondary format with
     * {@link ExifInterface#setAttribute(String, String)} should automatically convert it to the
     * primary format.
     *
     * JPEG_WITH_DATETIME_TAG_SECONDARY_FORMAT contains the following tags:
     *   TAG_DATETIME, TAG_DATETIME_ORIGINAL, TAG_DATETIME_DIGITIZED = "2016:01:29 18:32:27"
     *   TAG_OFFSET_TIME, TAG_OFFSET_TIME_ORIGINAL, TAG_OFFSET_TIME_DIGITIZED = "100000"
     *   TAG_DATETIME, TAG_DATETIME_ORIGINAL, TAG_DATETIME_DIGITIZED = "+09:00"
     */
    @Test
    @SmallTest
    public void testGetSetDateTimeForSecondaryFormat() throws Exception {
        // Test getting datetime values
        final long expectedGetDatetimeValue =
                1454027547000L /* TAG_DATETIME value ("2016:01:29 18:32:27") converted to msec */
                + 100L /* TAG_SUBSEC_TIME value ("100000") converted to msec */
                + 32400000L /* TAG_OFFSET_TIME value ("+09:00") converted to msec */;
        final String expectedDateTimeStringValue = "2016-01-29 18:32:27";

        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_datetime_tag_secondary_format,
                        "jpeg_with_datetime_tag_secondary_format.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(expectedDateTimeStringValue,
                exif.getAttribute(ExifInterface.TAG_DATETIME));
        assertEquals(expectedGetDatetimeValue, (long) exif.getDateTime());

        // Test setting datetime value: check that secondary format value is modified correctly
        // when it is saved.
        final long newDateTimeLongValue =
                1577772000000L /* TAG_DATETIME value ("2020-01-01 00:00:00") converted to msec */
                + 100L /* TAG_SUBSEC_TIME value ("100000") converted to msec */
                + 32400000L /* TAG_OFFSET_TIME value ("+09:00") converted to msec */;
        final String newDateTimeStringValue = "2020-01-01 00:00:00";
        final String modifiedNewDateTimeStringValue = "2020:01:01 00:00:00";

        exif.setAttribute(ExifInterface.TAG_DATETIME, newDateTimeStringValue);
        exif.saveAttributes();
        assertEquals(modifiedNewDateTimeStringValue, exif.getAttribute(ExifInterface.TAG_DATETIME));
        assertEquals(newDateTimeLongValue, (long) exif.getDateTime());
    }

    @Test
    @LargeTest
    public void testAddDefaultValuesForCompatibility() throws Exception {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_datetime_tag_primary_format,
                        "jpeg_with_datetime_tag_primary_format.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());

        // 1. Check that the TAG_DATETIME value is not overwritten by TAG_DATETIME_ORIGINAL's value
        // when TAG_DATETIME value exists.
        final String dateTimeValue = "2017:02:02 22:22:22";
        final String dateTimeOriginalValue = "2017:01:01 11:11:11";
        exif.setAttribute(ExifInterface.TAG_DATETIME, dateTimeValue);
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTimeOriginalValue);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(dateTimeValue, exif.getAttribute(ExifInterface.TAG_DATETIME));
        assertEquals(dateTimeOriginalValue, exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));

        // 2. Check that when TAG_DATETIME has no value, it is set to TAG_DATETIME_ORIGINAL's value.
        exif.setAttribute(ExifInterface.TAG_DATETIME, null);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(dateTimeOriginalValue, exif.getAttribute(ExifInterface.TAG_DATETIME));
    }

    @Test
    @LargeTest
    public void testSubsec() throws IOException {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_datetime_tag_primary_format,
                        "jpeg_with_datetime_tag_primary_format.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());

        // Set initial value to 0
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, /* 0ms */ "000");
        exif.saveAttributes();
        assertEquals("000", exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));
        long currentDateTimeValue = exif.getDateTime();

        // Test that single and double-digit values are set properly.
        // Note that since SubSecTime tag records fractions of a second, a single-digit value
        // should be counted as the first decimal value, which is why "1" becomes 100ms and "11"
        // becomes 110ms.
        String oneDigitSubSec = "1";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, oneDigitSubSec);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 100, (long) exif.getDateTime());
        assertEquals(oneDigitSubSec, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        String twoDigitSubSec1 = "01";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, twoDigitSubSec1);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 10, (long) exif.getDateTime());
        assertEquals(twoDigitSubSec1, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        String twoDigitSubSec2 = "11";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, twoDigitSubSec2);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 110, (long) exif.getDateTime());
        assertEquals(twoDigitSubSec2, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        // Test that 3-digit values are set properly.
        String hundredMs = "100";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, hundredMs);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 100, (long) exif.getDateTime());
        assertEquals(hundredMs, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        // Test that values starting with zero are also supported.
        String oneMsStartingWithZeroes = "001";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, oneMsStartingWithZeroes);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 1, (long) exif.getDateTime());
        assertEquals(oneMsStartingWithZeroes, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        String tenMsStartingWithZero = "010";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, tenMsStartingWithZero);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 10, (long) exif.getDateTime());
        assertEquals(tenMsStartingWithZero, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        // Test that values with more than three digits are set properly. getAttribute() should
        // return the whole string, but getDateTime() should only add the first three digits
        // because it supports only up to 1/1000th of a second.
        String fourDigitSubSec = "1234";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, fourDigitSubSec);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 123, (long) exif.getDateTime());
        assertEquals(fourDigitSubSec, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        String fiveDigitSubSec = "23456";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, fiveDigitSubSec);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 234, (long) exif.getDateTime());
        assertEquals(fiveDigitSubSec, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));

        String sixDigitSubSec = "345678";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, sixDigitSubSec);
        exif.saveAttributes();
        assertEquals(currentDateTimeValue + 345, (long) exif.getDateTime());
        assertEquals(sixDigitSubSec, exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME));
    }

    @Test
    @LargeTest
    public void testRotation() throws IOException {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_ii, "jpeg_with_exif_byte_order_ii.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());

        int num;
        // Test flip vertically.
        for (num = 0; num < TEST_FLIP_VERTICALLY_STATE_MACHINE.length; num++) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                    Integer.toString(TEST_FLIP_VERTICALLY_STATE_MACHINE[num][0]));
            exif.flipVertically();
            exif.saveAttributes();
            exif = new ExifInterface(imageFile.getAbsolutePath());
            assertIntTag(exif, ExifInterface.TAG_ORIENTATION,
                    TEST_FLIP_VERTICALLY_STATE_MACHINE[num][1]);

        }

        // Test flip horizontally.
        for (num = 0; num < TEST_FLIP_VERTICALLY_STATE_MACHINE.length; num++) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                    Integer.toString(TEST_FLIP_HORIZONTALLY_STATE_MACHINE[num][0]));
            exif.flipHorizontally();
            exif.saveAttributes();
            exif = new ExifInterface(imageFile.getAbsolutePath());
            assertIntTag(exif, ExifInterface.TAG_ORIENTATION,
                    TEST_FLIP_HORIZONTALLY_STATE_MACHINE[num][1]);

        }

        // Test rotate by degrees
        exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                Integer.toString(ExifInterface.ORIENTATION_NORMAL));
        try {
            exif.rotate(108);
            fail("Rotate with 108 degree should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Success
        }

        for (num = 0; num < TEST_ROTATION_STATE_MACHINE.length; num++) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                    Integer.toString(TEST_ROTATION_STATE_MACHINE[num][0]));
            exif.rotate(TEST_ROTATION_STATE_MACHINE[num][1]);
            exif.saveAttributes();
            exif = new ExifInterface(imageFile.getAbsolutePath());
            assertIntTag(exif, ExifInterface.TAG_ORIENTATION, TEST_ROTATION_STATE_MACHINE[num][2]);
        }

        // Test get flip state and rotation degrees.
        for (Integer key : FLIP_STATE_AND_ROTATION_DEGREES.keySet()) {
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, key.toString());
            exif.saveAttributes();
            exif = new ExifInterface(imageFile.getAbsolutePath());
            assertEquals(FLIP_STATE_AND_ROTATION_DEGREES.get(key).first, exif.isFlipped());
            assertEquals((int) FLIP_STATE_AND_ROTATION_DEGREES.get(key).second,
                    exif.getRotationDegrees());
        }

        // Test reset the rotation.
        exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                Integer.toString(ExifInterface.ORIENTATION_FLIP_HORIZONTAL));
        exif.resetOrientation();
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertIntTag(exif, ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

    }

    @SuppressWarnings("deprecation")
    @Test
    @SmallTest
    public void testInterchangeabilityBetweenTwoIsoSpeedTags() throws IOException {
        // Tests that two tags TAG_ISO_SPEED_RATINGS and TAG_PHOTOGRAPHIC_SENSITIVITY can be used
        // interchangeably.
        final String oldTag = ExifInterface.TAG_ISO_SPEED_RATINGS;
        final String newTag = ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY;
        final String isoValue = "50";

        ExifInterface exif = createTestExifInterface();
        exif.setAttribute(oldTag, isoValue);
        assertEquals(isoValue, exif.getAttribute(oldTag));
        assertEquals(isoValue, exif.getAttribute(newTag));

        exif = createTestExifInterface();
        exif.setAttribute(newTag, isoValue);
        assertEquals(isoValue, exif.getAttribute(oldTag));
        assertEquals(isoValue, exif.getAttribute(newTag));
    }

    private void printExifTagsAndValues(String fileName, ExifInterface exifInterface) {
        // Prints thumbnail information.
        if (exifInterface.hasThumbnail()) {
            byte[] thumbnailBytes = exifInterface.getThumbnailBytes();
            if (thumbnailBytes != null) {
                Log.v(TAG, fileName + " Thumbnail size = " + thumbnailBytes.length);
                Bitmap bitmap = exifInterface.getThumbnailBitmap();
                if (bitmap == null) {
                    Log.e(TAG, fileName + " Corrupted thumbnail!");
                } else {
                    Log.v(TAG, fileName + " Thumbnail size: " + bitmap.getWidth() + ", "
                            + bitmap.getHeight());
                }
            } else {
                Log.e(TAG, fileName + " Unexpected result: No thumbnails were found. "
                        + "A thumbnail is expected.");
            }
        } else {
            if (exifInterface.getThumbnailBytes() != null) {
                Log.e(TAG, fileName + " Unexpected result: A thumbnail was found. "
                        + "No thumbnail is expected.");
            } else {
                Log.v(TAG, fileName + " No thumbnail");
            }
        }

        // Prints GPS information.
        Log.v(TAG, fileName + " Altitude = " + exifInterface.getAltitude(.0));

        double[] latLong = exifInterface.getLatLong();
        if (latLong != null) {
            Log.v(TAG, fileName + " Latitude = " + latLong[0]);
            Log.v(TAG, fileName + " Longitude = " + latLong[1]);
        } else {
            Log.v(TAG, fileName + " No latlong data");
        }

        // Prints values.
        for (String tagKey : EXIF_TAGS) {
            String tagValue = exifInterface.getAttribute(tagKey);
            Log.v(TAG, fileName + " Key{" + tagKey + "} = '" + tagValue + "'");
        }
    }

    private void assertIntTag(ExifInterface exifInterface, String tag, int expectedValue) {
        int intValue = exifInterface.getAttributeInt(tag, 0);
        assertEquals(expectedValue, intValue);
    }

    private void assertFloatTag(ExifInterface exifInterface, String tag, float expectedValue) {
        double doubleValue = exifInterface.getAttributeDouble(tag, 0.0);
        assertEquals(expectedValue, doubleValue, DIFFERENCE_TOLERANCE);
    }

    private void assertStringTag(ExifInterface exifInterface, String tag, String expectedValue) {
        String stringValue = exifInterface.getAttribute(tag);
        if (stringValue != null) {
            stringValue = stringValue.trim();
        }
        stringValue = ("".equals(stringValue)) ? null : stringValue;

        assertEquals(expectedValue, stringValue);
    }

    private void compareWithExpectedValue(ExifInterface exifInterface,
            ExpectedValue expectedValue, String verboseTag, boolean assertRanges) {
        if (VERBOSE) {
            printExifTagsAndValues(verboseTag, exifInterface);
        }
        // Checks a thumbnail image.
        assertEquals(expectedValue.hasThumbnail, exifInterface.hasThumbnail());
        if (expectedValue.hasThumbnail) {
            assertNotNull(exifInterface.getThumbnailRange());
            if (assertRanges) {
                final long[] thumbnailRange = exifInterface.getThumbnailRange();
                assertEquals(expectedValue.thumbnailOffset, thumbnailRange[0]);
                assertEquals(expectedValue.thumbnailLength, thumbnailRange[1]);
            }
            testThumbnail(expectedValue, exifInterface);
        } else {
            assertNull(exifInterface.getThumbnailRange());
            assertNull(exifInterface.getThumbnail());
        }

        // Checks GPS information.
        double[] latLong = exifInterface.getLatLong();
        assertEquals(expectedValue.hasLatLong, latLong != null);
        if (expectedValue.hasLatLong) {
            assertNotNull(exifInterface.getAttributeRange(ExifInterface.TAG_GPS_LATITUDE));
            if (assertRanges) {
                final long[] latitudeRange = exifInterface
                        .getAttributeRange(ExifInterface.TAG_GPS_LATITUDE);
                assertEquals(expectedValue.latitudeOffset, latitudeRange[0]);
                assertEquals(expectedValue.latitudeLength, latitudeRange[1]);
            }
            assertEquals(expectedValue.latitude, latLong[0], DIFFERENCE_TOLERANCE);
            assertEquals(expectedValue.longitude, latLong[1], DIFFERENCE_TOLERANCE);
            assertTrue(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LATITUDE));
            assertTrue(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LONGITUDE));
        } else {
            assertNull(exifInterface.getAttributeRange(ExifInterface.TAG_GPS_LATITUDE));
            assertFalse(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LATITUDE));
            assertFalse(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LONGITUDE));
        }
        assertEquals(expectedValue.altitude, exifInterface.getAltitude(.0), DIFFERENCE_TOLERANCE);

        // Checks Make information.
        String make = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        assertEquals(expectedValue.hasMake, make != null);
        if (expectedValue.hasMake) {
            assertNotNull(exifInterface.getAttributeRange(ExifInterface.TAG_MAKE));
            if (assertRanges) {
                final long[] makeRange = exifInterface
                        .getAttributeRange(ExifInterface.TAG_MAKE);
                assertEquals(expectedValue.makeOffset, makeRange[0]);
                assertEquals(expectedValue.makeLength, makeRange[1]);
            }
            assertEquals(expectedValue.make, make);
        } else {
            assertNull(exifInterface.getAttributeRange(ExifInterface.TAG_MAKE));
            assertFalse(exifInterface.hasAttribute(ExifInterface.TAG_MAKE));
        }

        // Checks values.
        assertStringTag(exifInterface, ExifInterface.TAG_MAKE, expectedValue.make);
        assertStringTag(exifInterface, ExifInterface.TAG_MODEL, expectedValue.model);
        assertFloatTag(exifInterface, ExifInterface.TAG_F_NUMBER, expectedValue.aperture);
        assertStringTag(exifInterface, ExifInterface.TAG_DATETIME_ORIGINAL,
                expectedValue.dateTimeOriginal);
        assertFloatTag(exifInterface, ExifInterface.TAG_EXPOSURE_TIME, expectedValue.exposureTime);
        assertStringTag(exifInterface, ExifInterface.TAG_FOCAL_LENGTH, expectedValue.focalLength);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_ALTITUDE, expectedValue.gpsAltitude);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_ALTITUDE_REF,
                expectedValue.gpsAltitudeRef);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_DATESTAMP, expectedValue.gpsDatestamp);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_LATITUDE, expectedValue.gpsLatitude);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_LATITUDE_REF,
                expectedValue.gpsLatitudeRef);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_LONGITUDE, expectedValue.gpsLongitude);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_LONGITUDE_REF,
                expectedValue.gpsLongitudeRef);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_PROCESSING_METHOD,
                expectedValue.gpsProcessingMethod);
        assertStringTag(exifInterface, ExifInterface.TAG_GPS_TIMESTAMP, expectedValue.gpsTimestamp);
        assertIntTag(exifInterface, ExifInterface.TAG_IMAGE_LENGTH, expectedValue.imageLength);
        assertIntTag(exifInterface, ExifInterface.TAG_IMAGE_WIDTH, expectedValue.imageWidth);
        assertStringTag(exifInterface, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                expectedValue.iso);
        assertIntTag(exifInterface, ExifInterface.TAG_ORIENTATION, expectedValue.orientation);

        if (expectedValue.hasXmp) {
            assertNotNull(exifInterface.getAttributeRange(ExifInterface.TAG_XMP));
            if (assertRanges) {
                final long[] xmpRange = exifInterface.getAttributeRange(ExifInterface.TAG_XMP);
                assertEquals(expectedValue.xmpOffset, xmpRange[0]);
                assertEquals(expectedValue.xmpLength, xmpRange[1]);
            }
            final String xmp = new String(exifInterface.getAttributeBytes(ExifInterface.TAG_XMP),
                    Charset.forName("UTF-8"));
            // We're only interested in confirming that we were able to extract
            // valid XMP data, which must always include this XML tag; a full
            // XMP parser is beyond the scope of ExifInterface. See XMP
            // Specification Part 1, Section C.2.2 for additional details.
            if (!xmp.contains("<rdf:RDF")) {
                fail("Invalid XMP: " + xmp);
            }
        } else {
            assertNull(exifInterface.getAttributeRange(ExifInterface.TAG_XMP));
        }
    }

    private void readFromStandaloneDataWithExif(File imageFile, ExpectedValue expectedValue)
            throws IOException {
        String verboseTag = imageFile.getName();

        byte[] exifBytes;
        try (FileInputStream fis = new FileInputStream(imageFile)) {
            // Skip the following marker bytes (0xff, 0xd8, 0xff, 0xe1)
            ByteStreams.skipFully(fis, 4);
            // Read the value of the length of the exif data
            short length = readShort(fis);
            exifBytes = new byte[length];
            ByteStreams.readFully(fis, exifBytes);
        }

        ByteArrayInputStream bin = new ByteArrayInputStream(exifBytes);
        ExifInterface exifInterface =
                new ExifInterface(bin, ExifInterface.STREAM_TYPE_EXIF_DATA_ONLY);
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);
    }

    private void testExifInterfaceCommon(File imageFile, ExpectedValue expectedValue)
            throws IOException {
        String verboseTag = imageFile.getName();

        // Creates via file.
        ExifInterface exifInterface = new ExifInterface(imageFile);
        assertNotNull(exifInterface);
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);

        // Creates via path.
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        assertNotNull(exifInterface);
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);

        // Creates via InputStream.
        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            exifInterface = new ExifInterface(in);
            compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);
        }

        // Creates via FileDescriptor.
        if (Build.VERSION.SDK_INT >= 21) {
            FileDescriptor fd = null;
            try {
                fd = Os.open(imageFile.getAbsolutePath(), OsConstants.O_RDONLY,
                        OsConstants.S_IRWXU);
                exifInterface = new ExifInterface(fd);
                compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);
            } catch (Exception e) {
                throw new IOException("Failed to open file descriptor", e);
            } finally {
                closeQuietly(fd);
            }
        }
    }

    private void testExifInterfaceRange(File imageFile, ExpectedValue expectedValue)
            throws IOException {
        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            if (expectedValue.hasThumbnail) {
                ByteStreams.skipFully(in, expectedValue.thumbnailOffset);
                byte[] thumbnailBytes = new byte[expectedValue.thumbnailLength];
                ByteStreams.readFully(in, thumbnailBytes);
                // TODO: Need a way to check uncompressed thumbnail file
                Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnailBytes, 0,
                        thumbnailBytes.length);
                assertNotNull(thumbnailBitmap);
                assertEquals(expectedValue.thumbnailWidth, thumbnailBitmap.getWidth());
                assertEquals(expectedValue.thumbnailHeight, thumbnailBitmap.getHeight());
            }
        }

        // TODO: Creating a new input stream is a temporary
        //  workaround for BufferedInputStream#mark/reset not working properly for
        //  LG_G4_ISO_800_DNG. Need to investigate cause.
        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            if (expectedValue.hasMake) {
                ByteStreams.skipFully(in, expectedValue.makeOffset);
                byte[] makeBytes = new byte[expectedValue.makeLength];
                ByteStreams.readFully(in, makeBytes);
                String makeString = new String(makeBytes);
                // Remove null bytes
                makeString = makeString.replaceAll("\u0000.*", "");
                assertEquals(expectedValue.make, makeString);
            }
        }

        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            if (expectedValue.hasXmp) {
                ByteStreams.skipFully(in, expectedValue.xmpOffset);
                byte[] identifierBytes = new byte[expectedValue.xmpLength];
                ByteStreams.readFully(in, identifierBytes);
                final String xmpIdentifier = "<?xpacket begin=";
                assertTrue(new String(identifierBytes, Charset.forName("UTF-8"))
                        .startsWith(xmpIdentifier));
            }
            // TODO: Add code for retrieving raw latitude data using offset and length
        }
    }

    private void writeToFilesWithExif(File srcFile, ExpectedValue expectedValue)
            throws IOException {
        File imageFile = clone(srcFile);
        String verboseTag = imageFile.getName();

        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, false);
        assertBitmapsEquivalent(srcFile, imageFile);
        assertSecondSaveProducesSameSizeFile(imageFile);

        // Test for modifying one attribute.
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        String backupValue = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
        exifInterface.saveAttributes();
        // Check if thumbnail offset and length are properly updated without parsing the data again.
        if (expectedValue.hasThumbnail) {
            testThumbnail(expectedValue, exifInterface);
        }
        assertEquals("abc", exifInterface.getAttribute(ExifInterface.TAG_MAKE));
        // Check if thumbnail bytes can be retrieved from the new thumbnail range.
        if (expectedValue.hasThumbnail) {
            testThumbnail(expectedValue, exifInterface);
        }

        // Restore the backup value.
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, backupValue);
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, false);

        // Creates via FileDescriptor.
        if (Build.VERSION.SDK_INT >= 21) {
            FileDescriptor fd = null;
            try {
                fd = Os.open(imageFile.getAbsolutePath(), OsConstants.O_RDWR,
                        OsConstants.S_IRWXU);
                exifInterface = new ExifInterface(fd);
                exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
                exifInterface.saveAttributes();
                assertEquals("abc", exifInterface.getAttribute(ExifInterface.TAG_MAKE));
            } catch (Exception e) {
                throw new IOException("Failed to open file descriptor", e);
            } finally {
                closeQuietly(fd);
            }
        }
    }

    private void readFromFilesWithExif(File imageFile, ExpectedValue expectedValue)
            throws IOException {

        // Test for reading from external data storage.
        testExifInterfaceCommon(imageFile, expectedValue);

        // Test for checking expected range by retrieving raw data with given offset and length.
        testExifInterfaceRange(imageFile, expectedValue);
    }

    private void writeToFilesWithoutExif(File srcFile) throws IOException {
        File imageFile = clone(srcFile);

        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
        exifInterface.saveAttributes();

        assertBitmapsEquivalent(srcFile, imageFile);
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        String make = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        assertEquals("abc", make);

        assertSecondSaveProducesSameSizeFile(imageFile);
    }

    private void testThumbnail(ExpectedValue expectedValue, ExifInterface exifInterface) {
        byte[] thumbnail = exifInterface.getThumbnail();
        assertNotNull(thumbnail);
        Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnail, 0,
                thumbnail.length);
        assertNotNull(thumbnailBitmap);
        assertEquals(expectedValue.thumbnailWidth, thumbnailBitmap.getWidth());
        assertEquals(expectedValue.thumbnailHeight, thumbnailBitmap.getHeight());
    }

    @RequiresApi(21)
    private void closeQuietly(FileDescriptor fd) {
        if (fd != null) {
            try {
                Os.close(fd);
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    private void assertLatLongValuesAreNotSet(ExifInterface exif) {
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
    }

    private ExifInterface createTestExifInterface() throws IOException {
        File originalFile = tempFolder.newFile();
        File jpgFile = new File(originalFile.getAbsolutePath() + ".jpg");
        if (!originalFile.renameTo(jpgFile)) {
            throw new IOException("Rename from " + originalFile + " to " + jpgFile + " failed.");
        }
        return new ExifInterface(jpgFile.getAbsolutePath());
    }

    private short readShort(InputStream is) throws IOException {
        int ch1 = is.read();
        int ch2 = is.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << 8) + (ch2));
    }

    /**
     * Asserts that {@code expectedImageFile} and {@code actualImageFile} can be decoded by
     * {@link BitmapFactory} and the results have the same width, height and MIME type.
     *
     * <p>The assertion is skipped if the test is running on an API level where
     * {@link BitmapFactory} is known not to support the image format of {@code expectedImageFile}
     * (as determined by file extension).
     *
     * <p>This does not check the image itself for similarity/equality.
     */
    private void assertBitmapsEquivalent(File expectedImageFile, File actualImageFile) {
        if (Build.VERSION.SDK_INT < 26
                && expectedImageFile.getName().equals(WEBP_WITHOUT_EXIF_WITH_ANIM_DATA)) {
            // BitmapFactory can't parse animated WebP files on API levels before 26: b/259964971
            return;
        }
        BitmapFactory.Options expectedOptions = new BitmapFactory.Options();
        Bitmap expectedBitmap = Objects.requireNonNull(
                decodeBitmap(expectedImageFile, expectedOptions));
        BitmapFactory.Options actualOptions = new BitmapFactory.Options();
        Bitmap actualBitmap = Objects.requireNonNull(decodeBitmap(actualImageFile, actualOptions));

        assertEquals(expectedOptions.outWidth, actualOptions.outWidth);
        assertEquals(expectedOptions.outHeight, actualOptions.outHeight);
        assertEquals(expectedOptions.outMimeType, actualOptions.outMimeType);
        assertEquals(expectedBitmap.getWidth(), actualBitmap.getWidth());
        assertEquals(expectedBitmap.getHeight(), actualBitmap.getHeight());
        assertEquals(expectedBitmap.hasAlpha(), actualBitmap.hasAlpha());
    }

    /**
     * Equivalent to {@link BitmapFactory#decodeFile(String, BitmapFactory.Options)} but uses a
     * {@link BufferedInputStream} to avoid violating
     * {@link StrictMode.ThreadPolicy.Builder#detectUnbufferedIo()}.
     */
    private static Bitmap decodeBitmap(File file, BitmapFactory.Options options) {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            return BitmapFactory.decodeStream(inputStream, /* outPadding= */ null, options);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Asserts that saving the file the second time (without modifying any attributes) produces
     * exactly the same length file as the first save. The first save (with no modifications) is
     * expected to (possibly) change the file length because {@link ExifInterface} may move/reformat
     * the Exif block within the file, but the second save should not make further modifications.
     */
    private void assertSecondSaveProducesSameSizeFile(File imageFileAfterOneSave)
            throws IOException {
        File imageFileAfterTwoSaves = clone(imageFileAfterOneSave);
        ExifInterface exifInterface = new ExifInterface(imageFileAfterTwoSaves.getAbsolutePath());
        exifInterface.saveAttributes();
        if (imageFileAfterOneSave.getAbsolutePath().endsWith(".png")
                || imageFileAfterOneSave.getAbsolutePath().endsWith(".webp")) {
            // PNG and (some) WebP files are (surprisingly) modified between the first and second
            // save (b/249097443), so we check the difference between second and third save instead.
            File imageFileAfterThreeSaves = clone(imageFileAfterTwoSaves);
            exifInterface = new ExifInterface(imageFileAfterThreeSaves.getAbsolutePath());
            exifInterface.saveAttributes();
            assertEquals(imageFileAfterTwoSaves.length(), imageFileAfterThreeSaves.length());
        } else {
            assertEquals(imageFileAfterOneSave.length(), imageFileAfterTwoSaves.length());
        }
    }

    private File clone(File original) throws IOException {
        File cloned =
                File.createTempFile("tmp_", System.nanoTime() + "_" + original.getName());
        Files.copy(original, cloned);
        return cloned;
    }

    private File copyFromResourceToFile(int resourceId, String filename) throws IOException {
        File file = tempFolder.newFile(filename);
        try (InputStream inputStream =
                        getApplicationContext().getResources().openRawResource(resourceId);
                FileOutputStream outputStream = new FileOutputStream(file)) {
            ByteStreams.copy(inputStream, outputStream);
        }
        return file;
    }
}
