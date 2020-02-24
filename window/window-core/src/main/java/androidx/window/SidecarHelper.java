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
import static androidx.window.Version.VERSION_0_1;

import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarInterface;
import androidx.window.sidecar.SidecarProvider;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class with convenience methods to communicate with the {@link SidecarInterface}.
 */
final class SidecarHelper {
    static final boolean DEBUG = false;
    private static final String TAG = "WindowSidecarHelper";

    /**
     * Version of the sidecar present on the device. Inflated only once and not supposed to change
     * at runtime, so no need for synchronization.
     */
    private static Version sSidecarVersion;
    /**
     * Wrapper around sidecar interfaces to allow backwards compatibility. The only state it
     * holds is the {@link #sSidecarVersion}, so no synchronization needed as well.
     */
    private static SidecarVersionCompat sSidecarVersionCompat;

    private SidecarHelper() {}

    /**
     * Get an instance of {@link SidecarInterface} implemented by OEM if available on this device.
     */
    static SidecarInterface getSidecarImpl(Context context) {
        Version sidecarVersion;
        try {
            sidecarVersion = getSidecarVersion();
        } catch (Throwable t) {
            if (DEBUG) {
                Log.d(TAG, "Failed to load sidecar version: " + t);
            }
            return null;
        }
        if (!isSidecarVersionSupported(sidecarVersion)) {
            return null;
        }

        SidecarInterface impl;
        try {
            impl = SidecarProvider.getSidecarImpl(context);
            if (impl == null) {
                throw new IllegalArgumentException("Sidecar provider returned null");
            }
        } catch (Throwable t) {
            if (DEBUG) {
                Log.d(TAG, "Failed to load sidecar: " + t);
            }
            return null;
        }
        if (!validateSidecarInterface(impl, sidecarVersion)) {
            if (DEBUG) {
                Log.d(TAG, "Loaded sidecar doesn't match the interface version");
            }
            return null;
        }
        return impl;
    }

    /**
     * Get the version of the Sidecar on this device to match the current version of the library.
     */
    @VisibleForTesting
    @NonNull
    static Version getSidecarVersion() {
        if (sSidecarVersion == null) {
            try {
                String vendorVersion = SidecarProvider.getApiVersion();
                sSidecarVersion = Version.parse(vendorVersion);
            } catch (NoClassDefFoundError e) {
                if (DEBUG) {
                    Log.d(TAG, "Sidecar version not found");
                }
                sSidecarVersion = Version.UNKNOWN;
            } catch (UnsupportedOperationException e) {
                if (DEBUG) {
                    Log.d(TAG, "Stub Sidecar");
                }
                sSidecarVersion = Version.UNKNOWN;
            }
        }

        return sSidecarVersion;
    }

    /**
     * Check if the Sidecar version provided on this device is supported by the current version
     * of the library.
     */
    private static boolean isSidecarVersionSupported(@NonNull Version sidecarVersion) {
        return Version.CURRENT.getMajor() >= sidecarVersion.getMajor();
    }

    /**
     * Get the version compatibility wrapper that invokes the right sidecar method corresponding
     * to the current sidecar version.
     */
    static SidecarVersionCompat versionCompat() {
        if (sSidecarVersionCompat == null) {
            sSidecarVersionCompat = new SidecarVersionCompat(getSidecarVersion());
        }
        return sSidecarVersionCompat;
    }

    private static DisplayFeature displayFeatureFromSidecar(SidecarDisplayFeature feature) {
        Rect featureBounds = versionCompat().getFeatureBounds(feature);
        int featureType = feature.getType();
        return new DisplayFeature(featureBounds, featureType);
    }

    @NonNull
    private static List<DisplayFeature> displayFeatureListFromSidecar(
            SidecarWindowLayoutInfo sidecarWindowLayoutInfo) {
        List<DisplayFeature> displayFeatures = new ArrayList<>();
        List<SidecarDisplayFeature> sidecarFeatures =
                versionCompat().getSidecarDisplayFeatures(sidecarWindowLayoutInfo);
        if (sidecarFeatures == null) {
            return displayFeatures;
        }

        for (SidecarDisplayFeature sidecarFeature : sidecarFeatures) {
            displayFeatures.add(displayFeatureFromSidecar(sidecarFeature));
        }
        return displayFeatures;
    }

    @NonNull
    static WindowLayoutInfo windowLayoutInfoFromSidecar(
            @Nullable SidecarWindowLayoutInfo sidecarInfo) {
        if (sidecarInfo == null) {
            return new WindowLayoutInfo(new ArrayList<>());
        }

        List<DisplayFeature> displayFeatures = displayFeatureListFromSidecar(sidecarInfo);
        return new WindowLayoutInfo(displayFeatures);
    }

    @DeviceState.Posture
    private static int postureFromSidecar(SidecarDeviceState sidecarDeviceState) {
        int sidecarPosture = versionCompat().getSidecarDevicePosture(sidecarDeviceState);
        if (sidecarPosture > POSTURE_MAX_KNOWN) {
            if (DEBUG) {
                Log.d(TAG, "Unknown posture reported, WindowManager library should be updated");
            }
            return POSTURE_UNKNOWN;
        }
        return sidecarPosture;
    }

