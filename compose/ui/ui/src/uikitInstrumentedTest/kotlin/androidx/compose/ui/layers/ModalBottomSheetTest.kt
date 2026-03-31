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

package androidx.compose.ui.layers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import kotlin.test.Test
import kotlin.test.assertEquals

class ModalBottomSheetTest {
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun testContentBottomInset() = runUIKitInstrumentedTest {
        var contentRect = DpRectZero()

        setContent {
            ModalBottomSheet(
                onDismissRequest = {},
                content = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .onGloballyPositioned { contentRect = it.boundsInWindow().toDpRect(density) }
                    )
                }
            )
        }

        assertEquals(safeDrawingRect.bottom, contentRect.bottom)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun testContentBottomInsetWithKeyboardOpen() = runUIKitInstrumentedTest {
        val focusRequester = FocusRequester()
        var contentRect = DpRectZero()

        setContent {
            ModalBottomSheet(
                onDismissRequest = {},
                content = {
                    TextField(
                        value = "",
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onGloballyPositioned { contentRect = it.boundsInWindow().toDpRect(density) }
                    )

                }
            )
        }

        focusRequester.requestFocus()

        waitForIdle()

        assertEquals(screenSize.height - keyboardHeight, contentRect.bottom)
    }
}