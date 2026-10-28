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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.runner.RunWith

private val rtlWord = RtlChar.repeat(5)

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class TextField2SelectionGesturesRtlTest :
    TextField2SelectionGesturesTest(
        initialText = "$rtlWord\n$rtlWord $rtlWord $rtlWord\n$rtlWord",
        layoutDirection = LayoutDirection.Rtl,
    ) {
    override val word = rtlWord
    override var textDirection: ResolvedTextDirection = ResolvedTextDirection.Rtl

    override fun characterPosition(offset: Int): Offset {
        val textLayoutResult = rule.onNodeWithTag(pointerAreaTag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset).centerRight.nudge(HorizontalDirection.END)
    }
}
