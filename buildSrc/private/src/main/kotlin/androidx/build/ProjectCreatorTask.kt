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

// This suppression is for usage of UserInputHandler and UserQuestions (from Gradle).
// For more information, see https://github.com/gradle/gradle/issues/28216
@file:Suppress("InternalGradleApiUsage")

package androidx.build

import com.google.common.annotations.VisibleForTesting
import java.io.File
import java.time.LocalDate
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.internal.tasks.userinput.UserQuestions
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.tomlj.Toml
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable

@DisableCachingByDefault(because = "Interactive task, must run every time")
abstract class ProjectCreatorTask : DefaultTask() {
    private val supportDir = project.getSupportRootFolder()

    @TaskAction
    fun exec() {
        val spec: ProjectSpec = promptForProjectSpec()
        val catalogEditor = VersionCatalogEditor(File(supportDir, "libraryversions.toml"), spec)
        val settingsEditor = GradleSettingsEditor(File(supportDir, "settings.gradle"))
        val docsTotBuildGradleEditor =
            DocsTotBuildGradleEditor(File(supportDir, "docs-tip-of-tree/build.gradle"))
        val projectGenerator = ProjectGenerator()

        catalogEditor.updateLibraryVersionsToml()
        settingsEditor.updateSettingsGradle(spec)
        docsTotBuildGradleEditor.updateDocsTotBuildGradle(spec)
        projectGenerator.createDirectories(spec, catalogEditor.isGroupIdAtomic())

        printTodoList(spec)
    }

    private fun promptForProjectSpec(): ProjectSpec {
        // This println here is intentional, it allows any error messages from groupIdIsValid() and
        // artifactIdIsValid() to be shown right after the respective prompt. For some reason,
        // without this empty println, those printlns in the validation functions don't get flushed
        // until after the user tries the prompt for the second time.
        println()
        val userInput = services.get(UserInputHandler::class.java)

        var groupId = ""
        do {
            groupId =
                userInput
                    .askUser { interaction: UserQuestions ->
                        interaction.askQuestion(
                            "Enter group id (must start with 'androidx', e.g. androidx.core)",
                            "none",
                        )
                    }
                    .get()
        } while (!isGroupIdValid(groupId))
        var artifactId = ""
        do {
            artifactId =
                userInput
                    .askUser { interaction: UserQuestions ->
                        interaction.askQuestion("Enter artifact id (e.g. core-telecom)", "none")
                    }
                    .get()
        } while (!isArtifactIdValid(groupId, artifactId))

        val projectTypeName =
            userInput
                .askUser { interaction: UserQuestions ->
                    interaction.selectOption(
                        "Please choose the type of project you would like to create",
                        ProjectType.entries.map { it.description },
                        ProjectType.ANDROID_LIBRARY.description,
                    )
                }
                .get()
        val projectDescription =
            userInput
                .askUser { interaction: UserQuestions ->
                    interaction.askQuestion("Enter project description", "none")
                }
                .get()

        val projectType = ProjectType.entries.find { it.description == projectTypeName }

        if (projectType == null) {
            error("Unknown project type: $projectTypeName")
        }

        return ProjectSpec(groupId, artifactId, projectType, projectDescription, supportDir)
    }

    private fun printTodoList(projectSpec: ProjectSpec) {
        val buildGradlePath = projectSpec.fullArtifactPath.resolve("build.gradle")
        val ownersFilePath = projectSpec.fullArtifactPath.resolve("OWNERS")
        val packageDocsPath =
            getPackageDocumentationFileDir(projectSpec)
                .resolve(
                    getPackageDocumentationFilename(
                        projectSpec.groupIdWithPrefix,
                        projectSpec.artifactId,
                    )
                )

        println(
            """
            ---
            Created the project. The following TODOs need to be completed by you:

            1. Check that the OWNERS file is in the correct place. It is currently at:
               ${ownersFilePath.path}
            2. Add your name (and others) to the OWNERS file:
               ${ownersFilePath.path}
            3. Check that the correct library version is assigned in the build.gradle:
               ${buildGradlePath.path}
            4. Fill out the project/module name in the build.gradle:
               ${buildGradlePath.path}
            5. Update the project/module package documentation:
               ${packageDocsPath.path}
            6. Check the libraryversions.toml file:
               ${supportDir.resolve("libraryversions.toml").path}
            """
                .trimIndent()
        )
    }
}

