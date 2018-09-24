package androidx.ui.widgets.binding

import androidx.ui.engine.window.AppLifecycleState
import androidx.ui.engine.window.Locale
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

/**
 * Interface for classes that register with the Widgets layer binding.
 *
 * See [WidgetsBinding.addObserver] and [WidgetsBinding.removeObserver].
 *
 * This class can be extended directly, to get default behaviors for all of the
 * handlers, or can used with the `implements` keyword, in which case all the
 * handlers must be implemented (and the analyzer will list those that have
 * been omitted).
 *
 * ## Sample code
 *
 * This [StatefulWidget] implements the parts of the [State] and
 * [WidgetsBindingObserver] protocols necessary to react to application
 * lifecycle messages. See [didChangeAppLifecycleState].
 *
 * ```dart
 * class AppLifecycleReactor extends StatefulWidget {
 *   const AppLifecycleReactor({ Key key }) : super(key: key);
 *
 *   @override
 *   _AppLifecycleReactorState createState() => new _AppLifecycleReactorState();
 * }
 *
 * class _AppLifecycleReactorState extends State<AppLifecycleReactor> with WidgetsBindingObserver {
 *   @override
 *   void initState() {
 *     super.initState();
 *     WidgetsBinding.instance.addObserver(this);
 *   }
 *
 *   @override
 *   void dispose() {
 *     WidgetsBinding.instance.removeObserver(this);
 *     super.dispose();
 *   }
 *
 *   AppLifecycleState _notification;
 *
 *   @override
 *   void didChangeAppLifecycleState(AppLifecycleState state) {
 *     setState(() { _notification = state; });
 *   }
 *
 *   @override
 *   Widget build(BuildContext context) {
 *     return new Text('Last notification: $_notification');
 *   }
 * }
 * ```
 *
 * To respond to other notifications, replace the [didChangeAppLifecycleState]
 * method above with other methods from this class.
 */
abstract class WidgetsBindingObserver {
    /**
     * Called when the system tells the app to pop the current route.
     * For example, on Android, this is called when the user presses
     * the back button.
     *
     * Observers are notified in registration order until one returns
     * true. If none return true, the application quits.
     *
     * Observers are expected to return true if they were able to
     * handle the notification, for example by closing an active dialog
     * box, and false otherwise. The [WidgetsApp] widget uses this
     * mechanism to notify the [Navigator] widget that it should pop
     * its current route if possible.
     *
     * This method exposes the `popRoute` notification from
     * [SystemChannels.navigation].
     */
    fun didPopRoute(): Deferred<Boolean> {
        return async { false }
    }

    /**
     * Called when the host tells the app to push a new route onto the
     * navigator.
     *
     * Observers are expected to return true if they were able to
     * handle the notification. Observers are notified in registration
     * order until one returns true.
     *
     * This method exposes the `pushRoute` notification from
     * [SystemChannels.navigation].
     */
    fun didPushRoute(route: String): Deferred<Boolean> {
        return async { false }
    }

    /**
     * Called when the application's dimensions change. For example,
     * when a phone is rotated.
     *
     * This method exposes notifications from [Window.onMetricsChanged].
     *
     * ## Sample code
     *
     * This [StatefulWidget] implements the parts of the [State] and
     * [WidgetsBindingObserver] protocols necessary to react when the device is
     * rotated (or otherwise changes dimensions).
     *
     * ```dart
     * class MetricsReactor extends StatefulWidget {
     *   const MetricsReactor({ Key key }) : super(key: key);
     *
     *   @override
     *   _MetricsReactorState createState() => new _MetricsReactorState();
     * }
     *
     * class _MetricsReactorState extends State<MetricsReactor> with WidgetsBindingObserver {
     *   @override
     *   void initState() {
     *     super.initState();
     *     WidgetsBinding.instance.addObserver(this);
     *   }
     *
     *   @override
     *   void dispose() {
     *     WidgetsBinding.instance.removeObserver(this);
     *     super.dispose();
     *   }
     *
     *   Size _lastSize;
     *
     *   @override
     *   void didChangeMetrics() {
     *     setState(() { _lastSize = ui.window.physicalSize; });
     *   }
     *
     *   @override
     *   Widget build(BuildContext context) {
     *     return new Text('Current size: $_lastSize');
     *   }
     * }
     * ```
     *
     * In general, this is unnecessary as the layout system takes care of
     * automatically recomputing the application geometry when the application
     * size changes.
     *
     * See also:
     *
     *  * [MediaQuery.of], which provides a similar service with less
     *    boilerplate.
     */
    fun didChangeMetrics() { }

    /**
     * Called when the platform's text scale factor changes.
     *
     * This typically happens as the result of the user changing system
     * preferences, and it should affect all of the text sizes in the
     * application.
     *
     * This method exposes notifications from [Window.onTextScaleFactorChanged].
     *
     * ## Sample code
     *
     * ```dart
     * class TextScaleFactorReactor extends StatefulWidget {
     *   const TextScaleFactorReactor({ Key key }) : super(key: key);
     *
     *   @override
     *   _TextScaleFactorReactorState createState() => new _TextScaleFactorReactorState();
     * }
     *
     * class _TextScaleFactorReactorState extends State<TextScaleFactorReactor> with WidgetsBindingObserver {
     *   @override
     *   void initState() {
     *     super.initState();
     *     WidgetsBinding.instance.addObserver(this);
     *   }
     *
     *   @override
     *   void dispose() {
     *     WidgetsBinding.instance.removeObserver(this);
     *     super.dispose();
     *   }
     *
     *   double _lastTextScaleFactor;
     *
     *   @override
     *   void didChangeTextScaleFactor() {
     *     setState(() { _lastTextScaleFactor = ui.window.textScaleFactor; });
     *   }
     *
     *   @override
     *   Widget build(BuildContext context) {
     *     return new Text('Current scale factor: $_lastTextScaleFactor');
     *   }
     * }
     * ```
     *
     * See also:
     *
     *  * [MediaQuery.of], which provides a similar service with less
     *    boilerplate.
     */
    fun didChangeTextScaleFactor() { }

    /**
     * Called when the system tells the app that the user's locale has
     * changed. For example, if the user changes the system language
     * settings.
     *
     * This method exposes notifications from [Window.onLocaleChanged].
     */
    fun didChangeLocale(locale: Locale) { }

    /**
     * Called when the system puts the app in the background or returns
     * the app to the foreground.
     *
     * An example of implementing this method is provided in the class-level
     * documentation for the [WidgetsBindingObserver] class.
     *
     * This method exposes notifications from [SystemChannels.lifecycle].
     */
    fun didChangeAppLifecycleState(state: AppLifecycleState) { }

    /**
     * Called when the system is running low on memory.
     *
     * This method exposes the `memoryPressure` notification from
     * [SystemChannels.system].
     */
    fun didHaveMemoryPressure() { }
}
