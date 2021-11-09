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

import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageScrollStateChangedEvent
import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageScrolledEvent
import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageSelectedEvent
import androidx.viewpager2.widget.ViewPager2

sealed class OnPageChangeCallbackEvent {
    data class OnPageScrolledEvent(
        val position: Int,
        val positionOffset: Float,
        val positionOffsetPixels: Int
    ) : OnPageChangeCallbackEvent()

    data class OnPageSelectedEvent(val position: Int) : OnPageChangeCallbackEvent()
    data class OnPageScrollStateChangedEvent(val state: Int) : OnPageChangeCallbackEvent()
}

class EventRecorder : ViewPager2.OnPageChangeCallback() {
    private val events = mutableListOf<OnPageChangeCallbackEvent>()
    val allEvents: List<OnPageChangeCallbackEvent> get() = events

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
        synchronized(events) {
            events.add(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
        }
    }

    override fun onPageSelected(position: Int) {
        synchronized(events) {
            events.add(OnPageSelectedEvent(position))
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        synchronized(events) {
            events.add(OnPageScrollStateChangedEvent(state))
        }
    }
}
