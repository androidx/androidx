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

package androidx.paging.futures

import androidx.arch.core.util.Function
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

@RunWith(Parameterized::class)
class FuturesTest(private val type: String) {
    private fun testFutures(tester: (ListenableFuture<String>, ListenableFuture<String>) -> Unit) {
        if (type == guava) {
            val future = SettableFuture.create<String>()

            val wrapper = future.transform(
                Function<String, String> { it },
                DirectExecutor
            )

            tester(future, wrapper)
        } else {
            val future = ResolvableFuture.create<String>()

            val wrapper = future.transform(
                Function<String, String> { it },
                DirectExecutor
            )
            tester(future, wrapper)
        }
    }

    private fun <T> ListenableFuture<T>.set(value: T) {
        if (type == guava) {
            (this as SettableFuture).set(value)
        } else {
            (this as ResolvableFuture).set(value)
        }
    }

    private fun <T> ListenableFuture<T>.setException(throwable: Throwable) {
        if (type == guava) {
            (this as SettableFuture).setException(throwable)
        } else {
            (this as ResolvableFuture).setException(throwable)
        }
    }

    private fun <T> ListenableFuture<T>.verifyException(verifier: (Throwable) -> Unit) {
        assertTrue(isDone)
        try {
            get()
        } catch (throwable: Throwable) {
            verifier(throwable)
            return
        }
        throw IllegalStateException("No exception found")
    }

    @Test
    fun set() = testFutures { original, wrapper ->
        assertFalse(original.isCancelled)
        assertFalse(wrapper.isCancelled)

        original.set("foo")

        assertEquals("foo", original.get())
        assertEquals("foo", wrapper.get())
    }

    @Test
    fun cancel() = testFutures { original, wrapper ->
        assertFalse(original.isCancelled)
        assertFalse(wrapper.isCancelled)

        wrapper.cancel(false)

        assertTrue(original.isCancelled)
        assertTrue(wrapper.isCancelled)

        val verification = { throwable: Throwable ->
            assertTrue(throwable is CancellationException)
        }
        original.verifyException(verification)
        wrapper.verifyException(verification)
    }

    @Test
    fun setException() = testFutures { original, wrapper ->
        val exception = Throwable()

        assertFalse(original.isDone)
        assertFalse(wrapper.isDone)

        original.setException(exception)

        val verification = { throwable: Throwable ->
            assertTrue(throwable is ExecutionException)
            assertEquals(throwable.cause, exception)
        }
        original.verifyException(verification)
        wrapper.verifyException(verification)
    }

    companion object {
        val guava = "GuavaFutures"
        val fake = "FakeFutures"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Array<Array<String>> {
            return arrayOf(arrayOf(guava), arrayOf(fake))
        }
    }
}