@VisibleForTesting
internal class GradleSettingsEditor(val settingsGradleFile: File) {
    fun updateSettingsGradle(spec: ProjectSpec) {
        val settingsLines = settingsGradleFile.readLines().toMutableList()
        val newLine = getNewSettingsGradleLine(spec)

        val insertLine =
            settingsLines.indexOfFirst { it.contains("includeProject") && it > newLine }
        if (insertLine != -1) {
            settingsLines.add(insertLine, newLine)
        } else {
            settingsLines.add(newLine)
        }

        settingsGradleFile.writeText(settingsLines.joinToString("\n") + "\n")
    }

    private fun getNewSettingsGradleLine(spec: ProjectSpec): String {
        val buildType = getBuildType(spec)
        val gradlePath = getGradleProjectCoordinates(spec.groupId, spec.artifactId)
        return "includeProject(\"$gradlePath\", [BuildType.$buildType])"
    }

    private fun getBuildType(spec: ProjectSpec): String {
        return if (isComposeProject(spec.groupId, spec.artifactId)) {
            "COMPOSE"
        } else if (spec.projectType == ProjectType.KMP) {
            "KMP"
        } else {
            "MAIN"
        }
    }
}

@VisibleForTesting
internal class VersionCatalogEditor(val tomlFile: File, val spec: ProjectSpec) {

    /**
     * Checks if a group ID is atomic using the libraryversions.toml file.
     *
     * If one already exists, then this function evaluates the group id and returns the appropriate
     * atomicity. Otherwise, it returns False.
     *
     * Example of an atomic library group: ACTIVITY = { group = "androidx.work", atomicGroupVersion
     * = "WORK" } Example of a non-atomic library group: WEAR = { group = "androidx.wear" }
     */
    fun isGroupIdAtomic(): Boolean {
        val tomlParseResult: TomlParseResult = Toml.parse(tomlFile.toPath())
        val groupsTable = tomlParseResult.getTable("groups") ?: return false
        val groupEntry = groupsTable.getTable(getGroupIdVersionMacro(spec.groupId))
        return groupEntry?.contains("atomicGroupVersion") == true
    }

    fun updateLibraryVersionsToml() {
        val tomlLines = tomlFile.readLines().toMutableList()
        val tomlParseResult: TomlParseResult = Toml.parse(tomlFile.toPath())

        registerVersion(tomlLines, tomlParseResult, spec.groupId)
        registerGroup(tomlLines, tomlParseResult, spec.groupIdWithPrefix)

        tomlFile.writeText(tomlLines.joinToString("\n", postfix = "\n"))
    }

    private fun registerVersion(
        tomlLines: MutableList<String>,
        parseResult: TomlParseResult,
        groupId: String,
    ) {
        // Update [versions] section

        val groupIdVersionMacro = getGroupIdVersionMacro(groupId)

        val versionsTable: TomlTable? = parseResult.getTable("versions")
        val versionExists = versionsTable?.contains(groupIdVersionMacro) == true

        if (!versionExists) {
            val versionsBlockStart = tomlLines.indexOf("[versions]")
            val groupsBlockStart = tomlLines.indexOf("[groups]")

            val newVersionLine = "$groupIdVersionMacro = \"1.0.0-alpha01\""
            var versionInsertIndex = groupsBlockStart // Default insert point

            // Find the correct alphabetical insertion index within the [versions] block
            for (i in versionsBlockStart + 1 until groupsBlockStart) {
                val line = tomlLines[i].trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue
                if (line > newVersionLine) {
                    versionInsertIndex = i
                    break
                }
            }
            tomlLines.add(versionInsertIndex, newVersionLine)
            println("Added version entry for '$groupIdVersionMacro' in libraryversions.toml.")
        } else {
            println(
                "Version entry for '$groupIdVersionMacro' already exists in libraryversions.toml. Skipping."
            )
        }
    }

