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

package androidx.compose.remote.creation.compose.capture.headless

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test

class HeadlessLifecycleOwnerTest {

    private data class LifecycleTrace(
        val observer: String,
        val event: Lifecycle.Event,
        val state: Lifecycle.State,
    )

    @Test
    fun lifecycle_deliversUpwardInOrder_andDownwardInReverseOrder() {
        val owner = HeadlessLifecycleOwner()
        val trace = mutableListOf<LifecycleTrace>()

        owner.lifecycle.addObserver(
            LifecycleEventObserver { source, event ->
                trace += LifecycleTrace("first", event, source.lifecycle.currentState)
            }
        )
        owner.lifecycle.addObserver(
            LifecycleEventObserver { source, event ->
                trace += LifecycleTrace("second", event, source.lifecycle.currentState)
            }
        )

        owner.close()

        assertThat(trace)
            .containsExactly(
                LifecycleTrace("first", Lifecycle.Event.ON_CREATE, Lifecycle.State.RESUMED),
                LifecycleTrace("first", Lifecycle.Event.ON_START, Lifecycle.State.RESUMED),
                LifecycleTrace("first", Lifecycle.Event.ON_RESUME, Lifecycle.State.RESUMED),
                LifecycleTrace("second", Lifecycle.Event.ON_CREATE, Lifecycle.State.RESUMED),
                LifecycleTrace("second", Lifecycle.Event.ON_START, Lifecycle.State.RESUMED),
                LifecycleTrace("second", Lifecycle.Event.ON_RESUME, Lifecycle.State.RESUMED),
                LifecycleTrace("second", Lifecycle.Event.ON_PAUSE, Lifecycle.State.STARTED),
                LifecycleTrace("first", Lifecycle.Event.ON_PAUSE, Lifecycle.State.STARTED),
                LifecycleTrace("second", Lifecycle.Event.ON_STOP, Lifecycle.State.CREATED),
                LifecycleTrace("first", Lifecycle.Event.ON_STOP, Lifecycle.State.CREATED),
                LifecycleTrace("second", Lifecycle.Event.ON_DESTROY, Lifecycle.State.DESTROYED),
                LifecycleTrace("first", Lifecycle.Event.ON_DESTROY, Lifecycle.State.DESTROYED),
            )
            .inOrder()
    }

    @Test
    fun addObserver_sameInstanceTwice_isIdempotentAndRemovedCleanly() {
        val owner = HeadlessLifecycleOwner()
        val events = mutableListOf<Lifecycle.Event>()
        val observer = LifecycleEventObserver { _, event -> events += event }

        owner.lifecycle.addObserver(observer)
        owner.lifecycle.addObserver(observer)

        assertThat(events)
            .containsExactly(
                Lifecycle.Event.ON_CREATE,
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME,
            )
            .inOrder()

        events.clear()
        owner.lifecycle.removeObserver(observer)
        owner.close()

        assertThat(events).isEmpty()
    }

    @Test
    fun currentStateFlow_returnsSameInstance_andMatchesCurrentStateDuringCallbacks() {
        val owner = HeadlessLifecycleOwner()
        val flow = owner.lifecycle.currentStateFlow
        assertThat(owner.lifecycle.currentStateFlow).isSameInstanceAs(flow)
        assertThat(flow.value).isEqualTo(Lifecycle.State.RESUMED)

        val statesSeen = mutableListOf<Pair<Lifecycle.State, Lifecycle.State>>()
        owner.lifecycle.addObserver(
            LifecycleEventObserver { source, _ ->
                statesSeen +=
                    source.lifecycle.currentState to source.lifecycle.currentStateFlow.value
            }
        )

        owner.close()

        assertThat(statesSeen)
            .containsExactly(
                Lifecycle.State.RESUMED to Lifecycle.State.RESUMED,
                Lifecycle.State.RESUMED to Lifecycle.State.RESUMED,
                Lifecycle.State.RESUMED to Lifecycle.State.RESUMED,
                Lifecycle.State.STARTED to Lifecycle.State.STARTED,
                Lifecycle.State.CREATED to Lifecycle.State.CREATED,
                Lifecycle.State.DESTROYED to Lifecycle.State.DESTROYED,
            )
            .inOrder()
        assertThat(flow.value).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun currentStateFlow_concurrentAccessAndClose_isThreadSafe() {
        val owner = HeadlessLifecycleOwner()
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val flows = CopyOnWriteArrayList<StateFlow<Lifecycle.State>>()
        val errors = CopyOnWriteArrayList<Throwable>()

        val threads =
            List(8) { index ->
                Thread {
                    ready.countDown()
                    start.await(5, TimeUnit.SECONDS)
                    try {
                        val flow = owner.lifecycle.currentStateFlow
                        flows += flow
                        if (index == 0) {
                            owner.close()
                        } else {
                            repeat(100) {
                                val state = flow.value
                                assertThat(state)
                                    .isIn(
                                        listOf(
                                            Lifecycle.State.RESUMED,
                                            Lifecycle.State.STARTED,
                                            Lifecycle.State.CREATED,
                                            Lifecycle.State.DESTROYED,
                                        )
                                    )
                            }
                        }
                    } catch (t: Throwable) {
                        errors += t
                    }
                }
            }
        threads.forEach { it.start() }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
        start.countDown()
        threads.forEach { it.join(5_000) }

        assertThat(errors).isEmpty()
        assertThat(flows).hasSize(8)
        flows.forEach { assertThat(it).isSameInstanceAs(flows.first()) }
        assertThat(owner.lifecycle.currentStateFlow.value).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun addObserver_duringClose_onlyReceivesUpwardEventsUpToCurrentState() {
        val owner = HeadlessLifecycleOwner()
        val lateEvents = mutableListOf<Lifecycle.Event>()
        val lateObserver = LifecycleEventObserver { _, event -> lateEvents += event }

        owner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onPause(owner: LifecycleOwner) {
                    // During ON_PAUSE, currentState is already STARTED, so lateObserver must only
                    // receive ON_CREATE and ON_START upward, followed by ON_STOP and ON_DESTROY.
                    owner.lifecycle.addObserver(lateObserver)
                }
            }
        )

        owner.close()

        assertThat(lateEvents)
            .containsExactly(
                Lifecycle.Event.ON_CREATE,
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY,
            )
            .inOrder()
    }

    @Test
    fun addObserver_afterClose_isIgnored() {
        val owner = HeadlessLifecycleOwner()
        owner.close()

        val events = mutableListOf<Lifecycle.Event>()
        owner.lifecycle.addObserver(LifecycleEventObserver { _, event -> events += event })

        assertThat(events).isEmpty()
    }

    @Test
    fun observerImplementingBothInterfaces_removingItselfInOnCreate_skipsOnStateChanged() {
        val owner = HeadlessLifecycleOwner()
        val callbacks = mutableListOf<String>()
        val dualObserver =
            object : DefaultLifecycleObserver, LifecycleEventObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    callbacks += "default:onCreate"
                    owner.lifecycle.removeObserver(this)
                }

                override fun onStart(owner: LifecycleOwner) {
                    callbacks += "default:onStart"
                }

                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    callbacks += "event:$event"
                }
            }

        owner.lifecycle.addObserver(dualObserver)
        owner.close()

        assertThat(callbacks).containsExactly("default:onCreate")
    }
}
