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

package androidx.build

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations

fun Project.configureJavaFormat() {
    val javaFormatClasspath = getJavaFormatConfiguration()
    tasks.register("javaFormat", JavaFormatTask::class.java) { task ->
        task.javaFormatClasspath.from(javaFormatClasspath)
    }
}

private fun Project.getJavaFormatConfiguration(): FileCollection {
    val config =
        configurations.detachedConfiguration(
            dependencies.create(getLibraryByName("googlejavaformat"))
        )
    return files(config)
}

@CacheableTask
abstract class JavaFormatTask : DefaultTask() {
    init {
        description = "Fix Java code style deviations."
        group = "formatting"
    }

    @get:Input
    @set:Option(option = "fix-imports-only", description = "Only correct imports")
    var importsOnly: Boolean = false

    @get:Inject abstract val execOperations: ExecOperations

    @get:Classpath abstract val javaFormatClasspath: ConfigurableFileCollection

    @get:Inject abstract val objects: ObjectFactory

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE) SkipWhenEmpty IgnoreEmptyDirectories]
    open fun getInputFiles(): FileTree {
        return objects.fileTree().setDir(INPUT_DIR).apply {
            include(INCLUDED_FILES)
            exclude(excludedDirectoryGlobs)
        }
    }

    // Format task rewrites inputs, so the outputs are the same as inputs.
    @OutputFiles fun getRewrittenFiles(): FileTree = getInputFiles()

    private fun getArgsList(): List<String> {
        val arguments = mutableListOf("--aosp", "--replace")
        if (importsOnly) arguments.add("--fix-imports-only")
        arguments.addAll(getInputFiles().files.map { it.absolutePath })
        return arguments
    }

    @TaskAction
    fun runFormat() {
        execOperations.javaexec { javaExecSpec ->
            javaExecSpec.mainClass.set(MAIN_CLASS)
            javaExecSpec.classpath = javaFormatClasspath
            javaExecSpec.args = getArgsList()
            javaExecSpec.jvmArgs(
                "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            )
        }
    }

    companion object {
        private val excludedDirectories =
            listOf(
                "test-data",
                "external",
            )

        private val excludedDirectoryGlobs = excludedDirectories.map { "**/$it/**/*.java" }
        private const val MAIN_CLASS = "com.google.googlejavaformat.java.Main"
        private const val INPUT_DIR = "src"
        private const val INCLUDED_FILES = "**/*.java"
    }
}
