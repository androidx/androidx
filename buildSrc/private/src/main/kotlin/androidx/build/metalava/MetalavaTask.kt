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
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
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
}
