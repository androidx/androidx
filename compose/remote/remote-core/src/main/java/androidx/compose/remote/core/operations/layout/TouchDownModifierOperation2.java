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
package androidx.compose.remote.core.operations.layout;

// This class duplicates parts of TouchDownModifierOperation until that class is removed.
// CPD-OFF

import static androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NOT_CONSUMED;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.input.MetadataUtil;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Touch down modifier + actions dispatched through the unified pointer input router.
 *
 * <p>Same opcode and (empty) wire format as {@link TouchDownModifierOperation}. Runs its actions
 * on every {@code DOWN} that reaches the hosting component, in the <em>main</em> pass. Like the
 * ripple modifier it is an observer: it does not consume the down and does not care whether a
 * click modifier on the same component or a clickable child already claimed it, so {@code
 * onTouchDown} keeps working next to {@code onClick} and on a parent of a clickable child, as it
 * did with the legacy touch path.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TouchDownModifierOperation2 extends ListActionsOperation implements EventHandler {

    private static final int OP_CODE = Operations.MODIFIER_TOUCH_DOWN;

    private @Nullable Component mComponent;

    public TouchDownModifierOperation2() {
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

    /**
     * Returns whether this handler matches the given event type.
     *
     * @param eventType event type identifier
     * @return true if event is a pointer event
     */
    @Override
    public boolean matchesEvent(int eventType) {
        return eventType == EVENT_TYPE_POINTER;
    }

    /**
     * Runs the actions on pointer down. Never consumes.
     *
     * @param context current remote context
     * @param document document containing this component
     * @param event pointer event being dispatched
     * @return always {@link
     *     androidx.compose.remote.core.input.PointerInputRouter#POINTER_STATUS_NOT_CONSUMED}
     */
    @Override
    public int onEvent(
            @NonNull RemoteContext context, @NonNull CoreDocument document, @NonNull Event event) {
        int metadata = event.getMetadata();
        if (MetadataUtil.unpackPass(metadata) != POINTER_PASS_MAIN
                || MetadataUtil.unpackSubtype(metadata) != POINTER_SUBTYPE_DOWN) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        float[] data = event.getData();
        Component component = mComponent;
        if (data == null || data.length < 3 || component == null || !component.isVisible()) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        runActions(context, document);
        return POINTER_STATUS_NOT_CONSUMED;
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {
        super.layout(context, component, width, height);
        mComponent = component;
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
     * Write the operation to the buffer. The wire format is identical to {@link
     * TouchDownModifierOperation}.
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
        operations.add(new TouchDownModifierOperation2());
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
                                + " a list of action operations executed on touch down");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer.addType("TouchDownModifierOperation");
    }
}
// CPD-ON
