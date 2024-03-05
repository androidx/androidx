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

import static org.junit.Assert.assertThrows;
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

import androidx.annotation.RequiresApi;
import androidx.exifinterface.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import com.google.common.truth.Expect;

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

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule public final Expect expect = Expect.create();

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
        readFromFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_BYTE_ORDER_II);
        writeToFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_BYTE_ORDER_II);
    }

    @Test
    @LargeTest
    public void testJpegWithExifMotorolaByteOrder() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_mm, "jpeg_with_exif_byte_order_mm.jpg");
        readFromFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_BYTE_ORDER_MM);
        writeToFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_BYTE_ORDER_MM);
    }

    @Test
    @LargeTest
    public void testJpegWithExifAndXmp() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_with_xmp, "jpeg_with_exif_with_xmp.jpg");
        readFromFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_WITH_XMP);
        writeToFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_WITH_XMP);
    }

    // https://issuetracker.google.com/264729367
    @Test
    @LargeTest
    public void testJpegWithInvalidOffset() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_invalid_offset, "jpeg_with_exif_invalid_offset.jpg");
        readFromFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_INVALID_OFFSET);
        writeToFilesWithExif(imageFile, ExpectedAttributes.JPEG_WITH_EXIF_INVALID_OFFSET);
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
        expectBitmapsEquivalent(srcFile, imageFile);
    }

    @Test
    @LargeTest
    public void testDngWithExifAndXmp() throws Throwable {
        File imageFile =
                copyFromResourceToFile(R.raw.dng_with_exif_with_xmp, "dng_with_exif_with_xmp.dng");
        readFromFilesWithExif(imageFile, ExpectedAttributes.DNG_WITH_EXIF_WITH_XMP);
    }

    @Test
    @LargeTest
    public void testPngWithExif() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.png_with_exif_byte_order_ii, "png_with_exif_byte_order_ii.png");
        readFromFilesWithExif(imageFile, ExpectedAttributes.PNG_WITH_EXIF_BYTE_ORDER_II);
        writeToFilesWithExif(imageFile, ExpectedAttributes.PNG_WITH_EXIF_BYTE_ORDER_II);
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
                imageFile, ExpectedAttributes.JPEG_WITH_EXIF_BYTE_ORDER_II_STANDALONE);
    }

    @Test
    @LargeTest
    public void testStandaloneData_jpegMotorolaByteOrder() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_mm, "jpeg_with_exif_byte_order_mm.jpg");
        readFromStandaloneDataWithExif(
                imageFile, ExpectedAttributes.JPEG_WITH_EXIF_BYTE_ORDER_MM_STANDALONE);
    }

    @Test
    @LargeTest
    public void testWebpWithExif() throws Throwable {
        File imageFile = copyFromResourceToFile(R.raw.webp_with_exif, "webp_with_exif.jpg");
        readFromFilesWithExif(imageFile, ExpectedAttributes.WEBP_WITH_EXIF);
        writeToFilesWithExif(imageFile, ExpectedAttributes.WEBP_WITH_EXIF);
    }

    // https://issuetracker.google.com/281638358
    @Test
    @LargeTest
    public void testWebpWithExifApp1() throws Throwable {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.invalid_webp_with_jpeg_app1_marker,
                        "invalid_webp_with_jpeg_app1_marker.webp");
        readFromFilesWithExif(imageFile, ExpectedAttributes.INVALID_WEBP_WITH_JPEG_APP1_MARKER);
        writeToFilesWithExif(imageFile, ExpectedAttributes.INVALID_WEBP_WITH_JPEG_APP1_MARKER);
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
                            ? ExpectedAttributes.HEIF_WITH_EXIF_API_31_AND_ABOVE
                            : ExpectedAttributes.HEIF_WITH_EXIF_BELOW_API_31);
        } else {
            // Make sure that an exception is not thrown and that image length/width tag values
            // return default values, not the actual values.
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            String defaultTagValue = "0";
            assertThat(exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH))
                    .isEqualTo(defaultTagValue);
            assertThat(exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)).isEqualTo(defaultTagValue);
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
        double latitudeDegrees = -68.3434534737;
        double longitudeDegrees = -58.57834236352;
        double altitudeMeters = 18.02038;
        Location location = new Location(provider);
        location.setLatitude(latitudeDegrees);
        location.setLongitude(longitudeDegrees);
        location.setAltitude(altitudeMeters);
        location.setSpeed(speedInMeterPerSec);
        location.setTime(timestamp);
        ExifInterface exif = createTestExifInterface();
        exif.setGpsInfo(location);

        double[] latLong = exif.getLatLong();
        assertThat(latLong)
                .usingTolerance(DELTA)
                .containsExactly(latitudeDegrees, longitudeDegrees)
                .inOrder();
        assertThat(exif.getAltitude(0)).isWithin(RATIONAL_DELTA).of(altitudeMeters);
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_SPEED_REF)).isEqualTo("K");
        float speedInKmph = speedInMeterPerSec * TimeUnit.HOURS.toSeconds(1) / 1000;
        assertThat(exif.getAttributeDouble(ExifInterface.TAG_GPS_SPEED, 0.0))
                .isWithin(RATIONAL_DELTA)
                .of(speedInKmph);
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD)).isEqualTo(provider);
        // GPS time's precision is secs.
        assertThat(TimeUnit.MILLISECONDS.toSeconds(exif.getGpsDateTime()))
                .isEqualTo(TimeUnit.MILLISECONDS.toSeconds(timestamp));
    }

    @Test
    @SmallTest
    public void testSetLatLong_withValidValues() throws IOException {
        testSetLatLong(0d, 0d);
        testSetLatLong(45d, -45d);
        testSetLatLong(90d, 90d);
        testSetLatLong(-60d, -120d);
        testSetLatLong(0.00000001, 180d);
        testSetLatLong(89.999999999, 0.00000001);
        testSetLatLong(14.2465923626, -179.99999999999);
        testSetLatLong(-68.3434534737, -58.57834236352);
    }

    private void testSetLatLong(double latitude, double longitude) throws IOException {
        ExifInterface exif = createTestExifInterface();

        exif.setLatLong(latitude, longitude);

        assertThat(exif.getLatLong())
                .usingTolerance(DELTA)
                .containsExactly(latitude, longitude)
                .inOrder();
    }

    @Test
    @SmallTest
    public void testSetLatLong_withInvalidLatitude() throws IOException {
        double longitude = -5.003427;
        assertLatLongInvalidAndNotPersisted(Double.NaN, longitude);
        assertLatLongInvalidAndNotPersisted(Double.POSITIVE_INFINITY, longitude);
        assertLatLongInvalidAndNotPersisted(Double.NEGATIVE_INFINITY, longitude);
        assertLatLongInvalidAndNotPersisted(90.0000000001, longitude);
        assertLatLongInvalidAndNotPersisted(263.34763236326, longitude);
        assertLatLongInvalidAndNotPersisted(-1e5, longitude);
        assertLatLongInvalidAndNotPersisted(347.32525, longitude);
        assertLatLongInvalidAndNotPersisted(-176.346347754, longitude);
    }

    @Test
    @SmallTest
    public void testSetLatLong_withInvalidLongitude() throws IOException {
        double latitude = 56.796626;
        assertLatLongInvalidAndNotPersisted(latitude, Double.NaN);
        assertLatLongInvalidAndNotPersisted(latitude, Double.POSITIVE_INFINITY);
        assertLatLongInvalidAndNotPersisted(latitude, Double.NEGATIVE_INFINITY);
        assertLatLongInvalidAndNotPersisted(latitude, 180.0000000001);
        assertLatLongInvalidAndNotPersisted(latitude, 263.34763236326);
        assertLatLongInvalidAndNotPersisted(latitude, -1e10);
        assertLatLongInvalidAndNotPersisted(latitude, 347.325252623);
        assertLatLongInvalidAndNotPersisted(latitude, -4000.346323236);
    }

    /**
     * Passes the parameters to {@link ExifInterface#setLatLong} and asserts it throws an exception
     * because one or both are invalid, and then all the corresponding getters still return 'unset'
     * values.
     */
    private void assertLatLongInvalidAndNotPersisted(double latitude, double longitude)
            throws IOException {
        ExifInterface exif = createTestExifInterface();

        assertThrows(IllegalArgumentException.class, () -> exif.setLatLong(latitude, longitude));

        assertThat(exif.getLatLong()).isNull();
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)).isNull();
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)).isNull();
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)).isNull();
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)).isNull();
    }

    @Test
    @SmallTest
    public void testSetAltitude() throws IOException {
        testSetAltitudeInternal(0);
        testSetAltitudeInternal(-2000);
        testSetAltitudeInternal(10000);
        testSetAltitudeInternal(-355.99999999999);
        testSetAltitudeInternal(18.02038);
    }

    private void testSetAltitudeInternal(double altitude) throws IOException {
        ExifInterface exif = createTestExifInterface();
        exif.setAltitude(altitude);
        assertThat(exif.getAltitude(Double.NaN)).isWithin(RATIONAL_DELTA).of(altitude);
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
        assertThat(exif.getDateTime()).isEqualTo(expectedGetDatetimeValue);
        assertThat(exif.getDateTimeOriginal()).isEqualTo(expectedGetDatetimeValue);
        assertThat(exif.getDateTimeDigitized()).isEqualTo(expectedGetDatetimeValue);
        assertThat(exif.getGpsDateTime()).isEqualTo(expectedGetGpsDatetimeValue);
        assertThat(exif.getAttribute(ExifInterface.TAG_OFFSET_TIME))
                .isEqualTo(expectedDatetimeOffsetStringValue);
        assertThat(exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
                .isEqualTo(expectedDatetimeOffsetStringValue);
        assertThat(exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED))
                .isEqualTo(expectedDatetimeOffsetStringValue);

        // Test setting datetime values
        final long newTimestamp = 1689328448000L; // 2023-07-14T09:54:32.000Z
        final long expectedDatetimeOffsetLongValue = 32400000L;
        exif.setDateTime(newTimestamp);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertThat(exif.getDateTime()).isEqualTo(newTimestamp - expectedDatetimeOffsetLongValue);

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
        assertThat(exif.getAttribute(ExifInterface.TAG_DATETIME))
                .isEqualTo(expectedDateTimeStringValue);
        assertThat(exif.getDateTime()).isEqualTo(expectedGetDatetimeValue);

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
        assertThat(exif.getAttribute(ExifInterface.TAG_DATETIME))
                .isEqualTo(modifiedNewDateTimeStringValue);
        assertThat(exif.getDateTime()).isEqualTo(newDateTimeLongValue);
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
        assertThat(exif.getAttribute(ExifInterface.TAG_DATETIME)).isEqualTo(dateTimeValue);
        assertThat(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                .isEqualTo(dateTimeOriginalValue);

        // 2. Check that when TAG_DATETIME has no value, it is set to TAG_DATETIME_ORIGINAL's value.
        exif.setAttribute(ExifInterface.TAG_DATETIME, null);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertThat(exif.getAttribute(ExifInterface.TAG_DATETIME)).isEqualTo(dateTimeOriginalValue);
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
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo("000");
        long currentDateTimeValue = exif.getDateTime();

        // Test that single and double-digit values are set properly.
        // Note that since SubSecTime tag records fractions of a second, a single-digit value
        // should be counted as the first decimal value, which is why "1" becomes 100ms and "11"
        // becomes 110ms.
        String oneDigitSubSec = "1";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, oneDigitSubSec);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 100);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo(oneDigitSubSec);

        String twoDigitSubSec1 = "01";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, twoDigitSubSec1);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 10);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo(twoDigitSubSec1);

        String twoDigitSubSec2 = "11";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, twoDigitSubSec2);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 110);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo(twoDigitSubSec2);

        // Test that 3-digit values are set properly.
        String hundredMs = "100";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, hundredMs);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 100);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo(hundredMs);

        // Test that values starting with zero are also supported.
        String oneMsStartingWithZeroes = "001";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, oneMsStartingWithZeroes);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 1);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME))
                .isEqualTo(oneMsStartingWithZeroes);

        String tenMsStartingWithZero = "010";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, tenMsStartingWithZero);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 10);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME))
                .isEqualTo(tenMsStartingWithZero);

        // Test that values with more than three digits are set properly. getAttribute() should
        // return the whole string, but getDateTime() should only add the first three digits
        // because it supports only up to 1/1000th of a second.
        String fourDigitSubSec = "1234";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, fourDigitSubSec);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 123);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo(fourDigitSubSec);

        String fiveDigitSubSec = "23456";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, fiveDigitSubSec);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 234);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo(fiveDigitSubSec);

        String sixDigitSubSec = "345678";
        exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, sixDigitSubSec);
        exif.saveAttributes();
        assertThat(exif.getDateTime()).isEqualTo(currentDateTimeValue + 345);
        assertThat(exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME)).isEqualTo(sixDigitSubSec);
    }

    @Test
    @LargeTest
    public void testRotation() throws IOException {
        // Test flip vertically.
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_TRANSPOSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_ROTATE_270);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                ExifInterface::flipVertically,
                ExifInterface.ORIENTATION_ROTATE_90);

        // Test flip horizontally.
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_TRANSPOSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_ROTATE_90);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                ExifInterface::flipHorizontally,
                ExifInterface.ORIENTATION_ROTATE_270);

        // Test rotate by degrees
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_UNDEFINED,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_UNDEFINED);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_ROTATE_270);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_ROTATE_90);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_ROTATE_270);
        testModifyOrientation(
                ExifInterface.ORIENTATION_NORMAL,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_ROTATE_90);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_ROTATE_270);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_90,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_ROTATE_270);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_ROTATE_90);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_ROTATE_270);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_ROTATE_90);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_180,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_ROTATE_270);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_NORMAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_ROTATE_90);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_ROTATE_180);
        testModifyOrientation(
                ExifInterface.ORIENTATION_ROTATE_270,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_ROTATE_90);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_TRANSPOSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_VERTICAL,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_TRANSPOSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_TRANSPOSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_TRANSPOSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSPOSE,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                exifInterface -> exifInterface.rotate(-90),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                exifInterface -> exifInterface.rotate(0),
                ExifInterface.ORIENTATION_TRANSVERSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                exifInterface -> exifInterface.rotate(90),
                ExifInterface.ORIENTATION_FLIP_VERTICAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                exifInterface -> exifInterface.rotate(180),
                ExifInterface.ORIENTATION_TRANSPOSE);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                exifInterface -> exifInterface.rotate(270),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL);
        testModifyOrientation(
                ExifInterface.ORIENTATION_TRANSVERSE,
                exifInterface -> exifInterface.rotate(540),
                ExifInterface.ORIENTATION_TRANSPOSE);
    }

    private void testModifyOrientation(
            int originalOrientation,
            ExifInterfaceOperation rotationOperation,
            int expectedOrientation)
            throws IOException {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_ii, "jpeg_with_exif_byte_order_ii.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(originalOrientation));
        rotationOperation.applyTo(exif);
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertThat(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0))
                .isEqualTo(expectedOrientation);
        imageFile.delete();
    }

    @Test
    @LargeTest
    public void testRotation_byDegrees_invalid() throws IOException {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_ii, "jpeg_with_exif_byte_order_ii.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        assertThrows(IllegalArgumentException.class, () -> exif.rotate(108));
    }

    @Test
    @LargeTest
    public void testRotation_flipState() throws IOException {
        testFlipStateAndRotation(ExifInterface.ORIENTATION_UNDEFINED, false, 0);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_NORMAL, false, 0);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_ROTATE_90, false, 90);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_ROTATE_180, false, 180);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_ROTATE_270, false, 270);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, true, 0);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_TRANSVERSE, true, 90);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_FLIP_VERTICAL, true, 180);
        testFlipStateAndRotation(ExifInterface.ORIENTATION_TRANSPOSE, true, 270);
    }

    private void testFlipStateAndRotation(
            int orientation, boolean expectedFlipState, int expectedDegrees) throws IOException {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_ii, "jpeg_with_exif_byte_order_ii.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertThat(exif.isFlipped()).isEqualTo(expectedFlipState);
        assertThat(exif.getRotationDegrees()).isEqualTo(expectedDegrees);
        imageFile.delete();
    }

    @Test
    @LargeTest
    public void testResetOrientation() throws IOException {
        File imageFile =
                copyFromResourceToFile(
                        R.raw.jpeg_with_exif_byte_order_ii, "jpeg_with_exif_byte_order_ii.jpg");
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                Integer.toString(ExifInterface.ORIENTATION_FLIP_HORIZONTAL));
        exif.resetOrientation();
        exif.saveAttributes();
        exif = new ExifInterface(imageFile.getAbsolutePath());
        assertThat(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0))
                .isEqualTo(ExifInterface.ORIENTATION_NORMAL);
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
        assertThat(exif.getAttribute(oldTag)).isEqualTo(isoValue);
        assertThat(exif.getAttribute(newTag)).isEqualTo(isoValue);

        exif = createTestExifInterface();
        exif.setAttribute(newTag, isoValue);
        assertThat(exif.getAttribute(oldTag)).isEqualTo(isoValue);
        assertThat(exif.getAttribute(newTag)).isEqualTo(isoValue);
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

    private void expectStringTag(ExifInterface exifInterface, String tag, String expectedValue) {
        String stringValue = exifInterface.getAttribute(tag);
        if (stringValue != null) {
            stringValue = stringValue.trim();
        }
        stringValue = ("".equals(stringValue)) ? null : stringValue;

        expect.that(stringValue).isEqualTo(expectedValue);
    }

    private void compareWithExpectedAttributes(
            ExifInterface exifInterface,
            ExpectedAttributes expectedAttributes,
            String verboseTag,
            boolean assertRanges) {
        if (VERBOSE) {
            printExifTagsAndValues(verboseTag, exifInterface);
        }
        // Checks a thumbnail image.
        expect.that(exifInterface.hasThumbnail()).isEqualTo(expectedAttributes.hasThumbnail);
        if (expectedAttributes.hasThumbnail) {
            expect.that(exifInterface.getThumbnailRange()).isNotNull();
            if (assertRanges) {
                expect.that(exifInterface.getThumbnailRange())
                        .asList()
                        .containsExactly(
                                expectedAttributes.thumbnailOffset,
                                expectedAttributes.thumbnailLength)
                        .inOrder();
            }
            testThumbnail(expectedAttributes, exifInterface);
        } else {
            expect.that(exifInterface.getThumbnailRange()).isNull();
            expect.that(exifInterface.getThumbnail()).isNull();
        }

        // Checks GPS information.
        double[] latLong = exifInterface.getLatLong();
        long[] latitudeRange = exifInterface.getAttributeRange(ExifInterface.TAG_GPS_LATITUDE);
        if (expectedAttributes.hasLatLong) {
            expect.that(latitudeRange).isNotNull();
            if (assertRanges) {
                expect.that(latitudeRange)
                        .asList()
                        .containsExactly(
                                expectedAttributes.latitudeOffset,
                                expectedAttributes.latitudeLength)
                        .inOrder();
            }
            expect.that(latLong)
                    .usingTolerance(DIFFERENCE_TOLERANCE)
                    .containsExactly(expectedAttributes.latitude, expectedAttributes.longitude)
                    .inOrder();
            expect.that(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LATITUDE)).isTrue();
            expect.that(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LONGITUDE)).isTrue();
        } else {
            expect.that(latLong).isNull();
            expect.that(latitudeRange).isNull();
            expect.that(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LATITUDE)).isFalse();
            expect.that(exifInterface.hasAttribute(ExifInterface.TAG_GPS_LONGITUDE)).isFalse();
        }
        expect.that(exifInterface.getAltitude(.0))
                .isWithin(DIFFERENCE_TOLERANCE)
                .of(expectedAttributes.altitude);

        // Checks Make information.
        String make = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        long[] makeRange = exifInterface.getAttributeRange(ExifInterface.TAG_MAKE);
        if (expectedAttributes.hasMake) {
            expect.that(makeRange).isNotNull();
            if (assertRanges) {
                expect.that(makeRange)
                        .asList()
                        .containsExactly(
                                expectedAttributes.makeOffset, expectedAttributes.makeLength)
                        .inOrder();
            }
            expect.that(make).isEqualTo(expectedAttributes.make);
        } else {
            expect.that(make).isNull();
            expect.that(makeRange).isNull();
            expect.that(exifInterface.hasAttribute(ExifInterface.TAG_MAKE)).isFalse();
        }

        // Checks values.
        expectStringTag(exifInterface, ExifInterface.TAG_MAKE, expectedAttributes.make);
        expectStringTag(exifInterface, ExifInterface.TAG_MODEL, expectedAttributes.model);
        expect.that(exifInterface.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0))
                .isWithin(DIFFERENCE_TOLERANCE)
                .of(expectedAttributes.aperture);
        expectStringTag(
                exifInterface,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                expectedAttributes.dateTimeOriginal);
        expect.that(exifInterface.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0))
                .isWithin(DIFFERENCE_TOLERANCE)
                .of(expectedAttributes.exposureTime);
        expectStringTag(
                exifInterface, ExifInterface.TAG_FOCAL_LENGTH, expectedAttributes.focalLength);
        expectStringTag(
                exifInterface, ExifInterface.TAG_GPS_ALTITUDE, expectedAttributes.gpsAltitude);
        expectStringTag(
                exifInterface,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                expectedAttributes.gpsAltitudeRef);
        expectStringTag(
                exifInterface, ExifInterface.TAG_GPS_DATESTAMP, expectedAttributes.gpsDatestamp);
        expectStringTag(
                exifInterface, ExifInterface.TAG_GPS_LATITUDE, expectedAttributes.gpsLatitude);
        expectStringTag(
                exifInterface,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                expectedAttributes.gpsLatitudeRef);
        expectStringTag(
                exifInterface, ExifInterface.TAG_GPS_LONGITUDE, expectedAttributes.gpsLongitude);
        expectStringTag(
                exifInterface,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                expectedAttributes.gpsLongitudeRef);
        expectStringTag(
                exifInterface,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                expectedAttributes.gpsProcessingMethod);
        expectStringTag(
                exifInterface, ExifInterface.TAG_GPS_TIMESTAMP, expectedAttributes.gpsTimestamp);
        expect.that(exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0))
                .isEqualTo(expectedAttributes.imageLength);
        expect.that(exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0))
                .isEqualTo(expectedAttributes.imageWidth);
        expectStringTag(
                exifInterface, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, expectedAttributes.iso);
        expect.that(exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0))
                .isEqualTo(expectedAttributes.orientation);

        long[] xmpRange = exifInterface.getAttributeRange(ExifInterface.TAG_XMP);
        if (expectedAttributes.hasXmp) {
            expect.that(xmpRange).isNotNull();
            if (assertRanges) {
                expect.that(xmpRange)
                        .asList()
                        .containsExactly(expectedAttributes.xmpOffset, expectedAttributes.xmpLength)
                        .inOrder();
            }
            final String xmp =
                    new String(
                            exifInterface.getAttributeBytes(ExifInterface.TAG_XMP),
                            Charset.forName("UTF-8"));
            // We're only interested in confirming that we were able to extract
            // valid XMP data, which must always include this XML tag; a full
            // XMP parser is beyond the scope of ExifInterface. See XMP
            // Specification Part 1, Section C.2.2 for additional details.
            expect.that(xmp).contains("<rdf:RDF");
        } else {
            expect.that(xmpRange).isNull();
        }
    }

    private void readFromStandaloneDataWithExif(
            File imageFile, ExpectedAttributes expectedAttributes) throws IOException {
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
        compareWithExpectedAttributes(exifInterface, expectedAttributes, verboseTag, true);
    }

    private void testExifInterfaceCommon(File imageFile, ExpectedAttributes expectedAttributes)
            throws IOException {
        String verboseTag = imageFile.getName();

        // Creates via file.
        ExifInterface exifInterface = new ExifInterface(imageFile);
        compareWithExpectedAttributes(exifInterface, expectedAttributes, verboseTag, true);

        // Creates via path.
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedAttributes(exifInterface, expectedAttributes, verboseTag, true);

        // Creates via InputStream.
        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            exifInterface = new ExifInterface(in);
            compareWithExpectedAttributes(exifInterface, expectedAttributes, verboseTag, true);
        }

        // Creates via FileDescriptor.
        if (Build.VERSION.SDK_INT >= 21) {
            FileDescriptor fd = null;
            try {
                fd = Os.open(imageFile.getAbsolutePath(), OsConstants.O_RDONLY,
                        OsConstants.S_IRWXU);
                exifInterface = new ExifInterface(fd);
                compareWithExpectedAttributes(exifInterface, expectedAttributes, verboseTag, true);
            } catch (Exception e) {
                throw new IOException("Failed to open file descriptor", e);
            } finally {
                closeQuietly(fd);
            }
        }
    }

    private void testExifInterfaceRange(File imageFile, ExpectedAttributes expectedAttributes)
            throws IOException {
        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            if (expectedAttributes.hasThumbnail) {
                ByteStreams.skipFully(in, expectedAttributes.thumbnailOffset);
                byte[] thumbnailBytes =
                        new byte[Ints.checkedCast(expectedAttributes.thumbnailLength)];
                ByteStreams.readFully(in, thumbnailBytes);
                // TODO: Need a way to check uncompressed thumbnail file
                Bitmap thumbnailBitmap =
                        BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length);
                expect.that(thumbnailBitmap.getWidth())
                        .isEqualTo(expectedAttributes.thumbnailWidth);
                expect.that(thumbnailBitmap.getHeight())
                        .isEqualTo(expectedAttributes.thumbnailHeight);
            }
        }

        // TODO: Creating a new input stream is a temporary
        //  workaround for BufferedInputStream#mark/reset not working properly for
        //  LG_G4_ISO_800_DNG. Need to investigate cause.
        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            if (expectedAttributes.hasMake) {
                ByteStreams.skipFully(in, expectedAttributes.makeOffset);
                byte[] makeBytes = new byte[Ints.checkedCast(expectedAttributes.makeLength)];
                ByteStreams.readFully(in, makeBytes);
                String makeString = new String(makeBytes);
                // Remove null bytes
                makeString = makeString.replaceAll("\u0000.*", "");
                expect.that(makeString).isEqualTo(expectedAttributes.make);
            }
        }

        try (InputStream in =
                new BufferedInputStream(new FileInputStream(imageFile.getAbsolutePath()))) {
            if (expectedAttributes.hasXmp) {
                ByteStreams.skipFully(in, expectedAttributes.xmpOffset);
                byte[] identifierBytes = new byte[Ints.checkedCast(expectedAttributes.xmpLength)];
                ByteStreams.readFully(in, identifierBytes);
                final String xmpIdentifier = "<?xpacket begin=";
                expect.that(new String(identifierBytes, Charset.forName("UTF-8")))
                        .startsWith(xmpIdentifier);
            }
            // TODO: Add code for retrieving raw latitude data using offset and length
        }
    }

    private void writeToFilesWithExif(File srcFile, ExpectedAttributes expectedAttributes)
            throws IOException {
        File imageFile = clone(srcFile);
        String verboseTag = imageFile.getName();

        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedAttributes(exifInterface, expectedAttributes, verboseTag, false);
        expectBitmapsEquivalent(srcFile, imageFile);
        expectSecondSaveProducesSameSizeFile(imageFile);

        // Test for modifying one attribute.
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        String backupValue = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
        exifInterface.saveAttributes();
        // Check if thumbnail offset and length are properly updated without parsing the data again.
        if (expectedAttributes.hasThumbnail) {
            testThumbnail(expectedAttributes, exifInterface);
        }
        expect.that(exifInterface.getAttribute(ExifInterface.TAG_MAKE)).isEqualTo("abc");
        // Check if thumbnail bytes can be retrieved from the new thumbnail range.
        if (expectedAttributes.hasThumbnail) {
            testThumbnail(expectedAttributes, exifInterface);
        }

        // Restore the backup value.
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, backupValue);
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        compareWithExpectedAttributes(exifInterface, expectedAttributes, verboseTag, false);

        // Creates via FileDescriptor.
        if (Build.VERSION.SDK_INT >= 21) {
            FileDescriptor fd = null;
            try {
                fd = Os.open(imageFile.getAbsolutePath(), OsConstants.O_RDWR,
                        OsConstants.S_IRWXU);
                exifInterface = new ExifInterface(fd);
                exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
                exifInterface.saveAttributes();
                expect.that(exifInterface.getAttribute(ExifInterface.TAG_MAKE)).isEqualTo("abc");
            } catch (Exception e) {
                throw new IOException("Failed to open file descriptor", e);
            } finally {
                closeQuietly(fd);
            }
        }
    }

    private void readFromFilesWithExif(File imageFile, ExpectedAttributes expectedAttributes)
            throws IOException {

        // Test for reading from external data storage.
        testExifInterfaceCommon(imageFile, expectedAttributes);

        // Test for checking expected range by retrieving raw data with given offset and length.
        testExifInterfaceRange(imageFile, expectedAttributes);
    }

    private void writeToFilesWithoutExif(File srcFile) throws IOException {
        File imageFile = clone(srcFile);

        ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "abc");
        exifInterface.saveAttributes();

        expectBitmapsEquivalent(srcFile, imageFile);
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        String make = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
        expect.that(make).isEqualTo("abc");

        expectSecondSaveProducesSameSizeFile(imageFile);
    }

    private void testThumbnail(ExpectedAttributes expectedAttributes, ExifInterface exifInterface) {
        byte[] thumbnail = exifInterface.getThumbnail();
        Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
        expect.that(thumbnailBitmap.getWidth()).isEqualTo(expectedAttributes.thumbnailWidth);
        expect.that(thumbnailBitmap.getHeight()).isEqualTo(expectedAttributes.thumbnailHeight);
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
     * Asserts (using {@link #expect}) that {@code expectedImageFile} and {@code actualImageFile}
     * can be decoded by {@link BitmapFactory} and the results have the same width, height and MIME
     * type.
     *
     * <p>The assertion is skipped if the test is running on an API level where {@link
     * BitmapFactory} is known not to support the image format of {@code expectedImageFile} (as
     * determined by file extension).
     *
     * <p>This does not check the image itself for similarity/equality.
     */
    private void expectBitmapsEquivalent(File expectedImageFile, File actualImageFile) {
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

        expect.that(actualOptions.outWidth).isEqualTo(expectedOptions.outWidth);
        expect.that(actualOptions.outHeight).isEqualTo(expectedOptions.outHeight);
        expect.that(actualOptions.outMimeType).isEqualTo(expectedOptions.outMimeType);
        expect.that(actualBitmap.getWidth()).isEqualTo(expectedBitmap.getWidth());
        expect.that(actualBitmap.getHeight()).isEqualTo(expectedBitmap.getHeight());
        expect.that(actualBitmap.hasAlpha()).isEqualTo(expectedBitmap.hasAlpha());
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
     * Asserts (using {@link #expect}) that saving the file the second time (without modifying any
     * attributes) produces exactly the same length file as the first save. The first save (with no
     * modifications) is expected to (possibly) change the file length because {@link ExifInterface}
     * may move/reformat the Exif block within the file, but the second save should not make further
     * modifications.
     */
    private void expectSecondSaveProducesSameSizeFile(File imageFileAfterOneSave)
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
            expect.that(imageFileAfterThreeSaves.length())
                    .isEqualTo(imageFileAfterTwoSaves.length());
        } else {
            expect.that(imageFileAfterTwoSaves.length()).isEqualTo(imageFileAfterOneSave.length());
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

    /**
     * An operation that can be applied to an {@link ExifInterface} instance.
     *
     * <p>We would use java.util.Consumer but it's not available before API 24, and there's no Guava
     * equivalent.
     */
    private interface ExifInterfaceOperation {
        void applyTo(ExifInterface exifInterface);
    }
}
