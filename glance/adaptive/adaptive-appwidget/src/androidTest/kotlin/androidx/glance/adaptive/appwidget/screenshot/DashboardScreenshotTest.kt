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

package androidx.glance.adaptive.appwidget.screenshot

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.templates.dashboard.DashboardMatrixSelector
import androidx.glance.adaptive.appwidget.test.R
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.DashboardTemplate
import androidx.glance.adaptive.core.templates.DashboardTemplateScope
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.TemplateIcon
import androidx.glance.adaptive.core.templates.dashboardTemplate
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Screenshots of the Dashboard template at every container of the design's density matrix, for a
 * payload with a header (Smart home) and one without (Contacts), plus edge cases: a header with a
 * trailing button (Workouts), a large font scale, the dark theme, a short row, and redaction on the
 * lock screen.
 */
@LargeTest
// Goldens are drawn on the API 35 emulator, as the screenshot rule requires.
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(Parameterized::class)
class DashboardScreenshotTest(private val case: Case) {

    @get:Rule val screenshotRule = templateScreenshotRule()

    @Test
    fun matchesGolden() {
        screenshotRule.assertTemplateAgainstGolden(
            case.template,
            case.renderer,
            case.size,
            golden = case.golden,
            surface = case.surface ?: case.size.surface,
            nightMode = case.nightMode,
            fontScale = case.fontScale,
        )
    }

    /**
     * One screenshot.
     *
     * @property golden Name of the golden, which also names the case.
     */
    class Case
    internal constructor(
        internal val golden: String,
        internal val template: GlanceAdaptiveTemplate,
        internal val size: TierSize,
        internal val renderer:
            TemplateRenderer<GlanceAdaptiveTemplate, AppWidgetSurface, AppWidgetRenderOutput> =
            DashboardRenderer,
        internal val surface: AppWidgetSurface? = null,
        internal val nightMode: Boolean = false,
        internal val fontScale: Float = 1f,
    ) {
        override fun toString(): String = golden
    }

