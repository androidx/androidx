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

package androidx.compose.ui.graphics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RootGraphicsLayerTest {
    private val size = Size(100.0f, 60.0f)

    @Test
    fun rootLayerRedrawnAfterRootRemoval() = runSkikoComposeUiTest(size) {
        var showContent by mutableStateOf(true)
        setContent {
            if (showContent) {
                Box(Modifier.fillMaxSize().background(Color.Red))
            }
        }
        waitForIdle()
        captureToImage().assertCenterPixelColor(Color.Red)

        showContent = false

        waitForIdle()
        captureToImage().assertCenterPixelColor(Color.Transparent)
    }
}

private fun ImageBitmap.assertCenterPixelColor(expectedColor: Color) {
    asSkiaBitmap().assertColor(expectedColor, width / 2, height / 2)
}
