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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.Manifest;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;

import androidx.exifinterface.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
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

    private static final String JPEG_WITH_EXIF_BYTE_ORDER_II = "jpeg_with_exif_byte_order_ii.jpg";
    private static final String JPEG_WITH_EXIF_BYTE_ORDER_MM = "jpeg_with_exif_byte_order_mm.jpg";
    private static final String DNG_WITH_EXIF_WITH_XMP = "dng_with_exif_with_xmp.dng";
    private static final String JPEG_WITH_EXIF_WITH_XMP = "jpeg_with_exif_with_xmp.jpg";
    private static final String PNG_WITH_EXIF_BYTE_ORDER_II = "png_with_exif_byte_order_ii.png";
    private static final String PNG_WITHOUT_EXIF = "png_without_exif.png";
    private static final String WEBP_WITH_EXIF = "webp_with_exif.webp";
    private static final String WEBP_WITHOUT_EXIF_WITH_ANIM_DATA =
            "webp_with_anim_without_exif.webp";
    private static final String WEBP_WITHOUT_EXIF = "webp_without_exif.webp";
    private static final String WEBP_WITHOUT_EXIF_WITH_LOSSLESS_ENCODING =
            "webp_lossless_without_exif.webp";
    private static final String JPEG_WITH_DATETIME_TAG_PRIMARY_FORMAT =
            "jpeg_with_datetime_tag_primary_format.jpg";
    private static final String JPEG_WITH_DATETIME_TAG_SECONDARY_FORMAT =
            "jpeg_with_datetime_tag_secondary_format.jpg";
    private static final String HEIF_WITH_EXIF = "heif_with_exif.heic";
    private static final int[] IMAGE_RESOURCES = new int[] {
            R.raw.jpeg_with_exif_byte_order_ii, R.raw.jpeg_with_exif_byte_order_mm,
            R.raw.dng_with_exif_with_xmp, R.raw.jpeg_with_exif_with_xmp,
            R.raw.png_with_exif_byte_order_ii, R.raw.png_without_exif, R.raw.webp_with_exif,
            R.raw.webp_with_anim_without_exif, R.raw.webp_without_exif,
            R.raw.webp_lossless_without_exif, R.raw.jpeg_with_datetime_tag_primary_format,
            R.raw.jpeg_with_datetime_tag_secondary_format, R.raw.heif_with_exif};
    private static final String[] IMAGE_FILENAMES = new String[] {
            JPEG_WITH_EXIF_BYTE_ORDER_II, JPEG_WITH_EXIF_BYTE_ORDER_MM, DNG_WITH_EXIF_WITH_XMP,
            JPEG_WITH_EXIF_WITH_XMP, PNG_WITH_EXIF_BYTE_ORDER_II, PNG_WITHOUT_EXIF,
            WEBP_WITH_EXIF, WEBP_WITHOUT_EXIF_WITH_ANIM_DATA, WEBP_WITHOUT_EXIF,
            WEBP_WITHOUT_EXIF_WITH_LOSSLESS_ENCODING, JPEG_WITH_DATETIME_TAG_PRIMARY_FORMAT,
            JPEG_WITH_DATETIME_TAG_SECONDARY_FORMAT, HEIF_WITH_EXIF};

    private static final int USER_READ_WRITE = 0600;
    private static final String TEST_TEMP_FILE_NAME = "testImage";
    private static final double DELTA = 1e-8;
    // We translate double to rational in a 1/10000 precision.
    private static final double RATIONAL_DELTA = 0.0001;
    private static final int TEST_LAT_LONG_VALUES_ARRAY_LENGTH = 8;
    private static final int TEST_NUMBER_OF_CORRUPTED_IMAGE_STREAMS = 30;
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
    private static final HashMap<Integer, Pair> FLIP_STATE_AND_ROTATION_DEGREES = new HashMap<>();
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

    private static class ExpectedValue {
        // Thumbnail information.
        public final boolean hasThumbnail;
        public final int thumbnailWidth;
        public final int thumbnailHeight;
        public final boolean isThumbnailCompressed;
        public final int thumbnailOffset;
        public final int thumbnailLength;

        // GPS information.
        public final boolean hasLatLong;
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
        public final float flash;
        public final String focalLength;
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
        public final int whiteBalance;

        // XMP information.
        public final boolean hasXmp;
        public final int xmpOffset;
        public final int xmpLength;

        private static String getString(TypedArray typedArray, int index) {
            String stringValue = typedArray.getString(index);
            if (stringValue == null || stringValue.equals("")) {
                return null;
            }
            return stringValue.trim();
        }

        ExpectedValue(TypedArray typedArray) {
            int index = 0;

            // Reads thumbnail information.
            hasThumbnail = typedArray.getBoolean(index++, false);
            thumbnailOffset = typedArray.getInt(index++, -1);
            thumbnailLength = typedArray.getInt(index++, -1);
            thumbnailWidth = typedArray.getInt(index++, 0);
            thumbnailHeight = typedArray.getInt(index++, 0);
            isThumbnailCompressed = typedArray.getBoolean(index++, false);

            // Reads GPS information.
            hasLatLong = typedArray.getBoolean(index++, false);
            latitudeOffset = typedArray.getInt(index++, -1);
            latitudeLength = typedArray.getInt(index++, -1);
            latitude = typedArray.getFloat(index++, 0f);
            longitude = typedArray.getFloat(index++, 0f);
            altitude = typedArray.getFloat(index++, 0f);

            // Reads Make information.
            hasMake = typedArray.getBoolean(index++, false);
            makeOffset = typedArray.getInt(index++, -1);
            makeLength = typedArray.getInt(index++, -1);
            make = getString(typedArray, index++);

            // Reads values.
            model = getString(typedArray, index++);
            aperture = typedArray.getFloat(index++, 0f);
            dateTimeOriginal = getString(typedArray, index++);
            exposureTime = typedArray.getFloat(index++, 0f);
            flash = typedArray.getFloat(index++, 0f);
            focalLength = getString(typedArray, index++);
            gpsAltitude = getString(typedArray, index++);
            gpsAltitudeRef = getString(typedArray, index++);
            gpsDatestamp = getString(typedArray, index++);
            gpsLatitude = getString(typedArray, index++);
            gpsLatitudeRef = getString(typedArray, index++);
            gpsLongitude = getString(typedArray, index++);
            gpsLongitudeRef = getString(typedArray, index++);
            gpsProcessingMethod = getString(typedArray, index++);
            gpsTimestamp = getString(typedArray, index++);
            imageLength = typedArray.getInt(index++, 0);
            imageWidth = typedArray.getInt(index++, 0);
            iso = getString(typedArray, index++);
            orientation = typedArray.getInt(index++, 0);
            whiteBalance = typedArray.getInt(index++, 0);

            // Reads XMP information.
            hasXmp = typedArray.getBoolean(index++, false);
            xmpOffset = typedArray.getInt(index++, 0);
            xmpLength = typedArray.getInt(index++, 0);

            typedArray.recycle();
        }
    }

    @Before
    public void setUp() throws Exception {
        if (ENABLE_STRICT_MODE_FOR_UNBUFFERED_IO && Build.VERSION.SDK_INT >= 26) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectUnbufferedIo()
                    .penaltyDeath()
                    .build());
        }

        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            File file = getFileFromExternalDir(IMAGE_FILENAMES[i]);
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                inputStream = getApplicationContext()
                        .getResources().openRawResource(IMAGE_RESOURCES[i]);
                outputStream = new FileOutputStream(file);
                copy(inputStream, outputStream);
            } finally {
                closeQuietly(inputStream);
                closeQuietly(outputStream);
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            File imageFile = getFileFromExternalDir(IMAGE_FILENAMES[i]);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    @Test
    @LargeTest
    public void testJpegFiles() throws Throwable {
        readFromFilesWithExif(JPEG_WITH_EXIF_BYTE_ORDER_II, R.array.jpeg_with_exif_byte_order_ii);
        writeToFilesWithExif(JPEG_WITH_EXIF_BYTE_ORDER_II, R.array.jpeg_with_exif_byte_order_ii);

        readFromFilesWithExif(JPEG_WITH_EXIF_BYTE_ORDER_MM, R.array.jpeg_with_exif_byte_order_mm);
        writeToFilesWithExif(JPEG_WITH_EXIF_BYTE_ORDER_MM, R.array.jpeg_with_exif_byte_order_mm);

        readFromFilesWithExif(JPEG_WITH_EXIF_WITH_XMP, R.array.jpeg_with_exif_with_xmp);
        writeToFilesWithExif(JPEG_WITH_EXIF_WITH_XMP, R.array.jpeg_with_exif_with_xmp);
    }

    @Test
    @LargeTest
    public void testDngFiles() throws Throwable {
        readFromFilesWithExif(DNG_WITH_EXIF_WITH_XMP, R.array.dng_with_exif_with_xmp);
        writeToFilesWithExif(DNG_WITH_EXIF_WITH_XMP, R.array.dng_with_exif_with_xmp);
    }

    @Test
    @LargeTest
    public void testPngFiles() throws Throwable {
        readFromFilesWithExif(PNG_WITH_EXIF_BYTE_ORDER_II, R.array.png_with_exif_byte_order_ii);

        writeToFilesWithoutExif(PNG_WITHOUT_EXIF);
    }

    @Test
    @LargeTest
    public void testStandaloneData() throws Throwable {
        readFromStandaloneDataWithExif(JPEG_WITH_EXIF_BYTE_ORDER_II,
                R.array.standalone_data_with_exif_byte_order_ii);
        readFromStandaloneDataWithExif(JPEG_WITH_EXIF_BYTE_ORDER_MM,
                R.array.standalone_data_with_exif_byte_order_mm);
    }

    @Test
    @LargeTest
    public void testWebpFiles() throws Throwable {
        readFromFilesWithExif(WEBP_WITH_EXIF, R.array.webp_with_exif);
        writeToFilesWithExif(WEBP_WITH_EXIF, R.array.webp_with_exif);

        writeToFilesWithoutExif(WEBP_WITHOUT_EXIF_WITH_ANIM_DATA);
        writeToFilesWithoutExif(WEBP_WITHOUT_EXIF);
        writeToFilesWithoutExif(WEBP_WITHOUT_EXIF_WITH_LOSSLESS_ENCODING);
    }

    /**
     * Support for retrieving EXIF from HEIF was added in SDK 28.
     */
    @Test
    @LargeTest
    public void testHeifFile() throws Throwable {
        if (Build.VERSION.SDK_INT >= 28) {
            // Reading XMP data from HEIF was added in SDK 31.
            readFromFilesWithExif(HEIF_WITH_EXIF,
                    Build.VERSION.SDK_INT >= 31
                            ? R.array.heif_with_exif_31_and_above
                            : R.array.heif_with_exif);
        } else {
            // Make sure that an exception is not thrown and that image length/width tag values
            // return default values, not the actual values.
            File imageFile = getFileFromExternalDir(HEIF_WITH_EXIF);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            String defaultTagValue = "0";
            assertEquals(defaultTagValue, exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            assertEquals(defaultTagValue, exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
        }
    }

    @Test
    @LargeTest
    public void testDoNotFailOnCorruptedImage() throws Throwable {
        // ExifInterface shouldn't raise any exceptions except an IOException when unable to open
        // a file, even with a corrupted image. Generates randomly corrupted image stream for
        // testing. Uses Epoch date count as random seed so that we can reproduce a broken test.
        long seed = System.currentTimeMillis() / (86400 * 1000);
        Log.d(TAG, "testDoNotFailOnCorruptedImage random seed: " + seed);
        Random random = new Random(seed);
        byte[] bytes = new byte[8096];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i < TEST_NUMBER_OF_CORRUPTED_IMAGE_STREAMS; i++) {
            buffer.clear();
            random.nextBytes(bytes);
            if (!randomlyCorrupted(random)) {
                buffer.put(ExifInterface.JPEG_SIGNATURE);
            }
            if (!randomlyCorrupted(random)) {
                buffer.put(ExifInterface.MARKER_APP1);
            }
            buffer.putShort((short) (random.nextInt(100) + 300));
            if (!randomlyCorrupted(random)) {
                buffer.put(ExifInterface.IDENTIFIER_EXIF_APP1);
            }
            if (!randomlyCorrupted(random)) {
                buffer.putShort(ExifInterface.BYTE_ALIGN_MM);
            }
            if (!randomlyCorrupted(random)) {
                buffer.put((byte) 0);
                buffer.put(ExifInterface.START_CODE);
            }
            buffer.putInt(8);

            // Primary Tags
            int numberOfDirectory = random.nextInt(8) + 1;
            if (!randomlyCorrupted(random)) {
                buffer.putShort((short) numberOfDirectory);
            }
            for (int j = 0; j < numberOfDirectory; j++) {
                generateRandomExifTag(buffer, ExifInterface.IFD_TYPE_PRIMARY, random);
            }
            if (!randomlyCorrupted(random)) {
                buffer.putInt(buffer.position() - 8);
            }

            // Thumbnail Tags
            numberOfDirectory = random.nextInt(8) + 1;
            if (!randomlyCorrupted(random)) {
                buffer.putShort((short) numberOfDirectory);
            }
            for (int j = 0; j < numberOfDirectory; j++) {
                generateRandomExifTag(buffer, ExifInterface.IFD_TYPE_THUMBNAIL, random);
            }
            if (!randomlyCorrupted(random)) {
                buffer.putInt(buffer.position() - 8);
            }

            // Preview Tags
            numberOfDirectory = random.nextInt(8) + 1;
            if (!randomlyCorrupted(random)) {
                buffer.putShort((short) numberOfDirectory);
            }
            for (int j = 0; j < numberOfDirectory; j++) {
                generateRandomExifTag(buffer, ExifInterface.IFD_TYPE_PREVIEW, random);
            }
            if (!randomlyCorrupted(random)) {
                buffer.putInt(buffer.position() - 8);
            }

            if (!randomlyCorrupted(random)) {
                buffer.put(ExifInterface.MARKER);
            }
            if (!randomlyCorrupted(random)) {
                buffer.put(ExifInterface.MARKER_EOI);
            }

            try {
                new ExifInterface(new ByteArrayInputStream(bytes));
                // Always success
            } catch (IOException e) {
                fail("Should not reach here!");
            }
        }
    }

    @Test
    @SmallTest
    public void testSetGpsInfo() throws IOException {
        final String provider = "ExifInterfaceTest";
        final long timestamp = System.currentTimeMillis();
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

        File imageFile = getFileFromExternalDir(JPEG_WITH_DATETIME_TAG_PRIMARY_FORMAT);
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
        final long currentTimeStamp = System.currentTimeMillis();
        final long expectedDatetimeOffsetLongValue = 32400000L;
        exif.setDateTime(currentTimeStamp);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(currentTimeStamp - expectedDatetimeOffsetLongValue, (long) exif.getDateTime());

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

        File imageFile = getFileFromExternalDir(JPEG_WITH_DATETIME_TAG_SECONDARY_FORMAT);
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
        File imageFile = getFileFromExternalDir(JPEG_WITH_DATETIME_TAG_PRIMARY_FORMAT);
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
        File imageFile = getFileFromExternalDir(JPEG_WITH_DATETIME_TAG_PRIMARY_FORMAT);
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
        File imageFile = getFileFromExternalDir(JPEG_WITH_EXIF_BYTE_ORDER_II);
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
            assertEquals(FLIP_STATE_AND_ROTATION_DEGREES.get(key).second,
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
        assertFloatTag(exifInterface, ExifInterface.TAG_FLASH, expectedValue.flash);
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
        assertIntTag(exifInterface, ExifInterface.TAG_WHITE_BALANCE, expectedValue.whiteBalance);

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

    private void readFromStandaloneDataWithExif(String fileName, int typedArrayResourceId)
            throws IOException {
        ExpectedValue expectedValue = new ExpectedValue(
                getApplicationContext().getResources().obtainTypedArray(typedArrayResourceId));

        File imageFile = getFileFromExternalDir(fileName);
        String verboseTag = imageFile.getName();

        FileInputStream fis = new FileInputStream(imageFile);
        // Skip the following marker bytes (0xff, 0xd8, 0xff, 0xe1)
        fis.skip(4);
        // Read the value of the length of the exif data
        short length = readShort(fis);
        byte[] exifBytes = new byte[length];
        fis.read(exifBytes);

        ByteArrayInputStream bin = new ByteArrayInputStream(exifBytes);
        ExifInterface exifInterface =
                new ExifInterface(bin, ExifInterface.STREAM_TYPE_EXIF_DATA_ONLY);
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);
    }

    private void testExifInterfaceCommon(String fileName, ExpectedValue expectedValue)
            throws IOException {
        File imageFile = getFileFromExternalDir(fileName);
        String verboseTag = imageFile.getName();

        // Creates via file.
        ExifInterface exifInterface = new ExifInterface(imageFile);
        assertNotNull(exifInterface);
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);

        // Creates via path.
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        assertNotNull(exifInterface);
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);

        InputStream in = null;
        // Creates via InputStream.
        try {
            in = new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()));
            exifInterface = new ExifInterface(in);
            compareWithExpectedValue(exifInterface, expectedValue, verboseTag, true);
        } finally {
            closeQuietly(in);
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

    private void testExifInterfaceRange(String fileName, ExpectedValue expectedValue)
            throws IOException {
        File imageFile = getFileFromExternalDir(fileName);

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()));
            if (expectedValue.hasThumbnail) {
                in.skip(expectedValue.thumbnailOffset);
                byte[] thumbnailBytes = new byte[expectedValue.thumbnailLength];
                if (in.read(thumbnailBytes) != expectedValue.thumbnailLength) {
                    throw new IOException("Failed to read the expected thumbnail length");
                }
                // TODO: Need a way to check uncompressed thumbnail file
                Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnailBytes, 0,
                        thumbnailBytes.length);
                assertNotNull(thumbnailBitmap);
                assertEquals(expectedValue.thumbnailWidth, thumbnailBitmap.getWidth());
                assertEquals(expectedValue.thumbnailHeight, thumbnailBitmap.getHeight());
            }

            // TODO: Creating a new input stream is a temporary
            //  workaround for BufferedInputStream#mark/reset not working properly for
            //  LG_G4_ISO_800_DNG. Need to investigate cause.
            in = new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()));
            if (expectedValue.hasMake) {
                in.skip(expectedValue.makeOffset);
                byte[] makeBytes = new byte[expectedValue.makeLength];
                if (in.read(makeBytes) != expectedValue.makeLength) {
                    throw new IOException("Failed to read the expected make length");
                }
                String makeString = new String(makeBytes);
                // Remove null bytes
                makeString = makeString.replaceAll("\u0000.*", "");
                assertEquals(expectedValue.make, makeString);
            }

            in = new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()));
            if (expectedValue.hasXmp) {
                in.skip(expectedValue.xmpOffset);
                byte[] identifierBytes = new byte[expectedValue.xmpLength];
                if (in.read(identifierBytes) != expectedValue.xmpLength) {
                    throw new IOException("Failed to read the expected xmp length");
                }
                final String xmpIdentifier = "<?xpacket begin=";
                assertTrue(new String(identifierBytes, Charset.forName("UTF-8"))
                        .startsWith(xmpIdentifier));
            }
            // TODO: Add code for retrieving raw latitude data using offset and length
        } finally {
            closeQuietly(in);
        }
    }

    private void writeToFilesWithExif(String fileName, int typedArrayResourceId)
            throws IOException {
        ExpectedValue expectedValue = new ExpectedValue(
                getApplicationContext().getResources().obtainTypedArray(typedArrayResourceId));

        File imageFile = getFileFromExternalDir(fileName);
        String verboseTag = imageFile.getName();

        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag, false);

        // Test for modifying one attribute.
        String backupValue = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
        exifInterface.saveAttributes();
        // Check if thumbnail offset and length are properly updated without parsing the data again.
        if (expectedValue.hasThumbnail) {
            testThumbnail(expectedValue, exifInterface);
        }
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
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

    private void readFromFilesWithExif(String fileName, int typedArrayResourceId)
            throws IOException {
        ExpectedValue expectedValue = new ExpectedValue(
                getApplicationContext().getResources().obtainTypedArray(typedArrayResourceId));

        // Test for reading from external data storage.
        testExifInterfaceCommon(fileName, expectedValue);

        // Test for checking expected range by retrieving raw data with given offset and length.
        testExifInterfaceRange(fileName, expectedValue);
    }

    private void writeToFilesWithoutExif(String fileName) throws IOException {
        File imageFile = getFileFromExternalDir(fileName);

        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
        exifInterface.saveAttributes();

        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        String make = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        assertEquals("abc", make);
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

    private void generateRandomExifTag(ByteBuffer buffer, int ifdType, Random random) {
        ExifInterface.ExifTag[] tagGroup = ExifInterface.EXIF_TAGS[ifdType];
        ExifInterface.ExifTag tag = tagGroup[random.nextInt(tagGroup.length)];
        if (!randomlyCorrupted(random)) {
            buffer.putShort((short) tag.number);
        }
        int dataFormat = random.nextInt(ExifInterface.IFD_FORMAT_NAMES.length);
        if (!randomlyCorrupted(random)) {
            buffer.putShort((short) dataFormat);
        }
        buffer.putInt(1);
        int dataLength = ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[dataFormat];
        if (dataLength > 4) {
            buffer.putShort((short) random.nextInt(8096 - dataLength));
            buffer.position(buffer.position() + 2);
        } else {
            buffer.position(buffer.position() + 4);
        }
    }

    private boolean randomlyCorrupted(Random random) {
        // Corrupts somewhere in a possibility of 1/500.
        return random.nextInt(500) == 0;
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

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

    private int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            total += c;
            out.write(buffer, 0, c);
        }
        return total;
    }

    private void assertLatLongValuesAreNotSet(ExifInterface exif) {
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
    }

    private ExifInterface createTestExifInterface() throws IOException {
        File image = File.createTempFile(TEST_TEMP_FILE_NAME, ".jpg");
        image.deleteOnExit();
        return new ExifInterface(image.getAbsolutePath());
    }

    private short readShort(InputStream is) throws IOException {
        int ch1 = is.read();
        int ch2 = is.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << 8) + (ch2));
    }

    private File getFileFromExternalDir(String fileName) {
        return new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                fileName);
    }
}
