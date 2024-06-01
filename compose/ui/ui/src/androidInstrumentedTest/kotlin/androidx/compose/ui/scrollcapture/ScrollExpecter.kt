/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.scrollcapture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

internal suspend fun expectingScrolls(
    rule: ComposeTestRule,
    block: suspend ScrollExpecter.() -> Unit
) {
    coroutineScope {
        val scrollExpecter = ScrollExpecter(this, rule::awaitIdle)
        block(scrollExpecter)
    }
}

internal class ScrollExpecter(
    private val coroutineScope: CoroutineScope,
    private val awaitIdle: suspend () -> Unit
) {
    private val scrollRequests = Channel<ScrollRequest>(capacity = Channel.RENDEZVOUS)

    /** This must be wired up to be called from [scrollByOffset]. */
    suspend fun respondToScrollExpectation(offset: Offset): Offset {
        val result = CompletableDeferred<Offset>(parent = currentCoroutineContext().job)
        scrollRequests.send(ScrollRequest(offset, consumeScroll = result::complete))
        return result.await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectScrollRequest(expectedOffset: Offset, consume: Offset = expectedOffset) {
        coroutineScope.launch {
            val request = select {
                scrollRequests.onReceive { it }
                onTimeout(1000) { fail("No scroll request received after 1000ms") }
            }
            assertThat(request.requestedOffset).isEqualTo(expectedOffset)
            request.consumeScroll(consume)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun assertNoPendingScrollRequests() {
        awaitIdle()
        if (!scrollRequests.isEmpty) {
            val requests = buildList {
                do {
                    val request = scrollRequests.tryReceive()
                    request.getOrNull()?.let(::add)
                } while (request.isSuccess)
            }
            fail(
                "Expected no scroll requests, but had ${requests.size}: " +
                    requests.joinToString { it.requestedOffset.toString() }
            )
        }
    }

    private data class ScrollRequest(
        val requestedOffset: Offset,
        val consumeScroll: (Offset) -> Unit
    )
}
