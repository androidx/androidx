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

package androidx.compose.remote.core.events;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Manages event routing and registration of handlers. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EventManager {
    /** Status indicating the event was not processed by any router or handler. */
    public static final int STATUS_UNHANDLED = 0;

    private final Map<Integer, EventRouter> mRouterMap = new HashMap<>();

    private final Set<EventHandler> mEventHandlerSet = new HashSet<>();

    /**
     * Registers an [EventHandler] on all compatible registered routers.
     *
     * @param eventHandler the handler to register
     */
    public void registerHandler(@NonNull EventHandler eventHandler) {
        mEventHandlerSet.add(eventHandler);
        for (Map.Entry<Integer, EventRouter> entry : mRouterMap.entrySet()) {
            if (eventHandler.matchesEvent(entry.getKey())) {
                entry.getValue().registerHandler(eventHandler);
            }
        }
    }

    /**
     * Registers an [EventRouter] for a specific event type.
     *
     * @param eventType the event type to route
     * @param eventRouter the router to associate with the event type
     */
    public void registerRouter(int eventType, @NonNull EventRouter eventRouter) {
        if (mRouterMap.containsKey(eventType)) {
            throw new IllegalStateException("Existing router for this event type.");
        }
        mRouterMap.put(eventType, eventRouter);
        for (EventHandler eventHandler : mEventHandlerSet) {
            if (eventHandler.matchesEvent(eventType)) {
                eventRouter.registerHandler(eventHandler);
            }
        }
    }

    /**
     * Dispatches an event through the matching registered router.
     *
     * @param context the current paint and state context
     * @param document the root document
     * @param event the event to dispatch
     * @return status code resulting from dispatching the event
     */
    public int dispatchEvent(
            @NonNull RemoteContext context, @NonNull CoreDocument document, @NonNull Event event) {
        EventRouter router = mRouterMap.get(event.getType());
        if (router == null) {
            return STATUS_UNHANDLED;
        }
        return router.routeEvent(context, document, event);
    }
}
