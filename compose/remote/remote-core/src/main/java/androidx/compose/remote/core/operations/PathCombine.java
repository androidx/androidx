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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Operation to perform Constructive area geometry operations, combining two Paths */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PathCombine extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.PATH_COMBINE;
    private static final String CLASS_NAME = "PathCombine";
    public int mOutId;
    public int mPathId1;
    public int mPathId2;
    private byte mOperation;

    /** Subtract the second path from the first path. */
    public static final byte OP_DIFFERENCE = 0;

    /** Intersect the second path with the first path. */
    public static final byte OP_INTERSECT = 1;

    /** Subtract the first path from the second path. */
    public static final byte OP_REVERSE_DIFFERENCE = 2;

    /** Union (inclusive-or) the two paths. */
    public static final byte OP_UNION = 3;

    /** Exclusive-or the two paths. */
    public static final byte OP_XOR = 4;

    public PathCombine(int outId, int pathId1, int pathId2, byte operation) {
        this.mOutId = outId;
        this.mPathId1 = pathId1;
        this.mPathId2 = pathId2;
        this.mOperation = operation;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {}

    @Override
    public void registerListening(@NonNull RemoteContext context) {}

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mOutId, mPathId1, mPathId2, mOperation);
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME
                + "["
                + mOutId
                + "] = ["
                + mPathId1
                + " ] + [ "
                + mPathId2
                + "], "
                + mOperation;
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
     * Writes out the operation to the buffer
     *
     * @param buffer buffer to write to
     * @param outId id of the path
     * @param pathId1 source path 1
     * @param pathId2 source path 2
     * @param op the operation to perform
     */
    public static void apply(
            @NonNull WireBuffer buffer, int outId, int pathId1, int pathId2, byte op) {
        buffer.start(OP_CODE);
        buffer.writeInt(outId);
        buffer.writeInt(pathId1);
        buffer.writeInt(pathId2);
        buffer.writeByte(op);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int outId1 = buffer.readInt();
        int pathId1 = buffer.readInt();
        int pathId2 = buffer.readInt();
        byte op = (byte) buffer.readByte();
        operations.add(new PathCombine(outId1, pathId1, pathId2, op));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Merge two string into one")
                .field(INT, "srcPathId1", "id of the path")
                .field(INT, "srcPathId1", "x Shift of the path")
                .field(DocumentedOperation.BYTE, "operation", "the operation");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.combinePath(mOutId, mPathId1, mPathId2, mOperation);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("outId", mOutId)
                .add("pathId1", mPathId1)
                .add("pathId2", mPathId2)
                .add("operation", mOperation);
    }
}
