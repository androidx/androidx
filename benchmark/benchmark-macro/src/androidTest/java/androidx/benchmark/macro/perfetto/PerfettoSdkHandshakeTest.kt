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

package androidx.benchmark.macro.perfetto

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.Shell
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Packages
import androidx.benchmark.macro.perfetto.PerfettoSdkHandshakeTest.SdkDelivery.MISSING
import androidx.benchmark.macro.perfetto.PerfettoSdkHandshakeTest.SdkDelivery.PROVIDED_BY_BENCHMARK
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

private const val tracingPerfettoVersion = "1.0.0-alpha01" // TODO(224510255): get by 'reflection'
private const val minSupportedSdk = Build.VERSION_CODES.R // TODO(234351579): Support API < 30

@RunWith(Parameterized::class)
/**
 * End-to-end test verifying the process of enabling Perfetto SDK tracing using a broadcast.
 * @see [androidx.tracing.perfetto.TracingReceiver]
 */
@RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
class PerfettoSdkHandshakeTest(private val testConfig: TestConfig) {
    private val perfettoCapture = PerfettoCapture()
    private val targetPackage = Packages.TARGET
    private lateinit var scope: MacrobenchmarkScope

    companion object {
        @Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = listOf(
            TestConfig(sdkDelivery = PROVIDED_BY_BENCHMARK, packageAlive = true),
            TestConfig(sdkDelivery = MISSING, packageAlive = true),
            // TODO: tests verifying tracing on app start-up
        )
    }

    @Before
    fun setUp() {
        scope = MacrobenchmarkScope(targetPackage, launchWithClearTask = true)

        // kill process if running to ensure a clean test start
        if (Shell.isPackageAlive(targetPackage)) scope.killProcess()
        assertPackageAlive(false)

        // clean target process app data to ensure a clean start
        Shell.executeCommand("pm clear $targetPackage").let { response ->
            assertThat(response).matches("Success\r?\n")
        }
    }

    @After
    fun tearDown() {
        // kill the process at the end of the test
        scope.killProcess()
        assertPackageAlive(false)
    }

    @Test
    fun test_enable() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)

        // start the process if required to already be running when the handshake starts
        if (testConfig.packageAlive) enablePackage()

        // issue an 'enable' broadcast
        perfettoCapture.enableAndroidxTracingPerfetto(
            targetPackage, shouldProvideBinaries(testConfig.sdkDelivery)
        ).let { response: String? ->
            when (testConfig.sdkDelivery) {
                PROVIDED_BY_BENCHMARK -> assertThat(response).isNull()
                MISSING -> assertMissingBinariesResponse(response)
            }
        }

        // issue an 'enable' broadcast again
        perfettoCapture.enableAndroidxTracingPerfetto(
            targetPackage, shouldProvideBinaries(testConfig.sdkDelivery)
        ).let { response: String? ->
            when (testConfig.sdkDelivery) {
                PROVIDED_BY_BENCHMARK -> assertAlreadyEnabledResponse(response)
                MISSING -> assertMissingBinariesResponse(response)
            }
        }

        // check that the process 'survived' the test
        assertPackageAlive(true)
    }

    @Test
    fun test_detectUnsupported_abi() {
        assumeTrue(!isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)

        if (testConfig.packageAlive) enablePackage()

        try {
            perfettoCapture.enableAndroidxTracingPerfetto(
                targetPackage,
                shouldProvideBinaries(testConfig.sdkDelivery)
            )
        } catch (e: IllegalStateException) {
            assertThat(e.message).ignoringCase().contains("Unsupported ABI")
        }
    }

    @Test
    fun test_detectUnsupported_sdk() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT < minSupportedSdk)

        if (testConfig.packageAlive) enablePackage()

        val response =
            perfettoCapture.enableAndroidxTracingPerfetto(
                targetPackage,
                shouldProvideBinaries(testConfig.sdkDelivery)
            )

        assertThat(response).ignoringCase().contains("SDK version not supported")
    }

    private fun enablePackage() {
        scope.pressHome()
        scope.startActivityAndWait()
        assertPackageAlive(true)
    }

    private fun shouldProvideBinaries(sdkDelivery: SdkDelivery): Boolean {
        return when (sdkDelivery) {
            PROVIDED_BY_BENCHMARK -> true
            MISSING -> false
        }
    }

    private fun assertAlreadyEnabledResponse(response: String?) {
        assertThat(response).ignoringCase().contains("already enabled")
    }

    private fun assertMissingBinariesResponse(response: String?) {
        assertThat(response).ignoringCase().contains("binary dependencies missing")
        assertThat(response).contains("Required version: $tracingPerfettoVersion")
        assertThat(response).containsMatch(
            "Perfetto SDK binary dependencies missing" +
                ".*UnsatisfiedLinkError.*libtracing_perfetto.so"
        )
    }

    private fun assertPackageAlive(expected: Boolean) =
        assertThat(Shell.isPackageAlive(targetPackage)).isEqualTo(expected)

    data class TestConfig(
        /** Determines if and how Perfetto binaries are provided to the test app. */
        val sdkDelivery: SdkDelivery,

        /** Determines if the app is already running as the actual testing starts. */
        val packageAlive: Boolean,
    )

    /**
     * Determines if and how Perfetto binaries are provided to the test app.
     *
     * Note: the case where SDK binaries are already present is not tested here.
     * To some degree we test that case in
     * [androidx.tracing.perfetto.test.TracingTest.test_endToEnd_binaryDependenciesPresent].
     */
    enum class SdkDelivery {
        /** Benchmark detects they're missing and provides them to the app */
        PROVIDED_BY_BENCHMARK,

        /** Remain unresolved */
        MISSING
    }
}
