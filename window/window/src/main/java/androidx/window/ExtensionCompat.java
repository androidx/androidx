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
    final ExtensionAdapter mAdapter;

    ExtensionCompat(Context context) {
        this(ExtensionProvider.getExtensionImpl(context), new ExtensionAdapter());
        if (mWindowExtension == null) {
            throw new IllegalArgumentException("Extension provider returned null");
        }
    }

    @VisibleForTesting
    ExtensionCompat(ExtensionInterface extension, ExtensionAdapter adapter) {
        // Empty implementation to avoid null checks
        mWindowExtension = extension;
        mAdapter = adapter;
    }

    @Override
    public void setExtensionCallback(@NonNull ExtensionCallbackInterface extensionCallback) {
        ExtensionTranslatingCallback translatingCallback = new ExtensionTranslatingCallback(
                extensionCallback, mAdapter);
        mWindowExtension.setExtensionCallback(translatingCallback);
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull Activity activity) {
        mWindowExtension.onWindowLayoutChangeListenerAdded(activity);
    }

    @Override
    public void onWindowLayoutChangeListenerRemoved(@NonNull Activity activity) {
        mWindowExtension.onWindowLayoutChangeListenerRemoved(activity);
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

            // extension.onDeviceStateListenersChanged(boolean);
            mWindowExtension.onDeviceStateListenersChanged(true /* empty */);

            // extension.onWindowLayoutChangeListenerAdded(Activity);
            Method methodRegisterWindowLayoutChangeListener = mWindowExtension.getClass()
                    .getMethod("onWindowLayoutChangeListenerAdded", Activity.class);
            Class<?> rtRegisterWindowLayoutChangeListener =
                    methodRegisterWindowLayoutChangeListener.getReturnType();
            if (!rtRegisterWindowLayoutChangeListener.equals(void.class)) {
                throw new NoSuchMethodException(
                        "Illegal return type for 'onWindowLayoutChangeListenerAdded': "
                                + rtRegisterWindowLayoutChangeListener);
            }

            // extension.onWindowLayoutChangeListenerRemoved(Activity);
            Method methodUnregisterWindowLayoutChangeListener = mWindowExtension.getClass()
                    .getMethod("onWindowLayoutChangeListenerRemoved", Activity.class);
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
