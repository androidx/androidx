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

package androidx.xr.testutils

import androidx.xr.runtime.math.Vector3
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.model.Statement

@RunWith(JUnit4::class)
class FakeHeadTrackingRuleTest {

    @Test
    fun checkFakeHeadTrackingStatus_runtimeEnabled_returnsEnabledWithoutReadingPersist() {
        val commands = mutableListOf<String>()
        val status =
            checkFakeHeadTrackingStatus(
                shellExecutor =
                    fakePropExecutor(
                        PROP_FAKE_HEAD_TRACKING to "true",
                        PROP_PERSIST_FAKE_HEAD_TRACKING to "false",
                        commands = commands,
                    )
            )

        assertTrue(status.isEnabled)
        // Only cpm.fake_headtracking matters; xr.pose_read_mode does not affect fake poses.
        assertEquals(listOf("getprop $PROP_FAKE_HEAD_TRACKING"), commands)
    }

    @Test
    fun checkFakeHeadTrackingStatus_runtimeUnsetAndPersistNumericOne_returnsEnabled() {
        val status =
            checkFakeHeadTrackingStatus(
                shellExecutor = fakePropExecutor(PROP_PERSIST_FAKE_HEAD_TRACKING to "1")
            )

        assertTrue(status.isEnabled)
    }

    @Test
    fun checkFakeHeadTrackingStatus_disabledOrEmptyOutput_returnsDisabled() {
        val disabledStatus =
            checkFakeHeadTrackingStatus(
                shellExecutor = fakePropExecutor(PROP_FAKE_HEAD_TRACKING to "false")
            )
        assertFalse(disabledStatus.isEnabled)
        assertTrue(disabledStatus.diagnosticMessage.contains(PROP_FAKE_HEAD_TRACKING))
        assertTrue(disabledStatus.diagnosticMessage.contains("xr-device-tests.gradle"))

        val emptyStatus = checkFakeHeadTrackingStatus(shellExecutor = { "" })
        assertFalse(emptyStatus.isEnabled)
    }

    @Test
    fun checkFakeHeadTrackingStatus_multilineOutputWithLeadingWarning_parsesPropertyValue() {
        val status =
            checkFakeHeadTrackingStatus(
                shellExecutor = { cmd ->
                    if (cmd == "getprop $PROP_FAKE_HEAD_TRACKING") {
                        "WARNING: linker: some message\nTRUE\n"
                    } else {
                        ""
                    }
                }
            )

        assertTrue(status.isEnabled)
    }

    @Test
    fun fakeHeadTrackingRule_whenDisabled_throwsAssumptionViolatedAndSkipsBody() {
        val commands = mutableListOf<String>()
        var bodyRan = false
        val rule =
            FakeHeadTrackingRule(
                settleDelayMs = 25L,
                shellExecutor = { cmd ->
                    commands.add(cmd)
                    if (cmd == "getprop $PROP_FAKE_HEAD_TRACKING") "false" else ""
                },
            )

        val statement = rule.apply(statementOf { bodyRan = true }, TEST_DESCRIPTION)

        assertFailsWith<AssumptionViolatedException> { statement.evaluate() }
        assertFalse(bodyRan)
        assertEquals(listOf("getprop cpm.fake_headtracking"), commands)
    }

    @Test
    fun fakeHeadTrackingRule_whenEnabled_resetsPoseBeforeAndAfterTest() {
        val events = mutableListOf<String>()
        val sleeps = mutableListOf<Long>()
        val rule =
            FakeHeadTrackingRule(
                settleDelayMs = 25L,
                shellExecutor = enabledDeviceExecutor(events),
                sleeper = { ms -> sleeps.add(ms) },
            )

        rule.apply(statementOf { events.add("testBody") }, TEST_DESCRIPTION).evaluate()

        assertEquals(
            listOf(
                RESET_POSITION,
                RESET_ORIENTATION,
                "testBody",
                RESET_POSITION,
                RESET_ORIENTATION,
            ),
            events,
        )
        assertEquals(listOf(25L, 25L), sleeps)
    }

    @Test
    fun performHeadMovement_insideGuardedTest_drivesHeadPoseAndSettles() {
        val events = mutableListOf<String>()
        val sleeps = mutableListOf<Long>()
        val rule =
            FakeHeadTrackingRule(
                settleDelayMs = 10L,
                shellExecutor = enabledDeviceExecutor(events),
                sleeper = { ms -> sleeps.add(ms) },
            )

        rule
            .apply(
                statementOf {
                    events.add("testBody")
                    rule.performHeadMovement { moveHeadTo(Vector3(0.5f, 0f, 0f)) }
                    rule.resetHeadMovement()
                },
                TEST_DESCRIPTION,
            )
            .evaluate()

        assertEquals(
            listOf(
                RESET_POSITION,
                RESET_ORIENTATION,
                "testBody",
                "getprop cpm.fake_het_position",
                "getprop cpm.fake_het_orientation",
                "setprop cpm.fake_het_position 0.50000,0.00000,0.00000",
                "setprop cpm.fake_het_orientation 0.00000,0.00000,0.00000,1.00000",
                RESET_POSITION,
                RESET_ORIENTATION,
                RESET_POSITION,
                RESET_ORIENTATION,
            ),
            events,
        )
        // Rule reset, moveHeadTo settle, resetHeadMovement, and the final rule reset.
        assertEquals(listOf(10L, 10L, 10L, 10L), sleeps)
    }

