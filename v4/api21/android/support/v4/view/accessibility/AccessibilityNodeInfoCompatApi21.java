/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;

import java.util.List;

/**
 * Api21-specific AccessibilityNodeInfo API implementation.
 */
class AccessibilityNodeInfoCompatApi21 {
    static List<Object> getActionList(Object info) {
        Object result = ((AccessibilityNodeInfo) info).getActionList();
        return (List<Object>) result;
    }

    static void addAction(Object info, Object action) {
        ((AccessibilityNodeInfo) info).addAction((AccessibilityAction) action);
    }

    public static boolean removeAction(Object info, Object action) {
        return ((AccessibilityNodeInfo) info).removeAction((AccessibilityAction) action);
    }

    public static Object obtainCollectionInfo(int rowCount, int columnCount,
            boolean hierarchical, int selectionMode) {
        return AccessibilityNodeInfo.CollectionInfo.obtain(rowCount, columnCount, hierarchical,
                selectionMode);
    }

    public static Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
            int columnSpan, boolean heading, boolean selected) {
        return AccessibilityNodeInfo.CollectionItemInfo.obtain(rowIndex, rowSpan, columnIndex,
                columnSpan, heading, selected);
    }

    public static CharSequence getError(Object info) {
        return ((AccessibilityNodeInfo) info).getError();
    }

    public static void setError(Object info, CharSequence error) {
        ((AccessibilityNodeInfo) info).setError(error);
    }

    public static void setMaxTextLength(Object info, int max) {
        ((AccessibilityNodeInfo) info).setMaxTextLength(max);
    }

    public static int getMaxTextLength(Object info) {
        return ((AccessibilityNodeInfo) info).getMaxTextLength();
    }

    public static Object getWindow(Object info) {
        return ((AccessibilityNodeInfo) info).getWindow();
    }

    public static boolean removeChild(Object info, View child) {
        return ((AccessibilityNodeInfo) info).removeChild(child);
    }

    public static boolean removeChild(Object info, View root, int virtualDescendantId) {
        return ((AccessibilityNodeInfo) info).removeChild(root, virtualDescendantId);
    }

    static class CollectionItemInfo {
        public static boolean isSelected(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).isSelected();
        }
    }

    static Object newAccessibilityAction(int actionId, CharSequence label) {
        return new AccessibilityAction(actionId, label);
    }

    static int getAccessibilityActionId(Object action) {
        return ((AccessibilityNodeInfo.AccessibilityAction) action).getId();
    }

    static CharSequence getAccessibilityActionLabel(Object action) {
        return ((AccessibilityNodeInfo.AccessibilityAction) action).getLabel();
    }
}
