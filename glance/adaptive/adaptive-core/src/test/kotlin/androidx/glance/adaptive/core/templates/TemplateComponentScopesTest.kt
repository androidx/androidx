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
}
