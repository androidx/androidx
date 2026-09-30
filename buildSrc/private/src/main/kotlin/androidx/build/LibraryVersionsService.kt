/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.build.pinneddependencies.TIP_OF_TREE_EXEMPTIONS_FILE_NAME
import androidx.build.pinneddependencies.TipOfTreeExemption
import androidx.build.pinneddependencies.parseTipOfTreeExemptions
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.dataformat.toml.TomlMapper

/** Loads Library groups and versions from a specified TOML file. */
abstract class LibraryVersionsService : BuildService<LibraryVersionsService.Parameters> {
    interface Parameters : BuildServiceParameters {
        val tomlFileName: Property<String>
        val tomlFileContents: Property<String>
        val tipOfTreeExemptionsFileName: Property<String>
        val tipOfTreeExemptionsFileContents: Property<String>
    }

    private val parsedTomlFile: JsonNode by lazy {
        val fileName = parameters.tomlFileName.get()
        try {
            TomlMapper().readTree(parameters.tomlFileContents.get())
        } catch (e: JacksonException) {
            val location = e.location
            val locationDesc =
                if (location != null && location.lineNr != -1) {
                    "line ${location.lineNr}, column ${location.columnNr}: "
                } else {
                    ""
                }
            throw Exception(
                "$fileName file has issues.\n" +
                    "$fileName:$locationDesc${e.originalMessage ?: e.message}",
                e,
            )
        }
    }

    /**
     * Exemptions declared in [TIP_OF_TREE_EXEMPTIONS_FILE_NAME], parsed only if something reads
     * them.
     *
     * An absent file parses to an empty list rather than failing, so that a checkout without it, or
     * a build that never consults it, behaves as though nothing is exempted.
     */
    private val tipOfTreeExemptions: List<TipOfTreeExemption> by lazy {
        parseTipOfTreeExemptions(
            parameters.tipOfTreeExemptionsFileContents.orNull,
            parameters.tipOfTreeExemptionsFileName.get(),
        )
    }

    private val tipOfTreeExemptionsByLibrary: Map<String, List<TipOfTreeExemption>> by lazy {
        tipOfTreeExemptions.groupBy { it.library }
    }

    /** Returns only the exemptions applicable to [projectPath]. */
    internal fun exemptionsFor(projectPath: String): List<TipOfTreeExemption> =
        tipOfTreeExemptionsByLibrary[projectPath] ?: emptyList()

    private fun getTable(key: String): JsonNode {
        val table = parsedTomlFile.get(key)
        if (table == null || !table.isObject) {
            throw GradleException("Library versions toml file is missing [$key] table")
        }
        return table
    }

    // map from name of constant to Version
    val libraryVersions: Map<String, Version> by lazy {
        val versions = getTable("versions")
        versions.propertyNames().associateWith { versionName ->
            val versionValue = versions.get(versionName)!!.asString()
            Version.parseOrNull(versionValue)
                ?: throw GradleException(
                    "$versionName does not match expected format - $versionValue"
                )
        }
    }

    // map of library groups keyed by their variable name in the toml file
    val libraryGroups: Map<String, LibraryGroup> by lazy {
        val result = mutableMapOf<String, LibraryGroup>()
        for (association in libraryGroupAssociations) {
            result[association.declarationName] = association.libraryGroup
        }
        result
    }

    // map of library groups keyed by group name
    val libraryGroupsByGroupId: Map<String, LibraryGroup> by lazy {
        val result = mutableMapOf<String, LibraryGroup>()
        for (association in libraryGroupAssociations) {
            // Check for duplicate groups
            val groupId = association.libraryGroup.group
            val existingAssociation = result[groupId]
            if (existingAssociation != null) {
                if (
                    existingAssociation.atomicGroupVersion != null &&
                        association.libraryGroup.atomicGroupVersion != null &&
                        existingAssociation.group !in ALLOWED_ATOMIC_GROUP_EXCEPTIONS
                ) {
                    throw GradleException(
                        "Multiple atomic groups defined with the same Maven group ID: $groupId"
                    )
                }
                if (association.overrideIncludeInProjectPaths.isEmpty()) {
                    throw GradleException(
                        "Duplicate library group $groupId defined in " +
                            "${association.declarationName} does not set overrideInclude. " +
                            "Declarations beyond the first can only have an effect if they set " +
                            "overrideInclude"
                    )
                }
            } else {
                result[groupId] = association.libraryGroup
            }
        }
        result
    }

    // map from project name to group override if applicable
    val overrideLibraryGroupsByProjectPath: Map<String, LibraryGroup> by lazy {
        val result = mutableMapOf<String, LibraryGroup>()
        for (association in libraryGroupAssociations) {
            for (overridePath in association.overrideIncludeInProjectPaths) {
                result[overridePath] = association.libraryGroup
            }
        }
        result
    }

