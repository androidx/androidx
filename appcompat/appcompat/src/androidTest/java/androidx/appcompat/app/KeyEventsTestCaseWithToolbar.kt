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
package androidx.appcompat.app

import android.view.KeyEvent
import android.view.Window
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test

@MediumTest
class KeyEventsTestCaseWithToolbar : BaseKeyEventsTestCase<ToolbarAppCompatActivity>(
    ToolbarAppCompatActivity::class.java
) {
    @Test
    @Throws(InterruptedException::class)
    override fun testMenuKeyEventReachesActivity() {
        // With Toolbar, MENU key gets sent-to (and consumed by) Toolbar rather than Activity
    }

    @Test
    fun testMenuKeyOpensToolbarMenu() {
        // Base test only checks that *a* menu is opened, we check here that the toolbar's menu
        // specifically is opened.
        val toolbar = mActivityTestRule.activity!!.toolbar
        Assert.assertFalse(toolbar.isOverflowMenuShowing)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Assert.assertTrue(toolbar.isOverflowMenuShowing)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Assert.assertFalse(toolbar.isOverflowMenuShowing)
    }

    @Test
    @Throws(Throwable::class)
    fun testOpenMenuOpensToolbarMenu() {
        if (!mActivityTestRule.activity!!.window.hasFeature(Window.FEATURE_OPTIONS_PANEL)) {
            return
        }
        val toolbar = mActivityTestRule.activity!!.toolbar
        Assert.assertFalse(toolbar.isOverflowMenuShowing)
        mActivityTestRule.runOnUiThread { mActivityTestRule.activity!!.openOptionsMenu() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Assert.assertTrue(toolbar.isOverflowMenuShowing)
        mActivityTestRule.runOnUiThread { mActivityTestRule.activity!!.closeOptionsMenu() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Assert.assertFalse(toolbar.isOverflowMenuShowing)
    }
}
