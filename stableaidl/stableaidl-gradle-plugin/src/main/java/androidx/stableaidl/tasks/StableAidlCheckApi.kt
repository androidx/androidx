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

import androidx.stableaidl.internal.LoggerWrapper
import androidx.stableaidl.internal.process.GradleProcessExecutor
import com.android.ide.common.process.LoggedProcessOutputHandler
import com.google.common.annotations.VisibleForTesting
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
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
abstract class StableAidlCheckApi : DefaultTask() {

    @get:Internal
    abstract var variantName: String

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

    // We cannot use InputDirectory here because the directory may not exist yet.
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val expectedApiDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val actualApiDir: DirectoryProperty

    @get:Input
    abstract val checkApiMode: Property<String>

    @get:Input
    @get:Optional
    abstract val failOnMissingExpected: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val extraArgs: ListProperty<String>

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun checkApi() {
        val checkApiMode = checkApiMode.get()
        val expectedApiDir = expectedApiDir.get()
        val actualApiDir = actualApiDir.get()
        val extraArgs = extraArgs.get() + listOf(
            "--structured",
            "--checkapi=$checkApiMode",
            expectedApiDir.asFile.absolutePath,
            actualApiDir.asFile.absolutePath,
        )

        if (!expectedApiDir.asFile.exists()) {
            if (failOnMissingExpected.getOrElse(false)) {
                throw GradleException("Missing expected API directory: $expectedApiDir")
            }
            return
        }

        aidlCheckApiDelegate(
            workerExecutor,
            aidlExecutable.get().asFile,
            aidlFrameworkProvider.get().asFile,
            extraArgs,
            importDirs.get(),
            dependencyImportDirs.get().map { it.asFile }
        )
    }

    abstract class StableAidlCheckApiRunnable : WorkAction<StableAidlCheckApiRunnable.Params> {

        abstract class Params : WorkParameters {
            abstract val aidlExecutable: RegularFileProperty
            abstract val frameworkLocation: DirectoryProperty
            abstract val importFolders: ConfigurableFileCollection
            abstract val extraArgs: ListProperty<String>
        }

        @get:Inject
        abstract val execOperations: ExecOperations

        override fun execute() {
            val executor =
                GradleProcessExecutor(
                    execOperations::exec
                )
            val logger = LoggedProcessOutputHandler(
                LoggerWrapper.getLogger(StableAidlCheckApiRunnable::class.java))

            callStableAidlProcessor(
                parameters.aidlExecutable.get().asFile.canonicalPath,
                parameters.frameworkLocation.get().asFile.canonicalPath,
                parameters.importFolders.asIterable(),
                parameters.extraArgs.get(),
                executor,
                logger
            )
        }
    }

    companion object {
        const val MODE_EQUAL = "equal"
        const val MODE_COMPATIBLE = "compatible"

        @VisibleForTesting
        fun aidlCheckApiDelegate(
            workerExecutor: WorkerExecutor,
            aidlExecutable: File,
            frameworkLocation: File,
            extraArgs: List<String>,
            projectImportList: Collection<Directory>,
            dependencyImportList: Collection<File>
        ) {
            workerExecutor.noIsolation().submit(StableAidlCheckApiRunnable::class.java) {
                it.aidlExecutable.set(aidlExecutable)
                it.frameworkLocation.set(frameworkLocation)
                it.importFolders.from(projectImportList, dependencyImportList)
                it.extraArgs.set(extraArgs)
            }
        }
    }
}
