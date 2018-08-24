package androidx.ui.widgets.binding

import androidx.annotation.CallSuper
import androidx.ui.assert
import androidx.ui.engine.window.AppLifecycleState
import androidx.ui.engine.window.Locale
import androidx.ui.engine.window.Window
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.binding.BindingBase
import androidx.ui.foundation.binding.BindingBaseImpl
import androidx.ui.rendering.binding.RendererBinding
import androidx.ui.rendering.binding.RendererBindingImpl
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.RenderObjectWithChildMixin
import androidx.ui.scheduler.binding.SchedulerBinding
import androidx.ui.services.ServicesBinding
import androidx.ui.services.SystemChannels
import androidx.ui.services.SystemNavigator
import androidx.ui.widgets.framework.BuildOwner
import androidx.ui.widgets.framework.Element
import androidx.ui.widgets.framework.Widget
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch

interface WidgetsBinding : RendererBinding, SchedulerBinding, ServicesBinding

open class WidgetsMixinsWrapper(
    base: BindingBase,
    renderer: RendererBinding
) : BindingBase by base, RendererBinding by renderer

// TODO(Migration/popam): do mixins
object WidgetsBindingImpl : WidgetsMixinsWrapper(
        BindingBaseImpl,
        RendererBindingImpl
), WidgetsBinding /* with GestureBinding */ {

    // / The [BuildOwner] in charge of executing the build pipeline for the
    // / widget tree rooted at this binding.
    val buildOwner = BuildOwner()

    // was initInstances
    init {
        buildOwner.onBuildScheduled = ::_handleBuildScheduled
        launch(Unconfined) {
            Window.onLocaleChanged.consumeEach { handleLocaleChanged() }
        }
//        SystemChannels.navigation.setMethodCallHandler(_handleNavigationInvocation);
//        SystemChannels.system.setMessageHandler(_handleSystemMessage);

        // TODO(Migration/Andrey): we are subscribing to onTextScaleFactorChanged and
        // onMetricsChanged like in RenderingBindingImpl here as well and to lifecycle events
        // like in SchedulerBindingImpl as its impossible to override a method with the delegation
        // approach we are using for bindings.
        launch(Unconfined) {
            Window.onTextScaleFactorChanged.consumeEach { handleTextScaleFactorChanged() }
        }
        launch(Unconfined) {
            Window.onMetricsChanged.consumeEach { handleMetricsChanged() }
        }
        launch(Unconfined) {
            SystemChannels.lifecycle.consumeEach { handleAppLifecycleStateChanged(it) }
        }
    }

    // was initServiceExtensions
    init {
        registerSignalServiceExtension(
                "debugDumpApp",
                async {
                    debugDumpApp()
                    // TODO(migration/popam)
                    // debugPrintDone;
                }
        )

//        registerBoolServiceExtension(
//                name: 'showPerformanceOverlay',
//        getter: () => new Future<bool>.value(WidgetsApp.showPerformanceOverlayOverride),
//        setter: (bool value) {
//            if (WidgetsApp.showPerformanceOverlayOverride == value)
//                return new Future<Null>.value();
//            WidgetsApp.showPerformanceOverlayOverride = value;
//            return _forceRebuild();
//        }
//        );
//
//        registerBoolServiceExtension(
//                name: 'debugAllowBanner',
//        getter: () => new Future<bool>.value(WidgetsApp.debugAllowBannerOverride),
//        setter: (bool value) {
//            if (WidgetsApp.debugAllowBannerOverride == value)
//                return new Future<Null>.value();
//            WidgetsApp.debugAllowBannerOverride = value;
//            return _forceRebuild();
//        }
//        );
//
//        // This service extension is deprecated and will be removed by 7/1/2018.
//        // Use ext.flutter.inspector.show instead.
//        registerBoolServiceExtension(
//                name: 'debugWidgetInspector',
//        getter: () async => WidgetsApp.debugShowWidgetInspectorOverride,
//        setter: (bool value) {
//            if (WidgetsApp.debugShowWidgetInspectorOverride == value)
//                return new Future<Null>.value();
//            WidgetsApp.debugShowWidgetInspectorOverride = value;
//            return _forceRebuild();
//        }
//        );
//
//        WidgetInspectorService.instance.initServiceExtensions(registerServiceExtension);
    }

//    private fun _forceRebuild(): Deferred<Unit> {
//        if (renderViewElement != null) {
//            buildOwner.reassemble(renderViewElement);
//            return endOfFrame;
//        }
//        return async {};
//    }

    // / The object in charge of the focus tree.
    // /
    // / Rarely used directly. Instead, consider using [FocusScope.of] to obtain
    // / the [FocusScopeNode] for a given [BuildContext].
    // /
    // / See [FocusManager] for more details.
    val focusManager
        get() = buildOwner.focusManager

    val _observers = mutableListOf<WidgetsBindingObserver>()

    // / Registers the given object as a binding observer. Binding
    // / observers are notified when various application events occur,
    // / for example when the system locale changes. Generally, one
    // / widget in the widget tree registers itself as a binding
    // / observer, and converts the system state into inherited widgets.
    // /
    // / For example, the [WidgetsApp] widget registers as a binding
    // / observer and passes the screen size to a [MediaQuery] widget
    // / each time it is built, which enables other widgets to use the
    // / [MediaQuery.of] static method and (implicitly) the
    // / [InheritedWidget] mechanism to be notified whenever the screen
    // / size changes (e.g. whenever the screen rotates).
    // /
    // / See also:
    // /
    // /  * [removeObserver], to release the resources reserved by this method.
    // /  * [WidgetsBindingObserver], which has an example of using this method.
    fun addObserver(observer: WidgetsBindingObserver) = _observers.add(observer)

    // / Unregisters the given observer. This should be used sparingly as
    // / it is relatively expensive (O(N) in the number of registered
    // / observers).
    // /
    // / See also:
    // /
    // /  * [addObserver], for the method that adds observers in the first place.
    // /  * [WidgetsBindingObserver], which has an example of using this method.
    fun removeObserver(observer: WidgetsBindingObserver): Boolean = _observers.remove(observer)

    private fun handleMetricsChanged() {
        for (observer in _observers) {
            observer.didChangeMetrics()
        }
    }

    private fun handleTextScaleFactorChanged() {
        for (observer in _observers) {
            observer.didChangeTextScaleFactor()
        }
    }

    // / Called when the system locale changes.
    // /
    // / Calls [dispatchLocaleChanged] to notify the binding observers.
    // /
    // / See [Window.onLocaleChanged].
    @CallSuper
    internal fun handleLocaleChanged() {
        TODO("migration/popam/Implement this")
//        dispatchLocaleChanged(ui.window.locale);
    }

    // / Notify all the observers that the locale has changed (using
    // / [WidgetsBindingObserver.didChangeLocale]), giving them the
    // / `locale` argument.
    // /
    // / This is called by [handleLocaleChanged] when the [Window.onLocaleChanged]
    // / notification is received.
    @CallSuper
    internal fun dispatchLocaleChanged(locale: Locale) {
        for (observer in _observers) {
            observer.didChangeLocale(locale)
        }
    }

    // / Called when the system pops the current route.
    // /
    // / This first notifies the binding observers (using
    // / [WidgetsBindingObserver.didPopRoute]), in registration order, until one
    // / returns true, meaning that it was able to handle the request (e.g. by
    // / closing a dialog box). If none return true, then the application is shut
    // / down by calling [SystemNavigator.pop].
    // /
    // / [WidgetsApp] uses this in conjunction with a [Navigator] to
    // / cause the back button to close dialog boxes, return from modal
    // / pages, and so forth.
    // /
    // / This method exposes the `popRoute` notification from
    // / [SystemChannels.navigation].
    internal fun handlePopRoute(): Deferred<Unit> {
        return async {
            for (observer in _observers.toList()) {
                if (observer.didPopRoute().await()) {
                    return@async
                }
            }
            SystemNavigator.pop()
        }
    }

    // / Called when the host tells the app to push a new route onto the
    // / navigator.
    // /
    // / This notifies the binding observers (using
    // / [WidgetsBindingObserver.didPushRoute]), in registration order, until one
    // / returns true, meaning that it was able to handle the request (e.g. by
    // / opening a dialog box). If none return true, then nothing happens.
    // /
    // / This method exposes the `pushRoute` notification from
    // / [SystemChannels.navigation].
    @CallSuper
    internal fun handlePushRoute(route: String): Deferred<Unit> {
        return async {
            for (observer in _observers.toList()) {
                if (observer.didPushRoute(route).await()) {
                    return@async
                }
            }
        }
    }

//    Future<dynamic> _handleNavigationInvocation(MethodCall methodCall) {
//        switch (methodCall.method) {
//            case 'popRoute':
//            return handlePopRoute();
//            case 'pushRoute':
//            return handlePushRoute(methodCall.arguments);
//        }
//        return new Future<Null>.value();
//    }

    private fun handleAppLifecycleStateChanged(state: AppLifecycleState) {
        for (observer in _observers) {
            observer.didChangeAppLifecycleState(state)
        }
    }

    // / Called when the operating system notifies the application of a memory
    // / pressure situation.
    // /
    // / Notifies all the observers using
    // / [WidgetsBindingObserver.didHaveMemoryPressure].
    // /
    // / This method exposes the `memoryPressure` notification from
    // / [SystemChannels.system].
    fun handleMemoryPressure() {
        for (observer in _observers) {
            observer.didHaveMemoryPressure()
        }
    }

//    fun _handleSystemMessage(systemMessage: Any): Deferred<Unit> {
//        val message = systemMessage as Map<String, Any?>;
//        val type = message["type"] as String;
//        when (type) {
//            "memoryPressure" -> handleMemoryPressure();
//            else -> break;
//        }
//        return null;
//    }

    var _needToReportFirstFrame = true
    var _deferFirstFrameReportCount = 0
    val _reportFirstFrame
        get() = _deferFirstFrameReportCount == 0

    // / Whether the first frame has finished rendering.
    // /
    // / Only valid in profile and debug builds, it can't be used in release
    // / builds.
    // / It can be deferred using [deferFirstFrameReport] and
    // / [allowFirstFrameReport].
    // / The value is set at the end of the call to [drawFrame].
    val debugDidSendFirstFrameEvent
        get() = !_needToReportFirstFrame

    // / Tell the framework not to report the frame it is building as a "useful"
    // / first frame until there is a corresponding call to [allowFirstFrameReport].
    // /
    // / This is used by [WidgetsApp] to report the first frame.
    //
    // TODO(ianh): This method should only be available in debug and profile modes.
    fun deferFirstFrameReport() {
        assert(_deferFirstFrameReportCount >= 0)
        _deferFirstFrameReportCount += 1
    }

    // / When called after [deferFirstFrameReport]: tell the framework to report
    // / the frame it is building as a "useful" first frame.
    // /
    // / This method may only be called once for each corresponding call
    // / to [deferFirstFrameReport].
    // /
    // / This is used by [WidgetsApp] to report the first frame.
    //
    // TODO(ianh): This method should only be available in debug and profile modes.
    fun allowFirstFrameReport() {
        assert(_deferFirstFrameReportCount >= 1)
        _deferFirstFrameReportCount -= 1
    }

    fun _handleBuildScheduled() {
        // If we're in the process of building dirty elements, then changes
        // should not trigger a new frame.
        assert {
            if (debugBuildingDirtyElements) {
                throw FlutterError(
                        "Build scheduled during frame.\n" +
                "While the widget tree was being built, laid out, and painted, " +
                "a new frame was scheduled to rebuild the widget tree. " +
                "This might be because setState() was called from a layout or " +
                "paint callback. " +
                "If a change is needed to the widget tree, it should be applied " +
                "as the tree is being built. Scheduling a change for the subsequent " +
                "frame instead results in an interface that lags behind by one frame. " +
                "If this was done to make your build dependent on a size measured at " +
                "layout time, consider using a LayoutBuilder, CustomSingleChildLayout, " +
                "or CustomMultiChildLayout. If, on the other hand, the one frame delay " +
                "is the desired effect, for example because this is an " +
                "animation, consider scheduling the frame in a post-frame callback " +
                "using SchedulerBinding.addPostFrameCallback or " +
                "using an AnimationController to trigger the animation."
                )
            }
            true
            }
        ensureVisualUpdate()
    }

    // / Whether we are currently in a frame. This is used to verify
    // / that frames are not scheduled redundantly.
    // /
    // / This is public so that test frameworks can change it.
    // /
    // / This flag is not used in release builds.
    internal var debugBuildingDirtyElements = false

    // / Pump the build and rendering pipeline to generate a frame.
    // /
    // / This method is called by [handleDrawFrame], which itself is called
    // / automatically by the engine when when it is time to lay out and paint a
    // / frame.
    // /
    // / Each frame consists of the following phases:
    // /
    // / 1. The animation phase: The [handleBeginFrame] method, which is registered
    // / with [Window.onBeginFrame], invokes all the transient frame callbacks
    // / registered with [scheduleFrameCallback], in
    // / registration order. This includes all the [Ticker] instances that are
    // / driving [AnimationController] objects, which means all of the active
    // / [Animation] objects tick at this point.
    // /
    // / 2. Microtasks: After [handleBeginFrame] returns, any microtasks that got
    // / scheduled by transient frame callbacks get to run. This typically includes
    // / callbacks for futures from [Ticker]s and [AnimationController]s that
    // / completed this frame.
    // /
    // / After [handleBeginFrame], [handleDrawFrame], which is registered with
    // / [Window.onDrawFrame], is called, which invokes all the persistent frame
    // / callbacks, of which the most notable is this method, [drawFrame], which
    // / proceeds as follows:
    // /
    // / 3. The build phase: All the dirty [Element]s in the widget tree are
    // / rebuilt (see [State.build]). See [State.setState] for further details on
    // / marking a widget dirty for building. See [BuildOwner] for more information
    // / on this step.
    // /
    // / 4. The layout phase: All the dirty [RenderObject]s in the system are laid
    // / out (see [RenderObject.performLayout]). See [RenderObject.markNeedsLayout]
    // / for further details on marking an object dirty for layout.
    // /
    // / 5. The compositing bits phase: The compositing bits on any dirty
    // / [RenderObject] objects are updated. See
    // / [RenderObject.markNeedsCompositingBitsUpdate].
    // /
    // / 6. The paint phase: All the dirty [RenderObject]s in the system are
    // / repainted (see [RenderObject.paint]). This generates the [Layer] tree. See
    // / [RenderObject.markNeedsPaint] for further details on marking an object
    // / dirty for paint.
    // /
    // / 7. The compositing phase: The layer tree is turned into a [Scene] and
    // / sent to the GPU.
    // /
    // / 8. The semantics phase: All the dirty [RenderObject]s in the system have
    // / their semantics updated (see [RenderObject.semanticsAnnotator]). This
    // / generates the [SemanticsNode] tree. See
    // / [RenderObject.markNeedsSemanticsUpdate] for further details on marking an
    // / object dirty for semantics.
    // /
    // / For more details on steps 4-8, see [PipelineOwner].
    // /
    // / 9. The finalization phase in the widgets layer: The widgets tree is
    // / finalized. This causes [State.dispose] to be invoked on any objects that
    // / were removed from the widgets tree this frame. See
    // / [BuildOwner.finalizeTree] for more details.
    // /
    // / 10. The finalization phase in the scheduler layer: After [drawFrame]
    // / returns, [handleDrawFrame] then invokes post-frame callbacks (registered
    // / with [addPostFrameCallback]).
    //
    // When editing the above, also update rendering/binding.dart's copy.
    override fun drawFrame() {
        assert(!debugBuildingDirtyElements)
        assert({
            debugBuildingDirtyElements = true
            true
        })
        try {
            if (renderViewElement != null)
                buildOwner.buildScope(renderViewElement!!, null)
            super.drawFrame()
            buildOwner.finalizeTree()
        } finally {
            assert({
                debugBuildingDirtyElements = false
                true
            })
        }
        // TODO(ianh): Following code should not be included in release mode, only profile and debug modes.
        // See https://github.com/dart-lang/sdk/issues/27192
        if (_needToReportFirstFrame && _reportFirstFrame) {
            TODO("migration/popam/Implement this")
//            developer.Timeline.instantSync('Widgets completed first useful frame');
//            developer.postEvent('Flutter.FirstFrame', <String, dynamic>{});
            _needToReportFirstFrame = false
        }
    }

    // / The [Element] that is at the root of the hierarchy (and which wraps the
    // / [RenderView] object at the root of the rendering hierarchy).
    // /
    // / This is initialized the first time [runApp] is called.
    var _renderViewElement: Element? = null
    val renderViewElement
        get() = _renderViewElement

    // / Takes a widget and attaches it to the [renderViewElement], creating it if
    // / necessary.
    // /
    // / This is called by [runApp] to configure the widget tree.
    // /
    // / See also [RenderObjectToWidgetAdapter.attachToRenderTree].
    fun attachRootWidget(rootWidget: Widget) {
        // TODO(migration/popam): complete this
        _renderViewElement = RenderObjectToWidgetAdapter<RenderBox>(
                // TODO(Migration/Filip): I'm forced to cast it here
                container = RendererBindingImpl.renderView as RenderObjectWithChildMixin<RenderBox>,
                debugShortDescription = "[root]",
                child = rootWidget
                // TODO(Migration/Filip): I'm forced to cast it here
        ).attachToRenderTree(buildOwner,
                renderViewElement as RenderObjectToWidgetElement<RenderBox>?)
    }

    override fun performReassemble(): Deferred<Unit> {
        deferFirstFrameReport()
        if (renderViewElement != null)
            buildOwner.reassemble(renderViewElement as Element)
        // TODO(hansmuller): eliminate the value variable after analyzer bug
        // https://github.com/flutter/flutter/issues/11646 is fixed.
        val value = performReassembleRenderer { // renderer first
            super.performReassemble() // and then base
        }
        return async {
            value.await()
            allowFirstFrameReport()
        }
    }
}

// TODO(migration/popam)
fun debugDumpApp() {
}
