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

package androidx.glance.adaptive.core.templates.list

import android.app.Activity
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.action.GlanceAdaptiveActionCallback
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.ListItem
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.quickactions.QuickActionsTemplate
import androidx.glance.adaptive.core.templates.track.TrackTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ListTemplateTest {

    @Test
    fun constructor_itemsOnly_hasNoHeader() {
        val template = ListTemplate(items = listOf(EVENT_1))

        assertThat(template.items).containsExactly(EVENT_1)
        assertThat(template.leading).isNull()
        assertThat(template.title).isNull()
        assertThat(template.trailing).isNull()
        assertThat(template.hasHeader).isFalse()
    }

    @Test
    fun listTemplate_isGlanceAdaptiveTemplate() {
        val template = ListTemplate(items = listOf(EVENT_1))

        assertThat(template).isInstanceOf(GlanceAdaptiveTemplate::class.java)
        assertThat(template.templateId).isEqualTo(ListTemplate.TEMPLATE_ID)
        assertThat(ListTemplate.TEMPLATE_ID).isEqualTo(3)
        assertThat(ListTemplate.TEMPLATE_ID).isNotEqualTo(TrackTemplate.TEMPLATE_ID)
        assertThat(ListTemplate.TEMPLATE_ID).isNotEqualTo(QuickActionsTemplate.TEMPLATE_ID)
    }

    @Test
    fun constructor_fullHeader_hasHeader() {
        val template = fullTemplate()

        assertThat(template.leading).isEqualTo(Icon(GLYPH))
        assertThat(template.title).isEqualTo(TemplateText("Oct 6, TUE"))
        assertThat(template.trailing).isEqualTo(IconButton(GLYPH, "Create event", ADD_ACTION))
        assertThat(template.hasHeader).isTrue()
    }

    @Test
    fun constructor_partialHeader_succeeds() {
        val leadingOnly = ListTemplate(items = listOf(EVENT_1), leading = Icon(GLYPH))
        val titleOnly = ListTemplate(items = listOf(EVENT_1), title = TemplateText("Oct 6, TUE"))
        val trailingOnly =
            ListTemplate(items = listOf(EVENT_1), trailing = IconButton(GLYPH, "Add"))

        assertThat(leadingOnly.hasHeader).isTrue()
        assertThat(titleOnly.hasHeader).isTrue()
        assertThat(trailingOnly.hasHeader).isTrue()
    }

    @Test
    fun constructor_itemCountWithinBounds_succeeds() {
        assertThat(ListTemplate(items = items(1)).items).hasSize(1)
        assertThat(ListTemplate(items = items(ListTemplate.MAX_ITEMS)).items)
            .hasSize(ListTemplate.MAX_ITEMS)
    }

    @Test
    fun constructor_noItems_throws() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                ListTemplate(items = emptyList())
            }

        assertThat(error)
            .hasMessageThat()
            .isEqualTo(
                "ListTemplate needs 1 to ${ListTemplate.MAX_ITEMS} items (call items { item(...) })."
            )
    }

    @Test
    fun constructor_tooManyItems_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ListTemplate(items = items(ListTemplate.MAX_ITEMS + 1))
        }
    }

    @Test
    fun constructor_copiesItemsAndActionsListsAndBoundsActions() {
        val sourceItems = mutableListOf<ListItem>(EVENT_1)
        val sourceActions = mutableListOf(IconButton(GLYPH, "Add", ADD_ACTION))
        val template = ListTemplate(items = sourceItems, actions = sourceActions)
        sourceItems.add(EVENT_2)
        sourceActions.add(IconButton(GLYPH, "Checklist"))

        assertThat(template.items).containsExactly(EVENT_1)
        assertThat(template.actions).containsExactly(IconButton(GLYPH, "Add", ADD_ACTION))

        val atMaxActions =
            ListTemplate(
                items = listOf(EVENT_1),
                actions = List(ListTemplate.MAX_ACTIONS) { i -> IconButton(GLYPH, "Action $i") },
            )
        assertThat(atMaxActions.actions).hasSize(ListTemplate.MAX_ACTIONS)

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                ListTemplate(
                    items = listOf(EVENT_1),
                    actions =
                        List(ListTemplate.MAX_ACTIONS + 1) { i -> IconButton(GLYPH, "Action $i") },
                )
            }
        assertThat(error).hasMessageThat().contains("at most ${ListTemplate.MAX_ACTIONS} actions")
    }

    @Test
    fun constructor_dividerOnly_throws() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                ListTemplate(items = listOf(ListItem.Divider(title = TemplateText("TOMORROW"))))
            }
        assertThat(error).hasMessageThat().contains("at least one non-divider item")
    }

    @Test
    fun constructor_trailingAndActionsTogether_throws() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                ListTemplate(
                    items = listOf(EVENT_1),
                    trailing = IconButton(GLYPH, "Add"),
                    actions = listOf(IconButton(GLYPH, "New note")),
                )
            }
        assertThat(error).hasMessageThat().contains("at most one of trailing or actions")
    }

    @Test
    fun constructor_combinedUnmetRequirements_reportsAllFailures() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                ListTemplate(
                    items = List(26) { ListItem.Divider(title = TemplateText("DIV $it")) },
                    trailing = IconButton(GLYPH, "Add"),
                    actions =
                        List(ListTemplate.MAX_ACTIONS + 1) { i -> IconButton(GLYPH, "Action $i") },
                )
            }
        assertThat(error).hasMessageThat().contains("1 to 25 items")
        assertThat(error).hasMessageThat().contains("at least one non-divider item")
        assertThat(error).hasMessageThat().contains("at most ${ListTemplate.MAX_ACTIONS} actions")
        assertThat(error).hasMessageThat().contains("at most one of trailing or actions")
    }

    @Test
    fun listItemStyle_toString_describesAllValues() {
        assertThat(ListItemStyle.Default.toString()).isEqualTo("Default")
        assertThat(ListItemStyle.Focused.toString()).isEqualTo("Focused")
        assertThat(ListItemStyle.Neutral.toString()).isEqualTo("Neutral")
        assertThat(ListItemStyle.Outlined.toString()).isEqualTo("Outlined")
    }

    @Test
    fun equalsHashCodeAndToString_reflectAllFields() {
        val base = fullTemplate()
        assertThat(base).isEqualTo(fullTemplate())
        assertThat(base.hashCode()).isEqualTo(fullTemplate().hashCode())
        assertThat(base).isNotEqualTo(ListTemplate(items = listOf(EVENT_1)))
        assertThat(base)
            .isNotEqualTo(
                ListTemplate(
                    items = base.items,
                    leading = null,
                    title = base.title,
                    trailing = base.trailing,
                )
            )
        assertThat(base)
            .isNotEqualTo(
                ListTemplate(
                    items = base.items,
                    leading = base.leading,
                    title = null,
                    trailing = base.trailing,
                )
            )
        assertThat(base)
            .isNotEqualTo(
                ListTemplate(
                    items = base.items,
                    leading = base.leading,
                    title = base.title,
                    trailing = null,
                )
            )
        assertThat(base)
            .isNotEqualTo(
                ListTemplate(
                    items = base.items,
                    leading = base.leading,
                    title = base.title,
                    trailing = base.trailing,
                    isScrollable = false,
                )
            )
        assertThat(base)
            .isNotEqualTo(
                ListTemplate(
                    items = base.items,
                    leading = base.leading,
                    title = base.title,
                    trailing = base.trailing,
                    showHeaderInCompactHeight = false,
                )
            )
        assertThat(base)
            .isNotEqualTo(
                ListTemplate(
                    items = base.items,
                    leading = base.leading,
                    title = base.title,
                    trailing = base.trailing,
                    compactTitle = TemplateText("10/6", sensitiveReplacement = "Date"),
                )
            )
        assertThat(ListTemplate(items = base.items, leading = base.leading, title = base.title))
            .isNotEqualTo(
                ListTemplate(
                    items = base.items,
                    leading = base.leading,
                    title = base.title,
                    actions = listOf(IconButton(GLYPH, "Add note")),
                )
            )
        assertThat(base.toString()).contains("ListTemplate(")
        assertThat(base.toString()).contains("isScrollable=true")
        assertThat(base.toString()).contains("actions=")
    }

    private companion object {
        const val GLYPH = 1
        val OPEN_ACTION = GlanceAdaptiveAction.LaunchAction<Activity>()
        val ADD_ACTION = GlanceAdaptiveAction.CallbackAction<GlanceAdaptiveActionCallback>()
        val EVENT_1 =
            ListItem.Compact(
                title = TemplateText("Widget Weekly Sync"),
                subtitle = TemplateText("10:30 - 11:30"),
                tertiaryText = TemplateText("204-Phone-Room"),
                trailingChip = Badge("Now"),
                action = OPEN_ACTION,
            )
        val EVENT_2 = ListItem.Compact(title = TemplateText("Lunch"))

        fun items(count: Int): List<ListItem> =
            List(count) { i -> ListItem.Compact(title = TemplateText("Item $i")) }

        fun fullTemplate(): ListTemplate =
            ListTemplate(
                items = listOf(EVENT_1, EVENT_2),
                leading = Icon(GLYPH),
                title = TemplateText("Oct 6, TUE"),
                trailing = IconButton(GLYPH, "Create event", ADD_ACTION),
            )
    }
}
