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

package androidx.core.view

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.TestActivity
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class ViewWithActivityTest {
    @JvmField @Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var view: View

    @Before
    fun setup() {
        view = View(rule.activity)
    }

    @Test
    fun doOnAttach() {
        var calls = 0
        view.doOnAttach {
            calls++
        }

        addViewToWindow()
        assertEquals(1, calls)

        // Now detach, and re-attach to make sure that the listener was removed
        removeViewFromWindow()
        addViewToWindow()
        assertEquals(1, calls)
    }

    @Test
    fun doOnAttach_whenAttached() {
        addViewToWindow()

        var calls = 0
        view.doOnAttach {
            calls++
        }

        assertEquals(1, calls)
    }

    @Test
    fun doOnDetach() {
        addViewToWindow()

        var calls = 0
        view.doOnDetach {
            calls++
        }

        removeViewFromWindow()
        assertEquals(1, calls)

        // Now re-attach and detach to make sure that the listener was removed
        addViewToWindow()
        removeViewFromWindow()
        assertEquals(1, calls)
    }

    @Test
    fun doOnDetach_whenDetached() {
        var calls = 0
        view.doOnDetach {
            calls++
        }

        assertEquals(1, calls)
    }

    private fun addViewToWindow() = rule.runOnUiThread {
        val contentView = rule.activity.findViewById<ViewGroup>(android.R.id.content)
        contentView.addView(view, WRAP_CONTENT, WRAP_CONTENT)
    }

    private fun removeViewFromWindow() = rule.runOnUiThread {
        rule.activity.findViewById<ViewGroup>(android.R.id.content).removeView(view)
    }
}
