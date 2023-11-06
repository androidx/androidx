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

import androidx.room.gradle.RoomExtension.VariantMatchName
import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ComponentIdentity
import com.android.build.api.variant.HasAndroidTest
import com.android.build.gradle.api.AndroidBasePlugin
import com.google.devtools.ksp.gradle.KspTaskJvm
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
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
import org.gradle.api.tasks.compile.JavaCompile
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
        project.check(componentsExtension != null, isFatal = true) {
            "Could not find the Android Gradle Plugin (AGP) extension, the Room Gradle plugin " +
                "should be only applied to an Android projects."
        }
        project.check(componentsExtension.pluginVersion >= AndroidPluginVersion(7, 3)) {
            "The Room Gradle plugin is only compatible with Android Gradle plugin (AGP) " +
                "version 7.3.0 or higher (found ${componentsExtension.pluginVersion})."
        }
        componentsExtension.onVariants { variant ->
            project.check(roomExtension.schemaDirectories.isNotEmpty(), isFatal = true) {
                "The Room Gradle plugin was applied but no schema location was specified. " +
                    "Use the `room { schemaDirectory(...) }` DSL to specify one."
            }
            configureVariant(project, roomExtension, variant)
            variant.unitTest?.let { configureVariant(project, roomExtension, it) }
            if (variant is HasAndroidTest) {
                variant.androidTest?.let { configureVariant(project, roomExtension, it) }
            }
        }
    }

    private fun configureVariant(
        project: Project,
        roomExtension: RoomExtension,
        variant: ComponentIdentity
    ) {
        val configureTask: (Task, ComponentIdentity) -> RoomSchemaDirectoryArgumentProvider = {
                task, variantIdentity ->
            // Find schema location for variant from user declared location with priority:
            // * Full variant name specified, e.g. `schemaLocation("demoDebug", "...")`
            // * Flavor name, e.g. `schemaLocation("demo", "...")`
            // * Build type name, e.g. `schemaLocation("debug", "...")`
            // * All variants location, e.g. `schemaLocation("...")`
            val schemaDirectories = roomExtension.schemaDirectories
            fun <V> Map<VariantMatchName, V>.findPair(key: String) =
                VariantMatchName(key).let { if (containsKey(it)) it to getValue(it) else null }
            val matchedPair = schemaDirectories.findPair(variantIdentity.name)
                    ?: variantIdentity.flavorName?.let { schemaDirectories.findPair(it) }
                    ?: variantIdentity.buildType?.let { schemaDirectories.findPair(it) }
                    ?: schemaDirectories.findPair(RoomExtension.ALL_VARIANTS.actual)
            project.check(matchedPair != null, isFatal = true) {
               "No matching schema directory for variant '${variantIdentity.name}'."
            }
            val (matchedName, schemaDirectoryProvider) = matchedPair
            val schemaDirectory = schemaDirectoryProvider.get()
            project.check(schemaDirectory.isNotEmpty()) {
                "The schema directory path for variant '${variantIdentity.name}' must not be empty."
            }
            val schemaDirectoryPath = Path(schemaDirectory)
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

            val copyTask = roomExtension.copyTasks.getOrPut(matchedName) {
                project.tasks.register(
                    "copyRoomSchemas${matchedName.actual.capitalize()}",
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

        val androidVariantTaskNames = AndroidVariantsTaskNames(variant.name, variant)
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
        private val javaCompileName by lazy {
            "compile${variantName.capitalize()}JavaWithJavac"
        }

        private val kaptTaskName by lazy {
            "kapt${variantName.capitalize()}Kotlin"
        }

        private val kspTaskJvm by lazy {
            "ksp${variantName.capitalize()}Kotlin"
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
            // Map of relative path to its source file hash.
            val copiedHashes = mutableMapOf<String, MutableMap<String, String>>()
            variantSchemaOutputDirectories.files
                .filter { it.exists() }
                .forEach { outputDir ->
                    outputDir.walkTopDown().filter { it.isFile }.forEach { schemaFile ->
                        val schemaPath = schemaFile.toPath()
                        val basePath = outputDir.toPath().relativize(schemaPath)
                        schemaPath.copyTo(
                            target = schemaDirectory.get().asFile.toPath().resolve(basePath)
                                .apply { parent?.createDirectories() },
                            overwrite = true
                        )
                        copiedHashes.getOrPut(basePath.toString()) { mutableMapOf() }
                            .put(schemaFile.sha256(), schemaPath.toString())
                    }
                }
            // Validate that if multiple schema files for the same database and version are copied
            // to the same schema directory that they are the same in content (via checksum), as
            // otherwise it would indicate per-variant schemas and thus requiring per-variant
            // schema directories.
            copiedHashes.filterValues { it.size > 1 }.forEach { (schemaDir, hashes) ->
                val errorMsg = buildString {
                    appendLine(
                        "Inconsistency detected exporting schema files (checksum - source):"
                    )
                    hashes.entries.forEach {
                        appendLine("  ${it.key} - ${it.value}")
                    }
                    appendLine(
                        "The listed files differ in content but were copied into the same " +
                            "schema directory '$schemaDir'. A possible indicator that " +
                            "per-variant schema locations must be provided."
                    )
                }
                throw GradleException(errorMsg)
            }
        }

        private fun File.sha256(): String {
            return MessageDigest.getInstance("SHA-256").digest(this.readBytes())
                .joinToString("") { "%02x".format(it) }
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
        internal fun Project.check(
            value: Boolean,
            isFatal: Boolean = false,
            lazyMessage: () -> String
        ) {
            contract {
                returns() implies value
            }
            if (isGradleSyncRunning() && !isFatal) return
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
