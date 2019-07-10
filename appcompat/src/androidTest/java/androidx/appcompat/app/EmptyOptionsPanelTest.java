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

import static androidx.appcompat.testutils.TestUtils.executeShellCommandAndFind;

import static org.junit.Assert.assertFalse;

import android.app.Instrumentation;
import android.view.KeyEvent;

import androidx.appcompat.testutils.TestUtils.Predicate;
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

    // executeShellCommandAndFind() is only available on API 18+
    @SdkSuppress(minSdkVersion = 18)
    @Test
    @MediumTest
    public void testEmptyOptionsPanelNotShown() throws Exception {
        mActivity.setShouldPopulateOptionsMenu(false);

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();

        assertFalse("Sub-panel was added after first KEYCODE_MENU",
                executeShellCommandAndFind("wm dump", new Predicate<String>() {
                    public boolean test(String t) {
                        return t.contains(
                                "SubPanel:" + mActivity.getComponentName().flattenToString());
                    }
                }));

        mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        mInstrumentation.waitForIdleSync();

        assertFalse("Sub-panel was added after second KEYCODE_MENU",
                executeShellCommandAndFind("wm dump", new Predicate<String>() {
                    public boolean test(String t) {
                        return t.contains(
                                "SubPanel:" + mActivity.getComponentName().flattenToString());
                    }
                }));
    }
}