    @Test
    fun performHeadMovementAndReset_outsideGuardedTest_throwIllegalStateException() {
        val rule =
            FakeHeadTrackingRule(
                settleDelayMs = 0L,
                shellExecutor = { cmd -> throw AssertionError("Unexpected command: $cmd") },
            )

        val performFailure = assertFailsWith<IllegalStateException> { rule.performHeadMovement {} }
        assertTrue(performFailure.message!!.contains("@get:Rule"), "${performFailure.message}")
        assertFailsWith<IllegalStateException> { rule.resetHeadMovement() }

        // The rule is no longer active once a guarded test has finished, even if it failed.
        val enabledRule =
            FakeHeadTrackingRule(
                settleDelayMs = 0L,
                shellExecutor = enabledDeviceExecutor(mutableListOf()),
            )
        assertFailsWith<IllegalStateException> {
            enabledRule
                .apply(statementOf { throw IllegalStateException("failure") }, TEST_DESCRIPTION)
                .evaluate()
        }
        assertFailsWith<IllegalStateException> { enabledRule.performHeadMovement {} }
    }

    @Test
    fun fakeHeadTrackingRule_whenTestBodyAndCleanupThrow_propagatesOriginalException() {
        val events = mutableListOf<String>()
        val expectedFailure = IllegalStateException("Test assertion failed")
        val executor = enabledDeviceExecutor(events)
        val rule =
            FakeHeadTrackingRule(
                settleDelayMs = 0L,
                shellExecutor = { cmd ->
                    executor(cmd).also {
                        if (events.lastOrNull() == RESET_POSITION && "testBody" in events) {
                            throw RuntimeException("Shell error during cleanup")
                        }
                    }
                },
            )

        val statement =
            rule.apply(
                statementOf {
                    events.add("testBody")
                    throw expectedFailure
                },
                TEST_DESCRIPTION,
            )

        val thrown = assertFailsWith<IllegalStateException> { statement.evaluate() }
        assertSame(expectedFailure, thrown)
        assertEquals("Shell error during cleanup", thrown.suppressed.single().message)
        assertEquals(listOf(RESET_POSITION, RESET_ORIENTATION, "testBody", RESET_POSITION), events)
    }

    @Test
    fun fakeHeadTrackingRule_whenInitialResetCannotWrite_failsWithSetupHintAndSkipsBody() {
        var bodyRan = false
        val setpropFailure = IllegalStateException("Failed to set property")
        val executor = enabledDeviceExecutor(mutableListOf())
        val rule =
            FakeHeadTrackingRule(
                settleDelayMs = 0L,
                shellExecutor = { cmd ->
                    if (cmd.startsWith("setprop ")) throw setpropFailure else executor(cmd)
                },
            )

        val statement = rule.apply(statementOf { bodyRan = true }, TEST_DESCRIPTION)

        val thrown = assertFailsWith<IllegalStateException> { statement.evaluate() }
        assertTrue(thrown.message!!.contains("root"))
        assertTrue(thrown.message!!.contains("xr-device-tests.gradle"))
        assertSame(setpropFailure, thrown.cause)
        assertFalse(bodyRan)
    }

    @Test
    fun fakeHeadTrackingRule_whenTestPassesButCleanupFails_propagatesCleanupFailure() {
        val events = mutableListOf<String>()
        val cleanupFailure = IllegalStateException("Shell error during cleanup")
        val executor = enabledDeviceExecutor(events)
        val rule =
            FakeHeadTrackingRule(
                settleDelayMs = 0L,
                shellExecutor = { cmd ->
                    if ("testBody" in events && cmd.startsWith("setprop ")) throw cleanupFailure
                    executor(cmd)
                },
            )

        val statement = rule.apply(statementOf { events.add("testBody") }, TEST_DESCRIPTION)

        assertSame(cleanupFailure, assertFailsWith<IllegalStateException> { statement.evaluate() })
    }

    @Test
    fun fakeHeadTrackingRule_negativeSettleDelay_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { FakeHeadTrackingRule(settleDelayMs = -1L) }
    }

    private fun enabledDeviceExecutor(events: MutableList<String>): (String) -> String = { cmd ->
        when (cmd) {
            "getprop $PROP_FAKE_HEAD_TRACKING" -> "true"
            else -> {
                events.add(cmd)
                ""
            }
        }
    }

    private fun fakePropExecutor(
        vararg entries: Pair<String, String>,
        commands: MutableList<String> = mutableListOf(),
    ): (String) -> String {
        val props = mapOf(*entries)
        return { cmd ->
            commands.add(cmd)
            assertTrue(cmd.startsWith("getprop "), "Unexpected command: $cmd")
            props[cmd.removePrefix("getprop ").trim()].orEmpty()
        }
    }

    private fun statementOf(body: () -> Unit): Statement =
        object : Statement() {
            override fun evaluate() = body()
        }

    private companion object {
        val TEST_DESCRIPTION: Description =
            Description.createTestDescription(FakeHeadTrackingRuleTest::class.java, "sampleTest")
        const val RESET_POSITION = "setprop cpm.fake_het_position 0.00000,0.00000,0.00000"
        const val RESET_ORIENTATION =
            "setprop cpm.fake_het_orientation 0.00000,0.00000,0.00000,1.00000"
    }
}
