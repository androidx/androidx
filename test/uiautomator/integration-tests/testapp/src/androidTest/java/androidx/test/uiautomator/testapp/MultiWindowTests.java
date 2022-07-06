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

import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Until;

import org.junit.Test;

/** Integration tests for multi-window support. */
@LargeTest
public class MultiWindowTests extends BaseTest {

    private static final long TIMEOUT_MS = 10_000;
    private static final long PIP_DELAY_MS = 5_000;

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testHasStatusBar() {
        assertTrue(mDevice.hasObject(By.res("com.android.systemui", "status_bar")));
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testPictureInPicture() {
        launchTestActivity(PictureInPictureTestActivity.class);
        mDevice.findObject(By.res(TEST_APP, "pip_button")).click();
        launchTestActivity(PictureInPictureTestActivity.class,
                new Intent().setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK));
        SystemClock.sleep(PIP_DELAY_MS); // Wait for the PiP window to settle.

        // Both PiP and standard windows are present and searchable.
        BySelector defaultMode = By.res(TEST_APP, "pip_mode").text("Default Mode");
        BySelector pipMode = By.res(TEST_APP, "pip_mode").text("PiP Mode");
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

        // Both split screen windows are present and searchable.
        BySelector firstWindow = By.res(TEST_APP, "window_id").text("first");
        BySelector secondWindow = By.res(TEST_APP, "window_id").text("second");
        assertTrue(mDevice.wait(Until.hasObject(secondWindow), TIMEOUT_MS));
        assertTrue(mDevice.hasObject(firstWindow));
    }
}
