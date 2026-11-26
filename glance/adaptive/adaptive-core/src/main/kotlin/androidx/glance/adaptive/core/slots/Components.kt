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
import androidx.collection.MutableFloatList
import androidx.collection.emptyIntList
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.templates.list.ListItemStyle

/*
 * The slot component library: backend-agnostic data models, one per component, each implementing
 * the contract of the slots it fits. Every backend draws them in its own renderer.
 *
 * Each component checks its own data when it is built, so one that exists is valid and a renderer
 * never has to.
 */

/** Fewest values a [Graph] draws: a single value has no history to show. */
internal const val MIN_GRAPH_VALUES = 2

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
 * @property isStruckThrough Whether the text should be rendered with strikethrough (for example, a
 *   declined event or completed task).
 * @throws IllegalArgumentException if [value] or [sensitiveReplacement] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TemplateText(
    public val value: String,
    public val sensitiveReplacement: String? = null,
    public val isStruckThrough: Boolean = false,
) : PrimaryComponent, SecondaryComponent, ActionComponent {
    init {
        require(value.isNotBlank()) { "Text must not be blank." }
        require(sensitiveReplacement == null || sensitiveReplacement.isNotBlank()) {
            "Sensitive replacement must not be blank."
        }
    }

    override val capabilities: Set<Capability>
        get() = TextCapabilities

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TemplateText) return false
        return value == other.value &&
            sensitiveReplacement == other.sensitiveReplacement &&
            isStruckThrough == other.isStruckThrough
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + (sensitiveReplacement?.hashCode() ?: 0)
        result = 31 * result + isStruckThrough.hashCode()
        return result
    }

    override fun toString(): String =
        "TemplateText(value=$value, sensitiveReplacement=$sensitiveReplacement, " +
            "isStruckThrough=$isStruckThrough)"
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
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    @DrawableRes public val icon: Int? = null,
) : LeadingComponent {
    @get:FloatRange(from = 0.0, to = 1.0)
    public val progress: Float = if (progress == 0f) 0f else progress

    init {
        require(progress in 0f..1f) { "Ring progress must be in [0, 1], was $progress." }
    }

    override val capabilities: Set<Capability>
        get() = RingCapabilities

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ring) return false
        return progress.compareTo(other.progress) == 0 && icon == other.icon
    }

    override fun hashCode(): Int = 31 * progress.hashCode() + (icon ?: 0)

    override fun toString(): String = "Ring(progress=$progress, icon=$icon)"
}

