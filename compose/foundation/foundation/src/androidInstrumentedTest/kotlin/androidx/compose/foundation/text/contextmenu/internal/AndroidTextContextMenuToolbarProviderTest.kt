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

import android.view.Menu
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.test.SpyTextActionModeCallback
import androidx.compose.foundation.text.contextmenu.test.assertNotNull
import androidx.compose.foundation.text.contextmenu.test.items
import androidx.compose.foundation.text.contextmenu.test.numberForLabel
import androidx.compose.foundation.text.contextmenu.test.numbersToData
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class AndroidTextContextMenuToolbarProviderTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun whenDefault_expectedItemsAppear() = runTest {
        showTextContextMenu(testDataProvider(1, 2, 3))
        assertContextMenuExistsWithNumbers(1, 2, 3)
    }

    @Test
    fun whenCoroutineCancelled_toolbarDisappears() = runTest {
        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        rule.runOnUiThread { contextMenuCoroutine.cancel() }

        assertContextMenuDoesNotExist()
        assertThatJob(contextMenuCoroutine).run {
            isCompleted()
            isCancelled()
        }
    }

    @Test
    fun whenCallingTwice_toolbarIsReplaced() = runTest {
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
    fun whenClickItemThatClosesToolbar_toolbarDisappearsAndCoroutineEnds() = runTest {
        val dataProvider =
            testDataProvider(data = { TextContextMenuData(listOf(testItem(1) { close() })) })
        val contextMenuCoroutine = showTextContextMenu(dataProvider)
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        clickItem(1)
        rule.waitForIdle()

        assertContextMenuDoesNotExist()
        assertThatJob(contextMenuCoroutine).isCompleted()
    }

    @Test
    fun whenClickItemThatDoesNothing_toolbarRemains() = runTest {
        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        clickItem(1)
        rule.waitForIdle()

        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()
    }

    @Test
    fun whenOuterContentRemoved_toolbarDisappearsAndCoroutineEnds() {
        var showOuterContent by mutableStateOf(true)
        runTest(outerContent = { if (showOuterContent) it() }) {
            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            showOuterContent = false
            rule.waitForIdle()

            assertContextMenuDoesNotExist()
            assertThatJob(contextMenuCoroutine).isCompleted()
        }
    }

    @Test
    fun whenInnerContentRemoved_toolbarRemains() {
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

    @Test
    fun whenAnchorLayoutMoves_toolbarMoves() {
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

            val initialPosition = getContentMenuContentBoundsOnScreen()

            val expectedLength = 50
            length = expectedLength
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = getContentMenuContentBoundsOnScreen()
            val delta = finalPosition - initialPosition
            assertThat(delta).isEqualTo(IntOffset(expectedLength, expectedLength))
        }
    }

    @Test
    fun whenAnchorLayoutScrolled_toolbarMoves() {
        val scrollState = ScrollState(0)
        val baseLength = 100
        val baseLengthDp = with(rule.density) { baseLength.toDp() }
        runTest(
            outerContent = {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.background(Color.LightGray)
                            .size(baseLengthDp * 2, baseLengthDp * 2)
                            .verticalScroll(scrollState)
                            .background(verticalGradient(0f to Color.Red, 1f to Color.Blue))
                            .size(baseLengthDp * 2, baseLengthDp * 3)
                    ) {
                        it()
                    }
                }
            }
        ) {
            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = getContentMenuContentBoundsOnScreen()

            launch { scrollState.scrollTo(scrollState.maxValue) }
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = getContentMenuContentBoundsOnScreen()
            val delta = finalPosition - initialPosition
            assertThat(delta).isEqualTo(IntOffset(0, -baseLength))
        }
    }

    @Test
    fun whenDataProviderPositionUpdates_toolbarMoves() {
        var offset by mutableStateOf(Offset.Zero)
        runTest {
            val dataProvider =
                testDataProvider(positioner = { offset }, data = { numbersToData(1) })
            val contextMenuCoroutine = showTextContextMenu(dataProvider)
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = getContentMenuContentBoundsOnScreen()

            offset = Offset(10f, 10f)
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalPosition = getContentMenuContentBoundsOnScreen()
            val delta = finalPosition - initialPosition
            assertThat(delta).isEqualTo(offset.round())
        }
    }

    @Test
    fun whenDataProviderDataUpdates_positionDoesNotMove() {
        var itemNumber by mutableStateOf(1)
        runTest {
            val dataProvider = testDataProvider(data = { numbersToData(itemNumber) })
            val contextMenuCoroutine = showTextContextMenu(dataProvider)
            assertContextMenuExistsWithNumbers(1)
            assertContextMenuItemsWithNumbersDoNotExist(2, 3)
            assertThatJob(contextMenuCoroutine).isActive()

            val initialPosition = getContentMenuContentBoundsOnScreen()

            itemNumber = 2
            rule.waitForIdle()
            assertContextMenuItemsWithNumbersDoNotExist(1, 3)
            assertContextMenuExistsWithNumbers(2)
            assertThatJob(contextMenuCoroutine).isActive()
            assertThat(getContentMenuContentBoundsOnScreen()).isEqualTo(initialPosition)

            itemNumber = 3
            rule.waitForIdle()
            assertContextMenuItemsWithNumbersDoNotExist(1, 2)
            assertContextMenuExistsWithNumbers(3)
            assertThatJob(contextMenuCoroutine).isActive()
            assertThat(getContentMenuContentBoundsOnScreen()).isEqualTo(initialPosition)
        }
    }

    @Test
    fun whenDataProviderDataUpdates_itemsUpdate() {
        var itemNumber by mutableStateOf(1)
        runTest {
            val dataProvider = testDataProvider(data = { numbersToData(itemNumber) })
            val contextMenuCoroutine = showTextContextMenu(dataProvider)
            assertContextMenuExistsWithNumbers(1)
            assertContextMenuItemsWithNumbersDoNotExist(2, 3)
            assertThatJob(contextMenuCoroutine).isActive()

            itemNumber = 2
            rule.waitForIdle()
            assertContextMenuItemsWithNumbersDoNotExist(1, 3)
            assertContextMenuExistsWithNumbers(2)
            assertThatJob(contextMenuCoroutine).isActive()

            itemNumber = 3
            rule.waitForIdle()
            assertContextMenuItemsWithNumbersDoNotExist(1, 2)
            assertContextMenuExistsWithNumbers(3)
            assertThatJob(contextMenuCoroutine).isActive()
        }
    }

    private fun runTest(
        outerContent: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
        innerContent: @Composable () -> Unit = { Box(Modifier.fillMaxSize()) },
        testBlock: TestScope.() -> Unit,
    ) {
        val spyTextActionModeCallback = SpyTextActionModeCallback()

        lateinit var provider: TextContextMenuProvider
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            outerContent {
                ProvidePlatformTextContextMenuToolbar(
                    callbackInjector = { spyTextActionModeCallback.apply { delegate = it } }
                ) {
                    val localProvider = LocalTextContextMenuToolbarProvider.current
                    assertThat(localProvider).isNotNull()
                    provider = localProvider!!

                    innerContent()
                }
            }
        }

        TestScope(provider, spyTextActionModeCallback, coroutineScope).testBlock()
    }

    private inner class TestScope(
        private val provider: TextContextMenuProvider,
        private val spyTextActionModeCallback: SpyTextActionModeCallback,
        coroutineScope: CoroutineScope,
    ) : CoroutineScope by coroutineScope {
        fun showTextContextMenu(
            dataProvider: TextContextMenuDataProvider = testDataProvider()
        ): Job =
            rule
                .runOnUiThread {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        provider.showTextContextMenu(dataProvider)
                    }
                }
                .also { rule.waitForIdle() }

        fun assertContextMenuDoesNotExist() {
            assertWithMessage("The toolbar context menu should not exist (menu should be null).")
                .that(spyTextActionModeCallback.menu)
                .isNull()
        }

        fun assertContextMenuExistsWithNumbers(vararg expectedItemNumbers: Int) {
            assertThat(getActualItemNumbers())
                .containsAtLeastElementsIn(expectedItemNumbers.toList())
        }

        fun assertContextMenuItemsWithNumbersDoNotExist(vararg expectedItemNumbers: Int) {
            assertThat(getActualItemNumbers()).containsNoneIn(expectedItemNumbers.toList())
        }

        fun getContentMenuContentBoundsOnScreen(): IntOffset =
            assertNotNull(spyTextActionModeCallback.contentRect).topLeft.round()

        fun clickItem(i: Int) {
            val menu = getExistingContextMenu()
            val item = menu.items().first { it.title?.let(::numberForLabel) == i }
            rule.runOnUiThread {
                menu.performIdentifierAction(item.itemId, Menu.FLAG_PERFORM_NO_CLOSE)
            }
        }

        private fun getActualItemNumbers(): List<Int> {
            val menu = getExistingContextMenu()
            val actualNumbers = menu.items().map { it.title?.let(::numberForLabel) }
            assertThat(actualNumbers).doesNotContain(null)
            return actualNumbers.filterNotNull()
        }

        private fun getExistingContextMenu(): Menu {
            val menu = spyTextActionModeCallback.menu
            assertWithMessage("The toolbar context menu should exist (menu should be non-null).")
                .that(menu)
                .isNotNull()

            return menu!!
        }
    }
}
