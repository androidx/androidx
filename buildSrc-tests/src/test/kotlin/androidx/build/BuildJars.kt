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

    val privateJar = outBuildSrc.resolve("private/build/libs/private.jar")
    private val publicJar = outBuildSrc.resolve("public/build/libs/public.jar")
    private val jetpadIntegrationJar =
        outBuildSrc.resolve("jetpad-integration/build/libs/jetpad-integration.jar")

    fun classpathEntries(): String {
        return """|// Needed for androidx extension
                  |classpath(project.files("${privateJar.path}"))
                  |
                  |// Needed for androidx/build/gradle/ExtensionsKt, among others
                  |classpath(project.files("${publicJar.path}"))
                  |
                  |// Needed for androidx/build/jetpad/LibraryBuildInfoFile
                  |classpath(project.files("${jetpadIntegrationJar.path}"))
                  |""".trimMargin()
    }
}