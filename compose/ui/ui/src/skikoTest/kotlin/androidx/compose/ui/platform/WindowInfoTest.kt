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

package androidx.compose.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WindowInfoTest {

    @Test
    fun windowInfoContainerSize() = runSkikoComposeUiTest(
        size = Size(234f, 432f),
        density = Density(2f),
    ) {
        lateinit var windowInfo: WindowInfo
        setContent {
            Box(Modifier.fillMaxSize().testTag("box"))
            windowInfo = LocalWindowInfo.current
        }

        val containerSize = windowInfo.containerSize
        assertEquals(234, containerSize.width)
        assertEquals(432, containerSize.height)

        val containerDpSize = windowInfo.containerDpSize
        assertEquals(117.dp, containerDpSize.width)
        assertEquals(216.dp, containerDpSize.height)

        onNodeWithTag("box").assertWidthIsEqualTo(117.dp)
        onNodeWithTag("box").assertHeightIsEqualTo(216.dp)
    }
}
