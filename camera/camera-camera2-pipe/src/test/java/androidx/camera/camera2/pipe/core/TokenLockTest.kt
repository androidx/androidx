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

package androidx.camera.camera2.pipe.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
internal class TokenLockTest {
    @Test
    fun testTokenLockReportsNoAvailableCapacityWhenClosed() {
        val tokenLock = TokenLockImpl(2)
        assertThat(tokenLock.available).isEqualTo(2)
        assertThat(tokenLock.size).isEqualTo(0)
        tokenLock.close()
        assertThat(tokenLock.available).isEqualTo(0)
        assertThat(tokenLock.size).isEqualTo(2)
    }

    @Test
    fun testTokenLockCanAcquireAndCloseTokens() = runBlocking {
        val tokenLock = TokenLockImpl(2)
        assertThat(tokenLock.available).isEqualTo(2)
        assertThat(tokenLock.size).isEqualTo(0)

        val token1 = tokenLock.acquire(1)
        assertThat(tokenLock.available).isEqualTo(1)
        assertThat(tokenLock.size).isEqualTo(1)

        val token2 = tokenLock.acquire(1)
        assertThat(tokenLock.available).isEqualTo(0)
        assertThat(tokenLock.size).isEqualTo(2)

        // Check to make sure that acquireOrNull returns null when all tokens have been allocated.
        assertThat(tokenLock.acquireOrNull(1)).isNull()

        // Close a token, freeing up a token slot
        token2.close()

        // Check to make sure acquireOrNull does *not* return null if the tokenLock has a free
        // slot.
        val token3 = tokenLock.acquireOrNull(1)
        assertThat(token3).isNotNull()

        // Close all outstanding tokens, and check to make sure the capacity is subsequently
        // reported correctly.
        token3?.close()
        token1.close()
        assertThat(tokenLock.available).isEqualTo(2)
        assertThat(tokenLock.size).isEqualTo(0)
    }

    @Test
    fun tokenLockHandlesRequestsThatTimeOut() = runBlocking {
        val tokenLock = TokenLockImpl(2)
        val token1 = tokenLock.acquire(1)

        // This should suspend, and then cancel the request for the token.
        val token2: TokenLock.Token? = withTimeoutOrNull(10) {
            tokenLock.acquire(2)
        }

        assertThat(token2).isNull()
        assertThat(tokenLock.available).isEqualTo(1)

        // Make sure we can still acquire a token after a previous request timed out.
        val token3 = tokenLock.acquire(1)
        assertThat(tokenLock.available).isEqualTo(0)
        token3.close()

        token1.close()
        assertThat(tokenLock.available).isEqualTo(2)
    }

    @Test
    fun tokenLockSuspendsWithAsync() = runBlocking {
        val tokenLock = TokenLockImpl(2)
        val token1 = tokenLock.acquire(1)
        val token2 = tokenLock.acquire(1)

        // This launches a suspendable job that should resume when at least one of the other tokens
        // is closed.
        val token3Job = async { tokenLock.acquire(1) }

        token1.close()
        token2.close()
        token3Job.await().close()
    }

    @Test
    fun tokenLockIsFair() = runBlocking {
        val tokenLock = TokenLockImpl(3)
        val token1 = tokenLock.acquire(2)
        assertThat(tokenLock.available).isEqualTo(1)

        // Using CoroutineStart.UNDISPATCHED, while experimental, has a unique property: It will
        // run synchronously until the coroutine suspends. For this test, this is exactly the
        // behavior we want to test the token lock with because we want to make sure the coroutine
        // has been placed into the internal request queue before we create the second pending job.
        val token2Job = async(start = CoroutineStart.UNDISPATCHED) { tokenLock.acquire(2) }
        val token3Job = async(start = CoroutineStart.UNDISPATCHED) { tokenLock.acquire(2) }

        // Assert that the token lock is fully utilized, which happens when the next pending request
        // is larger than the available capacity of the lock.
        assertThat(tokenLock.available).isEqualTo(0)

        // Assert that all of the async jobs are running, but not complete.
        assertThat(token2Job.isCompleted).isFalse()
        assertThat(token2Job.isCancelled).isFalse()
        assertThat(token2Job.isActive).isTrue()
        assertThat(token3Job.isCompleted).isFalse()
        assertThat(token3Job.isCancelled).isFalse()
        assertThat(token3Job.isActive).isTrue()

        // Closing token1 releases enough capacity for the next job to acquire and resume.
        token1.close()

        // The token lock is well ordered, which means that token2Job is next in the queue and will
        // always resume first.
        val token2 = token2Job.await()

        // token2 is large enough that token3 is still waiting
        assertThat(token3Job.isCompleted).isFalse()
        assertThat(token3Job.isCancelled).isFalse()
        assertThat(token3Job.isActive).isTrue()

        // Closing token2 releases enough capacity for the token3 job to acquire and resume.
        token2.close()
        val token3 = token3Job.await()

        // There are no pending job, and token3 holds onto 2 / 3 of the values.
        assertThat(tokenLock.available).isEqualTo(1)

        // Closing the last token causes memory to be released.
        token3.close()
        assertThat(tokenLock.available).isEqualTo(3)
    }

