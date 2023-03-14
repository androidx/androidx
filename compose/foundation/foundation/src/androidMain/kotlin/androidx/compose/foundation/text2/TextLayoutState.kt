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

package androidx.compose.foundation.text2

import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.updateTextDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

@OptIn(InternalFoundationTextApi::class)
internal class TextLayoutState(initialTextDelegate: TextDelegate) {
    /**
     * Set of parameters and an internal cache to compute text layout.
     */
    var textDelegate: TextDelegate by mutableStateOf(initialTextDelegate)
        private set

    /**
     * Text Layout State.
     */
    var layoutResult: TextLayoutResult? by mutableStateOf(null)
        private set

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

        val newTextDelegate = updateTextDelegate(
            current = textDelegate,
            text = text,
            style = textStyle,
            softWrap = softWrap,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList(),
        )

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