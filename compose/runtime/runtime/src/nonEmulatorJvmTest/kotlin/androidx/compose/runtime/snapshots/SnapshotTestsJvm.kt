/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.internal.AtomicInt
import androidx.compose.runtime.internal.AtomicReference
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.runTest
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SnapshotTestsJvm {

    @Test
    @Suppress("BanThreadSleep", "AutoboxingStateCreation") // Required to reproduce the issue
    fun testMultiThreadedReadingAndWritingOfGlobalScope() {
        val running = AtomicBoolean(true)
        val reads = AtomicInt(0)
        val writes = AtomicInt(0)
        val lowNumberSeen = AtomicInt(0)
        val exception = AtomicReference<Throwable?>(null)
        try {
            val state = mutableStateOf(0)
            Snapshot.notifyObjectsInitialized()

            // Create 20 reader threads of state
            repeat(20) {
                thread {
                    try {
                        while (running.get()) {
                            reads.postIncrement()
                            if (state.value < 1000) lowNumberSeen.postIncrement()
                        }
                    } catch (e: Throwable) {
                        exception.set(e)
                        running.set(false)
                    }
                }
            }

            // Create 10 writer threads
            repeat(10) {
                thread {
                    while (running.get()) {
                        writes.postIncrement()
                        state.value = Random.nextInt(10000)
                        Snapshot.sendApplyNotifications()
                    }
                }
            }

            while (running.get() && writes.get() < 10000) {
                Thread.sleep(0)
            }
        } finally {
            running.set(false)
        }

        exception.get()?.let { throw it }
        assertNull(exception.get())
    }

    @Test
    fun listWriteRace() {
        val iterations = 10000
        val list = SnapshotStateList<Int>().apply { add(0) }
        val max by derivedStateOf { list.max() }
        var exception: Throwable? = null

        Snapshot.notifyObjectsInitialized()
        Snapshot.sendApplyNotifications()

        val mutator =
            thread(name = "mutator") {
                var counter = 0
                while (counter < iterations) {
                    Snapshot.withMutableSnapshot { list[0] = counter++ }
                }
            }

        val reader =
            thread(name = "reader") {
                var counter = 0
                while (exception == null && counter < iterations) {
                    try {
                        // !!! ISE thrown from this derivedStateOf read.
                        @Suppress("UNUSED_EXPRESSION") max
                        counter++
                    } catch (e: Throwable) {
                        exception = e
                    }
                }
            }
        mutator.join()
        reader.join()

        exception?.let { throw it }
    }

    /**
     * This is a regression test against a race condition that caused [writableRecord] to throw
     * unnecessary [IllegalStateException]s. More details about the race condition are available in
     * b/440975176.
     */
    @Test
    fun writableRecordRegressionTest() = runTest {
        repeat(1000) {
            val state = mutableIntStateOf(0)
            val list = mutableStateListOf<Int>()
            coroutineScope {
                Snapshot.notifyObjectsInitialized()
                repeat(100) { index ->
                    launch(Dispatchers.Default) { list += index }
                    launch(Dispatchers.Default) {
                        state.intValue += 1
                        Snapshot.sendApplyNotifications()
                    }
                }
            }
        }
    }

    @Test
    fun writableRecordRegressionTest_forced() = runTest {
        // The println's in this test are load-bearing.
        // With them the test will fail regularly with older version of withCurrent
        // but only sporadically without them.
        val pause = Semaphore(0)
        val resume = Semaphore(0)
        val state = PausingStateObject(0, pause, resume)
        var exception: IllegalStateException? = null
        coroutineScope {
            Snapshot.notifyObjectsInitialized()
            val threadA = thread {
                try {
                    state.intValue = 1
                    state.intValue = 2
                    state.intValue = 3
                } catch (e: IllegalStateException) {
                    exception = e
                }
                resume.release(100)
            }
            val threadB = thread {
                try {
                    state.intValueNoWait = 100
                    Snapshot.notifyObjectsInitialized()
                    Snapshot.notifyObjectsInitialized()
                    Snapshot.notifyObjectsInitialized()
                    pause.release()
                    resume.acquire()
                    println("resume acquire() returned")

                    println("Set 200, 300 - before")
                    state.intValueNoWait = 200
                    Snapshot.notifyObjectsInitialized()
                    state.intValueNoWait = 300
                    println("pause.release()")
                    pause.release()
                    println("pause.release() returned")
                    println("resume.acquire()")
                    resume.acquire()
                    println("resume acquire() returned")

                    println("Set 400 - before")
                    state.intValueNoWait = 400
                    Snapshot.notifyObjectsInitialized()
                    println("pause.release()")
                    pause.release()
                    println("pause.release() returned")
                } catch (e: IllegalStateException) {
                    exception = e
                }
                pause.release(100)
            }

            threadA.join()
            threadB.join()
        }
        exception?.let { throw it }
    }

    private class TestRecord(snapshotId: SnapshotId) : StateRecord(snapshotId) {
        var value: Int = 0

        override fun assign(value: StateRecord) {
            this.value = (value as TestRecord).value
        }

        override fun create(): StateRecord = TestRecord(snapshotId).also { it.value = this.value }
    }

    private class MyStateObject(first: StateRecord) : StateObject {
        private var _first: StateRecord = first
        override val firstStateRecord: StateRecord
            get() = _first

        override fun prependStateRecord(value: StateRecord) {
            value.next = _first
            _first = value
        }

        override fun mergeRecords(
            previous: StateRecord,
            current: StateRecord,
            applied: StateRecord,
        ): StateRecord? = null
    }

    @Test
    fun writableRecordRegressionTestDeterministic() {
        val b = TestRecord(0L) // 0L is INVALID_SNAPSHOT (SnapshotIdZero)
        val a = TestRecord(Snapshot.current.snapshotId)
        a.next = b
        val state = MyStateObject(a)

        // Verify that calling the new `withCurrent(state)` overload succeeds and returns `a`
        // by starting the traversal from `state.firstStateRecord` under the sync fallback.
        val result = b.withCurrent(state) { it }
        kotlin.test.assertEquals(a, result)

        // Verify that calling the old `current(b)` (without state object) still fails for
        // compatibility.
        val exception = kotlin.test.assertFailsWith<IllegalStateException> { current(b) }
        kotlin.test.assertEquals(
            exception.message?.contains("Reading a state that was created after the snapshot"),
            true,
        )
    }
}

