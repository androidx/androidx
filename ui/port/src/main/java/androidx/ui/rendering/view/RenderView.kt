package androidx.ui.rendering.view

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.painting.matrixutils.transformRect
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.layer.OffsetLayer
import androidx.ui.rendering.layer.TransformLayer
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.obj.RenderObjectWithChildMixin
import androidx.ui.vectormath64.Matrix4

// / The root of the render tree.
// /
// / The view represents the total output surface of the render tree and handles
// / bootstrapping the rendering pipeline. The view has a unique child
// / [RenderBox], which is required to fill the entire output surface.

// / Creates the root of the render tree.
// /
// / Typically created by the binding (e.g., [RendererBinding]).
class RenderView(
    child: RenderBox? = null,
    configuration: ViewConfiguration
) : RenderObjectWithChildMixin<RenderBox>() {

    // / The current layout size of the view.
    var size: Size = Size.zero
        private set

    // / The constraints used for the root layout.
    var configuration: ViewConfiguration = configuration
    // / The configuration is initially set by the `configuration` argument
    // / passed to the constructor.
    // /
    // / Always call [scheduleInitialFrame] before changing the configuration.
        private set(value) {
            if (field == value)
                return
            field = value
            replaceRootLayer(_updateMatricesAndCreateNewRootLayer())
            assert(_rootTransform != null)
            markNeedsLayout()
        }

    // / Bootstrap the rendering pipeline by scheduling the first frame.
    // /
    // / This should only be called once, and must be called before changing
    // / [configuration]. It is typically called immediately after calling the
    // / constructor.
    fun scheduleInitialFrame() {
        assert(owner != null)
        assert(_rootTransform == null)
        scheduleInitialLayout()
        scheduleInitialPaint(_updateMatricesAndCreateNewRootLayer())
        assert(_rootTransform != null)
        owner!!.requestVisualUpdate()
    }

    private var _rootTransform: Matrix4? = null

    private fun _updateMatricesAndCreateNewRootLayer(): OffsetLayer {
        val matrix: Matrix4 = configuration.toMatrix()
        _rootTransform = matrix
        val rootLayer = TransformLayer(transform = matrix)
        rootLayer.attach(this)
        assert(_rootTransform != null)
        return rootLayer
    }

    // We never call layout() on this class, so this should never get
    // checked. (This class is laid out using scheduleInitialLayout().)
    override fun debugAssertDoesMeetConstraints() { assert(false); }

    override fun performResize() {
        assert(false)
    }

    override fun performLayout() {
        assert(_rootTransform != null)
        size = configuration.size
        assert(size.isFinite())

        child?.layout(BoxConstraints.tight(size))
    }

    // TODO(Migration/andrey): Needs Duration. And comment int RenderObject saying rotation is not implemented yet.
//    override fun rotate(oldAngle: Int, newAngle: Int, time: Duration?) {
//        assert(false); // nobody tells the screen to rotate, the whole rotate() dance is started from our performResize()
//    }

    // TODO(Migration/andrey): Needs HitTestResult
//    /// Determines the set of render objects located at the given position.
//    ///
//    /// Returns true if the given point is contained in this render object or one
//    /// of its descendants. Adds any render objects that contain the point to the
//    /// given hit test result.
//    ///
//    /// The [position] argument is in the coordinate system of the render view,
//    /// which is to say, in logical pixels. This is not necessarily the same
//    /// coordinate system as that expected by the root [Layer], which will
//    /// normally be in physical (device) pixels.
//    fun hitTest(result : HitTestResult, position : Offset? = null) : Boolean {
//        if (child != null)
//            child.hitTest(result, position: position);
//        result.add(new HitTestEntry(this));
//        return true;
//    }

    override var isRepaintBoundary: Boolean = true

    override fun paint(context: PaintingContext, offset: Offset) {
        child?.let { context.paintChild(it, offset) }
    }

    override fun applyPaintTransform(child: RenderObject, transform: Matrix4) {
        assert(_rootTransform != null)
        transform.multiply(_rootTransform)
        super.applyPaintTransform(child, transform)
    }

    // TODO(Migration/andrey): Needs Timeline
//    /// Uploads the composited layer tree to the engine.
//    ///
//    /// Actually causes the output of the rendering pipeline to appear on screen.
//    fun compositeFrame() {
//        Timeline.startSync('Compositing', arguments = timelineWhitelistArguments);
//        try {
//            val builder : ui.SceneBuilder = ui.SceneBuilder();
//            layer.addToScene(builder, Offset.zero);
//            val scene : ui.Scene = builder.build();
//            ui.window.render(scene);
//            scene.dispose();
//            assert {
//                if (debugRepaintRainbowEnabled || debugRepaintTextRainbowEnabled)
//                    debugCurrentRepaintColor = debugCurrentRepaintColor.withHue(debugCurrentRepaintColor.hue + 2.0);
//                true;
//            }
//        } finally {
//            Timeline.finishSync();
//        }
//    }

    override val paintBounds: Rect? = Offset.zero.and(size * configuration.devicePixelRatio)

    override val semanticBounds: Rect
        get() {
            assert(_rootTransform != null)
            return _rootTransform!!.transformRect(Offset.zero.and(size))
        }

    // TODO(Migration/andrey): probably not important for us
//    @override
//    void debugFillProperties(DiagnosticPropertiesBuilder properties) {
//        // call to ${super.debugFillProperties(description)} is omitted because the
//        // root superclasses don't include any interesting information for this
//        // class
//        assert(() {
//            properties.add(new DiagnosticsNode.message('debug mode enabled - ${Platform.operatingSystem}'));
//            return true;
//        }());
//        properties.add(new DiagnosticsProperty<Size>('window size', ui.window.physicalSize, tooltip: 'in physical pixels'));
//        properties.add(new DoubleProperty('device pixel ratio', ui.window.devicePixelRatio, tooltip: 'physical pixels per logical pixel'));
//        properties.add(new DiagnosticsProperty<ViewConfiguration>('configuration', configuration, tooltip: 'in logical pixels'));
//        if (ui.window.semanticsEnabled)
//            properties.add(new DiagnosticsNode.message('semantics enabled'));
//    }
}
