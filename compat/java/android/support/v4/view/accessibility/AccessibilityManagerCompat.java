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
import android.os.Build;
import android.support.v4.view.accessibility.AccessibilityManagerCompatIcs.AccessibilityStateChangeListenerBridge;
import android.support.v4.view.accessibility.AccessibilityManagerCompatIcs.AccessibilityStateChangeListenerWrapper;
import android.support.v4.view.accessibility.AccessibilityManagerCompatKitKat.TouchExplorationStateChangeListenerBridge;
import android.support.v4.view.accessibility.AccessibilityManagerCompatKitKat.TouchExplorationStateChangeListenerWrapper;
import android.view.accessibility.AccessibilityManager;

import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing features in {@link AccessibilityManager}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class AccessibilityManagerCompat {

    interface AccessibilityManagerVersionImpl {
        AccessibilityStateChangeListenerWrapper newAccessibilityStateChangeListener(
                AccessibilityStateChangeListener listener);
        boolean addAccessibilityStateChangeListener(AccessibilityManager manager,
                AccessibilityStateChangeListener listener);
        boolean removeAccessibilityStateChangeListener(AccessibilityManager manager,
                AccessibilityStateChangeListener listener);
        List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(
                AccessibilityManager manager,int feedbackTypeFlags);
        List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(
                AccessibilityManager manager);
        boolean isTouchExplorationEnabled(AccessibilityManager manager);
        TouchExplorationStateChangeListenerWrapper newTouchExplorationStateChangeListener(
                TouchExplorationStateChangeListener listener);
        boolean addTouchExplorationStateChangeListener(AccessibilityManager manager,
                TouchExplorationStateChangeListener listener);
        boolean removeTouchExplorationStateChangeListener(AccessibilityManager manager,
                TouchExplorationStateChangeListener listener);
    }

    static class AccessibilityManagerStubImpl implements AccessibilityManagerVersionImpl {
        @Override
        public AccessibilityStateChangeListenerWrapper newAccessibilityStateChangeListener(
                AccessibilityStateChangeListener listener) {
            return null;
        }

        @Override
        public boolean addAccessibilityStateChangeListener(AccessibilityManager manager,
                AccessibilityStateChangeListener listener) {
            return false;
        }

        @Override
        public boolean removeAccessibilityStateChangeListener(AccessibilityManager manager,
                AccessibilityStateChangeListener listener) {
            return false;
        }

        @Override
        public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(
                AccessibilityManager manager, int feedbackTypeFlags) {
            return Collections.emptyList();
        }

        @Override
        public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(
                AccessibilityManager manager) {
            return Collections.emptyList();
        }

        @Override
        public boolean isTouchExplorationEnabled(AccessibilityManager manager) {
            return false;
        }

        @Override
        public TouchExplorationStateChangeListenerWrapper newTouchExplorationStateChangeListener(
                TouchExplorationStateChangeListener listener) {
            return null;
        }

        @Override
        public boolean addTouchExplorationStateChangeListener(AccessibilityManager manager,
                TouchExplorationStateChangeListener listener) {
            return false;
        }

        @Override
        public boolean removeTouchExplorationStateChangeListener(AccessibilityManager manager,
                TouchExplorationStateChangeListener listener) {
            return false;
        }
    }

    static class AccessibilityManagerIcsImpl extends AccessibilityManagerStubImpl {
        @Override
        public AccessibilityStateChangeListenerWrapper newAccessibilityStateChangeListener(
                final AccessibilityStateChangeListener listener) {
            return new AccessibilityStateChangeListenerWrapper(listener,
                    new AccessibilityStateChangeListenerBridge() {
                        @Override
                        public void onAccessibilityStateChanged(boolean enabled) {
                            listener.onAccessibilityStateChanged(enabled);
                        }
                    });
        }

        @Override
        public boolean addAccessibilityStateChangeListener(AccessibilityManager manager,
                AccessibilityStateChangeListener listener) {
            return AccessibilityManagerCompatIcs.addAccessibilityStateChangeListener(manager,
                    newAccessibilityStateChangeListener(listener));
        }

        @Override
        public boolean removeAccessibilityStateChangeListener(AccessibilityManager manager,
                AccessibilityStateChangeListener listener) {
            return AccessibilityManagerCompatIcs.removeAccessibilityStateChangeListener(manager,
                    newAccessibilityStateChangeListener(listener));
        }

        @Override
        public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(
                AccessibilityManager manager, int feedbackTypeFlags) {
            return AccessibilityManagerCompatIcs.getEnabledAccessibilityServiceList(manager,
                    feedbackTypeFlags);
        }

        @Override
        public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(
                AccessibilityManager manager) {
            return AccessibilityManagerCompatIcs.getInstalledAccessibilityServiceList(manager);
        }

        @Override
        public boolean isTouchExplorationEnabled(AccessibilityManager manager) {
            return AccessibilityManagerCompatIcs.isTouchExplorationEnabled(manager);
        }
    }

    static class AccessibilityManagerKitKatImpl extends AccessibilityManagerIcsImpl {
        @Override
        public TouchExplorationStateChangeListenerWrapper newTouchExplorationStateChangeListener(
                final TouchExplorationStateChangeListener listener) {
            return new TouchExplorationStateChangeListenerWrapper(listener,
                    new TouchExplorationStateChangeListenerBridge() {
                        @Override
                        public void onTouchExplorationStateChanged(boolean enabled) {
                            listener.onTouchExplorationStateChanged(enabled);
                        }
                    });
        }

        @Override
        public boolean addTouchExplorationStateChangeListener(AccessibilityManager manager,
                TouchExplorationStateChangeListener listener) {
            return AccessibilityManagerCompatKitKat.addTouchExplorationStateChangeListener(
                    manager, newTouchExplorationStateChangeListener(listener));
        }

        @Override
        public boolean removeTouchExplorationStateChangeListener(AccessibilityManager manager,
                TouchExplorationStateChangeListener listener) {
            return AccessibilityManagerCompatKitKat.removeTouchExplorationStateChangeListener(
                    manager, newTouchExplorationStateChangeListener(listener));
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 19) { // KitKat
            IMPL = new AccessibilityManagerKitKatImpl();
        } else if (Build.VERSION.SDK_INT >= 14) { // ICS
            IMPL = new AccessibilityManagerIcsImpl();
        } else {
            IMPL = new AccessibilityManagerStubImpl();
        }
    }

    private static final AccessibilityManagerVersionImpl IMPL;

    /**
     * Registers an {@link AccessibilityManager.AccessibilityStateChangeListener} for changes in
     * the global accessibility state of the system.
     *
     * @param manager The accessibility manager.
     * @param listener The listener.
     * @return True if successfully registered.
     */
    public static boolean addAccessibilityStateChangeListener(AccessibilityManager manager,
            AccessibilityStateChangeListener listener) {
        return IMPL.addAccessibilityStateChangeListener(manager, listener);
    }

    /**
     * Unregisters an {@link AccessibilityManager.AccessibilityStateChangeListener}.
     *
     * @param manager The accessibility manager.
     * @param listener The listener.
     * @return True if successfully unregistered.
     */
    public static boolean removeAccessibilityStateChangeListener(AccessibilityManager manager,
            AccessibilityStateChangeListener listener) {
        return IMPL.removeAccessibilityStateChangeListener(manager, listener);
    }

    /**
     * Returns the {@link AccessibilityServiceInfo}s of the installed accessibility services.
     *
     * @param manager The accessibility manager.
     * @return An unmodifiable list with {@link AccessibilityServiceInfo}s.
     */
    public static List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(
            AccessibilityManager manager) {
        return IMPL.getInstalledAccessibilityServiceList(manager);
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
     */
    public static List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(
            AccessibilityManager manager, int feedbackTypeFlags) {
        return IMPL.getEnabledAccessibilityServiceList(manager, feedbackTypeFlags);
    }

    /**
     * Returns if the touch exploration in the system is enabled.
     *
     * @param manager The accessibility manager.
     * @return True if touch exploration is enabled, false otherwise.
     */
    public static boolean isTouchExplorationEnabled(AccessibilityManager manager) {
        return IMPL.isTouchExplorationEnabled(manager);
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
        return IMPL.addTouchExplorationStateChangeListener(manager, listener);
    }

    /**
     * Unregisters a {@link TouchExplorationStateChangeListener}.
     *
     * @param listener The listener.
     * @return True if successfully unregistered.
     */
    public static boolean removeTouchExplorationStateChangeListener(AccessibilityManager manager,
            TouchExplorationStateChangeListener listener) {
        return IMPL.removeTouchExplorationStateChangeListener(manager, listener);
    }

    /**
     * Listener for the accessibility state.
     * @deprecated Use {@link AccessibilityStateChangeListener} instead.
     */
    @Deprecated
    public static abstract class AccessibilityStateChangeListenerCompat
            implements AccessibilityStateChangeListener {
    }

    /**
     * Listener for the accessibility state.
     */
    public interface AccessibilityStateChangeListener {
        /**
         * Called back on change in the accessibility state.
         *
         * @param enabled Whether accessibility is enabled.
         */
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
