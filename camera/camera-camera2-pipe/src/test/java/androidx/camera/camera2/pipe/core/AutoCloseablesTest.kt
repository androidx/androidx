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

package androidx.camera.camera2.pipe.core

import androidx.camera.camera2.pipe.core.AutoCloseables.useEachIndexed
import androidx.camera.camera2.pipe.core.AutoCloseables.useEachIndexedAsync
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.annotation.Config

@RunWith(JUnit4::class)
@Config(sdk = [Config.NEWEST_SDK])
class AutoCloseablesTest {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val simpleCloseables = listOf(FakeCloseable(), FakeCloseable(), FakeCloseable())

    private val throwingCloseables =
        listOf(
            FakeCloseable(throwOnClose = true),
            FakeCloseable(throwOnClose = true),
            FakeCloseable(throwOnClose = true),
        )

    @Test
    fun useEachIndexedClosesAllElements() {
        val indexes = mutableListOf<Int>()
        val elements = mutableListOf<FakeCloseable>()

        useEachIndexed(simpleCloseables) { index, closeable ->
            indexes.add(index)
            elements.add(closeable)
        }

        assertThat(elements).containsExactlyElementsIn(simpleCloseables).inOrder()
        assertThat(indexes).containsExactlyElementsIn(listOf(0, 1, 2)).inOrder()
        for (fakeCloseable in simpleCloseables) {
            assertThat(fakeCloseable.closeInvocations.value).isEqualTo(1)
        }
    }

    @Test
    fun useEachIndexedClosesAllElementsEvenIfActionThrows() {
        val indexes = mutableListOf<Int>()
        val elements = mutableListOf<FakeCloseable>()

        try {
            useEachIndexed(simpleCloseables) { index, closeable ->
                indexes.add(index)
                elements.add(closeable)
                if (index == 1) {
                    throw IllegalStateException()
                }
            }
        } catch (e: Exception) {
            // Root exception should be `IllegalStateException` since it's the first exception.
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.suppressed).isEmpty()
        }

        assertThat(indexes).containsExactlyElementsIn(listOf(0, 1)).inOrder()
        for (fakeCloseable in simpleCloseables) {
            assertThat(fakeCloseable.closeInvocations.value).isEqualTo(1)
        }
    }

    @Test
    fun useEachIndexedClosesAllElementsEvenIfEverythingThrows() {
        try {
            useEachIndexed(throwingCloseables) { _, _ -> }

            // Should never reach this point (close will throw)
            throw AssertionError()
        } catch (e: Exception) {
            // Root exception should be `IllegalStateException` since it's the first exception.
            assertThat(e).isInstanceOf(Exception::class.java)

            // Exceptions from close are suppressed.
            assertThat(e.suppressed.size).isEqualTo(2)
        }

        for (fakeCloseable in throwingCloseables) {
            assertThat(fakeCloseable.closeInvocations.value).isEqualTo(1)
        }
    }

    @Test
    fun useEachIndexedAsyncClosesAllElements(): Unit {
        val indexes = mutableListOf<Int>()
        val elements = mutableListOf<FakeCloseable>()
        val lock = Any()

        val results =
            useEachIndexedAsync(scope, simpleCloseables) { index, closeable ->
                synchronized(lock) {
                    indexes.add(index)
                    elements.add(closeable)
                    index // return the index
                }
            }

        val returnValues = runBlocking { results.awaitAll() }

        // Should contain one of each, but not required to be in order.
        assertThat(elements).containsExactlyElementsIn(simpleCloseables)
        assertThat(indexes).containsExactlyElementsIn(listOf(0, 1, 2))
        assertThat(returnValues).containsExactlyElementsIn(listOf(0, 1, 2)).inOrder()

        for (fakeCloseable in simpleCloseables) {
            assertThat(fakeCloseable.closeInvocations.value).isEqualTo(1)
        }
    }

    @Test
    fun useEachIndexedAsyncClosesElementsEvenOnThrow(): Unit {
        val results =
            useEachIndexedAsync(scope, throwingCloseables) { _, _ -> throw IllegalStateException() }

        runBlocking { results.joinAll() }

        // Close was attempted on each closeable.
        for (fakeCloseable in throwingCloseables) {
            assertThat(fakeCloseable.closeInvocations.value).isEqualTo(1)
        }
    }

    private class FakeCloseable(private val throwOnClose: Boolean = false) : AutoCloseable {
        val closeInvocations = atomic(0)

        override fun close() {
            closeInvocations.incrementAndGet()
            if (throwOnClose) {
                throw Exception()
            }
        }
    }
}
