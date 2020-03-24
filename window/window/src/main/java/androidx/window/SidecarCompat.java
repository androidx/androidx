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
import static androidx.window.ExtensionCompat.DEBUG;
import static androidx.window.Version.VERSION_0_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarInterface;
import androidx.window.sidecar.SidecarProvider;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Extension interface compatibility wrapper for v0.1 sidecar. */
@SuppressWarnings("deprecation")
final class SidecarCompat implements ExtensionInterfaceCompat {
    private static final String TAG = "SidecarCompat";

    @VisibleForTesting
    final SidecarInterface mSidecar;

    SidecarCompat(Context context) {
        this(SidecarProvider.getSidecarImpl(context));
        if (mSidecar == null) {
            throw new IllegalArgumentException("Sidecar provider returned null");
        }
    }

    @VisibleForTesting
    SidecarCompat(@NonNull SidecarInterface sidecar) {
        mSidecar = sidecar;
    }

    @Override
    public void setExtensionCallback(@NonNull ExtensionCallbackInterface extensionCallback) {
        mSidecar.setSidecarCallback(new SidecarInterface.SidecarCallback() {
            @Override
            @SuppressLint("SyntheticAccessor")
            public void onDeviceStateChanged(@NonNull SidecarDeviceState newDeviceState) {
                extensionCallback.onDeviceStateChanged(deviceStateFromSidecar(newDeviceState));
            }

            @Override
            @SuppressLint("SyntheticAccessor")
            public void onWindowLayoutChanged(@NonNull IBinder windowToken,
                    @NonNull SidecarWindowLayoutInfo newLayout) {
                extensionCallback.onWindowLayoutChanged(windowToken,
                        windowLayoutInfoFromSidecar(newLayout));
            }
        });
    }

    @NonNull
    @Override
    public WindowLayoutInfo getWindowLayoutInfo(@NonNull IBinder windowToken) {
        SidecarWindowLayoutInfo windowLayoutInfo = mSidecar.getWindowLayoutInfo(windowToken);
        return windowLayoutInfoFromSidecar(windowLayoutInfo);
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull IBinder windowToken) {
        mSidecar.onWindowLayoutChangeListenerAdded(windowToken);
    }

    @Override
    public void onWindowLayoutChangeListenerRemoved(@NonNull IBinder windowToken) {
        mSidecar.onWindowLayoutChangeListenerRemoved(windowToken);
    }

    @NonNull
    @Override
    public DeviceState getDeviceState() {
        SidecarDeviceState deviceState = mSidecar.getDeviceState();
        return deviceStateFromSidecar(deviceState);
    }

    @Override
    public void onDeviceStateListenersChanged(boolean isEmpty) {
        mSidecar.onDeviceStateListenersChanged(isEmpty);
    }

