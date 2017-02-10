/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view;

import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * ICS specific AccessibilityDelegate API implementation.
 */

@RequiresApi(14)
class AccessibilityDelegateCompatIcs {

    interface AccessibilityDelegateBridge {
        boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event);
        void onInitializeAccessibilityEvent(View host, AccessibilityEvent event);
        void onInitializeAccessibilityNodeInfo(View host, Object info);
        void onPopulateAccessibilityEvent(View host, AccessibilityEvent event);
        boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
            AccessibilityEvent event);
        void sendAccessibilityEvent(View host, int eventType);
        void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event);
    }

    public static AccessibilityDelegate newAccessibilityDelegateBridge(
            final AccessibilityDelegateBridge bridge) {
        return new AccessibilityDelegate() {
            @Override
            public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                return bridge.dispatchPopulateAccessibilityEvent(host, event);
            }

            @Override
            public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                bridge.onInitializeAccessibilityEvent(host, event);
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                bridge.onInitializeAccessibilityNodeInfo(host, info);
            }

            @Override
            public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                bridge.onPopulateAccessibilityEvent(host, event);
            }

            @Override
            public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                    AccessibilityEvent event) {
                return bridge.onRequestSendAccessibilityEvent(host, child, event);
            }

            @Override
            public void sendAccessibilityEvent(View host, int eventType) {
                bridge.sendAccessibilityEvent(host, eventType);
            }

            @Override
            public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {
                bridge.sendAccessibilityEventUnchecked(host, event);
            }
        };
    }
}
