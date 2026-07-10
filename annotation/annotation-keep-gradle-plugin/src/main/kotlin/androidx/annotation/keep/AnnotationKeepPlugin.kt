/*
 * Copyright 2024 The Android Open Source Project
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

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

internal const val GENERATED_KEEP_RULES = "androidx.annotation.keep.rules.pro"

class AnnotationKeepPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.configureEach { plugin ->
            when (plugin) {
                // Application
                is AppPlugin -> configureAndroidApplication(project)
                // Library
                is LibraryPlugin -> configureAndroidLibrary(project)
            }
        }
    }

    private fun configureAndroidApplication(project: Project) {
        val androidComponents =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant -> variant.instrumentClasses(project = project) }
    }

    private fun configureAndroidLibrary(project: Project) {
        val libraryComponents =
            project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
        libraryComponents.onVariants { variant -> variant.instrumentClasses(project = project) }
    }

    private fun Variant.instrumentClasses(project: Project) {
        val keepRules = project.layout.buildDirectory.file("generated/$GENERATED_KEEP_RULES")
        instrumentation.transformClassesWith(
            AnnotationPluginVisitorFactory::class.java,
            InstrumentationScope.PROJECT,
        ) {
            it.keepRules = keepRules
        }
        instrumentation.setAsmFramesComputationMode(
            FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES
        )
        // For Libraries this should become consumer rules.
        this.proguardFiles.add(keepRules)
    }
}
