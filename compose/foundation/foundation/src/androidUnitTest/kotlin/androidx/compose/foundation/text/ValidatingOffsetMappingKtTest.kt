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
package androidx.compose.foundation.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ValidatingOffsetMappingKtTest {

    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun params() = (0..100).toList()
    }

    @Parameterized.Parameter(0)
    @JvmField
    var index: Int = -1
    private val maxValue = 10

    private val longText = "a".repeat(100)

    @Test(expected = IllegalStateException::class)
    fun throwIfNotValidTransform_detectsErrorsInAllPositionsUntilMax() {
        assumeTrue(index < maxValue)
        throwOriginalToTransformed(index).throwIfNotValidTransform(longText.length, maxValue)
    }

    @Test(expected = IllegalStateException::class)
    fun throwIfNotValidOriginal_detectsErrorsInAllPositionsUntilMax() {
        assumeTrue(index < maxValue)
        throwTransformedToOriginal(index).throwIfNotValidTransform(longText.length, maxValue)
    }

    @Test
    fun dontThrowAfterMaxBeforeLast() {
        assumeTrue(index >= maxValue && index < longText.length)
        throwOriginalToTransformed(index).throwIfNotValidTransform(longText.length, maxValue)
        throwTransformedToOriginal(index).throwIfNotValidTransform(longText.length, maxValue)
    }

    @Test(expected = IllegalStateException::class)
    fun throwAtLastPosition_transformed() {
        assumeTrue(index == longText.length)
        throwOriginalToTransformed(index).throwIfNotValidTransform(longText.length, maxValue)
    }

    @Test(expected = IllegalStateException::class)
    fun throwAtLastPosition_original() {
        assumeTrue(index == longText.length)
        throwTransformedToOriginal(index).throwIfNotValidTransform(longText.length, maxValue)
    }

    private fun throwOriginalToTransformed(throwAtPosition: Int) = TransformedText(
        AnnotatedString(longText),
        offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset == throwAtPosition)
                Int.MAX_VALUE
            else
                offset

            override fun transformedToOriginal(offset: Int) = offset
        }
    )

    private fun throwTransformedToOriginal(throwAtPosition: Int) = TransformedText(
        AnnotatedString(longText),
        offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset
            override fun transformedToOriginal(offset: Int) = if (offset == throwAtPosition)
                Int.MAX_VALUE
            else
                offset
        }
    )
}
