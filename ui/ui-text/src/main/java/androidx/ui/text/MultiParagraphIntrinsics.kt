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

import androidx.ui.core.Density
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextDirectionAlgorithm

/**
 * Calculates and provides the intrinsic width and height of text that contains [ParagraphStyle].
 *
 * @see MultiParagraph
 *
 * @throws IllegalArgumentException if [ParagraphStyle.textDirectionAlgorithm] is not set
 *
 */
class MultiParagraphIntrinsics(
    val annotatedString: AnnotatedString,
    textStyle: TextStyle,
    paragraphStyle: ParagraphStyle,
    density: Density,
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
        requireNotNull(paragraphStyle.textDirectionAlgorithm) {
            "ParagraphStyle.textDirectionAlgorithm should not be null"
        }

        infoList = annotatedString
            .mapEachParagraphStyle(paragraphStyle) { annotatedString, paragraphStyleItem ->
                ParagraphIntrinsicInfo(
                    intrinsics = ParagraphIntrinsics(
                        text = annotatedString.text,
                        paragraphStyle = resolveTextDirection(
                            paragraphStyleItem.item,
                            paragraphStyle
                        ),
                        textStyles = annotatedString.textStyles,
                        style = textStyle,
                        density = density,
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

    /**
     * if the [style] does `not` have [TextDirectionAlgorithm] set, it will return a new
     * [ParagraphStyle] where [TextDirectionAlgorithm] is set using the [defaultStyle]. Otherwise
     * returns the same [style] object.
     *
     * @param style ParagraphStyle to be checked for [TextDirectionAlgorithm]
     * @param defaultStyle [ParagraphStyle] passed to [MultiParagraphIntrinsics] as the main style
     */
    private fun resolveTextDirection(style: ParagraphStyle, defaultStyle: ParagraphStyle):
            ParagraphStyle {
        return style.textDirectionAlgorithm?.let { style } ?: style.copy(
            textDirectionAlgorithm = defaultStyle.textDirectionAlgorithm
        )
    }
}

internal data class ParagraphIntrinsicInfo(
    val intrinsics: ParagraphIntrinsics,
    val startIndex: Int,
    val endIndex: Int
)