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

package androidx.build.optimization

import androidx.build.androidcli.AndroidCliBuildService
import androidx.build.androidcli.AndroidCliPack
import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.gradle.api.GradleException
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KeepRulesAnalysisTest {

    @get:Rule val tmpFolder = TemporaryFolder()

    @get:Rule val projectSetup = ProjectSetupRule()

    @Test
    fun validateLibraryResult_passesWithinDefaults() {
        val result =
            LibraryAnalyzerResultJson(
                d8CompileResult = D8CompileResultJson(dexSizeBytes = 100_000L),
                r8CompileResult =
                    R8CompileResultJson(
                        dexSizeBytes = 10_000L,
                        configuration = ConfigurationSummaryJson(),
                    ),
                validateConsumerKeepRulesResult = ValidateConsumerKeepRulesResultJson(),
            )
        KeepRulesErrorReporter.validateLibraryResult(
            projectPath = ":sample:sample",
            result = result,
            baseline = null,
        )
    }

    @Test
    fun validateLibraryResult_failsOnHighKeepRatioAndPassesWithBaseline() {
        val result =
            LibraryAnalyzerResultJson(
                d8CompileResult = D8CompileResultJson(dexSizeBytes = 100_000L),
                r8CompileResult =
                    R8CompileResultJson(
                        dexSizeBytes = 80_000L,
                        configuration =
                            ConfigurationSummaryJson(
                                keepRules =
                                    listOf(
                                        KeepRuleSummaryJson(
                                            source = "-keep class androidx.sample.Foo { *; }",
                                            keptItemCount = 42,
                                        )
                                    )
                            ),
                    ),
            )

        val ex =
            assertThrows(GradleException::class.java) {
                KeepRulesErrorReporter.validateLibraryResult(
                    projectPath = ":sample:sample",
                    result = result,
                    baseline = null,
                )
            }
        assertThat(ex.message)
            .contains("Keep rule analysis failed for :sample:sample: found 1 problem pattern:")
        assertThat(ex.message).contains("Keep ratio is too high (0.75, limit: 0.2")
        assertThat(ex.message).contains("(impact = 42) -keep class androidx.sample.Foo { *; }")
        assertThat(ex.message)
            .contains("PROJECT_PREFIX=:sample:sample ./gradlew updateKeepRulesBaseline")

        KeepRulesErrorReporter.validateLibraryResult(
            projectPath = ":sample:sample",
            result = result,
            baseline = BaselineConfig(overrideMaxKeepRatio = 0.77f),
        )
    }

    @Test
    fun computeBaselineKeepRatio_addsTwoPercentAndFloorsToPercentile() {
        assertThat(KeepRulesErrorReporter.computeBaselineKeepRatio(0.4492f)).isEqualTo(0.46f)
        assertThat(KeepRulesErrorReporter.computeBaselineKeepRatio(0.4018f)).isEqualTo(0.42f)
        assertThat(KeepRulesErrorReporter.computeBaselineKeepRatio(0.2040f)).isEqualTo(0.22f)
        assertThat(KeepRulesErrorReporter.computeBaselineKeepRatio(0.1925f)).isEqualTo(0.21f)
    }

    @Test
    fun computeBaselineMaxD8SizeMegabytes_ceilingsToNearestHundredKilobytes() {
        assertThat(KeepRulesErrorReporter.computeBaselineMaxD8SizeMegabytes(5_738_052L))
            .isEqualTo(5.8f)
        assertThat(KeepRulesErrorReporter.computeBaselineMaxD8SizeMegabytes(6_000_001L))
            .isEqualTo(6.1f)
        assertThat(KeepRulesErrorReporter.computeBaselineMaxD8SizeMegabytes(5_200_000L))
            .isEqualTo(5.2f)
    }

    @Test
    fun validateLibraryResult_failsOnHighD8AndR8Sizes() {
        val result =
            LibraryAnalyzerResultJson(
                d8CompileResult = D8CompileResultJson(dexSizeBytes = 5_738_052L),
                r8CompileResult = R8CompileResultJson(dexSizeBytes = 2_000_000L),
            )

        val ex =
            assertThrows(GradleException::class.java) {
                KeepRulesErrorReporter.validateLibraryResult(
                    projectPath = ":sample:sample",
                    result = result,
                    baseline = null,
                )
            }
        assertThat(ex.message)
            .contains("D8 size is too high (5738052 bytes, limit: 5000000 bytes).")
        assertThat(ex.message).contains("Are you including unnecessary code?")
        assertThat(ex.message).contains("R8 size is too high")

        KeepRulesErrorReporter.validateLibraryResult(
            projectPath = ":sample:sample",
            result =
                LibraryAnalyzerResultJson(
                    d8CompileResult = D8CompileResultJson(dexSizeBytes = 5_738_052L),
                    r8CompileResult = R8CompileResultJson(dexSizeBytes = 0L),
                ),
            baseline = BaselineConfig(overrideMaxD8SizeMegabytes = 5.8f),
        )
    }

    @Test
    fun validateLibraryResult_failsOnPackageWideRulesAndPassesWithBaseline() {
        val usedRule = "-keep class androidx.sample.** { *; }"
        val unusedRule = "-keep class com.example.other.** { *; }"
        val result =
            LibraryAnalyzerResultJson(
                d8CompileResult = D8CompileResultJson(dexSizeBytes = 50_000L),
                r8CompileResult =
                    R8CompileResultJson(
                        dexSizeBytes = 2_000L,
                        configuration =
                            ConfigurationSummaryJson(
                                usedPackageWideKeepRules =
                                    listOf(KeepRuleSummaryJson(source = "$usedRule\n")),
                                unusedPackageWideKeepRules =
                                    listOf(KeepRuleSummaryJson(source = unusedRule)),
                            ),
                    ),
            )

        val ex =
            assertThrows(GradleException::class.java) {
                KeepRulesErrorReporter.validateLibraryResult(
                    projectPath = ":sample:sample",
                    result = result,
                    baseline = null,
                )
            }
        assertThat(ex.message).contains("1 used package-wide keep rule:")
        assertThat(ex.message).contains("1 unused package-wide keep rule:")

        KeepRulesErrorReporter.validateLibraryResult(
            projectPath = ":sample:sample",
            result = result,
            baseline =
                BaselineConfig(
                    baselineUsedPackageWideKeepRules = setOf(usedRule),
                    baselineUnusedPackageWideKeepRules = setOf(unusedRule),
                ),
        )
    }

    @Test
    fun validateLibraryResult_failsOnUnsuppressableBlockedRules() {
        val result =
            LibraryAnalyzerResultJson(
                d8CompileResult = D8CompileResultJson(dexSizeBytes = 50_000L),
                r8CompileResult = R8CompileResultJson(dexSizeBytes = 0L),
                validateConsumerKeepRulesResult =
                    ValidateConsumerKeepRulesResultJson(
                        blockedKeepRules = listOf(BlockedConsumerKeepRuleJson("-dontobfuscate"))
                    ),
            )

        val ex =
            assertThrows(GradleException::class.java) {
                KeepRulesErrorReporter.validateLibraryResult(
                    projectPath = ":sample:sample",
                    result = result,
                    baseline = null,
                )
            }
        assertThat(ex.message).contains("1 blocked global keep rule:")
        assertThat(ex.message).contains("-dontobfuscate")
    }

    @Test
    fun reconcileBaselines_prunesFixedAndPreservesOutOfScope() {
        val existing =
            mapOf(
                ":out:of-scope" to
                    BaselineConfig(
                        baselineUsedPackageWideKeepRules =
                            setOf("-keep class androidx.out.** { *; }")
                    ),
                ":fixed:lib" to
                    BaselineConfig(
                        baselineUsedPackageWideKeepRules =
                            setOf("-keep class androidx.fixed.** { *; }")
                    ),
            )
        val reports =
            listOf(
                KeepRulesAnalysisReport(
                    projectPath = ":fixed:lib",
                    results =
                        listOf(
                            AnalyzeResultItemJson(
                                optimization =
                                    OptimizationAnalysisJson(
                                        libraryAnalysisResult =
                                            LibraryAnalysisResultJson(
                                                dex =
                                                    LibraryAnalyzerResultJson(
                                                        d8CompileResult =
                                                            D8CompileResultJson(100_000L),
                                                        r8CompileResult =
                                                            R8CompileResultJson(dexSizeBytes = 0L),
                                                    )
                                            )
                                    )
                            )
                        ),
                ),
                KeepRulesAnalysisReport(
                    projectPath = ":new:violation",
                    results =
                        listOf(
                            AnalyzeResultItemJson(
                                optimization =
                                    OptimizationAnalysisJson(
                                        libraryAnalysisResult =
                                            LibraryAnalysisResultJson(
                                                dex =
                                                    LibraryAnalyzerResultJson(
                                                        d8CompileResult =
                                                            D8CompileResultJson(100_000L),
                                                        r8CompileResult =
                                                            R8CompileResultJson(
                                                                dexSizeBytes = 2_000L,
                                                                configuration =
                                                                    ConfigurationSummaryJson(
                                                                        usedPackageWideKeepRules =
                                                                            listOf(
                                                                                KeepRuleSummaryJson(
                                                                                    source =
                                                                                        "-keep class androidx.new.** { *; }"
                                                                                )
                                                                            )
                                                                    ),
                                                            ),
                                                    )
                                            )
                                    )
                            )
                        ),
                ),
            )

        val reconciled = reconcileBaselines(existing, reports)
        assertThat(reconciled.keys).containsExactly(":out:of-scope", ":new:violation")
        assertThat(reconciled[":new:violation"]?.baselineUsedPackageWideKeepRules)
            .containsExactly("-keep class androidx.new.** { *; }")

        val json = KeepRulesJson.renderBaselineJson(reconciled)
        assertThat(KeepRulesJson.parseBaselineJson(json)).isEqualTo(reconciled)

        val checkedInBaseline =
            File(projectSetup.props.rootProjectPath, KEEP_RULES_BASELINE_RELATIVE_PATH)
        assertThat(checkedInBaseline.isFile).isTrue()
        val checkedInText = checkedInBaseline.readText()
        val parsedCheckedIn = KeepRulesJson.parseBaselineJson(checkedInText)
        assertThat(parsedCheckedIn).isNotEmpty()
        assertThat(KeepRulesJson.renderBaselineJson(parsedCheckedIn)).isEqualTo(checkedInText)
    }

    @Test
    fun parseAnalyzeOutput_supportsNumericAndStringDexSizeBytes() {
        val numericJson =
            """
            {
              "results": [{
                "optimization": {
                  "libraryAnalysisResult": {
                    "dex": {
                      "d8CompileResult": { "dexSizeBytes": 120000 },
                      "r8CompileResult": { "dexSizeBytes": 30000 }
                    }
                  }
                }
              }]
            }
            """
                .trimIndent()
        val stringJson =
            """
            {
              "results": [{
                "optimization": {
                  "libraryAnalysisResult": {
                    "dex": {
                      "d8CompileResult": { "dexSizeBytes": "120000" },
                      "r8CompileResult": { "dexSizeBytes": "30000" }
                    }
                  }
                }
              }]
            }
            """
                .trimIndent()

        val numericDex =
            KeepRulesJson.gson
                .fromJson(numericJson, AnalyzeCommandOutput::class.java)
                .results
                ?.single()
                ?.optimization
                ?.libraryAnalysisResult
                ?.dex
        val stringDex =
            KeepRulesJson.gson
                .fromJson(stringJson, AnalyzeCommandOutput::class.java)
                .results
                ?.single()
                ?.optimization
                ?.libraryAnalysisResult
                ?.dex

        assertThat(numericDex?.d8CompileResult?.dexSizeBytes).isEqualTo(120_000L)
        assertThat(numericDex?.r8CompileResult?.dexSizeBytes).isEqualTo(30_000L)
        assertThat(stringDex?.d8CompileResult?.dexSizeBytes).isEqualTo(120_000L)
        assertThat(stringDex?.r8CompileResult?.dexSizeBytes).isEqualTo(30_000L)
    }

    @Test
    fun validateLibraryResult_passesWhenImprovedBelowBaseline() {
        val improvedResult =
            LibraryAnalyzerResultJson(
                d8CompileResult = D8CompileResultJson(dexSizeBytes = 100_000L),
                r8CompileResult =
                    R8CompileResultJson(
                        dexSizeBytes = 10_000L,
                        configuration = ConfigurationSummaryJson(),
                    ),
            )
        KeepRulesErrorReporter.validateLibraryResult(
            projectPath = ":sample:sample",
            result = improvedResult,
            baseline =
                BaselineConfig(
                    overrideMaxKeepRatio = 0.75f,
                    baselineUsedPackageWideKeepRules =
                        setOf("-keep class androidx.sample.** { *; }"),
                    baselineUnusedPackageWideKeepRules = setOf("-keep class com.other.** { *; }"),
                ),
        )
    }

    @Test
    fun androidCliPrebuilts_stagesPackAndAnalyzesJarOffline() {
        val prebuiltsPath = checkNotNull(projectSetup.props.prebuiltsPath)
        val cliPrebuiltsDir = File(prebuiltsPath, "androidx/android-cli")
        val versionFile = cliPrebuiltsDir.resolve("VERSION")
        assertThat(versionFile.isFile).isTrue()
        val expectedVersion = versionFile.readText().trim()
        assertThat(expectedVersion).isNotEmpty()

        for (binaryName in listOf("android_linux_x86_64", "android_darwin_arm64")) {
            val binary = cliPrebuiltsDir.resolve(binaryName)
            assertThat(binary.isFile).isTrue()
            assertThat(binary.canExecute()).isTrue()
        }
        for (pack in AndroidCliPack.entries) {
            assertThat(
                    cliPrebuiltsDir.resolve("packs/${pack.packName}/${pack.packName}.jar").isFile
                )
                .isTrue()
        }

        val project = ProjectBuilder.builder().withProjectDir(projectSetup.rootDir).build()
        val ext = project.rootProject.property("ext") as ExtraPropertiesExtension
        ext.set("prebuiltsRoot", File(prebuiltsPath))
        val outDir = tmpFolder.newFolder("out")
        ext.set("outDir", outDir)

        val cliService = AndroidCliBuildService.registerOrGet(project).get()

        // Verify that the checked-in VERSION file matches the binary's reported version so
        // PackManager resolves `$ANDROID_USER_HOME/cli/packs/<version>/` at the staged path.
        val versionStdout = cliService.execute(listOf("--version")).trim()
        assertThat(versionStdout).contains(expectedVersion)

        val sampleJar = tmpFolder.newFile("sample.jar")
        val sampleClassBytes =
            checkNotNull(BaselineConfig::class.java.getResourceAsStream("BaselineConfig.class"))
                .use { it.readBytes() }
        val keepRule = "-keep class androidx.build.optimization.** { *; }"
        ZipOutputStream(sampleJar.outputStream()).use { jarZip ->
            jarZip.putNextEntry(ZipEntry("androidx/build/optimization/BaselineConfig.class"))
            jarZip.write(sampleClassBytes)
            jarZip.closeEntry()
            jarZip.putNextEntry(ZipEntry("META-INF/proguard/sample.pro"))
            jarZip.write(keepRule.toByteArray(Charsets.UTF_8))
            jarZip.closeEntry()
        }

        val jsonOutput =
            cliService.execute(
                args =
                    listOf(
                        "analyze",
                        "--analysis=optimization",
                        "-f",
                        "json",
                        sampleJar.absolutePath,
                    ),
                requiredPacks = setOf(AndroidCliPack.OPTIMIZATION),
            )
        val stagedPack =
            outDir.resolve(
                "android-cli-home/cli/packs/$expectedVersion/optimization/optimization.jar"
            )
        assertThat(stagedPack.isFile).isTrue()

        val parsed = KeepRulesJson.gson.fromJson(jsonOutput, AnalyzeCommandOutput::class.java)
        val dex = parsed?.results?.single()?.optimization?.libraryAnalysisResult?.dex
        assertThat(dex).isNotNull()
        assertThat(dex!!.d8CompileResult?.dexSizeBytes).isGreaterThan(0L)
        assertThat(
                dex.r8CompileResult?.configuration?.usedPackageWideKeepRules?.map {
                    it.source.trim()
                }
            )
            .contains(keepRule)
    }
}
