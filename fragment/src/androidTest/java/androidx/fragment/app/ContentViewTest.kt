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

package androidx.fragment.app

import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ContentViewTest {

    @get:Rule
    var activityRule = ActivityTestRule(ContentViewActivity::class.java, false, false)

    @Test
    fun testContentViewWithInflatedFragment() {
        // The StrictViewFragment runs the appropriate checks to make sure
        // we're moving through the states appropriately
        activityRule.launchActivity(null)
    }
}

class ContentViewActivity : FragmentActivity(R.layout.activity_inflated_fragment)
