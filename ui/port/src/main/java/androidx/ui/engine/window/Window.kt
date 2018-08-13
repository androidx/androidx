package androidx.ui.engine.window

import androidx.ui.VoidCallback
import androidx.ui.async.Zone
import androidx.ui.compositing.Scene
import androidx.ui.engine.geometry.Size
import androidx.ui.semantics.SemanticsUpdate

/**
 * The most basic interface to the host operating system's user interface.
 *
 * There is a single Window instance in the system, which you can
 * obtain from the [window] property.
 */
// TODO(Migration/Andrey): some methods are calling native code here(not ported)
object Window {

    /**
     * The number of device pixels for each logical pixel. This number might not
     * be a power of two. Indeed, it might not even be an integer. For example,
     * the Nexus 6 has a device pixel ratio of 3.5.
     *
     * Device pixels are also referred to as physical pixels. Logical pixels are
     * also referred to as device-independent or resolution-independent pixels.
     *
     * By definition, there are roughly 38 logical pixels per centimeter, or
     * about 96 logical pixels per inch, of the physical display. The value
     * returned by [devicePixelRatio] is ultimately obtained either from the
     * hardware itself, the device drivers, or a hard-coded value stored in the
     * operating system or firmware, and may be inaccurate, sometimes by a
     * significant margin.
     *
     * The Flutter framework operates in logical pixels, so it is rarely
     * necessary to directly deal with this property.
     *
     * When this changes, [onMetricsChanged] is called.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this value changes.
     */
    var devicePixelRatio: Double = 1.0
        internal set

    /**
     * The dimensions of the rectangle into which the application will be drawn,
     * in physical pixels.
     *
     * When this changes, [onMetricsChanged] is called.
     *
     * At startup, the size of the application window may not be known before Dart
     * code runs. If this value is observed early in the application lifecycle,
     * it may report [Size.zero].
     *
     * This value does not take into account any on-screen keyboards or other
     * system UI. The [padding] and [viewInsets] properties provide a view into
     * how much of each side of the application may be obscured by system UI.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this value changes.
     */
    var physicalSize: Size = Size.zero
        internal set

    /**
     * The number of physical pixels on each side of the display rectangle into
     * which the application can render, but over which the operating system
     * will likely place system UI, such as the keyboard, that fully obscures
     * any content.
     *
     * When this changes, [onMetricsChanged] is called.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this value changes.
     *  * [MediaQuery.of], a simpler mechanism for the same.
     *  * [Scaffold], which automatically applies the view insets in material
     *    design applications.
     */
    var viewInsets: WindowPadding = WindowPadding.zero
        internal set

    /**
     * The number of physical pixels on each side of the display rectangle into
     * which the application can render, but which may be partially obscured by
     * system UI (such as the system notification area), or or physical
     * intrusions in the display (e.g. overscan regions on television screens or
     * phone sensor housings).
     *
     * When this changes, [onMetricsChanged] is called.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this value changes.
     *  * [MediaQuery.of], a simpler mechanism for the same.
     *  * [Scaffold], which automatically applies the padding in material design
     *    applications.
     */
    var padding: WindowPadding = WindowPadding.zero
        internal set

    /**
     * A callback that is invoked whenever the [devicePixelRatio],
     * [physicalSize], [padding], or [viewInsets] values change, for example
     * when the device is rotated or when the application is resized (e.g. when
     * showing applications side-by-side on Android).
     *
     * The engine invokes this callback in the same zone in which the callback
     * was set.
     *
     * The framework registers with this callback and updates the layout
     * appropriately.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    register for notifications when this is called.
     *  * [MediaQuery.of], a simpler mechanism for the same.
     */
    private var onMetricsChangedZone: Zone? = null
    var onMetricsChanged: VoidCallback? = null
        internal set(value) {
            field = value
            onMetricsChangedZone = Zone.current
        }

    /**
     * The system-reported locale.
     *
     * This establishes the language and formatting conventions that application
     * should, if possible, use to render their user interface.
     *
     * The [onLocaleChanged] callback is called whenever this value changes.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this value changes.
     */
    var locale: Locale? = null
        internal set

    /**
     * A callback that is invoked whenever [locale] changes value.
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this callback is invoked.
     */
    private var onLocaleChangedZone: Zone? = null
    var onLocaleChanged: VoidCallback? = null
        internal set(value) {
            field = value
            onLocaleChangedZone = Zone.current
        }

    /**
     * The system-reported text scale.
     *
     * This establishes the text scaling factor to use when rendering text,
     * according to the user's platform preferences.
     *
     * The [onTextScaleFactorChanged] callback is called whenever this value
     * changes.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this value changes.
     */
    var textScaleFactor: Double = 1.0
        internal set

