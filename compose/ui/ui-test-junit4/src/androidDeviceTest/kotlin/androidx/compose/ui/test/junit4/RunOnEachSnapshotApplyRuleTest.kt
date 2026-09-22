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

package androidx.compose.ui.test.junit4

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@MediumTest
@RunWith(AndroidJUnit4::class)
class RunOnEachSnapshotApplyRuleTest {

    @get:Rule(order = 0) val expectedFailure = ExpectedFailureRule()

    @get:Rule(order = 1) val rule = createComposeRule()

    @Test
    fun runOnEachSnapshotApply_doesNotThrow_whenStateStaysValidUntilDisposed() {
        var count by mutableIntStateOf(0)
        rule.setContent {
            Column {
                Button(onClick = { count++ }, Modifier.testTag(BUTTON_TAG)) {}
                Text("Count: $count", Modifier.testTag("countText"))
            }
        }

        val handle = rule.runOnEachSnapshotApply {
            assertTrue("Count stays within [0, 3]", count in 0..3)
        }
        try {
            rule.onNodeWithTag(BUTTON_TAG).performClick()
            rule.onNodeWithTag(BUTTON_TAG).performClick()
            rule.onNodeWithTag(BUTTON_TAG).performClick()
        } finally {
            handle.dispose()
        }

        rule.onNodeWithTag(BUTTON_TAG).performClick()
        rule.onNodeWithTag("countText").assertTextEquals("Count: 4")
    }

    @Test
    fun runOnEachSnapshotApply_throwsAtNextSync_whenTestBreaksBlock() {
        var registerLine = 0
        var writeLine = 0
        var reachedEnd = false
        expectedFailure.expect { error ->
            assertThat(reachedEnd).isFalse()
            assertThat(error).hasMessageThat().isEqualTo("expected:<0> but was:<1>")
            // The assertion that failed, then the test statement that wrote the state
            assertThat(error.linesInThisFile().take(2))
                .containsExactly(registerLine + 1, writeLine)
                .inOrder()
            assertThat(error.stateChanges().single())
                .hasMessageThat()
                .endsWith(" was written while the test was at ($CURRENT_FILE_NAME:$writeLine)")
        }
        var count by mutableIntStateOf(0)
        rule.setContent { Text("Count: $count") }
        registerLine = nextLine()
        rule.runOnEachSnapshotApply {
            assertEquals(0, count)
        }

        writeLine = nextLine()
        count = 1
        rule.waitForIdle()
        reachedEnd = true
    }

    @Test
    fun runOnEachSnapshotApply_showsTestStatement_whenClickBreaksBlock() {
        var registerLine = 0
        var clickLine = 0
        var writeLine = 0
        expectedFailure.expect { error ->
            assertThat(error).hasMessageThat().isEqualTo("expected:<0> but was:<1>")
            // The assertion that failed, then the click
            assertThat(error.linesInThisFile().take(2))
                .containsExactly(registerLine + 1, clickLine)
                .inOrder()
            // The click listener wrote the state while the test was at the click
            val change = error.stateChanges().single()
            assertThat(change)
                .hasMessageThat()
                .endsWith(" was written while the test was at ($CURRENT_FILE_NAME:$clickLine)")
            assertThat(change.linesInThisFile()).contains(writeLine)
        }
        var count by mutableIntStateOf(0)
        rule.setContent {
            Button(
                onClick = {
                    writeLine = nextLine()
                    count = 1
                },
                modifier = Modifier.testTag(BUTTON_TAG),
            ) {}
        }
        registerLine = nextLine()
        rule.runOnEachSnapshotApply {
            assertEquals(0, count)
        }

        clickLine = nextLine()
        rule.onNodeWithTag(BUTTON_TAG).performClick()
        rule.waitForIdle()
    }

    @Test
    fun runOnEachSnapshotApply_throwsAtEndOfTest_whenLastWriteBreaksBlock() {
        expectedFailure.expect { error ->
            assertThat(error).hasMessageThat().isEqualTo("expected:<0> but was:<1>")
        }
        var count by mutableIntStateOf(0)
        rule.setContent { Text("Count: $count") }
        rule.runOnEachSnapshotApply { assertEquals(0, count) }
        // The test finishes right after this write, without synchronizing
        count = 1
    }

