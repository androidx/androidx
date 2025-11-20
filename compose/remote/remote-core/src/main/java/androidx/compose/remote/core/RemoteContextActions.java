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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.semantics.ScrollableComponent;

import org.jspecify.annotations.NonNull;

/**
 * This interface defines a contract for objects that are aware of a {@link RemoteContext}.
 *
 * <p>PlayerViews should implement to provide access to the RemoteContext.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteContextActions {

    /**
     * Show a child with the given ID on the screen, typically scrolling so it's fully on screen.
     *
     * @param component The child (including nested) to check for visibility.
     * @return {@code true} if the child with the given ID could be shown on screen; {@code false}
     *     otherwise.
     */
    boolean showOnScreen(@NonNull Component component);

    /**
     * Scrolls the content by the specified offset.
     *
     * @param component the component to scroll
     * @param offset The amount to scroll by in pixels. Positive values indicate scrolling down or
     *     to the right, while negative values indicate scrolling up or to the left.
     * @return The offset value that was consumed by this component scrolling.
     */
    int scrollByOffset(@NonNull Component component, int offset);

    /**
     * Scrolls the content in the specified direction.
     *
     * @param component the component to scroll
     * @param direction the direction to scroll
     * @return whether a scroll was possible
     */
    boolean scrollDirection(
            @NonNull Component component, ScrollableComponent.@NonNull ScrollDirection direction);

    /**
     * Perform a click on the given component
     *
     * @param document the document to perform the click on
     * @param component the component to click on
     * @param metadata the metadata of the click event
     * @return whether the event was handled
     */
    boolean performClick(
            @NonNull CoreDocument document, @NonNull Component component, @NonNull String metadata);
}
