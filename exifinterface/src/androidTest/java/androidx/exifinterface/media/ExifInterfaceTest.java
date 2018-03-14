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

import static android.support.test.InstrumentationRegistry.getContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Environment;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.util.Pair;

import androidx.exifinterface.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link ExifInterface}.
 */
@RunWith(AndroidJUnit4.class)
public class ExifInterfaceTest {
    private static final String TAG = ExifInterface.class.getSimpleName();
    private static final boolean VERBOSE = false;  // lots of logging
    private static final double DIFFERENCE_TOLERANCE = .001;

    private static final String EXIF_BYTE_ORDER_II_JPEG = "image_exif_byte_order_ii.jpg";
    private static final String EXIF_BYTE_ORDER_MM_JPEG = "image_exif_byte_order_mm.jpg";
    private static final String LG_G4_ISO_800_DNG = "lg_g4_iso_800.dng";
    private static final int[] IMAGE_RESOURCES = new int[] {
            R.raw.image_exif_byte_order_ii, R.raw.image_exif_byte_order_mm, R.raw.lg_g4_iso_800};
    private static final String[] IMAGE_FILENAMES = new String[] {
            EXIF_BYTE_ORDER_II_JPEG, EXIF_BYTE_ORDER_MM_JPEG, LG_G4_ISO_800_DNG};

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
                ExifInterface.ORIENTATION_UNDEFINED, new Pair(false, 0));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_NORMAL, new Pair(false, 0));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_ROTATE_90, new Pair(false, 90));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_ROTATE_180, new Pair(false, 180));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_ROTATE_270, new Pair(false, 270));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL, new Pair(true, 0));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_TRANSVERSE, new Pair(true, 90));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_FLIP_VERTICAL, new Pair(true, 180));
        FLIP_STATE_AND_ROTATION_DEGREES.put(
                ExifInterface.ORIENTATION_TRANSPOSE, new Pair(true, 270));
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

        // GPS information.
        public final boolean hasLatLong;
        public final float latitude;
        public final float longitude;
        public final float altitude;

        // Values.
        public final String make;
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

        private static String getString(TypedArray typedArray, int index) {
            String stringValue = typedArray.getString(index);
            if (stringValue == null || stringValue.equals("")) {
                return null;
            }
            return stringValue.trim();
        }

        ExpectedValue(TypedArray typedArray) {
            // Reads thumbnail information.
            hasThumbnail = typedArray.getBoolean(0, false);
            thumbnailWidth = typedArray.getInt(1, 0);
            thumbnailHeight = typedArray.getInt(2, 0);

            // Reads GPS information.
            hasLatLong = typedArray.getBoolean(3, false);
            latitude = typedArray.getFloat(4, 0f);
            longitude = typedArray.getFloat(5, 0f);
            altitude = typedArray.getFloat(6, 0f);

            // Reads values.
            make = getString(typedArray, 7);
            model = getString(typedArray, 8);
            aperture = typedArray.getFloat(9, 0f);
            dateTimeOriginal = getString(typedArray, 10);
            exposureTime = typedArray.getFloat(11, 0f);
            flash = typedArray.getFloat(12, 0f);
            focalLength = getString(typedArray, 13);
            gpsAltitude = getString(typedArray, 14);
            gpsAltitudeRef = getString(typedArray, 15);
            gpsDatestamp = getString(typedArray, 16);
            gpsLatitude = getString(typedArray, 17);
            gpsLatitudeRef = getString(typedArray, 18);
            gpsLongitude = getString(typedArray, 19);
            gpsLongitudeRef = getString(typedArray, 20);
            gpsProcessingMethod = getString(typedArray, 21);
            gpsTimestamp = getString(typedArray, 22);
            imageLength = typedArray.getInt(23, 0);
            imageWidth = typedArray.getInt(24, 0);
            iso = getString(typedArray, 25);
            orientation = typedArray.getInt(26, 0);
            whiteBalance = typedArray.getInt(27, 0);

            typedArray.recycle();
        }
    }

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < IMAGE_RESOURCES.length; ++i) {
            String outputPath =
                    new File(Environment.getExternalStorageDirectory(), IMAGE_FILENAMES[i])
                            .getAbsolutePath();

            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                inputStream = getContext().getResources().openRawResource(IMAGE_RESOURCES[i]);
                outputStream = new FileOutputStream(outputPath);
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
            String imageFilePath =
                    new File(Environment.getExternalStorageDirectory(), IMAGE_FILENAMES[i])
                            .getAbsolutePath();
            File imageFile = new File(imageFilePath);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    @Test
    @LargeTest
    public void testReadExifDataFromExifByteOrderIIJpeg() throws Throwable {
        testExifInterfaceForJpeg(EXIF_BYTE_ORDER_II_JPEG, R.array.exifbyteorderii_jpg);
    }

    @Test
    @LargeTest
    public void testReadExifDataFromExifByteOrderMMJpeg() throws Throwable {
        testExifInterfaceForJpeg(EXIF_BYTE_ORDER_MM_JPEG, R.array.exifbyteordermm_jpg);
    }

    @Test
    @LargeTest
    public void testReadExifDataFromLgG4Iso800Dng() throws Throwable {
        testExifInterfaceForRaw(LG_G4_ISO_800_DNG, R.array.lg_g4_iso_800_dng);
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

    @Test
    @SmallTest
    public void testSetDateTime() throws IOException {
        final String dateTimeValue = "2017:02:02 22:22:22";
        final String dateTimeOriginalValue = "2017:01:01 11:11:11";

        File imageFile = new File(
                Environment.getExternalStorageDirectory(), EXIF_BYTE_ORDER_II_JPEG);
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_DATETIME, dateTimeValue);
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTimeOriginalValue);
        exif.saveAttributes();

        // Check that the DATETIME value is not overwritten by DATETIME_ORIGINAL's value.
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(dateTimeValue, exif.getAttribute(ExifInterface.TAG_DATETIME));
        assertEquals(dateTimeOriginalValue, exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));

        // Now remove the DATETIME value.
        exif.setAttribute(ExifInterface.TAG_DATETIME, null);
        exif.saveAttributes();

        // When the DATETIME has no value, then it should be set to DATETIME_ORIGINAL's value.
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(dateTimeOriginalValue, exif.getAttribute(ExifInterface.TAG_DATETIME));

        long currentTimeStamp = System.currentTimeMillis();
        exif.setDateTime(currentTimeStamp);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals(currentTimeStamp, exif.getDateTime());
    }

    @Test
    @SmallTest
    public void testRotation() throws IOException {
        File imageFile = new File(
                Environment.getExternalStorageDirectory(), EXIF_BYTE_ORDER_II_JPEG);
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
            ExpectedValue expectedValue, String verboseTag) {
        if (VERBOSE) {
            printExifTagsAndValues(verboseTag, exifInterface);
        }
        // Checks a thumbnail image.
        assertEquals(expectedValue.hasThumbnail, exifInterface.hasThumbnail());
        if (expectedValue.hasThumbnail) {
            byte[] thumbnailBytes = exifInterface.getThumbnailBytes();
            assertNotNull(thumbnailBytes);
            Bitmap thumbnailBitmap = exifInterface.getThumbnailBitmap();
            assertNotNull(thumbnailBitmap);
            assertEquals(expectedValue.thumbnailWidth, thumbnailBitmap.getWidth());
            assertEquals(expectedValue.thumbnailHeight, thumbnailBitmap.getHeight());
        } else {
            assertNull(exifInterface.getThumbnail());
        }

        // Checks GPS information.
        double[] latLong = exifInterface.getLatLong();
        assertEquals(expectedValue.hasLatLong, latLong != null);
        if (expectedValue.hasLatLong) {
            assertEquals(expectedValue.latitude, latLong[0], DIFFERENCE_TOLERANCE);
            assertEquals(expectedValue.longitude, latLong[1], DIFFERENCE_TOLERANCE);
        }
        assertEquals(expectedValue.altitude, exifInterface.getAltitude(.0), DIFFERENCE_TOLERANCE);

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
    }

    private void testExifInterfaceCommon(String fileName, ExpectedValue expectedValue)
            throws IOException {
        File imageFile = new File(Environment.getExternalStorageDirectory(), fileName);
        String verboseTag = imageFile.getName();

        // Creates via path.
        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        assertNotNull(exifInterface);
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag);

        InputStream in = null;
        // Creates via InputStream.
        try {
            in = new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()));
            exifInterface = new ExifInterface(in);
            compareWithExpectedValue(exifInterface, expectedValue, verboseTag);
        } finally {
            closeQuietly(in);
        }
    }

    private void testSaveAttributes_withFileName(String fileName, ExpectedValue expectedValue)
            throws IOException {
        File imageFile = new File(Environment.getExternalStorageDirectory(), fileName);
        String verboseTag = imageFile.getName();

        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag);

        // Test for modifying one attribute.
        String backupValue = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        assertEquals("abc", exifInterface.getAttribute(ExifInterface.TAG_MAKE));
        // Restore the backup value.
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, backupValue);
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedValue(exifInterface, expectedValue, verboseTag);
    }

    private void testExifInterfaceForJpeg(String fileName, int typedArrayResourceId)
            throws IOException {
        ExpectedValue expectedValue = new ExpectedValue(
                getContext().getResources().obtainTypedArray(typedArrayResourceId));

        // Test for reading from external data storage.
        testExifInterfaceCommon(fileName, expectedValue);

        // Test for saving attributes.
        testSaveAttributes_withFileName(fileName, expectedValue);
    }

    private void testExifInterfaceForRaw(String fileName, int typedArrayResourceId)
            throws IOException {
        ExpectedValue expectedValue = new ExpectedValue(
                getContext().getResources().obtainTypedArray(typedArrayResourceId));

        // Test for reading from external data storage.
        testExifInterfaceCommon(fileName, expectedValue);

        // Since ExifInterface does not support for saving attributes for RAW files, do not test
        // about writing back in here.
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
}
