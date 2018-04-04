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

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a class for reading and writing Exif tags in a JPEG file or a RAW image file.
 * <p>
 * Supported formats are: JPEG, DNG, CR2, NEF, NRW, ARW, RW2, ORF, PEF, SRW and RAF.
 * <p>
 * Attribute mutation is supported for JPEG image files.
 */
public class ExifInterface {
    private static final String TAG = "ExifInterface";
    private static final boolean DEBUG = false;

    // The Exif tag names. See JEITA CP-3451C specifications (Exif 2.3) Section 3-8.
    // A. Tags related to image data structure
    /**
     *  <p>The number of columns of image data, equal to the number of pixels per row. In JPEG
     *  compressed data, this tag shall not be used because a JPEG marker is used instead of it.</p>
     *
     *  <ul>
     *      <li>Tag = 256</li>
     *      <li>Type = Unsigned short or Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_IMAGE_WIDTH = "ImageWidth";
    /**
     *  <p>The number of rows of image data. In JPEG compressed data, this tag shall not be used
     *  because a JPEG marker is used instead of it.</p>
     *
     *  <ul>
     *      <li>Tag = 257</li>
     *      <li>Type = Unsigned short or Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_IMAGE_LENGTH = "ImageLength";
    /**
     *  <p>The number of bits per image component. In this standard each component of the image is
     *  8 bits, so the value for this tag is 8. See also {@link #TAG_SAMPLES_PER_PIXEL}. In JPEG
     *  compressed data, this tag shall not be used because a JPEG marker is used instead of it.</p>
     *
     *  <ul>
     *      <li>Tag = 258</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 3</li>
     *      <li>Default = {@link #BITS_PER_SAMPLE_RGB}</li>
     *  </ul>
     */
    public static final String TAG_BITS_PER_SAMPLE = "BitsPerSample";
    /**
     *  <p>The compression scheme used for the image data. When a primary image is JPEG compressed,
     *  this designation is not necessary. So, this tag shall not be recorded. When thumbnails use
     *  JPEG compression, this tag value is set to 6.</p>
     *
     *  <ul>
     *      <li>Tag = 259</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #DATA_UNCOMPRESSED
     *  @see #DATA_JPEG
     */
    public static final String TAG_COMPRESSION = "Compression";
    /**
     *  <p>The pixel composition. In JPEG compressed data, this tag shall not be used because a JPEG
     *  marker is used instead of it.</p>
     *
     *  <ul>
     *      <li>Tag = 262</li>
     *      <li>Type = SHORT</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #PHOTOMETRIC_INTERPRETATION_RGB
     *  @see #PHOTOMETRIC_INTERPRETATION_YCBCR
     */
    public static final String TAG_PHOTOMETRIC_INTERPRETATION = "PhotometricInterpretation";
    /**
     *  <p>The image orientation viewed in terms of rows and columns.</p>
     *
     *  <ul>
     *      <li>Tag = 274</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #ORIENTATION_NORMAL}</li>
     *  </ul>
     *
     *  @see #ORIENTATION_UNDEFINED
     *  @see #ORIENTATION_NORMAL
     *  @see #ORIENTATION_FLIP_HORIZONTAL
     *  @see #ORIENTATION_ROTATE_180
     *  @see #ORIENTATION_FLIP_VERTICAL
     *  @see #ORIENTATION_TRANSPOSE
     *  @see #ORIENTATION_ROTATE_90
     *  @see #ORIENTATION_TRANSVERSE
     *  @see #ORIENTATION_ROTATE_270
     */
    public static final String TAG_ORIENTATION = "Orientation";
    /**
     *  <p>The number of components per pixel. Since this standard applies to RGB and YCbCr images,
     *  the value set for this tag is 3. In JPEG compressed data, this tag shall not be used because
     *  a JPEG marker is used instead of it.</p>
     *
     *  <ul>
     *      <li>Tag = 277</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = 3</li>
     *  </ul>
     */
    public static final String TAG_SAMPLES_PER_PIXEL = "SamplesPerPixel";
    /**
     *  <p>Indicates whether pixel components are recorded in chunky or planar format. In JPEG
     *  compressed data, this tag shall not be used because a JPEG marker is used instead of it.
     *  If this field does not exist, the TIFF default, {@link #FORMAT_CHUNKY}, is assumed.</p>
     *
     *  <ul>
     *      <li>Tag = 284</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *  </ul>
     *
     *  @see #FORMAT_CHUNKY
     *  @see #FORMAT_PLANAR
     */
    public static final String TAG_PLANAR_CONFIGURATION = "PlanarConfiguration";
    /**
     *  <p>The sampling ratio of chrominance components in relation to the luminance component.
     *  In JPEG compressed data a JPEG marker is used instead of this tag. So, this tag shall not
     *  be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 530</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 2</li>
     *      <ul>
     *          <li>[2, 1] = YCbCr4:2:2</li>
     *          <li>[2, 2] = YCbCr4:2:0</li>
     *          <li>Other = reserved</li>
     *      </ul>
     *  </ul>
     */
    public static final String TAG_Y_CB_CR_SUB_SAMPLING = "YCbCrSubSampling";
    /**
     *  <p>The position of chrominance components in relation to the luminance component. This field
     *  is designated only for JPEG compressed data or uncompressed YCbCr data. The TIFF default is
     *  {@link #Y_CB_CR_POSITIONING_CENTERED}; but when Y:Cb:Cr = 4:2:2 it is recommended in this
     *  standard that {@link #Y_CB_CR_POSITIONING_CO_SITED} be used to record data, in order to
     *  improve the image quality when viewed on TV systems. When this field does not exist,
     *  the reader shall assume the TIFF default. In the case of Y:Cb:Cr = 4:2:0, the TIFF default
     *  ({@link #Y_CB_CR_POSITIONING_CENTERED}) is recommended. If the Exif/DCF reader does not
     *  have the capability of supporting both kinds of positioning, it shall follow the TIFF
     *  default regardless of the value in this field. It is preferable that readers can support
     *  both centered and co-sited positioning.</p>
     *
     *  <ul>
     *      <li>Tag = 531</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #Y_CB_CR_POSITIONING_CENTERED}</li>
     *  </ul>
     *
     *  @see #Y_CB_CR_POSITIONING_CENTERED
     *  @see #Y_CB_CR_POSITIONING_CO_SITED
     */
    public static final String TAG_Y_CB_CR_POSITIONING = "YCbCrPositioning";
    /**
     *  <p>The number of pixels per {@link #TAG_RESOLUTION_UNIT} in the {@link #TAG_IMAGE_WIDTH}
     *  direction. When the image resolution is unknown, 72 [dpi] shall be designated.</p>
     *
     *  <ul>
     *      <li>Tag = 282</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = 72</li>
     *  </ul>
     *
     *  @see #TAG_Y_RESOLUTION
     *  @see #TAG_RESOLUTION_UNIT
     */
    public static final String TAG_X_RESOLUTION = "XResolution";
    /**
     *  <p>The number of pixels per {@link #TAG_RESOLUTION_UNIT} in the {@link #TAG_IMAGE_WIDTH}
     *  direction. The same value as {@link #TAG_X_RESOLUTION} shall be designated.</p>
     *
     *  <ul>
     *      <li>Tag = 283</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = 72</li>
     *  </ul>
     *
     *  @see #TAG_X_RESOLUTION
     *  @see #TAG_RESOLUTION_UNIT
     */
    public static final String TAG_Y_RESOLUTION = "YResolution";
    /**
     *  <p>The unit for measuring {@link #TAG_X_RESOLUTION} and {@link #TAG_Y_RESOLUTION}. The same
     *  unit is used for both {@link #TAG_X_RESOLUTION} and {@link #TAG_Y_RESOLUTION}. If the image
     *  resolution is unknown, {@link #RESOLUTION_UNIT_INCHES} shall be designated.</p>
     *
     *  <ul>
     *      <li>Tag = 296</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #RESOLUTION_UNIT_INCHES}</li>
     *  </ul>
     *
     *  @see #RESOLUTION_UNIT_INCHES
     *  @see #RESOLUTION_UNIT_CENTIMETERS
     *  @see #TAG_X_RESOLUTION
     *  @see #TAG_Y_RESOLUTION
     */
    public static final String TAG_RESOLUTION_UNIT = "ResolutionUnit";

    // B. Tags related to recording offset
    /**
     *  <p>For each strip, the byte offset of that strip. It is recommended that this be selected
     *  so the number of strip bytes does not exceed 64 KBytes.In the case of JPEG compressed data,
     *  this designation is not necessary. So, this tag shall not be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 273</li>
     *      <li>Type = Unsigned short or Unsigned long</li>
     *      <li>Count = StripsPerImage (for {@link #FORMAT_CHUNKY})
     *               or {@link #TAG_SAMPLES_PER_PIXEL} * StripsPerImage
     *               (for {@link #FORMAT_PLANAR})</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  <p>StripsPerImage = floor(({@link #TAG_IMAGE_LENGTH} + {@link #TAG_ROWS_PER_STRIP} - 1)
     *  / {@link #TAG_ROWS_PER_STRIP})</p>
     *
     *  @see #TAG_ROWS_PER_STRIP
     *  @see #TAG_STRIP_BYTE_COUNTS
     */
    public static final String TAG_STRIP_OFFSETS = "StripOffsets";
    /**
     *  <p>The number of rows per strip. This is the number of rows in the image of one strip when
     *  an image is divided into strips. In the case of JPEG compressed data, this designation is
     *  not necessary. So, this tag shall not be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 278</li>
     *      <li>Type = Unsigned short or Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #TAG_STRIP_OFFSETS
     *  @see #TAG_STRIP_BYTE_COUNTS
     */
    public static final String TAG_ROWS_PER_STRIP = "RowsPerStrip";
    /**
     *  <p>The total number of bytes in each strip. In the case of JPEG compressed data, this
     *  designation is not necessary. So, this tag shall not be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 279</li>
     *      <li>Type = Unsigned short or Unsigned long</li>
     *      <li>Count = StripsPerImage (when using {@link #FORMAT_CHUNKY})
     *               or {@link #TAG_SAMPLES_PER_PIXEL} * StripsPerImage
     *               (when using {@link #FORMAT_PLANAR})</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  <p>StripsPerImage = floor(({@link #TAG_IMAGE_LENGTH} + {@link #TAG_ROWS_PER_STRIP} - 1)
     *  / {@link #TAG_ROWS_PER_STRIP})</p>
     */
    public static final String TAG_STRIP_BYTE_COUNTS = "StripByteCounts";
    /**
     *  <p>The offset to the start byte (SOI) of JPEG compressed thumbnail data. This shall not be
     *  used for primary image JPEG data.</p>
     *
     *  <ul>
     *      <li>Tag = 513</li>
     *      <li>Type = Unsigned long</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_JPEG_INTERCHANGE_FORMAT = "JPEGInterchangeFormat";
    /**
     *  <p>The number of bytes of JPEG compressed thumbnail data. This is not used for primary image
     *  JPEG data. JPEG thumbnails are not divided but are recorded as a continuous JPEG bitstream
     *  from SOI to EOI. APPn and COM markers should not be recorded. Compressed thumbnails shall be
     *  recorded in no more than 64 KBytes, including all other data to be recorded in APP1.</p>
     *
     *  <ul>
     *      <li>Tag = 514</li>
     *      <li>Type = Unsigned long</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = "JPEGInterchangeFormatLength";

    // C. Tags related to Image Data Characteristics
    /**
     *  <p>A transfer function for the image, described in tabular style. Normally this tag need not
     *  be used, since color space is specified in {@link #TAG_COLOR_SPACE}.</p>
     *
     *  <ul>
     *      <li>Tag = 301</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 3 * 256</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_TRANSFER_FUNCTION = "TransferFunction";
    /**
     *  <p>The chromaticity of the white point of the image. Normally this tag need not be used,
     *  since color space is specified in {@link #TAG_COLOR_SPACE}.</p>
     *
     *  <ul>
     *      <li>Tag = 318</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 2</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_WHITE_POINT = "WhitePoint";
    /**
     *  <p>The chromaticity of the three primary colors of the image. Normally this tag need not
     *  be used, since color space is specified in {@link #TAG_COLOR_SPACE}.</p>
     *
     *  <ul>
     *      <li>Tag = 319</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 6</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_PRIMARY_CHROMATICITIES = "PrimaryChromaticities";
    /**
     *  <p>The matrix coefficients for transformation from RGB to YCbCr image data. About
     *  the default value, please refer to JEITA CP-3451C Spec, Annex D.</p>
     *
     *  <ul>
     *      <li>Tag = 529</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 3</li>
     *  </ul>
     */
    public static final String TAG_Y_CB_CR_COEFFICIENTS = "YCbCrCoefficients";
    /**
     *  <p>The reference black point value and reference white point value. No defaults are given
     *  in TIFF, but the values below are given as defaults here. The color space is declared in
     *  a color space information tag, with the default being the value that gives the optimal image
     *  characteristics Interoperability these conditions</p>
     *
     *  <ul>
     *      <li>Tag = 532</li>
     *      <li>Type = RATIONAL</li>
     *      <li>Count = 6</li>
     *      <li>Default = [0, 255, 0, 255, 0, 255] (when {@link #TAG_PHOTOMETRIC_INTERPRETATION}
     *                 is {@link #PHOTOMETRIC_INTERPRETATION_RGB})
     *                 or [0, 255, 0, 128, 0, 128] (when {@link #TAG_PHOTOMETRIC_INTERPRETATION}
     *                 is {@link #PHOTOMETRIC_INTERPRETATION_YCBCR})</li>
     *  </ul>
     */
    public static final String TAG_REFERENCE_BLACK_WHITE = "ReferenceBlackWhite";

    // D. Other tags
    /**
     *  <p>The date and time of image creation. In this standard it is the date and time the file
     *  was changed. The format is "YYYY:MM:DD HH:MM:SS" with time shown in 24-hour format, and
     *  the date and time separated by one blank character ({@code 0x20}). When the date and time
     *  are unknown, all the character spaces except colons (":") should be filled with blank
     *  characters, or else the Interoperability field should be filled with blank characters.
     *  The character string length is 20 Bytes including NULL for termination. When the field is
     *  left blank, it is treated as unknown.</p>
     *
     *  <ul>
     *      <li>Tag = 306</li>
     *      <li>Type = String</li>
     *      <li>Length = 19</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_DATETIME = "DateTime";
    /**
     *  <p>An ASCII string giving the title of the image. It is possible to be added a comment
     *  such as "1988 company picnic" or the like. Two-byte character codes cannot be used. When
     *  a 2-byte code is necessary, {@link #TAG_USER_COMMENT} is to be used.</p>
     *
     *  <ul>
     *      <li>Tag = 270</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_IMAGE_DESCRIPTION = "ImageDescription";
    /**
     *  <p>The manufacturer of the recording equipment. This is the manufacturer of the DSC,
     *  scanner, video digitizer or other equipment that generated the image. When the field is left
     *  blank, it is treated as unknown.</p>
     *
     *  <ul>
     *      <li>Tag = 271</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_MAKE = "Make";
    /**
     *  <p>The model name or model number of the equipment. This is the model name of number of
     *  the DSC, scanner, video digitizer or other equipment that generated the image. When
     *  the field is left blank, it is treated as unknown.</p>
     *
     *  <ul>
     *      <li>Tag = 272</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_MODEL = "Model";
    /**
     *  <p>This tag records the name and version of the software or firmware of the camera or image
     *  input device used to generate the image. The detailed format is not specified, but it is
     *  recommended that the example shown below be followed. When the field is left blank, it is
     *  treated as unknown.</p>
     *
     *  <p>Ex.) "Exif Software Version 1.00a".</p>
     *
     *  <ul>
     *      <li>Tag = 305</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SOFTWARE = "Software";
    /**
     *  <p>This tag records the name of the camera owner, photographer or image creator.
     *  The detailed format is not specified, but it is recommended that the information be written
     *  as in the example below for ease of Interoperability. When the field is left blank, it is
     *  treated as unknown.</p>
     *
     *  <p>Ex.) "Camera owner, John Smith; Photographer, Michael Brown; Image creator,
     *  Ken James"</p>
     *
     *  <ul>
     *      <li>Tag = 315</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_ARTIST = "Artist";
    /**
     *  <p>Copyright information. In this standard the tag is used to indicate both the photographer
     *  and editor copyrights. It is the copyright notice of the person or organization claiming
     *  rights to the image. The Interoperability copyright statement including date and rights
     *  should be written in this field; e.g., "Copyright, John Smith, 19xx. All rights reserved."
     *  In this standard the field records both the photographer and editor copyrights, with each
     *  recorded in a separate part of the statement. When there is a clear distinction between
     *  the photographer and editor copyrights, these are to be written in the order of photographer
     *  followed by editor copyright, separated by NULL (in this case, since the statement also ends
     *  with a NULL, there are two NULL codes) (see example 1). When only the photographer copyright
     *  is given, it is terminated by one NULL code (see example 2). When only the editor copyright
     *  is given, the photographer copyright part consists of one space followed by a terminating
     *  NULL code, then the editor copyright is given (see example 3). When the field is left blank,
     *  it is treated as unknown.</p>
     *
     *  <p>Ex. 1) When both the photographer copyright and editor copyright are given.
     *  <ul><li>Photographer copyright + NULL + editor copyright + NULL</li></ul></p>
     *  <p>Ex. 2) When only the photographer copyright is given.
     *  <ul><li>Photographer copyright + NULL</li></ul></p>
     *  <p>Ex. 3) When only the editor copyright is given.
     *  <ul><li>Space ({@code 0x20}) + NULL + editor copyright + NULL</li></ul></p>
     *
     *  <ul>
     *      <li>Tag = 315</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_COPYRIGHT = "Copyright";

    // Exif IFD Attribute Information
    // A. Tags related to version
    /**
     *  <p>The version of this standard supported. Nonexistence of this field is taken to mean
     *  nonconformance to the standard. In according with conformance to this standard, this tag
     *  shall be recorded like "0230” as 4-byte ASCII.</p>
     *
     *  <ul>
     *      <li>Tag = 36864</li>
     *      <li>Type = Undefined</li>
     *      <li>Length = 4</li>
     *      <li>Default = "0230"</li>
     *  </ul>
     */
    public static final String TAG_EXIF_VERSION = "ExifVersion";
    /**
     *  <p>The Flashpix format version supported by a FPXR file. If the FPXR function supports
     *  Flashpix format Ver. 1.0, this is indicated similarly to {@link #TAG_EXIF_VERSION} by
     *  recording "0100" as 4-byte ASCII.</p>
     *
     *  <ul>
     *      <li>Tag = 40960</li>
     *      <li>Type = Undefined</li>
     *      <li>Length = 4</li>
     *      <li>Default = "0100"</li>
     *  </ul>
     */
    public static final String TAG_FLASHPIX_VERSION = "FlashpixVersion";

