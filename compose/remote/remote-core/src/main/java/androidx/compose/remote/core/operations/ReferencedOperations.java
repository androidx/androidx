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
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableProvider;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.loom.ExpansionContext;
import androidx.compose.remote.core.operations.loom.LoomManager;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A container of operations identified by an id */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ReferencedOperations extends Operation
        implements ComponentData, Container, VariableProvider, Serializable {
    private static final int OP_CODE = Operations.REFERENCED_OPERATIONS;
    private static final String CLASS_NAME = "ReferencedOperations";
    private int mId;
    @NonNull private final ArrayList<Operation> mList = new ArrayList<>();
    private byte[] mBodyBytes;

    public ReferencedOperations(int id) {
        mId = id;
    }

    /**
     * Set the body of the referenced operations as a byte array
     *
     * @param bytes the body of the referenced operations
     */
    public void setBody(byte @NonNull [] bytes) {
        mBodyBytes = bytes;
    }

    /**
     * Get the body of the referenced operations as a byte array
     *
     * @return the body of the referenced operations
     */
    public byte @NonNull [] getBody() {
        return mBodyBytes;
    }

    @Override
    public int getId() {
        return mId;
    }

    /**
     * Set the id of the referenced operations
     *
     * @param id the new id
     */
    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public void materialize(
            @NonNull ExpansionContext context,
            @NonNull ArrayList<Operation> result,
            @NonNull LoomManager loomManager) {
        if (mBodyBytes == null) {
            androidx.compose.remote.core.RemoteComposeBuffer buffer =
                    new androidx.compose.remote.core.RemoteComposeBuffer();
            // Add a header so inflateFromBuffer works
            buffer.addHeader(
                    new short[] {androidx.compose.remote.core.operations.Header.DOC_PROFILES},
                    new Object[] {
                        androidx.compose.remote.core.RcProfiles.PROFILE_EXPERIMENTAL
                                | androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
                    });
            for (Operation op : mList) {
                Operation.writeRecursive(op, buffer.getBuffer());
            }
            mBodyBytes = buffer.getBuffer().cloneBytes();
        }
        super.materialize(context, result, loomManager);
    }

    @Override
    public @NonNull ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.putObject(mId, this);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId);
    }

    /**
     * write the operation to the buffer
     *
     * @param buffer
     * @param id
     */
    public static void apply(@NonNull WireBuffer buffer, int id) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
    }

    /**
     * read the operation from the buffer
     *
     * @param buffer
     * @param operations
     */

    /**
     * read the operation from the buffer
     *
     * @param buffer
     * @param operations
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.declareId();
        operations.add(new ReferencedOperations(id));
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("id", mId).add("operations", mList);
    }

    /**
     * Documentation
     *
     * @param doc
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("A container of operations identified by an id")
                .field(INT, "id", "The id of the container");
    }

    @Override
    public String toString() {
        return CLASS_NAME + "[" + mId + "]";
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(indent).append(toString()).append("\n");
        for (Operation op : mList) {
            builder.append(op.deepToString(indent + "  ")).append("\n");
        }
        return builder.toString();
    }
}
