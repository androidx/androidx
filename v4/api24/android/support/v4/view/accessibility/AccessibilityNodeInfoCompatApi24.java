/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v4.view.accessibility;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Api24-specific AccessibilityNodeInfo API implementation.
 */
class AccessibilityNodeInfoCompatApi24 {
    public static int getDrawingOrder(Object info) {
        return ((AccessibilityNodeInfo) info).getDrawingOrder();
    }

    public static void setDrawingOrder(Object info, int drawingOrderInParent) {
        ((AccessibilityNodeInfo) info).setDrawingOrder(drawingOrderInParent);
    }

    public static boolean isImportantForAccessibility(Object info) {
        return ((AccessibilityNodeInfo) info).isImportantForAccessibility();
    }

    public static void setImportantForAccessibility(Object info,
            boolean importantForAccessibility) {
        ((AccessibilityNodeInfo) info).setImportantForAccessibility(importantForAccessibility);
    }
}
