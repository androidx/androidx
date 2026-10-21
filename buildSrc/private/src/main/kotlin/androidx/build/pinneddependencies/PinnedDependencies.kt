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

internal const val EXEMPTIONS_FILE_NAME = "tip-of-tree-or-alpha-dependency-exemptions.toml"

internal const val EXEMPTION_TABLE = "exemption"

private val ALLOWED_EXEMPTION_KEYS =
    setOf("library", "dependsOn", "validThroughLibraryVersion", "reason")

/** Coordinate and versioning metadata for a project in the build. */
internal data class LibraryCoordinates(
    val projectPath: String?,
    val groupId: String,
    val artifactId: String,
    val version: Version? = null,
    val versionGroup: String? = null,
) : Serializable {
    val coordinate: String
        get() = "$groupId:$artifactId"

    val isPinned: Boolean
        get() = projectPath == null

    fun releasesTogetherWith(other: LibraryCoordinates): Boolean =
        versionGroup != null && versionGroup == other.versionGroup
}

/** An entry in [EXEMPTIONS_FILE_NAME]. */
internal data class DependencyExemption(
    val library: String,
    val dependsOn: String,
    val validThroughLibraryVersion: String,
    val reason: String,
) : Serializable

/** Parses [EXEMPTIONS_FILE_NAME] contents into a list of [DependencyExemption] entries. */
internal fun parseDependencyExemptions(
    tomlContents: String?,
    fileName: String = EXEMPTIONS_FILE_NAME,
): List<DependencyExemption> {
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

    val unexpectedKeys = parsed.propertyNames().toSet() - EXEMPTION_TABLE
    if (unexpectedKeys.isNotEmpty()) {
        throw GradleException(
            "$fileName contains unexpected table/key(s): $unexpectedKeys. Expected '[[$EXEMPTION_TABLE]]'."
        )
    }

    val entries = parsed.get(EXEMPTION_TABLE) ?: return emptyList()
    if (!entries.isArray) {
        throw GradleException("$fileName: $EXEMPTION_TABLE is not an array")
    }

    return entries.mapIndexed { index, table ->
        if (!table.isObject) {
            throw GradleException("$fileName: $EXEMPTION_TABLE[$index] is not a table")
        }

        val extraKeys = table.propertyNames().toSet() - ALLOWED_EXEMPTION_KEYS
        if (extraKeys.isNotEmpty()) {
            throw GradleException(
                "$fileName: $EXEMPTION_TABLE[$index] contains unrecognized key(s): $extraKeys"
            )
        }

        fun requireString(key: String): String =
            table.get(key)?.asString()
                ?: throw GradleException(
                    "$fileName: $EXEMPTION_TABLE[$index] is missing required key \"$key\""
                )

        DependencyExemption(
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

/** Returns [library]'s tip-of-tree and alpha-pinned [dependencies] that release separately. */
internal fun findDependenciesNeedingExemption(
    library: LibraryCoordinates,
    dependencies: Collection<LibraryCoordinates>,
): List<LibraryCoordinates> =
    dependencies
        .filter { !it.isPinned || it.isPinnedToAlpha() }
        .distinctBy { it.coordinate }
        .filterNot { it.coordinate == library.coordinate || library.releasesTogetherWith(it) }

private fun LibraryCoordinates.isPinnedToAlpha(): Boolean =
    isPinned && version?.isFinalApi() == false

/**
 * A new exemption for [library] depending on [dependsOn], seeded with a suggested expiry version
 * and a placeholder reason that a human must replace.
 */
internal fun placeholderExemption(
    library: String,
    libraryVersion: Version?,
    dependsOn: String,
): DependencyExemption =
    DependencyExemption(
        library = library,
        dependsOn = dependsOn,
        validThroughLibraryVersion =
            libraryVersion?.let { suggestedExemptionVersion(it) } ?: "1.0.0-alpha01",
        reason = "b/TODO - <reason>",
    )

/** Returns the verification error messages for [library]'s [dependencies]. */
internal fun findVerificationErrors(
    library: LibraryCoordinates,
    dependencies: Collection<LibraryCoordinates>,
    exemptions: Collection<DependencyExemption>,
): List<String> {
    val applicableExemptions =
        exemptions.filter { it.library == library.coordinate }.associateBy { it.dependsOn }

    return findDependenciesNeedingExemption(library, dependencies).mapNotNull { dependency ->
        val exemption = applicableExemptions[dependency.coordinate]
        val validThrough = exemption?.let { Version.parseOrNull(it.validThroughLibraryVersion) }

        when {
            exemption == null && dependency.isPinned -> formatAlphaPinError(library, dependency)
            exemption == null -> formatShouldBePinnedError(library, dependency)
            !exemption.reason.containsBug() -> formatMissingBugError(library, dependency, exemption)
            validThrough == null -> formatUnparseableVersionError(library, dependency, exemption)
            library.version != null && library.version > validThrough ->
                formatExpiredError(library, dependency, exemption)
            else -> null
        }
    }
}

private fun formatShouldBePinnedError(
    library: LibraryCoordinates,
    dependency: LibraryCoordinates,
): String =
    """
        ${library.coordinate} depends on tip-of-tree ${dependency.coordinate}.
        They release separately (${library.versionGroup ?: "independent"} vs ${dependency.versionGroup ?: "independent"}).

        This couples ${library.coordinate}'s release schedule to ${dependency.coordinate}'s, preventing it from releasing independently.

        In ${library.coordinate}'s build file, change:
          - <configuration>(project("${dependency.projectPath}"))
          + <configuration>("${dependency.coordinate}:<latest-version>")
        (Pick the latest API stable version (beta, rc, stable) in ${dependency.prebuiltsPath}; alpha versions are not allowed)

        CI still builds this against tip-of-tree ${dependency.artifactId} via -Pandroidx.useMaxDepVersions, so pinning does not reduce head coverage.

        If ${library.coordinate} genuinely needs unreleased ${dependency.artifactId} APIs, add an exemption to $EXEMPTIONS_FILE_NAME:
    """
        .trimIndent() + "\n" + formatExemptionSnippet(library, dependency, "unreleased")

private fun formatAlphaPinError(
    library: LibraryCoordinates,
    dependency: LibraryCoordinates,
): String =
    """
        ${library.coordinate} depends on ${dependency.coordinate}:${dependency.version}, an alpha release.
        They release separately (${library.versionGroup ?: "independent"} vs ${dependency.versionGroup ?: "independent"}).

        Alpha APIs may still change, so this couples ${library.coordinate} to ${dependency.coordinate}'s unstable API and release schedule.

        In ${library.coordinate}'s build file, change:
          - <configuration>("${dependency.coordinate}:${dependency.version}")
          + <configuration>("${dependency.coordinate}:<latest-version>")
        (Pick the latest beta, rc, or stable version in ${dependency.prebuiltsPath})

        If ${library.coordinate} genuinely needs alpha ${dependency.artifactId} APIs, add an exemption to $EXEMPTIONS_FILE_NAME.
        It only helps while ${library.coordinate} is in alpha: once it reaches beta, the stability rule rejects alpha dependencies regardless of exemptions.
    """
        .trimIndent() + "\n" + formatExemptionSnippet(library, dependency, "alpha")

private fun formatExemptionSnippet(
    library: LibraryCoordinates,
    dependency: LibraryCoordinates,
    apiKind: String,
): String =
    """
          [[$EXEMPTION_TABLE]]
          library = "${library.coordinate}"
          dependsOn = "${dependency.coordinate}"
          # ${library.coordinate}'s own version, not ${dependency.coordinate}'s.
          # Its version is ${library.version?.let { "$it today" } ?: "unspecified"}
          validThroughLibraryVersion = "${library.version?.let { suggestedExemptionVersion(it) } ?: "<a future version of ${library.coordinate}>"}"
          reason = "b/NNNNN - <which $apiKind ${dependency.artifactId} API you need>"
    """
        .trimIndent()
        .prependIndent("  ")

private val LibraryCoordinates.prebuiltsPath: String
    get() = "prebuilts/androidx/internal/${groupId.replace('.', '/')}/$artifactId"

private fun formatExpiredError(
    library: LibraryCoordinates,
    dependency: LibraryCoordinates,
    exemption: DependencyExemption,
): String =
    """
        ${library.coordinate} is at version ${library.version}, which has moved past ${exemption.validThroughLibraryVersion} (the version its exemption for ${dependency.coordinate} in $EXEMPTIONS_FILE_NAME was valid through).

        Pin ${dependency.coordinate} to a beta, rc, or stable version, or, if it is still genuinely needed, raise validThroughLibraryVersion and refresh the reason. Raising it is a decision to keep paying for the coupling, not a routine version bump.

        Current reason: ${exemption.reason}
    """
        .trimIndent()

private fun formatMissingBugError(
    library: LibraryCoordinates,
    dependency: LibraryCoordinates,
    exemption: DependencyExemption,
) =
    """
    The exemption for ${library.coordinate} -> ${dependency.coordinate} in $EXEMPTIONS_FILE_NAME must give a reason referencing a bug (b/NNNNN or a GitHub issue URL). Found: "${exemption.reason}"
    """
        .trimIndent()

private fun formatUnparseableVersionError(
    library: LibraryCoordinates,
    dependency: LibraryCoordinates,
    exemption: DependencyExemption,
) =
    """
        The exemption for ${library.coordinate} -> ${dependency.coordinate} in $EXEMPTIONS_FILE_NAME has validThroughLibraryVersion "${exemption.validThroughLibraryVersion}", which is not a valid version. It must be a version of ${library.coordinate} itself (e.g., "${library.version ?: "1.0.0-alpha01"}").
    """
        .trimIndent()

private fun String.containsBug() = contains("b/") || (contains("github.com") && contains("issues"))
