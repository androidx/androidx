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

import androidx.annotation.RestrictTo

/** Configures optional settings on a template text component. */
@GlanceAdaptiveTemplateDsl
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TextScope internal constructor() {
    internal var sensitiveReplacement: String? = null
        private set

    /**
     * Replaces the text value with [replacement] on locked or privacy-restricted surfaces.
     *
     * ```kotlin
     * primary {
     *     text("11,056") { sensitive("--") }
     * }
     * ```
     *
     * @param replacement non-blank placeholder string shown when content is redacted
     * @throws IllegalArgumentException if [replacement] is blank
     * @throws IllegalStateException if [sensitive] was already called in this scope
     */
    public fun sensitive(replacement: String) {
        check(sensitiveReplacement == null) { "Sensitive replacement was already set." }
        require(replacement.isNotBlank()) { "Sensitive replacement must not be blank." }
        sensitiveReplacement = replacement
    }
}

/** Configures optional leading and trailing summary captions beneath a template bar chart. */
@GlanceAdaptiveTemplateDsl
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BarChartScope internal constructor() {
    internal var startLabelText: String? = null
        private set

    internal var startLabelIcon: TemplateIcon? = null
        private set

    internal var endLabelText: String? = null
        private set

    internal var endLabelIcon: TemplateIcon? = null
        private set

    /**
     * Sets the leading caption in the summary row beneath the bar chart.
     *
     * @param text non-blank caption text (for example, `"7 Day Avg"`)
     * @param icon optional [TemplateIcon] drawn immediately before [text]
     * @throws IllegalArgumentException if [text] is blank
     * @throws IllegalStateException if [startLabel] was already called in this scope
     */
    public fun startLabel(text: String, icon: TemplateIcon? = null) {
        check(startLabelText == null) { "Chart startLabel was already set." }
        require(text.isNotBlank()) { "Chart label must not be blank." }
        startLabelText = text
        startLabelIcon = icon
    }

    /**
     * Sets the trailing caption in the summary row beneath the bar chart.
     *
     * ```kotlin
     * barChart(bars = weeklySteps) {
     *     startLabel("7 Day Avg")
     *     endLabel("13,843", icon = TemplateIcon.fromResource(R.drawable.ic_arrow_up))
     * }
     * ```
     *
     * @param text non-blank caption text (for example, `"13,843"`)
     * @param icon optional [TemplateIcon] drawn immediately before [text]
     * @throws IllegalArgumentException if [text] is blank
     * @throws IllegalStateException if [endLabel] was already called in this scope
     */
    public fun endLabel(text: String, icon: TemplateIcon? = null) {
        check(endLabelText == null) { "Chart endLabel was already set." }
        require(text.isNotBlank()) { "Chart label must not be blank." }
        endLabelText = text
        endLabelIcon = icon
    }
}
