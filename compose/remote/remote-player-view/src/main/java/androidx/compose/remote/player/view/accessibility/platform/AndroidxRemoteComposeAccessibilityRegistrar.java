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
package androidx.compose.remote.player.view.accessibility.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContextActions;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.semantics.AccessibilitySemantics;
import androidx.compose.remote.player.view.accessibility.CoreDocumentAccessibility;
import androidx.compose.remote.player.view.accessibility.RemoteComposeAccessibilityRegistrar;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * This class registers {@link AndroidxRemoteComposeTouchHelper} to the given {@link View} for
 * accessibility support. {@link AndroidxRemoteComposeTouchHelper} implements {@link
 * androidx.core.view.AccessibilityDelegateCompat} to provide accessibility services.
 */
@RestrictTo(LIBRARY_GROUP)
public class AndroidxRemoteComposeAccessibilityRegistrar implements
        RemoteComposeAccessibilityRegistrar {
    /**
     * Creates an {@link AndroidxRemoteComposeTouchHelper} instance for a given {@link View} player
     * and {@link CoreDocument}.
     *
     * <p>This helper will manage accessibility for the remote Compose content displayed in the
     * player.
     *
     * @param player       The View that is displaying the remote Compose content.
     * @param coreDocument The CoreDocument representing the remote Compose UI.
     * @return A new instance of AndroidxRemoteComposeTouchHelper.
     */
    public @NonNull AndroidxRemoteComposeTouchHelper<AccessibilityNodeInfoCompat, Component,
            AccessibilitySemantics> forRemoteComposePlayer(
            @NonNull View player, @NonNull CoreDocument coreDocument) {
        return new AndroidxRemoteComposeTouchHelper<>(player,
                new CoreDocumentAccessibility(coreDocument, ((RemoteContextActions) player)),
                new AndroidxSemanticNodeApplier(player));
    }

    /**
     * Set the accessibility delegate on the player
     *
     * @param remoteComposePlayer The View representing the remote compose player.
     * @param document            The CoreDocument containing the accessibility information for
     *                            the UI
     *                            elements.
     */
    @Override
    public void setAccessibilityDelegate(@NonNull View remoteComposePlayer,
            @NonNull CoreDocument document) {
        ViewCompat.setAccessibilityDelegate(remoteComposePlayer,
                forRemoteComposePlayer(remoteComposePlayer, document));
    }

    /**
     * Clear the accessibility delegate on the player
     *
     * @param remoteComposePlayer The View representing the remote compose player.
     */
    @Override
    public void clearAccessibilityDelegate(@NonNull View remoteComposePlayer) {
        ViewCompat.setAccessibilityDelegate(remoteComposePlayer, null);
    }

    /**
     * Gets the accessibility delegate from the player if it is an instance of
     * {@link ExploreByTouchHelper}.
     *
     * @param remoteComposePlayer The View representing the remote compose player.
     * @return The ExploreByTouchHelper delegate, or null if it's not set or not of that type.
     */
    public @Nullable ExploreByTouchHelper getAccessibilityDelegate(
            @NonNull View remoteComposePlayer) {
        AccessibilityDelegateCompat accessibilityDelegate = ViewCompat.getAccessibilityDelegate(
                remoteComposePlayer);
        return accessibilityDelegate instanceof ExploreByTouchHelper
                ? (ExploreByTouchHelper) accessibilityDelegate
                : null;
    }

    /**
     * Dispatch a hover event.
     *
     * @param event the motion event to be dispatched.
     * @return true if the event was handled by the view, false otherwise.
     */
    @Override
    public boolean dispatchHoverEvent(@NonNull View remoteComposePlayer,
            @NonNull MotionEvent event) {
        ExploreByTouchHelper exploreByTouchHelper = getAccessibilityDelegate(remoteComposePlayer);
        return (exploreByTouchHelper != null && exploreByTouchHelper.dispatchHoverEvent(event));
    }


    /**
     * Dispatch a key event to the next view on the focus path.
     *
     * @param event the key event to be dispatched.
     * @return true if the event was handled, false otherwise.
     */
    @Override
    public boolean dispatchKeyEvent(@NonNull View remoteComposePlayer, @NonNull KeyEvent event) {
        ExploreByTouchHelper exploreByTouchHelper = getAccessibilityDelegate(remoteComposePlayer);
        return (exploreByTouchHelper != null && exploreByTouchHelper.dispatchKeyEvent(event));
    }


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
    @Override
    public void onFocusChanged(@NonNull View remoteComposePlayer, boolean gainFocus, int direction,
            @Nullable Rect previouslyFocusedRect) {
        ExploreByTouchHelper exploreByTouchHelper = getAccessibilityDelegate(remoteComposePlayer);
        if (exploreByTouchHelper != null) {
            exploreByTouchHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        }
    }
}
