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

/**
 * The self-built jars that we need to successfully apply the plugin
 *
 * @param outBuildSrcPath: Might be something like $HOME/src/androidx-main/out/buildSrc
 */
class BuildJars(private val outBuildSrcPath: String) {
    private val outBuildSrc = File(outBuildSrcPath)

    private fun findJar(name: String) = outBuildSrc.resolve("$name/build/libs/$name.jar")
    val privateJar = findJar("private")
    val pluginsJar = findJar("plugins")
    private val publicJar = findJar("public")
    private val jetpadIntegrationJar = findJar("jetpad-integration")

    fun classpathEntries(): String {
        return """|// Needed for androidx extension
                  |classpath(project.files("${privateJar.path}"))
                  |
                  |// Needed for androidx/build/gradle/ExtensionsKt, among others
                  |classpath(project.files("${publicJar.path}"))
                  |
                  |// Needed to resolve plugin { id("AndroidXPlugin") }
                  |classpath(project.files("${pluginsJar.path}"))
                  |
                  |// Needed for androidx/build/jetpad/LibraryBuildInfoFile
                  |classpath(project.files("${jetpadIntegrationJar.path}"))
                  |""".trimMargin()
    }
}