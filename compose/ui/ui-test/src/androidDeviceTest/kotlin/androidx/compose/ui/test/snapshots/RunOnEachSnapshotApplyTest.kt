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

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunOnEachSnapshotApplyTest {

    @Test
    fun runOnEachSnapshotApply_doesNotThrow_whenStateStaysValidUntilDisposed() = runComposeUiTest {
        var count by mutableIntStateOf(0)
        setContent {
            Column {
                TestButton(onClick = { count++ })
                Text("Count: $count", Modifier.testTag("countText"))
            }
        }

        val handle = runOnEachSnapshotApply {
            assertTrue("Count stays within [0, 3]", count in 0..3)
        }
        try {
            onNodeWithTag(BUTTON_TAG).performClick()
            onNodeWithTag(BUTTON_TAG).performClick()
            onNodeWithTag(BUTTON_TAG).performClick()
        } finally {
            handle.dispose()
        }

        onNodeWithTag(BUTTON_TAG).performClick()
        onNodeWithTag("countText").assertTextEquals("Count: 4")
    }

    @Test
    fun runOnEachSnapshotApply_throwsAtNextSync_whenWriteIsPendingAtDispose() = runComposeUiTest {
        var count by mutableIntStateOf(0)
        setContent { Text("Count: $count") }
        val handle = runOnEachSnapshotApply { assertEquals(0, count) }
        // On the main thread, so only dispose() can send the apply notification
        runOnUiThread {
            count = 1
            handle.dispose()
        }

        // The block failed before it was disposed, so the next synchronization still throws
        val error = assertFailsWith<AssertionError> { waitForIdle() }
        assertThat(error).hasMessageThat().isEqualTo("expected:<0> but was:<1>")
    }

    @Test
    fun runOnEachSnapshotApply_throwsAtEndOfTest_whenLastWriteBreaksBlock() {
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    setContent { Text("Count: $count") }
                    runOnEachSnapshotApply { assertEquals(0, count) }
                    // The test finishes right after this write, without synchronizing
                    count = 1
                }
            }

        assertThat(error).hasMessageThat().isEqualTo("expected:<0> but was:<1>")
    }

    @Test
    fun runOnEachSnapshotApply_keepsRunningAndTracingOtherBlock_whenOneIsDisposed() =
        runComposeUiTest {
            var count by mutableIntStateOf(0)
            setContent { Text("Count: $count") }
            val handle = runOnEachSnapshotApply { assertTrue("count must not be 1", count != 1) }
            val registerLine = nextLine()
            runOnEachSnapshotApply { assertTrue("count must not be 2", count != 2) }

            // Each snapshot that breaks a block is followed by one that fixes it, so that other
            // snapshots, e.g. when the window gets focus, can't make the block fail again after
            // it throws
            Snapshot.withMutableSnapshot { count = 2 }
            Snapshot.withMutableSnapshot { count = 0 }
            assertThat(assertFailsWith<AssertionError> { waitForIdle() })
                .hasMessageThat()
                .isEqualTo("count must not be 2")
            Snapshot.withMutableSnapshot { count = 1 }
            Snapshot.withMutableSnapshot { count = 0 }
            assertThat(assertFailsWith<AssertionError> { waitForIdle() })
                .hasMessageThat()
                .isEqualTo("count must not be 1")

            // Disposing one handle, also more than once, keeps the other block running
            handle.dispose()
            handle.dispose()
            val writeLine = nextLine()
            count = 2
            // Checks this write now, then fixes the block for the same reason
            runOnUiThread { Snapshot.sendApplyNotifications() }
            count = 0
            assertFailsWith<AssertionError> { waitForIdle() }
                .assertShowsTestStatement(
                    assertionLine = registerLine,
                    statementLine = writeLine,
                    writeLine = writeLine,
                )

            // The disposed block doesn't run anymore
            count = 1
            waitForIdle()
        }

    @Test
    fun runOnEachSnapshotApply_doesNotReportReads_toReadObserverOfApplyingThread() =
        runComposeUiTest {
            var count by mutableIntStateOf(0)
            runOnEachSnapshotApply { assertTrue(count >= 0) }
            val observedReads = mutableListOf<Any>()
            // Applies a snapshot while observing reads, like a layout pass can
            Snapshot.observe(readObserver = { observedReads += it }) {
                Snapshot.withMutableSnapshot { count = 1 }
            }

            // The block read count in its own snapshot
            assertThat(observedReads).isEmpty()
        }

    @Test
    fun runOnEachSnapshotApply_throwsAtWaitUntil_whenConditionIsNeverMet() {
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    runOnEachSnapshotApply { assertEquals(0, count) }
                    Snapshot.withMutableSnapshot { count = 1 }
                    // Throws the block's error instead of timing out
                    waitUntil(timeoutMillis = 1_000) { false }
                }
            }

        assertThat(error).hasMessageThat().isEqualTo("expected:<0> but was:<1>")
    }

    @Test
    fun runOnEachSnapshotApply_throwsOnlyFirstError_whenAssertionFailsRepeatedly() {
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    runOnEachSnapshotApply { assertEquals(0, count) }
                    Snapshot.withMutableSnapshot { count = 1 }
                    Snapshot.withMutableSnapshot { count = 2 }
                }
            }

        assertThat(error).hasMessageThat().isEqualTo("expected:<0> but was:<1>")
        assertThat(error.suppressedExceptions.map { it.message })
            .doesNotContain("expected:<0> but was:<2>")
    }

    @Test
    fun runOnEachSnapshotApply_throws_whenAssertionWritesState() {
        assertFailsWith<IllegalStateException> {
            runComposeUiTest {
                var count by mutableIntStateOf(0)
                var other by mutableIntStateOf(0)
                runOnEachSnapshotApply { other = 42 }
                Snapshot.withMutableSnapshot { count++ }
            }
        }
    }

    @Test
    fun runOnEachSnapshotApply_throws_whenAssertionCallsWaitForIdleOnMainThread() {
        val error =
            assertFailsWith<IllegalStateException> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    runOnEachSnapshotApply { waitForIdle() }
                    runOnUiThread { Snapshot.withMutableSnapshot { count++ } }
                }
            }

        assertThat(error).hasMessageThat().startsWith("A runOnEachSnapshotApply block can't")
    }

    @Test
    fun runOnEachSnapshotApply_throws_whenAssertionLooksUpNode() {
        val error =
            assertFailsWith<IllegalStateException> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    setContent { Text("Count: $count", Modifier.testTag("countText")) }
                    runOnEachSnapshotApply {
                        // Skip the implicit wait, so only the node lookup check can catch this
                        runWithoutImplicitWait { onNodeWithTag("countText").assertExists() }
                    }
                    Snapshot.withMutableSnapshot { count++ }
                }
            }

        assertThat(error).hasMessageThat().startsWith("A runOnEachSnapshotApply block can't")
    }

    @Test
    fun runOnEachSnapshotApply_showsTestStatement_whenTestBodyWriteBreaksBlock() {
        var registerLine = 0
        var writeLine = 0
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    setContent { Text("Count: $count") }
                    registerLine = nextLine()
                    runOnEachSnapshotApply {
                        assertEquals(0, count)
                    }

                    writeLine = nextLine()
                    count = 1
                    waitForIdle()
                }
            }

        // The test wrote the state itself, so the statement is the write
        val change =
            error.assertShowsTestStatement(
                assertionLine = registerLine + 1,
                statementLine = writeLine,
                writeLine = writeLine,
            )
        assertThat(change).hasMessageThat().startsWith("MutableIntState(value=1)")
    }

    @Test
    fun runOnEachSnapshotApply_showsTestStatementAndLastWrite_whenClickBreaksBlock() {
        var registerLine = 0
        var clickLine = 0
        var firstWriteLine = 0
        var lastWriteLine = 0
        var reachedEnd = false
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    setContent {
                        TestButton(
                            onClick = {
                                firstWriteLine = nextLine()
                                count = 5
                                lastWriteLine = nextLine()
                                count = 6
                            }
                        )
                    }
                    registerLine = nextLine()
                    runOnEachSnapshotApply {
                        assertEquals(0, count)
                    }

                    clickLine = nextLine()
                    onNodeWithTag(BUTTON_TAG).performClick()
                    // Synchronizes, so it throws the block's error
                    onNodeWithTag(BUTTON_TAG).assertExists()
                    reachedEnd = true
                }
            }

        assertThat(reachedEnd).isFalse()
        val change =
            error.assertShowsTestStatement(
                assertionLine = registerLine + 1,
                statementLine = clickLine,
                writeLine = lastWriteLine,
            )
        // The value that the block saw, and only the write of that value
        assertThat(change).hasMessageThat().startsWith("MutableIntState(value=6)")
        assertThat(change.linesInThisFile()).doesNotContain(firstWriteLine)
    }

    @Test
    fun runOnEachSnapshotApply_showsTestStatement_whenWriteInSnapshotBreaksBlock() {
        var registerLine = 0
        var clickLine = 0
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    setContent {
                        TestButton(onClick = { Snapshot.withMutableSnapshot { count++ } })
                    }
                    registerLine = nextLine()
                    runOnEachSnapshotApply {
                        assertEquals(0, count)
                    }

                    clickLine = nextLine()
                    onNodeWithTag(BUTTON_TAG).performClick()
                    waitForIdle()
                }
            }

        // The snapshot was applied while the test was in the click
        assertThat(error.linesInThisFile().take(2))
            .containsExactly(registerLine + 1, clickLine)
            .inOrder()
        // Writes in nested snapshots aren't traced
        assertThat(error.stateChanges()).isEmpty()
    }

    @Test
    fun runOnEachSnapshotApply_showsTestStatement_whenSnapshotChangesStateWrittenBefore() {
        var registerLine = 0
        var snapshotLine = 0
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    registerLine = nextLine()
                    runOnEachSnapshotApply {
                        assertTrue("count must stay below 2", count < 2)
                    }

                    // On the main thread, so the block checks and accepts this write before
                    // runOnUiThread returns
                    runOnUiThread {
                        count = 1
                        Snapshot.sendApplyNotifications()
                    }
                    snapshotLine = nextLine()
                    runOnUiThread { Snapshot.withMutableSnapshot { count = 2 } }
                }
            }

        // The earlier write was already applied, so it doesn't explain the change that broke the
        // block: the error shows where the test was when the snapshot was applied, and no write
        assertThat(error.linesInThisFile().take(2))
            .containsExactly(registerLine + 1, snapshotLine)
            .inOrder()
        assertThat(error.stateChanges()).isEmpty()
    }

    @Test
    fun runOnEachSnapshotApply_showsWhereTestWas_whenCoroutineBreaksBlock() {
        var registerLine = 0
        var waitLine = 0
        var writeLine = 0
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    var count by mutableIntStateOf(0)
                    setContent {
                        val scope = rememberCoroutineScope()
                        TestButton(
                            onClick = {
                                scope.launch {
                                    delay(100.milliseconds)
                                    writeLine = nextLine()
                                    count++
                                }
                            }
                        )
                    }
                    registerLine = nextLine()
                    runOnEachSnapshotApply {
                        assertEquals(0, count)
                    }

                    onNodeWithTag(BUTTON_TAG).performClick()
                    waitLine = nextLine()
                    waitForIdle()
                }
            }

        // The click only launched the coroutine, so the statement is where the test was
        error.assertShowsTestStatement(
            assertionLine = registerLine + 1,
            statementLine = waitLine,
            writeLine = writeLine,
        )
    }

    @Test
    fun runOnEachSnapshotApply_showsRegistration_whenStateIsInvalidWhenRegistered() {
        var registerLine = 0
        val error =
            assertFailsWith<AssertionError> {
                runComposeUiTest {
                    val count by mutableIntStateOf(0)
                    setContent { TestButton(onClick = {}) }
                    registerLine = nextLine()
                    runOnEachSnapshotApply {
                        assertTrue("count must be positive", count > 0)
                    }

                    // Changes other state, e.g. the button's pressed state, which runs the block
                    onNodeWithTag(BUTTON_TAG).performClick()
                    waitForIdle()
                }
            }

        // The failed assertion, then where the block was registered, as the state didn't change
        assertThat(error.linesInThisFile().take(2))
            .containsExactly(registerLine + 1, registerLine)
            .inOrder()
        assertThat(error.stateChanges()).isEmpty()
    }

    /**
     * Asserts that this error, which a block threw, shows the failed assertion at [assertionLine]
     * and then the test statement at [statementLine], and that one [StateChange] explains it: the
     * write at [writeLine], made while the test was at [statementLine].
     *
     * @return the [StateChange]
     */
    private fun Throwable.assertShowsTestStatement(
        assertionLine: Int,
        statementLine: Int,
        writeLine: Int,
    ): StateChange {
        assertThat(linesInThisFile().take(2))
            .containsExactly(assertionLine, statementLine)
            .inOrder()
        val changes = stateChanges()
        assertThat(changes).hasSize(1)
        assertThat(changes[0])
            .hasMessageThat()
            .endsWith(" was written while the test was at ($CURRENT_FILE_NAME:$statementLine)")
        assertThat(changes[0].linesInThisFile()).contains(writeLine)
        return changes[0]
    }

    /** Returns the number of the line after the one that calls this. */
    private fun nextLine(): Int =
        // Skips this function and its synthetic accessors
        Throwable().stackTrace.first { !it.methodName.contains("nextLine") }.lineNumber + 1

    /** Returns the [StateChange]s that explain this error. */
    private fun Throwable.stateChanges(): List<StateChange> =
        suppressedExceptions.filterIsInstance<StateChange>()

    /** Returns the lines of this file that this throwable's stack trace shows, in order. */
    private fun Throwable.linesInThisFile(): List<Int> =
        stackTrace
            .filter {
                it.className.startsWith(RunOnEachSnapshotApplyTest::class.java.name) &&
                    it.lineNumber > 0
            }
            .map { it.lineNumber }

    /** A button that runs [onClick] when the node tagged [BUTTON_TAG] is clicked. */
    @Composable
    private fun TestButton(onClick: () -> Unit) {
        Button(onClick = onClick, modifier = Modifier.testTag(BUTTON_TAG)) { Text("Button") }
    }

    private companion object {
        const val CURRENT_FILE_NAME = "RunOnEachSnapshotApplyTest.kt"
        const val BUTTON_TAG = "button"
    }
}
