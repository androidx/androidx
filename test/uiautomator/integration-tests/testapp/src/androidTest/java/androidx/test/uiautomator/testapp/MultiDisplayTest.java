/*
 * Copyright 2023 The Android Open Source Project
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
import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@SdkSuppress(minSdkVersion = 30)
public class MultiDisplayTest extends BaseTest {
    private static final int MULTI_DISPLAY_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;

    @Before
    public void assumeMultiDisplay() {
        // Tests need to run with multiple displays.
        assumeTrue(getDisplayIds().size() > 1);
    }

    @Test
    public void testMultiDisplay_click() {
        int selectedDisplayId = getSecondaryDisplayId();
        launchTestActivityOnDisplay(ClickTestActivity.class, selectedDisplayId);

        UiObject2 button = mDevice.findObject(By.res(TEST_APP, "button1"));
        button.click();
        assertEquals(selectedDisplayId, button.getDisplayId());
        assertTrue(button.wait(Until.textEquals("text1_clicked"), TIMEOUT_MS));
    }

    // Helper to launch an activity on a specific display.
    private void launchTestActivityOnDisplay(@NonNull Class<? extends Activity> activity,
            int displayId) {
        launchTestActivity(activity, new Intent().setFlags(MULTI_DISPLAY_FLAGS),
                ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle());
    }

    // Helper to get all the display IDs in the current testing environment.
    private static Set<Integer> getDisplayIds() {
        Context context = ApplicationProvider.getApplicationContext();
        DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        return Arrays.stream(displayManager.getDisplays()).map(Display::getDisplayId).collect(
                Collectors.toSet());
    }

    // Helper to get the ID of the first non-default display.
    private static int getSecondaryDisplayId() {
        int selectedDisplayId = 0;
        for (int displayId : getDisplayIds()) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                selectedDisplayId = displayId;
                break;
            }
        }
        return selectedDisplayId;
    }
}
