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
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.gradle.api.GradleException
import org.junit.Test

class PinnedDependenciesTest {

    private val fragment =
        LibraryCoordinates(
            projectPath = ":fragment:fragment",
            groupId = "androidx.fragment",
            artifactId = "fragment",
            version = Version.parse("1.9.0-beta01"),
            versionGroup = "FRAGMENT",
        )

    private val tracing =
        LibraryCoordinates(
            projectPath = ":tracing:tracing",
            groupId = "androidx.tracing",
            artifactId = "tracing",
            version = Version.parse("2.0.0-beta01"),
            versionGroup = "TRACING",
        )

    private fun exemption(
        validThrough: String = "1.9.0-rc01",
        reason: String = "b/12345 - uses Trace.setCounter, which no released tracing has yet",
    ) =
        DependencyExemption(
            "androidx.fragment:fragment",
            "androidx.tracing:tracing",
            validThrough,
            reason,
        )

    @Test
    fun dependencyInAnotherVersionGroupIsReported() {
        val errors = findVerificationErrors(fragment, listOf(tracing), listOf())
        assertThat(errors).hasSize(1)
        assertThat(errors.single())
            .contains("androidx.fragment:fragment depends on tip-of-tree androidx.tracing:tracing")
    }

    @Test
    fun sameVersionGroupIsNotReported() {
        val sibling =
            fragment.copy(projectPath = ":fragment:fragment-ktx", artifactId = "fragment-ktx")
        assertThat(findVerificationErrors(fragment, listOf(sibling), listOf())).isEmpty()
    }

    @Test
    fun selfDependencyIsNotReported() {
        val self = fragment.copy(versionGroup = null)
        assertThat(findVerificationErrors(fragment, listOf(self), listOf())).isEmpty()
    }

    @Test
    fun librariesWithoutVersionGroupsNeverCountAsShippingTogether() {
        val ungrouped = fragment.copy(versionGroup = null)
        val dependency = tracing.copy(versionGroup = null)
        assertThat(findVerificationErrors(ungrouped, listOf(dependency), listOf())).hasSize(1)
    }

    @Test
    fun validExemptionSuppressesTheReport() {
        assertThat(findVerificationErrors(fragment, listOf(tracing), listOf(exemption()))).isEmpty()
    }

    @Test
    fun exemptionForADifferentDependencyDoesNotApply() {
        val other = exemption().copy(dependsOn = "androidx.collection:collection")
        assertThat(findVerificationErrors(fragment, listOf(tracing), listOf(other))).hasSize(1)
    }

    @Test
    fun exemptionForADifferentLibraryDoesNotApply() {
        val other = exemption().copy(library = "androidx.activity:activity")
        assertThat(findVerificationErrors(fragment, listOf(tracing), listOf(other))).hasSize(1)
    }

    @Test
    fun exemptionExpiresOnceTheLibraryMovesPastIt() {
        val past = fragment.copy(version = Version.parse("1.9.0-rc02"))
        val errors = findVerificationErrors(past, listOf(tracing), listOf(exemption()))
        assertThat(errors.single()).contains("has moved past 1.9.0-rc01")
    }

    @Test
    fun exemptionIsStillValidAtExactlyItsVersion() {
        val atBoundary = fragment.copy(version = Version.parse("1.9.0-rc01"))
        assertThat(findVerificationErrors(atBoundary, listOf(tracing), listOf(exemption())))
            .isEmpty()
    }

    @Test
    fun aNewMajorLineExpiresTheExemption() {
        val nextMajor = fragment.copy(version = Version.parse("2.0.0-alpha01"))
        val errors = findVerificationErrors(nextMajor, listOf(tracing), listOf(exemption()))
        assertThat(errors.single()).contains("has moved past 1.9.0-rc01")
    }

    @Test
    fun skippingThePhaseTheExemptionNamesStillExpiresIt() {
        val skippedBeta = fragment.copy(version = Version.parse("1.9.0-rc01"))
        val errors =
            findVerificationErrors(
                skippedBeta,
                listOf(tracing),
                listOf(exemption(validThrough = "1.9.0-beta01")),
            )
        assertThat(errors.single()).contains("has moved past 1.9.0-beta01")
    }