    @Test
    fun runOnEachSnapshotApply_stopsBlock_whenHandleIsDisposed() {
        var count by mutableIntStateOf(0)
        rule.setContent { Text("Count: $count") }
        val handle = rule.runOnEachSnapshotApply { assertEquals(0, count) }
        // Breaks the block, then fixes it, so it can't fail again after it throws below
        Snapshot.withMutableSnapshot { count = 1 }
        Snapshot.withMutableSnapshot { count = 0 }
        assertThat(assertThrows(AssertionError::class.java) { rule.waitForIdle() })
            .hasMessageThat()
            .isEqualTo("expected:<0> but was:<1>")

        handle.dispose()
        // The block doesn't run anymore
        count = 1
        rule.waitForIdle()
    }

    @Test
    fun runOnEachSnapshotApply_doesNotThrow_whenStateChangesWhileRuleClosesActivity() {
        var isShown by mutableStateOf(false)

        // The rule closes the Activity after the test body, which disposes the content and sets
        // isShown to false. The block must have stopped by then.
        rule.setContent {
            DisposableEffect(Unit) {
                isShown = true
                onDispose { isShown = false }
            }
        }
        rule.runOnEachSnapshotApply { assertTrue("isShown must stay true", isShown) }
        rule.waitForIdle()
    }

    @Test
    fun runOnEachSnapshotApply_throwsOnlyTestError_whenTestFailsAndRuleClosesActivity() {
        var isShown by mutableStateOf(false)

        // The block must also have stopped when the rule closes the Activity after a failed test,
        // so the state changes that closing it makes don't add errors to the test's error
        expectedFailure.expect { error ->
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Test failed")
            assertThat(error.suppressedExceptions.map { it.message })
                .doesNotContain("isShown must stay true")
        }
        rule.setContent {
            DisposableEffect(Unit) {
                isShown = true
                onDispose { isShown = false }
            }
        }
        rule.runOnEachSnapshotApply { assertTrue("isShown must stay true", isShown) }
        rule.waitForIdle()
        throw IllegalStateException("Test failed")
    }

    /** Returns the number of the line after the one that calls this. */
    private fun nextLine(): Int =
        // Skips this function and its synthetic accessors
        Throwable().stackTrace.first { !it.methodName.contains("nextLine") }.lineNumber + 1

    /** Returns the lines of this file that this throwable's stack trace shows, in order. */
    private fun Throwable.linesInThisFile(): List<Int> =
        stackTrace
            .filter {
                it.className.startsWith(RunOnEachSnapshotApplyRuleTest::class.java.name) &&
                    it.lineNumber > 0
            }
            .map { it.lineNumber }

    /** Returns the state changes that explain this error, which a block threw. */
    private fun Throwable.stateChanges(): List<Throwable> = suppressedExceptions.filter {
        it.javaClass.simpleName == "StateChange"
    }

    private companion object {
        const val CURRENT_FILE_NAME = "RunOnEachSnapshotApplyRuleTest.kt"
        const val BUTTON_TAG = "button"
    }
}

/**
 * Lets a test expect to fail, and checks what it threw once the rules inside this one finished it,
 * e.g. after the compose rule checked and stopped the runOnEachSnapshotApply blocks. A test that
 * doesn't call [expect] must pass as usual.
 */
class ExpectedFailureRule : TestRule {
    private var assertion: ((Throwable) -> Unit)? = null

    fun expect(assertion: (Throwable) -> Unit) {
        this.assertion = assertion
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val failure =
                    try {
                        base.evaluate()
                        null
                    } catch (t: Throwable) {
                        t
                    }
                val assertion = assertion
                when {
                    assertion != null ->
                        assertion(failure ?: throw AssertionError("Expected the test to fail"))
                    failure != null -> throw failure
                }
            }
        }
}
