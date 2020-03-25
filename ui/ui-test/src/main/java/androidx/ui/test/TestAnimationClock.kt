/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test

import androidx.animation.AnimationClockObservable
import androidx.animation.rootAnimationClockFactory
import androidx.test.espresso.IdlingResource
import androidx.ui.test.android.AndroidTestAnimationClock
import androidx.ui.test.android.registerTestClock
import androidx.ui.test.android.unregisterTestClock
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Interface for animation clocks that can report their idleness and can switch between ticking
 * automatically (e.g., if it's driven by the main loop of the host) and ticking manually.
 *
 * An idle clock is one that is currently not driving any animations. Typically, that means a
 * clock where no observers are registered. The idleness can be retrieved by [isIdle].
 *
 * Use [pauseClock] to switch from automatic ticking to manual ticking, [resumeClock] to switch
 * from manual to automatic with; and manually tick the clock with [advanceClock].
 */
interface TestAnimationClock : AnimationClockObservable {
    /**
     * Whether the clock is idle or not. An idle clock is one that is not driving animations,
     * which happens (1) when no observers are observing this clock, or (2) when the clock is
     * paused.
     */
    val isIdle: Boolean

    /**
     * Pauses the automatic ticking of the clock. The clock shall only tick in response to
     * [advanceClock], and shall continue ticking automatically when [resumeClock] is called.
     * It's safe to call this method when the clock is already paused.
     */
    fun pauseClock()

    /**
     * Resumes the automatic ticking of the clock. It's safe to call this method when the clock
     * is already resumed.
     */
    fun resumeClock()

    /**
     * Whether the clock is [paused][pauseClock] or [not][resumeClock].
     */
    val isPaused: Boolean

    /**
     * Advances the clock by the given number of [milliseconds]. It is safe to call this method
     * both when the clock is paused and resumed.
     */
    fun advanceClock(milliseconds: Long)
}

/**
 * A [TestRule] to monitor and take over the animation clock in the composition. It substitutes
 * the ambient animation clock provided at the root of the composition tree with a
 * [TestAnimationClock] and registers it with [registerTestClock].
 *
 * Usually you don't need to create this rule by yourself, it is done for you in
 * [ComposeTestRule]. If you don't use [ComposeTestRule], use this rule in your test and make
 * sure it is run _before_ your activity is created.
 *
 * If your app provides a custom animation clock somewhere in your composition, make sure to have
 * it implement [TestAnimationClock] and register it with [registerTestClock]. Alternatively,
 * if you use Espresso you can create your own [IdlingResource] to let Espresso await your
 * animations. Otherwise, built in steps that make sure the UI is stable when performing actions
 * or assertions will fail to work.
 */
class AnimationClockTestRule : TestRule {

    /** Backing property for [clock] */
    private val _clock = AndroidTestAnimationClock()

    /**
     * The ambient animation clock that is provided at the root of the composition tree.
     * Typically, apps will only use this clock. If your app provides another clock in the tree,
     * make sure to let it implement [TestAnimationClock] and register it with
     * [registerTestClock].
     */
    val clock: TestAnimationClock get() = _clock

    /**
     * Convenience property for calling [`clock.isPaused`][TestAnimationClock.isPaused]
     */
    val isPaused: Boolean get() = clock.isPaused

    /**
     * Convenience method for calling [`clock.pauseClock()`][TestAnimationClock.pauseClock]
     */
    fun pauseClock() = clock.pauseClock()

    /**
     * Convenience method for calling [`clock.resumeClock()`][TestAnimationClock.resumeClock]
     */
    fun resumeClock() = clock.resumeClock()

    /**
     * Convenience method for calling [`clock.advanceClock()`][TestAnimationClock.advanceClock]
     */
    fun advanceClock(milliseconds: Long) = clock.advanceClock(milliseconds)

    override fun apply(base: Statement, description: Description?): Statement {
        return AnimationClockStatement(base)
    }

    private inner class AnimationClockStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            val oldFactory = rootAnimationClockFactory
            registerTestClock(clock)
            rootAnimationClockFactory = { clock }
            try {
                base.evaluate()
            } finally {
                try {
                    _clock.dispose()
                } finally {
                    rootAnimationClockFactory = oldFactory
                    unregisterTestClock(clock)
                }
            }
        }
    }
}
