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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.BYTE_ARRAY;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.VariableProvider;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * An opaque binary blob stored in the document under an id.
 *
 * <p>Wire format: {@code id, type, flags, length, data[length]}. {@code type} says what the bytes
 * are ({@link #TYPE_RAW}, {@link #TYPE_RC_DOC}); {@code flags} is reserved for the type and is
 * {@code 0} today. The blob is registered in the context with {@link RemoteContext#putObject} so
 * other operations can find it by id: an {@code rcDoc} whose url id refers to a {@link
 * #TYPE_RC_DOC} blob loads the embedded document from these bytes instead of asking the host's
 * {@code RcResolver}, which lets a parent document carry its children in its own byte array.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BinaryData extends Operation
        implements VariableSupport, VariableProvider, SerializableToString, Serializable {
    private static final int OP_CODE = Operations.BINARY_DATA;
    private static final String CLASS_NAME = "BinaryData";

    /** Uninterpreted bytes. */
    public static final int TYPE_RAW = 0;

    /** A complete RemoteCompose document (header included), playable on its own. */
    public static final int TYPE_RC_DOC = 1;

    private int mId;
    private final int mType;
    private final int mFlags;
    private final byte @NonNull [] mData;

    /**
     * Create a binary blob.
     *
     * @param id the id the blob is registered under
     * @param type what the bytes are, {@link #TYPE_RAW} or {@link #TYPE_RC_DOC}
     * @param flags type-specific flags, {@code 0} for now
     * @param data the bytes
     */
    public BinaryData(int id, int type, int flags, byte @NonNull [] data) {
        mId = id;
        mType = type;
        mFlags = flags;
        mData = data;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    /** Returns the type of the blob ({@link #TYPE_RAW}, {@link #TYPE_RC_DOC}). */
    public int getType() {
        return mType;
    }

    /** Returns the type-specific flags. */
    public int getFlags() {
        return mFlags;
    }

    /** Returns the bytes of the blob. */
    public byte @NonNull [] getData() {
        return mData;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {}

    @Override
    public void registerListening(@NonNull RemoteContext context) {}

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.putObject(mId, this);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mType, mFlags, mData);
    }

    /**
     * Write a {@link BinaryData} operation to the buffer.
     *
     * @param buffer the WireBuffer to write to
     * @param id the id the blob is registered under
     * @param type what the bytes are, {@link #TYPE_RAW} or {@link #TYPE_RC_DOC}
     * @param flags type-specific flags, {@code 0} for now
     * @param data the bytes (at most {@link Limits#MAX_BINARY_DATA})
     */
    public static void apply(
            @NonNull WireBuffer buffer, int id, int type, int flags, byte @NonNull [] data) {
        if (data.length > Limits.MAX_BINARY_DATA) {
            throw new IllegalArgumentException(
                    "binary data too large: " + data.length + " > " + Limits.MAX_BINARY_DATA);
        }
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(type);
        buffer.writeInt(flags);
        buffer.writeBuffer(data);
    }

    /**
     * Read this operation and add it to the list of operations.
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readId();
        int type = buffer.readInt();
        int flags = buffer.readInt();
        byte[] data = buffer.readBuffer(Limits.MAX_BINARY_DATA);
        operations.add(new BinaryData(id, type, flags, data));
    }

    /**
     * Populate the documentation with a description of this operation.
     *
     * @param doc the DocumentationBuilder to append to
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description(
                        "An opaque binary blob stored under an id. Type 1 (RC_DOC) is a complete"
                                + " RemoteCompose document that rcDoc can embed by id.")
                .field(INT, "id", "The id the blob is registered under")
                .field(INT, "type", "What the bytes are (0=RAW, 1=RC_DOC)")
                .field(INT, "flags", "Type-specific flags, 0 for now")
                .field(INT, "length", "The number of bytes")
                .field(BYTE_ARRAY, "data", "The bytes");
    }

    @NonNull
    @Override
    public String toString() {
        return "BinaryData(id=" + mId + ", type=" + mType + ", flags=" + mFlags + ", length="
                + mData.length + ")";
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
                "BINARY_DATA id=" + mId + " type=" + mType + " flags=" + mFlags + " length="
                        + mData.length);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mId)
                .add("type", mType)
                .add("flags", mFlags)
                .add("length", mData.length);
    }
}
