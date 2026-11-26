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
 * Draw a cached 3D mesh by id with the current paint color and matrix state.
 *
 * <p>{@code mode} (see {@link Paint3DContext#drawMesh3D(int, int)}): bit 0 shading (smooth),
 * bits 1–3 engine hint, bits 4–7 reserved, bits 8–11 wireframe style, bits 12–16 capability flags
 * ({@link Paint3DContext#MODE_TRANSPARENT}, {@link Paint3DContext#MODE_GPU}, {@link
 * Paint3DContext#MODE_PBR}, {@link Paint3DContext#MODE_DOUBLE_SIDED}, {@link
 * Paint3DContext#MODE_FRONT_CW}), bits 17–29 reserved, bit 30 ({@link
 * Paint3DContext#MODE_ADVANCED}) the advanced form of a later format version, bit 31 reserved.
 * {@link Paint3DContext#MODE_FRONT_CW} makes clockwise triangles front-facing, like {@code
 * glFrontFace(GL_CW)}; infinite planes ignore it. A reader refuses a mode with bit 30 set.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawMesh3D extends PaintOperation {
    private static final int OP_CODE = Operations.DRAW_MESH_3D;
    private static final String CLASS_NAME = "DrawMesh3D";

    private final int mMeshId;
    private final int mMode;

    public DrawMesh3D(int meshId, int mode) {
        mMeshId = meshId;
        mMode = mode;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mMeshId, mMode);
    }

    /**
     * Write a {@link DrawMesh3D} to a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param meshId id of the mesh to draw
     * @param mode render mode
     */
    public static void apply(@NonNull WireBuffer buffer, int meshId, int mode) {
        buffer.start(OP_CODE);
        buffer.writeInt(meshId);
        buffer.writeInt(mode);
    }

    /**
     * Read a {@link DrawMesh3D} from a {@link WireBuffer}.
     * @param buffer wire buffer
     * @param operations list to add the operation to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int meshId = buffer.readInt();
        int mode = buffer.readInt();
        Advanced3D.check(mode, CLASS_NAME);
        operations.add(new DrawMesh3D(meshId, mode));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (context instanceof Paint3DContext) {
            ((Paint3DContext) context).drawMesh3D(mMeshId, mMode);
        }
    }

    /**
     * Documentation for {@link DrawMesh3D}.
     * @param doc documentation builder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("3D Operations", OP_CODE, CLASS_NAME)
                .description("Draw a cached mesh with current paint color and matrices.")
                .field(DocumentedOperation.INT, "meshId", "Mesh id from DefineMesh3D")
                .field(
                        DocumentedOperation.INT,
                        "mode",
                        "Render mode bits: 0 shading (0=flat, 1=smooth); 1-3 engine hint"
                                + " (0=software, 1=canvas, 2=drawMesh, 3=GL, 4=drawMesh z-buffer,"
                                + " 5=canvas z-buffer; unknown engines draw as software);"
                                + " 4-7 reserved (0); 8-11 wireframe (8=wireframe, 9-11 edges"
                                + " 0-2); 12-16 flags (12=transparent, 13=gpu, 14=pbr,"
                                + " 15=double-sided, 16=front-cw: clockwise triangles are"
                                + " front-facing, like glFrontFace(GL_CW); infinite planes ignore"
                                + " it); 17-29 reserved, ignored; 30 advanced form of a later"
                                + " format version, readers refuse it; 31 reserved");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("meshId", mMeshId).add("mode", mMode);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(meshId=" + mMeshId + ", mode=" + mMode + ")";
    }
}
