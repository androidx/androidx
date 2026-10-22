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

import androidx.build.pinneddependencies.DependencyExemption
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
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
            .isEqualTo(LibraryGroup(group = "g.g1", atomicGroupVersion = Version.parse("1.2.3")))
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
            .contains("libraryversions.toml:line 5, column 23: Duplicate key")
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
            .isEqualTo(LibraryGroup(group = "g.g1", atomicGroupVersion = Version.parse("1.2.3")))
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
            .contains("Multiple atomic groups defined with the same Maven group ID: g.g1")
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

        assertThrows<Exception> { service.libraryGroupsByGroupId["g.g1"] }
            .hasMessageThat()
            .contains("Multiple atomic groups defined with the same Maven group ID: g.g1")
    }

    @Test
    fun libraryVersionsDirectAccess() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                V2 = "2.0.0-alpha01"
                [groups]
                """
                    .trimIndent()
            )
        assertThat(service.libraryVersions["V1"]).isEqualTo(Version.parse("1.2.3"))
        assertThat(service.libraryVersions["V2"]).isEqualTo(Version.parse("2.0.0-alpha01"))
    }

    @Test
    fun malformedVersionFormat() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "not_a_valid_semver"
                [groups]
                """
                    .trimIndent()
            )
        assertThrows<Exception> { service.libraryVersions["V1"] }
            .hasMessageThat()
            .contains("V1 does not match expected format - not_a_valid_semver")
    }

    @Test
    fun missingVersionsTable() {
        val service =
            createLibraryVersionsService(
                """
                [groups]
                G1 = { group = "g.g1" }
                """
                    .trimIndent()
            )
        assertThrows<Exception> { service.libraryVersions }
            .hasMessageThat()
            .contains("Library versions toml file is missing [versions] table")
    }

    @Test
    fun missingGroupsTable() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                """
                    .trimIndent()
            )
        assertThrows<Exception> { service.libraryGroups }
            .hasMessageThat()
            .contains("Library versions toml file is missing [groups] table")
    }

    @Test
    fun missingGroupFieldInGroupDefinition() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                [groups]
                G1 = { atomicGroupVersion = "versions.V1" }
                """
                    .trimIndent()
            )
        assertThrows<Exception> { service.libraryGroups["G1"] }
            .hasMessageThat()
            .contains("Group entry G1 is missing 'group' field")
    }

    @Test
    fun duplicateNonAtomicGroupIdsWithoutOverrideInclude() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                [groups]
                G1 = { group = "g.g1" }
                G2 = { group = "g.g1" }
                """
                    .trimIndent()
            )
        assertThrows<Exception> { service.libraryGroupsByGroupId["g.g1"] }
            .hasMessageThat()
            .contains(
                "Duplicate library group g.g1 defined in G2 does not set overrideInclude. " +
                    "Declarations beyond the first can only have an effect if they set overrideInclude"
            )
    }

    @Test
    fun duplicateNonAtomicGroupIdsWithOverrideInclude() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                [groups]
                G1 = { group = "g.g1" }
                G2 = { group = "g.g1", overrideInclude = [":other:project"] }
                """
                    .trimIndent()
            )
        assertThat(service.libraryGroupsByGroupId["g.g1"]?.group).isEqualTo("g.g1")
        assertThat(service.overrideLibraryGroupsByProjectPath[":other:project"]?.group)
            .isEqualTo("g.g1")
    }

    @Test
    fun allowedAtomicGroupExceptionWithOverrideInclude() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.0.0"
                V2 = "1.1.0"
                [groups]
                CAM1 = { group = "androidx.camera", atomicGroupVersion = "versions.V1" }
                CAM2 = { group = "androidx.camera", atomicGroupVersion = "versions.V2", overrideInclude = [":camera:camera-extensions"] }
                """
                    .trimIndent()
            )
        assertThat(service.libraryGroupsByGroupId["androidx.camera"]?.atomicGroupVersion)
            .isEqualTo(Version.parse("1.0.0"))
        assertThat(service.libraryGroupsByGroupId["androidx.camera"]?.requireSameVersion).isTrue()
        assertThat(
                service.overrideLibraryGroupsByProjectPath[":camera:camera-extensions"]
                    ?.atomicGroupVersion
            )
            .isEqualTo(Version.parse("1.1.0"))
        assertThat(
                service.overrideLibraryGroupsByProjectPath[":camera:camera-extensions"]
                    ?.requireSameVersion
            )
            .isFalse()
    }

    // Edge case: when an overrideInclude entry specifies the same atomicGroupVersion as the base
    // group (or is the sole entry mapping a project from another directory), default to keeping
    // the group atomic (requireSameVersion = true).
    @Test
    fun overrideIncludeWithSameAtomicGroupVersionKeepsRequireSameVersionTrue() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.0.0"
                [groups]
                M3 = { group = "androidx.compose.material3", atomicGroupVersion = "versions.V1" }
                M3_NAV = { group = "androidx.compose.material3", atomicGroupVersion = "versions.V1", overrideInclude = [":compose:material3:material3-adaptive-navigation-suite"] }
                """
                    .trimIndent()
            )
        assertThat(
                service.overrideLibraryGroupsByProjectPath[
                        ":compose:material3:material3-adaptive-navigation-suite"]
                    ?.requireSameVersion
            )
            .isTrue()
    }

    @Test
    fun invalidTomlSyntax() {
        val service =
            createLibraryVersionsService(
                """
                [versions
                V1 = "1.2.3"
                """
                    .trimIndent()
            )
        assertThrows<Exception> { service.libraryVersions }
            .hasMessageThat()
            .contains("libraryversions.toml file has issues.")
    }

    @Test
    fun exemptionsFor_absentFileIsEmpty() {
        val service = createLibraryVersionsService("[versions]\n[groups]\n", exemptions = null)
        assertThat(service.exemptionsFor(":fragment:fragment")).isEmpty()
    }

    @Test
    fun exemptionsFor_emptyListIsEmpty() {
        val service = createLibraryVersionsService("[versions]\n[groups]\n")
        assertThat(service.exemptionsFor(":fragment:fragment")).isEmpty()
    }

    @Test
    fun exemptionsFor_parsesEntries() {
        val service =
            createLibraryVersionsService(
                "[versions]\n[groups]\n",
                exemptions =
                    """
                    [[exemption]]
                    library = ":fragment:fragment"
                    dependsOn = ":tracing:tracing"
                    validThroughLibraryVersion = "1.9.0-rc01"
                    reason = "b/12345 - needs an unreleased API"
                    """
                        .trimIndent(),
            )
        val expected =
            DependencyExemption(
                library = ":fragment:fragment",
                dependsOn = ":tracing:tracing",
                validThroughLibraryVersion = "1.9.0-rc01",
                reason = "b/12345 - needs an unreleased API",
            )
        assertThat(service.exemptionsFor(":fragment:fragment")).containsExactly(expected)
        assertThat(service.exemptionsFor(":other:module")).isEmpty()
    }

    @Test
    fun versionGroupFor_atomicGroupReturnsGroupVersionName() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            V1 = "1.2.3"
            [groups]
            COMPOSE_UI = { group = "androidx.compose.ui", atomicGroupVersion = "versions.V1" }
            """
            )
        assertThat(
                service.versionGroupFor(
                    ":compose:ui:ui",
                    "androidx.compose.ui",
                    mavenVersionGroup = null,
                )
            )
            .isEqualTo("V1")
    }

    @Test
    fun versionGroupFor_nonAtomicGroupReturnsNull() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            CORE = "1.2.3"
            [groups]
            CORE = { group = "androidx.core" }
            """
            )
        assertThat(service.versionGroupFor(":core:core", "androidx.core", mavenVersionGroup = null))
            .isNull()
        assertThat(
                service.versionGroupFor(
                    ":core:core-splashscreen",
                    "androidx.core",
                    mavenVersionGroup = null,
                )
            )
            .isNull()
    }

    @Test
    fun versionGroupFor_ownMavenVersionOverridesAtomicGroup() {
        val service =
            createLibraryVersionsService(
                """
            [versions]
            COMPOSE_MATERIAL3 = "1.6.0-alpha01"
            COMPOSE_MATERIAL3_A2UI_QUARANTINE = "1.0.0-alpha02"
            [groups]
            COMPOSE_MATERIAL3 = { group = "androidx.compose.material3", atomicGroupVersion = "versions.COMPOSE_MATERIAL3" }
            """
            )
        assertThat(
                service.versionGroupFor(
                    ":compose:material3:material3-a2ui",
                    "androidx.compose.material3",
                    mavenVersionGroup = "COMPOSE_MATERIAL3_A2UI_QUARANTINE",
                )
            )
            .isEqualTo("COMPOSE_MATERIAL3_A2UI_QUARANTINE")
    }

    @Test
    fun mavenCoordinatesToProjectPathMap_standardAndNestedProjects() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                [groups]
                CORE = { group = "androidx.core", atomicGroupVersion = "versions.V1" }
                COMPOSE_UI = { group = "androidx.compose.ui", atomicGroupVersion = "versions.V1" }
                """
                    .trimIndent(),
                allPossibleProjects =
                    listOf(
                        IncludedProject(":core:core", "core/core"),
                        IncludedProject(":compose:ui:ui-graphics", "compose/ui/ui-graphics"),
                    ),
            )
        assertThat(service.mavenCoordinatesToProjectPathMap)
            .containsExactly(
                "androidx.core:core",
                ":core:core",
                "androidx.compose.ui:ui-graphics",
                ":compose:ui:ui-graphics",
            )
    }

    @Test
    fun mavenCoordinatesToProjectPathMap_overrideInclude() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                [groups]
                CORE = { group = "androidx.core", atomicGroupVersion = "versions.V1" }
                CUSTOM = { group = "androidx.custom", atomicGroupVersion = "versions.V1", overrideInclude = [":core:core-custom", ":other:subproject"] }
                """
                    .trimIndent(),
                allPossibleProjects =
                    listOf(
                        IncludedProject(":core:core", "core/core"),
                        IncludedProject(":core:core-custom", "core/core-custom"),
                        IncludedProject(":other:subproject", "other/subproject"),
                    ),
            )
        assertThat(service.mavenCoordinatesToProjectPathMap)
            .containsExactly(
                "androidx.core:core",
                ":core:core",
                "androidx.custom:core-custom",
                ":core:core-custom",
                "androidx.custom:subproject",
                ":other:subproject",
            )
    }

    @Test
    fun mavenCoordinatesToProjectPathMap_samplesInheritParentGroup() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                [groups]
                CORE = { group = "androidx.core", atomicGroupVersion = "versions.V1" }
                COMPOSE_UI = { group = "androidx.compose.ui", atomicGroupVersion = "versions.V1" }
                """
                    .trimIndent(),
                allPossibleProjects =
                    listOf(
                        IncludedProject(":core:core:samples", "core/core/samples"),
                        IncludedProject(":compose:ui:ui:samples", "compose/ui/ui/samples"),
                    ),
            )
        assertThat(service.mavenCoordinatesToProjectPathMap)
            .containsExactly(
                "androidx.core:samples",
                ":core:core:samples",
                "androidx.compose.ui:samples",
                ":compose:ui:ui:samples",
            )
    }

    @Test
    fun mavenCoordinatesToProjectPathMap_externalProjects() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                [groups]
                LIBYUV = { group = "libyuv", atomicGroupVersion = "versions.V1" }
                """
                    .trimIndent(),
                allPossibleProjects =
                    listOf(IncludedProject(":external:libyuv", "external/libyuv")),
            )
        assertThat(service.mavenCoordinatesToProjectPathMap)
            .containsExactly("libyuv:libyuv", ":external:libyuv")
    }

    @Test
    fun mavenCoordinatesToProjectPathMap_unmatchedProjectsExcluded() {
        val service =
            createLibraryVersionsService(
                """
                [versions]
                V1 = "1.2.3"
                [groups]
                CORE = { group = "androidx.core", atomicGroupVersion = "versions.V1" }
                """
                    .trimIndent(),
                allPossibleProjects =
                    listOf(
                        IncludedProject(":icing", "icing"),
                        IncludedProject(":unknown:project", "unknown/project"),
                        IncludedProject(":core:core", "core/core"),
                    ),
            )
        assertThat(service.mavenCoordinatesToProjectPathMap)
            .containsExactly("androidx.core:core", ":core:core")
    }

    private fun createLibraryVersionsService(
        tomlFileContents: String,
        tomlFileName: String = "libraryversions.toml",
        exemptions: String? = "exemption = []",
        allPossibleProjects: List<IncludedProject> = emptyList(),
        project: Project = ProjectBuilder.builder().withProjectDir(tempDir.newFolder()).build(),
    ): LibraryVersionsService {
        val serviceProvider =
            project.gradle.sharedServices.registerIfAbsent(
                "libraryVersionsService",
                LibraryVersionsService::class.java,
            ) { spec ->
                spec.parameters.tomlFileContents.set(tomlFileContents)
                spec.parameters.tomlFileName.set(tomlFileName)
                spec.parameters.dependencyExemptionsFileContents.set(exemptions)
                spec.parameters.allPossibleProjects.set(allPossibleProjects)
            }
        return serviceProvider.get()
    }
}
