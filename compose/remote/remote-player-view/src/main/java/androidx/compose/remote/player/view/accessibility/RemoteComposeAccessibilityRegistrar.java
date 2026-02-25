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
package androidx.compose.remote.player.view.accessibility;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface for registering and clearing accessibility delegates for remote compose players.
 *
 * <p>This interface is responsible for managing the accessibility delegate associated with a remote
 * compose player view. It allows for setting and clearing the delegate, which is used to handle
 * accessibility events and provide accessibility information for the remote compose content.
 */
@RestrictTo(LIBRARY_GROUP)
public interface RemoteComposeAccessibilityRegistrar {

    /**
     * Sets the accessibility delegate for the given remote compose player. Used when a new document
     * is displayed.
     *
     * <p>This method configures the accessibility services for the remote compose player, enabling
     * assistive technologies to interact with the UI elements rendered by the player.
     *
     * @param remoteComposePlayer The View representing the remote compose player.
     * @param document            The CoreDocument containing the accessibility information for
     *                            the UI
     *                            elements.
     */
    void setAccessibilityDelegate(@NonNull View remoteComposePlayer,
            @NonNull CoreDocument document);

    /**
     * Clears the accessibility delegate for the given remote compose player. Used when the document
     * is no longer displayed.
     *
     * <p>This method removes the accessibility services for the remote compose player, disabling
     * assistive technologies from interacting with the UI elements rendered by the player.
     *
     * @param remoteComposePlayer The View representing the remote compose player.
     */
    void clearAccessibilityDelegate(@NonNull View remoteComposePlayer);


    /**
     * Dispatch a hover event.
     *
     * @param event the motion event to be dispatched.
     * @return true if the event was handled by the view, false otherwise.
     */
    boolean dispatchHoverEvent(@NonNull View remoteComposePlayer, @NonNull MotionEvent event);


    /**
     * Dispatch a key event to the next view on the focus path.
     *
     * @param event the key event to be dispatched.
     * @return true if the event was handled, false otherwise.
     */
    boolean dispatchKeyEvent(@NonNull View remoteComposePlayer, @NonNull KeyEvent event);


    /**
     * Called by the view system when the focus state of this view changes.
     * When the focus change event is caused by directional navigation, direction
     * and previouslyFocusedRect provide insight into where the focus is coming from.
     *
     * @param gainFocus             true if the View has focus; false otherwise.
     * @param direction             the direction focus has moved when requestFocus()
     *                              is called to give this view focus.
     * @param previouslyFocusedRect the rectangle, in this view's coordinate
     *                              system, of the previously focused view.
     */
    void onFocusChanged(@NonNull View remoteComposePlayer, boolean gainFocus, int direction,
            @Nullable Rect previouslyFocusedRect);
}
