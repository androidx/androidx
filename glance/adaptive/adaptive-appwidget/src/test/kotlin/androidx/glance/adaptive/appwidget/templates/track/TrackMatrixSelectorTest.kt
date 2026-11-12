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

package androidx.glance.adaptive.appwidget.templates.track

import androidx.collection.floatListOf
import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.TrackTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TrackMatrixSelectorTest {

    private val template = fullTemplate()

    @Test
    fun narrowAndShort_usesTheBulbWithTemplateRingAndSensitivePrimary() {
        for (height in listOf(HeightTier.H0, HeightTier.H1)) {
            val bulb = select(WidthTier.W1, height) as MatrixBulbContent
            val ring = bulb.component as ContainedRing
            assertThat(ring.value).isEqualTo(PRIMARY)
            assertThat(ring.progress).isEqualTo(PROGRESS)
            assertThat(ring.icon).isEqualTo(ICON_RES)
            assertThat(ring.sensitiveReplacement).isEqualTo(SENSITIVE_PRIMARY)
        }
    }

    @Test
    fun narrowAndShort_withoutLeadingRing_stillRendersBulbWithEmptyRing() {
        val chartOnly =
            TrackTemplate(
                primary = TemplateText("72 bpm"),
                hero = Graph(values = floatListOf(0.4f, 0.8f), goal = 1.0f),
            )

        val bulb = select(WidthTier.W1, HeightTier.H1, chartOnly) as MatrixBulbContent
        val ring = bulb.component as ContainedRing
        assertThat(ring.value).isEqualTo("72 bpm")
        assertThat(ring.progress).isNull()
        assertThat(ring.icon).isNull()
    }

    @Test
    fun card_alwaysHasLeadingAndPrimaryFromTemplate() {
        for (width in WidthTier.entries) {
            for (height in listOf(HeightTier.H2, HeightTier.H3, HeightTier.H4)) {
                val card = card(width, height)
                assertThat((card.leading as Ring).progress).isEqualTo(PROGRESS)
                assertThat((card.primary as TemplateText).value).isEqualTo(PRIMARY)
            }
        }
    }

    @Test
    fun card_secondaryFollowsWidthAndNarrowHeight() {
        assertThat(card(WidthTier.W1, HeightTier.H2).secondary).isNull()
        assertThat(card(WidthTier.W1, HeightTier.H3).secondary).isNotNull()
        assertThat(card(WidthTier.W2, HeightTier.H4).secondary).isNull()
        assertThat(card(WidthTier.W3, HeightTier.H2).secondary).isNotNull()
        assertThat(card(WidthTier.W4, HeightTier.H2).secondary).isNotNull()
        assertThat(card(WidthTier.W4, HeightTier.H2, fullTemplate(secondaryText = null)).secondary)
            .isNull()
    }

    @Test
    fun card_actionCountFollowsWidthFromH2AndPreservesComponentTypes() {
        assertThat(card(WidthTier.W4, HeightTier.H1).actions).isEmpty()
        assertThat(card(WidthTier.W2, HeightTier.H2).actions)
            .containsExactly(template.supporting[0])
        assertThat(card(WidthTier.W3, HeightTier.H2).actions)
            .containsExactly(template.supporting[0], template.supporting[1])
            .inOrder()
        val w4Actions = card(WidthTier.W4, HeightTier.H2).actions
        assertThat(w4Actions)
            .containsExactly(template.supporting[0], template.supporting[1], template.supporting[2])
            .inOrder()
        assertThat((w4Actions[0] as TemplateText).value).isEqualTo(FIRST_SUPPORTING)
        assertThat((w4Actions[1] as TemplateText).value).isEqualTo(SECOND_SUPPORTING)
        assertThat((w4Actions[2] as Badge).label).isEqualTo(BADGE_LABEL)
        assertThat(card(WidthTier.W1, HeightTier.H3).actions).isEmpty()
        assertThat(card(WidthTier.W1, HeightTier.H4).actions).hasSize(1)
    }

    @Test
    fun card_heroGraphStripsDescriptionAtH3AndKeepsDescriptionAtH4() {
        assertThat(card(WidthTier.W4, HeightTier.H2).hero).isNull()
        assertThat(card(WidthTier.W1, HeightTier.H4).hero).isNull()

        val compactHero = card(WidthTier.W4, HeightTier.H3).hero as Graph
        assertThat(compactHero.hasDescription).isFalse()

        val fullHero = card(WidthTier.W4, HeightTier.H4).hero as Graph
        assertThat(fullHero.startLabel?.text).isEqualTo(START_LABEL)
        assertThat(fullHero.endLabel?.text).isEqualTo(END_LABEL)
        assertThat(fullHero.endLabel?.icon).isEqualTo(ICON_RES)
    }

    @Test
    fun card_withoutHeroOrLeading_leavesOmittedSlotNull() {
        val ringOnly = TrackTemplate(leading = Ring(0.4f), primary = TemplateText("4,000"))
        val chartOnly =
            TrackTemplate(
                primary = TemplateText("72 bpm"),
                hero = Graph(values = floatListOf(0.4f, 0.8f), goal = 1.0f),
            )

        assertThat(card(WidthTier.W4, HeightTier.H4, ringOnly).hero).isNull()
        assertThat(card(WidthTier.W4, HeightTier.H4, chartOnly).leading).isNull()
    }

    @Test
    fun hasDroppedSlots_whenOptionalSlotsAbsent_isAlwaysFalse() {
        val absent = TrackTemplate(leading = Ring(0.8f), primary = TemplateText("7,922"))

        for (width in WidthTier.entries) {
            for (height in HeightTier.entries) {
                assertThat(select(width, height, absent).hasDroppedSlots).isFalse()
            }
        }
    }

    @Test
    fun hasDroppedSlots_whenSecondaryPresent_isTrueWhereSecondaryIsHidden() {
        val template =
            TrackTemplate(
                leading = Ring(0.8f),
                primary = TemplateText("7,922"),
                secondary = TemplateText("steps"),
            )

        for (width in WidthTier.entries) {
            for (height in HeightTier.entries) {
                val expectedDropped =
                    when (width) {
                        WidthTier.W1 -> height < HeightTier.H3
                        WidthTier.W2 -> true
                        WidthTier.W3,
                        WidthTier.W4 -> false
                    }
                assertThat(select(width, height, template).hasDroppedSlots)
                    .isEqualTo(expectedDropped)
            }
        }
    }

    @Test
    fun hasDroppedSlots_whenSupportingPresent_isTrueWhereSupportingIsHidden() {
        val template =
            TrackTemplate(
                leading = Ring(0.8f),
                primary = TemplateText("7,922"),
                supporting = listOf(TemplateText("340 kcal")),
            )

        for (width in WidthTier.entries) {
            for (height in HeightTier.entries) {
                val expectedDropped =
                    when (width) {
                        WidthTier.W1 -> height < HeightTier.H4
                        WidthTier.W2,
                        WidthTier.W3,
                        WidthTier.W4 -> height < HeightTier.H2
                    }
                assertThat(select(width, height, template).hasDroppedSlots)
                    .isEqualTo(expectedDropped)
            }
        }
    }

    @Test
    fun hasDroppedSlots_whenHeroPresent_isTrueWhereHeroIsHidden() {
        val template =
            TrackTemplate(
                primary = TemplateText("7,922"),
                hero = Graph(values = floatListOf(0.5f, 1.0f), goal = 1.0f),
            )

        for (width in WidthTier.entries) {
            for (height in HeightTier.entries) {
                val expectedDropped = width == WidthTier.W1 || height < HeightTier.H3
                assertThat(select(width, height, template).hasDroppedSlots)
                    .isEqualTo(expectedDropped)
            }
        }
    }

    @Test
    fun hasDroppedSlots_whenBothOptionalSlotsPresent_isFalseOnlyWhereBothFit() {
        val template =
            TrackTemplate(
                leading = Ring(0.8f),
                primary = TemplateText("7,922"),
                secondary = TemplateText("steps"),
                supporting = listOf(TemplateText("Goal: 10,000")),
            )

        for (width in WidthTier.entries) {
            for (height in HeightTier.entries) {
                val bothFit =
                    (width == WidthTier.W1 && height == HeightTier.H4) ||
                        (width >= WidthTier.W3 && height >= HeightTier.H2)
                assertThat(select(width, height, template).hasDroppedSlots).isEqualTo(!bothFit)
            }
        }
    }

    private fun card(
        width: WidthTier,
        height: HeightTier,
        template: TrackTemplate = this.template,
    ): MatrixCardContent = select(width, height, template) as MatrixCardContent

    private fun select(
        width: WidthTier,
        height: HeightTier,
        template: TrackTemplate = this.template,
    ): MatrixContent =
        TrackMatrixSelector.select(
            template,
            AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(nominalDimensions(width, height)),
        )

    private fun fullTemplate(secondaryText: String? = SECONDARY): TrackTemplate =
        TrackTemplate(
            leading = Ring(progress = PROGRESS, icon = ICON_RES),
            primary = TemplateText(value = PRIMARY, sensitiveReplacement = SENSITIVE_PRIMARY),
            secondary = secondaryText?.let(::TemplateText),
            hero =
                Graph(
                    values = floatListOf(0.65f, 1.0f, 0.80f),
                    labels = listOf("M", "T", "W"),
                    goal = 1.0f,
                    startLabel = ChartLabel(START_LABEL),
                    endLabel = ChartLabel(END_LABEL, icon = ICON_RES),
                ),
            supporting =
                listOf(
                    TemplateText(FIRST_SUPPORTING),
                    TemplateText(SECOND_SUPPORTING),
                    Badge(BADGE_LABEL, outlineColor = 0),
                ),
        )

    private companion object {
        const val PRIMARY = "11,056"
        const val SENSITIVE_PRIMARY = "--"
        const val SECONDARY = "Steps"
        const val PROGRESS = 0.55f
        const val FIRST_SUPPORTING = "1,982 Cal"
        const val SECOND_SUPPORTING = "3,118 to go"
        const val BADGE_LABEL = "Z2"
        const val START_LABEL = "7 Day Avg"
        const val END_LABEL = "13,843"
        const val ICON_RES = 101
    }
}
