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

package androidx.compose.ui.text.android

import android.graphics.Canvas
import android.graphics.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TextAndroidCanvasTest {

    @Test
    fun getClipBounds_useOverrideValue() {
        val textCanvas = TextAndroidCanvas()
        val canvas = mock<Canvas>()
        whenever(canvas.getClipBounds(any())).thenReturn(true)
        textCanvas.setCanvas(canvas)

        val rect = Rect()
        assertThat(textCanvas.getClipBounds(rect)).isTrue()
        assertThat(rect.height()).isEqualTo(Int.MAX_VALUE)
    }
}
