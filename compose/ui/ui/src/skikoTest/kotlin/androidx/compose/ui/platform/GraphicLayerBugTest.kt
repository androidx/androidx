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

package androidx.compose.ui.platform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

// Tests for fixed bugs
class GraphicLayerBugTest {
    // https://github.com/JetBrains/compose-multiplatform/issues/4681
    @OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)
    @Test
    fun draw_invalidates_inside_complex_layout() =
        runComposeUiTest {
            var valueForDraw by mutableStateOf(0f)
            var lastDrawnValue = -1f

            setContent {
                val pagerState = rememberPagerState { 1 }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Spacer(Modifier.drawBehind { pagerState.targetPage })
                    },
                ) {
                    HorizontalPager(modifier = Modifier.fillMaxSize(), state = pagerState) {
                        var value by remember { mutableStateOf("") }
                        TextField(
                            value = value,
                            onValueChange = { value = it },
                        )

                        Canvas(Modifier.size(40.dp)) {
                            lastDrawnValue = valueForDraw
                        }
                    }
                }
            }

            waitForIdle()
            assertThat(lastDrawnValue).isEqualTo(0f)

            valueForDraw = 1f
            waitForIdle()
            assertThat(lastDrawnValue).isEqualTo(1f)
        }
}
