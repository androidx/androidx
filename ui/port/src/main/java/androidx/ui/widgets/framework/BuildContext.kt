package androidx.ui.widgets.framework

import androidx.ui.Type
import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.obj.RenderObject

// / A handle to the location of a widget in the widget tree.
// /
// / This class presents a set of methods that can be used from
// / [StatelessWidget.build] methods and from methods on [State] objects.
// /
// / [BuildContext] objects are passed to [WidgetBuilder] functions (such as
// / [StatelessWidget.build]), and are available from the [State.context] member.
// / Some static functions (e.g. [showDialog], [Theme.of], and so forth) also
// / take build contexts so that they can act on behalf of the calling widget, or
// / obtain data specifically for the given context.
// /
// / Each widget has its own [BuildContext], which becomes the parent of the
// / widget returned by the [StatelessWidget.build] or [State.build] function.
// / (And similarly, the parent of any children for [RenderObjectWidget]s.)
// /
// / In particular, this means that within a build method, the build context of
// / the widget of the build method is not the same as the build context of the
// / widgets returned by that build method. This can lead to some tricky cases.
// / For example, [Theme.of(context)] looks for the nearest enclosing [Theme] of
// / the given build context. If a build method for a widget Q includes a [Theme]
// / within its returned widget tree, and attempts to use [Theme.of] passing its
// / own context, the build method for Q will not find that [Theme] object. It
// / will instead find whatever [Theme] was an ancestor to the widget Q. If the
// / build context for a subpart of the returned tree is needed, a [Builder]
// / widget can be used: the build context passed to the [Builder.builder]
// / callback will be that of the [Builder] itself.
// /
// / For example, in the following snippet, the [ScaffoldState.showSnackBar]
// / method is called on the [Scaffold] widget that the build method itself
// / creates. If a [Builder] had not been used, and instead the `context`
// / argument of the build method itself had been used, no [Scaffold] would have
// / been found, and the [Scaffold.of] function would have returned null.
// /
// / ```dart
// /   @override
// /   Widget build(BuildContext context) {
// /     // here, Scaffold.of(context) returns null
// /     return new Scaffold(
// /       appBar: new AppBar(title: new Text('Demo')),
// /       body: new Builder(
// /         builder: (BuildContext context) {
// /           return new FlatButton(
// /             child: new Text('BUTTON'),
// /             onPressed: () {
// /               // here, Scaffold.of(context) returns the locally created Scaffold
// /               Scaffold.of(context).showSnackBar(new SnackBar(
// /                 content: new Text('Hello.')
// /               ));
// /             }
// /           );
// /         }
// /       )
// /     );
// /   }
// / ```
// /
// / The [BuildContext] for a particular widget can change location over time as
// / the widget is moved around the tree. Because of this, values returned from
// / the methods on this class should not be cached beyond the execution of a
// / single synchronous function.
// /
// / [BuildContext] objects are actually [Element] objects. The [BuildContext]
// / interface is used to discourage direct manipulation of [Element] objects.
interface BuildContext {
    // / The current configuration of the [Element] that is this [BuildContext].
    val widget: Widget

    // / The [BuildOwner] for this context. The [BuildOwner] is in charge of
    // / managing the rendering pipeline for this context.
    val owner: BuildOwner?

    // / The current [RenderObject] for the widget. If the widget is a
    // / [RenderObjectWidget], this is the render object that the widget created
    // / for itself. Otherwise, it is the render object of the first descendant
    // / [RenderObjectWidget].
    // /
    // / This method will only return a valid result after the build phase is
    // / complete. It is therefore not valid to call this from a build method.
    // / It should only be called from interaction event handlers (e.g.
    // / gesture callbacks) or layout or paint callbacks.
    // /
    // / If the render object is a [RenderBox], which is the common case, then the
    // / size of the render object can be obtained from the [size] getter. This is
    // / only valid after the layout phase, and should therefore only be examined
    // / from paint callbacks or interaction event handlers (e.g. gesture
    // / callbacks).
    // /
    // / For details on the different phases of a frame, see the discussion at
    // / [WidgetsBinding.drawFrame].
    // /
    // / Calling this method is theoretically relatively expensive (O(N) in the
    // / depth of the tree), but in practice is usually cheap because the tree
    // / usually has many render objects and therefore the distance to the nearest
    // / render object is usually short.
    fun findRenderObject(): RenderObject

