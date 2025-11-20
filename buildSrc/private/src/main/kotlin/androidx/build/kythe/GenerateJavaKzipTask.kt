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

import androidx.build.checkapi.CompilationInputs
import androidx.build.getCheckoutRoot
import androidx.build.getPrebuiltsRoot
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
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
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.ExecOperations

/** Generates kzip files that are used to index the Java source code in Kythe. */
@CacheableTask
abstract class GenerateJavaKzipTask
@Inject
constructor(private val execOperations: ExecOperations) : DefaultTask() {

    /** Must be run in the checkout root so as to be free of relative markers */
    @get:Internal val checkoutRoot: File = project.getCheckoutRoot()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val javaExtractorJar: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourcePaths: ConfigurableFileCollection

    @get:Input abstract val javacCompilerArgs: ListProperty<String>

    /** Path to `vnames.json` file, used for name mappings within Kythe. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val vnamesJson: RegularFileProperty

    @get:Classpath abstract val dependencyClasspath: ConfigurableFileCollection

    @get:Classpath abstract val compiledSources: ConfigurableFileCollection

    @get:Classpath abstract val annotationProcessor: ConfigurableFileCollection

    @get:OutputFile abstract val kzipOutputFile: RegularFileProperty

    @get:OutputDirectory abstract val kytheBuildDirectory: DirectoryProperty

    @TaskAction
    fun exec() {
        val sourceFiles =
            sourcePaths.asFileTree.files
                .filter { it.extension == "java" }
                .map { it.relativeTo(checkoutRoot) }

        if (sourceFiles.isEmpty()) {
            return
        }

        val dependencyClasspath =
            dependencyClasspath
                .filter { it.extension == "jar" }
                .let { filteredClasspath ->
                    if (sourcePaths.asFileTree.files.any { it.extension == "kt" }) {
                        filteredClasspath + compiledSources
                    } else {
                        filteredClasspath
                    }
                }

        val kytheBuildDirectory = kytheBuildDirectory.get().asFile.apply { mkdirs() }

        execOperations.javaexec {
            it.mainClass.set("-jar")
            it.args(javaExtractorJar.get().asFile)
            it.args("--class-path", dependencyClasspath.joinToString(":"))
            it.args("--processor-path", annotationProcessor.joinToString(":"))
            it.args(javacCompilerArgs.get())
            it.args("-d", kytheBuildDirectory)
            it.args(sourceFiles)
            it.jvmArgs(
                // Without all these flags, the extractor fails to run. Copied from:
                // https://github.com/kythe/kythe/blob/v0.0.67/kythe/release/release.BUILD#L99-L106
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-exports=jdk.internal.opt/jdk.internal.opt=ALL-UNNAMED",
            )
            it.environment("KYTHE_CORPUS", ANDROIDX_CORPUS)
            it.environment("KYTHE_KZIP_ENCODING", "proto")
            it.environment(
                "KYTHE_OUTPUT_FILE",
                kzipOutputFile.get().asFile.relativeTo(checkoutRoot).path,
            )
            it.environment("KYTHE_ROOT_DIRECTORY", checkoutRoot.path)
            it.environment("KYTHE_VNAMES", vnamesJson.get().asFile.path)
            it.workingDir = checkoutRoot
        }
    }

    internal companion object {
        fun setupProject(
            project: Project,
            compilationInputs: CompilationInputs,
            compiledSources: Configuration,
        ) {
            val annotationProcessorPaths =
                project.objects.fileCollection().apply {
                    project.tasks.withType(JavaCompile::class.java).configureEach {
                        it.options.annotationProcessorPath?.let { path -> from(path) }
                    }
                }

            val javacCompilerArgs =
                project.objects.listProperty(String::class.java).apply {
                    project.tasks.withType(JavaCompile::class.java).configureEach {
                        addAll(it.options.compilerArgs)
                    }
                }

            project.tasks.register("generateJavaKzip", GenerateJavaKzipTask::class.java) { task ->
                task.apply {
                    javaExtractorJar.set(
                        File(project.getPrebuiltsRoot(), "build-tools/common/javac_extractor.jar")
                    )
                    sourcePaths.setFrom(compilationInputs.sourcePaths)
                    vnamesJson.set(project.getVnamesJson())
                    dependencyClasspath.setFrom(
                        compilationInputs.dependencyClasspath + compilationInputs.bootClasspath
                    )
                    this.compiledSources.setFrom(compiledSources)
                    kzipOutputFile.set(
                        project.layout.buildDirectory.file(
                            "kzips/${project.group}-${project.name}.java.kzip"
                        )
                    )
                    kytheBuildDirectory.set(project.layout.buildDirectory.dir("kythe-java-classes"))
                    annotationProcessor.setFrom(annotationProcessorPaths)
                    this.javacCompilerArgs.set(javacCompilerArgs)
                    // Needed so generated files (e.g. protos) are present when generating kzip
                    // Without this, javac_extractor will throw a compilation error
                    dependsOn(project.tasks.withType(JavaCompile::class.java))
                }
            }
        }
    }
}
