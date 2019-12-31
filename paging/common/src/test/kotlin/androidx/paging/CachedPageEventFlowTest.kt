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

import androidx.paging.PageEvent.Insert.Companion.End
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PageEvent.Insert.Companion.Start
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@FlowPreview
@UseExperimental(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CachedPageEventFlowTest {
    private val testScope = TestCoroutineScope()

    @Test
    fun ensureSharing() = testScope.runBlockingTest {
        val refreshEvent = Refresh(
            listOf(
                TransformablePage(
                    listOf("a", "b", "c")
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = emptyMap()
        )
        val appendEvent = End(
            listOf(
                TransformablePage(
                    listOf("d", "e")
                )
            ),
            placeholdersEnd = 0,
            loadStates = emptyMap()
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
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = emptyMap()
        )
        assertThat(collector2.items()).containsExactly(firstSnapshotRefreshEvent)
        val prependEvent = Start(
            listOf(
                TransformablePage(
                    listOf("a0", "a1")
                ),
                TransformablePage(
                    listOf("a2", "a3")
                )
            ),
            placeholdersStart = 0,
            loadStates = emptyMap()
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
        assertThat(collector3.items()).containsExactly(
            Refresh(
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
                placeholdersStart = 0,
                placeholdersEnd = 0,
                loadStates = emptyMap()
            )
        )
        assertThat(collector1.isActive()).isTrue()
        assertThat(collector2.isActive()).isTrue()
        assertThat(collector3.isActive()).isTrue()
        subject.close()
        runCurrent()
        assertThat(collector1.isActive()).isFalse()
        assertThat(collector2.isActive()).isFalse()
        assertThat(collector3.isActive()).isFalse()
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
}