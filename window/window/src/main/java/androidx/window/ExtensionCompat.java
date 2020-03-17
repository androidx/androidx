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

import static androidx.window.DeviceState.POSTURE_MAX_KNOWN;
import static androidx.window.DeviceState.POSTURE_UNKNOWN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionInterface;
import androidx.window.extensions.ExtensionProvider;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Compatibility wrapper for extension versions v1.0+. */
final class ExtensionCompat implements ExtensionInterfaceCompat {
    static final boolean DEBUG = false;
    private static final String TAG = "ExtensionVersionCompat";

    @VisibleForTesting
    final ExtensionInterface mWindowExtension;

    ExtensionCompat(Context context) {
        this(ExtensionProvider.getExtensionImpl(context));
        if (mWindowExtension == null) {
            throw new IllegalArgumentException("Extension provider returned null");
        }
    }

    @VisibleForTesting
    ExtensionCompat(ExtensionInterface extension) {
        mWindowExtension = extension;
    }

    @Override
    public void setExtensionCallback(@NonNull ExtensionCallbackInterface extensionCallback) {
        mWindowExtension.setExtensionCallback(new ExtensionInterface.ExtensionCallback() {
            @Override
            @SuppressLint("SyntheticAccessor")
            public void onDeviceStateChanged(@NonNull ExtensionDeviceState newDeviceState) {
                extensionCallback.onDeviceStateChanged(deviceStateFromExtension(newDeviceState));
            }

            @Override
            @SuppressLint("SyntheticAccessor")
            public void onWindowLayoutChanged(@NonNull IBinder windowToken,
                    @NonNull ExtensionWindowLayoutInfo newLayout) {
                extensionCallback.onWindowLayoutChanged(windowToken,
                        windowLayoutInfoFromExtension(newLayout));
            }
        });
    }

    @NonNull
    @Override
    public WindowLayoutInfo getWindowLayoutInfo(@NonNull IBinder windowToken) {
        ExtensionWindowLayoutInfo windowLayoutInfo =
                mWindowExtension.getWindowLayoutInfo(windowToken);
        return windowLayoutInfoFromExtension(windowLayoutInfo);
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull IBinder windowToken) {
        mWindowExtension.onWindowLayoutChangeListenerAdded(windowToken);
    }

    @Override
    public void onWindowLayoutChangeListenerRemoved(@NonNull IBinder windowToken) {
        mWindowExtension.onWindowLayoutChangeListenerRemoved(windowToken);
    }

    @NonNull
    @Override
    public DeviceState getDeviceState() {
        ExtensionDeviceState deviceState = mWindowExtension.getDeviceState();
        return deviceStateFromExtension(deviceState);
    }

    @Override
    public void onDeviceStateListenersChanged(boolean isEmpty) {
        mWindowExtension.onDeviceStateListenersChanged(isEmpty);
    }

    @Nullable
    static Version getExtensionVersion() {
        try {
            String vendorVersion = ExtensionProvider.getApiVersion();
            return !TextUtils.isEmpty(vendorVersion) ? Version.parse(vendorVersion) : null;
        } catch (NoClassDefFoundError e) {
            if (DEBUG) {
                Log.d(TAG, "Extension version not found");
            }
            return null;
        } catch (UnsupportedOperationException e) {
            if (DEBUG) {
                Log.d(TAG, "Stub Extension");
            }
            return null;
        }
    }

    /**
     * Converts the display feature from extension. Can return {@code null} if there is an issue
     * with the value passed from extension.
     */
    @Nullable
    private static DisplayFeature displayFeatureFromExtension(ExtensionDisplayFeature feature) {
        if (feature.getBounds().width() == 0 && feature.getBounds().height() == 0) {
            if (DEBUG) {
                Log.d(TAG, "Passed a display feature with empty rect, skipping: " + feature);
            }
            return null;
        }
        return new DisplayFeature(feature.getBounds(), feature.getType());
    }

    @NonNull
    private static List<DisplayFeature> displayFeatureListFromExtension(
            ExtensionWindowLayoutInfo extensionWindowLayoutInfo) {
        List<DisplayFeature> displayFeatures = new ArrayList<>();
        List<ExtensionDisplayFeature> extensionFeatures =
                extensionWindowLayoutInfo.getDisplayFeatures();
        if (extensionFeatures == null) {
            return displayFeatures;
        }

        for (ExtensionDisplayFeature extensionFeature : extensionFeatures) {
            final DisplayFeature displayFeature = displayFeatureFromExtension(extensionFeature);
            if (displayFeature != null) {
                displayFeatures.add(displayFeature);
            }
        }
        return displayFeatures;
    }

