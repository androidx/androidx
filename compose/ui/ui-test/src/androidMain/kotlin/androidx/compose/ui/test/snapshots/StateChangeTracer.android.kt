/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.snapshots

import androidx.compose.ui.test.platform.makeSynchronizedObject
import androidx.compose.ui.util.fastForEach
import java.util.IdentityHashMap

/**
 * Records, for each traced state object, its last write: the stack trace of the code that wrote it
 * and, the stack trace of the test thread at that moment.
 *
 * To explain a failure, it finds the test's code in these stack traces with [TestCode] and puts the
 * test statements right after the frames of the block that failed.
 */
internal actual class StateChangeTracer {
    private val testThread = Thread.currentThread()
    private val lock = makeSynchronizedObject()
    private val writes = IdentityHashMap<Any, StateWrite>()

    actual fun recordWrite(state: Any) {
        val write = StateWrite(testThread)
        synchronized(lock) { writes[state] = write }
    }

    actual fun retainWrites(states: Set<Any>) {
        synchronized(lock) { writes.keys.retainAll(states) }
    }

    actual fun forgetWrites(states: Set<Any>) {
        synchronized(lock) { writes.keys.removeAll(states) }
    }

    actual fun explain(failure: Throwable, changed: Map<Any, String>, registeredAt: Throwable) {
        val frames = failure.stackTrace
        // The block's frames are above the frame of the observer that ran it
        val observerIndex = frames.indexOfFirst { it.isIn(StateIntegrityObserver::class.java) }
        if (observerIndex <= 0) return
        val test = TestCode(blockFrame = frames[observerIndex - 1], registration = registeredAt)
        val testStackTraceNow by lazy { testThread.stackTraceNow() }
        // The test thread's stack traces each time the state that the block read changed, oldest
        // first. If none changed, where the test registered the block, and where it is now.
        val testStackTraces = mutableListOf<Array<StackTraceElement>>()
        if (changed.isEmpty()) {
            testStackTraces += registeredAt.stackTrace
            testStackTraces += testStackTraceNow
        } else {
            val changes =
                synchronized(lock) {
                    changed.mapTo(mutableListOf()) { (state, description) ->
                        writes[state] to description
                    }
                }
            changes.sortBy { (write, _) -> write?.time ?: Long.MAX_VALUE }
            changes.fastForEach { (write, description) ->
                if (write == null) {
                    // Not recorded, e.g. if the block didn't read the state when it was written
                    testStackTraces += testStackTraceNow
                } else {
                    testStackTraces += write.testStackTrace
                    failure.addSuppressed(write.toStateChange(description, test))
                }
            }
        }
        val testFrames = test.statementFrames(testStackTraces)
        if (testFrames.isEmpty()) return
        val blockFrames = frames.copyOfRange(0, observerIndex).dropLastWhile { !it.hasLine() }
        failure.stackTrace = (blockFrames + testFrames).toTypedArray()
    }

    /**
     * Write of a state object: the code that wrote it, and where the test was.
     *
     * Create it on the thread that writes, while it writes.
     *
     * @param testThread the thread that runs the test
     */
    private class StateWrite(testThread: Thread) {
        val time = System.nanoTime()
        private val site = Throwable()
        // If the test thread writes, the stack trace of the site already shows where the test is
        private val sampledTestStackTrace =
            if (Thread.currentThread() === testThread) null else testThread.stackTraceNow()

        /** The stack trace of the test thread when the state was written. */
        val testStackTrace: Array<StackTraceElement>
            get() = sampledTestStackTrace ?: site.stackTrace

        /**
         * Returns a [StateChange] that shows where the state that [description] describes was
         * written, and where the test was. If the test thread wrote it, its stack trace ends at the
         * test statement, as the failure shows the frames below it.
         *
         * @param test finds the test statement in stack traces
         */
        fun toStateChange(description: String, test: TestCode): StateChange {
            val siteFrames = site.stackTrace.dropWhile { it.isTracingFrame() }
            val testStackTrace = testStackTrace
            val statement = testStackTrace.getOrNull(test.indexOfStatement(testStackTrace))
            val whileAt = statement?.let {
                " while the test was at (${it.fileName}:${it.lineNumber})"
            }
            // The site shows the statement only if the test thread wrote the state
            val statementIndex = if (statement == null) -1 else siteFrames.indexOf(statement)
            val end = if (statementIndex >= 0) statementIndex + 1 else siteFrames.size
            return StateChange(
                "$description was written${whileAt.orEmpty()}",
                siteFrames.subList(0, end),
            )
        }
    }
}

