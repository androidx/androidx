/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OnBackPressedCallbackTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @UiThreadTest
    @Test
    fun testOnBackPressed() {
        val activity = activityRule.activity
        var onBackPressed = false
        activity.addOnBackPressedCallback {
            onBackPressed = true
            false
        }
        val fragmentManager = activity.supportFragmentManager
        val fragment = StrictFragment()
        fragmentManager.beginTransaction()
            .replace(R.id.content, fragment)
            .addToBackStack("back_stack")
            .commit()
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isSameAs(fragment)
        activity.onBackPressed()
        assertWithMessage("OnBackPressedCallbacks should be called before FragmentManager")
            .that(onBackPressed)
            .isTrue()
        assertThat(fragmentManager.findFragmentById(R.id.content))
            .isNull()
    }
}
