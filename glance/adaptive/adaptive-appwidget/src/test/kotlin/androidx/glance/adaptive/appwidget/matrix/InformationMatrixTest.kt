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

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier
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
    fun card_trailingTakesTheLeadingSizeOutOfTheRow() {
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
        assertThat(with.trailingSize).isEqualTo(40.dp)
        assertThat(without.trailing).isNull()
        assertThat(without.trailingSize).isEqualTo(0.dp)
        // 176 dp less 2 x 16 dp padding, a 40 dp icon and an 8 dp gap leaves 96 dp for the
        // metric, or 48 dp with a trailing button: too narrow for it at 16 sp.
        assertThat((without.primary as TemplateText).value).isEqualTo("1,234,567")
        assertThat((with.primary as TemplateText).value).isNotEqualTo("1,234,567")
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

    private fun at(
        widthDp: Int,
        heightDp: Int,
        surface: AppWidgetSurface = AppWidgetSurface.MOBILE_HOME_SCREEN,
    ) = surface.withDimensions(Dimensions(widthDp, heightDp))

    private companion object {
        const val GLYPH = 1
        val ACTION = GlanceWidgetAction.LaunchDeeplink("app://run")
    }
}
