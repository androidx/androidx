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

package androidx.ui.tooling.preview.animation

import android.util.Log
import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationClockObserver
import androidx.animation.ManualAnimationClock
import androidx.annotation.VisibleForTesting

/**
 * [AnimationClockObservable] used to control animations in the context of Compose Previews. This
 * clock is expected to be controlled by the Animation Inspector in Android Studio, and most of
 * its methods will be called via reflection, either directly from Android Studio or through
 * `ComposeViewAdapter`.
 *
 * It uses an underlying [ManualAnimationClock], as users will be able to select specific frames
 * of subscribed animations when inspecting them in Android Studio.
 *
 * @suppress
 */
class PreviewAnimationClock(private val initialTimeMs: Long = 0L) : AnimationClockObservable {

    private val TAG = "PreviewAnimationClock"

    private val DEBUG = false

    @VisibleForTesting
    internal val clock = ManualAnimationClock(initialTimeMs)

    override fun subscribe(observer: AnimationClockObserver) {
        if (DEBUG) {
            Log.d(TAG, "AnimationClockObserver $observer subscribed")
        }
        clock.subscribe(observer)
        // TODO: parse observer and notify Android Studio that the observers list has been updated.
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        if (DEBUG) {
            Log.d(TAG, "AnimationClockObserver $observer unsubscribed")
        }
        clock.unsubscribe(observer)
        // TODO: parse observer and notify Android Studio that the observers list has been updated.
    }

    /**
     * Sets [clock] time to the given [animationTimeMs], relative to [initialTimeMs].
     *
     * @suppress
     */
    fun setClockTime(animationTimeMs: Long) {
        clock.clockTimeMillis = initialTimeMs + animationTimeMs
    }
}