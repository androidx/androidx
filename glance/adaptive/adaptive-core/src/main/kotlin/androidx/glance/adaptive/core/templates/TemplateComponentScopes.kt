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
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.slots.TemplateText

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

/**
 * Configures what a tappable template component, such as an icon button, does when tapped, and how
 * it is named on locked surfaces.
 */
@GlanceAdaptiveTemplateDsl
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClickableScope internal constructor() {
    internal var action: GlanceAdaptiveAction? = null
        private set

    internal var sensitiveReplacement: String? = null
        private set

    /**
     * Runs [action] when the component is tapped. A component without it is shown but cannot be
     * tapped.
     *
     * ```kotlin
     * trailing {
     *     iconButton(TemplateIcon.fromResource(R.drawable.ic_add), "Log activity") {
     *         onClick(logActivity)
     *     }
     * }
     * ```
     *
     * @param action what the tap does
     * @throws IllegalStateException if [onClick] was already called in this scope
     */
    public fun onClick(action: GlanceAdaptiveAction) {
        check(this.action == null) { "onClick was already set." }
        this.action = action
    }

    /**
     * Replaces the component's label with [replacement] on locked or privacy-restricted surfaces,
     * both where it is drawn and where accessibility services announce it.
     *
     * @param replacement non-blank label shown and announced when content is redacted
     * @throws IllegalArgumentException if [replacement] is blank
     * @throws IllegalStateException if [sensitive] was already called in this scope
     */
    public fun sensitive(replacement: String) {
        check(sensitiveReplacement == null) { "Sensitive replacement was already set." }
        require(replacement.isNotBlank()) { "Sensitive replacement must not be blank." }
        sensitiveReplacement = replacement
    }
}

/**
 * Configures a quick action: what it does when tapped, how it is named on locked surfaces, and an
 * optional second line beneath its label.
 */
@GlanceAdaptiveTemplateDsl
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ActionScope internal constructor() {
    internal val clickable: ClickableScope = ClickableScope()

    internal var subtitle: TemplateText? = null
        private set

    /**
     * Runs [action] when the action is tapped. An action without it is shown but cannot be tapped.
     *
     * @param action what the tap does
     * @throws IllegalStateException if [onClick] was already called in this scope
     */
    public fun onClick(action: GlanceAdaptiveAction) {
        clickable.onClick(action)
    }

    /**
     * Replaces the action's label with [replacement] on locked or privacy-restricted surfaces, both
     * where it is drawn and where accessibility services announce it.
     *
     * ```kotlin
     * action(TemplateIcon.fromResource(R.drawable.ic_person), "Mom") {
     *     sensitive("Contact")
     *     onClick(callMom)
     * }
     * ```
     *
     * @param replacement non-blank label shown and announced when content is redacted
     * @throws IllegalArgumentException if [replacement] is blank
     * @throws IllegalStateException if [sensitive] was already called in this scope
     */
    public fun sensitive(replacement: String) {
        clickable.sensitive(replacement)
    }

    /**
     * Adds a second line beneath the action's label, such as its state (`"Open"`, `"On · 80%"`). It
     * is drawn more faintly than the label where the tile has room for it, and announced after the
     * label.
     *
     * Like any template text, it can be replaced on locked surfaces:
     * ```kotlin
     * action(TemplateIcon.fromResource(R.drawable.ic_person), "Mom") {
     *     subtitle("Calling") { sensitive("Call") }
     * }
     * ```
     *
     * @param value non-blank subtitle
     * @param options optional block configuring [TextScope] settings such as [TextScope.sensitive]
     * @throws IllegalArgumentException if [value] is blank
     * @throws IllegalStateException if [subtitle] was already called in this scope
     */
    public fun subtitle(value: String, options: TextScope.() -> Unit = {}) {
        check(subtitle == null) { "Subtitle was already set." }
        subtitle = buildTemplateText(value, options)
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

internal fun buildTemplateText(value: String, options: TextScope.() -> Unit): TemplateText {
    // Validate before running options so blank text fails prior to nested calls.
    require(value.isNotBlank()) { "Text must not be blank." }
    val scope = TextScope().apply(options)
    return TemplateText(value = value, sensitiveReplacement = scope.sensitiveReplacement)
}
