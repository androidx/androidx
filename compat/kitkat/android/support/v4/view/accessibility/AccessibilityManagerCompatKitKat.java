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

import android.support.annotation.RequiresApi;
import android.annotation.TargetApi;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;

/**
 * KitKat-specific AccessibilityManager API implementation.
 */

@RequiresApi(19)
@TargetApi(19)
class AccessibilityManagerCompatKitKat {

    public static class TouchExplorationStateChangeListenerWrapper
            implements TouchExplorationStateChangeListener {
        final Object mListener;
        final TouchExplorationStateChangeListenerBridge mListenerBridge;

        public TouchExplorationStateChangeListenerWrapper(Object listener,
                TouchExplorationStateChangeListenerBridge listenerBridge) {
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
            TouchExplorationStateChangeListenerWrapper other =
                    (TouchExplorationStateChangeListenerWrapper) o;
            return mListener == null ? other.mListener == null : mListener.equals(other.mListener);
        }

        @Override
        public void onTouchExplorationStateChanged(boolean enabled) {
            mListenerBridge.onTouchExplorationStateChanged(enabled);
        }
    }

    interface TouchExplorationStateChangeListenerBridge {
        void onTouchExplorationStateChanged(boolean enabled);
    }

    public static Object newTouchExplorationStateChangeListener(
            final TouchExplorationStateChangeListenerBridge bridge) {
        return new TouchExplorationStateChangeListener() {
            @Override
            public void onTouchExplorationStateChanged(boolean enabled) {
                bridge.onTouchExplorationStateChanged(enabled);
            }
        };
    }

    public static boolean addTouchExplorationStateChangeListener(AccessibilityManager manager,
            Object listener) {
        return manager.addTouchExplorationStateChangeListener(
                (TouchExplorationStateChangeListener) listener);
    }

    public static boolean removeTouchExplorationStateChangeListener(AccessibilityManager manager,
            Object listener) {
        return manager.removeTouchExplorationStateChangeListener(
                (TouchExplorationStateChangeListener) listener);
    }
}
