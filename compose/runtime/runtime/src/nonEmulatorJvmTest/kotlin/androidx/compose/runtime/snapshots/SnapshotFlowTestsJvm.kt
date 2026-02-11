/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.SnapshotFlowManager
import androidx.compose.runtime.internal.AtomicBoolean
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class SnapshotFlowTestsJvm {
    private enum class SnapshotFlowManagerKind {
        /** Make the test feature a [SnapshotFlowManager] that only manages one [snapshotFlow]. */
        SINGLE_SUBSCRIPTION,

        /** Make the test feature a [SnapshotFlowManager] that manages multiple [snapshotFlow]s. */
        MULTI_SUBSCRIPTION,
    }

    /**
     * Apply observers can run concurrently with other parts of [snapshotFlow] logic, so this test
     * body checks that no data corruption occurs when [Snapshot.sendApplyNotifications] is called
     * from one thread while a [snapshotFlow] emits values on a different thread. The constraints on
     * the [SnapshotFlowManager] backing the [snapshotFlow] are determined by
     * [snapshotFlowManagerKind].
     */
    private fun snapshotFlowManagerRaceTestsImpl(snapshotFlowManagerKind: SnapshotFlowManagerKind) =
        runBlocking {
            (0 until 100).forEach { _ ->
                val numStateObjects = 10
                val stateObjects = List(numStateObjects) { mutableIntStateOf(1) }

                var firstValueReceived = AtomicBoolean(false)
                var lastValueReceived = AtomicBoolean(false)
                lateinit var job: Job
                // When [SnapshotFlowManagerKind.MULTI_SUBSCRIPTION] is specified as the
                // [snapshotFlowManagerKind] argument, [manager] is made to manage subscriptions
                // for the [snapshotFlow] launched in [job] and the one launched in
                // [supplementalJob].
                var supplementalJob: Job? = null
                val collector =
                    thread(name = "collector") {
                        runBlocking {
                            var manager: SnapshotFlowManager? = null
                            var launchSupplementalJob: ((CoroutineScope) -> Unit)? = null
                            if (
                                snapshotFlowManagerKind ==
                                    SnapshotFlowManagerKind.MULTI_SUBSCRIPTION
                            ) {
                                manager = SnapshotFlowManager()
                                launchSupplementalJob = { scope ->
                                    supplementalJob =
                                        snapshotFlowFactory(manager) {
                                                stateObjects.first().intValue
                                            }
                                            .onEach {
                                                // The cancellation of the supplemental job is
                                                // intertwined with
                                                // [Snapshot.sendApplyNotifications] calls (see
                                                // [mutator]) so that this test can also cover races
                                                // between the cancellation of a [snapshotFlow] and
                                                // [Snapshot.sendApplyNotifications] calls running
                                                // on another thread.
                                                supplementalJob!!.cancel()
                                            }
                                            .launchIn(scope)
                                }
                            }

                            job =
                                snapshotFlowFactory(manager) {
                                        (0 until numStateObjects).map { stateObjects[it].intValue }
                                    }
                                    .onEach { l ->
                                        if (l.all { it == 1 }) {
                                            firstValueReceived.set(true)
                                            // The launching of the supplemental job is intertwined
                                            // with [Snapshot.sendApplyNotifications] calls (see
                                            // [mutator]) so that this test can also cover races
                                            // between the promotion of a
                                            // [SingleSubscriptionSnapshotFlowManager] and
                                            // [Snapshot.sendApplyNotifications] calls running on
                                            // another thread.
                                            launchSupplementalJob?.invoke(this)
                                        } else if (l.all { it == 2 }) {
                                            // If the `snapshotFlow` fails to emit an array full of
                                            // `2`s because data corruption causes the manager to
                                            // skip sending a notification, the `fail` invocation
                                            // below will be triggered.
                                            lastValueReceived.set(true)
                                        }
                                    }
                                    .launchIn(this)
                        }
                    }

                while (!firstValueReceived.get()) {}

                val mutator =
                    thread(name = "mutator") {
                        for (i in 0 until numStateObjects) {
                            stateObjects[i].intValue = 2
                            Snapshot.sendApplyNotifications()
                        }
                    }

                var timesDelayed = 0
                while (!lastValueReceived.get() && timesDelayed < 1000) {
                    timesDelayed++
                    delay(1)
                }
                job.cancel()
                collector.join()
                mutator.join()
                if (!lastValueReceived.get()) {
                    fail("Data corruption caused snapshotFlow to miss a notification")
                }
            }
        }

    @Test
    fun snapshotFlowManagerRace_managingOneSnapshotFlow() =
        snapshotFlowManagerRaceTestsImpl(SnapshotFlowManagerKind.SINGLE_SUBSCRIPTION)

    @Test
    fun snapshotFlowManagerRace_managingMultipleSnapshotFlows() =
        snapshotFlowManagerRaceTestsImpl(SnapshotFlowManagerKind.MULTI_SUBSCRIPTION)

    /**
     * We previously encountered deadlocks when:
     * 1. Two `SnapshotFlowManager`s on different threads would each acquire their own locks
     * 2. Both `SnapshotFlowManager`s would try to advance the global snapshot simultaneously, which
     *    entails running the global apply observers
     * 3. Both threads would run the apply observer associated with the `SnapshotFlowManager`
     *    running on the other thread, and both threads would get stuck trying to acquire the lock
     *    of the `SnapshotFlowManager` running on the other thread
     *
     * This test serves as a regression test against such deadlocks.
     */
    @Test
    fun snapshotFlowDeadlockTest() = runTest {
        val state = mutableIntStateOf(0)
        val timesToIncrementState = 10000

        val collectors = Array<Thread?>(2) { null }

        withContext(Dispatchers.Default) {
            (0 until collectors.size).forEach { i ->
                collectors[i] = thread {
                    runBlocking {
                        lateinit var job: Job
                        job =
                            snapshotFlow {
                                    Snapshot.takeSnapshot().run {
                                        try {
                                            enter { state.intValue }
                                        } finally {
                                            dispose()
                                        }
                                    }
                                }
                                .onEach {
                                    if (it == timesToIncrementState) {
                                        job.cancel()
                                    }
                                }
                                .launchIn(this)
                    }
                }
            }
        }

        (0 until timesToIncrementState).forEach { _ ->
            state.intValue++
            Snapshot.sendApplyNotifications()
        }

        collectors.forEach { it!!.join() }
    }

    companion object {
        // Like `snapshotFlow`, but with a nullable `manager` parameter.
        fun <T> snapshotFlowFactory(manager: SnapshotFlowManager?, block: () -> T): Flow<T> {
            return if (manager == null) {
                snapshotFlow(block)
            } else {
                snapshotFlow(manager, block)
            }
        }
    }
}
