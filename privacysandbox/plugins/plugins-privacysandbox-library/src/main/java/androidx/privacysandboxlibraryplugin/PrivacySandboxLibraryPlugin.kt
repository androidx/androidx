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

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/*
 * For modules that are used by a privacy sandbox sdk module using Androidx, we need to configure
 * KSP code generation. This plugin intends to apply KSP with the required dependencies and arguments
 * such as the AIDL compiler path.
 */
abstract class PrivacySandboxLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("com.android.library")
        project.pluginManager.apply("com.google.devtools.ksp")
        project.pluginManager.withPlugin("com.google.devtools.ksp") {
            configureKsp(project)
            addSandboxDependencies(project)
        }
    }

    private fun configureKsp(project: Project) {
        val androidComponents =
            project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
        val libraryExtension = project.extensions.getByType(LibraryExtension::class.java)
        val kspExtension = project.extensions.getByType(KspExtension::class.java)

        val sdkDir = androidComponents.sdkComponents.sdkDirectory
        val aidlInputs =
            project.objects.newInstance(AidlExecutableInputs::class.java).apply {
                aidl.set(
                    sdkDir.map { sdk ->
                        sdk.dir("build-tools")
                            .dir(libraryExtension.buildToolsVersion)
                            .file(
                                if (System.getProperty("os.name").startsWith("Windows")) "aidl.exe"
                                else "aidl"
                            )
                    }
                )
                buildToolsVersion.set(aidl.map { it.asFile.parentFile.name })
            }

        val frameworkInputs =
            project.objects.newInstance(FrameworkAidlInputs::class.java).apply {
                frameworkAidl.set(androidComponents.sdkComponents.aidl.flatMap { it.framework })
                platformSdk.set(frameworkAidl.map { it.asFile.parentFile.absolutePath })
            }

        kspExtension.apply {
            arg(aidlInputs)
            arg(frameworkInputs)
        }
    }

    private fun addSandboxDependencies(project: Project) {
        val toolsVersion = "1.0.0-alpha13"
        val sdkRuntimeVersion = "1.0.0-alpha18"
        project.dependencies.apply {
            add("ksp", "androidx.privacysandbox.tools:tools-apicompiler:$toolsVersion")
            add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
            add("implementation", "androidx.privacysandbox.tools:tools:$toolsVersion")
            add(
                "implementation",
                "androidx.privacysandbox.sdkruntime:sdkruntime-core:$sdkRuntimeVersion",
            )
            add(
                "implementation",
                "androidx.privacysandbox.sdkruntime:sdkruntime-client:$sdkRuntimeVersion",
            )
            add(
                "implementation",
                "androidx.privacysandbox.sdkruntime:sdkruntime-provider:$sdkRuntimeVersion",
            )
        }
    }

    companion object {
        // From build.gradle
        const val pluginId = "androidx.privacysandbox.plugins.privacysandbox-library"
    }
}
