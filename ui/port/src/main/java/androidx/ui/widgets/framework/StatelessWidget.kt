package androidx.ui.widgets.framework

import androidx.ui.foundation.Key

/// A widget that does not require mutable state.
///
/// A stateless widget is a widget that describes part of the user interface by
/// building a constellation of other widgets that describe the user interface
/// more concretely. The building process continues recursively until the
/// description of the user interface is fully concrete (e.g., consists
/// entirely of [RenderObjectWidget]s, which describe concrete [RenderObject]s).
///
/// Stateless widget are useful when the part of the user interface you are
/// describing does not depend on anything other than the configuration
/// information in the object itself and the [BuildContext] in which the widget
/// is inflated. For compositions that can change dynamically, e.g. due to
/// having an internal clock-driven state, or depending on some system state,
/// consider using [StatefulWidget].
///
/// ## Performance considerations
///
/// The [build] method of a stateless widget is typically only called in three
/// situations: the first time the widget is inserted in the tree, when the
/// widget's parent changes its configuration, and when an [InheritedWidget] it
/// depends on changes.
///
/// If a widget's parent will regularly change the widget's configuration, or if
/// it depends on inherited widgets that frequently change, then it is important
/// to optimize the performance of the [build] method to maintain a fluid
/// rendering performance.
///
/// There are several techniques one can use to minimize the impact of
/// rebuilding a stateless widget:
///
///  * Minimize the number of nodes transitively created by the build method and
///    any widgets it creates. For example, instead of an elaborate arrangement
///    of [Row]s, [Column]s, [Padding]s, and [SizedBox]es to position a single
///    child in a particularly fancy manner, consider using just an [Align] or a
///    [CustomSingleChildLayout]. Instead of an intricate layering of multiple
///    [Container]s and with [Decoration]s to draw just the right graphical
///    effect, consider a single [CustomPaint] widget.
///
///  * Use `const` widgets where possible, and provide a `const` constructor for
///    the widget so that users of the widget can also do so.
///
///  * Consider refactoring the stateless widget into a stateful widget so that
///    it can use some of the techniques described at [StatefulWidget], such as
///    caching common parts of subtrees and using [GlobalKey]s when changing the
///    tree structure.
///
///  * If the widget is likely to get rebuilt frequently due to the use of
///    [InheritedWidget]s, consider refactoring the stateless widget into
///    multiple widgets, with the parts of the tree that change being pushed to
///    the leaves. For example instead of building a tree with four widgets, the
///    inner-most widget depending on the [Theme], consider factoring out the
///    part of the build function that builds the inner-most widget into its own
///    widget, so that only the inner-most widget needs to be rebuilt when the
///    theme changes.
///
/// ## Sample code
///
/// The following is a skeleton of a stateless widget subclass called `GreenFrog`:
///
/// ```dart
/// class GreenFrog extends StatelessWidget {
///   const GreenFrog({ Key key }) : super(key: key);
///
///   @override
///   Widget build(BuildContext context) {
///     return new Container(color: const Color(0xFF2DBD3A));
///   }
/// }
/// ```
///
/// Normally widgets have more constructor arguments, each of which corresponds
/// to a `final` property. The next example shows the more generic widget `Frog`
/// which can be given a color and a child:
///
/// ```dart
/// class Frog extends StatelessWidget {
///   const Frog({
///     Key key,
///     this.color: const Color(0xFF2DBD3A),
///     this.child,
///   }) : super(key: key);
///
///   final Color color;
///
///   final Widget child;
///
///   @override
///   Widget build(BuildContext context) {
///     return new Container(color: color, child: child);
///   }
/// }
/// ```
///
/// By convention, widget constructors only use named arguments. Named arguments
/// can be marked as required using [@required]. Also by convention, the first
/// argument is [key], and the last argument is `child`, `children`, or the
/// equivalent.
///
/// See also:
///
///  * [StatefulWidget] and [State], for widgets that can build differently
///    several times over their lifetime.
///  * [InheritedWidget], for widgets that introduce ambient state that can
///    be read by descendant widgets.
abstract class StatelessWidget(key: Key) : Widget(key) {

    /// Creates a [StatelessElement] to manage this widget's location in the tree.
    ///
    /// It is uncommon for subclasses to override this method.
    override fun createElement(): StatelessElement = StatelessElement(this);

    /// Describes the part of the user interface represented by this widget.
    ///
    /// The framework calls this method when this widget is inserted into the
    /// tree in a given [BuildContext] and when the dependencies of this widget
    /// change (e.g., an [InheritedWidget] referenced by this widget changes).
    ///
    /// The framework replaces the subtree below this widget with the widget
    /// returned by this method, either by updating the existing subtree or by
    /// removing the subtree and inflating a new subtree, depending on whether the
    /// widget returned by this method can update the root of the existing
    /// subtree, as determined by calling [Widget.canUpdate].
    ///
    /// Typically implementations return a newly created constellation of widgets
    /// that are configured with information from this widget's constructor and
    /// from the given [BuildContext].
    ///
    /// The given [BuildContext] contains information about the location in the
    /// tree at which this widget is being built. For example, the context
    /// provides the set of inherited widgets for this location in the tree. A
    /// given widget might be built with multiple different [BuildContext]
    /// arguments over time if the widget is moved around the tree or if the
    /// widget is inserted into the tree in multiple places at once.
    ///
    /// The implementation of this method must only depend on:
    ///
    /// * the fields of the widget, which themselves must not change over time,
    ///   and
    /// * any ambient state obtained from the `context` using
    ///   [BuildContext.inheritFromWidgetOfExactType].
    ///
    /// If a widget's [build] method is to depend on anything else, use a
    /// [StatefulWidget] instead.
    ///
    /// See also:
    ///
    ///  * The discussion on performance considerations at [StatelessWidget].
    internal abstract fun build(context: BuildContext): Widget;
}