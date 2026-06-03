package androidx.compose.ui.layout

/**
 * Interface holding the size and alignment lines of the measured layout, as well as the children
 * positioning logic. [placeChildren] is the function used for positioning children.
 * [Placeable.placeAt] should be called on children inside [placeChildren]. The alignment lines can
 * be used by the parent layouts to decide layout, and can be queried using the [Placeable.get]
 * operator. Note that alignment lines will be inherited by parent layouts, such that indirect
 * parents will be able to query them as well.
 */
interface MeasureResult {
    /** The measured width of the layout, in pixels. */
    val width: Int

    /** The measured height of the layout, in pixels. */
    val height: Int

    /**
     * Alignment lines that can be used by parents to align this layout. This only includes the
     * alignment lines of this layout and not children.
     */
    val alignmentLines: Map<AlignmentLine, Int>

    /**
     * An optional lambda function used to create [Ruler]s for child layout. This may be
     * reevealuated when the layout's position moves.
     */
    val rulers: (RulerScope.() -> Unit)?
        get() = null

    /**
     * Works in conjunction with [rulerProvider] to [provide][RulerScope.provides] Ruler values
     * individually. A return value of `true` indicates that [rulerProvider] might be able to
     * provide a value for the passed-in [Ruler]. A value of `false` means it can never provide the
     * value.
     */
    val isRulerProvided: ((Ruler) -> Boolean)?
        get() = null

    /**
     * A lambda that can [provide][RulerScope.provides] [Ruler] values. When [isRulerProvided]
     * returns `true` for a [Ruler], [rulerProvider] will be called to provide its value.
     * [rulerProvider] can choose not to provide the value if it isn't available. It can also
     * provide more [Ruler] values if it is convenient to provide them. For example, it may be
     * convenient to provide all values for a [RectRulers] when one is provided.
     */
    val rulerProvider: (RulerScope.(Ruler) -> Unit)?
        get() = null

    /**
     * A method used to place children of this layout. It may also be used to measure children that
     * were not needed for determining the size of this layout.
     */
    fun placeChildren()
}
