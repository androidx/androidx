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

package androidx.build

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import java.util.zip.ZipInputStream
import net.saff.checkmark.Checkmark.Companion.check
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.AssumptionViolatedException
import org.junit.rules.TemporaryFolder

/**
 * Main entry point for AndroidX plugin tests.
 *
 * @param action: this function will be run with the receiver being an [AndroidXPluginTestContext]
 *                 that is correctly setup before the function, and will be appropriately torn down
 *                 after.
 */
fun pluginTest(action: AndroidXPluginTestContext.() -> Unit) {
    TemporaryFolder().wrap { tmpFolder ->
        ProjectSetupRule().wrap { setup ->
            AndroidXPluginTestContext(tmpFolder, setup).action()
        }
    }
}

typealias GradleRunAction = (GradleRunner) -> BuildResult

/**
 * Context for tests of the AndroidX plugins.  Entry point is [pluginTest], and the values
 *
 * @param tmpFolder: a temporary folder in which temporary output files can be safely stored, and
 *                   cleaned up after the test.
 * @param setup: Gradle project setup (see [ProjectSetupRule])
 */
data class AndroidXPluginTestContext(val tmpFolder: TemporaryFolder, val setup: ProjectSetupRule) {
    // Default empty environment for runGradle (otherwise the host environment leaks through)
    private val defaultEnv: Map<String, String> = mapOf()

    val props = setup.props
    val buildJars = BuildJars(props.buildSrcOutPath)

    val outDir: File by lazy { tmpFolder.newFolder() }
    private val mavenLocalDir: File by lazy { tmpFolder.newFolder() }

    // Gradle sometimes canonicalizes this path, so we have to or things don't match up.
    val supportRoot: File = setup.rootDir.canonicalFile

    private val defaultBuildAction: GradleRunAction = { it.build() }

    fun runGradle(
        vararg args: String,
        env: Map<String, String> = defaultEnv,
        buildAction: GradleRunAction = defaultBuildAction
    ): BuildResult {
        try {
            return GradleRunner.create().withProjectDir(supportRoot)
                .withArguments(
                    "-Dmaven.repo.local=$mavenLocalDir",
                    "-P$ALLOW_MISSING_LINT_CHECKS_PROJECT=true",
                    *args
                )
                .withEnvironment(env).withEnvironment(env).let { buildAction(it) }.also {
                    assumeNoClassloaderErrors(it)
                }
        } catch (e: UnexpectedBuildFailure) {
            assumeNoClassloaderErrors(e.buildResult)
            throw e
        }
    }

    private fun assumeNoClassloaderErrors(result: BuildResult) {
        // We're seeing b/237103195 flakily.  When we do, let's grab additional debugging info, and
        // then throw an AssumptionViolatedException, because this is a bug in our test-running
        // infrastructure, not a bug in the underlying plugins.
        val className = "androidx.build.gradle.ExtensionsKt"
        val classNotFound = "java.lang.ClassNotFoundException: $className"

        val mpe = "groovy.lang.MissingPropertyException"
        val propertyMissing = "$mpe: Could not get unknown property 'androidx' for root project"
        val messages = listOf(classNotFound, propertyMissing)
        if (messages.any { result.output.contains(it) }) {
            buildString {
                appendLine("classloader error START")
                append(result.output)
                appendLine("classloader error END")
                appendLine("Contents of public.jar:")
                buildJars.publicJar.let { jar ->
                    ZipInputStream(jar.inputStream()).use {
                        while (true) {
                            val entry = it.nextEntry ?: return@use
                            appendLine(entry.name)
                        }
                    }
                }
            }.let {
                // Log to stderr, which we can find in the test output XML in host-test-reports,
                // so we can manually debug an instance.
                val ave = AssumptionViolatedException(it)
                ave.printStackTrace()
                throw ave
            }
        }
    }

    fun AndroidXSelfTestProject.checkConfigurationSucceeds() {
        runGradle(":$groupId:$artifactId:tasks", "--stacktrace").output.check {
            it.contains("BUILD SUCCESSFUL")
        }
    }

