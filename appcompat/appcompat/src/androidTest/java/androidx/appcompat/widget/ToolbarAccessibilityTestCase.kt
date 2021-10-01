/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.appcompat.widget

import androidx.appcompat.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Test

@MediumTest
public class ToolbarAccessibilityTestCase {

    /**
     * Test for b/200845656 which propagates the nav button's content description to its tooltip.
     */
    @SdkSuppress(minSdkVersion = 28)
    @Test
    public fun testSetNavigationContentDescriptionSetsTooltip() {
        ActivityScenario.launch(ToolbarTestActivity::class.java).onActivity { activity ->
            val toolbar: Toolbar = activity.requireViewById(R.id.toolbar)
            val expectedString = "TEST"
            toolbar.navigationContentDescription = expectedString

            assertEquals(expectedString, toolbar.navButtonView?.tooltipText)
        }
    }
}
