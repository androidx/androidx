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

package android.support.v4.view.accessibility;

import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.List;

/**
 * JellyBean specific AccessibilityNodeProvider API implementation.
 */
class AccessibilityNodeProviderCompatJellyBean {
    interface AccessibilityNodeInfoBridge {
        public Object createAccessibilityNodeInfo(int virtualViewId);
        public boolean performAction(int virtualViewId, int action, Bundle arguments);
        public List<Object> findAccessibilityNodeInfosByText(String text,
            int virtualViewId);
    }

    public static Object newAccessibilityNodeProviderBridge(
            final AccessibilityNodeInfoBridge bridge) {
        return new AccessibilityNodeProvider() {
            @Override
            public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
                return (AccessibilityNodeInfo) bridge.createAccessibilityNodeInfo(virtualViewId);
            }

            @Override
            @SuppressWarnings("unchecked")
            public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(
                    String text, int virtualViewId) {
                // Use some voodoo to avoid creating intermediary instances.
                return (List<AccessibilityNodeInfo>) (List<?>)
                    bridge.findAccessibilityNodeInfosByText(text, virtualViewId);
            }

            @Override
            public boolean performAction(int virtualViewId, int action, Bundle arguments) {
                return bridge.performAction(virtualViewId, action, arguments);
            }
        };
    }
}
