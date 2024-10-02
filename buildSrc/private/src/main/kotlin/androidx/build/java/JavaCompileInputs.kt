/*
 * Copyright 2018 The Android Open Source Project
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
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.build.java

import androidx.build.DeprecatedKotlinMultiplatformAndroidTarget
import androidx.build.getAndroidJar
import androidx.build.multiplatformExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

// JavaCompileInputs contains the information required to compile Java/Kotlin code
// This can be helpful for creating Metalava and Dokka tasks with the same settings
data class JavaCompileInputs(
    // Source files to process
    val sourcePaths: FileCollection,

    // Source files from the KMP common module of this project
    val commonModuleSourcePaths: FileCollection,

    // Dependencies (compiled classes) of [sourcePaths].
    val dependencyClasspath: FileCollection,

    // Android's boot classpath.
    val bootClasspath: FileCollection
) {
    companion object {
        // Constructs a JavaCompileInputs from a library and its variant
        fun fromLibraryVariant(variant: LibraryVariant, project: Project): JavaCompileInputs {
            val kotlinCollection = project.files(variant.sources.kotlin?.all)
            val javaCollection = project.files(variant.sources.java?.all)

            val androidJvmTarget =
                project.multiplatformExtension
                    ?.targets
                    ?.requirePlatform(KotlinPlatformType.androidJvm)
                    ?.findCompilation(compilationName = variant.name)

            val sourceCollection =
                androidJvmTarget?.let { project.files(project.sourceFiles(it)) }
                    ?: (kotlinCollection + javaCollection)

            val commonModuleSourceCollection =
                project
                    .files(androidJvmTarget?.let { project.commonModuleSourcePaths(it) })
                    .builtBy(
                        // Remove task dependency when b/332711506 is fixed, which should get us an
                        // API to get all sources (static and generated)
                        project.tasks.named("compileReleaseJavaWithJavac")
                    )

            val bootClasspath =
                project.extensions
                    .findByType(LibraryAndroidComponentsExtension::class.java)!!
                    .sdkComponents
                    .bootClasspath

            return JavaCompileInputs(
                sourcePaths = sourceCollection,
                commonModuleSourcePaths = commonModuleSourceCollection,
                dependencyClasspath = variant.compileClasspath,
                bootClasspath = project.files(bootClasspath)
            )
        }

        /**
         * Returns the JavaCompileInputs for the `jvm` target of a KMP project.
         *
         * @param project The project whose main jvm target inputs will be returned.
         */
        fun fromKmpJvmTarget(project: Project): JavaCompileInputs {
            val kmpExtension =
                checkNotNull(project.multiplatformExtension) {
                    """
                ${project.path} needs to have Kotlin Multiplatform Plugin applied to obtain its
                jvm source sets.
                """
                        .trimIndent()
                }
            val jvmTarget = kmpExtension.targets.requirePlatform(KotlinPlatformType.jvm)
            val jvmCompilation =
                jvmTarget.findCompilation(compilationName = KotlinCompilation.MAIN_COMPILATION_NAME)

            val sourceCollection = project.sourceFiles(jvmCompilation)

            val commonModuleSourcePaths = project.commonModuleSourcePaths(jvmCompilation)

            return JavaCompileInputs(
                sourcePaths = sourceCollection,
                commonModuleSourcePaths = commonModuleSourcePaths,
                dependencyClasspath =
                    jvmTarget.compilations[KotlinCompilation.MAIN_COMPILATION_NAME]
                        .compileDependencyFiles,
                bootClasspath = project.getAndroidJar()
            )
        }

        /**
         * Returns the JavaCompileInputs for the `android` target of a KMP project.
         *
         * @param project The project whose main android target inputs will be returned.
         */
        fun fromKmpAndroidTarget(project: Project): JavaCompileInputs {
            val kmpExtension =
                checkNotNull(project.multiplatformExtension) {
                    """
                ${project.path} needs to have Kotlin Multiplatform Plugin applied to obtain its
                android source sets.
                """
                        .trimIndent()
                }
            val target =
                kmpExtension.targets
                    .withType(DeprecatedKotlinMultiplatformAndroidTarget::class.java)
                    .single()
            val compilation = target.findCompilation(KotlinCompilation.MAIN_COMPILATION_NAME)
            val sourceCollection = project.sourceFiles(compilation)

            val commonModuleSourcePaths = project.commonModuleSourcePaths(compilation)

            return JavaCompileInputs(
                sourcePaths = sourceCollection,
                commonModuleSourcePaths = commonModuleSourcePaths,
                dependencyClasspath =
                    target.compilations[KotlinCompilation.MAIN_COMPILATION_NAME]
                        .compileDependencyFiles,
                bootClasspath = project.getAndroidJar()
            )
        }

        // Constructs a JavaCompileInputs from a sourceset
        fun fromSourceSet(sourceSet: SourceSet, project: Project): JavaCompileInputs {
            val sourcePaths: FileCollection =
                project.files(project.provider { sourceSet.allSource.srcDirs })
            val dependencyClasspath = sourceSet.compileClasspath
            return JavaCompileInputs(
                sourcePaths = sourcePaths,
                commonModuleSourcePaths = project.files(),
                dependencyClasspath = dependencyClasspath,
                bootClasspath = project.getAndroidJar()
            )
        }

        /**
         * Returns the list of Files (might be directories) that are included in the compilation of
         * this target.
         *
         * @param compilationName The name of the compilation. A target might have separate
         *   compilations (e.g. main vs test for jvm or debug vs release for Android)
         */
        private fun KotlinTarget.findCompilation(
            compilationName: String
        ): Provider<KotlinCompilation<*>> {
            return project.provider {
                val selectedCompilation =
                    checkNotNull(compilations.findByName(compilationName)) {
                        """
                    Cannot find $compilationName compilation configuration of $name in
                    ${project.parent}.
                    Available compilations: ${compilations.joinToString(", ") { it.name }}
                    """
                            .trimIndent()
                    }
                selectedCompilation
            }
        }

        private fun Project.sourceFiles(
            kotlinCompilation: Provider<KotlinCompilation<*>>
        ): ConfigurableFileCollection {
            return project.files(
                project.provider {
                    kotlinCompilation
                        .get()
                        .allKotlinSourceSets
                        .flatMap { it.kotlin.sourceDirectories }
                        .also {
                            require(it.isNotEmpty()) {
                                """
                                    Didn't find any source sets for $kotlinCompilation in ${project.path}.
                                    """
                                    .trimIndent()
                            }
                        }
                }
            )
        }

        private fun Project.commonModuleSourcePaths(
            kotlinCompilation: Provider<KotlinCompilation<*>>
        ): ConfigurableFileCollection {
            return project.files(
                project.provider {
                    kotlinCompilation
                        .get()
                        .allKotlinSourceSets
                        .filter { it.dependsOn.isEmpty() }
                        .flatMap { it.kotlin.sourceDirectories.files }
                }
            )
        }

        /**
         * Returns the [KotlinTarget] that targets the given platform type.
         *
         * This method will throw if there are no matching targets or there are more than 1 matching
         * target.
         */
        private fun Collection<KotlinTarget>.requirePlatform(
            expectedPlatformType: KotlinPlatformType
        ): KotlinTarget {
            return this.singleOrNull { it.platformType == expectedPlatformType }
                ?: error(
                    """
                Expected 1 and only 1 kotlin target with $expectedPlatformType. Found $size.
                Matching compilation targets:
                    ${joinToString(",") { it.name }}
                All compilation targets:
                    ${this@requirePlatform.joinToString(",") { it.name }}
                """
                        .trimIndent()
                )
        }
    }
}
