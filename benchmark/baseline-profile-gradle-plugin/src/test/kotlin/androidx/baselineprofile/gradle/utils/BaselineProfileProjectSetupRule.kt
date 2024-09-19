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

package androidx.baselineprofile.gradle.utils

import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_3_1
import androidx.testutils.gradle.ProjectSetupRule
import com.google.testing.platform.proto.api.core.LabelProto
import com.google.testing.platform.proto.api.core.PathProto
import com.google.testing.platform.proto.api.core.TestArtifactProto
import com.google.testing.platform.proto.api.core.TestResultProto
import com.google.testing.platform.proto.api.core.TestStatusProto
import com.google.testing.platform.proto.api.core.TestSuiteResultProto
import java.io.File
import java.util.Properties
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal const val ANDROID_APPLICATION_PLUGIN = "com.android.application"
internal const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
internal const val ANDROID_TEST_PLUGIN = "com.android.test"
internal const val EXPECTED_PROFILE_FOLDER = "generated/baselineProfiles"

class BaselineProfileProjectSetupRule(
    private val forceAgpVersion: String? = null,
    private val addKotlinGradlePluginToClasspath: Boolean = false
) : ExternalResource() {

    private val forcedTestAgpVersion = TestAgpVersion.fromVersionString(forceAgpVersion)

    /** Root folder for the project setup that contains 3 modules. */
    val rootFolder = TemporaryFolder().also { it.create() }

    /** Represents a module with the app target plugin applied. */
    val appTarget by lazy {
        AppTargetModule(
            rule = appTargetSetupRule,
            name = appTargetName,
        )
    }

    /** Represents a module with the consumer plugin applied. */
    val consumer by lazy {
        ConsumerModule(
            rule = consumerSetupRule,
            name = consumerName,
            producerName = producerName,
            dependencyName = dependencyName
        )
    }

    /** Represents a module with the producer plugin applied. */
    val producer by lazy {
        ProducerModule(
            rule = producerSetupRule,
            name = producerName,
            tempFolder = tempFolder,
            consumer = consumer
        )
    }

    /** Represents a simple java library dependency module. */
    val dependency by lazy { DependencyModule(name = dependencyName) }

    // Temp folder for temp generated files that need to be referenced by a module.
    private val tempFolder by lazy { File(rootFolder.root, "temp").apply { mkdirs() } }

    // Project setup rules
    private val appTargetSetupRule by lazy { ProjectSetupRule(rootFolder.root) }
    private val consumerSetupRule by lazy { ProjectSetupRule(rootFolder.root) }
    private val producerSetupRule by lazy { ProjectSetupRule(rootFolder.root) }
    private val dependencySetupRule by lazy { ProjectSetupRule(rootFolder.root) }

    // Module names (generated automatically)
    private val appTargetName: String by lazy {
        appTargetSetupRule.rootDir.relativeTo(rootFolder.root).name
    }
    private val consumerName: String by lazy {
        consumerSetupRule.rootDir.relativeTo(rootFolder.root).name
    }
    private val producerName: String by lazy {
        producerSetupRule.rootDir.relativeTo(rootFolder.root).name
    }
    private val dependencyName: String by lazy {
        dependencySetupRule.rootDir.relativeTo(rootFolder.root).name
    }

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.outerRule(appTargetSetupRule)
            .around(producerSetupRule)
            .around(dependencySetupRule)
            .around(consumerSetupRule)
            .around { b, _ -> applyInternal(b) }
            .apply(base, description)
    }

    private fun applyInternal(base: Statement) =
        object : Statement() {
            override fun evaluate() {

                // Creates the main gradle.properties
                rootFolder.newFile("gradle.properties").writer().use {
                    val props = Properties()
                    props.setProperty(
                        "org.gradle.jvmargs",
                        "-Xmx4g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g"
                    )
                    props.setProperty("android.useAndroidX", "true")
                    props.store(it, null)
                }

                // Creates the main settings.gradle
                rootFolder
                    .newFile("settings.gradle")
                    .writeText(
                        """
                include '$appTargetName'
                include '$producerName'
                include '$dependencyName'
                include '$consumerName'
            """
                            .trimIndent()
                    )

                val repositoriesBlock =
                    """
                repositories {
                    ${producerSetupRule.allRepositoryPaths.joinToString("\n") { """ maven { url "$it" } """ }}
                }
            """
                        .trimIndent()

                val agpDependency =
                    if (forceAgpVersion == null) {
                        """"${appTargetSetupRule.props.agpDependency}""""
                    } else {
                        """
                    ("com.android.tools.build:gradle") { version { strictly "$forceAgpVersion" } }
                    """
                            .trimIndent()
                    }

                val kotlinGradlePluginDependency =
                    if (addKotlinGradlePluginToClasspath) {
                        """ "${appTargetSetupRule.props.kgpDependency}" """
                    } else {
                        null
                    }

                rootFolder
                    .newFile("build.gradle")
                    .writeText(
                        """
                buildscript {
                    $repositoriesBlock
                    dependencies {

                        // Specifies agp dependency
                        ${
                            listOfNotNull(
                                agpDependency,
                                kotlinGradlePluginDependency,
                            ).joinToString("\n") { "classpath $it".trim() }
                        }

                        // Specifies plugin dependency
                        ${
                            listOf(
                                "consumer",
                                "producer",
                                "apptarget",
                            ).joinToString(separator = System.lineSeparator()) {
                                """
            classpath "androidx.baselineprofile.$it:androidx.baselineprofile.$it.gradle.plugin:+"
                                """.trimIndent()
                            }
                        }
                    }
                }

                allprojects {
                    $repositoriesBlock
                }

            """
                            .trimIndent()
                    )

                // Copies test project data
                mapOf(
                        "app-target" to appTargetSetupRule,
                        "consumer" to consumerSetupRule,
                        "producer" to producerSetupRule,
                        "dependency" to dependencySetupRule,
                    )
                    .forEach { (folder, project) ->
                        File("src/test/test-data", folder)
                            .apply { deleteOnExit() }
                            .copyRecursively(project.rootDir, overwrite = true)
                    }

                base.evaluate()
            }
        }

    fun baselineProfileFile(variantName: String): File {
        // Warning: support for baseline profile source sets in library module was added with
        // agp 8.3.0 alpha 15 (b/309858620). Therefore, before then, we can only always merge into
        // main and always output only in src/main/baseline-prof.txt.
        return if (
            consumer.isLibraryModule == false ||
                (consumer.isLibraryModule == true &&
                    forcedTestAgpVersion.isAtLeast(TEST_AGP_VERSION_8_3_1))
        ) {
            File(consumer.rootDir, "src/$variantName/$EXPECTED_PROFILE_FOLDER/baseline-prof.txt")
        } else if (consumer.isLibraryModule == true /* and version is not at least AGP 8.3.0 */) {
            if (variantName != "main") {
                throw IllegalArgumentException(
                    """
                    Invalid variant name `$variantName` for library pre-agp 8.3.0. Only main is supported.
                """
                        .trimIndent()
                )
            }
            File(consumer.rootDir, "src/main/baseline-prof.txt")
        } else {
            // This happens only when trying to read the baseline profile file before defining
            // the consumer type (library or app).
            throw IllegalStateException("Consumer is nether a library or app.")
        }
    }

    fun startupProfileFile(variantName: String) =
        File(consumer.rootDir, "src/$variantName/$EXPECTED_PROFILE_FOLDER/startup-prof.txt")

    fun mergedArtProfile(variantName: String): File {
        // Task name folder in path was first observed in the update to AGP 8.3.0-alpha10.
        // Before that, the folder was omitted in path.
        val taskNameFolder =
            if (forcedTestAgpVersion.isAtLeast(TEST_AGP_VERSION_8_3_1)) {
                camelCase("merge", variantName, "artProfile")
            } else {
                ""
            }
        return File(
            consumer.rootDir,
            "build/intermediates/merged_art_profile/$variantName/$taskNameFolder/baseline-prof.txt"
        )
    }

    fun readBaselineProfileFileContent(variantName: String): List<String> =
        baselineProfileFile(variantName).readLines()

    fun readStartupProfileFileContent(variantName: String): List<String> =
        startupProfileFile(variantName).readLines()
}

