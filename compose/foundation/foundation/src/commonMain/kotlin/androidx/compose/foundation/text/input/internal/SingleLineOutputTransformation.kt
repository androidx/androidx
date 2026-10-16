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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * [OutputTransformation] that converts all line breaks (\n) into white space(U+0020) and carriage
 * returns(\r) to zero-width no-break space (U+FEFF). This transformation forces any content to
 * appear as single line.
 */
internal object SingleLineOutputTransformation : OutputTransformation {

    private const val LINE_FEED = '\n'
    private const val CARRIAGE_RETURN = '\r'

    private const val WHITESPACE = " "
    private const val ZERO_WIDTH_SPACE = "\uFEFF"

    override fun TextFieldBuffer.transformOutput() {
        for (i in 0 until length) {
            val char = asCharSequence()[i]
            if (char == LINE_FEED) replace(i, i + 1, WHITESPACE)
            if (char == CARRIAGE_RETURN) replace(i, i + 1, ZERO_WIDTH_SPACE)
        }
    }

    override fun toString(): String {
        return "SingleLineOutputTransformation"
    }

    fun wrap(innerTransformation: OutputTransformation): OutputTransformation {
        return OutputTransformation {
            with(innerTransformation) { transformOutput() }
            with(SingleLineOutputTransformation) { transformOutput() }
        }
    }
}
