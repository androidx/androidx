package androidx.ui.widgets.framework

import androidx.ui.foundation.Key

// / Base class for widgets that efficiently propagate information down the tree.
// /
// / To obtain the nearest instance of a particular type of inherited widget from
// / a build context, use [BuildContext.inheritFromWidgetOfExactType].
// /
// / Inherited widgets, when referenced in this way, will cause the consumer to
// / rebuild when the inherited widget itself changes state.
// /
// / ## Sample code
// /
// / The following is a skeleton of an inherited widget called `FrogColor`:
// /
// / ```dart
// / class FrogColor extends InheritedWidget {
// /   const FrogColor({
// /     Key key,
// /     @required this.color,
// /     @required Widget child,
// /   }) : assert(color != null),
// /        assert(child != null),
// /        super(key: key, child: child);
// /
// /   final Color color;
// /
// /   static FrogColor of(BuildContext context) {
// /     return context.inheritFromWidgetOfExactType(FrogColor);
// /   }
// /
// /   @override
// /   bool updateShouldNotify(FrogColor old) => color != old.color;
// / }
// / ```
// /
// / The convention is to provide a static method `of` on the [InheritedWidget]
// / which does the call to [BuildContext.inheritFromWidgetOfExactType]. This
// / allows the class to define its own fallback logic in the case of there not
// / being a widget in scope. In the example above, the value returned will be
// / null in that case, but it could also have defaulted to a value.
// /
// / Sometimes, the `of` method returns the data rather than the inherited
// / widget; for example, in this case it could have returned a [Color] instead
// / of the `FrogColor` widget.
// /
// / Occasionally, the inherited widget is an implementation detail of another
// / class, and is therefore private. The `of` method in that case is typically
// / put on the public class instead. For example, [Theme] is implemented as a
// / [StatelessWidget] that builds a private inherited widget; [Theme.of] looks
// / for that inherited widget using [BuildContext.inheritFromWidgetOfExactType]
// / and then returns the [ThemeData].
// /
// / See also:
// /
// /  * [StatefulWidget] and [State], for widgets that can build differently
// /    several times over their lifetime.
// /  * [StatelessWidget], for widgets that always build the same way given a
// /    particular configuration and ambient state.
// /  * [Widget], for an overview of widgets in general.
abstract class InheritedWidget(key: Key, child: Widget) : ProxyWidget(key, child) {

    override fun createElement(): InheritedElement = InheritedElement(this)

    // / Whether the framework should notify widgets that inherit from this widget.
    // /
    // / When this widget is rebuilt, sometimes we need to rebuild the widgets that
    // / inherit from this widget but sometimes we do not. For example, if the data
    // / held by this widget is the same as the data held by `oldWidget`, then then
    // / we do not need to rebuild the widgets that inherited the data held by
    // / `oldWidget`.
    // /
    // / The framework distinguishes these cases by calling this function with the
    // / widget that previously occupied this location in the tree as an argument.
    // / The given widget is guaranteed to have the same [runtimeType] as this
    // / object.
    // TODO (Migration/Filip): Dropped covariance
    internal abstract fun updateShouldNotify(oldWidget: InheritedWidget): Boolean
}