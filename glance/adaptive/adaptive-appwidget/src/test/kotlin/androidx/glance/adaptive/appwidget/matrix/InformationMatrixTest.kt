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

package androidx.glance.adaptive.appwidget.matrix

import android.app.Activity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.components.actionrail.ActionRailDefaults
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.ItemList
import androidx.glance.adaptive.core.slots.ItemListLayoutMode
import androidx.glance.adaptive.core.slots.ListItem
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.list.ListItemStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InformationMatrixTest {

    @Test
    fun card_rowHeaderUsesTheTierDefaults() {
        val card = resolve(MatrixCardContent(primary = TemplateText("11,056")), at(348, 132))

        assertThat(card.stacked).isFalse()
        assertThat(card.padding).isEqualTo(20.dp)
        assertThat(card.slotSpacing).isEqualTo(12.dp)
        assertThat(card.primaryTextSize).isEqualTo(40.sp)
        assertThat(card.cornerRadius).isNull()
    }

    @Test
    fun card_explicitContainerValuesOverrideTheDefaults() {
        val card =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("11,056"),
                    padding = 4.dp,
                    slotSpacing = 2.dp,
                    cornerRadius = 10.dp,
                ),
                at(348, 132),
            )

        assertThat(card.padding).isEqualTo(4.dp)
        assertThat(card.slotSpacing).isEqualTo(2.dp)
        assertThat(card.cornerRadius).isEqualTo(10.dp)
    }

    @Test
    fun card_stackedHeaderGivesUpPaddingBeforeShrinkingTheLeadingComponent() {
        val content =
            MatrixCardContent(leading = Ring(progress = 0.5f), primary = TemplateText("1"))

        val nominal = resolve(content, at(88, 176))
        assertThat(nominal.stacked).isTrue()
        assertThat(nominal.padding).isEqualTo(16.dp)
        assertThat(nominal.leadingSize).isEqualTo(56.dp)

        val narrow = resolve(content, at(68, 176))
        assertThat(narrow.padding).isEqualTo(8.dp)
        assertThat(narrow.leadingSize).isEqualTo(52.dp)
    }

    @Test
    fun card_rowHeaderGivesUpPaddingBeforeShrinkingTheLeadingComponentInShortContainers() {
        val content =
            MatrixCardContent(leading = Ring(progress = 0.5f), primary = TemplateText("11,056"))

        val nominal = resolve(content, at(176, 88))
        assertThat(nominal.padding).isEqualTo(16.dp)
        assertThat(nominal.leadingSize).isEqualTo(56.dp)
        assertThat(nominal.headerRowMinHeight).isEqualTo(56.dp)

        // Tight H1 cell gives up padding first so the ring keeps its full 56 dp size.
        val tightHome = resolve(content, at(176, 76))
        assertThat(tightHome.padding).isEqualTo(10.dp)
        assertThat(tightHome.leadingSize).isEqualTo(56.dp)
        assertThat(tightHome.headerRowMinHeight).isEqualTo(56.dp)

        // Lock screen 176x56 dp hits the 8 dp padding floor and shrinks the ring and row minHeight
        // to the remaining 40 dp content height.
        val lockScreen = resolve(content, at(176, 56, AppWidgetSurface.MOBILE_LOCK_SCREEN))
        assertThat(lockScreen.padding).isEqualTo(8.dp)
        assertThat(lockScreen.leadingSize).isEqualTo(40.dp)
        assertThat(lockScreen.headerRowMinHeight).isEqualTo(40.dp)

        // A standard row header with no leading component (e.g. bar-chart-only TrackTemplate on
        // lock screen) also yields padding against the 56 dp row minHeight.
        val textOnly =
            resolve(
                MatrixCardContent(primary = TemplateText("11,056")),
                at(176, 56, AppWidgetSurface.MOBILE_LOCK_SCREEN),
            )
        assertThat(textOnly.padding).isEqualTo(8.dp)
        assertThat(textOnly.headerRowMinHeight).isEqualTo(40.dp)
    }

    @Test
    fun card_rowHeaderShortensThePrimaryTextToItsBudget() {
        val card =
            resolve(
                MatrixCardContent(
                    leading = Ring(progress = 0.5f),
                    primary = TemplateText("1,234,567"),
                    secondary = TemplateText("Steps"),
                ),
                at(176, 132),
            )

        val primary = card.primary as TemplateText
        assertThat(primary.value).isNotEqualTo("1,234,567")
        assertThat(primary.value.length).isLessThan("1,234,567".length)
    }

    @Test
    fun card_rowHeaderCapsALongSecondaryTextBeforeItSqueezesThePrimaryText() {
        val card =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("11,056"),
                    secondary = TemplateText("Steps walked since midnight on all of your devices"),
                ),
                at(348, 132),
            )

        // 348 dp wide less 2 x 20 dp padding leaves 308 dp, of which the secondary takes 40%.
        assertThat(card.secondaryMaxWidth).isEqualTo((308f * 0.4f).dp)
        assertThat((card.primary as TemplateText).value).isEqualTo("11,056")
    }

    @Test
    fun card_stackedSecondaryTextIsNotCapped() {
        val card =
            resolve(
                MatrixCardContent(primary = TemplateText("1"), secondary = TemplateText("Steps")),
                at(88, 176),
            )

        assertThat(card.stacked).isTrue()
        assertThat(card.secondaryMaxWidth).isNull()
    }

    @Test
    fun card_trailingEmptyActionsAreTrimmed() {
        val action = TemplateText("3,118 to go")
        val card =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("11,056"),
                    actions = listOf(null, action, null),
                ),
                at(348, 132),
            )

        assertThat(card.actions).containsExactly(null, action).inOrder()
    }

    @Test
    fun card_emptyPrimaryStaysEmpty() {
        val card = resolve(MatrixCardContent(secondary = TemplateText("Steps")), at(348, 132))

        assertThat(card.primary).isNull()
    }

    @Test
    fun card_withEverySlotEmptyResolvesToAnEmptyCard() {
        val card = resolve(MatrixCardContent(actions = listOf(null, null)), at(348, 176))

        assertThat(card.leading).isNull()
        assertThat(card.primary).isNull()
        assertThat(card.secondary).isNull()
        assertThat(card.hero).isNull()
        assertThat(card.actions).isEmpty()
        assertThat(card.padding).isEqualTo(20.dp)
    }

    @Test
    fun card_carriesTheTiersItWasLaidOutAt() {
        val card = resolve(MatrixCardContent(primary = TemplateText("1")), at(264, 176))

        assertThat(card.tiers).isEqualTo(SizeTiers(WidthTier.W3, HeightTier.H3))
    }

    @Test
    fun card_sensitiveTextIsReplacedOnlyOnTheLockScreen() {
        val content =
            MatrixCardContent(
                primary = TemplateText("6 / 8", sensitiveReplacement = "-- / 8"),
                secondary = TemplateText("Glasses", sensitiveReplacement = "Water"),
                actions = listOf(TemplateText("2 to go", sensitiveReplacement = "Keep going")),
            )

        val home = resolve(content, at(348, 132))
        assertThat((home.primary as TemplateText).value).isEqualTo("6 / 8")
        assertThat((home.secondary as TemplateText).value).isEqualTo("Glasses")
        assertThat((home.actions.single() as TemplateText).value).isEqualTo("2 to go")

        val locked = resolve(content, at(348, 132, AppWidgetSurface.MOBILE_LOCK_SCREEN))
        assertThat((locked.primary as TemplateText).value).isEqualTo("-- / 8")
        assertThat((locked.secondary as TemplateText).value).isEqualTo("Water")
        assertThat((locked.actions.single() as TemplateText).value).isEqualTo("Keep going")
    }

    @Test
    fun bulb_spansTheShorterSideAndNeverCollapses() {
        val ring = ContainedRing(progress = 0.5f, value = "42")

        assertThat(resolve(MatrixBulbContent(ring), at(88, 120)).containerSize).isEqualTo(88.dp)
        assertThat(resolve(MatrixBulbContent(ring), at(0, 0)).containerSize).isEqualTo(48.dp)
    }

    @Test
    fun bulb_sensitiveValueIsReplacedOnlyOnTheLockScreen() {
        val content =
            MatrixBulbContent(
                ContainedRing(progress = 0.75f, value = "6 / 8", sensitiveReplacement = "-- / 8")
            )

        val home = resolve(content, at(88, 88)).component as ContainedRing
        assertThat(home.value).isEqualTo("6 / 8")

        val locked =
            resolve(content, at(88, 88, AppWidgetSurface.MOBILE_LOCK_SCREEN)).component
                as ContainedRing
        assertThat(locked.value).isEqualTo("-- / 8")
        assertThat(locked.progress).isEqualTo(0.75f)
    }

    @Test
    fun card_compactCardUsesItsLeadingAndPrimaryTextSizes() {
        val card =
            resolve(
                MatrixCardContent(
                    leading = Icon(icon = GLYPH),
                    primary = TemplateText("Start a run"),
                    padding = 0.dp,
                    leadingSize = 16.dp,
                    primaryTextSize = 14.sp,
                ),
                at(168, 28, AppWidgetSurface.MOBILE_LOCK_SCREEN),
            )

        assertThat(card.padding).isEqualTo(0.dp)
        assertThat(card.leadingSize).isEqualTo(16.dp)
        assertThat(card.primaryTextSize).isEqualTo(14.sp)
        assertThat((card.primary as TemplateText).value).isEqualTo("Start a run")
    }

    @Test
    fun card_rejectsNegativeLeadingSizeOrNonPositivePrimaryTextSize() {
        assertThrows(IllegalArgumentException::class.java) {
            MatrixCardContent(leadingSize = (-1).dp)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MatrixCardContent(primaryTextSize = 0.sp)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MatrixCardContent(primaryTextSize = (-1).sp)
        }
        assertThat(MatrixCardContent(leadingSize = 0.dp).leadingSize).isEqualTo(0.dp)
    }

    @Test
    fun card_compactHeaderGivesTheSecondaryTextTheLineUnderThePrimaryText() {
        val card =
            resolve(
                MatrixCardContent(
                    leading = Icon(icon = GLYPH),
                    primary = TemplateText("1,234,567"),
                    secondary = TemplateText("Steps walked since midnight"),
                    leadingSize = 40.dp,
                    primaryTextSize = 16.sp,
                ),
                at(176, 88),
            )

        // 176 dp less 2 x 16 dp padding, a 40 dp icon and an 8 dp gap leaves 96 dp for both lines.
        assertThat(card.compactHeader).isTrue()
        assertThat(card.secondaryMaxWidth).isEqualTo(96.dp)
        assertThat((card.primary as TemplateText).value).isEqualTo("1,234,567")
    }

    @Test
    fun card_stackedHeaderShrinksAnOverriddenLeadingSizeToItsColumn() {
        val card =
            resolve(
                MatrixCardContent(leading = Icon(icon = GLYPH), leadingSize = 100.dp),
                at(88, 176),
            )

        // 88 dp wide gives up padding down to 8 dp, which leaves 72 dp for the leading component.
        assertThat(card.stacked).isTrue()
        assertThat(card.leadingSize).isEqualTo(72.dp)
    }

    @Test
    fun card_carriesItsInteractiveComponents() {
        val button = IconButton(GLYPH, "More", ACTION)
        val grid = IconGrid(listOf(IconTile(GLYPH, "Run", ACTION)), columns = 2, rows = 1)
        val card = resolve(MatrixCardContent(hero = grid, actions = listOf(button)), at(348, 132))

        assertThat(card.hero).isSameInstanceAs(grid)
        assertThat(card.actions.single()).isSameInstanceAs(button)
    }

    @Test
    fun card_trailingTakesTheDefaultButtonSizeOutOfTheRow() {
        val button = IconButton(GLYPH, "Other workouts", ACTION)
        fun content(trailing: IconButton?) =
            MatrixCardContent(
                leading = Icon(GLYPH),
                primary = TemplateText("1,234,567"),
                trailing = trailing,
                leadingSize = 40.dp,
                primaryTextSize = 16.sp,
            )

        val without = resolve(content(trailing = null), at(176, 88))
        val with = resolve(content(trailing = button), at(176, 88))

        assertThat(with.trailing).isSameInstanceAs(button)
        // Contained trailing buttons keep the full 56 dp pill height independent of leadingSize.
        assertThat(with.trailingSize).isEqualTo(MatrixDefaults.LeadingSize)
        assertThat(without.trailing).isNull()
        assertThat(without.trailingSize).isEqualTo(0.dp)
        // 176 dp less 2 x 16 dp padding, a 40 dp icon and an 8 dp gap leaves 96 dp for the
        // metric, or 24 dp with a 64 dp trailing pill + 8 dp gap: too narrow for it at 16 sp.
        assertThat((without.primary as TemplateText).value).isEqualTo("1,234,567")
        assertThat((with.primary as TemplateText).value).isNotEqualTo("1,234,567")
    }

    @Test
    fun card_containedTrailingButtonShrinksWithLeadingInShortContainers() {
        val content =
            MatrixCardContent(
                leading = Icon(GLYPH),
                primary = TemplateText("Workout"),
                trailing = IconButton(GLYPH, "Start", ACTION),
                padding = 12.dp,
                leadingSize = 56.dp,
            )

        val nominal = resolve(content, at(264, 88))
        assertThat(nominal.leadingSize).isEqualTo(56.dp)
        assertThat(nominal.trailingSize).isEqualTo(56.dp)

        // Tight H1 cell (76 - 24 = 52 dp content height) shrinks both slots together.
        val tight = resolve(content, at(264, 76))
        assertThat(tight.leadingSize).isEqualTo(52.dp)
        assertThat(tight.trailingSize).isEqualTo(52.dp)

        // With default padding and a small leading icon, the 56 dp contained trailing button
        // still yields padding first before shrinking.
        val smallLeading =
            MatrixCardContent(
                leading = Icon(GLYPH),
                primary = TemplateText("Workout"),
                trailing = IconButton(GLYPH, "Start", ACTION),
                leadingSize = 20.dp,
            )
        val tightDefaultPadding = resolve(smallLeading, at(176, 76))
        assertThat(tightDefaultPadding.padding).isEqualTo(10.dp)
        assertThat(tightDefaultPadding.trailingSize).isEqualTo(56.dp)

        val shortDefaultPadding =
            resolve(smallLeading, at(176, 56, AppWidgetSurface.MOBILE_LOCK_SCREEN))
        assertThat(shortDefaultPadding.padding).isEqualTo(8.dp)
        assertThat(shortDefaultPadding.trailingSize).isEqualTo(40.dp)
    }

    @Test
    fun card_stackedHeaderHidesTheTrailingComponent() {
        val card =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("Start"),
                    trailing = IconButton(GLYPH, "More", ACTION),
                ),
                at(88, 176),
            )

        assertThat(card.stacked).isTrue()
        assertThat(card.trailing).isNull()
    }

    @Test
    fun card_sensitiveTrailingDescriptionIsReplacedOnTheLockScreen() {
        val content =
            MatrixCardContent(
                primary = TemplateText("Favorites"),
                trailing = IconButton(GLYPH, "Call Alex", ACTION, sensitiveReplacement = "Call"),
            )

        val home = resolve(content, at(348, 132)).trailing as IconButton
        val locked =
            resolve(content, at(348, 132, AppWidgetSurface.MOBILE_LOCK_SCREEN)).trailing
                as IconButton
        assertThat(home.contentDescription).isEqualTo("Call Alex")
        assertThat(locked.contentDescription).isEqualTo("Call")
        assertThat(locked.action).isEqualTo(ACTION)
    }

    @Test
    fun card_sensitiveButtonTextIsReplacedOnlyOnTheLockScreen() {
        val content =
            MatrixCardContent(
                hero =
                    IconGrid(
                        listOf(
                            IconTile(GLYPH, "Call Alex", ACTION, sensitiveReplacement = "Call"),
                            IconTile(GLYPH, "Run", ACTION),
                        ),
                        columns = 2,
                        rows = 1,
                        showLabels = false,
                    ),
                actions =
                    listOf(
                        IconButton(GLYPH, "Message Alex", ACTION, sensitiveReplacement = "Message")
                    ),
            )

        val home = resolve(content, at(348, 176))
        val homeGrid = home.hero as IconGrid
        assertThat(homeGrid.tiles.map { it.label }).containsExactly("Call Alex", "Run")
        assertThat((home.actions[0] as IconButton).contentDescription).isEqualTo("Message Alex")

        val locked = resolve(content, at(348, 176, AppWidgetSurface.MOBILE_LOCK_SCREEN))
        val lockedGrid = locked.hero as IconGrid
        assertThat(lockedGrid.tiles.map { it.label }).containsExactly("Call", "Run")
        assertThat(lockedGrid.tiles.map { it.action }).containsExactly(ACTION, ACTION)
        assertThat(lockedGrid.columns).isEqualTo(2)
        assertThat(lockedGrid.showLabels).isFalse()
        val lockedButton = locked.actions[0] as IconButton
        assertThat(lockedButton.contentDescription).isEqualTo("Message")
        assertThat(lockedButton.action).isEqualTo(ACTION)

        val nonSensitiveGrid =
            IconGrid(listOf(IconTile(GLYPH, "Run", ACTION)), columns = 1, rows = 1)
        assertThat(
                resolve(
                        MatrixCardContent(hero = nonSensitiveGrid),
                        at(348, 176, AppWidgetSurface.MOBILE_LOCK_SCREEN),
                    )
                    .hero
            )
            .isSameInstanceAs(nonSensitiveGrid)
    }

    @Test
    fun card_tileSubtitleIsReplacedOnlyOnTheLockScreen() {
        val content =
            MatrixCardContent(
                hero =
                    IconGrid(
                        listOf(
                            IconTile(
                                GLYPH,
                                "Home Alarm",
                                ACTION,
                                subtitle =
                                    TemplateText("Code 1234", sensitiveReplacement = "Armed"),
                            ),
                            IconTile(GLYPH, "Garage Door", subtitle = TemplateText("Open")),
                        ),
                        columns = 2,
                        rows = 1,
                    )
            )

        val home = resolve(content, at(348, 176)).hero as IconGrid
        assertThat(home.tiles.map { it.subtitle?.value }).containsExactly("Code 1234", "Open")

        val locked =
            resolve(content, at(348, 176, AppWidgetSurface.MOBILE_LOCK_SCREEN)).hero as IconGrid
        assertThat(locked.tiles.map { it.label }).containsExactly("Home Alarm", "Garage Door")
        assertThat(locked.tiles.map { it.subtitle?.value }).containsExactly("Armed", "Open")
        // A tile with nothing sensitive is kept as it is.
        assertThat(locked.tiles[1]).isSameInstanceAs((content.hero as IconGrid).tiles[1])
    }

    @Test
    fun bulb_iconButtonContentDescriptionIsReplacedOnlyOnTheLockScreen() {
        val button = IconButton(GLYPH, "Call Alex", ACTION, sensitiveReplacement = "Call")

        assertThat(resolve(MatrixBulbContent(button), at(88, 88)).component)
            .isSameInstanceAs(button)
        val locked =
            resolve(MatrixBulbContent(button), at(88, 88, AppWidgetSurface.MOBILE_LOCK_SCREEN))
                .component as IconButton
        assertThat(locked.contentDescription).isEqualTo("Call")
        assertThat(locked.icon).isEqualTo(GLYPH)
        assertThat(locked.action).isEqualTo(ACTION)
    }

    @Test
    fun card_preservesStruckThroughAcrossPrimaryFitToAndDropsOnLockScreenRedaction() {
        val fitted =
            resolve(
                MatrixCardContent(
                    leading = Icon(GLYPH),
                    primary = TemplateText("1,234,567", isStruckThrough = true),
                    trailing = IconButton(GLYPH, "More", ACTION),
                    leadingSize = 40.dp,
                    primaryTextSize = 16.sp,
                ),
                at(176, 88),
            )
        val fittedPrimary = fitted.primary as TemplateText
        assertThat(fittedPrimary.value).isNotEqualTo("1,234,567")
        assertThat(fittedPrimary.isStruckThrough).isTrue()

        val locked =
            resolve(
                MatrixCardContent(
                    primary =
                        TemplateText(
                            "Secret",
                            sensitiveReplacement = "Hidden",
                            isStruckThrough = true,
                        ),
                    secondary =
                        TemplateText(
                            "10:00",
                            sensitiveReplacement = "Time",
                            isStruckThrough = true,
                        ),
                ),
                at(348, 132, AppWidgetSurface.MOBILE_LOCK_SCREEN),
            )
        val lockedPrimary = locked.primary as TemplateText
        val lockedSecondary = locked.secondary as TemplateText
        assertThat(lockedPrimary.value).isEqualTo("Hidden")
        assertThat(lockedPrimary.isStruckThrough).isFalse()
        assertThat(lockedSecondary.value).isEqualTo("Time")
        assertThat(lockedSecondary.isStruckThrough).isFalse()
    }

    @Test
    fun cardAndBulb_preserveUncontainedIconButtonAcrossLockScreenRedaction() {
        val button =
            IconButton(GLYPH, "Call Alex", ACTION, sensitiveReplacement = "Call", contained = false)

        val lockedCardTrailing =
            resolve(
                    MatrixCardContent(primary = TemplateText("Favorites"), trailing = button),
                    at(348, 132, AppWidgetSurface.MOBILE_LOCK_SCREEN),
                )
                .trailing as IconButton
        assertThat(lockedCardTrailing.contentDescription).isEqualTo("Call")
        assertThat(lockedCardTrailing.contained).isFalse()

        val lockedBulbButton =
            resolve(MatrixBulbContent(button), at(88, 88, AppWidgetSurface.MOBILE_LOCK_SCREEN))
                .component as IconButton
        assertThat(lockedBulbButton.contentDescription).isEqualTo("Call")
        assertThat(lockedBulbButton.contained).isFalse()
    }

    @Test
    fun card_itemListRedactionPreservesLayoutAndItemMetadata() {
        val leadingIcon = Icon(GLYPH, "Check")
        val trailingIcon = Icon(GLYPH, "Video")
        val tertiaryIcon = Icon(GLYPH)
        val trailingChip = Badge("New")
        val sensitiveCompact =
            ListItem.Compact(
                title =
                    TemplateText(
                        "Salary review",
                        sensitiveReplacement = "Busy",
                        isStruckThrough = true,
                    ),
                subtitle = TemplateText("10:00", isStruckThrough = true),
                tertiaryText = TemplateText("Room 204", sensitiveReplacement = "Office"),
                tertiaryIcon = tertiaryIcon,
                leadingIcon = leadingIcon,
                trailingChip = trailingChip,
                action = ACTION,
                style = ListItemStyle.Focused,
            )
        val plainCompact =
            ListItem.Compact(
                title = TemplateText("Team Lunch", isStruckThrough = true),
                trailingIcon = trailingIcon,
                action = ACTION,
            )
        val sensitiveCheckbox =
            ListItem.Checkbox(
                title = TemplateText("Buy birthday gift", sensitiveReplacement = "Task"),
                isChecked = true,
                action = ACTION,
            )
        val sensitiveCard =
            ListItem.DetailCard(
                title = TemplateText("Passcodes", sensitiveReplacement = "Note"),
                body = TemplateText("1234", sensitiveReplacement = "Hidden"),
                action = ACTION,
                style = ListItemStyle.Neutral,
            )
        val plainChecklistEntry =
            ListItem.DetailCard.ChecklistEntry(
                TemplateText("Flour", isStruckThrough = true),
                isChecked = true,
            )
        val sensitiveChecklistCard =
            ListItem.DetailCard(
                title = TemplateText("Bread"),
                checklistItems =
                    listOf(
                        plainChecklistEntry,
                        ListItem.DetailCard.ChecklistEntry(
                            TemplateText("Secret spice", sensitiveReplacement = "Ingredient"),
                            isChecked = true,
                        ),
                    ),
            )
        val plainDivider = ListItem.Divider(title = TemplateText("TOMORROW"))
        val sensitiveDivider =
            ListItem.Divider(title = TemplateText("Q4 REVIEW", sensitiveReplacement = "UPCOMING"))
        val list =
            ItemList(
                items =
                    listOf(
                        sensitiveCompact,
                        plainCompact,
                        sensitiveCheckbox,
                        sensitiveCard,
                        sensitiveChecklistCard,
                        plainDivider,
                        sensitiveDivider,
                    ),
                isScrollable = false,
                showTertiaryText = true,
                layoutMode = ItemListLayoutMode.ExpandFeatured,
            )
        val content =
            MatrixCardContent(
                primary = TemplateText("Today"),
                trailing =
                    IconButton(
                        GLYPH,
                        "Refresh",
                        ACTION,
                        sensitiveReplacement = "Action",
                        contained = false,
                    ),
                liveResize = LiveResizeSpec(collapseHeaderInCompactHeight = true),
                hero = list,
            )

        // Unlocked surfaces return the exact same ItemList instance without copying.
        val unlocked = resolve(content, at(176, 88))
        assertThat(unlocked.hero).isSameInstanceAs(list)

        val resolved = resolve(content, at(176, 88, AppWidgetSurface.MOBILE_LOCK_SCREEN))
        assertThat(resolved.liveResize?.collapseHeaderInCompactHeight).isTrue()
        val lockedTrailing = resolved.trailing as IconButton
        assertThat(lockedTrailing.contentDescription).isEqualTo("Action")
        assertThat(lockedTrailing.contained).isFalse()
        val lockedList = resolved.hero as ItemList
        assertThat(lockedList.isScrollable).isFalse()
        assertThat(lockedList.showTertiaryText).isTrue()
        assertThat(lockedList.layoutMode).isEqualTo(ItemListLayoutMode.ExpandFeatured)

        val lockedFirst = lockedList.items[0] as ListItem.Compact
        assertThat(lockedFirst.title.value).isEqualTo("Busy")
        // Redacted rows drop strikethrough across all fields (including unredacted siblings) so
        // declined/completed state is not leaked.
        assertThat(lockedFirst.title.isStruckThrough).isFalse()
        assertThat(lockedFirst.subtitle?.value).isEqualTo("10:00")
        assertThat(lockedFirst.subtitle?.isStruckThrough).isFalse()
        assertThat(lockedFirst.tertiaryText?.value).isEqualTo("Office")
        assertThat(lockedFirst.tertiaryIcon).isEqualTo(tertiaryIcon)
        assertThat(lockedFirst.leadingIcon).isEqualTo(leadingIcon)
        assertThat(lockedFirst.trailingChip).isEqualTo(trailingChip)
        assertThat(lockedFirst.action).isEqualTo(ACTION)
        assertThat(lockedFirst.style).isEqualTo(ListItemStyle.Focused)
        val lockedCheckbox = lockedList.items[2] as ListItem.Checkbox
        assertThat(lockedCheckbox.title.value).isEqualTo("Task")
        // Redacted checkable items reset isChecked so completion state is not leaked.
        assertThat(lockedCheckbox.isChecked).isFalse()
        assertThat(lockedCheckbox.action).isEqualTo(ACTION)

        val lockedCard = lockedList.items[3] as ListItem.DetailCard
        assertThat(lockedCard.title.value).isEqualTo("Note")
        assertThat(lockedCard.body?.value).isEqualTo("Hidden")
        assertThat(lockedCard.style).isEqualTo(ListItemStyle.Neutral)

        val lockedChecklistCard = lockedList.items[4] as ListItem.DetailCard
        assertThat(lockedChecklistCard.title.value).isEqualTo("Bread")
        assertThat(lockedChecklistCard.checklistItems[0]).isSameInstanceAs(plainChecklistEntry)
        assertThat(lockedChecklistCard.checklistItems[0].isChecked).isTrue()
        assertThat(lockedChecklistCard.checklistItems[1].text.value).isEqualTo("Ingredient")
        assertThat(lockedChecklistCard.checklistItems[1].isChecked).isFalse()

        val lockedSensitiveDivider = lockedList.items[6] as ListItem.Divider
        assertThat(lockedSensitiveDivider.title.value).isEqualTo("UPCOMING")

        // Non-sensitive items (including strikethrough) and dividers are reused as-is.
        assertThat(lockedList.items[1]).isSameInstanceAs(plainCompact)
        assertThat(lockedList.items[1].title.isStruckThrough).isTrue()
        assertThat(lockedList.items[5]).isSameInstanceAs(plainDivider)

        // An ItemList with no sensitive fields returns itself on the lock screen too.
        val nonSensitiveList = ItemList(listOf(plainCompact, plainDivider))
        val lockedPlain =
            resolve(
                MatrixCardContent(hero = nonSensitiveList),
                at(176, 88, AppWidgetSurface.MOBILE_LOCK_SCREEN),
            )
        assertThat(lockedPlain.hero).isSameInstanceAs(nonSensitiveList)
    }

    @Test
    fun card_compactHeaderValidationPaddingBudgetAndUncontainedTrailingSizing() {
        assertThrows(IllegalArgumentException::class.java) {
            MatrixCardContent(headerHorizontalPadding = (-1).dp)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MatrixCardContent(leadingSize = 40.dp, compactHeader = false)
        }

        // headerHorizontalPadding deducts from both primary and beside secondary width budgets,
        // and is zeroed on stacked W1 headers.
        val unpadded =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("123456"),
                    secondary = TemplateText("Steps"),
                ),
                at(220, 176),
            )
        val padded =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("123456"),
                    secondary = TemplateText("Steps"),
                    headerHorizontalPadding = 16.dp,
                ),
                at(220, 176),
            )
        assertThat(padded.headerHorizontalPadding).isEqualTo(16.dp)
        assertThat(padded.secondaryMaxWidth!!.value).isLessThan(unpadded.secondaryMaxWidth!!.value)

        val unpaddedPrimary =
            resolve(
                MatrixCardContent(
                    leading = Icon(GLYPH),
                    primary = TemplateText("1,234,567"),
                    leadingSize = 40.dp,
                    primaryTextSize = 16.sp,
                ),
                at(176, 88),
            )
        val paddedPrimary =
            resolve(
                MatrixCardContent(
                    leading = Icon(GLYPH),
                    primary = TemplateText("1,234,567"),
                    leadingSize = 40.dp,
                    primaryTextSize = 16.sp,
                    headerHorizontalPadding = 24.dp,
                ),
                at(176, 88),
            )
        assertThat((unpaddedPrimary.primary as TemplateText).value).isEqualTo("1,234,567")
        assertThat((paddedPrimary.primary as TemplateText).value).isNotEqualTo("1,234,567")
        val stackedPadded =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("Title"),
                    headerHorizontalPadding = 16.dp,
                ),
                at(88, 176),
            )
        assertThat(stackedPadded.stacked).isTrue()
        assertThat(stackedPadded.headerHorizontalPadding).isEqualTo(0.dp)

        // Uncontained trailing buttons budget narrower width (40dp vs 64dp) and shrink to 24dp at
        // H1 only on compact headers (standard H1 headers already enforce 56dp minHeight and keep
        // the full 40dp touch target). Contained trailing buttons keep 56dp even alongside a small
        // leading icon.
        val containedCard =
            MatrixCardContent(
                leading = Icon(GLYPH),
                primary = TemplateText("Agenda"),
                secondary = TemplateText("Sub"),
                trailing = IconButton(GLYPH, "Add", ACTION, contained = true),
                hero = ItemList(listOf(ListItem.Compact(TemplateText("Event")))),
                leadingSize = 20.dp,
                compactHeader = true,
            )
        val uncontainedCard =
            MatrixCardContent(
                leading = Icon(GLYPH),
                primary = TemplateText("Morning Brief"),
                secondary = TemplateText("Sub"),
                trailing = IconButton(GLYPH, "Refresh", ACTION, contained = false),
                hero = ItemList(listOf(ListItem.Compact(TemplateText("Event")))),
                leadingSize = 20.dp,
                compactHeader = true,
            )
        val standardUncontainedCard =
            MatrixCardContent(
                primary = TemplateText("Morning Brief"),
                trailing = IconButton(GLYPH, "Refresh", ACTION, contained = false),
                compactHeader = false,
            )
        val resolvedContainedH2 = resolve(containedCard, at(220, 132))
        val resolvedUncontainedH1 = resolve(uncontainedCard, at(176, 88))
        val resolvedUncontainedH2 = resolve(uncontainedCard, at(220, 132))
        val resolvedStandardUncontainedH1 = resolve(standardUncontainedCard, at(176, 88))

        assertThat(resolvedContainedH2.compactHeader).isTrue()
        assertThat(resolvedContainedH2.leadingSize).isEqualTo(20.dp)
        assertThat(resolvedContainedH2.trailingSize).isEqualTo(MatrixDefaults.LeadingSize)
        assertThat(resolvedUncontainedH1.trailingSize).isEqualTo(24.dp)
        assertThat(resolvedUncontainedH2.trailingSize).isEqualTo(40.dp)
        assertThat(resolvedStandardUncontainedH1.trailingSize).isEqualTo(40.dp)
        assertThat(resolvedUncontainedH2.secondaryMaxWidth!!.value)
            .isGreaterThan(resolvedContainedH2.secondaryMaxWidth!!.value)
        assertThat(resolvedUncontainedH2.heroHeight!!.value)
            .isGreaterThan(resolvedContainedH2.heroHeight!!.value)
    }

    @Test
    fun card_quickActionsValidationRedactionAndWidthBudget() {
        val primary = IconButton(GLYPH, "New note", ACTION, sensitiveReplacement = "Add")
        val secondary = IconButton(GLYPH, "Voice note", ACTION, contained = false)

        assertThrows(IllegalArgumentException::class.java) {
            CardQuickActions.AdaptiveRail(emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            MatrixCardContent(
                actions = listOf(TemplateText("Goal")),
                quickActions = CardQuickActions.Floating(primary),
            )
        }

        val railCard =
            MatrixCardContent(
                primary = TemplateText("Notes"),
                secondary = TemplateText("Updated"),
                quickActions = CardQuickActions.AdaptiveRail(listOf(primary, secondary)),
                padding = 10.dp,
            )
        val unlockedRail = resolve(railCard, at(264, 229))
        assertThat(unlockedRail.contentHeight).isEqualTo(209.dp)
        assertThat(unlockedRail.quickActions).isSameInstanceAs(railCard.quickActions)

        val noRail =
            resolve(
                MatrixCardContent(
                    primary = TemplateText("Notes"),
                    secondary = TemplateText("Updated"),
                    padding = 10.dp,
                ),
                at(264, 229),
            )
        assertThat(unlockedRail.secondaryMaxWidth!!.value)
            .isLessThan(noRail.secondaryMaxWidth!!.value)

        val lockedRail =
            resolve(railCard, at(264, 229, AppWidgetSurface.MOBILE_LOCK_SCREEN)).quickActions
                as CardQuickActions.AdaptiveRail
        assertThat(lockedRail.actions[0].contentDescription).isEqualTo("Add")
        assertThat(lockedRail.actions[1]).isSameInstanceAs(secondary)

        val floatingCard = MatrixCardContent(quickActions = CardQuickActions.Floating(primary))
        val lockedFloating =
            resolve(floatingCard, at(176, 132, AppWidgetSurface.MOBILE_LOCK_SCREEN)).quickActions
                as CardQuickActions.Floating
        assertThat(lockedFloating.action.contentDescription).isEqualTo("Add")

        // Non-sensitive quickActions return the exact same instance on the lock screen, and
        // unsized containers leave contentHeight null.
        val plainFloating = CardQuickActions.Floating(secondary)
        val plainRail = CardQuickActions.AdaptiveRail(listOf(secondary))
        assertThat(
                resolve(
                        MatrixCardContent(quickActions = plainFloating),
                        at(0, 0, AppWidgetSurface.MOBILE_LOCK_SCREEN),
                    )
                    .also { assertThat(it.contentHeight).isNull() }
                    .quickActions
            )
            .isSameInstanceAs(plainFloating)
        assertThat(
                resolve(
                        MatrixCardContent(quickActions = plainRail),
                        at(264, 229, AppWidgetSurface.MOBILE_LOCK_SCREEN),
                    )
                    .quickActions
            )
            .isSameInstanceAs(plainRail)
    }

    @Test
    fun liveResizeSpec_rejectsNegativePaddingOrSpacingAndNonPositiveWidthThreshold() {
        assertThrows(IllegalArgumentException::class.java) {
            LiveResizeSpec(compactHeightVerticalPadding = (-1).dp)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LiveResizeSpec(compactHeightSlotSpacing = (-1).dp)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LiveResizeSpec(collapseHeaderTrailingBelowWidthDp = 0.dp)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LiveResizeSpec(collapseHeaderTrailingBelowWidthDp = (-10).dp)
        }
        val zeroSpec =
            LiveResizeSpec(
                compactHeightVerticalPadding = 0.dp,
                compactHeightSlotSpacing = 0.dp,
                collapseHeaderTrailingBelowWidthDp = 160.dp,
            )
        assertThat(zeroSpec.compactHeightVerticalPadding).isEqualTo(0.dp)
        assertThat(zeroSpec.compactHeightSlotSpacing).isEqualTo(0.dp)
    }

    @Test
    fun card_stackHeaderInNarrowWidthFalseKeepsHorizontalHeaderAndCompactUncontainedTrailing() {
        val card =
            MatrixCardContent(
                primary = TemplateText("Brief"),
                trailing = IconButton(GLYPH, "Refresh", ACTION, contained = false),
                compactHeader = true,
                stackHeaderInNarrowWidth = false,
            )
        val resolved = resolve(card, at(88, 88))
        assertThat(resolved.stacked).isFalse()
        assertThat(resolved.trailing).isNotNull()
        assertThat(MatrixDefaults.uncontainedTrailingWidth(resolved.tiers)).isEqualTo(24.dp)
        assertThat(MatrixDefaults.uncontainedTrailingGlyphSize(resolved.tiers)).isEqualTo(16.dp)
        assertThat(MatrixDefaults.uncontainedTrailingWidth(SizeTiers(WidthTier.W2, HeightTier.H2)))
            .isEqualTo(40.dp)
        assertThat(
                MatrixDefaults.uncontainedTrailingGlyphSize(SizeTiers(WidthTier.W2, HeightTier.H2))
            )
            .isEqualTo(24.dp)
    }

    @Test
    fun actionRailDefaults_maxFittingActionsPreventsCrushedIcons() {
        assertThat(ActionRailDefaults.maxFittingActions(null, HeightTier.H3)).isEqualTo(4)
        assertThat(ActionRailDefaults.maxFittingActions(null, HeightTier.H4)).isEqualTo(6)
        assertThat(ActionRailDefaults.maxFittingActions(209f, HeightTier.H4)).isEqualTo(6)
        assertThat(ActionRailDefaults.maxFittingActions(195f, HeightTier.H4)).isEqualTo(5)
        assertThat(ActionRailDefaults.maxFittingActions(134f, HeightTier.H3)).isEqualTo(4)
        assertThat(ActionRailDefaults.maxFittingActions(110f, HeightTier.H3)).isEqualTo(3)
        assertThat(ActionRailDefaults.maxFittingActions(20f, HeightTier.H3)).isEqualTo(1)
    }

    private fun at(
        widthDp: Int,
        heightDp: Int,
        surface: AppWidgetSurface = AppWidgetSurface.MOBILE_HOME_SCREEN,
    ) = surface.withDimensions(Dimensions(widthDp, heightDp))

    private companion object {
        const val GLYPH = 1
        val ACTION = GlanceAdaptiveAction.LaunchAction<Activity>()
    }
}