    /**
     * The setting indicating whether time should always be shown in the 24-hour
     * format.
     *
     * This option is used by [showTimePicker].
     */
    var alwaysUse24HourFormat: Boolean = false
        internal set

    /**
     * A callback that is invoked whenever [textScaleFactor] changes value.
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     *
     * See also:
     *
     *  * [WidgetsBindingObserver], for a mechanism at the widgets layer to
     *    observe when this callback is invoked.
     */
    private var onTextScaleFactorChangedZone: Zone? = null
    var onTextScaleFactorChanged: VoidCallback? = null
        internal set(value) {
            field = value
            onTextScaleFactorChangedZone = Zone.current
        }

    /**
     * A callback that is invoked to notify the application that it is an
     * appropriate time to provide a scene using the [SceneBuilder] API and the
     * [render] method. When possible, this is driven by the hardware VSync
     * signal. This is only called if [scheduleFrame] has been called since the
     * last time this callback was invoked.
     *
     * The [onDrawFrame] callback is invoked immediately after [onBeginFrame],
     * after draining any microtasks (e.g. completions of any [Future]s) queued
     * by the [onBeginFrame] handler.
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     *
     * See also:
     *
     *  * [SchedulerBinding], the Flutter framework class which manages the
     *    scheduling of frames.
     *  * [RendererBinding], the Flutter framework class which manages layout and
     *    painting.
     */
    private var onBeginFrameZone: Zone? = null
    var onBeginFrame: VoidCallback? = null
        internal set(value) {
            field = value
            onBeginFrameZone = Zone.current
        }

    /**
     * A callback that is invoked for each frame after [onBeginFrame] has
     * completed and after the microtask queue has been drained. This can be
     * used to implement a second phase of frame rendering that happens
     * after any deferred work queued by the [onBeginFrame] phase.
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     *
     * See also:
     *
     *  * [SchedulerBinding], the Flutter framework class which manages the
     *    scheduling of frames.
     *  * [RendererBinding], the Flutter framework class which manages layout and
     *    painting.
     */
    private var onDrawFrameZone: Zone? = null
    var onDrawFrame: VoidCallback? = null
        internal set(value) {
            field = value
            onDrawFrameZone = Zone.current
        }

    /**
     * A callback that is invoked when pointer data is available.
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     *
     * See also:
     *
     *  * [GestureBinding], the Flutter framework class which manages pointer
     *    events.
     */
    private var onPointerDataPacketZone: Zone? = null
    var onPointerDataPacket: VoidCallback? = null
        internal set(value) {
            field = value
            onPointerDataPacketZone = Zone.current
        }

    /**
     * The route or path that the embedder requested when the application was
     * launched.
     *
     * This will be the string "`/`" if no particular route was requested.
     *
     * ## Android
     *
     * On Android, calling
     * [`FlutterView.setInitialRoute`](/javadoc/io/flutter/view/FlutterView.html#setInitialRoute-java.lang.String-)
     * will set this value. The value must be set sufficiently early, i.e. before
     * the [runApp] call is executed in Dart, for this to have any effect on the
     * framework. The `createFlutterView` method in your `FlutterActivity`
     * subclass is a suitable time to set the value. The application's
     * `AndroidManifest.xml` file must also be updated to have a suitable
     * [`<intent-filter>`](https://developer.android.com/guide/topics/manifest/intent-filter-element.html).
     *
     * ## iOS
     *
     * On iOS, calling
     * [`FlutterViewController.setInitialRoute`](/objcdoc/Classes/FlutterViewController.html#/c:objc%28cs%29FlutterViewController%28im%29setInitialRoute:)
     * will set this value. The value must be set sufficiently early, i.e. before
     * the [runApp] call is executed in Dart, for this to have any effect on the
     * framework. The `application:didFinishLaunchingWithOptions:` method is a
     * suitable time to set this value.
     *
     * See also:
     *
     *  * [Navigator], a widget that handles routing.
     *  * [SystemChannels.navigation], which handles subsequent navigation
     *    requests from the embedder.
     */
    val defaultRouteName get() = {
        TODO()
        // native 'Window_defaultRouteName';
    }

    /**
     * Requests that, at the next appropriate opportunity, the [onBeginFrame]
     * and [onDrawFrame] callbacks be invoked.
     *
     * See also:
     *
     *  * [SchedulerBinding], the Flutter framework class which manages the
     *    scheduling of frames.
     */
    val scheduleFrame get() = {
        TODO()
        // native 'Window_scheduleFrame';
    }

