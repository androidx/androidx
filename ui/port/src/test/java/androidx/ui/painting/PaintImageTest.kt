/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.alignment.Alignment
import androidx.ui.rendering.mockImage
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class PaintImageTest {

    @Test
    fun `Cover and align`() {
        val image = mockImage(300, 300)
        val canvas = mock<Canvas>()
        paintImage(
                canvas = canvas,
                rect = Rect.fromLTWH(50.0, 75.0, 200.0, 100.0),
                image = image,
                fit = BoxFit.cover,
                alignment = Alignment(-1.0, 0.0)
        )

        val srcCaptor = argumentCaptor<Rect>()
        val dstCaptor = argumentCaptor<Rect>()
        verify(canvas).drawImageRect(eq(image), srcCaptor.capture(), dstCaptor.capture(), any())

        assertEquals(Rect.fromLTWH(0.0, 75.0, 300.0, 150.0),
                srcCaptor.firstValue)
        assertEquals(Rect.fromLTWH(50.0, 75.0, 200.0, 100.0),
                dstCaptor.firstValue)
    }
}