    @Test
    fun anEntryWrittenAtTheCurrentVersionIsBornExpired() {
        val sameVersion = exemption(validThrough = "1.9.0-alpha01")
        val errors = findVerificationErrors(fragment, listOf(tracing), listOf(sameVersion))
        assertThat(errors.single()).contains("has moved past 1.9.0-alpha01")
    }

    @Test
    fun reasonWithoutABugIsRejected() {
        val noBug = exemption(reason = "we need tip of tree")
        val errors = findVerificationErrors(fragment, listOf(tracing), listOf(noBug))
        assertThat(errors.single()).contains("must give a reason referencing a bug")
    }

    @Test
    fun githubIssueCountsAsABug() {
        val gh = exemption(reason = "https://github.com/androidx/androidx/issues/42 - new API")
        assertThat(findVerificationErrors(fragment, listOf(tracing), listOf(gh))).isEmpty()
    }

    @Test
    fun unparseableVersionIsRejected() {
        val bad = exemption(validThrough = "soon")
        val errors = findVerificationErrors(fragment, listOf(tracing), listOf(bad))
        assertThat(errors.single()).contains("is not a valid version")
    }

    @Test
    fun messageNamesTheConsumersOwnVersionForTheExemptionField() {
        val message = findVerificationErrors(fragment, listOf(tracing), listOf()).single()
        assertThat(message)
            .contains("androidx.fragment:fragment's own version, not androidx.tracing:tracing's")
        assertThat(message).contains("1.9.0-beta01 today")
        assertThat(message).doesNotContain("validThroughLibraryVersion = \"1.9.0-beta01\"")
    }

    @Test
    fun shouldBePinnedMessagePointsToPrebuiltDirectory() {
        val message = findVerificationErrors(fragment, listOf(tracing), listOf()).single()
        assertThat(message).contains("prebuilts/androidx/internal/androidx/tracing/tracing")
        assertThat(message).contains("<latest-version>")
    }

    @Test
    fun suggestedExemptionVersionAdvancesExactlyOnePhase() {
        assertThat(suggestedExemptionVersion(Version.parse("1.9.0-alpha03")))
            .isEqualTo("1.9.0-beta01")
        assertThat(suggestedExemptionVersion(Version.parse("1.9.0-beta01"))).isEqualTo("1.9.0-rc01")
        assertThat(suggestedExemptionVersion(Version.parse("1.9.0-rc01"))).isEqualTo("1.9.0")
        assertThat(suggestedExemptionVersion(Version.parse("1.9.0"))).isEqualTo("1.10.0-alpha01")
        assertThat(suggestedExemptionVersion(Version.parse("1.2.1-alpha01")))
            .isEqualTo("1.2.1-beta01")
        assertThat(suggestedExemptionVersion(Version.parse("1.2.1-beta01"))).isEqualTo("1.2.1-rc01")
        assertThat(suggestedExemptionVersion(Version.parse("1.2.1-rc01"))).isEqualTo("1.2.1")
        assertThat(suggestedExemptionVersion(Version.parse("1.2.1"))).isEqualTo("1.3.0-alpha01")
    }

    @Test
    fun duplicateDependenciesAreReportedAtMostOnce() {
        val duplicateTracing = tracing.copy(version = Version.parse("1.0.0"))
        val errors = findVerificationErrors(fragment, listOf(tracing, duplicateTracing), listOf())
        assertThat(errors).hasSize(1)
    }

    private fun pinnedTracing(version: String) =
        tracing.copy(projectPath = null, version = Version.parse(version))

    @Test
    fun alphaPinIsReported() {
        val errors =
            findVerificationErrors(fragment, listOf(pinnedTracing("2.0.0-alpha03")), listOf())
        assertThat(errors.single())
            .contains(
                "androidx.fragment:fragment depends on androidx.tracing:tracing:2.0.0-alpha03, " +
                    "an alpha release"
            )
    }

