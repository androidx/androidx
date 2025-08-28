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

import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContextActions;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.semantics.AccessibilitySemantics;
import androidx.compose.remote.player.view.accessibility.CoreDocumentAccessibility;
import androidx.compose.remote.player.view.accessibility.RemoteComposeAccessibilityRegistrar;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.jspecify.annotations.NonNull;

/**
 * This class registers {@link AndroidxRemoteComposeTouchHelper} to the given {@link View} for
 * accessibility support. {@link AndroidxRemoteComposeTouchHelper} implements {@link
 * androidx.core.view.AccessibilityDelegateCompat} to provide accessibility services.
 */
@RestrictTo(LIBRARY_GROUP)
public class AndroidxRemoteComposeAccessibilityRegistrar
        implements RemoteComposeAccessibilityRegistrar {
    /**
     * Creates an {@link AndroidxRemoteComposeTouchHelper} instance for a given {@link View} player
     * and {@link CoreDocument}.
     *
     * <p>This helper will manage accessibility for the remote Compose content displayed in the
     * player.
     *
     * @param player The View that is displaying the remote Compose content.
     * @param coreDocument The CoreDocument representing the remote Compose UI.
     * @return A new instance of AndroidxRemoteComposeTouchHelper.
     */
    public AndroidxRemoteComposeTouchHelper<
                    AccessibilityNodeInfoCompat, Component, AccessibilitySemantics>
            forRemoteComposePlayer(View player, @NonNull CoreDocument coreDocument) {
        return new AndroidxRemoteComposeTouchHelper<>(
                player,
                new CoreDocumentAccessibility(coreDocument, ((RemoteContextActions) player)),
                new AndroidxSemanticNodeApplier(player));
    }

    /**
     * Set the accessibility delegate on the player
     *
     * @param remoteComposePlayer The View representing the remote compose player.
     * @param document The CoreDocument containing the accessibility information for the UI
     *     elements.
     */
    @Override
    public void setAccessibilityDelegate(View remoteComposePlayer, CoreDocument document) {
        ViewCompat.setAccessibilityDelegate(
                remoteComposePlayer, forRemoteComposePlayer(remoteComposePlayer, document));
    }

    /**
     * Clear the accessibility delegate on the player
     *
     * @param remoteComposePlayer The View representing the remote compose player.
     */
    @Override
    public void clearAccessibilityDelegate(View remoteComposePlayer) {
        ViewCompat.setAccessibilityDelegate(remoteComposePlayer, null);
    }
}
