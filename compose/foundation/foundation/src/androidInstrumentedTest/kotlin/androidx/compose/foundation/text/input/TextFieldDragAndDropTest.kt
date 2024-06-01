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

package androidx.compose.foundation.text.input

import android.net.Uri
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TestActivity
import androidx.compose.foundation.content.DragAndDropScope
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.createClipData
import androidx.compose.foundation.content.testDragAndDrop
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.firstUriOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldDragAndDropTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun nonTextContent_isNotAccepted() {
        rule.setContentAndTestDragAndDrop {
            val startSelection = state.selection
            drag(Offset(fontSize.toPx() * 2, 10f), defaultUri)
            assertThat(state.selection).isEqualTo(startSelection)
        }
    }

    @Test
    fun textContent_isAccepted() {
        rule.setContentAndTestDragAndDrop {
            drag(Offset(fontSize.toPx() * 2, 10f), "hello")
            assertThat(state.selection).isEqualTo(TextRange(2))
        }
    }

    @Test
    fun draggingText_updatesSelection() {
        rule.setContentAndTestDragAndDrop {
            drag(Offset(fontSize.toPx() * 1, 10f), "hello")
            assertThat(state.selection).isEqualTo(TextRange(1))
            drag(Offset(fontSize.toPx() * 2, 10f), "hello")
            assertThat(state.selection).isEqualTo(TextRange(2))
            drag(Offset(fontSize.toPx() * 3, 10f), "hello")
            assertThat(state.selection).isEqualTo(TextRange(3))
        }
    }

    @Test
    fun draggingNonText_updatesSelection_withReceiveContent() {
        rule.setContentAndTestDragAndDrop(modifier = Modifier.contentReceiver { null }) {
            drag(Offset(fontSize.toPx() * 1, 10f), defaultUri)
            assertThat(state.selection).isEqualTo(TextRange(1))
            drag(Offset(fontSize.toPx() * 2, 10f), defaultUri)
            assertThat(state.selection).isEqualTo(TextRange(2))
            drag(Offset(fontSize.toPx() * 3, 10f), defaultUri)
            assertThat(state.selection).isEqualTo(TextRange(3))
        }
    }

    @Test
    fun draggingText_toEndPadding_updatesSelection() {
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            modifier = Modifier.width(300.dp)
        ) {
            drag(Offset.Zero, "hello")
            assertThat(state.selection).isEqualTo(TextRange(0))
            drag(Offset(295.dp.toPx(), 10f), "hello")
            assertThat(state.selection).isEqualTo(TextRange(4))
        }
    }

    @Test
    fun interactionSource_receivesHoverEnter_whenDraggingTextEnters() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()
        }
    }

    @Test
    fun interactionSource_receivesHoverExit_whenDraggingTextExits() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()

            drag(Offset(1000f, 1f), "hello")
            assertThat(isHovered).isFalse()
        }
    }

    @Test
    fun interactionSource_receivesHoverExit_whenDraggingTextEnds() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()

            cancelDrag()
            assertThat(isHovered).isFalse()
        }
    }

    @Test
    fun interactionSource_receivesHoverExit_whenDraggingTextDrops() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()

            drop()
            assertThat(isHovered).isFalse()
        }
    }

    @Test
    fun draggingOntoTextField_keepsWrapperReceiveContentEntered() {
        // this is a nested scenario where moving a dragging item from receiveContent to
        // BTF2 area does not send an exit event to receiveContent drag listener
        lateinit var view: View
        val density = Density(1f, 1f)
        val calls = mutableListOf<String>()
        rule.setContent { // Do not use setTextFieldTestContent for DnD tests.
            view = LocalView.current
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalWindowInfo provides
                    object : WindowInfo {
                        override val isWindowFocused = false
                    }
            ) {
                Box(
                    modifier =
                        Modifier.size(200.dp)
                            .contentReceiver(
                                object : ReceiveContentListener {
                                    override fun onDragStart() {
                                        calls += "start"
                                    }

                                    override fun onDragEnd() {
                                        calls += "end"
                                    }

                                    override fun onDragEnter() {
                                        calls += "enter"
                                    }

                                    override fun onDragExit() {
                                        calls += "exit"
                                    }

                                    override fun onReceive(
                                        c: TransferableContent
                                    ): TransferableContent? {
                                        calls += "receive"
                                        return null
                                    }
                                }
                            )
                ) {
                    BasicTextField(
                        state = rememberTextFieldState(),
                        textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.width(100.dp).height(40.dp).align(Alignment.Center)
                    )
                }
            }
        }

        testDragAndDrop(view, density) {
            drag(Offset(1f, 1f), defaultUri)
            assertThat(calls).isEqualTo(listOf("start", "enter"))

            cancelDrag()
            assertThat(calls).isEqualTo(listOf("start", "enter", "end"))
            calls.clear()

            drag(Offset(1f, 1f), defaultUri)
            drag(Offset(100f, 100f), defaultUri) // should be inside TextField's area

            // expect no extra enter/exit calls
            assertThat(calls).isEqualTo(listOf("start", "enter"))
            drop()

            assertThat(calls).isEqualTo(listOf("start", "enter", "receive"))
        }
    }

    @Test
    fun draggingOutOfTextField_keepsWrapperReceiveContentEntered() {
        // this is a nested scenario where moving a dragging item from receiveContent to
        // BTF2 area does not send an exit event to receiveContent drag listener
        lateinit var view: View
        val density = Density(1f, 1f)
        val calls = mutableListOf<String>()
        rule.setContent { // Do not use setTextFieldTestContent for DnD tests.
            view = LocalView.current
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalWindowInfo provides
                    object : WindowInfo {
                        override val isWindowFocused = false
                    }
            ) {
                Box(
                    modifier =
                        Modifier.size(200.dp)
                            .contentReceiver(
                                object : ReceiveContentListener {
                                    override fun onDragStart() {
                                        calls += "start"
                                    }

                                    override fun onDragEnd() {
                                        calls += "end"
                                    }

                                    override fun onDragEnter() {
                                        calls += "enter"
                                    }

                                    override fun onDragExit() {
                                        calls += "exit"
                                    }

                                    override fun onReceive(
                                        c: TransferableContent
                                    ): TransferableContent? {
                                        calls += "receive"
                                        return null
                                    }
                                }
                            )
                ) {
                    BasicTextField(
                        state = rememberTextFieldState(),
                        textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.width(100.dp).height(40.dp).align(Alignment.Center)
                    )
                }
            }
        }

        testDragAndDrop(view, density) {
            drag(Offset(100f, 100f), defaultUri) // should be inside TextField's area
            assertThat(calls).isEqualTo(listOf("start", "enter"))

            drag(Offset(199f, 199f), defaultUri)
            assertThat(calls).isEqualTo(listOf("start", "enter")) // no exit event

            drag(Offset(201f, 201f), defaultUri)
            assertThat(calls).isEqualTo(listOf("start", "enter", "exit")) // no exit event
        }
    }

    @Test
    fun droppedText_insertsAtCursor() {
        rule.setContentAndTestDragAndDrop("Hello World!") {
            drag(Offset(fontSize.toPx() * 5, 10f), " Awesome")
            drop()
            assertThat(state.selection).isEqualTo(TextRange("Hello Awesome".length))
            assertThat(state.text.toString()).isEqualTo("Hello Awesome World!")
        }
    }

    @Test
    fun dropped_textAndNonTextCombined_insertsAtCursor() {
        lateinit var receivedContent: TransferableContent
        rule.setContentAndTestDragAndDrop(
            "Hello World!",
            modifier =
                Modifier.contentReceiver { content ->
                    receivedContent = content
                    receivedContent.consume {
                        // do not consume text
                        it.uri != null
                    }
                }
        ) {
            val clipData = createClipData {
                addText(" Awesome")
                addUri(defaultUri)
            }
            drag(Offset(fontSize.toPx() * 5, 10f), clipData)
            drop()
            assertThat(state.selection).isEqualTo(TextRange("Hello Awesome".length))
            assertThat(state.text.toString()).isEqualTo("Hello Awesome World!")
            assertThat(receivedContent.clipEntry.clipData.itemCount).isEqualTo(2)
            assertThat(receivedContent.clipEntry.firstUriOrNull()).isEqualTo(defaultUri)
        }
    }

    @Test
    fun dropped_textAndNonTextCombined_consumedEverything_doesNotInsert() {
        lateinit var receivedContent: TransferableContent
        rule.setContentAndTestDragAndDrop(
            "Hello World!",
            modifier =
                Modifier.contentReceiver {
                    receivedContent = it
                    // consume everything
                    null
                }
        ) {
            val clipData = createClipData {
                addText(" Awesome")
                addUri(defaultUri)
            }
            drag(Offset(fontSize.toPx() * 5, 10f), clipData)
            drop()
            assertThat(state.selection).isEqualTo(TextRange(5))
            assertThat(state.text.toString()).isEqualTo("Hello World!")
            assertThat(receivedContent.clipEntry.clipData.itemCount).isEqualTo(2)
            assertThat(receivedContent.clipEntry.firstUriOrNull()).isEqualTo(defaultUri)
        }
    }

    @Test
    fun dropped_consumedAndReplaced_insertsAtCursor() {
        lateinit var receivedContent: TransferableContent
        rule.setContentAndTestDragAndDrop(
            "Hello World!",
            modifier =
                Modifier.contentReceiver {
                    receivedContent = it
                    val uri = receivedContent.clipEntry.firstUriOrNull()
                    // replace the content
                    val clipData = createClipData { addText(uri.toString()) }
                    TransferableContent(clipData)
                }
        ) {
            val clipData = createClipData { addUri(defaultUri) }
            drag(Offset(fontSize.toPx() * 5, 10f), clipData)
            drop()
            assertThat(state.text.toString()).isEqualTo("Hello$defaultUri World!")
        }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun droppedItem_requestsPermission_ifReceiveContent() {
        rule.setContentAndTestDragAndDrop(
            "Hello World!",
            modifier = Modifier.contentReceiver { null }
        ) {
            drag(Offset(fontSize.toPx() * 5, 10f), defaultUri)
            drop()
            assertThat(rule.activity.requestedDragAndDropPermissions).isNotEmpty()
        }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun droppedItem_doesNotRequestPermission_ifNoReceiveContent() {
        rule.setContentAndTestDragAndDrop("Hello World!") {
            drag(
                Offset(fontSize.toPx() * 5, 10f),
                createClipData {
                    addText()
                    addUri()
                }
            )
            drop()
            assertThat(rule.activity.requestedDragAndDropPermissions).isEmpty()
        }
    }

    @Test
    fun multipleClipDataItems_concatsByNewLine() {
        rule.setContentAndTestDragAndDrop("aaaa") {
            drag(
                Offset(fontSize.toPx() * 2, 10f),
                createClipData {
                    addText("Hello")
                    addText("World")
                }
            )
            drop()
            assertThat(state.text.toString()).isEqualTo("aaHello\nWorldaa")
        }
    }

    private fun ComposeContentTestRule.setContentAndTestDragAndDrop(
        textContent: String = "aaaa",
        isWindowFocused: Boolean = false,
        style: TextStyle = TextStyle.Default,
        interactionSource: MutableInteractionSource? = null,
        modifier: Modifier = Modifier,
        block: DragAndDropTestScope.() -> Unit
    ) {
        val state = TextFieldState(textContent, initialSelection = TextRange.Zero)
        var view: View? = null
        val density = Density(1f, 1f)
        val mergedStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp).merge(style)
        var isHovered: State<Boolean>? = null
        setContent { // Do not use setTextFieldTestContent for DnD tests.
            view = LocalView.current
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalWindowInfo provides
                    object : WindowInfo {
                        override val isWindowFocused = isWindowFocused
                    }
            ) {
                isHovered = interactionSource?.collectIsHoveredAsState()
                BasicTextField(
                    state = state,
                    textStyle = mergedStyle,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    interactionSource = interactionSource,
                    modifier = modifier
                )
            }
        }

        testDragAndDrop(view!!, density) {
            DragAndDropTestScope(state, mergedStyle.fontSize, isHovered, this).block()
        }
    }

    private class DragAndDropTestScope(
        val state: TextFieldState,
        val fontSize: TextUnit,
        isHovered: State<Boolean>?,
        dragAndDropScopeImpl: DragAndDropScope,
    ) : DragAndDropScope by dragAndDropScopeImpl {
        val isHovered: Boolean by (isHovered ?: mutableStateOf(false))
    }
}

private val defaultUri = Uri.parse("content://com.example/content.jpg")
