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

package androidx.compose.ui.modifier

import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.materializerOfWithCompositionLocalInjection
import androidx.compose.ui.materializeWithCompositionLocalInjectionInternal
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CompositionLocalMapInjectionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun consumeInDraw() {
        // No assertions needed. This not crashing is the test
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides 1) {
                OldBox(modifierOf { ConsumeInDrawNode() }.size(10.dp))
            }
        }
    }

    @Test
    fun consumeInLayout() {
        // No assertions needed. This not crashing is the test
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides 1) {
                OldBox(modifierOf { ConsumeInLayoutNode() }.size(10.dp))
            }
        }
    }

    @Test
    fun consumeInAttach() {
        // No assertions needed. This not crashing is the test
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides 1) {
                OldBox(modifierOf { ConsumeInAttachNode() }.size(10.dp))
            }
        }
    }

    @Test
    fun consumeInDrawGetsNotifiedOfChanges() {
        val node = ConsumeInDrawNode()
        var state by mutableStateOf(1)
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides state) {
                OldBox(modifierOf { node }.size(10.dp))
            }
        }
        assertThat(node.int).isEqualTo(state)
        state = 2
        rule.runOnIdle {
            assertThat(node.int).isEqualTo(state)
        }
    }

    @Test
    fun consumeInLayoutGetsNotifiedOfChanges() {
        val node = ConsumeInLayoutNode()
        var state by mutableStateOf(1)
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides state) {
                OldBox(modifierOf { node }.size(10.dp))
            }
        }
        assertThat(node.int).isEqualTo(state)
        state = 2
        rule.runOnIdle {
            assertThat(node.int).isEqualTo(state)
        }
    }

    @Test
    fun consumeInAttachGetsNotifiedOfChanges() {
        val node = ConsumeInAttachNode()
        var state by mutableStateOf(1)
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides state) {
                OldBox(modifierOf { node }.size(10.dp))
            }
        }
        assertThat(node.int).isEqualTo(state)
        state = 2
        rule.runOnIdle {
            assertThat(node.int).isEqualTo(state)
        }
    }

    @Test
    fun consumeInDrawSkippableUpdate() {
        // No assertions needed. This not crashing is the test
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides 1) {
                OldBoxSkippableUpdate(modifierOf { ConsumeInDrawNode() }.size(10.dp))
            }
        }
    }

    @Test
    fun consumeInLayoutSkippableUpdate() {
        // No assertions needed. This not crashing is the test
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides 1) {
                OldBoxSkippableUpdate(modifierOf { ConsumeInLayoutNode() }.size(10.dp))
            }
        }
    }

    @Test
    fun consumeInAttachSkippableUpdate() {
        // No assertions needed. This not crashing is the test
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides 1) {
                OldBoxSkippableUpdate(modifierOf { ConsumeInAttachNode() }.size(10.dp))
            }
        }
    }

    @Test
    fun consumeInDrawGetsNotifiedOfChangesSkippableUpdate() {
        val node = ConsumeInDrawNode()
        var state by mutableStateOf(1)
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides state) {
                OldBoxSkippableUpdate(modifierOf { node }.size(10.dp))
            }
        }
        assertThat(node.int).isEqualTo(state)
        state = 2
        rule.runOnIdle {
            assertThat(node.int).isEqualTo(state)
        }
    }

    @Test
    fun consumeInLayoutGetsNotifiedOfChangesSkippableUpdate() {
        val node = ConsumeInLayoutNode()
        var state by mutableStateOf(1)
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides state) {
                OldBoxSkippableUpdate(modifierOf { node }.size(10.dp))
            }
        }
        assertThat(node.int).isEqualTo(state)
        state = 2
        rule.runOnIdle {
            assertThat(node.int).isEqualTo(state)
        }
    }

    @Test
    fun consumeInAttachGetsNotifiedOfChangesSkippableUpdate() {
        val node = ConsumeInAttachNode()
        var state by mutableStateOf(1)
        rule.setContent {
            CompositionLocalProvider(SomeLocal provides state) {
                OldBoxSkippableUpdate(modifierOf { node }.size(10.dp))
            }
        }
        assertThat(node.int).isEqualTo(state)
        state = 2
        rule.runOnIdle {
            assertThat(node.int).isEqualTo(state)
        }
    }
}

