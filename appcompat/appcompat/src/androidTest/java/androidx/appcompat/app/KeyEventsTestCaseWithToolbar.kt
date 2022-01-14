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

import android.view.Window
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressMenuKey
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.filters.MediumTest
import androidx.testutils.PollingCheck
import androidx.testutils.withActivity
import org.junit.Assert.assertFalse
import org.junit.Test

@MediumTest
class KeyEventsTestCaseWithToolbar : BaseKeyEventsTestCase<ToolbarAppCompatActivity>(
    ToolbarAppCompatActivity::class.java
) {
    @Test
    override fun testMenuKeyEventReachesActivity() {
        // With Toolbar, MENU key gets sent-to (and consumed by) Toolbar rather than Activity
    }

    /**
     * The base test only checks that *a* menu is opened. Here, we check that the *toolbar's* menu
     * is opened.
     */
    @Test
    fun testMenuKeyOpensToolbarMenu() {
        with(ActivityScenario.launch(ToolbarAppCompatActivity::class.java)) {
            val toolbar = withActivity { toolbar }
            assertFalse(toolbar.isOverflowMenuShowing)

            onView(isRoot()).perform(pressMenuKey())
            PollingCheck.waitFor { toolbar.isOverflowMenuShowing }

            onView(isRoot()).perform(pressMenuKey())
            PollingCheck.waitFor { !toolbar.isOverflowMenuShowing }
        }
    }

    @Test
    fun testOpenMenuOpensToolbarMenu() {
        with(ActivityScenario.launch(ToolbarAppCompatActivity::class.java)) {
            val hasOptionsPanel = withActivity { window.hasFeature(Window.FEATURE_OPTIONS_PANEL) }
            if (!hasOptionsPanel) {
                return
            }

            val toolbar = withActivity { toolbar }
            assertFalse(toolbar.isOverflowMenuShowing)

            withActivity { openOptionsMenu() }
            PollingCheck.waitFor { toolbar.isOverflowMenuShowing }

            withActivity { closeOptionsMenu() }
            PollingCheck.waitFor { !toolbar.isOverflowMenuShowing }
        }
    }
}
