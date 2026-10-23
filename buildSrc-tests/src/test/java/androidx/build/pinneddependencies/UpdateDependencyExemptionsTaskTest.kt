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

class UpdateDependencyExemptionsTaskTest {

    @Test
    fun renderAndParseExemptionsTomlRoundTrips() {
        val exemptions =
            listOf(
                DependencyExemption(
                    library = "androidx.a2ui:a2ui-engine",
                    dependsOn = "androidx.collection:collection",
                    validThroughLibraryVersion = "1.0.0-beta01",
                    reason = "b/12345 - needs new API",
                ),
                DependencyExemption(
                    library = "androidx.fragment:fragment",
                    dependsOn = "androidx.tracing:tracing",
                    validThroughLibraryVersion = "1.9.0-rc01",
                    reason = "b/67890 - needs trace counter",
                ),
            )
        val toml = renderExemptionsToml(exemptions)
        val parsed = parseDependencyExemptions(toml)
        assertThat(parsed).isEqualTo(exemptions)
    }

    @Test
    fun renderAndParseExemptionsTomlEscapesSpecialCharacters() {
        val exemptions =
            listOf(
                DependencyExemption(
                    library = "androidx.sample:sample",
                    dependsOn = "androidx.core:core",
                    validThroughLibraryVersion = "1.0.0",
                    reason = "b/12345 - reasons with \"quotes\" and \\backslashes\\ and \ttabs",
                )
            )
        val toml = renderExemptionsToml(exemptions)
        assertThat(toml)
            .contains(
                "reason = \"b/12345 - reasons with \\\"quotes\\\" and \\\\backslashes\\\\ and \\ttabs\""
            )
        val parsed = parseDependencyExemptions(toml)
        assertThat(parsed).isEqualTo(exemptions)
    }

    @Test
    fun reconcileExemptionsFullRepoPrunesStaleEntriesAndPreservesReasons() {
        val existing =
            listOf(
                DependencyExemption(
                    library = "androidx.activity:activity",
                    dependsOn = "androidx.core:core",
                    validThroughLibraryVersion = "1.10.0-alpha01",
                    reason = "b/11111 - obsolete entry that was pinned",
                ),
                DependencyExemption(
                    library = "androidx.fragment:fragment",
                    dependsOn = "androidx.tracing:tracing",
                    validThroughLibraryVersion = "1.9.0-rc01",
                    reason = "b/22222 - custom reason to preserve",
                ),
            )
        val reports =
            listOf(
                // androidx.activity:activity was evaluated and no longer has dependencies needing
                // exemption
                DependencyExemptionsReport(
                    library = "androidx.activity:activity",
                    libraryVersion = "1.10.0-alpha01",
                    dependenciesNeedingExemption = emptyList(),
                ),
                // androidx.fragment:fragment was evaluated and still has its exempted dependency
                DependencyExemptionsReport(
                    library = "androidx.fragment:fragment",
                    libraryVersion = "1.9.0-alpha01",
                    dependenciesNeedingExemption = listOf("androidx.tracing:tracing"),
                ),
                // androidx.wear:wear was evaluated and has a new dependency needing exemption
                DependencyExemptionsReport(
                    library = "androidx.wear:wear",
                    libraryVersion = "1.0.0-alpha01",
                    dependenciesNeedingExemption = listOf("androidx.tracing:tracing"),
                ),
            )

        val updated = reconcileExemptions(existing, reports)

        // androidx.activity:activity was pruned because it was evaluated with no dependencies
        // needing
        // exemption
        assertThat(updated.map { it.library })
            .containsExactly("androidx.fragment:fragment", "androidx.wear:wear")
        // androidx.fragment:fragment preserved its custom reason and version
        val fragmentExemption = updated.first { it.library == "androidx.fragment:fragment" }
        assertThat(fragmentExemption.reason).isEqualTo("b/22222 - custom reason to preserve")
        assertThat(fragmentExemption.validThroughLibraryVersion).isEqualTo("1.9.0-rc01")
        // androidx.wear:wear added as a new placeholder entry, expiring one phase after its version
        val wearExemption = updated.first { it.library == "androidx.wear:wear" }
        assertThat(wearExemption)
            .isEqualTo(
                DependencyExemption(
                    library = "androidx.wear:wear",
                    dependsOn = "androidx.tracing:tracing",
                    validThroughLibraryVersion = "1.0.0-beta01",
                    reason = "b/TODO - <reason>",
                )
            )
    }

    @Test
    fun reconcileExemptionsScopedPreservesUnevaluatedLibraries() {
        val existing =
            listOf(
                DependencyExemption(
                    library = "androidx.camera:camera-core",
                    dependsOn = "androidx.core:core",
                    validThroughLibraryVersion = "1.5.0-alpha01",
                    reason = "b/33333 - camera reason",
                ),
                DependencyExemption(
                    library = "androidx.wear:wear-compose",
                    dependsOn = "androidx.compose.ui:ui",
                    validThroughLibraryVersion = "1.4.0-alpha01",
                    reason = "b/44444 - wear compose reason",
                ),
            )
        // Scoped execution: ONLY androidx.wear:wear-compose was evaluated
        val scopedReports =
            listOf(
                DependencyExemptionsReport(
                    library = "androidx.wear:wear-compose",
                    libraryVersion = "1.4.0-alpha01",
                    dependenciesNeedingExemption =
                        listOf("androidx.compose.material:material", "androidx.compose.ui:ui"),
                )
            )

        val updated = reconcileExemptions(existing, scopedReports)

        // androidx.camera:camera-core was outside the evaluation scope and is preserved untouched!
        assertThat(updated.map { it.library })
            .containsExactly(
                "androidx.camera:camera-core",
                "androidx.wear:wear-compose",
                "androidx.wear:wear-compose",
            )
        val cameraExemption = updated.first { it.library == "androidx.camera:camera-core" }
        assertThat(cameraExemption.reason).isEqualTo("b/33333 - camera reason")

        // androidx.wear:wear-compose preserved its existing reason for androidx.compose.ui:ui and
        // added new entry
        val wearUi = updated.first {
            it.library == "androidx.wear:wear-compose" && it.dependsOn == "androidx.compose.ui:ui"
        }
        assertThat(wearUi.reason).isEqualTo("b/44444 - wear compose reason")
        val wearMaterial = updated.first {
            it.library == "androidx.wear:wear-compose" &&
                it.dependsOn == "androidx.compose.material:material"
        }
        assertThat(wearMaterial.reason).isEqualTo("b/TODO - <reason>")
    }

    @Test
    fun reconcileExemptionsSeedsFallbackVersionWhenLibraryVersionIsUnknown() {
        val reports =
            listOf(
                DependencyExemptionsReport(
                    library = "androidx.wear:wear",
                    libraryVersion = null,
                    dependenciesNeedingExemption = listOf("androidx.tracing:tracing"),
                )
            )
        val updated = reconcileExemptions(emptyList(), reports)
        assertThat(updated.single().validThroughLibraryVersion).isEqualTo("1.0.0-alpha01")
    }
}