    private fun registerGroup(
        tomlLines: MutableList<String>,
        parseResult: TomlParseResult,
        groupId: String,
    ) {
        // update [groups] section

        val groupIdVersionMacro = getGroupIdVersionMacro(groupId)

        // Re-find groupsBlockStart as tomlLines might have been modified
        val newGroupsBlockStart = tomlLines.indexOf("[groups]")

        val groupsTable: TomlTable? = parseResult.getTable("groups")
        // Check if any key within [groups] has a sub-table with 'group = "$groupId"'
        val groupExists =
            groupsTable?.keySet()?.any { key ->
                val groupSpec = groupsTable.getTable(key)
                groupSpec?.getString("group") == groupId
            } == true

        if (!groupExists) {
            val newGroupLine =
                """$groupIdVersionMacro = { group = "$groupId", atomicGroupVersion = "versions.$groupIdVersionMacro" }"""
            var groupInsertIndex = tomlLines.size // Default insert at the end

            // Find the correct alphabetical insertion index within the [groups] block
            for (i in newGroupsBlockStart + 1 until tomlLines.size) {
                val line = tomlLines[i].trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue
                if (line > newGroupLine) {
                    groupInsertIndex = i
                    break
                }
            }
            tomlLines.add(groupInsertIndex, newGroupLine)
            println("Added group entry for '$groupId' in libraryversions.toml.")
        } else {
            println("Group entry for '$groupId' already exists in libraryversions.toml. Skipping.")
        }
    }
}

internal class DocsTotBuildGradleEditor(val docsTotBuildGradleFile: File) {
    fun updateDocsTotBuildGradle(spec: ProjectSpec) {
        if (
            ("test" in spec.groupId ||
                "test" in spec.artifactId ||
                "benchmark" in spec.groupId ||
                "benchmark" in spec.artifactId)
        ) {
            println(
                "Skipping docs-tip-of-tree update for test/benchmark library " +
                    "$spec.groupId:$spec.artifactId. Please add manually if needed."
            )
            return
        }

        val newLine = spec.getNewDocsTotBuildGradleLine() ?: return
        val docLines = docsTotBuildGradleFile.readLines().toMutableList()

        val dependenciesBlockStart =
            docLines.indexOfFirst { it.trim().startsWith("dependencies {") }
        if (dependenciesBlockStart == -1) {
            error("Error: Could not find 'dependencies {' block in " + docsTotBuildGradleFile.path)
        }

        val newProjectPart = newLine.split("project")[1]
        val insertLine =
            docLines.indexOfFirst {
                it.contains("project") && it.substringAfter("project") >= newProjectPart
            }

        if (insertLine != -1) {
            docLines.add(insertLine, newLine)
        } else {
            docLines.add(dependenciesBlockStart + 1, newLine)
        }

        docsTotBuildGradleFile.writeText(docLines.joinToString("\n", postfix = "\n"))
    }

    private fun ProjectSpec.getNewDocsTotBuildGradleLine(): String? {
        if ("sample" in artifactId) {
            println(
                "Auto-detected sample project. Please add the sample dependency to the " +
                    "androidx block of the library's build.gradle file."
            )
            return null
        }
        val gradlePath = getGradleProjectCoordinates(groupId, artifactId)
        return """    ${if (projectType == ProjectType.KMP) "kmpDocs" else "docs"}(project("$gradlePath"))"""
    }
}

@VisibleForTesting
internal class ProjectGenerator {
    fun createDirectories(spec: ProjectSpec, isGroupIdAtomic: Boolean) {
        spec.fullArtifactPath.mkdirs()

        // create src dir
        createSrcDir(spec)

        // create OWNERS file
        val ownersFile = File(spec.fullArtifactPath, "OWNERS")
        ownersFile.writeText("# example@google.com\n")

        // create build.gradle file
        val buildGradleFile = File(spec.fullArtifactPath, "build.gradle")
        buildGradleFile.writeText(spec.getBuildGradleText(isGroupIdAtomic))

        // Write current.txt, res-current.txt, and restricted_current.txt
        for (signatureFileName: String in listOf("current", "res-current", "restricted_current")) {
            val txtFile = File(spec.fullArtifactPath, "api/$signatureFileName.txt")
            txtFile.parentFile.mkdirs()
            txtFile.writeText(
                if (signatureFileName != "res-current") "// Signature format: 4.0\n" else ""
            )
        }
    }

