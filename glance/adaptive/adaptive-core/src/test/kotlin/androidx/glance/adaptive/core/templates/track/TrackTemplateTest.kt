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

package androidx.glance.adaptive.core.templates.track

import androidx.collection.floatListOf
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class TrackTemplateTest {

    @Test
    fun slotConstructor_populatesSemanticSlots() {
        val template = sampleSlotTemplate("11,056")

        assertThat(template).isInstanceOf(GlanceAdaptiveTemplate::class.java)
        assertThat(template.templateId).isEqualTo(TrackTemplate.TEMPLATE_ID)
        assertThat(TrackTemplate.TEMPLATE_ID).isEqualTo(1)

        val leading = template.leading as Ring
        assertThat(leading.progress).isEqualTo(0.55f)
        assertThat(leading.icon).isEqualTo(1)

        val primary = template.primary as TemplateText
        assertThat(primary.value).isEqualTo("11,056")
        assertThat(primary.sensitiveReplacement).isEqualTo("--")

        assertThat((template.secondary as TemplateText).value).isEqualTo("Steps")
        val hero = template.hero as Graph
        assertThat(hero.startLabel?.text).isEqualTo("7 Day Avg")
        assertThat(hero.endLabel?.text).isEqualTo("13,843")
        assertThat(template.supporting).hasSize(2)
        assertThat((template.supporting[0] as TemplateText).value).isEqualTo("55%")
        assertThat((template.supporting[1] as Badge).label).isEqualTo("Z2")
    }

    @Test
    fun slotConstructor_acceptsLeadingRingOrHeroGraphAsTrackingVisual() {
        val withRing =
            TrackTemplate(leading = Ring(progress = 0.25f), primary = TemplateText("2,500"))
        assertThat((withRing.leading as Ring).progress).isEqualTo(0.25f)
        assertThat((withRing.primary as TemplateText).value).isEqualTo("2,500")
        assertThat(withRing.hero).isNull()

        val withHero =
            TrackTemplate(
                primary = TemplateText("72 bpm"),
                hero = Graph(values = floatListOf(0.3f, 0.6f), goal = 1.0f),
            )
        assertThat(withHero.leading).isNull()
        assertThat(withHero.hero).isNotNull()
    }

    @Test
    fun rule_reportsMissingPrimaryTrackingVisualAndSupportingOverflow() {
        val missingAll = assertFailsWith<IllegalArgumentException> { TrackTemplate() }
        assertThat(missingAll.message).contains("a primary text")
        assertThat(missingAll.message).contains("a tracking visual")

        val tooManySupporting =
            assertFailsWith<IllegalArgumentException> {
                TrackTemplate(
                    leading = Ring(0.5f),
                    primary = TemplateText("11,056"),
                    supporting =
                        listOf(
                            TemplateText("1"),
                            TemplateText("2"),
                            TemplateText("3"),
                            Badge("4", outlineColor = 0),
                        ),
                )
            }
        assertThat(tooManySupporting.message).contains("at most 3 supporting items")
    }

    @Test
    fun equalsHashCodeAndToString_compareAllSlots() {
        val base = sampleSlotTemplate("11,056")
        val same = sampleSlotTemplate("11,056")
        val diffPrimary = sampleSlotTemplate("12,000")
        val diffLeading =
            TrackTemplate(
                leading = Ring(progress = 0.75f, icon = 1),
                primary = base.primary,
                secondary = base.secondary,
                hero = base.hero,
                supporting = base.supporting,
            )
        val diffHero =
            TrackTemplate(
                leading = base.leading,
                primary = base.primary,
                secondary = base.secondary,
                hero =
                    Graph(
                        values = floatListOf(0.2f, 0.9f),
                        labels = listOf("M", "T"),
                        goal = 1.0f,
                        startLabel = ChartLabel("7 Day Avg"),
                        endLabel = ChartLabel("13,843"),
                    ),
                supporting = base.supporting,
            )
        val diffSupporting =
            TrackTemplate(
                leading = base.leading,
                primary = base.primary,
                secondary = base.secondary,
                hero = base.hero,
                supporting = listOf(TemplateText("55%"), Badge("Z3", outlineColor = 0)),
            )

        assertThat(base).isEqualTo(same)
        assertThat(base.hashCode()).isEqualTo(same.hashCode())
        assertThat(base).isNotEqualTo(diffPrimary)
        assertThat(base).isNotEqualTo(diffLeading)
        assertThat(base).isNotEqualTo(diffHero)
        assertThat(base).isNotEqualTo(diffSupporting)
        assertThat(base.toString()).contains("TrackTemplate(")
        assertThat(base.toString()).contains("11,056")
    }

    private fun sampleSlotTemplate(primaryValue: String): TrackTemplate =
        TrackTemplate(
            leading = Ring(progress = 0.55f, icon = 1),
            primary = TemplateText(value = primaryValue, sensitiveReplacement = "--"),
            secondary = TemplateText("Steps"),
            hero =
                Graph(
                    values = floatListOf(0.5f, 1.0f),
                    labels = listOf("M", "T"),
                    goal = 1.0f,
                    startLabel = ChartLabel("7 Day Avg"),
                    endLabel = ChartLabel("13,843"),
                ),
            supporting = listOf(TemplateText("55%"), Badge("Z2", outlineColor = 0)),
        )
}
