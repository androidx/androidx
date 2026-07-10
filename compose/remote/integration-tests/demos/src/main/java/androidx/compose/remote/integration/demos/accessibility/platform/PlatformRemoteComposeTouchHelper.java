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
package androidx.compose.remote.integration.demos.accessibility.platform;

import static androidx.compose.remote.player.view.accessibility.RemoteComposeDocumentAccessibility.RootId;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContextActions;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.semantics.AccessibilitySemantics;
import androidx.compose.remote.core.semantics.AccessibleComponent.Mode;
import androidx.compose.remote.player.view.accessibility.CoreDocumentAccessibility;
import androidx.compose.remote.player.view.accessibility.RemoteComposeDocumentAccessibility;
import androidx.compose.remote.player.view.accessibility.SemanticNodeApplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
@SuppressLint({"RestrictedApiAndroidX", "deprecation", "PrimitiveInCollection", "UnknownNullness"})
public class PlatformRemoteComposeTouchHelper extends ExploreByTouchHelper {
    private final RemoteComposeDocumentAccessibility mRemoteDocA11y;

    private final SemanticNodeApplier<AccessibilityNodeInfo> mApplier;
    private final View mHost;

    // Cache for last known child to semantic parent mapping
    // to allow correct calculation of boundsInParent
    // May grow, but not indefinitely O(200) entries
    private final Map<Integer, Integer> mChildToParentMapping = new HashMap<>();

    public PlatformRemoteComposeTouchHelper(
            View host,
            RemoteComposeDocumentAccessibility remoteDocA11y,
            SemanticNodeApplier<AccessibilityNodeInfo> applier) {
        super(host);
        this.mRemoteDocA11y = remoteDocA11y;
        this.mApplier = applier;
        this.mHost = host;
    }

    /**
     * access the helper
     */
    public static PlatformRemoteComposeTouchHelper forRemoteComposePlayer(
            View player, @NonNull CoreDocument coreDocument) {
        return new PlatformRemoteComposeTouchHelper(
                player,
                new CoreDocumentAccessibility(coreDocument, ((RemoteContextActions) player)),
                new AndroidPlatformSemanticNodeApplier(player));
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
        Integer root = mRemoteDocA11y.getComponentIdAt(new PointF(x, y));

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
    public void getVisibleVirtualViews(IntArray virtualViewIds) {
        List<Integer> children = getVisibleChildVirtualViews();
        for (int child : children) {
            virtualViewIds.add(child);
        }
    }

    /**
     * returns the list of visible children
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
            int virtualViewId, @NonNull AccessibilityNodeInfo node) {
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
        Integer semanticParentId = mChildToParentMapping.get(virtualViewId);
        mApplier.applyComponent(mRemoteDocA11y, node, component, semantics, semanticParentId);

        if (mergeMode == Mode.SET) {
            List<Integer> childViews =
                    mRemoteDocA11y.semanticallyRelevantChildComponents(component, false);

            // declare children so parent is known
            childViews.forEach((id) -> mChildToParentMapping.put(id, virtualViewId));

            mApplier.addChildren(node, childViews);
        }
    }

    @Override
    protected void onPopulateEventForVirtualView(
            int virtualViewId, @NonNull AccessibilityEvent event) {
        // This field should always be filled to keep the Accessibility framework happy.
        event.setContentDescription("");
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
}