    @NonNull
    static DeviceState deviceStateFromSidecar(
            @Nullable SidecarDeviceState sidecarDeviceState) {
        if (sidecarDeviceState == null) {
            return new DeviceState(POSTURE_UNKNOWN);
        }

        int posture = postureFromSidecar(sidecarDeviceState);
        return new DeviceState(posture);
    }

    /** Verify that sidecar implementation corresponds to the interface of the provided version. */
    @VisibleForTesting
    static boolean validateSidecarInterface(@NonNull SidecarInterface sidecar,
            @NonNull Version sidecarVersion) {
        try {
            if (sidecarVersion.getMajor() >= 1 || VERSION_0_1.equals(sidecarVersion)) {
                // sidecar.setSidecarCallback(SidecarInterface.SidecarCallback);
                Method methodSetSidecarCallback = sidecar.getClass().getMethod("setSidecarCallback",
                        SidecarInterface.SidecarCallback.class);
                Class<?> rSetSidecarCallback = methodSetSidecarCallback.getReturnType();
                if (!rSetSidecarCallback.equals(void.class)) {
                    throw new NoSuchMethodException("Illegal return type for 'setSidecarCallback': "
                            + rSetSidecarCallback);
                }

                // sidecar.getDeviceState()
                Method methodGetDeviceState = sidecar.getClass().getMethod("getDeviceState");
                Class<?> rtGetDeviceState = methodGetDeviceState.getReturnType();
                if (!rtGetDeviceState.equals(SidecarDeviceState.class)) {
                    throw new NoSuchMethodException("Illegal return type for 'getDeviceState': "
                            + rtGetDeviceState);
                }

                // sidecar.onDeviceStateListenersChanged(boolean);
                Method methodRegisterDeviceStateChangeListener = sidecar.getClass()
                        .getMethod("onDeviceStateListenersChanged", boolean.class);
                Class<?> rtRegisterDeviceStateChangeListener =
                        methodRegisterDeviceStateChangeListener.getReturnType();
                if (!rtRegisterDeviceStateChangeListener.equals(void.class)) {
                    throw new NoSuchMethodException(
                            "Illegal return type for 'onDeviceStateListenersChanged': "
                                    + rtRegisterDeviceStateChangeListener);
                }

                // sidecar.getWindowLayoutInfo(IBinder)
                Method methodGetWindowLayoutInfo = sidecar.getClass()
                        .getMethod("getWindowLayoutInfo", IBinder.class);
                Class<?> rtGetWindowLayoutInfo = methodGetWindowLayoutInfo.getReturnType();
                if (!rtGetWindowLayoutInfo.equals(SidecarWindowLayoutInfo.class)) {
                    throw new NoSuchMethodException(
                            "Illegal return type for 'getWindowLayoutInfo': "
                                    + rtGetWindowLayoutInfo);
                }

                // sidecar.onWindowLayoutChangeListenerAdded(IBinder);
                Method methodRegisterWindowLayoutChangeListener = sidecar.getClass()
                        .getMethod("onWindowLayoutChangeListenerAdded", IBinder.class);
                Class<?> rtRegisterWindowLayoutChangeListener =
                        methodRegisterWindowLayoutChangeListener.getReturnType();
                if (!rtRegisterWindowLayoutChangeListener.equals(void.class)) {
                    throw new NoSuchMethodException(
                            "Illegal return type for 'onWindowLayoutChangeListenerAdded': "
                                    + rtRegisterWindowLayoutChangeListener);
                }

                // sidecar.onWindowLayoutChangeListenerRemoved(IBinder);
                Method methodUnregisterWindowLayoutChangeListener = sidecar.getClass()
                        .getMethod("onWindowLayoutChangeListenerRemoved", IBinder.class);
                Class<?> rtUnregisterWindowLayoutChangeListener =
                        methodUnregisterWindowLayoutChangeListener.getReturnType();
                if (!rtUnregisterWindowLayoutChangeListener.equals(void.class)) {
                    throw new NoSuchMethodException(
                            "Illegal return type for 'onWindowLayoutChangeListenerRemoved': "
                                    + rtUnregisterWindowLayoutChangeListener);
                }

                // SidecarDeviceState constructor
                SidecarDeviceState deviceState = versionCompat()
                        .newSidecarDeviceState(POSTURE_UNKNOWN);

                if (sidecarVersion.getMajor() >= 1) {
                    // deviceState.getPosture();
                    Method methodSidecarDeviceStateGetPosture = deviceState.getClass().getMethod(
                            "getPosture");
                    Class<?> rSidecarDeviceStateGetPosture =
                            methodSidecarDeviceStateGetPosture.getReturnType();
                    if (!rSidecarDeviceStateGetPosture.equals(int.class)) {
                        throw new NoSuchMethodException("Illegal return type for "
                                + "'SidecarDeviceState.getPosture': "
                                + rSidecarDeviceStateGetPosture);
                    }
                } else {
                    // deviceState.posture
                    Field fieldSidecarDeviceStatePosture = deviceState.getClass()
                            .getField("posture");

                    Class<?> rSidecarDeviceStatePosture =
                            fieldSidecarDeviceStatePosture.getType();
                    if (!rSidecarDeviceStatePosture.equals(int.class)) {
                        throw new NoSuchMethodException("Illegal return type for "
                                + "'SidecarDeviceState.posture': "
                                + rSidecarDeviceStatePosture);
                    }
                }

                // SidecarDisplayFeature constructor
                SidecarDisplayFeature displayFeature = versionCompat().newSidecarDisplayFeature(
                        new Rect(0, 0, 0, 0), SidecarDisplayFeature.TYPE_FOLD);

                // displayFeature.getBoundingRect()
                Method methodSidecarDisplayFeatureGetRect = displayFeature.getClass().getMethod(
                        sidecarVersion.getMajor() >= 1 ? "getBounds" : "getRect");
                Class<?> rSidecarDisplayFeatureGetRect =
                        methodSidecarDisplayFeatureGetRect.getReturnType();
                if (!rSidecarDisplayFeatureGetRect.equals(Rect.class)) {
                    throw new NoSuchMethodException("Illegal return type for "
                            + "'SidecarDisplayFeature.getRect': " + rSidecarDisplayFeatureGetRect);
                }

                if (VERSION_0_1.equals(sidecarVersion)) {
                    // displayFeature.setRect()
                    Method methodSidecarDisplayFeatureSetRect = displayFeature.getClass().getMethod(
                            "setRect", Rect.class);
                    Class<?> rSidecarDisplayFeatureSetRect =
                            methodSidecarDisplayFeatureSetRect.getReturnType();
                    if (!rSidecarDisplayFeatureSetRect.equals(void.class)) {
                        throw new NoSuchMethodException("Illegal return type for "
                                + "'SidecarDisplayFeature.setRect': "
                                + rSidecarDisplayFeatureSetRect);
                    }
                }

                // displayFeature.getType()
                Method methodSidecarDisplayFeatureGetType = displayFeature.getClass().getMethod(
                        "getType");
                Class<?> rSidecarDisplayFeatureGetType =
                        methodSidecarDisplayFeatureGetType.getReturnType();
                if (!rSidecarDisplayFeatureGetType.equals(int.class)) {
                    throw new NoSuchMethodException("Illegal return type for "
                            + "'SidecarDisplayFeature.getType': " + rSidecarDisplayFeatureGetType);
                }

                if (VERSION_0_1.equals(sidecarVersion)) {
                    // displayFeature.setType()
                    Method methodSidecarDisplayFeatureSetType = displayFeature.getClass().getMethod(
                            "setType", int.class);
                    Class<?> rSidecarDisplayFeatureSetType =
                            methodSidecarDisplayFeatureSetType.getReturnType();
                    if (!rSidecarDisplayFeatureSetType.equals(void.class)) {
                        throw new NoSuchMethodException("Illegal return type for "
                                + "'SidecarDisplayFeature.setType': "
                                + rSidecarDisplayFeatureSetType);
                    }
                }

                // SidecarWindowLayoutInfo constructor
                SidecarWindowLayoutInfo windowLayoutInfo =
                        versionCompat().newSidecarWindowLayoutInfo(new ArrayList<>());

                if (sidecarVersion.getMajor() >= 1) {
                    // windowLayoutInfo.getDisplayFeatures()
                    Method methodSidecarWindowLayoutInfoGetDisplayFeatures =
                            windowLayoutInfo.getClass().getMethod("getDisplayFeatures");
                    Class<?> rSidecarWindowLayoutInfoGetDisplayFeatures =
                            methodSidecarWindowLayoutInfoGetDisplayFeatures.getReturnType();
                    if (!rSidecarWindowLayoutInfoGetDisplayFeatures.equals(List.class)) {
                        throw new NoSuchMethodException("Illegal return type for "
                                + "'SidecarWindowLayoutInfo.getDisplayFeatures': "
                                + rSidecarWindowLayoutInfoGetDisplayFeatures);
                    }
                } else {
                    // windowLayoutInfo.displayFeatures
                    Field fieldSidecarWindowLayoutInfoDisplayFeatures = windowLayoutInfo.getClass()
                            .getField("displayFeatures");

                    Class<?> rSidecarWindowLayoutInfoDisplayFeatures =
                            fieldSidecarWindowLayoutInfoDisplayFeatures.getType();
                    if (!rSidecarWindowLayoutInfoDisplayFeatures.equals(List.class)) {
                        throw new NoSuchMethodException("Illegal return type for "
                                + "'SidecarWindowLayoutInfo.displayFeatures': "
                                + rSidecarWindowLayoutInfoDisplayFeatures);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "Sidecar implementation doesn't conform to interface version "
                        + sidecarVersion + ", error: " + e);
            }
            return false;
        }
    }
}

