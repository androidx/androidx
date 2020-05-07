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

package androidx.ui.foundation

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.setValue
import androidx.test.filters.LargeTest
import androidx.ui.core.Modifier
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.height
import androidx.ui.layout.preferredHeight
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.test.assertCountEquals
import androidx.ui.test.children
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendSwipeUp
import androidx.ui.test.waitForIdle
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch

@LargeTest
@RunWith(JUnit4::class)
class AdapterListTest {
    private val AdapterListTag = "TestAdapterList"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun compositionsAreDisposed_whenNodesAreScrolledOff() {
        var composed: Boolean
        var disposed = false
        // Ten 31dp spacers in a 300dp list
        val latch = CountDownLatch(10)
        // Make it long enough that it's _definitely_ taller than the screen
        val data = (1..50).toList()

        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = AdapterListTag }) {
                // Fixed height to eliminate device size as a factor
                Box(Modifier.preferredHeight(300.dp)) {
                    AdapterList(data = data, modifier = Modifier.fillMaxSize()) {
                        onCommit {
                            composed = true
                            // Signal when everything is done composing
                            latch.countDown()
                            onDispose {
                                disposed = true
                            }
                        }

                        // There will be 10 of these in the 300dp box
                        Spacer(Modifier.preferredHeight(31.dp))
                    }
                }
            }
        }

        latch.await()
        composed = false

        assertWithMessage("Compositions were disposed before we did any scrolling")
            .that(disposed).isFalse()

        // Mostly a sanity check, this is not part of the behavior under test
        assertWithMessage("Additional composition occurred for no apparent reason")
            .that(composed).isFalse()

        findByTag(AdapterListTag)
            .doGesture { sendSwipeUp() }

        waitForIdle()

        assertWithMessage("No additional items were composed after scroll, scroll didn't work")
            .that(composed).isTrue()

        // We may need to modify this test once we prefetch/cache items outside the viewport
        assertWithMessage(
            "No compositions were disposed after scrolling, compositions were leaked"
        ).that(disposed).isTrue()
    }

    @Test
    fun compositionsAreDisposed_whenDataIsChanged() {
        var composed: Boolean
        var disposals = 0
        val latch1 = CountDownLatch(3)
        val latch2 = CountDownLatch(2)
        val data1 = (1..3).toList()
        val data2 = (4..5).toList() // smaller, to ensure removal is handled properly

        var part2 by mutableStateOf(false)

        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = AdapterListTag }) {
                AdapterList(
                    data = if (!part2) data1 else data2,
                    modifier = Modifier.fillMaxSize()
                ) {
                    onCommit {
                        composed = true
                        // Signal when everything is done composing
                        if (!part2) {
                            latch1.countDown()
                        } else {
                            latch2.countDown()
                        }
                        onDispose {
                            disposals++
                        }
                    }

                    Spacer(Modifier.height(50.dp))
                }
            }
        }

        latch1.await()

        composed = false

        runOnIdleCompose { part2 = true }

        latch2.await()

        assertWithMessage(
            "No additional items were composed after data change, something didn't work"
        ).that(composed).isTrue()

        // We may need to modify this test once we prefetch/cache items outside the viewport
        assertWithMessage(
            "Not enough compositions were disposed after scrolling, compositions were leaked"
        ).that(disposals).isEqualTo(data1.size)
    }

    @Test
    fun removeItemsTest() {
        val startingNumItems = 3
        var numItems = startingNumItems
        var numItemsModel by mutableStateOf(numItems)
        val tag = "List"
        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = tag }) {
                AdapterList((1..numItemsModel).toList()) {
                    Semantics(container = true) {
                        Text("$it")
                    }
                }
            }
        }

        while (numItems >= 0) {
            // Confirm the number of children to ensure there are no extra items
            findByTag(tag)
                .children()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (i in 1..3) {
                findByText("$i").apply {
                    if (i <= numItems) {
                        assertExists()
                    } else {
                        assertDoesNotExist()
                    }
                }
            }
            numItems--
            if (numItems >= 0) {
                // Don't set the model to -1
                runOnIdleCompose { numItemsModel = numItems }
            }
        }
    }

    @Test
    fun changingDataTest() {
        val dataLists = listOf(
            (1..3).toList(),
            (4..8).toList(),
            (3..4).toList()
        )
        var dataModel by mutableStateOf(dataLists[0])
        val tag = "List"
        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = tag }) {
                AdapterList(dataModel) {
                    Semantics(container = true) {
                        Text("$it")
                    }
                }
            }
        }

        for (data in dataLists) {
            runOnIdleCompose { dataModel = data }

            // Confirm the number of children to ensure there are no extra items
            val numItems = data.size
            findByTag(tag)
                .children()
                .assertCountEquals(numItems)

            // Confirm the children's content
            for (item in data) {
                findByText("$item").assertExists()
            }
        }
    }
}