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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class HoverableTest {
    // Test that an element appearing under the pointer receives the synthetic pointer-enter event
    // when listening to it via InteractionSource.collectHoveredAsState.
    // https://youtrack.jetbrains.com/issue/COMPOSE-666/Hover-does-not-trigger-on-an-element-that-appears-on-the-cursor
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun collectHoveredAsStateReceivesSyntheticEnter() = runComposeUiTest {
        var isInnerBoxHovered = false

        setContent {
            var showInner by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .onPointerEvent(eventType = PointerEventType.Release) {
                        showInner = true
                    }
                    .testTag("container"),
                contentAlignment = Alignment.Center
            ) {
                if (showInner) {
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hoverable(interactionSource)
                    )

                    val isHovered by interactionSource.collectIsHoveredAsState()
                    LaunchedEffect(isHovered) {
                        isInnerBoxHovered = isHovered
                    }
                }
            }
        }

        onNodeWithTag("container").performClick()
        waitForIdle()
        assertTrue(isInnerBoxHovered)
    }
}