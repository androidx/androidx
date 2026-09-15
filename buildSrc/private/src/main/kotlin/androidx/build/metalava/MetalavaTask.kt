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

package androidx.build.metalava

import androidx.build.getSupportRootFolder
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** Base class for invoking Metalava. */
@CacheableTask
abstract class MetalavaTask
@Inject
constructor(@Internal protected val workerExecutor: WorkerExecutor) : DefaultTask() {
    /** Classpath containing Metalava and its dependencies. */
    @get:Classpath abstract val metalavaClasspath: ConfigurableFileCollection

    /** Android's boot classpath */
    @get:Classpath abstract val bootClasspath: ConfigurableFileCollection

    /** Dependencies (compiled classes) of the project. */
    @get:Classpath abstract val dependencyClasspath: ConfigurableFileCollection

    @get:Input abstract val kotlinSourceLevel: Property<KotlinVersion>

    // Marked optional for car app protocol API tasks
    @get:Optional @get:Input abstract val targetsJavaConsumers: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val configFile: RegularFileProperty =
        project.objects.fileProperty().convention {
            File(project.getSupportRootFolder(), "buildSrc/metalava-config.xml")
        }

    fun runWithArgs(args: List<String>) {
        val allArgs = buildList {
            addAll(args)
            add("--config-file")
            add(configFile.get().asFile.absolutePath)
        }
        runMetalavaWithArgs(metalavaClasspath, allArgs, kotlinSourceLevel.get(), workerExecutor)
    }

    fun runMetalavaWithArgs(
        metalavaClasspath: FileCollection,
        args: List<String>,
        kotlinSourceLevel: KotlinVersion,
        workerExecutor: WorkerExecutor,
    ) {
        val allArgs =
            args +
                listOf(
                    "--hide",
                    // Removing final from a method does not cause compatibility issues for
                    // AndroidX.
                    "RemovedFinalStrict",
                    "--warning",
                    "UnresolvedImport",
                    "--kotlin-source",
                    kotlinSourceLevel.version,

                    // Metalava arguments to suppress compatibility checks for experimental API
                    // surfaces.
                    "--suppress-compatibility-meta-annotation",
                    "androidx.annotation.RequiresOptIn",
                    "--suppress-compatibility-meta-annotation",
                    "kotlin.RequiresOptIn",

                    // Skip reading comments in Metalava for two reasons:
                    // - We prefer for developers to specify api information via annotations instead
                    //   of just javadoc comments (like @hide)
                    // - This allows us to improve cacheability of Metalava tasks
                    "--ignore-comments",

                    // Don't track annotations that aren't needed for review or checking compat.
                    "--exclude-annotation",
                    "androidx.annotation.ReplaceWith",
                    "--exclude-annotation",
                    "androidx.compose.runtime.ComposableInferredTarget",
                    "--exclude-annotation",
                    "androidx.compose.runtime.ComposableTarget",
                    // internal annotation, includes debug information and values are not constant
                    "--exclude-annotation",
                    "androidx.compose.runtime.internal.FunctionKeyMeta",

                    // This issue is important for stubs generation, which we don't do here.
                    "--hide",
                    "InheritChangesSignature",
                )
        val workQueue = workerExecutor.processIsolation()
        workQueue.submit(MetalavaWorkAction::class.java) { parameters ->
            parameters.args.set(allArgs)
            parameters.metalavaClasspath.set(metalavaClasspath.files)
        }
    }

    interface MetalavaParams : WorkParameters {
        val args: ListProperty<String>
        val metalavaClasspath: SetProperty<File>
    }

    abstract class MetalavaWorkAction
    @Inject
    constructor(private val execOperations: ExecOperations) : WorkAction<MetalavaParams> {
        override fun execute() {
            val outputStream = ByteArrayOutputStream()
            var successful = false
            try {
                execOperations.javaexec {
                    // Intellij core reflects into java.util.ResourceBundle
                    it.jvmArgs = listOf("--add-opens", "java.base/java.util=ALL-UNNAMED")
                    it.systemProperty("java.awt.headless", "true")
                    it.classpath(parameters.metalavaClasspath.get())
                    it.mainClass.set("com.android.tools.metalava.Driver")
                    it.args = parameters.args.get()
                    it.standardOutput = outputStream
                    it.errorOutput = outputStream
                }
                successful = true
            } finally {
                if (!successful) {
                    System.err.println(outputStream.toString(Charsets.UTF_8))
                }
            }
        }
    }
}
