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

package androidx.annotation.keep

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import org.gradle.testkit.runner.GradleRunner

internal val ProjectSetupRule.annoKeepVersion: String
    get() = getLibraryLatestVersionInLocalRepo("androidx/annotation/annotation-keep")

internal val ProjectSetupRule.resolvers: String
    get() =
        allRepositoryPaths.joinToString(separator = "\n") {
            """
                |maven {
                |  url "$it"
                |}
            """
                .trimMargin()
        }

internal val ProjectSetupRule.repositoriesBlock: String
    get() =
        """
        |repositories {
        |  $resolvers
        |}
        """
            .trimMargin()

// TODO(b/557395311): Avoid withPluginClassPath() and test against well known AGP versions.
@Suppress("WithPluginClasspathUsage")
internal fun ProjectSetupRule.createRunner(vararg args: String): GradleRunner =
    GradleRunner.create().withProjectDir(rootDir).withPluginClasspath().withArguments(*args)

internal fun ProjectSetupRule.setupAndroidLibrary(
    namespace: String = "androidx.keep.annotation.plugin.example",
    projectName: String = "android-library-keep-example",
) {
    val buildScript =
        """
        |plugins {
        |  id("com.android.library")
        |  id("androidx.annotation.keep")
        |}
        |
        |$repositoriesBlock
        |
        |$androidProject
        |
        |dependencies {
        |  implementation "androidx.annotation:annotation-keep:$annoKeepVersion"
        |}
        |
        |android {
        |  namespace = "$namespace"
        |}
        """
            .trimMargin()
    File(rootDir, "build.gradle").writeText(buildScript)
    File(rootDir, "settings.gradle").writeText("rootProject.name = '$projectName'")
}

internal fun ProjectSetupRule.setupAndroidApplication(
    namespace: String = "androidx.keep.annotation.plugin.app.example",
    projectName: String = "android-app-keep-example",
) {
    val buildScript =
        """
        |plugins {
        |  id("com.android.application")
        |  id("androidx.annotation.keep")
        |}
        |
        |$repositoriesBlock
        |
        |$androidProject
        |
        |dependencies {
        |  implementation "androidx.annotation:annotation-keep:$annoKeepVersion"
        |}
        |
        |android {
        |  namespace = "$namespace"
        |}
        """
            .trimMargin()
    File(rootDir, "build.gradle").writeText(buildScript)
    File(rootDir, "settings.gradle").writeText("rootProject.name = '$projectName'")
    val manifestDir = File(rootDir, "src/main")
    manifestDir.mkdirs()
    File(manifestDir, "AndroidManifest.xml")
        .writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application />
            </manifest>
            """
                .trimIndent()
        )
}

internal fun ProjectSetupRule.setupJavaLibrary(
    withKotlin: Boolean = false,
    projectName: String = "java-keep-example",
    additionalBuildScript: String = "",
) {
    val kotlinPlugin = if (withKotlin) "  id(\"org.jetbrains.kotlin.jvm\")\n" else ""
    val buildScript =
        """
        |plugins {
        |$kotlinPlugin  id("java-library")
        |  id("androidx.annotation.keep")
        |}
        |
        |$repositoriesBlock
        |
        |dependencies {
        |  implementation "androidx.annotation:annotation-keep:$annoKeepVersion"
        |}
        |
        |$additionalBuildScript
        """
            .trimMargin()
    File(rootDir, "build.gradle").writeText(buildScript)
    File(rootDir, "settings.gradle").writeText("rootProject.name = '$projectName'")
}
