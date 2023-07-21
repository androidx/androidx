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

package androidx.room.gradle

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ComponentIdentity
import com.android.build.api.variant.Variant
import com.android.build.gradle.api.AndroidBasePlugin
import com.google.devtools.ksp.gradle.KspTaskJvm
import java.util.Locale
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.io.path.notExists
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.KaptTask

class RoomGradlePlugin @Inject constructor(
    private val projectLayout: ProjectLayout,
    private val objectFactory: ObjectFactory,
) : Plugin<Project> {
    override fun apply(project: Project) {
        var configured = false
        project.plugins.withType(AndroidBasePlugin::class.java) {
            configured = true
            configureRoom(project)
        }
        project.afterEvaluate {
            project.check(configured) {
                "The Room Gradle plugin can only be applied to an Android project."
            }
        }
    }

    private fun configureRoom(project: Project) {
        // TODO(b/277899741): Validate version of Room supports the AP options configured by plugin.
        val roomExtension =
            project.extensions.create("room", RoomExtension::class.java)
        val componentsExtension =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
        project.check(componentsExtension != null) {
            "Could not find the Android Gradle Plugin (AGP) extension, the Room Gradle plugin " +
                "should be only applied to an Android projects."
        }
        project.check(componentsExtension.pluginVersion >= AndroidPluginVersion(7, 3)) {
            "The Room Gradle plugin is only compatible with Android Gradle plugin (AGP) " +
                "version 7.3.0 or higher (found ${componentsExtension.pluginVersion})."
        }
        componentsExtension.onVariants { variant ->
            val locationProvider = roomExtension.schemaDirectory
            project.check(locationProvider != null) {
                "The Room Gradle plugin was applied but not schema location was specified. " +
                    "Use the `room { schemaDirectory(...) }` DSL to specify one."
            }
            val schemaDirectory = locationProvider.get()
            project.check(schemaDirectory.isNotEmpty()) {
                "The schemaDirectory path must not be empty."
            }
            configureVariant(project, schemaDirectory, variant)
        }
    }

    private fun configureVariant(
        project: Project,
        schemaDirectory: String,
        variant: Variant
    ) {
        val androidVariantTaskNames = AndroidVariantsTaskNames(variant.name, variant)
        val configureTask: (Task, ComponentIdentity) -> RoomSchemaDirectoryArgumentProvider = {
                task, variantIdentity ->
            val schemaDirectoryPath = Path(schemaDirectory, variantIdentity.name)
            if (schemaDirectoryPath.notExists()) {
                project.check(schemaDirectoryPath.toFile().mkdirs()) {
                    "Unable to create directory: $schemaDirectoryPath"
                }
            }
            val schemaInputDir = objectFactory.directoryProperty().apply {
                set(project.file(schemaDirectoryPath))
            }

            val schemaOutputDir =
                projectLayout.buildDirectory.dir("intermediates/room/schemas/${task.name}")

            val copyTask = androidVariantTaskNames.copyTasks.getOrPut(variant.name) {
                project.tasks.register(
                    "copyRoomSchemas${variantIdentity.name.capitalize()}",
                    RoomSchemaCopyTask::class.java
                ) {
                    it.schemaDirectory.set(schemaInputDir)
                }
            }
            copyTask.configure { it.variantSchemaOutputDirectories.from(schemaOutputDir) }
            task.finalizedBy(copyTask)

            RoomSchemaDirectoryArgumentProvider(
                forKsp = task.isKspTask(),
                schemaInputDir = schemaInputDir,
                schemaOutputDir = schemaOutputDir
            )
        }

        configureJavaTasks(project, androidVariantTaskNames, configureTask)
        configureKaptTasks(project, androidVariantTaskNames, configureTask)
        configureKspTasks(project, androidVariantTaskNames, configureTask)

        // TODO: Consider also setting up the androidTest and test source set to include the
        //  relevant schema location so users can use MigrationTestHelper without additional
        //  configuration.
    }

    private fun configureJavaTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        configureBlock: (Task, ComponentIdentity) -> RoomSchemaDirectoryArgumentProvider
    ) = project.tasks.withType(JavaCompile::class.java) { task ->
        androidVariantsTaskNames.withJavaCompile(task.name)?.let { variantIdentity ->
            val argProvider = configureBlock.invoke(task, variantIdentity)
            task.options.compilerArgumentProviders.add(argProvider)
        }
    }

    private fun configureKaptTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        configureBlock: (Task, ComponentIdentity) -> RoomSchemaDirectoryArgumentProvider
    ) = project.plugins.withId("kotlin-kapt") {
        project.tasks.withType(KaptTask::class.java) { task ->
            androidVariantsTaskNames.withKaptTask(task.name)?.let { variantIdentity ->
                val argProvider = configureBlock.invoke(task, variantIdentity)
                // TODO: Update once KT-58009 is fixed.
                try {
                    // Because of KT-58009, we need to add a `listOf(argProvider)` instead
                    // of `argProvider`.
                    task.annotationProcessorOptionProviders.add(listOf(argProvider))
                } catch (e: Throwable) {
                    // Once KT-58009 is fixed, adding `listOf(argProvider)` will fail, we will
                    // pass `argProvider` instead, which is the correct way.
                    task.annotationProcessorOptionProviders.add(argProvider)
                }
            }
        }
    }

    private fun configureKspTasks(
        project: Project,
        androidVariantsTaskNames: AndroidVariantsTaskNames,
        configureBlock: (Task, ComponentIdentity) -> RoomSchemaDirectoryArgumentProvider
    ) = project.plugins.withId("com.google.devtools.ksp") {
        project.tasks.withType(KspTaskJvm::class.java) { task ->
            androidVariantsTaskNames.withKspTaskJvm(task.name)?.let { variantIdentity ->
                val argProvider = configureBlock.invoke(task, variantIdentity)
                task.commandLineArgumentProviders.add(argProvider)
            }
        }
    }

    internal class AndroidVariantsTaskNames(
        private val variantName: String,
        private val variantIdentity: ComponentIdentity
    ) {
        // Variant name to copy task
        val copyTasks = mutableMapOf<String, TaskProvider<RoomSchemaCopyTask>>()

        private val javaCompileName by lazy {
            "compile${variantName.capitalized()}JavaWithJavac"
        }

        private val kaptTaskName by lazy {
            "kapt${variantName.capitalized()}Kotlin"
        }

        private val kspTaskJvm by lazy {
            "ksp${variantName.capitalized()}Kotlin"
        }

        fun withJavaCompile(taskName: String) =
            if (taskName == javaCompileName) variantIdentity else null

        fun withKaptTask(taskName: String) =
            if (taskName == kaptTaskName) variantIdentity else null

        fun withKspTaskJvm(taskName: String) =
            if (taskName == kspTaskJvm) variantIdentity else null
    }

    @DisableCachingByDefault(because = "Simple disk bound task.")
    abstract class RoomSchemaCopyTask : DefaultTask() {
        @get:InputFiles
        @get:SkipWhenEmpty
        @get:IgnoreEmptyDirectories
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val variantSchemaOutputDirectories: ConfigurableFileCollection

        @get:Internal
        abstract val schemaDirectory: DirectoryProperty

        @TaskAction
        fun copySchemas() {
            variantSchemaOutputDirectories.files
                .filter { it.exists() }
                .forEach {
                    // TODO(b/278266663): Error when two same relative path schemas are found in out
                    //  dirs and their content is different an indicator of an inconsistency between
                    //  the compile tasks of the same variant.
                    it.copyRecursively(schemaDirectory.get().asFile, overwrite = true)
                }
        }
    }

    class RoomSchemaDirectoryArgumentProvider(
        @get:Input
        val forKsp: Boolean,
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val schemaInputDir: Provider<Directory>,
        @get:OutputDirectory
        val schemaOutputDir: Provider<Directory>
    ) : CommandLineArgumentProvider {
        override fun asArguments() = buildList {
            val prefix = if (forKsp) "" else "-A"
            add("${prefix}room.internal.schemaInput=${schemaInputDir.get().asFile.path}")
            add("${prefix}room.internal.schemaOutput=${schemaOutputDir.get().asFile.path}")
        }
    }

    companion object {
        internal fun String.capitalize(): String = this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }

        internal fun Task.isKspTask(): Boolean = try {
            val kspTaskClass = Class.forName("com.google.devtools.ksp.gradle.KspTask")
            kspTaskClass.isAssignableFrom(this::class.java)
        } catch (ex: ClassNotFoundException) {
            false
        }

        @OptIn(ExperimentalContracts::class)
        internal fun Project.check(value: Boolean, lazyMessage: () -> String) {
            contract {
                returns() implies value
            }
            if (isGradleSyncRunning()) return
            if (!value) {
                throw GradleException(lazyMessage())
            }
        }

        private fun Project.isGradleSyncRunning() = gradleSyncProps.any {
            it in this.properties && this.properties[it].toString().toBoolean()
        }

        private val gradleSyncProps by lazy {
            listOf(
                "android.injected.build.model.v2",
                "android.injected.build.model.only",
                "android.injected.build.model.only.advanced",
            )
        }
    }
}