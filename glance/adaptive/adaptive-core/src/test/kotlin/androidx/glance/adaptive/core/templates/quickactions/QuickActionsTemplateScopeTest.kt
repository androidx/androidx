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

package androidx.glance.adaptive.core.templates.quickactions

import android.app.Activity
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.action.GlanceAdaptiveActionCallback
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.TemplateIcon
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class QuickActionsTemplateScopeTest {

    @Test
    fun quickActionsTemplate_fullDsl_populatesAllSlotsAndComponents() {
        val template = quickActionsTemplate {
            leading { icon(APP_ICON) }
            primary { text("Start a workout") }
            secondary { text("Fitbit") }
            trailing { iconButton(ADD_ICON, "Log activity") { onClick(LOG) } }
            actions {
                action(RUN_ICON, "Run") { onClick(RUN) }
                action(RUN_ICON, "Mom") {
                    sensitive("Contact")
                    onClick(RUN)
                }
                action(RUN_ICON, "Lights")
            }
        }

        assertThat(template).isInstanceOf(GlanceAdaptiveTemplate::class.java)
        assertThat(template.templateId).isEqualTo(QuickActionsTemplate.TEMPLATE_ID)
        assertThat(QuickActionsTemplate.TEMPLATE_ID).isEqualTo(2)

        assertThat(template.hasHeader).isTrue()
        assertThat(template.leading).isEqualTo(Icon(icon = 100))
        assertThat(template.primary).isEqualTo(TemplateText("Start a workout"))
        assertThat(template.secondary).isEqualTo(TemplateText("Fitbit"))
        assertThat(template.trailing)
            .isEqualTo(IconButton(icon = 101, contentDescription = "Log activity", action = LOG))
        assertThat(template.actions)
            .containsExactly(
                IconTile(icon = 102, label = "Run", action = RUN),
                IconTile(icon = 102, label = "Mom", action = RUN, sensitiveReplacement = "Contact"),
                IconTile(icon = 102, label = "Lights"),
            )
            .inOrder()
    }

    @Test
    fun quickActionsTemplate_actionsOnly_hasNoHeader() {
        val template = quickActionsTemplate { actions { action(RUN_ICON, "Run") { onClick(RUN) } } }

        assertThat(template.hasHeader).isFalse()
        assertThat(template.leading).isNull()
        assertThat(template.primary).isNull()
        assertThat(template.secondary).isNull()
        assertThat(template.trailing).isNull()
        assertThat(template.actions).hasSize(1)
    }

    @Test
    fun quickActionsTemplate_withoutActions_reportsTheMissingActions() {
        val error = assertFailsWith<IllegalArgumentException> { quickActionsTemplate {} }

        assertThat(error.message).contains("QuickActionsTemplate needs")
        assertThat(error.message).contains("1 to 8 actions")
        assertThat(error.message).doesNotContain("header")
    }

    @Test
    fun quickActionsTemplate_moreThanEightActions_throwsIllegalArgumentException() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                quickActionsTemplate { actions { repeat(9) { action(RUN_ICON, "Workout $it") } } }
            }

        assertThat(error.message).contains("1 to 8 actions")
    }

    @Test
    fun quickActionsTemplate_eightActions_succeeds() {
        val template = quickActionsTemplate {
            actions { repeat(QuickActionsTemplate.MAX_ACTIONS) { action(RUN_ICON, "Workout $it") } }
        }

        assertThat(template.actions).hasSize(8)
    }

    @Test
    fun header_isAllOrNothing() {
        // Any header slot needs both the icon and the title.
        val subtitleOnly =
            assertFailsWith<IllegalArgumentException> {
                quickActionsTemplate {
                    secondary { text("Fitbit") }
                    actions { action(RUN_ICON, "Run") }
                }
            }
        assertThat(subtitleOnly.message).contains("a header icon")
        assertThat(subtitleOnly.message).contains("a header title")

        val trailingOnly =
            assertFailsWith<IllegalArgumentException> {
                quickActionsTemplate {
                    trailing { iconButton(ADD_ICON, "Log activity") }
                    actions { action(RUN_ICON, "Run") }
                }
            }
        assertThat(trailingOnly.message).contains("a header icon")

        val titleOnly =
            assertFailsWith<IllegalArgumentException> {
                quickActionsTemplate {
                    primary { text("Quick notes") }
                    actions { action(RUN_ICON, "New note") }
                }
            }
        assertThat(titleOnly.message).contains("a header icon")
        assertThat(titleOnly.message).doesNotContain("a header title")

        val iconOnly =
            assertFailsWith<IllegalArgumentException> {
                quickActionsTemplate {
                    leading { icon(APP_ICON) }
                    actions { action(RUN_ICON, "New note") }
                }
            }
        assertThat(iconOnly.message).contains("a header title")
        assertThat(iconOnly.message).doesNotContain("a header icon")

        val minimal = quickActionsTemplate {
            leading { icon(APP_ICON) }
            primary { text("Quick notes") }
            actions { action(RUN_ICON, "New note") }
        }
        assertThat(minimal.hasHeader).isTrue()
        assertThat(minimal.secondary).isNull()
        assertThat(minimal.trailing).isNull()
    }

    @Test
    fun action_subtitle_isCarriedWithItsOwnSensitiveReplacement() {
        val template = quickActionsTemplate {
            actions {
                action(RUN_ICON, "Garage Door") {
                    subtitle("Open")
                    onClick(RUN)
                }
                action(RUN_ICON, "Mom") {
                    sensitive("Contact")
                    subtitle("Calling") { sensitive("Call") }
                }
                action(RUN_ICON, "Lights")
            }
        }

        assertThat(template.actions.map { it.subtitle })
            .containsExactly(
                TemplateText("Open"),
                TemplateText("Calling", sensitiveReplacement = "Call"),
                null,
            )
            .inOrder()
        assertThat(template.actions[0].action).isEqualTo(RUN)
        assertThat(template.actions[1].sensitiveReplacement).isEqualTo("Contact")
    }

    @Test
    fun action_subtitle_rejectsBlankOrDuplicate() {
        assertFailsWith<IllegalArgumentException> {
            quickActionsTemplate { actions { action(RUN_ICON, "Garage Door") { subtitle(" ") } } }
        }
        assertFailsWith<IllegalStateException> {
            quickActionsTemplate {
                actions {
                    action(RUN_ICON, "Garage Door") {
                        subtitle("Open")
                        subtitle("Closed")
                    }
                }
            }
        }
    }

    @Test
    fun labels_areRequired() {
        assertFailsWith<IllegalArgumentException> {
            quickActionsTemplate { actions { action(RUN_ICON, " ") } }
        }
        assertFailsWith<IllegalArgumentException> {
            quickActionsTemplate {
                leading { icon(APP_ICON) }
                primary { text("Start a workout") }
                trailing { iconButton(ADD_ICON, "") }
                actions { action(RUN_ICON, "Run") }
            }
        }
    }

    @Test
    fun slotBlocks_emptyBlocks_throwExpectedExceptions() {
        assertFailsWith<IllegalStateException> { quickActionsTemplate { leading {} } }
        assertFailsWith<IllegalStateException> { quickActionsTemplate { primary {} } }
        assertFailsWith<IllegalStateException> { quickActionsTemplate { secondary {} } }
        assertFailsWith<IllegalStateException> { quickActionsTemplate { trailing {} } }
        assertFailsWith<IllegalArgumentException> { quickActionsTemplate { actions {} } }
    }

    @Test
    fun slotBlocks_duplicateSlotOrComponentCalls_throwIllegalStateException() {
        assertFailsWith<IllegalStateException> {
            quickActionsTemplate {
                actions { action(RUN_ICON, "Run") }
                actions { action(RUN_ICON, "Walk") }
            }
        }
        assertFailsWith<IllegalStateException> {
            quickActionsTemplate {
                leading {
                    icon(APP_ICON)
                    icon(APP_ICON)
                }
            }
        }
        assertFailsWith<IllegalStateException> {
            quickActionsTemplate {
                trailing {
                    iconButton(ADD_ICON, "Log activity")
                    iconButton(ADD_ICON, "Log activity")
                }
            }
        }
        assertFailsWith<IllegalStateException> {
            quickActionsTemplate {
                actions {
                    action(RUN_ICON, "Run") {
                        onClick(RUN)
                        onClick(LOG)
                    }
                }
            }
        }
    }

    @Test
    fun equalsAndHashCode_compareAllSlots() {
        fun sample(title: String) = quickActionsTemplate {
            leading { icon(APP_ICON) }
            primary { text(title) }
            actions { action(RUN_ICON, "Run") { onClick(RUN) } }
        }

        assertThat(sample("Start a workout")).isEqualTo(sample("Start a workout"))
        assertThat(sample("Start a workout").hashCode())
            .isEqualTo(sample("Start a workout").hashCode())
        assertThat(sample("Start a workout")).isNotEqualTo(sample("Workouts"))
        assertThat(sample("Start a workout").toString()).contains("Start a workout")
    }

    private companion object {
        val APP_ICON = TemplateIcon.fromResource(100)
        val ADD_ICON = TemplateIcon.fromResource(101)
        val RUN_ICON = TemplateIcon.fromResource(102)
        val RUN = GlanceAdaptiveAction.LaunchAction<Activity>()
        val LOG = GlanceAdaptiveAction.CallbackAction<GlanceAdaptiveActionCallback>()
    }
}
