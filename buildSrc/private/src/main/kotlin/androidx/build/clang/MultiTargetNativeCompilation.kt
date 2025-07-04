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

package androidx.build.clang

import com.android.utils.appendCapitalized
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.listProperty
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.LinkerOutputKind

/**
 * A native compilation setup (C code) that can target multiple platforms.
 *
 * New targets can be added via the [configureTarget] method. Each configured target will have tasks
 * to produce machine code (.o), shared library (.so / .dylib) or archive (.a).
 *
 * Common configuration between targets can be done via the [configureEachTarget] method.
 *
 * @see NativeTargetCompilation for configuration details for each target.
 */
class MultiTargetNativeCompilation(
    internal val project: Project,
    internal val archiveName: String,
    internal val outputKind: LinkerOutputKind,
) {
    private val hostManager = HostManager()

    private val nativeTargets =
        project.objects.domainObjectContainer(
            NativeTargetCompilation::class.java,
            Factory(project = project, archiveName = archiveName, outputKind = outputKind),
        )

    /** Returns true if native code targeting [konanTarget] can be compiled on this host machine. */
    fun canCompileOnCurrentHost(konanTarget: KonanTarget) = hostManager.isEnabled(konanTarget)

    /** Calls the given [action] for each added [KonanTarget] in this compilation. */
    @Suppress("unused") // used in build.gradle
    fun configureEachTarget(action: Action<NativeTargetCompilation>) {
        nativeTargets.configureEach(action)
    }

    /**
     * Returns a [RegularFile] provider that points to the shared library output for the given
     * [konanTarget].
     */
    fun sharedObjectOutputFor(konanTarget: KonanTarget): Provider<RegularFile> {
        return nativeTargets.named(konanTarget.name).flatMap { nativeTargetCompilation ->
            nativeTargetCompilation.linkerTask.flatMap { it.clangParameters.outputFile }
        }
    }

    fun sharedArchiveOutputFor(konanTarget: KonanTarget): Provider<RegularFile> {
        return nativeTargets.named(konanTarget.name).flatMap { nativeTargetCompilation ->
            nativeTargetCompilation.archiveTask.flatMap { it.llvmArchiveParameters.outputFile }
        }
    }

    /**
     * Adds the given [konanTarget] to the list of compilation target if it can be built on this
     * machine. The [action] block can be used to further configure the parameters of that
     * compilation.
     */
    @Suppress("MemberVisibilityCanBePrivate") // used in build.gradle
    @JvmOverloads
    fun configureTarget(konanTarget: KonanTarget, action: Action<NativeTargetCompilation>? = null) {
        if (!canCompileOnCurrentHost(konanTarget)) {
            // Cannot compile it on this host. This is similar to calling `ios` block in the build
            // gradle file on a linux machine.
            return
        }
        val nativeTarget =
            if (nativeTargets.names.contains(konanTarget.name)) {
                nativeTargets.named(konanTarget.name)
            } else {
                nativeTargets.register(konanTarget.name).also {
                    // force evaluation of target so that tasks are registered b/325518502
                    nativeTargets.getByName(konanTarget.name)
                }
            }
        if (action != null) {
            nativeTarget.configure(action)
        }
    }

    /**
     * Returns a provider for the given konan target and throws an exception if it is not
     * registered.
     */
    fun targetProvider(konanTarget: KonanTarget): Provider<NativeTargetCompilation> =
        nativeTargets.named(konanTarget.name)

    /**
     * Returns a provider that contains the list of [NativeTargetCompilation]s that matches the
     * given [predicate].
     *
     * You can use this provider to obtain the compilation for targets needed without forcing the
     * creation of all other targets.
     */
    internal fun targetsProvider(
        predicate: (KonanTarget) -> Boolean
    ): Provider<List<NativeTargetCompilation>> =
        project.provider {
            nativeTargets.names
                .filter { predicate(SerializableKonanTarget(it).asKonanTarget) }
                .map { nativeTargets.getByName(it) }
        }

    /** Returns true if the given [konanTarget] is configured as a compilation target. */
    fun hasTarget(konanTarget: KonanTarget) = nativeTargets.names.contains(konanTarget.name)

    /**
     * Convenience method to configure multiple targets at the same time. This is equal to calling
     * [configureTarget] for each given [konanTargets].
     */
    @Suppress("unused") // used in build.gradle
    @JvmOverloads
    fun configureTargets(
        konanTargets: List<KonanTarget>,
        action: Action<NativeTargetCompilation>? = null,
    ) = konanTargets.map { configureTarget(it, action) }

    /**
     * Internal factory for creating instances of [nativeTargets]. This factory sets up all
     * necessary inputs and their tasks for the native target.
     */
    private class Factory(
        private val project: Project,
        private val archiveName: String,
        private val outputKind: LinkerOutputKind,
    ) : NamedDomainObjectFactory<NativeTargetCompilation> {
        /** Shared task prefix for this archive */
        private val taskPrefix = "nativeCompilationFor".appendCapitalized(archiveName)

        /** Shared output directory prefix for tasks of this archive. */
        private val outputDir =
            project.layout.buildDirectory.dir("clang".appendCapitalized(archiveName))

        override fun create(name: String): NativeTargetCompilation {
            return create(SerializableKonanTarget(name))
        }

        @JvmName("createWithSerializableKonanTarget")
        private fun create(
            serializableKonanTarget: SerializableKonanTarget
        ): NativeTargetCompilation {
            val includes = project.objects.fileCollection()
            val sources = project.objects.fileCollection()
            val freeArgs = project.objects.listProperty<String>()
            val linkedObjects = project.objects.fileCollection()
            val linkerArgs = project.objects.listProperty<String>()
            val compileTask =
                createCompileTask(serializableKonanTarget, includes, sources, freeArgs)
            val archiveTask = createArchiveTask(serializableKonanTarget, compileTask)
            val sharedLibTask =
                createLinkerTask(serializableKonanTarget, compileTask, linkedObjects, linkerArgs)
            return NativeTargetCompilation(
                project = project,
                konanTarget = serializableKonanTarget.asKonanTarget,
                compileTask = compileTask,
                archiveTask = archiveTask,
                linkerTask = sharedLibTask,
                sources = sources,
                includes = includes,
                linkedObjects = linkedObjects,
                linkerArgs = linkerArgs,
                freeArgs = freeArgs,
            )
        }

        private fun createArchiveTask(
            serializableKonanTarget: SerializableKonanTarget,
            compileTask: TaskProvider<ClangCompileTask>,
        ): TaskProvider<ClangArchiveTask> {
            val archiveTaskName =
                taskPrefix.appendCapitalized("archive", serializableKonanTarget.name)
            val archiveTask =
                project.tasks.register(archiveTaskName, ClangArchiveTask::class.java) { task ->
                    val konanTarget = serializableKonanTarget.asKonanTarget
                    val archiveFileName =
                        listOf(
                                konanTarget.family.staticPrefix,
                                archiveName,
                                ".",
                                konanTarget.family.staticSuffix,
                            )
                            .joinToString("")
                    task.usesService(KonanBuildService.obtain(project))
                    task.llvmArchiveParameters.let { llvmAr ->
                        llvmAr.outputFile.set(
                            outputDir.map { it.file("$serializableKonanTarget/$archiveFileName") }
                        )
                        llvmAr.konanTarget.set(serializableKonanTarget)
                        llvmAr.objectFiles.from(compileTask.map { it.clangParameters.output })
                    }
                }
            return archiveTask
        }

        private fun createCompileTask(
            serializableKonanTarget: SerializableKonanTarget,
            includes: ConfigurableFileCollection?,
            sources: ConfigurableFileCollection?,
            freeArgs: ListProperty<String>,
        ): TaskProvider<ClangCompileTask> {
            val compileTaskName =
                taskPrefix.appendCapitalized("compile", serializableKonanTarget.name)
            val compileTask =
                project.tasks.register(compileTaskName, ClangCompileTask::class.java) { compileTask
                    ->
                    compileTask.usesService(KonanBuildService.obtain(project))
                    compileTask.clangParameters.let { clang ->
                        clang.output.set(
                            outputDir.map { it.dir("compile/$serializableKonanTarget") }
                        )
                        clang.includes.from(includes)
                        clang.sources.from(sources)
                        clang.freeArgs.addAll(freeArgs)
                        clang.konanTarget.set(serializableKonanTarget)
                    }
                }
            return compileTask
        }

        private fun createLinkerTask(
            serializableKonanTarget: SerializableKonanTarget,
            compileTask: TaskProvider<ClangCompileTask>,
            linkedObjects: ConfigurableFileCollection,
            linkerArgs: ListProperty<String>,
        ): TaskProvider<ClangLinkerTask> {
            val archiveTaskName =
                taskPrefix.appendCapitalized("runLinker", serializableKonanTarget.name)
            val archiveTask =
                project.tasks.register(archiveTaskName, ClangLinkerTask::class.java) { task ->
                    val konanTarget = serializableKonanTarget.asKonanTarget

                    val archiveFileName =
                        if (outputKind == LinkerOutputKind.EXECUTABLE) {
                            archiveName
                        } else {
                            listOf(
                                    konanTarget.family.dynamicPrefix,
                                    archiveName,
                                    ".",
                                    konanTarget.family.dynamicSuffix,
                                )
                                .joinToString("")
                        }

                    task.usesService(KonanBuildService.obtain(project))
                    task.clangParameters.let { clang ->
                        clang.outputFile.set(
                            outputDir.map { it.file("$serializableKonanTarget/$archiveFileName") }
                        )
                        clang.linkerOutputKind.set(outputKind)
                        clang.konanTarget.set(serializableKonanTarget)
                        clang.objectFiles.from(compileTask.map { it.clangParameters.output })
                        clang.linkedObjects.from(linkedObjects)
                        clang.linkerArgs.addAll(linkerArgs)
                    }
                }
            return archiveTask
        }
    }
}
