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

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.model.Statement

@RunWith(JUnit4::class)
class XrResourceLeakRuleTest {

    private val testDescription =
        Description.createTestDescription(XrResourceLeakRuleTest::class.java, "sampleTest")

    private fun sceneWithStats(stats: SpatialSceneStats?): SnapshotXrScene =
        SnapshotXrScene(rootNodes = emptyList(), stats = stats)

    @Test
    fun resourceLeakRule_passesAfterGcReclaimsLeakedNodesWithinTimeout() {
        val scenes =
            ArrayDeque(
                listOf(
                    sceneWithStats(SpatialSceneStats(attachedNodes = 40, bufferPixels = 1024L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 44, bufferPixels = 2048L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 40, bufferPixels = 1024L)),
                )
            )
        var gcCalls = 0
        var nowMs = 0L
        val rule =
            XrResourceLeakRule(
                maxAttachedNodesDelta = 0,
                maxBufferPixelsDelta = 0L,
                settleTimeoutMs = 500L,
                pollIntervalMs = 100L,
                sceneProvider = { scenes.removeFirst() },
                gcTrigger = { gcCalls++ },
                sleeper = { nowMs += it },
                clockMs = { nowMs },
            )

        rule
            .apply(
                object : Statement() {
                    override fun evaluate() {}
                },
                testDescription,
            )
            .evaluate()

        assertEquals(1, gcCalls)
        assertTrue(scenes.isEmpty())
    }

    @Test
    fun resourceLeakRule_failsWhenLeakPersistsAfterTimeout() {
        val baseline = SpatialSceneStats(attachedNodes = 35, bufferPixels = 1000L)
        val leaked = SpatialSceneStats(attachedNodes = 38, bufferPixels = 9000L)
        var callCount = 0
        var gcCalls = 0
        var nowMs = 0L
        val rule =
            XrResourceLeakRule(
                maxAttachedNodesDelta = 1,
                maxBufferPixelsDelta = 1024L,
                settleTimeoutMs = 300L,
                pollIntervalMs = 100L,
                sceneProvider = {
                    callCount++
                    if (callCount == 1) sceneWithStats(baseline) else sceneWithStats(leaked)
                },
                gcTrigger = { gcCalls++ },
                sleeper = { nowMs += it },
                clockMs = { nowMs },
            )

        val error =
            assertFailsWith<AssertionError> {
                rule
                    .apply(
                        object : Statement() {
                            override fun evaluate() {}
                        },
                        testDescription,
                    )
                    .evaluate()
            }

        assertEquals(2, gcCalls)
        assertTrue(error.message!!.contains("attachedNodes 35 -> 38"))
        assertTrue(error.message!!.contains("bufferPixels 1000 -> 9000"))
    }

    @Test
    fun resourceLeakRule_doesNotMaskTestFailure() {
        val baseline = SpatialSceneStats(attachedNodes = 35, bufferPixels = 1000L)
        var calls = 0
        val rule =
            XrResourceLeakRule(
                maxAttachedNodesDelta = 0,
                maxBufferPixelsDelta = 0L,
                settleTimeoutMs = 100L,
                pollIntervalMs = 50L,
                sceneProvider = {
                    calls++
                    sceneWithStats(baseline)
                },
                gcTrigger = {},
                sleeper = {},
                clockMs = { 0L },
            )

        val thrown =
            assertFailsWith<IllegalStateException> {
                rule
                    .apply(
                        object : Statement() {
                            override fun evaluate() {
                                throw IllegalStateException("original test failure")
                            }
                        },
                        testDescription,
                    )
                    .evaluate()
            }

        assertEquals("original test failure", thrown.message)
        assertEquals(1, calls)
    }

