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

package android.support.v4.view;

import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

/**
 * JellyBean specific AccessibilityDelegate API implementation.
 */

@RequiresApi(16)
class AccessibilityDelegateCompatJellyBean {

    interface AccessibilityDelegateBridgeJellyBean {
        boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event);
        void onInitializeAccessibilityEvent(View host, AccessibilityEvent event);
        void onInitializeAccessibilityNodeInfo(View host, Object info);
        void onPopulateAccessibilityEvent(View host, AccessibilityEvent event);
        boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                AccessibilityEvent event);
        void sendAccessibilityEvent(View host, int eventType);
        void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event);
        Object getAccessibilityNodeProvider(View host);
        boolean performAccessibilityAction(View host, int action, Bundle args);
    }

    public static AccessibilityDelegate newAccessibilityDelegateBridge(
            final AccessibilityDelegateBridgeJellyBean bridge) {
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

            @Override
            public AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
                return (AccessibilityNodeProvider) bridge.getAccessibilityNodeProvider(host);
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                return bridge.performAccessibilityAction(host, action, args);
            }
        };
    }

    public static Object getAccessibilityNodeProvider(AccessibilityDelegate delegate,
            View host) {
        return delegate.getAccessibilityNodeProvider(host);
    }

    public static boolean performAccessibilityAction(AccessibilityDelegate delegate, View host,
            int action, Bundle args) {
        return delegate.performAccessibilityAction(host, action, args);
    }
}
