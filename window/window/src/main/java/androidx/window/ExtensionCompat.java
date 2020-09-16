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
import static androidx.window.WindowManager.getActivityFromContext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
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
            public void onWindowLayoutChanged(@NonNull Context context,
                    @NonNull ExtensionWindowLayoutInfo newLayout) {
                extensionCallback.onWindowLayoutChanged(context,
                        windowLayoutInfoFromExtension(context, newLayout));
            }
        });
    }

    @NonNull
    @Override
    public WindowLayoutInfo getWindowLayoutInfo(@NonNull Context context) {
        ExtensionWindowLayoutInfo windowLayoutInfo =
                mWindowExtension.getWindowLayoutInfo(context);
        return windowLayoutInfoFromExtension(context, windowLayoutInfo);
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull Context context) {
        mWindowExtension.onWindowLayoutChangeListenerAdded(context);
    }

    @Override
    public void onWindowLayoutChangeListenerRemoved(@NonNull Context context) {
        mWindowExtension.onWindowLayoutChangeListenerRemoved(context);
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
    private static DisplayFeature displayFeatureFromExtension(ExtensionDisplayFeature feature,
            Rect windowBounds) {
        Rect bounds = feature.getBounds();
        if (bounds.width() == 0 && bounds.height() == 0) {
            if (DEBUG) {
                Log.d(TAG, "Passed a display feature with empty rect, skipping: " + feature);
            }
            return null;
        }

        if (feature.getType() == ExtensionDisplayFeature.TYPE_FOLD) {
            if (bounds.width() != 0 && bounds.height() != 0) {
                // Bounds for fold types are expected to be zero-wide or zero-high.
                // See DisplayFeature#getBounds().
                if (DEBUG) {
                    Log.d(TAG, "Passed a non-zero area display feature expected to be zero-area, "
                            + "skipping: " + feature);
                }
                return null;
            }
        }

        if (feature.getType() == ExtensionDisplayFeature.TYPE_HINGE
                || feature.getType() == ExtensionDisplayFeature.TYPE_FOLD) {
            if (!((bounds.left == 0 && bounds.right == windowBounds.width())
                    || (bounds.top == 0 && bounds.bottom == windowBounds.height()))) {
                // Bounds for fold and hinge types are expected to span the entire window space.
                // See DisplayFeature#getBounds().
                if (DEBUG) {
                    Log.d(TAG, "Passed a display feature expected to span the entire window but "
                            + "does not, skipping: " + feature);
                }
                return null;
            }
        }

        return new DisplayFeature(feature.getBounds(), feature.getType());
    }

    @NonNull
    private static List<DisplayFeature> displayFeatureListFromExtension(
            ExtensionWindowLayoutInfo extensionWindowLayoutInfo,
            Rect windowBounds) {
        List<DisplayFeature> displayFeatures = new ArrayList<>();
        List<ExtensionDisplayFeature> extensionFeatures =
                extensionWindowLayoutInfo.getDisplayFeatures();
        if (extensionFeatures == null) {
            return displayFeatures;
        }

        for (ExtensionDisplayFeature extensionFeature : extensionFeatures) {
            final DisplayFeature displayFeature = displayFeatureFromExtension(extensionFeature,
                    windowBounds);
            if (displayFeature != null) {
                displayFeatures.add(displayFeature);
            }
        }
        return displayFeatures;
    }

    @NonNull
    private static WindowLayoutInfo windowLayoutInfoFromExtension(
            @NonNull Context context, @Nullable ExtensionWindowLayoutInfo extensionInfo) {
        if (extensionInfo == null) {
            return new WindowLayoutInfo(new ArrayList<>());
        }

        Activity activity = getActivityFromContext(context);
        if (activity == null) {
            throw new IllegalArgumentException("Used non-visual Context with WindowManager. "
                    + "Please use an Activity or a ContextWrapper around an Activity instead.");
        }
        Rect windowBounds = WindowBoundsHelper.getInstance().computeCurrentWindowBounds(activity);
        List<DisplayFeature> displayFeatures = displayFeatureListFromExtension(extensionInfo,
                windowBounds);
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

            // extension.getWindowLayoutInfo(Context)
            Method methodGetWindowLayoutInfo = mWindowExtension.getClass()
                    .getMethod("getWindowLayoutInfo", Context.class);
            Class<?> rtGetWindowLayoutInfo = methodGetWindowLayoutInfo.getReturnType();
            if (!rtGetWindowLayoutInfo.equals(ExtensionWindowLayoutInfo.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'getWindowLayoutInfo': "
                                + rtGetWindowLayoutInfo);
            }

            // extension.onWindowLayoutChangeListenerAdded(Context);
            Method methodRegisterWindowLayoutChangeListener = mWindowExtension.getClass()
                    .getMethod("onWindowLayoutChangeListenerAdded", Context.class);
            Class<?> rtRegisterWindowLayoutChangeListener =
                    methodRegisterWindowLayoutChangeListener.getReturnType();
            if (!rtRegisterWindowLayoutChangeListener.equals(void.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'onWindowLayoutChangeListenerAdded': "
                                + rtRegisterWindowLayoutChangeListener);
            }

            // extension.onWindowLayoutChangeListenerRemoved(Context);
            Method methodUnregisterWindowLayoutChangeListener = mWindowExtension.getClass()
                    .getMethod("onWindowLayoutChangeListenerRemoved", Context.class);
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