/**
 * Finds the code of the test in stack traces of the test thread: the frames of the top-level class
 * that defines the block, and among them the statement that the test runs in the function where it
 * registered the block, e.g. the body of runComposeUiTest.
 *
 * @param blockFrame the frame of the block, i.e. the frame that the observer called
 * @param registration a [Throwable] created where the block was registered
 */
private class TestCode(blockFrame: StackTraceElement, registration: Throwable) {
    // Classes nested in a class, including lambdas, have binary names that start with its name
    private val testClassName = blockFrame.className.substringBefore('$')
    private val registrationFrame = registration.stackTrace.firstOrNull { it.isTestCode() }

    /**
     * Returns the index of the statement that the test runs in [stackTrace]: the frame of the
     * function that registered the block, or else the first frame of the test's code, or -1 if
     * there's none.
     */
    fun indexOfStatement(stackTrace: Array<StackTraceElement>): Int {
        val registrationFrame = registrationFrame
        if (registrationFrame != null) {
            val index = stackTrace.indexOfFirst { it.isInSameFunctionAs(registrationFrame) }
            if (index >= 0) return index
        }
        return stackTrace.indexOfFirst { it.isTestCode() }
    }

    /**
     * Returns the statement that the test runs in each of [stackTraces], then the frames of the
     * test code that called the statement in the last one, without repeats.
     */
    fun statementFrames(stackTraces: List<Array<StackTraceElement>>): List<StackTraceElement> {
        val frames = mutableListOf<StackTraceElement>()
        fun add(frame: StackTraceElement) {
            if (frames.lastOrNull() != frame) frames += frame
        }
        for (i in stackTraces.indices) {
            val stackTrace = stackTraces[i]
            val statement = indexOfStatement(stackTrace)
            if (statement < 0) continue
            if (i < stackTraces.lastIndex) {
                add(stackTrace[statement])
            } else {
                for (j in statement until stackTrace.size) {
                    if (stackTrace[j].isTestCode()) add(stackTrace[j])
                }
            }
        }
        return frames
    }

    private fun StackTraceElement.isTestCode() = hasLine() && isIn(testClassName)
}

/**
 * Shows where a state object that a failing runOnEachSnapshotApply block read was written.
 *
 * @param message which state was written, and where the test was
 * @param stackTrace the stack trace of the code that wrote the state
 */
internal class StateChange(message: String, stackTrace: List<StackTraceElement>) :
    Throwable(message) {
    init {
        this.stackTrace = stackTrace.toTypedArray()
    }
}

/** Returns the stack trace of this thread now, or an empty one if it can't be read. */
private fun Thread.stackTraceNow(): Array<StackTraceElement> =
    if (this === Thread.currentThread()) Throwable().stackTrace
    else runCatching { stackTrace }.getOrDefault(emptyArray())

/** Whether this frame runs code of the class [className] or of a class nested in it. */
private fun StackTraceElement.isIn(className: String) =
    this.className == className || this.className.startsWith("$className$")

/** Whether this frame runs code of [javaClass] or of a class nested in it. */
private fun StackTraceElement.isIn(javaClass: Class<*>) = isIn(javaClass.name)

/** Whether this frame runs the same function as [other]. */
private fun StackTraceElement.isInSameFunctionAs(other: StackTraceElement) =
    className == other.className && methodName == other.methodName

/** Whether this frame has a source line, unlike e.g. a synthetic bridge. */
private fun StackTraceElement.hasLine() = fileName != null && lineNumber > 0

/** Whether this frame runs the code that traces writes. */
private fun StackTraceElement.isTracingFrame() =
    isIn(StateChangeTracer::class.java) || isIn(StateIntegrityChecker::class.java)
