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

package androidx.compose.remote.core.operations.layout.managers

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.TextInputSession
import androidx.compose.remote.core.TextInputSupport
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout.FILTER_DIGITS
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout.FLAG_COMMIT_ON_DONE
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout.FLAG_SINGLE_LINE
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout.IME_ACTION_DONE
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout.KEYBOARD_NUMBER
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertIs

class TextInputLayoutTest {

    @Test
    fun `write then read preserves every field`() {
        // Arrange.
        val buffer = WireBuffer()
        TextInputLayout.apply(
            buffer,
            /* componentId= */ 7,
            /* animationId= */ -1,
            /* textId= */ 42,
            KEYBOARD_NUMBER,
            IME_ACTION_DONE,
            FLAG_SINGLE_LINE or FLAG_COMMIT_ON_DONE,
            /* maxLength= */ 12,
            FILTER_DIGITS,
            /* hintId= */ 43,
            TextInputLayout.CONTENT_TYPE_EMAIL_ADDRESS,
        )
        buffer.setIndex(0)

        // Act.
        assertThat(buffer.readByte()).isEqualTo(Operations.LAYOUT_TEXT_INPUT)
        val operations = ArrayList<Operation>()
        TextInputLayout.read(buffer, operations)

        // Assert.
        val op = assertIs<TextInputLayout>(operations.single())
        assertThat(op.componentId).isEqualTo(7)
        assertThat(op.textId).isEqualTo(42)
        assertThat(op.keyboardType).isEqualTo(KEYBOARD_NUMBER)
        assertThat(op.imeAction).isEqualTo(IME_ACTION_DONE)
        assertThat(op.inputFlags).isEqualTo(FLAG_SINGLE_LINE or FLAG_COMMIT_ON_DONE)
        assertThat(op.hasInputFlag(FLAG_SINGLE_LINE)).isTrue()
        assertThat(op.hasInputFlag(FLAG_COMMIT_ON_DONE)).isTrue()
        assertThat(op.hasInputFlag(TextInputLayout.FLAG_AUTO_CORRECT)).isFalse()
        assertThat(op.maxLength).isEqualTo(12)
        assertThat(op.filter).isEqualTo(FILTER_DIGITS)
        assertThat(op.hintId).isEqualTo(43)
        assertThat(op.contentType).isEqualTo(TextInputLayout.CONTENT_TYPE_EMAIL_ADDRESS)
        assertThat(buffer.available()).isFalse()
    }

    @Test
    fun `attributes at their default value are not written`() {
        // Arrange: only the hint differs from the defaults.
        val buffer = WireBuffer()
        TextInputLayout.apply(
            buffer,
            /* componentId= */ 7,
            /* animationId= */ -1,
            /* textId= */ 42,
            TextInputLayout.KEYBOARD_UNSPECIFIED,
            TextInputLayout.IME_ACTION_NONE,
            /* inputFlags= */ 0,
            /* maxLength= */ 0,
            TextInputLayout.FILTER_NONE,
            /* hintId= */ 43,
            TextInputLayout.CONTENT_TYPE_NONE,
        )
        buffer.setIndex(0)

        // Act / Assert: opcode, ids, count, then a single (id, value) pair.
        assertThat(buffer.readByte()).isEqualTo(Operations.LAYOUT_TEXT_INPUT)
        assertThat(buffer.readInt()).isEqualTo(7)
        assertThat(buffer.readInt()).isEqualTo(-1)
        assertThat(buffer.readInt()).isEqualTo(42)
        assertThat(buffer.readShort()).isEqualTo(1)
        assertThat(buffer.readByte()).isEqualTo(TextInputLayout.P_HINT_ID.toInt())
        assertThat(buffer.readInt()).isEqualTo(43)
        assertThat(buffer.available()).isFalse()

        // And reading it back restores the defaults.
        buffer.setIndex(1)
        val operations = ArrayList<Operation>()
        TextInputLayout.read(buffer, operations)
        val op = assertIs<TextInputLayout>(operations.single())
        assertThat(op.keyboardType).isEqualTo(TextInputLayout.KEYBOARD_UNSPECIFIED)
        assertThat(op.imeAction).isEqualTo(TextInputLayout.IME_ACTION_NONE)
        assertThat(op.inputFlags).isEqualTo(0)
        assertThat(op.maxLength).isEqualTo(0)
        assertThat(op.filter).isEqualTo(TextInputLayout.FILTER_NONE)
        assertThat(op.hintId).isEqualTo(43)
        assertThat(op.contentType).isEqualTo(TextInputLayout.CONTENT_TYPE_NONE)
    }

