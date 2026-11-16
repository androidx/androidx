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

import android.app.Activity
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TemplateComponentScopesTest {

    @Test
    fun textScope_sensitive_recordsReplacementAndRejectsBlankOrDuplicate() {
        val scope = TextScope().apply { sensitive("--") }
        assertThat(scope.sensitiveReplacement).isEqualTo("--")

        assertThrows(IllegalStateException::class.java) { scope.sensitive("***") }
        assertThrows(IllegalArgumentException::class.java) { TextScope().sensitive("   ") }
    }

    @Test
    fun barChartScope_labels_recordTextAndIconAndRejectBlankOrDuplicate() {
        val icon = TemplateIcon.fromResource(42)
        val scope =
            BarChartScope().apply {
                startLabel("7 Day Avg")
                endLabel("13,843", icon = icon)
            }

        assertThat(scope.startLabelText).isEqualTo("7 Day Avg")
        assertThat(scope.startLabelIcon).isNull()
        assertThat(scope.endLabelText).isEqualTo("13,843")
        assertThat(scope.endLabelIcon).isEqualTo(icon)

        assertThrows(IllegalStateException::class.java) { scope.startLabel("Other") }
        assertThrows(IllegalStateException::class.java) { scope.endLabel("Other") }
        assertThrows(IllegalArgumentException::class.java) { BarChartScope().startLabel(" ") }
        assertThrows(IllegalArgumentException::class.java) { BarChartScope().endLabel("") }
    }

    @Test
    fun actionScope_recordsClickReplacementAndSubtitle() {
        val action = GlanceAdaptiveAction.LaunchAction<Activity>()
        val scope =
            ActionScope().apply {
                onClick(action)
                sensitive("Door")
                subtitle("Open") { sensitive("--") }
            }

        assertThat(scope.clickable.action).isEqualTo(action)
        assertThat(scope.clickable.sensitiveReplacement).isEqualTo("Door")
        assertThat(scope.subtitle?.value).isEqualTo("Open")
        assertThat(scope.subtitle?.sensitiveReplacement).isEqualTo("--")
        assertThat(ActionScope().subtitle).isNull()

        assertThrows(IllegalStateException::class.java) { scope.subtitle("Closed") }
        assertThrows(IllegalArgumentException::class.java) { ActionScope().subtitle("") }
    }

    @Test
    fun clickableScope_recordsActionAndReplacementAndRejectsBlankOrDuplicate() {
        val action = GlanceAdaptiveAction.LaunchAction<Activity>()
        val scope =
            ClickableScope().apply {
                onClick(action)
                sensitive("Contact")
            }

        assertThat(scope.action).isEqualTo(action)
        assertThat(scope.sensitiveReplacement).isEqualTo("Contact")
        assertThat(ClickableScope().action).isNull()

        assertThrows(IllegalStateException::class.java) { scope.onClick(action) }
        assertThrows(IllegalStateException::class.java) { scope.sensitive("Other") }
        assertThrows(IllegalArgumentException::class.java) { ClickableScope().sensitive(" ") }
    }
}
