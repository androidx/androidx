/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.appcompat.app;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.view.KeyEvent;
import android.view.inspector.WindowInspector;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EmptyOptionsPanelTest {
    @Rule
    public final ActivityTestRule<EmptyOptionsPanelActivity> mActivityTestRule;

    private Instrumentation mInstrumentation;
    private EmptyOptionsPanelActivity mActivity;

    public EmptyOptionsPanelTest() {
        mActivityTestRule = new ActivityTestRule<>(EmptyOptionsPanelActivity.class);
    }

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityTestRule.getActivity();
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    @MediumTest
    public void testEmptyOptionsPanelNotShown() throws Exception {
        mActivity.setShouldPopulateOptionsMenu(false);

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();

        // UiAutomator is flaky, so instead we'll just check how many windows are showing. This
        // is... not a great way to test this behavior, but we don't have any other way to hook
        // into an empty options panel.
        assertEquals("Sub-panel should not be added after first KEYCODE_MENU",
                1, WindowInspector.getGlobalWindowViews().size());

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();

        assertEquals("Sub-panel should not be added after second KEYCODE_MENU",
                1, WindowInspector.getGlobalWindowViews().size());
    }
}
