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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text.contextmenu.test

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.JUnitCore
import org.junit.runner.RunWith
import org.junit.runner.notification.RunListener

// KEEP THIS FILE UP TO DATE WITH
// compose/foundation/foundation/src/androidUnitTest/kotlin/androidx/compose/foundation/text/contextmenu/test/ContextMenuFlagFlipperRunnerTest.kt

@RunWith(ContextMenuFlagFlipperRunner::class)
class ContextMenuFlagFlipperRunnerTest {
    companion object {
        val staticFlagsReceived = mutableListOf<Boolean>()
    }

    @Test
    fun test_noSuppress() {
        staticFlagsReceived.add(ComposeFoundationFlags.isNewContextMenuEnabled)
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun test_suppressFalse() {
        assertThat(ComposeFoundationFlags.isNewContextMenuEnabled).isTrue()
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun test_suppressTrue() {
        assertThat(ComposeFoundationFlags.isNewContextMenuEnabled).isFalse()
    }
}

@ContextMenuFlagSuppress(suppressedFlagValue = false)
@RunWith(ContextMenuFlagFlipperRunner::class)
class ContextMenuFlagFlipperRunnerTestSuppressFalse {
    @Test
    fun test_noSuppress() {
        assertThat(ComposeFoundationFlags.isNewContextMenuEnabled).isTrue()
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun test_suppressFalse() {
        assertThat(ComposeFoundationFlags.isNewContextMenuEnabled).isTrue()
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun test_suppressTrue() {
        fail("Suppressed test should not run.")
    }
}

@ContextMenuFlagSuppress(suppressedFlagValue = true)
@RunWith(ContextMenuFlagFlipperRunner::class)
class ContextMenuFlagFlipperRunnerTestSuppressTrue {
    @Test
    fun test_noSuppress() {
        assertThat(ComposeFoundationFlags.isNewContextMenuEnabled).isFalse()
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = false)
    @Test
    fun test_suppressFalse() {
        fail("Suppressed test should not run.")
    }

    @ContextMenuFlagSuppress(suppressedFlagValue = true)
    @Test
    fun test_suppressTrue() {
        assertThat(ComposeFoundationFlags.isNewContextMenuEnabled).isFalse()
    }
}

class ContextMenuFlagFlipperRunnerMetaTest {
    @Test
    fun noSuppress_calledFourTimes_withBothFlagValues() {
        val junit = JUnitCore()
        val testExecutionCounterListener = TestExecutionCounterListener()

        junit.addListener(testExecutionCounterListener)

        ContextMenuFlagFlipperRunnerTest.staticFlagsReceived.clear()
        val result = junit.run(ContextMenuFlagFlipperRunnerTest::class.java)
        val actualFlagsReceived = ContextMenuFlagFlipperRunnerTest.staticFlagsReceived.toList()

        assertThat(result.wasSuccessful()).isTrue()
        assertThat(result.runCount).isEqualTo(4)
        assertThat(result.ignoreCount).isEqualTo(0)

        assertThat(testExecutionCounterListener.counts["test_noSuppress"]).isEqualTo(2)
        assertThat(testExecutionCounterListener.counts["test_suppressFalse"]).isEqualTo(1)
        assertThat(testExecutionCounterListener.counts["test_suppressTrue"]).isEqualTo(1)

        assertThat(actualFlagsReceived).containsExactly(true, false)
    }

    @Test
    fun suppressFalse_calledTwice_withOnlyTrueFlagValues() {
        val junit = JUnitCore()
        val testExecutionCounterListener = TestExecutionCounterListener()

        junit.addListener(testExecutionCounterListener)

        val result = junit.run(ContextMenuFlagFlipperRunnerTestSuppressFalse::class.java)

        assertThat(result.wasSuccessful()).isTrue()
        assertThat(result.runCount).isEqualTo(2)
        assertThat(result.ignoreCount).isEqualTo(0)

        assertThat(testExecutionCounterListener.counts["test_noSuppress"]).isEqualTo(1)
        assertThat(testExecutionCounterListener.counts["test_suppressFalse"]).isEqualTo(1)
    }

    @Test
    fun suppressTrue_calledTwice_withOnlyFalseFlagValues() {
        val junit = JUnitCore()
        val testExecutionCounterListener = TestExecutionCounterListener()

        junit.addListener(testExecutionCounterListener)

        val result = junit.run(ContextMenuFlagFlipperRunnerTestSuppressTrue::class.java)

        assertThat(result.wasSuccessful()).isTrue()
        assertThat(result.runCount).isEqualTo(2)
        assertThat(result.ignoreCount).isEqualTo(0)

        assertThat(testExecutionCounterListener.counts["test_noSuppress"]).isEqualTo(1)
        assertThat(testExecutionCounterListener.counts["test_suppressTrue"]).isEqualTo(1)
    }
}

private class TestExecutionCounterListener : RunListener() {
    val counts = mutableMapOf<String, Int>()

    override fun testStarted(description: Description) {
        val baseMethodName = description.methodName?.substringBefore('[')
        baseMethodName?.let { counts[it] = counts.getOrPut(it) { 0 } + 1 }
    }
}
