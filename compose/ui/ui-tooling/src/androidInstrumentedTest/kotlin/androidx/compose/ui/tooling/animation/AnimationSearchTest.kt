/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.tooling.animation

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.AnimateAsStatePreview
import androidx.compose.ui.tooling.AnimateAsStateWithLabelsPreview
import androidx.compose.ui.tooling.AnimateContentSizePreview
import androidx.compose.ui.tooling.AnimatedContentExtensionPreview
import androidx.compose.ui.tooling.AnimatedContentPreview
import androidx.compose.ui.tooling.AnimatedVisibilityPreview
import androidx.compose.ui.tooling.CrossFadePreview
import androidx.compose.ui.tooling.CrossFadeWithLabelPreview
import androidx.compose.ui.tooling.DecayAnimationPreview
import androidx.compose.ui.tooling.InfiniteTransitionPreview
import androidx.compose.ui.tooling.NullAnimateAsStatePreview
import androidx.compose.ui.tooling.NullAnimatedContentPreview
import androidx.compose.ui.tooling.NullTransitionPreview
import androidx.compose.ui.tooling.TargetBasedAnimationPreview
import androidx.compose.ui.tooling.TransitionAnimatedVisibilityPreview
import androidx.compose.ui.tooling.TransitionPreview
import androidx.compose.ui.tooling.animation.InfiniteTransitionComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.Utils.addAnimations
import androidx.compose.ui.tooling.animation.Utils.attachAllAnimations
import androidx.compose.ui.tooling.animation.Utils.hasAnimations
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimationSearchTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun targetBasedIsAddedAndTracked() {
        var callbacks = 0
        val search = AnimationSearch.TargetBasedSearch { callbacks++ }
        rule.addAnimations(search) { TargetBasedAnimationPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(200f, search.animations.first().initialValue)
    }

    @Test
    fun targetBasedIsFound() {
        animationIsFound(AnimationSearch.TargetBasedSearch {}) {
            TargetBasedAnimationPreview()
        }
    }

    @Test
    fun targetBasedAnimationIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { TargetBasedAnimationPreview() }
        assertEquals(1, clock.trackedUnsupportedAnimations.size)
    }

    @Test
    fun decayAnimationIsAddedAndTracked() {
        var callbacks = 0
        val search = AnimationSearch.DecaySearch { callbacks++ }
        rule.addAnimations(search) { DecayAnimationPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(200f, search.animations.first().initialValue)
    }

    @Test
    fun decayBasedIsFound() {
        animationIsFound(AnimationSearch.DecaySearch {}) {
            DecayAnimationPreview()
        }
    }

    @Test
    fun decayAnimationIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { DecayAnimationPreview() }
        assertEquals(1, clock.trackedUnsupportedAnimations.size)
    }

    @Test
    fun infiniteTransitionIsAddedAndTracked() {
        var callbacks = 0
        val search = AnimationSearch.InfiniteTransitionSearch { callbacks++ }
        rule.addAnimations(search) { InfiniteTransitionPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        val composeAnimation = search.animations.first().parse()!!
        Assert.assertNotNull(composeAnimation)
        Assert.assertNotNull(composeAnimation.animationObject)
        Assert.assertNotNull(composeAnimation.label)
        assertEquals(1, composeAnimation.states.size)
        assertEquals(ComposeAnimationType.INFINITE_TRANSITION, composeAnimation.type)
    }

    @Test
    fun infiniteTransitionIsFound() {
        animationIsFound(AnimationSearch.InfiniteTransitionSearch {}) {
            InfiniteTransitionPreview()
        }
    }

    @Test
    fun infiniteTransitionIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { InfiniteTransitionPreview() }
        assertEquals(1, clock.infiniteTransitionClocks.size)
    }

    @Test
    fun multipleInfiniteTransitionIsAdded() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.addAnimations(search) {
            rememberInfiniteTransition()
            rememberInfiniteTransition()
            rememberInfiniteTransition()
            rememberInfiniteTransition()
            rememberInfiniteTransition()
        }
        assertEquals(5, search.animations.size)
        assertTrue(search.animations.isNotEmpty())
    }

    @Test
    fun animatedXAsStateSearchIsAddedAndTracked() {
        var callbacks = 0
        val search = AnimationSearch.AnimateXAsStateSearch { callbacks++ }
        rule.addAnimations(search) { AnimateAsStatePreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(2, search.animations.size)
        search.animations.first().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("IntAnimation", it.animatable.label)
        }
        search.animations.last().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("DpAnimation", it.animatable.label)
        }
        search.track()
        assertEquals(2, callbacks)
        assertEquals(0.dp, search.animations.last().animatable.targetValue)
        assertEquals(2, search.animations.first().animatable.targetValue)
    }

    @Test
    fun animatedXAsStateIsFound() {
        animationIsFound(AnimationSearch.AnimateXAsStateSearch {}) {
            AnimateAsStatePreview()
        }
    }

    @Test
    fun animatedXAsStateSearchIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { AnimateAsStatePreview() }
        assertEquals(2, clock.animateXAsStateClocks.size)
    }

    @Test
    fun animatedXAsStateWithLabelsSearchIsAddedAndTracked() {
        var callbacks = 0
        val search = AnimationSearch.AnimateXAsStateSearch { callbacks++ }
        rule.addAnimations(search) { AnimateAsStateWithLabelsPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(2, search.animations.size)
        search.animations.first().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("CustomIntLabel", it.animatable.label)
        }
        search.animations.last().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("CustomDpLabel", it.animatable.label)
        }
        search.track()
        assertEquals(2, callbacks)
        assertEquals(0.dp, search.animations.last().animatable.targetValue)
        assertEquals(2, search.animations.first().animatable.targetValue)
    }

    @Test
    fun animatedXAsStateWithLabelsSearchIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { AnimateAsStateWithLabelsPreview() }
        assertEquals(2, clock.animateXAsStateClocks.size)
    }

    @Test
    fun animatedContentSizeIsAddedAndTracke() {
        var callbacks = 0
        val search = AnimationSearch.AnimateContentSizeSearch { callbacks++ }
        rule.addAnimations(search) { AnimateContentSizePreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
    }

    @Test
    fun animatedContentSizeIsFound() {
        animationIsFound(AnimationSearch.AnimateContentSizeSearch {}) {
            AnimateContentSizePreview()
        }
    }

    @Test
    fun animatedContentSizeIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { AnimateContentSizePreview() }
        assertEquals(1, clock.trackedUnsupportedAnimations.size)
    }

    @Test
    fun transitionIsAddedAndTracked() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.addAnimations(search) { TransitionPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("checkBoxAnim", search.animations.first().label)
    }

    @Test
    fun transitionIsFound() {
        animationIsFound(AnimationSearch.TransitionSearch {}) {
            TransitionPreview()
        }
    }

    @Test
    fun transitionIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { TransitionPreview() }
        assertEquals(1, clock.transitionClocks.size)
    }

    @Test
    fun animatedVisibilityExtensionIsAddedAsTransition() {
        var transitionSearchCallbacks = 0
        var animatedVisibilitySearchCallbacks = 0
        val transitionSearch = AnimationSearch.TransitionSearch { transitionSearchCallbacks++ }
        val animatedVisibilitySearch =
            AnimationSearch.AnimatedVisibilitySearch { animatedVisibilitySearchCallbacks++ }
        rule.addAnimations(transitionSearch, animatedVisibilitySearch) {
            TransitionAnimatedVisibilityPreview()
        }
        assertTrue(transitionSearch.animations.isNotEmpty())
        assertFalse(animatedVisibilitySearch.animations.isNotEmpty())
        assertEquals(1, transitionSearch.animations.size)
        assertEquals(0, animatedVisibilitySearch.animations.size)
        // Track animations.
        transitionSearch.track()
        animatedVisibilitySearch.track()
        assertEquals(1, transitionSearchCallbacks)
        assertEquals(0, animatedVisibilitySearchCallbacks)
    }

    @Test
    fun animatedVisibilityExtensionIsFound() {
        animationIsFound(AnimationSearch.TransitionSearch {}) {
            TransitionAnimatedVisibilityPreview()
        }
    }

    @Test
    fun animatedVisibilityExtensionIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { TransitionAnimatedVisibilityPreview() }
        assertEquals(1, clock.transitionClocks.size)
    }

    @Test
    fun crossFadeIsAddedAsTransition() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.addAnimations(search) { CrossFadePreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("A", search.animations.first().targetState)
        assertEquals("Crossfade", search.animations.first().label)
    }

    @Test
    fun crossFadeIsFound() {
        animationIsFound(AnimationSearch.TransitionSearch {}) {
            CrossFadePreview()
        }
    }

    @Test
    fun crossFadeIsConnectedAsTransition() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { CrossFadePreview() }
        assertEquals(1, clock.transitionClocks.size)
    }

    @Test
    fun crossFadeWithLabelIsAddedAsTransition() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.addAnimations(search) { CrossFadeWithLabelPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("A", search.animations.first().targetState)
        assertEquals("CrossfadeWithLabel", search.animations.first().label)
    }

    @Test
    fun crossFadeWithLabelIsConnectedAsTransition() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { CrossFadeWithLabelPreview() }
        assertEquals(1, clock.transitionClocks.size)
    }

    @Test
    fun animatedVisibilityIsAdded() {
        var callbacks = 0
        val search = AnimationSearch.AnimatedVisibilitySearch { callbacks++ }
        rule.addAnimations(search) { AnimatedVisibilityPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("My Animated Visibility", search.animations.first().label)
    }

    @Test
    fun animatedVisibilityIsFoundd() {
        animationIsFound(AnimationSearch.AnimatedVisibilitySearch {}) {
            AnimatedVisibilityPreview()
        }
    }

    @Test
    fun animatedVisibilityIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { AnimatedVisibilityPreview() }
        assertEquals(1, clock.animatedVisibilityClocks.size)
    }

    @Test
    fun animatedContentIsAdded() {
        var callbacks = 0
        val search = AnimationSearch.AnimatedContentSearch { callbacks++ }
        rule.addAnimations(search) { AnimatedContentPreview() }
        assertTrue(search.animations.isNotEmpty())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(0, search.animations.first().targetState)
    }

    @Test
    fun animatedContentIsFound() {
        animationIsFound(AnimationSearch.AnimatedContentSearch {}) {
            AnimatedContentPreview()
        }
    }

    @Test
    fun animatedContentIsConnected() {
        val clock = PreviewAnimationClock {}
        rule.attachAllAnimations(clock) { AnimatedContentPreview() }
        assertEquals(1, clock.animatedContentClocks.size)
    }

    @Test
    fun nullAnimatedContentIsConnectedAsUnsupported() {
        val clock = PreviewAnimationClock()
        rule.attachAllAnimations(clock) { NullAnimatedContentPreview() }
        assertEquals(1, clock.trackedUnsupportedAnimations.size)
        assertTrue(clock.animatedContentClocks.isEmpty())
    }

    @Test
    fun nullAnimatedXAsStateIsConnectedAsUnsupported() {
        val clock = PreviewAnimationClock()
        rule.attachAllAnimations(clock) { NullAnimateAsStatePreview() }
        assertEquals(1, clock.trackedUnsupportedAnimations.size)
        assertTrue(clock.animateXAsStateClocks.isEmpty())
    }

    @Test
    fun nullTransitionIsConnectedAsUnsupported() {
        val clock = PreviewAnimationClock()
        rule.attachAllAnimations(clock) { NullTransitionPreview() }
        assertEquals(1, clock.trackedUnsupportedAnimations.size)
        assertTrue(clock.transitionClocks.isEmpty())
    }

    @Test
    fun animatedContentExtensionIsAddedAsTransition() {
        var transitionCallbacks = 0
        var animatedContentCallbacks = 0
        val transitionSearch = AnimationSearch.TransitionSearch { transitionCallbacks++ }
        val animatedContentSearch =
            AnimationSearch.AnimatedContentSearch { animatedContentCallbacks++ }
        rule.addAnimations(transitionSearch, animatedContentSearch) {
            AnimatedContentExtensionPreview()
        }
        assertTrue(transitionSearch.animations.isNotEmpty())
        assertFalse(animatedContentSearch.animations.isNotEmpty())
        assertEquals(1, transitionSearch.animations.size)
        assertEquals(0, animatedContentSearch.animations.size)
        transitionSearch.track()
        animatedContentSearch.track()
        assertEquals(1, transitionCallbacks)
        assertEquals(0, animatedContentCallbacks)
    }

    @Test
    fun animatedContentExtensionIsFoundAsTransitionAndSupported() {
        val clock = PreviewAnimationClock()
        rule.attachAllAnimations(clock) { AnimatedContentExtensionPreview() }
        assertEquals(1, clock.transitionClocks.size)
    }

    private fun animationIsFound(
        search: AnimationSearch.Search<*>,
        content: @Composable () -> Unit
    ) {
        val hasAnimations = rule.hasAnimations(search) { content() }
        assertTrue(hasAnimations)
        assertFalse(search.animations.isNotEmpty())
    }
}
