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

package androidx.privacysandboxlibraryplugin

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.dependencies

/*
* For modules that are used by a privacy sandbox sdk module using Androidx, we need to configure
* KSP code generation. This plugin intends to apply KSP with the required dependencies and arguments
* such as the AIDL compiler path.
*/
abstract class PrivacySandboxLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        var kspPluginApplied = false
        val libraryPluginId = "com.android.library"
        project.pluginManager.apply(libraryPluginId)
        project.pluginManager.withPlugin(libraryPluginId) {
            val libraryAndroidComponentsExtension =
                project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            val libraryExtension = project.extensions.getByType(LibraryExtension::class.java)
            val kspGradlePluginId = "com.google.devtools.ksp"
            project.pluginManager.apply(kspGradlePluginId)
            project.pluginManager.withPlugin(kspGradlePluginId) {
                kspPluginApplied = true
                val sdkDirectory = libraryAndroidComponentsExtension.sdkComponents.sdkDirectory
                val aidlExecutableInputs =
                    project.objects.newInstance(AidlExecutableInputs::class.java)
                val frameworkAidlInputs =
                    project.objects.newInstance(FrameworkAidlInputs::class.java)
                frameworkAidlInputs.frameworkAidl.set(
                    sdkDirectory.map {
                        it.dir("platforms")
                            .dir(libraryExtension.compileSdkVersion!!)
                            .file("framework.aidl")
                    }
                )
                frameworkAidlInputs.platformSdk.set(
                    frameworkAidlInputs.frameworkAidl.map { it.asFile.parentFile.absolutePath })
                val aidlFile = sdkDirectory.map {
                    it.dir("build-tools").dir(libraryExtension.buildToolsVersion)
                        .file(
                            if (System.getProperty("os.name").startsWith("Windows")) {
                                "aidl.exe"
                            } else {
                                "aidl"
                            }
                        )
                }
                aidlExecutableInputs.aidl.set(aidlFile)
                aidlExecutableInputs.buildToolsVersion.set(
                    aidlExecutableInputs.aidl.map { it.asFile.parentFile.name })
                val kspExtension = project.extensions.getByType(KspExtension::class.java)
                kspExtension.arg(aidlExecutableInputs)
                kspExtension.arg(frameworkAidlInputs)
            }

            // Add additional dependencies required for KSP outputs

            val toolsVersion = "1.0.0-alpha03"
            project.dependencies {
                add(
                    "ksp",
                    "androidx.privacysandbox.tools:tools-apicompiler:$toolsVersion"
                )
                add(
                    "implementation",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3"
                )
                add(
                    "implementation",
                    "androidx.privacysandbox.tools:tools:$toolsVersion"
                )
                add(
                    "implementation",
                    "androidx.privacysandbox.sdkruntime:sdkruntime-core:1.0.0-alpha01"
                )
                add(
                    "implementation",
                    "androidx.privacysandbox.sdkruntime:sdkruntime-client:1.0.0-alpha01"
                )
            }
            project.afterEvaluate {
                if (!kspPluginApplied) {
                    throw StopExecutionException(
                        """Plugin '$pluginId' was unable to apply plugin '$kspGradlePluginId'.
                            Please apply the '$kspGradlePluginId' plugin in ${project.buildFile.absolutePath}.
                            """
                    )
                }
            }
        }
    }

    companion object {
        // From build.gradle
        const val pluginId = "androidx.privacysandbox.plugins.privacysandbox-library"
    }
}