/**
 * Caption displayed beneath a [Graph] in its description row, with an optional leading [icon].
 *
 * @property text Caption text (for example, `"7 Day Avg"` or `"13,843"`).
 * @property icon Optional drawable resource displayed immediately before [text].
 * @throws IllegalArgumentException if [text] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ChartLabel(
    public val text: String,
    @param:DrawableRes @get:DrawableRes public val icon: Int? = null,
) {
    init {
        require(text.isNotBlank()) { "Chart label must not be blank." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChartLabel) return false
        return text == other.text && icon == other.icon
    }

    override fun hashCode(): Int = 31 * text.hashCode() + (icon ?: 0)

    override fun toString(): String = "ChartLabel(text=$text, icon=$icon)"
}

/**
 * Bar or dot graph — the design's `ContentBlock_chart` / `Progress.BarChart`. Fits the hero slot.
 *
 * Expands to fill the height of the hero slot: the bars take whatever vertical space the labels and
 * the optional description leave.
 *
 * A value's bar is drawn as a fraction of the goal, or of the largest value when that is larger.
 * Accent emphasis is controlled per bar via [emphasized] when provided, or falls back to reaching
 * [goal].
 *
 * @property values The values to chart, one bar per value, leading to trailing.
 * @property labels One label per value, drawn under the bars, or empty for none.
 * @property goal The value a full bar stands for, or `null` to scale to the largest value.
 * @property emphasized Optional per-bar emphasis flags matching [values] length. When `null`, a bar
 *   is emphasized once its value reaches [goal]. Ignored when [dotted]. Must not be modified once
 *   passed in.
 * @property dotted Whether to draw dots instead of bars, for counting discrete items.
 * @property startLabel Optional leading caption in the description row beneath the bars.
 * @property endLabel Optional trailing caption in the description row beneath the bars.
 * @throws IllegalArgumentException if there are fewer than 2 values, a value is negative or not
 *   finite, [labels] or [emphasized] does not match [values] size, or [goal] is not positive.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Graph(
    values: FloatList,
    public val labels: List<String> = emptyList(),
    public val goal: Float? = null,
    public val emphasized: BooleanArray? = null,
    public val dotted: Boolean = false,
    public val startLabel: ChartLabel? = null,
    public val endLabel: ChartLabel? = null,
) : HeroComponent {
    /** The values to chart, one bar per value, leading to trailing. */
    public val values: FloatList =
        MutableFloatList(values.size).apply {
            for (i in values.indices) {
                val value = values[i]
                add(if (value == 0f) 0f else value)
            }
        }

    init {
        require(this.values.size >= MIN_GRAPH_VALUES) {
            "A graph needs at least $MIN_GRAPH_VALUES values, had ${this.values.size}."
        }
        for (i in this.values.indices) {
            val value = this.values[i]
            require(value.isFinite() && value >= 0f) {
                "Graph values must be finite and not negative, had $value."
            }
        }
        require(labels.isEmpty() || labels.size == this.values.size) {
            "A graph needs one label per value or none, had ${labels.size} for ${this.values.size}."
        }
        require(emphasized == null || emphasized.size == this.values.size) {
            "A graph needs one emphasis flag per value or none, had ${emphasized?.size} for ${this.values.size}."
        }
        require(goal == null || (goal.isFinite() && goal > 0f)) {
            "A graph goal must be positive, was $goal."
        }
    }

    override val capabilities: Set<Capability>
        get() = GraphCapabilities

    /** Whether either [startLabel] or [endLabel] is present. */
    public val hasDescription: Boolean
        get() = startLabel != null || endLabel != null

    /** Value a full bar stands for: the goal, or the largest value when that is larger. */
    private val scale: Float = run {
        var max = goal ?: 0f
        for (i in this.values.indices) max = maxOf(max, this.values[i])
        max
    }

    /** Height of the bar at [index] as a fraction of a full bar, in `[0.0, 1.0]`. */
    public fun fractionAt(index: Int): Float =
        if (scale > 0f) (values[index] / scale).coerceIn(0f, 1f) else 0f

    /** Whether the bar at [index] takes the emphasized accent color. */
    public fun isEmphasizedAt(index: Int): Boolean = emphasized?.get(index) ?: meetsGoalAt(index)

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

    /** Returns a copy of this [Graph] without [startLabel] or [endLabel], or `this` if absent. */
    public fun withoutDescription(): Graph =
        if (!hasDescription) {
            this
        } else {
            Graph(
                values = values,
                labels = labels,
                goal = goal,
                emphasized = emphasized,
                dotted = dotted,
                startLabel = null,
                endLabel = null,
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Graph) return false
        return values == other.values &&
            labels == other.labels &&
            goal == other.goal &&
            emphasized.contentEquals(other.emphasized) &&
            dotted == other.dotted &&
            startLabel == other.startLabel &&
            endLabel == other.endLabel
    }

    override fun hashCode(): Int {
        var result = values.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + (goal?.hashCode() ?: 0)
        result = 31 * result + (emphasized?.contentHashCode() ?: 0)
        result = 31 * result + dotted.hashCode()
        result = 31 * result + (startLabel?.hashCode() ?: 0)
        result = 31 * result + (endLabel?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "Graph(values=$values, labels=$labels, goal=$goal, " +
            "emphasized=${emphasized?.contentToString()}, dotted=$dotted, " +
            "startLabel=$startLabel, endLabel=$endLabel)"
}

/**
 * Outlined pill flanked by status dots — the design's `Special_badge`. Fits an action slot. As in
 * the design, the first dot goes before the pill and the remaining ones after it.
 *
 * @property label Text inside the pill.
 * @property outlineColor Optional ARGB color of the pill's outline; when `null`, host renderers
 *   apply the active surface theme's accent color.
 * @property dotColors ARGB colors of the status dots, leading to trailing. May be empty.
 * @throws IllegalArgumentException if [label] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Badge(
    public val label: String,
    @param:ColorInt @get:ColorInt public val outlineColor: Int? = null,
    public val dotColors: IntList = emptyIntList(),
) : ActionComponent {
    init {
        require(label.isNotBlank()) { "A badge label must not be blank." }
    }

    override val capabilities: Set<Capability>
        get() = BadgeCapabilities

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Badge) return false
        return label == other.label &&
            outlineColor == other.outlineColor &&
            dotColors == other.dotColors
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + (outlineColor ?: 0)
        result = 31 * result + dotColors.hashCode()
        return result
    }

    override fun toString(): String =
        "Badge(label=$label, outlineColor=$outlineColor, dotColors=$dotColors)"
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
 * @property contained Whether the glyph sits inside a filled circle, as a Quick Actions header's
 *   brand icon does, rather than drawing as a bare glyph.
 * @throws IllegalArgumentException if [icon] is `0`, or [contentDescription] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Icon(
    @DrawableRes public val icon: Int,
    public val contentDescription: String? = null,
    @get:JvmName("isContained") public val contained: Boolean = false,
) : LeadingComponent {
    init {
        require(icon != 0) { "An icon needs a drawable resource." }
        require(contentDescription == null || contentDescription.isNotBlank()) {
            "Content description must not be blank."
        }
    }

    override val capabilities: Set<Capability>
        get() = emptySet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Icon) return false
        return icon == other.icon &&
            contentDescription == other.contentDescription &&
            contained == other.contained
    }

    override fun hashCode(): Int {
        var result = icon
        result = 31 * result + (contentDescription?.hashCode() ?: 0)
        result = 31 * result + contained.hashCode()
        return result
    }

    override fun toString(): String =
        "Icon(icon=$icon, contentDescription=$contentDescription, contained=$contained)"
}

/**
 * A glyph, inside a filled circle when [contained] is `true`, that runs [action] when tapped. Fits
 * an action slot, the header's trailing slot, and the bulb slot, where it is the whole widget and
 * spans the circle the bulb gives it.
 *
 * @property icon Drawable resource of the glyph, tinted with `onSecondaryContainer` when
 *   [contained] is `true` and `primary` when uncontained.
 * @property contentDescription What the button does, for accessibility services.
 * @property action Action to run when tapped, or `null` for a button that only shows its glyph.
 * @property sensitiveReplacement Content description used instead of [contentDescription] on the
 *   lock screen, or `null` when [contentDescription] is not sensitive, such as a contact's name.
 * @property contained Whether the glyph sits inside a filled circle, rather than drawing as a bare
 *   glyph.
 * @throws IllegalArgumentException if [icon] is `0`, or [contentDescription] or
 *   [sensitiveReplacement] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IconButton(
    @DrawableRes public val icon: Int,
    public val contentDescription: String,
    public val action: GlanceAdaptiveAction? = null,
    public val sensitiveReplacement: String? = null,
    @get:JvmName("isContained") public val contained: Boolean = true,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IconButton) return false
        return icon == other.icon &&
            contentDescription == other.contentDescription &&
            action == other.action &&
            sensitiveReplacement == other.sensitiveReplacement &&
            contained == other.contained
    }

    override fun hashCode(): Int {
        var result = icon
        result = 31 * result + contentDescription.hashCode()
        result = 31 * result + (action?.hashCode() ?: 0)
        result = 31 * result + (sensitiveReplacement?.hashCode() ?: 0)
        result = 31 * result + contained.hashCode()
        return result
    }

    override fun toString(): String =
        "IconButton(icon=$icon, contentDescription=$contentDescription, action=$action, " +
            "sensitiveReplacement=$sensitiveReplacement, contained=$contained)"
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
 * One cell of an [IconGrid]: a glyph, with its label and optional [subtitle] beneath it when the
 * grid shows labels, on a filled rounded tile, which runs [action] when tapped.
 *
 * @property icon Drawable resource of the glyph, tinted with the primary color.
 * @property label Name of the tile, announced by accessibility services, and drawn beneath the
 *   glyph when [IconGrid.showLabels] is `true`.
 * @property action Action to run when tapped, or `null` for a tile that only shows its glyph.
 * @property sensitiveReplacement Label shown and announced instead of [label] on the lock screen,
 *   or `null` when [label] is not sensitive, such as a contact's name.
 * @property subtitle Optional second line beneath [label], such as a state like `Open` or `On ·
 *   80%`, drawn more faintly where the tile has room for it and announced after [label]. Its own
 *   [TemplateText.sensitiveReplacement] replaces it on the lock screen.
 * @throws IllegalArgumentException if [icon] is `0`, or [label] or [sensitiveReplacement] is blank.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IconTile(
    @DrawableRes public val icon: Int,
    public val label: String,
    public val action: GlanceAdaptiveAction? = null,
    public val sensitiveReplacement: String? = null,
    public val subtitle: TemplateText? = null,
) {
    init {
        require(icon != 0) { "An icon tile needs a drawable resource." }
        require(label.isNotBlank()) { "An icon tile label must not be blank." }
        require(sensitiveReplacement == null || sensitiveReplacement.isNotBlank()) {
            "Sensitive replacement must not be blank."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IconTile) return false
        return icon == other.icon &&
            label == other.label &&
            action == other.action &&
            sensitiveReplacement == other.sensitiveReplacement &&
            subtitle == other.subtitle
    }

    override fun hashCode(): Int {
        var result = icon
        result = 31 * result + label.hashCode()
        result = 31 * result + (action?.hashCode() ?: 0)
        result = 31 * result + (sensitiveReplacement?.hashCode() ?: 0)
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "IconTile(icon=$icon, label=$label, action=$action, " +
            "sensitiveReplacement=$sensitiveReplacement, subtitle=$subtitle)"
}

private val ListCapabilities = setOf(Capability.Text)
private val ListActionCapabilities = setOf(Capability.Text, Capability.Action)
private val ListStatusCapabilities = setOf(Capability.Text, Capability.Status)
private val ListActionStatusCapabilities =
    setOf(Capability.Text, Capability.Action, Capability.Status)

/**
 * Single row inside an [ItemList].
 *
 * Concrete row shapes model the design's list item variants: [Compact] rows (events, briefs),
 * [Checkbox] rows (standalone checkable items), [DetailCard] cards (multi-line body or inline
 * checklist preview), and [Divider] section headers.
 *
 * @property title Primary headline text of the row.
 * @property action Optional tap action for this row (`null` on non-interactive rows such as
 *   [Divider]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ListItem private constructor() {
    public abstract val title: TemplateText

    public open val action: GlanceAdaptiveAction?
        get() = null

    /**
     * Standard list item row (for example, a calendar event or morning brief summary).
     *
     * @property title Primary headline text of the row.
     * @property subtitle Optional secondary line (for example, time range or status).
     * @property tertiaryText Optional third line (for example, location), shown when the row is the
     *   first content item and [ItemList.showTertiaryText] is `true`.
     * @property tertiaryIcon Optional small icon displayed before [tertiaryText] (requires
     *   [tertiaryText] to be non-null).
     * @property leadingIcon Optional uncontained leading [Icon] displayed before the text column.
     * @property trailingIcon Optional trailing [Icon] (mutually exclusive with [trailingChip]).
     * @property trailingChip Optional trailing status [Badge] (mutually exclusive with
     *   [trailingIcon]).
     * @property action Optional tap action for this row.
     * @property style Container prominence style ([ListItemStyle.Default], [ListItemStyle.Focused],
     *   [ListItemStyle.Neutral], or [ListItemStyle.Outlined]).
     * @throws IllegalArgumentException if [leadingIcon] is contained, both [trailingIcon] and
     *   [trailingChip] are set, or [tertiaryIcon] is set without [tertiaryText].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Compact(
        override val title: TemplateText,
        public val subtitle: TemplateText? = null,
        public val tertiaryText: TemplateText? = null,
        public val tertiaryIcon: Icon? = null,
        public val leadingIcon: Icon? = null,
        public val trailingIcon: Icon? = null,
        public val trailingChip: Badge? = null,
        override val action: GlanceAdaptiveAction? = null,
        public val style: ListItemStyle = ListItemStyle.Default,
    ) : ListItem() {
        init {
            require(leadingIcon == null || !leadingIcon.contained) {
                "A list item leading icon must not be contained."
            }
            require(trailingIcon == null || trailingChip == null) {
                "A list item cannot have both a trailingIcon and a trailingChip."
            }
            require(tertiaryIcon == null || tertiaryText != null) {
                "A list item cannot set a tertiaryIcon without tertiaryText."
            }
            require(tertiaryIcon == null || !tertiaryIcon.contained) {
                "A list item tertiary icon must not be contained."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Compact) return false
            return title == other.title &&
                subtitle == other.subtitle &&
                tertiaryText == other.tertiaryText &&
                tertiaryIcon == other.tertiaryIcon &&
                leadingIcon == other.leadingIcon &&
                trailingIcon == other.trailingIcon &&
                trailingChip == other.trailingChip &&
                action == other.action &&
                style == other.style
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + (subtitle?.hashCode() ?: 0)
            result = 31 * result + (tertiaryText?.hashCode() ?: 0)
            result = 31 * result + (tertiaryIcon?.hashCode() ?: 0)
            result = 31 * result + (leadingIcon?.hashCode() ?: 0)
            result = 31 * result + (trailingIcon?.hashCode() ?: 0)
            result = 31 * result + (trailingChip?.hashCode() ?: 0)
            result = 31 * result + (action?.hashCode() ?: 0)
            result = 31 * result + style.hashCode()
            return result
        }

        override fun toString(): String =
            "ListItem.Compact(title=$title, subtitle=$subtitle, tertiaryText=$tertiaryText, " +
                "tertiaryIcon=$tertiaryIcon, leadingIcon=$leadingIcon, " +
                "trailingIcon=$trailingIcon, trailingChip=$trailingChip, action=$action, " +
                "style=$style)"
    }

    /**
     * Standalone checkable list row (for example, a checklist or to-do item).
     *
     * @property title Label text of the checkable row.
     * @property isChecked Whether the checkbox is currently checked.
     * @property action Optional tap action invoked when toggling the row.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Checkbox(
        override val title: TemplateText,
        public val isChecked: Boolean = false,
        override val action: GlanceAdaptiveAction? = null,
    ) : ListItem() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Checkbox) return false
            return title == other.title && isChecked == other.isChecked && action == other.action
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + isChecked.hashCode()
            result = 31 * result + (action?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String =
            "ListItem.Checkbox(title=$title, isChecked=$isChecked, action=$action)"
    }

    /**
     * Multi-line detail card displaying a [title] with either a multi-line [body] snippet or a
     * read-only inline [checklistItems] preview (for example, a note, message preview, or task
     * summary card).
     *
     * @property title Headline text of the detail card.
     * @property body Optional multi-line text preview (mutually exclusive with [checklistItems]).
     * @param checklistItems Optional read-only inline [ChecklistEntry] preview rows (at most 4,
     *   mutually exclusive with [body]), defensively copied.
     * @property action Optional tap action opening the detail item.
     * @property style Container prominence style ([ListItemStyle.Default], [ListItemStyle.Focused],
     *   [ListItemStyle.Neutral], or [ListItemStyle.Outlined]).
     * @throws IllegalArgumentException if [body] is non-null and [checklistItems] is non-empty, or
     *   if [checklistItems] has more than 4 entries.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class DetailCard(
        override val title: TemplateText,
        public val body: TemplateText? = null,
        checklistItems: List<ChecklistEntry> = emptyList(),
        override val action: GlanceAdaptiveAction? = null,
        public val style: ListItemStyle = ListItemStyle.Default,
    ) : ListItem() {

        /**
         * Single read-only checkable entry inside a [DetailCard]'s inline checklist preview.
         *
         * @property text Display text of the checklist entry.
         * @property isChecked Whether the entry is currently checked.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public class ChecklistEntry(
            public val text: TemplateText,
            public val isChecked: Boolean = false,
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is ChecklistEntry) return false
                return text == other.text && isChecked == other.isChecked
            }

            override fun hashCode(): Int {
                var result = text.hashCode()
                result = 31 * result + isChecked.hashCode()
                return result
            }

            override fun toString(): String = "ChecklistEntry(text=$text, isChecked=$isChecked)"
        }

        /** Inline checklist preview rows, defensively copied. */
        public val checklistItems: List<ChecklistEntry> =
            List(checklistItems.size) { i -> checklistItems[i] }

        init {
            require(body == null || this.checklistItems.isEmpty()) {
                "A detail card item cannot have both body text and inline checklist items."
            }
            require(this.checklistItems.size <= MAX_CHECKLIST_ITEMS) {
                "A detail card item can hold at most $MAX_CHECKLIST_ITEMS checklist items, " +
                    "was ${this.checklistItems.size}."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DetailCard) return false
            return title == other.title &&
                body == other.body &&
                checklistItems == other.checklistItems &&
                action == other.action &&
                style == other.style
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + (body?.hashCode() ?: 0)
            result = 31 * result + checklistItems.hashCode()
            result = 31 * result + (action?.hashCode() ?: 0)
            result = 31 * result + style.hashCode()
            return result
        }

        override fun toString(): String =
            "ListItem.DetailCard(title=$title, body=$body, checklistItems=$checklistItems, " +
                "action=$action, style=$style)"

        internal companion object {
            /** Most inline checklist preview entries a [DetailCard] holds. */
            const val MAX_CHECKLIST_ITEMS: Int = 4
        }
    }

    /**
     * Non-clickable section divider row (for example, `"TOMORROW"`).
     *
     * @property title Section heading text (must not be struck through).
     * @throws IllegalArgumentException if [title] is struck through.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Divider(override val title: TemplateText) : ListItem() {
        init {
            require(!title.isStruckThrough) {
                "A divider list item title must not be struck through."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Divider) return false
            return title == other.title
        }

        override fun hashCode(): Int = title.hashCode()

        override fun toString(): String = "ListItem.Divider(title=$title)"
    }
}

/** How an [ItemList] sizes and styles its rows within the hero slot. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class ItemListLayoutMode private constructor(private val value: Int) {
    override fun toString(): String =
        when (this) {
            Default -> "Default"
            ExpandSingle -> "ExpandSingle"
            UncontainedSingle -> "UncontainedSingle"
            ExpandFeatured -> "ExpandFeatured"
            else -> "Unknown($value)"
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Standard compact rows sized to their intrinsic content. */
        public val Default: ItemListLayoutMode = ItemListLayoutMode(0)

        /** Single item fills the hero slot height and wraps its title up to two lines. */
        public val ExpandSingle: ItemListLayoutMode = ItemListLayoutMode(1)

        /**
         * Single item fills the hero slot directly on the outer widget card without an inner
         * container background or padding (e.g. a headerless `1×1` compact square), wrapping its
         * title up to two lines.
         */
        public val UncontainedSingle: ItemListLayoutMode = ItemListLayoutMode(2)

        /**
         * Featured first content item absorbs available vertical slack in non-scrollable layouts
         * while remaining items stay compact beneath it.
         */
        public val ExpandFeatured: ItemListLayoutMode = ItemListLayoutMode(3)
    }
}

