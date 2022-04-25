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

import static androidx.camera.core.impl.utils.ExifAttribute.ASCII;
import static androidx.camera.core.impl.utils.ExifData.Builder.sExifTagMapsForWriting;
import static androidx.camera.core.impl.utils.ExifData.EXIF_POINTER_TAGS;
import static androidx.camera.core.impl.utils.ExifData.EXIF_TAGS;
import static androidx.camera.core.impl.utils.ExifData.IFD_TYPE_EXIF;
import static androidx.camera.core.impl.utils.ExifData.IFD_TYPE_GPS;
import static androidx.camera.core.impl.utils.ExifData.IFD_TYPE_INTEROPERABILITY;
import static androidx.camera.core.impl.utils.ExifData.IFD_TYPE_PRIMARY;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.Map;

/**
 * This class provides a way to replace the Exif header of a JPEG image.
 * <p>
 * Below is an example of writing EXIF data into a file
 *
 * <pre>
 * public static void writeExif(byte[] jpeg, ExifData exif, String path) {
 *     OutputStream os = null;
 *     try {
 *         os = new FileOutputStream(path);
 *         // Set the exif header on the output stream
 *         ExifOutputStream eos = new ExifOutputStream(os, exif);
 *         // Write the original jpeg out, the header will be added into the file.
 *         eos.write(jpeg);
 *     } catch (FileNotFoundException e) {
 *         e.printStackTrace();
 *     } catch (IOException e) {
 *         e.printStackTrace();
 *     } finally {
 *         if (os != null) {
 *             try {
 *                 os.close();
 *             } catch (IOException e) {
 *                 e.printStackTrace();
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ExifOutputStream extends FilterOutputStream {
    private static final String TAG = "ExifOutputStream";
    private static final boolean DEBUG = false;
    private static final int STREAMBUFFER_SIZE = 0x00010000; // 64Kb

    private static final int STATE_SOI = 0;
    private static final int STATE_FRAME_HEADER = 1;
    private static final int STATE_JPEG_DATA = 2;

    // Identifier for EXIF APP1 segment in JPEG
    private static final byte[] IDENTIFIER_EXIF_APP1 = "Exif\0\0".getBytes(ASCII);

    // Types of Exif byte alignments (see JEITA CP-3451C Section 4.5.2)
    private static final short BYTE_ALIGN_II = 0x4949;  // II: Intel order
    private static final short BYTE_ALIGN_MM = 0x4d4d;  // MM: Motorola order

    // TIFF Header Fixed Constant (see JEITA CP-3451C Section 4.5.2)
    private static final byte START_CODE = 0x2a; // 42
    private static final int IFD_OFFSET = 8;

    private final ExifData mExifData;
    private final byte[] mSingleByteArray = new byte[1];
    private final ByteBuffer mBuffer = ByteBuffer.allocate(4);
    private int mState = STATE_SOI;
    private int mByteToSkip;
    private int mByteToCopy;

    /**
     * Creates an ExifOutputStream that wraps the given {@link OutputStream} and overwrites exif
     * with the provided {@link ExifData}.
     * @param ou OutputStream which will be sent the final output.
     * @param exifData Exif data which will overwrite any exif data sent to this stream.
     */
    public ExifOutputStream(@NonNull OutputStream ou, @NonNull ExifData exifData) {
        super(new BufferedOutputStream(ou, STREAMBUFFER_SIZE));
        mExifData = exifData;
    }

    private int requestByteToBuffer(int requestByteCount, byte[] buffer, int offset, int length) {
        int byteNeeded = requestByteCount - mBuffer.position();
        int byteToRead = Math.min(length, byteNeeded);
        mBuffer.put(buffer, offset, byteToRead);
        return byteToRead;
    }

    /**
     * Writes the image out. The input data should be a valid JPEG format. After
     * writing, it's Exif header will be replaced by the given header.
     */
    @Override
    public void write(@NonNull byte[] buffer, int offset, int length) throws IOException {
        while ((mByteToSkip > 0 || mByteToCopy > 0 || mState != STATE_JPEG_DATA)
                && length > 0) {
            if (mByteToSkip > 0) {
                int byteToProcess = Math.min(length, mByteToSkip);
                length -= byteToProcess;
                mByteToSkip -= byteToProcess;
                offset += byteToProcess;
            }
            if (mByteToCopy > 0) {
                int byteToProcess = Math.min(length, mByteToCopy);
                out.write(buffer, offset, byteToProcess);
                length -= byteToProcess;
                mByteToCopy -= byteToProcess;
                offset += byteToProcess;
            }
            if (length == 0) {
                return;
            }
            switch (mState) {
                case STATE_SOI:
                    int byteRead = requestByteToBuffer(2, buffer, offset, length);
                    offset += byteRead;
                    length -= byteRead;
                    if (mBuffer.position() < 2) {
                        return;
                    }
                    mBuffer.rewind();
                    if (mBuffer.getShort() != JpegHeader.SOI) {
                        throw new IOException("Not a valid jpeg image, cannot write exif");
                    }
                    out.write(mBuffer.array(), 0, 2);
                    mState = STATE_FRAME_HEADER;
                    mBuffer.rewind();
                    ByteOrderedDataOutputStream dataOutputStream =
                            new ByteOrderedDataOutputStream(out, ByteOrder.BIG_ENDIAN);
                    dataOutputStream.writeShort(JpegHeader.APP1);
                    writeExifSegment(dataOutputStream);
                    break;
                case STATE_FRAME_HEADER:
                    // We ignore the APP1 segment and copy all other segments
                    // until SOF tag.
                    byteRead = requestByteToBuffer(4, buffer, offset, length);
                    offset += byteRead;
                    length -= byteRead;
                    // Check if this image data doesn't contain SOF.
                    if (mBuffer.position() == 2) {
                        short tag = mBuffer.getShort();
                        if (tag == JpegHeader.EOI) {
                            out.write(mBuffer.array(), 0, 2);
                            mBuffer.rewind();
                        }
                    }
                    if (mBuffer.position() < 4) {
                        return;
                    }
                    mBuffer.rewind();
                    short marker = mBuffer.getShort();
                    if (marker == JpegHeader.APP1) {
                        mByteToSkip = (mBuffer.getShort() & 0x0000ffff) - 2;
                        mState = STATE_JPEG_DATA;
                    } else if (!JpegHeader.isSofMarker(marker)) {
                        out.write(mBuffer.array(), 0, 4);
                        mByteToCopy = (mBuffer.getShort() & 0x0000ffff) - 2;
                    } else {
                        out.write(mBuffer.array(), 0, 4);
                        mState = STATE_JPEG_DATA;
                    }
                    mBuffer.rewind();
            }
        }
        if (length > 0) {
            out.write(buffer, offset, length);
        }
    }

    /**
     * Writes the one bytes out. The input data should be a valid JPEG format.
     * After writing, it's Exif header will be replaced by the given header.
     */
    @Override
    public void write(int oneByte) throws IOException {
        mSingleByteArray[0] = (byte) (0xff & oneByte);
        write(mSingleByteArray);
    }

    /**
     * Equivalent to calling write(buffer, 0, buffer.length).
     */
    @Override
    public void write(@NonNull byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    // Writes an Exif segment into the given output stream.
    private void writeExifSegment(@NonNull ByteOrderedDataOutputStream dataOutputStream)
            throws IOException {
        // The following variables are for calculating each IFD tag group size in bytes.
        int[] ifdOffsets = new int[EXIF_TAGS.length];
        int[] ifdDataSizes = new int[EXIF_TAGS.length];

        // Remove IFD pointer tags (we'll re-add it later.)
        for (ExifTag tag : EXIF_POINTER_TAGS) {
            for (int ifdIndex = 0; ifdIndex < EXIF_TAGS.length; ++ifdIndex) {
                mExifData.getAttributes(ifdIndex).remove(tag.name);
            }
        }

        // Add IFD pointer tags. The next offset of primary image TIFF IFD will have thumbnail IFD
        // offset when there is one or more tags in the thumbnail IFD.
        if (!mExifData.getAttributes(IFD_TYPE_EXIF).isEmpty()) {
            mExifData.getAttributes(IFD_TYPE_PRIMARY).put(EXIF_POINTER_TAGS[1].name,
                    ExifAttribute.createULong(0, mExifData.getByteOrder()));
        }
        if (!mExifData.getAttributes(IFD_TYPE_GPS).isEmpty()) {
            mExifData.getAttributes(IFD_TYPE_PRIMARY).put(EXIF_POINTER_TAGS[2].name,
                    ExifAttribute.createULong(0, mExifData.getByteOrder()));
        }
        if (!mExifData.getAttributes(IFD_TYPE_INTEROPERABILITY).isEmpty()) {
            mExifData.getAttributes(IFD_TYPE_EXIF).put(EXIF_POINTER_TAGS[3].name,
                    ExifAttribute.createULong(0, mExifData.getByteOrder()));
        }

        // Calculate IFD group data area sizes. IFD group data area is assigned to save the entry
        // value which has a bigger size than 4 bytes.
        for (int i = 0; i < EXIF_TAGS.length; ++i) {
            int sum = 0;
            for (Map.Entry<String, ExifAttribute> entry : mExifData.getAttributes(i).entrySet()) {
                final ExifAttribute exifAttribute = entry.getValue();
                final int size = exifAttribute.size();
                if (size > 4) {
                    sum += size;
                }
            }
            ifdDataSizes[i] += sum;
        }

        // Calculate IFD offsets.
        // 8 bytes are for TIFF headers: 2 bytes (byte order) + 2 bytes (identifier) + 4 bytes
        // (offset of IFDs)
        int position = 8;
        for (int ifdType = 0; ifdType < EXIF_TAGS.length; ++ifdType) {
            if (!mExifData.getAttributes(ifdType).isEmpty()) {
                ifdOffsets[ifdType] = position;
                position += 2 + mExifData.getAttributes(ifdType).size() * 12 + 4
                        + ifdDataSizes[ifdType];
            }
        }

        int totalSize = position;
        // Add 8 bytes for APP1 size and identifier data
        totalSize += 8;
        if (DEBUG) {
            for (int i = 0; i < EXIF_TAGS.length; ++i) {
                Logger.d(TAG, String.format(Locale.US, "index: %d, offsets: %d, tag count: %d, "
                                + "data sizes: %d, total size: %d", i, ifdOffsets[i],
                        mExifData.getAttributes(i).size(),
                        ifdDataSizes[i], totalSize));
            }
        }

        // Update IFD pointer tags with the calculated offsets.
        if (!mExifData.getAttributes(IFD_TYPE_EXIF).isEmpty()) {
            mExifData.getAttributes(IFD_TYPE_PRIMARY).put(EXIF_POINTER_TAGS[1].name,
                    ExifAttribute.createULong(ifdOffsets[IFD_TYPE_EXIF], mExifData.getByteOrder()));
        }
        if (!mExifData.getAttributes(IFD_TYPE_GPS).isEmpty()) {
            mExifData.getAttributes(IFD_TYPE_PRIMARY).put(EXIF_POINTER_TAGS[2].name,
                    ExifAttribute.createULong(ifdOffsets[IFD_TYPE_GPS], mExifData.getByteOrder()));
        }
        if (!mExifData.getAttributes(IFD_TYPE_INTEROPERABILITY).isEmpty()) {
            mExifData.getAttributes(IFD_TYPE_EXIF).put(EXIF_POINTER_TAGS[3].name,
                    ExifAttribute.createULong(
                            ifdOffsets[IFD_TYPE_INTEROPERABILITY], mExifData.getByteOrder()));
        }

        // Write JPEG specific data (APP1 size, APP1 identifier)
        dataOutputStream.writeUnsignedShort(totalSize);
        dataOutputStream.write(IDENTIFIER_EXIF_APP1);

        // Write TIFF Headers. See JEITA CP-3451C Section 4.5.2. Table 1.
        dataOutputStream.writeShort(mExifData.getByteOrder() == ByteOrder.BIG_ENDIAN
                ? BYTE_ALIGN_MM : BYTE_ALIGN_II);
        dataOutputStream.setByteOrder(mExifData.getByteOrder());
        dataOutputStream.writeUnsignedShort(START_CODE);
        dataOutputStream.writeUnsignedInt(IFD_OFFSET);

        // Write IFD groups. See JEITA CP-3451C Section 4.5.8. Figure 9.
        for (int ifdType = 0; ifdType < EXIF_TAGS.length; ++ifdType) {
            if (!mExifData.getAttributes(ifdType).isEmpty()) {
                // See JEITA CP-3451C Section 4.6.2: IFD structure.
                // Write entry count
                dataOutputStream.writeUnsignedShort(mExifData.getAttributes(ifdType).size());

                // Write entry info
                int dataOffset = ifdOffsets[ifdType] + 2 + mExifData.getAttributes(ifdType).size()
                        * 12 + 4;
                for (Map.Entry<String, ExifAttribute> entry : mExifData.getAttributes(
                        ifdType).entrySet()) {
                    // Convert tag name to tag number.
                    final ExifTag tag = sExifTagMapsForWriting.get(ifdType).get(entry.getKey());
                    final int tagNumber =
                            Preconditions.checkNotNull(tag,
                                    "Tag not supported: " + entry.getKey() + ". Tag needs to be "
                                            + "ported from ExifInterface to ExifData.").number;
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

                // Write the next offset. Since we aren't handling thumbnails, this is just 0.
                dataOutputStream.writeUnsignedInt(0);

                // Write values of data field exceeding 4 bytes after the next offset.
                for (Map.Entry<String, ExifAttribute> entry : mExifData.getAttributes(
                        ifdType).entrySet()) {
                    ExifAttribute attribute = entry.getValue();

                    if (attribute.bytes.length > 4) {
                        dataOutputStream.write(attribute.bytes, 0, attribute.bytes.length);
                    }
                }
            }
        }

        // Reset the byte order to big endian in order to write remaining parts of the JPEG file.
        dataOutputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
    }

    static final class JpegHeader {
        public static final short SOI =  (short) 0xFFD8;
        public static final short APP1 = (short) 0xFFE1;
        public static final short EOI = (short) 0xFFD9;

        /**
         *  SOF (start of frame). All value between SOF0 and SOF15 is SOF marker except for DHT,
         *  JPG, and DAC marker.
         */
        public static final short SOF0 = (short) 0xFFC0;
        public static final short SOF15 = (short) 0xFFCF;
        public static final short DHT = (short) 0xFFC4;
        public static final short JPG = (short) 0xFFC8;
        public static final short DAC = (short) 0xFFCC;

        public static boolean isSofMarker(short marker) {
            return marker >= SOF0 && marker <= SOF15 && marker != DHT && marker != JPG
                    && marker != DAC;
        }

        private JpegHeader() {}
    }
}
