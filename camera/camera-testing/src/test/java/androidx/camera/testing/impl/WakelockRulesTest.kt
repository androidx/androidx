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

package androidx.camera.testing.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [WakelockEmptyActivityRule], [WakelockRule], and [IgnoreProblematicDeviceRule].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class WakelockRulesTest {

    private val testDescription =
        Description.createTestDescription(WakelockRulesTest::class.java, "sampleTest")

    @Test
    fun wakelockEmptyActivityRule_continuesWhenClearDeviceUiThrowsIllegalStateException() {
        var baseEvaluated = false
        val rule =
            WakelockEmptyActivityRule(
                brandsToEnable = null,
                clearDeviceUi = { throw IllegalStateException("UiAutomation not connected") },
            )

        rule
            .apply(
                object : Statement() {
                    override fun evaluate() {
                        baseEvaluated = true
                    }
                },
                testDescription,
            )
            .evaluate()

        assertThat(baseEvaluated).isTrue()
    }

    @Test
    fun wakelockEmptyActivityRule_skipsClearDeviceUiWhenBrandDoesNotMatch() {
        var clearCalled = false
        var baseEvaluated = false
        val rule =
            WakelockEmptyActivityRule(
                brandsToEnable = listOf("unmatched_brand"),
                clearDeviceUi = { clearCalled = true },
            )

        rule
            .apply(
                object : Statement() {
                    override fun evaluate() {
                        baseEvaluated = true
                    }
                },
                testDescription,
            )
            .evaluate()

        assertThat(clearCalled).isFalse()
        assertThat(baseEvaluated).isTrue()
    }

    @Test
    fun wakelockEmptyActivityRule_propagatesTestBodyFailureAfterClearDeviceUiFailure() {
        val rule =
            WakelockEmptyActivityRule(
                brandsToEnable = null,
                clearDeviceUi = { throw IllegalStateException("UiAutomation not connected") },
            )

        val thrown =
            assertThrows(AssertionError::class.java) {
                rule
                    .apply(
                        object : Statement() {
                            override fun evaluate() {
                                throw AssertionError("expected test failure")
                            }
                        },
                        testDescription,
                    )
                    .evaluate()
            }

        assertThat(thrown).hasMessageThat().isEqualTo("expected test failure")
    }

    @Test
    fun wakelockRule_continuesWhenClearDeviceUiThrowsIllegalStateException() {
        var baseEvaluated = false
        val rule =
            WakelockRule(
                timeoutMs = 1_000L,
                clearDeviceUi = {
                    throw IllegalStateException("UiAutomationService already registered!")
                },
            )

        rule
            .apply(
                object : Statement() {
                    override fun evaluate() {
                        baseEvaluated = true
                    }
                },
                testDescription,
            )
            .evaluate()

        assertThat(baseEvaluated).isTrue()
    }

    @Test
    fun ignoreProblematicDeviceRule_resolveAvdName_skipsPropLookupOnPhysicalDevice() {
        val queriedKeys = mutableListOf<String>()

        val resolved =
            IgnoreProblematicDeviceRule.resolveAvdName(isEmulatorDevice = false) { key ->
                queriedKeys.add(key)
                "Pixel_2_API_26"
            }

        assertThat(resolved).isEmpty()
        assertThat(queriedKeys).isEmpty()
    }

    @Test
    fun ignoreProblematicDeviceRule_resolveAvdName_fallsBackToBootQemuAvdNameOnEmulator() {
        val queriedKeys = mutableListOf<String>()

        val resolved =
            IgnoreProblematicDeviceRule.resolveAvdName(isEmulatorDevice = true) { key ->
                queriedKeys.add(key)
                if (key == "ro.boot.qemu.avd_name") "MediumPhone_API_35" else ""
            }

        assertThat(resolved).isEqualTo("MediumPhone_API_35")
        assertThat(queriedKeys)
            .containsExactly("ro.kernel.qemu.avd_name", "ro.boot.qemu.avd_name")
            .inOrder()
    }
}
