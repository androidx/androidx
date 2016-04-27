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

import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

class AccessibilityNodeInfoCompatJellybeanMr1 {

    public static void setLabelFor(Object info, View labeled) {
        ((AccessibilityNodeInfo) info).setLabelFor(labeled);
    }

    public static void setLabelFor(Object info, View root, int virtualDescendantId) {
        ((AccessibilityNodeInfo) info).setLabelFor(root, virtualDescendantId);
    }

    public static Object getLabelFor(Object info) {
        return ((AccessibilityNodeInfo) info).getLabelFor();
    }

    public static void setLabeledBy(Object info, View labeled) {
        ((AccessibilityNodeInfo) info).setLabeledBy(labeled);
    }

    public static void setLabeledBy(Object info, View root, int virtualDescendantId) {
        ((AccessibilityNodeInfo) info).setLabeledBy(root, virtualDescendantId);
    }

    public static Object getLabeledBy(Object info) {
        return ((AccessibilityNodeInfo) info).getLabeledBy();
    }
}
