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

package androidx.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class CachedPageEventFlowTest(
    private val terminationType: TerminationType
) {
    private val testScope = TestCoroutineScope()

    @Test
    fun slowFastCollectors() = testScope.runBlockingTest {
        val upstream = Channel<PageEvent<String>>(Channel.UNLIMITED)
        val subject = CachedPageEventFlow(
            src = upstream.consumeAsFlow(),
            scope = testScope
        )
        val fastCollector = PageCollector(subject.downstreamFlow)
        fastCollector.collectIn(testScope)
        val slowCollector = PageCollector(
            subject.downstreamFlow.onEach {
                delay(1_000)
            }
        )
        slowCollector.collectIn(testScope)
        val refreshEvent = localRefresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                )
            ),
        )
        upstream.send(refreshEvent)
        runCurrent()
        // removal of canDispatchWithoutInsert check causes an additional NotLoading state update
        assertThat(fastCollector.items()).containsExactly(
            localLoadStateUpdate<String>(),
            refreshEvent
        )
        assertThat(slowCollector.items()).isEmpty()

        val appendEvent = localAppend(
            listOf(
                TransformablePage(
                    listOf("d", "e")
                )
            ),
        )
        upstream.send(appendEvent)
        runCurrent()
        assertThat(fastCollector.items()).containsExactly(
            localLoadStateUpdate<String>(),
            refreshEvent,
            appendEvent
        )
        assertThat(slowCollector.items()).isEmpty()
        advanceTimeBy(3_000)
        assertThat(slowCollector.items()).containsExactly(
            localLoadStateUpdate<String>(),
            refreshEvent,
            appendEvent
        )
        val manyNewAppendEvents = (0 until 100).map {
            localAppend(
                listOf(
                    TransformablePage(
                        listOf("f", "g")
                    )
                ),
            )
        }
        manyNewAppendEvents.forEach {
            upstream.send(it)
        }
        val lateSlowCollector = PageCollector(subject.downstreamFlow.onEach { delay(1_000) })
        lateSlowCollector.collectIn(testScope)
        val finalAppendEvent = localAppend(
            listOf(
                TransformablePage(
                    listOf("d", "e")
                )
            ),
        )
        upstream.send(finalAppendEvent)
        when (terminationType) {
            TerminationType.CLOSE_UPSTREAM -> upstream.close()
            TerminationType.CLOSE_CACHED_EVENT_FLOW -> subject.close()
        }
        val fullList = listOf(
            localLoadStateUpdate<String>(),
            refreshEvent,
            appendEvent
        ) + manyNewAppendEvents + finalAppendEvent
        runCurrent()
        assertThat(fastCollector.items()).containsExactlyElementsIn(fullList).inOrder()
        assertThat(fastCollector.isActive()).isFalse()
        assertThat(slowCollector.isActive()).isTrue()
        assertThat(lateSlowCollector.isActive()).isTrue()
        advanceUntilIdle()
        assertThat(slowCollector.items()).containsExactlyElementsIn(fullList).inOrder()
        assertThat(slowCollector.isActive()).isFalse()

        val lateCollectorState = localRefresh(
            pages = (listOf(refreshEvent, appendEvent) + manyNewAppendEvents).flatMap {
                it.pages
            },
        )
        assertThat(lateSlowCollector.items()).containsExactly(
            lateCollectorState, finalAppendEvent
        ).inOrder()
        assertThat(lateSlowCollector.isActive()).isFalse()

        upstream.close()
    }

    @Test
    fun ensureSharing() = testScope.runBlockingTest {
        val refreshEvent = localRefresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                )
            ),
        )
        val appendEvent = localAppend(
            listOf(
                TransformablePage(
                    listOf("d", "e")
                )
            ),
        )
        val upstream = Channel<PageEvent<String>>(Channel.UNLIMITED)
        val subject = CachedPageEventFlow(
            src = upstream.consumeAsFlow(),
            scope = testScope
        )

        val collector1 = PageCollector(subject.downstreamFlow)
        upstream.send(refreshEvent)
        upstream.send(appendEvent)
        collector1.collectIn(testScope)
        runCurrent()
        assertThat(collector1.items()).isEqualTo(
            listOf(localLoadStateUpdate<String>(), refreshEvent, appendEvent)
        )
        val collector2 = PageCollector(subject.downstreamFlow)
        collector2.collectIn(testScope)
        runCurrent()
        val firstSnapshotRefreshEvent = localRefresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                ),
                TransformablePage(
                    listOf("d", "e")
                )
            ),
        )
        assertThat(collector2.items()).containsExactly(firstSnapshotRefreshEvent)
        val prependEvent = localPrepend(
            listOf(
                TransformablePage(
                    listOf("a0", "a1")
                ),
                TransformablePage(
                    listOf("a2", "a3")
                )
            ),
        )
        upstream.send(prependEvent)
        assertThat(collector1.items()).isEqualTo(
            listOf(localLoadStateUpdate<String>(), refreshEvent, appendEvent, prependEvent)
        )
        assertThat(collector2.items()).isEqualTo(
            listOf(firstSnapshotRefreshEvent, prependEvent)
        )
        val collector3 = PageCollector(subject.downstreamFlow)
        collector3.collectIn(testScope)
        val finalState = localRefresh(
            listOf(
                TransformablePage(
                    listOf("a0", "a1")
                ),
                TransformablePage(
                    listOf("a2", "a3")
                ),
                TransformablePage(
                    listOf("a", "b", "c")
                ),
                TransformablePage(
                    listOf("d", "e")
                )
            ),
        )
        assertThat(collector3.items()).containsExactly(
            finalState
        )
        assertThat(collector1.isActive()).isTrue()
        assertThat(collector2.isActive()).isTrue()
        assertThat(collector3.isActive()).isTrue()
        when (terminationType) {
            TerminationType.CLOSE_UPSTREAM -> upstream.close()
            TerminationType.CLOSE_CACHED_EVENT_FLOW -> subject.close()
        }
        runCurrent()
        assertThat(collector1.isActive()).isFalse()
        assertThat(collector2.isActive()).isFalse()
        assertThat(collector3.isActive()).isFalse()
        val collector4 = PageCollector(subject.downstreamFlow).also {
            it.collectIn(testScope)
        }
        runCurrent()
        // since upstream is closed, this should just close
        assertThat(collector4.isActive()).isFalse()
        assertThat(collector4.items()).containsExactly(
            finalState
        )
    }

    @Test
    fun emptyPage_singlelocalLoadStateUpdate() = testScope.runBlockingTest {
        val upstream = Channel<PageEvent<String>>(Channel.UNLIMITED)
        val subject = CachedPageEventFlow(
            src = upstream.consumeAsFlow(),
            scope = testScope
        )

        // creating two collectors and collecting right away to assert that all collectors
        // collecting before any INSERT event would receive an initial IDLE state update.
        val collector = PageCollector(subject.downstreamFlow)
        collector.collectIn(testScope)

        val collector2 = PageCollector(subject.downstreamFlow)
        collector2.collectIn(testScope)

        runCurrent()

        // an empty input stream should cause collect to receive a single state update where
        // source = LoadStates.IDLE and mediator = null
        // any collectors that started collection before an Insert should receive this IDLE state
        // update
        assertThat(collector.items()).containsExactly(
            localLoadStateUpdate<String>(),
        )

        assertThat(collector2.items()).containsExactly(
            localLoadStateUpdate<String>(),
        )

        // now send refresh event
        val refreshEvent = localRefresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                )
            ),
        )
        upstream.send(refreshEvent)
        runCurrent()

        assertThat(collector.items()).containsExactly(
            localLoadStateUpdate<String>(),
            refreshEvent
        )

        assertThat(collector2.items()).containsExactly(
            localLoadStateUpdate<String>(),
            refreshEvent
        )

        upstream.close()
    }

    @Test
    fun idleStateUpdate_collectedBySingleCollector() = testScope.runBlockingTest {
        val upstream = Channel<PageEvent<String>>(Channel.UNLIMITED)
        val subject = CachedPageEventFlow(
            src = upstream.consumeAsFlow(),
            scope = testScope
        )

        val refreshEvent = localRefresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                )
            ),
        )
        upstream.send(refreshEvent)
        runCurrent()

        val collector = PageCollector(subject.downstreamFlow)
        collector.collectIn(testScope)

        runCurrent()

        // regardless of when the first collector started collecting (before or after first
        // INSERT), it would receive the initial IDLE state update
        assertThat(collector.items()).containsExactly(
            localLoadStateUpdate<String>(),
            refreshEvent
        )

        val delayedCollector = PageCollector(subject.downstreamFlow)
        delayedCollector.collectIn(testScope)

        // if a collector started collecting an INSERT but the initial IDLE state has
        // already been collected by another collector, then these delayed collectors would not
        // receive it.
        assertThat(delayedCollector.items()).containsExactly(
            refreshEvent
        )

        upstream.close()
    }

    private class PageCollector<T : Any>(val src: Flow<T>) {
        private val items = mutableListOf<T>()
        private var job: Job? = null
        fun collectIn(scope: CoroutineScope) {
            job = scope.launch {
                src.collect {
                    items.add(it)
                }
            }
        }

        fun isActive() = job?.isActive ?: false
        fun items() = items.toList()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = TerminationType.values()
    }

    enum class TerminationType {
        CLOSE_UPSTREAM,
        CLOSE_CACHED_EVENT_FLOW
    }
}