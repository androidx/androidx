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

package androidx.glance.adaptive.core.slots

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.collection.FloatList
import androidx.collection.IntList
import androidx.collection.emptyIntList
import androidx.glance.adaptive.core.action.GlanceWidgetAction

/*
 * The slot component library: backend-agnostic data models, one per component, each implementing
 * the contract of the slots it fits. Every backend draws them in its own renderer.
 *
 * Each component checks its own data when it is built, so one that exists is valid and a renderer
 * never has to.
 */

/** Fewest values a [Graph] draws: a single value has no history to show. */
private const val MIN_GRAPH_VALUES = 2

private val TextCapabilities = setOf(Capability.Text)
private val ActionCapabilities = setOf(Capability.Action)
private val RingCapabilities = setOf(Capability.Tracking)
private val GraphCapabilities = setOf(Capability.Tracking, Capability.History)
private val BadgeCapabilities = setOf(Capability.Status)
private val ContainedRingCapabilities = setOf(Capability.Tracking, Capability.Text)

/**
 * Text, such as a metric, a unit label or a supporting value. Fits the primary, secondary and
 * action slots, and the slot it is placed in decides its style: the design's
 * `ContentBlock_primary_text`, `ContentBlock_tertiary_text` or `ContentBlock_additional_*`.
 *
 * @property value The text as the app formatted it, e.g. `11,056` or `Steps`.
 * @property sensitiveReplacement Text shown instead of [value] on the lock screen, or `null` when
 *   [value] is not sensitive.
 * @throws IllegalArgumentException if [value] or [sensitiveReplacement] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TemplateText(
    public val value: String,
    public val sensitiveReplacement: String? = null,
) : PrimaryComponent, SecondaryComponent, ActionComponent {
    init {
        require(value.isNotBlank()) { "Text must not be blank." }
        require(sensitiveReplacement == null || sensitiveReplacement.isNotBlank()) {
            "Sensitive replacement must not be blank."
        }
    }

    override val capabilities: Set<Capability>
        get() = TextCapabilities
}

/**
 * Determinate progress ring with an optional glyph at its center — the design's
 * `ContentBlock_progress_ring`. Fits the leading slot.
 *
 * @property progress Normalized progress in `[0.0, 1.0]`.
 * @property icon Optional glyph drawn at the center, tinted with the primary color. Its drawable
 *   must be declared at least as large as the glyph is drawn, about 55% of the ring.
 * @throws IllegalArgumentException if [progress] is outside `[0.0, 1.0]`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Ring(
    @FloatRange(from = 0.0, to = 1.0) public val progress: Float,
    @DrawableRes public val icon: Int? = null,
) : LeadingComponent {
    init {
        require(progress in 0f..1f) { "Ring progress must be in [0, 1], was $progress." }
    }

    override val capabilities: Set<Capability>
        get() = RingCapabilities
}

/**
 * Bar or dot graph — the design's `ContentBlock_chart` / `Progress.BarChart`. Fits the hero slot.
 *
 * Expands to fill the height of the hero slot: the bars take whatever vertical space the labels and
 * the optional description leave.
 *
 * A value's bar is drawn as a fraction of the goal, or of the largest value when that is larger,
 * and takes the accent color once it reaches the goal.
 *
 * @property values The values to chart, one bar per value, leading to trailing.
 * @property labels One label per value, drawn under the bars, or empty for none.
 * @property goal The value a full bar stands for, or `null` to scale to the largest value.
 * @property dotted Whether to draw dots instead of bars, for counting discrete items.
 * @property description Leading and trailing captions beneath the bars, or `null` for none.
 * @throws IllegalArgumentException if there are fewer than 2 values, a value is negative or not
 *   finite, the labels do not match the values, or [goal] is not positive.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Graph(
    public val values: FloatList,
    public val labels: List<String> = emptyList(),
    public val goal: Float? = null,
    public val dotted: Boolean = false,
    public val description: Pair<String, String>? = null,
) : HeroComponent {
    init {
        require(values.size >= MIN_GRAPH_VALUES) {
            "A graph needs at least $MIN_GRAPH_VALUES values, had ${values.size}."
        }
        for (i in values.indices) {
            val value = values[i]
            require(value.isFinite() && value >= 0f) {
                "Graph values must be finite and not negative, had $value."
            }
        }
        require(labels.isEmpty() || labels.size == values.size) {
            "A graph needs one label per value or none, had ${labels.size} for ${values.size}."
        }
        require(goal == null || (goal.isFinite() && goal > 0f)) {
            "A graph goal must be positive, was $goal."
        }
    }

    override val capabilities: Set<Capability>
        get() = GraphCapabilities

    /** Value a full bar stands for: the goal, or the largest value when that is larger. */
    private val scale: Float = run {
        var max = goal ?: 0f
        for (i in values.indices) max = maxOf(max, values[i])
        max
    }

    /** Height of the bar at [index] as a fraction of a full bar, in `[0.0, 1.0]`. */
    public fun fractionAt(index: Int): Float =
        if (scale > 0f) (values[index] / scale).coerceIn(0f, 1f) else 0f

    /** Whether the value at [index] reached [goal], and so takes the accent color. */
    public fun meetsGoalAt(index: Int): Boolean = goal != null && values[index] >= goal

    /**
     * Whether the dot at [index] of a [dotted] graph is filled: its value reached the goal, or the
     * largest value when there is no goal.
     */
    public fun isDotFilledAt(index: Int): Boolean =
        if (goal != null) values[index] >= goal else scale > 0f && values[index] >= scale

    /** The label under the bar at [index], or `null` when the graph has no labels. */
    public fun labelAt(index: Int): String? = labels.getOrNull(index)
}