    private fun createSrcDir(spec: ProjectSpec) {
        val basePath = if (spec.projectType == ProjectType.KMP) "src/commonMain" else "src/main"
        val fullPath =
            "$basePath/${spec.projectType.getLanguage()}/androidx/${
                spec.groupId.replace(
                    ".",
                    "/",
                )
            }"

        val docFile =
            File(
                spec.fullArtifactPath,
                "$fullPath/${getPackageDocumentationFilename(spec.groupId, spec.artifactId)}",
            )
        docFile.parentFile.mkdirs()
        docFile.writeText(spec.toPackageDocsText())

        if (spec.projectType == ProjectType.JAVA) {
            val packageInfoFile = File(spec.fullArtifactPath, "$fullPath/package-info.java")

            packageInfoFile.writeText(spec.getPackageInfoFileText())
        }

        if (spec.projectType == ProjectType.KMP) {
            val testFile =
                File(
                    spec.fullArtifactPath,
                    "${fullPath.replace("commonMain", "commonTest")}/Test.kt",
                )
            testFile.parentFile.mkdirs()
            testFile.writeText(spec.createTestFileText())
        }
    }

    private fun ProjectSpec.getPackageInfoFileText(): String {
        return """
            ${getAOSPHeader()}

            package androidx.${groupId}.${artifactId.removePrefix(groupId.split(".").last()).removePrefix("-").replace("-", ".")}
        """
            .trimIndent()
    }

    private fun getAOSPHeader(): String {
        return """
            /*
             * Copyright ${getYear()} The Android Open Source Project
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
        """
    }

    private fun ProjectSpec.createTestFileText(): String {
        return """
            ${getAOSPHeader()}
            package androidx.${groupId}

            class Test {
            }
        """
            .trimIndent()
    }

    private fun ProjectSpec.toPackageDocsText(): String {
        return """
            # Module root

            $groupId $artifactId

            # Package ${generatePackageName(groupId, artifactId)}

            Insert package level documentation here
        """
            .trimIndent()
    }

    private fun ProjectSpec.getBuildGradleText(isGroupIdAtomic: Boolean): String {
        return """
            ${getAOSPHeader()}

            /**
             * This file was created using the `createProject` gradle task (./gradlew createProject)
             *
             * Please use the task when creating a new project, rather than copying an existing project and
             * modifying its settings.
             */
            import androidx.build.SoftwareType
            ${if (projectType == ProjectType.KMP) "import androidx.build.PlatformIdentifier" else ""}

            plugins {
                id("AndroidXPlugin")
                ${getGradlePlugin()}
            }

            dependencies {
                // Add dependencies here
            }

            ${
            when (projectType) {
                ProjectType.KMP -> """
            androidXMultiplatform {
                ${getMultiplatformBuildGradleText()}
            }
                    """
                ProjectType.ANDROID_LIBRARY -> """
            android {
                namespace = "${generatePackageName(groupId, artifactId)}"
            }
                    """
                ProjectType.JAVA -> """
            java {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
                    """
            }
        }

            androidx {
                name = "${groupId}:${artifactId}"
                type = SoftwareType.${getLibraryType(artifactId)}
                ${if (isGroupIdAtomic) "" else "mavenVersion = LibraryVersions.${getGroupIdVersionMacro(groupId)}"}
                inceptionYear = "${getYear()}"
                description = "$description"
            }
        """
            .trimIndent()
    }

    private fun ProjectSpec.getMultiplatformBuildGradleText(): String {
        return """
            ${
            if (isComposeProject(groupId, artifactId)) {
                """
                androidLibrary {
                    namespace = "androidx.compose.${artifactId.removePrefix("compose-").replace("-", ".")}"
                    compileSdk { version = release(35) }
                }
                jvmStubs()
                linuxX64Stubs()

                defaultPlatform(PlatformIdentifier.ANDROID)
                """
            } else {
                """
                ios()
                js()
                jvm()
                linux()
                mac()
                mingwX64()
                tvos()
                wasmJs()
                watchos()

                defaultPlatform(PlatformIdentifier.JVM)
                """
            }
        }

                sourceSets {
                    commonMain.dependencies {
                    }

                    commonTest.dependencies {
                    }
                    ${if (isComposeProject(groupId, artifactId)) {
            """

                    commonStubsMain.dependsOn(commonMain)
                    jvmStubsMain.dependsOn(commonStubsMain)
                    linuxx64StubsMain.dependsOn(commonStubsMain)
                    """
        } else { "" }}
                }
            """
    }

    private fun ProjectSpec.getGradlePlugin(): String {
        if (isComposeProject(groupId, artifactId)) {
            return """id("AndroidXComposePlugin")"""
        }
        return when (this.projectType) {
            ProjectType.ANDROID_LIBRARY -> """id("com.android.library")"""
            ProjectType.KMP -> ""
            ProjectType.JAVA -> """id("java-library")"""
        }
    }

    private fun ProjectType.getLanguage(): String {
        return when (this) {
            ProjectType.ANDROID_LIBRARY -> "kotlin"
            ProjectType.KMP -> "kotlin"
            ProjectType.JAVA -> "java"
        }
    }

    private fun getYear(): String = LocalDate.now().year.toString()
}

