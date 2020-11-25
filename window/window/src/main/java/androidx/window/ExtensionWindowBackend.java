/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window;

import static androidx.window.ExtensionCompat.DEBUG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Consumer;
import androidx.window.extensions.ExtensionInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Default implementation of {@link WindowBackend} that uses a combination of platform APIs and
 * device-dependent OEM extensions.
 */
final class ExtensionWindowBackend implements WindowBackend {
    private static volatile ExtensionWindowBackend sInstance;
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    @VisibleForTesting
    ExtensionInterfaceCompat mWindowExtension;

    /**
     * List of all registered callbacks for window layout info. Not protected by {@link #sLock} to
     * allow iterating and callback execution without holding the global lock.
     */
    @VisibleForTesting
    final List<WindowLayoutChangeCallbackWrapper> mWindowLayoutChangeCallbacks =
            new CopyOnWriteArrayList<>();

    /**
     * List of all registered callbacks for window layout info. Not protected by {@link #sLock} to
     * allow iterating and callback execution without holding the global lock.
     */
    @VisibleForTesting
    final List<DeviceStateChangeCallbackWrapper> mDeviceStateChangeCallbacks =
            new CopyOnWriteArrayList<>();

    /** Device state that was last reported through callbacks, used to filter out duplicates. */
    @GuardedBy("sLock")
    @VisibleForTesting
    DeviceState mLastReportedDeviceState;

    /** Window layouts that were last reported through callbacks, used to filter out duplicates. */
    @GuardedBy("sLock")
    @VisibleForTesting
    final Map<Activity, WindowLayoutInfo> mLastReportedWindowLayouts = new WeakHashMap<>();

    private static final String TAG = "WindowServer";

    private ExtensionWindowBackend() {
        // Empty
    }

