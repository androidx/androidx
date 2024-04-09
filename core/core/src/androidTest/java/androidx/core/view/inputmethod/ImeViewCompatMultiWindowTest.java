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

import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
@SdkSuppress(minSdkVersion = 30)
public class ImeViewCompatMultiWindowTest {

    @Rule
    public final ActivityScenarioRule<ImeBaseSplitTestActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(ImeBaseSplitTestActivity.class);

    private static final long ACTIVITY_LAUNCH_TIMEOUT_MS = 10000;
    private static final long VISIBILITY_TIMEOUT_MS = 2000;
    private static final long FIND_OBJECT_TIMEOUT_MS = 5000;
    private static final long CLICK_DURATION_MS = 200;

    private static final String TEST_APP = "androidx.core.test";

    private UiDevice mDevice;

    @Before
    public void setup() throws RemoteException {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.wakeUp();
    }

    /**
     * This test is using a deprecated codepath that doesn't support the workaround, so it is
     * expected to fail hiding the IME.
     * If this test begins failing on a new API version (that is, an assertion error is no longer
     * being thrown), it is likely that the workaround is no longer needed on that API version:
     * b/280532442
     */
    @Test(expected = AssertionError.class)
    @SdkSuppress(minSdkVersion = 30, excludedSdks = { 30 }) // Excluded due to flakes (b/324889554)
    public void testImeShowAndHide_splitScreen() {
        if (Build.VERSION.SDK_INT < 32) {
            // FLAG_ACTIVITY_LAUNCH_ADJACENT is not support before Sdk 32, using the
            // GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN instead.
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
        }

        ActivityScenario<ImeBaseSplitTestActivity> scenario = mActivityScenarioRule.getScenario();
        scenario.onActivity(activity -> {
            // Launch ime test activity in secondary split.
            Intent intent = new Intent(activity, ImeSecondarySplitViewCompatTestActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            activity.startActivity(intent);
        });

        assertTrue("Test app is not visible after launching activity",
                mDevice.wait(Until.hasObject(By.pkg(TEST_APP)), ACTIVITY_LAUNCH_TIMEOUT_MS));

        UiObject2 editText = waitForFindObject("edit_text_id");
        editText.click(CLICK_DURATION_MS);

        AtomicReference<WindowManager> wmRef = new AtomicReference<>();
        scenario.onActivity(activity -> {
            wmRef.set(activity.getSystemService(WindowManager.class));
        });

        WindowManager wm = wmRef.get();
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
