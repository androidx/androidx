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
import androidx.glance.adaptive.core.templates.TemplateIcon
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ListTemplateScopeTest {

    @Test
    fun listTemplate_buildsFullTemplate() {
        val template = listTemplate {
            leading { icon(CALENDAR_ICON) }
            title { text("Oct 6, TUE") { sensitive("Today") } }
            trailing {
                iconButton(ADD_ICON, "Create event") {
                    sensitive("Add")
                    onClick(CREATE_EVENT)
                }
            }
            items {
                item("Widget Weekly Sync") {
                    sensitive("Busy")
                    subtitle("10:30 - 11:30") { sensitive("1 hr") }
                    tertiaryText("204-Phone-Room", icon = LOCATION_ICON) {
                        sensitive("Meeting room")
                    }
                    trailingChip("Now")
                    onClick(OPEN_EVENT)
                }
                item("Design Review") {
                    subtitle("13:00 - 14:00")
                    trailingIcon(VIDEO_ICON, contentDescription = "Video call", contained = true)
                }
            }
        }

        assertThat(template.leading).isEqualTo(Icon(CALENDAR_ICON.resId))
        assertThat(template.title).isEqualTo(TemplateText("Oct 6, TUE", "Today"))
        assertThat(template.trailing)
            .isEqualTo(IconButton(ADD_ICON.resId, "Create event", CREATE_EVENT, "Add"))
        assertThat(template.showHeaderInCompactHeight).isTrue()
        assertThat(template.compactTitle).isNull()
        assertThat(template.items)
            .containsExactly(
                ListItem.Compact(
                    title = TemplateText("Widget Weekly Sync", "Busy"),
                    subtitle = TemplateText("10:30 - 11:30", "1 hr"),
                    tertiaryText = TemplateText("204-Phone-Room", "Meeting room"),
                    tertiaryIcon = Icon(LOCATION_ICON.resId),
                    trailingChip = Badge("Now"),
                    action = OPEN_EVENT,
                ),
                ListItem.Compact(
                    title = TemplateText("Design Review"),
                    subtitle = TemplateText("13:00 - 14:00"),
                    trailingIcon =
                        Icon(VIDEO_ICON.resId, contentDescription = "Video call", contained = true),
                ),
            )
            .inOrder()
    }

    @Test
    fun listTemplate_partialHeaderCompactOptionsAndUncontainedButton() {
        val template = listTemplate {
            showHeaderInCompactHeight(false)
            compactTitle("10/6") { sensitive("Date") }
            leading { icon(CALENDAR_ICON, contentDescription = "Calendar") }
            trailing {
                iconButton(ADD_ICON, "Refresh", contained = false) { onClick(CREATE_EVENT) }
            }
            items { item("Standup") }
        }

        assertThat(template.leading).isEqualTo(Icon(CALENDAR_ICON.resId, "Calendar"))
        assertThat(template.title).isNull()
        assertThat(template.trailing)
            .isEqualTo(IconButton(ADD_ICON.resId, "Refresh", CREATE_EVENT, contained = false))
        assertThat(template.showHeaderInCompactHeight).isFalse()
        assertThat(template.compactTitle).isEqualTo(TemplateText("10/6", "Date"))
    }

    @Test
    fun listTemplate_options_validateFailFastAndRejectDuplicates() {
        var reachedNextLine = false
        assertThrows(IllegalArgumentException::class.java) {
            listTemplate {
                compactTitle("   ")
                reachedNextLine = true
                items { item("Event") }
            }
        }
        assertThat(reachedNextLine).isFalse()

        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                compactTitle("10/6")
                compactTitle("10/7")
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                scrollable(false)
                scrollable(true)
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                showHeaderInCompactHeight(false)
                showHeaderInCompactHeight(true)
                items { item("Event") }
            }
        }
    }

    @Test
    fun listTemplate_itemsOnly_buildsWithoutHeader() {
        val template = listTemplate { items { item("Standup") } }

        assertThat(template.hasHeader).isFalse()
        assertThat(template.items).containsExactly(ListItem.Compact(TemplateText("Standup")))
    }

    @Test
    fun listTemplate_maxItemsBoundary_succeedsAt25AndThrowsAt26() {
        val atMax = listTemplate {
            items {
                for (i in 1..25) {
                    item("Event $i")
                }
            }
        }
        assertThat(atMax.items).hasSize(25)

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate {
                    items {
                        for (i in 1..26) {
                            item("Event $i")
                        }
                    }
                }
            }
        assertThat(error).hasMessageThat().contains("1 to 25 items")
    }

    @Test
    fun listTemplate_missingEmptyOrDividerOnlyItems_throws() {
        val missingError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate { title { text("Oct 6, TUE") } }
            }
        assertThat(missingError).hasMessageThat().contains("1 to 25 items")

        val emptyError =
            assertThrows(IllegalArgumentException::class.java) { listTemplate { items {} } }
        assertThat(emptyError).hasMessageThat().contains("at least one item")

        val dividerOnlyError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate { items { divider("TOMORROW") } }
            }
        assertThat(dividerOnlyError).hasMessageThat().contains("at least one non-divider item")
    }

    @Test
    fun listTemplate_blankComponentArguments_throw() {
        assertThrows(IllegalArgumentException::class.java) { listTemplate { items { item("  ") } } }
        assertThrows(IllegalArgumentException::class.java) {
            listTemplate {
                items {
                    item("Event 1")
                    divider("   ")
                }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            listTemplate {
                leading { icon(CALENDAR_ICON, "  ") }
                items { item("Event") }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            listTemplate {
                trailing { iconButton(ADD_ICON, "  ") }
                items { item("Event") }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            listTemplate { items { item("Event") { trailingChip("  ") } } }
        }
        assertThrows(IllegalArgumentException::class.java) {
            listTemplate { items { item("Event") { leadingIcon(CALENDAR_ICON, "  ") } } }
        }
        assertThrows(IllegalArgumentException::class.java) {
            listTemplate { items { item("Event") { trailingIcon(VIDEO_ICON, "  ") } } }
        }
    }

    @Test
    fun listTemplate_emptyHeaderSlots_throw() {
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                leading {}
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                title {}
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                trailing {}
                items { item("Event") }
            }
        }
    }

    @Test
    fun listTemplate_duplicateSlotBlocksOrComponentsInsideSlot_throw() {
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                leading { icon(CALENDAR_ICON) }
                leading { icon(CALENDAR_ICON) }
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                leading {
                    icon(CALENDAR_ICON)
                    icon(ADD_ICON)
                }
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                title { text("Oct 6") }
                title { text("Oct 7") }
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                title {
                    text("Oct 6")
                    text("Oct 7")
                }
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                trailing { iconButton(ADD_ICON, "Add") }
                trailing { iconButton(ADD_ICON, "Add") }
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                trailing {
                    iconButton(ADD_ICON, "Add")
                    iconButton(ADD_ICON, "Edit")
                }
                items { item("Event") }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items { item("Event 1") }
                items { item("Event 2") }
            }
        }
    }

    @Test
    fun item_conflictingOrDuplicateOptions_throw() {
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        trailingIcon(VIDEO_ICON)
                        trailingChip("Now")
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        trailingChip("Now")
                        trailingIcon(VIDEO_ICON)
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        subtitle("10:00")
                        subtitle("11:00")
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        tertiaryText("Room 1")
                        tertiaryText("Room 2")
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        leadingIcon(CALENDAR_ICON)
                        leadingIcon(ADD_ICON)
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        sensitive("Busy")
                        sensitive("Hidden")
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        onClick(OPEN_EVENT)
                        onClick(CREATE_EVENT)
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        style(ListItemStyle.Focused)
                        style(ListItemStyle.Outlined)
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        strikethrough(true)
                        strikethrough(false)
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        subtitle("10:00") {
                            strikethrough(true)
                            strikethrough(false)
                        }
                    }
                }
            }
        }
        assertThrows(IllegalStateException::class.java) {
            listTemplate {
                items {
                    item("Event") {
                        subtitle("10:00") {
                            sensitive("Morning")
                            sensitive("AM")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun item_stylesLeadingIconTextStrikethroughAndDividerOptions() {
        val template = listTemplate {
            scrollable(false)
            items {
                item("Sync") {
                    style(ListItemStyle.Focused)
                    subtitle("10:00") { strikethrough() }
                }
                item("OOO") {
                    leadingIcon(CALENDAR_ICON, "Busy")
                    style(ListItemStyle.Neutral)
                }
                divider("TOMORROW") { sensitive("UPCOMING") }
                item("Declined") {
                    subtitle("14:00")
                    tertiaryText("Room A")
                    style(ListItemStyle.Outlined)
                    strikethrough()
                }
                item("Rescheduled") {
                    subtitle("15:00") { strikethrough(false) }
                    tertiaryText("Room B")
                    strikethrough()
                }
            }
        }
        assertThat(template.isScrollable).isFalse()
        assertThat(template.items).hasSize(5)
        val first = template.items[0] as ListItem.Compact
        val second = template.items[1] as ListItem.Compact
        val third = template.items[2] as ListItem.Divider
        val fourth = template.items[3] as ListItem.Compact
        val fifth = template.items[4] as ListItem.Compact
        assertThat(first.style).isEqualTo(ListItemStyle.Focused)
        assertThat(first.title.isStruckThrough).isFalse()
        assertThat(first.subtitle?.isStruckThrough).isTrue()
        assertThat(second.style).isEqualTo(ListItemStyle.Neutral)
        assertThat(second.leadingIcon?.icon).isEqualTo(CALENDAR_ICON.resId)
        assertThat(third.title).isEqualTo(TemplateText("TOMORROW", "UPCOMING"))
        assertThat(fourth.style).isEqualTo(ListItemStyle.Outlined)
        assertThat(fourth.title.isStruckThrough).isTrue()
        assertThat(fourth.subtitle?.isStruckThrough).isTrue()
        assertThat(fourth.tertiaryText?.isStruckThrough).isTrue()
        // Explicit child strikethrough(false) opts out even when item-level strikethrough is set.
        assertThat(fifth.title.isStruckThrough).isTrue()
        assertThat(fifth.subtitle?.isStruckThrough).isFalse()
        assertThat(fifth.tertiaryText?.isStruckThrough).isTrue()
    }

    @Test
    fun listTemplate_actionsCheckboxItemsAndRichNotes() {
        val template = listTemplate {
            actions {
                action(ADD_ICON, "New note") {
                    sensitive("New")
                    onClick(CREATE_EVENT)
                }
                action(VIDEO_ICON, "New checklist") { onClick(OPEN_EVENT) }
            }
            items {
                checkboxItem("Bread flour (500g)") {
                    sensitive("Ingredient")
                    onClick(CREATE_EVENT)
                }
                checkboxItem("Fine sea salt (10g)", isChecked = true)
                checkboxItem("Keep visible", isChecked = true) { strikethrough(false) }
                detailCard("Material Next") {
                    sensitive("Note")
                    body("Audit widgets for dynamic color") { sensitive("Details") }
                    style(ListItemStyle.Focused)
                    onClick(OPEN_EVENT)
                }
                detailCard("Low-Knead Bread") {
                    checklist {
                        item("Bread flour (500g)") { sensitive("Item") }
                        item("Fine sea salt (10g)", isChecked = true)
                        item("No strike", isChecked = true) { strikethrough(false) }
                    }
                }
            }
        }

        assertThat(template.actions)
            .containsExactly(
                IconButton(ADD_ICON.resId, "New note", CREATE_EVENT, "New"),
                IconButton(VIDEO_ICON.resId, "New checklist", OPEN_EVENT),
            )
            .inOrder()
        assertThat(template.items)
            .containsExactly(
                ListItem.Checkbox(
                    title = TemplateText("Bread flour (500g)", "Ingredient"),
                    isChecked = false,
                    action = CREATE_EVENT,
                ),
                ListItem.Checkbox(
                    title = TemplateText("Fine sea salt (10g)", isStruckThrough = true),
                    isChecked = true,
                ),
                ListItem.Checkbox(
                    title = TemplateText("Keep visible", isStruckThrough = false),
                    isChecked = true,
                ),
                ListItem.DetailCard(
                    title = TemplateText("Material Next", "Note"),
                    body = TemplateText("Audit widgets for dynamic color", "Details"),
                    action = OPEN_EVENT,
                    style = ListItemStyle.Focused,
                ),
                ListItem.DetailCard(
                    title = TemplateText("Low-Knead Bread"),
                    checklistItems =
                        listOf(
                            ListItem.DetailCard.ChecklistEntry(
                                TemplateText("Bread flour (500g)", "Item"),
                                false,
                            ),
                            ListItem.DetailCard.ChecklistEntry(
                                TemplateText("Fine sea salt (10g)", isStruckThrough = true),
                                true,
                            ),
                            ListItem.DetailCard.ChecklistEntry(
                                TemplateText("No strike", isStruckThrough = false),
                                true,
                            ),
                        ),
                ),
            )
            .inOrder()
    }

    @Test
    fun listTemplate_actionsValidation_throws() {
        val emptyError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate {
                    actions {}
                    items { item("Event") }
                }
            }
        assertThat(emptyError).hasMessageThat().contains("at least one action")

        val blankLabelError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate {
                    actions { action(ADD_ICON, "  ") }
                    items { item("Event") }
                }
            }
        assertThat(blankLabelError).hasMessageThat().contains("action label must not be blank")

        val duplicateError =
            assertThrows(IllegalStateException::class.java) {
                listTemplate {
                    actions { action(ADD_ICON, "One") }
                    actions { action(ADD_ICON, "Two") }
                    items { item("Event") }
                }
            }
        assertThat(duplicateError).hasMessageThat().contains("actions slot was already configured")

        val overflowError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate {
                    actions {
                        for (i in 1..7) {
                            action(ADD_ICON, "Action $i")
                        }
                    }
                    items { item("Event") }
                }
            }
        assertThat(overflowError).hasMessageThat().contains("at most 6 actions")
    }

    @Test
    fun listTemplate_trailingAndActionsConflict_throws() {
        val actionsAfterTrailing =
            assertThrows(IllegalStateException::class.java) {
                listTemplate {
                    trailing { iconButton(ADD_ICON, "Add") }
                    actions { action(ADD_ICON, "New note") }
                    items { item("Event") }
                }
            }
        assertThat(actionsAfterTrailing)
            .hasMessageThat()
            .contains("trailing and actions slots cannot both be configured")

        val trailingAfterActions =
            assertThrows(IllegalStateException::class.java) {
                listTemplate {
                    actions { action(ADD_ICON, "New note") }
                    trailing { iconButton(ADD_ICON, "Add") }
                    items { item("Event") }
                }
            }
        assertThat(trailingAfterActions)
            .hasMessageThat()
            .contains("trailing and actions slots cannot both be configured")
    }

    @Test
    fun listTemplate_checkboxItemValidation_throws() {
        val blankError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate { items { checkboxItem("  ") } }
            }
        assertThat(blankError).hasMessageThat().contains("checkbox item title must not be blank")

        val duplicateStrikeError =
            assertThrows(IllegalStateException::class.java) {
                listTemplate {
                    items {
                        checkboxItem("Milk") {
                            strikethrough(true)
                            strikethrough(false)
                        }
                    }
                }
            }
        assertThat(duplicateStrikeError)
            .hasMessageThat()
            .contains("Checkbox strikethrough was already set")
    }

    @Test
    fun listTemplate_detailCardValidation_throws() {
        val blankTitleError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate { items { detailCard("  ") } }
            }
        assertThat(blankTitleError).hasMessageThat().contains("detail card title must not be blank")

        val emptyChecklistError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate { items { detailCard("Bread") { checklist {} } } }
            }
        assertThat(emptyChecklistError).hasMessageThat().contains("at least one entry")

        val overflowChecklistError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate {
                    items {
                        detailCard("Bread") {
                            checklist {
                                for (i in 1..ListItem.DetailCard.MAX_CHECKLIST_ITEMS + 1) {
                                    item("Step $i")
                                }
                            }
                        }
                    }
                }
            }
        assertThat(overflowChecklistError)
            .hasMessageThat()
            .contains("at most ${ListItem.DetailCard.MAX_CHECKLIST_ITEMS} checklist items")

        val blankChecklistItemError =
            assertThrows(IllegalArgumentException::class.java) {
                listTemplate { items { detailCard("Bread") { checklist { item("  ") } } } }
            }
        assertThat(blankChecklistItemError)
            .hasMessageThat()
            .contains("checklist item title must not be blank")

        val checklistAfterBodyError =
            assertThrows(IllegalStateException::class.java) {
                listTemplate {
                    items {
                        detailCard("Bread") {
                            body("Mix")
                            checklist { item("Flour") }
                        }
                    }
                }
            }
        assertThat(checklistAfterBodyError)
            .hasMessageThat()
            .contains("Detail card content was already set")

        val bodyAfterChecklistError =
            assertThrows(IllegalStateException::class.java) {
                listTemplate {
                    items {
                        detailCard("Bread") {
                            checklist { item("Flour") }
                            body("Mix")
                        }
                    }
                }
            }
        assertThat(bodyAfterChecklistError)
            .hasMessageThat()
            .contains("Detail card content was already set")

        val duplicateStyleError =
            assertThrows(IllegalStateException::class.java) {
                listTemplate {
                    items {
                        detailCard("Bread") {
                            style(ListItemStyle.Focused)
                            style(ListItemStyle.Neutral)
                        }
                    }
                }
            }
        assertThat(duplicateStyleError)
            .hasMessageThat()
            .contains("Detail card style was already set")
    }

    private companion object {
        val CALENDAR_ICON = TemplateIcon.fromResource(1)
        val ADD_ICON = TemplateIcon.fromResource(2)
        val VIDEO_ICON = TemplateIcon.fromResource(3)
        val LOCATION_ICON = TemplateIcon.fromResource(4)
        val CREATE_EVENT = GlanceAdaptiveAction.CallbackAction<GlanceAdaptiveActionCallback>()
        val OPEN_EVENT = GlanceAdaptiveAction.LaunchAction<Activity>()
    }
}
