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

package androidx.stableaidl.tasks

import androidx.stableaidl.internal.DirectoryWalker
import androidx.stableaidl.internal.LoggerWrapper
import androidx.stableaidl.internal.compiling.DependencyFileProcessor
import androidx.stableaidl.internal.incremental.DependencyData
import androidx.stableaidl.internal.process.GradleProcessExecutor
import com.android.ide.common.process.LoggedProcessOutputHandler
import com.android.utils.FileUtils
import com.google.common.annotations.VisibleForTesting
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.nio.file.Path
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

/**
 * Extension of AidlCompile that allows specifying extra command-line arguments.
 */
@CacheableTask
abstract class StableAidlCompile : DefaultTask() {

    @get:Internal
    abstract var variantName: String

    /**
     * List of directories containing AIDL sources to be compiled.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirs: ListProperty<Directory>

    /**
     * List of directories containing AIDL sources available as imports.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val importDirs: ListProperty<Directory>

    /**
     * List of file system locations containing AIDL sources available as imports from dependencies.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dependencyImportDirs: SetProperty<FileSystemLocation>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aidlFrameworkProvider: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aidlExecutable: RegularFileProperty

    /**
     * Directory for storing AIDL-generated Java sources.
     */
    @get:OutputDirectory
    abstract val sourceOutputDir: DirectoryProperty

    /**
     * Directory for storing Parcelable headers for consumers.
     */
    @get:OutputDirectory
    @get:Optional
    abstract val packagedDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val extraArgs: ListProperty<String>

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    private class DepFileProcessor : DependencyFileProcessor {
        override fun processFile(dependencyFile: File): DependencyData? {
            return DependencyData.parseDependencyFile(dependencyFile)
        }
    }

    @TaskAction
    fun compile() {
        // this is full run, clean the previous output
        // TODO: Is this actually necessary?
        val destinationDir = sourceOutputDir.get().asFile
        FileUtils.cleanOutputDir(destinationDir)
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val parcelableDir = packagedDir.orNull
        if (parcelableDir != null) {
            FileUtils.cleanOutputDir(parcelableDir.asFile)
        }

        val fullImportList = sourceDirs.get() + importDirs.get()
        val sourceDirsAsFiles = sourceDirs.get().map { it.asFile }

        aidlCompileDelegate(
            workerExecutor,
            aidlExecutable.get().asFile,
            aidlFrameworkProvider.get().asFile,
            destinationDir,
            parcelableDir?.asFile,
            extraArgs.get(),
            sourceDirsAsFiles,
            fullImportList,
            dependencyImportDirs.get().map { it.asFile }
        )
    }

    internal class ProcessingRequest(val root: File, val file: File) : Serializable

    abstract class StableAidlCompileRunnable : WorkAction<StableAidlCompileRunnable.Params> {

        abstract class Params : WorkParameters {
            abstract val aidlExecutable: RegularFileProperty
            abstract val frameworkLocation: DirectoryProperty
            abstract val importFolders: ConfigurableFileCollection
            abstract val sourceOutputDir: DirectoryProperty
            abstract val packagedOutputDir: DirectoryProperty
            abstract val dir: Property<File>
            abstract val extraArgs: ListProperty<String>
        }

        @get:Inject
        abstract val execOperations: ExecOperations

        override fun execute() {
            // Collect all aidl files in the directory then process them
            val processingRequests = mutableListOf<ProcessingRequest>()

            val collector =
                DirectoryWalker.FileAction { root: Path, file: Path ->
                    processingRequests.add(ProcessingRequest(root.toFile(), file.toFile()))
                }

            try {
                DirectoryWalker.builder()
                    .root(parameters.dir.get().toPath())
                    .extensions("aidl")
                    .action(collector)
                    .build()
                    .walk()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            val depFileProcessor = DepFileProcessor()
            val executor =
                GradleProcessExecutor(
                    execOperations::exec
                )
            val logger = LoggedProcessOutputHandler(
                LoggerWrapper.getLogger(StableAidlCompileRunnable::class.java))

            for (request in processingRequests) {
                callStableAidlProcessor(
                    parameters.aidlExecutable.get().asFile.canonicalPath,
                    parameters.frameworkLocation.get().asFile.canonicalPath,
                    parameters.importFolders.asIterable(),
                    parameters.extraArgs.get(),
                    executor,
                    logger,
                    parameters.sourceOutputDir.get().asFile,
                    parameters.packagedOutputDir.orNull?.asFile,
                    depFileProcessor,
                    request.root.toPath(),
                    request.file.toPath()
                )
            }
        }
    }

    companion object {
        @VisibleForTesting
        fun aidlCompileDelegate(
            workerExecutor: WorkerExecutor,
            aidlExecutable: File,
            frameworkLocation: File,
            destinationDir: File,
            parcelableDir: File?,
            extraArgs: List<String>,
            sourceFolders: Collection<File>,
            projectImportList: Collection<Directory>,
            dependencyImportList: Collection<File>
        ) {
            for (dir in sourceFolders) {
                workerExecutor.noIsolation().submit(StableAidlCompileRunnable::class.java) {
                    it.aidlExecutable.set(aidlExecutable)
                    it.frameworkLocation.set(frameworkLocation)
                    it.importFolders.from(projectImportList, dependencyImportList)
                    it.sourceOutputDir.set(destinationDir)
                    it.packagedOutputDir.set(parcelableDir)
                    it.extraArgs.set(extraArgs)
                    it.dir.set(dir)
                }
            }
        }
    }
}
