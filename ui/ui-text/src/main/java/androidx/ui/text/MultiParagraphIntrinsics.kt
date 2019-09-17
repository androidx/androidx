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

package androidx.ui.text

import androidx.annotation.RestrictTo
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.text.font.Font

/**
 * Calculates and provides the intrinsic width and height of text that contains [ParagraphStyle].
 *
 * @see MultiParagraph
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MultiParagraphIntrinsics(
    val annotatedString: AnnotatedString,
    val textStyle: TextStyle = TextStyle(),
    val paragraphStyle: ParagraphStyle = ParagraphStyle(),
    val density: Density,
    val layoutDirection: LayoutDirection,
    val resourceLoader: Font.ResourceLoader
) : ParagraphIntrinsics {

    override val minIntrinsicWidth: Float

    override val maxIntrinsicWidth: Float

    init {
        val paragraphIntrinsicsList = annotatedString
            .forEachParagraphStyle(paragraphStyle) { annotatedString, paragraphStyleItem ->
                ParagraphIntrinsics(
                    text = annotatedString.text,
                    paragraphStyle = paragraphStyleItem.style,
                    textStyles = annotatedString.textStyles,
                    style = textStyle,
                    density = density,
                    layoutDirection = layoutDirection,
                    resourceLoader = resourceLoader
                )
            }

        minIntrinsicWidth = paragraphIntrinsicsList.map { it.minIntrinsicWidth }.max() ?: 0f
        maxIntrinsicWidth = paragraphIntrinsicsList.map { it.maxIntrinsicWidth }.max() ?: 0f
    }
}