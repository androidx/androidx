/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.modifiers;

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.ActionOperation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Capture a host action information. This can be triggered on eg. a click. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HostActionMetadataOperation extends Operation
        implements ActionOperation, SerializableToString, Serializable {
    private static final int OP_CODE = Operations.HOST_METADATA_ACTION;

    int mActionId = -1;
    int mMetadataId = -1;

    public HostActionMetadataOperation(int id, int metadataId) {
        mActionId = id;
        mMetadataId = metadataId;
    }

    @NonNull
    @Override
    public String toString() {
        return "HostMetadataActionOperation(" + mActionId + ":" + mMetadataId + ")";
    }

    public int getActionId() {
        return mActionId;
    }

    /**
     * Returns the serialized name for this operation
     *
     * @return the serialized name
     */
    @NonNull
    public String serializedName() {
        return "HOST_METADATA_ACTION";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, serializedName() + " = " + mActionId + ", " + mMetadataId);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {}

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {}

    @Override
    public void runAction(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        String metadata = context.getText(mMetadataId);
        if (metadata == null) {
            metadata = "";
        }
        context.runAction(mActionId, metadata);
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param actionId the action id
     * @param metadataId the metadata id
     */
    public static void apply(@NonNull WireBuffer buffer, int actionId, int metadataId) {
        buffer.start(OP_CODE);
        buffer.writeInt(actionId);
        buffer.writeInt(metadataId);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int actionId = buffer.readInt();
        int metadataId = buffer.readInt();
        operations.add(new HostActionMetadataOperation(actionId, metadataId));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", OP_CODE, "HostAction")
                .description("Host action. This operation represents a host action")
                .field(INT, "ACTION_ID", "Host Action ID")
                .field(INT, "METADATA", "Host Action Text Metadata ID");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("HostActionOperation")
                .add("id", mActionId)
                .add("metadata", mMetadataId);
    }
}
