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

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.Properties
import net.saff.checkmark.Checkmark.Companion.check
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.junit.Test

class SdkResourceGeneratorTest {
    @Test
    fun `All SDK properties are resolved`() {
        androidx.build.dependencies.agpVersion = "1.2.3"
        androidx.build.dependencies.kspVersion = "3.4.5"
        androidx.build.dependencies.kotlinGradlePluginVersion = "1.7.10"

        val project = ProjectBuilder.builder().build()
        project.extensions.create(
            "androidXConfiguration",
            AndroidXConfigImpl::class.java,
            project.provider { KotlinVersion.KOTLIN_1_7 },
            project.provider { "1.7.10" },
            project.provider { KotlinVersion.KOTLIN_1_9 },
            project.provider { "1.9.20" }
        )

        project.setSupportRootFolder(File("files/support"))
        val extension = project.rootProject.property("ext") as ExtraPropertiesExtension
        extension.set("prebuiltsRoot", project.projectDir.resolve("relative/prebuilts"))
        extension.set("androidx.compileSdk", 33)
        extension.set("outDir", project.layout.buildDirectory.dir("out").get().asFile)

        val taskProvider = SdkResourceGenerator.registerSdkResourceGeneratorTask(project)
        val tasks = project.getTasksByName(SdkResourceGenerator.TASK_NAME, false)
        val generator = tasks.first() as SdkResourceGenerator
        generator.prebuiltsRelativePath.check { it == "relative/prebuilts" }

        val task = taskProvider.get()
        val propsFile = task.outputDir.file("sdk.prop").get().asFile
        propsFile.parentFile.mkdirs()
        propsFile.createNewFile()
        task.generateFile()

        val stream = propsFile.inputStream()
        val properties = Properties()
        properties.load(stream)

        // All properties must be resolved.
        properties.values.forEach { propertyValue ->
            assertThat(propertyValue.toString()).doesNotMatch("task '.+?' property '.+?'")
        }
    }

    internal open class AndroidXConfigImpl(
        override val kotlinApiVersion: Provider<KotlinVersion>,
        override val kotlinBomVersion: Provider<String>,
        override val kotlinTestApiVersion: Provider<KotlinVersion>,
        override val kotlinTestBomVersion: Provider<String>
    ) : AndroidXConfiguration
}
