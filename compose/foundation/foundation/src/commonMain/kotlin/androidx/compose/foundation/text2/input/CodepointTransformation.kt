/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.appendCodePointX
import androidx.compose.runtime.Stable

/**
 * Visual transformation interface for input fields.
 *
 * This interface is responsible for 1-to-1 mapping of every codepoint in input state to another
 * codepoint before text is rendered. Visual transformation is useful when the underlying source
 * of input needs to remain but rendered content should look different, e.g. password obscuring.
 */
@ExperimentalFoundationApi
fun interface CodepointTransformation {

    /**
     * Transforms a single [codepoint] located at [codepointIndex] to another codepoint.
     *
     * A codepoint is an integer that always maps to a single character. Every codepoint in Unicode
     * is comprised of 16 bits, 2 bytes.
     */
    // TODO: add more codepoint explanation or doc referral
    fun transform(codepointIndex: Int, codepoint: Int): Int

    companion object {

        @Stable
        val None = CodepointTransformation { _, codepoint -> codepoint }
    }
}

/**
 * Creates a masking [CodepointTransformation] that maps all codepoints to a specific [character].
 */
@ExperimentalFoundationApi
fun CodepointTransformation.Companion.mask(character: Char): CodepointTransformation =
    MaskCodepointTransformation(character)

@OptIn(ExperimentalFoundationApi::class)
private class MaskCodepointTransformation(val character: Char) : CodepointTransformation {
    override fun transform(codepointIndex: Int, codepoint: Int): Int {
        return character.code
    }

    override fun toString(): String {
        return "MaskCodepointTransformation(character=$character)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MaskCodepointTransformation) return false

        if (character != other.character) return false

        return true
    }

    override fun hashCode(): Int {
        return character.hashCode()
    }
}

/**
 * [CodepointTransformation] that converts all line breaks (\n) into white space(U+0020) and
 * carriage returns(\r) to zero-width no-break space (U+FEFF). This transformation forces any
 * content to appear as single line.
 */
@OptIn(ExperimentalFoundationApi::class)
internal object SingleLineCodepointTransformation : CodepointTransformation {

    private const val LINE_FEED = '\n'.code
    private const val CARRIAGE_RETURN = '\r'.code

    private const val WHITESPACE = ' '.code
    private const val ZERO_WIDTH_SPACE = '\uFEFF'.code

    override fun transform(codepointIndex: Int, codepoint: Int): Int {
        if (codepoint == LINE_FEED) return WHITESPACE
        if (codepoint == CARRIAGE_RETURN) return ZERO_WIDTH_SPACE
        return codepoint
    }

    override fun toString(): String {
        return "SingleLineCodepointTransformation"
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun CharSequence.toVisualText(
    codepointTransformation: CodepointTransformation?
): CharSequence {
    codepointTransformation ?: return this
    val text = this
    return buildString {
        (0 until Character.codePointCount(text, 0, text.length)).forEach { codepointIndex ->
            val codepoint = codepointTransformation.transform(
                codepointIndex, Character.codePointAt(text, codepointIndex)
            )
            appendCodePointX(codepoint)
        }
    }
}