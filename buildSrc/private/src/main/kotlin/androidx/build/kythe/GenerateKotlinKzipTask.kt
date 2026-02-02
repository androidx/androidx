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

package androidx.build.kythe

import androidx.build.KotlinTarget
import androidx.build.OperatingSystem
import androidx.build.checkapi.CompilationInputs
import androidx.build.checkapi.MultiplatformCompilationInputs
import androidx.build.getCheckoutRoot
import androidx.build.getOperatingSystem
import androidx.build.getPrebuiltsRoot
import androidx.build.multiplatformExtension
import java.io.File
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/** Generates kzip files that are used to index the Kotlin source code in Kythe. */
@CacheableTask
abstract class GenerateKotlinKzipTask
@Inject
constructor(private val execOperations: ExecOperations) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val kotlincExtractorBin: RegularFileProperty

    /** Must be run in the checkout root so as to be free of relative markers */
    @get:Internal val checkoutRoot: File = project.getCheckoutRoot()

    @get:Internal val isKmp: Boolean = project.multiplatformExtension != null

    @get:Input abstract val kotlincFreeCompilerArgs: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourcePaths: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val commonModuleSourcePaths: ConfigurableFileCollection

    /** Path to `vnames.json` file, used for name mappings within Kythe. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val vnamesJson: RegularFileProperty

    @get:Classpath abstract val dependencyClasspath: ConfigurableFileCollection

    @get:Classpath abstract val compiledSources: ConfigurableFileCollection

    @get:Input abstract val kotlinTarget: Property<KotlinTarget>

    @get:Input abstract val jvmTarget: Property<JvmTarget>

    @get:OutputFile abstract val kzipOutputFile: RegularFileProperty

    @get:OutputDirectory abstract val kytheClassJarsDir: DirectoryProperty

    @TaskAction
    fun exec() {
        val sourceFiles =
            sourcePaths.asFileTree.files
                .takeIf { files -> files.any { it.extension == "kt" } }
                ?.filter { it.extension == "kt" || it.extension == "java" }
                ?.map { it.relativeTo(checkoutRoot) }
                .orEmpty()

        if (sourceFiles.isEmpty()) {
            return
        }

        val commonSourceFiles =
            commonModuleSourcePaths.asFileTree.files
                .filter { it.extension == "kt" || it.extension == "java" }
                .map { it.relativeTo(checkoutRoot) }

        val dependencyClasspath =
            dependencyClasspath.files
                .filter { it.exists() }
                .mapNotNull { file ->
                    when {
                        file.isFile && file.extension == "jar" -> {
                            file.relativeTo(checkoutRoot)
                        }
                        file.isDirectory -> {
                            file
                                .createJarFromDirectory(
                                    kytheClassJarsDir.get().asFile,
                                    checkoutRoot,
                                )
                                .relativeTo(checkoutRoot)
                        }
                        else -> null
                    }
                }

        val args = buildList {
            addAll(
                listOf(
                    // Kythe drops arg[0] as it's unix convention that is the executable name
                    "kotlinc",
                    "-jvm-target",
                    jvmTarget.get().target,
                    "-no-reflect",
                    "-no-stdlib",
                    "-api-version",
                    kotlinTarget.get().apiVersion.version,
                    "-language-version",
                    kotlinTarget.get().apiVersion.version,
                    "-opt-in=kotlin.contracts.ExperimentalContracts",
                )
            )
        }

        val multiplatformArg =
            if (isKmp) {
                listOf("-Xmulti-platform")
            } else emptyList()

        val filteredKotlincFreeCompilerArgs =
            kotlincFreeCompilerArgs.get().distinct().filter { !it.startsWith("-Xjdk-release") }

        val command = buildList {
            add(kotlincExtractorBin.get().asFile)
            addAll(
                listOf(
                    "-corpus",
                    ANDROIDX_CORPUS,
                    "-kotlin_out",
                    compiledSources.singleFile.relativeTo(checkoutRoot).path,
                    "-o",
                    kzipOutputFile.get().asFile.relativeTo(checkoutRoot).path,
                    "-vnames",
                    vnamesJson.get().asFile.relativeTo(checkoutRoot).path,
                    "-args",
                    (args + multiplatformArg + filteredKotlincFreeCompilerArgs).joinToString(" "),
                )
            )
            sourceFiles.forEach { addAll(listOf("-srcs", it.path)) }
            commonSourceFiles.forEach { addAll(listOf("-common_srcs", it.path)) }
            dependencyClasspath.forEach { addAll(listOf("-cp", it.path)) }
        }

        execOperations.exec {
            it.commandLine(command)
            it.workingDir = checkoutRoot
        }
    }

    internal companion object {
        fun setupProject(
            project: Project,
            compilationInputs: CompilationInputs,
            compiledSources: Configuration,
            kotlinTarget: Property<KotlinTarget>,
            javaVersion: JavaVersion,
        ) {
            val kotlincFreeCompilerArgs =
                project.objects.listProperty(String::class.java).apply {
                    project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
                        addAll(it.compilerOptions.freeCompilerArgs)
                    }
                }
            project.tasks.register("generateKotlinKzip", GenerateKotlinKzipTask::class.java) { task
                ->
                task.apply {
                    kotlincExtractorBin.set(
                        File(
                            project.getPrebuiltsRoot(),
                            "build-tools/${osName()}/bin/kotlinc_extractor",
                        )
                    )
                    sourcePaths.setFrom(compilationInputs.sourcePaths)
                    (compilationInputs as? MultiplatformCompilationInputs)
                        ?.commonModuleSourcePaths
                        ?.let { commonModuleSourcePaths.from(it) }
                    vnamesJson.set(project.getVnamesJson())
                    dependencyClasspath.setFrom(
                        compilationInputs.dependencyClasspath + compilationInputs.bootClasspath
                    )
                    this.compiledSources.setFrom(compiledSources)
                    this.kotlinTarget.set(kotlinTarget)
                    jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
                    kzipOutputFile.set(
                        File(
                            project.layout.buildDirectory.get().asFile,
                            "kzips/${project.group}-${project.name}.kotlin.kzip",
                        )
                    )
                    kytheClassJarsDir.set(project.layout.buildDirectory.dir("kythe-class-jars"))
                    this.kotlincFreeCompilerArgs.set(kotlincFreeCompilerArgs)
                }
            }
        }
    }
}

private fun osName() =
    when (getOperatingSystem()) {
        OperatingSystem.LINUX -> "linux-x86"
        OperatingSystem.MAC -> "darwin-x86"
        OperatingSystem.WINDOWS -> error("Kzip generation not supported in Windows")
    }

/* Kythe processes only JARs, so we create JARs from directory content. */
private fun File.createJarFromDirectory(kytheClassJarsDir: File, baseDir: File): File {
    val jarParentDir = File(kytheClassJarsDir, this.relativeTo(baseDir).invariantSeparatorsPath)
    jarParentDir.mkdirs()

    val jarFile = File(jarParentDir, "${this.name}.jar")
    JarOutputStream(jarFile.outputStream()).use { jarOut ->
        this.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val entryName = file.relativeTo(this).invariantSeparatorsPath
                jarOut.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(jarOut) }
                jarOut.closeEntry()
            }
    }
    return jarFile
}
