/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_BYTE;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_DOUBLE;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_SLONG;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_SRATIONAL;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_STRING;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_ULONG;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_UNDEFINED;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_URATIONAL;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_USHORT;
import static androidx.exifinterface.media.ExifInterface.CONTRAST_NORMAL;
import static androidx.exifinterface.media.ExifInterface.EXPOSURE_PROGRAM_NOT_DEFINED;
import static androidx.exifinterface.media.ExifInterface.FILE_SOURCE_DSC;
import static androidx.exifinterface.media.ExifInterface.FLAG_FLASH_FIRED;
import static androidx.exifinterface.media.ExifInterface.FLAG_FLASH_NO_FLASH_FUNCTION;
import static androidx.exifinterface.media.ExifInterface.GPS_DIRECTION_TRUE;
import static androidx.exifinterface.media.ExifInterface.GPS_DISTANCE_KILOMETERS;
import static androidx.exifinterface.media.ExifInterface.GPS_SPEED_KILOMETERS_PER_HOUR;
import static androidx.exifinterface.media.ExifInterface.LIGHT_SOURCE_FLASH;
import static androidx.exifinterface.media.ExifInterface.LIGHT_SOURCE_UNKNOWN;
import static androidx.exifinterface.media.ExifInterface.METERING_MODE_UNKNOWN;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL;
import static androidx.exifinterface.media.ExifInterface.RENDERED_PROCESS_NORMAL;
import static androidx.exifinterface.media.ExifInterface.RESOLUTION_UNIT_INCHES;
import static androidx.exifinterface.media.ExifInterface.SATURATION_NORMAL;
import static androidx.exifinterface.media.ExifInterface.SCENE_CAPTURE_TYPE_STANDARD;
import static androidx.exifinterface.media.ExifInterface.SCENE_TYPE_DIRECTLY_PHOTOGRAPHED;
import static androidx.exifinterface.media.ExifInterface.SENSITIVITY_TYPE_ISO_SPEED;
import static androidx.exifinterface.media.ExifInterface.SHARPNESS_NORMAL;
import static androidx.exifinterface.media.ExifInterface.TAG_APERTURE_VALUE;
import static androidx.exifinterface.media.ExifInterface.TAG_BRIGHTNESS_VALUE;
import static androidx.exifinterface.media.ExifInterface.TAG_COLOR_SPACE;
import static androidx.exifinterface.media.ExifInterface.TAG_COMPONENTS_CONFIGURATION;
import static androidx.exifinterface.media.ExifInterface.TAG_CONTRAST;
import static androidx.exifinterface.media.ExifInterface.TAG_CUSTOM_RENDERED;
import static androidx.exifinterface.media.ExifInterface.TAG_DATETIME;
import static androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED;
import static androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL;
import static androidx.exifinterface.media.ExifInterface.TAG_EXIF_VERSION;
import static androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_BIAS_VALUE;
import static androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_MODE;
import static androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_PROGRAM;
import static androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME;
import static androidx.exifinterface.media.ExifInterface.TAG_FILE_SOURCE;
import static androidx.exifinterface.media.ExifInterface.TAG_FLASH;
import static androidx.exifinterface.media.ExifInterface.TAG_FLASHPIX_VERSION;
import static androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH;
import static androidx.exifinterface.media.ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT;
import static androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_BEARING_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_DEST_DISTANCE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_IMG_DIRECTION_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_SPEED_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_TRACK_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_VERSION_ID;
import static androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH;
import static androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH;
import static androidx.exifinterface.media.ExifInterface.TAG_INTEROPERABILITY_INDEX;
import static androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS;
import static androidx.exifinterface.media.ExifInterface.TAG_LIGHT_SOURCE;
import static androidx.exifinterface.media.ExifInterface.TAG_MAKE;
import static androidx.exifinterface.media.ExifInterface.TAG_MAX_APERTURE_VALUE;
import static androidx.exifinterface.media.ExifInterface.TAG_METERING_MODE;
import static androidx.exifinterface.media.ExifInterface.TAG_MODEL;
import static androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION;
import static androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY;
import static androidx.exifinterface.media.ExifInterface.TAG_PIXEL_X_DIMENSION;
import static androidx.exifinterface.media.ExifInterface.TAG_PIXEL_Y_DIMENSION;
import static androidx.exifinterface.media.ExifInterface.TAG_RESOLUTION_UNIT;
import static androidx.exifinterface.media.ExifInterface.TAG_SATURATION;
import static androidx.exifinterface.media.ExifInterface.TAG_SCENE_CAPTURE_TYPE;
import static androidx.exifinterface.media.ExifInterface.TAG_SCENE_TYPE;
import static androidx.exifinterface.media.ExifInterface.TAG_SENSING_METHOD;
import static androidx.exifinterface.media.ExifInterface.TAG_SENSITIVITY_TYPE;
import static androidx.exifinterface.media.ExifInterface.TAG_SHARPNESS;
import static androidx.exifinterface.media.ExifInterface.TAG_SHUTTER_SPEED_VALUE;
import static androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE;
import static androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME;
import static androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_DIGITIZED;
import static androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL;
import static androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE;
import static androidx.exifinterface.media.ExifInterface.TAG_X_RESOLUTION;
import static androidx.exifinterface.media.ExifInterface.TAG_Y_CB_CR_POSITIONING;
import static androidx.exifinterface.media.ExifInterface.TAG_Y_RESOLUTION;
import static androidx.exifinterface.media.ExifInterface.WHITE_BALANCE_AUTO;
import static androidx.exifinterface.media.ExifInterface.WHITE_BALANCE_MANUAL;
import static androidx.exifinterface.media.ExifInterface.Y_CB_CR_POSITIONING_CENTERED;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureMetaData;
import androidx.core.util.Preconditions;
import androidx.exifinterface.media.ExifInterface;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class stores the EXIF header in IFDs according to the JPEG specification.
 */
