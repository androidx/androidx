/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Draw to a bitmap. This command redirects drawing to a bitmap. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawToBitmap extends PaintOperation implements Serializable {
    private static final int OP_CODE = Operations.DRAW_TO_BITMAP;
    private static final String CLASS_NAME = "DrawToBitmap";
    private final int mBitmapId;
    private final int mMode;
    private final int mColor;

    public static final int MODE_NO_INITIALIZE = 1;

    public DrawToBitmap(int bitmapId, int mode, int color) {
        mBitmapId = bitmapId;
        mMode = mode;
        mColor = color;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mBitmapId, mMode, mColor);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int bitmapId = buffer.readInt();
        int mode = buffer.readInt();
        int color = buffer.readInt();
        DrawToBitmap op = new DrawToBitmap(bitmapId, mode, color);
        operations.add(op);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawToBitmap";
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
     * add a matrix restore operation to the buffer
     *
     * @param buffer the buffer to add to
     * @param bitmapId the id of the bitmap to draw to
     * @param mode
     * @param color
     */
    public static void apply(@NonNull WireBuffer buffer, int bitmapId, int mode, int color) {
        buffer.start(OP_CODE);
        buffer.writeInt(bitmapId);
        buffer.writeInt(mode);
        buffer.writeInt(color);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Canvas Operations", OP_CODE, CLASS_NAME)
                .description("Draw to a bitmap")
                .field(
                        DocumentedOperation.INT,
                        "bitmapId",
                        "The bitmap to draw to or 0 to draw to the canvas")
                .field(DocumentedOperation.INT, "mode", "Flags to support configuration of bitmap")
                .field(DocumentedOperation.INT, "color", "set the initial of the bitmap");
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.drawToBitmap(getId(mBitmapId, context), mMode, mColor);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("bitmapId", mBitmapId);
    }
}
