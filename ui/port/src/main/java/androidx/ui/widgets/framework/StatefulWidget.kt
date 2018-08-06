package androidx.ui.widgets.framework

import androidx.ui.foundation.Key

// / A widget that has mutable state.
// /
// / State is information that (1) can be read synchronously when the widget is
// / built and (2) might change during the lifetime of the widget. It is the
// / responsibility of the widget implementer to ensure that the [State] is
// / promptly notified when such state changes, using [State.setState].
// /
// / A stateful widget is a widget that describes part of the user interface by
// / building a constellation of other widgets that describe the user interface
// / more concretely. The building process continues recursively until the
// / description of the user interface is fully concrete (e.g., consists
// / entirely of [RenderObjectWidget]s, which describe concrete [RenderObject]s).
// /
// / Stateful widget are useful when the part of the user interface you are
// / describing can change dynamically, e.g. due to having an internal
// / clock-driven state, or depending on some system state. For compositions that
// / depend only on the configuration information in the object itself and the
// / [BuildContext] in which the widget is inflated, consider using
// / [StatelessWidget].
// /
// / [StatefulWidget] instances themselves are immutable and store their mutable
// / state either in separate [State] objects that are created by the
// / [createState] method, or in objects to which that [State] subscribes, for
// / example [Stream] or [ChangeNotifier] objects, to which references are stored
// / in final fields on the [StatefulWidget] itself.
// /
// / The framework calls [createState] whenever it inflates a
// / [StatefulWidget], which means that multiple [State] objects might be
// / associated with the same [StatefulWidget] if that widget has been inserted
// / into the tree in multiple places. Similarly, if a [StatefulWidget] is
// / removed from the tree and later inserted in to the tree again, the framework
// / will call [createState] again to create a fresh [State] object, simplifying
// / the lifecycle of [State] objects.
// /
// / A [StatefulWidget] keeps the same [State] object when moving from one
// / location in the tree to another if its creator used a [GlobalKey] for its
// / [key]. Because a widget with a [GlobalKey] can be used in at most one
// / location in the tree, a widget that uses a [GlobalKey] has at most one
// / associated element. The framework takes advantage of this property when
// / moving a widget with a global key from one location in the tree to another
// / by grafting the (unique) subtree associated with that widget from the old
// / location to the new location (instead of recreating the subtree at the new
// / location). The [State] objects associated with [StatefulWidget] are grafted
// / along with the rest of the subtree, which means the [State] object is reused
// / (instead of being recreated) in the new location. However, in order to be
// / eligible for grafting, the widget must be inserted into the new location in
// / the same animation frame in which it was removed from the old location.
// /
// / ## Performance considerations
// /
// / There are two primary categories of [StatefulWidget]s.
// /
// / The first is one which allocates resources in [State.initState] and disposes
// / of them in [State.dispose], but which does not depend on [InheritedWidget]s
// / or call [State.setState]. Such widgets are commonly used at the root of an
// / application or page, and communicate with subwidgets via [ChangeNotifier]s,
// / [Stream]s, or other such objects. Stateful widgets following such a pattern
// / are relatively cheap (in terms of CPU and GPU cycles), because they are
// / built once then never update. They can, therefore, have somewhat complicated
// / and deep build methods.
// /
// / The second category is widgets that use [State.setState] or depend on
// / [InheritedWidget]s. These will typically rebuild many times during the
// / application's lifetime, and it is therefore important to minimize the impact
// / of rebuilding such a widget. (They may also use [State.initState] or
// / [State.didChangeDependencies] and allocate resources, but the important part
// / is that they rebuild.)
// /
// / There are several techniques one can use to minimize the impact of
// / rebuilding a stateful widget:
// /
// /  * Push the state to the leaves. For example, if your page has a ticking
// /    clock, rather than putting the state at the top of the page and
// /    rebuilding the entire page each time the clock ticks, create a dedicated
// /    clock widget that only updates itself.
// /
// /  * Minimize the number of nodes transitively created by the build method and
// /    any widgets it creates. Ideally, a stateful widget would only create a
// /    single widget, and that widget would be a [RenderObjectWidget].
// /    (Obviously this isn't always practical, but the closer a widget gets to
// /    this ideal, the more efficient it will be.)
// /
// /  * If a subtree does not change, cache the widget that represents that
// /    subtree and re-use it each time it can be used. It is massively more
// /    efficient for a widget to be re-used than for a new (but
// /    identically-configured) widget to be created. Factoring out the stateful
// /    part into a widget that takes a child argument is a common way of doing
// /    this.
// /
// /  * Use `const` widgets where possible. (This is equivalent to caching a
// /    widget and re-using it.)
// /
// /  * Avoid changing the depth of any created subtrees or changing the type of
// /    any widgets in the subtree. For example, rather than returning either the
// /    child or the child wrapped in an [IgnorePointer], always wrap the child
// /    widget in an [IgnorePointer] and control the [IgnorePointer.ignoring]
// /    property. This is because changing the depth of the subtree requires
// /    rebuilding, laying out, and painting the entire subtree, whereas just
// /    changing the property will require the least possible change to the
// /    render tree (in the case of [IgnorePointer], for example, no layout or
// /    repaint is necessary at all).
// /
// /  * If the depth must be changed for some reason, consider wrapping the
// /    common parts of the subtrees in widgets that have a [GlobalKey] that
// /    remains consistent for the life of the stateful widget. (The
// /    [KeyedSubtree] widget may be useful for this purpose if no other widget
// /    can conveniently be assigned the key.)
// /
// / ## Sample code
// /
// / The following is a skeleton of a stateful widget subclass called `YellowBird`:
// /
// / ```dart
// / class YellowBird extends StatefulWidget {
// /   const YellowBird({ Key key }) : super(key: key);
// /
// /   @override
// /   _YellowBirdState createState() => new _YellowBirdState();
// / }
// /
// / class _YellowBirdState extends State<YellowBird> {
// /   @override
// /   Widget build(BuildContext context) {
// /     return new Container(color: const Color(0xFFFFE306));
// /   }
// / }
// / ```
// /
// / In this example. the [State] has no actual state. State is normally
// / represented as private member fields. Also, normally widgets have more
// / constructor arguments, each of which corresponds to a `final` property.
// /
// / The next example shows the more generic widget `Bird` which can be given a
// / color and a child, and which has some internal state with a method that
// / can be called to mutate it:
// /
// / ```dart
// / class Bird extends StatefulWidget {
// /   const Bird({
// /     Key key,
// /     this.color: const Color(0xFFFFE306),
// /     this.child,
// /   }) : super(key: key);
// /
// /   final Color color;
// /
// /   final Widget child;
// /
// /   _BirdState createState() => new _BirdState();
// / }
// /
// / class _BirdState extends State<Bird> {
// /   double _size = 1.0;
// /
// /   void grow() {
// /     setState(() { _size += 0.1; });
// /   }
// /
// /   @override
// /   Widget build(BuildContext context) {
// /     return new Container(
// /       color: widget.color,
// /       transform: new Matrix4.diagonal3Values(_size, _size, 1.0),
// /       child: widget.child,
// /     );
// /   }
// / }
// / ```
// /
// / By convention, widget constructors only use named arguments. Named arguments
// / can be marked as required using [@required]. Also by convention, the first
// / argument is [key], and the last argument is `child`, `children`, or the
// / equivalent.
// /
// / See also:
// /
// /  * [State], where the logic behind a [StatefulWidget] is hosted.
// /  * [StatelessWidget], for widgets that always build the same way given a
// /    particular configuration and ambient state.
// /  * [InheritedWidget], for widgets that introduce ambient state that can
// /    be read by descendant widgets.
abstract class StatefulWidget(key: Key) : Widget(key) {

    // / Creates a [StatefulElement] to manage this widget's location in the tree.
    // /
    // / It is uncommon for subclasses to override this method.
    override fun createElement(): StatefulElement = StatefulElement(this)

    // / Creates the mutable state for this widget at a given location in the tree.
    // /
    // / Subclasses should override this method to return a newly created
    // / instance of their associated [State] subclass:
    // /
    // / ```dart
    // / @override
    // / _MyState createState() => new _MyState();
    // / ```
    // /
    // / The framework can call this method multiple times over the lifetime of
    // / a [StatefulWidget]. For example, if the widget is inserted into the tree
    // / in multiple locations, the framework will create a separate [State] object
    // / for each location. Similarly, if the widget is removed from the tree and
    // / later inserted into the tree again, the framework will call [createState]
    // / again to create a fresh [State] object, simplifying the lifecycle of
    // / [State] objects.
    abstract fun createState(): State<StatefulWidget>
}