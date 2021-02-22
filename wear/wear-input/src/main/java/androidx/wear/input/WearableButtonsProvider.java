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

/**
 * A provider interface to allow {@link WearableButtons} to query for information on the device's
 * buttons from the platform. This exists to allow for the button provider to be switched out for
 * testing, for example, by using {@link androidx.wear.input.testing.TestWearableButtonsProvider}.
 */
public interface WearableButtonsProvider {
    /**
     * Returns a bundle containing the metadata of a specific button. Currently, only location is
     * supported. Use with {@link com.google.android.wearable.input.WearableInputDevice#X_KEY} and
     * {@link com.google.android.wearable.input.WearableInputDevice#Y_KEY}. The key will not be
     * present if the information is not available for the requested keycode.
     *
     * <p>The location returned is a Cartesian coordinate where the bottom left corner of the screen
     * is the origin. The unit of measurement is in pixels. The coordinates do not take rotation
     * into account and assume that the device is in the standard upright position.
     *
     * @param context The context of the current activity
     * @param keycode The keycode associated with the hardware button of interest
     * @return A {@link Bundle} containing the metadata for the given keycode
     */
    @NonNull
    Bundle getButtonInfo(@NonNull Context context, int keycode);

    /**
     * Get the keycodes of available hardware buttons on device. This function based on key's
     * locations from system property. This count includes the primary stem key as well as any
     * secondary stem keys available.
     *
     * @param context The context of the current activity
     * @return An int array of available button keycodes, or null if no keycodes could be read.
     */
    @Nullable
    int[] getAvailableButtonKeyCodes(@NonNull Context context);
}
