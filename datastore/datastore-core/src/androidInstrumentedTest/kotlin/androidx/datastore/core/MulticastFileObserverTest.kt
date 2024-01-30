/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.datastore.core

import android.annotation.SuppressLint
import androidx.kruth.assertWithMessage
import androidx.test.filters.LargeTest
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@LargeTest
class MulticastFileObserverTest {
    // use real coroutines in this test as we need to rely on FileObserver behavior which
    // we cannot control
    private val testScope = CoroutineScope(Dispatchers.IO)

    @get:Rule
    val tmpFolder = TemporaryFolder()

    @After
    fun cleanup() {
        testScope.cancel()
        // wait until there are no observers left
        assertNoObserversAreLeft()
    }

    @Before
    fun prepare() {
        MulticastFileObserver.removeAllObservers()
    }

    @SuppressLint("BanThreadSleep") // no observable available to get notified besides polling
    private fun assertNoObserversAreLeft() {
        // wait 5 seconds for it to close. It really shouldn't take 5 seconds but just being safe
        for (i in 0 until 50) {
            if (MulticastFileObserver.fileObservers.isEmpty()) {
                break;
            }
            Thread.sleep(100)
        }
        assertWithMessage(
            "Didn't reach expected observer count"
        ).that(
            MulticastFileObserver.fileObservers.size
        ).isEqualTo(0)
    }

    @Test
    fun twoObserversNotified() = runBlocking {
        val folder = tmpFolder.newFolder()
        folder.mkdirs()
        val f1 = folder.resolve("f1").also {
            it.writeText("x")
        }
        val f2 = folder.resolve("f2").also {
            it.writeText("y")
        }
        val subject1 = UpdateCollector(testScope, f1)
        val subject2 = UpdateCollector(testScope, f2)
        subject1.awaitValue("x")
        subject2.awaitValue("y")
        f1.modify("a")
        f2.modify("b")
        subject1.awaitValue("a")
        subject2.awaitValue("b")
        f1.modify("a2")
        subject1.awaitValue("a2")
        f1.modify("a3")
        subject1.awaitValue("a3")
        f2.modify("b2")
        subject2.awaitValue("b2")
    }

    @Test
    fun observerAddedRemovedReAdded() = runBlocking {
        val folder = tmpFolder.newFolder()
        folder.mkdirs()
        val f1 = folder.resolve("f1").also {
            it.writeText("x")
        }
        val f2 = folder.resolve("f2").also {
            it.writeText("y")
        }
        val subject1Scope = CoroutineScope(
            testScope.coroutineContext + Job(
                testScope.coroutineContext[Job]
            )
        )
        val subject2Scope = CoroutineScope(
            testScope.coroutineContext + Job(
                testScope.coroutineContext[Job]
            )
        )
        val subject1 = UpdateCollector(subject1Scope, f1)
        val subject2 = UpdateCollector(subject2Scope, f2)
        subject1.awaitValue("x")
        subject2.awaitValue("y")
        subject1Scope.cancel()
        subject2Scope.cancel()
        assertNoObserversAreLeft()
        val subject3 = UpdateCollector(testScope, f1)
        subject3.awaitValue("x")
        f1.modify("x2")
        subject3.awaitValue("x2")
    }

    @Test
    fun stressTest() = runBlocking {
        // create many folders and many files, ensure observers get the latest value and
        // we also cleanup properly
        val folders = (0 until 10).map {
            tmpFolder.newFolder()
        }
        val files = folders.flatMap { folder ->
            (0 until 10).map {
                folder.resolve("f$it").also {
                    it.writeText("init")
                }
            }
        }
        val updateCollectors = files.map { file ->
            UpdateCollector(testScope, file)
        }
        files.mapIndexed { fileIndex, file ->
            async(Dispatchers.IO) {
                repeat(10) { subIndex ->
                    file.modify("$fileIndex/$subIndex")
                    delay(100)
                }
            }
        }.awaitAll()
        updateCollectors.forEachIndexed { fileIndex, updateCollector ->
            updateCollector.awaitValue("$fileIndex/9")
        }
    }

    @Test
    fun blockedObserverDoesntBlockTheOther() = runBlocking {
        val folder = tmpFolder.newFolder()
        folder.mkdirs()
        val f1 = folder.resolve("f1").also {
            it.writeText("x")
        }
        val subject = UpdateCollector(testScope, f1)
        subject.awaitValue("x")
        val blockedObserverReceivedValue = CompletableDeferred<Unit>()
        val blockedObserver = testScope.async {
            MulticastFileObserver.observe(f1).onEach {
                blockedObserverReceivedValue.complete(Unit)
                // suspend indefinitely
                suspendCancellableCoroutine { }
            }.collect()
        }
        withTimeout(5.seconds) {
            blockedObserverReceivedValue.await()
        }
        repeat(10) {
            f1.modify("x$it")
        }
        subject.awaitValue("x9")
        blockedObserver.cancelAndJoin()
    }

    private fun File.modify(value: String) {
        val tmp = this.parentFile!!.resolve("${this.name}.tmp")
        tmp.writeText(value)
        check(tmp.renameTo(this))
        tmp.delete()
    }

    private class UpdateCollector(
        scope: CoroutineScope,
        private val file: File
    ) {
        private val state = MulticastFileObserver.observe(file).map {
            file.readText()
        }.stateIn(scope, SharingStarted.Eagerly, "no-value")

        suspend fun awaitValue(expected: String) {
            try {
                withTimeout(5.seconds) {
                    state.takeWhile {
                        it != expected
                    }.collect()
                }
            } catch (timeout: TimeoutCancellationException) {
                throw AssertionError("""
                    [${file.name}] expected "$expected" but value is "${state.value}"
                """.trimIndent())
            }
        }
    }
}
