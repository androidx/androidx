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

    private fun createLibraryVersionsService(
        tomlFile: String,
        composeCustomVersion: String? = null,
        composeCustomGroup: String? = null,
        useMultiplatformGroupVersions: Boolean = false
    ): LibraryVersionsService {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder()).build()
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent(
            "libraryVersionsService", LibraryVersionsService::class.java
        ) { spec ->
            spec.parameters.tomlFile = project.provider {
                tomlFile
            }
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