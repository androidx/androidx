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

package androidx.compose.foundation.text.contextmenu.provider

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.test.assertThatJob
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastForEach
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class BasicTextContextMenuProviderTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun whenDefault_expectedItemsAppear() = runProviderTest {
        showTextContextMenu(testDataProvider(1, 2))
        assertContextMenuExistsWithNumbers(1, 2)
    }

    @Test
    fun whenSessionCloseCalled_contextMenuDisappears() = runProviderTest {
        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        assertNotNull(session).close()
        rule.waitForIdle()

        assertContextMenuDoesNotExist()
        assertThatJob(contextMenuCoroutine).isCompleted()
    }

    @Test
    fun whenCoroutineCancelled_contextMenuDisappears() = runProviderTest {
        val contextMenuCoroutine = launch {
            assertFailsWith<CancellationException> {
                assertNotNull(provider).showTextContextMenu(testDataProvider(1))
            }
        }
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        contextMenuCoroutine.cancel()
        rule.waitForIdle()

        assertContextMenuDoesNotExist()
        assertThatJob(contextMenuCoroutine).isCancelled()
    }

    @Test
    fun whenCallingShowTwice_contextMenuIsReplaced() = runProviderTest {
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
    fun whenRemovingAnchorLayout_contextMenuIsClosed() {
        var showAnchorLayout by mutableStateOf(true)
        runProviderTest(
            outerContent = { content -> OuterBox { if (showAnchorLayout) content() } }
        ) {
            rule.onNodeWithTag(AnchorLayoutTag).assertIsDisplayed()

            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))

            rule.onNodeWithTag(AnchorLayoutTag).assertIsDisplayed()
            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            showAnchorLayout = false
            rule.waitForIdle()

            rule.onNodeWithTag(AnchorLayoutTag).assertDoesNotExist()
            assertContextMenuDoesNotExist()
            assertThatJob(contextMenuCoroutine).isCompleted()
        }
    }

    @Test
    fun whenRemovingProvider_contextMenuIsClosed() = runProviderTest {
        rule.onNodeWithTag(AnchorLayoutTag).assertIsDisplayed()

        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))

        rule.onNodeWithTag(AnchorLayoutTag).assertIsDisplayed()
        assertContextMenuExistsWithNumbers(1)
        assertThatJob(contextMenuCoroutine).isActive()

        enabled = false
        rule.waitForIdle()

        rule.onNodeWithTag(AnchorLayoutTag).assertDoesNotExist()
        assertContextMenuDoesNotExist()
        assertThatJob(contextMenuCoroutine).isCompleted()
    }

    @Test
    fun whenShowingThenInstantlyClosing_coroutineDoesNotHang() = runProviderTest {
        val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))
        enabled = false
        rule.waitForIdle()

        rule.onNodeWithTag(AnchorLayoutTag).assertDoesNotExist()
        assertContextMenuDoesNotExist()
        assertThatJob(contextMenuCoroutine).isCompleted()
    }

    @Test
    fun whenMovingAnchorLayout_contextMenuReceivesUpdate() {
        var length by mutableIntStateOf(0)
        runProviderTest(
            outerContent = { content ->
                OuterBox(Modifier.offset { IntOffset(length, length) }.size(150.dp)) { content() }
            }
        ) {
            val contextMenuCoroutine = showTextContextMenu(testDataProvider(1))

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()
            val initialBounds = anchorLayoutCoordinates.boundsInRoot().roundToIntRect()

            length = 50
            rule.waitForIdle()

            assertContextMenuExistsWithNumbers(1)
            assertThatJob(contextMenuCoroutine).isActive()

            val finalBounds = anchorLayoutCoordinates.boundsInRoot().roundToIntRect()
            val expectedBounds = initialBounds.translate(IntOffset(50, 50))
            assertThat(finalBounds).isEqualTo(expectedBounds)
        }
    }

    @Test
    fun whenContextMenuChanges_contextMenuUpdates() {
        lateinit var coroutineScope: CoroutineScope
        lateinit var provider: TextContextMenuProvider

        fun contextMenuFunction(
            tag: String
        ): @Composable
        (
            session: TextContextMenuSession,
            dataProvider: TextContextMenuDataProvider,
            anchorLayoutCoordinates: () -> LayoutCoordinates,
        ) -> Unit = { _, _, _ ->
            Box(modifier = Modifier.background(Color.LightGray).size(50.dp).testTag(tag))
        }

        val tag1 = "ContextMenu1"
        val tag2 = "ContextMenu2"
        var contextMenu by mutableStateOf(contextMenuFunction(tag1))

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            OuterBox {
                ProvideBasicTextContextMenu(
                    modifier = Modifier.testTag(AnchorLayoutTag),
                    providableCompositionLocal = LocalTestContextMenuProvider,
                    contextMenu = contextMenu,
                ) {
                    provider = LocalTestContextMenuProvider.current!!
                    InnerBox()
                }
            }
        }

        val job1 = coroutineScope.launch { provider.showTextContextMenu(testDataProvider(1)) }
        rule.waitForIdle()

        rule.onNodeWithTag(tag1).assertIsDisplayed()
        rule.onNodeWithTag(tag2).assertDoesNotExist()
        assertThatJob(job1).isActive()

        contextMenu = contextMenuFunction(tag2)
        rule.waitForIdle()

        rule.onNodeWithTag(tag1).assertDoesNotExist()
        rule.onNodeWithTag(tag2).assertDoesNotExist()
        assertThatJob(job1).isCompleted()

        val job2 = coroutineScope.launch { provider.showTextContextMenu(testDataProvider(1)) }
        rule.waitForIdle()

        rule.onNodeWithTag(tag1).assertDoesNotExist()
        rule.onNodeWithTag(tag2).assertIsDisplayed()
        assertThatJob(job2).isActive()
    }

    /**
     * @param outerContent Content that goes around the context menu provider, must call the content
     *   lambda exactly once
     * @param innerContent Content that goes inside the context menu provider
     * @param testBlock actions and assertions to run after the content is set
     */
    private fun runProviderTest(
        outerContent: @Composable (content: @Composable () -> Unit) -> Unit = { content ->
            OuterBox(content = content)
        },
        innerContent: @Composable () -> Unit = { InnerBox() },
        testBlock: TestScope.() -> Unit,
    ) {
        val testScope = TestScope()
        rule.setContent {
            testScope.coroutineScope = rememberCoroutineScope()
            outerContent {
                if (testScope.enabled) {
                    ProvideTestBasicTextContextMenu(
                        onContextMenuComposition = { session, anchorLayoutCoordinates ->
                            testScope.session = session
                            testScope.anchorLayoutCoordinatesFunction = anchorLayoutCoordinates
                        }
                    ) {
                        testScope.provider = LocalTestContextMenuProvider.current
                        innerContent()
                    }
                } else {
                    innerContent()
                }
            }
        }

        testScope.testBlock()
    }

    private inner class TestScope {
        var anchorLayoutCoordinatesFunction: (() -> LayoutCoordinates)? = null
        val anchorLayoutCoordinates: LayoutCoordinates
            get() = assertNotNull(anchorLayoutCoordinatesFunction).invoke()

        var coroutineScope: CoroutineScope? = null
        var provider: TextContextMenuProvider? = null
        var session: TextContextMenuSession? = null
        var enabled by mutableStateOf(true)

        fun launch(block: suspend CoroutineScope.() -> Unit): Job =
            assertNotNull(coroutineScope).launch(block = block)

        fun showTextContextMenu(dataProvider: TextContextMenuDataProvider): Job = launch {
            assertNotNull(provider).showTextContextMenu(dataProvider)
        }

        fun assertContextMenuExistsWithNumbers(vararg itemNumbers: Int) {
            rule.onNodeWithTag(ContextMenuTag).assertIsDisplayed()
            itemNumbers.forEach { rule.onNodeWithTag("$it").assertIsDisplayed() }
        }

        fun assertContextMenuItemsWithNumbersDoNotExist(vararg itemNumbers: Int) {
            itemNumbers.forEach { rule.onNodeWithTag("$it").assertDoesNotExist() }
        }

        fun assertContextMenuDoesNotExist() {
            rule.onNodeWithTag(ContextMenuTag).assertDoesNotExist()
        }
    }
}

