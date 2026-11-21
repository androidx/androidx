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

package androidx.compose.remote.core.input;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.events.EventManager;
import androidx.compose.remote.core.events.EventRouter;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Routes pointer input events through a multi-pass hit testing hierarchy.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PointerInputRouter implements EventRouter {

    // TODO(b/585716538): find a central place for all the event types (most probably EventManager).
    public static final int EVENT_TYPE_POINTER = 1;

    /**
     * Empty status: no bits set. Used as the seed when accumulating the status of several
     * handlers; a handler that did not consume an event returns {@link
     * #POINTER_STATUS_NOT_CONSUMED}.
     */
    public static final int POINTER_STATUS_NONE = 0;

    public static final int POINTER_STATUS_NOT_CONSUMED = EventManager.STATUS_UNHANDLED;
    public static final int POINTER_STATUS_CONSUMED = 1;

    /**
     * Set in the status returned by {@link #routeEvent} when the event reached at least one
     * pointer event handler, whether or not it was consumed. Lets the host view claim the
     * pointer stream (return {@code true} from {@code dispatchTouchEvent}) for handlers that
     * observe the down without consuming it, such as scroll containers waiting for touch slop.
     */
    public static final int POINTER_STATUS_DISPATCHED = 1 << 1;

    public static final int POINTER_SUBTYPE_DOWN = 0;
    public static final int POINTER_SUBTYPE_UP = 1;
    public static final int POINTER_SUBTYPE_MOVE = 2;
    public static final int POINTER_SUBTYPE_CANCEL = 3;

    public static final int POINTER_PASS_DISPATCH = 0;
    public static final int POINTER_PASS_INITIAL = 1;
    public static final int POINTER_PASS_MAIN = 2;
    public static final int POINTER_PASS_FINAL = 3;

    private static final int DISPATCH_MODE_TUNNELING = 1;

    private static final int DISPATCH_MODE_BUBBLING = 2;

    /** The hit paths of the current document, or null before the first event. */
    @Nullable HitPathTracker mHitPathTracker = null;

    /** Reused to get the location of a component, so that dispatch does not allocate. */
    private final float @NonNull [] mLocationInWindow = new float[2];

    /**
     * Registers an event handler. No-op since handlers are discovered via hit testing.
     *
     * @param eventHandler event handler to register
     */
    @Override
    public void registerHandler(@NonNull EventHandler eventHandler) {
        // Unlike other event handlers, PointerInputRouter will find the routers from the
        // component tree. (Pointer Input uses hit testing to route events).
    }

    /**
     * Routes a pointer input event through initial, main, and final passes.
     *
     * @param context current remote context
     * @param document document containing the component hierarchy
     * @param event pointer event to route
     * @return status: {@link #POINTER_STATUS_CONSUMED} if a handler consumed the event, plus
     *     {@link #POINTER_STATUS_DISPATCHED} if the event reached at least one handler
     * @throws IllegalArgumentException if the pointer id of the event is not from 0 to {@link
     *     HitPathTracker#MAX_POINTER_ID}
     */
    @Override
    public int routeEvent(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Event event) {
        PointerInputEvent inputEvent = (PointerInputEvent) event;
        int subtype = inputEvent.getSubtype();
        int pointerId = inputEvent.getPointerId();

        Component root = document.getRootLayoutComponent();
        if (root == null) {
            // A document without a layout has no components to send the event to.
            mHitPathTracker = null;
            return POINTER_STATUS_NONE;
        }
        if (mHitPathTracker == null || mHitPathTracker.getRoot() != root) {
            // The first event, or the first event of a new document. The pointers that went
            // down on the previous document are forgotten.
            mHitPathTracker = new HitPathTracker(root);
        }

        HitPathTracker hitPathTracker = mHitPathTracker;

        // Add pointer to hit path.
        if (subtype == POINTER_SUBTYPE_DOWN) {
            hitPathTracker.addHitPath(context, pointerId, inputEvent.getX(), inputEvent.getY());
        }

        int status = POINTER_STATUS_NONE;

        // INITIAL PASS.
        inputEvent.setPass(POINTER_PASS_INITIAL);
        status |= dispatch(context, document, hitPathTracker, inputEvent, DISPATCH_MODE_TUNNELING);

        // MAIN PASS.
        inputEvent.setPass(POINTER_PASS_MAIN);
        status |= dispatch(context, document, hitPathTracker, inputEvent, DISPATCH_MODE_BUBBLING);

        // FINAL PASS.
        inputEvent.setPass(POINTER_PASS_FINAL);
        status |= dispatch(context, document, hitPathTracker, inputEvent, DISPATCH_MODE_TUNNELING);

        // Remove pointer from hit path.
        if (subtype == POINTER_SUBTYPE_UP || subtype == POINTER_SUBTYPE_CANCEL) {
            hitPathTracker.removeHitPath(pointerId);
        }

        return status;
    }

    /**
     * Dispatches a pointer input event to the components on the hit paths, in one pass.
     *
     * <p>Mirrors the ordering of Compose's hit path traversal: when tunneling (initial / final
     * passes) a component gets the event before its children, and when bubbling (main pass) it
     * gets the event after its children. The nodes of the tracker are in tree order, so tunneling
     * goes from the first node to the last and bubbling from the last node to the first.
     *
     * <p>Only the nodes that the pointer of the event is on get the event. With several pointers
     * down, the components under the other pointers do not get it. Sibling nodes only exist when
     * several pointers hit different components, so their relative order carries no meaning.
     *
     * @param context current remote context
     * @param document document containing the component hierarchy
     * @param hitPathTracker hit paths of the document
     * @param event pointer input event being dispatched
     * @param dispatchMode tunneling (top-down) or bubbling (bottom-up)
     * @return status bits: {@link #POINTER_STATUS_CONSUMED} and / or {@link
     *     #POINTER_STATUS_DISPATCHED}
     */
    private int dispatch(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull HitPathTracker hitPathTracker,
            @NonNull PointerInputEvent event,
            int dispatchMode) {
        int pointerId = event.getPointerId();
        int status = POINTER_STATUS_NONE;
        int size = hitPathTracker.size();
        if (dispatchMode == DISPATCH_MODE_BUBBLING) {
            for (int i = size - 1; i >= 0; i--) {
                if (hitPathTracker.hasPointer(i, pointerId)) {
                    Component component = hitPathTracker.getComponent(i);
                    status |=
                            dispatchToComponent(context, document, component, event, dispatchMode);
                }
            }
        } else if (dispatchMode == DISPATCH_MODE_TUNNELING) {
            for (int i = 0; i < size; i++) {
                if (hitPathTracker.hasPointer(i, pointerId)) {
                    Component component = hitPathTracker.getComponent(i);
                    status |=
                            dispatchToComponent(context, document, component, event, dispatchMode);
                }
            }
        }
        return status;
    }

    /**
     * Dispatches a pointer input event to the modifiers and the handlers of one component.
     *
     * <p>Mirrors the ordering of Compose's hit path traversal: when tunneling the order is
     * modifiers (outer to inner), then content handlers; when bubbling the exact reverse: content
     * handlers, then modifiers (inner to outer).
     *
     * <p>The handlers see the position of the pointer local to the component. The router moves the
     * event into the coordinates of the component, and moves it back to window coordinates after
     * the handlers ran. This changes the event in place, so that dispatch does not allocate, and so
     * that a handler that consumes the event marks the one event that every later handler sees.
     *
     * @param context current remote context
     * @param document document containing the component hierarchy
     * @param component component whose modifiers and handlers receive the event
     * @param event pointer input event in window coordinates
     * @param dispatchMode tunneling (top-down) or bubbling (bottom-up)
     * @return status bits: {@link #POINTER_STATUS_CONSUMED} and / or {@link
     *     #POINTER_STATUS_DISPATCHED}
     */
    private int dispatchToComponent(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            @NonNull PointerInputEvent event,
            int dispatchMode) {
        float eventX = event.getX();
        float eventY = event.getY();
        convertToLocalCoordinates(context, component, event);

        int status = POINTER_STATUS_NONE;
        try {
            if (dispatchMode == DISPATCH_MODE_BUBBLING) {
                status |=
                        dispatchToComponentHandlers(
                                context, document, component, event, dispatchMode);
                status |= dispatchToModifiers(context, document, component, event, dispatchMode);
            } else if (dispatchMode == DISPATCH_MODE_TUNNELING) {
                status |= dispatchToModifiers(context, document, component, event, dispatchMode);
                status |=
                        dispatchToComponentHandlers(
                                context, document, component, event, dispatchMode);
            }
        } finally {
            event.setPosition(eventX, eventY);
        }
        return status;
    }

    /**
     * Changes the position of an event from window coordinates to the local coordinates of a
     * component.
     *
     * @param context current remote context
     * @param component component whose coordinates the event is moved into
     * @param event pointer input event in window coordinates, changed in place
     */
    private void convertToLocalCoordinates(
            @NonNull RemoteContext context,
            @NonNull Component component,
            @NonNull PointerInputEvent event) {
        // Clear the reused array: getLocationInWindow adds to it instead of overwriting it.
        mLocationInWindow[0] = 0f;
        mLocationInWindow[1] = 0f;
        component.getLocationInWindow(context, mLocationInWindow);
        event.setPosition(event.getX() - mLocationInWindow[0], event.getY() - mLocationInWindow[1]);
    }

    /**
     * Dispatches a pointer input event to the event handling modifiers of a layout component.
     *
     * <p>The modifier list is ordered outer to inner. Tunneling passes visit it in order and the
     * bubbling pass visits it in reverse, matching the modifier chain traversal in Compose.
     *
     * @param context current remote context
     * @param document document containing the component hierarchy
     * @param component component whose modifiers receive the event
     * @param event pointer input event in the component's local coordinates
     * @param dispatchMode tunneling (outer to inner) or bubbling (inner to outer)
     * @return status bits accumulated over the modifiers
     */
    private int dispatchToModifiers(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            @NonNull PointerInputEvent event,
            int dispatchMode) {
        int status = POINTER_STATUS_NONE;
        if (!(component instanceof LayoutComponent)) {
            return status;
        }
        List<ModifierOperation> modifiers =
                ((LayoutComponent) component).getComponentModifiers().getModifiersList();
        int size = modifiers.size();
        for (int i = 0; i < size; i++) {
            int index = dispatchMode == DISPATCH_MODE_BUBBLING ? size - 1 - i : i;
            ModifierOperation modifier = modifiers.get(index);
            if (modifier instanceof EventHandler) {
                status |= dispatchToHandler(context, document, (EventHandler) modifier, event);
            }
        }
        return status;
    }

    /**
     * Dispatches a pointer input event to event handlers hosted directly in a component's
     * operation list (e.g. {@code TouchExpression2}). Child components and modifiers are skipped
     * as they are dispatched separately.
     *
     * @param context current remote context
     * @param document document containing the component hierarchy
     * @param component component whose hosted handlers receive the event
     * @param event pointer input event in the component's local coordinates
     * @param dispatchMode tunneling (document order) or bubbling (reverse document order)
     * @return status bits accumulated over the handlers
     */
    private int dispatchToComponentHandlers(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            @NonNull PointerInputEvent event,
            int dispatchMode) {
        int status = POINTER_STATUS_NONE;
        List<Operation> operations = component.mList;
        int size = operations.size();
        for (int i = 0; i < size; i++) {
            int index = dispatchMode == DISPATCH_MODE_BUBBLING ? size - 1 - i : i;
            Operation op = operations.get(index);
            if (op instanceof EventHandler
                    && !(op instanceof Component)
                    && !(op instanceof ModifierOperation)) {
                status |= dispatchToHandler(context, document, (EventHandler) op, event);
            }
        }
        return status;
    }

    /**
     * Dispatches a pointer input event to a single handler, marking the event consumed if the
     * handler reports so.
     *
     * @param context current remote context
     * @param document document containing the component hierarchy
     * @param eventHandler handler receiving the event
     * @param event pointer input event in the handler's local coordinates
     * @return {@link #POINTER_STATUS_DISPATCHED} if the handler accepted the event type, plus
     *     {@link #POINTER_STATUS_CONSUMED} if it consumed the event
     */
    private int dispatchToHandler(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull EventHandler eventHandler,
            @NonNull PointerInputEvent event) {
        if (!eventHandler.matchesEvent(EVENT_TYPE_POINTER)) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        int status = eventHandler.onEvent(context, document, event) | POINTER_STATUS_DISPATCHED;
        if ((status & POINTER_STATUS_CONSUMED) != 0) {
            event.markConsumed();
        }
        return status;
    }
}
