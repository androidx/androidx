package androidx.ui.widgets.basic

import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.painting.alignment.Alignment
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.shiftedbox.RenderPositionedBox
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

// / A widget that aligns its child within itself and optionally sizes itself
// / based on the child's size.
// /
// / For example, to align a box at the bottom right, you would pass this box a
// / tight constraint that is bigger than the child's natural size,
// / with an alignment of [Alignment.bottomRight].
// /
// / This widget will be as big as possible if its dimensions are constrained and
// / [widthFactor] and [heightFactor] are null. If a dimension is unconstrained
// / and the corresponding size factor is null then the widget will match its
// / child's size in that dimension. If a size factor is non-null then the
// / corresponding dimension of this widget will be the product of the child's
// / dimension and the size factor. For example if widthFactor is 2.0 then
// / the width of this widget will always be twice its child's width.
// /
// / See also:
// /
// /  * [CustomSingleChildLayout], which uses a delegate to control the layout of
// /    a single child.
// /  * [Center], which is the same as [Align] but with the [alignment] always
// /    set to [Alignment.center].
// /  * [FractionallySizedBox], which sizes its child based on a fraction of its
// /    own size and positions the child according to an [Alignment] value.
// /  * The [catalog of layout widgets](https://flutter.io/widgets/layout/).
open class Align(
    key: Key,
        // / How to align the child.
        // /
        // / The x and y values of the [Alignment] control the horizontal and vertical
        // / alignment, respectively. An x value of -1.0 means that the left edge of
        // / the child is aligned with the left edge of the parent whereas an x value
        // / of 1.0 means that the right edge of the child is aligned with the right
        // / edge of the parent. Other values interpolate (and extrapolate) linearly.
        // / For example, a value of 0.0 means that the center of the child is aligned
        // / with the center of the parent.
        // /
        // / See also:
        // /
        // /  * [Alignment], which has more details and some convenience constants for
        // /    common positions.
        // /  * [AlignmentDirectional], which has a horizontal coordinate orientation
        // /    that depends on the [TextDirection].
    val alignment: Alignment = Alignment.center,
        // / If non-null, sets its width to the child's width multiplied by this factor.
        // /
        // / Can be both greater and less than 1.0 but must be positive.
    val widthFactor: Double?,
        // / If non-null, sets its height to the child's height multiplied by this factor.
        // /
        // / Can be both greater and less than 1.0 but must be positive.
    val heightFactor: Double?,
    child: Widget
) : SingleChildRenderObjectWidget(key = key, child = child) {

    init {
        assert(alignment != null)
        assert(widthFactor == null || widthFactor >= 0.0)
        assert(heightFactor == null || heightFactor >= 0.0)
    }

    override fun createRenderObject(context: BuildContext): RenderPositionedBox {
        return RenderPositionedBox(
                alignment = alignment,
                widthFactor = widthFactor,
                heightFactor = heightFactor,
                textDirection = Directionality.of(context)
        )
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject?) {
        renderObject as RenderPositionedBox
        renderObject.let {
            it.alignment = alignment
            it.widthFactor = widthFactor
            it.heightFactor = heightFactor
            it.textDirection = Directionality.of(context!!)
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("alignment", alignment))
        properties.add(DoubleProperty.create("widthFactor", widthFactor, defaultValue = null))
        properties.add(DoubleProperty.create("heightFactor", heightFactor, defaultValue = null))
    }
}