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

package androidx.build.clang

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CreateDefFileWithLibraryPathTaskTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test
    fun buildDefFile() {
        val testDir = tmpFolder.newFolder()
        val project = ProjectBuilder.builder().withProjectDir(
            testDir.resolve("project")
        ).build()
        val inputFile = tmpFolder.newFile("input.def").apply {
            writeText(
                """
                package=foo.bar
                headers=foo.h
                """.trimIndent()
            )
        }
        val soFile = testDir.resolve("soFiles/myso.so").apply {
            parentFile.mkdirs()
            writeText("this is some so file contents")
        }
        val outputFile = project.layout.buildDirectory.file("output.def")
        val task = project.tasks.register(
            "testTask",
            CreateDefFileWithLibraryPathTask::class.java
        ) { task ->
            task.original.set(inputFile)
            task.objectFile.set(soFile)
            task.target.set(outputFile)
            task.projectDir.set(project.layout.projectDirectory)
        }
        task.get().createPlatformSpecificDefFile()
        // make sure the libraryPaths is relative to the projectDir for maximum cacheability since
        // the contents of this file will be a task input for cinterop task.
        assertThat(
            outputFile.get().asFile.readText()
        ).contains(
            """
            package=foo.bar
            headers=foo.h
            libraryPaths="../soFiles"
            staticLibraries=myso.so
            """.trimIndent()
        )
    }
}
