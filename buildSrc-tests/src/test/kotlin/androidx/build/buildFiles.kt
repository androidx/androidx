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

import java.io.File

fun AndroidXPluginTestContext.writeBuildFiles(vararg projects: AndroidXSelfTestProject) {
    writeBuildFiles(projects.toList())
}

fun AndroidXPluginTestContext.writeBuildFiles(projects: List<AndroidXSelfTestProject>) {
    writeRootSettingsFile(projects.map { it.gradlePath })
    writeRootBuildFile()

    File(supportRoot, "libraryversions.toml").writeText(
        """|[groups]
               |[versions]
               |""".trimMargin()
    )

    // Matches behavior of root properties
    File(supportRoot, "gradle.properties").writeText(
        """|# Do not automatically include stdlib
               |kotlin.stdlib.default.dependency=false
               |
               |# Avoid OOM in subgradle
               |# (https://github.com/gradle/gradle/issues/10527#issuecomment-887704062)
               |org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g
               |""".trimMargin()
    )

    projects.forEach { it.writeFiles() }
}

fun AndroidXPluginTestContext.writeRootSettingsFile(projectPaths: List<String>) {
    val settingsString = buildString {
        append(
            """|pluginManagement {
                   |  ${setup.repositories}
                   |}
                   |""".trimMargin()
        )
        appendLine()
        projectPaths.forEach {
            appendLine("include(\"$it\")")
        }
    }
    File(setup.rootDir, "settings.gradle").writeText(settingsString)
}