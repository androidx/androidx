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

import static androidx.compose.remote.player.view.accessibility.RemoteComposeDocumentAccessibility.RootId;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.semantics.AccessibilitySemantics;
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode;
import androidx.compose.remote.player.view.accessibility.RemoteComposeDocumentAccessibility;
import androidx.compose.remote.player.view.accessibility.SemanticNodeApplier;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A helper class for implementing a custom accessibility service for remote Compose content.
 *
 * <p>This class extends {@link ExploreByTouchHelper} to provide accessibility support for a view
 * that displays remote Compose UI. It manages a tree of virtual views, each corresponding to a
 * {@link Component} in the remote UI.
 *
 * <p>Key responsibilities include:
 *
 * <ul>
 *   <li><b>Virtual View Management:</b> Translates touch events and accessibility requests to the
 *       appropriate virtual view.
 *   <li><b>Node Information:</b> Populates {@link AccessibilityNodeInfoCompat} objects with
 *       semantic information from {@link Component} data, using a {@link SemanticNodeApplier}.
 *   <li><b>Action Handling:</b> Delegates accessibility actions performed on virtual views to the
 *       underlying {@link RemoteComposeDocumentAccessibility} instance.
 * </ul>
 *
 * <p>This class relies on several other components:
 *
 * <ul>
 *   <li>{@link Component}: Represents a UI element in the remote Compose hierarchy, containing
 *       information about its position, size, and semantics.
 *   <li>{@link SemanticNodeApplier}: Responsible for translating the semantic properties of a
 *       {@link Component} into properties of an {@link AccessibilityNodeInfoCompat} object.
 *   <li>{@link RemoteComposeDocumentAccessibility}: Provides access to the remote Compose
 *       document's components and their semantic information. It also handles the execution of
 *       accessibility actions.
 *   <li>{@link AndroidxRemoteComposeAccessibilityRegistrar}: Typically used to register this helper
 *       with the Android accessibility framework and manage the overall accessibility lifecycle for
 *       the remote Compose view.
 * </ul>
 *
 * @param <N> The type of the node used by the {@link SemanticNodeApplier} (typically {@link
 *     AccessibilityNodeInfoCompat}).
 * @param <C> The type of the component data used by the {@link SemanticNodeApplier} (typically
 *     {@link Component}).
 * @param <S> The type of the semantic information used by the {@link SemanticNodeApplier}
 *     (typically {@link AccessibilitySemantics}).
 */
@RestrictTo(LIBRARY_GROUP)
public class AndroidxRemoteComposeTouchHelper<N, C, S> extends ExploreByTouchHelper {
    private final RemoteComposeDocumentAccessibility mRemoteDocA11y;

    private final SemanticNodeApplier<AccessibilityNodeInfoCompat> mApplier;
    private final View mHost;

    /**
     * Constructs an {@link AndroidxRemoteComposeTouchHelper}.
     *
     * @param host The {@link View} that hosts the remote Compose content and to which this touch
     *     helper will be attached.
     * @param remoteDocA11y The {@link RemoteComposeDocumentAccessibility} instance that provides
     *     access to the remote Compose document's structure and semantics, and handles
     *     accessibility actions.
     * @param applier The {@link SemanticNodeApplier} responsible for translating {@link Component}
     *     data and {@link AccessibilitySemantics} into {@link AccessibilityNodeInfoCompat}
     *     properties.
     */
    public AndroidxRemoteComposeTouchHelper(
            View host,
            RemoteComposeDocumentAccessibility remoteDocA11y,
            SemanticNodeApplier<AccessibilityNodeInfoCompat> applier) {
        super(host);
        this.mRemoteDocA11y = remoteDocA11y;
        this.mApplier = applier;
        this.mHost = host;
    }

    /**
     * Gets the virtual view ID at a given location on the screen.
     *
     * <p>This method is called by the Accessibility framework to determine which virtual view, if
     * any, is located at a specific point on the screen. It uses the {@link
     * RemoteComposeDocumentAccessibility#getComponentIdAt(PointF)} method to find the ID of the
     * component at the given coordinates.
     *
     * @param x The x-coordinate of the location in pixels.
     * @param y The y-coordinate of the location in pixels.
     * @return The ID of the virtual view at the given location, or {@link #INVALID_ID} if no
     *     virtual view is found at that location.
     */
    @Override
    protected int getVirtualViewAt(float x, float y) {
        @Nullable Integer root = mRemoteDocA11y.getComponentIdAt(new PointF(x, y));

        if (root == null) {
            return INVALID_ID;
        }

        return root;
    }

    /**
     * Populates a list with the visible virtual view IDs.
     *
     * <p>This method is called by the accessibility framework to retrieve the IDs of all visible
     * virtual views in the accessibility hierarchy. It traverses the hierarchy starting from the
     * root node (RootId) and adds the ID of each visible view to the provided list.
     *
     * @param virtualViewIds The list to be populated with the visible virtual view IDs.
     */
    @Override
    public void getVisibleVirtualViews(List<Integer> virtualViewIds) {
        virtualViewIds.addAll(getVisibleChildVirtualViews());
    }

    /**
     * Retrieves a list of IDs for all visible child virtual views.
     *
     * <p>This method determines the set of child virtual views that are currently visible and
     * should be exposed to the accessibility framework.
     *
     * @return A list of integer IDs representing the visible child virtual views.
     */
    @SuppressWarnings("JdkImmutableCollections")
    public List<Integer> getVisibleChildVirtualViews() {
        Component rootComponent = mRemoteDocA11y.findComponentById(RootId);

        if (rootComponent == null
                || !mRemoteDocA11y.semanticModifiersForComponent(rootComponent).isEmpty()) {
            return List.of(RootId);
        }

        return mRemoteDocA11y.semanticallyRelevantChildComponents(rootComponent, false);
    }

    @Override
    public void onPopulateNodeForVirtualView(
            int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
        Component component = mRemoteDocA11y.findComponentById(virtualViewId);

        Mode mergeMode = mRemoteDocA11y.mergeMode(component);

        // default to enabled
        node.setEnabled(true);

        if (mergeMode == Mode.MERGE) {
            List<Integer> childViews =
                    mRemoteDocA11y.semanticallyRelevantChildComponents(component, true);

            for (Integer childView : childViews) {
                onPopulateNodeForVirtualView(childView, node);
            }
        }

        List<AccessibilitySemantics> semantics =
                mRemoteDocA11y.semanticModifiersForComponent(component);
        mApplier.applyComponent(mRemoteDocA11y, node, component, semantics);

        if (mergeMode == Mode.SET) {
            List<Integer> childViews =
                    mRemoteDocA11y.semanticallyRelevantChildComponents(component, false);

            mApplier.addChildren(node, childViews);
        }
    }

    @Override
    protected boolean onPerformActionForVirtualView(
            int virtualViewId, int action, @Nullable Bundle arguments) {
        Component component = mRemoteDocA11y.findComponentById(virtualViewId);

        if (component != null) {
            boolean performed = mRemoteDocA11y.performAction(component, action, arguments);

            if (performed) {
                mHost.invalidate();
                invalidateRoot();
            }

            return performed;
        } else {
            return false;
        }
    }

    @Override
    protected void onPopulateEventForVirtualView(
            int virtualViewId, @NonNull AccessibilityEvent event) {
        super.onPopulateEventForVirtualView(virtualViewId, event);
    }
}