    // / The size of the [RenderBox] returned by [findRenderObject].
    // /
    // / This getter will only return a valid result after the layout phase is
    // / complete. It is therefore not valid to call this from a build method.
    // / It should only be called from paint callbacks or interaction event
    // / handlers (e.g. gesture callbacks).
    // /
    // / For details on the different phases of a frame, see the discussion at
    // / [WidgetsBinding.drawFrame].
    // /
    // / This getter will only return a valid result if [findRenderObject] actually
    // / returns a [RenderBox]. If [findRenderObject] returns a render object that
    // / is not a subtype of [RenderBox] (e.g., [RenderView]), this getter will
    // / throw an exception in checked mode and will return null in release mode.
    // /
    // / Calling this getter is theoretically relatively expensive (O(N) in the
    // / depth of the tree), but in practice is usually cheap because the tree
    // / usually has many render objects and therefore the distance to the nearest
    // / render object is usually short.
    val size: Size?

    // / Obtains the nearest widget of the given type, which must be the type of a
    // / concrete [InheritedWidget] subclass, and registers this build context with
    // / that widget such that when that widget changes (or a new widget of that
    // / type is introduced, or the widget goes away), this build context is
    // / rebuilt so that it can obtain new values from that widget.
    // /
    // / This is typically called implicitly from `of()` static methods, e.g.
    // / [Theme.of].
    // /
    // / This should not be called from widget constructors or from
    // / [State.initState] methods, because those methods would not get called
    // / again if the inherited value were to change. To ensure that the widget
    // / correctly updates itself when the inherited value changes, only call this
    // / (directly or indirectly) from build methods, layout and paint callbacks, or
    // / from [State.didChangeDependencies].
    // /
    // / This method should not be called from [State.dispose] because the element
    // / tree is no longer stable at that time. To refer to an ancestor from that
    // / method, save a reference to the ancestor in [State.didChangeDependencies].
    // / It is safe to use this method from [State.deactivate], which is called
    // / whenever the widget is removed from the tree.
    // /
    // / It is also possible to call this from interaction event handlers (e.g.
    // / gesture callbacks) or timers, to obtain a value once, if that value is not
    // / going to be cached and reused later.
    // /
    // / Calling this method is O(1) with a small constant factor, but will lead to
    // / the widget being rebuilt more often.
    // /
    // / Once a widget registers a dependency on a particular type by calling this
    // / method, it will be rebuilt, and [State.didChangeDependencies] will be
    // / called, whenever changes occur relating to that widget until the next time
    // / the widget or one of its ancestors is moved (for example, because an
    // / ancestor is added or removed).
    fun inheritFromWidgetOfExactType(targetType: Type): InheritedWidget?

    // / Obtains the element corresponding to the nearest widget of the given type,
    // / which must be the type of a concrete [InheritedWidget] subclass.
    // /
    // / Calling this method is O(1) with a small constant factor.
    // /
    // / This method does not establish a relationship with the target in the way
    // / that [inheritFromWidgetOfExactType] does.
    // /
    // / This method should not be called from [State.dispose] because the element
    // / tree is no longer stable at that time. To refer to an ancestor from that
    // / method, save a reference to the ancestor by calling
    // / [inheritFromWidgetOfExactType] in [State.didChangeDependencies]. It is
    // / safe to use this method from [State.deactivate], which is called whenever
    // / the widget is removed from the tree.
    fun ancestorInheritedElementForWidgetOfExactType(targetType: Type): InheritedElement?

    // / Returns the nearest ancestor widget of the given type, which must be the
    // / type of a concrete [Widget] subclass.
    // /
    // / This should not be used from build methods, because the build context will
    // / not be rebuilt if the value that would be returned by this method changes.
    // / In general, [inheritFromWidgetOfExactType] is more useful. This method is
    // / appropriate when used in interaction event handlers (e.g. gesture
    // / callbacks), or for performing one-off tasks.
    // /
    // / Calling this method is relatively expensive (O(N) in the depth of the
    // / tree). Only call this method if the distance from this widget to the
    // / desired ancestor is known to be small and bounded.
    // /
    // / This method should not be called from [State.deactivate] or [State.dispose]
    // / because the widget tree is no longer stable at that time. To refer to
    // / an ancestor from one of those methods, save a reference to the ancestor
    // / by calling [ancestorWidgetOfExactType] in [State.didChangeDependencies].
    fun ancestorWidgetOfExactType(targetType: Type): Widget?