    @Test
    fun `attributes are read in any order and any subset`() {
        // Arrange: a payload written by hand, out of declaration order.
        val buffer = WireBuffer()
        buffer.start(Operations.LAYOUT_TEXT_INPUT)
        buffer.writeInt(7)
        buffer.writeInt(-1)
        buffer.writeInt(42)
        buffer.writeShort(3)
        TextInputLayout.PARAMETERS.write(
            buffer,
            TextInputLayout.P_FILTER,
            TextInputLayout.FILTER_HEXADECIMAL,
        )
        TextInputLayout.PARAMETERS.write(buffer, TextInputLayout.P_MAX_LENGTH, 8)
        TextInputLayout.PARAMETERS.write(
            buffer,
            TextInputLayout.P_KEYBOARD_TYPE,
            TextInputLayout.KEYBOARD_ASCII,
        )
        buffer.setIndex(1)

        // Act.
        val operations = ArrayList<Operation>()
        TextInputLayout.read(buffer, operations)

        // Assert: present attributes are decoded, absent ones keep their defaults.
        val op = assertIs<TextInputLayout>(operations.single())
        assertThat(op.textId).isEqualTo(42)
        assertThat(op.filter).isEqualTo(TextInputLayout.FILTER_HEXADECIMAL)
        assertThat(op.maxLength).isEqualTo(8)
        assertThat(op.keyboardType).isEqualTo(TextInputLayout.KEYBOARD_ASCII)
        assertThat(op.imeAction).isEqualTo(TextInputLayout.IME_ACTION_NONE)
        assertThat(op.hintId).isEqualTo(0)
        assertThat(buffer.available()).isFalse()
    }

    @Test
    fun `layout finds the text component bound to its string`() {
        // Arrange / Act.
        val context = HeadlessRemoteContext()
        val document = createEditableDocument(context)

        // Assert.
        val layout = assertIs<TextInputLayout>(document.getComponent(INPUT_COMPONENT_ID))
        val text = assertIs<CoreText>(document.getComponent(TEXT_COMPONENT_ID))
        assertThat(layout.textComponent).isSameInstanceAs(text)
        assertThat(layout.width).isEqualTo(100f)
        assertThat(layout.height).isEqualTo(40f)
        assertThat(layout.isEditing).isFalse()
    }

    @Test
    fun `click without text input support is not handled`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val document = createEditableDocument(context)

        // Act.
        val handled = document.onClick(context, 50f, 20f)