    @Test
    fun devPinIsReported() {
        val errors =
            findVerificationErrors(fragment, listOf(pinnedTracing("2.0.0-dev01")), listOf())
        assertThat(errors).hasSize(1)
    }

    @Test
    fun betaRcAndStablePinsAreNotReported() {
        for (version in listOf("2.0.0-beta01", "2.0.0-rc01", "2.0.0")) {
            assertThat(findVerificationErrors(fragment, listOf(pinnedTracing(version)), listOf()))
                .isEmpty()
        }
    }

    @Test
    fun alphaPinInTheSameVersionGroupIsNotReported() {
        val sibling =
            pinnedTracing("1.9.0-alpha01")
                .copy(
                    groupId = "androidx.fragment",
                    artifactId = "fragment-ktx",
                    versionGroup = "FRAGMENT",
                )
        assertThat(findVerificationErrors(fragment, listOf(sibling), listOf())).isEmpty()
    }

    @Test
    fun exemptionCoversAnAlphaPin() {
        val alphaPin = pinnedTracing("2.0.0-alpha03")
        assertThat(findVerificationErrors(fragment, listOf(alphaPin), listOf(exemption())))
            .isEmpty()
    }

    @Test
    fun exemptionForAnAlphaPinExpires() {
        val past = fragment.copy(version = Version.parse("1.9.0-rc02"))
        val alphaPin = pinnedTracing("2.0.0-alpha03")
        val errors = findVerificationErrors(past, listOf(alphaPin), listOf(exemption()))
        assertThat(errors.single()).contains("has moved past 1.9.0-rc01")
        assertThat(errors.single()).contains("to a beta, rc, or stable version")
    }

    @Test
    fun tipOfTreeAndAlphaPinOnTheSameLibraryAreReportedOnce() {
        val errors =
            findVerificationErrors(
                fragment,
                listOf(tracing, pinnedTracing("2.0.0-alpha03")),
                listOf(),
            )
        assertThat(errors).hasSize(1)
    }

    @Test
    fun alphaPinMessageSuggestsANonAlphaVersionOrAnExemption() {
        val message =
            findVerificationErrors(fragment, listOf(pinnedTracing("2.0.0-alpha03")), listOf())
                .single()
        assertThat(message)
            .contains(
                "Pick the latest beta, rc, or stable version in " +
                    "prebuilts/androidx/internal/androidx/tracing/tracing"
            )
        assertThat(message).contains("\n  [[exemption]]\n")
        assertThat(message).contains("  dependsOn = \"androidx.tracing:tracing\"")
        assertThat(message).contains("<which alpha tracing API you need>")
        assertThat(message)
            .contains(
                "once it reaches beta, the stability rule rejects alpha dependencies regardless " +
                    "of exemptions"
            )
    }

    @Test
    fun shouldBePinnedMessageRulesOutAlphaVersions() {
        val message = findVerificationErrors(fragment, listOf(tracing), listOf()).single()
        assertThat(message).contains("alpha versions are not allowed")
        assertThat(message).contains("\n  [[exemption]]\n")
        assertThat(message).contains("<which unreleased tracing API you need>")
    }

    @Test
    fun findDependenciesNeedingExemption_includesAlphaPinsButNotOtherPins() {
        val alphaPin = pinnedTracing("2.0.0-alpha03")
        val betaPin =
            pinnedTracing("1.3.0-beta01")
                .copy(
                    groupId = "androidx.collection",
                    artifactId = "collection",
                    versionGroup = "COLLECTION",
                )

        val needingExemption = findDependenciesNeedingExemption(fragment, listOf(alphaPin, betaPin))

        assertThat(needingExemption.map { it.coordinate })
            .containsExactly("androidx.tracing:tracing")
    }

    @Test
    fun findDependenciesNeedingExemption_keepsOnlySeparatelyReleasedDependencies() {
        val self = fragment.copy(versionGroup = null)
        val sibling =
            fragment.copy(projectPath = ":fragment:fragment-ktx", artifactId = "fragment-ktx")
        val duplicateTracing = tracing.copy(version = Version.parse("1.0.0"))

        val needingExemption =
            findDependenciesNeedingExemption(
                fragment,
                listOf(self, sibling, tracing, duplicateTracing),
            )

        assertThat(needingExemption.map { it.coordinate })
            .containsExactly("androidx.tracing:tracing")
    }

