/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.fragment.compose

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.compose.test.EmptyTestActivity
import androidx.fragment.compose.test.R
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidFragmentTest {
    @get:Rule val testRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun showContent() {
        testRule.setContent { AndroidFragment<FragmentForCompose>() }

        testRule.waitForIdle()

        onView(withText("Show me on Screen")).check(matches(isDisplayed()))
    }

    @Test
    fun ensureArguments() {
        val name = "Molly"
        val bundle = bundleOf("name" to name)
        testRule.setContent { AndroidFragment<FragmentForCompose>(arguments = bundle) }

        testRule.waitForIdle()

        onView(withText("My name is $name")).check(matches(isDisplayed()))
    }

    @Test
    fun restoreState() {
        val name = "Molly"
        val bundle = bundleOf("name" to name)
        var stateChanger by mutableStateOf(0)
        testRule.setContent {
            if (stateChanger % 2 == 0) {
                AndroidFragment<FragmentForCompose>(arguments = bundle)
            } else {
                Text("No Fragment here")
            }
        }

        testRule.waitForIdle()

        onView(withText("My name is $name")).check(matches(isDisplayed()))

        testRule.runOnIdle { stateChanger = 1 }

        testRule.waitForIdle()
        testRule.onNodeWithText("No Fragment here").isDisplayed()

        testRule.runOnIdle { stateChanger = 2 }

        testRule.waitForIdle()

        onView(withText("My name is $name")).check(matches(isDisplayed()))
    }

    @Test
    fun addAfterStateSaved() {
        lateinit var number: MutableState<Int>
        testRule.setContent {
            number = remember { mutableStateOf(0) }
            if (number.value > 0) {
                AndroidFragment<FragmentForCompose>()
            }
        }

        testRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        testRule.runOnIdle { number.value = 1 }

        testRule.waitForIdle()

        testRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        onView(withText("Show me on Screen")).check(matches(isDisplayed()))

        testRule.runOnIdle { number.value = 0 }

        testRule.waitForIdle()

        // Validate that the fragment was removed
        val fragment =
            testRule.activity.supportFragmentManager.fragments.firstOrNull {
                it is FragmentForCompose
            }
        assertThat(fragment).isNull()
    }

    @Test
    fun addAndRemoveAfterStateSaved() {
        lateinit var number: MutableState<Int>
        testRule.setContent {
            number = remember { mutableStateOf(0) }
            if (number.value > 0) {
                AndroidFragment<FragmentForCompose>()
            }
        }

        testRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        testRule.runOnIdle { number.value = 1 }

        testRule.waitForIdle()

        testRule.runOnIdle { number.value = 0 }

        testRule.waitForIdle()

        testRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        // Validate that the fragment was removed
        val fragment =
            testRule.activity.supportFragmentManager.fragments.firstOrNull {
                it is FragmentForCompose
            }
        assertThat(fragment).isNull()
    }

    @Test
    fun recomposeInsideKey() {

        lateinit var number: MutableState<Int>
        testRule.setContent {
            number = remember { mutableStateOf(0) }
            key(number.value) { AndroidFragment<FragmentForCompose>() }
        }

        testRule.waitForIdle()

        onView(withText("Show me on Screen")).check(matches(isDisplayed()))

        testRule.runOnIdle { number.value++ }

        testRule.waitForIdle()

        onView(withText("Show me on Screen")).check(matches(isDisplayed()))
    }

    @Test
    fun recomposeWhenSwapFragmentClass() {

        lateinit var clazz: MutableState<Class<out Fragment>>
        testRule.setContent {
            clazz = remember { mutableStateOf(FragmentForCompose::class.java) }
            AndroidFragment(
                clazz = clazz.value,
                arguments = bundleOf("name" to clazz.value.simpleName)
            )
        }

        testRule.waitForIdle()

        onView(withText("My name is ${FragmentForCompose::class.simpleName}"))
            .check(matches(isDisplayed()))

        testRule.runOnIdle { clazz.value = FragmentForCompose2::class.java }

        testRule.waitForIdle()

        onView(withText("My name is ${FragmentForCompose2::class.simpleName}"))
            .check(matches(isDisplayed()))
    }
}

class FragmentForCompose : Fragment(R.layout.content) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = arguments?.getString("name")
        if (name != null) {
            val textView = view.findViewById<TextView>(R.id.text)
            textView.text = "My name is $name"
        }
    }
}

class FragmentForCompose2 : Fragment(R.layout.content) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val name = arguments?.getString("name")
        if (name != null) {
            val textView = view.findViewById<TextView>(R.id.text)
            textView.text = "My name is $name"
        }
    }
}