private fun getPackageDocumentationFileDir(spec: ProjectSpec): File {
    val subPath =
        when (spec.projectType) {
            ProjectType.ANDROID_LIBRARY -> {
                "src/main/kotlin/"
            }
            ProjectType.KMP -> {
                "src/commonMain/kotlin/"
            }
            ProjectType.JAVA -> {
                "src/main/java/"
            }
        } + spec.groupIdWithPrefix.replace('.', '/')
    return File(spec.fullArtifactPath, subPath)
}

@VisibleForTesting
internal enum class ProjectType(val description: String) {
    ANDROID_LIBRARY("Android (AAR)"),
    KMP("KMP (All platforms) (AAR)"),
    JAVA("Java (JVM - JAR)"),
}

@VisibleForTesting
internal data class ProjectSpec(
    val groupIdWithPrefix: String,
    val artifactId: String,
    val projectType: ProjectType,
    val description: String,
    val supportRoot: File,
) {
    val groupId = groupIdWithPrefix.removePrefix("androidx.")

    val fullArtifactPath =
        File(supportRoot, groupId.replace('.', '/')).resolve(artifactId.removePrefix("compose-"))
}

@VisibleForTesting
internal fun isGroupIdValid(groupId: String): Boolean {
    if (!groupId.startsWith("androidx.")) {
        println("Group ID must start with 'androidx'.")
        return false
    } else if (
        listOf("compose", "wear", "xr").any { it == groupId.split(".")[1] } &&
            groupId.split(".").size == 2
    ) {
        println(
            "New ${groupId.split(".")[1]} projects must be nested inside an existing sub-project"
        )
        return false
    } else {
        return true
    }
}

@VisibleForTesting
internal fun isArtifactIdValid(groupId: String, artifactId: String): Boolean {
    val finalGroupWord = groupId.substringAfterLast('.')
    if (!artifactId.startsWith(finalGroupWord)) {
        println("Artifact ID must start with the last segment of the Group ID ($finalGroupWord).")
        return false
    }
    return true
}

private fun isComposeProject(groupId: String, artifactId: String): Boolean =
    "compose" in groupId || "compose" in artifactId

internal fun generatePackageName(groupId: String, artifactId: String): String {
    val groupLast = groupId.split('.').last()

    val suffix = artifactId.removePrefix(groupLast).replace('-', '.').trim('.')

    return if (suffix.isEmpty()) groupId else "$groupId.$suffix"
}

internal fun getGroupIdVersionMacro(groupId: String): String {
    return groupId.removePrefix("androidx.").replace(".", "_").uppercase()
}

internal fun getGradleProjectCoordinates(groupId: String, artifactId: String): String {
    return ":${groupId.removePrefix("androidx.").replace(".", ":")}:${artifactId.removePrefix("compose-")}"
}

internal fun getLibraryType(artifactId: String): String =
    when {
        "sample" in artifactId -> "SAMPLES"
        "compiler" in artifactId -> "ANNOTATION_PROCESSOR"
        "lint" in artifactId -> "LINT"
        "inspection" in artifactId -> "IDE_PLUGIN"
        else -> "PUBLISHED_LIBRARY"
    }

internal fun getPackageDocumentationFilename(groupId: String, artifactId: String): String {
    return "androidx-${groupId.replace('.', '-')}-$artifactId-documentation.md"
}