    /**
     * Updates the application's rendering on the GPU with the newly provided
     * [Scene]. This function must be called within the scope of the
     * [onBeginFrame] or [onDrawFrame] callbacks being invoked. If this function
     * is called a second time during a single [onBeginFrame]/[onDrawFrame]
     * callback sequence or called outside the scope of those callbacks, the call
     * will be ignored.
     *
     * To record graphical operations, first create a [PictureRecorder], then
     * construct a [Canvas], passing that [PictureRecorder] to its constructor.
     * After issuing all the graphical operations, call the
     * [PictureRecorder.endRecording] function on the [PictureRecorder] to obtain
     * the final [Picture] that represents the issued graphical operations.
     *
     * Next, create a [SceneBuilder], and add the [Picture] to it using
     * [SceneBuilder.addPicture]. With the [SceneBuilder.build] method you can
     * then obtain a [Scene] object, which you can display to the user via this
     * [render] function.
     *
     * See also:
     *
     *  * [SchedulerBinding], the Flutter framework class which manages the
     *    scheduling of frames.
     *  * [RendererBinding], the Flutter framework class which manages layout and
     *    painting.
     */
    fun render(scene: Scene) = {
        TODO()
        // native 'Window_render';
    }

    /**
     * Whether the user has requested that [updateSemantics] be called when
     * the semantic contents of window changes.
     *
     * The [onSemanticsEnabledChanged] callback is called whenever this value
     * changes.
     */
    var semanticsEnabled: Boolean = false
        internal set

    /**
     * A callback that is invoked when the value of [semanticsEnabled] changes.
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     */
    private var onSemanticsEnabledChangedZone: Zone? = null
    var onSemanticsEnabledChanged: VoidCallback? = null
        internal set(value) {
            field = value
            onSemanticsEnabledChangedZone = Zone.current
        }

    /**
     * A callback that is invoked whenever the user requests an action to be
     * performed.
     *
     * This callback is used when the user expresses the action they wish to
     * perform based on the semantics supplied by [updateSemantics].
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     */
    private var onSemanticsActionZone: Zone? = null
    var onSemanticsAction: VoidCallback? = null
        internal set(value) {
            field = value
            onSemanticsActionZone = Zone.current
        }

    /**
     * Change the retained semantics data about this window.
     *
     * If [semanticsEnabled] is true, the user has requested that this funciton
     * be called whenever the semantic content of this window changes.
     *
     * In either case, this function disposes the given update, which means the
     * semantics update cannot be used further.
     */
    fun updateSemantics(update: SemanticsUpdate) = {
        TODO()
        // native 'Window_updateSemantics';
    }

    /**
     * Sends a message to a platform-specific plugin.
     *
     * The `name` parameter determines which plugin receives the message. The
     * `data` parameter contains the message payload and is typically UTF-8
     * encoded JSON but can be arbitrary data. If the plugin replies to the
     * message, `callback` will be called with the response.
     *
     * The framework invokes [callback] in the same zone in which this method
     * was called.
     */

    /**
     * Called whenever this window receives a message from a platform-specific
     * plugin.
     *
     * The `name` parameter determines which plugin sent the message. The `data`
     * parameter is the payload and is typically UTF-8 encoded JSON but can be
     * arbitrary data.
     *
     * Message handlers must call the function given in the `callback` parameter.
     * If the handler does not need to respond, the handler should pass null to
     * the callback.
     *
     * The framework invokes this callback in the same zone in which the
     * callback was set.
     */
    private var onPlatformMessageZone: Zone? = null
    var onPlatformMessage: VoidCallback? = null
        internal set(value) {
            field = value
            onPlatformMessageZone = Zone.current
        }

    // TODO(Migration/Andrey): needs ByteData and native code inside
//    /**
//     * Called by [_dispatchPlatformMessage].
//     */
//    internal fun respondToPlatformMessage(responseId: Int, data: Byte /*ByteData*/) {
//    native 'Window_respondToPlatformMessage';
//    }

    // TODO(Migration/Andrey): needs PlatformMessageResponseCallback, ByteData
//    void sendPlatformMessage(String name,
//    ByteData data,
//    PlatformMessageResponseCallback callback) {
//        final String error =
//                _sendPlatformMessage(name, _zonedPlatformMessageResponseCallback(callback), data);
//        if (error != null)
//            throw new Exception(error);
//    }
//    String _sendPlatformMessage(String name,
//    PlatformMessageResponseCallback callback,
//    ByteData data) native 'Window_sendPlatformMessage';

    // TODO(Migration/Andrey): needs PlatformMessageResponseCallback, ByteData and Zone.runUnaryGuarded
//    /// Wraps the given [callback] in another callback that ensures that the
//    /// original callback is called in the zone it was registered in.
//    internal fun zonedPlatformMessageResponseCallback(callback : PlatformMessageResponseCallback) : PlatformMessageResponseCallback {
//        if (callback == null)
//            return null;
//
//        // Store the zone in which the callback is being registered.
//        val registrationZone : Zone = Zone.current;
//
//        return (ByteData data) {
//            registrationZone.runUnaryGuarded(callback, data);
//        };
//    }
}