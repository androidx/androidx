/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT_ARRAY;
import static androidx.compose.remote.core.documentation.DocumentedOperation.SHORT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Operation to deal with bitmap data On getting an Image during a draw call the bitmap is
 * compressed and saved in playback the image is decompressed
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BitmapData extends Operation implements SerializableToString, Serializable {
    private static final int OP_CODE = Operations.DATA_BITMAP;
    private static final String CLASS_NAME = "BitmapData";
    public final int mImageId;
    int mImageWidth;
    int mImageHeight;
    short mType;
    short mEncoding;
    byte @NonNull [] mBitmap;

    /** The max size of width or height */
    public static final int MAX_IMAGE_DIMENSION = 8000;

    /** The data is encoded in the file (default) */
    public static final short ENCODING_INLINE = 0;

    /** The data is encoded in the url */
    public static final short ENCODING_URL = 1;

    /** The data is encoded as a reference to file */
    public static final short ENCODING_FILE = 2;

    /** allocates a new bitmap data with value = 0 */
    public static final short ENCODING_EMPTY = 3;

    /** The data is encoded as PNG_8888 (default) */
    public static final short TYPE_PNG_8888 = 0;

    /** The data is encoded as PNG */
    public static final short TYPE_PNG = 1;

    /** The data is encoded as RAW 8 bit */
    public static final short TYPE_RAW8 = 2;

    /** The data is encoded as RAW 8888 bit */
    public static final short TYPE_RAW8888 = 3;

    /** The data is encoded as PNG_8888 but decoded as ALPHA_8 */
    public static final short TYPE_PNG_ALPHA_8 = 4;

    /**
     * create a bitmap structure
     *
     * @param imageId the id to store the image
     * @param width the width of the image
     * @param height the height of the image
     * @param bitmap the data
     */
    public BitmapData(int imageId, int width, int height, byte @NonNull [] bitmap) {
        this.mImageId = imageId;
        this.mImageWidth = width;
        this.mImageHeight = height;
        this.mBitmap = bitmap;
    }

    /**
     * Update the bitmap data
     *
     * @param from the bitmap to copy
     */
    public void update(@NonNull BitmapData from) {
        this.mImageWidth = from.mImageWidth;
        this.mImageHeight = from.mImageHeight;
        this.mBitmap = from.mBitmap;
        this.mType = from.mType;
        this.mEncoding = from.mEncoding;
    }

    /**
     * The width of the image
     *
     * @return the width
     */
    public int getWidth() {
        return mImageWidth;
    }

    /**
     * The height of the image
     *
     * @return the height
     */
    public int getHeight() {
        return mImageHeight;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mImageId, mImageWidth, mImageHeight, mBitmap);
    }

    @NonNull
    @Override
    public String toString() {
        return "BITMAP DATA " + mImageId;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * The type of the image
     *
     * @return the type of the image
     */
    public int getType() {
        return mType;
    }

    /**
     * Add the image to the document
     *
     * @param buffer document to write to
     * @param imageId the id the image will be stored under
     * @param width the width of the image
     * @param height the height of the image
     * @param bitmap the data used to store/encode the image
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int imageId,
            int width,
            int height,
            byte @NonNull [] bitmap) {
        buffer.start(OP_CODE);
        buffer.writeInt(imageId);
        buffer.writeInt(width);
        buffer.writeInt(height);
        buffer.writeBuffer(bitmap);
    }

    /**
     * Add the image to the document (using the ehanced encoding)
     *
     * @param buffer document to write to
     * @param imageId the id the image will be stored under
     * @param type the type of image
     * @param width the width of the image
     * @param encoding the encoding
     * @param height the height of the image
     * @param bitmap the data used to store/encode the image
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int imageId,
            short type,
            short width,
            short encoding,
            short height,
            byte @NonNull [] bitmap) {
        buffer.start(OP_CODE);
        buffer.writeInt(imageId);
        int w = (((int) type) << 16) | width;
        int h = (((int) encoding) << 16) | height;
        buffer.writeInt(w);
        buffer.writeInt(h);
        buffer.writeBuffer(bitmap);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int imageId = buffer.readInt();
        int width = buffer.readInt();
        int height = buffer.readInt();
        int type;
        if (width > 0xffff) {
            type = width >> 16;
            width = width & 0xffff;
        } else {
            type = TYPE_PNG_8888;
        }

        int encoding;
        if (height > 0xffff) {
            encoding = height >> 16;
            height = height & 0xffff;
        } else {
            encoding = ENCODING_INLINE;
        }
        if (width < 1
                || height < 1
                || height > MAX_IMAGE_DIMENSION
                || width > MAX_IMAGE_DIMENSION) {
            throw new RuntimeException("Dimension of image is invalid " + width + "x" + height);
        }
        byte[] bitmap = buffer.readBuffer();
        BitmapData bitmapData = new BitmapData(imageId, width, height, bitmap);
        bitmapData.mType = (short) type;
        bitmapData.mEncoding = (short) encoding;
        operations.add(bitmapData);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Bitmap data")
                .field(DocumentedOperation.INT, "id", "id of bitmap data")
                .field(SHORT, "type", "width of the image")
                .field(SHORT, "width", "width of the image")
                .field(SHORT, "encoding", "height of the image")
                .field(INT, "width", "width of the image")
                .field(SHORT, "height", "height of the image")
                .field(INT_ARRAY, "values", "length", "Array of ints");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.putObject(mImageId, this);
        context.loadBitmap(mImageId, mEncoding, mType, mImageWidth, mImageHeight, mBitmap);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                CLASS_NAME + " id " + mImageId + " (" + mImageWidth + "x" + mImageHeight + ")");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("imageId", mImageId)
                .add("imageWidth", mImageWidth)
                .add("imageHeight", mImageHeight)
                .add("imageType", getImageTypeString(mType))
                .add("encoding", getEncodingString(mEncoding));
    }

    private String getEncodingString(short encoding) {
        switch (encoding) {
            case ENCODING_INLINE:
                return "ENCODING_INLINE";
            case ENCODING_URL:
                return "ENCODING_URL";
            case ENCODING_FILE:
                return "ENCODING_FILE";
            default:
                return "ENCODING_INVALID";
        }
    }

    private String getImageTypeString(short type) {
        switch (type) {
            case TYPE_PNG_8888:
                return "TYPE_PNG_8888";
            case TYPE_PNG:
                return "TYPE_PNG";
            case TYPE_RAW8:
                return "TYPE_RAW8";
            case TYPE_RAW8888:
                return "TYPE_RAW8888";
            case TYPE_PNG_ALPHA_8:
                return "TYPE_PNG_ALPHA_8";
            default:
                return "TYPE_INVALID";
        }
    }
}
