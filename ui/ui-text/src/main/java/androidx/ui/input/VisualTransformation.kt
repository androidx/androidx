/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.input

import androidx.annotation.RestrictTo
import androidx.ui.text.AnnotatedString

/**
 * The map interface used for bidirectional offset mapping from original to transformed text.
 */
interface OffsetMap {
    /**
     * Convert offset in original text into the offset in transformed text.
     *
     * This function must be a monotonically non-decreasing function. In other words, if a cursor
     * advances in the original text, the cursor in the transformed text must advance or stay there.
     *
     * @param offset offset in original text.
     * @return offset in transformed text
     * @see VisualTransformation
     */
    fun originalToTransformed(offset: Int): Int

    /**
     * Convert offset in transformed text into the offset in original text.
     *
     * This function must be a monotonically non-decreasing function. In other words, if a cursor
     * advances in the transformed text, the cusrsor in the original text must advance or stay
     * there.
     *
     * @param offset offset in transformed text
     * @return offset in original text
     * @see VisualTransformation
     */
    fun transformedToOriginal(offset: Int): Int
}

/**
 * The transformed text with offset offset mapping
 */
data class TransformedText(
    /**
     * The transformed text
     */
    val transformedText: AnnotatedString,

    /**
     * The map used for bidirectional offset mapping from original to transformed text.
     */
    val offsetMap: OffsetMap
)

/**
 * Interface used for changing visual output of the input field.
 *
 * This interface can be used for changing visual output of the text in the input field.
 * For example, you can mask characters in password filed with asterisk with
 * PasswordVisualTransformation.
 */
interface VisualTransformation {
    /**
     * Change the visual output of given text.
     *
     * Note that the returned text length can be different length from the given text. The widget
     * will call the offset translator for converting offsets for various reasons, cursor drawing
     * position, text selection by gesture, etc.
     *
     * Example: Credit Card Visual Output (inserting hyphens each 4 digits)
     *  original text   : 1234567890123456
     *  transformed text: 1234-5678-9012-3456
     *
     *  Then, the offset translator should ignore the hyphen characters, so conversion from
     *  original offset to transformed text works like
     *  - The 4th char of the original text is 5th char in the transformed text.
     *  - The 13th char of the original text is 15th char in the transformed text.
     *  Similarly, the reverse conversion works like
     *  - The 5th char of the transformed text is 4th char in the original text.
     *  - The 12th char of the transformed text is 10th char in the original text.
     *
     *  The reference implementation would be like as follows:
     *  <pre>
     *  val creditCardOffsetTranslator = object : OffsetMap {
     *      override fun originalToTransformed(originalOffset: Int): Int {
     *          if (originalOffset <= 3) return originalOffset
     *          if (originalOffset <= 7) return originalOffset + 1
     *          if (originalOffset <= 11) return originalOffset + 2
     *          if (originalOffset <= 16) return originalOffset + 3
     *          return 19
     *      }
     *
     *      override fun transformedToOriginal(transformedOffset: Int): Int {
     *          if (transformedOffset <= 4) return transformedOffset
     *          if (transformedOffset <= 9) return transformedOffset - 1
     *          if (transformedOffset <= 14) return transformedOffset - 2
     *          if (transformedOffset <= 19) return transformedOffset - 3
     *          return 16
     *      }
     *  }
     *  </pre>
     *
     * TODO(nona): Add paragraph direction argument for determining offset conversion.
     *
     * @param text The original text
     * @return the pair of filtered text and offset translator.
     */
    fun filter(text: AnnotatedString): TransformedText
}

/**
 * The Visual Filter can be used for password Input Field.
 *
 * Note that this visual filter only works for ASCII characters.
 *
 * @param mask The mask character used instead of original text.
 */
class PasswordVisualTransformation(val mask: Char = '\u2022') : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(Character.toString(mask).repeat(text.text.length)),
            identityOffsetMap
        )
    }
}

/**
 * The offset map used for identity mapping.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val identityOffsetMap = object : OffsetMap {
    override fun originalToTransformed(offset: Int): Int = offset
    override fun transformedToOriginal(offset: Int): Int = offset
}
