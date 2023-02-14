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

package androidx.build

import java.io.File
import net.saff.checkmark.Checkmark.Companion.check
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SdkResourceGeneratorTest {
    @Test
    fun buildSrcOutPathIsRelative() {
        androidx.build.dependencies.agpVersion = "1.2.3"
        androidx.build.dependencies.kotlinVersion = "2.3.4"
        androidx.build.dependencies.kspVersion = "3.4.5"

        val project = ProjectBuilder.builder().build()

        project.setSupportRootFolder(File("files/support"))
        val extension = project.rootProject.property("ext") as ExtraPropertiesExtension
        extension.set("buildSrcOut", project.projectDir.resolve("relative/path"))

        SdkResourceGenerator.registerSdkResourceGeneratorTask(project)

        val tasks = project.getTasksByName(SdkResourceGenerator.TASK_NAME, false)
        val generator = tasks.first() as SdkResourceGenerator
        generator.buildSrcOutRelativePath.check { it == "relative/path" }
    }
}