// Note: This class is adapted from {@link androidx.exifinterface.media.ExifInterface}, and is
// currently expected to be used for writing a subset of Exif values. Support for other mime
// types besides JPEG have been removed. Support for thumbnails/strips has been removed along
// with many exif tags. If more tags are required, the source code for ExifInterface should be
// referenced and can be adapted to this class.
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExifData {
    private static final String TAG = "ExifData";
    private static final boolean DEBUG = false;

    /**
     * Enum representing the white balance mode.
     */
    public enum WhiteBalanceMode {
        /** AWB is turned on. */
        AUTO,
        /** AWB is turned off. */
        MANUAL
    }

    // Names for the data formats for debugging purpose.
    static final String[] IFD_FORMAT_NAMES = new String[]{
            "", "BYTE", "STRING", "USHORT", "ULONG", "URATIONAL", "SBYTE", "UNDEFINED", "SSHORT",
            "SLONG", "SRATIONAL", "SINGLE", "DOUBLE", "IFD"
    };

    /**
     * Private tags used for pointing the other IFD offsets.
     * The types of the following tags are int.
     * See JEITA CP-3451C Section 4.6.3: Exif-specific IFD.
     * For SubIFD, see Note 1 of Adobe PageMaker® 6.0 TIFF Technical Notes.
     */
    static final String TAG_EXIF_IFD_POINTER = "ExifIFDPointer";
    static final String TAG_GPS_INFO_IFD_POINTER = "GPSInfoIFDPointer";
    static final String TAG_INTEROPERABILITY_IFD_POINTER = "InteroperabilityIFDPointer";
    static final String TAG_SUB_IFD_POINTER = "SubIFDPointer";

    // Primary image IFD TIFF tags (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    // This is only a subset of the tags defined in ExifInterface
    private static final ExifTag[] IFD_TIFF_TAGS = new ExifTag[]{
            // For below two, see TIFF 6.0 Spec Section 3: Bilevel Images.
            new ExifTag(TAG_IMAGE_WIDTH, 256, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_IMAGE_LENGTH, 257, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_MAKE, 271, IFD_FORMAT_STRING),
            new ExifTag(TAG_MODEL, 272, IFD_FORMAT_STRING),
            new ExifTag(TAG_ORIENTATION, 274, IFD_FORMAT_USHORT),
            new ExifTag(TAG_X_RESOLUTION, 282, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_Y_RESOLUTION, 283, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_RESOLUTION_UNIT, 296, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SOFTWARE, 305, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME, 306, IFD_FORMAT_STRING),
            new ExifTag(TAG_Y_CB_CR_POSITIONING, 531, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SUB_IFD_POINTER, 330, IFD_FORMAT_ULONG),
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
    };

    // Primary image IFD Exif Private tags (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    // This is only a subset of the tags defined in ExifInterface
    private static final ExifTag[] IFD_EXIF_TAGS = new ExifTag[]{
            new ExifTag(TAG_EXPOSURE_TIME, 33434, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_F_NUMBER, 33437, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_EXPOSURE_PROGRAM, 34850, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PHOTOGRAPHIC_SENSITIVITY, 34855, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SENSITIVITY_TYPE, 34864, IFD_FORMAT_USHORT),
            new ExifTag(TAG_EXIF_VERSION, 36864, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME_ORIGINAL, 36867, IFD_FORMAT_STRING),
            new ExifTag(TAG_DATETIME_DIGITIZED, 36868, IFD_FORMAT_STRING),
            new ExifTag(TAG_COMPONENTS_CONFIGURATION, 37121, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_SHUTTER_SPEED_VALUE, 37377, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_APERTURE_VALUE, 37378, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_BRIGHTNESS_VALUE, 37379, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_EXPOSURE_BIAS_VALUE, 37380, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_MAX_APERTURE_VALUE, 37381, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_METERING_MODE, 37383, IFD_FORMAT_USHORT),
            new ExifTag(TAG_LIGHT_SOURCE, 37384, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FLASH, 37385, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FOCAL_LENGTH, 37386, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_SUBSEC_TIME, 37520, IFD_FORMAT_STRING),
            new ExifTag(TAG_SUBSEC_TIME_ORIGINAL, 37521, IFD_FORMAT_STRING),
            new ExifTag(TAG_SUBSEC_TIME_DIGITIZED, 37522, IFD_FORMAT_STRING),
            new ExifTag(TAG_FLASHPIX_VERSION, 40960, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_COLOR_SPACE, 40961, IFD_FORMAT_USHORT),
            new ExifTag(TAG_PIXEL_X_DIMENSION, 40962, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_PIXEL_Y_DIMENSION, 40963, IFD_FORMAT_USHORT, IFD_FORMAT_ULONG),
            new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, IFD_FORMAT_ULONG),
            new ExifTag(TAG_FOCAL_PLANE_RESOLUTION_UNIT, 41488, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SENSING_METHOD, 41495, IFD_FORMAT_USHORT),
            new ExifTag(TAG_FILE_SOURCE, 41728, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_SCENE_TYPE, 41729, IFD_FORMAT_UNDEFINED),
            new ExifTag(TAG_CUSTOM_RENDERED, 41985, IFD_FORMAT_USHORT),
            new ExifTag(TAG_EXPOSURE_MODE, 41986, IFD_FORMAT_USHORT),
            new ExifTag(TAG_WHITE_BALANCE, 41987, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SCENE_CAPTURE_TYPE, 41990, IFD_FORMAT_USHORT),
            new ExifTag(TAG_CONTRAST, 41992, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SATURATION, 41993, IFD_FORMAT_USHORT),
            new ExifTag(TAG_SHARPNESS, 41994, IFD_FORMAT_USHORT)
    };

    // Primary image IFD GPS Info tags (See JEITA CP-3451C Section 4.6.6 Tag Support Levels)
    // This is only a subset of the tags defined in ExifInterface
    private static final ExifTag[] IFD_GPS_TAGS = new ExifTag[]{
            new ExifTag(TAG_GPS_VERSION_ID, 0, IFD_FORMAT_BYTE),
            new ExifTag(TAG_GPS_LATITUDE_REF, 1, IFD_FORMAT_STRING),
            // Allow SRATIONAL to be compatible with apps using wrong format and
            // even if it is negative, it may be valid latitude / longitude.
            new ExifTag(TAG_GPS_LATITUDE, 2, IFD_FORMAT_URATIONAL, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_GPS_LONGITUDE_REF, 3, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_LONGITUDE, 4, IFD_FORMAT_URATIONAL, IFD_FORMAT_SRATIONAL),
            new ExifTag(TAG_GPS_ALTITUDE_REF, 5, IFD_FORMAT_BYTE),
            new ExifTag(TAG_GPS_ALTITUDE, 6, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_TIMESTAMP, 7, IFD_FORMAT_URATIONAL),
            new ExifTag(TAG_GPS_SPEED_REF, 12, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_TRACK_REF, 14, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_IMG_DIRECTION_REF, 16, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_BEARING_REF, 23, IFD_FORMAT_STRING),
            new ExifTag(TAG_GPS_DEST_DISTANCE_REF, 25, IFD_FORMAT_STRING)
    };

    // List of tags for pointing to the other image file directory offset.
    static final ExifTag[] EXIF_POINTER_TAGS = new ExifTag[]{
            new ExifTag(TAG_SUB_IFD_POINTER, 330, IFD_FORMAT_ULONG),
            new ExifTag(TAG_EXIF_IFD_POINTER, 34665, IFD_FORMAT_ULONG),
            new ExifTag(TAG_GPS_INFO_IFD_POINTER, 34853, IFD_FORMAT_ULONG),
            new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, IFD_FORMAT_ULONG),
    };

    // Primary image IFD Interoperability tag (See JEITA CP-3451C Section 4.6.8 Tag Support Levels)
    private static final ExifTag[] IFD_INTEROPERABILITY_TAGS = new ExifTag[]{
            new ExifTag(TAG_INTEROPERABILITY_INDEX, 1, IFD_FORMAT_STRING)
    };

    // List of Exif tag groups
    static final ExifTag[][] EXIF_TAGS = new ExifTag[][]{
            IFD_TIFF_TAGS, IFD_EXIF_TAGS, IFD_GPS_TAGS, IFD_INTEROPERABILITY_TAGS
    };

    // Indices for the above tags. Note these must stay in sync with the order of EXIF_TAGS.
    static final int IFD_TYPE_PRIMARY = 0;
    static final int IFD_TYPE_EXIF = 1;
    static final int IFD_TYPE_GPS = 2;
    static final int IFD_TYPE_INTEROPERABILITY = 3;

    // NOTE: This is a subset of the tags from ExifInterface. Only supports tags in this class.
    static final HashSet<String> sTagSetForCompatibility = new HashSet<>(Arrays.asList(
            TAG_F_NUMBER, TAG_EXPOSURE_TIME, TAG_GPS_TIMESTAMP));

    private static final int MM_IN_MICRONS = 1000;

    private final List<Map<String, ExifAttribute>> mAttributes;
    private final ByteOrder mByteOrder;

    ExifData(ByteOrder order, List<Map<String, ExifAttribute>> attributes) {
        Preconditions.checkState(attributes.size() == EXIF_TAGS.length, "Malformed attributes "
                + "list. Number of IFDs mismatch.");
        mByteOrder = order;
        mAttributes = attributes;
    }

    /**
     * Gets the byte order.
     */
    @NonNull
    public ByteOrder getByteOrder() {
        return mByteOrder;
    }

    @NonNull
    Map<String, ExifAttribute> getAttributes(int ifdIndex) {
        Preconditions.checkArgumentInRange(ifdIndex, 0, EXIF_TAGS.length,
                "Invalid IFD index: " + ifdIndex + ". Index should be between [0, EXIF_TAGS"
                        + ".length] ");
        return mAttributes.get(ifdIndex);
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
                return attribute.getStringValue(mByteOrder);
            }
            if (tag.equals(TAG_GPS_TIMESTAMP)) {
                // Convert the rational values to the custom formats for backwards compatibility.
                if (attribute.format != IFD_FORMAT_URATIONAL
                        && attribute.format != IFD_FORMAT_SRATIONAL) {
                    Logger.w(TAG,
                            "GPS Timestamp format is not rational. format=" + attribute.format);
                    return null;
                }
                LongRational[] array =
                        (LongRational[]) attribute.getValue(mByteOrder);
                if (array == null || array.length != 3) {
                    Logger.w(TAG, "Invalid GPS Timestamp array. array=" + Arrays.toString(array));
                    return null;
                }
                return String.format(Locale.US, "%02d:%02d:%02d",
                        (int) ((float) array[0].getNumerator() / array[0].getDenominator()),
                        (int) ((float) array[1].getNumerator() / array[1].getDenominator()),
                        (int) ((float) array[2].getNumerator() / array[2].getDenominator()));
            }
            try {
                return Double.toString(attribute.getDoubleValue(mByteOrder));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the EXIF attribute of the specified tag or {@code null} if there is no such tag.
     *
     * @param tag the name of the tag.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    private ExifAttribute getExifAttribute(@NonNull String tag) {
        // Maintain compatibility.
        if (TAG_ISO_SPEED_RATINGS.equals(tag)) {
            if (DEBUG) {
                Logger.d(TAG, "getExifAttribute: Replacing TAG_ISO_SPEED_RATINGS with "
                        + "TAG_PHOTOGRAPHIC_SENSITIVITY.");
            }
            tag = TAG_PHOTOGRAPHIC_SENSITIVITY;
        }
        // Retrieves all tag groups. The value from primary image tag group has a higher priority
        // than the value from the thumbnail tag group if there are more than one candidates.
        for (int i = 0; i < EXIF_TAGS.length; ++i) {
            ExifAttribute value = mAttributes.get(i).get(tag);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Generates an empty builder suitable for generating ExifData for JPEG from the current device.
     */
    @NonNull
    public static Builder builderForDevice() {
        // Add PRIMARY defaults. EXIF and GPS defaults will be added in build()
        return new Builder(ByteOrder.BIG_ENDIAN)
                .setAttribute(TAG_ORIENTATION, String.valueOf(ORIENTATION_NORMAL))
                .setAttribute(TAG_X_RESOLUTION, "72/1")
                .setAttribute(TAG_Y_RESOLUTION, "72/1")
                .setAttribute(TAG_RESOLUTION_UNIT, String.valueOf(RESOLUTION_UNIT_INCHES))
                .setAttribute(TAG_Y_CB_CR_POSITIONING,
                        String.valueOf(Y_CB_CR_POSITIONING_CENTERED))
                // Defaults derived from device
                .setAttribute(TAG_MAKE, Build.MANUFACTURER)
                .setAttribute(TAG_MODEL, Build.MODEL);
    }

    /**
     * Builder for the {@link ExifData} class.
     */
    public static final class Builder {
        // Pattern to check gps timestamp
        private static final Pattern GPS_TIMESTAMP_PATTERN =
                Pattern.compile("^(\\d{2}):(\\d{2}):(\\d{2})$");
        // Pattern to check date time primary format (e.g. 2020:01:01 00:00:00)
        private static final Pattern DATETIME_PRIMARY_FORMAT_PATTERN =
                Pattern.compile("^(\\d{4}):(\\d{2}):(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})$");
        // Pattern to check date time secondary format (e.g. 2020-01-01 00:00:00)
        private static final Pattern DATETIME_SECONDARY_FORMAT_PATTERN =
                Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})$");
        private static final int DATETIME_VALUE_STRING_LENGTH = 19;

        // Mappings from tag name to tag number and each item represents one IFD tag group.
        static final List<HashMap<String, ExifTag>> sExifTagMapsForWriting =
                Collections.list(new Enumeration<HashMap<String, ExifTag>>() {
                    int mIfdIndex = 0;

                    @Override
                    public boolean hasMoreElements() {
                        return mIfdIndex < EXIF_TAGS.length;
                    }

                    @Override
                    public HashMap<String, ExifTag> nextElement() {
                        // Build up the hash tables to look up Exif tags for writing Exif tags.
                        HashMap<String, ExifTag> map = new HashMap<>();
                        for (ExifTag tag : EXIF_TAGS[mIfdIndex]) {
                            map.put(tag.name, tag);
                        }
                        mIfdIndex++;
                        return map;
                    }
                });

        final List<Map<String, ExifAttribute>> mAttributes = Collections.list(
                new Enumeration<Map<String, ExifAttribute>>() {
                    int mIfdIndex = 0;

                    @Override
                    public boolean hasMoreElements() {
                        return mIfdIndex < EXIF_TAGS.length;
                    }

                    @Override
                    public Map<String, ExifAttribute> nextElement() {
                        mIfdIndex++;
                        return new HashMap<>();
                    }
                });
        private final ByteOrder mByteOrder;

        Builder(@NonNull ByteOrder byteOrder) {
            mByteOrder = byteOrder;
        }

        /**
         * Sets the width of the image.
         *
         * @param width the width of the image.
         */
        @NonNull
        public Builder setImageWidth(int width) {
            return setAttribute(TAG_IMAGE_WIDTH, String.valueOf(width));
        }

        /**
         * Sets the height of the image.
         *
         * @param height the height of the image.
         */
        @NonNull
        public Builder setImageHeight(int height) {
            return setAttribute(TAG_IMAGE_LENGTH, String.valueOf(height));
        }

        /**
         * Sets the orientation of the image in degrees.
         *
         * @param orientationDegrees the orientation in degrees. Can be one of (0, 90, 180, 270)
         */
        @NonNull
        public Builder setOrientationDegrees(int orientationDegrees) {
            int orientationEnum;
            switch (orientationDegrees) {
                case 0:
                    orientationEnum = ExifInterface.ORIENTATION_NORMAL;
                    break;
                case 90:
                    orientationEnum = ExifInterface.ORIENTATION_ROTATE_90;
                    break;
                case 180:
                    orientationEnum = ExifInterface.ORIENTATION_ROTATE_180;
                    break;
                case 270:
                    orientationEnum = ExifInterface.ORIENTATION_ROTATE_270;
                    break;
                default:
                    Logger.w(TAG,
                            "Unexpected orientation value: " + orientationDegrees
                                    + ". Must be one of 0, 90, 180, 270.");
                    orientationEnum = ExifInterface.ORIENTATION_UNDEFINED;
                    break;
            }
            return setAttribute(TAG_ORIENTATION, String.valueOf(orientationEnum));
        }

        /**
         * Sets the flash information from
         * {@link androidx.camera.core.impl.CameraCaptureMetaData.FlashState}.
         *
         * @param flashState the state of the flash at capture time.
         */
        @NonNull
        public Builder setFlashState(@NonNull CameraCaptureMetaData.FlashState flashState) {
            if (flashState == CameraCaptureMetaData.FlashState.UNKNOWN) {
                // Cannot set flash state information
                return this;
            }

            short value;
            switch (flashState) {
                case READY:
                    value = 0;
                    break;
                case NONE:
                    value = FLAG_FLASH_NO_FLASH_FUNCTION;
                    break;
                case FIRED:
                    value = FLAG_FLASH_FIRED;
                    break;
                default:
                    Logger.w(TAG, "Unknown flash state: " + flashState);
                    return this;
            }

            if ((value & FLAG_FLASH_FIRED) == FLAG_FLASH_FIRED) {
                // Set light source to flash
                setAttribute(TAG_LIGHT_SOURCE, String.valueOf(LIGHT_SOURCE_FLASH));
            }


            return setAttribute(TAG_FLASH, String.valueOf(value));
        }

        /**
         * Sets the amount of time the sensor was exposed for, in nanoseconds.
         * @param exposureTimeNs The exposure time in nanoseconds.
         */
        @NonNull
        public Builder setExposureTimeNanos(long exposureTimeNs) {
            return setAttribute(TAG_EXPOSURE_TIME,
                    String.valueOf(exposureTimeNs / (double) TimeUnit.SECONDS.toNanos(1)));
        }

        /**
         * Sets the lens f-number.
         *
         * <p>The lens f-number has precision 1.xx, for example, 1.80.
         * @param fNumber The f-number.
         */
        @NonNull
        public Builder setLensFNumber(float fNumber) {
            return setAttribute(TAG_F_NUMBER, String.valueOf(fNumber));
        }

        /**
         * Sets the ISO.
         *
         * @param iso the standard ISO sensitivity value, as defined in ISO 12232:2006.
         */
        @NonNull
        public Builder setIso(int iso) {
            return setAttribute(TAG_SENSITIVITY_TYPE, String.valueOf(SENSITIVITY_TYPE_ISO_SPEED))
                    .setAttribute(TAG_PHOTOGRAPHIC_SENSITIVITY, String.valueOf(Math.min(65535,
                            iso)));
        }

        /**
         * Sets lens focal length, in millimeters.
         *
         * @param focalLength The lens focal length in millimeters.
         */
        @NonNull
        public Builder setFocalLength(float focalLength) {
            LongRational focalLengthRational =
                    new LongRational((long) (focalLength * MM_IN_MICRONS), MM_IN_MICRONS);
            return setAttribute(TAG_FOCAL_LENGTH, focalLengthRational.toString());
        }

        /**
         * Sets the white balance mode.
         *
         * @param whiteBalanceMode The white balance mode. One of {@link WhiteBalanceMode#AUTO}
         *                        or {@link WhiteBalanceMode#MANUAL}.
         */
        @NonNull
        public Builder setWhiteBalanceMode(@NonNull WhiteBalanceMode whiteBalanceMode) {
            String wbString = null;
            switch (whiteBalanceMode) {
                case AUTO:
                    wbString = String.valueOf(WHITE_BALANCE_AUTO);
                    break;
                case MANUAL:
                    wbString = String.valueOf(WHITE_BALANCE_MANUAL);
                    break;
            }
            return setAttribute(TAG_WHITE_BALANCE, wbString);
        }

        /**
         * Sets the value of the specified tag.
         *
         * @param tag   the name of the tag.
         * @param value the value of the tag.
         */
        @NonNull
        public Builder setAttribute(@NonNull String tag, @NonNull String value) {
            setAttributeInternal(tag, value, mAttributes);
            return this;
        }

        /**
         * Removes the attribute with the given tag.
         *
         * @param tag the name of the tag.
         */
        @NonNull
        public Builder removeAttribute(@NonNull String tag) {
            setAttributeInternal(tag, null, mAttributes);
            return this;
        }

        private void setAttributeIfMissing(@NonNull String tag, @NonNull String value,
                @NonNull List<Map<String, ExifAttribute>> attributes) {
            for (Map<String, ExifAttribute> attrs : attributes) {
                if (attrs.containsKey(tag)) {
                    // Attr already exists
                    return;
                }
            }

            // Add missing attribute.
            setAttributeInternal(tag, value, attributes);
        }

        @SuppressWarnings("deprecation")
        // Allows null values to remove attributes
        private void setAttributeInternal(@NonNull String tag, @Nullable String value,
                @NonNull List<Map<String, ExifAttribute>> attributes) {
            // Validate and convert if necessary.
            if (TAG_DATETIME.equals(tag) || TAG_DATETIME_ORIGINAL.equals(tag)
                    || TAG_DATETIME_DIGITIZED.equals(tag)) {
                if (value != null) {
                    boolean isPrimaryFormat = DATETIME_PRIMARY_FORMAT_PATTERN.matcher(value).find();
                    boolean isSecondaryFormat = DATETIME_SECONDARY_FORMAT_PATTERN.matcher(
                            value).find();
                    // Validate
                    if (value.length() != DATETIME_VALUE_STRING_LENGTH
                            || (!isPrimaryFormat && !isSecondaryFormat)) {
                        Logger.w(TAG, "Invalid value for " + tag + " : " + value);
                        return;
                    }
                    // If datetime value has secondary format (e.g. 2020-01-01 00:00:00), convert it
                    // to primary format (e.g. 2020:01:01 00:00:00) since it is the format in the
                    // official documentation.
                    // See JEITA CP-3451C Section 4.6.4. D. Other Tags, DateTime
                    if (isSecondaryFormat) {
                        // Replace "-" with ":" to match the primary format.
                        value = value.replaceAll("-", ":");
                    }
                }
            }
            // Maintain compatibility.
            if (TAG_ISO_SPEED_RATINGS.equals(tag)) {
                if (DEBUG) {
                    Logger.d(TAG, "setAttribute: Replacing TAG_ISO_SPEED_RATINGS with "
                            + "TAG_PHOTOGRAPHIC_SENSITIVITY.");
                }
                tag = TAG_PHOTOGRAPHIC_SENSITIVITY;
            }
            // Convert the given value to rational values for backwards compatibility.
            if (value != null && sTagSetForCompatibility.contains(tag)) {
                if (tag.equals(TAG_GPS_TIMESTAMP)) {
                    Matcher m = GPS_TIMESTAMP_PATTERN.matcher(value);
                    if (!m.find()) {
                        Logger.w(TAG, "Invalid value for " + tag + " : " + value);
                        return;
                    }
                    value = Integer.parseInt(Preconditions.checkNotNull(m.group(1))) + "/1,"
                            + Integer.parseInt(Preconditions.checkNotNull(m.group(2))) + "/1,"
                            + Integer.parseInt(Preconditions.checkNotNull(m.group(3))) + "/1";
                } else {
                    try {
                        double doubleValue = Double.parseDouble(value);
                        value = new LongRational(doubleValue).toString();
                    } catch (NumberFormatException e) {
                        Logger.w(TAG, "Invalid value for " + tag + " : " + value, e);
                        return;
                    }
                }
            }

            for (int i = 0; i < EXIF_TAGS.length; ++i) {
                final ExifTag exifTag = sExifTagMapsForWriting.get(i).get(tag);
                if (exifTag != null) {
                    if (value == null) {
                        attributes.get(i).remove(tag);
                        continue;
                    }
                    Pair<Integer, Integer> guess = guessDataFormat(value);
                    int dataFormat;
                    if (exifTag.primaryFormat == guess.first
                            || exifTag.primaryFormat == guess.second) {
                        dataFormat = exifTag.primaryFormat;
                    } else if (exifTag.secondaryFormat != -1 && (
                            exifTag.secondaryFormat == guess.first
                                    || exifTag.secondaryFormat == guess.second)) {
                        dataFormat = exifTag.secondaryFormat;
                    } else if (exifTag.primaryFormat == IFD_FORMAT_BYTE
                            || exifTag.primaryFormat == IFD_FORMAT_UNDEFINED
                            || exifTag.primaryFormat == IFD_FORMAT_STRING) {
                        dataFormat = exifTag.primaryFormat;
                    } else {
                        if (DEBUG) {
                            Logger.d(TAG, "Given tag (" + tag
                                    + ") value didn't match with one of expected "
                                    + "formats: " + IFD_FORMAT_NAMES[exifTag.primaryFormat]
                                    + (exifTag.secondaryFormat == -1 ? "" : ", "
                                    + IFD_FORMAT_NAMES[exifTag.secondaryFormat]) + " (guess: "
                                    + IFD_FORMAT_NAMES[guess.first] + (guess.second == -1 ? ""
                                    : ", "
                                            + IFD_FORMAT_NAMES[guess.second]) + ")");
                        }
                        continue;
                    }
                    switch (dataFormat) {
                        case IFD_FORMAT_BYTE: {
                            attributes.get(i).put(tag, ExifAttribute.createByte(value));
                            break;
                        }
                        case IFD_FORMAT_UNDEFINED:
                        case IFD_FORMAT_STRING: {
                            attributes.get(i).put(tag, ExifAttribute.createString(value));
                            break;
                        }
                        case IFD_FORMAT_USHORT: {
                            final String[] values = value.split(",", -1);
                            final int[] intArray = new int[values.length];
                            for (int j = 0; j < values.length; ++j) {
                                intArray[j] = Integer.parseInt(values[j]);
                            }
                            attributes.get(i).put(tag,
                                    ExifAttribute.createUShort(intArray, mByteOrder));
                            break;
                        }
                        case IFD_FORMAT_SLONG: {
                            final String[] values = value.split(",", -1);
                            final int[] intArray = new int[values.length];
                            for (int j = 0; j < values.length; ++j) {
                                intArray[j] = Integer.parseInt(values[j]);
                            }
                            attributes.get(i).put(tag,
                                    ExifAttribute.createSLong(intArray, mByteOrder));
                            break;
                        }
                        case IFD_FORMAT_ULONG: {
                            final String[] values = value.split(",", -1);
                            final long[] longArray = new long[values.length];
                            for (int j = 0; j < values.length; ++j) {
                                longArray[j] = Long.parseLong(values[j]);
                            }
                            attributes.get(i).put(tag,
                                    ExifAttribute.createULong(longArray, mByteOrder));
                            break;
                        }
                        case IFD_FORMAT_URATIONAL: {
                            final String[] values = value.split(",", -1);
                            final LongRational[] rationalArray = new LongRational[values.length];
                            for (int j = 0; j < values.length; ++j) {
                                final String[] numbers = values[j].split("/", -1);
                                rationalArray[j] = new LongRational(
                                        (long) Double.parseDouble(numbers[0]),
                                        (long) Double.parseDouble(numbers[1]));
                            }
                            attributes.get(i).put(tag,
                                    ExifAttribute.createURational(rationalArray, mByteOrder));
                            break;
                        }
                        case IFD_FORMAT_SRATIONAL: {
                            final String[] values = value.split(",", -1);
                            final LongRational[] rationalArray = new LongRational[values.length];
                            for (int j = 0; j < values.length; ++j) {
                                final String[] numbers = values[j].split("/", -1);
                                rationalArray[j] = new LongRational(
                                        (long) Double.parseDouble(numbers[0]),
                                        (long) Double.parseDouble(numbers[1]));
                            }
                            attributes.get(i).put(tag,
                                    ExifAttribute.createSRational(rationalArray, mByteOrder));
                            break;
                        }
                        case IFD_FORMAT_DOUBLE: {
                            final String[] values = value.split(",", -1);
                            final double[] doubleArray = new double[values.length];
                            for (int j = 0; j < values.length; ++j) {
                                doubleArray[j] = Double.parseDouble(values[j]);
                            }
                            attributes.get(i).put(tag,
                                    ExifAttribute.createDouble(doubleArray, mByteOrder));
                            break;
                        }
                        default:
                            if (DEBUG) {
                                Logger.d(TAG,
                                        "Data format isn't one of expected formats: " + dataFormat);
                            }
                    }
                }
            }
        }

        /**
         * Builds an {@link ExifData} from the current state of the builder.
         */
        @NonNull
        public ExifData build() {
            // Create a read-only copy of all attributes. This needs to be a deep copy since
            // build() can be called multiple times. We'll remove null values as well.
            List<Map<String, ExifAttribute>> attributes = Collections.list(
                    new Enumeration<Map<String, ExifAttribute>>() {
                        final Enumeration<Map<String, ExifAttribute>> mMapEnumeration =
                                Collections.enumeration(mAttributes);

                        @Override
                        public boolean hasMoreElements() {
                            return mMapEnumeration.hasMoreElements();
                        }

                        @Override
                        public Map<String, ExifAttribute> nextElement() {
                            return new HashMap<>(mMapEnumeration.nextElement());
                        }
                    });
            // Add EXIF defaults if needed
            if (!attributes.get(IFD_TYPE_EXIF).isEmpty()) {
                setAttributeIfMissing(TAG_EXPOSURE_PROGRAM,
                        String.valueOf(EXPOSURE_PROGRAM_NOT_DEFINED), attributes);
                setAttributeIfMissing(TAG_EXIF_VERSION, "0230", attributes);
                // Default is for YCbCr components
                setAttributeIfMissing(TAG_COMPONENTS_CONFIGURATION, "1,2,3,0", attributes);
                setAttributeIfMissing(TAG_METERING_MODE, String.valueOf(METERING_MODE_UNKNOWN),
                        attributes);
                setAttributeIfMissing(TAG_LIGHT_SOURCE, String.valueOf(LIGHT_SOURCE_UNKNOWN),
                        attributes);
                setAttributeIfMissing(TAG_FLASHPIX_VERSION, "0100", attributes);
                setAttributeIfMissing(TAG_FOCAL_PLANE_RESOLUTION_UNIT,
                        String.valueOf(RESOLUTION_UNIT_INCHES), attributes);
                setAttributeIfMissing(TAG_FILE_SOURCE, String.valueOf(FILE_SOURCE_DSC), attributes);
                setAttributeIfMissing(TAG_SCENE_TYPE,
                        String.valueOf(SCENE_TYPE_DIRECTLY_PHOTOGRAPHED), attributes);
                setAttributeIfMissing(TAG_CUSTOM_RENDERED, String.valueOf(RENDERED_PROCESS_NORMAL),
                        attributes);
                setAttributeIfMissing(TAG_SCENE_CAPTURE_TYPE,
                        String.valueOf(SCENE_CAPTURE_TYPE_STANDARD), attributes);
                setAttributeIfMissing(TAG_CONTRAST, String.valueOf(CONTRAST_NORMAL), attributes);
                setAttributeIfMissing(TAG_SATURATION, String.valueOf(SATURATION_NORMAL),
                        attributes);
                setAttributeIfMissing(TAG_SHARPNESS, String.valueOf(SHARPNESS_NORMAL), attributes);
            }
            // Add GPS defaults if needed
            if (!attributes.get(IFD_TYPE_GPS).isEmpty()) {
                setAttributeIfMissing(TAG_GPS_VERSION_ID, "2300", attributes);
                setAttributeIfMissing(TAG_GPS_SPEED_REF, GPS_SPEED_KILOMETERS_PER_HOUR, attributes);
                setAttributeIfMissing(TAG_GPS_TRACK_REF, GPS_DIRECTION_TRUE, attributes);
                setAttributeIfMissing(TAG_GPS_IMG_DIRECTION_REF, GPS_DIRECTION_TRUE, attributes);
                setAttributeIfMissing(TAG_GPS_DEST_BEARING_REF, GPS_DIRECTION_TRUE, attributes);
                setAttributeIfMissing(TAG_GPS_DEST_DISTANCE_REF, GPS_DISTANCE_KILOMETERS,
                        attributes);
            }
            return new ExifData(mByteOrder, attributes);
        }

        /**
         * Determines the data format of EXIF entry value.
         *
         * @param entryValue The value to be determined.
         * @return Returns two data formats guessed as a pair in integer. If there is no two
         * candidate
         * data formats for the given entry value, returns {@code -1} in the second of the pair.
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
                    } catch (NumberFormatException e) {
                        // Ignored
                    }
                }
                return new Pair<>(IFD_FORMAT_STRING, -1);
            }
            try {
                long longValue = Long.parseLong(entryValue);
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
    }
}

