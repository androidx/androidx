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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A class for indicating EXIF attribute.
 *
 * This class was pulled from the {@link androidx.exifinterface.media.ExifInterface} class.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ExifAttribute {
    private static final String TAG = "ExifAttribute";
    public static final long BYTES_OFFSET_UNKNOWN = -1;

    // See JPEG File Interchange Format Version 1.02.
    // The following values are defined for handling JPEG streams. In this implementation, we are
    // not only getting information from EXIF but also from some JPEG special segments such as
    // MARKER_COM for user comment and MARKER_SOFx for image width and height.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final Charset ASCII = StandardCharsets.US_ASCII;

    // Formats for the value in IFD entry (See TIFF 6.0 Section 2, "Image File Directory".)
    static final int IFD_FORMAT_BYTE = 1;
    static final int IFD_FORMAT_STRING = 2;
    static final int IFD_FORMAT_USHORT = 3;
    static final int IFD_FORMAT_ULONG = 4;
    static final int IFD_FORMAT_URATIONAL = 5;
    static final int IFD_FORMAT_SBYTE = 6;
    static final int IFD_FORMAT_UNDEFINED = 7;
    static final int IFD_FORMAT_SSHORT = 8;
    static final int IFD_FORMAT_SLONG = 9;
    static final int IFD_FORMAT_SRATIONAL = 10;
    static final int IFD_FORMAT_SINGLE = 11;
    static final int IFD_FORMAT_DOUBLE = 12;
    // Names for the data formats for debugging purpose.
    static final String[] IFD_FORMAT_NAMES = new String[] {
            "", "BYTE", "STRING", "USHORT", "ULONG", "URATIONAL", "SBYTE", "UNDEFINED", "SSHORT",
            "SLONG", "SRATIONAL", "SINGLE", "DOUBLE", "IFD"
    };
    // Sizes of the components of each IFD value format
    static final int[] IFD_FORMAT_BYTES_PER_FORMAT = new int[] {
            0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8, 1
    };

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final byte[] EXIF_ASCII_PREFIX = new byte[] {
            0x41, 0x53, 0x43, 0x49, 0x49, 0x0, 0x0, 0x0
    };

    public final int format;
    public final int numberOfComponents;
    public final long bytesOffset;
    public final byte[] bytes;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ExifAttribute(int format, int numberOfComponents, byte[] bytes) {
        this(format, numberOfComponents, BYTES_OFFSET_UNKNOWN, bytes);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ExifAttribute(int format, int numberOfComponents, long bytesOffset, byte[] bytes) {
        this.format = format;
        this.numberOfComponents = numberOfComponents;
        this.bytesOffset = bytesOffset;
        this.bytes = bytes;
    }

    @NonNull
    public static ExifAttribute createUShort(@NonNull int[] values, @NonNull ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_USHORT] * values.length]);
        buffer.order(byteOrder);
        for (int value : values) {
            buffer.putShort((short) value);
        }
        return new ExifAttribute(IFD_FORMAT_USHORT, values.length, buffer.array());
    }

    @NonNull
    public static ExifAttribute createUShort(int value, @NonNull ByteOrder byteOrder) {
        return createUShort(new int[] {value}, byteOrder);
    }

    @NonNull
    public static ExifAttribute createULong(@NonNull long[] values, @NonNull ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_ULONG] * values.length]);
        buffer.order(byteOrder);
        for (long value : values) {
            buffer.putInt((int) value);
        }
        return new ExifAttribute(IFD_FORMAT_ULONG, values.length, buffer.array());
    }

    @NonNull
    public static ExifAttribute createULong(long value, @NonNull ByteOrder byteOrder) {
        return createULong(new long[] {value}, byteOrder);
    }

    @NonNull
    public static ExifAttribute createSLong(@NonNull int[] values, @NonNull ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_SLONG] * values.length]);
        buffer.order(byteOrder);
        for (int value : values) {
            buffer.putInt(value);
        }
        return new ExifAttribute(IFD_FORMAT_SLONG, values.length, buffer.array());
    }

    @NonNull
    public static ExifAttribute createSLong(int value, @NonNull ByteOrder byteOrder) {
        return createSLong(new int[] {value}, byteOrder);
    }

    @NonNull
    public static ExifAttribute createByte(@NonNull String value) {
        // Exception for GPSAltitudeRef tag
        if (value.length() == 1 && value.charAt(0) >= '0' && value.charAt(0) <= '1') {
            final byte[] bytes = new byte[] { (byte) (value.charAt(0) - '0') };
            return new ExifAttribute(IFD_FORMAT_BYTE, bytes.length, bytes);
        }
        final byte[] ascii = value.getBytes(ASCII);
        return new ExifAttribute(IFD_FORMAT_BYTE, ascii.length, ascii);
    }

    @NonNull
    public static ExifAttribute createString(@NonNull String value) {
        final byte[] ascii = (value + '\0').getBytes(ASCII);
        return new ExifAttribute(IFD_FORMAT_STRING, ascii.length, ascii);
    }

    @NonNull
    public static ExifAttribute createURational(@NonNull LongRational[] values,
            @NonNull ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_URATIONAL] * values.length]);
        buffer.order(byteOrder);
        for (LongRational value : values) {
            buffer.putInt((int) value.getNumerator());
            buffer.putInt((int) value.getDenominator());
        }
        return new ExifAttribute(IFD_FORMAT_URATIONAL, values.length, buffer.array());
    }

    @NonNull
    public static ExifAttribute createURational(@NonNull LongRational value,
            @NonNull ByteOrder byteOrder) {
        return createURational(new LongRational[] {value}, byteOrder);
    }

    @NonNull
    public static ExifAttribute createSRational(@NonNull LongRational[] values,
            @NonNull ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_SRATIONAL] * values.length]);
        buffer.order(byteOrder);
        for (LongRational value : values) {
            buffer.putInt((int) value.getNumerator());
            buffer.putInt((int) value.getDenominator());
        }
        return new ExifAttribute(IFD_FORMAT_SRATIONAL, values.length, buffer.array());
    }

    @NonNull
    public static ExifAttribute createSRational(@NonNull LongRational value,
            @NonNull ByteOrder byteOrder) {
        return createSRational(new LongRational[] {value}, byteOrder);
    }

    @NonNull
    public static ExifAttribute createDouble(@NonNull double[] values,
            @NonNull ByteOrder byteOrder) {
        final ByteBuffer buffer = ByteBuffer.wrap(
                new byte[IFD_FORMAT_BYTES_PER_FORMAT[IFD_FORMAT_DOUBLE] * values.length]);
        buffer.order(byteOrder);
        for (double value : values) {
            buffer.putDouble(value);
        }
        return new ExifAttribute(IFD_FORMAT_DOUBLE, values.length, buffer.array());
    }

    @NonNull
    public static ExifAttribute createDouble(double value, @NonNull ByteOrder byteOrder) {
        return createDouble(new double[] {value}, byteOrder);
    }

    @Override
    public String toString() {
        return "(" + IFD_FORMAT_NAMES[format] + ", data length:" + bytes.length + ")";
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Object getValue(ByteOrder byteOrder) {
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
                    final LongRational[] values = new LongRational[numberOfComponents];
                    for (int i = 0; i < numberOfComponents; ++i) {
                        final long numerator = inputStream.readUnsignedInt();
                        final long denominator = inputStream.readUnsignedInt();
                        values[i] = new LongRational(numerator, denominator);
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
                    final LongRational[] values = new LongRational[numberOfComponents];
                    for (int i = 0; i < numberOfComponents; ++i) {
                        final long numerator = inputStream.readInt();
                        final long denominator = inputStream.readInt();
                        values[i] = new LongRational(numerator, denominator);
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
            Logger.w(TAG, "IOException occurred during reading a value", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Logger.e(TAG, "IOException occurred while closing InputStream", e);
                }
            }
        }
    }

    public double getDoubleValue(@NonNull ByteOrder byteOrder) {
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
        if (value instanceof LongRational[]) {
            LongRational[] array = (LongRational[]) value;
            if (array.length == 1) {
                return array[0].toDouble();
            }
            throw new NumberFormatException("There are more than one component");
        }
        throw new NumberFormatException("Couldn't find a double value");
    }

    public int getIntValue(@NonNull ByteOrder byteOrder) {
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

    @Nullable
    public String getStringValue(@NonNull ByteOrder byteOrder) {
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
        if (value instanceof LongRational[]) {
            LongRational[] array = (LongRational[]) value;
            for (int i = 0; i < array.length; ++i) {
                stringBuilder.append(array[i].getNumerator());
                stringBuilder.append('/');
                stringBuilder.append(array[i].getDenominator());
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
