/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.RenderingTestScope
import androidx.compose.ui.platform.renderingTest
import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import org.junit.Test

class DesktopDialogTest {

    @Test
    fun scrimDisappearsAfterDialogHideAnimation() = renderingTest(width = 200, height = 200) {
        var showDialog by mutableStateOf(true)

        setContent {
            if (showDialog) {
                Dialog(onDismissRequest = {}) {
                    Box(Modifier.size(50.dp))
                }
            }
        }

        // Settle the shown state (the appearance animation also runs through the frame loop).
        awaitNextRender()
        skipRendersUntilIdle()
        assertEquals(Color.Black.copy(alpha = 0.6f), colorOfCornerPixel())

        // Dismiss the dialog and let the on-demand loop run the hide animation to completion.
        showDialog = false
        skipRendersUntilIdle()

        assertEquals(Color.Transparent, colorOfCornerPixel())
    }

    private fun RenderingTestScope.colorOfCornerPixel(): Color =
        surface.makeImageSnapshot().toComposeImageBitmap().toPixelMap()[0, 0]
}