private fun AtomicInt.postIncrement(): Int = add(1) - 1

private class PausingStateObject(value: Int, val pause: Semaphore, val resume: Semaphore) :
    StateObject {
    private var next = PausingStateRecord(currentSnapshot().snapshotId, value)

    var intValueNoWait: Int
        get() = next.readable(this).value
        set(value) {
            next.withCurrent(this) {
                if (it.value != value) {
                    next.overwritable(this, it) { this.value = value }
                }
            }
        }

    private val waitingNext: PausingStateRecord
        get() {
            val result = next
            println("pause.acquire()")
            pause.acquire()
            println("pause.acquire() returned")
            println("resume.release()")
            resume.release()
            println("resume.release() returned")
            return result
        }

    var intValue: Int
        get() = next.readable(this).value
        set(value) {
            waitingNext.withCurrent(this) {
                if (it.value != value) {
                    next.overwritable(this, it) { this.value = value }
                }
            }
            println("intValue.set returning")
        }

    override val firstStateRecord: StateRecord
        get() = next

    override fun prependStateRecord(value: StateRecord) {
        next = value as PausingStateRecord
    }

    private class PausingStateRecord(snapshotId: SnapshotId, var value: Int) :
        StateRecord(snapshotId) {
        override fun assign(value: StateRecord) {
            this.value = (value as PausingStateRecord).value
        }

        override fun create(): StateRecord = PausingStateRecord(currentSnapshot().snapshotId, value)
    }
}