/**
 * Outlined pill flanked by status dots — the design's `Special_badge`. Fits an action slot. As in
 * the design, the first dot goes before the pill and the remaining ones after it.
 *
 * @property label Text inside the pill.
 * @property outlineColor ARGB color of the pill's outline.
 * @property dotColors ARGB colors of the status dots, leading to trailing. May be empty.
 * @throws IllegalArgumentException if [label] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Badge(
    public val label: String,
    @ColorInt public val outlineColor: Int,
    public val dotColors: IntList = emptyIntList(),
) : ActionComponent {
    init {
        require(label.isNotBlank()) { "A badge label must not be blank." }
    }

    override val capabilities: Set<Capability>
        get() = BadgeCapabilities
}

/**
 * The whole widget as one progress ring, with [value] inside it — the design's
 * `Progress.Ring_Contained`. Fits the bulb slot, and sizes itself to the circle the bulb gives it.
 *
 * @property progress Normalized progress in `[0.0, 1.0]`, or `null` to draw an empty ring.
 * @property value Headline value drawn inside the ring.
 * @property icon Optional glyph drawn above [value], tinted with the primary color.
 * @property sensitiveReplacement Text shown instead of [value] on the lock screen, or `null` when
 *   [value] is not sensitive. The progress is still drawn.
 * @throws IllegalArgumentException if [progress] is outside `[0.0, 1.0]`, or [value] or
 *   [sensitiveReplacement] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ContainedRing(
    @FloatRange(from = 0.0, to = 1.0) public val progress: Float?,
    public val value: String,
    @DrawableRes public val icon: Int? = null,
    public val sensitiveReplacement: String? = null,
) : BulbComponent {
    init {
        require(progress == null || progress in 0f..1f) {
            "Contained ring progress must be in [0, 1], was $progress."
        }
        require(value.isNotBlank()) { "A contained ring value must not be blank." }
        require(sensitiveReplacement == null || sensitiveReplacement.isNotBlank()) {
            "Sensitive replacement must not be blank."
        }
    }

    override val capabilities: Set<Capability>
        get() = ContainedRingCapabilities
}

/**
 * A glyph, such as an app's badge before a header text. Fits the leading slot, which sizes it.
 *
 * Drawn tinted with the primary color.
 *
 * @property icon Drawable resource of the glyph.
 * @property contentDescription What the glyph shows, for accessibility services, or `null` when it
 *   is decorative.
 * @throws IllegalArgumentException if [icon] is `0`, or [contentDescription] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Icon(
    @DrawableRes public val icon: Int,
    public val contentDescription: String? = null,
) : LeadingComponent {
    init {
        require(icon != 0) { "An icon needs a drawable resource." }
        require(contentDescription == null || contentDescription.isNotBlank()) {
            "Content description must not be blank."
        }
    }

    override val capabilities: Set<Capability>
        get() = emptySet()
}

/**
 * A glyph on a filled circle that runs [action] when tapped. Fits an action slot, the header's
 * trailing slot, and the bulb slot, where it is the whole widget and spans the circle the bulb
 * gives it.
 *
 * @property icon Drawable resource of the glyph, tinted with the primary color.
 * @property contentDescription What the button does, for accessibility services.
 * @property action Action to run when tapped, or `null` for a button that only shows its glyph.
 * @property sensitiveReplacement Content description used instead of [contentDescription] on the
 *   lock screen, or `null` when [contentDescription] is not sensitive, such as a contact's name.
 * @throws IllegalArgumentException if [icon] is `0`, or [contentDescription] or
 *   [sensitiveReplacement] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IconButton(
    @DrawableRes public val icon: Int,
    public val contentDescription: String,
    public val action: GlanceWidgetAction? = null,
    public val sensitiveReplacement: String? = null,
) : ActionComponent, TrailingComponent, BulbComponent {
    init {
        require(icon != 0) { "An icon button needs a drawable resource." }
        require(contentDescription.isNotBlank()) { "Content description must not be blank." }
        require(sensitiveReplacement == null || sensitiveReplacement.isNotBlank()) {
            "Sensitive replacement must not be blank."
        }
    }

    override val capabilities: Set<Capability>
        get() = if (action != null) ActionCapabilities else emptySet()
}

/**
 * Grid of tappable glyph tiles, filled row by row — the design's `Range of buttons`. Fits the hero
 * slot, and fills its height: the rows share whatever height the other slots leave, and the columns
 * share the card's width.
 *
 * A grid with fewer [tiles] than cells leaves the trailing cells of its last row empty, so the
 * tiles keep the size they have in a full grid.
 *
 * @property tiles The tiles, leading to trailing and top to bottom.
 * @property columns Number of tiles in a row.
 * @property rows Number of rows.
 * @property showLabels Whether each tile draws its label beneath the glyph. Set it to `false` when
 *   a row is too short for a label line; accessibility services announce the label either way.
 * @throws IllegalArgumentException if [columns] or [rows] is not positive, or there are no tiles or
 *   more tiles than cells.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IconGrid(
    tiles: List<IconTile>,
    public val columns: Int,
    public val rows: Int,
    public val showLabels: Boolean = true,
) : HeroComponent {

    /** The tiles, leading to trailing and top to bottom. */
    public val tiles: List<IconTile> = List(tiles.size) { i -> tiles[i] }

    init {
        require(columns > 0 && rows > 0) {
            "An icon grid needs at least one row and one column, had $columns x $rows."
        }
        require(this.tiles.size in 1..columns * rows) {
            "An icon grid of $columns x $rows needs 1 to ${columns * rows} tiles, had ${this.tiles.size}."
        }
    }

    override val capabilities: Set<Capability>
        get() {
            for (i in tiles.indices) if (tiles[i].action != null) return ActionCapabilities
            return emptySet()
        }

    /**
     * The tile in the cell at [row] and [column], or `null` when the cell is empty or out of
     * bounds.
     */
    public fun tileAt(row: Int, column: Int): IconTile? =
        if (row in 0 until rows && column in 0 until columns) {
            tiles.getOrNull(row * columns + column)
        } else {
            null
        }
}

/**
 * One cell of an [IconGrid]: a glyph, with its label beneath it when the grid shows labels,
 * centered on a filled rounded tile, which runs [action] when tapped.
 *
 * @property icon Drawable resource of the glyph, tinted with the primary color.
 * @property label Name of the tile, announced by accessibility services, and drawn beneath the
 *   glyph when [IconGrid.showLabels] is `true`.
 * @property action Action to run when tapped, or `null` for a tile that only shows its glyph.
 * @property sensitiveReplacement Label shown and announced instead of [label] on the lock screen,
 *   or `null` when [label] is not sensitive, such as a contact's name.
 * @throws IllegalArgumentException if [icon] is `0`, or [label] or [sensitiveReplacement] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IconTile(
    @DrawableRes public val icon: Int,
    public val label: String,
    public val action: GlanceWidgetAction? = null,
    public val sensitiveReplacement: String? = null,
) {
    init {
        require(icon != 0) { "An icon tile needs a drawable resource." }
        require(label.isNotBlank()) { "An icon tile label must not be blank." }
        require(sensitiveReplacement == null || sensitiveReplacement.isNotBlank()) {
            "Sensitive replacement must not be blank."
        }
    }
}
