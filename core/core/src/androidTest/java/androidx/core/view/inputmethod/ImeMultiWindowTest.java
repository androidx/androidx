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

package androidx.core.view.inputmethod;

import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
@SdkSuppress(minSdkVersion = 30)
public class ImeMultiWindowTest extends BaseInstrumentationTestCase<ImeBaseSplitTestActivity> {

    private static final long ACTIVITY_LAUNCH_TIMEOUT_MS = 10000;
    private static final long VISIBILITY_TIMEOUT_MS = 2000;
    private static final long FIND_OBJECT_TIMEOUT_MS = 5000;
    private static final long CLICK_DURATION_MS = 200;

    private static final String TEST_APP = "androidx.core.test";

    private Activity mActivity;

    private UiDevice mDevice;

    public ImeMultiWindowTest() {
        super(ImeBaseSplitTestActivity.class);
    }

    @Before
    public void setup() throws RemoteException {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.wakeUp();
        mActivity = mActivityTestRule.getActivity();
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void testImeShowAndHide_splitScreen() {
        if (Build.VERSION.SDK_INT < 32) {
            // FLAG_ACTIVITY_LAUNCH_ADJACENT is not support before Sdk 32, using the
            // GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN instead.
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
        }

        // Launch ime test activity in secondary split.
        Intent intent = new Intent(mActivity, ImeSecondarySplitTestActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        mActivity.startActivity(intent);

        assertTrue("Test app is not visible after launching activity",
                mDevice.wait(Until.hasObject(By.pkg(TEST_APP)), ACTIVITY_LAUNCH_TIMEOUT_MS));

        UiObject2 editText = waitForFindObject("edit_text_id");
        editText.click(CLICK_DURATION_MS);

        WindowManager wm = mActivity.getSystemService(WindowManager.class);
        PollingCheck.waitFor(VISIBILITY_TIMEOUT_MS, () -> {
            WindowInsets insets = wm.getCurrentWindowMetrics().getWindowInsets();
            return insets.isVisible(WindowInsetsCompat.Type.ime());
        });

        UiObject2 hideImeButton = waitForFindObject("hide_ime_id");
        hideImeButton.click();

        PollingCheck.waitFor(VISIBILITY_TIMEOUT_MS, () -> {
            WindowInsets insets = wm.getCurrentWindowMetrics().getWindowInsets();
            return !insets.isVisible(WindowInsetsCompat.Type.ime());
        });
    }

    private UiObject2 waitForFindObject(String resId) {
        final UiObject2 object =
                mDevice.wait(Until.findObject(By.res(TEST_APP, resId)), FIND_OBJECT_TIMEOUT_MS);
        assertNotNull("Find object fail", object);
        return object;
    }
}