    /**
     * Gets the shared instance of the class.
     */
    @NonNull
    public static ExtensionWindowBackend getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new ExtensionWindowBackend();
                    sInstance.initExtension(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    /** Tries to initialize Extension, returns early if it's not available. */
    @SuppressLint("SyntheticAccessor")
    @GuardedBy("sLock")
    private void initExtension(Context context) {
        mWindowExtension = initAndVerifyExtension(context);
        if (mWindowExtension == null) {
            return;
        }
        mWindowExtension.setExtensionCallback(new ExtensionListenerImpl());
    }

    @Override
    public void registerLayoutChangeCallback(@NonNull Activity activity,
            @NonNull Executor executor, @NonNull Consumer<WindowLayoutInfo> callback) {
        synchronized (sLock) {
            if (mWindowExtension == null) {
                if (DEBUG) {
                    Log.v(TAG, "Extension not loaded, skipping callback registration.");
                }
                return;
            }

            // Check if the activity was already registered, in case we need to report tracking of a
            // new activity to the extension.
            boolean isActivityRegistered = isActivityRegistered(activity);

            WindowLayoutChangeCallbackWrapper callbackWrapper =
                    new WindowLayoutChangeCallbackWrapper(activity, executor, callback);
            mWindowLayoutChangeCallbacks.add(callbackWrapper);
            if (!isActivityRegistered) {
                mWindowExtension.onWindowLayoutChangeListenerAdded(activity);
            }
            WindowLayoutInfo lastReportedValue = mLastReportedWindowLayouts.get(activity);
            if (lastReportedValue != null) {
                callbackWrapper.accept(lastReportedValue);
            }
        }
    }

    private boolean isActivityRegistered(@NonNull Activity activity) {
        for (WindowLayoutChangeCallbackWrapper callbackWrapper : mWindowLayoutChangeCallbacks) {
            if (callbackWrapper.mActivity.equals(activity)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void unregisterLayoutChangeCallback(@NonNull Consumer<WindowLayoutInfo> callback) {
        synchronized (sLock) {
            if (mWindowExtension == null) {
                if (DEBUG) {
                    Log.v(TAG, "Extension not loaded, skipping callback un-registration.");
                }
                return;
            }

            // The same callback may be registered for multiple different window tokens, and
            // vice-versa. First collect all items to be removed.
            List<WindowLayoutChangeCallbackWrapper> itemsToRemove = new ArrayList<>();
            for (WindowLayoutChangeCallbackWrapper callbackWrapper : mWindowLayoutChangeCallbacks) {
                Consumer<WindowLayoutInfo> registeredCallback = callbackWrapper.mCallback;
                if (registeredCallback == callback) {
                    itemsToRemove.add(callbackWrapper);
                }
            }
            // Remove the items from the list and notify extension if needed.
            mWindowLayoutChangeCallbacks.removeAll(itemsToRemove);
            for (WindowLayoutChangeCallbackWrapper callbackWrapper : itemsToRemove) {
                callbackRemovedForActivity(callbackWrapper.mActivity);
            }
        }
    }

    /**
     * Checks if there are no more registered callbacks left for the activity and inform
     * extension if needed.
     */
    @GuardedBy("sLock")
    private void callbackRemovedForActivity(Activity activity) {
        for (WindowLayoutChangeCallbackWrapper callbackWrapper : mWindowLayoutChangeCallbacks) {
            if (callbackWrapper.mActivity.equals(activity)) {
                // Found a registered callback for token.
                return;
            }
        }
        // No registered callbacks left for the activity - report to extension.
        mWindowExtension.onWindowLayoutChangeListenerRemoved(activity);
    }

    @Override
    public void registerDeviceStateChangeCallback(@NonNull Executor executor,
            @NonNull Consumer<DeviceState> callback) {
        synchronized (sLock) {
            final DeviceStateChangeCallbackWrapper callbackWrapper =
                    new DeviceStateChangeCallbackWrapper(executor, callback);
            if (mWindowExtension == null) {
                if (DEBUG) {
                    Log.d(TAG, "Extension not loaded, skipping callback registration.");
                }
                callback.accept(new DeviceState(DeviceState.POSTURE_UNKNOWN));
                return;
            }

            if (mDeviceStateChangeCallbacks.isEmpty()) {
                mWindowExtension.onDeviceStateListenersChanged(false /* isEmpty */);
            }

            mDeviceStateChangeCallbacks.add(callbackWrapper);
            if (mLastReportedDeviceState != null) {
                callbackWrapper.accept(mLastReportedDeviceState);
            }
        }
    }

    @Override
    public void unregisterDeviceStateChangeCallback(@NonNull Consumer<DeviceState> callback) {
        synchronized (sLock) {
            if (mWindowExtension == null) {
                if (DEBUG) {
                    Log.d(TAG, "Extension not loaded, skipping callback un-registration.");
                }
                return;
            }

            for (DeviceStateChangeCallbackWrapper callbackWrapper : mDeviceStateChangeCallbacks) {
                if (callbackWrapper.mCallback.equals(callback)) {
                    mDeviceStateChangeCallbacks.remove(callbackWrapper);
                    if (mDeviceStateChangeCallbacks.isEmpty()) {
                        mWindowExtension.onDeviceStateListenersChanged(true /* isEmpty */);
                        // Clear device state so we do not replay stale data.
                        mLastReportedDeviceState = null;
                    }
                    return;
                }
            }
        }
    }

    @VisibleForTesting
    class ExtensionListenerImpl implements ExtensionInterfaceCompat.ExtensionCallbackInterface {
        @Override
        @SuppressLint("SyntheticAccessor")
        public void onDeviceStateChanged(@NonNull DeviceState newDeviceState) {
            synchronized (sLock) {
                if (newDeviceState.equals(mLastReportedDeviceState)) {
                    // Skipping, value already reported
                    if (DEBUG) {
                        Log.w(TAG, "Extension reported old layout value");
                    }
                    return;
                }
                mLastReportedDeviceState = newDeviceState;
            }

            for (DeviceStateChangeCallbackWrapper callbackWrapper : mDeviceStateChangeCallbacks) {
                callbackWrapper.accept(newDeviceState);
            }
        }

        @Override
        @SuppressLint("SyntheticAccessor")
        public void onWindowLayoutChanged(@NonNull Activity activity,
                @NonNull WindowLayoutInfo newLayout) {
            synchronized (sLock) {
                WindowLayoutInfo lastReportedValue = mLastReportedWindowLayouts.get(activity);
                if (newLayout.equals(lastReportedValue)) {
                    // Skipping, value already reported
                    if (DEBUG) {
                        Log.w(TAG, "Extension reported an old layout value");
                    }
                    return;
                }
                mLastReportedWindowLayouts.put(activity, newLayout);
            }

            for (WindowLayoutChangeCallbackWrapper callbackWrapper : mWindowLayoutChangeCallbacks) {
                if (!callbackWrapper.mActivity.equals(activity)) {
                    continue;
                }

                callbackWrapper.accept(newLayout);
            }
        }
    }

    /**
     * Wrapper around {@link Consumer<WindowLayoutInfo>} that also includes the {@link Executor}
     * on which the callback should run and the {@link Activity}.
     */
    private static class WindowLayoutChangeCallbackWrapper {
        final Executor mExecutor;
        final Consumer<WindowLayoutInfo> mCallback;
        final Activity mActivity;

        WindowLayoutChangeCallbackWrapper(@NonNull Activity activity, @NonNull Executor executor,
                @NonNull Consumer<WindowLayoutInfo> callback) {
            mActivity = activity;
            mExecutor = executor;
            mCallback = callback;
        }

        void accept(WindowLayoutInfo layoutInfo) {
            mExecutor.execute(() -> mCallback.accept(layoutInfo));
        }
    }

    /**
     * Wrapper around {@link Consumer<DeviceState>} that also includes the {@link Executor} on
     * which the callback should run.
     */
    private static class DeviceStateChangeCallbackWrapper {
        final Executor mExecutor;
        final Consumer<DeviceState> mCallback;

        DeviceStateChangeCallbackWrapper(@NonNull Executor executor,
                @NonNull Consumer<DeviceState> callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        void accept(DeviceState state) {
            mExecutor.execute(() -> mCallback.accept(state));
        }
    }

    /**
     * Loads an instance of {@link ExtensionInterface} implemented by OEM if available on this
     * device. This also verifies if the loaded implementation conforms to the declared API version.
     */
    @Nullable
    static ExtensionInterfaceCompat initAndVerifyExtension(Context context) {
        ExtensionInterfaceCompat impl = null;
        try {
            if (isExtensionVersionSupported(ExtensionCompat.getExtensionVersion())) {
                impl = new ExtensionCompat(context);
                if (!impl.validateExtensionInterface()) {
                    if (DEBUG) {
                        Log.d(TAG, "Loaded extension doesn't match the interface version");
                    }
                    impl = null;
                }
            }
        } catch (Throwable t) {
            if (DEBUG) {
                Log.d(TAG, "Failed to load extension: " + t);
            }
            impl = null;
        }

        if (impl == null) {
            // Falling back to Sidecar
            try {
                if (isExtensionVersionSupported(SidecarCompat.getSidecarVersion())) {
                    impl = new SidecarCompat(context);
                    if (!impl.validateExtensionInterface()) {
                        if (DEBUG) {
                            Log.d(TAG, "Loaded Sidecar doesn't match the interface version");
                        }
                        impl = null;
                    }
                }
            } catch (Throwable t) {
                if (DEBUG) {
                    Log.d(TAG, "Failed to load sidecar: " + t);
                }
                impl = null;
            }
        }

        if (impl == null) {
            if (DEBUG) {
                Log.d(TAG, "No supported extension or sidecar found");
            }
        }

        return impl;
    }

    /**
     * Checks if the Extension version provided on this device is supported by the current version
     * of the library.
     */
    @VisibleForTesting
    static boolean isExtensionVersionSupported(@Nullable Version extensionVersion) {
        if (extensionVersion == null) {
            return false;
        }
        if (extensionVersion.getMajor() == 1) {
            // Disable androidx.window.extensions support in release builds of the library until the
            // extensions API is finalized.
            return DEBUG;
        }
        return Version.CURRENT.getMajor() >= extensionVersion.getMajor();
    }

    /**
     * Test-only affordance to forget the existing instance.
     */
    @VisibleForTesting
    static void resetInstance() {
        sInstance = null;
    }
}