    @NonNull
    private static WindowLayoutInfo windowLayoutInfoFromExtension(
            @Nullable ExtensionWindowLayoutInfo extensionInfo) {
        if (extensionInfo == null) {
            return new WindowLayoutInfo(new ArrayList<>());
        }

        List<DisplayFeature> displayFeatures = displayFeatureListFromExtension(extensionInfo);
        return new WindowLayoutInfo(displayFeatures);
    }

    @DeviceState.Posture
    private static int postureFromExtension(ExtensionDeviceState extensionDeviceState) {
        int extensionPosture = extensionDeviceState.getPosture();
        if (extensionPosture > POSTURE_MAX_KNOWN) {
            if (DEBUG) {
                Log.d(TAG, "Unknown posture reported, WindowManager library should be updated");
            }
            return POSTURE_UNKNOWN;
        }
        return extensionPosture;
    }

    @NonNull
    private static DeviceState deviceStateFromExtension(
            @Nullable ExtensionDeviceState extensionDeviceState) {
        if (extensionDeviceState == null) {
            return new DeviceState(POSTURE_UNKNOWN);
        }

        int posture = postureFromExtension(extensionDeviceState);
        return new DeviceState(posture);
    }

    /** Verifies that extension implementation corresponds to the interface of the version. */
    @Override
    @SuppressWarnings("unused")
    public boolean validateExtensionInterface() {
        try {
            // extension.setExtensionCallback(ExtensionInterface.ExtensionCallback);
            Method methodSetExtensionCallback = mWindowExtension.getClass().getMethod(
                    "setExtensionCallback", ExtensionInterface.ExtensionCallback.class);
            Class<?> rSetExtensionCallback = methodSetExtensionCallback.getReturnType();
            if (!rSetExtensionCallback.equals(void.class)) {
                throw new NoSuchMethodException("Illegal return type for 'setExtensionCallback': "
                        + rSetExtensionCallback);
            }

            // extension.getDeviceState()
            ExtensionDeviceState tmpDeviceState = mWindowExtension.getDeviceState();

            // extension.onDeviceStateListenersChanged(boolean);
            mWindowExtension.onDeviceStateListenersChanged(true /* empty */);

            // extension.getWindowLayoutInfo(IBinder)
            Method methodGetWindowLayoutInfo = mWindowExtension.getClass()
                    .getMethod("getWindowLayoutInfo", IBinder.class);
            Class<?> rtGetWindowLayoutInfo = methodGetWindowLayoutInfo.getReturnType();
            if (!rtGetWindowLayoutInfo.equals(ExtensionWindowLayoutInfo.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'getWindowLayoutInfo': "
                                + rtGetWindowLayoutInfo);
            }

            // extension.onWindowLayoutChangeListenerAdded(IBinder);
            Method methodRegisterWindowLayoutChangeListener = mWindowExtension.getClass()
                    .getMethod("onWindowLayoutChangeListenerAdded", IBinder.class);
            Class<?> rtRegisterWindowLayoutChangeListener =
                    methodRegisterWindowLayoutChangeListener.getReturnType();
            if (!rtRegisterWindowLayoutChangeListener.equals(void.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'onWindowLayoutChangeListenerAdded': "
                                + rtRegisterWindowLayoutChangeListener);
            }

            // extension.onWindowLayoutChangeListenerRemoved(IBinder);
            Method methodUnregisterWindowLayoutChangeListener = mWindowExtension.getClass()
                    .getMethod("onWindowLayoutChangeListenerRemoved", IBinder.class);
            Class<?> rtUnregisterWindowLayoutChangeListener =
                    methodUnregisterWindowLayoutChangeListener.getReturnType();
            if (!rtUnregisterWindowLayoutChangeListener.equals(void.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'onWindowLayoutChangeListenerRemoved': "
                                + rtUnregisterWindowLayoutChangeListener);
            }

            // ExtensionDeviceState constructor
            ExtensionDeviceState deviceState = new ExtensionDeviceState(
                    ExtensionDeviceState.POSTURE_UNKNOWN);

            // deviceState.getPosture();
            int tmpPosture = deviceState.getPosture();

            // ExtensionDisplayFeature constructor
            ExtensionDisplayFeature displayFeature =
                    new ExtensionDisplayFeature(new Rect(0, 0, 0, 0),
                            ExtensionDisplayFeature.TYPE_FOLD);

            // displayFeature.getBounds()
            Rect tmpRect = displayFeature.getBounds();

            // displayFeature.getType()
            int tmpType = displayFeature.getType();

            // ExtensionWindowLayoutInfo constructor
            ExtensionWindowLayoutInfo windowLayoutInfo =
                    new ExtensionWindowLayoutInfo(new ArrayList<>());

            // windowLayoutInfo.getDisplayFeatures()
            List<ExtensionDisplayFeature> tmpDisplayFeatures =
                    windowLayoutInfo.getDisplayFeatures();
            return true;
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "Extension implementation doesn't conform to interface version "
                        + getExtensionVersion() + ", error: " + e);
            }
            return false;
        }
    }
}
