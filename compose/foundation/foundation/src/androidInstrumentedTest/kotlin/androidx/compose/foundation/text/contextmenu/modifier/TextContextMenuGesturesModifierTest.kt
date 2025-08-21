/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.contextmenu.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.test.FakeTextContextMenuProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextContextMenuGesturesModifierTest {
    @get:Rule val rule = createComposeRule()

    private val baseLength = 100
    private val baseLengthDp = with(rule.density) { baseLength.toDp() }

    @Test
    fun whenTouchTap_nothingHappens() =
        runTest(postGestureModifier = Modifier.background(Color.LightGray).size(baseLengthDp)) {
            onBox().performTouchInput { click() }
            assertNullDataProvider()
            assertPreShowContextMenuInvocationCount(0)
        }

    @Test
    fun whenLeftClick_nothingHappens() =
        runTest(postGestureModifier = Modifier.background(Color.LightGray).size(baseLengthDp)) {
            onBox().performMouseInput { click() }
            assertNullDataProvider()
            assertPreShowContextMenuInvocationCount(0)
        }

    @Test
    fun whenRightClick_dataProviderTriggers() =
        runTest(
            preGestureModifier =
                Modifier.appendTextContextMenuComponents { testItem(TestItem.One) },
            postGestureModifier = Modifier.background(Color.LightGray).size(baseLengthDp),
        ) {
            val clickOffset = IntOffset(10, 10)
            rightClick(clickOffset.toOffset())
            assertDataContains(TestItem.One)
            assertClickPositionEquals(clickOffset)
            assertPreShowContextMenuInvocationCount(1)
        }

    @Test
    fun whenDestinationCoordinatesMove_dataProviderPositionsCorrectly() {
        val initialOffset = IntOffset.Zero
        val finalOffset = IntOffset(baseLength, baseLength)
        var offset by mutableStateOf(initialOffset)
        runTest(
            preGestureModifier = Modifier.size(baseLengthDp * 2),
            postGestureModifier =
                Modifier.offset { offset }.background(Color.LightGray).size(baseLengthDp),
        ) {
            val clickOffset = IntOffset(10, 10)

            rightClick(clickOffset.toOffset())
            assertClickPositionEquals(clickOffset)
            assertPreShowContextMenuInvocationCount(1)

            offset = finalOffset
            rule.waitForIdle()
            // The destinationCoordinates move,
            // but the localCoordinates at the gesture location do not,
            // so the result in the destination coordinates should be subtracting any movement.
            assertClickPositionEquals(clickOffset - finalOffset)
            assertPreShowContextMenuInvocationCount(1)
        }
    }

    @Test
    fun whenGestureCoordinatesMove_dataProviderPositionsCorrectly() {
        val initialOffset = IntOffset.Zero
        val finalOffset = IntOffset(baseLength, baseLength)
        var offset by mutableStateOf(initialOffset)
        runTest(
            preGestureModifier = Modifier.size(baseLengthDp * 2).offset { offset },
            postGestureModifier = Modifier.background(Color.LightGray).size(baseLengthDp),
        ) {
            val clickOffset = IntOffset(10, 10)

            rightClick(clickOffset.toOffset())
            assertClickPositionEquals(clickOffset)
            assertPreShowContextMenuInvocationCount(1)

            offset = finalOffset
            rule.waitForIdle()
            // Both layouts move the same amount, so the result should be the same
            assertClickPositionEquals(clickOffset)
            assertPreShowContextMenuInvocationCount(1)
        }
    }

    @Test
    fun whenDataProviderDataUpdates_dataResultUpdates() {
        var item by mutableStateOf(TestItem.One)
        runTest(
            preGestureModifier = Modifier.appendTextContextMenuComponents { testItem(item) },
            postGestureModifier = Modifier.background(Color.LightGray).size(baseLengthDp),
        ) {
            rightClick()
            assertDataContains(TestItem.One)
            assertPreShowContextMenuInvocationCount(1)

            item = TestItem.Two
            rule.waitForIdle()
            assertDataContains(TestItem.Two)
            assertPreShowContextMenuInvocationCount(1)
        }
    }

    private fun runTest(
        preGestureModifier: Modifier = Modifier,
        postGestureModifier: Modifier = Modifier,
        block: TestScope.() -> Unit,
    ) {
        TestScope(preGestureModifier, postGestureModifier).block()
    }

    private inner class TestScope(preGestureModifier: Modifier, postGestureModifier: Modifier) {
        private val tag = "testTag"

        private var dataProvider by
            mutableStateOf<TextContextMenuDataProvider?>(null, neverEqualPolicy())

        private var destinationCoordinates by
            mutableStateOf<LayoutCoordinates?>(null, neverEqualPolicy())

        private var onPreShowContextMenuInvocationCount = 0

        private val actualPosition by derivedStateOf {
            assertNotNull(dataProvider).position(assertNotNull(destinationCoordinates))
        }

        private val actualDataComponents by derivedStateOf {
            assertNotNull(dataProvider).data().components
        }

        init {
            rule.setContent {
                CompositionLocalProvider(
                    LocalTextContextMenuDropdownProvider provides
                        FakeTextContextMenuProvider { dataProvider = it }
                ) {
                    Box(
                        preGestureModifier
                            .showTextContextMenuOnSecondaryClick {
                                onPreShowContextMenuInvocationCount++
                            }
                            .then(postGestureModifier)
                            .onGloballyPositioned { destinationCoordinates = it }
                            .testTag(tag)
                    )
                }
            }
        }

        fun assertNullDataProvider() {
            assertThat(dataProvider).isNull()
        }

        fun assertClickPositionEquals(expectedOffset: IntOffset) {
            assertThat(actualPosition.round()).isEqualTo(expectedOffset)
        }

        fun assertDataContains(vararg items: TestItem) {
            assertThat(actualDataComponents).hasSize(items.size)
            items.zip(actualDataComponents).forEach { (expectedTestItem, actualComponent) ->
                assertThat(actualComponent.key).isEqualTo(expectedTestItem)
            }
        }

        fun assertPreShowContextMenuInvocationCount(n: Int) {
            assertThat(onPreShowContextMenuInvocationCount).isEqualTo(n)
        }

        fun onBox(): SemanticsNodeInteraction = rule.onNodeWithTag(tag)

        fun rightClick(clickPosition: Offset = Offset.Unspecified) {
            onBox().performMouseInput { rightClick(clickPosition.takeOrElse { center }) }
        }
    }
}

private enum class TestItem(val label: String) {
    One("One"),
    Two("Two"),
}

private fun TextContextMenuBuilderScope.testItem(
    item: TestItem,
    onClick: TextContextMenuSession.() -> Unit = {},
) {
    item(key = item, label = item.label, onClick = onClick)
}

private fun <T : Any> assertNotNull(value: T?): T = value.also { assertThat(it).isNotNull() }!!
