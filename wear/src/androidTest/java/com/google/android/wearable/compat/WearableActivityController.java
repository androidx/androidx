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

package com.google.android.wearable.compat;

import android.app.Activity;
import android.os.Bundle;

/**
 * Mock version of {@link WearableActivityController}. During instrumentation testing, the tests
 * would end up using this instead of the version implemented on device.
 */
public class WearableActivityController {

    private static WearableActivityController sLastInstance;

    public static WearableActivityController getLastInstance() {
        return sLastInstance;
    }

    private AmbientCallback mCallback;
    private boolean mAmbientEnabled = false;
    private boolean mAutoResumeEnabled = true;
    private boolean mAmbient = false;

    public WearableActivityController(String tag, Activity activity, AmbientCallback callback) {
        sLastInstance = this;
        mCallback = callback;
    }

    // Methods required by the stub but not currently used in tests.
    public void onCreate() {}
    public void onResume() {}
    public void onPause() {}
    public void onStop() {}
    public void onDestroy() {}

    public void enterAmbient() {
        mCallback.onEnterAmbient(null);
    }

    public void exitAmbient() {
        mCallback.onExitAmbient();
    }

    public void updateAmbient() {
        mCallback.onUpdateAmbient();
    }

    public void setAmbientEnabled() {
        mAmbientEnabled = true;
    }

    public boolean isAmbientEnabled() {
        return mAmbientEnabled;
    }

    public void setAutoResumeEnabled(boolean enabled) {
        mAutoResumeEnabled = enabled;
    }

    public boolean isAutoResumeEnabled() {
        return mAutoResumeEnabled;
    }

    public final boolean isAmbient() {
        return mAmbient;
    }

    public void setAmbient(boolean ambient) {
        mAmbient = ambient;
    }

    /** Stub version of {@link WearableActivityController.AmbientCallback}. */
    public static class AmbientCallback {
        public void onEnterAmbient(Bundle ambientDetails) {}

        public void onExitAmbient() {}

        public void onUpdateAmbient() {}
    }
}