    @Test
    fun resourceLeakRule_assertWithinBudget_checksLeaksAndDoesNotMaskException() {
        var nowMs = 0L
        val scenes =
            ArrayDeque(
                listOf(
                    sceneWithStats(SpatialSceneStats(attachedNodes = 25, bufferPixels = 1000L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 25, bufferPixels = 1000L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 25, bufferPixels = 1000L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 30, bufferPixels = 1000L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 30, bufferPixels = 1000L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 30, bufferPixels = 1000L)),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 25, bufferPixels = 1000L)),
                )
            )
        val rule =
            XrResourceLeakRule(
                maxAttachedNodesDelta = 0,
                maxBufferPixelsDelta = 0L,
                settleTimeoutMs = 100L,
                pollIntervalMs = 50L,
                sceneProvider = { scenes.removeFirst() },
                gcTrigger = {},
                sleeper = { nowMs += it },
                clockMs = { nowMs },
            )

        assertEquals("ok", rule.assertWithinBudget { "ok" })

        val leakError = assertFailsWith<AssertionError> { rule.assertWithinBudget {} }
        assertTrue(leakError.message!!.contains("attachedNodes 25 -> 30"))

        val bodyError =
            assertFailsWith<IllegalStateException> {
                rule.assertWithinBudget<Unit> { throw IllegalStateException("scoped failure") }
            }
        assertEquals("scoped failure", bodyError.message)
        assertTrue(scenes.isEmpty())
    }

    @Test
    fun resourceLeakRule_failsOnMissingBaselineStats() {
        val rule =
            XrResourceLeakRule(
                maxAttachedNodesDelta = 0,
                maxBufferPixelsDelta = 0L,
                settleTimeoutMs = 100L,
                pollIntervalMs = 50L,
                sceneProvider = { sceneWithStats(null) },
                gcTrigger = {},
                sleeper = {},
                clockMs = { 0L },
            )

        val error = assertFailsWith<AssertionError> { rule.assertWithinBudget {} }
        assertTrue(error.message!!.contains("Missing baseline CPM scene stats"))
    }

    @Test
    fun resourceLeakRule_retriesTransientNullDumpAndFailsOnPersistentNull() {
        var nowMs = 0L
        val transientScenes =
            ArrayDeque(
                listOf(
                    sceneWithStats(SpatialSceneStats(attachedNodes = 20, bufferPixels = 512L)),
                    sceneWithStats(null),
                    sceneWithStats(SpatialSceneStats(attachedNodes = 20, bufferPixels = 512L)),
                )
            )
        val transientRule =
            XrResourceLeakRule(
                maxAttachedNodesDelta = 0,
                maxBufferPixelsDelta = 0L,
                settleTimeoutMs = 300L,
                pollIntervalMs = 50L,
                sceneProvider = { transientScenes.removeFirst() },
                gcTrigger = {},
                sleeper = { nowMs += it },
                clockMs = { nowMs },
            )
        transientRule.assertWithinBudget {}
        assertTrue(transientScenes.isEmpty())

        var persistentNowMs = 0L
        var calls = 0
        val persistentNullRule =
            XrResourceLeakRule(
                maxAttachedNodesDelta = 0,
                maxBufferPixelsDelta = 0L,
                settleTimeoutMs = 150L,
                pollIntervalMs = 50L,
                sceneProvider = {
                    calls++
                    if (calls == 1) {
                        sceneWithStats(SpatialSceneStats(attachedNodes = 20, bufferPixels = 512L))
                    } else {
                        sceneWithStats(null)
                    }
                },
                gcTrigger = {},
                sleeper = { persistentNowMs += it },
                clockMs = { persistentNowMs },
            )
        val timeoutError =
            assertFailsWith<AssertionError> { persistentNullRule.assertWithinBudget {} }
        assertTrue(timeoutError.message!!.contains("missing telemetry"))
        assertTrue(calls >= 3)
    }

    @Test
    fun resourceLeakRule_publicConstructor_rejectsNegativeBudgets() {
        XrResourceLeakRule()
        assertFailsWith<IllegalArgumentException> {
            XrResourceLeakRule(maxAttachedNodesDelta = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            XrResourceLeakRule(maxBufferPixelsDelta = -1L)
        }
        assertFailsWith<IllegalArgumentException> { XrResourceLeakRule(settleTimeoutMs = -1L) }
        assertFailsWith<IllegalArgumentException> { XrResourceLeakRule(pollIntervalMs = -1L) }
    }
}