    @Test
    fun parseDependencyExemptions_emptyOrNullReturnsEmpty() {
        assertThat(parseDependencyExemptions(null)).isEmpty()
        assertThat(parseDependencyExemptions("")).isEmpty()
        assertThat(parseDependencyExemptions("   ")).isEmpty()
    }

    @Test
    fun parseDependencyExemptions_missingFieldThrows() {
        val toml =
            """
            [[exemption]]
            library = "androidx.fragment:fragment"
            dependsOn = "androidx.tracing:tracing"
            """
                .trimIndent()
        assertThrows<GradleException> { parseDependencyExemptions(toml) }
            .hasMessageThat()
            .contains("is missing required key \"validThroughLibraryVersion\"")
    }

    @Test
    fun parseDependencyExemptions_unrecognizedFieldThrows() {
        val toml =
            """
            [[exemption]]
            library = "androidx.fragment:fragment"
            dependsOn = "androidx.tracing:tracing"
            validThroughLibraryVersion = "1.0.0"
            reason = "b/12345"
            typoKey = "invalid"
            """
                .trimIndent()
        assertThrows<GradleException> { parseDependencyExemptions(toml) }
            .hasMessageThat()
            .contains("contains unrecognized key(s)")
    }

    @Test
    fun parseDependencyExemptions_unexpectedHeaderThrows() {
        val toml =
            """
            [[exemptions]]
            library = "androidx.fragment:fragment"
            dependsOn = "androidx.tracing:tracing"
            validThroughLibraryVersion = "1.0.0"
            reason = "b/12345"
            """
                .trimIndent()
        assertThrows<GradleException> { parseDependencyExemptions(toml) }
            .hasMessageThat()
            .contains("contains unexpected table/key(s)")
    }

    @Test
    fun parseDependencyExemptions_validEntries() {
        val toml =
            """
            [[exemption]]
            library = "androidx.fragment:fragment"
            dependsOn = "androidx.tracing:tracing"
            validThroughLibraryVersion = "1.9.0-rc01"
            reason = "b/12345"

            [[exemption]]
            library = "androidx.activity:activity"
            dependsOn = "androidx.core:core"
            validThroughLibraryVersion = "1.10.0-alpha01"
            reason = "b/67890"
            """
                .trimIndent()
        val exemptions = parseDependencyExemptions(toml)
        assertThat(exemptions)
            .containsExactly(
                DependencyExemption(
                    library = "androidx.fragment:fragment",
                    dependsOn = "androidx.tracing:tracing",
                    validThroughLibraryVersion = "1.9.0-rc01",
                    reason = "b/12345",
                ),
                DependencyExemption(
                    library = "androidx.activity:activity",
                    dependsOn = "androidx.core:core",
                    validThroughLibraryVersion = "1.10.0-alpha01",
                    reason = "b/67890",
                ),
            )
            .inOrder()
    }

    @Test
    fun parseDependencyExemptions_invalidSyntaxThrows() {
        val toml =
            """
            [[exemption
            library = "androidx.fragment:fragment"
            """
                .trimIndent()
        assertThrows<GradleException> { parseDependencyExemptions(toml) }
            .hasMessageThat()
            .contains("tip-of-tree-or-alpha-dependency-exemptions.toml has issues.")
    }

    @Test
    fun parseDependencyExemptions_notAnArrayThrows() {
        val toml =
            """
            exemption = "not an array"
            """
                .trimIndent()
        assertThrows<GradleException> { parseDependencyExemptions(toml) }
            .hasMessageThat()
            .contains("exemption is not an array")
    }

    @Test
    fun parseDependencyExemptions_entryNotATableThrows() {
        val toml =
            """
            exemption = ["not a table"]
            """
                .trimIndent()
        assertThrows<GradleException> { parseDependencyExemptions(toml) }
            .hasMessageThat()
            .contains("exemption[0] is not a table")
    }
}
