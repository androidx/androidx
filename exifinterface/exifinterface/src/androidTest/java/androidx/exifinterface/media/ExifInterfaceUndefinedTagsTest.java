/*
 * Copyright 2026 The Android Open Source Project
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

import static androidx.exifinterface.media.ExifInterfaceTestUtil.copyFromResourceToFile;
import static androidx.exifinterface.media.ExifInterfaceTestUtil.countOccurrences;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import androidx.exifinterface.test.R;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.io.Files;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Tests unsigned byte {@code IFD_FORMAT_UNDEFINED} tag handling in {@link ExifInterface}. */
// TODO(b/455513752): Remove @SdkSuppress once a fix for
// https://github.com/google/TestParameterInjector/issues/67 is released.
@SdkSuppress(minSdkVersion = 26)
@RunWith(TestParameterInjector.class)
public class ExifInterfaceUndefinedTagsTest {
    private static final int TAG_COMPONENTS_CONFIGURATION_NUMBER = 0x9101;
    private static final int TAG_FILE_SOURCE_NUMBER = 0xA300;
    private static final int TAG_SCENE_TYPE_NUMBER = 0xA301;
    private static final int TAG_EXIF_IFD_POINTER_NUMBER = 0x8769;

    private static final int IFD_FORMAT_BYTE = 1;
    private static final int IFD_FORMAT_STRING = 2;
    private static final int IFD_FORMAT_ULONG = 4;
    private static final int IFD_FORMAT_UNDEFINED = 7;

    @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

    enum TestByteOrder {
        LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN),
        BIG_ENDIAN(ByteOrder.BIG_ENDIAN);

        final ByteOrder byteOrder;

