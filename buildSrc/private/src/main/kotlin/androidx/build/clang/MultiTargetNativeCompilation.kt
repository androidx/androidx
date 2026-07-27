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

    /** Returns true if native code targeting [target] can be compiled on this host machine. */
    fun canCompileOnCurrentHost(target: NativeTarget): Boolean {
        if (target.isAndroid) {
            return true
        }
        return hostManager.isEnabled(target.toKonanTarget())
    }

    /** Calls the given [action] for each added [NativeTarget] in this compilation. */
    @Suppress("unused") // used in build.gradle
    fun configureEachTarget(action: Action<NativeTargetCompilation>) {
        nativeTargets.configureEach(action)
    }

    /**
     * Returns a [RegularFile] provider that points to the shared library output for the given
     * [target].
     */
    fun sharedObjectOutputFor(target: NativeTarget): Provider<RegularFile> {
        return nativeTargets.named(target.name).flatMap { nativeTargetCompilation ->
            nativeTargetCompilation.linkerTask.flatMap { it.clangParameters.outputFile }
        }
    }

    fun sharedArchiveOutputFor(target: NativeTarget): Provider<RegularFile> {
        return nativeTargets.named(target.name).flatMap { nativeTargetCompilation ->
            nativeTargetCompilation.archiveTask.flatMap { it.llvmArchiveParameters.outputFile }
        }
    }

    /**
     * Adds the given [target] to the list of compilation target if it can be built on this machine.
     * The [action] block can be used to further configure the parameters of that compilation.
     */
    @Suppress("MemberVisibilityCanBePrivate") // used in build.gradle
    @JvmOverloads
    fun configureTarget(target: NativeTarget, action: Action<NativeTargetCompilation>? = null) {
        if (!canCompileOnCurrentHost(target)) {
            // Cannot compile it on this host. This is similar to calling `ios` block in the build
            // gradle file on a linux machine.
            return
        }
        val nativeTarget =
            if (nativeTargets.names.contains(target.name)) {
                nativeTargets.named(target.name)
            } else {
                nativeTargets.register(target.name).also {
                    // force evaluation of target so that tasks are registered b/325518502
                    nativeTargets.getByName(target.name)
                }
            }
        if (action != null) {
            nativeTarget.configure(action)
        }
    }

    /**
     * Returns a provider for the given native target and throws an exception if it is not
     * registered.
     */
    fun targetProvider(target: NativeTarget): Provider<NativeTargetCompilation> =
        nativeTargets.named(target.name)

    /**
     * Returns a provider that contains the list of [NativeTargetCompilation]s that matches the
     * given [predicate].
     *
     * You can use this provider to obtain the compilation for targets needed without forcing the
     * creation of all other targets.
     */
    internal fun targetsProvider(
        predicate: (NativeTarget) -> Boolean
    ): Provider<List<NativeTargetCompilation>> =
        project.provider {
            nativeTargets.names
                .filter { predicate(SerializableNativeTarget(it).asNativeTarget) }
                .map { nativeTargets.getByName(it) }
        }

    /** Returns true if the given [target] is configured as a compilation target. */
    fun hasTarget(target: NativeTarget) = nativeTargets.names.contains(target.name)

    /**
     * Convenience method to configure multiple targets at the same time. This is equal to calling
     * [configureTarget] for each given [targets].
     */
    @Suppress("unused") // used in build.gradle
    @JvmOverloads
    fun configureTargets(
        targets: List<NativeTarget>,
        action: Action<NativeTargetCompilation>? = null,
    ) = targets.map { configureTarget(it, action) }

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
            return create(SerializableNativeTarget(name))
        }

        @JvmName("createWithSerializableNativeTarget")
        private fun create(
            serializableNativeTarget: SerializableNativeTarget
        ): NativeTargetCompilation {
            val includes = project.objects.fileCollection()
            val sources = project.objects.fileCollection()
            val freeArgs = project.objects.listProperty<String>()
            val linkedObjects = project.objects.fileCollection()
            val linkerArgs = project.objects.listProperty<String>()
            val compileTask =
                createCompileTask(serializableNativeTarget, includes, sources, freeArgs)
            val archiveTask = createArchiveTask(serializableNativeTarget, compileTask)
            val sharedLibTask =
                createLinkerTask(
                    serializableNativeTarget,
                    compileTask,
                    sources,
                    linkedObjects,
                    linkerArgs,
                )
            return NativeTargetCompilation(
                project = project,
                target = serializableNativeTarget.asNativeTarget,
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
            serializableNativeTarget: SerializableNativeTarget,
            compileTask: TaskProvider<ClangCompileTask>,
        ): TaskProvider<ClangArchiveTask> {
            val archiveTaskName =
                taskPrefix.appendCapitalized("archive", serializableNativeTarget.name)
            val archiveTask =
                project.tasks.register(archiveTaskName, ClangArchiveTask::class.java) { task ->
                    val target = serializableNativeTarget.asNativeTarget
                    val archiveFileName =
                        listOf(target.staticPrefix, archiveName, ".", target.staticSuffix)
                            .joinToString("")
                    task.usesService(ClangBuildService.obtain(project))
                    task.llvmArchiveParameters.let { llvmAr ->
                        llvmAr.outputFile.set(
                            outputDir.map { it.file("$serializableNativeTarget/$archiveFileName") }
                        )
                        llvmAr.target.set(serializableNativeTarget)
                        llvmAr.objectFiles.from(compileTask.map { it.clangParameters.output })
                    }
                }
            return archiveTask
        }

        private fun createCompileTask(
            serializableNativeTarget: SerializableNativeTarget,
            includes: ConfigurableFileCollection,
            sources: ConfigurableFileCollection,
            freeArgs: ListProperty<String>,
        ): TaskProvider<ClangCompileTask> {
            val compileTaskName =
                taskPrefix.appendCapitalized("compile", serializableNativeTarget.name)
            val compileTask =
                project.tasks.register(compileTaskName, ClangCompileTask::class.java) { compileTask
                    ->
                    compileTask.usesService(ClangBuildService.obtain(project))
                    compileTask.clangParameters.let { clang ->
                        clang.output.set(
                            outputDir.map { it.dir("compile/$serializableNativeTarget") }
                        )
                        clang.includes.from(includes)
                        clang.sources.from(sources)
                        clang.freeArgs.addAll(freeArgs)
                        clang.target.set(serializableNativeTarget)
                    }
                }
            return compileTask
        }

        private fun createLinkerTask(
            serializableNativeTarget: SerializableNativeTarget,
            compileTask: TaskProvider<ClangCompileTask>,
            sources: ConfigurableFileCollection,
            linkedObjects: ConfigurableFileCollection,
            linkerArgs: ListProperty<String>,
        ): TaskProvider<ClangLinkerTask> {
            val archiveTaskName =
                taskPrefix.appendCapitalized("runLinker", serializableNativeTarget.name)
            val archiveTask =
                project.tasks.register(archiveTaskName, ClangLinkerTask::class.java) { task ->
                    val target = serializableNativeTarget.asNativeTarget

                    val archiveFileName =
                        if (outputKind == LinkerOutputKind.EXECUTABLE) {
                            archiveName
                        } else {
                            listOf(target.dynamicPrefix, archiveName, ".", target.dynamicSuffix)
                                .joinToString("")
                        }

                    task.usesService(ClangBuildService.obtain(project))
                    task.clangParameters.let { clang ->
                        clang.outputFile.set(
                            outputDir.map { it.file("$serializableNativeTarget/$archiveFileName") }
                        )
                        clang.linkerOutputKind.set(outputKind)
                        clang.target.set(serializableNativeTarget)
                        clang.objectFiles.from(compileTask.map { it.clangParameters.output })
                        clang.linkedObjects.from(linkedObjects)
                        clang.linkerArgs.addAll(linkerArgs)
                        clang.sources.from(sources)
                    }
                }
            return archiveTask
        }
    }
}