    // B. Tags related to image data characteristics
    /**
     *  <p>The color space information tag is always recorded as the color space specifier.
     *  Normally {@link #COLOR_SPACE_S_RGB} is used to define the color space based on the PC
     *  monitor conditions and environment. If a color space other than {@link #COLOR_SPACE_S_RGB}
     *  is used, {@link #COLOR_SPACE_UNCALIBRATED} is set. Image data recorded as
     *  {@link #COLOR_SPACE_UNCALIBRATED} may be treated as {@link #COLOR_SPACE_S_RGB} when it is
     *  converted to Flashpix.</p>
     *
     *  <ul>
     *      <li>Tag = 40961</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *  </ul>
     *
     *  @see #COLOR_SPACE_S_RGB
     *  @see #COLOR_SPACE_UNCALIBRATED
     */
    public static final String TAG_COLOR_SPACE = "ColorSpace";
    /**
     *  <p>Indicates the value of coefficient gamma. The formula of transfer function used for image
     *  reproduction is expressed as follows.</p>
     *
     *  <p>(Reproduced value) = (Input value) ^ gamma</p>
     *
     *  <p>Both reproduced value and input value indicate normalized value, whose minimum value is
     *  0 and maximum value is 1.</p>
     *
     *  <ul>
     *      <li>Tag = 42240</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GAMMA = "Gamma";

    // C. Tags related to image configuration
    /**
     *  <p>Information specific to compressed data. When a compressed file is recorded, the valid
     *  width of the meaningful image shall be recorded in this tag, whether or not there is padding
     *  data or a restart marker. This tag shall not exist in an uncompressed file.</p>
     *
     *  <ul>
     *      <li>Tag = 40962</li>
     *      <li>Type = Unsigned short or Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_PIXEL_X_DIMENSION = "PixelXDimension";
    /**
     *  <p>Information specific to compressed data. When a compressed file is recorded, the valid
     *  height of the meaningful image shall be recorded in this tag, whether or not there is
     *  padding data or a restart marker. This tag shall not exist in an uncompressed file.
     *  Since data padding is unnecessary in the vertical direction, the number of lines recorded
     *  in this valid image height tag will in fact be the same as that recorded in the SOF.</p>
     *
     *  <ul>
     *      <li>Tag = 40963</li>
     *      <li>Type = Unsigned short or Unsigned long</li>
     *      <li>Count = 1</li>
     *  </ul>
     */
    public static final String TAG_PIXEL_Y_DIMENSION = "PixelYDimension";
    /**
     *  <p>Information specific to compressed data. The channels of each component are arranged
     *  in order from the 1st component to the 4th. For uncompressed data the data arrangement is
     *  given in the {@link #TAG_PHOTOMETRIC_INTERPRETATION}. However, since
     *  {@link #TAG_PHOTOMETRIC_INTERPRETATION} can only express the order of Y, Cb and Cr, this tag
     *  is provided for cases when compressed data uses components other than Y, Cb, and Cr and to
     *  enable support of other sequences.</p>
     *
     *  <ul>
     *      <li>Tag = 37121</li>
     *      <li>Type = Undefined</li>
     *      <li>Length = 4</li>
     *      <li>Default = 4 5 6 0 (if RGB uncompressed) or 1 2 3 0 (other cases)</li>
     *      <ul>
     *          <li>0 = does not exist</li>
     *          <li>1 = Y</li>
     *          <li>2 = Cb</li>
     *          <li>3 = Cr</li>
     *          <li>4 = R</li>
     *          <li>5 = G</li>
     *          <li>6 = B</li>
     *          <li>other = reserved</li>
     *      </ul>
     *  </ul>
     */
    public static final String TAG_COMPONENTS_CONFIGURATION = "ComponentsConfiguration";
    /**
     *  <p>Information specific to compressed data. The compression mode used for a compressed image
     *  is indicated in unit bits per pixel.</p>
     *
     *  <ul>
     *      <li>Tag = 37122</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_COMPRESSED_BITS_PER_PIXEL = "CompressedBitsPerPixel";

    // D. Tags related to user information
    /**
     *  <p>A tag for manufacturers of Exif/DCF writers to record any desired information.
     *  The contents are up to the manufacturer, but this tag shall not be used for any other than
     *  its intended purpose.</p>
     *
     *  <ul>
     *      <li>Tag = 37500</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_MAKER_NOTE = "MakerNote";
    /**
     *  <p>A tag for Exif users to write keywords or comments on the image besides those in
     *  {@link #TAG_IMAGE_DESCRIPTION}, and without the character code limitations of it.</p>
     *
     *  <ul>
     *      <li>Tag = 37510</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_USER_COMMENT = "UserComment";

    // E. Tags related to related file information
    /**
     *  <p>This tag is used to record the name of an audio file related to the image data. The only
     *  relational information recorded here is the Exif audio file name and extension (an ASCII
     *  string consisting of 8 characters + '.' + 3 characters). The path is not recorded.</p>
     *
     *  <p>When using this tag, audio files shall be recorded in conformance to the Exif audio
     *  format. Writers can also store the data such as Audio within APP2 as Flashpix extension
     *  stream data. Audio files shall be recorded in conformance to the Exif audio format.</p>
     *
     *  <ul>
     *      <li>Tag = 40964</li>
     *      <li>Type = String</li>
     *      <li>Length = 12</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_RELATED_SOUND_FILE = "RelatedSoundFile";

    // F. Tags related to date and time
    /**
     *  <p>The date and time when the original image data was generated. For a DSC the date and time
     *  the picture was taken are recorded. The format is "YYYY:MM:DD HH:MM:SS" with time shown in
     *  24-hour format, and the date and time separated by one blank character ({@code 0x20}).
     *  When the date and time are unknown, all the character spaces except colons (":") should be
     *  filled with blank characters, or else the Interoperability field should be filled with blank
     *  characters. When the field is left blank, it is treated as unknown.</p>
     *
     *  <ul>
     *      <li>Tag = 36867</li>
     *      <li>Type = String</li>
     *      <li>Length = 19</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    /**
     *  <p>The date and time when the image was stored as digital data. If, for example, an image
     *  was captured by DSC and at the same time the file was recorded, then
     *  {@link #TAG_DATETIME_ORIGINAL} and this tag will have the same contents. The format is
     *  "YYYY:MM:DD HH:MM:SS" with time shown in 24-hour format, and the date and time separated by
     *  one blank character ({@code 0x20}). When the date and time are unknown, all the character
     *  spaces except colons (":")should be filled with blank characters, or else
     *  the Interoperability field should be filled with blank characters. When the field is left
     *  blank, it is treated as unknown.</p>
     *
     *  <ul>
     *      <li>Tag = 36868</li>
     *      <li>Type = String</li>
     *      <li>Length = 19</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    /**
     *  <p>A tag used to record fractions of seconds for {@link #TAG_DATETIME}.</p>
     *
     *  <ul>
     *      <li>Tag = 37520</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SUBSEC_TIME = "SubSecTime";
    /**
     *  <p>A tag used to record fractions of seconds for {@link #TAG_DATETIME_ORIGINAL}.</p>
     *
     *  <ul>
     *      <li>Tag = 37521</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SUBSEC_TIME_ORIGINAL = "SubSecTimeOriginal";
    /**
     *  <p>A tag used to record fractions of seconds for {@link #TAG_DATETIME_DIGITIZED}.</p>
     *
     *  <ul>
     *      <li>Tag = 37522</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SUBSEC_TIME_DIGITIZED = "SubSecTimeDigitized";

    // G. Tags related to picture-taking condition
    /**
     *  <p>Exposure time, given in seconds.</p>
     *
     *  <ul>
     *      <li>Tag = 33434</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_EXPOSURE_TIME = "ExposureTime";
    /**
     *  <p>The F number.</p>
     *
     *  <ul>
     *      <li>Tag = 33437</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_F_NUMBER = "FNumber";
    /**
     *  <p>TThe class of the program used by the camera to set exposure when the picture is taken.
     *  The tag values are as follows.</p>
     *
     *  <ul>
     *      <li>Tag = 34850</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #EXPOSURE_PROGRAM_NOT_DEFINED}</li>
     *  </ul>
     *
     *  @see #EXPOSURE_PROGRAM_NOT_DEFINED
     *  @see #EXPOSURE_PROGRAM_MANUAL
     *  @see #EXPOSURE_PROGRAM_NORMAL
     *  @see #EXPOSURE_PROGRAM_APERTURE_PRIORITY
     *  @see #EXPOSURE_PROGRAM_SHUTTER_PRIORITY
     *  @see #EXPOSURE_PROGRAM_CREATIVE
     *  @see #EXPOSURE_PROGRAM_ACTION
     *  @see #EXPOSURE_PROGRAM_PORTRAIT_MODE
     *  @see #EXPOSURE_PROGRAM_LANDSCAPE_MODE
     */
    public static final String TAG_EXPOSURE_PROGRAM = "ExposureProgram";
    /**
     *  <p>Indicates the spectral sensitivity of each channel of the camera used. The tag value is
     *  an ASCII string compatible with the standard developed by the ASTM Technical committee.</p>
     *
     *  <ul>
     *      <li>Tag = 34852</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SPECTRAL_SENSITIVITY = "SpectralSensitivity";
    /**
     *  @deprecated Use {@link #TAG_PHOTOGRAPHIC_SENSITIVITY} instead.
     *  @see #TAG_PHOTOGRAPHIC_SENSITIVITY
     */
    @Deprecated public static final String TAG_ISO_SPEED_RATINGS = "ISOSpeedRatings";
    /**
     *  <p>This tag indicates the sensitivity of the camera or input device when the image was shot.
     *  More specifically, it indicates one of the following values that are parameters defined in
     *  ISO 12232: standard output sensitivity (SOS), recommended exposure index (REI), or ISO
     *  speed. Accordingly, if a tag corresponding to a parameter that is designated by
     *  {@link #TAG_SENSITIVITY_TYPE} is recorded, the values of the tag and of this tag are
     *  the same. However, if the value is 65535 or higher, the value of this tag shall be 65535.
     *  When recording this tag, {@link #TAG_SENSITIVITY_TYPE} should also be recorded. In addition,
     *  while “Count = Any”, only 1 count should be used when recording this tag.</p>
     *
     *  <ul>
     *      <li>Tag = 34855</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = Any</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_PHOTOGRAPHIC_SENSITIVITY = "PhotographicSensitivity";
    /**
     *  <p>Indicates the Opto-Electric Conversion Function (OECF) specified in ISO 14524. OECF is
     *  the relationship between the camera optical input and the image values.</p>
     *
     *  <ul>
     *      <li>Tag = 34856</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_OECF = "OECF";
    /**
     *  <p>This tag indicates which one of the parameters of ISO12232 is
     *  {@link #TAG_PHOTOGRAPHIC_SENSITIVITY}. Although it is an optional tag, it should be recorded
     *  when {@link #TAG_PHOTOGRAPHIC_SENSITIVITY} is recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 34864</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #SENSITIVITY_TYPE_UNKNOWN
     *  @see #SENSITIVITY_TYPE_SOS
     *  @see #SENSITIVITY_TYPE_REI
     *  @see #SENSITIVITY_TYPE_ISO_SPEED
     *  @see #SENSITIVITY_TYPE_SOS_AND_REI
     *  @see #SENSITIVITY_TYPE_SOS_AND_ISO
     *  @see #SENSITIVITY_TYPE_REI_AND_ISO
     *  @see #SENSITIVITY_TYPE_SOS_AND_REI_AND_ISO
     */
    public static final String TAG_SENSITIVITY_TYPE = "SensitivityType";
    /**
     *  <p>This tag indicates the standard output sensitivity value of a camera or input device
     *  defined in ISO 12232. When recording this tag, {@link #TAG_PHOTOGRAPHIC_SENSITIVITY} and
     *  {@link #TAG_SENSITIVITY_TYPE} shall also be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 34865</li>
     *      <li>Type = Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_STANDARD_OUTPUT_SENSITIVITY = "StandardOutputSensitivity";
    /**
     *  <p>This tag indicates the recommended exposure index value of a camera or input device
     *  defined in ISO 12232. When recording this tag, {@link #TAG_PHOTOGRAPHIC_SENSITIVITY} and
     *  {@link #TAG_SENSITIVITY_TYPE} shall also be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 34866</li>
     *      <li>Type = Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_RECOMMENDED_EXPOSURE_INDEX = "RecommendedExposureIndex";
    /**
     *  <p>This tag indicates the ISO speed value of a camera or input device that is defined in
     *  ISO 12232. When recording this tag, {@link #TAG_PHOTOGRAPHIC_SENSITIVITY} and
     *  {@link #TAG_SENSITIVITY_TYPE} shall also be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 34867</li>
     *      <li>Type = Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_ISO_SPEED = "ISOSpeed";
    /**
     *  <p>This tag indicates the ISO speed latitude yyy value of a camera or input device that is
     *  defined in ISO 12232. However, this tag shall not be recorded without {@link #TAG_ISO_SPEED}
     *  and {@link #TAG_ISO_SPEED_LATITUDE_ZZZ}.</p>
     *
     *  <ul>
     *      <li>Tag = 34868</li>
     *      <li>Type = Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_ISO_SPEED_LATITUDE_YYY = "ISOSpeedLatitudeyyy";
    /**
     *  <p>This tag indicates the ISO speed latitude zzz value of a camera or input device that is
     *  defined in ISO 12232. However, this tag shall not be recorded without {@link #TAG_ISO_SPEED}
     *  and {@link #TAG_ISO_SPEED_LATITUDE_YYY}.</p>
     *
     *  <ul>
     *      <li>Tag = 34869</li>
     *      <li>Type = Unsigned long</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_ISO_SPEED_LATITUDE_ZZZ = "ISOSpeedLatitudezzz";
    /**
     *  <p>Shutter speed. The unit is the APEX setting.</p>
     *
     *  <ul>
     *      <li>Tag = 37377</li>
     *      <li>Type = Signed rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SHUTTER_SPEED_VALUE = "ShutterSpeedValue";
    /**
     *  <p>The lens aperture. The unit is the APEX value.</p>
     *
     *  <ul>
     *      <li>Tag = 37378</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_APERTURE_VALUE = "ApertureValue";
    /**
     *  <p>The value of brightness. The unit is the APEX value. Ordinarily it is given in the range
     *  of -99.99 to 99.99. Note that if the numerator of the recorded value is 0xFFFFFFFF,
     *  Unknown shall be indicated.</p>
     *
     *  <ul>
     *      <li>Tag = 37379</li>
     *      <li>Type = Signed rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_BRIGHTNESS_VALUE = "BrightnessValue";
    /**
     *  <p>The exposure bias. The unit is the APEX value. Ordinarily it is given in the range of
     *  -99.99 to 99.99.</p>
     *
     *  <ul>
     *      <li>Tag = 37380</li>
     *      <li>Type = Signed rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_EXPOSURE_BIAS_VALUE = "ExposureBiasValue";
    /**
     *  <p>The smallest F number of the lens. The unit is the APEX value. Ordinarily it is given
     *  in the range of 00.00 to 99.99, but it is not limited to this range.</p>
     *
     *  <ul>
     *      <li>Tag = 37381</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_MAX_APERTURE_VALUE = "MaxApertureValue";
    /**
     *  <p>The distance to the subject, given in meters. Note that if the numerator of the recorded
     *  value is 0xFFFFFFFF, Infinity shall be indicated; and if the numerator is 0, Distance
     *  unknown shall be indicated.</p>
     *
     *  <ul>
     *      <li>Tag = 37382</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SUBJECT_DISTANCE = "SubjectDistance";
    /**
     *  <p>The metering mode.</p>
     *
     *  <ul>
     *      <li>Tag = 37383</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #METERING_MODE_UNKNOWN}</li>
     *  </ul>
     *
     *  @see #METERING_MODE_UNKNOWN
     *  @see #METERING_MODE_AVERAGE
     *  @see #METERING_MODE_CENTER_WEIGHT_AVERAGE
     *  @see #METERING_MODE_SPOT
     *  @see #METERING_MODE_MULTI_SPOT
     *  @see #METERING_MODE_PATTERN
     *  @see #METERING_MODE_PARTIAL
     *  @see #METERING_MODE_OTHER
     */
    public static final String TAG_METERING_MODE = "MeteringMode";
    /**
     *  <p>The kind of light source.</p>
     *
     *  <ul>
     *      <li>Tag = 37384</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #LIGHT_SOURCE_UNKNOWN}</li>
     *  </ul>
     *
     *  @see #LIGHT_SOURCE_UNKNOWN
     *  @see #LIGHT_SOURCE_DAYLIGHT
     *  @see #LIGHT_SOURCE_FLUORESCENT
     *  @see #LIGHT_SOURCE_TUNGSTEN
     *  @see #LIGHT_SOURCE_FLASH
     *  @see #LIGHT_SOURCE_FINE_WEATHER
     *  @see #LIGHT_SOURCE_CLOUDY_WEATHER
     *  @see #LIGHT_SOURCE_SHADE
     *  @see #LIGHT_SOURCE_DAYLIGHT_FLUORESCENT
     *  @see #LIGHT_SOURCE_DAY_WHITE_FLUORESCENT
     *  @see #LIGHT_SOURCE_COOL_WHITE_FLUORESCENT
     *  @see #LIGHT_SOURCE_WHITE_FLUORESCENT
     *  @see #LIGHT_SOURCE_WARM_WHITE_FLUORESCENT
     *  @see #LIGHT_SOURCE_STANDARD_LIGHT_A
     *  @see #LIGHT_SOURCE_STANDARD_LIGHT_B
     *  @see #LIGHT_SOURCE_STANDARD_LIGHT_C
     *  @see #LIGHT_SOURCE_D55
     *  @see #LIGHT_SOURCE_D65
     *  @see #LIGHT_SOURCE_D75
     *  @see #LIGHT_SOURCE_D50
     *  @see #LIGHT_SOURCE_ISO_STUDIO_TUNGSTEN
     *  @see #LIGHT_SOURCE_OTHER
     */
    public static final String TAG_LIGHT_SOURCE = "LightSource";
    /**
     *  <p>This tag indicates the status of flash when the image was shot. Bit 0 indicates the flash
     *  firing status, bits 1 and 2 indicate the flash return status, bits 3 and 4 indicate
     *  the flash mode, bit 5 indicates whether the flash function is present, and bit 6 indicates
     *  "red eye" mode.</p>
     *
     *  <ul>
     *      <li>Tag = 37385</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *  </ul>
     *
     *  @see #FLAG_FLASH_FIRED
     *  @see #FLAG_FLASH_RETURN_LIGHT_NOT_DETECTED
     *  @see #FLAG_FLASH_RETURN_LIGHT_DETECTED
     *  @see #FLAG_FLASH_MODE_COMPULSORY_FIRING
     *  @see #FLAG_FLASH_MODE_COMPULSORY_SUPPRESSION
     *  @see #FLAG_FLASH_MODE_AUTO
     *  @see #FLAG_FLASH_NO_FLASH_FUNCTION
     *  @see #FLAG_FLASH_RED_EYE_SUPPORTED
     */
    public static final String TAG_FLASH = "Flash";
    /**
     *  <p>This tag indicates the location and area of the main subject in the overall scene.</p>
     *
     *  <ul>
     *      <li>Tag = 37396</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 2 or 3 or 4</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  <p>The subject location and area are defined by Count values as follows.</p>
     *
     *  <ul>
     *      <li>Count = 2 Indicates the location of the main subject as coordinates. The first value
     *                    is the X coordinate and the second is the Y coordinate.</li>
     *      <li>Count = 3 The area of the main subject is given as a circle. The circular area is
     *                    expressed as center coordinates and diameter. The first value is
     *                    the center X coordinate, the second is the center Y coordinate, and
     *                    the third is the diameter.</li>
     *      <li>Count = 4 The area of the main subject is given as a rectangle. The rectangular
     *                    area is expressed as center coordinates and area dimensions. The first
     *                    value is the center X coordinate, the second is the center Y coordinate,
     *                    the third is the width of the area, and the fourth is the height of
     *                    the area.</li>
     *  </ul>
     *
     *  <p>Note that the coordinate values, width, and height are expressed in relation to the upper
     *  left as origin, prior to rotation processing as per {@link #TAG_ORIENTATION}.</p>
     */
    public static final String TAG_SUBJECT_AREA = "SubjectArea";
    /**
     *  <p>The actual focal length of the lens, in mm. Conversion is not made to the focal length
     *  of a 35mm film camera.</p>
     *
     *  <ul>
     *      <li>Tag = 37386</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_FOCAL_LENGTH = "FocalLength";
    /**
     *  <p>Indicates the strobe energy at the time the image is captured, as measured in Beam Candle
     *  Power Seconds (BCPS).</p>
     *
     *  <ul>
     *      <li>Tag = 41483</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_FLASH_ENERGY = "FlashEnergy";
    /**
     *  <p>This tag records the camera or input device spatial frequency table and SFR values in
     *  the direction of image width, image height, and diagonal direction, as specified in
     *  ISO 12233.</p>
     *
     *  <ul>
     *      <li>Tag = 41484</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SPATIAL_FREQUENCY_RESPONSE = "SpatialFrequencyResponse";
    /**
     *  <p>Indicates the number of pixels in the image width (X) direction per
     *  {@link #TAG_FOCAL_PLANE_RESOLUTION_UNIT} on the camera focal plane.</p>
     *
     *  <ul>
     *      <li>Tag = 41486</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_FOCAL_PLANE_X_RESOLUTION = "FocalPlaneXResolution";
    /**
     *  <p>Indicates the number of pixels in the image height (Y) direction per
     *  {@link #TAG_FOCAL_PLANE_RESOLUTION_UNIT} on the camera focal plane.</p>
     *
     *  <ul>
     *      <li>Tag = 41487</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_FOCAL_PLANE_Y_RESOLUTION = "FocalPlaneYResolution";
    /**
     *  <p>Indicates the unit for measuring {@link #TAG_FOCAL_PLANE_X_RESOLUTION} and
     *  {@link #TAG_FOCAL_PLANE_Y_RESOLUTION}. This value is the same as
     *  {@link #TAG_RESOLUTION_UNIT}.</p>
     *
     *  <ul>
     *      <li>Tag = 41488</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #RESOLUTION_UNIT_INCHES}</li>
     *  </ul>
     *
     *  @see #TAG_RESOLUTION_UNIT
     *  @see #RESOLUTION_UNIT_INCHES
     *  @see #RESOLUTION_UNIT_CENTIMETERS
     */
    public static final String TAG_FOCAL_PLANE_RESOLUTION_UNIT = "FocalPlaneResolutionUnit";
    /**
     *  <p>Indicates the location of the main subject in the scene. The value of this tag represents
     *  the pixel at the center of the main subject relative to the left edge, prior to rotation
     *  processing as per {@link #TAG_ORIENTATION}. The first value indicates the X column number
     *  and second indicates the Y row number. When a camera records the main subject location,
     *  it is recommended that {@link #TAG_SUBJECT_AREA} be used instead of this tag.</p>
     *
     *  <ul>
     *      <li>Tag = 41492</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 2</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_SUBJECT_LOCATION = "SubjectLocation";
    /**
     *  <p>Indicates the exposure index selected on the camera or input device at the time the image
     *  is captured.</p>
     *
     *  <ul>
     *      <li>Tag = 41493</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_EXPOSURE_INDEX = "ExposureIndex";
    /**
     *  <p>Indicates the image sensor type on the camera or input device.</p>
     *
     *  <ul>
     *      <li>Tag = 41495</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #SENSOR_TYPE_NOT_DEFINED
     *  @see #SENSOR_TYPE_ONE_CHIP
     *  @see #SENSOR_TYPE_TWO_CHIP
     *  @see #SENSOR_TYPE_THREE_CHIP
     *  @see #SENSOR_TYPE_COLOR_SEQUENTIAL
     *  @see #SENSOR_TYPE_TRILINEAR
     *  @see #SENSOR_TYPE_COLOR_SEQUENTIAL_LINEAR
     */
    public static final String TAG_SENSING_METHOD = "SensingMethod";
    /**
     *  <p>Indicates the image source. If a DSC recorded the image, this tag value always shall
     *  be set to {@link #FILE_SOURCE_DSC}.</p>
     *
     *  <ul>
     *      <li>Tag = 41728</li>
     *      <li>Type = Undefined</li>
     *      <li>Length = 1</li>
     *      <li>Default = {@link #FILE_SOURCE_DSC}</li>
     *  </ul>
     *
     *  @see #FILE_SOURCE_OTHER
     *  @see #FILE_SOURCE_TRANSPARENT_SCANNER
     *  @see #FILE_SOURCE_REFLEX_SCANNER
     *  @see #FILE_SOURCE_DSC
     */
    public static final String TAG_FILE_SOURCE = "FileSource";
    /**
     *  <p>Indicates the type of scene. If a DSC recorded the image, this tag value shall always
     *  be set to {@link #SCENE_TYPE_DIRECTLY_PHOTOGRAPHED}.</p>
     *
     *  <ul>
     *      <li>Tag = 41729</li>
     *      <li>Type = Undefined</li>
     *      <li>Length = 1</li>
     *      <li>Default = 1</li>
     *  </ul>
     *
     *  @see #SCENE_TYPE_DIRECTLY_PHOTOGRAPHED
     */
    public static final String TAG_SCENE_TYPE = "SceneType";
    /**
     *  <p>Indicates the color filter array (CFA) geometric pattern of the image sensor when
     *  a one-chip color area sensor is used. It does not apply to all sensing methods.</p>
     *
     *  <ul>
     *      <li>Tag = 41730</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #TAG_SENSING_METHOD
     *  @see #SENSOR_TYPE_ONE_CHIP
     */
    public static final String TAG_CFA_PATTERN = "CFAPattern";
    /**
     *  <p>This tag indicates the use of special processing on image data, such as rendering geared
     *  to output. When special processing is performed, the Exif/DCF reader is expected to disable
     *  or minimize any further processing.</p>
     *
     *  <ul>
     *      <li>Tag = 41985</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #RENDERED_PROCESS_NORMAL}</li>
     *  </ul>
     *
     *  @see #RENDERED_PROCESS_NORMAL
     *  @see #RENDERED_PROCESS_CUSTOM
     */
    public static final String TAG_CUSTOM_RENDERED = "CustomRendered";
    /**
     *  <p>This tag indicates the exposure mode set when the image was shot.
     *  In {@link #EXPOSURE_MODE_AUTO_BRACKET}, the camera shoots a series of frames of the same
     *  scene at different exposure settings.</p>
     *
     *  <ul>
     *      <li>Tag = 41986</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #EXPOSURE_MODE_AUTO
     *  @see #EXPOSURE_MODE_MANUAL
     *  @see #EXPOSURE_MODE_AUTO_BRACKET
     */
    public static final String TAG_EXPOSURE_MODE = "ExposureMode";
    /**
     *  <p>This tag indicates the white balance mode set when the image was shot.</p>
     *
     *  <ul>
     *      <li>Tag = 41987</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #WHITEBALANCE_AUTO
     *  @see #WHITEBALANCE_MANUAL
     */
    public static final String TAG_WHITE_BALANCE = "WhiteBalance";
    /**
     *  <p>This tag indicates the digital zoom ratio when the image was shot. If the numerator of
     *  the recorded value is 0, this indicates that digital zoom was not used.</p>
     *
     *  <ul>
     *      <li>Tag = 41988</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_DIGITAL_ZOOM_RATIO = "DigitalZoomRatio";
    /**
     *  <p>This tag indicates the equivalent focal length assuming a 35mm film camera, in mm.
     *  A value of 0 means the focal length is unknown. Note that this tag differs from
     *  {@link #TAG_FOCAL_LENGTH}.</p>
     *
     *  <ul>
     *      <li>Tag = 41989</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_FOCAL_LENGTH_IN_35MM_FILM = "FocalLengthIn35mmFilm";
    /**
     *  <p>This tag indicates the type of scene that was shot. It may also be used to record
     *  the mode in which the image was shot. Note that this differs from
     *  {@link #TAG_SCENE_TYPE}.</p>
     *
     *  <ul>
     *      <li>Tag = 41990</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = 0</li>
     *  </ul>
     *
     *  @see #SCENE_CAPTURE_TYPE_STANDARD
     *  @see #SCENE_CAPTURE_TYPE_LANDSCAPE
     *  @see #SCENE_CAPTURE_TYPE_PORTRAIT
     *  @see #SCENE_CAPTURE_TYPE_NIGHT
     */
    public static final String TAG_SCENE_CAPTURE_TYPE = "SceneCaptureType";
    /**
     *  <p>This tag indicates the degree of overall image gain adjustment.</p>
     *
     *  <ul>
     *      <li>Tag = 41991</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #GAIN_CONTROL_NONE
     *  @see #GAIN_CONTROL_LOW_GAIN_UP
     *  @see #GAIN_CONTROL_HIGH_GAIN_UP
     *  @see #GAIN_CONTROL_LOW_GAIN_DOWN
     *  @see #GAIN_CONTROL_HIGH_GAIN_DOWN
     */
    public static final String TAG_GAIN_CONTROL = "GainControl";
    /**
     *  <p>This tag indicates the direction of contrast processing applied by the camera when
     *  the image was shot.</p>
     *
     *  <ul>
     *      <li>Tag = 41992</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #CONTRAST_NORMAL}</li>
     *  </ul>
     *
     *  @see #CONTRAST_NORMAL
     *  @see #CONTRAST_SOFT
     *  @see #CONTRAST_HARD
     */
    public static final String TAG_CONTRAST = "Contrast";
    /**
     *  <p>This tag indicates the direction of saturation processing applied by the camera when
     *  the image was shot.</p>
     *
     *  <ul>
     *      <li>Tag = 41993</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #SATURATION_NORMAL}</li>
     *  </ul>
     *
     *  @see #SATURATION_NORMAL
     *  @see #SATURATION_LOW
     *  @see #SATURATION_HIGH
     */
    public static final String TAG_SATURATION = "Saturation";
    /**
     *  <p>This tag indicates the direction of sharpness processing applied by the camera when
     *  the image was shot.</p>
     *
     *  <ul>
     *      <li>Tag = 41994</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = {@link #SHARPNESS_NORMAL}</li>
     *  </ul>
     *
     *  @see #SHARPNESS_NORMAL
     *  @see #SHARPNESS_SOFT
     *  @see #SHARPNESS_HARD
     */
    public static final String TAG_SHARPNESS = "Sharpness";
    /**
     *  <p>This tag indicates information on the picture-taking conditions of a particular camera
     *  model. The tag is used only to indicate the picture-taking conditions in the Exif/DCF
     *  reader.</p>
     *
     *  <ul>
     *      <li>Tag = 41995</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_DEVICE_SETTING_DESCRIPTION = "DeviceSettingDescription";
    /**
     *  <p>This tag indicates the distance to the subject.</p>
     *
     *  <ul>
     *      <li>Tag = 41996</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #SUBJECT_DISTANCE_RANGE_UNKNOWN
     *  @see #SUBJECT_DISTANCE_RANGE_MACRO
     *  @see #SUBJECT_DISTANCE_RANGE_CLOSE_VIEW
     *  @see #SUBJECT_DISTANCE_RANGE_DISTANT_VIEW
     */
    public static final String TAG_SUBJECT_DISTANCE_RANGE = "SubjectDistanceRange";

    // H. Other tags
    /**
     *  <p>This tag indicates an identifier assigned uniquely to each image. It is recorded as
     *  an ASCII string equivalent to hexadecimal notation and 128-bit fixed length.</p>
     *
     *  <ul>
     *      <li>Tag = 42016</li>
     *      <li>Type = String</li>
     *      <li>Length = 32</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_IMAGE_UNIQUE_ID = "ImageUniqueID";
    /**
     *  <p>This tag records the owner of a camera used in photography as an ASCII string.</p>
     *
     *  <ul>
     *      <li>Tag = 42032</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_CAMARA_OWNER_NAME = "CameraOwnerName";
    /**
     *  <p>This tag records the serial number of the body of the camera that was used in photography
     *  as an ASCII string.</p>
     *
     *  <ul>
     *      <li>Tag = 42033</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_BODY_SERIAL_NUMBER = "BodySerialNumber";
    /**
     *  <p>This tag notes minimum focal length, maximum focal length, minimum F number in the
     *  minimum focal length, and minimum F number in the maximum focal length, which are
     *  specification information for the lens that was used in photography. When the minimum
     *  F number is unknown, the notation is 0/0.</p>
     *
     *  <ul>
     *      <li>Tag = 42034</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 4</li>
     *      <li>Default = None</li>
     *      <ul>
     *          <li>Value 1 := Minimum focal length (unit: mm)</li>
     *          <li>Value 2 : = Maximum focal length (unit: mm)</li>
     *          <li>Value 3 : = Minimum F number in the minimum focal length</li>
     *          <li>Value 4 : = Minimum F number in the maximum focal length</li>
     *      </ul>
     *  </ul>
     */
    public static final String TAG_LENS_SPECIFICATION = "LensSpecification";
    /**
     *  <p>This tag records the lens manufacturer as an ASCII string.</p>
     *
     *  <ul>
     *      <li>Tag = 42035</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_LENS_MAKE = "LensMake";
    /**
     *  <p>This tag records the lens’s model name and model number as an ASCII string.</p>
     *
     *  <ul>
     *      <li>Tag = 42036</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_LENS_MODEL = "LensModel";
    /**
     *  <p>This tag records the serial number of the interchangeable lens that was used in
     *  photography as an ASCII string.</p>
     *
     *  <ul>
     *      <li>Tag = 42037</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_LENS_SERIAL_NUMBER = "LensSerialNumber";

    // GPS Attribute Information
    /**
     *  <p>Indicates the version of GPS Info IFD. The version is given as 2.3.0.0. This tag is
     *  mandatory when GPS-related tags are present. Note that this tag is written as a different
     *  byte than {@link #TAG_EXIF_VERSION}.</p>
     *
     *  <ul>
     *      <li>Tag = 0</li>
     *      <li>Type = Byte</li>
     *      <li>Count = 4</li>
     *      <li>Default = 2.3.0.0</li>
     *      <ul>
     *          <li>2300 = Version 2.3</li>
     *          <li>Other = reserved</li>
     *      </ul>
     *  </ul>
     */
    public static final String TAG_GPS_VERSION_ID = "GPSVersionID";
    /**
     *  <p>Indicates whether the latitude is north or south latitude.</p>
     *
     *  <ul>
     *      <li>Tag = 1</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #LATITUDE_NORTH
     *  @see #LATITUDE_SOUTH
     */
    public static final String TAG_GPS_LATITUDE_REF = "GPSLatitudeRef";
    /**
     *  <p>Indicates the latitude. The latitude is expressed as three RATIONAL values giving
     *  the degrees, minutes, and seconds, respectively. If latitude is expressed as degrees,
     *  minutes and seconds, a typical format would be dd/1,mm/1,ss/1. When degrees and minutes are
     *  used and, for example, fractions of minutes are given up to two decimal places, the format
     *  would be dd/1,mmmm/100,0/1.</p>
     *
     *  <ul>
     *      <li>Tag = 2</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 3</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_LATITUDE = "GPSLatitude";
    /**
     *  <p>Indicates whether the longitude is east or west longitude.</p>
     *
     *  <ul>
     *      <li>Tag = 3</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #LONGITUDE_EAST
     *  @see #LONGITUDE_WEST
     */
    public static final String TAG_GPS_LONGITUDE_REF = "GPSLongitudeRef";
    /**
     *  <p>Indicates the longitude. The longitude is expressed as three RATIONAL values giving
     *  the degrees, minutes, and seconds, respectively. If longitude is expressed as degrees,
     *  minutes and seconds, a typical format would be ddd/1,mm/1,ss/1. When degrees and minutes
     *  are used and, for example, fractions of minutes are given up to two decimal places,
     *  the format would be ddd/1,mmmm/100,0/1.</p>
     *
     *  <ul>
     *      <li>Tag = 4</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 3</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_LONGITUDE = "GPSLongitude";
    /**
     *  <p>Indicates the altitude used as the reference altitude. If the reference is sea level
     *  and the altitude is above sea level, 0 is given. If the altitude is below sea level,
     *  a value of 1 is given and the altitude is indicated as an absolute value in
     *  {@link #TAG_GPS_ALTITUDE}.</p>
     *
     *  <ul>
     *      <li>Tag = 5</li>
     *      <li>Type = Byte</li>
     *      <li>Count = 1</li>
     *      <li>Default = 0</li>
     *  </ul>
     *
     *  @see #ALTITUDE_ABOVE_SEA_LEVEL
     *  @see #ALTITUDE_BELOW_SEA_LEVEL
     */
    public static final String TAG_GPS_ALTITUDE_REF = "GPSAltitudeRef";
    /**
     *  <p>Indicates the altitude based on the reference in {@link #TAG_GPS_ALTITUDE_REF}.
     *  The reference unit is meters.</p>
     *
     *  <ul>
     *      <li>Tag = 6</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_ALTITUDE = "GPSAltitude";
    /**
     *  <p>Indicates the time as UTC (Coordinated Universal Time). TimeStamp is expressed as three
     *  unsigned rational values giving the hour, minute, and second.</p>
     *
     *  <ul>
     *      <li>Tag = 7</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 3</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_TIMESTAMP = "GPSTimeStamp";
    /**
     *  <p>Indicates the GPS satellites used for measurements. This tag may be used to describe
     *  the number of satellites, their ID number, angle of elevation, azimuth, SNR and other
     *  information in ASCII notation. The format is not specified. If the GPS receiver is incapable
     *  of taking measurements, value of the tag shall be set to {@code null}.</p>
     *
     *  <ul>
     *      <li>Tag = 8</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_SATELLITES = "GPSSatellites";
    /**
     *  <p>Indicates the status of the GPS receiver when the image is recorded. 'A' means
     *  measurement is in progress, and 'V' means the measurement is interrupted.</p>
     *
     *  <ul>
     *      <li>Tag = 9</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #GPS_MEASUREMENT_IN_PROGRESS
     *  @see #GPS_MEASUREMENT_INTERRUPTED
     */
    public static final String TAG_GPS_STATUS = "GPSStatus";
    /**
     *  <p>Indicates the GPS measurement mode. Originally it was defined for GPS, but it may
     *  be used for recording a measure mode to record the position information provided from
     *  a mobile base station or wireless LAN as well as GPS.</p>
     *
     *  <ul>
     *      <li>Tag = 10</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #GPS_MEASUREMENT_2D
     *  @see #GPS_MEASUREMENT_3D
     */
    public static final String TAG_GPS_MEASURE_MODE = "GPSMeasureMode";
    /**
     *  <p>Indicates the GPS DOP (data degree of precision). An HDOP value is written during
     *  two-dimensional measurement, and PDOP during three-dimensional measurement.</p>
     *
     *  <ul>
     *      <li>Tag = 11</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_DOP = "GPSDOP";
    /**
     *  <p>Indicates the unit used to express the GPS receiver speed of movement.</p>
     *
     *  <ul>
     *      <li>Tag = 12</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = {@link #GPS_SPEED_KILOMETERS_PER_HOUR}</li>
     *  </ul>
     *
     *  @see #GPS_SPEED_KILOMETERS_PER_HOUR
     *  @see #GPS_SPEED_MILES_PER_HOUR
     *  @see #GPS_SPEED_KNOTS
     */
    public static final String TAG_GPS_SPEED_REF = "GPSSpeedRef";
    /**
     *  <p>Indicates the speed of GPS receiver movement.</p>
     *
     *  <ul>
     *      <li>Tag = 13</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_SPEED = "GPSSpeed";
    /**
     *  <p>Indicates the reference for giving the direction of GPS receiver movement.</p>
     *
     *  <ul>
     *      <li>Tag = 14</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = {@link #GPS_DIRECTION_TRUE}</li>
     *  </ul>
     *
     *  @see #GPS_DIRECTION_TRUE
     *  @see #GPS_DIRECTION_MAGNETIC
     */
    public static final String TAG_GPS_TRACK_REF = "GPSTrackRef";
    /**
     *  <p>Indicates the direction of GPS receiver movement.
     *  The range of values is from 0.00 to 359.99.</p>
     *
     *  <ul>
     *      <li>Tag = 15</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_TRACK = "GPSTrack";
    /**
     *  <p>Indicates the reference for giving the direction of the image when it is captured.</p>
     *
     *  <ul>
     *      <li>Tag = 16</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = {@link #GPS_DIRECTION_TRUE}</li>
     *  </ul>
     *
     *  @see #GPS_DIRECTION_TRUE
     *  @see #GPS_DIRECTION_MAGNETIC
     */
    public static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";
    /**
     *  <p>ndicates the direction of the image when it was captured.
     *  The range of values is from 0.00 to 359.99.</p>
     *
     *  <ul>
     *      <li>Tag = 17</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
    /**
     *  <p>Indicates the geodetic survey data used by the GPS receiver. If the survey data is
     *  restricted to Japan,the value of this tag is 'TOKYO' or 'WGS-84'. If a GPS Info tag is
     *  recorded, it is strongly recommended that this tag be recorded.</p>
     *
     *  <ul>
     *      <li>Tag = 18</li>
     *      <li>Type = String</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_MAP_DATUM = "GPSMapDatum";
    /**
     *  <p>Indicates whether the latitude of the destination point is north or south latitude.</p>
     *
     *  <ul>
     *      <li>Tag = 19</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #LATITUDE_NORTH
     *  @see #LATITUDE_SOUTH
     */
    public static final String TAG_GPS_DEST_LATITUDE_REF = "GPSDestLatitudeRef";
    /**
     *  <p>Indicates the latitude of the destination point. The latitude is expressed as three
     *  unsigned rational values giving the degrees, minutes, and seconds, respectively.
     *  If latitude is expressed as degrees, minutes and seconds, a typical format would be
     *  dd/1,mm/1,ss/1. When degrees and minutes are used and, for example, fractions of minutes
     *  are given up to two decimal places, the format would be dd/1, mmmm/100, 0/1.</p>
     *
     *  <ul>
     *      <li>Tag = 20</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 3</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_DEST_LATITUDE = "GPSDestLatitude";
    /**
     *  <p>Indicates whether the longitude of the destination point is east or west longitude.</p>
     *
     *  <ul>
     *      <li>Tag = 21</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #LONGITUDE_EAST
     *  @see #LONGITUDE_WEST
     */
    public static final String TAG_GPS_DEST_LONGITUDE_REF = "GPSDestLongitudeRef";
    /**
     *  <p>Indicates the longitude of the destination point. The longitude is expressed as three
     *  unsigned rational values giving the degrees, minutes, and seconds, respectively.
     *  If longitude is expressed as degrees, minutes and seconds, a typical format would be ddd/1,
     *  mm/1, ss/1. When degrees and minutes are used and, for example, fractions of minutes are
     *  given up to two decimal places, the format would be ddd/1, mmmm/100, 0/1.</p>
     *
     *  <ul>
     *      <li>Tag = 22</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 3</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_DEST_LONGITUDE = "GPSDestLongitude";
    /**
     *  <p>Indicates the reference used for giving the bearing to the destination point.</p>
     *
     *  <ul>
     *      <li>Tag = 23</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = {@link #GPS_DIRECTION_TRUE}</li>
     *  </ul>
     *
     *  @see #GPS_DIRECTION_TRUE
     *  @see #GPS_DIRECTION_MAGNETIC
     */
    public static final String TAG_GPS_DEST_BEARING_REF = "GPSDestBearingRef";
    /**
     *  <p>Indicates the bearing to the destination point.
     *  The range of values is from 0.00 to 359.99.</p>
     *
     *  <ul>
     *      <li>Tag = 24</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_DEST_BEARING = "GPSDestBearing";
    /**
     *  <p>Indicates the unit used to express the distance to the destination point.</p>
     *
     *  <ul>
     *      <li>Tag = 25</li>
     *      <li>Type = String</li>
     *      <li>Length = 1</li>
     *      <li>Default = {@link #GPS_DISTANCE_KILOMETERS}</li>
     *  </ul>
     *
     *  @see #GPS_DISTANCE_KILOMETERS
     *  @see #GPS_DISTANCE_MILES
     *  @see #GPS_DISTANCE_NAUTICAL_MILES
     */
    public static final String TAG_GPS_DEST_DISTANCE_REF = "GPSDestDistanceRef";
    /**
     *  <p>Indicates the distance to the destination point.</p>
     *
     *  <ul>
     *      <li>Tag = 26</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_DEST_DISTANCE = "GPSDestDistance";
    /**
     *  <p>A character string recording the name of the method used for location finding.
     *  The first byte indicates the character code used, and this is followed by the name of
     *  the method.</p>
     *
     *  <ul>
     *      <li>Tag = 27</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_PROCESSING_METHOD = "GPSProcessingMethod";
    /**
     *  <p>A character string recording the name of the GPS area. The first byte indicates
     *  the character code used, and this is followed by the name of the GPS area.</p>
     *
     *  <ul>
     *      <li>Tag = 28</li>
     *      <li>Type = Undefined</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_AREA_INFORMATION = "GPSAreaInformation";
    /**
     *  <p>A character string recording date and time information relative to UTC (Coordinated
     *  Universal Time). The format is "YYYY:MM:DD".</p>
     *
     *  <ul>
     *      <li>Tag = 29</li>
     *      <li>Type = String</li>
     *      <li>Length = 10</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_DATESTAMP = "GPSDateStamp";
    /**
     *  <p>Indicates whether differential correction is applied to the GPS receiver.</p>
     *
     *  <ul>
     *      <li>Tag = 30</li>
     *      <li>Type = Unsigned short</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     *
     *  @see #GPS_MEASUREMENT_NO_DIFFERENTIAL
     *  @see #GPS_MEASUREMENT_DIFFERENTIAL_CORRECTED
     */
    public static final String TAG_GPS_DIFFERENTIAL = "GPSDifferential";
    /**
     *  <p>This tag indicates horizontal positioning errors in meters.</p>
     *
     *  <ul>
     *      <li>Tag = 31</li>
     *      <li>Type = Unsigned rational</li>
     *      <li>Count = 1</li>
     *      <li>Default = None</li>
     *  </ul>
     */
    public static final String TAG_GPS_H_POSITIONING_ERROR = "GPSHPositioningError";

