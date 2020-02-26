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

import static androidx.window.ExtensionHelper.DEBUG;
import static androidx.window.WindowManager.getActivityFromContext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Default implementation of {@link WindowBackend} that uses a combination of platform APIs and
 * device-dependent OEM extensions.
 */
public final class ExtensionWindowBackend implements WindowBackend {
    private static volatile ExtensionWindowBackend sInstance;
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private ExtensionInterfaceCompat mWindowExtension;
    /**
     * List of all registered callbacks for window layout info. Not protected by {@link #sLock} to
     * allow iterating and callback execution without holding the global lock.
     */
    private final List<WindowLayoutChangeCallbackWrapper> mWindowLayoutChangeCallbacks =
            new CopyOnWriteArrayList<>();
    /**
     * List of all registered callbacks for window layout info. Not protected by {@link #sLock} to
     * allow iterating and callback execution without holding the global lock.
     */
    private final List<DeviceStateChangeCallbackWrapper> mDeviceStateChangeCallbacks =
            new CopyOnWriteArrayList<>();
    /** Device state that was last reported through callbacks, used to filter out duplicates. */
    @GuardedBy("sLock")
    private DeviceState mLastReportedDeviceState;
    /** Window layouts that were last reported through callbacks, used to filter out duplicates. */
    @GuardedBy("sLock")
    private final HashMap<IBinder, WindowLayoutInfo> mLastReportedWindowLayouts =
            new HashMap<>();

    private static final String TAG = "WindowServer";

    private ExtensionWindowBackend() {
        // Empty
    }

    /**
     * Get the shared instance of the class.
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

    /** Try to initialize Extension, returns early if it's not available. */
    @SuppressLint("SyntheticAccessor")
    @GuardedBy("sLock")
    private void initExtension(Context context) {
        mWindowExtension = ExtensionHelper.getExtensionImpl(context);
        if (mWindowExtension == null) {
            return;
        }
        mWindowExtension.setExtensionCallback(new ExtensionListenerImpl());
    }

    @NonNull
    @Override
    public WindowLayoutInfo getWindowLayoutInfo(@NonNull Context context) {
        Activity activity = assertActivityContext(context);
        IBinder windowToken = getActivityWindowToken(activity);
        if (windowToken == null) {
            throw new IllegalStateException("Activity does not have a window attached.");
        }

        synchronized (sLock) {
            WindowLayoutInfo extensionWindowLayoutInfo = mWindowExtension != null
                    ? mWindowExtension.getWindowLayoutInfo(windowToken)
                    : new WindowLayoutInfo(new ArrayList<>());
            mLastReportedWindowLayouts.put(windowToken, extensionWindowLayoutInfo);
            return extensionWindowLayoutInfo;
        }
    }

    @NonNull
    @Override
    public DeviceState getDeviceState() {
        synchronized (sLock) {
            return mWindowExtension != null ? mWindowExtension.getDeviceState() :
                    new DeviceState(DeviceState.POSTURE_UNKNOWN);
        }
    }

