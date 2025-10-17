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

package androidx.compose.ui.platform

import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class WebWindowInfoTest : OnCanvasTests {

    @Test
    fun windowInfoContainerSizeIsSet() = runTest {
        lateinit var windowInfo: WindowInfo
        createComposeWindow {
            windowInfo = LocalWindowInfo.current
        }

        val containerSize = windowInfo.containerSize
        assertTrue(containerSize.width > 0)
        assertTrue(containerSize.height > 0)

        val containerDpSize = windowInfo.containerDpSize
        assertTrue(containerDpSize.width > 0.dp)
        assertTrue(containerDpSize.height > 0.dp)
    }
}
