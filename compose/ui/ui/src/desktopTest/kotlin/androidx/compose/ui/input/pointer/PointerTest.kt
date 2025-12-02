/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.awt.Point
import java.awt.Toolkit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class PointerTest {

    @Test
    fun customPointerChanges() = runComposeUiTest {
        val toolkit = Toolkit.getDefaultToolkit()
        val icon1 = PointerIcon(
            toolkit.createCustomCursor(
                ImageBitmap(32, 32).toAwtImage(),
                Point(16, 16),
                "cursor1")
        )
        val icon2 = PointerIcon(
            toolkit.createCustomCursor(
                ImageBitmap(32, 32).toAwtImage(),
                Point(0, 0),
                "cursor2"
            )
        )

        var requestedPointer by mutableStateOf(icon1)
        var pointerService: PointerIconService? = null
        setContent {
            pointerService = LocalPointerIconService.current
            Box(
                Modifier
                    .size(100.dp)
                    .pointerHoverIcon(requestedPointer)
                    .testTag("box")
            )
        }

        assertEquals(PointerIcon.Default, pointerService?.getIcon())

        onNodeWithTag("box").performMouseInput {
            moveTo(center)
        }
        assertSame(icon1, pointerService?.getIcon())
        requestedPointer = icon2
        waitForIdle()
        assertSame(icon2, pointerService?.getIcon())
    }

}