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

import static androidx.window.ActivityUtil.getActivityWindowToken;
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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SimpleArrayMap;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarInterface;
import androidx.window.sidecar.SidecarProvider;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/** Extension interface compatibility wrapper for v0.1 sidecar. */
@SuppressWarnings("deprecation")
final class SidecarCompat implements ExtensionInterfaceCompat {
    private static final String TAG = "SidecarCompat";

    // Map of active listeners registered with #onWindowLayoutChangeListenerAdded() and not yet
    // removed by #onWindowLayoutChangeListenerRemoved().
    protected final SimpleArrayMap<IBinder, Activity> mWindowListenerRegisteredContexts =
            new SimpleArrayMap<>();

    private ExtensionCallbackInterface mExtensionCallback;
    private final SidecarAdapter mSidecarAdapter;

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
        mExtensionCallback = new DistinctElementCallback(extensionCallback);
        mSidecar.setSidecarCallback(new DistinctSidecarElementCallback(mSidecarAdapter,
                new TranslatingCallback()));
    }

    //TODO(b/173739071) reduce visibility to @VisibleForTesting
    @NonNull
    WindowLayoutInfo getWindowLayoutInfo(@NonNull Activity activity) {
        IBinder windowToken = getActivityWindowToken(activity);

        SidecarWindowLayoutInfo windowLayoutInfo = mSidecar.getWindowLayoutInfo(windowToken);
        return mSidecarAdapter.translate(activity, windowLayoutInfo, mSidecar.getDeviceState());
    }

    @Override
    public void onWindowLayoutChangeListenerAdded(@NonNull Activity activity) {
        IBinder windowToken = getActivityWindowToken(activity);

        if (windowToken != null) {
            register(windowToken, activity);
        } else {
            FirstAttachAdapter attachAdapter = new FirstAttachAdapter(this, activity);
            activity.getWindow().getDecorView().addOnAttachStateChangeListener(attachAdapter);
        }
    }

    /**
     * Register an {@link IBinder} token and an {@link Activity} so that the given
     * {@link Activity} will receive updates when there is a new {@link WindowLayoutInfo}.
     * @param windowToken for the given {@link Activity}.
     * @param activity that is listening for changes of {@link WindowLayoutInfo}
     */
    void register(@NonNull IBinder windowToken, @NonNull Activity activity) {
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

    @SuppressLint("BanUncheckedReflection")
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
            // TODO(b/172620880): Workaround for Sidecar API implementation issue.
            try {
                tmpDeviceState.posture = SidecarDeviceState.POSTURE_OPENED;
            } catch (NoSuchFieldError error) {
                if (DEBUG) {
                    Log.w(TAG, "Sidecar implementation doesn't conform to primary interface "
                            + "version, continue to check for the secondary one "
                            + VERSION_0_1 + ", error: " + error);
                }
                Method methodSetPosture = SidecarDeviceState.class.getMethod("setPosture",
                        int.class);
                methodSetPosture.invoke(tmpDeviceState, SidecarDeviceState.POSTURE_OPENED);
                Method methodGetPosture = SidecarDeviceState.class.getMethod("getPosture");
                int posture = (int) methodGetPosture.invoke(tmpDeviceState);
                if (posture != SidecarDeviceState.POSTURE_OPENED) {
                    throw new Exception("Invalid device posture getter/setter");
                }
            }

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
            try {
                final List<SidecarDisplayFeature> tmpDisplayFeatures =
                        windowLayoutInfo.displayFeatures;
                // TODO(b/172620880): Workaround for Sidecar API implementation issue.
            } catch (NoSuchFieldError error) {
                if (DEBUG) {
                    Log.w(TAG, "Sidecar implementation doesn't conform to primary interface "
                            + "version, continue to check for the secondary one "
                            + VERSION_0_1 + ", error: " + error);
                }
                List<SidecarDisplayFeature> featureList = new ArrayList<>();
                featureList.add(displayFeature);
                Method methodSetFeatures = SidecarWindowLayoutInfo.class.getMethod(
                        "setDisplayFeatures", List.class);
                methodSetFeatures.invoke(windowLayoutInfo, featureList);
                Method methodGetFeatures = SidecarWindowLayoutInfo.class.getMethod(
                        "getDisplayFeatures");
                @SuppressWarnings("unchecked")
                final List<SidecarDisplayFeature> resultDisplayFeatures =
                        (List<SidecarDisplayFeature>) methodGetFeatures.invoke(windowLayoutInfo);
                if (!featureList.equals(resultDisplayFeatures)) {
                    throw new Exception("Invalid display feature getter/setter");
                }
            }

            return true;
        } catch (Throwable t) {
            if (DEBUG) {
                Log.e(TAG, "Sidecar implementation doesn't conform to interface version "
                        + VERSION_0_1 + ", error: " + t);
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
     * An adapter that will run a callback when a window is attached and then be removed from the
     * listener set.
     */
    private static class FirstAttachAdapter implements View.OnAttachStateChangeListener {

        private final SidecarCompat mSidecarCompat;
        private final WeakReference<Activity> mActivityWeakReference;

        FirstAttachAdapter(SidecarCompat sidecarCompat, Activity activity) {
            mSidecarCompat = sidecarCompat;
            mActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            view.removeOnAttachStateChangeListener(this);
            Activity activity = mActivityWeakReference.get();
            IBinder token = getActivityWindowToken(activity);
            if (activity == null) {
                if (DEBUG) {
                    Log.d(TAG, "Unable to register activity since activity is missing");
                }
                return;
            }
            if (token == null) {
                if (DEBUG) {
                    Log.w(TAG, "Unable to register activity since the window token is missing");
                }
                return;
            }
            mSidecarCompat.register(token, activity);
        }

        @Override
        public void onViewDetachedFromWindow(View view) { }
    }

    final class TranslatingCallback implements SidecarInterface.SidecarCallback {
        @Override
        @SuppressLint("SyntheticAccessor")
        public void onDeviceStateChanged(@NonNull SidecarDeviceState newDeviceState) {
            mExtensionCallback.onDeviceStateChanged(mSidecarAdapter.translate(newDeviceState));

            for (int i = 0; i < mWindowListenerRegisteredContexts.size(); i++) {
                Activity activity = mWindowListenerRegisteredContexts.valueAt(i);
                IBinder windowToken = getActivityWindowToken(activity);
                if (windowToken == null) {
                    continue;
                }
                SidecarWindowLayoutInfo layoutInfo = mSidecar.getWindowLayoutInfo(windowToken);
                mExtensionCallback.onWindowLayoutChanged(activity,
                        mSidecarAdapter.translate(activity, layoutInfo, newDeviceState));
            }
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

            mExtensionCallback.onWindowLayoutChanged(activity,
                    mSidecarAdapter.translate(activity, newLayout, mSidecar.getDeviceState()));
        }
    }

    /**
     * A class to record the last calculated values from {@link SidecarInterface} and filter out
     * duplicates. This class uses {@link WindowLayoutInfo} and {@link DeviceState} as opposed to
     * {@link SidecarDeviceState} and {@link SidecarDisplayFeature} since the methods
     * {@link Object#equals(Object)} and {@link Object#hashCode()} may not have been overridden.
     */
    private static final class DistinctElementCallback
            implements ExtensionCallbackInterface {

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private DeviceState mLastDeviceState;
        /**
         * A map from {@link Activity} to the last computed {@link WindowLayoutInfo} for the
         * given activity. A {@link WeakHashMap} is used to avoid retaining the {@link Activity}.
         */
        @GuardedBy("mLock")
        private final WeakHashMap<Activity, WindowLayoutInfo> mActivityWindowLayoutInfo =
                new WeakHashMap<>();
        private final ExtensionCallbackInterface mCallbackInterface;

        DistinctElementCallback(ExtensionCallbackInterface callbackInterface) {
            mCallbackInterface = callbackInterface;
        }

        @Override
        public void onDeviceStateChanged(@NonNull DeviceState newDeviceState) {
            synchronized (mLock) {
                if (newDeviceState.equals(mLastDeviceState)) {
                    return;
                }
                mLastDeviceState = newDeviceState;
                mCallbackInterface.onDeviceStateChanged(newDeviceState);
            }
        }

        @Override
        public void onWindowLayoutChanged(@NonNull Activity activity,
                @NonNull WindowLayoutInfo newLayout) {
            synchronized (mLock) {
                WindowLayoutInfo lastInfo = mActivityWindowLayoutInfo.get(activity);
                if (newLayout.equals(lastInfo)) {
                    return;
                }
                mActivityWindowLayoutInfo.put(activity, newLayout);
            }
            mCallbackInterface.onWindowLayoutChanged(activity, newLayout);
        }
    }

    /**
     * A class to record the last calculated values from {@link SidecarInterface} and filter out
     * duplicates. This class uses {@link SidecarAdapter} to compute equality since the methods
     * {@link Object#equals(Object)} and {@link Object#hashCode()} may not have been overridden.
     */
    private static final class DistinctSidecarElementCallback
            implements SidecarInterface.SidecarCallback {

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private SidecarDeviceState mLastDeviceState;
        /**
         * A map from {@link Activity} to the last computed {@link WindowLayoutInfo} for the
         * given activity. A {@link WeakHashMap} is used to avoid retaining the {@link Activity}.
         */
        @GuardedBy("mLock")
        private final WeakHashMap<IBinder, SidecarWindowLayoutInfo> mActivityWindowLayoutInfo =
                new WeakHashMap<>();
        private final SidecarAdapter mSidecarAdapter;
        private final SidecarInterface.SidecarCallback mCallbackInterface;

        DistinctSidecarElementCallback(SidecarAdapter adapter,
                SidecarInterface.SidecarCallback callbackInterface) {
            mSidecarAdapter = adapter;
            mCallbackInterface = callbackInterface;
        }

        @Override
        public void onDeviceStateChanged(@NonNull SidecarDeviceState newDeviceState) {
            synchronized (mLock) {
                if (mSidecarAdapter.isEqualSidecarDeviceState(mLastDeviceState, newDeviceState)) {
                    return;
                }
                mLastDeviceState = newDeviceState;
                mCallbackInterface.onDeviceStateChanged(newDeviceState);
            }
        }

        @Override
        public void onWindowLayoutChanged(@NonNull IBinder token,
                @NonNull SidecarWindowLayoutInfo newLayout) {
            synchronized (mLock) {
                SidecarWindowLayoutInfo lastInfo = mActivityWindowLayoutInfo.get(token);
                if (mSidecarAdapter.isEqualSidecarWindowLayoutInfo(lastInfo, newLayout)) {
                    return;
                }
                mActivityWindowLayoutInfo.put(token, newLayout);
            }
            mCallbackInterface.onWindowLayoutChanged(token, newLayout);
        }
    }
}
