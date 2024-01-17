/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.viewpager2.integration.testapp.test.util

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.viewpager2.widget.ViewPager2

/**
 * Watcher that uses a [CountingIdlingResource] to keep track of the idling state of a ViewPager2. A
 * ViewPager2 is idle when it is in [ViewPager2.SCROLL_STATE_IDLE]. To work around a bug in Espresso
 * where touch input is blocked when IdlingResources are not idle, this watcher will only flip its
 * idling state to not idle when [waitForIdle] is called while ViewPager2 is not idle. Once the
 * ViewPager2 reaches the idle state, the watcher will remain idle until waitForIdle is called again
 * during ViewPager2's non-idle state.
 *
 * @constructor Creates a ViewPagerIdleWatcher that keeps track of the given ViewPager2's idle
 * state. It will register its IdlingResource and starts listening to the viewPager's
 * OnPageChangeCallbacks on creation. Call [unregister] to remove its IdlingResource and stop
 * listening to viewPager.
 */
class ViewPagerIdleWatcher(private val viewPager: ViewPager2) : ViewPager2.OnPageChangeCallback() {
    private var state = ViewPager2.SCROLL_STATE_IDLE
    private var waitingForIdle = false
    private val lock = Object()
    private val counter = CountingIdlingResource("Idle when $viewPager is not scrolling")

    init {
        IdlingRegistry.getInstance().register(counter)
        viewPager.registerOnPageChangeCallback(this)
    }

    override fun onPageScrollStateChanged(state: Int) {
        synchronized(lock) {
            this.state = state
            if (waitingForIdle && state == ViewPager2.SCROLL_STATE_IDLE) {
                counter.decrement()
                waitingForIdle = false
            }
        }
    }

    /**
     * Flips the IdlingResource to non-idling if the watched ViewPager2 is in a non-idle state. Does
     * nothing otherwise. Call this when the watched ViewPager2 is in a non-idle state and you need
     * to wait until it reached its idle state.
     */
    fun waitForIdle() {
        synchronized(lock) {
            if (!waitingForIdle && state != ViewPager2.SCROLL_STATE_IDLE) {
                waitingForIdle = true
                counter.increment()
            }
        }
    }

    /**
     * Unregisters this watcher's IdlingResource from the IdlingRegistry and stops listening to the
     * watched ViewPager2.
     */
    fun unregister() {
        viewPager.unregisterOnPageChangeCallback(this)
        IdlingRegistry.getInstance().unregister(counter)
    }
}
