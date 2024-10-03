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

package androidx.build.sources

import androidx.build.multiplatformExtension
import com.google.common.truth.Truth
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.junit.Test

class SourceJarTaskHelperTest {
    @Test
    fun generateMetadata() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
        val extension = project.multiplatformExtension!!
        extension.jvm()
        val commonMain = extension.sourceSets.getByName("commonMain")
        val jvmMain = extension.sourceSets.getByName("jvmMain")
        val extraMain = extension.sourceSets.create("extraMain")
        extraMain.dependsOn(commonMain)
        jvmMain.dependsOn(commonMain)
        jvmMain.dependsOn(extraMain)

        val result = createSourceSetMetadata(extension)
        Truth.assertThat(result)
            .isEqualTo(
                mapOf(
                    "sourceSets" to
                        listOf(
                            mapOf(
                                "name" to "commonMain",
                                "dependencies" to emptyList<String>(),
                                "analysisPlatform" to "common"
                            ),
                            mapOf(
                                "name" to "extraMain",
                                "dependencies" to listOf("commonMain"),
                                "analysisPlatform" to "jvm"
                            ),
                            mapOf(
                                "name" to "jvmMain",
                                "dependencies" to listOf("commonMain", "extraMain"),
                                "analysisPlatform" to "jvm"
                            )
                        )
                )
            )
    }
}