data class VariantProfile(
    val flavorDimensions: Map<String, String>,
    val buildType: String,
    val profileFileLines: Map<String, List<String>>,
    val startupFileLines: Map<String, List<String>>,
    val ftlFileLines: Map<String, List<String>> = mapOf(),
) {

    companion object {

        fun release(
            baselineProfileLines: List<String> = listOf(),
            startupProfileLines: List<String> = listOf(),
            ftlFileLines: List<String> = listOf(),
        ) =
            listOf(
                VariantProfile(
                    flavorDimensions = mapOf(),
                    buildType = "release",
                    profileFileLines = mapOf("myTest" to baselineProfileLines),
                    startupFileLines = mapOf("myStartupTest" to startupProfileLines),
                    ftlFileLines = mapOf("anotherTest" to ftlFileLines),
                )
            )
    }

    val nonMinifiedVariant =
        camelCase(*flavorDimensions.map { it.value }.toTypedArray(), "nonMinified", buildType)

    constructor(
        flavor: String?,
        buildType: String = "release",
        profileFileLines: Map<String, List<String>> = mapOf(),
        startupFileLines: Map<String, List<String>> = mapOf(),
        ftlFileLines: Map<String, List<String>> = mapOf(),
    ) : this(
        flavorDimensions = if (flavor != null) mapOf("version" to flavor) else mapOf(),
        buildType = buildType,
        profileFileLines = profileFileLines,
        startupFileLines = startupFileLines,
        ftlFileLines = ftlFileLines
    )
}

