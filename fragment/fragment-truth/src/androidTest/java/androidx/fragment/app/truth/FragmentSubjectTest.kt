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

package androidx.fragment.app.truth

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.fragment.app.truth.FragmentSubject.Companion.assertThat
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FragmentSubjectTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    private lateinit var activity: TestActivity

    @Before
    fun setUp() {
        activity = activityRule.activity
    }

    @UiThreadTest
    @Test
    fun testIsAdded() {
        val fragment = Fragment()
        activity.supportFragmentManager.commitNow {
            add(fragment, "tag")
        }
        assertThat(fragment).isAdded()
        assertThrows {
            assertThat(fragment).isNotAdded()
        }
    }

    @UiThreadTest
    @Test
    fun testIsNotAdded() {
        val fragment = Fragment()
        assertThat(fragment).isNotAdded()
        assertThrows {
            assertThat(fragment).isAdded()
        }
    }
}

class TestActivity : FragmentActivity()