    @Override
    public void registerLayoutChangeCallback(@NonNull Context context,
            @NonNull Executor executor, @NonNull Consumer<WindowLayoutInfo> callback) {
        synchronized (sLock) {
            if (mWindowExtension == null) {
                if (DEBUG) {
                    Log.v(TAG, "Extension not loaded, skipping callback registration.");
                }
                return;
            }

            Activity activity = assertActivityContext(context);
            IBinder windowToken = getActivityWindowToken(activity);
            if (windowToken == null) {
                throw new IllegalStateException("Activity does not have a window attached.");
            }

            // Check if the token was already registered, in case we need to report tracking of a
            // new token to the extension.
            boolean registeredToken = false;
            for (WindowLayoutChangeCallbackWrapper callbackWrapper : mWindowLayoutChangeCallbacks) {
                if (callbackWrapper.mToken.equals(windowToken)) {
                    registeredToken = true;
                    break;
                }
            }

            final WindowLayoutChangeCallbackWrapper callbackWrapper =
                    new WindowLayoutChangeCallbackWrapper(windowToken, executor, callback);
            mWindowLayoutChangeCallbacks.add(callbackWrapper);
            if (!registeredToken) {
                // Added the first callback for the token.
                mWindowExtension.onWindowLayoutChangeListenerAdded(windowToken);
            }
        }
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
                callbackRemovedForToken(callbackWrapper.mToken);
            }
        }
    }

    /**
     * Check if there are no more registered callbacks left for the token and inform extension if
     * needed.
     */
    @GuardedBy("sLock")
    private void callbackRemovedForToken(IBinder token) {
        for (WindowLayoutChangeCallbackWrapper callbackWrapper : mWindowLayoutChangeCallbacks) {
            if (callbackWrapper.mToken.equals(token)) {
                // Found a registered callback for token.
                return;
            }
        }
        // No registered callbacks left for token - report to extension.
        mWindowExtension.onWindowLayoutChangeListenerRemoved(token);
    }

    @Override
    public void registerDeviceStateChangeCallback(@NonNull Executor executor,
            @NonNull Consumer<DeviceState> callback) {
        synchronized (sLock) {
            if (mWindowExtension == null) {
                if (DEBUG) {
                    Log.d(TAG, "Extension not loaded, skipping callback registration.");
                }
                return;
            }

            if (mDeviceStateChangeCallbacks.isEmpty()) {
                mWindowExtension.onDeviceStateListenersChanged(false /* isEmpty */);
            }

            final DeviceStateChangeCallbackWrapper callbackWrapper =
                    new DeviceStateChangeCallbackWrapper(executor, callback);
            mDeviceStateChangeCallbacks.add(callbackWrapper);
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
                    }
                    return;
                }
            }
        }
    }

    private class ExtensionListenerImpl implements
            ExtensionInterfaceCompat.ExtensionCallbackInterface {
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
                callbackWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callbackWrapper.mCallback.accept(newDeviceState);
                    }
                });
            }
        }

        @Override
        @SuppressLint("SyntheticAccessor")
        public void onWindowLayoutChanged(@NonNull IBinder windowToken,
                @NonNull WindowLayoutInfo newLayout) {
            synchronized (sLock) {
                WindowLayoutInfo lastReportedValue = mLastReportedWindowLayouts.get(windowToken);
                if (newLayout.equals(lastReportedValue)) {
                    // Skipping, value already reported
                    if (DEBUG) {
                        Log.w(TAG, "Extension reported an old layout value");
                    }
                    return;
                }
                mLastReportedWindowLayouts.put(windowToken, newLayout);
            }

            for (WindowLayoutChangeCallbackWrapper callbackWrapper : mWindowLayoutChangeCallbacks) {
                if (!callbackWrapper.mToken.equals(windowToken)) {
                    continue;
                }

                callbackWrapper.mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callbackWrapper.mCallback.accept(newLayout);
                    }
                });
            }
        }
    }

    private Activity assertActivityContext(Context context) {
        Activity activity = getActivityFromContext(context);
        if (activity == null) {
            throw new IllegalArgumentException("Used non-visual Context with WindowManager. "
                    + "Please use an Activity or a ContextWrapper around an Activity instead.");
        }
        return activity;
    }

    private IBinder getActivityWindowToken(Activity activity) {
        return activity.getWindow().getAttributes().token;
    }

    /**
     * Wrapper around {@link Consumer<WindowLayoutInfo>} that also includes the {@link Executor}
     * on which the callback should run and the associated token.
     */
    private static class WindowLayoutChangeCallbackWrapper {
        final Executor mExecutor;
        final Consumer<WindowLayoutInfo> mCallback;
        final IBinder mToken;

        WindowLayoutChangeCallbackWrapper(@NonNull IBinder token, @NonNull Executor executor,
                @NonNull Consumer<WindowLayoutInfo> callback) {
            mToken = token;
            mExecutor = executor;
            mCallback = callback;
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
    }
}
