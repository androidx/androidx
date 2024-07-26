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

package androidx.compose.material3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.util.fastForEach
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class InteractionSourceModifierNodeTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun interactionSourceData_findInteractionSources() {
        lateinit var sources: MutableState<List<InteractionSource>>
        lateinit var scope: CoroutineScope
        var childElementPressed = false

        rule.setContent {
            sources = remember { mutableStateOf(emptyList()) }
            scope = rememberCoroutineScope()
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier =
                    Modifier.onChildrenInteractionSourceChange { interactionSources ->
                        if (sources.value != interactionSources) {
                            sources.value = interactionSources
                        }
                    }
            ) {
                Box(
                    modifier =
                        Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                                onClick = {}
                            )
                            .interactionSourceData(interactionSource)
                )
            }
        }

        rule.waitForIdle()
        // Set up observer and change childElementPressd to true
        sources.value.fastForEach { interactionSource ->
            scope.launch {
                interactionSource.interactions.collectLatest { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            childElementPressed = true
                        }
                    }
                }
            }
        }

        rule.onNode(hasClickAction()).performClick()

        assertTrue(childElementPressed)
    }
}
