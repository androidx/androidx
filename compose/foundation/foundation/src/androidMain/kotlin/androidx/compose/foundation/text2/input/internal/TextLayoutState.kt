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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.updateTextDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/**
 * Manages text layout for TextField including layout coordinates of decoration box and inner text
 * field.
 */
@OptIn(InternalFoundationTextApi::class)
internal class TextLayoutState {
    /**
     * Set of parameters and an internal cache to compute text layout.
     */
    var textDelegate: TextDelegate? = null
        private set

    /**
     * Text Layout State.
     */
    var layoutResult: TextLayoutResult? by mutableStateOf(null)
        private set

    /** Measured bounds of the decoration box and inner text field. Together used to
     * calculate the relative touch offset. Because touches are applied on the decoration box, we
     * need to translate it to the inner text field coordinates.
     *
     * [LayoutCoordinates] object returned from onGloballyPositioned callback is usually the same
     * instance unless a node is detached and re-attached to the tree. To react to layout and
     * positional changes even though the object never changes, we employ a neverEqualPolicy.
     */
    var innerTextFieldCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
    var decorationBoxCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    fun MeasureScope.layout(
        text: AnnotatedString,
        textStyle: TextStyle,
        softWrap: Boolean,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        constraints: Constraints,
        onTextLayout: Density.(TextLayoutResult) -> Unit
    ): TextLayoutResult {
        val prevResult = Snapshot.withoutReadObservation { layoutResult }

        val currTextDelegate = textDelegate

        val newTextDelegate = if (currTextDelegate != null) {
            updateTextDelegate(
                current = currTextDelegate,
                text = text,
                style = textStyle,
                softWrap = softWrap,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = emptyList(),
            )
        } else {
            TextDelegate(
                text = text,
                style = textStyle,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = true,
                placeholders = emptyList()
            )
        }

        return newTextDelegate.layout(
            layoutDirection = layoutDirection,
            constraints = constraints,
            prevResult = prevResult
        ).also {
            textDelegate = newTextDelegate
            if (prevResult != it) {
                onTextLayout(it)
            }
            layoutResult = it
        }
    }
}
