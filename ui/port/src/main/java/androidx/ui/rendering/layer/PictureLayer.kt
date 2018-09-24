package androidx.ui.rendering.layer

import androidx.ui.compositing.SceneBuilder
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.painting.Picture

/**
 * A composited layer containing a [Picture].
 *
 * Picture layers are always leaves in the layer tree.
 * Creates a leaf layer for the layer tree.
 */
class PictureLayer(
    /**
     * The bounds that were used for the canvas that drew this layer's [picture].
     *
     * This is purely advisory. It is included in the information dumped with
     * [debugDumpLayerTree] (which can be triggered by pressing "L" when using
     * "flutter run" at the console), which can help debug why certain drawing
     * commands are being culled.
     */
    private val canvasBounds: Rect?
) : Layer() {

    /**
     * The picture recorded for this layer.
     *
     * The picture's coordinate system matches this layer's coordinate system.
     *
     * The scene must be explicitly recomposited after this property is changed
     * (as described at [Layer]).
     */
    var picture: Picture? = null

    /**
     * Hints that the painting in this layer is complex and would benefit from
     * caching.
     *
     * If this hint is not set, the compositor will apply its own heuristics to
     * decide whether the this layer is complex enough to benefit from caching.
     *
     * The scene must be explicitly recomposited after this property is changed
     * (as described at [Layer]).
     */
    var isComplexHint = false

    /**
     * Hints that the painting in this layer is likely to change next frame.
     *
     * This hint tells the compositor not to cache this layer because the cache
     * will not be used in the future. If this hint is not set, the compositor
     * will apply its own heuristics to decide whether this layer is likely to be
     * reused in the future.
     *
     * The scene must be explicitly recomposited after this property is changed
     * (as described at [Layer]).
     */
    var willChangeHint = false

    override fun addToScene(builder: SceneBuilder, layerOffset: Offset) {
        builder.addPicture(layerOffset, picture!!, isComplexHint, willChangeHint)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create(name = "paint bounds", value = canvasBounds))
    }
}