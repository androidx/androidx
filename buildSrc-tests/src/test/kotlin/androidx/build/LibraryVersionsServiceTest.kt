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

import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LibraryVersionsServiceTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun basic() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1" }
            G2 = { group = "g.g2"}
        """.trimIndent()
        )
        assertThat(
            service.libraryGroups["G1"]
        ).isEqualTo(
            LibraryGroup(
                group = "g.g1", atomicGroupVersion = Version("1.2.3")
            )
        )
        assertThat(
            service.libraryGroups["G2"]
        ).isEqualTo(
            LibraryGroup(
                group = "g.g2", atomicGroupVersion = null
            )
        )
    }

    @Test
    fun invalidToml() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1" }
            G1 = { group = "g.g1"}
        """.trimIndent()
        )
        assertThrows<Exception> {
            service.libraryGroups["G1"]
        }.hasMessageThat().contains(
            "libraryversions.toml:line 5, column 1: G1 previously defined at line 4, column 1"
        )
    }

    @Test
    fun withMultiplatformVersion() {
        val toml = """
            [versions]
            V1 = "1.2.3"
            V1_KMP = "1.2.3-dev05"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1", multiplatformGroupVersion = "versions.V1_KMP"}
        """.trimIndent()
        val dontUseKmpVersions = createLibraryVersionsService(toml)
        assertThat(
            dontUseKmpVersions.libraryGroups["G1"]
        ).isEqualTo(
            LibraryGroup(
                group = "g.g1", atomicGroupVersion = Version("1.2.3")
            )
        )

        val useKmpVersions =
            createLibraryVersionsService(toml, useMultiplatformGroupVersions = true)
        assertThat(
            useKmpVersions.libraryGroups["G1"]
        ).isEqualTo(
            LibraryGroup(
                group = "g.g1", atomicGroupVersion = Version("1.2.3-dev05")
            )
        )
    }

    @Test
    fun customComposeVersions() {
        val toml = """
            [versions]
            COMPOSE_V1 = "1.2.3"
            [groups]
            COMPOSE = { group = "androidx.compose.suffix", atomicGroupVersion = "versions.COMPOSE_V1" }
        """.trimIndent()
        val noCustomVersion = createLibraryVersionsService(toml)
        assertThat(
            noCustomVersion.libraryGroups["COMPOSE"]
        ).isEqualTo(
            LibraryGroup(
                group = "androidx.compose.suffix", atomicGroupVersion = Version("1.2.3")
            )
        )
        val customComposeVersion = createLibraryVersionsService(
            toml, composeCustomGroup = "not.androidx.compose", composeCustomVersion = "1.1.1"
        )
        assertThat(
            customComposeVersion.libraryGroups["COMPOSE"]
        ).isEqualTo(
            LibraryGroup(
                group = "not.androidx.compose.suffix", atomicGroupVersion = Version("1.1.1")
            )
        )
    }

    @Test
    fun missingVersionReference() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.doesNotExist" }
        """.trimIndent()
        )
        val result = runCatching {
            service.libraryGroups["G1"]
        }
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat().contains(
            "Group entry g.g1 specifies doesNotExist, but such version doesn't exist"
        )
    }

    @Test
    fun malformedVersionReference() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "v1" }
        """.trimIndent()
        )
        val result = runCatching {
            service.libraryGroups["G1"]
        }
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat().contains(
            "Group entry atomicGroupVersion is expected to start with versions"
        )
    }

    @Test
    fun missingVersionReference_multiplatform() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1", multiplatformGroupVersion = "versions.doesNotExist2" }
        """.trimIndent()
        )
        val result = runCatching {
            service.libraryGroups["G1"]
        }
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat().contains(
            "Group entry g.g1 specifies doesNotExist2, but such version doesn't exist"
        )
    }

    @Test
    fun malformedVersionReference_multiplatform() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1", multiplatformGroupVersion = "V1" }
        """.trimIndent()
        )
        val result = runCatching {
            service.libraryGroups["G1"]
        }
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat().contains(
            "Group entry multiplatformGroupVersion is expected to start with versions"
        )
    }

    @Test
    fun atomicMultiplatformGroupWithoutAtomicGroup() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", multiplatformGroupVersion = "versions.V1" }
            """.trimIndent()
        )
        val result = runCatching {
            service.libraryGroups["G1"]
        }
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat().contains(
            """
            Cannot specify multiplatformGroupVersion for G1 without specifying an atomicGroupVersion
            """.trimIndent()
        )
    }

    @Test
    fun overrideInclude() {
        val service = createLibraryVersionsService(
            """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1", overrideInclude = [ ":otherGroup:subproject" ]}
            """
        )
        assertThat(
            service.overrideLibraryGroupsByProjectPath.get(":otherGroup:subproject")
        ).isEqualTo(
            LibraryGroup(
                group = "g.g1", atomicGroupVersion = Version("1.2.3")
            )
        )
        assertThat(
            service.overrideLibraryGroupsByProjectPath.get(":normalGroup:subproject")
        ).isEqualTo(null)
    }

    @Test
    fun androidxExtension_noAtomicGroup() {
        runAndroidExtensionTest(
            projectPath = "myGroup:project1",
            tomlFile = """
                [versions]
                [groups]
                G1 = { group = "androidx.myGroup" }
            """.trimIndent(),
            validateWithKmp = { extension ->
                extension.mavenVersion = Version("1.0.0")
                extension.mavenMultiplatformVersion = Version("1.0.0-dev01")
                assertThat(
                    extension.mavenGroup
                ).isEqualTo(
                    LibraryGroup("androidx.myGroup", atomicGroupVersion = null)
                )
                assertThat(
                    extension.project.version
                ).isEqualTo(Version("1.0.0-dev01"))
                extension.validateMavenVersion()
            },
            validateWithoutKmp = { extension ->
                extension.mavenVersion = Version("1.0.0")
                extension.mavenMultiplatformVersion = Version("1.0.0-dev01")
                assertThat(
                    extension.mavenGroup
                ).isEqualTo(
                    LibraryGroup("androidx.myGroup", atomicGroupVersion = null)
                )
                assertThat(
                    extension.project.version
                ).isEqualTo(Version("1.0.0"))
                extension.validateMavenVersion()
            }
        )
    }

    @Test
    fun androidxExtension_noAtomicGroup_setKmpVersionFirst() {
        runAndroidExtensionTest(
            projectPath = "myGroup:project1",
            tomlFile = """
                [versions]
                [groups]
                G1 = { group = "androidx.myGroup" }
            """.trimIndent(),
            validateWithKmp = { extension ->
                extension.mavenMultiplatformVersion = Version("1.0.0-dev01")
                extension.mavenVersion = Version("1.0.0")
                assertThat(
                    extension.mavenGroup
                ).isEqualTo(
                    LibraryGroup("androidx.myGroup", atomicGroupVersion = null)
                )
                assertThat(
                    extension.project.version
                ).isEqualTo(Version("1.0.0-dev01"))
                extension.validateMavenVersion()
            },
            validateWithoutKmp = { extension ->
                extension.mavenMultiplatformVersion = Version("1.0.0-dev01")
                extension.mavenVersion = Version("1.0.0")
                assertThat(
                    extension.mavenGroup
                ).isEqualTo(
                    LibraryGroup("androidx.myGroup", atomicGroupVersion = null)
                )
                assertThat(
                    extension.project.version
                ).isEqualTo(Version("1.0.0"))
                extension.validateMavenVersion()
            }
        )
    }

    @Test
    fun androidxExtension_withAtomicGroup() {
        runAndroidExtensionTest(
            projectPath = "myGroup:project1",
            tomlFile = """
                [versions]
                V1 = "1.0.0"
                V1_KMP = "1.0.0-dev01"
                [groups]
                G1 = { group = "androidx.myGroup", atomicGroupVersion = "versions.V1", multiplatformGroupVersion = "versions.V1_KMP" }
            """.trimIndent(),
            validateWithKmp = { extension ->
                assertThat(
                    extension.mavenGroup
                ).isEqualTo(
                    LibraryGroup("androidx.myGroup", atomicGroupVersion = Version("1.0.0-dev01"))
                )
                assertThat(
                    extension.project.version
                ).isEqualTo(Version("1.0.0-dev01"))
                extension.validateMavenVersion()
            },
            validateWithoutKmp = { extension ->
                assertThat(
                    extension.mavenGroup
                ).isEqualTo(
                    LibraryGroup("androidx.myGroup", atomicGroupVersion = Version("1.0.0"))
                )
                assertThat(
                    extension.project.version
                ).isEqualTo(Version("1.0.0"))
                extension.validateMavenVersion()
            }
        )
    }

    private fun runAndroidExtensionTest(
        projectPath: String,
        tomlFile: String,
        validateWithoutKmp: (AndroidXExtension) -> Unit,
        validateWithKmp: (AndroidXExtension) -> Unit
    ) {
        listOf(false, true).forEach { useKmpVersions ->
            val rootProjectDir = tempDir.newFolder()
            val rootProject = ProjectBuilder.builder().withProjectDir(
                rootProjectDir
            ).build()
            val subject = ProjectBuilder.builder()
                .withParent(rootProject)
                .withName(projectPath)
                .build()
            // create the service before extensions are created so that they'll use the test service
            // we've created.
            createLibraryVersionsService(
                tomlFileContents = tomlFile,
                project = rootProject,
                useMultiplatformGroupVersions = useKmpVersions
            )
            // needed for AndroidXExtension initialization
            rootProject.setSupportRootFolder(rootProjectDir)
            // create androidx extensions
            val extension = subject.extensions
                .create<AndroidXExtension>(AndroidXImplPlugin.EXTENSION_NAME)
            if (useKmpVersions) {
                validateWithKmp(extension)
            } else {
                validateWithoutKmp(extension)
            }
        }
    }

    private fun createLibraryVersionsService(
        tomlFileContents: String,
        tomlFileName: String = "libraryversions.toml",
        composeCustomVersion: String? = null,
        composeCustomGroup: String? = null,
        useMultiplatformGroupVersions: Boolean = false,
        project: Project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder()).build()
    ): LibraryVersionsService {
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent(
            "libraryVersionsService", LibraryVersionsService::class.java
        ) { spec ->
            spec.parameters.tomlFileContents = project.provider {
                tomlFileContents
            }
            spec.parameters.tomlFileName = tomlFileName
            spec.parameters.composeCustomVersion = project.provider {
                composeCustomVersion
            }
            spec.parameters.composeCustomGroup = project.provider {
                composeCustomGroup
            }
            spec.parameters.useMultiplatformGroupVersions = project.provider {
                useMultiplatformGroupVersions
            }
        }
        return serviceProvider.get()
    }
}