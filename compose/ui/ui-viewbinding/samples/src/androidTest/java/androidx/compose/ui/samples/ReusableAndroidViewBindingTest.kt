/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.samples

import android.widget.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewbinding.samples.R
import androidx.compose.ui.viewbinding.samples.databinding.SampleButtonLayoutBinding
import androidx.compose.ui.viewbinding.samples.databinding.TestFragmentLayoutBinding
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ReusableAndroidViewBindingTest {

    @get:Rule
    val rule = createAndroidComposeRule<EmptyFragmentActivity>()

    @Test
    fun testViewIsReused_whenMovedAndUpdated() {
        val events = mutableListOf<String>()
        var slotWithContent by mutableStateOf(0)
        val movableContext = movableContentOf {
            AndroidViewBinding(
                factory = { inflater, parent, attachToParent ->
                    events += "inflate"
                    SampleButtonLayoutBinding.inflate(inflater, parent, attachToParent)
                },
                modifier = Modifier.heightIn(max = 200.dp),
                onReset = {
                    events += "reset"
                    myButton.text = null
                },
                onRelease = { events += "release" },
                update = {
                    events += "update"
                    myButton.text = "Button in slot $slotWithContent"
                }
            )
        }

        rule.setContent {
            Column {
                repeat(10) { slot ->
                    if (slot == slotWithContent) {
                        ReusableContent(Unit) {
                            movableContext()
                        }
                    } else {
                        BasicText("Slot $slot")
                    }
                }
            }
        }

        onView(withClassName(`is`(ButtonFqClassName)))
            .check(matches(withText("Button in slot 0")))
            .check(matches(withEffectiveVisibility(VISIBLE)))

        assertEquals(
            "Binding did not experience the expected event sequence when being " +
                "initialized in its first position",
            listOf("inflate", "update"),
            events
        )

        events.clear()
        slotWithContent++

        onView(withClassName(`is`(ButtonFqClassName)))
            .check(matches(withText("Button in slot 1")))
            .check(matches(withEffectiveVisibility(VISIBLE)))

        assertEquals(
            "Binding did not experience expected events when moving to its new position " +
                "when the moved content is updated via composition",
            listOf("update"),
            events
        )
    }

    @Test
    fun testViewIsReusedAndReleased_whenDeactivatedOrRemovedAndUpdated() {
        val events = mutableListOf<String>()
        var includeContent by mutableStateOf(true)
        var enableContent by mutableStateOf(true)

        rule.setContent {
            if (includeContent) {
                ReusableContentHost(enableContent) {
                    AndroidViewBinding(
                        factory = { inflater, parent, attachToParent ->
                            events += "inflate"
                            SampleButtonLayoutBinding.inflate(inflater, parent, attachToParent)
                        },
                        modifier = Modifier.heightIn(max = 200.dp),
                        onReset = {
                            events += "reset"
                            myButton.text = null
                        },
                        onRelease = { events += "release" },
                        update = {
                            events += "update"
                            myButton.text = "My Button"
                        }
                    )
                }
            }
        }

        onView(withClassName(`is`(ButtonFqClassName)))
            .check(matches(withText("My Button")))
            .check(matches(withEffectiveVisibility(VISIBLE)))

        assertEquals(
            "Binding did not experience the expected event sequence when being " +
                "created for the first time",
            listOf("inflate", "update"),
            events
        )

        events.clear()
        includeContent = false

        onView(withClassName(`is`(ButtonFqClassName)))
            .check(doesNotExist())

        assertEquals(
            "Binding should be released when its containing group is removed from composition",
            listOf("release"),
            events
        )

        events.clear()
        includeContent = true

        onView(withClassName(`is`(ButtonFqClassName)))
            .check(matches(withText("My Button")))
            .check(matches(withEffectiveVisibility(VISIBLE)))

        assertEquals(
            "Binding should re-experience its creation lifecycle when its previously removed " +
                "group is re-added to the composition",
            listOf("inflate", "update"),
            events
        )

        events.clear()
        enableContent = false

        onView(withClassName(`is`(ButtonFqClassName)))
            .check(doesNotExist())

        assertEquals(
            "Binding should be reset when its parent ReusableContentHost is deactivated",
            listOf("reset"),
            events
        )

        events.clear()
        enableContent = true

        onView(withClassName(`is`(ButtonFqClassName)))
            .check(matches(withText("My Button")))
            .check(matches(withEffectiveVisibility(VISIBLE)))

        assertEquals(
            "Binding should be re-awoken when its parent ReusableContentHost becomes active again",
            listOf("update"),
            events
        )
    }

    @Test
    fun testFragmentIsReused_whenMoved() {
        val events = mutableListOf<String>()
        var slotWithContent by mutableStateOf(0)
        val movableContext = movableContentOf {
            AndroidViewBinding(
                factory = { inflater, parent, attachToParent ->
                    events += "inflate"
                    TestFragmentLayoutBinding.inflate(inflater, parent, attachToParent)
                },
                modifier = Modifier.heightIn(max = 200.dp),
                onReset = { events += "reset" },
                onRelease = { events += "release" },
                update = { events += "update" }
            )
        }

        rule.setContent {
            Column {
                repeat(10) { slot ->
                    if (slot == slotWithContent) {
                        ReusableContent(Unit) {
                            movableContext()
                        }
                    } else {
                        BasicText("Slot $slot")
                    }
                }
            }
        }

        waitForIdleSync()

        assertEquals(
            "Binding did not experience the expected event sequence when being " +
                "initialized in its first position",
            listOf("inflate", "update"),
            events
        )

        events.clear()
        slotWithContent++
        waitForIdleSync()

        assertEquals(
            "Binding should not experience any events when moving to its new position",
            emptyList<String>(),
            events
        )
    }

    @Test
    fun testFragmentIsReusedAndReleased_whenDeactivatedOrRemoved() {
        val events = mutableListOf<String>()
        var includeContent by mutableStateOf(true)
        var enableContent by mutableStateOf(true)

        rule.setContent {
            if (includeContent) {
                ReusableContentHost(enableContent) {
                    AndroidViewBinding(
                        factory = { inflater, parent, attachToParent ->
                            events += "inflate"
                            TestFragmentLayoutBinding.inflate(inflater, parent, attachToParent)
                        },
                        modifier = Modifier.heightIn(max = 200.dp),
                        onReset = { events += "reset" },
                        onRelease = { events += "release" },
                        update = { events += "update" }
                    )
                }
            }
        }

        onView(withId(R.id.fragment_layout)).check(matches(withEffectiveVisibility(VISIBLE)))
        assertEquals(
            "Binding did not experience the expected event sequence when being " +
                "created for the first time",
            listOf("inflate", "update"),
            events
        )

        events.clear()
        includeContent = false

        onView(withId(R.id.fragment_layout)).check(doesNotExist())
        assertEquals(
            "Binding should be released when its containing group is removed from composition",
            listOf("release"),
            events
        )

        events.clear()
        includeContent = true

        onView(withId(R.id.fragment_layout)).check(matches(withEffectiveVisibility(VISIBLE)))
        assertEquals(
            "Binding should re-experience its creation lifecycle when its previously removed " +
                "group is re-added to the composition",
            listOf("inflate", "update"),
            events
        )

        events.clear()
        enableContent = false

        onView(withId(R.id.fragment_layout)).check(doesNotExist())
        assertEquals(
            "Binding should be reset when its parent ReusableContentHost is deactivated",
            listOf("reset"),
            events
        )

        events.clear()
        enableContent = true

        onView(withId(R.id.fragment_layout)).check(matches(withEffectiveVisibility(VISIBLE)))
        assertEquals(
            "Binding should be re-awoken when its parent ReusableContentHost becomes active again",
            listOf("update"),
            events
        )
    }

    private fun waitForIdleSync() = rule.runOnIdle { /* Do nothing. */ }

    companion object {
        private val ButtonFqClassName = Button::class.qualifiedName!!
    }
}