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
import static androidx.window.Version.VERSION_0_1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SimpleArrayMap;
import androidx.core.util.Consumer;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarInterface;
import androidx.window.sidecar.SidecarProvider;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.lang.reflect.Method;
import java.util.List;

/** Extension interface compatibility wrapper for v0.1 sidecar. */
@SuppressWarnings("deprecation")
final class SidecarCompat implements ExtensionInterfaceCompat {
    private static final String TAG = "SidecarCompat";

    // Map of active listeners registered with #onWindowLayoutChangeListenerAdded() and not yet
    // removed by #onWindowLayoutChangeListenerRemoved().
    protected final SimpleArrayMap<IBinder, Activity> mWindowListenerRegisteredContexts =
            new SimpleArrayMap<>();

    private ExtensionCallbackInterface mExtensionCallback;
    private SidecarAdapter mSidecarAdapter;

    @VisibleForTesting
    final SidecarInterface mSidecar;

    SidecarCompat(Context context) {
        this(SidecarProvider.getSidecarImpl(context), new SidecarAdapter());
        if (mSidecar == null) {
            throw new IllegalArgumentException("Sidecar provider returned null");
        }
    }

    @VisibleForTesting
    SidecarCompat(@NonNull SidecarInterface sidecar, SidecarAdapter sidecarAdapter) {
        // Empty implementation to avoid null checks.
        mExtensionCallback = new ExtensionCallbackInterface() {
            @Override
            public void onDeviceStateChanged(@NonNull DeviceState newDeviceState) {

            }

            @Override
            public void onWindowLayoutChanged(@NonNull Activity activity,
                    @NonNull WindowLayoutInfo newLayout) {

            }
        };
        mSidecar = sidecar;
        mSidecarAdapter = sidecarAdapter;
    }

    @Override
    public void setExtensionCallback(@NonNull ExtensionCallbackInterface extensionCallback) {
        mExtensionCallback = extensionCallback;
        mSidecar.setSidecarCallback(new SidecarInterface.SidecarCallback() {
            @Override
            @SuppressLint("SyntheticAccessor")
            public void onDeviceStateChanged(@NonNull SidecarDeviceState newDeviceState) {
                extensionCallback.onDeviceStateChanged(mSidecarAdapter.translate(newDeviceState));
            }

            @Override
            @SuppressLint("SyntheticAccessor")
            public void onWindowLayoutChanged(@NonNull IBinder windowToken,
                    @NonNull SidecarWindowLayoutInfo newLayout) {
                Activity activity = mWindowListenerRegisteredContexts.get(windowToken);
                if (activity == null) {
                    Log.w(TAG, "Unable to resolve activity from window token. Missing a call"
                            + "to #onWindowLayoutChangeListenerAdded()?");
                    return;
                }

                extensionCallback.onWindowLayoutChanged(activity,
                        mSidecarAdapter.translate(activity, newLayout));
            }
        });
    }

    @NonNull
    @VisibleForTesting
    WindowLayoutInfo getWindowLayoutInfo(@NonNull Activity activity) {
        IBinder windowToken = getActivityWindowToken(activity);

        SidecarWindowLayoutInfo windowLayoutInfo = mSidecar.getWindowLayoutInfo(windowToken);
        return mSidecarAdapter.translate(activity, windowLayoutInfo);
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull Activity activity) {
        IBinder windowToken = getActivityWindowToken(activity);

        if (windowToken != null) {
            register(windowToken, activity);
        } else {
            FirstAttachAdapter attachAdapter = new FirstAttachAdapter((token) -> {
                register(token, activity);
            });
            activity.getWindow().getDecorView().addOnAttachStateChangeListener(attachAdapter);
        }
    }

    private void register(IBinder windowToken, Activity activity) {
        mWindowListenerRegisteredContexts.put(windowToken, activity);

        mSidecar.onWindowLayoutChangeListenerAdded(windowToken);
        mExtensionCallback.onWindowLayoutChanged(activity, getWindowLayoutInfo(activity));
    }

    @Override
    public void onWindowLayoutChangeListenerRemoved(@NonNull Activity activity) {
        IBinder windowToken = getActivityWindowToken(activity);

        mSidecar.onWindowLayoutChangeListenerRemoved(windowToken);

        mWindowListenerRegisteredContexts.remove(windowToken);
    }

    @Override
    public void onDeviceStateListenersChanged(boolean isEmpty) {
        if (!isEmpty) {
            SidecarDeviceState deviceState = mSidecar.getDeviceState();
            mExtensionCallback.onDeviceStateChanged(mSidecarAdapter.translate(deviceState));
        }
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

            // DO NOT REMOVE SINCE THIS IS VALIDATING THE INTERFACE.
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

    @Nullable
    private IBinder getActivityWindowToken(Activity activity) {
        return activity.getWindow() != null ? activity.getWindow().getAttributes().token : null;
    }

    /**
     * An adapter that will run a callback when a window is attached and then be removed from the
     * listener set.
     */
    private static class FirstAttachAdapter implements View.OnAttachStateChangeListener {

        private final Consumer<IBinder> mCallback;

        FirstAttachAdapter(Consumer<IBinder> callback) {
            mCallback = callback;
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            mCallback.accept(view.getWindowToken());
            view.removeOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
        }
    }
}
