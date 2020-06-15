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

package androidx.ui.tooling.animation

import androidx.animation.AnimationClockObserver
import androidx.ui.tooling.preview.animation.PreviewAnimationClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewAnimationClockTest {
    @Test
    fun setClockTimeIsRelative() {
        val previewAnimationClock = PreviewAnimationClock(100)
        previewAnimationClock.setClockTime(300)
        assertEquals(400, previewAnimationClock.clock.clockTimeMillis)
    }

    @Test
    fun subscribeAndUnsubscribeShouldNotify() {
        val observer = object : AnimationClockObserver {
            override fun onAnimationFrame(frameTimeMillis: Long) {
                // Do nothing
            }
        }

        val previewAnimationClock = TestPreviewAnimationClock()
        assertFalse(previewAnimationClock.subscribed)
        previewAnimationClock.subscribe(observer)
        assertTrue(previewAnimationClock.subscribed)

        assertFalse(previewAnimationClock.unsubscribed)
        previewAnimationClock.unsubscribe(observer)
        assertTrue(previewAnimationClock.unsubscribed)
    }

    private inner class TestPreviewAnimationClock : PreviewAnimationClock(0) {
        var subscribed = false
        var unsubscribed = false

        override fun notifySubscribe(observer: AnimationClockObserver) {
            subscribed = true
        }

        override fun notifyUnsubscribe(observer: AnimationClockObserver) {
            unsubscribed = true
        }
    }
}