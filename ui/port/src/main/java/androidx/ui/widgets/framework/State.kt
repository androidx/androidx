package androidx.ui.widgets.framework

import androidx.annotation.CallSuper
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.Diagnosticable
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.ObjectFlagProperty

/// The logic and internal state for a [StatefulWidget].
///
/// State is information that (1) can be read synchronously when the widget is
/// built and (2) might change during the lifetime of the widget. It is the
/// responsibility of the widget implementer to ensure that the [State] is
/// promptly notified when such state changes, using [State.setState].
///
/// [State] objects are created by the framework by calling the
/// [StatefulWidget.createState] method when inflating a [StatefulWidget] to
/// insert it into the tree. Because a given [StatefulWidget] instance can be
/// inflated multiple times (e.g., the widget is incorporated into the tree in
/// multiple places at once), there might be more than one [State] object
/// associated with a given [StatefulWidget] instance. Similarly, if a
/// [StatefulWidget] is removed from the tree and later inserted in to the tree
/// again, the framework will call [StatefulWidget.createState] again to create
/// a fresh [State] object, simplifying the lifecycle of [State] objects.
///
/// [State] objects have the following lifecycle:
///
///  * The framework creates a [State] object by calling
///    [StatefulWidget.createState].
///  * The newly created [State] object is associated with a [BuildContext].
///    This association is permanent: the [State] object will never change its
///    [BuildContext]. However, the [BuildContext] itself can be moved around
///    the tree along with its subtree. At this point, the [State] object is
///    considered [mounted].
///  * The framework calls [initState]. Subclasses of [State] should override
///    [initState] to perform one-time initialization that depends on the
///    [BuildContext] or the widget, which are available as the [context] and
///    [widget] properties, respectively, when the [initState] method is
///    called.
///  * The framework calls [didChangeDependencies]. Subclasses of [State] should
///    override [didChangeDependencies] to perform initialization involving
///    [InheritedWidget]s. If [BuildContext.inheritFromWidgetOfExactType] is
///    called, the [didChangeDependencies] method will be called again if the
///    inherited widgets subsequently change or if the widget moves in the tree.
///  * At this point, the [State] object is fully initialized and the framework
///    might call its [build] method any number of times to obtain a
///    description of the user interface for this subtree. [State] objects can
///    spontaneously request to rebuild their subtree by callings their
///    [setState] method, which indicates that some of their internal state
///    has changed in a way that might impact the user interface in this
///    subtree.
///  * During this time, a parent widget might rebuild and request that this
///    location in the tree update to display a new widget with the same
///    [runtimeType] and [Widget.key]. When this happens, the framework will
///    update the [widget] property to refer to the new widget and then call the
///    [didUpdateWidget] method with the previous widget as an argument. [State]
///    objects should override [didUpdateWidget] to respond to changes in their
///    associated widget (e.g., to start implicit animations). The framework
///    always calls [build] after calling [didUpdateWidget], which means any
///    calls to [setState] in [didUpdateWidget] are redundant.
///  * During development, if a hot reload occurs (whether initiated from the
///    command line `flutter` tool by pressing `r`, or from an IDE), the
///    [reassemble] method is called. This provides an opportunity to
///    reinitialize any data that was prepared in the [initState] method.
///  * If the subtree containing the [State] object is removed from the tree
///    (e.g., because the parent built a widget with a different [runtimeType]
///    or [Widget.key]), the framework calls the [deactivate] method. Subclasses
///    should override this method to clean up any links between this object
///    and other elements in the tree (e.g. if you have provided an ancestor
///    with a pointer to a descendant's [RenderObject]).
///  * At this point, the framework might reinsert this subtree into another
///    part of the tree. If that happens, the framework will ensure that it
///    calls [build] to give the [State] object a chance to adapt to its new
///    location in the tree. If the framework does reinsert this subtree, it
///    will do so before the end of the animation frame in which the subtree was
///    removed from the tree. For this reason, [State] objects can defer
///    releasing most resources until the framework calls their [dispose]
///    method.
///  * If the framework does not reinsert this subtree by the end of the current
///    animation frame, the framework will call [dispose], which indicates that
///    this [State] object will never build again. Subclasses should override
///    this method to release any resources retained by this object (e.g.,
///    stop any active animations).
///  * After the framework calls [dispose], the [State] object is considered
///    unmounted and the [mounted] property is false. It is an error to call
///    [setState] at this point. This stage of the lifecycle is terminal: there
///    is no way to remount a [State] object that has been disposed.
///
/// See also:
///
///  * [StatefulWidget], where the current configuration of a [State] is hosted,
///    and whose documentation has sample code for [State].
///  * [StatelessWidget], for widgets that always build the same way given a
///    particular configuration and ambient state.
///  * [InheritedWidget], for widgets that introduce ambient state that can
///    be read by descendant widgets.
///  * [Widget], for an overview of widgets in general.
abstract class State<T : StatefulWidget>(
        /// The current configuration.
        ///
        /// A [State] object's configuration is the corresponding [StatefulWidget]
        /// instance. This property is initialized by the framework before calling
        /// [initState]. If the parent updates this location in the tree to a new
        /// widget with the same [runtimeType] and [Widget.key] as the current
        /// configuration, the framework will update this property to refer to the new
        /// widget and then call [didUpdateWidget], passing the old configuration as
        /// an argument.
        internal var widget: T
) : Diagnosticable() {

    /// The current stage in the lifecycle for this state object.
    ///
    /// This field is used by the framework when asserts are enabled to verify
    /// that [State] objects move through their lifecycle in an orderly fashion.
    var _debugLifecycleState: _StateLifecycle = _StateLifecycle.created;

    /// Verifies that the [State] that was created is one that expects to be
    /// created for that particular [Widget].
    // TODO(Migration/Filip): We cannot implement this :(
    // private fun _debugTypesAreRight(widget: Widget): Boolean = widget is T;
    internal fun _debugTypesAreRight(widget: Widget): Boolean = true

    /// The location in the tree where this widget builds.
    ///
    /// The framework associates [State] objects with a [BuildContext] after
    /// creating them with [StatefulWidget.createState] and before calling
    /// [initState]. The association is permanent: the [State] object will never
    /// change its [BuildContext]. However, the [BuildContext] itself can be moved
    /// around the tree.
    ///
    /// After calling [dispose], the framework severs the [State] object's
    /// connection with the [BuildContext].
    fun getContext(): BuildContext? = _element as BuildContext;
    internal var _element: StatefulElement? = null;

    /// Whether this [State] object is currently in a tree.
    ///
    /// After creating a [State] object and before calling [initState], the
    /// framework "mounts" the [State] object by associating it with a
    /// [BuildContext]. The [State] object remains mounted until the framework
    /// calls [dispose], after which time the framework will never ask the [State]
    /// object to [build] again.
    ///
    /// It is an error to call [setState] unless [mounted] is true.
    fun isMounted(): Boolean = _element != null;

    /// Called when this object is inserted into the tree.
    ///
    /// The framework will call this method exactly once for each [State] object
    /// it creates.
    ///
    /// Override this method to perform initialization that depends on the
    /// location at which this object was inserted into the tree (i.e., [context])
    /// or on the widget used to configure this object (i.e., [widget]).
    ///
    /// If a [State]'s [build] method depends on an object that can itself change
    /// state, for example a [ChangeNotifier] or [Stream], or some other object to
    /// which one can subscribe to receive notifications, then the [State] should
    /// subscribe to that object during [initState], unsubscribe from the old
    /// object and subscribe to the new object when it changes in
    /// [didUpdateWidget], and then unsubscribe from the object in [dispose].
    ///
    /// You cannot use [BuildContext.inheritFromWidgetOfExactType] from this
    /// method. However, [didChangeDependencies] will be called immediately
    /// following this method, and [BuildContext.inheritFromWidgetOfExactType] can
    /// be used there.
    ///
    /// If you override this, make sure your method starts with a call to
    /// super.initState().
    @CallSuper
    internal fun initState() {
        assert(_debugLifecycleState == _StateLifecycle.created);
    }

    /// Called whenever the widget configuration changes.
    ///
    /// If the parent widget rebuilds and request that this location in the tree
    /// update to display a new widget with the same [runtimeType] and
    /// [Widget.key], the framework will update the [widget] property of this
    /// [State] object to refer to the new widget and then call this method
    /// with the previous widget as an argument.
    ///
    /// Override this method to respond when the [widget] changes (e.g., to start
    /// implicit animations).
    ///
    /// The framework always calls [build] after calling [didUpdateWidget], which
    /// means any calls to [setState] in [didUpdateWidget] are redundant.
    ///
    /// If a [State]'s [build] method depends on an object that can itself change
    /// state, for example a [ChangeNotifier] or [Stream], or some other object to
    /// which one can subscribe to receive notifications, then the [State] should
    /// subscribe to that object during [initState], unsubscribe from the old
    /// object and subscribe to the new object when it changes in
    /// [didUpdateWidget], and then unsubscribe from the object in [dispose].
    ///
    /// If you override this, make sure your method starts with a call to
    /// super.didUpdateWidget(oldWidget).
    // TODO(Migration/Filip): Dropped covariant keyword
    @CallSuper
    internal fun didUpdateWidget(oldWidget: T) { }

    /// Called whenever the application is reassembled during debugging, for
    /// example during hot reload.
    ///
    /// This method should rerun any initialization logic that depends on global
    /// state, for example, image loading from asset bundles (since the asset
    /// bundle may have changed).
    ///
    /// In addition to this method being invoked, it is guaranteed that the
    /// [build] method will be invoked when a reassemble is signaled. Most
    /// widgets therefore do not need to do anything in the [reassemble] method.
    ///
    /// This function will only be called during development. In release builds,
    /// the `ext.flutter.reassemble` hook is not available, and so this code will
    /// never execute.
    ///
    /// See also:
    ///
    /// * [BindingBase.reassembleApplication].
    /// * [Image], which uses this to reload images.
    @CallSuper
    internal fun reassemble() { }

    /// Notify the framework that the internal state of this object has changed.
    ///
    /// Whenever you change the internal state of a [State] object, make the
    /// change in a function that you pass to [setState]:
    ///
    /// ```dart
    /// setState(() { _myState = newValue });
    /// ```
    ///
    /// The provided callback is immediately called synchronously. It must not
    /// return a future (the callback cannot be `async`), since then it would be
    /// unclear when the state was actually being set.
    ///
    /// Calling [setState] notifies the framework that the internal state of this
    /// object has changed in a way that might impact the user interface in this
    /// subtree, which causes the framework to schedule a [build] for this [State]
    /// object.
    ///
    /// If you just change the state directly without calling [setState], the
    /// framework might not schedule a [build] and the user interface for this
    /// subtree might not be updated to reflect the new state.
    ///
    /// Generally it is recommended that the `setState` method only be used to
    /// wrap the actual changes to the state, not any computation that might be
    /// associated with the change. For example, here a value used by the [build]
    /// function is incremented, and then the change is written to disk, but only
    /// the increment is wrapped in the `setState`:
    ///
    /// ```dart
    /// Future<Null> _incrementCounter() async {
    ///   setState(() {
    ///     _counter++;
    ///   });
    ///   Directory directory = await getApplicationDocumentsDirectory();
    ///   final String dirName = directory.path;
    ///   await new File('$dir/counter.txt').writeAsString('$_counter');
    ///   return null;
    /// }
    /// ```
    ///
    /// It is an error to call this method after the framework calls [dispose].
    /// You can determine whether it is legal to call this method by checking
    /// whether the [mounted] property is true.
//    protected fun setState(fn: VoidCallback) {
//        assert(fn != null);
//        assert(() {
//            if (_debugLifecycleState == _StateLifecycle.defunct) {
//                throw new FlutterError(
//                        'setState() called after dispose(): $this\n'
//                'This error happens if you call setState() on a State object for a widget that '
//                'no longer appears in the widget tree (e.g., whose parent widget no longer '
//                'includes the widget in its build). This error can occur when code calls '
//                'setState() from a timer or an animation callback. The preferred solution is '
//                'to cancel the timer or stop listening to the animation in the dispose() '
//                'callback. Another solution is to check the "mounted" property of this '
//                'object before calling setState() to ensure the object is still in the '
//                'tree.\n'
//                'This error might indicate a memory leak if setState() is being called '
//                'because another object is retaining a reference to this State object '
//                'after it has been removed from the tree. To avoid memory leaks, '
//                'consider breaking the reference to this object during dispose().'
//                );
//            }
//            if (_debugLifecycleState == _StateLifecycle.created && !mounted) {
//                throw new FlutterError(
//                        'setState() called in constructor: $this\n'
//                'This happens when you call setState() on a State object for a widget that '
//                'hasn\'t been inserted into the widget tree yet. It is not necessary to call '
//                'setState() in the constructor, since the state is already assumed to be dirty '
//                'when it is initially created.'
//                );
//            }
//            return true;
//        }());
//        final dynamic result = fn() as dynamic;
//        assert(() {
//            if (result is Future) {
//                throw new FlutterError(
//                        'setState() callback argument returned a Future.\n'
//                'The setState() method on $this was called with a closure or method that '
//                'returned a Future. Maybe it is marked as "async".\n'
//                'Instead of performing asynchronous work inside a call to setState(), first '
//                'execute the work (without updating the widget state), and then synchronously '
//                'update the state inside a call to setState().'
//                );
//            }
//            // We ignore other types of return values so that you can do things like:
//            //   setState(() => x = 3);
//            return true;
//        }());
//        _element.markNeedsBuild();
//    }

    /// Called when this object is removed from the tree.
    ///
    /// The framework calls this method whenever it removes this [State] object
    /// from the tree. In some cases, the framework will reinsert the [State]
    /// object into another part of the tree (e.g., if the subtree containing this
    /// [State] object is grafted from one location in the tree to another). If
    /// that happens, the framework will ensure that it calls [build] to give the
    /// [State] object a chance to adapt to its new location in the tree. If
    /// the framework does reinsert this subtree, it will do so before the end of
    /// the animation frame in which the subtree was removed from the tree. For
    /// this reason, [State] objects can defer releasing most resources until the
    /// framework calls their [dispose] method.
    ///
    /// Subclasses should override this method to clean up any links between
    /// this object and other elements in the tree (e.g. if you have provided an
    /// ancestor with a pointer to a descendant's [RenderObject]).
    ///
    /// If you override this, make sure to end your method with a call to
    /// super.deactivate().
    ///
    /// See also [dispose], which is called after [deactivate] if the widget is
    /// removed from the tree permanently.
    @CallSuper
    internal fun deactivate() { }

    /// Called when this object is removed from the tree permanently.
    ///
    /// The framework calls this method when this [State] object will never
    /// build again. After the framework calls [dispose], the [State] object is
    /// considered unmounted and the [mounted] property is false. It is an error
    /// to call [setState] at this point. This stage of the lifecycle is terminal:
    /// there is no way to remount a [State] object that has been disposed.
    ///
    /// Subclasses should override this method to release any resources retained
    /// by this object (e.g., stop any active animations).
    ///
    /// If a [State]'s [build] method depends on an object that can itself change
    /// state, for example a [ChangeNotifier] or [Stream], or some other object to
    /// which one can subscribe to receive notifications, then the [State] should
    /// subscribe to that object during [initState], unsubscribe from the old
    /// object and subscribe to the new object when it changes in
    /// [didUpdateWidget], and then unsubscribe from the object in [dispose].
    ///
    /// If you override this, make sure to end your method with a call to
    /// super.dispose().
    ///
    /// See also [deactivate], which is called prior to [dispose].
    @CallSuper
    internal fun dispose() {
        assert(_debugLifecycleState == _StateLifecycle.ready);
        // TODO(Migration/Filip): This used to be in assert block
        _debugLifecycleState = _StateLifecycle.defunct;
    }

    /// Describes the part of the user interface represented by this widget.
    ///
    /// The framework calls this method in a number of different situations:
    ///
    ///  * After calling [initState].
    ///  * After calling [didUpdateWidget].
    ///  * After receiving a call to [setState].
    ///  * After a dependency of this [State] object changes (e.g., an
    ///    [InheritedWidget] referenced by the previous [build] changes).
    ///  * After calling [deactivate] and then reinserting the [State] object into
    ///    the tree at another location.
    ///
    /// The framework replaces the subtree below this widget with the widget
    /// returned by this method, either by updating the existing subtree or by
    /// removing the subtree and inflating a new subtree, depending on whether the
    /// widget returned by this method can update the root of the existing
    /// subtree, as determined by calling [Widget.canUpdate].
    ///
    /// Typically implementations return a newly created constellation of widgets
    /// that are configured with information from this widget's constructor, the
    /// given [BuildContext], and the internal state of this [State] object.
    ///
    /// The given [BuildContext] contains information about the location in the
    /// tree at which this widget is being built. For example, the context
    /// provides the set of inherited widgets for this location in the tree. The
    /// [BuildContext] argument is always the same as the [context] property of
    /// this [State] object and will remain the same for the lifetime of this
    /// object. The [BuildContext] argument is provided redundantly here so that
    /// this method matches the signature for a [WidgetBuilder].
    ///
    /// ## Design discussion
    ///
    /// ### Why is the [build] method on [State], and not [StatefulWidget]?
    ///
    /// Putting a `Widget build(BuildContext context)` method on [State] rather
    /// putting a `Widget build(BuildContext context, State state)` method on
    /// [StatefulWidget] gives developers more flexibility when subclassing
    /// [StatefulWidget].
    ///
    /// For example, [AnimatedWidget] is a subclass of [StatefulWidget] that
    /// introduces an abstract `Widget build(BuildContext context)` method for its
    /// subclasses to implement. If [StatefulWidget] already had a [build] method
    /// that took a [State] argument, [AnimatedWidget] would be forced to provide
    /// its [State] object to subclasses even though its [State] object is an
    /// internal implementation detail of [AnimatedWidget].
    ///
    /// Conceptually, [StatelessWidget] could also be implemented as a subclass of
    /// [StatefulWidget] in a similar manner. If the [build] method were on
    /// [StatefulWidget] rather than [State], that would not be possible anymore.
    ///
    /// Putting the [build] function on [State] rather than [StatefulWidget] also
    /// helps avoid a category of bugs related to closures implicitly capturing
    /// `this`. If you defined a closure in a [build] function on a
    /// [StatefulWidget], that closure would implicitly capture `this`, which is
    /// the current widget instance, and would have the (immutable) fields of that
    /// instance in scope:
    ///
    /// ```dart
    /// class MyButton extends StatefulWidget {
    ///   ...
    ///   final Color color;
    ///
    ///   @override
    ///   Widget build(BuildContext context, MyButtonState state) {
    ///     ... () { print("color: $color"); } ...
    ///   }
    /// }
    /// ```
    ///
    /// For example, suppose the parent builds `MyButton` with `color` being blue,
    /// the `$color` in the print function refers to blue, as expected. Now,
    /// suppose the parent rebuilds `MyButton` with green. The closure created by
    /// the first build still implicitly refers to the original widget and the
    /// `$color` still prints blue even through the widget has been updated to
    /// green.
    ///
    /// In contrast, with the [build] function on the [State] object, closures
    /// created during [build] implicitly capture the [State] instance instead of
    /// the widget instance:
    ///
    /// ```dart
    /// class MyButtonState extends State<MyButton> {
    ///   ...
    ///   @override
    ///   Widget build(BuildContext context) {
    ///     ... () { print("color: ${widget.color}"); } ...
    ///   }
    /// }
    /// ```
    ///
    /// Now when the parent rebuilds `MyButton` with green, the closure created by
    /// the first build still refers to [State] object, which is preserved across
    /// rebuilds, but the framework has updated that [State] object's [widget]
    /// property to refer to the new `MyButton` instance and `${widget.color}`
    /// prints green, as expected.
    ///
    /// See also:
    ///
    ///  * The discussion on performance considerations at [StatefulWidget].
    internal abstract fun build(context: BuildContext): Widget;

    /// Called when a dependency of this [State] object changes.
    ///
    /// For example, if the previous call to [build] referenced an
    /// [InheritedWidget] that later changed, the framework would call this
    /// method to notify this object about the change.
    ///
    /// This method is also called immediately after [initState]. It is safe to
    /// call [BuildContext.inheritFromWidgetOfExactType] from this method.
    ///
    /// Subclasses rarely override this method because the framework always
    /// calls [build] after a dependency changes. Some subclasses do override
    /// this method because they need to do some expensive work (e.g., network
    /// fetches) when their dependencies change, and that work would be too
    /// expensive to do for every build.
    @CallSuper
    internal fun didChangeDependencies() { }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties);

        // TODO(Migration/Filip): This used to be in assert block
        properties.add(EnumProperty<_StateLifecycle>("lifecycle state", _debugLifecycleState, defaultValue = _StateLifecycle.ready));
        properties.add(ObjectFlagProperty<T>("_widget", widget, ifNull = "no widget"));
        properties.add(ObjectFlagProperty<StatefulElement>("_element", _element, ifNull = "not mounted"));
    }
}