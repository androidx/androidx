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

package androidx.compose.material3.internal

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class InteractionTrackerTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var node: TrackerNode

    @Test
    fun tracksFocus() {
        val source = MutableInteractionSource()
        setContent(source, InteractionTracker.Focus)
        val focus = FocusInteraction.Focus()

        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isFalse()
            source.tryEmit(focus)
        }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isTrue()
            assertThat(node.changes).isEqualTo(1)
            source.tryEmit(FocusInteraction.Unfocus(focus))
        }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isFalse()
            assertThat(node.changes).isEqualTo(2)
        }
    }

    @Test
    fun ignoresUntrackedKinds() {
        val source = MutableInteractionSource()
        setContent(source, InteractionTracker.Focus)

        rule.runOnIdle { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        rule.runOnIdle {
            assertThat(node.tracker.isPressed).isFalse()
            assertThat(node.changes).isEqualTo(0)
        }
    }

    @Test
    fun tracksSeveralKinds_untilAllEnd() {
        val source = MutableInteractionSource()
        setContent(source, InteractionTracker.Focus or InteractionTracker.Press)
        val focus = FocusInteraction.Focus()
        val press = PressInteraction.Press(Offset.Zero)

        rule.runOnIdle {
            source.tryEmit(focus)
            source.tryEmit(press)
        }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isTrue()
            assertThat(node.tracker.isPressed).isTrue()
            source.tryEmit(PressInteraction.Release(press))
        }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isTrue()
            assertThat(node.tracker.isPressed).isFalse()
            source.tryEmit(FocusInteraction.Unfocus(focus))
        }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isFalse()
            assertThat(node.changes).isEqualTo(4)
        }
    }

    @Test
    fun ignoresEndOfInteractionThatStartedBeforeTracking() {
        val source = MutableInteractionSource()
        setContent(source, InteractionTracker.Press)

        // A press that started before the node was tracking: its release must not leave the
        // tracker unable to see the next press.
        rule.runOnIdle {
            source.tryEmit(PressInteraction.Release(PressInteraction.Press(Offset.Zero)))
        }
        rule.runOnIdle {
            assertThat(node.changes).isEqualTo(0)
            source.tryEmit(PressInteraction.Press(Offset.Zero))
        }
        rule.runOnIdle {
            assertThat(node.tracker.isPressed).isTrue()
            assertThat(node.changes).isEqualTo(1)
        }
    }

    @Test
    fun interactionSourceChange_startsOver() {
        val source1 = MutableInteractionSource()
        val source2 = MutableInteractionSource()
        var source by mutableStateOf<InteractionSource>(source1)
        rule.setContent {
            Box(Modifier.size(10.dp).then(TrackerElement(source, InteractionTracker.Focus)))
        }

        rule.runOnIdle { source1.tryEmit(FocusInteraction.Focus()) }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isTrue()
            source = source2
        }
        rule.runOnIdle {
            // Interactions don't replay, so the new source starts unfocused.
            assertThat(node.tracker.isFocused).isFalse()
            assertThat(node.changes).isEqualTo(2)
            source1.tryEmit(FocusInteraction.Focus())
        }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isFalse()
            source2.tryEmit(FocusInteraction.Focus())
        }
        rule.runOnIdle { assertThat(node.tracker.isFocused).isTrue() }
    }

    @Test
    fun reattach_startsOver() {
        val source = MutableInteractionSource()
        node = TrackerNode(source, InteractionTracker.Focus)
        var attached by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.size(10.dp).then(if (attached) SameNodeElement(node) else Modifier))
        }

        rule.runOnIdle { source.tryEmit(FocusInteraction.Focus()) }
        rule.runOnIdle {
            assertThat(node.tracker.isFocused).isTrue()
            attached = false
        }
        rule.runOnIdle { attached = true }
        rule.runOnIdle {
            // Interactions don't replay, so it starts over unfocused, and tracks the source again.
            assertThat(node.tracker.isFocused).isFalse()
            source.tryEmit(FocusInteraction.Focus())
        }
        rule.runOnIdle { assertThat(node.tracker.isFocused).isTrue() }
    }

    private fun setContent(source: InteractionSource, kinds: Int) {
        rule.setContent { Box(Modifier.size(10.dp).then(TrackerElement(source, kinds))) }
    }

    // Adds the given node, so that it's reattached rather than recreated.
    private class SameNodeElement(private val node: TrackerNode) :
        ModifierNodeElement<TrackerNode>() {
        override fun create() = node

        override fun update(node: TrackerNode) {}

        override fun equals(other: Any?) = other is SameNodeElement && other.node === node

        override fun hashCode() = node.hashCode()

        override fun InspectorInfo.inspectableProperties() {
            name = "sameNodeElement"
        }
    }

    private inner class TrackerElement(
        private val interactionSource: InteractionSource,
        private val kinds: Int,
    ) : ModifierNodeElement<TrackerNode>() {
        override fun create() = TrackerNode(interactionSource, kinds).also { node = it }

        override fun update(node: TrackerNode) {
            node.interactionSource = interactionSource
            node.tracker.track(interactionSource)
        }

        override fun equals(other: Any?) =
            other is TrackerElement &&
                other.interactionSource === interactionSource &&
                other.kinds == kinds

        override fun hashCode() = 31 * interactionSource.hashCode() + kinds

        override fun InspectorInfo.inspectableProperties() {
            name = "trackerElement"
            properties["interactionSource"] = interactionSource
            properties["kinds"] = kinds
        }
    }

    private class TrackerNode(var interactionSource: InteractionSource, kinds: Int) :
        Modifier.Node() {
        var changes = 0
        val tracker =
            object : InteractionTracker(this@TrackerNode, kinds) {
                override fun onChanged() {
                    changes++
                }
            }

        override fun onAttach() {
            tracker.track(interactionSource)
        }
    }
}
