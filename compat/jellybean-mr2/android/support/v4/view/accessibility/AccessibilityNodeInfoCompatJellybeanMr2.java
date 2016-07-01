/*
 * Copyright (C) 2013 The Android Open Source Project
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

import java.util.List;

class AccessibilityNodeInfoCompatJellybeanMr2 {

    public static void setViewIdResourceName(Object info, String viewId) {
        ((AccessibilityNodeInfo) info).setViewIdResourceName(viewId);
    }

    public static String getViewIdResourceName(Object info) {
        return ((AccessibilityNodeInfo) info).getViewIdResourceName();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> findAccessibilityNodeInfosByViewId(Object info, String viewId) {
        Object result = ((AccessibilityNodeInfo) info).findAccessibilityNodeInfosByViewId(viewId);
        return (List<Object>) result;
    }

    public static void setTextSelection(Object info, int start, int end) {
        ((AccessibilityNodeInfo) info).setTextSelection(start, end);
    }

    public static int getTextSelectionStart(Object info) {
        return ((AccessibilityNodeInfo) info).getTextSelectionStart();
    }

    public static int getTextSelectionEnd(Object info) {
        return ((AccessibilityNodeInfo) info).getTextSelectionEnd();
    }

    public static boolean isEditable(Object info) {
        return ((AccessibilityNodeInfo) info).isEditable();
    }

    public static void setEditable(Object info, boolean editable) {
        ((AccessibilityNodeInfo) info).setEditable(editable);
    }

    public static boolean refresh(Object info) {
        return ((AccessibilityNodeInfo) info).refresh();
    }
}
