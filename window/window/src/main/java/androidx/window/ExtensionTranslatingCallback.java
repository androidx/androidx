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

import static androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface;
import static androidx.window.extensions.ExtensionInterface.ExtensionCallback;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

/**
 * A class to adapt from {@link ExtensionDeviceState} and {@link ExtensionWindowLayoutInfo} to
 * the local classes {@link DeviceState} and {@link WindowLayoutInfo}.
 */
class ExtensionTranslatingCallback implements ExtensionCallback {

    private final ExtensionCallbackInterface mCallback;
    private final ExtensionAdapter mAdapter;

    ExtensionTranslatingCallback(ExtensionCallbackInterface callback, ExtensionAdapter adapter) {
        mCallback = callback;
        mAdapter = adapter;
    }

    @Override
    public void onDeviceStateChanged(@NonNull ExtensionDeviceState newDeviceState) {
        mCallback.onDeviceStateChanged(mAdapter.translate(newDeviceState));
    }

    @Override
    public void onWindowLayoutChanged(@NonNull Activity activity,
            @NonNull ExtensionWindowLayoutInfo newLayout) {
        mCallback.onWindowLayoutChanged(activity, mAdapter.translate(activity, newLayout));
    }
}
