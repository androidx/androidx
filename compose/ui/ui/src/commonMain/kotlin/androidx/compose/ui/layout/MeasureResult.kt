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
    val alignmentLines: Map<out AlignmentLine, Int>

    /**
     * An optional lambda function used to create [Ruler]s for child layout. This may be
     * reevealuated when the layout's position moves.
     */
    val rulers: (RulerScope.() -> Unit)?
        get() = null

    /**
     * A method used to place children of this layout. It may also be used to measure children that
     * were not needed for determining the size of this layout.
     */
    fun placeChildren()
}
