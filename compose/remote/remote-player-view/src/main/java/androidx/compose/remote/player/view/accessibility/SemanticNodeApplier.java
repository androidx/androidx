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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.semantics.AccessibilitySemantics;

import java.util.List;

/**
 * An interface for applying semantic information to a semantics node.
 *
 * <p>Implementations of this interface are responsible for taking a node represented by {@code
 * nodeInfo} and applying a list of {@code semantics} (representing accessible actions and
 * properties) to it. This process might involve:
 *
 * <ul>
 *   <li>Modifying the node's properties (e.g., content description, clickable state).
 *   <li>Adding a child node to represent a specific semantic element.
 *   <li>Performing any other action necessary to make the node semantically meaningful and
 *       accessible to assistive technologies.
 * </ul>
 *
 * @param <N> The type representing information about the node. This could be an Androidx
 *     `AccessibilityNodeInfoCompat`, or potentially a platform `AccessibilityNodeInfo`.
 */
@RestrictTo(LIBRARY_GROUP)
public interface SemanticNodeApplier<N> {
    /**
     * This method applies semantic information from a list of {@link AccessibilitySemantics} to a
     * given node ({@code nodeInfo}).
     *
     * <p>The {@code remoteComposeAccessibility} parameter provides context about the broader remote
     * compose document's accessibility state. The {@code component} parameter provides context
     * about the component being processed.
     *
     * @param remoteComposeAccessibility An instance of {@link RemoteComposeDocumentAccessibility}
     *     providing context.
     * @param nodeInfo The node to which the semantic information will be applied. This could be an
     *     Androidx `AccessibilityNodeInfoCompat` or a platform `AccessibilityNodeInfo`.
     * @param component The component that this semantic information corresponds to.
     * @param semantics A list of {@link AccessibilitySemantics} objects representing the semantic
     *     properties and actions to apply.
     */
    void applyComponent(
            RemoteComposeDocumentAccessibility remoteComposeAccessibility,
            N nodeInfo,
            Component component,
            List<AccessibilitySemantics> semantics);

    /**
     * add children to the node
     *
     * @param nodeInfo
     * @param childIds
     */
    void addChildren(N nodeInfo, List<Integer> childIds);

    String VIRTUAL_VIEW_ID_KEY = "VirtualViewId";
}
