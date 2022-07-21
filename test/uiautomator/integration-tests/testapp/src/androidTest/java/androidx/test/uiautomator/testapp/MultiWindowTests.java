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

import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static androidx.test.uiautomator.testapp.SplitScreenTestActivity.WINDOW_ID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;

import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;

/** Integration tests for multi-window support. */
@LargeTest
public class MultiWindowTests extends BaseTest {

    private static final long TIMEOUT_MS = 10_000;
    private static final long DELAY_MS = 5_000;

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testHasStatusBar() {
        assertTrue(mDevice.hasObject(By.res("com.android.systemui", "status_bar")));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testPictureInPicture() {
        BySelector defaultMode = By.res(TEST_APP, "pip_mode").text("Default Mode");
        BySelector pipMode = By.res(TEST_APP, "pip_mode").text("PiP Mode");

        // Create window in PiP mode and verify its location (bounds correctly calculated).
        launchTestActivity(PictureInPictureTestActivity.class);
        mDevice.findObject(By.res(TEST_APP, "pip_button")).click();
        SystemClock.sleep(DELAY_MS); // Wait for the PiP window to settle.
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        Rect bottomHalf = new Rect(0, height / 2, width, height);
        UiObject2 pipWindow = mDevice.wait(Until.findObject(pipMode), TIMEOUT_MS);
        assertTrue(bottomHalf.contains(pipWindow.getVisibleBounds()));

        // Create window in default mode and verify that both windows are present and searchable.
        launchTestActivity(PictureInPictureTestActivity.class,
                new Intent().setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK));
        SystemClock.sleep(DELAY_MS); // Wait for the PiP window to settle.
        assertTrue(mDevice.wait(Until.hasObject(pipMode), TIMEOUT_MS));
        assertTrue(mDevice.hasObject(defaultMode));
    }

    @Test
    @SdkSuppress(minSdkVersion = 32)
    public void testSplitScreen() {
        launchTestActivity(SplitScreenTestActivity.class,
                new Intent().setFlags(DEFAULT_FLAGS).putExtra(WINDOW_ID, "first"));
        launchTestActivity(SplitScreenTestActivity.class,
                new Intent().setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_LAUNCH_ADJACENT
                        | FLAG_ACTIVITY_MULTIPLE_TASK).putExtra(WINDOW_ID, "second"));
        SystemClock.sleep(DELAY_MS); // Wait for the windows to settle.

        // Both split screen windows are present and searchable.
        UiObject2 firstWindow = mDevice.findObject(By.res(TEST_APP, "window_id").text("first"));
        assertNotNull(firstWindow);
        UiObject2 secondWindow = mDevice.findObject(By.res(TEST_APP, "window_id").text("second"));
        assertNotNull(secondWindow);

        // Window IDs are centered in each window (bounds correctly calculated).
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();
        assertTrue(firstWindow.getVisibleBounds().contains(width / 2, height / 4));
        assertTrue(secondWindow.getVisibleBounds().contains(width / 2, 3 * height / 4));
    }
}
