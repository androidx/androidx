/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appcompat.app.g3;

import static androidx.test.espresso.Espresso.pressBack;

import android.app.Instrumentation;

import androidx.test.espresso.NoActivityResumedException;

/**
 * Adapted from g3's UITestUtils.java with modifications to pass AndroidX lint.
 */
public class UITestUtils {
    public static void rotateScreen(Instrumentation instrumentation,
            AndroidTestUtil.ScreenOrientation originalOrientation) {
        AndroidTestUtil.setScreenOrientation(
                instrumentation.getTargetContext(),
                originalOrientation == AndroidTestUtil.ScreenOrientation.PORTRAIT
                        ? AndroidTestUtil.ScreenOrientation.LANDSCAPE
                        : AndroidTestUtil.ScreenOrientation.PORTRAIT);
    }

    /**
     * Verify that when back button is clicked, the app exits.
     * @return true if the app exits when back is pressed; false if it did not exit.
     */
    public static boolean verifyPressBackAndExit() {
        try {
            pressBack();
        } catch (NoActivityResumedException e) {
            return true;
        }

        return false;
    }
}
