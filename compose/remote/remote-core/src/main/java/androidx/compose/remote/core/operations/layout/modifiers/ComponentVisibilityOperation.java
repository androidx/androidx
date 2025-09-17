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
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.DecoratorComponent;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Allows setting visibility on a component */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComponentVisibilityOperation extends Operation
        implements ModifierOperation, VariableSupport, DecoratorComponent {
    private static final int OP_CODE = Operations.MODIFIER_VISIBILITY;

    int mVisibilityId;
    int mVisibility = Component.Visibility.VISIBLE;
    private LayoutComponent mParent;

    public ComponentVisibilityOperation(int id) {
        mVisibilityId = id;
    }

    @NonNull
    @Override
    public String toString() {
        return "ComponentVisibilityOperation(" + mVisibilityId + ")";
    }

    /**
     * Returns the serialized name for this operation
     *
     * @return the serialized name
     */
    @NonNull
    public String serializedName() {
        return "COMPONENT_VISIBILITY";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, serializedName() + " = " + mVisibilityId);
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

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param valueId visibility value
     */
    public static void apply(@NonNull WireBuffer buffer, int valueId) {
        buffer.start(OP_CODE);
        buffer.writeInt(valueId);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int valueId = buffer.readInt();
        operations.add(new ComponentVisibilityOperation(valueId));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", OP_CODE, "ComponentVisibility")
                .description(
                        "This operation allows setting a component"
                                + "visibility from a provided value")
                .field(INT, "VALUE_ID", "Value ID representing the visibility");
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.listensTo(mVisibilityId, this);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        int visibility = context.getInteger(mVisibilityId);
        if (Component.Visibility.isVisible(visibility)) {
            mVisibility = Component.Visibility.VISIBLE;
        } else if (Component.Visibility.isGone(visibility)) {
            mVisibility = Component.Visibility.GONE;
        } else if (Component.Visibility.isInvisible(visibility)) {
            mVisibility = Component.Visibility.INVISIBLE;
        } else {
            mVisibility = Component.Visibility.GONE;
        }
        if (mParent != null) {
            mParent.setVisibility(mVisibility);
        }
    }

    public void setParent(@Nullable LayoutComponent parent) {
        mParent = parent;
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {}

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("ComponentVisibilityOperation")
                .add("visibilityId", mVisibilityId)
                .add("visibility", Component.Visibility.toString(mVisibility));
    }
}
