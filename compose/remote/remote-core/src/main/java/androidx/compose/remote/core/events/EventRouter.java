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

/** Interface representing components capable of routing [Event]s to [EventHandler]s. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface EventRouter {

    /**
     * Registers an [EventHandler] on the router.
     *
     * @param eventHandler the handler to register
     */
    void registerHandler(@NonNull EventHandler eventHandler);

    /**
     * Routes the dispatched event to its compatible handlers.
     *
     * @param context the current paint and state context
     * @param document the root document
     * @param event the dispatched event
     * @return status code resulting from routing the event
     */
    int routeEvent(
            @NonNull RemoteContext context, @NonNull CoreDocument document, @NonNull Event event);
}
