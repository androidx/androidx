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

package androidx.kruth

import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.Future
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * Tests (and effectively sample code) for the Expect verb (implemented as a rule)
 */
class ExpectTest {
    private val oopsNotARule: Expect = Expect.create()
    private val expect: Expect = Expect.create()

    // We use ExpectedException so that we can test our code that runs after the test method completes.
    @Suppress("DEPRECATION")
    private val thrown: ExpectedException = ExpectedException.none()
    private val postTestWait: TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                base.evaluate()
                testMethodComplete.countDown()
                taskToAwait.get()
            }
        }
    }
    private val testMethodComplete: CountDownLatch = CountDownLatch(1)

    /**
     * A task that the main thread will await, to be provided by tests that do work in other threads.
     */
    private var taskToAwait: Future<*> = immediateFuture(null)

    @get:Rule
    val wrapper: TestRule = TestRule { statement, description ->
        var nextStatement = expect.apply(statement, description)
        nextStatement = postTestWait.apply(nextStatement, description)
        nextStatement = thrown.apply(nextStatement, description)
        nextStatement
    }

    @Test
    fun expectTrue() {
        expect.that(4).isEqualTo(4)
    }

    @Test
    fun singleExpectationFails() {
        thrown.expectMessage("1 expectation failed:")
        thrown.expectMessage("1. x")
        expect.withMessage("x").fail()
    }

    @Test
    fun expectFail() {
        thrown.expectMessage("3 expectations failed:")
        thrown.expectMessage("1. x")
        thrown.expectMessage("2. y")
        thrown.expectMessage("3. z")
        expect.withMessage("x").fail()
        expect.withMessage("y").fail()
        expect.withMessage("z").fail()
    }

    @Test
    fun expectFail10Aligned() {
        thrown.expectMessage("10 expectations failed:")
        thrown.expectMessage(" 1. x")
        thrown.expectMessage("10. x")
        repeat(10) {
            expect.withMessage("x").fail()
        }
    }

    @Test
    fun expectFail10WrappedAligned() {
        thrown.expectMessage("10 expectations failed:")
        thrown.expectMessage(" 1. abc\n      xyz")
        thrown.expectMessage("10. abc\n      xyz")
        repeat(10) {
            expect.withMessage("abc\nxyz").fail()
        }
    }

    @Test
    fun expectFailWithExceptionNoMessage() {
        thrown.expectMessage("3 expectations failed:")
        thrown.expectMessage("1. x")
        thrown.expectMessage("2. y")
        thrown.expectMessage("3. Also, after those failures, an exception was thrown:")
        expect.withMessage("x").fail()
        expect.withMessage("y").fail()
        throw IllegalStateException()
    }

    @Test
    fun expectFailWithExceptionWithMessage() {
        thrown.expectMessage("3 expectations failed:")
        thrown.expectMessage("1. x")
        thrown.expectMessage("2. y")
        thrown.expectMessage("3. Also, after those failures, an exception was thrown:")
        expect.withMessage("x").fail()
        expect.withMessage("y").fail()
        throw IllegalStateException("testing")
    }

    @Test
    fun expectFailWithExceptionBeforeExpectFailures() {
        thrown.expect(IllegalStateException::class.java)
        thrown.expectMessage("testing")
        throwException()
        expect.withMessage("x").fail()
        expect.withMessage("y").fail()
    }

    private fun throwException() {
        throw IllegalStateException("testing")
    }

    @Test
    fun warnWhenExpectIsNotRule() {
        val message = "assertion made on Expect instance, but it's not enabled as a @Rule."
        thrown.expectMessage(message)
        oopsNotARule.that(true).isEqualTo(true)
    }

    @Test
    fun bash() = runTest {
        val results = mutableListOf<Deferred<*>>()
        repeat(1000) {
            results.add(async { expect.that(3).isEqualTo(4) })
        }
        results.forEach { it.await() }
        thrown.expectMessage("1000 expectations failed:")
    }

    @Test
    fun failWhenCallingThatAfterTest() {
        val executor = newSingleThreadExecutor()
        taskToAwait = executor.submit {
            awaitUninterruptibly(testMethodComplete)
            assertFailsWith<IllegalStateException> {
                expect.that(3)
            }
        }
        executor.shutdown()
    }

    @Test
    fun failWhenCallingFailingAssertionMethodAfterTest() {
        val executor = newSingleThreadExecutor()
        /*
         * We wouldn't expect people to do this exactly. The point is that, if someone were to call
         * expect.that(3).isEqualTo(4), we would always either fail the test or throw an
         * IllegalStateException, not record a "failure" that we never read.
         */
        val expectThat3 = expect.that(3)
        taskToAwait = executor.submit {
            awaitUninterruptibly(testMethodComplete)
            val expectedException = assertFailsWith<IllegalStateException> {
                expectThat3.isEqualTo(4)
            }
            assertThat(expectedException).hasCauseThat().isInstanceOf<AssertionError>()
        }
        executor.shutdown()
    }
}
