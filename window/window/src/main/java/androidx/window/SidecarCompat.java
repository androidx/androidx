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
import static androidx.window.WindowManager.getActivityFromContext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SimpleArrayMap;
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

    // Map of active listeners registered with #onWindowLayoutChangeListenerAdded() and not yet
    // removed by #onWindowLayoutChangeListenerRemoved().
    protected final SimpleArrayMap<IBinder, Context> mWindowListenerRegisteredContexts =
            new SimpleArrayMap<>();

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
                Context context = mWindowListenerRegisteredContexts.get(windowToken);
                if (context == null) {
                    Log.w(TAG, "Unable to resolve context from window token. Missing a call"
                            + "to #onWindowLayoutChangeListenerAdded()?");
                    return;
                }

                extensionCallback.onWindowLayoutChanged(context,
                        windowLayoutInfoFromSidecar(context, newLayout));
            }
        });
    }

    @NonNull
    @Override
    public WindowLayoutInfo getWindowLayoutInfo(@NonNull Context context) {
        Activity activity = assertActivityContext(context);
        IBinder windowToken = getActivityWindowToken(activity);

        SidecarWindowLayoutInfo windowLayoutInfo = mSidecar.getWindowLayoutInfo(windowToken);
        return windowLayoutInfoFromSidecar(context, windowLayoutInfo);
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull Context context) {
        Activity activity = assertActivityContext(context);
        IBinder windowToken = getActivityWindowToken(activity);

        mWindowListenerRegisteredContexts.put(windowToken, activity);

        mSidecar.onWindowLayoutChangeListenerAdded(windowToken);
    }

    @Override
    public void onWindowLayoutChangeListenerRemoved(@NonNull Context context) {
        Activity activity = assertActivityContext(context);
        IBinder windowToken = getActivityWindowToken(activity);

        mSidecar.onWindowLayoutChangeListenerRemoved(windowToken);

        mWindowListenerRegisteredContexts.remove(windowToken);
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
    private static DisplayFeature displayFeatureFromExtension(SidecarDisplayFeature feature,
            Rect windowBounds) {
        Rect bounds = feature.getRect();
        if (bounds.width() == 0 && bounds.height() == 0) {
            if (DEBUG) {
                Log.d(TAG, "Passed a display feature with empty rect, skipping: " + feature);
            }
            return null;
        }

        if (feature.getType() == SidecarDisplayFeature.TYPE_FOLD) {
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
        if (feature.getType() == SidecarDisplayFeature.TYPE_HINGE
                || feature.getType() == SidecarDisplayFeature.TYPE_FOLD) {
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

        return new DisplayFeature(feature.getRect(), feature.getType());
    }

    @NonNull
    private static List<DisplayFeature> displayFeatureListFromSidecar(
            SidecarWindowLayoutInfo sidecarWindowLayoutInfo,
            Rect windowBounds) {
        List<DisplayFeature> displayFeatures = new ArrayList<>();
        if (sidecarWindowLayoutInfo.displayFeatures == null) {
            return displayFeatures;
        }

        for (SidecarDisplayFeature sidecarFeature : sidecarWindowLayoutInfo.displayFeatures) {
            final DisplayFeature displayFeature = displayFeatureFromExtension(sidecarFeature,
                    windowBounds);
            if (displayFeature != null) {
                displayFeatures.add(displayFeature);
            }
        }
        return displayFeatures;
    }

    @NonNull
    private static WindowLayoutInfo windowLayoutInfoFromSidecar(
            @NonNull Context context, @Nullable SidecarWindowLayoutInfo extensionInfo) {
        if (extensionInfo == null) {
            return new WindowLayoutInfo(new ArrayList<>());
        }

        Activity activity = getActivityFromContext(context);
        if (activity == null) {
            throw new IllegalArgumentException("Used non-visual Context with WindowManager. "
                    + "Please use an Activity or a ContextWrapper around an Activity instead.");
        }
        Rect windowBounds = WindowBoundsHelper.getInstance().computeCurrentWindowBounds(activity);
        List<DisplayFeature> displayFeatures = displayFeatureListFromSidecar(extensionInfo,
                windowBounds);
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

    private Activity assertActivityContext(Context context) {
        Activity activity = getActivityFromContext(context);
        if (activity == null) {
            throw new IllegalArgumentException("Used non-visual Context with WindowManager. "
                    + "Please use an Activity or a ContextWrapper around an Activity instead.");
        }
        return activity;
    }

    @Nullable
    private IBinder getActivityWindowToken(Activity activity) {
        return activity.getWindow() != null ? activity.getWindow().getAttributes().token : null;
    }
}
