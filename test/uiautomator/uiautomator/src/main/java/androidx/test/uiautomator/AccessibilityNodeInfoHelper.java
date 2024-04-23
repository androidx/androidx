/*
 * Copyright (C) 2012 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

/**
 * This class contains static helper methods to work with
 * {@link AccessibilityNodeInfo}
 */
class AccessibilityNodeInfoHelper {
    private static final String TAG = AccessibilityNodeInfoHelper.class.getSimpleName();

    private AccessibilityNodeInfoHelper() {}

    /**
     * Returns the node's bounds clipped to the size of the display
     *
     * @param node
     * @param width pixel width of the display
     * @param height pixel height of the display
     * @return null if node is null, else a Rect containing visible bounds
     */
    static Rect getVisibleBoundsInScreen(AccessibilityNodeInfo node, int width, int height,
            boolean trimScrollableParent) {
        return getVisibleBoundsInScreen(node, new Rect(0, 0, width, height), trimScrollableParent);
    }

    /**
     * Returns the node's bounds clipped to the size of the display
     *
     * @param node
     * @param displayRect the display rect
     * @return null if node is null, else a Rect containing visible bounds
     */
    static Rect getVisibleBoundsInScreen(AccessibilityNodeInfo node, Rect displayRect,
            boolean trimScrollableParent) {
        if (node == null) {
            return null;
        }
        // targeted node's bounds
        Rect nodeRect = new Rect();
        node.getBoundsInScreen(nodeRect);

        if (displayRect == null) {
            displayRect = new Rect();
        }
        intersectOrWarn(nodeRect, displayRect);

        // Trim any portion of the bounds that are outside the window
        Rect bounds = new Rect();
        AccessibilityWindowInfo window = node.getWindow();
        if (window != null) {
            window.getBoundsInScreen(bounds);
            intersectOrWarn(nodeRect, bounds);
        }

        // Trim the bounds into any scrollable ancestor, if required.
        if (trimScrollableParent) {
            for (AccessibilityNodeInfo ancestor = node.getParent(); ancestor != null; ancestor =
                    ancestor.getParent()) {
                if (ancestor.isScrollable()) {
                    Rect ancestorRect = getVisibleBoundsInScreen(ancestor, displayRect, true);
                    intersectOrWarn(nodeRect, ancestorRect);
                    break;
                }
            }
        }

        return nodeRect;
    }

    /**
     * Takes the intersection between the two input rectangles and stores the intersection in the
     * first one.
     *
     * @param target the targeted Rect to be clipped. The intersection result will be stored here.
     * @param bounds the bounds used to clip.
     */
    private static void intersectOrWarn(Rect target, Rect bounds) {
        if (!target.intersect(bounds)) {
            Log.v(TAG, String.format("No overlap between %s and %s. Ignoring.", target, bounds));
        }
    }
}
