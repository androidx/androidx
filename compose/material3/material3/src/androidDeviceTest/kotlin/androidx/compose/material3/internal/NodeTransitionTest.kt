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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
class NodeTransitionTest {

    @get:Rule val rule = createComposeRule()

    private val node = TransitionNode()

    @Test
    fun value_isTheTarget_untilTheTargetChanges() {
        setContent()

        rule.runOnIdle {
            assertThat(node.value.value).isEqualTo(0f)
            node.target = 1f
            // Values only move on update.
            assertThat(node.value.value).isEqualTo(0f)
        }
    }

    @Test
    fun update_animatesToTheNewTarget() {
        setContent()
        rule.mainClock.autoAdvance = false

        rule.runOnIdle {
            assertThat(node.value.value).isEqualTo(0f)
            node.target = 1f
            assertThat(node.transition.update()).isTrue()
        }
        rule.mainClock.advanceTimeBy(DurationMillis / 2L)
        rule.runOnIdle { assertThat(node.value.value).isWithin(0.2f).of(0.5f) }
        rule.mainClock.advanceTimeBy(DurationMillis.toLong())
        rule.runOnIdle { assertThat(node.value.value).isEqualTo(1f) }
    }

    @Test
    fun update_onlyReportsNewAnimatables() {
        setContent()

        rule.runOnIdle {
            node.value.value
            node.target = 1f
            assertThat(node.transition.update()).isTrue()
            node.target = 2f
            assertThat(node.transition.update()).isFalse()
            // Unchanged target.
            assertThat(node.transition.update()).isFalse()
        }
    }

    @Test
    fun update_beforeTheValueIsRead_doesNotAnimate() {
        setContent()

        rule.runOnIdle {
            node.target = 1f
            assertThat(node.transition.update()).isFalse()
            assertThat(node.value.value).isEqualTo(1f)
        }
    }

    @Test
    fun update_withAnImmediateSnap_appliesTheTargetRightAway() {
        setContent()
        rule.mainClock.autoAdvance = false

        rule.runOnIdle {
            node.value.value
            node.animate = false
            node.target = 1f
            val invalidate = node.transition.update()
            assertThat(node.value.value).isEqualTo(1f)
            // Callers must invalidate, as no Animatable does it.
            assertThat(invalidate).isTrue()
        }
    }

    @Test
    fun update_withAnImmediateSnap_interruptsTheAnimation() {
        setContent()
        rule.mainClock.autoAdvance = false

        rule.runOnIdle {
            node.value.value
            node.target = 1f
            node.transition.update()
        }
        rule.mainClock.advanceTimeBy(DurationMillis / 2L)
        rule.runOnIdle {
            node.animate = false
            node.target = 2f
            val invalidate = node.transition.update()
            assertThat(node.value.value).isEqualTo(2f)
            assertThat(invalidate).isTrue()
            // Later animations start from the snapped value.
            node.animate = true
            node.target = 3f
            node.transition.update()
        }
        rule.mainClock.advanceTimeBy(DurationMillis / 2L)
        rule.runOnIdle { assertThat(node.value.value).isWithin(0.2f).of(2.5f) }
    }

    @Test
    fun detach_dropsAnimations() {
        var attached by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.size(10.dp).then(if (attached) TransitionElement(node) else Modifier))
        }
        rule.mainClock.autoAdvance = false

        rule.runOnIdle {
            node.value.value
            node.target = 1f
            node.transition.update()
        }
        rule.mainClock.advanceTimeBy(DurationMillis / 2L)
        rule.runOnIdle {
            attached = false
            node.target = 2f
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { attached = true }
        rule.mainClock.advanceTimeByFrame()
        // The interrupted animation was dropped: the value is the current target.
        rule.runOnIdle { assertThat(node.value.value).isEqualTo(2f) }
    }

    private fun setContent() {
        rule.setContent { Box(Modifier.size(10.dp).then(TransitionElement(node))) }
    }

    private class TransitionElement(private val node: TransitionNode) :
        ModifierNodeElement<TransitionNode>() {
        override fun create() = node

        override fun update(node: TransitionNode) {}

        override fun equals(other: Any?) = other is TransitionElement && other.node === node

        override fun hashCode() = node.hashCode()

        override fun InspectorInfo.inspectableProperties() {
            name = "transitionElement"
        }
    }

    private class TransitionNode : Modifier.Node() {
        var target = 0f
        var animate = true
        val transition = NodeTransition(this)
        val value =
            transition.animateValue(
                createAnimatable = { Animatable(it) },
                animationSpec = {
                    if (animate) tween(DurationMillis, easing = LinearEasing) else snap()
                },
                targetValue = { target },
            )

        override fun onDetach() {
            transition.reset()
        }
    }

    private companion object {
        const val DurationMillis = 100
    }
}