    @Test
    fun cancelingSuspendedJobReleasesPendingRequest() = runBlocking {
        val tokenLock = TokenLockImpl(3)
        val token1 = tokenLock.acquire(1)
        val token2 = tokenLock.acquire(1)

        val token3Job = async { tokenLock.acquire(2) }
        delay(10)

        assertThat(tokenLock.available).isEqualTo(0)
        assertThat(token3Job.isCompleted).isFalse()
        assertThat(token3Job.isCancelled).isFalse()
        assertThat(token3Job.isActive).isTrue()

        token3Job.cancel()
        assertThat(tokenLock.available).isEqualTo(1)

        token1.close()
        token2.close()
        assertThat(tokenLock.available).isEqualTo(3)
    }

    @Test
    fun closingTokenLockCausesPendingRequestsToThrow() = runBlocking {
        val tokenLock = TokenLockImpl(1)
        val token1 = tokenLock.acquire(1)
        val token2Job = async { tokenLock.acquire(1) }

        // Close the tokenLock itself. This should inform all pending requests that they will
        // never receive a token.
        tokenLock.close()

        try {
            token2Job.await()
            fail("Await should throw an exception if the token lock is closed.")
        } catch (ex: CancellationException) {
            // Expected
        }

        token1.close()
    }

    @Test
    fun tokenLockAcquiresRange() = runBlocking {
        val tokenLock = TokenLockImpl(10)
        val token1 = tokenLock.acquire(8)
        assertThat(tokenLock.available).isEqualTo(2)

        val token2Job = async(start = CoroutineStart.UNDISPATCHED) { tokenLock.acquire(3, 8) }
        val token3Job = async(start = CoroutineStart.UNDISPATCHED) { tokenLock.acquire(3) }
        assertThat(tokenLock.available).isEqualTo(0)

        // Closing the token causes the first request (Token 2) to greedily acquire it's max (8).
        token1.close()
        val token2 = token2Job.await()
        assertThat(token2.value).isEqualTo(8)
        assertThat(tokenLock.available).isEqualTo(0)

        // Token 3 cannot be acquired until token2 is closed.
        token2.close()
        val token3 = token3Job.await()
        assertThat(token3.value).isEqualTo(3)
        token3.close()

        assertThat(tokenLock.available).isEqualTo(10)
    }

    @Test
    fun tokenLockFulfillsMultipleRequests() = runBlocking {
        val tokenLock = TokenLockImpl(10)
        val token1 = tokenLock.acquire(8)
        assertThat(tokenLock.available).isEqualTo(2)

        // Acquire using a range of values. Neither request can be fulfilled until token1 is
        // closed.
        val token2Job = async(start = CoroutineStart.UNDISPATCHED) { tokenLock.acquire(3, 4) }
        val token3Job = async(start = CoroutineStart.UNDISPATCHED) { tokenLock.acquire(3, 8) }
        assertThat(tokenLock.available).isEqualTo(0)

        // When the token is closed, the first request is greedy and should acquire as much as the
        // range allows (4). The second pending request (token 3) should also be greedy, but should
        // be capped at (6) to limit it to the capacity of the TokenLock.
        token1.close()
        val token2 = token2Job.await()
        val token3 = token3Job.await()
        assertThat(token2.value).isEqualTo(4)
        assertThat(token3.value).isEqualTo(6) // Not 8
    }

    @Test
    fun tokensAreClosedWithUseKeyword() = runBlocking {
        val tokenLock = TokenLockImpl(1)

        tokenLock.acquire(1).use {
            assertThat(tokenLock.size).isEqualTo(1)
        }
        assertThat(tokenLock.size).isEqualTo(0)
    }

    @Test
    fun testWithTokenExtension() = runBlocking {
        val tokenLock = TokenLockImpl(1)

        tokenLock.withToken(1) {
            assertThat(tokenLock.size).isEqualTo(1)
        }
        assertThat(tokenLock.size).isEqualTo(0)
    }

    @Test
    fun testWithTokenRange() = runBlocking {
        val tokenLock = TokenLockImpl(3)
        val token1 = tokenLock.acquire(1)

        tokenLock.withToken(1, 4) {
            assertThat(tokenLock.size).isEqualTo(3)
            assertThat(it.value).isEqualTo(2)
        }
        assertThat(tokenLock.size).isEqualTo(1)
        token1.close()
        assertThat(tokenLock.size).isEqualTo(0)
    }

    @Test
    fun tokenReleasesOnlyOnce() = runBlocking {
        val tokenLock = TokenLockImpl(2)
        val token1 = tokenLock.acquire(1)
        val token2 = tokenLock.acquire(1)

        assertThat(token1.release()).isTrue()
        assertThat(token1.release()).isFalse()

        tokenLock.close()

        // Token is not closed as a result of tokenLock.close().
        assertThat(token2.release()).isTrue()
        assertThat(token2.release()).isFalse()

        // The size of the token lock is unaffected by tokens that are released after close.
        assertThat(tokenLock.size).isEqualTo(2)
        assertThat(tokenLock.available).isEqualTo(0)
    }

    @Test
    fun tokenCloseIsTheSameAsRelease() = runBlocking {
        val tokenLock = TokenLockImpl(2)
        val token1 = tokenLock.acquire(1)
        val token2 = tokenLock.acquire(1)

        token1.close()
        assertThat(token1.release()).isFalse()

        tokenLock.close()

        // Token is not closed as a result of tokenLock
        token2.close()
        assertThat(token2.release()).isFalse()

        // The size of the token lock is unaffected by tokens that are released after close.
        assertThat(tokenLock.size).isEqualTo(2)
        assertThat(tokenLock.available).isEqualTo(0)
    }
}