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

package androidx.glance.adaptive.core.templates

import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class TrackTemplateScopeTest {

    @Test
    fun trackTemplate_fullDsl_populatesAllSlotsAndComponents() {
        val stepsIcon = TemplateIcon.fromResource(101)
        val arrowUpIcon = TemplateIcon.fromResource(102)
        val weeklySteps =
            listOf(
                ChartBar(0.65f, "M"),
                ChartBar(1.0f, "T", isEmphasized = true),
                ChartBar(0.80f, "W"),
                ChartBar(1.0f, "T"),
                ChartBar(0.45f, "F"),
                ChartBar(1.0f, "S"),
                ChartBar(0.55f, "S"),
            )

        val template = trackTemplate {
            leading { progressRing(progress = 0.55f, icon = stepsIcon) }

            primary { text("11,056") { sensitive("--") } }

            secondary { text("Steps") }

            hero {
                barChart(bars = weeklySteps) {
                    startLabel("7 Day Avg")
                    endLabel("13,843", icon = arrowUpIcon)
                }
            }

            supporting {
                text("55%")
                text("3,118 to go") { sensitive("--") }
                badge("Z2")
            }
        }

        val leading = template.leading as Ring
        assertThat(leading.progress).isEqualTo(0.55f)
        assertThat(leading.icon).isEqualTo(101)

        val primary = template.primary as TemplateText
        assertThat(primary.value).isEqualTo("11,056")
        assertThat(primary.sensitiveReplacement).isEqualTo("--")

        val secondary = template.secondary as TemplateText
        assertThat(secondary.value).isEqualTo("Steps")
        assertThat(secondary.sensitiveReplacement).isNull()

        val hero = template.hero as Graph
        assertThat(hero.values.size).isEqualTo(7)
        assertThat(hero.fractionAt(0)).isWithin(1e-6f).of(0.65f)
        assertThat(hero.fractionAt(1)).isWithin(1e-6f).of(1.0f)
        assertThat(hero.isEmphasizedAt(0)).isFalse()
        assertThat(hero.isEmphasizedAt(1)).isTrue()
        // A full bar is emphasized only when the caller asks for it.
        assertThat(hero.isEmphasizedAt(3)).isFalse()
        assertThat(hero.labelAt(0)).isEqualTo("M")
        assertThat(hero.startLabel?.text).isEqualTo("7 Day Avg")
        assertThat(hero.startLabel?.icon).isNull()
        assertThat(hero.endLabel?.text).isEqualTo("13,843")
        assertThat(hero.endLabel?.icon).isEqualTo(102)

        assertThat(template.supporting).hasSize(3)
        val firstSupporting = template.supporting[0] as TemplateText
        assertThat(firstSupporting.value).isEqualTo("55%")
        assertThat(firstSupporting.sensitiveReplacement).isNull()
        val secondSupporting = template.supporting[1] as TemplateText
        assertThat(secondSupporting.value).isEqualTo("3,118 to go")
        assertThat(secondSupporting.sensitiveReplacement).isEqualTo("--")
        val badge = template.supporting[2] as Badge
        assertThat(badge.label).isEqualTo("Z2")
        assertThat(badge.outlineColor).isNull()
    }

    @Test
    fun trackTemplate_minimalWithLeadingRingOnly_succeeds() {
        val template = trackTemplate {
            leading { progressRing(progress = 0.25f) }
            primary { text("2,500") }
        }

        val leading = template.leading as Ring
        assertThat(leading.progress).isEqualTo(0.25f)
        assertThat(leading.icon).isNull()
        val primary = template.primary as TemplateText
        assertThat(primary.value).isEqualTo("2,500")
        assertThat(primary.sensitiveReplacement).isNull()
        assertThat(template.secondary).isNull()
        assertThat(template.hero).isNull()
        assertThat(template.supporting).isEmpty()
    }

    @Test
    fun trackTemplate_minimalWithHeroBarChartOnly_succeeds() {
        val template = trackTemplate {
            primary { text("72 bpm") }
            hero { barChart(bars = listOf(ChartBar(0.3f), ChartBar(0.6f, isEmphasized = true))) }
        }

        assertThat(template.leading).isNull()
        assertThat((template.primary as TemplateText).value).isEqualTo("72 bpm")
        val hero = template.hero as Graph
        // Values below 1.0f stay normalized against 1.0f rather than stretching to full height.
        assertThat(hero.fractionAt(0)).isWithin(1e-6f).of(0.3f)
        assertThat(hero.fractionAt(1)).isWithin(1e-6f).of(0.6f)
        assertThat(hero.isEmphasizedAt(0)).isFalse()
        assertThat(hero.isEmphasizedAt(1)).isTrue()
        assertThat(hero.labels).isEmpty()
    }

    @Test
    fun barChart_mixedBarLabels_preservesColumnAlignmentWithEmptyStrings() {
        val template = trackTemplate {
            primary { text("42m") }
            hero {
                barChart(
                    bars = listOf(ChartBar(0.4f, "Mon"), ChartBar(0.8f), ChartBar(1.0f, "Wed"))
                )
            }
        }

        val hero = template.hero as Graph
        assertThat(hero.labels).containsExactly("Mon", "", "Wed").inOrder()
    }

    @Test
    fun trackTemplate_missingPrimaryAndTrackingVisual_reportsAllUnmetRequirements() {
        val error = assertFailsWith<IllegalArgumentException> { trackTemplate {} }

        assertThat(error.message).contains("TrackTemplate needs")
        assertThat(error.message).contains("a primary text")
        assertThat(error.message).contains("a tracking visual")
    }

    @Test
    fun trackTemplate_missingTrackingVisual_throwsIllegalArgumentException() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                trackTemplate {
                    primary { text("11,056") }
                    secondary { text("Steps") }
                }
            }

        assertThat(error.message).contains("a tracking visual")
        assertThat(error.message).doesNotContain("a primary text")
    }

    @Test
    fun trackTemplate_moreThanThreeSupportingItems_throwsIllegalArgumentException() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                trackTemplate {
                    leading { progressRing(0.5f) }
                    primary { text("11,056") }
                    supporting {
                        text("1")
                        text("2")
                        text("3")
                        badge("4")
                    }
                }
            }

        assertThat(error.message).contains("at most 3 supporting items")
    }

    @Test
    fun leading_whenEmpty_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading {}
                primary { text("100") }
            }
        }
    }

    @Test
    fun primary_whenEmpty_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading { progressRing(0.5f) }
                primary {}
            }
        }
    }

    @Test
    fun secondary_whenEmpty_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading { progressRing(0.5f) }
                primary { text("100") }
                secondary {}
            }
        }
    }

    @Test
    fun hero_whenEmpty_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                primary { text("100") }
                hero {}
            }
        }
    }

    @Test
    fun supporting_whenEmpty_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading { progressRing(0.5f) }
                primary { text("100") }
                supporting {}
            }
        }
    }

    @Test
    fun leading_whenConfiguredTwice_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading { progressRing(0.4f) }
                leading { progressRing(0.8f) }
                primary { text("100") }
            }
        }
    }

    @Test
    fun leading_whenSecondComponentAdded_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading {
                    progressRing(0.4f)
                    progressRing(0.8f)
                }
                primary { text("100") }
            }
        }
    }

    @Test
    fun primary_whenSecondComponentAdded_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading { progressRing(0.4f) }
                primary {
                    text("100")
                    text("200")
                }
            }
        }
    }

    @Test
    fun sensitive_whenConfiguredTwice_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                leading { progressRing(0.4f) }
                primary {
                    text("100") {
                        sensitive("--")
                        sensitive("***")
                    }
                }
            }
        }
    }

    @Test
    fun startLabel_whenConfiguredTwice_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                primary { text("100") }
                hero {
                    barChart(listOf(ChartBar(0.2f), ChartBar(0.8f))) {
                        startLabel("A")
                        startLabel("B")
                    }
                }
            }
        }
    }

    @Test
    fun endLabel_whenConfiguredTwice_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            trackTemplate {
                primary { text("100") }
                hero {
                    barChart(listOf(ChartBar(0.2f), ChartBar(0.8f))) {
                        endLabel("A")
                        endLabel("B")
                    }
                }
            }
        }
    }

    @Test
    fun progressRing_outOfRange_failsBeforeRestOfBlock() {
        var reachedAfterInvalidCall = false
        assertFailsWith<IllegalArgumentException> {
            trackTemplate {
                leading {
                    progressRing(1.5f)
                    reachedAfterInvalidCall = true
                }
                primary { text("100") }
            }
        }
        assertThat(reachedAfterInvalidCall).isFalse()
    }

    @Test
    fun text_blankValue_failsBeforeOptionsBlock() {
        var reachedOptionsBlock = false
        assertFailsWith<IllegalArgumentException> {
            trackTemplate {
                leading { progressRing(0.5f) }
                primary {
                    text(" ") {
                        reachedOptionsBlock = true
                        sensitive("--")
                    }
                }
            }
        }
        assertThat(reachedOptionsBlock).isFalse()
    }

    @Test
    fun sensitive_blankValue_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            trackTemplate {
                leading { progressRing(0.5f) }
                primary { text("100") { sensitive(" ") } }
            }
        }
    }

    @Test
    fun barChart_tooFewBars_failsBeforeOptionsBlock() {
        var reachedOptionsBlock = false
        assertFailsWith<IllegalArgumentException> {
            trackTemplate {
                primary { text("100") }
                hero {
                    barChart(listOf(ChartBar(0.5f))) {
                        reachedOptionsBlock = true
                        startLabel("Avg")
                    }
                }
            }
        }
        assertThat(reachedOptionsBlock).isFalse()
    }

    @Test
    fun badge_blankLabel_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            trackTemplate {
                leading { progressRing(0.5f) }
                primary { text("100") }
                supporting { badge(" ") }
            }
        }
    }
}
