/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.wear.ambient;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.wearable.compat.WearableActivityController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

/**
 * Provides compatibility for ambient mode.
 */
final class AmbientDelegate {

    private static final String TAG = "AmbientDelegate";

    private WearableActivityController mWearableController;

    private static boolean sInitAutoResumeEnabledMethod;
    private static boolean sHasAutoResumeEnabledMethod;
    private final WearableControllerProvider mWearableControllerProvider;
    private final AmbientCallback mCallback;
    private final WeakReference<Activity> mActivity;

    /**
     * AmbientCallback must be implemented by all users of the delegate.
     */
    interface AmbientCallback {
        /**
         * Called when an activity is entering ambient mode. This event is sent while an activity is
         * running (after onResume, before onPause). All drawing should complete by the conclusion
         * of this method. Note that {@code invalidate()} calls will be executed before resuming
         * lower-power mode.
         * <p>
         * <p><em>Derived classes must call through to the super class's implementation of this
         * method. If they do not, an exception will be thrown.</em>
         *
         * @param ambientDetails bundle containing information about the display being used.
         *                      It includes information about low-bit color and burn-in protection.
         */
        void onEnterAmbient(Bundle ambientDetails);

        /**
         * Called when the system is updating the display for ambient mode. Activities may use this
         * opportunity to update or invalidate views.
         */
        void onUpdateAmbient();

        /**
         * Called when an activity should exit ambient mode. This event is sent while an activity is
         * running (after onResume, before onPause).
         * <p>
         * <p><em>Derived classes must call through to the super class's implementation of this
         * method. If they do not, an exception will be thrown.</em>
         */
        void onExitAmbient();
    }

    AmbientDelegate(@Nullable Activity activity,
                           @NonNull WearableControllerProvider wearableControllerProvider,
                           @NonNull AmbientCallback callback) {
        mActivity = new WeakReference<>(activity);
        mCallback = callback;
        mWearableControllerProvider = wearableControllerProvider;
    }

    /**
     * Receives and handles the onCreate call from the associated {@link AmbientMode}
     */
    void onCreate() {
        Activity activity = mActivity.get();
        if (activity != null) {
            mWearableController =
                    mWearableControllerProvider.getWearableController(activity, mCallback);
        }
        if (mWearableController != null) {
            mWearableController.onCreate();
        }
    }

    /**
     * Receives and handles the onResume call from the associated {@link AmbientMode}
     */
    void onResume() {
        if (mWearableController != null) {
            mWearableController.onResume();
        }
    }

    /**
     * Receives and handles the onPause call from the associated {@link AmbientMode}
     */
    void onPause() {
        if (mWearableController != null) {
            mWearableController.onPause();
        }
    }

    /**
     * Receives and handles the onStop call from the associated {@link AmbientMode}
     */
    void onStop() {
        if (mWearableController != null) {
            mWearableController.onStop();
        }
    }

    /**
     * Receives and handles the onDestroy call from the associated {@link AmbientMode}
     */
    void onDestroy() {
        if (mWearableController != null) {
            mWearableController.onDestroy();
        }
    }

    /**
     * Sets that this activity should remain displayed when the system enters ambient mode. The
     * default is false. In this case, the activity is stopped when the system enters ambient mode.
     */
    void setAmbientEnabled() {
        if (mWearableController != null) {
            mWearableController.setAmbientEnabled();
        }
    }

    /**
     * @return {@code true} if the activity is currently in ambient.
     */
    boolean isAmbient() {
        if (mWearableController != null) {
            return mWearableController.isAmbient();
        }
        return false;
    }

    /**
     * Dump the current state of the wearableController responsible for implementing the Ambient
     * mode.
     */
    void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        if (mWearableController != null) {
            mWearableController.dump(prefix, fd, writer, args);
        }
    }
}
