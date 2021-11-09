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

package androidx.wear.input.testing;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.input.WearableButtonsProvider;

import com.google.android.wearable.input.WearableInputDevice;

import java.util.Map;

/**
 * A {@link WearableButtonsProvider} suitable for use in tests.
 *
 * <p>This allows for explicitly specifying which buttons are available for testing, and their
 * coordinates. It is intended to be used by passing in a map, mapping between the button keycode
 * (typically in the set {@link android.view.KeyEvent#KEYCODE_STEM_PRIMARY}, {@link
 * android.view.KeyEvent#KEYCODE_STEM_1}, {@link android.view.KeyEvent#KEYCODE_STEM_2}, or {@link
 * android.view.KeyEvent#KEYCODE_STEM_3}) and the location of the button. Take the following
 * example:
 *
 * <pre>
 *     Map<Integer, TestWearableButtonLocation> buttons = new HashMap<>();
 *     buttons.put(KEYCODE_STEM_1, new TestWearableButtonLocation(100, 100);
 *
 *     TestWearableButtonsProvider provider = new TestWearableButtonsProvider(buttons);
 *
 *     WearableButtons.setWearableButtonsProvider(provider);
 * </pre>
 */
public class TestWearableButtonsProvider implements WearableButtonsProvider {

    /**
     * Class describing the location of a button on a wearable device. This has two forms; it can
     * either store the absolute location of the button, or store both the absolute location of the
     * button, and the absolute location when the screen is rotated through 180 degrees.
     */
    public static class TestWearableButtonLocation {
        private final PointF mLocation;
        private final PointF mRotatedLocation;

        /**
         * Build a button location, with just the default button location.
         *
         * @param x X coordinate of the button.
         * @param y Y coordinate of the button.
         */
        public TestWearableButtonLocation(float x, float y) {
            mLocation = new PointF(x, y);
            mRotatedLocation = null;
        }

        /**
         * Build a button location, with both the default button location, and the location when the
         * device is rotated through 180 degrees.
         *
         * @param x X coordinate of the button.
         * @param y Y coordinate of the button.
         * @param rotatedX X coordinate of the button when the device is rotated.
         * @param rotatedY Y coordinate of the button when the device is rotated.
         */
        public TestWearableButtonLocation(float x, float y, float rotatedX, float rotatedY) {
            mLocation = new PointF(x, y);
            mRotatedLocation = new PointF(rotatedX, rotatedY);
        }

        /**
         * Get the location of this button.
         *
         * @return A point specifying the location of this button.
         */
        @NonNull
        public PointF getLocation() {
            return mLocation;
        }

        /**
         * Get the location of this button when the device is rotated.
         *
         * @return A point specifying the location of this button when the device is rotated.
         */
        @Nullable
        public PointF getRotatedLocation() {
            return mRotatedLocation;
        }
    }

    private final Map<Integer, TestWearableButtonLocation> mButtons;

    /**
     * Build a button provider, which will respond with the provided set of buttons.
     *
     * @param buttons The buttons returned by this provider.
     */
    public TestWearableButtonsProvider(@NonNull Map<Integer, TestWearableButtonLocation> buttons) {
        mButtons = buttons;
    }

    @NonNull
    @Override
    public Bundle getButtonInfo(@NonNull Context context, int keycode) {
        Bundle bundle = new Bundle();

        TestWearableButtonLocation location = mButtons.get(keycode);
        if (location != null) {
            bundle.putFloat(WearableInputDevice.X_KEY, location.getLocation().x);
            bundle.putFloat(WearableInputDevice.Y_KEY, location.getLocation().y);

            if (location.getRotatedLocation() != null) {
                bundle.putFloat(WearableInputDevice.X_KEY_ROTATED, location.getRotatedLocation().x);
                bundle.putFloat(WearableInputDevice.Y_KEY_ROTATED, location.getRotatedLocation().y);
            }
        }

        return bundle;
    }

    @Nullable
    @Override
    public int[] getAvailableButtonKeyCodes(@NonNull Context context) {
        int[] keys = new int[mButtons.size()];

        int i = 0;
        for (Integer keycode : mButtons.keySet()) {
            keys[i++] = keycode;
        }

        return keys;
    }
}