    // Interoperability IFD Attribute Information
    /**
     *  <p>Indicates the identification of the Interoperability rule.</p>
     *
     *  <ul>
     *      <li>Tag = 1</li>
     *      <li>Type = String</li>
     *      <li>Length = 4</li>
     *      <li>Default = None</li>
     *      <ul>
     *          <li>"R98" = Indicates a file conforming to R98 file specification of Recommended
     *                      Exif Interoperability Rules (Exif R 98) or to DCF basic file stipulated
     *                      by Design Rule for Camera File System.</li>
     *          <li>"THM" = Indicates a file conforming to DCF thumbnail file stipulated by Design
     *                      rule for Camera File System.</li>
     *          <li>“R03” = Indicates a file conforming to DCF Option File stipulated by Design rule
     *                      for Camera File System.</li>
     *      </ul>
     *  </ul>
     */
    public static final String TAG_INTEROPERABILITY_INDEX = "InteroperabilityIndex";

    /**
     * @see #TAG_IMAGE_LENGTH
     */
    public static final String TAG_THUMBNAIL_IMAGE_LENGTH = "ThumbnailImageLength";
    /**
     * @see #TAG_IMAGE_WIDTH
     */
    public static final String TAG_THUMBNAIL_IMAGE_WIDTH = "ThumbnailImageWidth";
    /** Type is int. DNG Specification 1.4.0.0. Section 4 */
    public static final String TAG_DNG_VERSION = "DNGVersion";
    /** Type is int. DNG Specification 1.4.0.0. Section 4 */
    public static final String TAG_DEFAULT_CROP_SIZE = "DefaultCropSize";
    /** Type is undefined. See Olympus MakerNote tags in http://www.exiv2.org/tags-olympus.html. */
    public static final String TAG_ORF_THUMBNAIL_IMAGE = "ThumbnailImage";
    /** Type is int. See Olympus Camera Settings tags in http://www.exiv2.org/tags-olympus.html. */
    public static final String TAG_ORF_PREVIEW_IMAGE_START = "PreviewImageStart";
    /** Type is int. See Olympus Camera Settings tags in http://www.exiv2.org/tags-olympus.html. */
    public static final String TAG_ORF_PREVIEW_IMAGE_LENGTH = "PreviewImageLength";
    /** Type is int. See Olympus Image Processing tags in http://www.exiv2.org/tags-olympus.html. */
    public static final String TAG_ORF_ASPECT_FRAME = "AspectFrame";
    /**
     * Type is int. See PanasonicRaw tags in
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PanasonicRaw.html
     */
    public static final String TAG_RW2_SENSOR_BOTTOM_BORDER = "SensorBottomBorder";
    /**
     * Type is int. See PanasonicRaw tags in
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PanasonicRaw.html
     */
    public static final String TAG_RW2_SENSOR_LEFT_BORDER = "SensorLeftBorder";
    /**
     * Type is int. See PanasonicRaw tags in
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PanasonicRaw.html
     */
    public static final String TAG_RW2_SENSOR_RIGHT_BORDER = "SensorRightBorder";
    /**
     * Type is int. See PanasonicRaw tags in
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PanasonicRaw.html
     */
    public static final String TAG_RW2_SENSOR_TOP_BORDER = "SensorTopBorder";
    /**
     * Type is int. See PanasonicRaw tags in
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PanasonicRaw.html
     */
    public static final String TAG_RW2_ISO = "ISO";
    /**
     * Type is undefined. See PanasonicRaw tags in
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PanasonicRaw.html
     */
    public static final String TAG_RW2_JPG_FROM_RAW = "JpgFromRaw";
    /** Type is int. See JEITA CP-3451C Spec Section 3: Bilevel Images. */
    public static final String TAG_NEW_SUBFILE_TYPE = "NewSubfileType";
    /** Type is int. See JEITA CP-3451C Spec Section 3: Bilevel Images. */
    public static final String TAG_SUBFILE_TYPE = "SubfileType";

    /**
     * Private tags used for pointing the other IFD offsets.
     * The types of the following tags are int.
     * See JEITA CP-3451C Section 4.6.3: Exif-specific IFD.
     * For SubIFD, see Note 1 of Adobe PageMaker® 6.0 TIFF Technical Notes.
     */
    private static final String TAG_EXIF_IFD_POINTER = "ExifIFDPointer";
    private static final String TAG_GPS_INFO_IFD_POINTER = "GPSInfoIFDPointer";
    private static final String TAG_INTEROPERABILITY_IFD_POINTER = "InteroperabilityIFDPointer";
    private static final String TAG_SUB_IFD_POINTER = "SubIFDPointer";
    // Proprietary pointer tags used for ORF files.
    // See http://www.exiv2.org/tags-olympus.html
    private static final String TAG_ORF_CAMERA_SETTINGS_IFD_POINTER = "CameraSettingsIFDPointer";
    private static final String TAG_ORF_IMAGE_PROCESSING_IFD_POINTER = "ImageProcessingIFDPointer";

    // Private tags used for thumbnail information.
    private static final String TAG_HAS_THUMBNAIL = "HasThumbnail";
    private static final String TAG_THUMBNAIL_OFFSET = "ThumbnailOffset";
    private static final String TAG_THUMBNAIL_LENGTH = "ThumbnailLength";
    private static final String TAG_THUMBNAIL_DATA = "ThumbnailData";
    private static final int MAX_THUMBNAIL_SIZE = 512;

    // Constants used for the Orientation Exif tag.
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_NORMAL = 1;
    /**
     * Indicates the image is left right reversed mirror.
     */
    public static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    /**
     * Indicates the image is rotated by 180 degree clockwise.
     */
    public static final int ORIENTATION_ROTATE_180 = 3;
    /**
     * Indicates the image is upside down mirror, it can also be represented by flip
     * horizontally firstly and rotate 180 degree clockwise.
     */
    public static final int ORIENTATION_FLIP_VERTICAL = 4;
    /**
     * Indicates the image is flipped about top-left <--> bottom-right axis, it can also be
     * represented by flip horizontally firstly and rotate 270 degree clockwise.
     */
    public static final int ORIENTATION_TRANSPOSE = 5;
    /**
     * Indicates the image is rotated by 90 degree clockwise.
     */
    public static final int ORIENTATION_ROTATE_90 = 6;
    /**
     * Indicates the image is flipped about top-right <--> bottom-left axis, it can also be
     * represented by flip horizontally firstly and rotate 90 degree clockwise.
     */
    public static final int ORIENTATION_TRANSVERSE = 7;
    /**
     * Indicates the image is rotated by 270 degree clockwise.
     */
    public static final int ORIENTATION_ROTATE_270 = 8;
    private static final List<Integer> ROTATION_ORDER = Arrays.asList(ORIENTATION_NORMAL,
            ORIENTATION_ROTATE_90, ORIENTATION_ROTATE_180, ORIENTATION_ROTATE_270);
    private static final List<Integer> FLIPPED_ROTATION_ORDER = Arrays.asList(
            ORIENTATION_FLIP_HORIZONTAL, ORIENTATION_TRANSVERSE, ORIENTATION_FLIP_VERTICAL,
            ORIENTATION_TRANSPOSE);

    /**
     * The contant used by {@link #TAG_PLANAR_CONFIGURATION} to denote Chunky format.
     */
    public static final short FORMAT_CHUNKY = 1;
    /**
     * The contant used by {@link #TAG_PLANAR_CONFIGURATION} to denote Planar format.
     */
    public static final short FORMAT_PLANAR = 2;

    /**
     * The contant used by {@link #TAG_Y_CB_CR_POSITIONING} to denote Centered positioning.
     */
    public static final short Y_CB_CR_POSITIONING_CENTERED = 1;
    /**
     * The contant used by {@link #TAG_Y_CB_CR_POSITIONING} to denote Co-sited positioning.
     */
    public static final short Y_CB_CR_POSITIONING_CO_SITED = 2;

    /**
     * The contant used to denote resolution unit as inches.
     */
    public static final short RESOLUTION_UNIT_INCHES = 2;
    /**
     * The contant used to denote resolution unit as centimeters.
     */
    public static final short RESOLUTION_UNIT_CENTIMETERS = 3;

    /**
     * The contant used by {@link #TAG_COLOR_SPACE} to denote sRGB color space.
     */
    public static final int COLOR_SPACE_S_RGB = 1;
    /**
     * The contant used by {@link #TAG_COLOR_SPACE} to denote Uncalibrated.
     */
    public static final int COLOR_SPACE_UNCALIBRATED = 65535;

    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is not defined.
     */
    public static final short EXPOSURE_PROGRAM_NOT_DEFINED = 0;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is Manual.
     */
    public static final short EXPOSURE_PROGRAM_MANUAL = 1;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is Normal.
     */
    public static final short EXPOSURE_PROGRAM_NORMAL = 2;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is
     * Aperture priority.
     */
    public static final short EXPOSURE_PROGRAM_APERTURE_PRIORITY = 3;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is
     * Shutter priority.
     */
    public static final short EXPOSURE_PROGRAM_SHUTTER_PRIORITY = 4;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is Creative
     * program (biased toward depth of field).
     */
    public static final short EXPOSURE_PROGRAM_CREATIVE = 5;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is Action
     * program (biased toward fast shutter speed).
     */
    public static final short EXPOSURE_PROGRAM_ACTION = 6;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is Portrait mode
     * (for closeup photos with the background out of focus).
     */
    public static final short EXPOSURE_PROGRAM_PORTRAIT_MODE = 7;
    /**
     * The contant used by {@link #TAG_EXPOSURE_PROGRAM} to denote exposure program is Landscape
     * mode (for landscape photos with the background in focus).
     */
    public static final short EXPOSURE_PROGRAM_LANDSCAPE_MODE = 8;

    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is unknown.
     */
    public static final short SENSITIVITY_TYPE_UNKNOWN = 0;
    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is Standard
     * output sensitivity (SOS).
     */
    public static final short SENSITIVITY_TYPE_SOS = 1;
    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is Recommended
     * exposure index (REI).
     */
    public static final short SENSITIVITY_TYPE_REI = 2;
    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is ISO speed.
     */
    public static final short SENSITIVITY_TYPE_ISO_SPEED = 3;
    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is Standard
     * output sensitivity (SOS) and recommended exposure index (REI).
     */
    public static final short SENSITIVITY_TYPE_SOS_AND_REI = 4;
    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is Standard
     * output sensitivity (SOS) and ISO speed.
     */
    public static final short SENSITIVITY_TYPE_SOS_AND_ISO = 5;
    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is Recommended
     * exposure index (REI) and ISO speed.
     */
    public static final short SENSITIVITY_TYPE_REI_AND_ISO = 6;
    /**
     * The contant used by {@link #TAG_SENSITIVITY_TYPE} to denote sensitivity type is Standard
     * output sensitivity (SOS) and recommended exposure index (REI) and ISO speed.
     */
    public static final short SENSITIVITY_TYPE_SOS_AND_REI_AND_ISO = 7;

    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is unknown.
     */
    public static final short METERING_MODE_UNKNOWN = 0;
    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is Average.
     */
    public static final short METERING_MODE_AVERAGE = 1;
    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is
     * CenterWeightedAverage.
     */
    public static final short METERING_MODE_CENTER_WEIGHT_AVERAGE = 2;
    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is Spot.
     */
    public static final short METERING_MODE_SPOT = 3;
    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is MultiSpot.
     */
    public static final short METERING_MODE_MULTI_SPOT = 4;
    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is Pattern.
     */
    public static final short METERING_MODE_PATTERN = 5;
    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is Partial.
     */
    public static final short METERING_MODE_PARTIAL = 6;
    /**
     * The contant used by {@link #TAG_METERING_MODE} to denote metering mode is other.
     */
    public static final short METERING_MODE_OTHER = 255;

    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is unknown.
     */
    public static final short LIGHT_SOURCE_UNKNOWN = 0;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Daylight.
     */
    public static final short LIGHT_SOURCE_DAYLIGHT = 1;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Fluorescent.
     */
    public static final short LIGHT_SOURCE_FLUORESCENT = 2;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Tungsten
     * (incandescent light).
     */
    public static final short LIGHT_SOURCE_TUNGSTEN = 3;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Flash.
     */
    public static final short LIGHT_SOURCE_FLASH = 4;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Fine weather.
     */
    public static final short LIGHT_SOURCE_FINE_WEATHER = 9;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Cloudy weather.
     */
    public static final short LIGHT_SOURCE_CLOUDY_WEATHER = 10;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Shade.
     */
    public static final short LIGHT_SOURCE_SHADE = 11;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Daylight fluorescent
     * (D 5700 - 7100K).
     */
    public static final short LIGHT_SOURCE_DAYLIGHT_FLUORESCENT = 12;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Day white fluorescent
     * (N 4600 - 5500K).
     */
    public static final short LIGHT_SOURCE_DAY_WHITE_FLUORESCENT = 13;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Cool white
     * fluorescent (W 3800 - 4500K).
     */
    public static final short LIGHT_SOURCE_COOL_WHITE_FLUORESCENT = 14;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is White fluorescent
     * (WW 3250 - 3800K).
     */
    public static final short LIGHT_SOURCE_WHITE_FLUORESCENT = 15;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Warm white
     * fluorescent (L 2600 - 3250K).
     */
    public static final short LIGHT_SOURCE_WARM_WHITE_FLUORESCENT = 16;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Standard light A.
     */
    public static final short LIGHT_SOURCE_STANDARD_LIGHT_A = 17;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Standard light B.
     */
    public static final short LIGHT_SOURCE_STANDARD_LIGHT_B = 18;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is Standard light C.
     */
    public static final short LIGHT_SOURCE_STANDARD_LIGHT_C = 19;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is D55.
     */
    public static final short LIGHT_SOURCE_D55 = 20;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is D65.
     */
    public static final short LIGHT_SOURCE_D65 = 21;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is D75.
     */
    public static final short LIGHT_SOURCE_D75 = 22;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is D50.
     */
    public static final short LIGHT_SOURCE_D50 = 23;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is ISO studio tungsten.
     */
    public static final short LIGHT_SOURCE_ISO_STUDIO_TUNGSTEN = 24;
    /**
     * The contant used by {@link #TAG_LIGHT_SOURCE} to denote light source is other.
     */
    public static final short LIGHT_SOURCE_OTHER = 255;

    /**
     * The flag used by {@link #TAG_FLASH} to indicate whether the flash is fired.
     */
    public static final short FLAG_FLASH_FIRED = 0b0000_0001;
    /**
     * The flag used by {@link #TAG_FLASH} to indicate strobe return light is not detected.
     */
    public static final short FLAG_FLASH_RETURN_LIGHT_NOT_DETECTED = 0b0000_0100;
    /**
     * The flag used by {@link #TAG_FLASH} to indicate strobe return light is detected.
     */
    public static final short FLAG_FLASH_RETURN_LIGHT_DETECTED = 0b0000_0110;
    /**
     * The flag used by {@link #TAG_FLASH} to indicate the camera's flash mode is Compulsory flash
     * firing.
     *
     * @see #FLAG_FLASH_MODE_COMPULSORY_SUPPRESSION
     * @see #FLAG_FLASH_MODE_AUTO
     */
    public static final short FLAG_FLASH_MODE_COMPULSORY_FIRING = 0b0000_1000;
    /**
     * The flag used by {@link #TAG_FLASH} to indicate the camera's flash mode is Compulsory flash
     * suppression.
     *
     * @see #FLAG_FLASH_MODE_COMPULSORY_FIRING
     * @see #FLAG_FLASH_MODE_AUTO
     */
    public static final short FLAG_FLASH_MODE_COMPULSORY_SUPPRESSION = 0b0001_0000;
    /**
     * The flag used by {@link #TAG_FLASH} to indicate the camera's flash mode is Auto.
     *
     * @see #FLAG_FLASH_MODE_COMPULSORY_FIRING
     * @see #FLAG_FLASH_MODE_COMPULSORY_SUPPRESSION
     */
    public static final short FLAG_FLASH_MODE_AUTO = 0b0001_1000;
    /**
     * The flag used by {@link #TAG_FLASH} to indicate no flash function is present.
     */
    public static final short FLAG_FLASH_NO_FLASH_FUNCTION = 0b0010_0000;
    /**
     * The flag used by {@link #TAG_FLASH} to indicate red-eye reduction is supported.
     */
    public static final short FLAG_FLASH_RED_EYE_SUPPORTED = 0b0100_0000;

    /**
     * The contant used by {@link #TAG_SENSING_METHOD} to denote the image sensor type is not
     * defined.
     */
    public static final short SENSOR_TYPE_NOT_DEFINED = 1;
    /**
     * The contant used by {@link #TAG_SENSING_METHOD} to denote the image sensor type is One-chip
     * color area sensor.
     */
    public static final short SENSOR_TYPE_ONE_CHIP = 2;
    /**
     * The contant used by {@link #TAG_SENSING_METHOD} to denote the image sensor type is Two-chip
     * color area sensor.
     */
    public static final short SENSOR_TYPE_TWO_CHIP = 3;
    /**
     * The contant used by {@link #TAG_SENSING_METHOD} to denote the image sensor type is Three-chip
     * color area sensor.
     */
    public static final short SENSOR_TYPE_THREE_CHIP = 4;
    /**
     * The contant used by {@link #TAG_SENSING_METHOD} to denote the image sensor type is Color
     * sequential area sensor.
     */
    public static final short SENSOR_TYPE_COLOR_SEQUENTIAL = 5;
    /**
     * The contant used by {@link #TAG_SENSING_METHOD} to denote the image sensor type is Trilinear
     * sensor.
     */
    public static final short SENSOR_TYPE_TRILINEAR = 7;
    /**
     * The contant used by {@link #TAG_SENSING_METHOD} to denote the image sensor type is Color
     * sequential linear sensor.
     */
    public static final short SENSOR_TYPE_COLOR_SEQUENTIAL_LINEAR = 8;

    /**
     * The contant used by {@link #TAG_FILE_SOURCE} to denote the source is other.
     */
    public static final short FILE_SOURCE_OTHER = 0;
    /**
     * The contant used by {@link #TAG_FILE_SOURCE} to denote the source is scanner of transparent
     * type.
     */
    public static final short FILE_SOURCE_TRANSPARENT_SCANNER = 1;
    /**
     * The contant used by {@link #TAG_FILE_SOURCE} to denote the source is scanner of reflex type.
     */
    public static final short FILE_SOURCE_REFLEX_SCANNER = 2;
    /**
     * The contant used by {@link #TAG_FILE_SOURCE} to denote the source is DSC.
     */
    public static final short FILE_SOURCE_DSC = 3;

    /**
     * The contant used by {@link #TAG_SCENE_TYPE} to denote the scene is directly photographed.
     */
    public static final short SCENE_TYPE_DIRECTLY_PHOTOGRAPHED = 1;

    /**
     * The contant used by {@link #TAG_CUSTOM_RENDERED} to denote no special processing is used.
     */
    public static final short RENDERED_PROCESS_NORMAL = 0;
    /**
     * The contant used by {@link #TAG_CUSTOM_RENDERED} to denote special processing is used.
     */
    public static final short RENDERED_PROCESS_CUSTOM = 1;

    /**
     * The contant used by {@link #TAG_EXPOSURE_MODE} to denote the exposure mode is Auto.
     */
    public static final short EXPOSURE_MODE_AUTO = 0;
    /**
     * The contant used by {@link #TAG_EXPOSURE_MODE} to denote the exposure mode is Manual.
     */
    public static final short EXPOSURE_MODE_MANUAL = 1;
    /**
     * The contant used by {@link #TAG_EXPOSURE_MODE} to denote the exposure mode is Auto bracket.
     */
    public static final short EXPOSURE_MODE_AUTO_BRACKET = 2;

    /**
     * The contant used by {@link #TAG_WHITE_BALANCE} to denote the white balance is Auto.
     *
     * @deprecated Use {@link #WHITE_BALANCE_AUTO} instead.
     */
    @Deprecated public static final int WHITEBALANCE_AUTO = 0;
    /**
     * The contant used by {@link #TAG_WHITE_BALANCE} to denote the white balance is Manual.
     *
     * @deprecated Use {@link #WHITE_BALANCE_MANUAL} instead.
     */
    @Deprecated public static final int WHITEBALANCE_MANUAL = 1;
    /**
     * The contant used by {@link #TAG_WHITE_BALANCE} to denote the white balance is Auto.
     */
    public static final short WHITE_BALANCE_AUTO = 0;
    /**
     * The contant used by {@link #TAG_WHITE_BALANCE} to denote the white balance is Manual.
     */
    public static final short WHITE_BALANCE_MANUAL = 1;

    /**
     * The contant used by {@link #TAG_SCENE_CAPTURE_TYPE} to denote the scene capture type is
     * Standard.
     */
    public static final short SCENE_CAPTURE_TYPE_STANDARD = 0;
    /**
     * The contant used by {@link #TAG_SCENE_CAPTURE_TYPE} to denote the scene capture type is
     * Landscape.
     */
    public static final short SCENE_CAPTURE_TYPE_LANDSCAPE = 1;
    /**
     * The contant used by {@link #TAG_SCENE_CAPTURE_TYPE} to denote the scene capture type is
     * Portrait.
     */
    public static final short SCENE_CAPTURE_TYPE_PORTRAIT = 2;
    /**
     * The contant used by {@link #TAG_SCENE_CAPTURE_TYPE} to denote the scene capture type is Night
     * scene.
     */
    public static final short SCENE_CAPTURE_TYPE_NIGHT = 3;

    /**
     * The contant used by {@link #TAG_GAIN_CONTROL} to denote none gain adjustment.
     */
    public static final short GAIN_CONTROL_NONE = 0;
    /**
     * The contant used by {@link #TAG_GAIN_CONTROL} to denote low gain up.
     */
    public static final short GAIN_CONTROL_LOW_GAIN_UP = 1;
    /**
     * The contant used by {@link #TAG_GAIN_CONTROL} to denote high gain up.
     */
    public static final short GAIN_CONTROL_HIGH_GAIN_UP = 2;
    /**
     * The contant used by {@link #TAG_GAIN_CONTROL} to denote low gain down.
     */
    public static final short GAIN_CONTROL_LOW_GAIN_DOWN = 3;
    /**
     * The contant used by {@link #TAG_GAIN_CONTROL} to denote high gain down.
     */
    public static final short GAIN_CONTROL_HIGH_GAIN_DOWN = 4;

    /**
     * The contant used by {@link #TAG_CONTRAST} to denote normal contrast.
     */
    public static final short CONTRAST_NORMAL = 0;
    /**
     * The contant used by {@link #TAG_CONTRAST} to denote soft contrast.
     */
    public static final short CONTRAST_SOFT = 1;
    /**
     * The contant used by {@link #TAG_CONTRAST} to denote hard contrast.
     */
    public static final short CONTRAST_HARD = 2;

    /**
     * The contant used by {@link #TAG_SATURATION} to denote normal saturation.
     */
    public static final short SATURATION_NORMAL = 0;
    /**
     * The contant used by {@link #TAG_SATURATION} to denote low saturation.
     */
    public static final short SATURATION_LOW = 0;
    /**
     * The contant used by {@link #TAG_SHARPNESS} to denote high saturation.
     */
    public static final short SATURATION_HIGH = 0;

    /**
     * The contant used by {@link #TAG_SHARPNESS} to denote normal sharpness.
     */
    public static final short SHARPNESS_NORMAL = 0;
    /**
     * The contant used by {@link #TAG_SHARPNESS} to denote soft sharpness.
     */
    public static final short SHARPNESS_SOFT = 1;
    /**
     * The contant used by {@link #TAG_SHARPNESS} to denote hard sharpness.
     */
    public static final short SHARPNESS_HARD = 2;

    /**
     * The contant used by {@link #TAG_SUBJECT_DISTANCE_RANGE} to denote the subject distance range
     * is unknown.
     */
    public static final short SUBJECT_DISTANCE_RANGE_UNKNOWN = 0;
    /**
     * The contant used by {@link #TAG_SUBJECT_DISTANCE_RANGE} to denote the subject distance range
     * is Macro.
     */
    public static final short SUBJECT_DISTANCE_RANGE_MACRO = 1;
    /**
     * The contant used by {@link #TAG_SUBJECT_DISTANCE_RANGE} to denote the subject distance range
     * is Close view.
     */
    public static final short SUBJECT_DISTANCE_RANGE_CLOSE_VIEW = 2;
    /**
     * The contant used by {@link #TAG_SUBJECT_DISTANCE_RANGE} to denote the subject distance range
     * is Distant view.
     */
    public static final short SUBJECT_DISTANCE_RANGE_DISTANT_VIEW = 3;

    /**
     * The contant used by GPS latitude-related tags to denote the latitude is North latitude.
     *
     * @see #TAG_GPS_LATITUDE_REF
     * @see #TAG_GPS_DEST_LATITUDE_REF
     */
    public static final String LATITUDE_NORTH = "N";
    /**
     * The contant used by GPS latitude-related tags to denote the latitude is South latitude.
     *
     * @see #TAG_GPS_LATITUDE_REF
     * @see #TAG_GPS_DEST_LATITUDE_REF
     */
    public static final String LATITUDE_SOUTH = "S";