    companion object {
        private val ACTION = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://dashboard"))

        /** Draws a Dashboard as the library would: the Dashboard selector, then the matrix. */
        private val DashboardRenderer =
            TemplateRenderer { template: GlanceAdaptiveTemplate, surface: AppWidgetSurface ->
                val dashboard = template as DashboardTemplate
                RemoteViewsMatrixRenderer.render(
                    dashboard,
                    DashboardMatrixSelector.select(dashboard, surface),
                    surface,
                )
            }

        /** The design's Smart home dashboard: a header and eight devices with their states. */
        private val SmartHome = dashboardTemplate {
            leading {
                icon(
                    TemplateIcon.fromResource(R.drawable.glance_adaptive_appwidget_test_glyph_home)
                )
            }
            primary { text("Home") }
            secondary { text("3 devices on") }
            actions {
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_garage, "Garage Door", "Open")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_bolt, "Tesla", "Charging")
                tile(
                    R.drawable.glance_adaptive_appwidget_test_glyph_house,
                    "Arrive Home",
                    "Routine",
                )
                tile(
                    R.drawable.glance_adaptive_appwidget_test_glyph_lightbulb,
                    "Living Room",
                    "On · 80%",
                )
                // Shows its state only, so it has no action.
                action(
                    TemplateIcon.fromResource(R.drawable.glance_adaptive_appwidget_test_glyph_lock),
                    "Front Lock",
                ) {
                    subtitle("Locked")
                }
                tile(
                    R.drawable.glance_adaptive_appwidget_test_glyph_bedtime,
                    "Good Night",
                    "Routine",
                )
                tile(
                    R.drawable.glance_adaptive_appwidget_test_glyph_logout,
                    "Leave Home",
                    "Routine",
                )
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_videocam, "Camera", "On")
            }
        }

        /**
         * Eight workouts under a header with a trailing button, which the design draws from W3 as a
         * pill.
         */
        private val Workouts = dashboardTemplate {
            leading {
                icon(TemplateIcon.fromResource(R.drawable.glance_adaptive_appwidget_test_glyph_run))
            }
            primary { text("Start a workout") }
            secondary { text("Last: Run, 5.2 km") }
            trailing {
                iconButton(
                    TemplateIcon.fromResource(R.drawable.glance_adaptive_appwidget_test_glyph_add),
                    "Log activity",
                ) {
                    onClick(ACTION)
                }
            }
            actions {
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_run, "Run", "5.2 km last")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_walk, "Walk", "Yesterday")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_bike, "Bike", "12 km last")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_weights, "Weights", "Monday")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_swim, "Swim", "1 km last")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_yoga, "Yoga", "20 min")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_hike, "Hike", "Saturday")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_timer, "HIIT", "15 min")
            }
        }

        /** Eight favorite contacts and no header, as the design's Contacts use case. */
        private val Contacts = dashboardTemplate {
            actions {
                favoriteContacts().forEach { name ->
                    tile(R.drawable.glance_adaptive_appwidget_test_glyph_person, name)
                }
            }
        }

        private fun favoriteContacts(): List<String> =
            listOf("Mom", "Dad", "Alex", "Sam", "Jordan", "Taylor", "Grandma", "Kiddo")

        /** Two devices of the Smart home dashboard, which fill half of a row at W4. */
        private val ShortRow = dashboardTemplate {
            leading {
                icon(
                    TemplateIcon.fromResource(R.drawable.glance_adaptive_appwidget_test_glyph_home)
                )
            }
            primary { text("Home") }
            secondary { text("3 devices on") }
            actions {
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_garage, "Garage Door", "Open")
                tile(R.drawable.glance_adaptive_appwidget_test_glyph_bolt, "Tesla", "Charging")
            }
        }

        private fun DashboardTemplateScope.ActionsScope.tile(
            @DrawableRes icon: Int,
            label: String,
            subtitle: String? = null,
        ) {
            action(TemplateIcon.fromResource(icon), label) {
                if (subtitle != null) subtitle(subtitle)
                onClick(ACTION)
            }
        }

        /**
         * Draws a grid of sensitive tiles as the matrix shows them on the lock screen. The lock
         * screen is always H0, where the Dashboard selector draws only its Mini layouts, so this
         * draws the matrix directly.
         */
        private val RedactedTiles =
            TemplateRenderer<GlanceAdaptiveTemplate, AppWidgetSurface, AppWidgetRenderOutput> {
                template,
                surface ->
                val tiles =
                    listOf("Mom", "Dad", "Alex", "Sam").map { name ->
                        IconTile(
                            R.drawable.glance_adaptive_appwidget_test_glyph_person,
                            name,
                            ACTION,
                            sensitiveReplacement = "Contact",
                            subtitle = TemplateText("Calling $name", sensitiveReplacement = "Call"),
                        )
                    }
                RemoteViewsMatrixRenderer.render(
                    template,
                    MatrixCardContent(
                        hero = IconGrid(tiles, columns = 4, rows = 1),
                        padding = 12.dp,
                    ),
                    surface,
                )
            }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Case> = buildList {
            for ((name, template) in listOf("smarthome" to SmartHome, "contacts" to Contacts)) {
                for (size in TierSize.entries) {
                    add(Case("dashboard_${name}_${size.name.lowercase()}", template, size))
                }
            }
            // The trailing button: absent at W2, a pill from W3.
            for (size in
                listOf(TierSize.W2H1, TierSize.W3H1, TierSize.W3H3, TierSize.W4H1, TierSize.W4H4)) {
                add(Case("dashboard_workouts_${size.name.lowercase()}", Workouts, size))
            }
            add(
                Case(
                    "dashboard_smarthome_w4h4_fontscale2",
                    SmartHome,
                    TierSize.W4H4,
                    fontScale = 2f,
                )
            )
            add(Case("dashboard_smarthome_w4h4_dark", SmartHome, TierSize.W4H4, nightMode = true))
            add(Case("dashboard_contacts_w4h3_dark", Contacts, TierSize.W4H3, nightMode = true))
            add(Case("dashboard_shortrow_w4h3", ShortRow, TierSize.W4H3))
            add(
                Case(
                    "dashboard_matrix_lockscreen_redacted_w4h3",
                    SmartHome,
                    TierSize.W4H3,
                    renderer = RedactedTiles,
                    surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
                )
            )
        }
    }
}