    private val prebuiltsPath = supportRoot.resolve("../../prebuilts").path

    val buildScriptDependencies =
        """|  dependencies {
           |    ${buildJars.classpathEntries()}
           |
           |    classpath '${props.agpDependency}'
           |    classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:${props.kotlinVersion}'
           |
           |    // These are largely duplicated with shared.gradle, but reusing shared.gradle
           |    // doesn't work because of path assumptions.
           |    classpath 'org.tomlj:tomlj:1.0.0'
           |
           |    // for sortPomDependencies
           |    classpath('org.dom4j:dom4j:2.1.3') {
           |      // Optional dependency where Ivy fails to parse the POM file.
           |      exclude(group:"net.java.dev.msv", module:"xsdlib")
           |    }
           |
           |    // Needed for ZipFile
           |    classpath('org.apache.ant:ant:1.10.11')
           |
           |    // Needed for docs-public
           |    classpath('org.jetbrains.dokka:dokka-gradle-plugin:0.9.17-g014')
           |    classpath('org.jetbrains.dokka:dokka-android-gradle-plugin:0.9.17-g014')
           |
           |    // Otherwise, comments get stripped from poms (b/230396269)
           |    classpath('xerces:xercesImpl:2.12.0')
           |  }
        """.trimMargin()

    private val buildGradleText =
        """|buildscript {
           |  // Required by AndroidXRootImplPlugin.configureRootProject
           |  project.ext.outDir = file("${outDir.path}")
           |
           |  // Required by AndroidXExtension constructor
           |  project.ext.supportRootFolder = file("${supportRoot.path}")
           |
           |  // Required by AndroidXImplPlugin.configureTestTask
           |  project.ext.prebuiltsRoot = file("$prebuiltsPath").absolutePath
           |
           |  ${setup.repositories}
           |  $buildScriptDependencies
           |}
           |
           |apply plugin: androidx.build.AndroidXRootImplPlugin
           |""".trimMargin()

    fun writeRootBuildFile() {
        File(setup.rootDir, "build.gradle").writeText(buildGradleText)
    }

    private fun makeProjectFolder(path: String): File {
        val projectFolder = setup.rootDir.resolve(path)
        projectFolder.mkdirs()
        return projectFolder
    }

    /**
     * Convenience function that can be added to a test when debugging and wanting to browse the
     * output
     */
    fun saveMavenFoldersForDebugging(where: String) {
        mavenLocalDir.copyRecursively(File(where).also {
            it.deleteRecursively()
            it.mkdirs()
        }, true)
    }

    fun AndroidXSelfTestProject.writeFiles() {
        val projectFolder = makeProjectFolder(relativePath)
        File(projectFolder, "build.gradle").writeText(buildGradleText)
    }

    fun AndroidXSelfTestProject.publishMavenLocal(vararg prefixArgs: String) {
        val args = arrayOf(*prefixArgs) + arrayOf(
            ":$groupId:$artifactId:publishToMavenLocal",
            "--stacktrace"
        )
        runGradle(*args).output.check { it.contains("BUILD SUCCESSFUL") }
    }

    fun AndroidXSelfTestProject.readPublishedFile(fileName: String) =
        mavenLocalDir.resolve("$groupId/$artifactId/$version/$fileName").assertExists().readText()

    var printBuildFileOnFailure: Boolean = false

    override fun toString(): String {
        return buildMap {
            put("root files", setup.rootDir.list().orEmpty().toList())
            if (printBuildFileOnFailure) {
                setup.rootDir.listFiles().orEmpty().filter { it.isDirectory }
                    .forEach { maybeGroupDir ->
                        maybeGroupDir.listFiles().orEmpty().filter { it.isDirectory }.forEach {
                            val maybeBuildFile = it.resolve("build.gradle")
                            if (maybeBuildFile.exists()) {
                                put(it.name + "/build.gradle", maybeBuildFile.readText())
                            }
                        }
                    }
            }
        }.toString()
    }
}