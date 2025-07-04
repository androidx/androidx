/*
 * Copyright 2025 The Android Open Source Project
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

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.KotlinMultiplatformAndroidPlugin
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun configureRobolectric(project: Project) {
    project.plugins.configureEach { plugin ->
        when (plugin) {
            is LibraryPlugin -> {
                configureNonKmpProjects(project)
                project.extensions.getByType<LibraryAndroidComponentsExtension>().onVariants {
                    variant ->
                    variant.hostTests.forEach { (_, hostTest) ->
                        hostTest.configureTestTask { configureJvmTestTask(project, it) }
                    }
                }
            }
            is AppPlugin -> {
                configureNonKmpProjects(project)
                project.extensions.getByType<ApplicationAndroidComponentsExtension>().onVariants {
                    variant ->
                    variant.hostTests.forEach { (_, hostTest) ->
                        hostTest.configureTestTask { configureJvmTestTask(project, it) }
                    }
                }
            }
            is KotlinMultiplatformAndroidPlugin -> {
                project.extensions
                    .getByType(KotlinMultiplatformExtension::class.java)
                    .targets
                    .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                    .configureEach { androidTarget ->
                        androidTarget.compilations
                            .withType(KotlinMultiplatformAndroidHostTestCompilation::class.java)
                            .configureEach { hostTest ->
                                hostTest.isReturnDefaultValues = true
                                hostTest.isIncludeAndroidResources = true
                            }
                    }
                // TODO(b/429552116): Use variants API to configure test when bug is fixed
                project.tasks.withType(Test::class.java).configureEach { task ->
                    if (task.name == "testAndroidHostTest") {
                        configureJvmTestTask(project, task)
                    }
                }
                project.configurations.named("androidUnitTestImplementation").configure {
                    configuration ->
                    configuration.dependencies.add(project.getLibraryByName("robolectric"))
                }
            }
        }
    }
}

private fun configureNonKmpProjects(project: Project) {
    project.extensions.getByType(CommonExtension::class.java).apply {
        testOptions.unitTests.isReturnDefaultValues = true
        testOptions.unitTests.isIncludeAndroidResources = true
    }
    project.configurations.named("testImplementation").configure { configuration ->
        configuration.dependencies.add(project.getLibraryByName("robolectric"))
    }
}

private fun configureJvmTestTask(project: Project, task: Test) {
    // Robolectric 1.7 increased heap size requirements, see b/207169653.
    task.maxHeapSize = "3g"

    // For non-playground setup use robolectric offline
    if (!ProjectLayoutType.isPlayground(project)) {
        task.systemProperty("robolectric.offline", "true")
        val robolectricDependencies =
            File(
                project.getPrebuiltsRoot(),
                "androidx/external/org/robolectric/android-all-instrumented",
            )
        task.systemProperty(
            "robolectric.dependency.dir",
            robolectricDependencies.relativeTo(project.projectDir),
        )
    }

    task.jvmArgs =
        listOf(
            // https://github.com/robolectric/robolectric/issues/7456
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            // Speculative fixes for b/428257656
            "-XX:CompileCommand=quiet",
            "-XX:CompileCommand=exclude,android/icu/util/Calendar,${"$$"}robo${"$$"}android_icu_util_Calendar${"$"}createInstance",
            "-XX:CompileCommand=exclude,android/widget/FrameLayout,${"$$"}robo${"$$"}android_widget_FrameLayout${"$"}layoutChildren",
        )
}
