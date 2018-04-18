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

package androidx.core.view.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Build;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

/**
 * Helper for accessing features in {@link AccessibilityManager}.
 */
public final class AccessibilityManagerCompat {
    /**
     * Registers an {@link AccessibilityManager.AccessibilityStateChangeListener} for changes in
     * the global accessibility state of the system.
     *
     * @param manager The accessibility manager.
     * @param listener The listener.
     * @return True if successfully registered.
     *
     * @deprecated Use {@link AccessibilityManager#addAccessibilityStateChangeListener(
     *             AccessibilityManager.AccessibilityStateChangeListener)} directly.
     */
    @Deprecated
    public static boolean addAccessibilityStateChangeListener(AccessibilityManager manager,
            AccessibilityStateChangeListener listener) {
        if (listener == null) {
            return false;
        }
        return manager.addAccessibilityStateChangeListener(
                new AccessibilityStateChangeListenerWrapper(listener));
    }

    /**
     * Unregisters an {@link AccessibilityManager.AccessibilityStateChangeListener}.
     *
     * @param manager The accessibility manager.
     * @param listener The listener.
     * @return True if successfully unregistered.
     *
     * @deprecated Use {@link AccessibilityManager#removeAccessibilityStateChangeListener(
     *             AccessibilityManager.AccessibilityStateChangeListener)} directly.
     */
    @Deprecated
    public static boolean removeAccessibilityStateChangeListener(AccessibilityManager manager,
            AccessibilityStateChangeListener listener) {
        if (listener == null) {
            return false;
        }
        return manager.removeAccessibilityStateChangeListener(
                new AccessibilityStateChangeListenerWrapper(listener));
    }

    private static class AccessibilityStateChangeListenerWrapper
            implements AccessibilityManager.AccessibilityStateChangeListener {
        AccessibilityStateChangeListener mListener;

        AccessibilityStateChangeListenerWrapper(
                @NonNull AccessibilityStateChangeListener listener) {
            mListener = listener;
        }

        @Override
        public int hashCode() {
            return mListener.hashCode();
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
            return mListener.equals(other.mListener);
        }

        @Override
        public void onAccessibilityStateChanged(boolean enabled) {
            mListener.onAccessibilityStateChanged(enabled);
        }
    }

    /**
     * Returns the {@link AccessibilityServiceInfo}s of the installed accessibility services.
     *
     * @param manager The accessibility manager.
     * @return An unmodifiable list with {@link AccessibilityServiceInfo}s.
     *
     * @deprecated Use {@link AccessibilityManager#getInstalledAccessibilityServiceList()} directly.
     */
    @Deprecated
    public static List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(
            AccessibilityManager manager) {
        return manager.getInstalledAccessibilityServiceList();
    }

    /**
     * Returns the {@link AccessibilityServiceInfo}s of the enabled accessibility services
     * for a given feedback type.
     *
     * @param manager The accessibility manager.
     * @param feedbackTypeFlags The feedback type flags.
     * @return An unmodifiable list with {@link AccessibilityServiceInfo}s.
     *
     * @see AccessibilityServiceInfo#FEEDBACK_AUDIBLE
     * @see AccessibilityServiceInfo#FEEDBACK_GENERIC
     * @see AccessibilityServiceInfo#FEEDBACK_HAPTIC
     * @see AccessibilityServiceInfo#FEEDBACK_SPOKEN
     * @see AccessibilityServiceInfo#FEEDBACK_VISUAL
     *
     * @deprecated Use {@link AccessibilityManager#getEnabledAccessibilityServiceList(int)}
     * directly.
     */
    @Deprecated
    public static List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(
            AccessibilityManager manager, int feedbackTypeFlags) {
        return manager.getEnabledAccessibilityServiceList(feedbackTypeFlags);
    }

    /**
     * Returns if the touch exploration in the system is enabled.
     *
     * @param manager The accessibility manager.
     * @return True if touch exploration is enabled, false otherwise.
     *
     * @deprecated Use {@link AccessibilityManager#isTouchExplorationEnabled()} directly.
     */
    @Deprecated
    public static boolean isTouchExplorationEnabled(AccessibilityManager manager) {
        return manager.isTouchExplorationEnabled();
    }

    /**
     * Registers a {@link TouchExplorationStateChangeListener} for changes in
     * the global touch exploration state of the system.
     *
     * @param listener The listener.
     * @return True if successfully registered.
     */
    public static boolean addTouchExplorationStateChangeListener(AccessibilityManager manager,
            TouchExplorationStateChangeListener listener) {
        if (Build.VERSION.SDK_INT >= 19) {
            if (listener == null) {
                return false;
            }
            return manager.addTouchExplorationStateChangeListener(
                    new TouchExplorationStateChangeListenerWrapper(listener));
        } else {
            return false;
        }
    }

    /**
     * Unregisters a {@link TouchExplorationStateChangeListener}.
     *
     * @param listener The listener.
     * @return True if successfully unregistered.
     */
    public static boolean removeTouchExplorationStateChangeListener(AccessibilityManager manager,
            TouchExplorationStateChangeListener listener) {
        if (Build.VERSION.SDK_INT >= 19) {
            if (listener == null) {
                return false;
            }
            return manager.removeTouchExplorationStateChangeListener(
                    new TouchExplorationStateChangeListenerWrapper(listener));
        } else {
            return false;
        }
    }

    @RequiresApi(19)
    private static class TouchExplorationStateChangeListenerWrapper
            implements AccessibilityManager.TouchExplorationStateChangeListener {
        final TouchExplorationStateChangeListener mListener;

        TouchExplorationStateChangeListenerWrapper(
                @NonNull TouchExplorationStateChangeListener listener) {
            mListener = listener;
        }

        @Override
        public int hashCode() {
            return mListener.hashCode();
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
            return mListener.equals(other.mListener);
        }

        @Override
        public void onTouchExplorationStateChanged(boolean enabled) {
            mListener.onTouchExplorationStateChanged(enabled);
        }
    }

    /**
     * Listener for the accessibility state.
     *
     * @deprecated Use {@link AccessibilityManager.AccessibilityStateChangeListener} directly
     * instead of this listener.
     */
    @Deprecated
    public static abstract class AccessibilityStateChangeListenerCompat
            implements AccessibilityStateChangeListener {
    }

    /**
     * Listener for the accessibility state.
     *
     * @deprecated Use {@link AccessibilityManager.AccessibilityStateChangeListener} directly
     * instead of this listener.
     */
    @Deprecated
    public interface AccessibilityStateChangeListener {
        /**
         * Called back on change in the accessibility state.
         *
         * @param enabled Whether accessibility is enabled.
         *
         * @deprecated Use {@link AccessibilityManager.AccessibilityStateChangeListener} directly.
         */
        @Deprecated
        void onAccessibilityStateChanged(boolean enabled);
    }

    /**
     * Listener for the system touch exploration state. To listen for changes to
     * the touch exploration state on the device, implement this interface and
     * register it with the system by calling
     * {@link #addTouchExplorationStateChangeListener}.
     */
    public interface TouchExplorationStateChangeListener {
        /**
         * Called when the touch exploration enabled state changes.
         *
         * @param enabled Whether touch exploration is enabled.
         */
        void onTouchExplorationStateChanged(boolean enabled);
    }

    private AccessibilityManagerCompat() {}
}
