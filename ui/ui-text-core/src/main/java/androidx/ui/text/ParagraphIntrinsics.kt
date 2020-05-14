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

import androidx.ui.text.font.Font
import androidx.ui.text.platform.AndroidParagraphIntrinsics
import androidx.ui.text.platform.TypefaceAdapter
import androidx.ui.unit.Density

/**
 * Calculates and presents the intrinsic width and height of text.
 */
interface ParagraphIntrinsics {
    /**
     * The width for text if all soft wrap opportunities were taken.
     */
    val minIntrinsicWidth: Float

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the height.
     */
    val maxIntrinsicWidth: Float
}

/**
 * @throws IllegalArgumentException if [ParagraphStyle.textDirectionAlgorithm] is not set
 *
 * @see ParagraphIntrinsics
 */
/* actual */ fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    density: Density,
    resourceLoader: Font.ResourceLoader
): ParagraphIntrinsics {
    return AndroidParagraphIntrinsics(
        text = text,
        style = style,
        placeholders = placeholders,
        typefaceAdapter = TypefaceAdapter(
            resourceLoader = resourceLoader
        ),
        spanStyles = spanStyles,
        density = density
    )
}
