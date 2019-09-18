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
    textStyle: TextStyle = TextStyle(),
    paragraphStyle: ParagraphStyle = ParagraphStyle(),
    density: Density,
    layoutDirection: LayoutDirection,
    resourceLoader: Font.ResourceLoader
) : ParagraphIntrinsics {

    override val minIntrinsicWidth: Float

    override val maxIntrinsicWidth: Float

    /**
     * [ParagraphIntrinsics] for each paragraph included in the [annotatedString]. For empty string
     * there will be a single empty paragraph intrinsics info.
     */
    internal val infoList: List<ParagraphIntrinsicInfo>

    init {
        infoList = annotatedString
            .mapEachParagraphStyle(paragraphStyle) { annotatedString, paragraphStyleItem ->
                ParagraphIntrinsicInfo(
                    intrinsics = ParagraphIntrinsics(
                        text = annotatedString.text,
                        paragraphStyle = paragraphStyleItem.style,
                        textStyles = annotatedString.textStyles,
                        style = textStyle,
                        density = density,
                        layoutDirection = layoutDirection,
                        resourceLoader = resourceLoader
                    ),
                    startIndex = paragraphStyleItem.start,
                    endIndex = paragraphStyleItem.end
                )
            }

        minIntrinsicWidth = infoList.maxBy {
            it.intrinsics.minIntrinsicWidth
        }?.intrinsics?.minIntrinsicWidth ?: 0f

        maxIntrinsicWidth = infoList.maxBy {
            it.intrinsics.maxIntrinsicWidth
        }?.intrinsics?.maxIntrinsicWidth ?: 0f
    }
}

internal data class ParagraphIntrinsicInfo(
    val intrinsics: ParagraphIntrinsics,
    val startIndex: Int,
    val endIndex: Int
)