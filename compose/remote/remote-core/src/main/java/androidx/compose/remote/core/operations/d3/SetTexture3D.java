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
package androidx.compose.remote.core.operations.d3;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Set the active texture for subsequent {@code drawMesh3D} calls to a previously uploaded
 * bitmap (by image id), sampled through each mesh's UV coordinates. {@code bitmapId == 0}
 * clears the texture. State persists like the camera/lights until changed.
 *
 * <p>Bit 30 of {@code bitmapId} marks the advanced form of a later format version; a reader of
 * this version refuses it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SetTexture3D extends PaintOperation {
    private static final int OP_CODE = Operations.SET_TEXTURE_3D;
    private static final String CLASS_NAME = "SetTexture3D";

    private final int mBitmapId;

    public SetTexture3D(int bitmapId) {
        mBitmapId = bitmapId;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mBitmapId);
    }

    /**
     * Write a {@link SetTexture3D} to a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param bitmapId image id
     */
    public static void apply(@NonNull WireBuffer buffer, int bitmapId) {
        buffer.start(OP_CODE);
        buffer.writeInt(bitmapId);
    }

    /**
     * Read a {@link SetTexture3D} from a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param operations list to add the operation to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int bitmapId = buffer.readInt();
        Advanced3D.check(bitmapId, CLASS_NAME);
        operations.add(new SetTexture3D(bitmapId));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (context instanceof Paint3DContext) {
            ((Paint3DContext) context).setTexture3D(mBitmapId);
        }
    }

    /**
     * Documentation for {@link SetTexture3D}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description("Set the texture bitmap (by image id) for subsequent 3D mesh draws.")
                .field(
                        DocumentedOperation.INT,
                        "bitmapId",
                        "Image id, or 0 to clear; bit 30: advanced form of a later format"
                                + " version, readers refuse it");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("bitmapId", mBitmapId);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(bitmapId=" + mBitmapId + ")";
    }
}
