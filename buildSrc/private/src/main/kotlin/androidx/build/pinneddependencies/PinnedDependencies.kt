/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.build.pinneddependencies

import androidx.build.Version
import java.io.Serializable
import org.gradle.api.GradleException
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.dataformat.toml.TomlMapper

internal const val TIP_OF_TREE_EXEMPTIONS_FILE_NAME = "tip-of-tree-dependency-exemptions.toml"

private val ALLOWED_EXEMPTION_KEYS =
    setOf("library", "dependsOn", "validThroughLibraryVersion", "reason")

/** Coordinate and versioning metadata for a project in the build. */
internal data class ProjectCoordinates(
    val projectPath: String,
    val groupId: String,
    val artifactId: String,
    val version: Version? = null,
    val versionGroup: String? = null,
) : Serializable {
    val coordinate: String
        get() = "$groupId:$artifactId"

    fun releasesTogetherWith(other: ProjectCoordinates): Boolean =
        versionGroup != null && versionGroup == other.versionGroup
}

/** An entry in [TIP_OF_TREE_EXEMPTIONS_FILE_NAME]. */
internal data class TipOfTreeExemption(
    val library: String,
    val dependsOn: String,
    val validThroughLibraryVersion: String,
    val reason: String,
) : Serializable

/**
 * Parses [TIP_OF_TREE_EXEMPTIONS_FILE_NAME] contents into a list of [TipOfTreeExemption] entries.
 */
internal fun parseTipOfTreeExemptions(
    tomlContents: String?,
    fileName: String = TIP_OF_TREE_EXEMPTIONS_FILE_NAME,
): List<TipOfTreeExemption> {
    if (tomlContents.isNullOrBlank()) return emptyList()
    val parsed: JsonNode =
        try {
            TomlMapper().readTree(tomlContents)
        } catch (e: JacksonException) {
            val location = e.location
            val locationDesc =
                if (location != null && location.lineNr != -1) {
                    "line ${location.lineNr}, column ${location.columnNr}: "
                } else {
                    ""
                }
            throw GradleException(
                "$fileName has issues.\n$fileName:$locationDesc${e.originalMessage ?: e.message}",
                e,
            )
        }

    if (parsed.isNull || parsed.isMissingNode) return emptyList()
    if (!parsed.isObject) {
        throw GradleException("$fileName is not a valid TOML table")
    }

    val unexpectedKeys = parsed.propertyNames().toSet() - "tipOfTreeExemptions"
    if (unexpectedKeys.isNotEmpty()) {
        throw GradleException(
            "$fileName contains unexpected table/key(s): $unexpectedKeys. Expected '[[tipOfTreeExemptions]]'."
        )
    }

    val entries = parsed.get("tipOfTreeExemptions") ?: return emptyList()
    if (!entries.isArray) {
        throw GradleException("$fileName: tipOfTreeExemptions is not an array")
    }

    return entries.mapIndexed { index, table ->
        if (!table.isObject) {
            throw GradleException("$fileName: tipOfTreeExemptions[$index] is not a table")
        }

        val extraKeys = table.propertyNames().toSet() - ALLOWED_EXEMPTION_KEYS
        if (extraKeys.isNotEmpty()) {
            throw GradleException(
                "$fileName: tipOfTreeExemptions[$index] contains unrecognized key(s): $extraKeys"
            )
        }

        fun requireString(key: String): String =
            table.get(key)?.asString()
                ?: throw GradleException(
                    "$fileName: tipOfTreeExemptions[$index] is missing required key \"$key\""
                )

        TipOfTreeExemption(
            library = requireString("library"),
            dependsOn = requireString("dependsOn"),
            validThroughLibraryVersion = requireString("validThroughLibraryVersion"),
            reason = requireString("reason"),
        )
    }
}

/**
 * Suggests a plausible future version to seed an exemption with, so that the value in the error
 * message models the convention rather than expiring at the next bump.
 */
internal fun suggestedExemptionVersion(current: Version): String =
    with(current) {
        val base = "$major.$minor.$patch"
        when {
            isAlpha() || isDev() -> "$base-beta01"
            isBeta() -> "$base-rc01"
            isRC() -> base
            else -> "$major.${minor + 1}.0-alpha01"
        }
    }

