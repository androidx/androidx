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

package androidx.viewpager2.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class AdapterTest : BaseTest() {
    @Test
    fun test_setAdapter() {
        val test = setUpTest(ViewPager2.ORIENTATION_HORIZONTAL)
        test.setAdapterSync(viewAdapterProvider(stringSequence(5)))
        test.assertBasicState(0)
        test.viewPager.setCurrentItemSync(1, false, 2, TimeUnit.SECONDS)
        test.assertBasicState(1)
        activityTestRule.runOnUiThread {
            test.viewPager.adapter = test.viewPager.adapter
        }
        test.assertBasicState(0)
    }
}