/*
 * Copyright 2019 The Android Open Source Project
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

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for modifying metadata on JPEG files.
 *
 * <p>Call {@link #save()} to persist changes to disc.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Exif {

    /** Timestamp value indicating a timestamp value that is either not set or not valid */
    public static final long INVALID_TIMESTAMP = -1;

    private static final String TAG = Exif.class.getSimpleName();

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                public SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy:MM:dd", Locale.US);
                }
            };
    private static final ThreadLocal<SimpleDateFormat> TIME_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                public SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("HH:mm:ss", Locale.US);
                }
            };
    private static final ThreadLocal<SimpleDateFormat> DATETIME_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                public SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
                }
            };

    private static final String KILOMETERS_PER_HOUR = "K";
    private static final String MILES_PER_HOUR = "M";
    private static final String KNOTS = "N";

    /** All public tags in {@link ExifInterface}. */
    private static final List<String> ALL_EXIF_TAGS = getAllExifTags();
    // Exif tags that should not be copied to the cropped image.
    private static final List<String> DO_NOT_COPY_EXIF_TAGS = Arrays.asList(
            // Dimension-related tags, which might change after cropping.
            ExifInterface.TAG_IMAGE_WIDTH,
            ExifInterface.TAG_IMAGE_LENGTH,
            ExifInterface.TAG_PIXEL_X_DIMENSION,
            ExifInterface.TAG_PIXEL_Y_DIMENSION,
            // Thumbnail-related tags. Currently we do not create thumbnail for cropped images.
            ExifInterface.TAG_COMPRESSION, // Our primary image is always Jpeg.
            ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
            ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
            ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
            ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
            ExifInterface.TAG_THUMBNAIL_ORIENTATION);

    private final ExifInterface mExifInterface;

    // When true, avoid saving any time. This is a privacy issue.
    private boolean mRemoveTimestamp = false;

    private Exif(ExifInterface exifInterface) {
        mExifInterface = exifInterface;
    }

    /**
     * Returns an Exif from the exif data contained in the file.
     *
     * @param file the file to read exif data from
     */
    @NonNull
    public static Exif createFromFile(@NonNull File file) throws IOException {
        return createFromFileString(file.toString());
    }

    /**
     * Returns an Exif extracted from the given {@link ImageProxy}.
     *
     * <p> This method rewinds and reads the given buffer.
     */
    @NonNull
    public static Exif createFromImageProxy(@NonNull ImageProxy imageProxy) throws IOException {
        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        // Rewind to make sure it is at the beginning of the buffer
        buffer.rewind();

        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        InputStream inputStream = new ByteArrayInputStream(data);
        return Exif.createFromInputStream(inputStream);
    }

    /**
     * Returns an Exif from the exif data contained in the file at the filePath
     *
     * @param filePath the path to the file to read exif data from
     */
    @NonNull
    public static Exif createFromFileString(@NonNull String filePath) throws IOException {
        return new Exif(new ExifInterface(filePath));
    }

    /**
     * Returns an Exif from the exif data contain in the input stream.
     *
     * @param is the input stream to read exif data from
     */
    @NonNull
    public static Exif createFromInputStream(@NonNull InputStream is) throws IOException {
        return new Exif(new ExifInterface(is));
    }

    private static String convertToExifDateTime(long timestamp) {
        return DATETIME_FORMAT.get().format(new Date(timestamp));
    }

    private static Date convertFromExifDateTime(String dateTime) throws ParseException {
        return DATETIME_FORMAT.get().parse(dateTime);
    }

    private static Date convertFromExifDate(String date) throws ParseException {
        return DATE_FORMAT.get().parse(date);
    }

    private static Date convertFromExifTime(String time) throws ParseException {
        return TIME_FORMAT.get().parse(time);
    }

    /** Persists changes to disc. */
    public void save() throws IOException {
        if (!mRemoveTimestamp) {
            attachLastModifiedTimestamp();
        }
        mExifInterface.saveAttributes();
    }

    /**
     * Copies Exif values to the given instance.
     *
     * <p> This methods is for copying exif data from the original image to the cropped image. Tags
     * affected by cropping are not copied.
     */
    public void copyToCroppedImage(@NonNull Exif croppedExif) {
        List<String> exifTags = new ArrayList<>(ALL_EXIF_TAGS);
        exifTags.removeAll(DO_NOT_COPY_EXIF_TAGS);
        for (String tag : exifTags) {
            String originalValue = mExifInterface.getAttribute(tag);
            if (originalValue != null) {
                croppedExif.mExifInterface.setAttribute(tag, originalValue);
            }
        }
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ENGLISH,
                "Exif{width=%s, height=%s, rotation=%d, "
                        + "isFlippedVertically=%s, isFlippedHorizontally=%s, location=%s, "
                        + "timestamp=%s, description=%s}",
                getWidth(),
                getHeight(),
                getRotation(),
                isFlippedVertically(),
                isFlippedHorizontally(),
                getLocation(),
                getTimestamp(),
                getDescription());
    }

    public int getOrientation() {
        return mExifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
    }

    /** Sets the orientation for the exif. */
    public void setOrientation(int orientation) {
        mExifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
    }

    /** Returns the width of the photo in pixels. */
    public int getWidth() {
        return mExifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
    }

    /** Returns the height of the photo in pixels. */
    public int getHeight() {
        return mExifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
    }

    @Nullable
    public String getDescription() {
        return mExifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
    }

    /** Sets the description for the exif. */
    public void setDescription(@Nullable String description) {
        mExifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description);
    }

    /** @return The degree of rotation (eg. 0, 90, 180, 270). */
    public int getRotation() {
        switch (getOrientation()) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return 0;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return 180;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                return 270;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            case ExifInterface.ORIENTATION_NORMAL:
                // Fall-through
            case ExifInterface.ORIENTATION_UNDEFINED:
                // Fall-through
            default:
                return 0;
        }
    }

    /** @return True if the image is flipped vertically after rotation. */
    public boolean isFlippedVertically() {
        switch (getOrientation()) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return false;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return false;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return true;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                return true;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return false;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return true;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return false;
            case ExifInterface.ORIENTATION_NORMAL:
                // Fall-through
            case ExifInterface.ORIENTATION_UNDEFINED:
                // Fall-through
            default:
                return false;
        }
    }

    /** @return True if the image is flipped horizontally after rotation. */
    public boolean isFlippedHorizontally() {
        switch (getOrientation()) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return true;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return false;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return false;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                return false;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return false;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return false;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return false;
            case ExifInterface.ORIENTATION_NORMAL:
                // Fall-through
            case ExifInterface.ORIENTATION_UNDEFINED:
                // Fall-through
            default:
                return false;
        }
    }

    private void attachLastModifiedTimestamp() {
        long now = System.currentTimeMillis();
        String datetime = convertToExifDateTime(now);

        mExifInterface.setAttribute(ExifInterface.TAG_DATETIME, datetime);

        try {
            String subsec = Long.toString(now - convertFromExifDateTime(datetime).getTime());
            mExifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME, subsec);
        } catch (ParseException e) {
        }
    }

    /**
     * @return The timestamp (in millis) that this picture was modified, or {@link
     * #INVALID_TIMESTAMP} if no time is available.
     */
    public long getLastModifiedTimestamp() {
        long timestamp = parseTimestamp(mExifInterface.getAttribute(ExifInterface.TAG_DATETIME));
        if (timestamp == INVALID_TIMESTAMP) {
            return INVALID_TIMESTAMP;
        }

        String subSecs = mExifInterface.getAttribute(ExifInterface.TAG_SUBSEC_TIME);
        if (subSecs != null) {
            try {
                long sub = Long.parseLong(subSecs);
                while (sub > 1000) {
                    sub /= 10;
                }
                timestamp += sub;
            } catch (NumberFormatException e) {
                // Ignored
            }
        }

        return timestamp;
    }

    /**
     * @return The timestamp (in millis) that this picture was taken, or {@link #INVALID_TIMESTAMP}
     * if no time is available.
     */
    public long getTimestamp() {
        long timestamp =
                parseTimestamp(mExifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
        if (timestamp == INVALID_TIMESTAMP) {
            return INVALID_TIMESTAMP;
        }

        String subSecs = mExifInterface.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL);
        if (subSecs != null) {
            try {
                long sub = Long.parseLong(subSecs);
                while (sub > 1000) {
                    sub /= 10;
                }
                timestamp += sub;
            } catch (NumberFormatException e) {
                // Ignored
            }
        }

        return timestamp;
    }

    /** @return The location this picture was taken, or null if no location is available. */
    @Nullable
    public Location getLocation() {
        String provider = mExifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);
        double[] latlng = mExifInterface.getLatLong();
        double altitude = mExifInterface.getAltitude(0);
        double speed = mExifInterface.getAttributeDouble(ExifInterface.TAG_GPS_SPEED, 0);
        String speedRef = mExifInterface.getAttribute(ExifInterface.TAG_GPS_SPEED_REF);
        speedRef = speedRef == null ? KILOMETERS_PER_HOUR : speedRef; // Ensure speedRef is not null
        long timestamp =
                parseTimestamp(
                        mExifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP),
                        mExifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP));
        if (latlng == null) {
            return null;
        }
        if (provider == null) {
            provider = TAG;
        }

        Location location = new Location(provider);
        location.setLatitude(latlng[0]);
        location.setLongitude(latlng[1]);
        if (altitude != 0) {
            location.setAltitude(altitude);
        }
        if (speed != 0) {
            switch (speedRef) {
                case MILES_PER_HOUR:
                    speed = Speed.fromMilesPerHour(speed).toMetersPerSecond();
                    break;
                case KNOTS:
                    speed = Speed.fromKnots(speed).toMetersPerSecond();
                    break;
                case KILOMETERS_PER_HOUR:
                    // fall through
                default:
                    speed = Speed.fromKilometersPerHour(speed).toMetersPerSecond();
                    break;
            }

            location.setSpeed((float) speed);
        }
        if (timestamp != INVALID_TIMESTAMP) {
            location.setTime(timestamp);
        }
        return location;
    }

    /**
     * Rotates the image by the given degrees. Can only rotate by right angles (eg. 90, 180, -90).
     * Other increments will set the orientation to undefined.
     */
    public void rotate(int degrees) {
        if (degrees % 90 != 0) {
            Logger.w(
                    TAG,
                    String.format(Locale.US,
                            "Can only rotate in right angles (eg. 0, 90, 180, 270). %d is "
                                    + "unsupported.",
                            degrees));
            mExifInterface.setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    String.valueOf(ExifInterface.ORIENTATION_UNDEFINED));
            return;
        }

        degrees %= 360;

        int orientation = getOrientation();
        while (degrees < 0) {
            degrees += 90;

            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    orientation = ExifInterface.ORIENTATION_TRANSPOSE;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientation = ExifInterface.ORIENTATION_ROTATE_90;
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    orientation = ExifInterface.ORIENTATION_TRANSVERSE;
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    orientation = ExifInterface.ORIENTATION_FLIP_VERTICAL;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = ExifInterface.ORIENTATION_NORMAL;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    orientation = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = ExifInterface.ORIENTATION_ROTATE_90;
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                    // Fall-through
                case ExifInterface.ORIENTATION_UNDEFINED:
                    // Fall-through
                default:
                    orientation = ExifInterface.ORIENTATION_ROTATE_270;
                    break;
            }
        }
        while (degrees > 0) {
            degrees -= 90;

            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    orientation = ExifInterface.ORIENTATION_TRANSVERSE;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientation = ExifInterface.ORIENTATION_ROTATE_270;
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    orientation = ExifInterface.ORIENTATION_TRANSPOSE;
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    orientation = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = ExifInterface.ORIENTATION_ROTATE_180;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    orientation = ExifInterface.ORIENTATION_FLIP_VERTICAL;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = ExifInterface.ORIENTATION_NORMAL;
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                    // Fall-through
                case ExifInterface.ORIENTATION_UNDEFINED:
                    // Fall-through
                default:
                    orientation = ExifInterface.ORIENTATION_ROTATE_90;
                    break;
            }
        }
        mExifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
    }

    /**
     * Sets attributes to represent a flip of the image over the horizon so that the top and bottom
     * are reversed.
     */
    public void flipVertically() {
        int orientation;
        switch (getOrientation()) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                orientation = ExifInterface.ORIENTATION_ROTATE_180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientation = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                orientation = ExifInterface.ORIENTATION_NORMAL;
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                orientation = ExifInterface.ORIENTATION_ROTATE_270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = ExifInterface.ORIENTATION_TRANSVERSE;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                orientation = ExifInterface.ORIENTATION_ROTATE_90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = ExifInterface.ORIENTATION_TRANSPOSE;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                // Fall-through
            case ExifInterface.ORIENTATION_UNDEFINED:
                // Fall-through
            default:
                orientation = ExifInterface.ORIENTATION_FLIP_VERTICAL;
                break;
        }
        mExifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
    }

    /**
     * Sets attributes to represent a flip of the image over the vertical so that the left and right
     * are reversed.
     */
    public void flipHorizontally() {
        int orientation;
        switch (getOrientation()) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                orientation = ExifInterface.ORIENTATION_NORMAL;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientation = ExifInterface.ORIENTATION_FLIP_VERTICAL;
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                orientation = ExifInterface.ORIENTATION_ROTATE_180;
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                orientation = ExifInterface.ORIENTATION_ROTATE_90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = ExifInterface.ORIENTATION_TRANSPOSE;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                orientation = ExifInterface.ORIENTATION_ROTATE_270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = ExifInterface.ORIENTATION_TRANSVERSE;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                // Fall-through
            case ExifInterface.ORIENTATION_UNDEFINED:
                // Fall-through
            default:
                orientation = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
                break;
        }
        mExifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
    }

    /** Attaches the current timestamp to the file. */
    public void attachTimestamp() {
        long now = System.currentTimeMillis();
        String datetime = convertToExifDateTime(now);

        mExifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, datetime);
        mExifInterface.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, datetime);

        try {
            String subsec = Long.toString(now - convertFromExifDateTime(datetime).getTime());
            mExifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, subsec);
            mExifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, subsec);
        } catch (ParseException e) {
        }

        mRemoveTimestamp = false;
    }

    /** Removes the timestamp from the file. */
    public void removeTimestamp() {
        mExifInterface.setAttribute(ExifInterface.TAG_DATETIME, null);
        mExifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, null);
        mExifInterface.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, null);
        mExifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME, null);
        mExifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, null);
        mExifInterface.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, null);
        mRemoveTimestamp = true;
    }

    /** Attaches the given location to the file. */
    public void attachLocation(@NonNull Location location) {
        mExifInterface.setGpsInfo(location);
    }

    /** Removes the location from the file. */
    public void removeLocation() {
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_SPEED, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_SPEED_REF, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null);
        mExifInterface.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null);
    }

    /** @return The timestamp (in millis), or {@link #INVALID_TIMESTAMP} if no time is available. */
    private long parseTimestamp(@Nullable String date, @Nullable String time) {
        if (date == null && time == null) {
            return INVALID_TIMESTAMP;
        }
        if (time == null) {
            try {
                return convertFromExifDate(date).getTime();
            } catch (ParseException e) {
                return INVALID_TIMESTAMP;
            }
        }
        if (date == null) {
            try {
                return convertFromExifTime(time).getTime();
            } catch (ParseException e) {
                return INVALID_TIMESTAMP;
            }
        }
        return parseTimestamp(date + " " + time);
    }

    /** @return The timestamp (in millis), or {@link #INVALID_TIMESTAMP} if no time is available. */
    private long parseTimestamp(@Nullable String datetime) {
        if (datetime == null) {
            return INVALID_TIMESTAMP;
        }
        try {
            return convertFromExifDateTime(datetime).getTime();
        } catch (ParseException e) {
            return INVALID_TIMESTAMP;
        }
    }

    private static final class Speed {
        static Converter fromKilometersPerHour(double kph) {
            return new Converter(kph * 0.621371);
        }

        static Converter fromMetersPerSecond(double mps) {
            return new Converter(mps * 2.23694);
        }

        static Converter fromMilesPerHour(double mph) {
            return new Converter(mph);
        }

        static Converter fromKnots(double knots) {
            return new Converter(knots * 1.15078);
        }

        static final class Converter {
            final double mMph;

            Converter(double mph) {
                mMph = mph;
            }

            double toKilometersPerHour() {
                return mMph / 0.621371;
            }

            double toMilesPerHour() {
                return mMph;
            }

            double toKnots() {
                return mMph / 1.15078;
            }

            double toMetersPerSecond() {
                return mMph / 2.23694;
            }
        }
    }

    /**
     * Creates a list that contains all public tags defined in {@link ExifInterface}.
     *
     * <p> Deprecated tags are not included.
     */
    @NonNull
    public static List<String> getAllExifTags() {
        return Arrays.asList(
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_IMAGE_LENGTH,
                ExifInterface.TAG_BITS_PER_SAMPLE,
                ExifInterface.TAG_COMPRESSION,
                ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_SAMPLES_PER_PIXEL,
                ExifInterface.TAG_PLANAR_CONFIGURATION,
                ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
                ExifInterface.TAG_Y_CB_CR_POSITIONING,
                ExifInterface.TAG_X_RESOLUTION,
                ExifInterface.TAG_Y_RESOLUTION,
                ExifInterface.TAG_RESOLUTION_UNIT,
                ExifInterface.TAG_STRIP_OFFSETS,
                ExifInterface.TAG_ROWS_PER_STRIP,
                ExifInterface.TAG_STRIP_BYTE_COUNTS,
                ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
                ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                ExifInterface.TAG_TRANSFER_FUNCTION,
                ExifInterface.TAG_WHITE_POINT,
                ExifInterface.TAG_PRIMARY_CHROMATICITIES,
                ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
                ExifInterface.TAG_REFERENCE_BLACK_WHITE,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_IMAGE_DESCRIPTION,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_EXIF_VERSION,
                ExifInterface.TAG_FLASHPIX_VERSION,
                ExifInterface.TAG_COLOR_SPACE,
                ExifInterface.TAG_GAMMA,
                ExifInterface.TAG_PIXEL_X_DIMENSION,
                ExifInterface.TAG_PIXEL_Y_DIMENSION,
                ExifInterface.TAG_COMPONENTS_CONFIGURATION,
                ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
                ExifInterface.TAG_MAKER_NOTE,
                ExifInterface.TAG_USER_COMMENT,
                ExifInterface.TAG_RELATED_SOUND_FILE,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_OFFSET_TIME,
                ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
                ExifInterface.TAG_SUBSEC_TIME,
                ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_PROGRAM,
                ExifInterface.TAG_SPECTRAL_SENSITIVITY,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_OECF,
                ExifInterface.TAG_SENSITIVITY_TYPE,
                ExifInterface.TAG_STANDARD_OUTPUT_SENSITIVITY,
                ExifInterface.TAG_RECOMMENDED_EXPOSURE_INDEX,
                ExifInterface.TAG_ISO_SPEED,
                ExifInterface.TAG_ISO_SPEED_LATITUDE_YYY,
                ExifInterface.TAG_ISO_SPEED_LATITUDE_ZZZ,
                ExifInterface.TAG_SHUTTER_SPEED_VALUE,
                ExifInterface.TAG_APERTURE_VALUE,
                ExifInterface.TAG_BRIGHTNESS_VALUE,
                ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
                ExifInterface.TAG_MAX_APERTURE_VALUE,
                ExifInterface.TAG_SUBJECT_DISTANCE,
                ExifInterface.TAG_METERING_MODE,
                ExifInterface.TAG_LIGHT_SOURCE,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_SUBJECT_AREA,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_FLASH_ENERGY,
                ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
                ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
                ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
                ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
                ExifInterface.TAG_SUBJECT_LOCATION,
                ExifInterface.TAG_EXPOSURE_INDEX,
                ExifInterface.TAG_SENSING_METHOD,
                ExifInterface.TAG_FILE_SOURCE,
                ExifInterface.TAG_SCENE_TYPE,
                ExifInterface.TAG_CFA_PATTERN,
                ExifInterface.TAG_CUSTOM_RENDERED,
                ExifInterface.TAG_EXPOSURE_MODE,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
                ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                ExifInterface.TAG_SCENE_CAPTURE_TYPE,
                ExifInterface.TAG_GAIN_CONTROL,
                ExifInterface.TAG_CONTRAST,
                ExifInterface.TAG_SATURATION,
                ExifInterface.TAG_SHARPNESS,
                ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
                ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
                ExifInterface.TAG_IMAGE_UNIQUE_ID,
                ExifInterface.TAG_CAMERA_OWNER_NAME,
                ExifInterface.TAG_BODY_SERIAL_NUMBER,
                ExifInterface.TAG_LENS_SPECIFICATION,
                ExifInterface.TAG_LENS_MAKE,
                ExifInterface.TAG_LENS_MODEL,
                ExifInterface.TAG_LENS_SERIAL_NUMBER,
                ExifInterface.TAG_GPS_VERSION_ID,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_SATELLITES,
                ExifInterface.TAG_GPS_STATUS,
                ExifInterface.TAG_GPS_MEASURE_MODE,
                ExifInterface.TAG_GPS_DOP,
                ExifInterface.TAG_GPS_SPEED_REF,
                ExifInterface.TAG_GPS_SPEED,
                ExifInterface.TAG_GPS_TRACK_REF,
                ExifInterface.TAG_GPS_TRACK,
                ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
                ExifInterface.TAG_GPS_IMG_DIRECTION,
                ExifInterface.TAG_GPS_MAP_DATUM,
                ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
                ExifInterface.TAG_GPS_DEST_LATITUDE,
                ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
                ExifInterface.TAG_GPS_DEST_LONGITUDE,
                ExifInterface.TAG_GPS_DEST_BEARING_REF,
                ExifInterface.TAG_GPS_DEST_BEARING,
                ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
                ExifInterface.TAG_GPS_DEST_DISTANCE,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_AREA_INFORMATION,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_DIFFERENTIAL,
                ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
                ExifInterface.TAG_INTEROPERABILITY_INDEX,
                ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
                ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
                ExifInterface.TAG_THUMBNAIL_ORIENTATION,
                ExifInterface.TAG_DNG_VERSION,
                ExifInterface.TAG_DEFAULT_CROP_SIZE,
                ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,
                ExifInterface.TAG_ORF_PREVIEW_IMAGE_START,
                ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH,
                ExifInterface.TAG_ORF_ASPECT_FRAME,
                ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER,
                ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER,
                ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER,
                ExifInterface.TAG_RW2_SENSOR_TOP_BORDER,
                ExifInterface.TAG_RW2_ISO,
                ExifInterface.TAG_RW2_JPG_FROM_RAW,
                ExifInterface.TAG_XMP,
                ExifInterface.TAG_NEW_SUBFILE_TYPE,
                ExifInterface.TAG_SUBFILE_TYPE);
    }
}

