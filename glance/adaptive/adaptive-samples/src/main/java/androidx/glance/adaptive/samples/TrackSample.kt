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

package androidx.glance.adaptive.samples

import androidx.glance.adaptive.core.templates.ChartBar
import androidx.glance.adaptive.core.templates.TemplateIcon
import androidx.glance.adaptive.core.templates.track.trackTemplate

/**
 * Shared [trackTemplate] payload pushed by [TrackRemoteViewsWidgetReceiver] and
 * [TrackRemoteComposeWidgetReceiver] so the two renderers can be compared side by side.
 */
internal object TrackSample {
    private val WeeklySteps =
        listOf(
            ChartBar(0.65f, "M"),
            ChartBar(1.0f, "T", isEmphasized = true),
            ChartBar(0.80f, "W"),
            ChartBar(1.0f, "T", isEmphasized = true),
            ChartBar(0.45f, "F"),
            ChartBar(1.0f, "S", isEmphasized = true),
            ChartBar(0.55f, "S"),
        )

    val SampleData = trackTemplate {
        leading {
            progressRing(
                progress = 0.78f,
                icon = TemplateIcon.fromResource(R.drawable.ic_steps),
            )
        }

        primary { text("11,056") { sensitive("--") } }

        secondary { text("Steps") }

        hero {
            barChart(bars = WeeklySteps) {
                startLabel("7 Day Avg")
                endLabel("13,843", icon = TemplateIcon.fromResource(R.drawable.ic_arrow_up))
            }
        }

        supporting {
            text("78%") { sensitive("--") }
            text("3,118 to go") { sensitive("--") }
            badge("Z2")
        }
    }
}
