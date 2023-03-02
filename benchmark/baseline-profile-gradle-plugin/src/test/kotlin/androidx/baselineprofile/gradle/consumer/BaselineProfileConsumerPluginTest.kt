/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.consumer

import androidx.baselineprofile.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofile.gradle.utils.GRADLE_CODE_PRINT_TASK
import androidx.baselineprofile.gradle.utils.build
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.camelCase
import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfileConsumerPluginTest {

    // To test the consumer plugin we need a module that exposes a baselineprofile configuration
    // to be consumed. This is why we'll be using 2 projects. The producer project build gradle
    // is generated ad hoc in the tests that require it in order to supply mock profiles.

    companion object {
        private const val expectedBaselineProfileOutputFolder = "generated/baselineProfiles"
        private const val ANDROID_APPLICATION_PLUGIN = "com.android.application"
        private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
        private const val ANDROID_TEST_PLUGIN = "com.android.test"
    }

    private val rootFolder = TemporaryFolder().also { it.create() }

    @get:Rule
    val consumerProjectSetup = ProjectSetupRule(rootFolder.root)

    @get:Rule
    val producerProjectSetup = ProjectSetupRule(rootFolder.root)

    private lateinit var consumerModuleName: String
    private lateinit var producerModuleName: String
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        consumerModuleName = consumerProjectSetup.rootDir.relativeTo(rootFolder.root).name
        producerModuleName = producerProjectSetup.rootDir.relativeTo(rootFolder.root).name

        rootFolder.newFile("settings.gradle").writeText(
            """
            include '$consumerModuleName'
            include '$producerModuleName'
        """.trimIndent()
        )
        gradleRunner = GradleRunner.create()
            .withProjectDir(consumerProjectSetup.rootDir)
            .withPluginClasspath()
    }

    private fun baselineProfileFile(variantName: String) = File(
        consumerProjectSetup.rootDir,
        "src/$variantName/$expectedBaselineProfileOutputFolder/baseline-prof.txt"
    )

    private fun readBaselineProfileFileContent(variantName: String): List<String> =
        baselineProfileFile(variantName).readLines()

    @Test
    fun testGenerateTaskWithNoFlavors() {
        setupConsumerProject(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = false
        )
        setupProducerProject(
            listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
            )
    }

    @Test
    fun testGenerateTaskWithFlavorsAndDefaultMerge() {
        setupConsumerProject(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            dependencyOnProducerProject = true
        )
        setupProducerProjectWithFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // Asserts that all per-variant, per-flavor and per-build type tasks are being generated.
        gradleRunner.buildAndAssertThatOutput("tasks") {
            contains("generateReleaseBaselineProfile - ")
            contains("generateFreeReleaseBaselineProfile - ")
            contains("generatePaidReleaseBaselineProfile - ")
        }

        gradleRunner
            .withArguments("generateReleaseBaselineProfile", "--stacktrace")
            .build()

        assertThat(readBaselineProfileFileContent("freeRelease"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
            )

        assertThat(readBaselineProfileFileContent("paidRelease"))
            .containsExactly(
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
            )
    }

    @Test
    fun testGenerateTaskWithFlavorsAndMergeAll() {
        setupConsumerProject(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            dependencyOnProducerProject = true,
            baselineProfileBlock = """
                mergeIntoMain = true
            """.trimIndent()
        )
        setupProducerProjectWithFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )

        // Asserts that all per-variant, per-flavor and per-build type tasks are being generated.
        gradleRunner.buildAndAssertThatOutput("tasks") {
            contains("generateBaselineProfile - ")
            contains("generateReleaseBaselineProfile - ")
            doesNotContain("generateFreeReleaseBaselineProfile - ")
            doesNotContain("generatePaidReleaseBaselineProfile - ")
        }

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
            )
    }

    @Test
    fun testPluginAppliedToApplicationModule() {
        setupProducerProject()
        setupConsumerProject(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            addAppTargetPlugin = false,
            dependencyOnProducerProject = true
        )
        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()
        // This should not fail.
    }

    @Test
    fun testPluginAppliedToLibraryModule() {
        setupProducerProject()
        setupConsumerProject(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            addAppTargetPlugin = false,
            dependencyOnProducerProject = true
        )
        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()
        // This should not fail.
    }

    @Test
    fun testPluginAppliedToNonApplicationAndNonLibraryModule() {
        setupProducerProject()
        setupConsumerProject(
            androidPlugin = ANDROID_TEST_PLUGIN,
            addAppTargetPlugin = false,
            dependencyOnProducerProject = true
        )

        gradleRunner
            .withArguments("generateReleaseBaselineProfile", "--stacktrace")
            .buildAndFail()
    }

    @Test
    fun testSrcSetAreAddedToVariants() {
        setupConsumerProject(
            androidPlugin = "com.android.application",
            flavors = true,
            dependencyOnProducerProject = false,
            baselineProfileBlock = """
                enableR8BaselineProfileRewrite = false
            """.trimIndent(),
            additionalGradleCodeBlock = """
                $GRADLE_CODE_PRINT_TASK

                androidComponents {
                    onVariants(selector()) { variant ->
                        tasks.register(variant.name + "Print", PrintTask) { t ->
                            t.text.set(variant.sources.baselineProfiles?.all?.get().toString())
                        }
                    }
                }
            """.trimIndent()
        )

        arrayOf("freeRelease", "paidRelease")
            .forEach {

                // Expected src set location. Note that src sets are not added if the folder does
                // not exist so we need to create it.
                val expected =
                    File(
                        consumerProjectSetup.rootDir,
                        "src/$it/$expectedBaselineProfileOutputFolder"
                    )
                        .apply {
                            mkdirs()
                            deleteOnExit()
                        }

                gradleRunner.buildAndAssertThatOutput("${it}Print") {
                    contains(expected.absolutePath)
                }
            }
    }

    @Test
    fun testR8RewriteBaselineProfilePropertySet() {
        setupConsumerProject(
            androidPlugin = "com.android.library",
            dependencyOnProducerProject = false,
            flavors = true,
            buildTypeAnotherRelease = true,
            additionalGradleCodeBlock = """
                $GRADLE_CODE_PRINT_TASK

                androidComponents {
                    onVariants(selector()) { variant ->
                        println(variant.name)
                        tasks.register("print" + variant.name, PrintTask) { t ->
                            def prop = "android.experimental.art-profile-r8-rewriting"
                            if (prop in variant.experimentalProperties) {
                                def value = variant.experimentalProperties[prop].get().toString()
                                t.text.set( "r8-rw=" + value)
                            } else {
                                t.text.set( "r8-rw=false")
                            }
                        }
                    }
                }
            """.trimIndent()
        )

        arrayOf(
            "printFreeRelease",
            "printPaidRelease",
            "printFreeAnotherRelease",
            "printPaidAnotherRelease",
        ).forEach { gradleRunner.buildAndAssertThatOutput(it) { contains("r8-rw=false") } }
    }

    @Test
    fun testFilterAndSortAndMerge() {
        setupConsumerProject(
            androidPlugin = "com.android.library",
            flavors = true,
            baselineProfileBlock = """
                filter {
                    include("com.sample.Utils")
                }
            """.trimIndent()
        )
        setupProducerProjectWithFlavors(
            freeReleaseProfileLines = listOf(
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_1_METHOD_2,
                Fixtures.CLASS_1,
            ),
            paidReleaseProfileLines = listOf(
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_2_METHOD_2,
                Fixtures.CLASS_2_METHOD_3,
                Fixtures.CLASS_2_METHOD_4,
                Fixtures.CLASS_2_METHOD_5,
                Fixtures.CLASS_2,
            )
        )

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        // In the final output there should be :
        //  - one single file in src/main/generatedBaselineProfiles because merge = `all`.
        //  - There should be only the Utils class [CLASS_2] because of the include filter.
        //  - The method `someOtherMethod` [CLASS_2_METHOD_3] should be included only once.
        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_2_METHOD_2,
                Fixtures.CLASS_2_METHOD_3,
            )
    }

    @Test
    fun testSaveInSrcTrueAndAutomaticGenerationDuringBuildTrue() {
        setupConsumerProject(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock = """
                saveInSrc = true
                automaticGenerationDuringBuild = true
            """.trimIndent()
        )
        setupProducerProjectWithFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // Asserts that assembling release triggers generation of profile
        gradleRunner.buildAndAssertThatOutput("assembleFreeRelease", "--dry-run") {
            contains(":$consumerModuleName:mergeFreeReleaseBaselineProfile")
            contains(":$consumerModuleName:copyFreeReleaseBaselineProfileIntoSrc")
            contains(":$consumerModuleName:mergeFreeReleaseArtProfile")
            contains(":$consumerModuleName:compileFreeReleaseArtProfile")
            contains(":$consumerModuleName:assembleFreeRelease")
        }

        // Asserts that the profile is generated in the src folder
        gradleRunner.build("generateFreeReleaseBaselineProfile") {
            assertThat(readBaselineProfileFileContent("freeRelease"))
                .containsExactly(
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_1_METHOD_1,
                )
        }
    }

    @Test
    fun testSaveInSrcTrueAndAutomaticGenerationDuringBuildFalse() {
        setupConsumerProject(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock = """
                saveInSrc = true
                automaticGenerationDuringBuild = false
            """.trimIndent()
        )
        setupProducerProjectWithFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // Asserts that assembling release does not trigger generation of profile
        gradleRunner.buildAndAssertThatOutput("assembleFreeRelease", "--dry-run") {
            doesNotContain(":$consumerModuleName:mergeFreeReleaseBaselineProfile")
            doesNotContain(":$consumerModuleName:copyFreeReleaseBaselineProfileIntoSrc")
            contains(":$consumerModuleName:mergeFreeReleaseArtProfile")
            contains(":$consumerModuleName:compileFreeReleaseArtProfile")
            contains(":$consumerModuleName:assembleFreeRelease")
        }

        // Asserts that the profile is generated in the src folder
        gradleRunner.build("generateFreeReleaseBaselineProfile") {
            assertThat(readBaselineProfileFileContent("freeRelease"))
                .containsExactly(
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_1_METHOD_1,
                )
        }
    }

    @Test
    fun testSaveInSrcFalseAndAutomaticGenerationDuringBuildTrue() {
        setupConsumerProject(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock = """
                saveInSrc = false
                automaticGenerationDuringBuild = true
            """.trimIndent()
        )
        setupProducerProjectWithFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // Asserts that assembling release triggers generation of profile
        gradleRunner.buildAndAssertThatOutput("assembleFreeRelease", "--dry-run") {
            contains(":$consumerModuleName:mergeFreeReleaseBaselineProfile")
            contains(":$consumerModuleName:mergeFreeReleaseArtProfile")
            contains(":$consumerModuleName:compileFreeReleaseArtProfile")
            contains(":$consumerModuleName:assembleFreeRelease")
        }

        // Asserts that the profile is not generated in the src folder
        gradleRunner.build("generateFreeReleaseBaselineProfile") {}

        val profileFile = baselineProfileFile("freeRelease")
        assertThat(profileFile.exists()).isFalse()
    }

    @Test
    fun testSaveInSrcFalseAndAutomaticGenerationDuringBuildFalse() {
        setupProducerProject()
        setupConsumerProject(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            baselineProfileBlock = """
                saveInSrc = false
                automaticGenerationDuringBuild = false
            """.trimIndent()
        )
        gradleRunner
            .withArguments("generateReleaseBaselineProfile", "--stacktrace")
            .buildAndFail()
            .output
            .replace(System.lineSeparator(), " ")
            .also {
                assertThat(it)
                    .contains(
                        "The current configuration of flags `saveInSrc` and " +
                            "`automaticGenerationDuringBuild` is not supported"
                    )
            }
    }

    private fun setupConsumerProject(
        androidPlugin: String,
        flavors: Boolean = false,
        dependencyOnProducerProject: Boolean = true,
        buildTypeAnotherRelease: Boolean = false,
        addAppTargetPlugin: Boolean = androidPlugin == ANDROID_APPLICATION_PLUGIN,
        baselineProfileBlock: String = "",
        additionalGradleCodeBlock: String = "",
    ) {
        val flavorsBlock = """
            productFlavors {
                flavorDimensions = ["version"]
                free { dimension "version" }
                paid { dimension "version" }
            }

        """.trimIndent()

        val buildTypeAnotherReleaseBlock = """
            buildTypes {
                anotherRelease { initWith(release) }
            }

        """.trimIndent()

        val dependencyOnProducerProjectBlock = """
            dependencies {
                baselineProfile(project(":$producerModuleName"))
            }

        """.trimIndent()

        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("$androidPlugin")
                    id("androidx.baselineprofile.consumer")
                    ${if (addAppTargetPlugin) "id(\"androidx.baselineprofile.apptarget\")" else ""}
                }
                android {
                    namespace 'com.example.namespace'
                    ${if (flavors) flavorsBlock else ""}
                    ${if (buildTypeAnotherRelease) buildTypeAnotherReleaseBlock else ""}
                }

               ${if (dependencyOnProducerProject) dependencyOnProducerProjectBlock else ""}

                baselineProfile {
                    $baselineProfileBlock
                }

                $additionalGradleCodeBlock

            """.trimIndent(),
            suffix = ""
        )
    }

    private fun setupProducerProjectWithFlavors(
        freeReleaseProfileLines: List<String>,
        paidReleaseProfileLines: List<String>,
    ) {
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withProducedBaselineProfile(
                    lines = freeReleaseProfileLines,
                    flavorName = "free",
                    buildType = "release",
                    productFlavors = mapOf("version" to "free")
                )
                .withProducedBaselineProfile(
                    lines = paidReleaseProfileLines,
                    flavorName = "paid",
                    buildType = "release",
                    productFlavors = mapOf("version" to "paid")
                )
                .build(),
            suffix = ""
        )
    }

    private fun setupProducerProject(
        releaseProfile: List<String> = listOf(
            Fixtures.CLASS_1_METHOD_1,
            Fixtures.CLASS_2_METHOD_2,
            Fixtures.CLASS_2,
            Fixtures.CLASS_1
        ),
        vararg additionalReleaseProfiles: List<String>
    ) {
        val mock = MockProducerBuildGrade()
            .withProducedBaselineProfile(
                lines = releaseProfile,
                flavorName = "",
                buildType = "release",
                productFlavors = mapOf()
            )
        for (profile in additionalReleaseProfiles) {
            mock.withProducedBaselineProfile(
                lines = profile,
                flavorName = "",
                buildType = "release",
                productFlavors = mapOf()
            )
        }
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = mock.build(),
            suffix = ""
        )
    }
}

private class MockProducerBuildGrade {

    private var profileIndex = 0
    private var content = """
        plugins { id("com.android.library") }
        android { namespace 'com.example.namespace' }

        import com.android.build.api.attributes.BuildTypeAttr
        import com.android.build.api.attributes.ProductFlavorAttr
        import com.android.build.gradle.internal.attributes.VariantAttr
        import androidx.baselineprofile.gradle.attributes.BaselineProfilePluginVersionAttr

        // This task produces a file with a fixed output
        abstract class TestProfileTask extends DefaultTask {
            @Input abstract Property<String> getFileContent()
            @OutputFile abstract RegularFileProperty getOutputFile()
            @TaskAction void exec() { getOutputFile().get().asFile.write(getFileContent().get()) }
        }

    """.trimIndent()

    fun withProducedBaselineProfile(
        lines: List<String>,
        productFlavors: Map<String, String>,
        flavorName: String = "",
        buildType: String
    ): MockProducerBuildGrade {
        val productFlavorAttributes = productFlavors.map { (name, value) ->
            """
            attribute(ProductFlavorAttr.of("$name"), objects.named(ProductFlavorAttr, "$value"))

            """.trimIndent()
        }.joinToString("\n")

        content += """

        configurations {
            ${configurationName(flavorName, buildType)} {
                canBeConsumed = true
                canBeResolved = false
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, "baselineProfile"))
                    attribute(BuildTypeAttr.ATTRIBUTE, objects.named(BuildTypeAttr, "$buildType"))
                    attribute(
                        TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                        objects.named(TargetJvmEnvironment, "android")
                    )
                    attribute(
                        BaselineProfilePluginVersionAttr.ATTRIBUTE,
                        objects.named(BaselineProfilePluginVersionAttr, "alpha1")
                    )

                    $productFlavorAttributes
                }
            }
        }

        """.trimIndent()
        profileIndex++
        content += """

        def task$profileIndex = tasks.register('testProfile$profileIndex', TestProfileTask)
        task$profileIndex.configure {
            it.outputFile.set(project.layout.buildDirectory.file("test$profileIndex"))
            it.fileContent.set(${"\"\"\"${lines.joinToString("\n")}\"\"\""})
        }
        artifacts {
            add("${
            configurationName(
                flavorName,
                buildType
            )
        }", task$profileIndex.map { it.outputFile })
        }

        """.trimIndent()
        return this
    }

    fun build() = content
}

private fun configurationName(flavor: String, buildType: String): String =
    camelCase(flavor, buildType, CONFIGURATION_NAME_BASELINE_PROFILES)

object Fixtures {
    const val CLASS_1 = "Lcom/sample/Activity;"
    const val CLASS_1_METHOD_1 = "HSPLcom/sample/Activity;-><init>()V"
    const val CLASS_1_METHOD_2 = "HSPLcom/sample/Activity;->onCreate(Landroid/os/Bundle;)V"
    const val CLASS_2 = "Lcom/sample/Utils;"
    const val CLASS_2_METHOD_1 = "HSLcom/sample/Utils;-><init>()V"
    const val CLASS_2_METHOD_2 = "HLcom/sample/Utils;->someMethod()V"
    const val CLASS_2_METHOD_3 = "HLcom/sample/Utils;->someOtherMethod()V"
    const val CLASS_2_METHOD_4 = "HSLcom/sample/Utils;->someOtherMethod()V"
    const val CLASS_2_METHOD_5 = "HSPLcom/sample/Utils;->someOtherMethod()V"
}