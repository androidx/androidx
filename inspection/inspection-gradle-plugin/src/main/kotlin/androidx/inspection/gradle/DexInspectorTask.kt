/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.inspection.gradle

import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

@CacheableTask
abstract class DexInspectorTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val d8Executable: RegularFileProperty

    @get:Classpath
    @get:InputFile
    abstract val androidJar: RegularFileProperty

    @get:Classpath
    @get:InputFiles
    abstract val compileClasspath: ConfigurableFileCollection

    @get:Classpath
    @get:InputFiles
    abstract val jars: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract var minSdkVersion: Int

    @get:javax.inject.Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun exec() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        val errorStream = ByteArrayOutputStream()
        val executionResult = execOperations.exec {
            it.executable = d8Executable.get().asFile.absolutePath
            val filesToDex = jars.map { file -> file.absolutePath }

            // All runtime dependencies of the inspector are already jarjar-ed and packed in
            // the single jar by previous steps. However, inspectors have compileOnly
            // dependencies as well that are required by d8 for clean dexing.
            // Those compileOnly libraries are inspected libraries, that are provided by an
            // inspected app in the runtime. But it's hard to access compileOnly Configuration
            // and easy to access the compileConfiguration and it's ok to pass extra libraries to
            // d8, so we pass the entire compileConfiguration here.
            // More on compileConfiguratioh, see here:
            // https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_plugin_and_dependency_management
            val libArgs = compileClasspath.map {
                listOf("--lib", it.absolutePath)
            }.flatten()
            val libSdk = listOf("--lib", androidJar.get().asFile.absolutePath)
            val minApiArg = listOf("--min-api", "$minSdkVersion")
            it.args = listOf("--output", output.absolutePath) + libArgs + libSdk + minApiArg +
                filesToDex
            it.errorOutput = errorStream
            it.isIgnoreExitValue = true
        }
        val errors = errorStream.toByteArray()
        val exitCode = executionResult.exitValue
        if (errors.isNotEmpty() || exitCode != 0) {
            logger.error("D8 errors: ${errors.toString(Charset.defaultCharset())}")
            throw GradleException(
                "Dexing didn't finish cleanly (exitCode = $exitCode), " +
                    "see logs for warnings and errors"
            )
        }
    }

    fun setD8(sdkDir: File, toolsVersion: String) {
        d8Executable.set(File(sdkDir, "build-tools/$toolsVersion/d8"))
    }

    fun setAndroidJar(sdkDir: File, compileSdk: String) {
        // Preview SDK compileSdkVersions are prefixed with "android-", e.g. "android-S".
        val platform = if (compileSdk.startsWith("android")) compileSdk else "android-$compileSdk"
        androidJar.set(File(sdkDir, "platforms/$platform/android.jar"))
    }
}

// variant.taskName relies on @ExperimentalStdlibApi api
@ExperimentalStdlibApi
@Suppress("DEPRECATION") // LibraryVariant
fun Project.registerUnzipTask(
    variant: com.android.build.gradle.api.LibraryVariant
): TaskProvider<CopyFixed> {
    return tasks.register(variant.taskName("unpackInspectorAAR"), CopyFixed::class.java) {
        it.inputJar.set(variant.packageLibraryProvider!!.get().archiveFile)
        it.outputDir.set(taskWorkingDir(variant, "unpackedInspectorAAR"))
        it.dependsOn(variant.assembleProvider)
    }
}

// Working around Gradle issue https://github.com/gradle/gradle/issues/17936
abstract class CopyFixed : DefaultTask() {
    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:javax.inject.Inject
    abstract val archiveOps: ArchiveOperations

    @TaskAction
    fun runTask() {
        val outputLocation = outputDir.get().asFile
        outputLocation.deleteRecursively()
        outputLocation.mkdirs()
        archiveOps.zipTree(inputJar.get().asFile).visit {
            val targetLocation = outputLocation.resolve(it.relativePath.toString())
            if (it.isDirectory()) {
                targetLocation.mkdirs()
            } else {
                targetLocation.parentFile.mkdirs()
                it.copyTo(targetLocation)
            }
        }
    }
}

// variant.taskName relies on @ExperimentalStdlibApi api
@ExperimentalStdlibApi
@Suppress("DEPRECATION") // BaseVariant
fun Project.registerBundleInspectorTask(
    variant: com.android.build.gradle.api.BaseVariant,
    extension: BaseExtension,
    jarName: String?,
    jar: TaskProvider<out Jar>
): TaskProvider<Zip> {
    val name = jarName ?: "${project.name}.jar"
    val out = File(taskWorkingDir(variant, "dexedInspector"), name)

    val dex = tasks.register(variant.taskName("dexInspector"), DexInspectorTask::class.java) {
        it.minSdkVersion = extension.defaultConfig.minSdk!!
        it.setD8(extension.sdkDirectory, extension.buildToolsVersion)
        it.setAndroidJar(extension.sdkDirectory, extension.compileSdkVersion!!)
        it.jars.from(jar.get().archiveFile)
        it.outputFile.set(out)
        it.compileClasspath.from(
            variant.compileConfiguration.incoming.artifactView {
                it.attributes {
                    it.attribute(
                        Attribute.of("artifactType", String::class.java),
                        "android-classes"
                    )
                }
            }.artifacts.artifactFiles
        )
        it.dependsOn(jar)
    }

    return tasks.register(variant.taskName("assembleInspectorJar"), Zip::class.java) {
        it.from(zipTree(jar.map { it.archiveFile }))
        it.from(zipTree(out))
        it.exclude("**/*.class")
        it.archiveFileName.set(name)
        it.destinationDirectory.set(taskWorkingDir(variant, "assembleInspectorJar"))
        it.dependsOn(dex)
        it.includeEmptyDirs = false
    }
}
