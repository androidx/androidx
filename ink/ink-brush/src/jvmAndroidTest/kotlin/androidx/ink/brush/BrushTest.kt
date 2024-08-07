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

package androidx.ink.brush

import androidx.ink.brush.color.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class BrushTest {
    @Test
    fun testSetAndGetColor() {
        val originalColor = Color.Cyan.value.toLong()
        val brush = Brush(color = originalColor, size = 2.5f)
        assertEquals(brush.color, originalColor)
    }

    @Test
    fun testSetAndGetSize() {
        val brush = Brush(color = Color.DarkGray.value.toLong(), size = 2.5f)
        assertEquals(brush.size, 2.5f)
    }
}
