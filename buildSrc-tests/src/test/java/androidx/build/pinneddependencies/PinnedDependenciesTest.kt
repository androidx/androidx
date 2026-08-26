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
        ProjectCoordinates(
            projectPath = ":fragment:fragment",
            groupId = "androidx.fragment",
            artifactId = "fragment",
            version = Version("1.9.0-beta01"),
            versionGroup = "FRAGMENT",
        )

    private val tracing =
        ProjectCoordinates(
            projectPath = ":tracing:tracing",
            groupId = "androidx.tracing",
            artifactId = "tracing",
            version = Version("2.0.0-beta01"),
            versionGroup = "TRACING",
        )

    private fun exemption(
        validThrough: String = "1.9.0-rc01",
        reason: String = "b/12345 - uses Trace.setCounter, which no released tracing has yet",
    ) = TipOfTreeExemption(":fragment:fragment", ":tracing:tracing", validThrough, reason)

    @Test
    fun dependencyInAnotherVersionGroupIsReported() {
        val errors = findVerificationErrors(fragment, listOf(tracing), listOf())
        assertThat(errors).hasSize(1)
        assertThat(errors.single())
            .contains(":fragment:fragment depends on tip-of-tree :tracing:tracing")
    }

    @Test
    fun sameVersionGroupIsNotReported() {
        val sibling =
            tracing.copy(projectPath = ":fragment:fragment-ktx", versionGroup = "FRAGMENT")
        assertThat(findVerificationErrors(fragment, listOf(sibling), listOf())).isEmpty()
    }

    @Test
    fun selfDependencyIsNotReported() {
        val self = tracing.copy(projectPath = ":fragment:fragment", versionGroup = null)
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
        val other = exemption().copy(dependsOn = ":collection:collection")
        assertThat(findVerificationErrors(fragment, listOf(tracing), listOf(other))).hasSize(1)
    }

    @Test
    fun exemptionForADifferentLibraryDoesNotApply() {
        val other = exemption().copy(library = ":activity:activity")
        assertThat(findVerificationErrors(fragment, listOf(tracing), listOf(other))).hasSize(1)
    }

    @Test
    fun exemptionExpiresOnceTheLibraryMovesPastIt() {
        val past = fragment.copy(version = Version("1.9.0-rc02"))
        val errors = findVerificationErrors(past, listOf(tracing), listOf(exemption()))
        assertThat(errors.single()).contains("has moved past 1.9.0-rc01")
    }

    @Test
    fun exemptionIsStillValidAtExactlyItsVersion() {
        val atBoundary = fragment.copy(version = Version("1.9.0-rc01"))
        assertThat(findVerificationErrors(atBoundary, listOf(tracing), listOf(exemption())))
            .isEmpty()
    }

    @Test
    fun aNewMajorLineExpiresTheExemption() {
        val nextMajor = fragment.copy(version = Version("2.0.0-alpha01"))
        val errors = findVerificationErrors(nextMajor, listOf(tracing), listOf(exemption()))
        assertThat(errors.single()).contains("has moved past 1.9.0-rc01")
    }

    @Test
    fun skippingThePhaseTheExemptionNamesStillExpiresIt() {
        val skippedBeta = fragment.copy(version = Version("1.9.0-rc01"))
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
        assertThat(message).contains(":fragment:fragment's own version, not :tracing:tracing's")
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
        assertThat(suggestedExemptionVersion(Version("1.9.0-alpha03"))).isEqualTo("1.9.0-beta01")
        assertThat(suggestedExemptionVersion(Version("1.9.0-beta01"))).isEqualTo("1.9.0-rc01")
        assertThat(suggestedExemptionVersion(Version("1.9.0-rc01"))).isEqualTo("1.9.0")
        assertThat(suggestedExemptionVersion(Version("1.9.0"))).isEqualTo("1.10.0-alpha01")
        assertThat(suggestedExemptionVersion(Version("1.2.1-alpha01"))).isEqualTo("1.2.1-beta01")
        assertThat(suggestedExemptionVersion(Version("1.2.1-beta01"))).isEqualTo("1.2.1-rc01")
        assertThat(suggestedExemptionVersion(Version("1.2.1-rc01"))).isEqualTo("1.2.1")
        assertThat(suggestedExemptionVersion(Version("1.2.1"))).isEqualTo("1.3.0-alpha01")
    }

    @Test
    fun duplicateDependenciesAreReportedAtMostOnce() {
        val duplicateTracing = tracing.copy(version = Version("1.0.0"))
        val errors = findVerificationErrors(fragment, listOf(tracing, duplicateTracing), listOf())
        assertThat(errors).hasSize(1)
    }

    @Test
    fun parseTipOfTreeExemptions_emptyOrNullReturnsEmpty() {
        assertThat(parseTipOfTreeExemptions(null)).isEmpty()
        assertThat(parseTipOfTreeExemptions("")).isEmpty()
        assertThat(parseTipOfTreeExemptions("   ")).isEmpty()
    }

    @Test
    fun parseTipOfTreeExemptions_missingFieldThrows() {
        val toml =
            """
            [[tipOfTreeExemptions]]
            library = ":fragment:fragment"
            dependsOn = ":tracing:tracing"
            """
                .trimIndent()
        assertThrows<GradleException> { parseTipOfTreeExemptions(toml) }
            .hasMessageThat()
            .contains("is missing required key \"validThroughLibraryVersion\"")
    }

    @Test
    fun parseTipOfTreeExemptions_unrecognizedFieldThrows() {
        val toml =
            """
            [[tipOfTreeExemptions]]
            library = ":fragment:fragment"
            dependsOn = ":tracing:tracing"
            validThroughLibraryVersion = "1.0.0"
            reason = "b/12345"
            typoKey = "invalid"
            """
                .trimIndent()
        assertThrows<GradleException> { parseTipOfTreeExemptions(toml) }
            .hasMessageThat()
            .contains("contains unrecognized key(s)")
    }

    @Test
    fun parseTipOfTreeExemptions_unexpectedHeaderThrows() {
        val toml =
            """
            [[tipOfTreeExemption]]
            library = ":fragment:fragment"
            dependsOn = ":tracing:tracing"
            validThroughLibraryVersion = "1.0.0"
            reason = "b/12345"
            """
                .trimIndent()
        assertThrows<GradleException> { parseTipOfTreeExemptions(toml) }
            .hasMessageThat()
            .contains("contains unexpected table/key(s)")
    }
}
