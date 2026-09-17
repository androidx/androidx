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

package androidx.glance.adaptive.core.ui.templates

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrackTemplateTest {

    @Test
    fun constructor_optionalFields_defaultToNull() {
        val template = TrackTemplate(title = "Morning Run")

        assertThat(template.title).isEqualTo("Morning Run")
        assertThat(template.subtitle).isNull()
        assertThat(template.progress).isNull()
        assertThat(template.statusText).isNull()
    }

    @Test
    fun trackTemplate_isAdaptiveGlanceTemplate() {
        assertThat(TrackTemplate(title = "Morning Run"))
            .isInstanceOf(AdaptiveGlanceTemplate::class.java)
    }

    @Test
    fun equals_sameValues_areEqual() {
        val first = fullTemplate()
        val second = fullTemplate()

        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun equals_differingValues_areNotEqual() {
        val template = fullTemplate()

        assertThat(template).isNotEqualTo(fullTemplate(title = "Evening Run"))
        assertThat(template).isNotEqualTo(fullTemplate(subtitle = null))
        assertThat(template).isNotEqualTo(fullTemplate(progress = 0.25f))
        assertThat(template).isNotEqualTo(fullTemplate(statusText = null))
    }

    @Test
    fun toString_containsAllFields() {
        assertThat(fullTemplate().toString())
            .isEqualTo(
                "TrackTemplate(title=Morning Run, subtitle=Cardio Workout, progress=0.75, " +
                    "statusText=5.2 km)"
            )
    }

    private fun fullTemplate(
        title: String = "Morning Run",
        subtitle: String? = "Cardio Workout",
        progress: Float? = 0.75f,
        statusText: String? = "5.2 km",
    ) =
        TrackTemplate(
            title = title,
            subtitle = subtitle,
            progress = progress,
            statusText = statusText,
        )
}
