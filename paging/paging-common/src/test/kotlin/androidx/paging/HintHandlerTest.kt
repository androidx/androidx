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

package androidx.paging

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HintHandlerTest {
    private val hintHandler = HintHandler()

    @Test
    fun initialState() {
        assertThat(hintHandler.lastAccessHint).isNull()
        assertThat(hintHandler.currentValue(APPEND)).isNull()
        assertThat(hintHandler.currentValue(PREPEND)).isNull()
    }

    @Test
    fun noStateForRefresh() = runBlockingTest {
        val refreshHints = kotlin.runCatching {
            hintHandler.hintFor(REFRESH)
        }
        assertThat(refreshHints.exceptionOrNull()).isInstanceOf(
            IllegalArgumentException::class.java
        )
    }

    @Test
    fun expandChecks() {
        val initialHint = ViewportHint.Initial(
            presentedItemsAfter = 0,
            presentedItemsBefore = 0,
            originalPageOffsetFirst = 0,
            originalPageOffsetLast = 0
        ).also(hintHandler::processHint)
        hintHandler.assertValues(
            prepend = initialHint,
            append = initialHint,
            lastAccessHint = null
        )

        // both hints get updated w/ access hint
        val accessHint1 = ViewportHint.Access(
            pageOffset = 0,
            indexInPage = 1,
            presentedItemsBefore = 100,
            presentedItemsAfter = 100,
            originalPageOffsetLast = 0,
            originalPageOffsetFirst = 0
        ).also(hintHandler::processHint)

        hintHandler.assertValues(
            prepend = accessHint1,
            append = accessHint1,
            lastAccessHint = accessHint1
        )

        // new access that only affects prepend
        val accessHintPrepend = accessHint1.copy(
            presentedItemsBefore = 70,
            presentedItemsAfter = 110
        ).also(hintHandler::processHint)
        hintHandler.assertValues(
            prepend = accessHintPrepend,
            append = accessHint1,
            lastAccessHint = accessHintPrepend
        )

        // new access hints that should be ignored
        val ignoredPrependHint = accessHintPrepend.copy(
            presentedItemsBefore = 90,
        ).also(hintHandler::processHint)
        hintHandler.assertValues(
            prepend = accessHintPrepend,
            append = accessHint1,
            lastAccessHint = ignoredPrependHint
        )

        val accessHintAppend = accessHintPrepend.copy(
            presentedItemsAfter = 80,
        ).also(hintHandler::processHint)
        hintHandler.assertValues(
            prepend = accessHintPrepend,
            append = accessHintAppend,
            lastAccessHint = accessHintAppend
        )

        // more ignored access hints
        hintHandler.processHint(
            accessHint1
        )
        hintHandler.assertValues(
            prepend = accessHintPrepend,
            append = accessHintAppend,
            lastAccessHint = accessHint1
        )
        hintHandler.processHint(
            initialHint
        )
        hintHandler.assertValues(
            prepend = accessHintPrepend,
            append = accessHintAppend,
            lastAccessHint = accessHint1
        )

        // try changing original page offsets
        val newFirstOffset = accessHintPrepend.copy(
            originalPageOffsetFirst = 2
        ).also(hintHandler::processHint)
        hintHandler.assertValues(
            prepend = newFirstOffset,
            append = newFirstOffset,
            lastAccessHint = newFirstOffset
        )
        val newLastOffset = newFirstOffset.copy(
            originalPageOffsetLast = 5
        ).also(hintHandler::processHint)
        hintHandler.assertValues(
            prepend = newLastOffset,
            append = newLastOffset,
            lastAccessHint = newLastOffset
        )
    }

    @Test
    fun reset() {
        val initial = ViewportHint.Access(
            pageOffset = 0,
            indexInPage = 3,
            presentedItemsBefore = 10,
            presentedItemsAfter = 10,
            originalPageOffsetFirst = 0,
            originalPageOffsetLast = 0
        )
        hintHandler.processHint(initial)

        val appendReset = initial.copy(
            presentedItemsBefore = 20,
            presentedItemsAfter = 5
        )
        hintHandler.forceSetHint(
            APPEND,
            appendReset
        )
        hintHandler.assertValues(
            prepend = initial,
            append = appendReset,
            lastAccessHint = initial
        )

        val prependReset = initial.copy(
            presentedItemsBefore = 4,
            presentedItemsAfter = 19
        )
        hintHandler.forceSetHint(
            PREPEND,
            prependReset
        )
        hintHandler.assertValues(
            prepend = prependReset,
            append = appendReset,
            lastAccessHint = initial
        )
    }

    @Test
    fun resetCanReSendSameValues() = runBlockingTest {
        val hint = ViewportHint.Access(
            pageOffset = 0,
            indexInPage = 1,
            presentedItemsAfter = 10,
            presentedItemsBefore = 10,
            originalPageOffsetFirst = 0,
            originalPageOffsetLast = 0,
        )
        val prependHints = collectAsync(
            hintHandler.hintFor(PREPEND)
        )
        val appendHints = collectAsync(
            hintHandler.hintFor(APPEND)
        )
        runCurrent()
        assertThat(prependHints.values).isEmpty()
        assertThat(appendHints.values).isEmpty()
        hintHandler.processHint(hint)
        runCurrent()
        assertThat(prependHints.values).containsExactly(hint)
        assertThat(appendHints.values).containsExactly(hint)

        // send same hint twice, it should not get dispatched
        hintHandler.processHint(hint.copy())
        runCurrent()
        assertThat(prependHints.values).containsExactly(hint)
        assertThat(appendHints.values).containsExactly(hint)

        // how send that hint as reset, now it should get dispatched
        hintHandler.forceSetHint(PREPEND, hint)
        runCurrent()
        assertThat(prependHints.values).containsExactly(hint, hint)
        assertThat(appendHints.values).containsExactly(hint)
        hintHandler.forceSetHint(APPEND, hint)
        runCurrent()
        assertThat(prependHints.values).containsExactly(hint, hint)
        assertThat(appendHints.values).containsExactly(hint, hint)
    }

    private fun HintHandler.assertValues(
        prepend: ViewportHint,
        append: ViewportHint,
        lastAccessHint: ViewportHint.Access?
    ) {
        assertThat(currentValue(PREPEND)).isEqualTo(prepend)
        assertThat(currentValue(APPEND)).isEqualTo(append)
        assertThat(hintHandler.lastAccessHint).isEqualTo(lastAccessHint)
    }

    private fun HintHandler.currentValue(
        loadType: LoadType
    ): ViewportHint? {
        var value: ViewportHint? = null
        runBlockingTest {
            val job = launch {
                this@currentValue.hintFor(loadType).take(1).collect {
                    value = it
                }
            }
            runCurrent()
            job.cancel()
        }
        return value
    }

    private suspend fun CoroutineScope.collectAsync(
        flow: Flow<ViewportHint>
    ): TestCollection {
        val impl = TestCollectionImpl()
        async(context = impl.job) {
            flow.collect {
                impl.values.add(it)
            }
        }
        return impl
    }

    private interface TestCollection {
        val values: List<ViewportHint>
        fun stop()
        val latest: ViewportHint?
            get() = values.lastOrNull()
    }

    private class TestCollectionImpl : TestCollection {
        val job = Job()
        override val values = mutableListOf<ViewportHint>()
        override fun stop() {
            job.cancel()
        }
    }

    private fun ViewportHint.Access.copy(
        pageOffset: Int = this@copy.pageOffset,
        indexInPage: Int = this@copy.indexInPage,
        presentedItemsBefore: Int = this@copy.presentedItemsBefore,
        presentedItemsAfter: Int = this@copy.presentedItemsAfter,
        originalPageOffsetFirst: Int = this@copy.originalPageOffsetFirst,
        originalPageOffsetLast: Int = this@copy.originalPageOffsetLast
    ) = ViewportHint.Access(
        pageOffset = pageOffset,
        indexInPage = indexInPage,
        presentedItemsBefore = presentedItemsBefore,
        presentedItemsAfter = presentedItemsAfter,
        originalPageOffsetFirst = originalPageOffsetFirst,
        originalPageOffsetLast = originalPageOffsetLast
    )
}