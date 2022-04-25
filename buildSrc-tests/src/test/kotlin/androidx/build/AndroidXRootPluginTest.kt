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
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class AndroidXRootPluginTest {
    @Test
    fun rootProjectConfigurationHasAndroidXTasks() {
        TemporaryFolder().wrap { tmpFolder ->
            ProjectSetupRule().wrap { setup ->
                val props = setup.props

                fun buildSrcFile(path: String) =
                    File(props.tipOfTreeMavenRepoPath, "../../../buildSrc/$path")

                val privateJar = buildSrcFile("private/build/libs/private.jar")
                val publicJar = buildSrcFile("public/build/libs/public.jar")

                val settingsGradleText = """
                  pluginManagement {
                    ${setup.repositories}
                  }

                  dependencyResolutionManagement {
                    versionCatalogs {
                      libs {
                        from(files("${props.rootProjectPath}/gradle/libs.versions.toml"))
                      }
                    }
                  }
                """.trimIndent()

                File(setup.rootDir, "settings.gradle").writeText(settingsGradleText)

                val buildGradleText = """
                  buildscript {
                    project.ext.outDir = file("${tmpFolder.newFolder().path}")
                    project.ext.supportRootFolder = file("${tmpFolder.newFolder().path}")

                    ${setup.repositories}

                    dependencies {
                      classpath(project.files("${privateJar.path}"))
                      classpath(project.files("${publicJar.path}"))
                      classpath '${props.agpDependency}'
                      classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:${props.kotlinVersion}'
                    }
                  }

                  apply plugin: androidx.build.AndroidXRootImplPlugin
                """.trimIndent()

                File(setup.rootDir, "build.gradle").writeText(buildGradleText)
                Assert.assertTrue(privateJar.path, privateJar.exists())
                val output = GradleRunner.create().withProjectDir(setup.rootDir)
                    .withArguments("tasks", "--stacktrace").build().output
                Assert.assertTrue(
                    output,
                    output.contains("listAndroidXProperties - Lists AndroidX-specific properties")
                )
            }
        }
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
        private fun <T : TestRule> T.wrap(fn: (T) -> Unit) = apply(object : Statement() {
            override fun evaluate() = fn(this@wrap)
        }, Description.EMPTY).evaluate()
    }
}