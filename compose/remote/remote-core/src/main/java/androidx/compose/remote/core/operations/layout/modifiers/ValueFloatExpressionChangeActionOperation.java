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
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.ActionOperation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Apply a value change on an integer variable. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ValueFloatExpressionChangeActionOperation extends Operation
        implements ActionOperation {
    private static final int OP_CODE = Operations.VALUE_FLOAT_EXPRESSION_CHANGE_ACTION;

    int mTargetValueId = -1;
    int mValueExpressionId = -1;

    public ValueFloatExpressionChangeActionOperation(int id, int valueId) {
        mTargetValueId = id;
        mValueExpressionId = valueId;
    }

    @NonNull
    @Override
    public String toString() {
        return "ValueFloatExpressionChangeActionOperation(" + mTargetValueId + ")";
    }

    /**
     * The name of the operation used during serialization
     *
     * @return the operation serialized name
     */
    @NonNull
    public String serializedName() {
        return "VALUE_FLOAT_EXPRESSION_CHANGE";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent, serializedName() + " = " + mTargetValueId + " -> " + mValueExpressionId);
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
        document.evaluateFloatExpression(mValueExpressionId, mTargetValueId, context);
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param valueId the value id
     * @param value the value to set
     */
    public static void apply(@NonNull WireBuffer buffer, int valueId, int value) {
        buffer.start(OP_CODE);
        buffer.writeInt(valueId);
        buffer.writeInt(value);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int valueId = buffer.readInt();
        int value = buffer.readInt();
        operations.add(new ValueFloatExpressionChangeActionOperation(valueId, value));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", OP_CODE, "ValueIntegerExpressionChangeActionOperation")
                .description(
                        "ValueIntegerExpressionChange action. "
                                + " This operation represents a value change for the given id")
                .field(INT, "TARGET_VALUE_ID", "Value ID")
                .field(INT, "VALUE_ID", "id of the value to be assigned to the target");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER, SerializeTags.ACTION)
                .addType("ValueFloatExpressionChangeActionOperation")
                .add("targetValueId", mTargetValueId)
                .add("valueExpressionId", mValueExpressionId);
    }
}
