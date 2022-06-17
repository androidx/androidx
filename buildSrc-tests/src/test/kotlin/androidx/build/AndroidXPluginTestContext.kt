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

import androidx.build.AndroidXPluginTestContext.Companion.wrap
import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

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

/**
 * Context for tests of the AndroidX plugins.  Entry point is [pluginTest], and the values
 *
 * @param tmpFolder: a temporary folder in which temporary output files can be safely stored, and
 *                   cleaned up after the test.
 * @param setup: Gradle project setup (see [ProjectSetupRule])
 */
data class AndroidXPluginTestContext(
    private val tmpFolder: TemporaryFolder,
    val setup: ProjectSetupRule
) {
    override fun toString() =
        mapOf("tmpFolder" to tmpFolder.root, "settings" to settingsWritten).toString()

    private val props = setup.props

    // Might be something like $HOME/src/androidx-main/out/buildSrc
    private val outBuildSrc = File(props.buildSrcOutPath)
    val privateJar = outBuildSrc.resolve("private/build/libs/private.jar")
    private val publicJar = outBuildSrc.resolve("public/build/libs/public.jar")

    val outDir: File by lazy { tmpFolder.newFolder() }

    // Gradle sometimes canonicalizes this path, so we have to or things don't match up.
    val supportRoot: File = setup.rootDir.canonicalFile

    fun runGradle(vararg args: String): BuildResult {
        // Empty environment so that the host environment does not leak through
        val env = mapOf<String, String>()
        return GradleRunner.create().withProjectDir(supportRoot)
            .withArguments(*args).withEnvironment(env).build()
    }

    private var settingsWritten: String? = null

    fun writeRootSettingsFile(vararg projectPaths: String) {
        val settingsString = buildString {
            append(
                """|pluginManagement {
                   |  repositories {
                   |    ${setup.defaultRepoLines}
                   |  }
                   |}
                   |""".trimMargin()
            )
            appendLine()
            projectPaths.forEach {
                appendLine("include(\"$it\")")
            }
        }
        settingsWritten = settingsString
        File(setup.rootDir, "settings.gradle").writeText(settingsString)
    }

    private val prebuiltsPath = supportRoot.resolve("../../prebuilts").path

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
           |
           |  dependencies {
           |    // Needed for androidx extension
           |    classpath(project.files("${privateJar.path}"))
           |
           |    // Needed for androidx/build/gradle/ExtensionsKt, among others
           |    classpath(project.files("${publicJar.path}"))
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
           |  }
           |}
           |
           |apply plugin: androidx.build.AndroidXRootImplPlugin
           |""".trimMargin()

    fun writeRootBuildFile() {
        File(setup.rootDir, "build.gradle").writeText(buildGradleText)
    }

    companion object {
        /**
         * JUnit 4 [TestRule]s are traditionally added to a test class as public JVM fields
         * with a @[org.junit.Rule] annotation.  This works decently in Java, but has drawbacks,
         * such as requiring all methods in a test class to be subject to the same [TestRule]s, and
         * making it difficult to configure [TestRule]s in different ways between test methods.
         * With lambdas, objects that have been built as [TestRule] can use this extension function
         * to allow per-method custom application.
         */
        fun <T : TestRule> T.wrap(fn: (T) -> Unit) = apply(object : Statement() {
            override fun evaluate() = fn(this@wrap)
        }, Description.EMPTY).evaluate()
    }
}