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
    @get:Rule val tempDir = TemporaryFolder()

    @Test
    fun basic() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1" }
            G2 = { group = "g.g2"}
        """
                    .trimIndent()
            )
        assertThat(service.libraryGroups["G1"])
            .isEqualTo(LibraryGroup(group = "g.g1", atomicGroupVersion = Version("1.2.3")))
        assertThat(service.libraryGroups["G2"])
            .isEqualTo(LibraryGroup(group = "g.g2", atomicGroupVersion = null))
    }

    @Test
    fun invalidToml() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1" }
            G1 = { group = "g.g1"}
        """
                    .trimIndent()
            )
        assertThrows<Exception> { service.libraryGroups["G1"] }
            .hasMessageThat()
            .contains(
                "libraryversions.toml:line 5, column 1: G1 previously defined at line 4, column 1"
            )
    }

    @Test
    fun missingVersionReference() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.doesNotExist" }
        """
                    .trimIndent()
            )
        val result = runCatching { service.libraryGroups["G1"] }
        assertThat(result.exceptionOrNull())
            .hasMessageThat()
            .contains("Group entry g.g1 specifies doesNotExist, but such version doesn't exist")
    }

    @Test
    fun malformedVersionReference() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "v1" }
        """
                    .trimIndent()
            )
        val result = runCatching { service.libraryGroups["G1"] }
        assertThat(result.exceptionOrNull())
            .hasMessageThat()
            .contains("Group entry atomicGroupVersion is expected to start with versions")
    }

    @Test
    fun overrideInclude() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1", overrideInclude = [ ":otherGroup:subproject" ]}
            """
            )
        assertThat(service.overrideLibraryGroupsByProjectPath.get(":otherGroup:subproject"))
            .isEqualTo(LibraryGroup(group = "g.g1", atomicGroupVersion = Version("1.2.3")))
        assertThat(service.overrideLibraryGroupsByProjectPath.get(":normalGroup:subproject"))
            .isEqualTo(null)
    }

    @Test
    fun duplicateGroupIdsWithoutOverrideInclude() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1" }
            G2 = { group = "g.g1", atomicGroupVersion = "versions.V1" }
            """
            )

        assertThrows<Exception> { service.libraryGroupsByGroupId["g.g1"] }
            .hasMessageThat()
            .contains(
                "Duplicate library group g.g1 defined in G2 does not set overrideInclude. " +
                    "Declarations beyond the first can only have an effect if they set overrideInclude"
            )
    }

    @Test
    fun duplicateGroupIdsWithOverrideInclude() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            G1 = { group = "g.g1", atomicGroupVersion = "versions.V1" }
            G2 = { group = "g.g1", atomicGroupVersion = "versions.V1", overrideInclude = ["sample"] }
            """
            )

        assertThat(service.libraryGroupsByGroupId["g.g1"])
            .isEqualTo(LibraryGroup(group = "g.g1", atomicGroupVersion = Version("1.2.3")))
    }

    private fun runAndroidExtensionTest(
        projectPath: String,
        tomlFile: String,
        validateWithoutKmp: (AndroidXExtension) -> Unit,
        validateWithKmp: (AndroidXExtension) -> Unit
    ) {
        listOf(false, true).forEach { useKmpVersions ->
            val rootProjectDir = tempDir.newFolder()
            val rootProject = ProjectBuilder.builder().withProjectDir(rootProjectDir).build()
            val subject =
                ProjectBuilder.builder().withParent(rootProject).withName(projectPath).build()
            // create the service before extensions are created so that they'll use the test service
            // we've created.
            createLibraryVersionsService(
                tomlFileContents = tomlFile,
                project = rootProject,
            )
            // needed for AndroidXExtension initialization
            rootProject.setSupportRootFolder(rootProjectDir)
            // create androidx extensions
            val extension =
                subject.extensions.create<AndroidXExtension>(AndroidXImplPlugin.EXTENSION_NAME)
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
        project: Project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder()).build()
    ): LibraryVersionsService {
        val serviceProvider =
            project.gradle.sharedServices.registerIfAbsent(
                "libraryVersionsService",
                LibraryVersionsService::class.java
            ) { spec ->
                spec.parameters.tomlFileContents = project.provider { tomlFileContents }
                spec.parameters.tomlFileName = tomlFileName
            }
        return serviceProvider.get()
    }
}