        // Assert.
        assertThat(handled).isFalse()
        val layout = assertIs<TextInputLayout>(document.getComponent(INPUT_COMPONENT_ID))
        assertThat(layout.isEditing).isFalse()
    }

    @Test
    fun `click starts a session over the text component bounds`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)

        // Act.
        val handled = document.onClick(context, 50f, 20f)

        // Assert.
        assertThat(handled).isTrue()
        val session = support.started.single()
        assertThat(session.isActive).isTrue()
        assertThat(session.componentId).isEqualTo(INPUT_COMPONENT_ID)
        val layout = session.layout
        assertThat(layout.isEditing).isTrue()
        assertThat(layout.session).isSameInstanceAs(session)
        assertThat(layout.keyboardType).isEqualTo(KEYBOARD_NUMBER)
        assertThat(layout.imeAction).isEqualTo(IME_ACTION_DONE)
        assertThat(layout.maxLength).isEqualTo(12)
        val text = assertIs<CoreText>(session.textComponent)
        assertThat(text.fontSizeValue).isEqualTo(16f)
        assertThat(text.colorValue).isEqualTo(-0x1000000)
        assertThat(session.text).isEqualTo("hello")
        assertThat(session.hint).isEqualTo("type here")
        // The layout is offset by 10,10 in the root box; the text is inset by 4 in the layout.
        assertThat(session.bounds.toList()).containsExactly(14f, 14f, 92f, 32f).inOrder()
    }

    @Test
    fun `without a text component the session covers the layout`() {
        // Arrange: the content is a plain box.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support, textContent = false)

        // Act.
        val handled = document.onClick(context, 50f, 20f)

        // Assert.
        assertThat(handled).isTrue()
        val session = support.started.single()
        assertThat(session.textComponent).isNull()
        assertThat(session.layout.textComponent).isNull()
        assertThat(session.bounds.toList()).containsExactly(10f, 10f, 100f, 40f).inOrder()
        assertThat(session.text).isEqualTo("hello")
    }

    @Test
    fun `click outside the layout does not start a session`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)

        // Act.
        document.onClick(context, 150f, 150f)

        // Assert.
        assertThat(support.started).isEmpty()
    }

    @Test
    fun `clicking again while editing does not start a second session`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)
        document.onClick(context, 50f, 20f)

        // Act.
        val handled = document.onClick(context, 50f, 20f)

        // Assert.
        assertThat(handled).isTrue()
        assertThat(support.started).hasSize(1)
    }

    @Test
    fun `setText updates the bound string`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)
        document.onClick(context, 50f, 20f)
        val session = support.started.single()

        // Act.
        session.setText("world")

        // Assert.
        assertThat(context.getText(session.layout.textId)).isEqualTo("world")
        assertThat(session.text).isEqualTo("world")
    }

    @Test
    fun `ending the session restores the layout and allows a new one`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)
        document.onClick(context, 50f, 20f)
        val session = support.started.single()

        // Act.
        session.end()
        session.end() // idempotent
        session.setText("ignored")

        // Assert.
        assertThat(session.isActive).isFalse()
        assertThat(session.layout.isEditing).isFalse()
        assertThat(session.layout.session).isNull()
        assertThat(context.getText(session.layout.textId)).isEqualTo("hello")

        // Act again: a new click starts a fresh session.
        document.onClick(context, 50f, 20f)
        assertThat(support.started).hasSize(2)
        assertThat(support.started[1]).isNotSameInstanceAs(session)
    }

    @Test
    fun `stopTextInput from the document tells the host and ends the session`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)
        document.onClick(context, 50f, 20f)
        val session = support.started.single()
        val layout = session.layout

        // Act.
        layout.stopTextInput(context)

        // Assert.
        assertThat(support.stopped).containsExactly(session)
        assertThat(session.isActive).isFalse()
        assertThat(layout.session).isNull()
        assertThat(layout.isEditing).isFalse()
    }

    @Test
    fun `repainting while editing reports bounds changes to the host`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)
        document.onClick(context, 50f, 20f)
        val session = support.started.single()

        // Act: a repaint with unchanged bounds, then one after the layout moved.
        document.paint(context, Theme.UNSPECIFIED)
        val unchanged = support.updated.size
        session.component.setX(30f)
        document.paint(context, Theme.UNSPECIFIED)

        // Assert.
        assertThat(unchanged).isEqualTo(0)
        assertThat(support.updated).containsExactly(session)
        // The layout's x is relative to its parent, whose padding adds 10; the text is inset by 4.
        assertThat(session.bounds.toList()).containsExactly(44f, 14f, 92f, 32f).inOrder()
    }

    @Test
    fun `text is not drawn while editing`() {
        // Arrange.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support)
        val paintContext = context.paintContext as CountingPaintContext
        paintContext.textDraws = 0
        document.paint(context, Theme.UNSPECIFIED)
        val textDrawsBefore = paintContext.textDraws

        // Act.
        document.onClick(context, 50f, 20f)
        paintContext.textDraws = 0
        document.paint(context, Theme.UNSPECIFIED)
        val textDrawsWhileEditing = paintContext.textDraws
        support.started.single().end()
        paintContext.textDraws = 0
        document.paint(context, Theme.UNSPECIFIED)
        val textDrawsAfter = paintContext.textDraws

        // Assert.
        assertThat(textDrawsBefore).isGreaterThan(0)
        assertThat(textDrawsWhileEditing).isEqualTo(0)
        assertThat(textDrawsAfter).isEqualTo(textDrawsBefore)
        // The text component is still a child of the layout after editing.
        val layout = assertIs<TextInputLayout>(document.getComponent(INPUT_COMPONENT_ID))
        assertThat(layout.childrenComponents).hasSize(1)
    }

    @Test
    fun `nested text is found, hidden while editing and restored`() {
        // Arrange: the text sits in a decoration box inside the layout.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support, nested = true)
        val paintContext = context.paintContext as CountingPaintContext
        val layout = assertIs<TextInputLayout>(document.getComponent(INPUT_COMPONENT_ID))
        val decoration = assertIs<LayoutComponent>(document.getComponent(DECORATION_COMPONENT_ID))
        val text = assertIs<CoreText>(document.getComponent(TEXT_COMPONENT_ID))
        assertThat(layout.textComponent).isSameInstanceAs(text)
        assertThat(text.parent).isSameInstanceAs(decoration)

        // Act.
        document.onClick(context, 50f, 20f)
        paintContext.textDraws = 0
        document.paint(context, Theme.UNSPECIFIED)
        val textDrawsWhileEditing = paintContext.textDraws
        support.started.single().end()
        paintContext.textDraws = 0
        document.paint(context, Theme.UNSPECIFIED)

        // Assert: no crash, the nested text was skipped then restored in its own parent, and the
        // session covers the nested text (layout at 10,10, padding 4, decoration padding 2).
        assertThat(textDrawsWhileEditing).isEqualTo(0)
        assertThat(paintContext.textDraws).isGreaterThan(0)
        assertThat(decoration.childrenComponents).containsExactly(text)
        assertThat(support.started.single().bounds.toList())
            .containsExactly(16f, 16f, 88f, 28f)
            .inOrder()
    }

    @Test
    fun `hint is painted while empty and not editing`() {
        // Arrange: an empty editable text.
        val context = HeadlessRemoteContext()
        val support = RecordingTextInputSupport()
        val document = createEditableDocument(context, support, initialText = "")
        val paintContext = context.paintContext as CountingPaintContext

        // Idle and empty: the hint is painted, the text is not.
        paintContext.drawnTextIds.clear()
        document.paint(context, Theme.UNSPECIFIED)
        assertThat(paintContext.drawnTextIds).containsExactly(lastHintId)

        // Editing: neither is painted, the host editor shows them.
        document.onClick(context, 50f, 20f)
        paintContext.drawnTextIds.clear()
        document.paint(context, Theme.UNSPECIFIED)
        assertThat(paintContext.drawnTextIds).isEmpty()

        // Edited to a value and dismissed: the text is painted, the hint is not.
        val session = support.started.single()
        session.setText("hi")
        session.end()
        paintContext.drawnTextIds.clear()
        document.paint(context, Theme.UNSPECIFIED)
        assertThat(paintContext.drawnTextIds).containsExactly(lastTextId)
    }

    private var lastTextId = -1
    private var lastHintId = -1

    /**
     * A root box (padding 10) containing a 100x40 text input layout (padding 4) whose content is a
     * text component bound to the edited string, or a plain box when [textContent] is false.
     */
    private fun createEditableDocument(
        context: RemoteContext,
        support: TextInputSupport? = null,
        initialText: String = "hello",
        textContent: Boolean = true,
        nested: Boolean = false,
    ): CoreDocument {
        val writer =
            RemoteComposeWriter(
                RcPlatformServices.None,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 200),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(
                    Header.DOC_PROFILES,
                    RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                ),
            )
        val textId = writer.addText(initialText)
        val hintId = writer.addText("type here")
        lastTextId = textId
        lastHintId = hintId
        writer.root {
            writer.box(
                RecordingModifier().componentId(BOX_COMPONENT_ID).padding(10, 10, 10, 10),
                BoxLayout.START,
                BoxLayout.TOP,
            ) {
                writer.textInputLayout(
                    RecordingModifier()
                        .componentId(INPUT_COMPONENT_ID)
                        .size(100, 40)
                        .padding(4, 4, 4, 4),
                    textId,
                    KEYBOARD_NUMBER,
                    IME_ACTION_DONE,
                    FLAG_SINGLE_LINE,
                    /* maxLength= */ 12,
                    FILTER_DIGITS,
                    hintId,
                    TextInputLayout.CONTENT_TYPE_PERSON_NAME,
                ) {
                    if (textContent && nested) {
                        writer.startBox(
                            RecordingModifier()
                                .componentId(DECORATION_COMPONENT_ID)
                                .padding(2, 2, 2, 2),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        )
                    }
                    if (textContent) {
                        writer.textComponent(
                            RecordingModifier().componentId(TEXT_COMPONENT_ID).fillMaxSize(),
                            textId,
                            /* textStyleId= */ -1,
                            /* color= */ -0x1000000,
                            /* colorId= */ -1,
                            /* fontSize= */ 16f,
                            /* minFontSize= */ 0f,
                            /* maxFontSize= */ 0f,
                            /* fontStyle= */ 0,
                            /* fontWeight= */ 400f,
                            /* fontFamily= */ null,
                            /* textAlign= */ 0,
                            /* overflow= */ 0,
                            /* maxLines= */ 1,
                            /* letterSpacing= */ 0f,
                            /* lineHeightAdd= */ 0f,
                            /* lineHeightMultiplier= */ 1f,
                            /* lineBreakStrategy= */ 0,
                            /* hyphenationFrequency= */ 0,
                            /* justificationMode= */ 0,
                            /* underline= */ false,
                            /* strikethrough= */ false,
                            /* fontAxis= */ null,
                            /* fontAxisValues= */ null,
                            /* autosize= */ false,
                            /* flags= */ 0,
                        ) {}
                    } else {
                        writer.box(
                            RecordingModifier().fillMaxSize(),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        )
                    }
                    if (textContent && nested) {
                        writer.endBox()
                    }
                }
            }
        }
        val document =
            CoreDocument().apply {
                initFromBuffer(
                    RemoteComposeBuffer.fromInputStream(
                        ByteArrayInputStream(writer.encodeToByteArray())
                    )
                )
            }
        context.setDocument(document)
        context.touchVersion = LayoutManager.FIX_TOUCH_EVENT
        val paintContext = CountingPaintContext(context)
        paintContext.setTextInputSupport(support)
        context.paintContext = paintContext
        context.mWidth = 200f
        context.mHeight = 200f
        document.initializeContext(context)
        document.paint(context, Theme.UNSPECIFIED)
        return document
    }

    /**
     * Counts text draws so a test can tell whether the text component painted its text. Glyphs are
     * not rasterized: that needs AWT font support (libfreetype), which presubmit hosts lack.
     */
    private class CountingPaintContext(context: RemoteContext) :
        SoftwareRasterPaintContext(context, 200, 200) {
        var textDraws = 0
        val drawnTextIds = mutableListOf<Int>()

        override fun drawTextRun(
            textID: Int,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            rtl: Boolean,
        ) {
            if (end <= start) {
                // An empty text still issues a (visually empty) run; ignore it.
                return
            }
            textDraws++
            drawnTextIds.add(textID)
        }
    }

    private class RecordingTextInputSupport : TextInputSupport {
        val started = mutableListOf<TextInputSession>()
        val updated = mutableListOf<TextInputSession>()
        val stopped = mutableListOf<TextInputSession>()

        override fun startTextInput(session: TextInputSession) {
            started.add(session)
        }

        override fun updateTextInputBounds(session: TextInputSession) {
            updated.add(session)
        }

        override fun stopTextInput(session: TextInputSession) {
            stopped.add(session)
        }
    }

    private companion object {
        const val BOX_COMPONENT_ID = 2
        const val INPUT_COMPONENT_ID = 3
        const val TEXT_COMPONENT_ID = 4
        const val DECORATION_COMPONENT_ID = 5
    }
}