        TestByteOrder(ByteOrder byteOrder) {
            this.byteOrder = byteOrder;
        }
    }

    enum UndefinedOrByteFormat {
        UNDEFINED(IFD_FORMAT_UNDEFINED),
        BYTE(IFD_FORMAT_BYTE);

        final int format;

        UndefinedOrByteFormat(int format) {
            this.format = format;
        }
    }

    @Test
    @SmallTest
    public void readJpegAsset_decodesUndefinedNumericTagsAndRanges() throws Exception {
        File imageFile = tempFolder.newFile("jpeg_read_undefined.jpg");
        copyFromResourceToFile(R.raw.jpeg_with_exif_byte_order_ii, imageFile);
        ExifInterface exifInterface = new ExifInterface(imageFile);

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isEqualTo("1");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_SCENE_TYPE, -1)).isEqualTo(1);
        assertThat(exifInterface.getAttributeDouble(ExifInterface.TAG_SCENE_TYPE, -1.0))
                .isEqualTo(1.0);
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_SCENE_TYPE))
                .isEqualTo(new byte[] {1});
        long[] sceneTypeRange = exifInterface.getAttributeRange(ExifInterface.TAG_SCENE_TYPE);
        assertThat(sceneTypeRange).isNotNull();
        assertThat(sceneTypeRange[0]).isGreaterThan(0L);
        assertThat(sceneTypeRange[1]).isEqualTo(1L);

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("1,2,3,0");
        assertThat(
                        exifInterface.getAttributeInt(
                                ExifInterface.TAG_COMPONENTS_CONFIGURATION, -1))
                .isEqualTo(-1);
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo(new byte[] {1, 2, 3, 0});
        long[] componentsRange =
                exifInterface.getAttributeRange(ExifInterface.TAG_COMPONENTS_CONFIGURATION);
        assertThat(componentsRange).isNotNull();
        assertThat(componentsRange[0]).isGreaterThan(0L);
        assertThat(componentsRange[1]).isEqualTo(4L);
    }

    @Test
    @SmallTest
    public void readWebpAsset_decodesUndefinedNumericTagsAndRanges() throws Exception {
        File imageFile = tempFolder.newFile("webp_read_undefined.webp");
        copyFromResourceToFile(R.raw.webp_with_exif, imageFile);
        ExifInterface exifInterface = new ExifInterface(imageFile);

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isEqualTo("1");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_SCENE_TYPE, -1)).isEqualTo(1);
        long[] sceneTypeRange = exifInterface.getAttributeRange(ExifInterface.TAG_SCENE_TYPE);
        assertThat(sceneTypeRange).isNotNull();
        assertThat(sceneTypeRange[0]).isGreaterThan(0L);
        assertThat(sceneTypeRange[1]).isEqualTo(1L);

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("1,2,3,0");
        long[] componentsRange =
                exifInterface.getAttributeRange(ExifInterface.TAG_COMPONENTS_CONFIGURATION);
        assertThat(componentsRange).isNotNull();
        assertThat(componentsRange[0]).isGreaterThan(0L);
        assertThat(componentsRange[1]).isEqualTo(4L);
    }

    @Test
    @SmallTest
    public void readMultiByteFileSource_decodesAsCommaSeparatedString() throws Exception {
        ExifInterface exifInterface =
                new ExifInterface(
                        createJpegWithSingleExifTag(
                                ByteOrder.LITTLE_ENDIAN,
                                TAG_FILE_SOURCE_NUMBER,
                                IFD_FORMAT_UNDEFINED,
                                new byte[] {3, 0, 0, 0}));

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_FILE_SOURCE)).isEqualTo("3,0,0,0");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_FILE_SOURCE, -1)).isEqualTo(-1);
        assertThat(exifInterface.getAttributeDouble(ExifInterface.TAG_FILE_SOURCE, -1.0))
                .isEqualTo(-1.0);
    }

    @Test
    @SmallTest
    public void readEmptySceneType_returnsEmptyStringAndDefaultValues() throws Exception {
        ExifInterface exifInterface =
                new ExifInterface(
                        createJpegWithSingleExifTag(
                                ByteOrder.LITTLE_ENDIAN,
                                TAG_SCENE_TYPE_NUMBER,
                                IFD_FORMAT_UNDEFINED,
                                new byte[0]));

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isEmpty();
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_SCENE_TYPE, -1)).isEqualTo(-1);
        assertThat(exifInterface.getAttributeDouble(ExifInterface.TAG_SCENE_TYPE, -1.0))
                .isEqualTo(-1.0);
    }

    @Test
    @SmallTest
    public void readNonNumericStringFileSource_preservesStringAndRange() throws Exception {
        byte[] valueBytes = new byte[] {'D', 'S', 'C', 0};
        ExifInterface exifInterface =
                new ExifInterface(
                        createJpegWithSingleExifTag(
                                ByteOrder.LITTLE_ENDIAN,
                                TAG_FILE_SOURCE_NUMBER,
                                IFD_FORMAT_STRING,
                                valueBytes));

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_FILE_SOURCE)).isEqualTo("DSC");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_FILE_SOURCE, -1)).isEqualTo(-1);
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_FILE_SOURCE))
                .isEqualTo(valueBytes);
        long[] range = exifInterface.getAttributeRange(ExifInterface.TAG_FILE_SOURCE);
        assertThat(range).isNotNull();
        assertThat(range[0]).isGreaterThan(0L);
        assertThat(range[1]).isEqualTo(4L);
    }

    @Test
    @SmallTest
    public void readNumericStringFileSource_preservesStringAndRange() throws Exception {
        byte[] valueBytes = new byte[] {'1', 0};
        ExifInterface exifInterface =
                new ExifInterface(
                        createJpegWithSingleExifTag(
                                ByteOrder.LITTLE_ENDIAN,
                                TAG_FILE_SOURCE_NUMBER,
                                IFD_FORMAT_STRING,
                                valueBytes));

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_FILE_SOURCE)).isEqualTo("1");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_FILE_SOURCE, -1)).isEqualTo(1);
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_FILE_SOURCE))
                .isEqualTo(valueBytes);
        long[] range = exifInterface.getAttributeRange(ExifInterface.TAG_FILE_SOURCE);
        assertThat(range).isNotNull();
        assertThat(range[0]).isGreaterThan(0L);
        assertThat(range[1]).isEqualTo(2L);
    }

    @Test
    @SmallTest
    public void setAndSaveUndefinedTags_littleEndian() throws Exception {
        File imageFile = tempFolder.newFile("jpeg_set_undefined_ii.jpg");
        copyFromResourceToFile(R.raw.jpeg_with_exif_byte_order_ii, imageFile);
        ExifInterface exifInterface = new ExifInterface(imageFile);

        long[] parsedSceneTypeRange =
                exifInterface.getAttributeRange(ExifInterface.TAG_SCENE_TYPE);
        assertThat(parsedSceneTypeRange).isNotNull();
        assertThat(parsedSceneTypeRange[0]).isGreaterThan(0L);
        assertThat(parsedSceneTypeRange[1]).isEqualTo(1L);
        long[] parsedComponentsRange =
                exifInterface.getAttributeRange(ExifInterface.TAG_COMPONENTS_CONFIGURATION);
        assertThat(parsedComponentsRange).isNotNull();
        assertThat(parsedComponentsRange[0]).isGreaterThan(0L);
        assertThat(parsedComponentsRange[1]).isEqualTo(4L);

        exifInterface.setAttribute(ExifInterface.TAG_SCENE_TYPE, "1");
        exifInterface.setAttribute(ExifInterface.TAG_FILE_SOURCE, "3");
        exifInterface.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, "4,5,6,0");

        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_SCENE_TYPE))
                .isEqualTo(new long[] {-1L, 1L});
        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_FILE_SOURCE))
                .isEqualTo(new long[] {-1L, 1L});
        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo(new long[] {-1L, 4L});

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isEqualTo("1");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_SCENE_TYPE, -1)).isEqualTo(1);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_FILE_SOURCE)).isEqualTo("3");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_FILE_SOURCE, -1)).isEqualTo(3);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("4,5,6,0");
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo(new byte[] {4, 5, 6, 0});

        exifInterface.saveAttributes();
        byte[] imageBytes = Files.toByteArray(imageFile);
        // Little-endian IFD entries: tag(2), format=7(2), count(4), inline bytes(4).
        byte[] expectedSceneType =
                new byte[] {0x01, (byte) 0xA3, 0x07, 0x00, 0x01, 0x00, 0x00, 0x00, 1, 0, 0, 0};
        byte[] expectedFileSource =
                new byte[] {0x00, (byte) 0xA3, 0x07, 0x00, 0x01, 0x00, 0x00, 0x00, 3, 0, 0, 0};
        byte[] expectedComponents =
                new byte[] {0x01, (byte) 0x91, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 4, 5, 6, 0};
        assertThat(countOccurrences(imageBytes, expectedSceneType)).isEqualTo(1);
        assertThat(countOccurrences(imageBytes, expectedFileSource)).isEqualTo(1);
        assertThat(countOccurrences(imageBytes, expectedComponents)).isEqualTo(1);

        exifInterface = new ExifInterface(imageFile);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isEqualTo("1");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_FILE_SOURCE, -1)).isEqualTo(3);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("4,5,6,0");
        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_FILE_SOURCE)[0])
                .isGreaterThan(0L);
    }

    @Test
    @SmallTest
    public void setAndSaveUndefinedTags_bigEndian() throws Exception {
        File imageFile = tempFolder.newFile("jpeg_set_undefined_mm.jpg");
        copyFromResourceToFile(R.raw.jpeg_with_exif_byte_order_mm, imageFile);
        ExifInterface exifInterface = new ExifInterface(imageFile);
        exifInterface.setAttribute(ExifInterface.TAG_SCENE_TYPE, "1");
        exifInterface.setAttribute(ExifInterface.TAG_FILE_SOURCE, "255");
        exifInterface.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, "4,5,6,0");

        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_SCENE_TYPE))
                .isEqualTo(new long[] {-1L, 1L});
        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_FILE_SOURCE))
                .isEqualTo(new long[] {-1L, 1L});
        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo(new long[] {-1L, 4L});

        exifInterface.saveAttributes();
        byte[] imageBytes = Files.toByteArray(imageFile);
        // Big-endian IFD entries: tag(2), format=7(2), count(4), inline bytes(4).
        byte[] expectedSceneType =
                new byte[] {(byte) 0xA3, 0x01, 0x00, 0x07, 0x00, 0x00, 0x00, 0x01, 1, 0, 0, 0};
        byte[] expectedFileSource =
                new byte[] {
                    (byte) 0xA3, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, 0x01, (byte) 255, 0, 0, 0
                };
        byte[] expectedComponents =
                new byte[] {(byte) 0x91, 0x01, 0x00, 0x07, 0x00, 0x00, 0x00, 0x04, 4, 5, 6, 0};
        assertThat(countOccurrences(imageBytes, expectedSceneType)).isEqualTo(1);
        assertThat(countOccurrences(imageBytes, expectedFileSource)).isEqualTo(1);
        assertThat(countOccurrences(imageBytes, expectedComponents)).isEqualTo(1);

        exifInterface = new ExifInterface(imageFile);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isEqualTo("1");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_FILE_SOURCE, -1)).isEqualTo(255);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("4,5,6,0");
    }

    @Test
    @SmallTest
    public void setAndSaveUndefinedTags_zeroAndClear() throws Exception {
        File imageFile = tempFolder.newFile("jpeg_clear_undefined.jpg");
        copyFromResourceToFile(R.raw.jpeg_with_exif_byte_order_mm, imageFile);
        ExifInterface exifInterface = new ExifInterface(imageFile);
        exifInterface.setAttribute(ExifInterface.TAG_FILE_SOURCE, "0");
        exifInterface.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, "1,2,3,0");
        exifInterface.saveAttributes();

        exifInterface = new ExifInterface(imageFile);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_FILE_SOURCE)).isEqualTo("0");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_FILE_SOURCE, -1)).isEqualTo(0);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("1,2,3,0");
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo(new byte[] {1, 2, 3, 0});

        exifInterface.setAttribute(ExifInterface.TAG_SCENE_TYPE, null);
        exifInterface.setAttribute(ExifInterface.TAG_FILE_SOURCE, null);
        exifInterface.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, null);
        exifInterface.saveAttributes();

        exifInterface = new ExifInterface(imageFile);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isNull();
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_FILE_SOURCE)).isNull();
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isNull();
        assertThat(exifInterface.getAttributeRange(ExifInterface.TAG_FILE_SOURCE)).isNull();
    }

    @Test
    @SmallTest
    public void undefinedAsciiTags_preservedAsAscii() throws Exception {
        File imageFile = tempFolder.newFile("jpeg_undefined_ascii.jpg");
        copyFromResourceToFile(R.raw.jpeg_with_exif_byte_order_ii, imageFile);
        ExifInterface exifInterface = new ExifInterface(imageFile);

        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, "1");
        exifInterface.setAttribute(ExifInterface.TAG_EXIF_VERSION, "0232");
        exifInterface.saveAttributes();

        exifInterface = new ExifInterface(imageFile);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)).isEqualTo("1");
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_USER_COMMENT))
                .isEqualTo(new byte[] {'1'});
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_EXIF_VERSION)).isEqualTo("0232");

        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, "A");
        exifInterface.saveAttributes();
        exifInterface = new ExifInterface(imageFile);
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)).isEqualTo("A");
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_USER_COMMENT))
                .isEqualTo(new byte[] {'A'});
    }

    enum SingleByteTagCase {
        FILE_SOURCE_OTHER(ExifInterface.TAG_FILE_SOURCE, TAG_FILE_SOURCE_NUMBER, 0),
        FILE_SOURCE_DSC(ExifInterface.TAG_FILE_SOURCE, TAG_FILE_SOURCE_NUMBER, 3),
        FILE_SOURCE_HIGH_BYTE(ExifInterface.TAG_FILE_SOURCE, TAG_FILE_SOURCE_NUMBER, 255),
        SCENE_TYPE_DIRECTLY_PHOTOGRAPHED(
                ExifInterface.TAG_SCENE_TYPE, TAG_SCENE_TYPE_NUMBER, 1),
        SCENE_TYPE_HIGH_BYTE(ExifInterface.TAG_SCENE_TYPE, TAG_SCENE_TYPE_NUMBER, 255);

        final String tagName;
        final int tagNumber;
        final int expectedValue;

        SingleByteTagCase(String tagName, int tagNumber, int expectedValue) {
            this.tagName = tagName;
            this.tagNumber = tagNumber;
            this.expectedValue = expectedValue;
        }
    }

    @Test
    @SmallTest
    public void readSingleByteTag_decodesUnsignedByteAndPreservesRange(
            @TestParameter SingleByteTagCase tagCase,
            @TestParameter UndefinedOrByteFormat format,
            @TestParameter TestByteOrder byteOrder)
            throws Exception {
        byte[] valueBytes = new byte[] {(byte) tagCase.expectedValue};
        ExifInterface exifInterface =
                new ExifInterface(
                        createJpegWithSingleExifTag(
                                byteOrder.byteOrder, tagCase.tagNumber, format.format, valueBytes));

        assertThat(exifInterface.getAttribute(tagCase.tagName))
                .isEqualTo(String.valueOf(tagCase.expectedValue));
        assertThat(exifInterface.getAttributeInt(tagCase.tagName, -1))
                .isEqualTo(tagCase.expectedValue);
        assertThat(exifInterface.getAttributeDouble(tagCase.tagName, -1.0))
                .isEqualTo((double) tagCase.expectedValue);
        assertThat(exifInterface.getAttributeBytes(tagCase.tagName)).isEqualTo(valueBytes);
        long[] range = exifInterface.getAttributeRange(tagCase.tagName);
        assertThat(range).isNotNull();
        assertThat(range[0]).isGreaterThan(0L);
        assertThat(range[1]).isEqualTo(1L);
    }

    @Test
    @SmallTest
    public void readComponentsConfiguration_decodesCommaSeparatedBytesAndPreservesRange(
            @TestParameter UndefinedOrByteFormat format, @TestParameter TestByteOrder byteOrder)
            throws Exception {
        byte[] valueBytes = new byte[] {4, 5, 6, 0};
        ExifInterface exifInterface =
                new ExifInterface(
                        createJpegWithSingleExifTag(
                                byteOrder.byteOrder,
                                TAG_COMPONENTS_CONFIGURATION_NUMBER,
                                format.format,
                                valueBytes));

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("4,5,6,0");
        assertThat(exifInterface.getAttributeInt(ExifInterface.TAG_COMPONENTS_CONFIGURATION, -1))
                .isEqualTo(-1);
        assertThat(exifInterface.getAttributeBytes(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo(valueBytes);
        long[] range = exifInterface.getAttributeRange(ExifInterface.TAG_COMPONENTS_CONFIGURATION);
        assertThat(range).isNotNull();
        assertThat(range[0]).isGreaterThan(0L);
        assertThat(range[1]).isEqualTo(4L);
    }

    enum InvalidSingleByteCase {
        NON_NUMERIC("abc"),
        NEGATIVE("-1"),
        OVERFLOW("256"),
        EMPTY("");

        final String value;

        InvalidSingleByteCase(String value) {
            this.value = value;
        }
    }

    @Test
    @SmallTest
    public void setAttribute_invalidSingleByteValueIgnored(
            @TestParameter InvalidSingleByteCase invalidCase) throws Exception {
        ExifInterface exifInterface;
        try (InputStream inputStream =
                getApplicationContext()
                        .getResources()
                        .openRawResource(R.raw.jpeg_with_exif_byte_order_ii)) {
            exifInterface = new ExifInterface(inputStream);
        }
        exifInterface.setAttribute(ExifInterface.TAG_SCENE_TYPE, "1");
        exifInterface.setAttribute(ExifInterface.TAG_FILE_SOURCE, "3");

        exifInterface.setAttribute(ExifInterface.TAG_SCENE_TYPE, invalidCase.value);
        exifInterface.setAttribute(ExifInterface.TAG_FILE_SOURCE, invalidCase.value);

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_SCENE_TYPE)).isEqualTo("1");
        assertThat(exifInterface.getAttribute(ExifInterface.TAG_FILE_SOURCE)).isEqualTo("3");
    }

    enum InvalidComponentsConfigurationCase {
        QUESTION_MARKS("???"),
        NON_NUMERIC("abc"),
        NEGATIVE_ELEMENT("1,-1,3,0"),
        SPACE_SEPARATED("4 5 6 0"),
        ELEMENT_OVERFLOW("1,2,999,0"),
        EMPTY(""),
        TOO_SHORT_1("1"),
        TOO_SHORT_3("1,2,3"),
        TOO_LONG_5("1,2,3,0,0"),
        RAW_CONTROL_BYTES("\u0001\u0002\u0003\u0000");

        final String value;

        InvalidComponentsConfigurationCase(String value) {
            this.value = value;
        }
    }

    @Test
    @SmallTest
    public void setAttribute_invalidComponentsConfigurationIgnored(
            @TestParameter InvalidComponentsConfigurationCase invalidCase) throws Exception {
        ExifInterface exifInterface;
        try (InputStream inputStream =
                getApplicationContext()
                        .getResources()
                        .openRawResource(R.raw.jpeg_with_exif_byte_order_ii)) {
            exifInterface = new ExifInterface(inputStream);
        }
        exifInterface.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, "4,5,6,0");

        exifInterface.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, invalidCase.value);

        assertThat(exifInterface.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION))
                .isEqualTo("4,5,6,0");
    }

    private static InputStream createJpegWithSingleExifTag(
            ByteOrder byteOrder, int tagNumber, int format, byte[] valueBytes) {
        return new ByteArrayInputStream(
                buildJpegWithSingleExifTag(byteOrder, tagNumber, format, valueBytes));
    }

    private static byte[] buildJpegWithSingleExifTag(
            ByteOrder byteOrder, int tagNumber, int format, byte[] valueBytes) {
        if (valueBytes.length > 4) {
            throw new IllegalArgumentException(
                    "Inline IFD value must be at most 4 bytes, got " + valueBytes.length);
        }
        int tiffSize = 44;
        int app1Length = 2 + ExifInterface.IDENTIFIER_EXIF_APP1.length + tiffSize;
        ByteBuffer buffer =
                ByteBuffer.allocate(ExifInterface.JPEG_SIGNATURE.length + 1 + app1Length + 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(ExifInterface.JPEG_SIGNATURE);
        buffer.put(ExifInterface.MARKER_APP1);
        buffer.putShort((short) app1Length);
        buffer.put(ExifInterface.IDENTIFIER_EXIF_APP1);

        // TIFF Header
        byte alignByte = byteOrder == ByteOrder.LITTLE_ENDIAN ? (byte) 'I' : (byte) 'M';
        buffer.put(alignByte);
        buffer.put(alignByte);
        buffer.order(byteOrder);
        buffer.putShort(ExifInterface.START_CODE);
        buffer.putInt(8);

        // IFD0: 1 entry pointing to Exif SubIFD at TIFF offset 26
        buffer.putShort((short) 1);
        buffer.putShort((short) TAG_EXIF_IFD_POINTER_NUMBER);
        buffer.putShort((short) IFD_FORMAT_ULONG);
        buffer.putInt(1);
        buffer.putInt(26);
        buffer.putInt(0);

        // Exif SubIFD at TIFF offset 26: 1 entry for the target 1-byte-per-component tag
        buffer.putShort((short) 1);
        buffer.putShort((short) tagNumber);
        buffer.putShort((short) format);
        buffer.putInt(valueBytes.length);
        buffer.put(valueBytes);
        for (int i = valueBytes.length; i < 4; i++) {
            buffer.put((byte) 0);
        }
        buffer.putInt(0);

        // JPEG EOI
        buffer.put((byte) 0xFF);
        buffer.put((byte) 0xD9);
        return buffer.array();
    }
}
