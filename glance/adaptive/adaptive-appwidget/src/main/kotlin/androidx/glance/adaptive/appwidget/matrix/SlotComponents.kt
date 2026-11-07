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

package androidx.glance.adaptive.appwidget.matrix

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.collection.FloatList
import androidx.collection.IntList
import androidx.collection.emptyIntList
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier

/*
 * The slot component library: backend-agnostic data models, one per component, each implementing
 * the contract of the slots it fits. Every backend draws them in its own matrix renderer, with each
 * component's drawing code in its own package under components/.
 *
 * Each component checks its own data when it is built, so one that exists is valid and a renderer
 * never has to.
 */

/** Total inset of the contained ring inside its circle, from the design's 80 dp ring in 88 dp. */
private const val CONTAINED_RING_INSET_DP = 8f

/**
 * Diameter of the contained ring the design draws, at which its value is [CONTAINED_RING_TEXT_SP].
 */
private const val CONTAINED_RING_DESIGN_SIZE_DP = 80f

/** Type size of the contained ring's value at [CONTAINED_RING_DESIGN_SIZE_DP]. */
private const val CONTAINED_RING_TEXT_SP = 20f

/**
 * Fraction of the contained ring's diameter its value may span: the chord narrows towards the
 * circle's edges, and two stroke widths in from each side is where the design's value sits.
 */
private const val CONTAINED_RING_TEXT_RATIO = 0.86f

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
internal class TemplateText(val value: String, val sensitiveReplacement: String? = null) :
    PrimaryComponent, SecondaryComponent, ActionComponent {
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
internal class Ring(
    @FloatRange(from = 0.0, to = 1.0) val progress: Float,
    @DrawableRes val icon: Int? = null,
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
 * the optional description leave. The gap between the bars widens with the card's width tier (see
 * [barSpacing]).
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
internal class Graph(
    val values: FloatList,
    val labels: List<String> = emptyList(),
    val goal: Float? = null,
    val dotted: Boolean = false,
    val description: Pair<String, String>? = null,
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
    fun fractionAt(index: Int): Float =
        if (scale > 0f) (values[index] / scale).coerceIn(0f, 1f) else 0f

    /** Whether the value at [index] reached [goal], and so takes the accent color. */
    fun meetsGoalAt(index: Int): Boolean = goal != null && values[index] >= goal

    /**
     * Whether the dot at [index] of a [dotted] graph is filled: its value reached the goal, or the
     * largest value when there is no goal.
     */
    fun isDotFilledAt(index: Int): Boolean =
        if (goal != null) values[index] >= goal else scale > 0f && values[index] >= scale

    /** The label under the bar at [index], or `null` when the graph has no labels. */
    fun labelAt(index: Int): String? = labels.getOrNull(index)

    companion object {
        /**
         * Horizontal gap between the bars of a graph in a card at [tiers], which widens from W3.
         */
        fun barSpacing(tiers: SizeTiers): Dp = if (tiers.width <= WidthTier.W2) 8.dp else 16.dp
    }
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
internal class Badge(
    val label: String,
    @ColorInt val outlineColor: Int,
    val dotColors: IntList = emptyIntList(),
) : ActionComponent {
    init {
        require(label.isNotBlank()) { "A badge label must not be blank." }
    }

    override val capabilities: Set<Capability>
        get() = BadgeCapabilities
}

/**
 * The whole widget as one progress ring, with [value] inside it — the design's
 * `Progress.Ring_Contained`. Fits the bulb slot, and sizes itself to the circle the bulb gives it
 * (see [geometry]).
 *
 * @property progress Normalized progress in `[0.0, 1.0]`, or `null` to draw an empty ring.
 * @property value Headline value drawn inside the ring.
 * @property icon Optional glyph drawn above [value], tinted with the primary color.
 * @property sensitiveReplacement Text shown instead of [value] on the lock screen, or `null` when
 *   [value] is not sensitive. The progress is still drawn.
 * @throws IllegalArgumentException if [progress] is outside `[0.0, 1.0]`, or [value] or
 *   [sensitiveReplacement] is blank.
 */
internal class ContainedRing(
    @FloatRange(from = 0.0, to = 1.0) val progress: Float?,
    val value: String,
    @DrawableRes val icon: Int? = null,
    val sensitiveReplacement: String? = null,
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

    /**
     * Resolves the ring's layout inside a circle [containerSize] across.
     *
     * The value shrinks with a ring drawn smaller than the design's, so the glyph and the value
     * stacked inside it still clear the ring.
     *
     * @param containerSize Diameter of the circle the bulb gives the ring.
     * @return The ring's size, and the room and type size of its value.
     */
    fun geometry(containerSize: Dp): ContainedRingGeometry {
        val ringDp = (containerSize.value - CONTAINED_RING_INSET_DP).coerceAtLeast(0f)
        return ContainedRingGeometry(
            ringSize = ringDp.dp,
            valueMaxWidth = (ringDp * CONTAINED_RING_TEXT_RATIO).dp,
            fontSize =
                (CONTAINED_RING_TEXT_SP * (ringDp / CONTAINED_RING_DESIGN_SIZE_DP).coerceAtMost(1f))
                    .sp,
        )
    }
}

/**
 * A [ContainedRing] laid out for its circle.
 *
 * @property ringSize Outer diameter of the ring.
 * @property valueMaxWidth Room the value has across the ring.
 * @property fontSize Type size of the value.
 */
internal class ContainedRingGeometry(
    val ringSize: Dp,
    val valueMaxWidth: Dp,
    val fontSize: TextUnit,
)

/**
 * A glyph, such as an app's badge before a header text. Fits the leading slot, which sizes it (see
 * [MatrixDefaults.LeadingSize] and [MatrixCardContent.leadingSize]).
 *
 * Drawn tinted with the primary color, as every glyph of the matrix is.
 *
 * @property icon Drawable resource of the glyph.
 * @property contentDescription What the glyph shows, for accessibility services, or `null` when it
 *   is decorative.
 * @throws IllegalArgumentException if [icon] is `0`, or [contentDescription] is blank.
 */
internal class Icon(@DrawableRes val icon: Int, val contentDescription: String? = null) :
    LeadingComponent {
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
internal class IconButton(
    @DrawableRes val icon: Int,
    val contentDescription: String,
    val action: GlanceWidgetAction? = null,
    val sensitiveReplacement: String? = null,
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
internal class IconGrid(
    tiles: List<IconTile>,
    val columns: Int,
    val rows: Int,
    val showLabels: Boolean = true,
) : HeroComponent {

    /** The tiles, leading to trailing and top to bottom. */
    val tiles: List<IconTile> = List(tiles.size) { i -> tiles[i] }

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
    fun tileAt(row: Int, column: Int): IconTile? =
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
internal class IconTile(
    @DrawableRes val icon: Int,
    val label: String,
    val action: GlanceWidgetAction? = null,
    val sensitiveReplacement: String? = null,
) {
    init {
        require(icon != 0) { "An icon tile needs a drawable resource." }
        require(label.isNotBlank()) { "An icon tile label must not be blank." }
        require(sensitiveReplacement == null || sensitiveReplacement.isNotBlank()) {
            "Sensitive replacement must not be blank."
        }
    }
}
