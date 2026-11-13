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

import android.net.Uri
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.TemplateText
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class DashboardTemplateTest {

    @Test
    fun constructor_actionsOnly_hasNoHeader() {
        val template = DashboardTemplate(actions = listOf(RUN))

        assertThat(template.actions).containsExactly(RUN)
        assertThat(template.leading).isNull()
        assertThat(template.primary).isNull()
        assertThat(template.secondary).isNull()
        assertThat(template.trailing).isNull()
        assertThat(template.hasHeader).isFalse()
    }

    @Test
    fun dashboardTemplate_isGlanceAdaptiveTemplate() {
        val template = DashboardTemplate(actions = listOf(RUN))

        assertThat(template).isInstanceOf(GlanceAdaptiveTemplate::class.java)
        assertThat(template.templateId).isEqualTo(DashboardTemplate.TEMPLATE_ID)
        assertThat(DashboardTemplate.TEMPLATE_ID).isEqualTo(2)
        assertThat(DashboardTemplate.TEMPLATE_ID).isNotEqualTo(TrackTemplate.TEMPLATE_ID)
    }

    @Test
    fun constructor_fullHeader_hasHeader() {
        val template = fullTemplate()

        assertThat(template.leading).isEqualTo(Icon(GLYPH))
        assertThat(template.primary).isEqualTo(TemplateText("Start a workout"))
        assertThat(template.secondary).isEqualTo(TemplateText("Last: Run, 5.2 km"))
        assertThat(template.trailing).isEqualTo(IconButton(GLYPH, "Log activity", LOG))
        assertThat(template.hasHeader).isTrue()
    }

    @Test
    fun constructor_actionCountWithinBounds_succeeds() {
        assertThat(DashboardTemplate(actions = actions(1)).actions).hasSize(1)
        assertThat(DashboardTemplate(actions = actions(DashboardTemplate.MAX_ACTIONS)).actions)
            .hasSize(DashboardTemplate.MAX_ACTIONS)
    }

    @Test
    fun constructor_noActions_throws() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                DashboardTemplate(actions = emptyList())
            }

        assertThat(error)
            .hasMessageThat()
            .isEqualTo("DashboardTemplate needs 1 to 8 actions (call actions { action(...) }).")
    }

    @Test
    fun constructor_tooManyActions_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            DashboardTemplate(actions = actions(DashboardTemplate.MAX_ACTIONS + 1))
        }
    }

    @Test
    fun constructor_headerIsAllOrNothing() {
        val withoutIcon =
            assertThrows(IllegalArgumentException::class.java) {
                DashboardTemplate(primary = TemplateText("Workouts"), actions = listOf(RUN))
            }
        assertThat(withoutIcon)
            .hasMessageThat()
            .isEqualTo(
                "DashboardTemplate needs a header icon with its header (call leading { icon(...) })."
            )

        val withoutTitle =
            assertThrows(IllegalArgumentException::class.java) {
                DashboardTemplate(leading = Icon(GLYPH), actions = listOf(RUN))
            }
        assertThat(withoutTitle)
            .hasMessageThat()
            .isEqualTo(
                "DashboardTemplate needs a header title with its header (call primary { text(...) })."
            )

        // A subtitle or a trailing button alone is a header too, so it needs both.
        val partialHeaders =
            listOf(
                { DashboardTemplate(secondary = TemplateText("Fitbit"), actions = listOf(RUN)) },
                {
                    DashboardTemplate(
                        trailing = IconButton(GLYPH, "Log activity"),
                        actions = listOf(RUN),
                    )
                },
            )
        for (build in partialHeaders) {
            val error = assertThrows(IllegalArgumentException::class.java) { build() }
            assertThat(error).hasMessageThat().contains("a header icon")
            assertThat(error).hasMessageThat().contains("a header title")
        }
    }

    @Test
    fun constructor_reportsEveryUnmetRequirement() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                DashboardTemplate(actions = emptyList(), secondary = TemplateText("Fitbit"))
            }

        assertThat(error)
            .hasMessageThat()
            .isEqualTo(
                "DashboardTemplate needs 1 to 8 actions (call actions { action(...) }); " +
                    "a header icon with its header (call leading { icon(...) }); " +
                    "a header title with its header (call primary { text(...) })."
            )
    }

    @Test
    fun constructor_copiesActions() {
        val actions = mutableListOf(RUN)
        val template = DashboardTemplate(actions = actions)

        actions += WALK

        assertThat(template.actions).containsExactly(RUN)
    }

    @Test
    fun equals_sameValues_areEqual() {
        assertThat(fullTemplate()).isEqualTo(fullTemplate())
        assertThat(fullTemplate().hashCode()).isEqualTo(fullTemplate().hashCode())
    }

    @Test
    fun equals_differingValues_areNotEqual() {
        val template = fullTemplate()

        assertThat(template).isNotEqualTo(fullTemplate(leading = Icon(OTHER_GLYPH)))
        assertThat(template).isNotEqualTo(fullTemplate(primary = TemplateText("Workouts")))
        assertThat(template).isNotEqualTo(fullTemplate(secondary = null))
        assertThat(template).isNotEqualTo(fullTemplate(trailing = null))
        assertThat(template).isNotEqualTo(fullTemplate(actions = listOf(RUN)))
        assertThat(template).isNotEqualTo(DashboardTemplate(actions = listOf(RUN, WALK)))
    }

    @Test
    fun toString_containsAllFields() {
        val text = fullTemplate().toString()

        assertThat(text).contains("Start a workout")
        assertThat(text).contains("Last: Run, 5.2 km")
        assertThat(text).contains("Log activity")
        assertThat(text).contains("Run")
        assertThat(text).contains("Walk")
    }

    private fun actions(count: Int): List<IconTile> = List(count) { IconTile(GLYPH, "Action $it") }

    private fun fullTemplate(
        leading: Icon? = Icon(GLYPH),
        primary: TemplateText? = TemplateText("Start a workout"),
        secondary: TemplateText? = TemplateText("Last: Run, 5.2 km"),
        trailing: IconButton? = IconButton(GLYPH, "Log activity", LOG),
        actions: List<IconTile> = listOf(RUN, WALK),
    ) =
        DashboardTemplate(
            actions = actions,
            leading = leading,
            primary = primary,
            secondary = secondary,
            trailing = trailing,
        )

    private companion object {
        const val GLYPH = 1
        const val OTHER_GLYPH = 2
        val LOG = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://log"))
        val RUN_ACTION = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://run"))
        val RUN = IconTile(GLYPH, "Run", RUN_ACTION)
        val WALK =
            IconTile(
                GLYPH,
                "Walk",
                GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://walk")),
                subtitle = TemplateText("Yesterday"),
            )
    }
}