private fun <T : Any> assertNotNull(obj: T?): T = obj.also { assertThat(it).isNotNull() }!!

private val LocalTestContextMenuProvider = compositionLocalOf<TextContextMenuProvider?> { null }

private const val AnchorLayoutTag = "AnchorLayout"
private const val ContextMenuTag = "ContextMenu"

@Composable
@SuppressLint("ModifierParameter")
private fun OuterBox(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable () -> Unit) {
    Box(modifier, Alignment.Center) { content() }
}

@Composable
private fun InnerBox() {
    Box(
        Modifier.background(Color.LightGray.copy(alpha = 0.3f))
            .sizeIn(minWidth = 200.dp, minHeight = 200.dp)
    )
}

@Composable
private fun ProvideTestBasicTextContextMenu(
    onContextMenuComposition:
        (
            session: TextContextMenuSession?, anchorLayoutCoordinates: () -> LayoutCoordinates,
        ) -> Unit,
    content: @Composable () -> Unit,
) {
    ProvideBasicTextContextMenu(
        modifier = Modifier.testTag(AnchorLayoutTag),
        providableCompositionLocal = LocalTestContextMenuProvider,
        contextMenu = { session, dataProvider, anchorLayoutCoordinates ->
            onContextMenuComposition(session, anchorLayoutCoordinates)
            TestContextMenu(session, dataProvider)
        },
        content = content,
    )
}

@Composable
private fun TestContextMenu(
    session: TextContextMenuSession,
    dataProvider: TextContextMenuDataProvider,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag(ContextMenuTag).background(Color.LightGray).padding(4.dp),
    ) {
        dataProvider.data().components.fastForEach {
            when (it) {
                is TextContextMenuItem ->
                    BasicText(
                        text = it.label,
                        modifier = Modifier.testTag(it.label).clickable { it.onClick(session) },
                    )
            }
        }
    }
}

private fun testDataProvider(vararg itemNumbers: Int): TextContextMenuDataProvider =
    object : TextContextMenuDataProvider {
        override fun position(destinationCoordinates: LayoutCoordinates): Offset =
            destinationCoordinates.size.toIntRect().center.toOffset()

        override fun contentBounds(destinationCoordinates: LayoutCoordinates): Rect =
            position(destinationCoordinates).let { Rect(it, it) }

        override fun data(): TextContextMenuData =
            TextContextMenuData(itemNumbers.map { TextContextMenuItem(key = it, label = "$it") {} })
    }
