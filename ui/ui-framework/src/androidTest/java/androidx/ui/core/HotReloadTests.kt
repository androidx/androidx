/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("USELESS_CAST", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.ui.core

import androidx.compose.Composable
import androidx.test.filters.MediumTest
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.rule.ActivityTestRule
import androidx.ui.framework.test.TestActivity
import androidx.ui.test.assertLabelEquals
import androidx.ui.test.findByTag
import org.junit.runners.JUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class HotReloadTests {
    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)

    @Test
    fun composeView() {
        val activity = rule.activity
        var value = "First value"

        @Composable fun text(text: String, id: Int = -1) {
            TextView(id = id, text = text)
        }

        @Composable fun column(children: @Composable() () -> Unit) {
            LinearLayout { children() }
        }

        val composeLatch = CountDownLatch(1)

        rule.runOnUiThread {
            activity.setContent {
                column {
                    text(text = "Hello", id = 101)
                    text(text = "World", id = 102)
                    text(text = value, id = 103)
                }
            }
            composeLatch.countDown()
        }

        assert(composeLatch.await(1, TimeUnit.SECONDS))

        assertEquals(activity.findViewById<TextView>(103).text, value)
        value = "Second value"
        assertNotEquals(activity.findViewById<TextView>(103).text, value)

        val hotReloadLatch = CountDownLatch(1)

        rule.runOnUiThread {
            simulateHotReload(activity)
            hotReloadLatch.countDown()
        }

        assert(hotReloadLatch.await(1, TimeUnit.SECONDS))

        assertEquals(activity.findViewById<TextView>(103).text, value)
    }

    @Test
    fun composeComponentNode() {
        val activity = rule.activity
        var value = "First value"

        @Composable fun textNode(text: String, id: Int) {
            TestTag(tag = "text$id") {
                Text(text)
            }
        }

        @Composable fun columnNode(children: @Composable() () -> Unit) {
            children()
        }

        val composeLatch = CountDownLatch(1)

        // Set the content of the view
        rule.runOnUiThread {
            activity.setContent {
                columnNode {
                    textNode(text = value, id = 103)
                }
            }
            composeLatch.countDown()
        }

        assert(composeLatch.await(1, TimeUnit.SECONDS))

        fun target() = findByTag("text103")

        // Assert that the composition has the correct value
        target().assertLabelEquals(value)

        value = "Second value"

        val hotReloadLatch = CountDownLatch(1)

        // Simulate hot-reload
        rule.runOnUiThread {
            simulateHotReload(activity)
            hotReloadLatch.countDown()
        }

        assert(hotReloadLatch.await(1, TimeUnit.SECONDS))

        // Detect tha tthe node changed
        target().assertLabelEquals(value)
    }
}
