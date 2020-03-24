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

package androidx.ui.animation

import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationClockObserver
import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.remember
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import java.util.concurrent.atomic.AtomicReference

/**
 * Return a new [AnimationClockObservable] wrapping this one that will auto-unsubscribe all
 * [AnimationClockObserver]s when this call leaves the composition, preventing clock
 * subscriptions from persisting beyond the composition lifecycle.
 *
 * If you are creating an animation object during composition that will subscribe to an
 * [AnimationClockObservable] or otherwise hold a long-lived reference to one to subscribe to later,
 * create that object with a clock returned by this method.
 */
@Composable
fun AnimationClockObservable.asDisposableClock(): DisposableAnimationClock {
    val disposable = remember(this) { DisposableAnimationClock(this) }
    onCommit(disposable) {
        onDispose {
            disposable.dispose()
        }
    }
    return disposable
}

/**
 * Clock that remembers all of its active subscriptions so that it can dispose of upstream
 * subscriptions. Create auto-disposing clocks in composition using [asDisposableClock].
 */
class DisposableAnimationClock(
    private val clock: AnimationClockObservable
) : AnimationClockObservable {

    // TODO switch to atomicfu if this class survives a move to suspending animation API
    private val allSubscriptions = AtomicReference<PersistentSet<AnimationClockObserver>?>(
        persistentSetOf()
    )

    override fun subscribe(observer: AnimationClockObserver) {
        while (true) {
            val old = allSubscriptions.get()
            val new = old?.add(observer) ?: return // null means already disposed
            if (allSubscriptions.compareAndSet(old, new)) {
                clock.subscribe(observer)
                return
            }
        }
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        while (true) {
            val old = allSubscriptions.get() ?: return // null means already disposed
            val new = old.remove(observer)
            if (old == new) return
            if (allSubscriptions.compareAndSet(old, new)) {
                clock.unsubscribe(observer)
                return
            }
        }
    }

    /**
     * Unsubscribe all current subscriptions to the clock. No new subscriptions are permitted;
     * further calls to [subscribe] will be ignored.
     * After a call to [dispose], [isDisposed] will return `true`.
     */
    fun dispose() {
        allSubscriptions.getAndSet(null)?.forEach { clock.unsubscribe(it) }
    }

    /**
     * `true` if [dispose] has been called and no new subscriptions are permitted.
     */
    val isDisposed: Boolean get() = allSubscriptions.get() == null
}