    @Override
    @SuppressWarnings("unused")
    public boolean validateExtensionInterface() {
        try {
            // sidecar.setSidecarCallback(SidecarInterface.SidecarCallback);
            Method methodSetSidecarCallback = mSidecar.getClass().getMethod("setSidecarCallback",
                    SidecarInterface.SidecarCallback.class);
            Class<?> rSetSidecarCallback = methodSetSidecarCallback.getReturnType();
            if (!rSetSidecarCallback.equals(void.class)) {
                throw new NoSuchMethodException("Illegal return type for 'setSidecarCallback': "
                        + rSetSidecarCallback);
            }

            // sidecar.getDeviceState()
            SidecarDeviceState tmpDeviceState = mSidecar.getDeviceState();

            // sidecar.onDeviceStateListenersChanged(boolean);
            mSidecar.onDeviceStateListenersChanged(true /* isEmpty */);

            // sidecar.getWindowLayoutInfo(IBinder)
            Method methodGetWindowLayoutInfo = mSidecar.getClass()
                    .getMethod("getWindowLayoutInfo", IBinder.class);
            Class<?> rtGetWindowLayoutInfo = methodGetWindowLayoutInfo.getReturnType();
            if (!rtGetWindowLayoutInfo.equals(SidecarWindowLayoutInfo.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'getWindowLayoutInfo': "
                                + rtGetWindowLayoutInfo);
            }

            // sidecar.onWindowLayoutChangeListenerAdded(IBinder);
            Method methodRegisterWindowLayoutChangeListener = mSidecar.getClass()
                    .getMethod("onWindowLayoutChangeListenerAdded", IBinder.class);
            Class<?> rtRegisterWindowLayoutChangeListener =
                    methodRegisterWindowLayoutChangeListener.getReturnType();
            if (!rtRegisterWindowLayoutChangeListener.equals(void.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'onWindowLayoutChangeListenerAdded': "
                                + rtRegisterWindowLayoutChangeListener);
            }

            // sidecar.onWindowLayoutChangeListenerRemoved(IBinder);
            Method methodUnregisterWindowLayoutChangeListener = mSidecar.getClass()
                    .getMethod("onWindowLayoutChangeListenerRemoved", IBinder.class);
            Class<?> rtUnregisterWindowLayoutChangeListener =
                    methodUnregisterWindowLayoutChangeListener.getReturnType();
            if (!rtUnregisterWindowLayoutChangeListener.equals(void.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'onWindowLayoutChangeListenerRemoved': "
                                + rtUnregisterWindowLayoutChangeListener);
            }

            // SidecarDeviceState constructor
            tmpDeviceState = new SidecarDeviceState();

            // deviceState.posture
            tmpDeviceState.posture = SidecarDeviceState.POSTURE_OPENED;

            // SidecarDisplayFeature constructor
            SidecarDisplayFeature displayFeature = new SidecarDisplayFeature();

            // displayFeature.getRect()/setRect()
            Rect tmpRect = displayFeature.getRect();
            displayFeature.setRect(tmpRect);

            // displayFeature.getType()/setType()
            int tmpType = displayFeature.getType();
            displayFeature.setType(SidecarDisplayFeature.TYPE_FOLD);

            // SidecarWindowLayoutInfo constructor
            SidecarWindowLayoutInfo windowLayoutInfo = new SidecarWindowLayoutInfo();

            // windowLayoutInfo.displayFeatures
            final List<SidecarDisplayFeature> tmpDisplayFeatures = windowLayoutInfo.displayFeatures;

            return true;
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "Extension implementation doesn't conform to interface version "
                        + VERSION_0_1 + ", error: " + e);
            }
            return false;
        }
    }

    @Nullable
    static Version getSidecarVersion() {
        try {
            String vendorVersion = SidecarProvider.getApiVersion();
            return !TextUtils.isEmpty(vendorVersion) ? Version.parse(vendorVersion) : null;
        } catch (NoClassDefFoundError e) {
            if (DEBUG) {
                Log.d(TAG, "Sidecar version not found");
            }
            return null;
        } catch (UnsupportedOperationException e) {
            if (DEBUG) {
                Log.d(TAG, "Stub Sidecar");
            }
            return null;
        }
    }

    /**
     * Converts the display feature from extension. Can return {@code null} if there is an issue
     * with the value passed from extension.
     */
    @Nullable
    private static DisplayFeature displayFeatureFromExtension(SidecarDisplayFeature feature) {
        if (feature.getRect().width() == 0 && feature.getRect().height() == 0) {
            if (DEBUG) {
                Log.d(TAG, "Passed a display feature with empty rect, skipping: " + feature);
            }
            return null;
        }
        return new DisplayFeature(feature.getRect(), feature.getType());
    }

    @NonNull
    private static List<DisplayFeature> displayFeatureListFromSidecar(
            SidecarWindowLayoutInfo sidecarWindowLayoutInfo) {
        List<DisplayFeature> displayFeatures = new ArrayList<>();
        if (sidecarWindowLayoutInfo.displayFeatures == null) {
            return displayFeatures;
        }

        for (SidecarDisplayFeature sidecarFeature : sidecarWindowLayoutInfo.displayFeatures) {
            final DisplayFeature displayFeature = displayFeatureFromExtension(sidecarFeature);
            if (displayFeature != null) {
                displayFeatures.add(displayFeature);
            }
        }
        return displayFeatures;
    }

    @NonNull
    private static WindowLayoutInfo windowLayoutInfoFromSidecar(
            @Nullable SidecarWindowLayoutInfo extensionInfo) {
        if (extensionInfo == null) {
            return new WindowLayoutInfo(new ArrayList<>());
        }

        List<DisplayFeature> displayFeatures = displayFeatureListFromSidecar(extensionInfo);
        return new WindowLayoutInfo(displayFeatures);
    }

    @DeviceState.Posture
    private static int postureFromSidecar(SidecarDeviceState sidecarDeviceState) {
        int sidecarPosture = sidecarDeviceState.posture;
        if (sidecarPosture > POSTURE_MAX_KNOWN) {
            if (DEBUG) {
                Log.d(TAG, "Unknown posture reported, WindowManager library should be updated");
            }
            return POSTURE_UNKNOWN;
        }
        return sidecarPosture;
    }

    @NonNull
    private static DeviceState deviceStateFromSidecar(
            @Nullable SidecarDeviceState sidecarDeviceState) {
        if (sidecarDeviceState == null) {
            return new DeviceState(POSTURE_UNKNOWN);
        }

        int posture = postureFromSidecar(sidecarDeviceState);
        return new DeviceState(posture);
    }
}
