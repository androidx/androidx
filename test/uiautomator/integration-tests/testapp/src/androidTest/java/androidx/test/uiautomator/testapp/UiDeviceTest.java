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

package androidx.test.uiautomator.testapp;

import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;

import org.junit.Test;

import java.util.List;

/** Integration tests for {@link androidx.test.uiautomator.UiDevice}. */
@LargeTest
public class UiDeviceTest extends BaseTest {

    private static final long DELAY_MS = 5_000;

    @Test
    public void testClick() {
        launchTestActivity(UiDeviceTestClickActivity.class);

        // Click a button in the middle of the activity.
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        mDevice.click(width / 2, height / 2);

        // Verify that the button was clicked.
        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button"));
        assertNotNull(button);
        assertEquals("I've been clicked!", button.getText());
    }

    @Test
    @SdkSuppress(minSdkVersion = 32)
    public void testClick_multiWindow() {
        // Launch two split-screen activities with buttons.
        launchTestActivity(UiDeviceTestClickActivity.class);
        launchTestActivity(UiDeviceTestClickActivity.class,
                new Intent().setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_LAUNCH_ADJACENT
                        | FLAG_ACTIVITY_MULTIPLE_TASK));
        SystemClock.sleep(DELAY_MS); // Wait for the windows to settle.

        // Click a button in the middle of each activity.
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        mDevice.click(width / 2, height / 4);
        mDevice.click(width / 2, 3 * height / 4);

        // Verify that both buttons were clicked.
        List<UiObject2> buttons = mDevice.findObjects(By.res(TEST_APP, "button"));
        assertEquals(2, buttons.size());
        assertEquals("I've been clicked!", buttons.get(0).getText());
        assertEquals("I've been clicked!", buttons.get(1).getText());
    }
}