    /**
     * Name of the set of libraries that share a version, keyed by Maven group id.
     *
     * This is the group's `atomicGroupVersion` reference when it has one, and null when it does not
     * (meaning libraries in the group version independently). Deliberately the *reference* name
     * rather than the resolved version: several groups reference `versions.COMPOSE` and so always
     * ship together, while two unrelated groups that happen to both sit at 1.0.0-alpha01 do not.
     */
    private val versionGroupByGroupId: Map<String, String?> by lazy {
        val result = mutableMapOf<String, String?>()
        for (association in libraryGroupAssociations) {
            if (association.overrideIncludeInProjectPaths.isNotEmpty()) continue
            result[association.libraryGroup.group] = association.versionGroupName
        }
        result
    }

    /** Version group names for projects whose group is set by `overrideInclude`. */
    private val versionGroupByProjectPath: Map<String, String?> by lazy {
        val result = mutableMapOf<String, String?>()
        for (association in libraryGroupAssociations) {
            for (path in association.overrideIncludeInProjectPaths) {
                result[path] = association.versionGroupName
            }
        }
        result
    }

    /**
     * The version group [projectPath] belongs to, or null if it versions on its own schedule.
     *
     * `overrideInclude` wins over the group id, because it exists precisely to put a project in a
     * different version group from the rest of its Maven group.
     */
    fun versionGroupFor(projectPath: String, groupId: String?): String? =
        versionGroupByProjectPath[projectPath] ?: groupId?.let { versionGroupByGroupId[it] }

    private val libraryGroupAssociations: List<LibraryGroupAssociation> by lazy {
        val groups = getTable("groups")

        fun readGroupVersion(groupDefinition: JsonNode, groupName: String, key: String): Version? {
            val versionRef = groupDefinition.get(key)?.asString() ?: return null
            if (!versionRef.startsWith(VersionReferencePrefix)) {
                throw GradleException(
                    "Group entry $key is expected to start with $VersionReferencePrefix"
                )
            }
            // name without `versions.`
            val atomicGroupVersionName = versionRef.removePrefix(VersionReferencePrefix)
            return libraryVersions[atomicGroupVersionName]
                ?: error(
                    "Group entry $groupName specifies $atomicGroupVersionName, but such version " +
                        "doesn't exist"
                )
        }
        groups.propertyNames().sorted().map { name ->
            // get group name
            val groupDefinition = groups.get(name)!!
            val groupName =
                groupDefinition.get("group")?.asString()
                    ?: throw GradleException("Group entry $name is missing 'group' field")

            // get group version, if any
            val atomicGroupVersion =
                readGroupVersion(
                    groupDefinition = groupDefinition,
                    groupName = groupName,
                    key = AtomicGroupVersion,
                )
            val overrideApplyToProjects =
                groupDefinition.get("overrideInclude")?.values()?.map { it.asString() }
                    ?: emptyList()

            val group = LibraryGroup(groupName, atomicGroupVersion)
            val atomicGroupVersionName =
                groupDefinition
                    .get(AtomicGroupVersion)
                    ?.asString()
                    ?.removePrefix(VersionReferencePrefix)
            LibraryGroupAssociation(
                declarationName = name,
                libraryGroup = group,
                overrideIncludeInProjectPaths = overrideApplyToProjects,
                versionGroupName = atomicGroupVersionName,
            )
        }
    }

    companion object {
        internal fun registerOrGet(project: Project): Provider<LibraryVersionsService> {
            val tomlFileName = "libraryversions.toml"
            val toml = project.lazyReadFile(tomlFileName)
            val exemptionsFileName = TIP_OF_TREE_EXEMPTIONS_FILE_NAME
            val exemptions = project.lazyReadFile(exemptionsFileName)

            return project.gradle.sharedServices.registerIfAbsent(
                "libraryVersionsService",
                LibraryVersionsService::class.java,
            ) { spec ->
                spec.parameters.tomlFileName.set(tomlFileName)
                spec.parameters.tomlFileContents.set(toml)
                spec.parameters.tipOfTreeExemptionsFileName.set(exemptionsFileName)
                spec.parameters.tipOfTreeExemptionsFileContents.set(exemptions)
            }
        }
    }
}

// a LibraryGroupSpec knows how to associate a LibraryGroup with the appropriate projects
private data class LibraryGroupAssociation(
    // the name of the variable to which it is assigned in the toml file
    val declarationName: String,
    // the group
    val libraryGroup: LibraryGroup,
    // the paths of any additional projects that this group should be assigned to
    val overrideIncludeInProjectPaths: List<String>,
    // the name identifying the set of libraries that release together with this group
    val versionGroupName: String?,
)

private const val VersionReferencePrefix = "versions."
private const val AtomicGroupVersion = "atomicGroupVersion"

// Maven groups that should be skipped for atomic duplication checks. Do not add further entries.
// TODO(b/401002936, b/401000219, b/401003097, b/401005632): Remove groups from this list
private val ALLOWED_ATOMIC_GROUP_EXCEPTIONS =
    listOf(
        "androidx.camera",
        "androidx.compose.material3",
        "androidx.lifecycle",
        "androidx.tracing",
    )
