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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdateTipOfTreeExemptionsTaskTest {

    @Test
    fun renderAndParseExemptionsTomlRoundTrips() {
        val exemptions =
            listOf(
                TipOfTreeExemption(
                    library = ":a2ui:a2ui-engine",
                    dependsOn = ":collection:collection",
                    validThroughLibraryVersion = "1.0.0-beta01",
                    reason = "b/12345 - needs new API",
                ),
                TipOfTreeExemption(
                    library = ":fragment:fragment",
                    dependsOn = ":tracing:tracing",
                    validThroughLibraryVersion = "1.9.0-rc01",
                    reason = "b/67890 - needs trace counter",
                ),
            )
        val toml = renderExemptionsToml(exemptions)
        val parsed = parseTipOfTreeExemptions(toml)
        assertThat(parsed).isEqualTo(exemptions)
    }

    @Test
    fun renderAndParseExemptionsTomlEscapesSpecialCharacters() {
        val exemptions =
            listOf(
                TipOfTreeExemption(
                    library = ":sample:sample",
                    dependsOn = ":core:core",
                    validThroughLibraryVersion = "1.0.0",
                    reason = "b/12345 - reasons with \"quotes\" and \\backslashes\\ and \ttabs",
                )
            )
        val toml = renderExemptionsToml(exemptions)
        assertThat(toml)
            .contains(
                "reason = \"b/12345 - reasons with \\\"quotes\\\" and \\\\backslashes\\\\ and \\ttabs\""
            )
        val parsed = parseTipOfTreeExemptions(toml)
        assertThat(parsed).isEqualTo(exemptions)
    }

    @Test
    fun reconcileExemptionsFullRepoPrunesStaleEntriesAndPreservesReasons() {
        val existing =
            listOf(
                TipOfTreeExemption(
                    library = ":activity:activity",
                    dependsOn = ":core:core",
                    validThroughLibraryVersion = "1.10.0-alpha01",
                    reason = "b/11111 - obsolete entry that was pinned",
                ),
                TipOfTreeExemption(
                    library = ":fragment:fragment",
                    dependsOn = ":tracing:tracing",
                    validThroughLibraryVersion = "1.9.0-rc01",
                    reason = "b/22222 - custom reason to preserve",
                ),
            )
        val reports =
            listOf(
                // :activity:activity was evaluated and no longer has unpinned dependencies
                UnpinnedDependenciesReport(
                    library = ":activity:activity",
                    libraryVersion = "1.10.0-alpha01",
                    unpinnedDependencies = emptyList(),
                ),
                // :fragment:fragment was evaluated and still has its exempted dependency
                UnpinnedDependenciesReport(
                    library = ":fragment:fragment",
                    libraryVersion = "1.9.0-alpha01",
                    unpinnedDependencies = listOf(":tracing:tracing"),
                ),
                // :wear:wear was evaluated and has a new unpinned dependency
                UnpinnedDependenciesReport(
                    library = ":wear:wear",
                    libraryVersion = "1.0.0-alpha01",
                    unpinnedDependencies = listOf(":tracing:tracing"),
                ),
            )

        val updated = reconcileExemptions(existing, reports)

        // :activity:activity was pruned because it was evaluated with no unpinned dependencies
        assertThat(updated.map { it.library }).containsExactly(":fragment:fragment", ":wear:wear")
        // :fragment:fragment preserved its custom reason and version
        val fragmentExemption = updated.first { it.library == ":fragment:fragment" }
        assertThat(fragmentExemption.reason).isEqualTo("b/22222 - custom reason to preserve")
        assertThat(fragmentExemption.validThroughLibraryVersion).isEqualTo("1.9.0-rc01")
        // :wear:wear added as a new placeholder entry, expiring one phase after its version
        val wearExemption = updated.first { it.library == ":wear:wear" }
        assertThat(wearExemption)
            .isEqualTo(
                TipOfTreeExemption(
                    library = ":wear:wear",
                    dependsOn = ":tracing:tracing",
                    validThroughLibraryVersion = "1.0.0-beta01",
                    reason = "b/TODO - <reason>",
                )
            )
    }

    @Test
    fun reconcileExemptionsScopedPreservesUnevaluatedLibraries() {
        val existing =
            listOf(
                TipOfTreeExemption(
                    library = ":camera:camera-core",
                    dependsOn = ":core:core",
                    validThroughLibraryVersion = "1.5.0-alpha01",
                    reason = "b/33333 - camera reason",
                ),
                TipOfTreeExemption(
                    library = ":wear:wear-compose",
                    dependsOn = ":compose:ui:ui",
                    validThroughLibraryVersion = "1.4.0-alpha01",
                    reason = "b/44444 - wear compose reason",
                ),
            )
        // Scoped execution: ONLY :wear:wear-compose was evaluated
        val scopedReports =
            listOf(
                UnpinnedDependenciesReport(
                    library = ":wear:wear-compose",
                    libraryVersion = "1.4.0-alpha01",
                    unpinnedDependencies = listOf(":compose:material:material", ":compose:ui:ui"),
                )
            )

        val updated = reconcileExemptions(existing, scopedReports)

        // :camera:camera-core was outside the evaluation scope and is preserved untouched!
        assertThat(updated.map { it.library })
            .containsExactly(":camera:camera-core", ":wear:wear-compose", ":wear:wear-compose")
        val cameraExemption = updated.first { it.library == ":camera:camera-core" }
        assertThat(cameraExemption.reason).isEqualTo("b/33333 - camera reason")

        // :wear:wear-compose preserved its existing reason for :compose:ui:ui and added new entry
        val wearUi = updated.first {
            it.library == ":wear:wear-compose" && it.dependsOn == ":compose:ui:ui"
        }
        assertThat(wearUi.reason).isEqualTo("b/44444 - wear compose reason")
        val wearMaterial = updated.first {
            it.library == ":wear:wear-compose" && it.dependsOn == ":compose:material:material"
        }
        assertThat(wearMaterial.reason).isEqualTo("b/TODO - <reason>")
    }

    @Test
    fun reconcileExemptionsSeedsFallbackVersionWhenLibraryVersionIsUnknown() {
        val reports =
            listOf(
                UnpinnedDependenciesReport(
                    library = ":wear:wear",
                    libraryVersion = null,
                    unpinnedDependencies = listOf(":tracing:tracing"),
                )
            )
        val updated = reconcileExemptions(emptyList(), reports)
        assertThat(updated.single().validThroughLibraryVersion).isEqualTo("1.0.0-alpha01")
    }
}