    /**
     * The contant used by GPS longitude-related tags to denote the longitude is East longitude.
     *
     * @see #TAG_GPS_LONGITUDE_REF
     * @see #TAG_GPS_DEST_LONGITUDE_REF
     */
    public static final String LONGITUDE_EAST = "E";
    /**
     * The contant used by GPS longitude-related tags to denote the longitude is West longitude.
     *
     * @see #TAG_GPS_LONGITUDE_REF
     * @see #TAG_GPS_DEST_LONGITUDE_REF
     */
    public static final String LONGITUDE_WEST = "W";

    /**
     * The contant used by {@link #TAG_GPS_ALTITUDE_REF} to denote the altitude is above sea level.
     */
    public static final short ALTITUDE_ABOVE_SEA_LEVEL = 0;
    /**
     * The contant used by {@link #TAG_GPS_ALTITUDE_REF} to denote the altitude is below sea level.
     */
    public static final short ALTITUDE_BELOW_SEA_LEVEL = 1;

    /**
     * The contant used by {@link #TAG_GPS_STATUS} to denote GPS measurement is in progress.
     */
    public static final String GPS_MEASUREMENT_IN_PROGRESS = "A";
    /**
     * The contant used by {@link #TAG_GPS_STATUS} to denote GPS measurement is interrupted.
     */
    public static final String GPS_MEASUREMENT_INTERRUPTED = "V";

    /**
     * The contant used by {@link #TAG_GPS_MEASURE_MODE} to denote GPS measurement is 2-dimensional.
     */
    public static final String GPS_MEASUREMENT_2D = "2";
    /**
     * The contant used by {@link #TAG_GPS_MEASURE_MODE} to denote GPS measurement is 3-dimensional.
     */
    public static final String GPS_MEASUREMENT_3D = "3";

    /**
     * The contant used by {@link #TAG_GPS_SPEED_REF} to denote the speed unit is kilometers per
     * hour.
     */
    public static final String GPS_SPEED_KILOMETERS_PER_HOUR = "K";
    /**
     * The contant used by {@link #TAG_GPS_SPEED_REF} to denote the speed unit is miles per hour.
     */
    public static final String GPS_SPEED_MILES_PER_HOUR = "M";
    /**
     * The contant used by {@link #TAG_GPS_SPEED_REF} to denote the speed unit is knots.
     */
    public static final String GPS_SPEED_KNOTS = "N";

    /**
     * The contant used by GPS attributes to denote the direction is true direction.
     */
    public static final String GPS_DIRECTION_TRUE = "T";
    /**
     * The contant used by GPS attributes to denote the direction is magnetic direction.
     */
    public static final String GPS_DIRECTION_MAGNETIC = "M";

    /**
     * The contant used by {@link #TAG_GPS_DEST_DISTANCE_REF} to denote the distance unit is
     * kilometers.
     */
    public static final String GPS_DISTANCE_KILOMETERS = "K";
    /**
     * The contant used by {@link #TAG_GPS_DEST_DISTANCE_REF} to denote the distance unit is miles.
     */
    public static final String GPS_DISTANCE_MILES = "M";
    /**
     * The contant used by {@link #TAG_GPS_DEST_DISTANCE_REF} to denote the distance unit is
     * nautical miles.
     */
    public static final String GPS_DISTANCE_NAUTICAL_MILES = "N";

    /**
     * The contant used by {@link #TAG_GPS_DIFFERENTIAL} to denote no differential correction is
     * applied.
     */
    public static final short GPS_MEASUREMENT_NO_DIFFERENTIAL = 0;
    /**
     * The contant used by {@link #TAG_GPS_DIFFERENTIAL} to denote differential correction is
     * applied.
     */
    public static final short GPS_MEASUREMENT_DIFFERENTIAL_CORRECTED = 1;

    /**
     * The constant used by {@link #TAG_COMPRESSION} to denote the image is not compressed.
     */
    public static final int DATA_UNCOMPRESSED = 1;
    /**
     * The constant used by {@link #TAG_COMPRESSION} to denote the image is huffman compressed.
     */
    public static final int DATA_HUFFMAN_COMPRESSED = 2;
    /**
     * The constant used by {@link #TAG_COMPRESSION} to denote the image is JPEG.
     */
    public static final int DATA_JPEG = 6;
    /**
     * The constant used by {@link #TAG_COMPRESSION}, see DNG Specification 1.4.0.0.
     * Section 3, Compression
     */
    public static final int DATA_JPEG_COMPRESSED = 7;
    /**
     * The constant used by {@link #TAG_COMPRESSION}, see DNG Specification 1.4.0.0.
     * Section 3, Compression
     */
    public static final int DATA_DEFLATE_ZIP = 8;
    /**
     * The constant used by {@link #TAG_COMPRESSION} to denote the image is pack-bits compressed.
     */
    public static final int DATA_PACK_BITS_COMPRESSED = 32773;
    /**
     * The constant used by {@link #TAG_COMPRESSION}, see DNG Specification 1.4.0.0.
     * Section 3, Compression
     */
    public static final int DATA_LOSSY_JPEG = 34892;

    /**
     * The constant used by {@link #TAG_BITS_PER_SAMPLE}.
     * See JEITA CP-3451C Spec Section 6, Differences from Palette Color Images
     */
    public static final int[] BITS_PER_SAMPLE_RGB = new int[] { 8, 8, 8 };
    /**
     * The constant used by {@link #TAG_BITS_PER_SAMPLE}.
     * See JEITA CP-3451C Spec Section 4, Differences from Bilevel Images
     */
    public static final int[] BITS_PER_SAMPLE_GREYSCALE_1 = new int[] { 4 };
    /**
     * The constant used by {@link #TAG_BITS_PER_SAMPLE}.
     * See JEITA CP-3451C Spec Section 4, Differences from Bilevel Images
     */
    public static final int[] BITS_PER_SAMPLE_GREYSCALE_2 = new int[] { 8 };

    /**
     * The constant used by {@link #TAG_PHOTOMETRIC_INTERPRETATION}.
     */
    public static final int PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO = 0;
    /**
     * The constant used by {@link #TAG_PHOTOMETRIC_INTERPRETATION}.
     */
    public static final int PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO = 1;
    /**
     * The constant used by {@link #TAG_PHOTOMETRIC_INTERPRETATION}.
     */
    public static final int PHOTOMETRIC_INTERPRETATION_RGB = 2;
    /**
     * The constant used by {@link #TAG_PHOTOMETRIC_INTERPRETATION}.
     */
    public static final int PHOTOMETRIC_INTERPRETATION_YCBCR = 6;

    /**
     * The constant used by {@link #TAG_NEW_SUBFILE_TYPE}. See JEITA CP-3451C Spec Section 8.
     */
    public static final int ORIGINAL_RESOLUTION_IMAGE = 0;
    /**
     * The constant used by {@link #TAG_NEW_SUBFILE_TYPE}. See JEITA CP-3451C Spec Section 8.
     */
    public static final int REDUCED_RESOLUTION_IMAGE = 1;

    // Maximum size for checking file type signature (see image_type_recognition_lite.cc)
    private static final int SIGNATURE_CHECK_SIZE = 5000;

    static final byte[] JPEG_SIGNATURE = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
    private static final String RAF_SIGNATURE = "FUJIFILMCCD-RAW";
    private static final int RAF_OFFSET_TO_JPEG_IMAGE_OFFSET = 84;
    private static final int RAF_INFO_SIZE = 160;
    private static final int RAF_JPEG_LENGTH_VALUE_SIZE = 4;

    // See http://fileformats.archiveteam.org/wiki/Olympus_ORF
    private static final short ORF_SIGNATURE_1 = 0x4f52;
    private static final short ORF_SIGNATURE_2 = 0x5352;
    // There are two formats for Olympus Makernote Headers. Each has different identifiers and
    // offsets to the actual data.
    // See http://www.exiv2.org/makernote.html#R1
    private static final byte[] ORF_MAKER_NOTE_HEADER_1 = new byte[] {(byte) 0x4f, (byte) 0x4c,
            (byte) 0x59, (byte) 0x4d, (byte) 0x50, (byte) 0x00}; // "OLYMP\0"
    private static final byte[] ORF_MAKER_NOTE_HEADER_2 = new byte[] {(byte) 0x4f, (byte) 0x4c,
            (byte) 0x59, (byte) 0x4d, (byte) 0x50, (byte) 0x55, (byte) 0x53, (byte) 0x00,
            (byte) 0x49, (byte) 0x49}; // "OLYMPUS\0II"
    private static final int ORF_MAKER_NOTE_HEADER_1_SIZE = 8;
    private static final int ORF_MAKER_NOTE_HEADER_2_SIZE = 12;

    // See http://fileformats.archiveteam.org/wiki/RW2
    private static final short RW2_SIGNATURE = 0x0055;

    // See http://fileformats.archiveteam.org/wiki/Pentax_PEF
    private static final String PEF_SIGNATURE = "PENTAX";
    // See http://www.exiv2.org/makernote.html#R11
    private static final int PEF_MAKER_NOTE_SKIP_SIZE = 6;

    private static SimpleDateFormat sFormatter;

    // See Exchangeable image file format for digital still cameras: Exif version 2.2.
    // The following values are for parsing EXIF data area. There are tag groups in EXIF data area.
    // They are called "Image File Directory". They have multiple data formats to cover various
    // image metadata from GPS longitude to camera model name.

    // Types of Exif byte alignments (see JEITA CP-3451C Section 4.5.2)
    static final short BYTE_ALIGN_II = 0x4949;  // II: Intel order
    static final short BYTE_ALIGN_MM = 0x4d4d;  // MM: Motorola order

    // TIFF Header Fixed Constant (see JEITA CP-3451C Section 4.5.2)
    static final byte START_CODE = 0x2a; // 42
    private static final int IFD_OFFSET = 8;

    // Formats for the value in IFD entry (See TIFF 6.0 Section 2, "Image File Directory".)
    private static final int IFD_FORMAT_BYTE = 1;
    private static final int IFD_FORMAT_STRING = 2;
    private static final int IFD_FORMAT_USHORT = 3;
    private static final int IFD_FORMAT_ULONG = 4;
    private static final int IFD_FORMAT_URATIONAL = 5;
    private static final int IFD_FORMAT_SBYTE = 6;
    private static final int IFD_FORMAT_UNDEFINED = 7;
    private static final int IFD_FORMAT_SSHORT = 8;
    private static final int IFD_FORMAT_SLONG = 9;
    private static final int IFD_FORMAT_SRATIONAL = 10;
    private static final int IFD_FORMAT_SINGLE = 11;
    private static final int IFD_FORMAT_DOUBLE = 12;
    // Format indicating a new IFD entry (See Adobe PageMaker® 6.0 TIFF Technical Notes, "New Tag")
    private static final int IFD_FORMAT_IFD = 13;
    // Names for the data formats for debugging purpose.
    static final String[] IFD_FORMAT_NAMES = new String[] {
            "", "BYTE", "STRING", "USHORT", "ULONG", "URATIONAL", "SBYTE", "UNDEFINED", "SSHORT",
            "SLONG", "SRATIONAL", "SINGLE", "DOUBLE"
    };
    // Sizes of the components of each IFD value format
    static final int[] IFD_FORMAT_BYTES_PER_FORMAT = new int[] {
            0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8, 1
    };
    private static final byte[] EXIF_ASCII_PREFIX = new byte[] {
            0x41, 0x53, 0x43, 0x49, 0x49, 0x0, 0x0, 0x0
    };

    // A class for indicating EXIF rational type.
    private static class Rational {
        public final long numerator;
        public final long denominator;

        private Rational(double value) {
            this((long) (value * 10000), 10000);
        }

        private Rational(long numerator, long denominator) {
            // Handle erroneous case
            if (denominator == 0) {
                this.numerator = 0;
                this.denominator = 1;
                return;
            }
            this.numerator = numerator;
            this.denominator = denominator;
        }

        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }

