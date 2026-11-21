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

// This class duplicates parts of TouchCancelModifierOperation until that class is removed.
// CPD-OFF

import static androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_FINAL;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NOT_CONSUMED;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_CANCEL;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP;

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

import java.util.List;

/**
 * Touch cancel modifier + actions dispatched through the unified pointer input router.
 *
 * <p>Same opcode and (empty) wire format as {@link TouchCancelModifierOperation}. Runs its actions
 * once per press when the gesture is cancelled, which happens when:
 *
 * <ul>
 *   <li>the platform cancels the stream ({@code CANCEL}), or
 *   <li>a {@code MOVE} is consumed by another handler — typically a parent scroll container
 *       taking over after the touch slop. Like {@link ClickModifierOperation2}, this is checked in
 *       the main pass (consumed by a descendant, or by an ancestor in the initial pass) and in the
 *       final pass (consumed by an ancestor in its own main pass, which runs after ours).
 * </ul>
 *
 * <p>It is an observer like the other touch modifiers: it never consumes and it tracks the press
 * from the {@code DOWN} regardless of who consumed it. Together with {@link
 * TouchUpModifierOperation2}, which skips its {@code UP} after such a cancellation, a press ends
 * with exactly one of up or cancel.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TouchCancelModifierOperation2 extends ListActionsOperation implements EventHandler {

    private static final int OP_CODE = Operations.MODIFIER_TOUCH_CANCEL;
    private static final int NO_POINTER = -1;

    private int mPointerId = NO_POINTER;

    public TouchCancelModifierOperation2() {
        super("TOUCH_CANCEL_MODIFIER");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @Override
    public String toString() {
        return "TouchCancelModifier";
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
     * Tracks the press and runs the actions when it is cancelled. Never consumes.
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
        int pass = MetadataUtil.unpackPass(metadata);
        int subtype = MetadataUtil.unpackSubtype(metadata);
        boolean consumed = MetadataUtil.unpackConsumed(metadata) == POINTER_STATUS_CONSUMED;
        float[] data = event.getData();
        if (data == null || data.length < 3) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        int pointerId = (int) data[0];

        if (pass == POINTER_PASS_FINAL) {
            // Cancel by position consumption: an ancestor took over the gesture in its main pass.
            if (subtype == POINTER_SUBTYPE_MOVE && consumed && isPressed(pointerId)) {
                cancel(context, document);
            }
            return POINTER_STATUS_NOT_CONSUMED;
        }
        if (pass != POINTER_PASS_MAIN) {
            return POINTER_STATUS_NOT_CONSUMED;
        }

        switch (subtype) {
            case POINTER_SUBTYPE_DOWN:
                if (!isPressed()) {
                    mPointerId = pointerId;
                }
                break;
            case POINTER_SUBTYPE_MOVE:
                if (consumed && isPressed(pointerId)) {
                    cancel(context, document);
                }
                break;
            case POINTER_SUBTYPE_CANCEL:
                if (isPressed(pointerId)) {
                    cancel(context, document);
                }
                break;
            case POINTER_SUBTYPE_UP:
                if (isPressed(pointerId)) {
                    mPointerId = NO_POINTER;
                }
                break;
            default:
                break;
        }
        return POINTER_STATUS_NOT_CONSUMED;
    }

    private void cancel(@NonNull RemoteContext context, @NonNull CoreDocument document) {
        mPointerId = NO_POINTER;
        runActions(context, document);
    }

    private boolean isPressed() {
        return mPointerId != NO_POINTER;
    }

    private boolean isPressed(int pointerId) {
        return isPressed() && mPointerId == pointerId;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "TouchCancelModifier";
    }

    /**
     * Write the operation to the buffer. The wire format is identical to {@link
     * TouchCancelModifierOperation}.
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
        operations.add(new TouchCancelModifierOperation2());
    }

    /**
     * Add documentation for this operation
     *
     * @param doc a DocumentationBuilder
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, name())
                .description(
                        "Touch cancel modifier. This operation contains"
                                + " a list of action operations executed on touch cancel");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer.addType("TouchCancelModifierOperation");
    }
}
// CPD-ON
