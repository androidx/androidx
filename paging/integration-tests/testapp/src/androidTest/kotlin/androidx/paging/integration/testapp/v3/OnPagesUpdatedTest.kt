/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.paging.integration.testapp.v3

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertTrue

@LargeTest
@RunWith(AndroidJUnit4::class)
class OnPagesUpdatedTest {
    @get:Rule
    val scenarioRule = ActivityScenarioRule(V3Activity::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Ignore("b/278918559")
    fun onPagesUpdatedFlow() = runBlocking {
        val scenario = scenarioRule.scenario

        lateinit var job: Job
        lateinit var adapter: V3Adapter
        scenario.onActivity { activity ->
            adapter = activity.pagingAdapter
        }

        // Wait for initial load to complete.
        adapter.onPagesUpdatedFlow.first { adapter.itemCount > 0 }

        val onPagesUpdatedEventsCh = Channel<Unit>(capacity = 100)
        val processNextPageUpdateCh = Channel<Unit>(capacity = 1)
        scenario.onActivity { activity ->
            // Items are loaded before we start observing.
            assertThat(activity.pagingAdapter.itemCount).isGreaterThan(0)

            job = activity.lifecycleScope.launch {
                activity.pagingAdapter.onPagesUpdatedFlow.collect {
                    onPagesUpdatedEventsCh.send(it)
                    processNextPageUpdateCh.receive()
                }
            }

            // Page update from before we started listening should not be buffered.
            assertTrue { onPagesUpdatedEventsCh.isEmpty }
        }

        // Trigger page update.
        adapter.refreshAndAwaitIdle()
        onPagesUpdatedEventsCh.receiveWithTimeoutMillis(10_000)

        // Trigger page update while still processing previous one, this should get buffered.
        adapter.refreshAndAwaitIdle()
        // Ensure we are still waiting for processNextPageUpdateCh to emit to continue.
        assertTrue { onPagesUpdatedEventsCh.isEmpty }

        // Now allow collector to continue until idle.
        processNextPageUpdateCh.send(Unit)
        onPagesUpdatedEventsCh.receiveWithTimeoutMillis(10_000)
        processNextPageUpdateCh.send(Unit)

        // Trigger a bunch of updates without unblocking page update collector.
        repeat(66) {
            adapter.refreshAndAwaitIdle()
        }

        // Fully unblock collector.
        var pageUpdates = 0
        try {
            while (true) {
                processNextPageUpdateCh.trySend(Unit)
                onPagesUpdatedEventsCh.receiveWithTimeoutMillis(10_000)
                pageUpdates++
            }
        } catch (e: TimeoutCancellationException) {
            // Ignored, we will eventually hit this once we receive all events.
        }

        // We should receive exactly 65 events, due to 64 getting buffered.
        assertThat(pageUpdates).isEqualTo(65)

        onPagesUpdatedEventsCh.close()
        processNextPageUpdateCh.close()
        job.cancel()
    }

    private suspend fun Channel<Unit>.receiveWithTimeoutMillis(timeoutMillis: Long) {
        withTimeout(timeoutMillis) { receive() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun V3Adapter.refreshAndAwaitIdle() {
        val scenario = scenarioRule.scenario
        val loadStateCollectorStarted = CompletableDeferred<Unit>()
        val result = CoroutineScope(EmptyCoroutineContext).async {
            loadStateFlow
                .onStart { loadStateCollectorStarted.complete(Unit) }
                .scan(RefreshState.INITIAL) { acc, next ->
                    when (acc) {
                        RefreshState.INITIAL -> {
                            if (next.source.refresh is LoadState.Loading) {
                                RefreshState.LOADING
                            } else {
                                RefreshState.INITIAL
                            }
                        }
                        RefreshState.LOADING -> {
                            if (next.source.refresh !is LoadState.Loading) {
                                RefreshState.DONE
                            } else {
                                RefreshState.LOADING
                            }
                        }
                        else -> acc
                    }
                }
                .first { it == RefreshState.DONE }
        }

        loadStateCollectorStarted.await()
        scenario.onActivity { refresh() }
        result.await()
    }

    private enum class RefreshState {
        INITIAL, LOADING, DONE
    }
}