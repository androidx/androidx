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

package androidx.wear.input;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.wearable.input.WearableInputDevice;

/**
 * Default implementation of {@link WearableButtonsProvider}, that reads the button locations from
 * the platform.
 */
public final class DeviceWearableButtonsProvider implements WearableButtonsProvider {

    @NonNull
    @Override
    public Bundle getButtonInfo(@NonNull Context context, int keycode) {
        if (!isSharedLibAvailable()) {
            return null;
        }

        return WearableInputDevice.getButtonInfo(context, keycode);
    }

    @Nullable
    @Override
    public int[] getAvailableButtonKeyCodes(@NonNull Context context) {
        if (!isSharedLibAvailable()) {
            return null;
        }

        return WearableInputDevice.getAvailableButtonKeyCodes(context);
    }

    private boolean isSharedLibAvailable() {
        return SharedLibraryVersion.version() >= 1;
    }
}
