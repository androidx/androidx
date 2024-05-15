/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;

/** Integration tests for multi-window support. */
@LargeTest
public class MultiWindowTest extends BaseTest {

    private static final BySelector STATUS_BAR = By.res("com.android.systemui", "status_bar");

    @Test
    public void testMultiWindow_statusBar() {
        // Can locate objects outside of current context.
        assertTrue(mDevice.hasObject(STATUS_BAR));
    }

    @Test
    @SdkSuppress(minSdkVersion = 32) // Multi-window operations not reliable enough prior to API 32.
    public void testMultiWindow_splitScreen() {
        BySelector firstWindowSelector = By.res(TEST_APP, "window_id").text("first");
        BySelector secondWindowSelector = By.res(TEST_APP, "window_id").text("second");

        // Launch app with the first window.
        launchTestActivity(SplitScreenTestActivity.class);
        assertTrue(mDevice.wait(Until.hasObject(firstWindowSelector), TIMEOUT_MS));
        UiObject2 firstWindow = mDevice.findObject(firstWindowSelector);

        // Launch the second window.
        firstWindow.longClick();
        assertTrue(mDevice.wait(Until.hasObject(secondWindowSelector), TIMEOUT_MS));
        UiObject2 secondWindow = mDevice.findObject(secondWindowSelector);

        // Operations (clicks) and coordinates are valid in both split screen windows.
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        mDevice.click(width / 2, height / 4);
        mDevice.click(width / 2, 3 * height / 4);
        assertEquals("I've been clicked!", firstWindow.getText());
        assertEquals("I've been clicked!", secondWindow.getText());
    }
}