interface Module {

    val name: String
    val rule: ProjectSetupRule
    val rootDir: File
        get() = rule.rootDir

    val gradleRunner: GradleRunner
        get() = GradleRunner.create().withProjectDir(rule.rootDir)

    fun setBuildGradle(buildGradleContent: String) =
        rule.writeDefaultBuildGradle(
            prefix = buildGradleContent,
            suffix =
                """
                $GRADLE_CODE_PRINT_TASK
            """
                    .trimIndent()
        )
}

class DependencyModule(
    val name: String,
)

class AppTargetModule(
    override val rule: ProjectSetupRule,
    override val name: String,
) : Module {

    fun setup(
        buildGradleContent: String =
            """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile.apptarget")
                }
                android {
                    namespace 'com.example.namespace'
                }
            """
                .trimIndent()
    ) {
        setBuildGradle(buildGradleContent)
    }
}

class ProducerModule(
    override val rule: ProjectSetupRule,
    override val name: String,
    private val tempFolder: File,
    private val consumer: Module,
) : Module {

    fun setupWithFreeAndPaidFlavors(
        freeReleaseProfileLines: List<String>? = null,
        paidReleaseProfileLines: List<String>? = null,
        freeAnotherReleaseProfileLines: List<String>? = null,
        paidAnotherReleaseProfileLines: List<String>? = null,
        freeReleaseStartupProfileLines: List<String> = listOf(),
        paidReleaseStartupProfileLines: List<String> = listOf(),
        freeAnotherReleaseStartupProfileLines: List<String> = listOf(),
        paidAnotherReleaseStartupProfileLines: List<String> = listOf(),
        otherPluginsBlock: String = "",
    ) {
        val variantProfiles = mutableListOf<VariantProfile>()

        fun addProfile(
            flavor: String,
            buildType: String,
            profile: List<String>?,
            startupProfile: List<String>,
        ) {
            if (profile != null) {
                variantProfiles.add(
                    VariantProfile(
                        flavor = flavor,
                        buildType = buildType,
                        profileFileLines = mapOf("my-$flavor-$buildType-profile" to profile),
                        startupFileLines =
                            mapOf("my-$flavor-$buildType-startup=profile" to startupProfile)
                    )
                )
            }
        }

        addProfile(
            flavor = "free",
            buildType = "release",
            profile = freeReleaseProfileLines,
            startupProfile = freeReleaseStartupProfileLines
        )
        addProfile(
            flavor = "free",
            buildType = "anotherRelease",
            profile = freeAnotherReleaseProfileLines,
            startupProfile = freeAnotherReleaseStartupProfileLines
        )
        addProfile(
            flavor = "paid",
            buildType = "release",
            profile = paidReleaseProfileLines,
            startupProfile = paidReleaseStartupProfileLines
        )
        addProfile(
            flavor = "paid",
            buildType = "anotherRelease",
            profile = paidAnotherReleaseProfileLines,
            startupProfile = paidAnotherReleaseStartupProfileLines
        )

        setup(
            variantProfiles = variantProfiles,
            otherPluginsBlock = otherPluginsBlock,
        )
    }

    fun setupWithoutFlavors(
        releaseProfileLines: List<String> = listOf(),
        releaseStartupProfileLines: List<String> = listOf(),
        otherPluginsBlock: String = "",
    ) {
        setup(
            variantProfiles =
                listOf(
                    VariantProfile(
                        flavor = null,
                        buildType = "release",
                        profileFileLines = mapOf("myTest" to releaseProfileLines),
                        startupFileLines = mapOf("myStartupTest" to releaseStartupProfileLines)
                    )
                ),
            otherPluginsBlock = otherPluginsBlock,
        )
    }

    fun setup(
        variantProfiles: List<VariantProfile> =
            listOf(
                VariantProfile(
                    flavor = null,
                    buildType = "release",
                    profileFileLines =
                        mapOf(
                            "myTest" to
                                listOf(
                                    Fixtures.CLASS_1_METHOD_1,
                                    Fixtures.CLASS_2_METHOD_2,
                                    Fixtures.CLASS_2,
                                    Fixtures.CLASS_1
                                )
                        ),
                    startupFileLines =
                        mapOf(
                            "myStartupTest" to
                                listOf(
                                    Fixtures.CLASS_3_METHOD_1,
                                    Fixtures.CLASS_4_METHOD_1,
                                    Fixtures.CLASS_3,
                                    Fixtures.CLASS_4
                                )
                        ),
                )
            ),
        otherPluginsBlock: String = "",
        baselineProfileBlock: String = "",
        additionalGradleCodeBlock: String = "",
        targetProject: Module = consumer,
        managedDevices: List<String> = listOf(),
        namespace: String = "com.example.namespace.test",
    ) {
        val managedDevicesBlock =
            if (managedDevices.isEmpty()) ""
            else
                """
            testOptions.managedDevices.devices {
            ${
                managedDevices.joinToString("\n") {
                    """
                $it(ManagedVirtualDevice) {
                    device = "Pixel 6"
                    apiLevel = 31
                    systemImageSource = "aosp"
                }

            """.trimIndent()
                }
            }
            }
        """
                    .trimIndent()

        val flavors = variantProfiles.flatMap { it.flavorDimensions.toList() }
        val flavorDimensionNames = flavors.map { it.first }.toSet().joinToString { """ "$it"""" }
        val flavorBlocks =
            flavors
                .groupBy { it.second }
                .toList()
                .map { it.second }
                .flatten()
                .joinToString("\n") { """ ${it.second} { dimension "${it.first}" } """ }
        val flavorsBlock =
            """
            productFlavors {
                flavorDimensions = [$flavorDimensionNames]
                $flavorBlocks
            }
        """
                .trimIndent()

        val buildTypesBlock =
            """
            buildTypes {
                ${
                variantProfiles
                    .filter { it.buildType.isNotBlank() && it.buildType != "release" }
                    .joinToString("\n") { " ${it.buildType} { initWith(debug) } " }
            }
            }
        """
                .trimIndent()

        val disableConnectedAndroidTestsBlock =
            variantProfiles.joinToString("\n") {

                // Creates a folder to use as results dir
                val variantOutputDir = File(tempFolder, it.nonMinifiedVariant)
                val testResultsOutputDir =
                    File(variantOutputDir, "testResultsOutDir").apply { mkdirs() }
                val profilesOutputDir =
                    File(variantOutputDir, "profilesOutputDir").apply { mkdirs() }

                // Writes the fake test result proto in it, with the given lines
                writeFakeTestResultsProto(
                    testResultsOutputDir = testResultsOutputDir,
                    profilesOutputDir = profilesOutputDir,
                    profileFileLines = it.profileFileLines,
                    startupFileLines = it.startupFileLines,
                    ftlProfileLines = it.ftlFileLines,
                )

                // Gradle script to injects a fake and disable the actual task execution for
                // android test
                """
            afterEvaluate {
                project.tasks.named("connected${it.nonMinifiedVariant.capitalized()}AndroidTest") {
                    it.resultsDir.set(new File("${testResultsOutputDir.absolutePath}"))
                    onlyIf { false }
                }
            }

                """
                    .trimIndent()
            }

        setBuildGradle(
            """
                import com.android.build.api.dsl.ManagedVirtualDevice

                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofile.producer")
                    $otherPluginsBlock
                }

                android {
                    $flavorsBlock

                    $buildTypesBlock

                    $managedDevicesBlock

                    namespace "${namespace.trim()}"
                    targetProjectPath = ":${targetProject.name}"
                }

                dependencies {
                }

                baselineProfile {
                    $baselineProfileBlock
                }

                $disableConnectedAndroidTestsBlock

                $additionalGradleCodeBlock

            """
                .trimIndent()
        )
    }

    private fun writeFakeTestResultsProto(
        testResultsOutputDir: File,
        profilesOutputDir: File,
        profileFileLines: Map<String, List<String>>,
        startupFileLines: Map<String, List<String>>,
        ftlProfileLines: Map<String, List<String>>,
    ) {
        // This function writes a profile file for each key of the map, containing for lines
        // the strings in the list in the value.
        fun buildProfileArtifact(
            testNameToProfileLines: Map<String, List<String>>,
            fileNamePart: String,
            label: String,
        ) =
            testNameToProfileLines.map {

                // Write the fake profile with the given list of profile rules.
                val profileFileName = "fake-$fileNamePart-${it.key}.txt"
                val fakeProfileFile =
                    File(profilesOutputDir, profileFileName).apply {
                        writeText(it.value.joinToString(System.lineSeparator()))
                    }

                // Creates an artifact for the test result proto. Note that this can be used
                // both as a test result artifact and a global artifact.
                TestArtifactProto.Artifact.newBuilder()
                    .setLabel(LabelProto.Label.newBuilder().setLabel(label).build())
                    .setSourcePath(
                        PathProto.Path.newBuilder().setPath(fakeProfileFile.absolutePath).build()
                    )
                    .build()
            }

        // Baseline and startup profiles are added as test results artifacts.
        // For testing with FTL instead, we add the profile as global artifact.
        val testSuiteResultProto =
            TestSuiteResultProto.TestSuiteResult.newBuilder()
                .setTestStatus(TestStatusProto.TestStatus.PASSED)
                .addTestResult(
                    TestResultProto.TestResult.newBuilder()
                        .addAllOutputArtifact(
                            buildProfileArtifact(
                                testNameToProfileLines = profileFileLines,
                                fileNamePart = "baseline-prof",
                                label = "additionaltestoutput.benchmark.trace"
                            )
                        )
                        .addAllOutputArtifact(
                            buildProfileArtifact(
                                testNameToProfileLines = startupFileLines,
                                fileNamePart = "startup-prof",
                                label = "additionaltestoutput.benchmark.trace"
                            )
                        )
                        .build()
                )
                .addAllOutputArtifact(
                    buildProfileArtifact(
                        testNameToProfileLines = ftlProfileLines,
                        fileNamePart = "baseline-prof",
                        label = "firebase.toolOutput"
                    )
                )
                .build()

        File(testResultsOutputDir, "test-result.pb").apply {
            outputStream().use { testSuiteResultProto.writeTo(it) }
        }
    }
}

class ConsumerModule(
    override val rule: ProjectSetupRule,
    override val name: String,
    private val producerName: String,
    private val dependencyName: String,
) : Module {

    var isLibraryModule: Boolean? = null

    fun setup(
        androidPlugin: String,
        flavors: Boolean = false,
        dependenciesBlock: String =
            """
            implementation(project(":$dependencyName"))
        """
                .trimIndent(),
        dependencyOnProducerProject: Boolean = true,
        buildTypeAnotherRelease: Boolean = false,
        addAppTargetPlugin: Boolean = androidPlugin == ANDROID_APPLICATION_PLUGIN,
        baselineProfileBlock: String = "",
        additionalGradleCodeBlock: String = "",
    ) =
        setupWithBlocks(
            androidPlugin = androidPlugin,
            otherPluginsBlock = "",
            flavorsBlock =
                if (flavors)
                    """
                flavorDimensions = ["version"]
                free { dimension "version" }
                paid { dimension "version" }
            """
                        .trimIndent()
                else "",
            dependencyOnProducerProject = dependencyOnProducerProject,
            dependenciesBlock = dependenciesBlock,
            buildTypesBlock =
                if (buildTypeAnotherRelease)
                    """
                anotherRelease { initWith(release) }
        """
                        .trimIndent()
                else "",
            addAppTargetPlugin = addAppTargetPlugin,
            baselineProfileBlock = baselineProfileBlock,
            additionalGradleCodeBlock = additionalGradleCodeBlock
        )

    fun setupWithBlocks(
        androidPlugin: String,
        otherPluginsBlock: String = "",
        flavorsBlock: String = "",
        buildTypesBlock: String = "",
        dependenciesBlock: String = "",
        dependencyOnProducerProject: Boolean = true,
        addAppTargetPlugin: Boolean = androidPlugin == ANDROID_APPLICATION_PLUGIN,
        baselineProfileBlock: String = "",
        additionalGradleCodeBlock: String = "",
    ) {
        isLibraryModule = androidPlugin == ANDROID_LIBRARY_PLUGIN
        setBuildGradle(
            """
                plugins {
                    id("$androidPlugin")
                    ${if (addAppTargetPlugin) "id(\"androidx.baselineprofile.apptarget\")" else ""}
                    id("androidx.baselineprofile.consumer")
                    $otherPluginsBlock
                }
                android {
                    namespace 'com.example.namespace'
                    ${
                """
                    productFlavors {
                        $flavorsBlock
                    }
                    """.trimIndent()
            }
                    ${
                """
                    buildTypes {
                        $buildTypesBlock
                    }
                    """.trimIndent()
            }
                }

                dependencies {
                    ${if (dependencyOnProducerProject) """baselineProfile(project(":$producerName"))""" else ""}
                    $dependenciesBlock

                }

                baselineProfile {
                    $baselineProfileBlock
                }

                $additionalGradleCodeBlock

            """
                .trimIndent()
        )
    }
}
