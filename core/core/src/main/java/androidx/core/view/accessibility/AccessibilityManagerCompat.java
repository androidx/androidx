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

import androidx.annotation.DoNotInline;
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
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    @Deprecated
    public static boolean removeAccessibilityStateChangeListener(AccessibilityManager manager,
            AccessibilityStateChangeListener listener) {
        if (listener == null) {
            return false;
        }
        return manager.removeAccessibilityStateChangeListener(
                new AccessibilityStateChangeListenerWrapper(listener));
    }

    @SuppressWarnings("deprecation")
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
            if (!(o instanceof AccessibilityStateChangeListenerWrapper)) {
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
     * @param manager AccessibilityManager for which to add the listener.
     * @param listener The listener.
     * @return True if successfully registered.
     */
    public static boolean addTouchExplorationStateChangeListener(
            @NonNull AccessibilityManager manager,
            @NonNull TouchExplorationStateChangeListener listener) {
        if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.addTouchExplorationStateChangeListenerWrapper(manager, listener);
        } else {
            return false;
        }
    }

    /**
     * Unregisters a {@link TouchExplorationStateChangeListener}.
     *
     * @param manager AccessibilityManager for which to remove the listener.
     * @param listener The listener.
     * @return True if successfully unregistered.
     */
    public static boolean removeTouchExplorationStateChangeListener(
            @NonNull AccessibilityManager manager,
            @NonNull TouchExplorationStateChangeListener listener) {
        if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.removeTouchExplorationStateChangeListenerWrapper(manager, listener);
        } else {
            return false;
        }
    }


    /**
     * Whether the current accessibility request comes from an
     * {@link android.accessibilityservice.AccessibilityService} with the
     * {@link AccessibilityServiceInfo#isAccessibilityTool}
     * property set to true.
     *
     * <p>
     * You can use this method inside {@link android.view.accessibility.AccessibilityNodeProvider}
     * to decide how to populate your nodes.
     * </p>
     *
     * <p>
     * <strong>Note:</strong> The return value is valid only when an
     * {@link android.view.accessibility.AccessibilityNodeInfo} request is in progress, can
     * change from one request to another, and has no meaning when a request is not in progress.
     * </p>
     *
     * @return True if the current request is from a tool that sets isAccessibilityTool.
     */
    public static boolean isRequestFromAccessibilityTool(@NonNull AccessibilityManager manager) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.isRequestFromAccessibilityTool(manager);
        } else {
            // To preserve behavior, assume every service isAccessibilityTool if the system does
            // not support this check.
            return true;
        }
    }

    @RequiresApi(19)
    private static final class TouchExplorationStateChangeListenerWrapper
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
            if (!(o instanceof TouchExplorationStateChangeListenerWrapper)) {
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
    @SuppressWarnings("deprecation")
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

    private AccessibilityManagerCompat() {
    }

    @RequiresApi(34)
    static class Api34Impl {
        private Api34Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isRequestFromAccessibilityTool(AccessibilityManager accessibilityManager) {
            return accessibilityManager.isRequestFromAccessibilityTool();
        }
    }
    @RequiresApi(19)
    static class Api19Impl {
        private Api19Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean addTouchExplorationStateChangeListenerWrapper(
                AccessibilityManager accessibilityManager,
                TouchExplorationStateChangeListener listener) {
            return accessibilityManager.addTouchExplorationStateChangeListener(
                    new TouchExplorationStateChangeListenerWrapper(listener));
        }

        @DoNotInline
        static boolean removeTouchExplorationStateChangeListenerWrapper(
                AccessibilityManager accessibilityManager,
                TouchExplorationStateChangeListener listener) {
            return accessibilityManager.removeTouchExplorationStateChangeListener(
                    new TouchExplorationStateChangeListenerWrapper(listener));
        }
    }
}
