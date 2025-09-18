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
package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Represents a touch down modifier + actions */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TouchDownModifierOperation extends ListActionsOperation implements TouchHandler {

    private static final int OP_CODE = Operations.MODIFIER_TOUCH_DOWN;

    public TouchDownModifierOperation() {
        super("TOUCH_DOWN_MODIFIER");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @Override
    public String toString() {
        return "TouchDownModifier";
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        if (context.getDocument() == null) {
            return;
        }
        RootLayoutComponent root = context.getDocument().getRootLayoutComponent();
        if (root != null) {
            root.setHasTouchListeners(true);
        }
        super.apply(context);
    }

    @Override
    public void onTouchDown(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        if (applyActions(context, document, component, x, y, false)) {
            document.appliedTouchOperation(component);
        }
    }

    @Override
    public void onTouchUp(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y,
            float dx,
            float dy) {
        // nothing
    }

    @Override
    public void onTouchCancel(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        // nothing
    }

    @Override
    public void onTouchDrag(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        // nothing
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "TouchModifier";
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     */
    public static void apply(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
    }

    /**
     * Read the operation from the buffer
     *
     * @param buffer a WireBuffer
     * @param operations the list of operations we read so far
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        operations.add(new TouchDownModifierOperation());
    }

    /**
     * Add documentation for this operation
     *
     * @param doc a DocumentationBuilder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, name())
                .description(
                        "Touch down modifier. This operation contains"
                                + " a list of action executed on Touch down");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer.addType("TouchDownModifierOperation");
    }
}
