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

package androidx.appcompat.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.view.KeyEvent;
import android.view.Window;

import androidx.appcompat.widget.Toolbar;

import org.junit.Test;

public class KeyEventsTestCaseWithToolbar extends BaseKeyEventsTestCase<ToolbarAppCompatActivity> {
    public KeyEventsTestCaseWithToolbar() {
        super(ToolbarAppCompatActivity.class);
    }

    @Test
    @SmallTest
    @Override
    public void testMenuKeyEventReachesActivity() throws InterruptedException {
        // With Toolbar, MENU key gets sent-to (and consumed by) Toolbar rather than Activity
    }

    @Test
    @SmallTest
    public void testMenuKeyOpensToolbarMenu() {
        // Base test only checks that *a* menu is opened, we check here that the toolbar's menu
        // specifically is opened.
        Toolbar toolbar = mActivityTestRule.getActivity().getToolbar();
        assertFalse(toolbar.isOverflowMenuShowing());

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertTrue(toolbar.isOverflowMenuShowing());

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertFalse(toolbar.isOverflowMenuShowing());
    }

    @Test
    @SmallTest
    public void testOpenMenuOpensToolbarMenu() throws Throwable {
        if (!mActivityTestRule.getActivity().getWindow().hasFeature(Window.FEATURE_OPTIONS_PANEL)) {
            return;
        }
        Toolbar toolbar = mActivityTestRule.getActivity().getToolbar();
        assertFalse(toolbar.isOverflowMenuShowing());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityTestRule.getActivity().openOptionsMenu();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertTrue(toolbar.isOverflowMenuShowing());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityTestRule.getActivity().closeOptionsMenu();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertFalse(toolbar.isOverflowMenuShowing());
    }
}
