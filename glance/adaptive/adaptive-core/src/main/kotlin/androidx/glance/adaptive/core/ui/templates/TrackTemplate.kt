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

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo

/**
 * Standard adaptive template representing tracking metrics, fitness stats, and progress.
 *
 * The payload is surface-agnostic: host modules (e.g. `adaptive-appwidget`) map it to a concrete
 * layout through their own selectors and renderers.
 *
 * @param title Primary title or metric headline (e.g. workout type or activity name).
 * @param subtitle Optional secondary label or status text.
 * @param progress Optional normalized progress value in the range `[0.0f, 1.0f]`.
 * @param statusText Optional metric summary text (e.g. "340 kcal", "4.2 km").
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TrackTemplate(
    public val title: String,
    public val subtitle: String? = null,
    @param:FloatRange(from = 0.0, to = 1.0)
    @get:FloatRange(from = 0.0, to = 1.0)
    public val progress: Float? = null,
    public val statusText: String? = null,
) : AdaptiveGlanceTemplate {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackTemplate) return false
        return title == other.title &&
            subtitle == other.subtitle &&
            progress == other.progress &&
            statusText == other.statusText
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        result = 31 * result + (progress?.hashCode() ?: 0)
        result = 31 * result + (statusText?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "TrackTemplate(title=$title, subtitle=$subtitle, progress=$progress, " +
            "statusText=$statusText)"
}
