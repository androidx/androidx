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

import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.TrackTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TrackMatrixSelectorTest {

    private val template =
        TrackTemplate(
            title = TrackTestData.TITLE,
            subtitle = TrackTestData.SUBTITLE,
            progress = TrackTestData.PROGRESS,
            statusText = TrackTestData.STATUS_TEXT,
        )

    @Test
    fun narrowAndShort_isTheBulb() {
        for (height in listOf(HeightTier.H0, HeightTier.H1)) {
            val bulb = select(WidthTier.W1, height) as MatrixBulbContent
            val ring = bulb.component as ContainedRing
            assertThat(ring.value).isEqualTo(TrackTestData.TITLE)
            assertThat(ring.progress).isEqualTo(TrackTestData.PROGRESS)
        }
    }

    @Test
    fun narrowAndShort_withoutATitle_staysACard() {
        val content = select(WidthTier.W1, HeightTier.H1, TrackTemplate(title = " "))

        assertThat(content).isInstanceOf(MatrixCardContent::class.java)
        assertThat((content as MatrixCardContent).primary).isNull()
    }

    @Test
    fun card_alwaysHasTheRingAndTheTitle() {
        for (width in WidthTier.entries) {
            for (height in listOf(HeightTier.H2, HeightTier.H3, HeightTier.H4)) {
                val card = card(width, height)
                assertThat((card.leading as Ring).progress).isEqualTo(TrackTestData.PROGRESS)
                assertThat((card.primary as TemplateText).value).isEqualTo(TrackTestData.TITLE)
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
        assertThat(
                card(
                        WidthTier.W4,
                        HeightTier.H2,
                        TrackTemplate(title = TrackTestData.TITLE, subtitle = " "),
                    )
                    .secondary
            )
            .isNull()
    }

    @Test
    fun card_actionCountFollowsWidthFromH2() {
        assertThat(card(WidthTier.W4, HeightTier.H1).actions).isEmpty()
        assertThat(card(WidthTier.W2, HeightTier.H2).actions).hasSize(1)
        assertThat(card(WidthTier.W3, HeightTier.H2).actions).hasSize(2)
        assertThat(card(WidthTier.W4, HeightTier.H2).actions).hasSize(3)
        assertThat(card(WidthTier.W1, HeightTier.H3).actions).isEmpty()
        assertThat(card(WidthTier.W1, HeightTier.H4).actions).hasSize(1)
    }

    @Test
    fun card_heroGraphFromH3AndCaptionedAtH4() {
        assertThat(card(WidthTier.W4, HeightTier.H2).hero).isNull()
        assertThat(card(WidthTier.W1, HeightTier.H4).hero).isNull()
        assertThat(card(WidthTier.W4, HeightTier.H3).hero)
            .isSameInstanceAs(TrackPlaceholderData.WeekGraph)
        assertThat(card(WidthTier.W4, HeightTier.H4).hero)
            .isSameInstanceAs(TrackPlaceholderData.CaptionedWeekGraph)
    }

    @Test
    fun card_missingStatusTextLeavesItsPositionEmpty() {
        val card = card(WidthTier.W4, HeightTier.H2, TrackTemplate(title = TrackTestData.TITLE))
        val blankStatusCard =
            card(
                WidthTier.W4,
                HeightTier.H2,
                TrackTemplate(title = TrackTestData.TITLE, statusText = " "),
            )

        assertThat(card.actions).hasSize(3)
        assertThat(card.actions[0]).isNull()
        assertThat(blankStatusCard.actions[0]).isNull()
    }

    @Test
    fun card_withoutProgress_hasNoRing() {
        assertThat(card(WidthTier.W4, HeightTier.H2, TrackTemplate(title = "1")).leading).isNull()
        assertThat(
                card(WidthTier.W4, HeightTier.H2, TrackTemplate(title = "1", progress = Float.NaN))
                    .leading
            )
            .isNull()
    }

    @Test
    fun card_outOfRangeProgressIsClamped() {
        val high = card(WidthTier.W4, HeightTier.H2, TrackTemplate(title = "1", progress = 2f))
        val low = card(WidthTier.W4, HeightTier.H2, TrackTemplate(title = "1", progress = -1f))

        assertThat((high.leading as Ring).progress).isEqualTo(1f)
        assertThat((low.leading as Ring).progress).isEqualTo(0f)
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
}
