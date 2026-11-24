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

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.SecondaryComponent
import androidx.glance.adaptive.core.slots.TrailingComponent
import androidx.glance.adaptive.core.templates.ActionScope
import androidx.glance.adaptive.core.templates.ClickableScope
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplateDsl
import androidx.glance.adaptive.core.templates.TemplateIcon
import androidx.glance.adaptive.core.templates.TextScope
import androidx.glance.adaptive.core.templates.buildTemplateText

/**
 * Builds a [QuickActionsTemplate] using scoped slot blocks.
 *
 * Populates [QuickActionsTemplateScope.actions], with an optional header of
 * [QuickActionsTemplateScope.leading] and [QuickActionsTemplateScope.primary], and optionally
 * [QuickActionsTemplateScope.secondary] and [QuickActionsTemplateScope.trailing]:
 * ```kotlin
 * val template = quickActionsTemplate {
 *     leading { icon(TemplateIcon.fromResource(R.drawable.ic_fitbit)) }
 *     primary { text("Start a workout") }
 *     secondary { text("Last: Run, 5.2 km") }
 *     trailing {
 *         iconButton(TemplateIcon.fromResource(R.drawable.ic_add), "Log activity") {
 *             onClick(logActivity)
 *         }
 *     }
 *     actions {
 *         action(TemplateIcon.fromResource(R.drawable.ic_run), "Run") {
 *             subtitle("5.2 km last")
 *             onClick(startRun)
 *         }
 *         action(TemplateIcon.fromResource(R.drawable.ic_walk), "Walk") { onClick(startWalk) }
 *     }
 * }
 * ```
 *
 * @param content DSL block configuring the slots of the [QuickActionsTemplate]
 * @return validated [QuickActionsTemplate] instance
 * @throws IllegalArgumentException if a component argument is invalid,
 *   [QuickActionsTemplateScope.actions] is missing, empty, or has more than 8 actions, or a header
 *   slot is set without [QuickActionsTemplateScope.leading] and [QuickActionsTemplateScope.primary]
 * @throws IllegalStateException if a header slot block is empty or any slot block is configured
 *   more than once
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun quickActionsTemplate(
    content: QuickActionsTemplateScope.() -> Unit
): QuickActionsTemplate = QuickActionsTemplateScope().apply(content).build()

/** Configures the semantic slots of a [QuickActionsTemplate]. */
@GlanceAdaptiveTemplateDsl
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class QuickActionsTemplateScope internal constructor() {
    private var leading: LeadingComponent? = null
    private var primary: PrimaryComponent? = null
    private var secondary: SecondaryComponent? = null
    private var trailing: TrailingComponent? = null
    private var actions: List<IconTile> = emptyList()

    /**
     * Configures the header's leading icon slot. Required when the template has a header.
     *
     * @param content block adding a single component in [LeadingScope]
     * @throws IllegalStateException if [leading] was already called or [content] adds no component
     */
    public fun leading(content: LeadingScope.() -> Unit) {
        check(leading == null) { "The leading slot was already configured." }
        val scope = LeadingScope().apply(content)
        leading =
            checkNotNull(scope.component) {
                "The leading block must contain a component (for example, icon(...))."
            }
    }

    /**
     * Configures the header's title slot. Required when the template has a header.
     *
     * @param content block adding a single component in [PrimaryScope]
     * @throws IllegalStateException if [primary] was already called or [content] adds no component
     */
    public fun primary(content: PrimaryScope.() -> Unit) {
        check(primary == null) { "The primary slot was already configured." }
        val scope = PrimaryScope().apply(content)
        primary =
            checkNotNull(scope.component) {
                "The primary block must contain a component (for example, text(...))."
            }
    }

    /**
     * Configures the header's optional subtitle slot, shown beneath [primary] where there is room.
     *
     * @param content block adding a single component in [SecondaryScope]
     * @throws IllegalStateException if [secondary] was already called or [content] adds no
     *   component
     */
    public fun secondary(content: SecondaryScope.() -> Unit) {
        check(secondary == null) { "The secondary slot was already configured." }
        val scope = SecondaryScope().apply(content)
        secondary =
            checkNotNull(scope.component) {
                "The secondary block must contain a component (for example, text(...))."
            }
    }

    /**
     * Configures the header's optional trailing button, shown where the widget is wide enough.
     *
     * @param content block adding a single component in [TrailingScope]
     * @throws IllegalStateException if [trailing] was already called or [content] adds no component
     */
    public fun trailing(content: TrailingScope.() -> Unit) {
        check(trailing == null) { "The trailing slot was already configured." }
        val scope = TrailingScope().apply(content)
        trailing =
            checkNotNull(scope.component) {
                "The trailing block must contain a component (for example, iconButton(...))."
            }
    }

    /**
     * Configures the required actions, 1 to 8, ordered by priority.
     *
     * Smaller surfaces keep the earlier actions and drop the trailing ones.
     *
     * @param content block adding 1 to 8 actions in [ActionsScope]
     * @throws IllegalArgumentException if [content] adds no actions
     * @throws IllegalStateException if [actions] was already called
     */
    public fun actions(content: ActionsScope.() -> Unit) {
        check(actions.isEmpty()) { "The actions slot was already configured." }
        actions = ActionsScope().apply(content).build()
    }

    internal fun build(): QuickActionsTemplate =
        QuickActionsTemplate(
            actions = actions,
            leading = leading,
            primary = primary,
            secondary = secondary,
            trailing = trailing,
        )

    /** Populates the [QuickActionsTemplateScope.leading] slot of a [QuickActionsTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class LeadingScope internal constructor() {
        internal var component: LeadingComponent? = null

        /**
         * Places the header's icon, such as the app's badge, in the leading slot.
         *
         * @param icon [TemplateIcon] of the header's glyph
         * @throws IllegalStateException if a component was already added to this slot
         */
        public fun icon(icon: TemplateIcon) {
            check(component == null) { "The leading slot accepts only one component." }
            component = Icon(icon = icon.resId)
        }
    }

    /** Populates the [QuickActionsTemplateScope.primary] title slot of a [QuickActionsTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class PrimaryScope internal constructor() {
        internal var component: PrimaryComponent? = null

        /**
         * Places the header's title in the primary slot.
         *
         * @param value non-blank title (for example, `"Start a workout"`)
         * @param options optional block configuring [TextScope] settings such as
         *   [TextScope.sensitive]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if a component was already added to this slot
         */
        public fun text(value: String, options: TextScope.() -> Unit = {}) {
            check(component == null) { "The primary slot accepts only one component." }
            component = buildTemplateText(value, options)
        }
    }

    /**
     * Populates the [QuickActionsTemplateScope.secondary] subtitle slot of a
     * [QuickActionsTemplate].
     */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class SecondaryScope internal constructor() {
        internal var component: SecondaryComponent? = null

        /**
         * Places the header's subtitle in the secondary slot.
         *
         * @param value non-blank subtitle (for example, `"Last: Run, 5.2 km"`)
         * @param options optional block configuring [TextScope] settings such as
         *   [TextScope.sensitive]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if a component was already added to this slot
         */
        public fun text(value: String, options: TextScope.() -> Unit = {}) {
            check(component == null) { "The secondary slot accepts only one component." }
            component = buildTemplateText(value, options)
        }
    }

    /**
     * Populates the [QuickActionsTemplateScope.trailing] button slot of a [QuickActionsTemplate].
     */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class TrailingScope internal constructor() {
        internal var component: TrailingComponent? = null

        /**
         * Places an icon button at the header's end.
         *
         * @param icon [TemplateIcon] of the button's glyph
         * @param label non-blank name of what the button does, announced by accessibility services
         * @param options optional block configuring [ClickableScope.onClick] and
         *   [ClickableScope.sensitive]
         * @throws IllegalArgumentException if [label] is blank
         * @throws IllegalStateException if a component was already added to this slot
         */
        public fun iconButton(
            icon: TemplateIcon,
            label: String,
            options: ClickableScope.() -> Unit = {},
        ) {
            check(component == null) { "The trailing slot accepts only one component." }
            require(label.isNotBlank()) { "An icon button label must not be blank." }
            val scope = ClickableScope().apply(options)
            component =
                IconButton(
                    icon = icon.resId,
                    contentDescription = label,
                    action = scope.action,
                    sensitiveReplacement = scope.sensitiveReplacement,
                )
        }
    }

    /** Populates the [QuickActionsTemplateScope.actions] grid of a [QuickActionsTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class ActionsScope internal constructor() {
        private val tiles: MutableList<IconTile> = mutableListOf()

        /**
         * Appends an action: a glyph with its label, which runs [ActionScope.onClick] when tapped.
         *
         * The label is always the action's accessible name. Whether it is also drawn beneath the
         * glyph is up to the layout at each size.
         *
         * ```kotlin
         * action(TemplateIcon.fromResource(R.drawable.ic_garage), "Garage Door") {
         *     subtitle("Open")
         *     onClick(toggleGarage)
         * }
         * ```
         *
         * @param icon [TemplateIcon] of the action's glyph
         * @param label non-blank name of the action (for example, `"Run"`)
         * @param options optional block configuring [ActionScope.onClick], [ActionScope.sensitive]
         *   and [ActionScope.subtitle]; an action without [ActionScope.onClick] is shown but cannot
         *   be tapped
         * @throws IllegalArgumentException if [label] is blank
         */
        public fun action(icon: TemplateIcon, label: String, options: ActionScope.() -> Unit = {}) {
            require(label.isNotBlank()) { "An action label must not be blank." }
            val scope = ActionScope().apply(options)
            tiles.add(
                IconTile(
                    icon = icon.resId,
                    label = label,
                    action = scope.clickable.action,
                    sensitiveReplacement = scope.clickable.sensitiveReplacement,
                    subtitle = scope.subtitle,
                )
            )
        }

        internal fun build(): List<IconTile> {
            require(tiles.isNotEmpty()) {
                "The actions block must contain at least one action (for example, action(...))."
            }
            return tiles.toList()
        }
    }
}
