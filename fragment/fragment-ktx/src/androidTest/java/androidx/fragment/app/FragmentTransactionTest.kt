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

import androidx.test.annotation.UiThreadTest
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@SmallTest
class FragmentTransactionTest {
    @get:Rule val activityRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager

    @UiThreadTest
    @Test fun addWithContainerId() {
        fragmentManager.beginTransaction()
            .add<TestFragment>(android.R.id.content, "tag")
            .commitNow()
        val fragment = fragmentManager.findFragmentById(android.R.id.content)
        assertThat(fragment)
            .isInstanceOf(TestFragment::class.java)
        assertThat(fragmentManager.findFragmentByTag("tag"))
            .isSameInstanceAs(fragment)
    }

    @UiThreadTest
    @Test fun addWithTag() {
        fragmentManager.beginTransaction()
            .add<TestFragment>("tag")
            .commitNow()
        assertThat(fragmentManager.findFragmentByTag("tag"))
            .isInstanceOf(TestFragment::class.java)
    }

    @UiThreadTest
    @Test fun replace() {
        fragmentManager.beginTransaction()
            .replace<TestFragment>(android.R.id.content)
            .commitNow()
        assertThat(fragmentManager.findFragmentById(android.R.id.content))
            .isInstanceOf(TestFragment::class.java)
    }

    @UiThreadTest
    @Test fun replaceWithTag() {
        fragmentManager.beginTransaction()
            .replace<TestFragment>(android.R.id.content, "tag")
            .commitNow()
        val fragment = fragmentManager.findFragmentById(android.R.id.content)
        assertThat(fragment)
            .isInstanceOf(TestFragment::class.java)
        assertThat(fragmentManager.findFragmentByTag("tag"))
            .isSameInstanceAs(fragment)
    }
}
