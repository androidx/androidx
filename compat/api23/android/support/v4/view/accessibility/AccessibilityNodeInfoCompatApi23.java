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

class AccessibilityNodeInfoCompatApi23 {
    public static Object getActionScrollToPosition() {
        return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION;
    }

    public static boolean isContextClickable(Object info) {
        return ((AccessibilityNodeInfo) info).isContextClickable();
    }

    public static void setContextClickable(Object info, boolean contextClickable) {
        ((AccessibilityNodeInfo) info).setContextClickable(contextClickable);
    }

    public static Object getActionShowOnScreen() {
        return AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN;
    }

    public static Object getActionScrollUp() {
        return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP;
    }

    public static Object getActionScrollDown() {
        return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN;
    }

    public static Object getActionScrollLeft() {
        return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT;
    }

    public static Object getActionScrollRight() {
        return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT;
    }

    public static Object getActionContextClick() {
        return AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK;
    }
}
