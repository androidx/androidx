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

import androidx.compose.mutableStateOf
import androidx.test.filters.MediumTest
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.layout.preferredSize
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.doPartialGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendDown
import androidx.ui.test.sendUp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class IndicationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    val testTag = "indication"

    @Test
    fun indication_receivesInitialState() {
        val state = InteractionState()
        val countDownLatch = CountDownLatch(1)
        val indication = makeIndication {
            // just wait for initial draw with empty interaction
            if (it.value.isEmpty()) {
                countDownLatch.countDown()
            }
        }
        composeTestRule.setContent {
            Box(Modifier.testTag(testTag).preferredSize(100.dp).indication(state, indication))
        }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun indication_click_receivesStateUpdates() {
        // indicaiton should be called 3 times: 0 indication, press, and after click 0 again
        val countDownLatch = CountDownLatch(3)
        val indication = makeIndication {
            it.value // value read
            countDownLatch.countDown()
        }
        composeTestRule.setContent {
            Box(Modifier
                .testTag(testTag)
                .preferredSize(100.dp)
                .clickable(indication = indication) {}
            )
        }
        assertThat(countDownLatch.count).isEqualTo(2)
        findByTag(testTag)
            .assertExists()
            .doPartialGesture {
                sendDown(center)
            }
        runOnIdleCompose {
            assertThat(countDownLatch.count).isEqualTo(1)
        }
        findByTag(testTag)
            .assertExists()
            .doPartialGesture {
                sendUp()
            }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun indication_disposed_whenIndicationRemoved() {
        val state = InteractionState()
        val switchState = mutableStateOf(true)
        val countDownLatch = CountDownLatch(2)
        val indication = makeIndication(
            onDispose = { countDownLatch.countDown() },
            onDraw = { countDownLatch.countDown() }
        )
        composeTestRule.setContent {
            val switchableIndication =
                if (switchState.value) Modifier.indication(state, indication) else Modifier
            Box(Modifier.testTag(testTag).preferredSize(100.dp).plus(switchableIndication))
        }
        assertThat(countDownLatch.count).isEqualTo(1)
        runOnIdleCompose {
            switchState.value = !switchState.value
        }
        runOnIdleCompose {
            assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    @Ignore("b/155466122: multitouch is not supported yet")
    fun indication_multiplyPress_firstWins() {
        var lastPosition: PxPosition? = null
        val indication = makeIndication {
            it.value // value read
            lastPosition = it.interactionPositionFor(Interaction.Pressed)
        }
        composeTestRule.setContent {
            Box(Modifier
                .testTag(testTag)
                .preferredSize(100.dp)
                .clickable(indication = indication) { }
            )
        }
        assertThat(lastPosition).isNull()
        var position1: PxPosition? = null
        findByTag(testTag)
            .assertExists()
            .doPartialGesture {
                position1 = PxPosition(center.x, center.y + 20f)
                // pointer 1, when we have multitouch
                sendDown(position1!!)
            }
        runOnIdleCompose {
            assertThat(lastPosition).isEqualTo(position1!!)
        }
        findByTag(testTag)
            .assertExists()
            .doPartialGesture {
                val position2 = PxPosition(center.x + 20f, center.y)
                // pointer 2, when we have multitouch
                sendDown(position2)
            }
        // should be still position1
        runOnIdleCompose {
            assertThat(lastPosition).isEqualTo(position1!!)
        }
        findByTag(testTag)
            .assertExists()
            .doPartialGesture {
                // pointer 1, when we have multitouch
                sendUp()
            }
        runOnIdleCompose {
            assertThat(lastPosition).isNull()
        }
        findByTag(testTag)
            .assertExists()
            .doPartialGesture {
                // pointer 2, when we have multitouch
                sendUp()
            }
    }

    private fun makeIndication(
        onDispose: () -> Unit = {},
        onDraw: (InteractionState) -> Unit
    ): Indication {
        return object : Indication {
            override fun createInstance(): IndicationInstance {
                return object : IndicationInstance {
                    override fun ContentDrawScope.drawIndication(
                        interactionState: InteractionState
                    ) {
                        onDraw(interactionState)
                    }

                    override fun onDispose() {
                        super.onDispose()
                        onDispose()
                    }
                }
            }
        }
    }
}