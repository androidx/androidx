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
package androidx.compose.remote.core.operations.layout.modifiers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Set an optional priority on a component within a collapsible layout */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CollapsiblePriorityModifierOperation extends Operation
        implements ModifierOperation, Serializable {
    private static final int OP_CODE = Operations.MODIFIER_COLLAPSIBLE_PRIORITY;
    public static final String CLASS_NAME = "CollapsiblePriorityModifierOperation";

    private float mPriority;
    private int mOrientation;

    public CollapsiblePriorityModifierOperation(int orientation, float priority) {
        mOrientation = orientation;
        mPriority = priority;
    }

    public float getPriority() {
        return mPriority;
    }

    public int getOrientation() {
        return mOrientation;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mOrientation, mPriority);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // nothing
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return "";
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int orientation = buffer.readInt();
        float priority = buffer.readFloat();
        operations.add(new CollapsiblePriorityModifierOperation(orientation, priority));
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
        doc.operation("Layout Operations", OP_CODE, "CollapsiblePriorityModifier")
                .description("Add additional priority to children of Collapsible layouts")
                .field(DocumentedOperation.INT, "orientation", "Horizontal(0) or Vertical (1)")
                .field(DocumentedOperation.FLOAT, "priority", "The associated priority");
    }

    /**
     * Writes out the CollapsiblePriorityModifier to the buffer
     *
     * @param buffer buffer to write to
     * @param priority priority value
     * @param orientation orientation (HORIZONTAL or VERTICAL)
     */
    public static void apply(@NonNull WireBuffer buffer, int orientation, float priority) {
        buffer.start(OP_CODE);
        buffer.writeInt(orientation);
        buffer.writeFloat(priority);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType(name())
                .add("orientation", mOrientation)
                .add("priority", mPriority);
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "PRIORITY = [" + getPriority() + "] (" + mOrientation + ")");
    }
}
