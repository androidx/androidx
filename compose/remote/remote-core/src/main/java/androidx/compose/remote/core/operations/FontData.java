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

import static androidx.compose.remote.core.documentation.DocumentedOperation.BYTE_ARRAY;

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

/** Operation to deal with transfer raw Font data */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FontData extends Operation implements SerializableToString, Serializable {
    private static final int OP_CODE = Operations.DATA_FONT;
    private static final String CLASS_NAME = "FontData";
    public final int mFontId;
    byte @NonNull [] mFontData;

    /**
     * create a Font structure
     *
     * @param fontId the id to store the image
     * @param type the type of the font (unused for now)
     * @param fontData the data
     */
    public FontData(int fontId, int type, byte @NonNull [] fontData) {
        this.mFontId = fontId;
        this.mFontData = fontData;
    }

    /**
     * Update the FontData
     *
     * @param from the fontData to copy
     */
    public void update(@NonNull FontData from) {
        this.mFontData = from.mFontData;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mFontId, 0, mFontData);
    }

    @NonNull
    @Override
    public String toString() {
        return "FONT DATA " + mFontId;
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
     * Add the image to the document
     *
     * @param buffer document to write to
     * @param fontId the id the font will be stored under
     * @param type the type of the font
     * @param fontData the data used to store/encode the image
     */
    public static void apply(
            @NonNull WireBuffer buffer, int fontId, int type, byte @NonNull [] fontData) {
        buffer.start(OP_CODE);
        buffer.writeInt(fontId);
        buffer.writeInt(type);
        buffer.writeBuffer(fontData);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int imageId = buffer.readInt();
        int type = buffer.readInt();
        byte[] fontData = buffer.readBuffer();
        FontData bitmapData = new FontData(imageId, type, fontData);
        operations.add(bitmapData);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Font data")
                .field(DocumentedOperation.INT, "id", "id of Font data")
                .field(BYTE_ARRAY, "values", "length", "Array of bytes");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.loadFont(mFontId, mFontData);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent, CLASS_NAME + " id " + mFontId + " byte[" + mFontData.length + "]");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("imageId", mFontId);
    }
}