/** Returns the verification error messages for [library]'s tip-of-tree dependencies. */
internal fun findVerificationErrors(
    library: ProjectCoordinates,
    dependencies: Collection<ProjectCoordinates>,
    exemptions: Collection<TipOfTreeExemption>,
): List<String> {
    val applicableExemptions =
        exemptions.filter { it.library == library.projectPath }.associateBy { it.dependsOn }

    return dependencies
        .distinctBy { it.projectPath }
        .filterNot { it.projectPath == library.projectPath || library.releasesTogetherWith(it) }
        .mapNotNull { dependency ->
            val exemption = applicableExemptions[dependency.projectPath]
            val validThrough = exemption?.let { Version.parseOrNull(it.validThroughLibraryVersion) }

            when {
                exemption == null -> formatShouldBePinnedError(library, dependency)
                !exemption.reason.containsBug() ->
                    formatMissingBugError(library, dependency, exemption)
                validThrough == null ->
                    formatUnparseableVersionError(library, dependency, exemption)
                library.version != null && library.version > validThrough ->
                    formatExpiredError(library, dependency, exemption)
                else -> null
            }
        }
}

private fun formatShouldBePinnedError(
    library: ProjectCoordinates,
    dependency: ProjectCoordinates,
): String {
    val prebuiltsPath =
        "prebuilts/androidx/internal/${dependency.groupId.replace('.', '/')}/${dependency.artifactId}"
    val depArtifact = dependency.artifactId
    return """
        ${library.projectPath} depends on tip-of-tree ${dependency.projectPath}.
        They release separately (${library.versionGroup ?: "independent"} vs ${dependency.versionGroup ?: "independent"}).

        This couples ${library.projectPath}'s release schedule to ${dependency.projectPath}'s, preventing it from releasing independently.

        In ${library.projectPath}'s build file, change:
          - <configuration>(project("${dependency.projectPath}"))
          + <configuration>("${dependency.coordinate}:<latest-version>")
        (Pick the latest released version in $prebuiltsPath)

        CI still builds this against tip-of-tree $depArtifact via -Pandroidx.useMaxDepVersions, so pinning does not reduce head coverage.

        If ${library.projectPath} genuinely needs unreleased $depArtifact APIs, add an exemption to $TIP_OF_TREE_EXEMPTIONS_FILE_NAME:
          [[tipOfTreeExemptions]]
          library = "${library.projectPath}"
          dependsOn = "${dependency.projectPath}"
          # ${library.projectPath}'s own version, not ${dependency.projectPath}'s.
          # Its version is ${library.version?.let { "$it today" } ?: "unspecified"}
          validThroughLibraryVersion = "${library.version?.let { suggestedExemptionVersion(it) } ?: "<a future version of ${library.projectPath}>"}"
          reason = "b/NNNNN - <which unreleased $depArtifact API you need>"
    """
        .trimIndent()
}

private fun formatExpiredError(
    library: ProjectCoordinates,
    dependency: ProjectCoordinates,
    exemption: TipOfTreeExemption,
): String =
    """
        ${library.projectPath} is at version ${library.version}, which has moved past ${exemption.validThroughLibraryVersion} (the version its exemption for ${dependency.projectPath} in $TIP_OF_TREE_EXEMPTIONS_FILE_NAME was valid through).

        Pin ${dependency.coordinate} to a released version, or, if it is still genuinely needed, raise validThroughLibraryVersion and refresh the reason. Raising it is a decision to keep paying for the coupling, not a routine version bump.

        Current reason: ${exemption.reason}
    """
        .trimIndent()

private fun formatMissingBugError(
    library: ProjectCoordinates,
    dependency: ProjectCoordinates,
    exemption: TipOfTreeExemption,
) =
    """
    The exemption for ${library.projectPath} -> ${dependency.projectPath} in $TIP_OF_TREE_EXEMPTIONS_FILE_NAME must give a reason referencing a bug (b/NNNNN or a GitHub issue URL). Found: "${exemption.reason}"
    """
        .trimIndent()

private fun formatUnparseableVersionError(
    library: ProjectCoordinates,
    dependency: ProjectCoordinates,
    exemption: TipOfTreeExemption,
) =
    """
        The exemption for ${library.projectPath} -> ${dependency.projectPath} in $TIP_OF_TREE_EXEMPTIONS_FILE_NAME has validThroughLibraryVersion "${exemption.validThroughLibraryVersion}", which is not a valid version. It must be a version of ${library.projectPath} itself (e.g., "${library.version ?: "1.0.0-alpha01"}").
    """
        .trimIndent()

private fun String.containsBug() = contains("b/") || (contains("github.com") && contains("issues"))
