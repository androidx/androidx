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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Set the min / max dimension constraints on a component */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DimensionConstraintsModifierOperation extends DimensionInModifierOperation {
    private static final int OP_CODE = Operations.MODIFIER_DIMENSION_CONSTRAINTS;
    public static final String CLASS_NAME = "DimensionConstraintsModifierOperation";

    public static final int HORIZONTAL_CONSTRAINTS = 0;
    public static final int VERTICAL_CONSTRAINTS = 1;
    public static final int REQUIRED_HORIZONTAL_CONSTRAINTS = 2;
    public static final int REQUIRED_VERTICAL_CONSTRAINTS = 3;

    private final int mType;

    public DimensionConstraintsModifierOperation(int type, float min, float max) {
        super(OP_CODE, min, max);
        mType = type;
    }

    @Override
    public float applyWidthConstraint(float width) {
        if (mType == REQUIRED_HORIZONTAL_CONSTRAINTS) {
            return Math.max(mValue1, Math.min(width, mValue2));
        }
        return width;
    }

    @Override
    public float applyHeightConstraint(float height) {
        if (mType == REQUIRED_VERTICAL_CONSTRAINTS) {
            return Math.max(mValue1, Math.min(height, mValue2));
        }
        return height;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mType, getMin(), getMax());
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int type = buffer.readByte();
        float v1 = buffer.readFloat();
        float v2 = buffer.readFloat();
        operations.add(new DimensionConstraintsModifierOperation(type, v1, v2));
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
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .description("Add additional constraints to the dimensions")
                .field(DocumentedOperation.BYTE, "type",
                        "The type of constraint (Horizontal, Vertical, Required Horizontal, "
                                + "Required Vertical)")
                .field(DocumentedOperation.FLOAT, "min", "The minimum dimension, -1 if not applied")
                .field(DocumentedOperation.FLOAT, "max",
                        "The maximum dimension, -1 if not applied");
    }

    /**
     * Writes out the DimensionConstraintsModifier to the buffer
     *
     * @param buffer buffer to write to
     * @param min    minimum dimension
     * @param max    maximum dimension
     * @param type   type of constraint
     */
    public static void apply(@NonNull WireBuffer buffer, int type, float min, float max) {
        buffer.start(OP_CODE);
        buffer.writeByte(type);
        buffer.writeFloat(min);
        buffer.writeFloat(max);
    }

    public int getType() {
        return mType;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        String typeStr = typeToString();
        serializer.append(indent,
                "DIMENSION_CONSTRAINTS [" + typeStr + "] = [" + getMin() + ", " + getMax() + "]");
    }

    @NonNull String typeToString() {
        String typeStr = "UNKNOWN";
        switch (mType) {
            case HORIZONTAL_CONSTRAINTS:
                typeStr = "HORIZONTAL";
                break;
            case VERTICAL_CONSTRAINTS:
                typeStr = "VERTICAL";
                break;
            case REQUIRED_HORIZONTAL_CONSTRAINTS:
                typeStr = "REQUIRED_HORIZONTAL";
                break;
            case REQUIRED_VERTICAL_CONSTRAINTS:
                typeStr = "REQUIRED_VERTICAL";
                break;
        }
        return typeStr;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType(CLASS_NAME)
                .add("constraints", typeToString())
                .add("min", mV1, mValue1)
                .add("max", mV2, mValue2);
    }
}
