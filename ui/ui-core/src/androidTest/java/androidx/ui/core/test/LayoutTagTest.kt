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

package androidx.ui.core.test

import androidx.compose.emptyContent
import androidx.test.filters.SmallTest
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.core.tag
import androidx.ui.framework.test.TestActivity
import androidx.ui.layout.Stack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class LayoutTagTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = rule.activity
    }

    @Test
    fun testTags() {
        val latch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    {
                        AtLeastSize(0, Modifier.tag("first"), children = emptyContent())
                        Stack(Modifier.tag("second")) {
                            AtLeastSize(
                                0,
                                children = emptyContent()
                            )
                        }
                        Stack(Modifier.tag("third")) {
                            AtLeastSize(0, children = emptyContent())
                        }
                    }
                ) { measurables, _, _ ->
                    assertEquals(3, measurables.size)
                    assertEquals("first", measurables[0].tag)
                    assertEquals("second", measurables[1].tag)
                    assertEquals("third", measurables[2].tag)
                    latch.countDown()
                    layout(0, 0) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
}