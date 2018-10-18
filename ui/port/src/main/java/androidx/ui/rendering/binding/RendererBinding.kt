package androidx.ui.rendering.binding

import androidx.ui.assert
import androidx.ui.developer.timeline.Timeline
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.window.Window
import androidx.ui.foundation.binding.BindingBase
import androidx.ui.foundation.timelineWhitelistArguments
import androidx.ui.gestures.hit_test.HitTestResult
import androidx.ui.gestures.hit_test.HitTestable
import androidx.ui.rendering.obj.PipelineOwner
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.obj.SemanticsHandle
import androidx.ui.rendering.view.RenderView
import androidx.ui.rendering.view.ViewConfiguration
import androidx.ui.scheduler.binding.SchedulerBinding
import androidx.ui.services.ServicesBinding
import androidx.ui.widgets.binding.WidgetsBinding
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch

interface RendererBinding : SchedulerBinding, ServicesBinding {
    fun drawFrame()

    fun performReassembleRenderer(superCall: () -> Deferred<Unit>): Deferred<Unit>

    val renderView: RenderView?

    val pipelineOwner: PipelineOwner?

    var renderingDrawFrameEnabled: Boolean
}

open class RendererMixinsWrapper(
    base: BindingBase,
    scheduler: SchedulerBinding
) : BindingBase by base, SchedulerBinding by scheduler

/**
 * The glue between the render tree and the Flutter engine.
 */
