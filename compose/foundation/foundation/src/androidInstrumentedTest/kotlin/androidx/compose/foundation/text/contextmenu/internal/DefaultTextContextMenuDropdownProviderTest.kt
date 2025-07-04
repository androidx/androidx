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

package androidx.compose.foundation.text.contextmenu.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.test.numbersToData
import androidx.compose.foundation.text.contextmenu.test.onItemWithNumber
import androidx.compose.foundation.text.contextmenu.test.positionInScreen
import androidx.compose.foundation.text.contextmenu.test.testDataProvider
import androidx.compose.foundation.text.contextmenu.test.testItem
import androidx.compose.foundation.text.test.assertThatJob
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class DefaultTextContextMenuDropdownProviderTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun whenDefault_expectedItemsAppear() = runTest {
        showTextContextMenu(testDataProvider(1, 2, 3))
        assertContextMenuExistsWithNumbers(1, 2, 3)
    }

    @Test
    fun whenCoroutineCancelled_popupDisappears() = runTest {
        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        contextMenuCoroutine.cancel()
        rule.waitForIdle()

        assertContextMenuDoesNotExist()
        assertContextMenuItemsWithNumbersDoNotExist(1)
        assertThatJob(contextMenuCoroutine).isCompleted()
    }

    @Test
    fun whenCallingTwice_popupIsReplaced() = runTest {
        val firstContextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        assertContextMenuExistsWithNumbers(1)
        assertContextMenuItemsWithNumbersDoNotExist(2)
        assertThatJob(firstContextMenuCoroutine).isActive()

        val secondContextMenuCoroutine = showTextContextMenu(testDataProvider(2))
        assertContextMenuItemsWithNumbersDoNotExist(1)
        assertContextMenuExistsWithNumbers(2)
        assertThatJob(firstContextMenuCoroutine).isCompleted()
        assertThatJob(secondContextMenuCoroutine).isActive()
    }

    @Test
    fun whenClickOffPopup_popupDisappearsAndCoroutineEnds() = runTest {
        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        // Need the click to register above the test framework, else it won't be directed to
        // the popup properly. So, we use a different way of dispatching the click.
        val rootRect =
            with(rule.density) { rule.onAllNodes(isRoot()).onFirst().getBoundsInRoot().toRect() }
        val offset = rootRect.roundToIntRect().topLeft
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(offset.x, offset.y)
        rule.waitForIdle()

        assertContextMenuDoesNotExist()
        assertContextMenuItemsWithNumbersDoNotExist(1)
        assertThatJob(contextMenuCoroutine).isCompleted()
    }

    @Test
    fun whenClickItemThatClosesPopup_popupDisappearsAndCoroutineEnds() = runTest {
        val dataProvider =
            testDataProvider(data = { TextContextMenuData(listOf(testItem(1) { close() })) })
        val contextMenuCoroutine = showTextContextMenu(dataProvider)
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        rule.onItemWithNumber(1).performClick()
        rule.waitForIdle()

        assertContextMenuDoesNotExist()
        assertContextMenuItemsWithNumbersDoNotExist(1)
        assertThatJob(contextMenuCoroutine).isCompleted()
    }

    @Test
    fun whenClickItemThatDoesNothing_popupRemains() = runTest {
        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        rule.onItemWithNumber(1).performClick()
        rule.waitForIdle()

        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()
    }

    @Test
    fun whenOuterContentRemoved_popupDisappearsAndCoroutineEnds() {
        var showOuterContent by mutableStateOf(true)
        runTest(outerContent = { if (showOuterContent) it() }) {
            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            showOuterContent = false
            rule.waitForIdle()

            assertContextMenuDoesNotExist()
            assertContextMenuItemsWithNumbersDoNotExist(1)
            assertThatJob(contextMenuCoroutine).isCompleted()
        }
    }

    @Test
    fun whenInnerContentRemoved_popupRemains() {
        var showInnerContent by mutableStateOf(true)
        runTest(innerContent = { if (showInnerContent) Box(Modifier.fillMaxSize()) }) {
            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            showInnerContent = false
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()
        }
    }

    @Test
    fun whenIncomingMinConstraints_contentReceivesMinConstraints() {
        val innerContentTestTag = "inner"
        val smallerSize = 50
        val largerSize = 100
        var expectedConstraints =
            Constraints(
                minWidth = smallerSize,
                minHeight = smallerSize,
                maxWidth = largerSize,
                maxHeight = largerSize,
            )

        var actualConstraints: Constraints? = null
        runTest(
            outerContent = {
                val density = LocalDensity.current
                val smallerSizeDp = with(density) { smallerSize.toDp() }
                val largerSizeDp = with(density) { largerSize.toDp() }

                Box(
                    propagateMinConstraints = true,
                    modifier =
                        Modifier.background(Color.LightGray)
                            .sizeIn(
                                minWidth = smallerSizeDp,
                                minHeight = smallerSizeDp,
                                maxWidth = largerSizeDp,
                                maxHeight = largerSizeDp,
                            ),
                ) {
                    it()
                }
            },
            innerContent = {
                Box(
                    Modifier.layout { measurable, constraints ->
                            actualConstraints = constraints
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                        .testTag(innerContentTestTag)
                )
            },
        ) {
            assertThat(actualConstraints).isEqualTo(expectedConstraints)
            rule.onNodeWithTag(innerContentTestTag).assertIsDisplayed()
        }
    }

    @Ignore // b/414664796
    @Test
    fun whenAnchorLayoutMoves_popupDoesNotMove() {
        var length by mutableIntStateOf(0)
        runTest(
            outerContent = {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.offset { IntOffset(length, length) }
                            .background(Color.LightGray)
                            .size(150.dp)
                    ) {
                        it()
                    }
                }
            }
        ) {
            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = rule.onItemWithNumber(1).positionInScreen()

            length = 50
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = rule.onItemWithNumber(1).positionInScreen()
            assertThat(finalPosition).isEqualTo(initialPosition)
        }
    }

    @Test
    @Ignore // b/414664796
    fun whenAnchorLayoutScrolled_popupDoesNotMove() {
        val scrollState = ScrollState(0)
        runTest(
            outerContent = {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.background(Color.LightGray)
                            .size(100.dp, 100.dp)
                            .verticalScroll(scrollState)
                            .background(verticalGradient(0f to Color.Red, 1f to Color.Blue))
                            .size(100.dp, 150.dp)
                    ) {
                        it()
                    }
                }
            }
        ) {
            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = rule.onItemWithNumber(1).positionInScreen()

            launch { scrollState.scrollTo(scrollState.maxValue) }
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = rule.onItemWithNumber(1).positionInScreen()
            assertThat(finalPosition).isEqualTo(initialPosition)
        }
    }

    @Test
    @Ignore // b/414664796
    fun whenDataProviderPositionUpdates_popupDoesNotMove() {
        var offset by mutableStateOf(Offset.Zero)
        runTest {
            val dataProvider =
                testDataProvider(positioner = { offset }, data = { numbersToData(1) })
            val contextMenuCoroutine = showTextContextMenu(dataProvider)
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = rule.onItemWithNumber(1).positionInScreen()

            offset = Offset(10f, 10f)
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = rule.onItemWithNumber(1).positionInScreen()
            assertThat(finalPosition).isEqualTo(initialPosition)
        }
    }

    @Test
    fun whenDataProviderDataUpdates_itemsUpdate() {
        var itemNumber by mutableStateOf(1)
        runTest {
            val dataProvider = testDataProvider(data = { numbersToData(itemNumber) })
            val contextMenuCoroutine = showTextContextMenu(dataProvider)
            assertContextMenuExistsWithNumbers(1)
            assertContextMenuItemsWithNumbersDoNotExist(2)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = rule.onItemWithNumber(1).positionInScreen()

            itemNumber = 2
            rule.waitForIdle()

            assertContextMenuItemsWithNumbersDoNotExist(1)
            assertContextMenuExistsWithNumbers(2)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = rule.onItemWithNumber(2).positionInScreen()
            assertThat(finalPosition).isEqualTo(initialPosition)
        }
    }

    @Test
    fun whenDataProviderPositionUpdates_andPopupContentSizeChanges_popupDoesMove() {
        var offset by mutableStateOf(Offset.Zero)
        var itemNumbers by mutableStateOf(listOf(1))
        runTest {
            val dataProvider =
                testDataProvider(
                    positioner = { offset },
                    data = { numbersToData(*itemNumbers.toIntArray()) },
                )
            val contextMenuCoroutine = showTextContextMenu(dataProvider)
            assertContextMenuExistsWithNumbers(1)
            assertContextMenuItemsWithNumbersDoNotExist(2)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = rule.onItemWithNumber(1).positionInScreen()

            offset = Offset(10f, 10f)
            itemNumbers = listOf(1, 2)
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1, 2)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = rule.onItemWithNumber(1).positionInScreen()
            val delta = finalPosition - initialPosition
            assertThat(delta).isEqualTo(offset.round())
        }
    }

    /**
     * @param outerContent Content that goes around the context menu provider, must call the content
     *   lambda exactly once
     * @param innerContent Content that goes inside the context menu provider
     * @param testBlock actions and assertions to run after the content is set
     */
    private fun runTest(
        outerContent: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
        innerContent: @Composable () -> Unit = { Box(Modifier.fillMaxSize()) },
        testBlock: TestScope.() -> Unit,
    ) {
        lateinit var provider: TextContextMenuProvider
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            outerContent {
                ProvideDefaultTextContextMenuDropdown {
                    val localProvider = LocalTextContextMenuDropdownProvider.current
                    assertThat(localProvider).isNotNull()
                    provider = localProvider!!

                    innerContent()
                }
            }
        }

        TestScope(provider, coroutineScope).testBlock()
    }

    private inner class TestScope(
        private val provider: TextContextMenuProvider,
        coroutineScope: CoroutineScope,
    ) : CoroutineScope by coroutineScope {
        fun showTextContextMenu(
            dataProvider: TextContextMenuDataProvider = testDataProvider()
        ): Job =
            launch(start = CoroutineStart.UNDISPATCHED) {
                    provider.showTextContextMenu(dataProvider)
                }
                .also { rule.waitForIdle() }

        fun SemanticsNodeInteractionsProvider.assertPopupCount(
            numberOfPopups: Int
        ): SemanticsNodeInteractionCollection =
            onAllNodes(isPopup()).assertCountEquals(numberOfPopups)

        fun assertContextMenuDoesNotExist() {
            rule.assertPopupCount(0)
        }

        fun assertContextMenuExistsWithNumbers(vararg itemNumbers: Int) {
            rule.assertPopupCount(1)
            itemNumbers.forEach { rule.onItemWithNumber(it).assertIsDisplayed() }
        }

        fun assertContextMenuItemsWithNumbersDoNotExist(vararg itemNumbers: Int) {
            itemNumbers.forEach { rule.onItemWithNumber(it).assertDoesNotExist() }
        }
    }
}
