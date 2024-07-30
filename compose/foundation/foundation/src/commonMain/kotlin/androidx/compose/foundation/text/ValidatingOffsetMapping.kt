/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.min

internal val ValidatingEmptyOffsetMappingIdentity: OffsetMapping =
    ValidatingOffsetMapping(
        delegate = OffsetMapping.Identity,
        originalLength = 0,
        transformedLength = 0
    )

internal fun VisualTransformation.filterWithValidation(text: AnnotatedString): TransformedText {
    val delegate = filter(text)
    // first throw if the transformation is faulty right away (limit 100)
    delegate.throwIfNotValidTransform(text.length)
    // we can't actually assume that transformations are pure, so add a runtime check to throw
    // better error messages at every transformation as well
    //
    // we also don't pre-validate more than 100 indexes, so faults may occur at end
    return TransformedText(
        delegate.text,
        ValidatingOffsetMapping(
            delegate = delegate.offsetMapping,
            originalLength = text.length,
            transformedLength = delegate.text.length
        )
    )
}

/**
 * Assuming TransformedText is a pure mapping this will validate:
 * 1. The first limit characters map to a valid transformed offset
 * 2. The first limit characters of transformed map to valid original offsets
 * 3. The last position for both transformed and original (catching off by 1)
 *
 * @param limit how many offsets to check (default 100)
 */
@VisibleForTesting
internal fun TransformedText.throwIfNotValidTransform(originalLength: Int, limit: Int = 100) {
    // validate originalToTransformed [0..limit] + last position
    val transformedLength = text.length
    for (offset in 0 until min(originalLength, limit)) {
        val transformedOffset = offsetMapping.originalToTransformed(offset)
        validateOriginalToTransformed(transformedOffset, transformedLength, offset)
    }
    val transformedOffset = offsetMapping.originalToTransformed(originalLength)
    validateOriginalToTransformed(transformedOffset, transformedLength, originalLength)

    // validate transformedToOriginal [0..limit] + last position
    for (offset in 0 until min(transformedLength, limit)) {
        val originalOffset = offsetMapping.transformedToOriginal(offset)
        validateTransformedToOriginal(originalOffset, originalLength, offset)
    }

    val originalOffset = offsetMapping.transformedToOriginal(transformedLength)
    validateTransformedToOriginal(originalOffset, originalLength, transformedLength)
}

private class ValidatingOffsetMapping(
    private val delegate: OffsetMapping,
    private val originalLength: Int,
    private val transformedLength: Int
) : OffsetMapping {

    /**
     * Calls [originalToTransformed][OffsetMapping.originalToTransformed] and throws a detailed
     * exception if the returned value is outside the range of indices [0, [transformedLength]].
     */
    override fun originalToTransformed(offset: Int): Int {
        return delegate.originalToTransformed(offset).also { transformedOffset ->
            if (offset in 0..originalLength) {
                // Only validate actually valid requests. The system is responsible for calling
                // these functions correctly.
                validateOriginalToTransformed(transformedOffset, transformedLength, offset)
            }
        }
    }

    /**
     * Calls [transformedToOriginal][OffsetMapping.transformedToOriginal] and throws a detailed
     * exception if the returned value is outside the range of indices [0, [originalLength]].
     */
    override fun transformedToOriginal(offset: Int): Int {
        return delegate.transformedToOriginal(offset).also { originalOffset ->
            if (offset in 0..transformedLength) {
                // Only validate actually valid requests. The system is responsible for calling
                // these functions correctly.
                validateTransformedToOriginal(originalOffset, originalLength, offset)
            }
        }
    }
}

private fun validateTransformedToOriginal(originalOffset: Int, originalLength: Int, offset: Int) {
    checkPrecondition(originalOffset in 0..originalLength) {
        "OffsetMapping.transformedToOriginal returned invalid mapping: " +
            "$offset -> $originalOffset is not in range of original text " +
            "[0, $originalLength]"
    }
}

private fun validateOriginalToTransformed(
    transformedOffset: Int,
    transformedLength: Int,
    offset: Int
) {
    checkPrecondition(transformedOffset in 0..transformedLength) {
        "OffsetMapping.originalToTransformed returned invalid mapping: " +
            "$offset -> $transformedOffset is not in range of transformed text " +
            "[0, $transformedLength]"
    }
}
