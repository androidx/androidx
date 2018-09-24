package androidx.ui.engine.window

/**
 * A representation of distances for each of the four edges of a rectangle,
 * used to encode the view insets and padding that applications should place
 * around their user interface, as exposed by [Window.viewInsets] and
 * [Window.padding]. View insets and padding are preferrably read via
 * [MediaQuery.of].
 *
 * For a generic class that represents distances around a rectangle, see the
 * [EdgeInsets] class.
 *
 * See also:
 *
 *  * [WidgetsBindingObserver], for a widgets layer mechanism to receive
 *    notifications when the padding changes.
 *  * [MediaQuery.of], for the preferred mechanism for accessing these values.
 *  * [Scaffold], which automatically applies the padding in material design
 *    applications.
 */
data class WindowPadding(
        // The distance from the left edge to the first unpadded pixel, in physical pixels.
    val left: Double,
        // The distance from the top edge to the first unpadded pixel, in physical pixels.
    val top: Double,
        // The distance from the right edge to the first unpadded pixel, in physical pixels.
    val right: Double,
        // The distance from the bottom edge to the first unpadded pixel, in physical pixels.
    val bottom: Double
) {

    companion object {
        /** A window padding that has zeros for each edge. */
        val zero = WindowPadding(0.0, 0.0, 0.0, 0.0)
    }
}
