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

package com.example.androidx.viewpager2.test

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.viewpager2.widget.ViewPager2

class ViewPagerIdleWatcher(private val counter: CountingIdlingResource) :
    ViewPager2.OnPageChangeCallback() {
    private var state = ViewPager2.SCROLL_STATE_IDLE
    private var waitingForIdle = false
    private val lock = Object()

    override fun onPageScrollStateChanged(state: Int) {
        synchronized(lock) {
            this.state = state
            if (waitingForIdle && state == ViewPager2.SCROLL_STATE_IDLE) {
                counter.decrement()
                waitingForIdle = false
            }
        }
    }

    fun waitForIdle() {
        synchronized(lock) {
            if (!waitingForIdle && state != ViewPager2.SCROLL_STATE_IDLE) {
                waitingForIdle = true
                counter.increment()
            }
        }
    }

    fun unregister() {
        IdlingRegistry.getInstance().unregister(counter)
    }

    companion object {
        fun registerViewPagerIdlingResource(viewPager: ViewPager2): ViewPagerIdleWatcher {
            val counter = CountingIdlingResource("Idle when $this is not scrolling")
            IdlingRegistry.getInstance().register(counter)
            val idleWatcher = ViewPagerIdleWatcher(counter)
            viewPager.registerOnPageChangeCallback(idleWatcher)
            return idleWatcher
        }
    }
}