    // / Returns the [State] object of the nearest ancestor [StatefulWidget] widget
    // / that matches the given [TypeMatcher].
    // /
    // / This should not be used from build methods, because the build context will
    // / not be rebuilt if the value that would be returned by this method changes.
    // / In general, [inheritFromWidgetOfExactType] is more appropriate for such
    // / cases. This method is useful for changing the state of an ancestor widget in
    // / a one-off manner, for example, to cause an ancestor scrolling list to
    // / scroll this build context's widget into view, or to move the focus in
    // / response to user interaction.
    // /
    // / In general, though, consider using a callback that triggers a stateful
    // / change in the ancestor rather than using the imperative style implied by
    // / this method. This will usually lead to more maintainable and reusable code
    // / since it decouples widgets from each other.
    // /
    // / Calling this method is relatively expensive (O(N) in the depth of the
    // / tree). Only call this method if the distance from this widget to the
    // / desired ancestor is known to be small and bounded.
    // /
    // / This method should not be called from [State.deactivate] or [State.dispose]
    // / because the widget tree is no longer stable at that time. To refer to
    // / an ancestor from one of those methods, save a reference to the ancestor
    // / by calling [ancestorStateOfType] in [State.didChangeDependencies].
    // /
    // / ## Sample code
    // /
    // / ```dart
    // / ScrollableState scrollable = context.ancestorStateOfType(
    // /   const TypeMatcher<ScrollableState>(),
    // / );
    // / ```
    fun ancestorStateOfType(matcher: TypeMatcher): State<*>?

    // / Returns the [State] object of the furthest ancestor [StatefulWidget] widget
    // / that matches the given [TypeMatcher].
    // /
    // / Functions the same way as [ancestorStateOfType] but keeps visiting subsequent
    // / ancestors until there are none of the type matching [TypeMatcher] remaining.
    // / Then returns the last one found.
    // /
    // / This operation is O(N) as well though N is the entire widget tree rather than
    // / a subtree.
    fun rootAncestorStateOfType(matcher: TypeMatcher): State<*>?

    // / Returns the [RenderObject] object of the nearest ancestor [RenderObjectWidget] widget
    // / that matches the given [TypeMatcher].
    // /
    // / This should not be used from build methods, because the build context will
    // / not be rebuilt if the value that would be returned by this method changes.
    // / In general, [inheritFromWidgetOfExactType] is more appropriate for such
    // / cases. This method is useful only in esoteric cases where a widget needs
    // / to cause an ancestor to change its layout or paint behavior. For example,
    // / it is used by [Material] so that [InkWell] widgets can trigger the ink
    // / splash on the [Material]'s actual render object.
    // /
    // / Calling this method is relatively expensive (O(N) in the depth of the
    // / tree). Only call this method if the distance from this widget to the
    // / desired ancestor is known to be small and bounded.
    // /
    // / This method should not be called from [State.deactivate] or [State.dispose]
    // / because the widget tree is no longer stable at that time. To refer to
    // / an ancestor from one of those methods, save a reference to the ancestor
    // / by calling [ancestorRenderObjectOfType] in [State.didChangeDependencies].
    fun ancestorRenderObjectOfType(matcher: TypeMatcher): RenderObject?

    // / Walks the ancestor chain, starting with the parent of this build context's
    // / widget, invoking the argument for each ancestor. The callback is given a
    // / reference to the ancestor widget's corresponding [Element] object. The
    // / walk stops when it reaches the root widget or when the callback returns
    // / false. The callback must not return null.
    // /
    // / This is useful for inspecting the widget tree.
    // /
    // / Calling this method is relatively expensive (O(N) in the depth of the tree).
    // /
    // / This method should not be called from [State.deactivate] or [State.dispose]
    // / because the element tree is no longer stable at that time. To refer to
    // / an ancestor from one of those methods, save a reference to the ancestor
    // / by calling [visitAncestorElements] in [State.didChangeDependencies].
    fun visitAncestorElements(visitor: (Element) -> Boolean)

    // / Walks the children of this widget.
    // /
    // / This is useful for applying changes to children after they are built
    // / without waiting for the next frame, especially if the children are known,
    // / and especially if there is exactly one child (as is always the case for
    // / [StatefulWidget]s or [StatelessWidget]s).
    // /
    // / Calling this method is very cheap for build contexts that correspond to
    // / [StatefulWidget]s or [StatelessWidget]s (O(1), since there's only one
    // / child).
    // /
    // / Calling this method is potentially expensive for build contexts that
    // / correspond to [RenderObjectWidget]s (O(N) in the number of children).
    // /
    // / Calling this method recursively is extremely expensive (O(N) in the number
    // / of descendants), and should be avoided if possible. Generally it is
    // / significantly cheaper to use an [InheritedWidget] and have the descendants
    // / pull data down, than it is to use [visitChildElements] recursively to push
    // / data down to them.
    fun visitChildElements(visitor: ElementVisitor)
}