class RendererBindingImpl(
    private val window: Window,
    base: BindingBase,
    scheduler: SchedulerBinding
) : RendererMixinsWrapper(base, scheduler), RendererBinding, HitTestable {
/*with ServicesBinding*/

    /**
     * The render tree's owner, which maintains dirty state for layout,
     * composite, paint, and accessibility semantics
     */
    override var pipelineOwner: PipelineOwner? = null
        internal set

    init { // was initInstances
        pipelineOwner = PipelineOwner(
                onNeedVisualUpdate = { ensureVisualUpdate() },
                onSemanticsOwnerCreated = { _handleSemanticsOwnerCreated() },
                onSemanticsOwnerDisposed = { _handleSemanticsOwnerDisposed() }
        )

        launch(Unconfined) {
            window.onMetricsChanged.consumeEach { handleMetricsChanged() }
        }
        launch(Unconfined) {
            window.onTextScaleFactorChanged.consumeEach { handleTextScaleFactorChanged() }
        }
        launch(Unconfined) {
            window.onSemanticsEnabledChanged.consumeEach { handleSemanticsEnabledChanged() }
        }
        launch(Unconfined) {
            window.onSemanticsAction.consumeEach {
                TODO("Migration/Andrey): needs SemanticsAction")
//                handleSemanticsAction()
            }
        }

        initRenderView()
        handleSemanticsEnabledChanged()
        assert(renderView != null)
        // NOTE(Migration/Mihai): as we do not have working inheritance between bindings,
        // we are adding the frame callback in WidgetsBinding, to make sure
        // WidgetsBinding#drawFrame is called as well. That will call RendererBinding#drawFrame.
    }

    // was initServiceExtensions
    init {
        assert {
        // TODO("Migration/Andrey): do we need this debug logic?")
            // these service extensions only work in checked mode
//            registerBoolServiceExtension(
//                    name: 'debugPaint',
//            getter: () async => debugPaintSizeEnabled,
//            setter: (bool value) {
//            if (debugPaintSizeEnabled == value)
//                return new Future<Null>.value();
//            debugPaintSizeEnabled = value;
//            return _forceRepaint();
//        }
//            );
//            registerBoolServiceExtension(
//                    name: 'debugPaintBaselinesEnabled',
//            getter: () async => debugPaintBaselinesEnabled,
//            setter: (bool value) {
//            if (debugPaintBaselinesEnabled == value)
//                return new Future<Null>.value();
//            debugPaintBaselinesEnabled = value;
//            return _forceRepaint();
//        }
//            );
//            registerBoolServiceExtension(
//                    name: 'repaintRainbow',
//            getter: () async => debugRepaintRainbowEnabled,
//            setter: (bool value) {
//            final bool repaint = debugRepaintRainbowEnabled && !value;
//            debugRepaintRainbowEnabled = value;
//            if (repaint)
//                return _forceRepaint();
//            return new Future<Null>.value();
//        }
//            );
            true
        }

        // TODO("Migration/Andrey): do we need this debug logic?")
//        registerSignalServiceExtension(
//                name = "debugDumpRenderTree",
//        callback= async { debugDumpRenderTree(); return debugPrintDone; }
//        )
//
//        registerSignalServiceExtension(
//                name= "debugDumpLayerTree",
//        callback= () { debugDumpLayerTree(); return debugPrintDone; }
//        )
//
//        registerSignalServiceExtension(
//                name= "debugDumpSemanticsTreeInTraversalOrder",
//        callback= () { debugDumpSemanticsTree(DebugSemanticsDumpOrder.traversalOrder); return debugPrintDone; }
//        )
//
//        registerSignalServiceExtension(
//                name= 'debugDumpSemanticsTreeInInverseHitTestOrder',
//        callback= () { debugDumpSemanticsTree(DebugSemanticsDumpOrder.inverseHitTest); return debugPrintDone; }
//        )
    }

    /**
     * Creates a [RenderView] object to be the root of the
     * [RenderObject] rendering tree, and initializes it so that it
     * will be rendered when the engine is next ready to display a
     * frame.
     *
     * Called automatically when the binding is created.
     */
    fun initRenderView() {
        assert(renderView == null)
        renderView = RenderView(window, configuration = createViewConfiguration())
        renderView!!.scheduleInitialFrame()
    }

    /**
     * The render tree that's attached to the output surface.
     */
    override var renderView: RenderView? get() = pipelineOwner!!.rootNode as? RenderView
    /**
     * Sets the given [RenderView] object (which must not be null), and its tree, to
     * be the new render tree to display. The previous tree, if any, is detached.
     */
    internal set(value) {
        assert(value != null)
        assert(pipelineOwner != null)
        pipelineOwner!!.rootNode = value
    }

    /**
     * Called when the system metrics change.
     *
     * See [Window.onMetricsChanged].
     */
    private fun handleMetricsChanged() {
        assert(renderView != null)
        renderView!!.configuration = createViewConfiguration()
        scheduleForcedFrame()
    }

    /**
     * Called when the platform text scale factor changes.
     *
     * See [Window.onTextScaleFactorChanged].
     */
    private fun handleTextScaleFactorChanged() { }

    /**
     * Returns a [ViewConfiguration] configured for the [RenderView] based on the
     * current environment.
     *
     * This is called during construction and also in response to changes to the
     * system metrics.
     *
     * Bindings can override this method to change what size or device pixel
     * ratio the [RenderView] will use. For example, the testing framework uses
     * this to force the display into 800x600 when a test is run on the device
     * using `flutter run`.
     */
    fun createViewConfiguration(): ViewConfiguration {
        val devicePixelRatio: Double = window.devicePixelRatio
        // TODO(Migration/ njawad revisit window class as sizing is relative to CraneView
        // dimensions not the window size of the device
        return ViewConfiguration(
                size = window.physicalSize,
                devicePixelRatio = devicePixelRatio
        )
    }

    private var _semanticsHandle: SemanticsHandle? = null

    private fun handleSemanticsEnabledChanged() {
        setSemanticsEnabled(window.semanticsEnabled)
    }

    /**
     * Whether the render tree associated with this binding should produce a tree
     * of [SemanticsNode] objects.
     */
    fun setSemanticsEnabled(enabled: Boolean) {
        if (enabled) {
            if (_semanticsHandle == null) {
                assert(pipelineOwner != null)
                _semanticsHandle = pipelineOwner!!.ensureSemantics()
            }
        } else {
            _semanticsHandle?.dispose()
            _semanticsHandle = null
        }
    }

    // TODO(Migration/Andrey): needs SemanticsAction and ByteData
//    fun _handleSemanticsAction(id : Int, action : SemanticsAction, args : ByteData) {
//        _pipelineOwner.semanticsOwner?.performAction(
//                id,
//                action,
//                args != null ? const StandardMessageCodec().decodeMessage(args) : null,
//        );
//    }

    private fun _handleSemanticsOwnerCreated() {
        TODO("Migration/Andrey: needs semantics code from RenderOwner")
//        renderView.scheduleInitialSemantics();
    }

    // TODO(Migration/Andrey): needs semantics code from RenderOwner
    private fun _handleSemanticsOwnerDisposed() {
        TODO("Migration/Andrey: needs semantics code from RenderOwner")
//        renderView.clearSemantics();
    }

    /**
     * Pump the rendering pipeline to generate a frame.
     *
     * This method is called by [handleDrawFrame], which itself is called
     * automatically by the engine when when it is time to lay out and paint a
     * frame.
     *
     * Each frame consists of the following phases:
     *
     * 1. The animation phase: The [handleBeginFrame] method, which is registered
     * with [Window.onBeginFrame], invokes all the transient frame callbacks
     * registered with [scheduleFrameCallback], in registration order. This
     * includes all the [Ticker] instances that are driving [AnimationController]
     * objects, which means all of the active [Animation] objects tick at this
     * point.
     *
     * 2. Microtasks: After [handleBeginFrame] returns, any microtasks that got
     * scheduled by transient frame callbacks get to run. This typically includes
     * callbacks for futures from [Ticker]s and [AnimationController]s that
     * completed this frame.
     *
     * After [handleBeginFrame], [handleDrawFrame], which is registered with
     * [Window.onDrawFrame], is called, which invokes all the persistent frame
     * callbacks, of which the most notable is this method, [drawFrame], which
     * proceeds as follows:
     *
     * 3. The layout phase: All the dirty [RenderObject]s in the system are laid
     * out (see [RenderObject.performLayout]). See [RenderObject.markNeedsLayout]
     * for further details on marking an object dirty for layout.
     *
     * 4. The compositing bits phase: The compositing bits on any dirty
     * [RenderObject] objects are updated. See
     * [RenderObject.markNeedsCompositingBitsUpdate].
     *
     * 5. The paint phase: All the dirty [RenderObject]s in the system are
     * repainted (see [RenderObject.paint]). This generates the [Layer] tree. See
     * [RenderObject.markNeedsPaint] for further details on marking an object
     * dirty for paint.
     *
     * 6. The compositing phase: The layer tree is turned into a [Scene] and
     * sent to the GPU.
     *
     * 7. The semantics phase: All the dirty [RenderObject]s in the system have
     * their semantics updated (see [RenderObject.semanticsAnnotator]). This
     * generates the [SemanticsNode] tree. See
     * [RenderObject.markNeedsSemanticsUpdate] for further details on marking an
     * object dirty for semantics.
     *
     * For more details on steps 3-7, see [PipelineOwner].
     *
     * 8. The finalization phase: After [drawFrame] returns, [handleDrawFrame]
     * then invokes post-frame callbacks (registered with [addPostFrameCallback]).
     *
     * Some bindings (for example, the [WidgetsBinding]) add extra steps to this
     * list (for example, see [WidgetsBinding.drawFrame]).
     */
    //
    // When editing the above, also update widgets/binding.dart's copy.
    override fun drawFrame() {
        if (!renderingDrawFrameEnabled) {
            return
        }
        assert(renderView != null)
        assert(pipelineOwner != null)
        pipelineOwner!!.flushLayout()
        pipelineOwner!!.flushCompositingBits()
        pipelineOwner!!.flushPaint()
        renderView!!.compositeFrame() // this sends the bits to the GPU
        pipelineOwner!!.flushSemantics() // this also sends the semantics to the OS.
    }

    /**
     * Temporary switcher to be able to disable Flutter drawing logic when we want to use our own.
     */
    override var renderingDrawFrameEnabled: Boolean = true

    override fun performReassemble(): Deferred<Unit> {
        return performReassembleRenderer { super.performReassemble() }
    }

    override fun performReassembleRenderer(superCall: () -> Deferred<Unit>): Deferred<Unit> {
        return async {
            superCall().await()
            Timeline.startSync("Dirty Render Tree", timelineWhitelistArguments)
            try {
                renderView!!.reassemble()
            } finally {
                Timeline.finishSync()
            }
            scheduleWarmUpFrame()
            endOfFrame().await()
        }
    }

    override fun hitTest(result: HitTestResult, position: Offset) {
        assert(renderView != null)
        renderView!!.hitTest(result, position)
        // This is how Flutter did it, which is something we can't do.
        // // This super call is safe since it will be bound to a mixed-in declaration.
        // super.hitTest(result, position); // ignore: abstract_super_member_reference
    }

    private fun _forceRepaintVisitor(child: RenderObject) {
        child.markNeedsPaint()
        child.visitChildren { _forceRepaintVisitor(it) }
    }

    private fun _forceRepaint(): Deferred<Unit> {
        renderView?.visitChildren { _forceRepaintVisitor(it) }
        return endOfFrame()
    }
}