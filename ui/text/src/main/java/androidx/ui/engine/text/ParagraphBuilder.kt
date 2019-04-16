/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.engine.text

import java.util.LinkedList
import java.util.Stack

/**
 * Builds a [Paragraph] containing text with the given styling information.
 *
 * To set the paragraph's alignment, truncation, and ellipsising behavior, pass
 * an appropriately-configured [ParagraphStyle] object to the [new
 * ParagraphBuilder] constructor.
 *
 * Then, call combinations of [pushStyle], [addText], and [pop] to add styled
 * text to the object.
 *
 * Finally, call [build] to obtain the constructed [Paragraph] object. After
 * this point, the builder is no longer usable.
 *
 * After constructing a [Paragraph], call [Paragraph.layout] on it and then
 * paint it with [Canvas.drawParagraph].
 *
 * Creates a [ParagraphBuilder] object, which is used to create a
 * [Paragraph].
 */
class ParagraphBuilder constructor(val paragraphStyle: ParagraphStyle) {

    internal data class TextStyleIndex(val textStyle: TextStyle, val start: Int, var end: Int = -1)

    // TODO(Migration/siyamed): this should not be accessible since it is mutable, private
    private val text: StringBuilder = StringBuilder()
    private val textStyleStack: Stack<TextStyleIndex> = Stack()
    private val textStyles: LinkedList<TextStyleIndex> = LinkedList()

    /**
     * Applies the given style to the added text until [pop] is called.
     *
     * See [pop] for details.
     */
    // TODO(Migration/siyamed): this would better if it returned a builder instance. change it.
    fun pushStyle(textStyle: TextStyle) {
        val textStyleIndex = TextStyleIndex(textStyle = textStyle, start = text.length)
        textStyleStack.push(textStyleIndex)
        textStyles.add(textStyleIndex)
    }

    /**
     * Ends the effect of the most recent call to [pushStyle].
     *
     * Internally, the paragraph builder maintains a stack of text styles. Text
     * added to the paragraph is affected by all the styles in the stack. Calling
     * [pop] removes the topmost style in the stack, leaving the remaining styles
     * in effect.
     */
    // TODO(Migration/siyamed): this would better if it returned a builder instance. change it.
    fun pop() {
        if (textStyleStack.isEmpty()) return
        val styleIndex = textStyleStack.pop()
        styleIndex.end = text.length
    }

    /**
     * Adds the given text to the paragraph.
     *
     * The text will be styled according to the current stack of text styles.
     */
    // TODO(Migration/siyamed): this would better if it returned a builder instance. change it.
    fun addText(text: String) {
        this.text.append(text)
    }

    private fun consumeStyleIndexStack() {
        while (!textStyleStack.empty()) {
            pop()
        }
    }

    /**
     * Applies the given paragraph style and returns a [Paragraph] containing the
     * added text and associated styling.
     *
     * After calling this function, the paragraph builder object is invalid and
     * cannot be used further.
     */
    fun build(): Paragraph {
        consumeStyleIndexStack()
        // TODO(Migration/siyamed): paragraph->SetFontCollection(font_collection_);
        return Paragraph(text, paragraphStyle, textStyles)
    }
}