/**
 * Vertical list of [ListItem]s — the design's `List` body. Fits the hero slot.
 *
 * @property items Ordered list items to render (at least one non-divider item).
 * @property isScrollable Whether host renderers should make the list vertically scrollable (e.g. on
 *   phone home screens) rather than drawing a fixed non-scrollable stack (e.g. Lockscreen, XR,
 *   Auto).
 * @property showTertiaryText Whether the first content item should display its
 *   [ListItem.Compact.tertiaryText] when present.
 * @property layoutMode How rows are sized within the hero slot (must be
 *   [ItemListLayoutMode.Default] when [isScrollable] is `true`; [ItemListLayoutMode.ExpandSingle]
 *   and [ItemListLayoutMode.UncontainedSingle] require exactly one item, and
 *   [ItemListLayoutMode.UncontainedSingle] requires that item to be a [ListItem.Compact]).
 * @throws IllegalArgumentException if [items] is empty or contains only dividers, if [isScrollable]
 *   is `true` with a non-default [layoutMode], if a single-item [layoutMode] is used with more than
 *   one item, or if [ItemListLayoutMode.UncontainedSingle] is used with a non-[ListItem.Compact]
 *   item.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ItemList(
    items: List<ListItem>,
    public val isScrollable: Boolean = false,
    @get:JvmName("shouldShowTertiaryText") public val showTertiaryText: Boolean = false,
    public val layoutMode: ItemListLayoutMode = ItemListLayoutMode.Default,
) : HeroComponent {

    /** Ordered list items, defensively copied. */
    public val items: List<ListItem> = List(items.size) { i -> items[i] }

    init {
        require(this.items.isNotEmpty()) { "An item list needs at least one item." }
        var hasContentItem = false
        for (i in this.items.indices) {
            if (this.items[i] !is ListItem.Divider) {
                hasContentItem = true
                break
            }
        }
        require(hasContentItem) { "An item list needs at least one non-divider item." }
        require(!isScrollable || layoutMode == ItemListLayoutMode.Default) {
            "A scrollable item list must use ItemListLayoutMode.Default, was $layoutMode."
        }
        require(
            (layoutMode != ItemListLayoutMode.ExpandSingle &&
                layoutMode != ItemListLayoutMode.UncontainedSingle) || this.items.size == 1
        ) {
            "Single-item layout mode $layoutMode requires exactly one item, had ${this.items.size}."
        }
        require(
            layoutMode != ItemListLayoutMode.UncontainedSingle || this.items[0] is ListItem.Compact
        ) {
            "ItemListLayoutMode.UncontainedSingle requires a ListItem.Compact item."
        }
    }

    override val capabilities: Set<Capability>
        get() {
            var hasAction = false
            var hasStatus = false
            for (i in items.indices) {
                val item = items[i]
                if (item.action != null) hasAction = true
                if (item is ListItem.Compact && item.trailingChip != null) hasStatus = true
                if (hasAction && hasStatus) return ListActionStatusCapabilities
            }
            return when {
                hasAction -> ListActionCapabilities
                hasStatus -> ListStatusCapabilities
                else -> ListCapabilities
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemList) return false
        return items == other.items &&
            isScrollable == other.isScrollable &&
            showTertiaryText == other.showTertiaryText &&
            layoutMode == other.layoutMode
    }

    override fun hashCode(): Int {
        var result = items.hashCode()
        result = 31 * result + isScrollable.hashCode()
        result = 31 * result + showTertiaryText.hashCode()
        result = 31 * result + layoutMode.hashCode()
        return result
    }

    override fun toString(): String =
        "ItemList(items=$items, isScrollable=$isScrollable, showTertiaryText=$showTertiaryText, " +
            "layoutMode=$layoutMode)"
}
