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

package androidx.ui.rendering

import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box._DebugSize
import androidx.ui.rendering.proxybox.RenderConstrainedBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SizeTest {

    @Test
    fun `Stack can layout with top, right, bottom, left 0,0`() {
        val box = RenderConstrainedBox(_additionalConstraints = BoxConstraints.tight(
                Size(100.0, 100.0)))

        box.layout(constraints = BoxConstraints())

        assertEquals(box.size!!.width, 100.0, 0.1)
        assertEquals(box.size!!.height, 100.0, 0.1)
        assertEquals(box.size, Size(100.0, 100.0))
        assertTrue(box.size is _DebugSize)
    }
}