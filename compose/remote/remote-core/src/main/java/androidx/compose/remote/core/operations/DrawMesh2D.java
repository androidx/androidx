/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Draws a 2D vertex mesh previously defined by {@link AddMesh2D}.
 *
 * <p>The image is named where it is used rather than bound by a separate operation, because 2D
 * cannot afford an opcode for texture binding the way 3D can. {@code imageId} refers to a bitmap
 * already in the document - the same ids {@code drawBitmap} uses - so there is no new loading
 * mechanism.
 *
 * <p>The mesh is positioned by the ordinary 2D canvas matrix, so this composes with {@code
 * save}/{@code restore}/{@code translate} exactly as {@code drawRect} does.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawMesh2D extends PaintOperation implements Serializable {
    private static final int OP_CODE = Operations.DRAW_MESH_2D;
    private static final String CLASS_NAME = "DrawMesh2D";

    /**
     * Interpolate the per-vertex colours across each triangle and ignore any texture.
     *
     * <p>This is Android's behaviour when {@code colors} is supplied and {@code texs} is not.
     */
    public static final int BLEND_COLORS_ONLY = 0;

    /**
     * Multiply the sampled texel by the interpolated vertex colour.
     *
     * <p>This is Android's documented behaviour when both {@code texs} and {@code colors} are
     * present, and exactly what the Skia path does with {@code kModulate}, so the two platforms
     * agree for free.
     */
    public static final int BLEND_MODULATE = 1;

    /** {@code imageId} value meaning the mesh is untextured. */
    public static final int NO_IMAGE = 0;

    private final int mMeshId;
    private final int mBlend;
    private final int mImageId;

    public DrawMesh2D(int meshId, int blend, int imageId) {
        mMeshId = meshId;
        mBlend = blend;
        mImageId = imageId;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mMeshId, mBlend, mImageId);
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.drawMesh(mMeshId, mBlend, mImageId);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + " [" + mMeshId + "] blend=" + mBlend + " image=" + mImageId;
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
     * Draw a previously defined 2D mesh.
     *
     * @param buffer the buffer to add to
     * @param meshId the mesh to draw
     * @param blend how vertex colour and texel combine
     * @param imageId the bitmap to sample, {@link #NO_IMAGE} for untextured
     */
    public static void apply(@NonNull WireBuffer buffer, int meshId, int blend, int imageId) {
        buffer.start(OP_CODE);
        buffer.writeInt(meshId);
        buffer.writeInt(blend);
        buffer.writeInt(imageId);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int meshId = buffer.readId();
        int blend = buffer.readInt();
        int imageId = buffer.readInt();
        operations.add(new DrawMesh2D(meshId, blend, imageId));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Canvas Operations", OP_CODE, CLASS_NAME)
                .addedVersion(7)
                .experimental(true)
                .description("Draw a 2D vertex mesh defined by AddMesh2D")
                .field(INT, "meshId", "The mesh to draw")
                .field(INT, "blend", "How vertex colour and texel combine")
                .possibleValues("BLEND_COLORS_ONLY", BLEND_COLORS_ONLY)
                .possibleValues("BLEND_MODULATE", BLEND_MODULATE)
                .field(INT, "imageId", "The bitmap to sample, 0 = untextured");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("meshId", mMeshId)
                .add("blend", mBlend)
                .add("imageId", mImageId);
    }
}
