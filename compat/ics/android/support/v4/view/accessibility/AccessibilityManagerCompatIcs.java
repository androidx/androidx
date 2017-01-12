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

package android.support.v4.view.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.support.annotation.RequiresApi;
import android.annotation.TargetApi;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;

import java.util.List;

/**
 * ICS specific AccessibilityManager API implementation.
 */

@RequiresApi(14)
@TargetApi(14)
class AccessibilityManagerCompatIcs {

    public static class AccessibilityStateChangeListenerWrapper
            implements AccessibilityStateChangeListener {
        Object mListener;
        AccessibilityStateChangeListenerBridge mListenerBridge;

        public AccessibilityStateChangeListenerWrapper(Object listener,
                AccessibilityStateChangeListenerBridge listenerBridge) {
            mListener = listener;
            mListenerBridge = listenerBridge;
        }

        @Override
        public int hashCode() {
            return mListener == null ? 0 : mListener.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AccessibilityStateChangeListenerWrapper other =
                    (AccessibilityStateChangeListenerWrapper) o;
            return mListener == null ? other.mListener == null : mListener.equals(other.mListener);
        }

        @Override
        public void onAccessibilityStateChanged(boolean enabled) {
            mListenerBridge.onAccessibilityStateChanged(enabled);
        }
    }

    interface AccessibilityStateChangeListenerBridge {
        void onAccessibilityStateChanged(boolean enabled);
    }

    public static boolean addAccessibilityStateChangeListener(AccessibilityManager manager,
            AccessibilityStateChangeListenerWrapper listener) {
        return manager.addAccessibilityStateChangeListener(listener);
    }

    public static boolean removeAccessibilityStateChangeListener(AccessibilityManager manager,
            AccessibilityStateChangeListenerWrapper listener) {
        return manager.removeAccessibilityStateChangeListener(listener);
    }

    public static List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(
            AccessibilityManager manager,int feedbackTypeFlags) {
        return manager.getEnabledAccessibilityServiceList(feedbackTypeFlags);
    }

    public static List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(
            AccessibilityManager manager) {
        return manager.getInstalledAccessibilityServiceList();
    }

    public static boolean isTouchExplorationEnabled(AccessibilityManager manager) {
        return manager.isTouchExplorationEnabled();
    }
}
