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

import androidx.paging.PageEvent.Insert.Companion.Append
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
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
        val refreshEvent = Refresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
        )
        upstream.send(refreshEvent)
        runCurrent()
        assertThat(fastCollector.items()).containsExactly(refreshEvent)
        assertThat(slowCollector.items()).isEmpty()

        val appendEvent = Append(
            listOf(
                TransformablePage(
                    listOf("d", "e")
                )
            ),
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
        )
        upstream.send(appendEvent)
        runCurrent()
        assertThat(fastCollector.items()).containsExactly(refreshEvent, appendEvent)
        assertThat(slowCollector.items()).isEmpty()
        advanceTimeBy(2_000)
        assertThat(slowCollector.items()).containsExactly(refreshEvent, appendEvent)
        val manyNewAppendEvents = (0 until 100).map {
            Append(
                listOf(
                    TransformablePage(
                        listOf("f", "g")
                    )
                ),
                placeholdersAfter = 0,
                combinedLoadStates = localLoadStatesOf()
            )
        }
        manyNewAppendEvents.forEach {
            upstream.send(it)
        }
        val lateSlowCollector = PageCollector(subject.downstreamFlow.onEach { delay(1_000) })
        lateSlowCollector.collectIn(testScope)
        val finalAppendEvent = Append(
            listOf(
                TransformablePage(
                    listOf("d", "e")
                )
            ),
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
        )
        upstream.send(finalAppendEvent)
        when (terminationType) {
            TerminationType.CLOSE_UPSTREAM -> upstream.close()
            TerminationType.CLOSE_CACHED_EVENT_FLOW -> subject.close()
        }
        val fullList = listOf(refreshEvent, appendEvent) + manyNewAppendEvents + finalAppendEvent
        runCurrent()
        assertThat(fastCollector.items()).containsExactlyElementsIn(fullList).inOrder()
        assertThat(fastCollector.isActive()).isFalse()
        assertThat(slowCollector.isActive()).isTrue()
        assertThat(lateSlowCollector.isActive()).isTrue()
        advanceUntilIdle()
        assertThat(slowCollector.items()).containsExactlyElementsIn(fullList).inOrder()
        assertThat(slowCollector.isActive()).isFalse()

        val lateCollectorState = Refresh(
            pages = (listOf(refreshEvent, appendEvent) + manyNewAppendEvents).flatMap {
                it.pages
            },
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
        )
        assertThat(lateSlowCollector.items()).containsExactly(
            lateCollectorState, finalAppendEvent
        ).inOrder()
        assertThat(lateSlowCollector.isActive()).isFalse()
    }

    @Test
    fun ensureSharing() = testScope.runBlockingTest {
        val refreshEvent = Refresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
        )
        val appendEvent = Append(
            listOf(
                TransformablePage(
                    listOf("d", "e")
                )
            ),
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
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
            listOf(refreshEvent, appendEvent)
        )
        val collector2 = PageCollector(subject.downstreamFlow)
        collector2.collectIn(testScope)
        runCurrent()
        val firstSnapshotRefreshEvent = Refresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                ),
                TransformablePage(
                    listOf("d", "e")
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
        )
        assertThat(collector2.items()).containsExactly(firstSnapshotRefreshEvent)
        val prependEvent = Prepend(
            listOf(
                TransformablePage(
                    listOf("a0", "a1")
                ),
                TransformablePage(
                    listOf("a2", "a3")
                )
            ),
            placeholdersBefore = 0,
            combinedLoadStates = localLoadStatesOf()
        )
        upstream.send(prependEvent)
        assertThat(collector1.items()).isEqualTo(
            listOf(refreshEvent, appendEvent, prependEvent)
        )
        assertThat(collector2.items()).isEqualTo(
            listOf(firstSnapshotRefreshEvent, prependEvent)
        )
        val collector3 = PageCollector(subject.downstreamFlow)
        collector3.collectIn(testScope)
        val finalState = Refresh(
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
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = localLoadStatesOf()
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