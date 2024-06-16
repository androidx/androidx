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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val fragmentManager
        get() = activityRule.activity.supportFragmentManager

    @UiThreadTest
    @Test
    fun findFragment() {
        val fragment = ViewFragment()
        fragmentManager.commitNow { add(android.R.id.content, fragment) }

        val foundFragment = fragment.requireView().findFragment<ViewFragment>()
        assertWithMessage("View should have Fragment set")
            .that(foundFragment)
            .isSameInstanceAs(fragment)
    }

    @Test
    fun findFragmentNull() {
        val view = View(ApplicationProvider.getApplicationContext() as Context)
        try {
            view.findFragment<Fragment>()
            fail("findFragment should throw IllegalStateException if a Fragment was not set")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("View $view does not have a Fragment set")
        }
    }
}

class ViewFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return View(context)
    }
}