val SomeLocal = compositionLocalOf<Int> { error("unprovided value") }

inline fun <reified T : Modifier.Node> modifierOf(crossinline fn: () -> T) =
    object : ModifierNodeElement<T>() {
        override fun create() = fn()
        override fun hashCode() = System.identityHashCode(this)
        override fun equals(other: Any?) = other === this
        override fun update(node: T) {}
    }

class ConsumeInDrawNode : CompositionLocalConsumerModifierNode, DrawModifierNode, Modifier.Node() {
    var view: View? = null
    var int: Int? = null
    override fun ContentDrawScope.draw() {
        // Consume Static local
        view = currentValueOf(LocalView)
        // Consume Freshly Provided Local
        int = currentValueOf(SomeLocal)
    }
}

class ConsumeInLayoutNode :
    CompositionLocalConsumerModifierNode,
    LayoutModifierNode,
    Modifier.Node() {
    var view: View? = null
    var int: Int? = null
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Consume Static local
        view = currentValueOf(LocalView)
        // Consume Freshly Provided Local
        int = currentValueOf(SomeLocal)
        val placeable = measurable.measure(constraints)
        return layout(constraints.minWidth, constraints.maxWidth) {
            placeable.place(0, 0)
        }
    }
}

class ConsumeInAttachNode :
    CompositionLocalConsumerModifierNode, ObserverModifierNode, Modifier.Node() {
    var view: View? = null
    var int: Int? = null
    private fun readLocals() {
        // Consume Static local
        view = currentValueOf(LocalView)
        // Consume Freshly Provided Local
        int = currentValueOf(SomeLocal)
    }
    override fun onAttach() {
        observeReads { readLocals() }
    }
    override fun onObservedReadsChanged() {
        observeReads { readLocals() }
    }
}

// This composable is intentionally written to look like the "old" version of Layout, before
// aosp/2318839. This function allows us to emulate what a module targeting an older version of
// compose UI would have inlined into their function body. See b/275067189 for more details.
@UiComposable
@Composable
inline fun OldLayoutSkippableUpdate(
    content: @Composable @UiComposable () -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    @Suppress("DEPRECATION")
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
        },
        // The old version of Layout called a function called "materializerOf". The function below
        // has the same JVM signature as that function used to have, so the code that this source
        // generates will be essentially identical to what will have been generated in older versions
        // of UI, despite this name being different now.
        skippableUpdate = materializerOfWithCompositionLocalInjection(modifier),
        content = content
    )
}

@Suppress("NOTHING_TO_INLINE")
@Composable
@UiComposable
internal inline fun OldLayout(
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    // The old version of Layout called a function called "materialize". The function below
    // has the same JVM signature as that function used to have, so the code that this source
    // generates will be essentially identical to what will have been generated in older versions
    // of UI, despite this name being different now.
    val materialized = currentComposer.materializeWithCompositionLocalInjectionInternal(modifier)
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
            set(materialized, ComposeUiNode.SetModifier)
        },
    )
}
private val EmptyBoxMeasurePolicy = MeasurePolicy { _, constraints ->
    layout(constraints.minWidth, constraints.minHeight) {}
}

@Composable fun OldBoxSkippableUpdate(modifier: Modifier = Modifier) {
    OldLayoutSkippableUpdate({ }, modifier, EmptyBoxMeasurePolicy)
}

@Composable fun OldBox(modifier: Modifier = Modifier) {
    OldLayout(modifier, EmptyBoxMeasurePolicy)
}