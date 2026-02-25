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

package androidx.build.checkapi

import androidx.build.AndroidXExtension
import androidx.build.getAndroidJar
import androidx.build.multiplatformExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.listProperty
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.utils.addToStdlib.foldMap

/**
 * [CompilationInputs] contains the information required to compile Java/Kotlin code. This can be
 * helpful for creating Metalava and Kzip tasks with the same settings.
 *
 * There are two implementations: [StandardCompilationInputs] for non-multiplatform projects and
 * [MultiplatformCompilationInputs] for multiplatform projects.
 */
internal sealed interface CompilationInputs {
    /** Source files to process */
    val sourcePaths: FileCollection

    /** Dependencies (compiled classes) of [sourcePaths]. */
    val dependencyClasspath: FileCollection

    /** Android's boot classpath. */
    val bootClasspath: FileCollection

    companion object {
        /** Constructs a [CompilationInputs] from a library and its variant */
        fun fromLibraryVariant(variant: LibraryVariant, project: Project): CompilationInputs {
            // The boot classpath is common to both multiplatform and standard configurations.
            val bootClasspath =
                project.files(
                    project.extensions
                        .findByType(LibraryAndroidComponentsExtension::class.java)!!
                        .sdkComponents
                        .bootClasspath
                )

            // Not a multiplatform project, set up standard inputs
            val kotlinCollection = project.files(variant.sources.kotlin?.all)
            val javaCollection = project.files(variant.sources.java?.all)
            val sourceCollection = kotlinCollection + javaCollection

            @Suppress("UnstableApiUsage") // Usage of compileClasspath
            return StandardCompilationInputs(
                sourcePaths = sourceCollection,
                dependencyClasspath = variant.compileClasspath,
                bootClasspath = bootClasspath,
            )
        }

        /**
         * Returns the CompilationInputs for the `jvm` target of a KMP project.
         *
         * @param project The project whose main jvm target inputs will be returned.
         */
        fun fromKmpJvmTarget(project: Project): CompilationInputs {
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

            return MultiplatformCompilationInputs.fromCompilation(
                project = project,
                kmpExtension = kmpExtension,
                mainCompilationProvider = jvmCompilation,
                bootClasspath = project.getAndroidJar(),
            )
        }

        /**
         * Returns the CompilationInputs for the `android` target of a KMP project.
         *
         * @param project The project whose main android target inputs will be returned.
         */
        fun fromKmpAndroidTarget(project: Project): CompilationInputs {
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
                    .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                    .single()
            val compilation = target.findCompilation(KotlinCompilation.MAIN_COMPILATION_NAME)

            val bootClasspath =
                project.files(
                    project.extensions
                        .findByType(KotlinMultiplatformAndroidComponentsExtension::class.java)!!
                        .sdkComponents
                        .bootClasspath
                )
            return MultiplatformCompilationInputs.fromCompilation(
                project = project,
                kmpExtension = kmpExtension,
                mainCompilationProvider = compilation,
                bootClasspath = bootClasspath,
            )
        }

        /** Constructs a [CompilationInputs] from a sourceset */
        fun fromSourceSet(sourceSet: SourceSet, project: Project): CompilationInputs {
            val sourcePaths: FileCollection =
                project.files(project.provider { sourceSet.allSource.srcDirs })
            val dependencyClasspath = sourceSet.compileClasspath
            return StandardCompilationInputs(
                sourcePaths = sourcePaths,
                dependencyClasspath = dependencyClasspath,
                bootClasspath = project.getAndroidJar(),
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
                    ${project.path}.
                    Available compilations: ${compilations.joinToString(", ") { it.name }}
                    """
                            .trimIndent()
                    }
                selectedCompilation
            }
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

/** Compile inputs for a regular (non-multiplatform) project */
internal data class StandardCompilationInputs(
    override val sourcePaths: FileCollection,
    override val dependencyClasspath: FileCollection,
    override val bootClasspath: FileCollection,
) : CompilationInputs

/** Compile inputs for a single source set from a multiplatform project. */
internal data class SourceSetInputs(
    /** Name of the source set, e.g. "androidMain" */
    val sourceSetName: String,
    /** Names of other source sets that this one depends on */
    val dependsOnSourceSets: List<String>,
    /** Source files of this source set */
    val sourcePaths: FileCollection,
    /** Compile dependencies for this source set */
    val dependencyClasspath: FileCollection,
    /** The platforms which this source set can be a part of a compilation for. */
    val kotlinPlatforms: Set<KotlinPlatformType>,
)

/** Inputs for a single compilation of a multiplatform project (just the android or jvm target) */
internal class MultiplatformCompilationInputs(
    project: Project,
    /**
     * The [SourceSetInputs] for this project's source sets. This is a [Provider] because not all
     * relationships between source sets will be loaded at configuration time.
     */
    val sourceSets: Provider<List<SourceSetInputs>>,
    // Classpath for the android or jvm compilation.
    override val dependencyClasspath: FileCollection,
    override val bootClasspath: FileCollection,
    // Source paths for all files involved in the android or jvm compilation.
    override val sourcePaths: ConfigurableFileCollection,
) : CompilationInputs {
    /**
     * Dependencies aggregated from all compilations (the [dependencyClasspath] only includes the
     * main jvm or android compilation).
     */
    val allSourceSetsDependencyClasspath =
        project.files(sourceSets.map { it.map { sourceSet -> sourceSet.dependencyClasspath } })

    /** Source files from the KMP common module of this project */
    val commonModuleSourcePaths: FileCollection =
        project.files(
            sourceSets.map {
                it.filter { sourceSet -> sourceSet.dependsOnSourceSets.isEmpty() }
                    .map { sourceSet -> sourceSet.sourcePaths }
            }
        )

    companion object {
        /**
         * Creates inputs based on a multiplatform project.
         *
         * The [mainCompilationProvider] is used for the
         * [MultiplatformCompilationInputs.dependencyClasspath] and
         * [MultiplatformCompilationInputs.sourcePaths], but all compilations from the
         * [kmpExtension] are included in the [MultiplatformCompilationInputs.sourceSets].
         */
        fun fromCompilation(
            project: Project,
            kmpExtension: KotlinMultiplatformExtension,
            mainCompilationProvider: Provider<KotlinCompilation<*>>,
            bootClasspath: FileCollection,
        ): MultiplatformCompilationInputs {
            // Find the sources and dependencies just from the main compilation.
            val compileDependencies = mainCompilationProvider.map { it.compileDependencyFiles }
            val sourcePaths =
                project.files(
                    mainCompilationProvider.map { compilation ->
                        compilation.allKotlinSourceSets.map { sourceSet ->
                            sourceSet.kotlin.sourceDirectories
                        }
                    }
                )

            // List all main compilations.
            val allCompilations = project.objects.listProperty<KotlinCompilation<*>>()
            kmpExtension.targets.configureEach { target ->
                val mainCompilation =
                    target.compilations.named(KotlinCompilation.MAIN_COMPILATION_NAME)
                allCompilations.add(mainCompilation)
            }

            val isIsolatedProjectsEnabled =
                project.extensions.getByType<AndroidXExtension>().isIsolatedProjectsEnabled()
            // Only include main source sets, not test.
            val allKotlinSourceSets = project.objects.listProperty<KotlinSourceSet>()
            kmpExtension.sourceSets.configureEach {
                if (
                    isIsolatedProjectsEnabled && it.name in setOf("jsMain", "wasmJsMain", "webMain")
                ) // https://youtrack.jetbrains.com/issue/KT-80311
                 return@configureEach
                if (it.name.lowercase().contains("test")) return@configureEach
                allKotlinSourceSets.add(it)
            }

            val sourceSets =
                allKotlinSourceSets.zip(allCompilations) { sourceSets, allCompilations ->
                    sourceSets.map { sourceSet ->
                        // Find the compilations that this source set is part of.
                        val allAssociatedCompilations =
                            allCompilations.filter { it.allKotlinSourceSets.contains(sourceSet) }
                        // Include dependencies from all compilations which this source set is
                        // associated with.
                        val sourceSetDependencies =
                            allAssociatedCompilations.foldMap(
                                { it.compileDependencyFiles },
                                { fc1, fc2 -> fc1 + fc2 },
                            )
                        val kotlinPlatforms =
                            allAssociatedCompilations.map { it.platformType }.toSet()
                        SourceSetInputs(
                            sourceSet.name,
                            sourceSet.dependsOn.map { it.name },
                            sourceSet.kotlin.sourceDirectories,
                            sourceSetDependencies,
                            kotlinPlatforms,
                        )
                    }
                }

            return MultiplatformCompilationInputs(
                project,
                sourceSets,
                project.files(compileDependencies),
                bootClasspath,
                sourcePaths,
            )
        }
    }
}
