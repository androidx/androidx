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
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/** Static helper methods for working with {@link AccessibilityNodeInfo}s. */
class AccessibilityNodeInfoHelper {

    private AccessibilityNodeInfoHelper() {}

    /**
     * Returns the visible bounds of an {@link AccessibilityNodeInfo}.
     *
     * @param node   node to analyze
     * @param width  display width in pixels
     * @param height display height in pixels
     * @return {@link Rect} containing the visible bounds
     */
    @NonNull
    static Rect getVisibleBoundsInScreen(@NonNull AccessibilityNodeInfo node, int width,
            int height) {
        Rect nodeBounds = new Rect();
        node.getBoundsInScreen(nodeBounds);

        // Trim portions that are outside the specified display bounds.
        Rect displayBounds = new Rect(0, 0, width, height);
        nodeBounds = intersect(nodeBounds, displayBounds);

        // Trim portions that are outside the window bounds on API 21+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Rect windowBounds = new Rect();
            AccessibilityWindowInfo window = Api21Impl.getWindow(node);
            if (window != null) {
                Api21Impl.getBoundsInScreen(window, windowBounds);
                nodeBounds = intersect(nodeBounds, windowBounds);
            }
        }

        // Trim portions that are outside the first scrollable ancestor.
        for (AccessibilityNodeInfo ancestor = node.getParent(); ancestor != null;
                ancestor = ancestor.getParent()) {
            if (ancestor.isScrollable()) {
                Rect ancestorBounds = getVisibleBoundsInScreen(ancestor, width, height);
                nodeBounds = intersect(nodeBounds, ancestorBounds);
                break;
            }
        }

        return nodeBounds;
    }

    /** Returns the intersection of two rectangles, or an empty rectangle if they do not overlap. */
    private static Rect intersect(Rect first, Rect second) {
        return first.intersect(second) ? first : new Rect();
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
        }

        @DoNotInline
        static void getBoundsInScreen(AccessibilityWindowInfo accessibilityWindowInfo,
                Rect outBounds) {
            accessibilityWindowInfo.getBoundsInScreen(outBounds);
        }

        @DoNotInline
        static AccessibilityWindowInfo getWindow(AccessibilityNodeInfo accessibilityNodeInfo) {
            return accessibilityNodeInfo.getWindow();
        }
    }
}