        public double calculate() {
            return (double) numerator / denominator;
        }
    }

    // A class for indicating EXIF attribute.
    private static class ExifAttribute {
        public final int format;
        public final int numberOfComponents;
        public final byte[] bytes;

        private ExifAttribute(int format, int numberOfComponents, byte[] bytes) {
            this.format = format;
            this.numberOfComponents = numberOfComponents;
            this.bytes = bytes;
        }

        public static ExifAttribute createUShort(int[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_USHORT] * values.length]);
            buffer.order(byteOrder);
            for (int value : values) {
                buffer.putShort((short) value);
            }
            return new ExifAttribute(IFD_FORMAT_USHORT, values.length, buffer.array());
        }

        public static ExifAttribute createUShort(int value, ByteOrder byteOrder) {
            return createUShort(new int[] {value}, byteOrder);
        }

        public static ExifAttribute createULong(long[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_ULONG] * values.length]);
            buffer.order(byteOrder);
            for (long value : values) {
                buffer.putInt((int) value);
            }
            return new ExifAttribute(IFD_FORMAT_ULONG, values.length, buffer.array());
        }

        public static ExifAttribute createULong(long value, ByteOrder byteOrder) {
            return createULong(new long[] {value}, byteOrder);
        }

        public static ExifAttribute createSLong(int[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_SLONG] * values.length]);
            buffer.order(byteOrder);
            for (int value : values) {
                buffer.putInt(value);
            }
            return new ExifAttribute(IFD_FORMAT_SLONG, values.length, buffer.array());
        }

        public static ExifAttribute createSLong(int value, ByteOrder byteOrder) {
            return createSLong(new int[] {value}, byteOrder);
        }

        public static ExifAttribute createByte(String value) {
            // Exception for GPSAltitudeRef tag
            if (value.length() == 1 && value.charAt(0) >= '0' && value.charAt(0) <= '1') {
                final byte[] bytes = new byte[] { (byte) (value.charAt(0) - '0') };
                return new ExifAttribute(IFD_FORMAT_BYTE, bytes.length, bytes);
            }
            final byte[] ascii = value.getBytes(ASCII);
            return new ExifAttribute(IFD_FORMAT_BYTE, ascii.length, ascii);
        }

        public static ExifAttribute createString(String value) {
            final byte[] ascii = (value + '\0').getBytes(ASCII);
            return new ExifAttribute(IFD_FORMAT_STRING, ascii.length, ascii);
        }

        public static ExifAttribute createURational(Rational[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_URATIONAL] * values.length]);
            buffer.order(byteOrder);
            for (Rational value : values) {
                buffer.putInt((int) value.numerator);
                buffer.putInt((int) value.denominator);
            }
            return new ExifAttribute(IFD_FORMAT_URATIONAL, values.length, buffer.array());
        }

        public static ExifAttribute createURational(Rational value, ByteOrder byteOrder) {
            return createURational(new Rational[] {value}, byteOrder);
        }

        public static ExifAttribute createSRational(Rational[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_SRATIONAL] * values.length]);
            buffer.order(byteOrder);
            for (Rational value : values) {
                buffer.putInt((int) value.numerator);
                buffer.putInt((int) value.denominator);
            }
            return new ExifAttribute(IFD_FORMAT_SRATIONAL, values.length, buffer.array());
        }

        public static ExifAttribute createSRational(Rational value, ByteOrder byteOrder) {
            return createSRational(new Rational[] {value}, byteOrder);
        }

        public static ExifAttribute createDouble(double[] values, ByteOrder byteOrder) {
            final ByteBuffer buffer = ByteBuffer.wrap(
                    new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_DOUBLE] * values.length]);
            buffer.order(byteOrder);
            for (double value : values) {
                buffer.putDouble(value);
            }
            return new ExifAttribute(IFD_FORMAT_DOUBLE, values.length, buffer.array());
        }

        public static ExifAttribute createDouble(double value, ByteOrder byteOrder) {
            return createDouble(new double[] {value}, byteOrder);
        }

        @Override
        public String toString() {
            return "(" + IFD_FORMAT_NAMES[format] + ", data length:" + bytes.length + ")";
        }

        private Object getValue(ByteOrder byteOrder) {
            ByteOrderedDataInputStream inputStream = null;
            try {
                inputStream = new ByteOrderedDataInputStream(bytes);
                inputStream.setByteOrder(byteOrder);
                switch (format) {
                    case IFD_FORMAT_BYTE:
                    case IFD_FORMAT_SBYTE: {
                        // Exception for GPSAltitudeRef tag
                        if (bytes.length == 1 && bytes[0] >= 0 && bytes[0] <= 1) {
                            return new String(new char[] { (char) (bytes[0] + '0') });
                        }
                        return new String(bytes, ASCII);
                    }
                    case IFD_FORMAT_UNDEFINED:
                    case IFD_FORMAT_STRING: {
                        int index = 0;
                        if (numberOfComponents >= EXIF_ASCII_PREFIX.length) {
                            boolean same = true;
                            for (int i = 0; i < EXIF_ASCII_PREFIX.length; ++i) {
                                if (bytes[i] != EXIF_ASCII_PREFIX[i]) {
                                    same = false;
                                    break;
                                }
                            }
                            if (same) {
                                index = EXIF_ASCII_PREFIX.length;
                            }
                        }

                        StringBuilder stringBuilder = new StringBuilder();
                        while (index < numberOfComponents) {
                            int ch = bytes[index];
                            if (ch == 0) {
                                break;
                            }
                            if (ch >= 32) {
                                stringBuilder.append((char) ch);
                            } else {
                                stringBuilder.append('?');
                            }
                            ++index;
                        }
                        return stringBuilder.toString();
                    }
                    case IFD_FORMAT_USHORT: {
                        final int[] values = new int[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readUnsignedShort();
                        }
                        return values;
                    }
                    case IFD_FORMAT_ULONG: {
                        final long[] values = new long[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readUnsignedInt();
                        }
                        return values;
                    }
                    case IFD_FORMAT_URATIONAL: {
                        final Rational[] values = new Rational[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            final long numerator = inputStream.readUnsignedInt();
                            final long denominator = inputStream.readUnsignedInt();
                            values[i] = new Rational(numerator, denominator);
                        }
                        return values;
                    }
                    case IFD_FORMAT_SSHORT: {
                        final int[] values = new int[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readShort();
                        }
                        return values;
                    }
                    case IFD_FORMAT_SLONG: {
                        final int[] values = new int[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readInt();
                        }
                        return values;
                    }
                    case IFD_FORMAT_SRATIONAL: {
                        final Rational[] values = new Rational[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            final long numerator = inputStream.readInt();
                            final long denominator = inputStream.readInt();
                            values[i] = new Rational(numerator, denominator);
                        }
                        return values;
                    }
                    case IFD_FORMAT_SINGLE: {
                        final double[] values = new double[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readFloat();
                        }
                        return values;
                    }
                    case IFD_FORMAT_DOUBLE: {
                        final double[] values = new double[numberOfComponents];
                        for (int i = 0; i < numberOfComponents; ++i) {
                            values[i] = inputStream.readDouble();
                        }
                        return values;
                    }
                    default:
                        return null;
                }
            } catch (IOException e) {
                Log.w(TAG, "IOException occurred during reading a value", e);
                return null;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "IOException occurred while closing InputStream", e);
                    }
                }
            }
        }

        public double getDoubleValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                throw new NumberFormatException("NULL can't be converted to a double value");
            }
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            if (value instanceof long[]) {
                long[] array = (long[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                int[] array = (int[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof double[]) {
                double[] array = (double[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof Rational[]) {
                Rational[] array = (Rational[]) value;
                if (array.length == 1) {
                    return array[0].calculate();
                }
                throw new NumberFormatException("There are more than one component");
            }
            throw new NumberFormatException("Couldn't find a double value");
        }

        public int getIntValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                throw new NumberFormatException("NULL can't be converted to a integer value");
            }
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            if (value instanceof long[]) {
                long[] array = (long[]) value;
                if (array.length == 1) {
                    return (int) array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                int[] array = (int[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            throw new NumberFormatException("Couldn't find a integer value");
        }

        public String getStringValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return (String) value;
            }

            final StringBuilder stringBuilder = new StringBuilder();
            if (value instanceof long[]) {
                long[] array = (long[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i]);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof int[]) {
                int[] array = (int[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i]);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof double[]) {
                double[] array = (double[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i]);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof Rational[]) {
                Rational[] array = (Rational[]) value;
                for (int i = 0; i < array.length; ++i) {
                    stringBuilder.append(array[i].numerator);
                    stringBuilder.append('/');
                    stringBuilder.append(array[i].denominator);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            return null;
        }

        public int size() {
            return IFD_FORMAT_BYTES_PER_FORMAT[format] * numberOfComponents;
        }
    }

    // A class for indicating EXIF tag.
    static class ExifTag {
        public final int number;
        public final String name;
        public final int primaryFormat;
        public final int secondaryFormat;

        private ExifTag(String name, int number, int format) {
            this.name = name;
            this.number = number;
            this.primaryFormat = format;
            this.secondaryFormat = -1;
        }

        private ExifTag(String name, int number, int primaryFormat, int secondaryFormat) {
            this.name = name;
            this.number = number;
            this.primaryFormat = primaryFormat;
            this.secondaryFormat = secondaryFormat;
        }

        private boolean isFormatCompatible(int format) {
            if (primaryFormat == IFD_FORMAT_UNDEFINED || format == IFD_FORMAT_UNDEFINED) {
                return true;
            } else if (primaryFormat == format || secondaryFormat == format) {
                return true;
            } else if ((primaryFormat == IFD_FORMAT_ULONG || secondaryFormat == IFD_FORMAT_ULONG)
                    && format == IFD_FORMAT_USHORT) {
                return true;
            } else if ((primaryFormat == IFD_FORMAT_SLONG || secondaryFormat == IFD_FORMAT_SLONG)
                    && format == IFD_FORMAT_SSHORT) {
                return true;
            } else if ((primaryFormat == IFD_FORMAT_DOUBLE || secondaryFormat == IFD_FORMAT_DOUBLE)
                    && format == IFD_FORMAT_SINGLE) {
                return true;
            }
            return false;
        }
    }

    // Primary image IFD TIFF tags (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    private static final ExifTag[] IFD_TIFF_TAGS = new ExifTag[] {
            // For below two, see TIFF 6.0 Spec Section 3: Bilevel Images.
            new ExifTag(TAG_NEW_SUBFILE_TYPE, 254, IFD_FORMAT_ULONG),
            new ExifTag(TAG_SUBFILE_TYPE, 255, IFD_FORMAT_ULONG),
            new ExifTag(TAG_IMAGE_WIDTH, 256, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_IMAGE_LENGTH, 257, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_BITS_PER_SAMPLE, 258, IFD_FORMAT_USHORT),
            new ExifTag(TAG_COMPRESSION, 259, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, IFD_FORMAT_USHORT),
            new ExifTag(TAG_IMAGE_DESCRIPTION, 270, IFD_FORMAT_STRING),
            new ExifTag(TAG_MAKE, 271, IFD_FORMAT_STRING),
            new ExifTag(TAG_MODEL, 272, IFD_FORMAT_STRING),
            new ExifTag(TAG_STRIP_OFFSETS, 273, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_ORIENTATION, 274, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, IFD_FORMAT_USHORT),
            new ExifTag(TAG_ROWS_PER_STRIP, 278, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_X_RESOLUTION, 282, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_RESOLUTION, 283, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PLANAR_CONFIGURATION, 284, IFD_FORMAT_USHORT),
            new ExifTag(TAG_RESOLUTION_UNIT, 296, IFD_FORMAT_USHORT),
            new ExifTag(TAG_TRANSFER_FUNCTION, 301, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SOFTWARE, 305, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME, 306, IFD_FORMAT_STRING),
            new ExifTag(TAG_ARTIST, 315, IFD_FORMAT_STRING),
            new ExifTag(TAG_WHITE_POINT, 318, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, IFD_FORMAT_URATIONAL),
            // See Adobe PageMaker® 6.0 TIFF Technical Notes, Note 1.
            new ExifTag(TAG_SUB_IFD_POINTER, 330, IFD_FORMAT_ULONG),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, IFD_FORMAT_ULONG),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, IFD_FORMAT_ULONG),
            new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, 529, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, 530, IFD_FORMAT_USHORT),
            new ExifTag(TAG_Y_CB_CR_POSITIONING, 531, IFD_FORMAT_USHORT),
            new ExifTag(TAG_REFERENCE_BLACK_WHITE, 532, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_COPYRIGHT, 33432, IFD_FORMAT_STRING),
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
            // RW2 file tags
            // See http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/PanasonicRaw.html)
            new ExifTag(TAG_RW2_SENSOR_TOP_BORDER, 4, IFD_FORMAT_ULONG),
            new ExifTag(TAG_RW2_SENSOR_LEFT_BORDER, 5, IFD_FORMAT_ULONG),
            new ExifTag(TAG_RW2_SENSOR_BOTTOM_BORDER, 6, IFD_FORMAT_ULONG),
            new ExifTag(TAG_RW2_SENSOR_RIGHT_BORDER, 7, IFD_FORMAT_ULONG),
            new ExifTag(TAG_RW2_ISO, 23, IFD_FORMAT_USHORT),
            new ExifTag(TAG_RW2_JPG_FROM_RAW, 46, IFD_FORMAT_UNDEFINED)
    };

    // Primary image IFD Exif Private tags (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    private static final ExifTag[] IFD_EXIF_TAGS = new ExifTag[] {
            new ExifTag(TAG_EXPOSURE_TIME, 33434, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_F_NUMBER, 33437, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_EXPOSURE_PROGRAM, 34850, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SPECTRAL_SENSITIVITY, 34852, IFD_FORMAT_STRING),
            new ExifTag(TAG_PHOTOGRAPHIC_SENSITIVITY, 34855, IFD_FORMAT_USHORT),
            new ExifTag(TAG_OECF, 34856, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_EXIF_VERSION, 36864, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME_ORIGINAL, 36867, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME_DIGITIZED, 36868, IFD_FORMAT_STRING),
            new ExifTag(TAG_COMPONENTS_CONFIGURATION, 37121, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_COMPRESSED_BITS_PER_PIXEL, 37122, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SHUTTER_SPEED_VALUE, 37377, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_APERTURE_VALUE, 37378, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_BRIGHTNESS_VALUE, 37379, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_EXPOSURE_BIAS_VALUE, 37380, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_MAX_APERTURE_VALUE, 37381, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SUBJECT_DISTANCE, 37382, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_METERING_MODE, 37383, IFD_FORMAT_USHORT),
            new ExifTag(TAG_LIGHT_SOURCE, 37384, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FLASH, 37385, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FOCAL_LENGTH, 37386, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SUBJECT_AREA, 37396, IFD_FORMAT_USHORT),
            new ExifTag(TAG_MAKER_NOTE, 37500, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_USER_COMMENT, 37510, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_SUBSEC_TIME, 37520, IFD_FORMAT_STRING),
            new ExifTag(TAG_SUBSEC_TIME_ORIGINAL, 37521, IFD_FORMAT_STRING),
            new ExifTag(TAG_SUBSEC_TIME_DIGITIZED, 37522, IFD_FORMAT_STRING),
            new ExifTag(TAG_FLASHPIX_VERSION, 40960, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_COLOR_SPACE, 40961, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PIXEL_X_DIMENSION, 40962, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_PIXEL_Y_DIMENSION, 40963, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_RELATED_SOUND_FILE, 40964, IFD_FORMAT_STRING),
            new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, IFD_FORMAT_ULONG),
            new ExifTag(TAG_FLASH_ENERGY, 41483, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SPATIAL_FREQUENCY_RESPONSE, 41484, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_FOCAL_PLANE_X_RESOLUTION, 41486, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_FOCAL_PLANE_Y_RESOLUTION, 41487, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_FOCAL_PLANE_RESOLUTION_UNIT, 41488, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SUBJECT_LOCATION, 41492, IFD_FORMAT_USHORT),
            new ExifTag(TAG_EXPOSURE_INDEX, 41493, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SENSING_METHOD, 41495, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FILE_SOURCE, 41728, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_SCENE_TYPE, 41729, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_CFA_PATTERN, 41730, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_CUSTOM_RENDERED, 41985, IFD_FORMAT_USHORT),
            new ExifTag(TAG_EXPOSURE_MODE, 41986, IFD_FORMAT_USHORT),
            new ExifTag(TAG_WHITE_BALANCE, 41987, IFD_FORMAT_USHORT),
            new ExifTag(TAG_DIGITAL_ZOOM_RATIO, 41988, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_FOCAL_LENGTH_IN_35MM_FILM, 41989, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SCENE_CAPTURE_TYPE, 41990, IFD_FORMAT_USHORT),
            new ExifTag(TAG_GAIN_CONTROL, 41991, IFD_FORMAT_USHORT),
            new ExifTag(TAG_CONTRAST, 41992, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SATURATION, 41993, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SHARPNESS, 41994, IFD_FORMAT_USHORT),
            new ExifTag(TAG_DEVICE_SETTING_DESCRIPTION, 41995, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_SUBJECT_DISTANCE_RANGE, 41996, IFD_FORMAT_USHORT),
            new ExifTag(TAG_IMAGE_UNIQUE_ID, 42016, IFD_FORMAT_STRING),
            new ExifTag(TAG_DNG_VERSION, 50706, IFD_FORMAT_BYTE),
            new ExifTag(TAG_DEFAULT_CROP_SIZE, 50720, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG)
    };

    // Primary image IFD GPS Info tags (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    private static final ExifTag[] IFD_GPS_TAGS = new ExifTag[] {
            new ExifTag(TAG_GPS_VERSION_ID, 0, IFD_FORMAT_BYTE),
            new ExifTag(TAG_GPS_LATITUDE_REF, 1, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_LATITUDE, 2, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_LONGITUDE_REF, 3, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_LONGITUDE, 4, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_ALTITUDE_REF, 5, IFD_FORMAT_BYTE),
            new ExifTag(TAG_GPS_ALTITUDE, 6, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_TIMESTAMP, 7, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_SATELLITES, 8, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_STATUS, 9, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_MEASURE_MODE, 10, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DOP, 11, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_SPEED_REF, 12, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_SPEED, 13, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_TRACK_REF, 14, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_TRACK, 15, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_IMG_DIRECTION_REF, 16, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_IMG_DIRECTION, 17, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_MAP_DATUM, 18, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_LATITUDE_REF, 19, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_LATITUDE, 20, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_DEST_LONGITUDE_REF, 21, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_LONGITUDE, 22, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_DEST_BEARING_REF, 23, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_BEARING, 24, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_DEST_DISTANCE_REF, 25, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_DISTANCE, 26, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_PROCESSING_METHOD, 27, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_GPS_AREA_INFORMATION, 28, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_GPS_DATESTAMP, 29, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DIFFERENTIAL, 30, IFD_FORMAT_USHORT)
    };
    // Primary image IFD Interoperability tag (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    private static final ExifTag[] IFD_INTEROPERABILITY_TAGS = new ExifTag[] {
            new ExifTag(TAG_INTEROPERABILITY_INDEX, 1, IFD_FORMAT_STRING)
    };
    // IFD Thumbnail tags (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    private static final ExifTag[] IFD_THUMBNAIL_TAGS = new ExifTag[] {
            // For below two, see TIFF 6.0 Spec Section 3: Bilevel Images.
            new ExifTag(TAG_NEW_SUBFILE_TYPE, 254, IFD_FORMAT_ULONG),
            new ExifTag(TAG_SUBFILE_TYPE, 255, IFD_FORMAT_ULONG),
            new ExifTag(TAG_THUMBNAIL_IMAGE_WIDTH, 256, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_THUMBNAIL_IMAGE_LENGTH, 257, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_BITS_PER_SAMPLE, 258, IFD_FORMAT_USHORT),
            new ExifTag(TAG_COMPRESSION, 259, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, IFD_FORMAT_USHORT),
            new ExifTag(TAG_IMAGE_DESCRIPTION, 270, IFD_FORMAT_STRING),
            new ExifTag(TAG_MAKE, 271, IFD_FORMAT_STRING),
            new ExifTag(TAG_MODEL, 272, IFD_FORMAT_STRING),
            new ExifTag(TAG_STRIP_OFFSETS, 273, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_ORIENTATION, 274, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, IFD_FORMAT_USHORT),
            new ExifTag(TAG_ROWS_PER_STRIP, 278, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_X_RESOLUTION, 282, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_RESOLUTION, 283, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PLANAR_CONFIGURATION, 284, IFD_FORMAT_USHORT),
            new ExifTag(TAG_RESOLUTION_UNIT, 296, IFD_FORMAT_USHORT),
            new ExifTag(TAG_TRANSFER_FUNCTION, 301, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SOFTWARE, 305, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME, 306, IFD_FORMAT_STRING),
            new ExifTag(TAG_ARTIST, 315, IFD_FORMAT_STRING),
            new ExifTag(TAG_WHITE_POINT, 318, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, IFD_FORMAT_URATIONAL),
            // See Adobe PageMaker® 6.0 TIFF Technical Notes, Note 1.
            new ExifTag(TAG_SUB_IFD_POINTER, 330, IFD_FORMAT_ULONG),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, IFD_FORMAT_ULONG),
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, IFD_FORMAT_ULONG),
            new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, 529, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, 530, IFD_FORMAT_USHORT),
            new ExifTag(TAG_Y_CB_CR_POSITIONING, 531, IFD_FORMAT_USHORT),
            new ExifTag(TAG_REFERENCE_BLACK_WHITE, 532, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_COPYRIGHT, 33432, IFD_FORMAT_STRING),
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
            new ExifTag(TAG_DNG_VERSION, 50706, IFD_FORMAT_BYTE),
            new ExifTag(TAG_DEFAULT_CROP_SIZE, 50720, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG)
    };

    // RAF file tag (See piex.cc line 372)
    private static final ExifTag TAG_RAF_IMAGE_SIZE =
            new ExifTag(TAG_STRIP_OFFSETS, 273, IFD_FORMAT_USHORT);

    // ORF file tags (See http://www.exiv2.org/tags-olympus.html)
    private static final ExifTag[] ORF_MAKER_NOTE_TAGS = new ExifTag[] {
            new ExifTag(TAG_ORF_THUMBNAIL_IMAGE, 256, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_ORF_CAMERA_SETTINGS_IFD_POINTER, 8224, IFD_FORMAT_ULONG),
            new ExifTag(TAG_ORF_IMAGE_PROCESSING_IFD_POINTER, 8256, IFD_FORMAT_ULONG)
    };
    private static final ExifTag[] ORF_CAMERA_SETTINGS_TAGS = new ExifTag[] {
            new ExifTag(TAG_ORF_PREVIEW_IMAGE_START, 257, IFD_FORMAT_ULONG),
            new ExifTag(TAG_ORF_PREVIEW_IMAGE_LENGTH, 258, IFD_FORMAT_ULONG)
    };
    private static final ExifTag[] ORF_IMAGE_PROCESSING_TAGS = new ExifTag[] {
            new ExifTag(TAG_ORF_ASPECT_FRAME, 4371, IFD_FORMAT_USHORT)
    };
    // PEF file tag (See http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/Pentax.html)
    private static final ExifTag[] PEF_TAGS = new ExifTag[] {
            new ExifTag(TAG_COLOR_SPACE, 55, IFD_FORMAT_USHORT)
    };

    // See JEITA CP-3451C Section 4.6.3: Exif-specific IFD.
    // The following values are used for indicating pointers to the other Image File Directories.

    // Indices of Exif Ifd tag groups
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({IFD_TYPE_PRIMARY, IFD_TYPE_EXIF, IFD_TYPE_GPS, IFD_TYPE_INTEROPERABILITY,
            IFD_TYPE_THUMBNAIL, IFD_TYPE_PREVIEW, IFD_TYPE_ORF_MAKER_NOTE,
            IFD_TYPE_ORF_CAMERA_SETTINGS, IFD_TYPE_ORF_IMAGE_PROCESSING, IFD_TYPE_PEF})
    public @interface IfdType {}

    static final int IFD_TYPE_PRIMARY = 0;
    private static final int IFD_TYPE_EXIF = 1;
    private static final int IFD_TYPE_GPS = 2;
    private static final int IFD_TYPE_INTEROPERABILITY = 3;
    static final int IFD_TYPE_THUMBNAIL = 4;
    static final int IFD_TYPE_PREVIEW = 5;
    private static final int IFD_TYPE_ORF_MAKER_NOTE = 6;
    private static final int IFD_TYPE_ORF_CAMERA_SETTINGS = 7;
    private static final int IFD_TYPE_ORF_IMAGE_PROCESSING = 8;
    private static final int IFD_TYPE_PEF = 9;

    // List of Exif tag groups
    static final ExifTag[][] EXIF_TAGS = new ExifTag[][] {
            IFD_TIFF_TAGS, IFD_EXIF_TAGS, IFD_GPS_TAGS, IFD_INTEROPERABILITY_TAGS,
            IFD_THUMBNAIL_TAGS, IFD_TIFF_TAGS, ORF_MAKER_NOTE_TAGS, ORF_CAMERA_SETTINGS_TAGS,
            ORF_IMAGE_PROCESSING_TAGS, PEF_TAGS
    };
    // List of tags for pointing to the other image file directory offset.
    private static final ExifTag[] EXIF_POINTER_TAGS = new ExifTag[] {
            new ExifTag(TAG_SUB_IFD_POINTER, 330, IFD_FORMAT_ULONG),
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
            new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, IFD_FORMAT_ULONG),
            new ExifTag(TAG_ORF_CAMERA_SETTINGS_IFD_POINTER, 8224, IFD_FORMAT_BYTE),
            new ExifTag(TAG_ORF_IMAGE_PROCESSING_IFD_POINTER, 8256, IFD_FORMAT_BYTE)
    };

    // Tags for indicating the thumbnail offset and length
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_TAG =
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, IFD_FORMAT_ULONG);
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_LENGTH_TAG =
            new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, IFD_FORMAT_ULONG);

    // Mappings from tag number to tag name and each item represents one IFD tag group.
    @SuppressWarnings("unchecked")
    private static final HashMap<Integer, ExifTag>[] sExifTagMapsForReading =
            new HashMap[EXIF_TAGS.length];
    // Mappings from tag name to tag number and each item represents one IFD tag group.
    @SuppressWarnings("unchecked")
    private static final HashMap<String, ExifTag>[] sExifTagMapsForWriting =
            new HashMap[EXIF_TAGS.length];
    private static final HashSet<String> sTagSetForCompatibility = new HashSet<>(Arrays.asList(
            TAG_F_NUMBER, TAG_DIGITAL_ZOOM_RATIO, TAG_EXPOSURE_TIME, TAG_SUBJECT_DISTANCE,
            TAG_GPS_TIMESTAMP));
    // Mappings from tag number to IFD type for pointer tags.
    @SuppressWarnings("unchecked")
    private static final HashMap<Integer, Integer> sExifPointerTagMap = new HashMap();

    // See JPEG File Interchange Format Version 1.02.
    // The following values are defined for handling JPEG streams. In this implementation, we are
    // not only getting information from EXIF but also from some JPEG special segments such as
    // MARKER_COM for user comment and MARKER_SOFx for image width and height.

    private static final Charset ASCII = Charset.forName("US-ASCII");
    // Identifier for EXIF APP1 segment in JPEG
    static final byte[] IDENTIFIER_EXIF_APP1 = "Exif\0\0".getBytes(ASCII);
    // JPEG segment markers, that each marker consumes two bytes beginning with 0xff and ending with
    // the indicator. There is no SOF4, SOF8, SOF16 markers in JPEG and SOFx markers indicates start
    // of frame(baseline DCT) and the image size info exists in its beginning part.
    static final byte MARKER = (byte) 0xff;
    private static final byte MARKER_SOI = (byte) 0xd8;
    private static final byte MARKER_SOF0 = (byte) 0xc0;
    private static final byte MARKER_SOF1 = (byte) 0xc1;
    private static final byte MARKER_SOF2 = (byte) 0xc2;
    private static final byte MARKER_SOF3 = (byte) 0xc3;
    private static final byte MARKER_SOF5 = (byte) 0xc5;
    private static final byte MARKER_SOF6 = (byte) 0xc6;
    private static final byte MARKER_SOF7 = (byte) 0xc7;
    private static final byte MARKER_SOF9 = (byte) 0xc9;
    private static final byte MARKER_SOF10 = (byte) 0xca;
    private static final byte MARKER_SOF11 = (byte) 0xcb;
    private static final byte MARKER_SOF13 = (byte) 0xcd;
    private static final byte MARKER_SOF14 = (byte) 0xce;
    private static final byte MARKER_SOF15 = (byte) 0xcf;
    private static final byte MARKER_SOS = (byte) 0xda;
    static final byte MARKER_APP1 = (byte) 0xe1;
    private static final byte MARKER_COM = (byte) 0xfe;
    static final byte MARKER_EOI = (byte) 0xd9;

    // Supported Image File Types
    private static final int IMAGE_TYPE_UNKNOWN = 0;
    private static final int IMAGE_TYPE_ARW = 1;
    private static final int IMAGE_TYPE_CR2 = 2;
    private static final int IMAGE_TYPE_DNG = 3;
    private static final int IMAGE_TYPE_JPEG = 4;
    private static final int IMAGE_TYPE_NEF = 5;
    private static final int IMAGE_TYPE_NRW = 6;
    private static final int IMAGE_TYPE_ORF = 7;
    private static final int IMAGE_TYPE_PEF = 8;
    private static final int IMAGE_TYPE_RAF = 9;
    private static final int IMAGE_TYPE_RW2 = 10;
    private static final int IMAGE_TYPE_SRW = 11;

    static {
        sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Build up the hash tables to look up Exif tags for reading Exif tags.
        for (int ifdType = 0; ifdType < EXIF_TAGS.length; ++ifdType) {
            sExifTagMapsForReading[ifdType] = new HashMap<>();
            sExifTagMapsForWriting[ifdType] = new HashMap<>();
            for (ExifTag tag : EXIF_TAGS[ifdType]) {
                sExifTagMapsForReading[ifdType].put(tag.number, tag);
                sExifTagMapsForWriting[ifdType].put(tag.name, tag);
            }
        }

        // Build up the hash table to look up Exif pointer tags.
        sExifPointerTagMap.put(EXIF_POINTER_TAGS[0].number, IFD_TYPE_PREVIEW); // 330
        sExifPointerTagMap.put(EXIF_POINTER_TAGS[1].number, IFD_TYPE_EXIF); // 34665
        sExifPointerTagMap.put(EXIF_POINTER_TAGS[2].number, IFD_TYPE_GPS); // 34853
        sExifPointerTagMap.put(EXIF_POINTER_TAGS[3].number, IFD_TYPE_INTEROPERABILITY); // 40965
        sExifPointerTagMap.put(EXIF_POINTER_TAGS[4].number, IFD_TYPE_ORF_CAMERA_SETTINGS); // 8224
        sExifPointerTagMap.put(EXIF_POINTER_TAGS[5].number, IFD_TYPE_ORF_IMAGE_PROCESSING); // 8256
    }

    private final String mFilename;
    private final AssetManager.AssetInputStream mAssetInputStream;
    private int mMimeType;
    @SuppressWarnings("unchecked")
    private final HashMap<String, ExifAttribute>[] mAttributes = new HashMap[EXIF_TAGS.length];
    private ByteOrder mExifByteOrder = ByteOrder.BIG_ENDIAN;
    private boolean mHasThumbnail;
    // The following values used for indicating a thumbnail position.
    private int mThumbnailOffset;
    private int mThumbnailLength;
    private byte[] mThumbnailBytes;
    private int mThumbnailCompression;
    private int mExifOffset;
    private int mOrfMakerNoteOffset;
    private int mOrfThumbnailOffset;
    private int mOrfThumbnailLength;
    private int mRw2JpgFromRawOffset;
    private boolean mIsSupportedFile;

    // Pattern to check non zero timestamp
    private static final Pattern sNonZeroTimePattern = Pattern.compile(".*[1-9].*");
    // Pattern to check gps timestamp
    private static final Pattern sGpsTimestampPattern =
            Pattern.compile("^([0-9][0-9]):([0-9][0-9]):([0-9][0-9])$");

    /**
     * Reads Exif tags from the specified image file.
     */
    public ExifInterface(@NonNull String filename) throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null");
        }
        FileInputStream in = null;
        mAssetInputStream = null;
        mFilename = filename;
        try {
            in = new FileInputStream(filename);
            loadAttributes(in);
        } finally {
            closeQuietly(in);
        }
    }

    /**
     * Reads Exif tags from the specified image input stream. Attribute mutation is not supported
     * for input streams. The given input stream will proceed its current position. Developers
     * should close the input stream after use. This constructor is not intended to be used with
     * an input stream that performs any networking operations.
     */
    public ExifInterface(@NonNull InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null");
        }
        mFilename = null;
        if (inputStream instanceof AssetManager.AssetInputStream) {
            mAssetInputStream = (AssetManager.AssetInputStream) inputStream;
        } else {
            mAssetInputStream = null;
        }
        loadAttributes(inputStream);
    }

    /**
     * Returns the EXIF attribute of the specified tag or {@code null} if there is no such tag in
     * the image file.
     *
     * @param tag the name of the tag.
     */
    @Nullable
    private ExifAttribute getExifAttribute(@NonNull String tag) {
        if (TAG_ISO_SPEED_RATINGS.equals(tag)) {
            if (DEBUG) {
                Log.d(TAG, "getExifAttribute: Replacing TAG_ISO_SPEED_RATINGS with "
                        + "TAG_PHOTOGRAPHIC_SENSITIVITY.");
            }
            tag = TAG_PHOTOGRAPHIC_SENSITIVITY;
        }
        // Retrieves all tag groups. The value from primary image tag group has a higher priority
        // than the value from the thumbnail tag group if there are more than one candidates.
        for (int i = 0; i < EXIF_TAGS.length; ++i) {
            ExifAttribute value = mAttributes[i].get(tag);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Returns the value of the specified tag or {@code null} if there
     * is no such tag in the image file.
     *
     * @param tag the name of the tag.
     */
    @Nullable
    public String getAttribute(@NonNull String tag) {
        ExifAttribute attribute = getExifAttribute(tag);
        if (attribute != null) {
            if (!sTagSetForCompatibility.contains(tag)) {
                return attribute.getStringValue(mExifByteOrder);
            }
            if (tag.equals(TAG_GPS_TIMESTAMP)) {
                // Convert the rational values to the custom formats for backwards compatibility.
                if (attribute.format != IFD_FORMAT_URATIONAL
                        && attribute.format != IFD_FORMAT_SRATIONAL) {
                    Log.w(TAG, "GPS Timestamp format is not rational. format=" + attribute.format);
                    return null;
                }
                Rational[] array = (Rational[]) attribute.getValue(mExifByteOrder);
                if (array == null || array.length != 3) {
                    Log.w(TAG, "Invalid GPS Timestamp array. array=" + Arrays.toString(array));
                    return null;
                }
                return String.format("%02d:%02d:%02d",
                        (int) ((float) array[0].numerator / array[0].denominator),
                        (int) ((float) array[1].numerator / array[1].denominator),
                        (int) ((float) array[2].numerator / array[2].denominator));
            }
            try {
                return Double.toString(attribute.getDoubleValue(mExifByteOrder));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the integer value of the specified tag. If there is no such tag
     * in the image file or the value cannot be parsed as integer, return
     * <var>defaultValue</var>.
     *
     * @param tag the name of the tag.
     * @param defaultValue the value to return if the tag is not available.
     */
    public int getAttributeInt(@NonNull String tag, int defaultValue) {
        ExifAttribute exifAttribute = getExifAttribute(tag);
        if (exifAttribute == null) {
            return defaultValue;
        }

        try {
            return exifAttribute.getIntValue(mExifByteOrder);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the double value of the tag that is specified as rational or contains a
     * double-formatted value. If there is no such tag in the image file or the value cannot be
     * parsed as double, return <var>defaultValue</var>.
     *
     * @param tag the name of the tag.
     * @param defaultValue the value to return if the tag is not available.
     */
    public double getAttributeDouble(@NonNull String tag, double defaultValue) {
        ExifAttribute exifAttribute = getExifAttribute(tag);
        if (exifAttribute == null) {
            return defaultValue;
        }

        try {
            return exifAttribute.getDoubleValue(mExifByteOrder);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets the value of the specified tag.
     *
     * @param tag the name of the tag.
     * @param value the value of the tag.
     */
    public void setAttribute(@NonNull String tag, @Nullable String value) {
        if (TAG_ISO_SPEED_RATINGS.equals(tag)) {
            if (DEBUG) {
                Log.d(TAG, "setAttribute: Replacing TAG_ISO_SPEED_RATINGS with "
                        + "TAG_PHOTOGRAPHIC_SENSITIVITY.");
            }
            tag = TAG_PHOTOGRAPHIC_SENSITIVITY;
        }
        // Convert the given value to rational values for backwards compatibility.
        if (value != null && sTagSetForCompatibility.contains(tag)) {
            if (tag.equals(TAG_GPS_TIMESTAMP)) {
                Matcher m = sGpsTimestampPattern.matcher(value);
                if (!m.find()) {
                    Log.w(TAG, "Invalid value for " + tag + " : " + value);
                    return;
                }
                value = Integer.parseInt(m.group(1)) + "/1," + Integer.parseInt(m.group(2)) + "/1,"
                        + Integer.parseInt(m.group(3)) + "/1";
            } else {
                try {
                    double doubleValue = Double.parseDouble(value);
                    value = new Rational(doubleValue).toString();
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid value for " + tag + " : " + value);
                    return;
                }
            }
        }

        for (int i = 0 ; i < EXIF_TAGS.length; ++i) {
            if (i == IFD_TYPE_THUMBNAIL && !mHasThumbnail) {
                continue;
            }
            final ExifTag exifTag = sExifTagMapsForWriting[i].get(tag);
            if (exifTag != null) {
                if (value == null) {
                    mAttributes[i].remove(tag);
                    continue;
                }
                Pair<Integer, Integer> guess = guessDataFormat(value);
                int dataFormat;
                if (exifTag.primaryFormat == guess.first || exifTag.primaryFormat == guess.second) {
                    dataFormat = exifTag.primaryFormat;
                } else if (exifTag.secondaryFormat != -1 && (exifTag.secondaryFormat == guess.first
                        || exifTag.secondaryFormat == guess.second)) {
                    dataFormat = exifTag.secondaryFormat;
                } else if (exifTag.primaryFormat == IFD_FORMAT_BYTE
                        || exifTag.primaryFormat == IFD_FORMAT_UNDEFINED
                        || exifTag.primaryFormat == IFD_FORMAT_STRING) {
                    dataFormat = exifTag.primaryFormat;
                } else {
                    Log.w(TAG, "Given tag (" + tag + ") value didn't match with one of expected "
                            + "formats: " + IFD_FORMAT_NAMES[exifTag.primaryFormat]
                            + (exifTag.secondaryFormat == -1 ? "" : ", "
                            + IFD_FORMAT_NAMES[exifTag.secondaryFormat]) + " (guess: "
                            + IFD_FORMAT_NAMES[guess.first] + (guess.second == -1 ? "" : ", "
                            + IFD_FORMAT_NAMES[guess.second]) + ")");
                    continue;
                }
                switch (dataFormat) {
                    case IFD_FORMAT_BYTE: {
                        mAttributes[i].put(tag, ExifAttribute.createByte(value));
                        break;
                    }
                    case IFD_FORMAT_UNDEFINED:
                    case IFD_FORMAT_STRING: {
                        mAttributes[i].put(tag, ExifAttribute.createString(value));
                        break;
                    }
                    case IFD_FORMAT_USHORT: {
                        final String[] values = value.split(",", -1);
                        final int[] intArray = new int[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            intArray[j] = Integer.parseInt(values[j]);
                        }
                        mAttributes[i].put(tag,
                                ExifAttribute.createUShort(intArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_SLONG: {
                        final String[] values = value.split(",", -1);
                        final int[] intArray = new int[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            intArray[j] = Integer.parseInt(values[j]);
                        }
                        mAttributes[i].put(tag,
                                ExifAttribute.createSLong(intArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_ULONG: {
                        final String[] values = value.split(",", -1);
                        final long[] longArray = new long[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            longArray[j] = Long.parseLong(values[j]);
                        }
                        mAttributes[i].put(tag,
                                ExifAttribute.createULong(longArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_URATIONAL: {
                        final String[] values = value.split(",", -1);
                        final Rational[] rationalArray = new Rational[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            final String[] numbers = values[j].split("/", -1);
                            rationalArray[j] = new Rational((long) Double.parseDouble(numbers[0]),
                                    (long) Double.parseDouble(numbers[1]));
                        }
                        mAttributes[i].put(tag,
                                ExifAttribute.createURational(rationalArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_SRATIONAL: {
                        final String[] values = value.split(",", -1);
                        final Rational[] rationalArray = new Rational[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            final String[] numbers = values[j].split("/", -1);
                            rationalArray[j] = new Rational((long) Double.parseDouble(numbers[0]),
                                    (long) Double.parseDouble(numbers[1]));
                        }
                        mAttributes[i].put(tag,
                                ExifAttribute.createSRational(rationalArray, mExifByteOrder));
                        break;
                    }
                    case IFD_FORMAT_DOUBLE: {
                        final String[] values = value.split(",", -1);
                        final double[] doubleArray = new double[values.length];
                        for (int j = 0; j < values.length; ++j) {
                            doubleArray[j] = Double.parseDouble(values[j]);
                        }
                        mAttributes[i].put(tag,
                                ExifAttribute.createDouble(doubleArray, mExifByteOrder));
                        break;
                    }
                    default:
                        Log.w(TAG, "Data format isn't one of expected formats: " + dataFormat);
                        continue;
                }
            }
        }
    }

    /**
     * Resets the {@link #TAG_ORIENTATION} of the image to be {@link #ORIENTATION_NORMAL}.
     */
    public void resetOrientation() {
        setAttribute(TAG_ORIENTATION, Integer.toString(ORIENTATION_NORMAL));
    }

    /**
     * Rotates the image by the given degree clockwise. The degree should be a multiple of
     * 90 (e.g, 90, 180, -90, etc.).
     *
     * @param degree The degree of rotation.
     */
    public void rotate(int degree) {
        if (degree % 90 !=0) {
            throw new IllegalArgumentException("degree should be a multiple of 90");
        }

        int currentOrientation = getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
        int currentIndex, newIndex;
        int resultOrientation;
        if (ROTATION_ORDER.contains(currentOrientation)) {
            currentIndex = ROTATION_ORDER.indexOf(currentOrientation);
            newIndex = (currentIndex + degree / 90) % 4;
            newIndex += newIndex < 0 ? 4 : 0;
            resultOrientation = ROTATION_ORDER.get(newIndex);
        } else if (FLIPPED_ROTATION_ORDER.contains(currentOrientation)) {
            currentIndex = FLIPPED_ROTATION_ORDER.indexOf(currentOrientation);
            newIndex = (currentIndex + degree / 90) % 4;
            newIndex += newIndex < 0 ? 4 : 0;
            resultOrientation = FLIPPED_ROTATION_ORDER.get(newIndex);
        } else {
            resultOrientation = ORIENTATION_UNDEFINED;
        }

        setAttribute(TAG_ORIENTATION, Integer.toString(resultOrientation));
    }

    /**
     * Flips the image vertically.
     */
    public void flipVertically() {
        int currentOrientation = getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
        int resultOrientation;
        switch (currentOrientation) {
            case ORIENTATION_FLIP_HORIZONTAL:
                resultOrientation = ORIENTATION_ROTATE_180;
                break;
            case ORIENTATION_ROTATE_180:
                resultOrientation = ORIENTATION_FLIP_HORIZONTAL;
                break;
            case ORIENTATION_FLIP_VERTICAL:
                resultOrientation = ORIENTATION_NORMAL;
                break;
            case ORIENTATION_TRANSPOSE:
                resultOrientation = ORIENTATION_ROTATE_270;
                break;
            case ORIENTATION_ROTATE_90:
                resultOrientation = ORIENTATION_TRANSVERSE;
                break;
            case ORIENTATION_TRANSVERSE:
                resultOrientation = ORIENTATION_ROTATE_90;
                break;
            case ORIENTATION_ROTATE_270:
                resultOrientation = ORIENTATION_TRANSPOSE;
                break;
            case ORIENTATION_NORMAL:
                resultOrientation = ORIENTATION_FLIP_VERTICAL;
                break;
            case ORIENTATION_UNDEFINED:
            default:
                resultOrientation = ORIENTATION_UNDEFINED;
                break;
        }
        setAttribute(TAG_ORIENTATION, Integer.toString(resultOrientation));
    }

    /**
     * Flips the image horizontally.
     */
    public void flipHorizontally() {
        int currentOrientation = getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
        int resultOrientation;
        switch (currentOrientation) {
            case ORIENTATION_FLIP_HORIZONTAL:
                resultOrientation = ORIENTATION_NORMAL;
                break;
            case ORIENTATION_ROTATE_180:
                resultOrientation = ORIENTATION_FLIP_VERTICAL;
                break;
            case ORIENTATION_FLIP_VERTICAL:
                resultOrientation = ORIENTATION_ROTATE_180;
                break;
            case ORIENTATION_TRANSPOSE:
                resultOrientation = ORIENTATION_ROTATE_90;
                break;
            case ORIENTATION_ROTATE_90:
                resultOrientation = ORIENTATION_TRANSPOSE;
                break;
            case ORIENTATION_TRANSVERSE:
                resultOrientation = ORIENTATION_ROTATE_270;
                break;
            case ORIENTATION_ROTATE_270:
                resultOrientation = ORIENTATION_TRANSVERSE;
                break;
            case ORIENTATION_NORMAL:
                resultOrientation = ORIENTATION_FLIP_HORIZONTAL;
                break;
            case ORIENTATION_UNDEFINED:
            default:
                resultOrientation = ORIENTATION_UNDEFINED;
                break;
        }
        setAttribute(TAG_ORIENTATION, Integer.toString(resultOrientation));
    }

    /**
     * Returns if the current image orientation is flipped.
     *
     * @see #getRotationDegrees()
     */
    public boolean isFlipped() {
        int orientation = getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
        switch (orientation) {
            case ORIENTATION_FLIP_HORIZONTAL:
            case ORIENTATION_TRANSVERSE:
            case ORIENTATION_FLIP_VERTICAL:
            case ORIENTATION_TRANSPOSE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the rotation degrees for the current image orientation. If the image is flipped,
     * i.e., {@link #isFlipped()} returns {@code true}, the rotation degrees will be base on
     * the assumption that the image is first flipped horizontally (along Y-axis), and then do
     * the rotation. For example, {@link #ORIENTATION_TRANSPOSE} will be interpreted as flipped
     * horizontally first, and then rotate 270 degrees clockwise.
     *
     * @return The rotation degrees of the image after the horizontal flipping is applied, if any.
     *
     * @see #isFlipped()
     */
    public int getRotationDegrees() {
        int orientation = getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
        switch (orientation) {
            case ORIENTATION_ROTATE_90:
            case ORIENTATION_TRANSVERSE:
                return 90;
            case ORIENTATION_ROTATE_180:
            case ORIENTATION_FLIP_VERTICAL:
                return 180;
            case ORIENTATION_ROTATE_270:
            case ORIENTATION_TRANSPOSE:
                return 270;
            case ORIENTATION_UNDEFINED:
            case ORIENTATION_NORMAL:
            case ORIENTATION_FLIP_HORIZONTAL:
            default:
                return 0;
        }
    }

    /**
     * Update the values of the tags in the tag groups if any value for the tag already was stored.
     *
     * @param tag the name of the tag.
     * @param value the value of the tag in a form of {@link ExifAttribute}.
     * @return Returns {@code true} if updating is placed.
     */
    private boolean updateAttribute(String tag, ExifAttribute value) {
        boolean updated = false;
        for (int i = 0 ; i < EXIF_TAGS.length; ++i) {
            if (mAttributes[i].containsKey(tag)) {
                mAttributes[i].put(tag, value);
                updated = true;
            }
        }
        return updated;
    }

    /**
     * Remove any values of the specified tag.
     *
     * @param tag the name of the tag.
     */
    private void removeAttribute(String tag) {
        for (int i = 0 ; i < EXIF_TAGS.length; ++i) {
            mAttributes[i].remove(tag);
        }
    }

    /**
     * This function decides which parser to read the image data according to the given input stream
     * type and the content of the input stream. In each case, it reads the first three bytes to
     * determine whether the image data format is JPEG or not.
     */
    private void loadAttributes(@NonNull InputStream in) throws IOException {
        try {
            // Initialize mAttributes.
            for (int i = 0; i < EXIF_TAGS.length; ++i) {
                mAttributes[i] = new HashMap<>();
            }

            // Check file type
            in = new BufferedInputStream(in, SIGNATURE_CHECK_SIZE);
            mMimeType = getMimeType((BufferedInputStream) in);

            // Create byte-ordered input stream
            ByteOrderedDataInputStream inputStream = new ByteOrderedDataInputStream(in);

            switch (mMimeType) {
                case IMAGE_TYPE_JPEG: {
                    getJpegAttributes(inputStream, 0, IFD_TYPE_PRIMARY); // 0 is offset
                    break;
                }
                case IMAGE_TYPE_RAF: {
                    getRafAttributes(inputStream);
                    break;
                }
                case IMAGE_TYPE_ORF: {
                    getOrfAttributes(inputStream);
                    break;
                }
                case IMAGE_TYPE_RW2: {
                    getRw2Attributes(inputStream);
                    break;
                }
                case IMAGE_TYPE_ARW:
                case IMAGE_TYPE_CR2:
                case IMAGE_TYPE_DNG:
                case IMAGE_TYPE_NEF:
                case IMAGE_TYPE_NRW:
                case IMAGE_TYPE_PEF:
                case IMAGE_TYPE_SRW:
                case IMAGE_TYPE_UNKNOWN: {
                    getRawAttributes(inputStream);
                    break;
                }
                default: {
                    break;
                }
            }
            // Set thumbnail image offset and length
            setThumbnailData(inputStream);
            mIsSupportedFile = true;
        } catch (IOException e) {
            // Ignore exceptions in order to keep the compatibility with the old versions of
            // ExifInterface.
            mIsSupportedFile = false;
            if (DEBUG) {
                Log.w(TAG, "Invalid image: ExifInterface got an unsupported image format file"
                        + "(ExifInterface supports JPEG and some RAW image formats only) "
                        + "or a corrupted JPEG file to ExifInterface.", e);
            }
        } finally {
            addDefaultValuesForCompatibility();

            if (DEBUG) {
                printAttributes();
            }
        }
    }

    // Prints out attributes for debugging.
    private void printAttributes() {
        for (int i = 0; i < mAttributes.length; ++i) {
            Log.d(TAG, "The size of tag group[" + i + "]: " + mAttributes[i].size());
            for (Map.Entry<String, ExifAttribute> entry : mAttributes[i].entrySet()) {
                final ExifAttribute tagValue = entry.getValue();
                Log.d(TAG, "tagName: " + entry.getKey() + ", tagType: " + tagValue.toString()
                        + ", tagValue: '" + tagValue.getStringValue(mExifByteOrder) + "'");
            }
        }
    }

    /**
     * Save the tag data into the original image file. This is expensive because it involves
     * copying all the data from one file to another and deleting the old file and renaming the
     * other. It's best to use {@link #setAttribute(String,String)} to set all attributes to write
     * and make a single call rather than multiple calls for each attribute.
     * <p>
     * This method is only supported for JPEG files.
     * </p>
     */
    public void saveAttributes() throws IOException {
        if (!mIsSupportedFile || mMimeType != IMAGE_TYPE_JPEG) {
            throw new IOException("ExifInterface only supports saving attributes on JPEG formats.");
        }
        if (mFilename == null) {
            throw new IOException(
                    "ExifInterface does not support saving attributes for the current input.");
        }

        // Keep the thumbnail in memory
        mThumbnailBytes = getThumbnail();

        File tempFile = new File(mFilename + ".tmp");
        File originalFile = new File(mFilename);
        if (!originalFile.renameTo(tempFile)) {
            throw new IOException("Could not rename to " + tempFile.getAbsolutePath());
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            // Save the new file.
            in = new FileInputStream(tempFile);
            out = new FileOutputStream(mFilename);
            saveJpegAttributes(in, out);
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            tempFile.delete();
        }

        // Discard the thumbnail in memory
        mThumbnailBytes = null;
    }

    /**
     * Returns true if the image file has a thumbnail.
     */
    public boolean hasThumbnail() {
        return mHasThumbnail;
    }

    /**
     * Returns the JPEG compressed thumbnail inside the image file, or {@code null} if there is no
     * JPEG compressed thumbnail.
     * The returned data can be decoded using
     * {@link android.graphics.BitmapFactory#decodeByteArray(byte[],int,int)}
     */
    @Nullable
    public byte[] getThumbnail() {
        if (mThumbnailCompression == DATA_JPEG || mThumbnailCompression == DATA_JPEG_COMPRESSED) {
            return getThumbnailBytes();
        }
        return null;
    }

    /**
     * Returns the thumbnail bytes inside the image file, regardless of the compression type of the
     * thumbnail image.
     */
    @Nullable
    public byte[] getThumbnailBytes() {
        if (!mHasThumbnail) {
            return null;
        }
        if (mThumbnailBytes != null) {
            return mThumbnailBytes;
        }

        // Read the thumbnail.
        InputStream in = null;
        try {
            if (mAssetInputStream != null) {
                in = mAssetInputStream;
                if (in.markSupported()) {
                    in.reset();
                } else {
                    Log.d(TAG, "Cannot read thumbnail from inputstream without mark/reset support");
                    return null;
                }
            } else if (mFilename != null) {
                in = new FileInputStream(mFilename);
            }
            if (in == null) {
                // Should not be reached this.
                throw new FileNotFoundException();
            }
            if (in.skip(mThumbnailOffset) != mThumbnailOffset) {
                throw new IOException("Corrupted image");
            }
            byte[] buffer = new byte[mThumbnailLength];
            if (in.read(buffer) != mThumbnailLength) {
                throw new IOException("Corrupted image");
            }
            mThumbnailBytes = buffer;
            return buffer;
        } catch (IOException e) {
            // Couldn't get a thumbnail image.
            Log.d(TAG, "Encountered exception while getting thumbnail", e);
        } finally {
            closeQuietly(in);
        }
        return null;
    }

    /**
     * Creates and returns a Bitmap object of the thumbnail image based on the byte array and the
     * thumbnail compression value, or {@code null} if the compression type is unsupported.
     */
    @Nullable
    public Bitmap getThumbnailBitmap() {
        if (!mHasThumbnail) {
            return null;
        } else if (mThumbnailBytes == null) {
            mThumbnailBytes = getThumbnailBytes();
        }

        if (mThumbnailCompression == DATA_JPEG || mThumbnailCompression == DATA_JPEG_COMPRESSED) {
            return BitmapFactory.decodeByteArray(mThumbnailBytes, 0, mThumbnailLength);
        } else if (mThumbnailCompression == DATA_UNCOMPRESSED) {
            int[] rgbValues = new int[mThumbnailBytes.length / 3];
            byte alpha = (byte) 0xff000000;
            for (int i = 0; i < rgbValues.length; i++) {
                rgbValues[i] = alpha + (mThumbnailBytes[3 * i] << 16)
                        + (mThumbnailBytes[3 * i + 1] << 8) + mThumbnailBytes[3 * i + 2];
            }

            ExifAttribute imageLengthAttribute =
                    (ExifAttribute) mAttributes[IFD_TYPE_THUMBNAIL].get(TAG_IMAGE_LENGTH);
            ExifAttribute imageWidthAttribute =
                    (ExifAttribute) mAttributes[IFD_TYPE_THUMBNAIL].get(TAG_IMAGE_WIDTH);
            if (imageLengthAttribute != null && imageWidthAttribute != null) {
                int imageLength = imageLengthAttribute.getIntValue(mExifByteOrder);
                int imageWidth = imageWidthAttribute.getIntValue(mExifByteOrder);
                return Bitmap.createBitmap(
                        rgbValues, imageWidth, imageLength, Bitmap.Config.ARGB_8888);
            }
        }
        return null;
    }

    /**
     * Returns true if thumbnail image is JPEG Compressed, or false if either thumbnail image does
     * not exist or thumbnail image is uncompressed.
     */
    public boolean isThumbnailCompressed() {
        return mThumbnailCompression == DATA_JPEG || mThumbnailCompression == DATA_JPEG_COMPRESSED;
    }

    /**
     * Returns the offset and length of thumbnail inside the image file, or
     * {@code null} if there is no thumbnail.
     *
     * @return two-element array, the offset in the first value, and length in
     *         the second, or {@code null} if no thumbnail was found.
     */
    @Nullable
    public long[] getThumbnailRange() {
        if (!mHasThumbnail) {
            return null;
        }

        long[] range = new long[2];
        range[0] = mThumbnailOffset;
        range[1] = mThumbnailLength;

        return range;
    }

    /**
     * Stores the latitude and longitude value in a float array. The first element is the latitude,
     * and the second element is the longitude. Returns false if the Exif tags are not available.
     *
     * @deprecated Use {@link #getLatLong()} instead.
     */
    @Deprecated
    public boolean getLatLong(float output[]) {
        double[] latLong = getLatLong();
        if (latLong == null) {
            return false;
        }

        output[0] = (float) latLong[0];
        output[1] = (float) latLong[1];
        return true;
    }

    /**
     * Gets the latitude and longitude values.
     * <p>
     * If there are valid latitude and longitude values in the image, this method returns a double
     * array where the first element is the latitude and the second element is the longitude.
     * Otherwise, it returns null.
     */
    @Nullable
    public double[] getLatLong() {
        String latValue = getAttribute(TAG_GPS_LATITUDE);
        String latRef = getAttribute(TAG_GPS_LATITUDE_REF);
        String lngValue = getAttribute(TAG_GPS_LONGITUDE);
        String lngRef = getAttribute(TAG_GPS_LONGITUDE_REF);

        if (latValue != null && latRef != null && lngValue != null && lngRef != null) {
            try {
                double latitude = convertRationalLatLonToDouble(latValue, latRef);
                double longitude = convertRationalLatLonToDouble(lngValue, lngRef);
                return new double[] {latitude, longitude};
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Latitude/longitude values are not parseable. " +
                        String.format("latValue=%s, latRef=%s, lngValue=%s, lngRef=%s",
                                latValue, latRef, lngValue, lngRef));
            }
        }
        return null;
    }

    /**
     * Sets the GPS-related information. It will set GPS processing method, latitude and longitude
     * values, GPS timestamp, and speed information at the same time.
     *
     * @param location the {@link Location} object returned by GPS service.
     */
    public void setGpsInfo(Location location) {
        if (location == null) {
            return;
        }
        setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, location.getProvider());
        setLatLong(location.getLatitude(), location.getLongitude());
        setAltitude(location.getAltitude());
        // Location objects store speeds in m/sec. Translates it to km/hr here.
        setAttribute(TAG_GPS_SPEED_REF, "K");
        setAttribute(TAG_GPS_SPEED, new Rational(location.getSpeed()
                * TimeUnit.HOURS.toSeconds(1) / 1000).toString());
        String[] dateTime = sFormatter.format(new Date(location.getTime())).split("\\s+", -1);
        setAttribute(ExifInterface.TAG_GPS_DATESTAMP, dateTime[0]);
        setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, dateTime[1]);
    }

    /**
     * Sets the latitude and longitude values.
     *
     * @param latitude the decimal value of latitude. Must be a valid double value between -90.0 and
     *                 90.0.
     * @param longitude the decimal value of longitude. Must be a valid double value between -180.0
     *                  and 180.0.
     * @throws IllegalArgumentException If {@code latitude} or {@code longitude} is outside the
     *                                  specified range.
     */
    public void setLatLong(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0 || Double.isNaN(latitude)) {
            throw new IllegalArgumentException("Latitude value " + latitude + " is not valid.");
        }
        if (longitude < -180.0 || longitude > 180.0 || Double.isNaN(longitude)) {
            throw new IllegalArgumentException("Longitude value " + longitude + " is not valid.");
        }
        setAttribute(TAG_GPS_LATITUDE_REF, latitude >= 0 ? "N" : "S");
        setAttribute(TAG_GPS_LATITUDE, convertDecimalDegree(Math.abs(latitude)));
        setAttribute(TAG_GPS_LONGITUDE_REF, longitude >= 0 ? "E" : "W");
        setAttribute(TAG_GPS_LONGITUDE, convertDecimalDegree(Math.abs(longitude)));
    }

    /**
     * Return the altitude in meters. If the exif tag does not exist, return
     * <var>defaultValue</var>.
     *
     * @param defaultValue the value to return if the tag is not available.
     */
    public double getAltitude(double defaultValue) {
        double altitude = getAttributeDouble(TAG_GPS_ALTITUDE, -1);
        int ref = getAttributeInt(TAG_GPS_ALTITUDE_REF, -1);

        if (altitude >= 0 && ref >= 0) {
            return (altitude * ((ref == 1) ? -1 : 1));
        } else {
            return defaultValue;
        }
    }

    /**
     * Sets the altitude in meters.
     */
    public void setAltitude(double altitude) {
        String ref = altitude >= 0 ? "0" : "1";
        setAttribute(TAG_GPS_ALTITUDE, new Rational(Math.abs(altitude)).toString());
        setAttribute(TAG_GPS_ALTITUDE_REF, ref);
    }

    /**
     * Set the date time value.
     *
     * @param timeStamp number of milliseconds since Jan. 1, 1970, midnight local time.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setDateTime(long timeStamp) {
        long sub = timeStamp % 1000;
        setAttribute(TAG_DATETIME, sFormatter.format(new Date(timeStamp)));
        setAttribute(TAG_SUBSEC_TIME, Long.toString(sub));
    }

    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight local time.
     * Returns -1 if the date time information if not available.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public long getDateTime() {
        String dateTimeString = getAttribute(TAG_DATETIME);
        if (dateTimeString == null
                || !sNonZeroTimePattern.matcher(dateTimeString).matches()) return -1;

        ParsePosition pos = new ParsePosition(0);
        try {
            // The exif field is in local time. Parsing it as if it is UTC will yield time
            // since 1/1/1970 local time
            Date datetime = sFormatter.parse(dateTimeString, pos);
            if (datetime == null) return -1;
            long msecs = datetime.getTime();

            String subSecs = getAttribute(TAG_SUBSEC_TIME);
            if (subSecs != null) {
                try {
                    long sub = Long.parseLong(subSecs);
                    while (sub > 1000) {
                        sub /= 10;
                    }
                    msecs += sub;
                } catch (NumberFormatException e) {
                    // Ignored
                }
            }
            return msecs;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /**
     * Returns number of milliseconds since Jan. 1, 1970, midnight UTC.
     * Returns -1 if the date time information if not available.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public long getGpsDateTime() {
        String date = getAttribute(TAG_GPS_DATESTAMP);
        String time = getAttribute(TAG_GPS_TIMESTAMP);
        if (date == null || time == null
                || (!sNonZeroTimePattern.matcher(date).matches()
                && !sNonZeroTimePattern.matcher(time).matches())) {
            return -1;
        }

        String dateTimeString = date + ' ' + time;

        ParsePosition pos = new ParsePosition(0);
        try {
            Date datetime = sFormatter.parse(dateTimeString, pos);
            if (datetime == null) return -1;
            return datetime.getTime();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    private static double convertRationalLatLonToDouble(String rationalString, String ref) {
        try {
            String [] parts = rationalString.split(",", -1);

            String [] pair;
            pair = parts[0].split("/", -1);
            double degrees = Double.parseDouble(pair[0].trim())
                    / Double.parseDouble(pair[1].trim());

            pair = parts[1].split("/", -1);
            double minutes = Double.parseDouble(pair[0].trim())
                    / Double.parseDouble(pair[1].trim());

            pair = parts[2].split("/", -1);
            double seconds = Double.parseDouble(pair[0].trim())
                    / Double.parseDouble(pair[1].trim());

            double result = degrees + (minutes / 60.0) + (seconds / 3600.0);
            if ((ref.equals("S") || ref.equals("W"))) {
                return -result;
            } else if (ref.equals("N") || ref.equals("E")) {
                return result;
            } else {
                // Not valid
                throw new IllegalArgumentException();
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // Not valid
            throw new IllegalArgumentException();
        }
    }

    private String convertDecimalDegree(double decimalDegree) {
        long degrees = (long) decimalDegree;
        long minutes = (long) ((decimalDegree - degrees) * 60.0);
        long seconds = Math.round((decimalDegree - degrees - minutes / 60.0) * 3600.0 * 1e7);
        return degrees + "/1," + minutes + "/1," + seconds + "/10000000";
    }

    // Checks the type of image file
    private int getMimeType(BufferedInputStream in) throws IOException {
        in.mark(SIGNATURE_CHECK_SIZE);
        byte[] signatureCheckBytes = new byte[SIGNATURE_CHECK_SIZE];
        in.read(signatureCheckBytes);
        in.reset();
        if (isJpegFormat(signatureCheckBytes)) {
            return IMAGE_TYPE_JPEG;
        } else if (isRafFormat(signatureCheckBytes)) {
            return IMAGE_TYPE_RAF;
        } else if (isOrfFormat(signatureCheckBytes)) {
            return IMAGE_TYPE_ORF;
        } else if (isRw2Format(signatureCheckBytes)) {
            return IMAGE_TYPE_RW2;
        }
        // Certain file formats (PEF) are identified in readImageFileDirectory()
        return IMAGE_TYPE_UNKNOWN;
    }

    /**
     * This method looks at the first 3 bytes to determine if this file is a JPEG file.
     * See http://www.media.mit.edu/pia/Research/deepview/exif.html, "JPEG format and Marker"
     */
    private static boolean isJpegFormat(byte[] signatureCheckBytes) throws IOException {
        for (int i = 0; i < JPEG_SIGNATURE.length; i++) {
            if (signatureCheckBytes[i] != JPEG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method looks at the first 15 bytes to determine if this file is a RAF file.
     * There is no official specification for RAF files from Fuji, but there is an online archive of
     * image file specifications:
     * http://fileformats.archiveteam.org/wiki/Fujifilm_RAF
     */
    private boolean isRafFormat(byte[] signatureCheckBytes) throws IOException {
        byte[] rafSignatureBytes = RAF_SIGNATURE.getBytes(Charset.defaultCharset());
        for (int i = 0; i < rafSignatureBytes.length; i++) {
            if (signatureCheckBytes[i] != rafSignatureBytes[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * ORF has a similar structure to TIFF but it contains a different signature at the TIFF Header.
     * This method looks at the 2 bytes following the Byte Order bytes to determine if this file is
     * an ORF file.
     * There is no official specification for ORF files from Olympus, but there is an online archive
     * of image file specifications:
     * http://fileformats.archiveteam.org/wiki/Olympus_ORF
     */
    private boolean isOrfFormat(byte[] signatureCheckBytes) throws IOException {
        ByteOrderedDataInputStream signatureInputStream =
                new ByteOrderedDataInputStream(signatureCheckBytes);
        // Read byte order
        mExifByteOrder = readByteOrder(signatureInputStream);
        // Set byte order
        signatureInputStream.setByteOrder(mExifByteOrder);

        short orfSignature = signatureInputStream.readShort();
        signatureInputStream.close();
        return orfSignature == ORF_SIGNATURE_1 || orfSignature == ORF_SIGNATURE_2;
    }

    /**
     * RW2 is TIFF-based, but stores 0x55 signature byte instead of 0x42 at the header
     * See http://lclevy.free.fr/raw/
     */
    private boolean isRw2Format(byte[] signatureCheckBytes) throws IOException {
        ByteOrderedDataInputStream signatureInputStream =
                new ByteOrderedDataInputStream(signatureCheckBytes);
        // Read byte order
        mExifByteOrder = readByteOrder(signatureInputStream);
        // Set byte order
        signatureInputStream.setByteOrder(mExifByteOrder);

        short signatureByte = signatureInputStream.readShort();
        signatureInputStream.close();
        return signatureByte == RW2_SIGNATURE;
    }

    /**
     * Loads EXIF attributes from a JPEG input stream.
     *
     * @param in The input stream that starts with the JPEG data.
     * @param jpegOffset The offset value in input stream for JPEG data.
     * @param imageType The image type from which to retrieve metadata. Use IFD_TYPE_PRIMARY for
     *                   primary image, IFD_TYPE_PREVIEW for preview image, and
     *                   IFD_TYPE_THUMBNAIL for thumbnail image.
     * @throws IOException If the data contains invalid JPEG markers, offsets, or length values.
     */
    private void getJpegAttributes(ByteOrderedDataInputStream in, int jpegOffset, int imageType)
            throws IOException {
        // See JPEG File Interchange Format Specification, "JFIF Specification"
        if (DEBUG) {
            Log.d(TAG, "getJpegAttributes starting with: " + in);
        }

        // JPEG uses Big Endian by default. See https://people.cs.umass.edu/~verts/cs32/endian.html
        in.setByteOrder(ByteOrder.BIG_ENDIAN);

        // Skip to JPEG data
        in.seek(jpegOffset);
        int bytesRead = jpegOffset;

        byte marker;
        if ((marker = in.readByte()) != MARKER) {
            throw new IOException("Invalid marker: " + Integer.toHexString(marker & 0xff));
        }
        ++bytesRead;
        if (in.readByte() != MARKER_SOI) {
            throw new IOException("Invalid marker: " + Integer.toHexString(marker & 0xff));
        }
        ++bytesRead;
        while (true) {
            marker = in.readByte();
            if (marker != MARKER) {
                throw new IOException("Invalid marker:" + Integer.toHexString(marker & 0xff));
            }
            ++bytesRead;
            marker = in.readByte();
            if (DEBUG) {
                Log.d(TAG, "Found JPEG segment indicator: " + Integer.toHexString(marker & 0xff));
            }
            ++bytesRead;

            // EOI indicates the end of an image and in case of SOS, JPEG image stream starts and
            // the image data will terminate right after.
            if (marker == MARKER_EOI || marker == MARKER_SOS) {
                break;
            }
            int length = in.readUnsignedShort() - 2;
            bytesRead += 2;
            if (DEBUG) {
                Log.d(TAG, "JPEG segment: " + Integer.toHexString(marker & 0xff) + " (length: "
                        + (length + 2) + ")");
            }
            if (length < 0) {
                throw new IOException("Invalid length");
            }
            switch (marker) {
                case MARKER_APP1: {
                    if (DEBUG) {
                        Log.d(TAG, "MARKER_APP1");
                    }
                    if (length < 6) {
                        // Skip if it's not an EXIF APP1 segment.
                        break;
                    }
                    byte[] identifier = new byte[6];
                    if (in.read(identifier) != 6) {
                        throw new IOException("Invalid exif");
                    }
                    bytesRead += 6;
                    length -= 6;
                    if (!Arrays.equals(identifier, IDENTIFIER_EXIF_APP1)) {
                        // Skip if it's not an EXIF APP1 segment.
                        break;
                    }
                    if (length <= 0) {
                        throw new IOException("Invalid exif");
                    }
                    if (DEBUG) {
                        Log.d(TAG, "readExifSegment with a byte array (length: " + length + ")");
                    }
                    // Save offset values for createJpegThumbnailBitmap() function
                    mExifOffset = bytesRead;

                    byte[] bytes = new byte[length];
                    if (in.read(bytes) != length) {
                        throw new IOException("Invalid exif");
                    }
                    bytesRead += length;
                    length = 0;

                    readExifSegment(bytes, imageType);
                    break;
                }

                case MARKER_COM: {
                    byte[] bytes = new byte[length];
                    if (in.read(bytes) != length) {
                        throw new IOException("Invalid exif");
                    }
                    length = 0;
                    if (getAttribute(TAG_USER_COMMENT) == null) {
                        mAttributes[IFD_TYPE_EXIF].put(TAG_USER_COMMENT, ExifAttribute.createString(
                                new String(bytes, ASCII)));
                    }
                    break;
                }

                case MARKER_SOF0:
                case MARKER_SOF1:
                case MARKER_SOF2:
                case MARKER_SOF3:
                case MARKER_SOF5:
                case MARKER_SOF6:
                case MARKER_SOF7:
                case MARKER_SOF9:
                case MARKER_SOF10:
                case MARKER_SOF11:
                case MARKER_SOF13:
                case MARKER_SOF14:
                case MARKER_SOF15: {
                    if (in.skipBytes(1) != 1) {
                        throw new IOException("Invalid SOFx");
                    }
                    mAttributes[imageType].put(TAG_IMAGE_LENGTH, ExifAttribute.createULong(
                            in.readUnsignedShort(), mExifByteOrder));
                    mAttributes[imageType].put(TAG_IMAGE_WIDTH, ExifAttribute.createULong(
                            in.readUnsignedShort(), mExifByteOrder));
                    length -= 5;
                    break;
                }

                default: {
                    break;
                }
            }
            if (length < 0) {
                throw new IOException("Invalid length");
            }
            if (in.skipBytes(length) != length) {
                throw new IOException("Invalid JPEG segment");
            }
            bytesRead += length;
        }
        // Restore original byte order
        in.setByteOrder(mExifByteOrder);
    }

    private void getRawAttributes(ByteOrderedDataInputStream in) throws IOException {
        // Parse TIFF Headers. See JEITA CP-3451C Section 4.5.2. Table 1.
        parseTiffHeaders(in, in.available());

        // Read TIFF image file directories. See JEITA CP-3451C Section 4.5.2. Figure 6.
        readImageFileDirectory(in, IFD_TYPE_PRIMARY);

        // Update ImageLength/Width tags for all image data.
        updateImageSizeValues(in, IFD_TYPE_PRIMARY);
        updateImageSizeValues(in, IFD_TYPE_PREVIEW);
        updateImageSizeValues(in, IFD_TYPE_THUMBNAIL);

        // Check if each image data is in valid position.
        validateImages(in);

        if (mMimeType == IMAGE_TYPE_PEF) {
            // PEF files contain a MakerNote data, which contains the data for ColorSpace tag.
            // See http://lclevy.free.fr/raw/ and piex.cc PefGetPreviewData()
            ExifAttribute makerNoteAttribute =
                    (ExifAttribute) mAttributes[IFD_TYPE_EXIF].get(TAG_MAKER_NOTE);
            if (makerNoteAttribute != null) {
                // Create an ordered DataInputStream for MakerNote
                ByteOrderedDataInputStream makerNoteDataInputStream =
                        new ByteOrderedDataInputStream(makerNoteAttribute.bytes);
                makerNoteDataInputStream.setByteOrder(mExifByteOrder);

                // Seek to MakerNote data
                makerNoteDataInputStream.seek(PEF_MAKER_NOTE_SKIP_SIZE);

                // Read IFD data from MakerNote
                readImageFileDirectory(makerNoteDataInputStream, IFD_TYPE_PEF);

                // Update ColorSpace tag
                ExifAttribute colorSpaceAttribute =
                        (ExifAttribute) mAttributes[IFD_TYPE_PEF].get(TAG_COLOR_SPACE);
                if (colorSpaceAttribute != null) {
                    mAttributes[IFD_TYPE_EXIF].put(TAG_COLOR_SPACE, colorSpaceAttribute);
                }
            }
        }
    }

    /**
     * RAF files contains a JPEG and a CFA data.
     * The JPEG contains two images, a preview and a thumbnail, while the CFA contains a RAW image.
     * This method looks at the first 160 bytes of a RAF file to retrieve the offset and length
     * values for the JPEG and CFA data.
     * Using that data, it parses the JPEG data to retrieve the preview and thumbnail image data,
     * then parses the CFA metadata to retrieve the primary image length/width values.
     * For data format details, see http://fileformats.archiveteam.org/wiki/Fujifilm_RAF
     */
    private void getRafAttributes(ByteOrderedDataInputStream in) throws IOException {
        // Retrieve offset & length values
        in.skipBytes(RAF_OFFSET_TO_JPEG_IMAGE_OFFSET);
        byte[] jpegOffsetBytes = new byte[4];
        byte[] cfaHeaderOffsetBytes = new byte[4];
        in.read(jpegOffsetBytes);
        // Skip JPEG length value since it is not needed
        in.skipBytes(RAF_JPEG_LENGTH_VALUE_SIZE);
        in.read(cfaHeaderOffsetBytes);
        int rafJpegOffset = ByteBuffer.wrap(jpegOffsetBytes).getInt();
        int rafCfaHeaderOffset = ByteBuffer.wrap(cfaHeaderOffsetBytes).getInt();

        // Retrieve JPEG image metadata
        getJpegAttributes(in, rafJpegOffset, IFD_TYPE_PREVIEW);

        // Skip to CFA header offset.
        in.seek(rafCfaHeaderOffset);

        // Retrieve primary image length/width values, if TAG_RAF_IMAGE_SIZE exists
        in.setByteOrder(ByteOrder.BIG_ENDIAN);
        int numberOfDirectoryEntry = in.readInt();
        if (DEBUG) {
            Log.d(TAG, "numberOfDirectoryEntry: " + numberOfDirectoryEntry);
        }
        // CFA stores some metadata about the RAW image. Since CFA uses proprietary tags, can only
        // find and retrieve image size information tags, while skipping others.
        // See piex.cc RafGetDimension()
        for (int i = 0; i < numberOfDirectoryEntry; ++i) {
            int tagNumber = in.readUnsignedShort();
            int numberOfBytes = in.readUnsignedShort();
            if (tagNumber == TAG_RAF_IMAGE_SIZE.number) {
                int imageLength = in.readShort();
                int imageWidth = in.readShort();
                ExifAttribute imageLengthAttribute =
                        ExifAttribute.createUShort(imageLength, mExifByteOrder);
                ExifAttribute imageWidthAttribute =
                        ExifAttribute.createUShort(imageWidth, mExifByteOrder);
                mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_LENGTH, imageLengthAttribute);
                mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_WIDTH, imageWidthAttribute);
                if (DEBUG) {
                    Log.d(TAG, "Updated to length: " + imageLength + ", width: " + imageWidth);
                }
                return;
            }
            in.skipBytes(numberOfBytes);
        }
    }

    /**
     * ORF files contains a primary image data and a MakerNote data that contains preview/thumbnail
     * images. Both data takes the form of IFDs and can therefore be read with the
     * readImageFileDirectory() method.
     * This method reads all the necessary data and updates the primary/preview/thumbnail image
     * information according to the GetOlympusPreviewImage() method in piex.cc.
     * For data format details, see the following:
     * http://fileformats.archiveteam.org/wiki/Olympus_ORF
     * https://libopenraw.freedesktop.org/wiki/Olympus_ORF
     */
    private void getOrfAttributes(ByteOrderedDataInputStream in) throws IOException {
        // Retrieve primary image data
        // Other Exif data will be located in the Makernote.
        getRawAttributes(in);

        // Additionally retrieve preview/thumbnail information from MakerNote tag, which contains
        // proprietary tags and therefore does not have offical documentation
        // See GetOlympusPreviewImage() in piex.cc & http://www.exiv2.org/tags-olympus.html
        ExifAttribute makerNoteAttribute =
                (ExifAttribute) mAttributes[IFD_TYPE_EXIF].get(TAG_MAKER_NOTE);
        if (makerNoteAttribute != null) {
            // Create an ordered DataInputStream for MakerNote
            ByteOrderedDataInputStream makerNoteDataInputStream =
                    new ByteOrderedDataInputStream(makerNoteAttribute.bytes);
            makerNoteDataInputStream.setByteOrder(mExifByteOrder);

            // There are two types of headers for Olympus MakerNotes
            // See http://www.exiv2.org/makernote.html#R1
            byte[] makerNoteHeader1Bytes = new byte[ORF_MAKER_NOTE_HEADER_1.length];
            makerNoteDataInputStream.readFully(makerNoteHeader1Bytes);
            makerNoteDataInputStream.seek(0);
            byte[] makerNoteHeader2Bytes = new byte[ORF_MAKER_NOTE_HEADER_2.length];
            makerNoteDataInputStream.readFully(makerNoteHeader2Bytes);
            // Skip the corresponding amount of bytes for each header type
            if (Arrays.equals(makerNoteHeader1Bytes, ORF_MAKER_NOTE_HEADER_1)) {
                makerNoteDataInputStream.seek(ORF_MAKER_NOTE_HEADER_1_SIZE);
            } else if (Arrays.equals(makerNoteHeader2Bytes, ORF_MAKER_NOTE_HEADER_2)) {
                makerNoteDataInputStream.seek(ORF_MAKER_NOTE_HEADER_2_SIZE);
            }

            // Read IFD data from MakerNote
            readImageFileDirectory(makerNoteDataInputStream, IFD_TYPE_ORF_MAKER_NOTE);

            // Retrieve & update preview image offset & length values
            ExifAttribute imageStartAttribute = (ExifAttribute)
                    mAttributes[IFD_TYPE_ORF_CAMERA_SETTINGS].get(TAG_ORF_PREVIEW_IMAGE_START);
            ExifAttribute imageLengthAttribute = (ExifAttribute)
                    mAttributes[IFD_TYPE_ORF_CAMERA_SETTINGS].get(TAG_ORF_PREVIEW_IMAGE_LENGTH);

            if (imageStartAttribute != null && imageLengthAttribute != null) {
                mAttributes[IFD_TYPE_PREVIEW].put(TAG_JPEG_INTERCHANGE_FORMAT,
                        imageStartAttribute);
                mAttributes[IFD_TYPE_PREVIEW].put(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                        imageLengthAttribute);
            }

            // TODO: Check this behavior in other ORF files
            // Retrieve primary image length & width values
            // See piex.cc GetOlympusPreviewImage()
            ExifAttribute aspectFrameAttribute = (ExifAttribute)
                    mAttributes[IFD_TYPE_ORF_IMAGE_PROCESSING].get(TAG_ORF_ASPECT_FRAME);
            if (aspectFrameAttribute != null) {
                int[] aspectFrameValues = (int[]) aspectFrameAttribute.getValue(mExifByteOrder);
                if (aspectFrameValues == null || aspectFrameValues.length != 4) {
                    Log.w(TAG, "Invalid aspect frame values. frame="
                            + Arrays.toString(aspectFrameValues));
                    return;
                }
                if (aspectFrameValues[2] > aspectFrameValues[0] &&
                        aspectFrameValues[3] > aspectFrameValues[1]) {
                    int primaryImageWidth = aspectFrameValues[2] - aspectFrameValues[0] + 1;
                    int primaryImageLength = aspectFrameValues[3] - aspectFrameValues[1] + 1;
                    // Swap width & length values
                    if (primaryImageWidth < primaryImageLength) {
                        primaryImageWidth += primaryImageLength;
                        primaryImageLength = primaryImageWidth - primaryImageLength;
                        primaryImageWidth -= primaryImageLength;
                    }
                    ExifAttribute primaryImageWidthAttribute =
                            ExifAttribute.createUShort(primaryImageWidth, mExifByteOrder);
                    ExifAttribute primaryImageLengthAttribute =
                            ExifAttribute.createUShort(primaryImageLength, mExifByteOrder);

                    mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_WIDTH, primaryImageWidthAttribute);
                    mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_LENGTH, primaryImageLengthAttribute);
                }
            }
        }
    }

    // RW2 contains the primary image data in IFD0 and the preview and/or thumbnail image data in
    // the JpgFromRaw tag
    // See https://libopenraw.freedesktop.org/wiki/Panasonic_RAW/ and piex.cc Rw2GetPreviewData()
    private void getRw2Attributes(ByteOrderedDataInputStream in) throws IOException {
        // Retrieve primary image data
        getRawAttributes(in);

        // Retrieve preview and/or thumbnail image data
        ExifAttribute jpgFromRawAttribute =
                (ExifAttribute) mAttributes[IFD_TYPE_PRIMARY].get(TAG_RW2_JPG_FROM_RAW);
        if (jpgFromRawAttribute != null) {
            getJpegAttributes(in, mRw2JpgFromRawOffset, IFD_TYPE_PREVIEW);
        }

        // Set ISO tag value if necessary
        ExifAttribute rw2IsoAttribute =
                (ExifAttribute) mAttributes[IFD_TYPE_PRIMARY].get(TAG_RW2_ISO);
        ExifAttribute exifIsoAttribute =
                (ExifAttribute) mAttributes[IFD_TYPE_EXIF].get(TAG_PHOTOGRAPHIC_SENSITIVITY);
        if (rw2IsoAttribute != null && exifIsoAttribute == null) {
            // Place this attribute only if it doesn't exist
            mAttributes[IFD_TYPE_EXIF].put(TAG_PHOTOGRAPHIC_SENSITIVITY, rw2IsoAttribute);
        }
    }

    // Stores a new JPEG image with EXIF attributes into a given output stream.
    private void saveJpegAttributes(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        // See JPEG File Interchange Format Specification, "JFIF Specification"
        if (DEBUG) {
            Log.d(TAG, "saveJpegAttributes starting with (inputStream: " + inputStream
                    + ", outputStream: " + outputStream + ")");
        }
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        ByteOrderedDataOutputStream dataOutputStream =
                new ByteOrderedDataOutputStream(outputStream, ByteOrder.BIG_ENDIAN);
        if (dataInputStream.readByte() != MARKER) {
            throw new IOException("Invalid marker");
        }
        dataOutputStream.writeByte(MARKER);
        if (dataInputStream.readByte() != MARKER_SOI) {
            throw new IOException("Invalid marker");
        }
        dataOutputStream.writeByte(MARKER_SOI);

        // Write EXIF APP1 segment
        dataOutputStream.writeByte(MARKER);
        dataOutputStream.writeByte(MARKER_APP1);
        writeExifSegment(dataOutputStream, 6);

        byte[] bytes = new byte[4096];

        while (true) {
            byte marker = dataInputStream.readByte();
            if (marker != MARKER) {
                throw new IOException("Invalid marker");
            }
            marker = dataInputStream.readByte();
            switch (marker) {
                case MARKER_APP1: {
                    int length = dataInputStream.readUnsignedShort() - 2;
                    if (length < 0) {
                        throw new IOException("Invalid length");
                    }
                    byte[] identifier = new byte[6];
                    if (length >= 6) {
                        if (dataInputStream.read(identifier) != 6) {
                            throw new IOException("Invalid exif");
                        }
                        if (Arrays.equals(identifier, IDENTIFIER_EXIF_APP1)) {
                            // Skip the original EXIF APP1 segment.
                            if (dataInputStream.skipBytes(length - 6) != length - 6) {
                                throw new IOException("Invalid length");
                            }
                            break;
                        }
                    }
                    // Copy non-EXIF APP1 segment.
                    dataOutputStream.writeByte(MARKER);
                    dataOutputStream.writeByte(marker);
                    dataOutputStream.writeUnsignedShort(length + 2);
                    if (length >= 6) {
                        length -= 6;
                        dataOutputStream.write(identifier);
                    }
                    int read;
                    while (length > 0 && (read = dataInputStream.read(
                            bytes, 0, Math.min(length, bytes.length))) >= 0) {
                        dataOutputStream.write(bytes, 0, read);
                        length -= read;
                    }
                    break;
                }
                case MARKER_EOI:
                case MARKER_SOS: {
                    dataOutputStream.writeByte(MARKER);
                    dataOutputStream.writeByte(marker);
                    // Copy all the remaining data
                    copy(dataInputStream, dataOutputStream);
                    return;
                }
                default: {
                    // Copy JPEG segment
                    dataOutputStream.writeByte(MARKER);
                    dataOutputStream.writeByte(marker);
                    int length = dataInputStream.readUnsignedShort();
                    dataOutputStream.writeUnsignedShort(length);
                    length -= 2;
                    if (length < 0) {
                        throw new IOException("Invalid length");
                    }
                    int read;
                    while (length > 0 && (read = dataInputStream.read(
                            bytes, 0, Math.min(length, bytes.length))) >= 0) {
                        dataOutputStream.write(bytes, 0, read);
                        length -= read;
                    }
                    break;
                }
            }
        }
    }

    // Reads the given EXIF byte area and save its tag data into attributes.
    private void readExifSegment(byte[] exifBytes, int imageType) throws IOException {
        ByteOrderedDataInputStream dataInputStream =
                new ByteOrderedDataInputStream(exifBytes);

        // Parse TIFF Headers. See JEITA CP-3451C Section 4.5.2. Table 1.
        parseTiffHeaders(dataInputStream, exifBytes.length);

        // Read TIFF image file directories. See JEITA CP-3451C Section 4.5.2. Figure 6.
        readImageFileDirectory(dataInputStream, imageType);
    }

    private void addDefaultValuesForCompatibility() {
        // If DATETIME tag has no value, then set the value to DATETIME_ORIGINAL tag's.
        String valueOfDateTimeOriginal = getAttribute(TAG_DATETIME_ORIGINAL);
        if (valueOfDateTimeOriginal != null && getAttribute(TAG_DATETIME) == null) {
            mAttributes[IFD_TYPE_PRIMARY].put(TAG_DATETIME,
                    ExifAttribute.createString(valueOfDateTimeOriginal));
        }

        // Add the default value.
        if (getAttribute(TAG_IMAGE_WIDTH) == null) {
            mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_WIDTH,
                    ExifAttribute.createULong(0, mExifByteOrder));
        }
        if (getAttribute(TAG_IMAGE_LENGTH) == null) {
            mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_LENGTH,
                    ExifAttribute.createULong(0, mExifByteOrder));
        }
        if (getAttribute(TAG_ORIENTATION) == null) {
            mAttributes[IFD_TYPE_PRIMARY].put(TAG_ORIENTATION,
                    ExifAttribute.createULong(0, mExifByteOrder));
        }
        if (getAttribute(TAG_LIGHT_SOURCE) == null) {
            mAttributes[IFD_TYPE_EXIF].put(TAG_LIGHT_SOURCE,
                    ExifAttribute.createULong(0, mExifByteOrder));
        }
    }

    private ByteOrder readByteOrder(ByteOrderedDataInputStream dataInputStream)
            throws IOException {
        // Read byte order.
        short byteOrder = dataInputStream.readShort();
        switch (byteOrder) {
            case BYTE_ALIGN_II:
                if (DEBUG) {
                    Log.d(TAG, "readExifSegment: Byte Align II");
                }
                return ByteOrder.LITTLE_ENDIAN;
            case BYTE_ALIGN_MM:
                if (DEBUG) {
                    Log.d(TAG, "readExifSegment: Byte Align MM");
                }
                return ByteOrder.BIG_ENDIAN;
            default:
                throw new IOException("Invalid byte order: " + Integer.toHexString(byteOrder));
        }
    }

    private void parseTiffHeaders(ByteOrderedDataInputStream dataInputStream,
            int exifBytesLength) throws IOException {
        // Read byte order
        mExifByteOrder = readByteOrder(dataInputStream);
        // Set byte order
        dataInputStream.setByteOrder(mExifByteOrder);

        // Check start code
        int startCode = dataInputStream.readUnsignedShort();
        if (mMimeType != IMAGE_TYPE_ORF && mMimeType != IMAGE_TYPE_RW2 && startCode != START_CODE) {
            throw new IOException("Invalid start code: " + Integer.toHexString(startCode));
        }

        // Read and skip to first ifd offset
        int firstIfdOffset = dataInputStream.readInt();
        if (firstIfdOffset < 8 || firstIfdOffset >= exifBytesLength) {
            throw new IOException("Invalid first Ifd offset: " + firstIfdOffset);
        }
        firstIfdOffset -= 8;
        if (firstIfdOffset > 0) {
            if (dataInputStream.skipBytes(firstIfdOffset) != firstIfdOffset) {
                throw new IOException("Couldn't jump to first Ifd: " + firstIfdOffset);
            }
        }
    }

    // Reads image file directory, which is a tag group in EXIF.
    private void readImageFileDirectory(ByteOrderedDataInputStream dataInputStream,
            @IfdType int ifdType) throws IOException {
        if (dataInputStream.mPosition + 2 > dataInputStream.mLength) {
            // Return if there is no data from the offset.
            return;
        }
        // See TIFF 6.0 Section 2: TIFF Structure, Figure 1.
        short numberOfDirectoryEntry = dataInputStream.readShort();
        if (DEBUG) {
            Log.d(TAG, "numberOfDirectoryEntry: " + numberOfDirectoryEntry);
        }
        if (dataInputStream.mPosition + 12 * numberOfDirectoryEntry > dataInputStream.mLength) {
            // Return if the size of entries is too big.
            return;
        }

        // See TIFF 6.0 Section 2: TIFF Structure, "Image File Directory".
        for (short i = 0; i < numberOfDirectoryEntry; ++i) {
            int tagNumber = dataInputStream.readUnsignedShort();
            int dataFormat = dataInputStream.readUnsignedShort();
            int numberOfComponents = dataInputStream.readInt();
            // Next four bytes is for data offset or value.
            long nextEntryOffset = dataInputStream.peek() + 4L;

            // Look up a corresponding tag from tag number
            ExifTag tag = (ExifTag) sExifTagMapsForReading[ifdType].get(tagNumber);

            if (DEBUG) {
                Log.d(TAG, String.format("ifdType: %d, tagNumber: %d, tagName: %s, dataFormat: %d, "
                        + "numberOfComponents: %d", ifdType, tagNumber,
                        tag != null ? tag.name : null, dataFormat, numberOfComponents));
            }

            long byteCount = 0;
            boolean valid = false;
            if (tag == null) {
                Log.w(TAG, "Skip the tag entry since tag number is not defined: " + tagNumber);
            } else if (dataFormat <= 0 || dataFormat >= IFD_FORMAT_BYTES_PER_FORMAT.length) {
                Log.w(TAG, "Skip the tag entry since data format is invalid: " + dataFormat);
            } else if (!tag.isFormatCompatible(dataFormat)) {
                Log.w(TAG, "Skip the tag entry since data format (" + IFD_FORMAT_NAMES[dataFormat]
                        + ") is unexpected for tag: " + tag.name);
            } else {
                if (dataFormat == IFD_FORMAT_UNDEFINED) {
                    dataFormat = tag.primaryFormat;
                }
                byteCount = (long) numberOfComponents * IFD_FORMAT_BYTES_PER_FORMAT[dataFormat];
                if (byteCount < 0 || byteCount > Integer.MAX_VALUE) {
                    Log.w(TAG, "Skip the tag entry since the number of components is invalid: "
                            + numberOfComponents);
                } else {
                    valid = true;
                }
            }
            if (!valid) {
                dataInputStream.seek(nextEntryOffset);
                continue;
            }

            // Read a value from data field or seek to the value offset which is stored in data
            // field if the size of the entry value is bigger than 4.
            if (byteCount > 4) {
                int offset = dataInputStream.readInt();
                if (DEBUG) {
                    Log.d(TAG, "seek to data offset: " + offset);
                }
                if (mMimeType == IMAGE_TYPE_ORF) {
                    if (TAG_MAKER_NOTE.equals(tag.name)) {
                        // Save offset value for reading thumbnail
                        mOrfMakerNoteOffset = offset;
                    } else if (ifdType == IFD_TYPE_ORF_MAKER_NOTE
                            && TAG_ORF_THUMBNAIL_IMAGE.equals(tag.name)) {
                        // Retrieve & update values for thumbnail offset and length values for ORF
                        mOrfThumbnailOffset = offset;
                        mOrfThumbnailLength = numberOfComponents;

                        ExifAttribute compressionAttribute =
                                ExifAttribute.createUShort(DATA_JPEG, mExifByteOrder);
                        ExifAttribute jpegInterchangeFormatAttribute =
                                ExifAttribute.createULong(mOrfThumbnailOffset, mExifByteOrder);
                        ExifAttribute jpegInterchangeFormatLengthAttribute =
                                ExifAttribute.createULong(mOrfThumbnailLength, mExifByteOrder);

                        mAttributes[IFD_TYPE_THUMBNAIL].put(TAG_COMPRESSION, compressionAttribute);
                        mAttributes[IFD_TYPE_THUMBNAIL].put(TAG_JPEG_INTERCHANGE_FORMAT,
                                jpegInterchangeFormatAttribute);
                        mAttributes[IFD_TYPE_THUMBNAIL].put(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                                jpegInterchangeFormatLengthAttribute);
                    }
                } else if (mMimeType == IMAGE_TYPE_RW2) {
                    if (TAG_RW2_JPG_FROM_RAW.equals(tag.name)) {
                        mRw2JpgFromRawOffset = offset;
                    }
                }
                if (offset + byteCount <= dataInputStream.mLength) {
                    dataInputStream.seek(offset);
                } else {
                    // Skip if invalid data offset.
                    Log.w(TAG, "Skip the tag entry since data offset is invalid: " + offset);
                    dataInputStream.seek(nextEntryOffset);
                    continue;
                }
            }

            // Recursively parse IFD when a IFD pointer tag appears.
            Integer nextIfdType = sExifPointerTagMap.get(tagNumber);
            if (DEBUG) {
                Log.d(TAG, "nextIfdType: " + nextIfdType + " byteCount: " + byteCount);
            }

            if (nextIfdType != null) {
                long offset = -1L;
                // Get offset from data field
                switch (dataFormat) {
                    case IFD_FORMAT_USHORT: {
                        offset = dataInputStream.readUnsignedShort();
                        break;
                    }
                    case IFD_FORMAT_SSHORT: {
                        offset = dataInputStream.readShort();
                        break;
                    }
                    case IFD_FORMAT_ULONG: {
                        offset = dataInputStream.readUnsignedInt();
                        break;
                    }
                    case IFD_FORMAT_SLONG:
                    case IFD_FORMAT_IFD: {
                        offset = dataInputStream.readInt();
                        break;
                    }
                    default: {
                        // Nothing to do
                        break;
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, String.format("Offset: %d, tagName: %s", offset, tag.name));
                }
                if (offset > 0L && offset < dataInputStream.mLength) {
                    dataInputStream.seek(offset);
                    readImageFileDirectory(dataInputStream, nextIfdType);
                } else {
                    Log.w(TAG, "Skip jump into the IFD since its offset is invalid: " + offset);
                }

                dataInputStream.seek(nextEntryOffset);
                continue;
            }

            byte[] bytes = new byte[(int) byteCount];
            dataInputStream.readFully(bytes);
            ExifAttribute attribute = new ExifAttribute(dataFormat, numberOfComponents, bytes);
            mAttributes[ifdType].put(tag.name, attribute);

            // DNG files have a DNG Version tag specifying the version of specifications that the
            // image file is following.
            // See http://fileformats.archiveteam.org/wiki/DNG
            if (TAG_DNG_VERSION.equals(tag.name)) {
                mMimeType = IMAGE_TYPE_DNG;
            }

            // PEF files have a Make or Model tag that begins with "PENTAX" or a compression tag
            // that is 65535.
            // See http://fileformats.archiveteam.org/wiki/Pentax_PEF
            if (((TAG_MAKE.equals(tag.name) || TAG_MODEL.equals(tag.name))
                    && attribute.getStringValue(mExifByteOrder).contains(PEF_SIGNATURE))
                    || (TAG_COMPRESSION.equals(tag.name)
                    && attribute.getIntValue(mExifByteOrder) == 65535)) {
                mMimeType = IMAGE_TYPE_PEF;
            }

            // Seek to next tag offset
            if (dataInputStream.peek() != nextEntryOffset) {
                dataInputStream.seek(nextEntryOffset);
            }
        }

        if (dataInputStream.peek() + 4 <= dataInputStream.mLength) {
            int nextIfdOffset = dataInputStream.readInt();
            if (DEBUG) {
                Log.d(TAG, String.format("nextIfdOffset: %d", nextIfdOffset));
            }
            // The next IFD offset needs to be bigger than 8
            // since the first IFD offset is at least 8.
            if (nextIfdOffset > 8 && nextIfdOffset < dataInputStream.mLength) {
                dataInputStream.seek(nextIfdOffset);
                if (mAttributes[IFD_TYPE_THUMBNAIL].isEmpty()) {
                    // Do not overwrite thumbnail IFD data if it alreay exists.
                    readImageFileDirectory(dataInputStream, IFD_TYPE_THUMBNAIL);
                } else if (mAttributes[IFD_TYPE_PREVIEW].isEmpty()) {
                    readImageFileDirectory(dataInputStream, IFD_TYPE_PREVIEW);
                }
            }
        }
    }

    /**
     * JPEG compressed images do not contain IMAGE_LENGTH & IMAGE_WIDTH tags.
     * This value uses JpegInterchangeFormat(JPEG data offset) value, and calls getJpegAttributes()
     * to locate SOF(Start of Frame) marker and update the image length & width values.
     * See JEITA CP-3451C Table 5 and Section 4.8.1. B.
     */
    private void retrieveJpegImageSize(ByteOrderedDataInputStream in, int imageType)
            throws IOException {
        // Check if image already has IMAGE_LENGTH & IMAGE_WIDTH values
        ExifAttribute imageLengthAttribute =
                (ExifAttribute) mAttributes[imageType].get(TAG_IMAGE_LENGTH);
        ExifAttribute imageWidthAttribute =
                (ExifAttribute) mAttributes[imageType].get(TAG_IMAGE_WIDTH);

        if (imageLengthAttribute == null || imageWidthAttribute == null) {
            // Find if offset for JPEG data exists
            ExifAttribute jpegInterchangeFormatAttribute =
                    (ExifAttribute) mAttributes[imageType].get(TAG_JPEG_INTERCHANGE_FORMAT);
            if (jpegInterchangeFormatAttribute != null) {
                int jpegInterchangeFormat =
                        jpegInterchangeFormatAttribute.getIntValue(mExifByteOrder);

                // Searches for SOF marker in JPEG data and updates IMAGE_LENGTH & IMAGE_WIDTH tags
                getJpegAttributes(in, jpegInterchangeFormat, imageType);
            }
        }
    }

    // Sets thumbnail offset & length attributes based on JpegInterchangeFormat or StripOffsets tags
    private void setThumbnailData(ByteOrderedDataInputStream in) throws IOException {
        HashMap thumbnailData = mAttributes[IFD_TYPE_THUMBNAIL];

        ExifAttribute compressionAttribute =
                (ExifAttribute) thumbnailData.get(TAG_COMPRESSION);
        if (compressionAttribute != null) {
            mThumbnailCompression = compressionAttribute.getIntValue(mExifByteOrder);
            switch (mThumbnailCompression) {
                case DATA_JPEG: {
                    handleThumbnailFromJfif(in, thumbnailData);
                    break;
                }
                case DATA_UNCOMPRESSED:
                case DATA_JPEG_COMPRESSED: {
                    if (isSupportedDataType(thumbnailData)) {
                        handleThumbnailFromStrips(in, thumbnailData);
                    }
                    break;
                }
            }
        } else {
            // Thumbnail data may not contain Compression tag value
            mThumbnailCompression = DATA_JPEG;
            handleThumbnailFromJfif(in, thumbnailData);
        }
    }

    // Check JpegInterchangeFormat(JFIF) tags to retrieve thumbnail offset & length values
    // and reads the corresponding bytes if stream does not support seek function
    private void handleThumbnailFromJfif(ByteOrderedDataInputStream in, HashMap thumbnailData)
            throws IOException {
        ExifAttribute jpegInterchangeFormatAttribute =
                (ExifAttribute) thumbnailData.get(TAG_JPEG_INTERCHANGE_FORMAT);
        ExifAttribute jpegInterchangeFormatLengthAttribute =
                (ExifAttribute) thumbnailData.get(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
        if (jpegInterchangeFormatAttribute != null
                && jpegInterchangeFormatLengthAttribute != null) {
            int thumbnailOffset = jpegInterchangeFormatAttribute.getIntValue(mExifByteOrder);
            int thumbnailLength = jpegInterchangeFormatLengthAttribute.getIntValue(mExifByteOrder);

            // The following code limits the size of thumbnail size not to overflow EXIF data area.
            thumbnailLength = Math.min(thumbnailLength, in.available() - thumbnailOffset);
            if (mMimeType == IMAGE_TYPE_JPEG || mMimeType == IMAGE_TYPE_RAF
                    || mMimeType == IMAGE_TYPE_RW2) {
                thumbnailOffset += mExifOffset;
            } else if (mMimeType == IMAGE_TYPE_ORF) {
                // Update offset value since RAF files have IFD data preceding MakerNote data.
                thumbnailOffset += mOrfMakerNoteOffset;
            }
            if (DEBUG) {
                Log.d(TAG, "Setting thumbnail attributes with offset: " + thumbnailOffset
                        + ", length: " + thumbnailLength);
            }
            if (thumbnailOffset > 0 && thumbnailLength > 0) {
                mHasThumbnail = true;
                mThumbnailOffset = thumbnailOffset;
                mThumbnailLength = thumbnailLength;
                if (mFilename == null && mAssetInputStream == null) {
                    // Save the thumbnail in memory if the input doesn't support reading again.
                    byte[] thumbnailBytes = new byte[thumbnailLength];
                    in.seek(thumbnailOffset);
                    in.readFully(thumbnailBytes);
                    mThumbnailBytes = thumbnailBytes;
                }
            }
        }
    }

    // Check StripOffsets & StripByteCounts tags to retrieve thumbnail offset & length values
    private void handleThumbnailFromStrips(ByteOrderedDataInputStream in, HashMap thumbnailData)
            throws IOException {
        ExifAttribute stripOffsetsAttribute =
                (ExifAttribute) thumbnailData.get(TAG_STRIP_OFFSETS);
        ExifAttribute stripByteCountsAttribute =
                (ExifAttribute) thumbnailData.get(TAG_STRIP_BYTE_COUNTS);

        if (stripOffsetsAttribute != null && stripByteCountsAttribute != null) {
            long[] stripOffsets =
                    convertToLongArray(stripOffsetsAttribute.getValue(mExifByteOrder));
            long[] stripByteCounts =
                    convertToLongArray(stripByteCountsAttribute.getValue(mExifByteOrder));

            if (stripOffsets == null) {
                Log.w(TAG, "stripOffsets should not be null.");
                return;
            }
            if (stripByteCounts == null) {
                Log.w(TAG, "stripByteCounts should not be null.");
                return;
            }

            long totalStripByteCount = 0;
            for (long byteCount : stripByteCounts) {
                totalStripByteCount += byteCount;
            }

            // Set thumbnail byte array data for non-consecutive strip bytes
            byte[] totalStripBytes = new byte[(int) totalStripByteCount];

            int bytesRead = 0;
            int bytesAdded = 0;
            for (int i = 0; i < stripOffsets.length; i++) {
                int stripOffset = (int) stripOffsets[i];
                int stripByteCount = (int) stripByteCounts[i];

                // Skip to offset
                int skipBytes = stripOffset - bytesRead;
                if (skipBytes < 0) {
                    Log.d(TAG, "Invalid strip offset value");
                }
                in.seek(skipBytes);
                bytesRead += skipBytes;

                // Read strip bytes
                byte[] stripBytes = new byte[stripByteCount];
                in.read(stripBytes);
                bytesRead += stripByteCount;

                // Add bytes to array
                System.arraycopy(stripBytes, 0, totalStripBytes, bytesAdded,
                        stripBytes.length);
                bytesAdded += stripBytes.length;
            }

            mHasThumbnail = true;
            mThumbnailBytes = totalStripBytes;
            mThumbnailLength = totalStripBytes.length;
        }
    }

    // Check if thumbnail data type is currently supported or not
    private boolean isSupportedDataType(HashMap thumbnailData) throws IOException {
        ExifAttribute bitsPerSampleAttribute =
                (ExifAttribute) thumbnailData.get(TAG_BITS_PER_SAMPLE);
        if (bitsPerSampleAttribute != null) {
            int[] bitsPerSampleValue = (int[]) bitsPerSampleAttribute.getValue(mExifByteOrder);

            if (Arrays.equals(BITS_PER_SAMPLE_RGB, bitsPerSampleValue)) {
                return true;
            }

            // See DNG Specification 1.4.0.0. Section 3, Compression.
            if (mMimeType == IMAGE_TYPE_DNG) {
                ExifAttribute photometricInterpretationAttribute =
                        (ExifAttribute) thumbnailData.get(TAG_PHOTOMETRIC_INTERPRETATION);
                if (photometricInterpretationAttribute != null) {
                    int photometricInterpretationValue
                            = photometricInterpretationAttribute.getIntValue(mExifByteOrder);
                    if ((photometricInterpretationValue == PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO
                            && Arrays.equals(bitsPerSampleValue, BITS_PER_SAMPLE_GREYSCALE_2))
                            || ((photometricInterpretationValue == PHOTOMETRIC_INTERPRETATION_YCBCR)
                            && (Arrays.equals(bitsPerSampleValue, BITS_PER_SAMPLE_RGB)))) {
                        return true;
                    } else {
                        // TODO: Add support for lossless Huffman JPEG data
                    }
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Unsupported data type value");
        }
        return false;
    }

    // Returns true if the image length and width values are <= 512.
    // See Section 4.8 of http://standardsproposals.bsigroup.com/Home/getPDF/567
    private boolean isThumbnail(HashMap map) throws IOException {
        ExifAttribute imageLengthAttribute = (ExifAttribute) map.get(TAG_IMAGE_LENGTH);
        ExifAttribute imageWidthAttribute = (ExifAttribute) map.get(TAG_IMAGE_WIDTH);

        if (imageLengthAttribute != null && imageWidthAttribute != null) {
            int imageLengthValue = imageLengthAttribute.getIntValue(mExifByteOrder);
            int imageWidthValue = imageWidthAttribute.getIntValue(mExifByteOrder);
            if (imageLengthValue <= MAX_THUMBNAIL_SIZE && imageWidthValue <= MAX_THUMBNAIL_SIZE) {
                return true;
            }
        }
        return false;
    }

    // Validate primary, preview, thumbnail image data by comparing image size
    private void validateImages(InputStream in) throws IOException {
        // Swap images based on size (primary > preview > thumbnail)
        swapBasedOnImageSize(IFD_TYPE_PRIMARY, IFD_TYPE_PREVIEW);
        swapBasedOnImageSize(IFD_TYPE_PRIMARY, IFD_TYPE_THUMBNAIL);
        swapBasedOnImageSize(IFD_TYPE_PREVIEW, IFD_TYPE_THUMBNAIL);

        // Check if image has PixelXDimension/PixelYDimension tags, which contain valid image
        // sizes, excluding padding at the right end or bottom end of the image to make sure that
        // the values are multiples of 64. See JEITA CP-3451C Table 5 and Section 4.8.1. B.
        ExifAttribute pixelXDimAttribute =
                (ExifAttribute) mAttributes[IFD_TYPE_EXIF].get(TAG_PIXEL_X_DIMENSION);
        ExifAttribute pixelYDimAttribute =
                (ExifAttribute) mAttributes[IFD_TYPE_EXIF].get(TAG_PIXEL_Y_DIMENSION);
        if (pixelXDimAttribute != null && pixelYDimAttribute != null) {
            mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_WIDTH, pixelXDimAttribute);
            mAttributes[IFD_TYPE_PRIMARY].put(TAG_IMAGE_LENGTH, pixelYDimAttribute);
        }

        // Check whether thumbnail image exists and whether preview image satisfies the thumbnail
        // image requirements
        if (mAttributes[IFD_TYPE_THUMBNAIL].isEmpty()) {
            if (isThumbnail(mAttributes[IFD_TYPE_PREVIEW])) {
                mAttributes[IFD_TYPE_THUMBNAIL] = mAttributes[IFD_TYPE_PREVIEW];
                mAttributes[IFD_TYPE_PREVIEW] = new HashMap<>();
            }
        }

        // Check if the thumbnail image satisfies the thumbnail size requirements
        if (!isThumbnail(mAttributes[IFD_TYPE_THUMBNAIL])) {
            Log.d(TAG, "No image meets the size requirements of a thumbnail image.");
        }
    }

    /**
     * If image is uncompressed, ImageWidth/Length tags are used to store size info.
     * However, uncompressed images often store extra pixels around the edges of the final image,
     * which results in larger values for TAG_IMAGE_WIDTH and TAG_IMAGE_LENGTH tags.
     * This method corrects those tag values by checking first the values of TAG_DEFAULT_CROP_SIZE
     * See DNG Specification 1.4.0.0. Section 4. (DefaultCropSize)
     *
     * If image is a RW2 file, valid image sizes are stored in SensorBorder tags.
     * See tiff_parser.cc GetFullDimension32()
     * */
    private void updateImageSizeValues(ByteOrderedDataInputStream in, int imageType)
            throws IOException {
        // Uncompressed image valid image size values
        ExifAttribute defaultCropSizeAttribute =
                (ExifAttribute) mAttributes[imageType].get(TAG_DEFAULT_CROP_SIZE);
        // RW2 image valid image size values
        ExifAttribute topBorderAttribute =
                (ExifAttribute) mAttributes[imageType].get(TAG_RW2_SENSOR_TOP_BORDER);
        ExifAttribute leftBorderAttribute =
                (ExifAttribute) mAttributes[imageType].get(TAG_RW2_SENSOR_LEFT_BORDER);
        ExifAttribute bottomBorderAttribute =
                (ExifAttribute) mAttributes[imageType].get(TAG_RW2_SENSOR_BOTTOM_BORDER);
        ExifAttribute rightBorderAttribute =
                (ExifAttribute) mAttributes[imageType].get(TAG_RW2_SENSOR_RIGHT_BORDER);

        if (defaultCropSizeAttribute != null) {
            // Update for uncompressed image
            ExifAttribute defaultCropSizeXAttribute, defaultCropSizeYAttribute;
            if (defaultCropSizeAttribute.format == IFD_FORMAT_URATIONAL) {
                Rational[] defaultCropSizeValue =
                        (Rational[]) defaultCropSizeAttribute.getValue(mExifByteOrder);
                if (defaultCropSizeValue == null || defaultCropSizeValue.length != 2) {
                    Log.w(TAG, "Invalid crop size values. cropSize="
                            + Arrays.toString(defaultCropSizeValue));
                    return;
                }
                defaultCropSizeXAttribute =
                        ExifAttribute.createURational(defaultCropSizeValue[0], mExifByteOrder);
                defaultCropSizeYAttribute =
                        ExifAttribute.createURational(defaultCropSizeValue[1], mExifByteOrder);
            } else {
                int[] defaultCropSizeValue =
                        (int[]) defaultCropSizeAttribute.getValue(mExifByteOrder);
                if (defaultCropSizeValue == null || defaultCropSizeValue.length != 2) {
                    Log.w(TAG, "Invalid crop size values. cropSize="
                            + Arrays.toString(defaultCropSizeValue));
                    return;
                }
                defaultCropSizeXAttribute =
                        ExifAttribute.createUShort(defaultCropSizeValue[0], mExifByteOrder);
                defaultCropSizeYAttribute =
                        ExifAttribute.createUShort(defaultCropSizeValue[1], mExifByteOrder);
            }
            mAttributes[imageType].put(TAG_IMAGE_WIDTH, defaultCropSizeXAttribute);
            mAttributes[imageType].put(TAG_IMAGE_LENGTH, defaultCropSizeYAttribute);
        } else if (topBorderAttribute != null && leftBorderAttribute != null &&
                bottomBorderAttribute != null && rightBorderAttribute != null) {
            // Update for RW2 image
            int topBorderValue = topBorderAttribute.getIntValue(mExifByteOrder);
            int bottomBorderValue = bottomBorderAttribute.getIntValue(mExifByteOrder);
            int rightBorderValue = rightBorderAttribute.getIntValue(mExifByteOrder);
            int leftBorderValue = leftBorderAttribute.getIntValue(mExifByteOrder);
            if (bottomBorderValue > topBorderValue && rightBorderValue > leftBorderValue) {
                int length = bottomBorderValue - topBorderValue;
                int width = rightBorderValue - leftBorderValue;
                ExifAttribute imageLengthAttribute =
                        ExifAttribute.createUShort(length, mExifByteOrder);
                ExifAttribute imageWidthAttribute =
                        ExifAttribute.createUShort(width, mExifByteOrder);
                mAttributes[imageType].put(TAG_IMAGE_LENGTH, imageLengthAttribute);
                mAttributes[imageType].put(TAG_IMAGE_WIDTH, imageWidthAttribute);
            }
        } else {
            retrieveJpegImageSize(in, imageType);
        }
    }

    // Writes an Exif segment into the given output stream.
    private int writeExifSegment(ByteOrderedDataOutputStream dataOutputStream,
            int exifOffsetFromBeginning) throws IOException {
        // The following variables are for calculating each IFD tag group size in bytes.
        int[] ifdOffsets = new int[EXIF_TAGS.length];
        int[] ifdDataSizes = new int[EXIF_TAGS.length];

        // Remove IFD pointer tags (we'll re-add it later.)
        for (ExifTag tag : EXIF_POINTER_TAGS) {
            removeAttribute(tag.name);
        }
        // Remove old thumbnail data
        removeAttribute(JPEG_INTERCHANGE_FORMAT_TAG.name);
        removeAttribute(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name);

        // Remove null value tags.
        for (int ifdType = 0; ifdType < EXIF_TAGS.length; ++ifdType) {
            for (Object obj : mAttributes[ifdType].entrySet().toArray()) {
                final Map.Entry entry = (Map.Entry) obj;
                if (entry.getValue() == null) {
                    mAttributes[ifdType].remove(entry.getKey());
                }
            }
        }

        // Add IFD pointer tags. The next offset of primary image TIFF IFD will have thumbnail IFD
        // offset when there is one or more tags in the thumbnail IFD.
        if (!mAttributes[IFD_TYPE_EXIF].isEmpty()) {
            mAttributes[IFD_TYPE_PRIMARY].put(EXIF_POINTER_TAGS[1].name,
                    ExifAttribute.createULong(0, mExifByteOrder));
        }
        if (!mAttributes[IFD_TYPE_GPS].isEmpty()) {
            mAttributes[IFD_TYPE_PRIMARY].put(EXIF_POINTER_TAGS[2].name,
                    ExifAttribute.createULong(0, mExifByteOrder));
        }
        if (!mAttributes[IFD_TYPE_INTEROPERABILITY].isEmpty()) {
            mAttributes[IFD_TYPE_EXIF].put(EXIF_POINTER_TAGS[3].name,
                    ExifAttribute.createULong(0, mExifByteOrder));
        }
        if (mHasThumbnail) {
            mAttributes[IFD_TYPE_THUMBNAIL].put(JPEG_INTERCHANGE_FORMAT_TAG.name,
                    ExifAttribute.createULong(0, mExifByteOrder));
            mAttributes[IFD_TYPE_THUMBNAIL].put(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name,
                    ExifAttribute.createULong(mThumbnailLength, mExifByteOrder));
        }

        // Calculate IFD group data area sizes. IFD group data area is assigned to save the entry
        // value which has a bigger size than 4 bytes.
        for (int i = 0; i < EXIF_TAGS.length; ++i) {
            int sum = 0;
            for (Map.Entry<String, ExifAttribute> entry : mAttributes[i].entrySet()) {
                final ExifAttribute exifAttribute = entry.getValue();
                final int size = exifAttribute.size();
                if (size > 4) {
                    sum += size;
                }
            }
            ifdDataSizes[i] += sum;
        }

        // Calculate IFD offsets.
        int position = 8;
        for (int ifdType = 0; ifdType < EXIF_TAGS.length; ++ifdType) {
            if (!mAttributes[ifdType].isEmpty()) {
                ifdOffsets[ifdType] = position;
                position += 2 + mAttributes[ifdType].size() * 12 + 4 + ifdDataSizes[ifdType];
            }
        }
        if (mHasThumbnail) {
            int thumbnailOffset = position;
            mAttributes[IFD_TYPE_THUMBNAIL].put(JPEG_INTERCHANGE_FORMAT_TAG.name,
                    ExifAttribute.createULong(thumbnailOffset, mExifByteOrder));
            mThumbnailOffset = exifOffsetFromBeginning + thumbnailOffset;
            position += mThumbnailLength;
        }

        // Calculate the total size
        int totalSize = position + 8;  // eight bytes is for header part.
        if (DEBUG) {
            Log.d(TAG, "totalSize length: " + totalSize);
            for (int i = 0; i < EXIF_TAGS.length; ++i) {
                Log.d(TAG, String.format("index: %d, offsets: %d, tag count: %d, data sizes: %d",
                        i, ifdOffsets[i], mAttributes[i].size(), ifdDataSizes[i]));
            }
        }

        // Update IFD pointer tags with the calculated offsets.
        if (!mAttributes[IFD_TYPE_EXIF].isEmpty()) {
            mAttributes[IFD_TYPE_PRIMARY].put(EXIF_POINTER_TAGS[1].name,
                    ExifAttribute.createULong(ifdOffsets[IFD_TYPE_EXIF], mExifByteOrder));
        }
        if (!mAttributes[IFD_TYPE_GPS].isEmpty()) {
            mAttributes[IFD_TYPE_PRIMARY].put(EXIF_POINTER_TAGS[2].name,
                    ExifAttribute.createULong(ifdOffsets[IFD_TYPE_GPS], mExifByteOrder));
        }
        if (!mAttributes[IFD_TYPE_INTEROPERABILITY].isEmpty()) {
            mAttributes[IFD_TYPE_EXIF].put(EXIF_POINTER_TAGS[3].name, ExifAttribute.createULong(
                    ifdOffsets[IFD_TYPE_INTEROPERABILITY], mExifByteOrder));
        }

        // Write TIFF Headers. See JEITA CP-3451C Section 4.5.2. Table 1.
        dataOutputStream.writeUnsignedShort(totalSize);
        dataOutputStream.write(IDENTIFIER_EXIF_APP1);
        dataOutputStream.writeShort(mExifByteOrder == ByteOrder.BIG_ENDIAN
                ? BYTE_ALIGN_MM : BYTE_ALIGN_II);
        dataOutputStream.setByteOrder(mExifByteOrder);
        dataOutputStream.writeUnsignedShort(START_CODE);
        dataOutputStream.writeUnsignedInt(IFD_OFFSET);

        // Write IFD groups. See JEITA CP-3451C Section 4.5.8. Figure 9.
        for (int ifdType = 0; ifdType < EXIF_TAGS.length; ++ifdType) {
            if (!mAttributes[ifdType].isEmpty()) {
                // See JEITA CP-3451C Section 4.6.2: IFD structure.
                // Write entry count
                dataOutputStream.writeUnsignedShort(mAttributes[ifdType].size());

                // Write entry info
                int dataOffset = ifdOffsets[ifdType] + 2 + mAttributes[ifdType].size() * 12 + 4;
                for (Map.Entry<String, ExifAttribute> entry : mAttributes[ifdType].entrySet()) {
                    // Convert tag name to tag number.
                    final ExifTag tag = sExifTagMapsForWriting[ifdType].get(entry.getKey());
                    final int tagNumber = tag.number;
                    final ExifAttribute attribute = entry.getValue();
                    final int size = attribute.size();

                    dataOutputStream.writeUnsignedShort(tagNumber);
                    dataOutputStream.writeUnsignedShort(attribute.format);
                    dataOutputStream.writeInt(attribute.numberOfComponents);
                    if (size > 4) {
                        dataOutputStream.writeUnsignedInt(dataOffset);
                        dataOffset += size;
                    } else {
                        dataOutputStream.write(attribute.bytes);
                        // Fill zero up to 4 bytes
                        if (size < 4) {
                            for (int i = size; i < 4; ++i) {
                                dataOutputStream.writeByte(0);
                            }
                        }
                    }
                }

                // Write the next offset. It writes the offset of thumbnail IFD if there is one or
                // more tags in the thumbnail IFD when the current IFD is the primary image TIFF
                // IFD; Otherwise 0.
                if (ifdType == 0 && !mAttributes[IFD_TYPE_THUMBNAIL].isEmpty()) {
                    dataOutputStream.writeUnsignedInt(ifdOffsets[IFD_TYPE_THUMBNAIL]);
                } else {
                    dataOutputStream.writeUnsignedInt(0);
                }

                // Write values of data field exceeding 4 bytes after the next offset.
                for (Map.Entry<String, ExifAttribute> entry : mAttributes[ifdType].entrySet()) {
                    ExifAttribute attribute = entry.getValue();

                    if (attribute.bytes.length > 4) {
                        dataOutputStream.write(attribute.bytes, 0, attribute.bytes.length);
                    }
                }
            }
        }

        // Write thumbnail
        if (mHasThumbnail) {
            dataOutputStream.write(getThumbnailBytes());
        }

        // Reset the byte order to big endian in order to write remaining parts of the JPEG file.
        dataOutputStream.setByteOrder(ByteOrder.BIG_ENDIAN);

        return totalSize;
    }

    /**
     * Determines the data format of EXIF entry value.
     *
     * @param entryValue The value to be determined.
     * @return Returns two data formats gussed as a pair in integer. If there is no two candidate
               data formats for the given entry value, returns {@code -1} in the second of the pair.
     */
    private static Pair<Integer, Integer> guessDataFormat(String entryValue) {
        // See TIFF 6.0 Section 2, "Image File Directory".
        // Take the first component if there are more than one component.
        if (entryValue.contains(",")) {
            String[] entryValues = entryValue.split(",", -1);
            Pair<Integer, Integer> dataFormat = guessDataFormat(entryValues[0]);
            if (dataFormat.first == IFD_FORMAT_STRING) {
                return dataFormat;
            }
            for (int i = 1; i < entryValues.length; ++i) {
                final Pair<Integer, Integer> guessDataFormat = guessDataFormat(entryValues[i]);
                int first = -1, second = -1;
                if (guessDataFormat.first.equals(dataFormat.first)
                        || guessDataFormat.second.equals(dataFormat.first)) {
                    first = dataFormat.first;
                }
                if (dataFormat.second != -1 && (guessDataFormat.first.equals(dataFormat.second)
                        || guessDataFormat.second.equals(dataFormat.second))) {
                    second = dataFormat.second;
                }
                if (first == -1 && second == -1) {
                    return new Pair<>(IFD_FORMAT_STRING, -1);
                }
                if (first == -1) {
                    dataFormat = new Pair<>(second, -1);
                    continue;
                }
                if (second == -1) {
                    dataFormat = new Pair<>(first, -1);
                    continue;
                }
            }
            return dataFormat;
        }

        if (entryValue.contains("/")) {
            String[] rationalNumber = entryValue.split("/", -1);
            if (rationalNumber.length == 2) {
                try {
                    long numerator = (long) Double.parseDouble(rationalNumber[0]);
                    long denominator = (long) Double.parseDouble(rationalNumber[1]);
                    if (numerator < 0L || denominator < 0L) {
                        return new Pair<>(IFD_FORMAT_SRATIONAL, -1);
                    }
                    if (numerator > Integer.MAX_VALUE || denominator > Integer.MAX_VALUE) {
                        return new Pair<>(IFD_FORMAT_URATIONAL, -1);
                    }
                    return new Pair<>(IFD_FORMAT_SRATIONAL, IFD_FORMAT_URATIONAL);
                } catch (NumberFormatException e)  {
                    // Ignored
                }
            }
            return new Pair<>(IFD_FORMAT_STRING, -1);
        }
        try {
            Long longValue = Long.parseLong(entryValue);
            if (longValue >= 0 && longValue <= 65535) {
                return new Pair<>(IFD_FORMAT_USHORT, IFD_FORMAT_ULONG);
            }
            if (longValue < 0) {
                return new Pair<>(IFD_FORMAT_SLONG, -1);
            }
            return new Pair<>(IFD_FORMAT_ULONG, -1);
        } catch (NumberFormatException e) {
            // Ignored
        }
        try {
            Double.parseDouble(entryValue);
            return new Pair<>(IFD_FORMAT_DOUBLE, -1);
        } catch (NumberFormatException e) {
            // Ignored
        }
        return new Pair<>(IFD_FORMAT_STRING, -1);
    }

    // An input stream to parse EXIF data area, which can be written in either little or big endian
    // order.
    private static class ByteOrderedDataInputStream extends InputStream implements DataInput {
        private static final ByteOrder LITTLE_ENDIAN = ByteOrder.LITTLE_ENDIAN;
        private static final ByteOrder BIG_ENDIAN = ByteOrder.BIG_ENDIAN;

        private DataInputStream mDataInputStream;
        private ByteOrder mByteOrder = ByteOrder.BIG_ENDIAN;
        private final int mLength;
        private int mPosition;

        public ByteOrderedDataInputStream(InputStream in) throws IOException {
            mDataInputStream = new DataInputStream(in);
            mLength = mDataInputStream.available();
            mPosition = 0;
            mDataInputStream.mark(mLength);
        }

        public ByteOrderedDataInputStream(byte[] bytes) throws IOException {
            this(new ByteArrayInputStream(bytes));
        }

        public void setByteOrder(ByteOrder byteOrder) {
            mByteOrder = byteOrder;
        }

        public void seek(long byteCount) throws IOException {
            if (mPosition > byteCount) {
                mPosition = 0;
                mDataInputStream.reset();
                mDataInputStream.mark(mLength);
            } else {
                byteCount -= mPosition;
            }

            if (skipBytes((int) byteCount) != (int) byteCount) {
                throw new IOException("Couldn't seek up to the byteCount");
            }
        }

        public int peek() {
            return mPosition;
        }

        @Override
        public int available() throws IOException {
            return mDataInputStream.available();
        }

        @Override
        public int read() throws IOException {
            ++mPosition;
            return mDataInputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = mDataInputStream.read(b, off, len);
            mPosition += bytesRead;
            return bytesRead;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            ++mPosition;
            return mDataInputStream.readUnsignedByte();
        }

        @Override
        public String readLine() throws IOException {
            Log.d(TAG, "Currently unsupported");
            return null;
        }

        @Override
        public boolean readBoolean() throws IOException {
            ++mPosition;
            return mDataInputStream.readBoolean();
        }

        @Override
        public char readChar() throws IOException {
            mPosition += 2;
            return mDataInputStream.readChar();
        }

        @Override
        public String readUTF() throws IOException {
            mPosition += 2;
            return mDataInputStream.readUTF();
        }

        @Override
        public void readFully(byte[] buffer, int offset, int length) throws IOException {
            mPosition += length;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            if (mDataInputStream.read(buffer, offset, length) != length) {
                throw new IOException("Couldn't read up to the length of buffer");
            }
        }

        @Override
        public void readFully(byte[] buffer) throws IOException {
            mPosition += buffer.length;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            if (mDataInputStream.read(buffer, 0, buffer.length) != buffer.length) {
                throw new IOException("Couldn't read up to the length of buffer");
            }
        }

        @Override
        public byte readByte() throws IOException {
            ++mPosition;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch = mDataInputStream.read();
            if (ch < 0) {
                throw new EOFException();
            }
            return (byte) ch;
        }

        @Override
        public short readShort() throws IOException {
            mPosition += 2;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = mDataInputStream.read();
            int ch2 = mDataInputStream.read();
            if ((ch1 | ch2) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return (short) ((ch2 << 8) + (ch1));
            } else if (mByteOrder == BIG_ENDIAN) {
                return (short) ((ch1 << 8) + (ch2));
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }

        @Override
        public int readInt() throws IOException {
            mPosition += 4;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = mDataInputStream.read();
            int ch2 = mDataInputStream.read();
            int ch3 = mDataInputStream.read();
            int ch4 = mDataInputStream.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
            } else if (mByteOrder == BIG_ENDIAN) {
                return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }

        @Override
        public int skipBytes(int byteCount) throws IOException {
            int totalSkip = Math.min(byteCount, mLength - mPosition);
            int skipped = 0;
            while (skipped < totalSkip) {
                skipped += mDataInputStream.skipBytes(totalSkip - skipped);
            }
            mPosition += skipped;
            return skipped;
        }

        @Override
        public int readUnsignedShort() throws IOException {
            mPosition += 2;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = mDataInputStream.read();
            int ch2 = mDataInputStream.read();
            if ((ch1 | ch2) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return ((ch2 << 8) + (ch1));
            } else if (mByteOrder == BIG_ENDIAN) {
                return ((ch1 << 8) + (ch2));
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }

        public long readUnsignedInt() throws IOException {
            return readInt() & 0xffffffffL;
        }

        @Override
        public long readLong() throws IOException {
            mPosition += 8;
            if (mPosition > mLength) {
                throw new EOFException();
            }
            int ch1 = mDataInputStream.read();
            int ch2 = mDataInputStream.read();
            int ch3 = mDataInputStream.read();
            int ch4 = mDataInputStream.read();
            int ch5 = mDataInputStream.read();
            int ch6 = mDataInputStream.read();
            int ch7 = mDataInputStream.read();
            int ch8 = mDataInputStream.read();
            if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
                throw new EOFException();
            }
            if (mByteOrder == LITTLE_ENDIAN) {
                return (((long) ch8 << 56) + ((long) ch7 << 48) + ((long) ch6 << 40)
                        + ((long) ch5 << 32) + ((long) ch4 << 24) + ((long) ch3 << 16)
                        + ((long) ch2 << 8) + (long) ch1);
            } else if (mByteOrder == BIG_ENDIAN) {
                return (((long) ch1 << 56) + ((long) ch2 << 48) + ((long) ch3 << 40)
                        + ((long) ch4 << 32) + ((long) ch5 << 24) + ((long) ch6 << 16)
                        + ((long) ch7 << 8) + (long) ch8);
            }
            throw new IOException("Invalid byte order: " + mByteOrder);
        }

        @Override
        public float readFloat() throws IOException {
            return Float.intBitsToFloat(readInt());
        }

        @Override
        public double readDouble() throws IOException {
            return Double.longBitsToDouble(readLong());
        }
    }

    // An output stream to write EXIF data area, which can be written in either little or big endian
    // order.
    private static class ByteOrderedDataOutputStream extends FilterOutputStream {
        private final OutputStream mOutputStream;
        private ByteOrder mByteOrder;

        public ByteOrderedDataOutputStream(OutputStream out, ByteOrder byteOrder) {
            super(out);
            mOutputStream = out;
            mByteOrder = byteOrder;
        }

        public void setByteOrder(ByteOrder byteOrder) {
            mByteOrder = byteOrder;
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            mOutputStream.write(bytes);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            mOutputStream.write(bytes, offset, length);
        }

        public void writeByte(int val) throws IOException {
            mOutputStream.write(val);
        }

        public void writeShort(short val) throws IOException {
            if (mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                mOutputStream.write((val >>> 0) & 0xFF);
                mOutputStream.write((val >>> 8) & 0xFF);
            } else if (mByteOrder == ByteOrder.BIG_ENDIAN) {
                mOutputStream.write((val >>> 8) & 0xFF);
                mOutputStream.write((val >>> 0) & 0xFF);
            }
        }

        public void writeInt(int val) throws IOException {
            if (mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                mOutputStream.write((val >>> 0) & 0xFF);
                mOutputStream.write((val >>> 8) & 0xFF);
                mOutputStream.write((val >>> 16) & 0xFF);
                mOutputStream.write((val >>> 24) & 0xFF);
            } else if (mByteOrder == ByteOrder.BIG_ENDIAN) {
                mOutputStream.write((val >>> 24) & 0xFF);
                mOutputStream.write((val >>> 16) & 0xFF);
                mOutputStream.write((val >>> 8) & 0xFF);
                mOutputStream.write((val >>> 0) & 0xFF);
            }
        }

        public void writeUnsignedShort(int val) throws IOException {
            writeShort((short) val);
        }

        public void writeUnsignedInt(long val) throws IOException {
            writeInt((int) val);
        }
    }

    // Swaps image data based on image size
    private void swapBasedOnImageSize(@IfdType int firstIfdType, @IfdType int secondIfdType)
            throws IOException {
        if (mAttributes[firstIfdType].isEmpty() || mAttributes[secondIfdType].isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "Cannot perform swap since only one image data exists");
            }
            return;
        }

        ExifAttribute firstImageLengthAttribute =
                (ExifAttribute) mAttributes[firstIfdType].get(TAG_IMAGE_LENGTH);
        ExifAttribute firstImageWidthAttribute =
                (ExifAttribute) mAttributes[firstIfdType].get(TAG_IMAGE_WIDTH);
        ExifAttribute secondImageLengthAttribute =
                (ExifAttribute) mAttributes[secondIfdType].get(TAG_IMAGE_LENGTH);
        ExifAttribute secondImageWidthAttribute =
                (ExifAttribute) mAttributes[secondIfdType].get(TAG_IMAGE_WIDTH);

        if (firstImageLengthAttribute == null || firstImageWidthAttribute == null) {
            if (DEBUG) {
                Log.d(TAG, "First image does not contain valid size information");
            }
        } else if (secondImageLengthAttribute == null || secondImageWidthAttribute == null) {
            if (DEBUG) {
                Log.d(TAG, "Second image does not contain valid size information");
            }
        } else {
            int firstImageLengthValue = firstImageLengthAttribute.getIntValue(mExifByteOrder);
            int firstImageWidthValue = firstImageWidthAttribute.getIntValue(mExifByteOrder);
            int secondImageLengthValue = secondImageLengthAttribute.getIntValue(mExifByteOrder);
            int secondImageWidthValue = secondImageWidthAttribute.getIntValue(mExifByteOrder);

            if (firstImageLengthValue < secondImageLengthValue &&
                    firstImageWidthValue < secondImageWidthValue) {
                HashMap<String, ExifAttribute> tempMap = mAttributes[firstIfdType];
                mAttributes[firstIfdType] = mAttributes[secondIfdType];
                mAttributes[secondIfdType] = tempMap;
            }
        }
    }

    /**
     * Closes 'closeable', ignoring any checked exceptions. Does nothing if 'closeable' is null.
     */
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Copies all of the bytes from {@code in} to {@code out}. Neither stream is closed.
     * Returns the total number of bytes transferred.
     */
    private static int copy(InputStream in, OutputStream out) throws IOException {
        int total = 0;
        byte[] buffer = new byte[8192];
        int c;
        while ((c = in.read(buffer)) != -1) {
            total += c;
            out.write(buffer, 0, c);
        }
        return total;
    }

    /**
     * Convert given int[] to long[]. If long[] is given, just return it.
     * Return null for other types of input.
     */
    private static long[] convertToLongArray(Object inputObj) {
        if (inputObj instanceof int[]) {
            int[] input = (int[]) inputObj;
            long[] result = new long[input.length];
            for (int i = 0; i < input.length; i++) {
                result[i] = input[i];
            }
            return result;
        } else if (inputObj instanceof long[]) {
            return (long[]) inputObj;
        }
        return null;
    }
}
