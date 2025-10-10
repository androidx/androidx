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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.SubcompositionReusableContentHost
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedStateAwareModifierNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class UnplacedStateAwareModifierNodeTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun calledWhenNotPlacedByParentAnymore() {
        val shouldPlace = mutableStateOf(true)
        var unplacedCount = 0
        val unplacedModifier = OnUnplacedModifierElement { unplacedCount++ }
        rule.setContent {
            Layout(content = { Box(Modifier.size(10.dp).then(unplacedModifier)) }) {
                measurables,
                constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    if (shouldPlace.value) {
                        placeable.place(0, 0)
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(unplacedCount).isEqualTo(0)
            shouldPlace.value = false
        }

        rule.runOnIdle { assertThat(unplacedCount).isEqualTo(1) }
    }

    @Test
    fun calledWhenNotComposedAnymore() {
        val shouldCompose = mutableStateOf(true)
        var unplacedCount = 0
        val unplacedModifier = OnUnplacedModifierElement { unplacedCount++ }
        rule.setContent {
            if (shouldCompose.value) {
                Box(Modifier.size(10.dp).then(unplacedModifier))
            }
        }

        rule.runOnIdle {
            assertThat(unplacedCount).isEqualTo(0)
            shouldCompose.value = false
        }

        rule.runOnIdle { assertThat(unplacedCount).isEqualTo(1) }
    }

    @Test
    fun calledWhenDeactivated() {
        val active = mutableStateOf(true)
        var unplacedCount = 0
        val unplacedModifier = OnUnplacedModifierElement { unplacedCount++ }
        rule.setContent {
            SubcompositionReusableContentHost(active = active.value) {
                Box(Modifier.size(10.dp).then(unplacedModifier))
            }
        }

        rule.runOnIdle {
            assertThat(unplacedCount).isEqualTo(0)
            active.value = false
        }

        rule.runOnIdle { assertThat(unplacedCount).isEqualTo(1) }
    }
}

private data class OnUnplacedModifierElement(val onUnplacedLambda: () -> Unit) :
    ModifierNodeElement<OnUnplacedModifierNode>() {
    override fun create() = OnUnplacedModifierNode(onUnplacedLambda)

    override fun update(node: OnUnplacedModifierNode) {
        node.onUnplacedLambda = onUnplacedLambda
    }
}

private class OnUnplacedModifierNode(var onUnplacedLambda: () -> Unit) :
    Modifier.Node(), UnplacedStateAwareModifierNode {

    override fun onUnplaced() {
        assertThat(isAttached).isTrue()
        onUnplacedLambda()
    }
}
