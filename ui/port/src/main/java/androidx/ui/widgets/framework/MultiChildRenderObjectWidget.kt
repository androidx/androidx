package androidx.ui.widgets.framework

import androidx.ui.foundation.Key

/**
 * A superclass for RenderObjectWidgets that configure RenderObject subclasses
 * that have a single list of children. (This superclass only provides the
 * storage for that child list, it doesn't actually provide the updating
 * logic.)
 */
abstract class MultiChildRenderObjectWidget(
    key: Key,
    /**
     * The widgets below this widget in the tree.
     *
     * If this list is going to be mutated, it is usually wise to put [Key]s on
     * the widgets, so that the framework can match old configurations to new
     * configurations and maintain the underlying render objects.
     */
    val children: List<Widget>
) : RenderObjectWidget(key) {

    init {
        assert(!children.any { it == null }) // https://github.com/dart-lang/sdk/issues/29276
    }

    override fun createElement(): MultiChildRenderObjectElement =
            MultiChildRenderObjectElement(this)
}
