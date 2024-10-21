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
import android.util.JsonReader
import androidx.annotation.RequiresApi
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Packages
import androidx.benchmark.macro.perfetto.PerfettoSdkHandshakeTest.SdkDelivery.MISSING
import androidx.benchmark.macro.perfetto.PerfettoSdkHandshakeTest.SdkDelivery.PROVIDED_BY_BENCHMARK
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig.InitialProcessState
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.perfetto.handshake.PerfettoSdkHandshake
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_CANCELLED
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_SUCCESS
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.StringReader
import java.util.regex.Pattern
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

private const val tracingPerfettoVersion = "1.0.0" // TODO(224510255): get by 'reflection'
private const val minSupportedSdk = Build.VERSION_CODES.R // TODO(234351579): Support API < 30

@RunWith(Parameterized::class)
/**
 * End-to-end test verifying the process of enabling Perfetto SDK tracing using a broadcast.
 *
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
        fun parameters() =
            listOf(
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
        Shell.executeScriptCaptureStdout("pm clear $targetPackage").let { response ->
            assertThat(response).matches("Success\r?\n")
        }
    }

    @After
    fun tearDown() {
        // kill the process at the end of the test
        killProcess()
    }

    @Test
    fun test_enable() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)

        // start the process if required to already be running when the handshake starts
        if (testConfig.packageAlive) enablePackage()

        // issue an 'enable' broadcast
        perfettoCapture
            .enableAndroidxTracingPerfetto(
                targetPackage,
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = false
            )
            .let { response: String? ->
                when (testConfig.sdkDelivery) {
                    PROVIDED_BY_BENCHMARK -> assertThat(response).isNull()
                    MISSING -> assertMissingBinariesResponse(response)
                }
            }

        // issue an 'enable' broadcast again
        perfettoCapture
            .enableAndroidxTracingPerfetto(
                targetPackage,
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = false
            )
            .let { response: String? ->
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
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = false
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
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = false
            )

        assertThat(response).ignoringCase().contains("SDK version not supported")
    }

    /**
     * This tests [androidx.tracing.perfetto.handshake.PerfettoSdkHandshake] which is used by both
     * Benchmark and Studio.
     *
     * By contrast, other tests use the [PerfettoCapture.enableAndroidxTracingPerfetto], which is
     * built on top of [androidx.tracing.perfetto.handshake.PerfettoSdkHandshake] and implements the
     * parts where Studio and Benchmark differ.
     */
    @Test
    fun test_handshake_framework() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)

        /** perform a handshake using [androidx.tracing.perfetto.handshake.PerfettoSdkHandshake] */
        val libraryZip: File? = resolvePerfettoAar()
        val tmpDir = Outputs.dirUsableByAppAndShell
        val mvTmpDst = createShellFileMover()
        val librarySource =
            libraryZip?.let {
                PerfettoSdkHandshake.LibrarySource.aarLibrarySource(libraryZip, tmpDir, mvTmpDst)
            }
        val versionRx = "\\d+(\\.\\d+){2}(-[\\w-]+)?"
        val handshake = constructPerfettoHandshake()
        when (testConfig.sdkDelivery) {
            PROVIDED_BY_BENCHMARK -> {
                checkNotNull(libraryZip)
                handshake.enableTracingImmediate(librarySource).let { response ->
                    assertThat(response.resultCode).isEqualTo(RESULT_CODE_SUCCESS)
                    assertThat(response.requiredVersion).matches(versionRx)
                }
                handshake.enableTracingImmediate(librarySource).let { response ->
                    assertThat(response.resultCode).isEqualTo(RESULT_CODE_ALREADY_ENABLED)
                    assertThat(response.requiredVersion).matches(versionRx)
                }
            }
            MISSING -> {
                check(libraryZip == null)
                handshake.enableTracingImmediate().let { response ->
                    assertThat(response.resultCode).isEqualTo(RESULT_CODE_ERROR_BINARY_MISSING)
                    assertThat(response.requiredVersion).matches(versionRx)
                }
                handshake.enableTracingImmediate().let { response ->
                    assertThat(response.resultCode).isEqualTo(RESULT_CODE_ERROR_BINARY_MISSING)
                    assertThat(response.requiredVersion).matches(versionRx)
                }
            }
        }
    }

    @Test
    fun test_handshake_framework_cold_start_persistent() =
        test_handshake_framework_cold_start(persistent = true)

    @Test
    fun test_handshake_framework_cold_start_non_persistent() =
        test_handshake_framework_cold_start(persistent = false)

    fun test_handshake_framework_cold_start(persistent: Boolean) {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)
        assumeTrue(testConfig.sdkDelivery == PROVIDED_BY_BENCHMARK)

        // perform a handshake setting up cold start tracing
        killProcess()
        assertPackageAlive(false)
        val handshake = constructPerfettoHandshake()
        val libraryZip = resolvePerfettoAar()
        val tmpDir = Outputs.dirUsableByAppAndShell
        val mvTmpDst = createShellFileMover()
        val librarySource =
            libraryZip?.let {
                PerfettoSdkHandshake.LibrarySource.aarLibrarySource(libraryZip, tmpDir, mvTmpDst)
            }

        try {
            val enableColdTracingResponse =
                handshake.enableTracingColdStart(persistent, librarySource)
            assertThat(enableColdTracingResponse.resultCode).isEqualTo(RESULT_CODE_SUCCESS)
            assertPackageAlive(false)

            // start the app
            // verify that tracing was enabled at app startup (once)
            enablePackage()
            handshake.enableTracingImmediate().let {
                assertThat(it.resultCode).isEqualTo(RESULT_CODE_ALREADY_ENABLED)
            }

            // verify that tracing was enabled at app startup (more than once)
            killProcess()
            enablePackage()
            handshake.enableTracingImmediate(librarySource).let {
                assertThat(it.resultCode)
                    .isEqualTo(
                        // in non-persistent mode, cold startup tracing should expire after one run
                        when (persistent) {
                            true -> RESULT_CODE_ALREADY_ENABLED
                            else -> RESULT_CODE_SUCCESS
                        }
                    )
            }
        } finally {
            // clean up
            handshake.disableTracingColdStart().let {
                assertThat(it.resultCode).isEqualTo(RESULT_CODE_SUCCESS)
            }
        }
    }

    /**
     * Tests [androidx.benchmark.perfetto.PerfettoCapture.enableAndroidxTracingPerfetto] as opposed
     * to [androidx.tracing.perfetto.handshake.PerfettoSdkHandshake.enableTracingColdStart]
     */
    @Test
    fun test_handshake_cold_start() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)
        assumeTrue(testConfig.sdkDelivery == PROVIDED_BY_BENCHMARK)

        // perform a handshake setting up cold start tracing
        killProcess()
        assertPackageAlive(false)

        perfettoCapture
            .enableAndroidxTracingPerfetto(
                targetPackage,
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = true
            )
            .let { assertThat(it).isNull() }
        assertPackageAlive(false)

        // start the app
        // verify that tracing was enabled at app startup (once)
        enablePackage()
        perfettoCapture
            .enableAndroidxTracingPerfetto(
                targetPackage,
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = false
            )
            .let { assertAlreadyEnabledResponse(it) }

        // verify that tracing was enabled at app startup (more than once)
        killProcess()
        enablePackage()
        perfettoCapture
            .enableAndroidxTracingPerfetto(
                targetPackage,
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = false
            )
            .let {
                // in non-persistent mode, cold startup tracing should expire after one run
                assertThat(it).isNull()
            }
    }

    @Test
    fun test_handshake_framework_cold_start_disable_persistent() =
        test_handshake_framework_cold_start_disable(persistent = true)

    @Test
    fun test_handshake_framework_cold_start_disable_non_persistent() =
        test_handshake_framework_cold_start_disable(persistent = true)

    fun test_handshake_framework_cold_start_disable(persistent: Boolean) {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)
        assumeTrue(testConfig.sdkDelivery == PROVIDED_BY_BENCHMARK)

        // perform a handshake setting up cold start tracing
        killProcess()
        val handshake = constructPerfettoHandshake()
        val libraryZip = resolvePerfettoAar()
        val tmpDir = Outputs.dirUsableByAppAndShell
        val mvTmpDst = createShellFileMover()
        val librarySource =
            libraryZip?.let {
                PerfettoSdkHandshake.LibrarySource.aarLibrarySource(libraryZip, tmpDir, mvTmpDst)
            }
        val enableColdTracingResponse = handshake.enableTracingColdStart(persistent, librarySource)
        assertThat(enableColdTracingResponse.resultCode).isEqualTo(RESULT_CODE_SUCCESS)

        // disable cold start tracing
        handshake.disableTracingColdStart()
        assertPackageAlive(false)

        // start the app
        enablePackage()

        /**
         * Verify that tracing was not enabled at app startup. Note: if cold start tracing was
         * enabled, we'd receive [RESULT_CODE_ALREADY_ENABLED]
         */
        val enableWarmTracingResponse = handshake.enableTracingImmediate(librarySource)
        assertThat(enableWarmTracingResponse.resultCode).isEqualTo(RESULT_CODE_SUCCESS)
    }

    @Test
    fun test_handshake_framework_cold_start_app_terminated_on_error() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)
        assumeTrue(testConfig.sdkDelivery == MISSING)

        // perform a handshake setting up cold start tracing
        val handshake = constructPerfettoHandshake()
        val enableColdTracingResponse = handshake.enableTracingColdStart()
        assertThat(enableColdTracingResponse.resultCode).isEqualTo(RESULT_CODE_ERROR_BINARY_MISSING)

        // verify that the app process has been terminated
        // in the non-error case we already have these verifications in other tests
        assertPackageAlive(false)
    }

    private fun killProcess() {
        scope.killProcess()
        assertPackageAlive(false)
    }

    @Test
    fun test_handshake_package_does_not_exist() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)

        val response =
            perfettoCapture.enableAndroidxTracingPerfetto(
                "package.does.not.exist.89e51176_bc28_41f1_ac73_ca717454b517",
                shouldProvideBinaries(testConfig.sdkDelivery),
                isColdStartupTracing = false
            )

        assertThat(response)
            .ignoringCase()
            .contains("The broadcast to enable tracing was not received")
    }

    /**
     * Unlike [test_handshake_package_does_not_exist], which uses [PerfettoCapture], this test uses
     * a lower-level component [PerfettoSdkHandshake].
     */
    @Test
    fun test_handshake_framework_package_does_not_exist() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)

        val handshake =
            PerfettoSdkHandshake(
                "package.does.not.exist.89e51176_bc28_41f1_ac73_ca717454b517",
                parseJsonMap = { emptyMap() },
                Shell::executeScriptCaptureStdout
            )

        // try
        handshake.enableTracingImmediate().also { response ->
            assertThat(response.resultCode).isEqualTo(RESULT_CODE_CANCELLED)
            assertThat(response.requiredVersion).isNull()
        }

        // try again
        handshake.enableTracingImmediate().also { response ->
            assertThat(response.resultCode).isEqualTo(RESULT_CODE_CANCELLED)
            assertThat(response.requiredVersion).isNull()
        }
    }

    @Test
    fun test_handshake_framework_parsing_error() {
        assumeTrue(isAbiSupported())
        assumeTrue(Build.VERSION.SDK_INT >= minSupportedSdk)

        val parsingException = "I don't know how to JSON"
        val handshake =
            PerfettoSdkHandshake(
                targetPackage,
                parseJsonMap = { throw IllegalArgumentException(parsingException) },
                Shell::executeScriptCaptureStdout
            )

        handshake.enableTracingImmediate().also { response ->
            assertThat(response.resultCode).isEqualTo(RESULT_CODE_ERROR_OTHER)
            assertThat(response.requiredVersion).isNull()
            assertThat(response.message)
                .containsMatch(
                    "Exception occurred while trying to parse a response.*Error.*$parsingException"
                        .toPattern(Pattern.CASE_INSENSITIVE)
                )
        }
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
        assertThat(response)
            .containsMatch(
                "Perfetto SDK binary dependencies missing" +
                    ".*UnsatisfiedLinkError.*libtracing_perfetto.so"
            )
        assertThat(response)
            .contains(
                "androidTestImplementation(" +
                    "\"androidx.tracing:tracing-perfetto-binary:$tracingPerfettoVersion\")"
            )
    }

    private fun assertPackageAlive(expected: Boolean) =
        assertThat(Shell.isPackageAlive(targetPackage)).isEqualTo(expected)

    private fun createShellFileMover() = { tmpFile: File, dstFile: File ->
        Shell.mkdir(dstFile.parentFile!!.path)
        Shell.mv(from = tmpFile.path, to = dstFile.path)
    }

    private fun constructPerfettoHandshake(): PerfettoSdkHandshake =
        PerfettoSdkHandshake(
            targetPackage,
            parseJsonMap = { jsonString: String ->
                sequence {
                        JsonReader(StringReader(jsonString)).use { reader ->
                            reader.beginObject()
                            while (reader.hasNext()) yield(reader.nextName() to reader.nextString())
                            reader.endObject()
                        }
                    }
                    .toMap()
            },
            executeShellCommand = { cmd ->
                val (stdout, stderr) = Shell.executeScriptCaptureStdoutStderr(cmd)
                listOf(stdout, stderr)
                    .filter { it.isNotBlank() }
                    .joinToString(separator = System.lineSeparator())
            }
        )

    private fun resolvePerfettoAar(): File? =
        when (testConfig.sdkDelivery) {
            MISSING -> null
            PROVIDED_BY_BENCHMARK -> {
                // find tracing-perfetto-binary AAR in test assets
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val libraryZipPath: String? = run {
                    val rx = Regex("tracing-perfetto-binary-[^/]+\\.aar", RegexOption.IGNORE_CASE)
                    val queue = ArrayDeque(context.assets.list("")?.asList() ?: emptyList())
                    while (queue.isNotEmpty<String?>()) {
                        val curr = queue.removeFirst()
                        val desc = context.assets.list(curr) ?: emptyArray()
                        when (desc.size) {
                            0 -> if (curr.matches(rx)) return@run curr
                            else -> queue.addAll(desc.map { "$curr/$it" })
                        }
                    }
                    null
                }
                assertThat(libraryZipPath).isNotNull()

                // place the AAR in a location that can be referenced by a file-system path
                val tmpLibFile =
                    File.createTempFile("tmplib", ".zip", Outputs.dirUsableByAppAndShell).also {
                        it.deleteOnExit()
                    }
                context.assets.open(libraryZipPath!!).use { input ->
                    tmpLibFile.outputStream().use { output -> input.copyTo(output) }
                }
                tmpLibFile
            }
        }

    data class TestConfig(
        /** Determines if and how Perfetto binaries are provided to the test app. */
        val sdkDelivery: SdkDelivery,

        /** Determines if the app is already running as the actual testing starts. */
        val packageAlive: Boolean,
    )

    /**
     * Determines if and how Perfetto binaries are provided to the test app.
     *
     * Note: the case where SDK binaries are already present is not tested here. To some degree we
     * test that case in
     * [androidx.tracing.perfetto.test.TracingTest.test_endToEnd_binaryDependenciesPresent].
     */
    enum class SdkDelivery {
        /** Benchmark detects they're missing and provides them to the app */
        PROVIDED_BY_BENCHMARK,

        /** Remain unresolved */
        MISSING
    }

    private fun PerfettoCapture.enableAndroidxTracingPerfetto(
        targetPackage: String,
        provideBinariesIfMissing: Boolean,
        isColdStartupTracing: Boolean
    ): String? =
        this.enableAndroidxTracingPerfetto(
                PerfettoSdkConfig(
                    targetPackage,
                    if (isColdStartupTracing) InitialProcessState.NotAlive
                    else InitialProcessState.Alive,
                    provideBinariesIfMissing
                )
            )
            .let { (resultCode, message) ->
                // Maps the response into the old contract of [enableAndroidxTracingPerfetto], where
                // for
                // success we get a [null] response; otherwise an error message
                if (resultCode == RESULT_CODE_SUCCESS) null else message
            }
}
