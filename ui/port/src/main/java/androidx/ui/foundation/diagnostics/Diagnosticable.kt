package androidx.ui.foundation.diagnostics

/// A base class for providing string and [DiagnosticsNode] debug
/// representations describing the properties of an object.
///
/// The string debug representation is generated from the intermediate
/// [DiagnosticsNode] representation. The [DiagnosticsNode] representation is
/// also used by debugging tools displaying interactive trees of objects and
/// properties.
///
/// See also:
///
///  * [DiagnosticableTree], which extends this class to also describe the
///    children of a tree structured object.
///  * [Diagnosticable.debugFillProperties], which lists best practices
///    for specifying the properties of a [DiagnosticNode]. The most common use
///    case is to override [debugFillProperties] defining custom properties for
///    a subclass of [TreeDiagnosticsMixin] using the existing
///    [DiagnosticsProperty] subclasses.
///  * [DiagnosticableTree.debugDescribeChildren], which lists best practices
///    for describing the children of a [DiagnosticNode]. Typically the base
///    class already describes the children of a node properly or a node has
///    no children.
///  * [DiagnosticsProperty], which should be used to create leaf diagnostic
///    nodes without properties or children. There are many [DiagnosticProperty]
///    subclasses to handle common use cases.
abstract class Diagnosticable {

    /// A brief description of this object, usually just the [runtimeType] and the
    /// [hashCode].
    ///
    /// See also:
    ///
    ///  * [toString], for a detailed description of the object.
    open fun toStringShort() = describeIdentity(this);

    override fun toString(): String {
        return toStringParametrized(DiagnosticLevel.debug)
    }

    open fun toStringParametrized(minLevel: DiagnosticLevel = DiagnosticLevel.debug): String {
        return toDiagnosticsNode(style = DiagnosticsTreeStyle.singleLine).toStringParametrized(minLevel = minLevel);
    }

    /// Returns a debug representation of the object that is used by debugging
    /// tools and by [toStringDeep].
    ///
    /// Leave [name] as null if there is not a meaningful description of the
    /// relationship between the this node and its parent.
    ///
    /// Typically the [style] argument is only specified to indicate an atypical
    /// relationship between the parent and the node. For example, pass
    /// [DiagnosticsTreeStyle.offstage] to indicate that a node is offstage.
    open fun toDiagnosticsNode(
            name: String? = null,
            style: DiagnosticsTreeStyle? = null
    ): DiagnosticsNode {
        return DiagnosticableNode<Diagnosticable>(
                name = name,
                value = this,
                style = style
        )
    }

    /// Add additional properties associated with the node.
    ///
    /// Use the most specific [DiagnosticsProperty] existing subclass to describe
    /// each property instead of the [DiagnosticsProperty] base class. There are
    /// only a small number of [DiagnosticsProperty] subclasses each covering a
    /// common use case. Consider what values a property is relevant for users
    /// debugging as users debugging large trees are overloaded with information.
    /// Common named parameters in [DiagnosticsNode] subclasses help filter when
    /// and how properties are displayed.
    ///
    /// `defaultValue`, `showName`, `showSeparator`, and `level` keep string
    /// representations of diagnostics terse and hide properties when they are not
    /// very useful.
    ///
    ///  * Use `defaultValue` any time the default value of a property is
    ///    uninteresting. For example, specify a default value of null any time
    ///    a property being null does not indicate an error.
    ///  * Avoid specifying the `level` parameter unless the result you want
    ///    cannot be be achieved by using the `defaultValue` parameter or using
    ///    the [ObjectFlagProperty] class to conditionally display the property
    ///    as a flag.
    ///  * Specify `showName` and `showSeparator` in rare cases where the string
    ///    output would look clumsy if they were not set.
    ///    ```dart
    ///    new DiagnosticsProperty<Object>('child(3, 4)', null, ifNull: 'is null', showSeparator: false).toString()
    ///    ```
    ///    Shows using `showSeparator` to get output `child(3, 4) is null` which
    ///    is more polished than `child(3, 4): is null`.
    ///    ```dart
    ///    new DiagnosticsProperty<IconData>('icon', icon, ifNull: '<empty>', showName: false)).toString()
    ///    ```
    ///    Shows using `showName` to omit the property name as in this context the
    ///    property name does not add useful information.
    ///
    /// `ifNull`, `ifEmpty`, `unit`, and `tooltip` make property
    /// descriptions clearer. The examples in the code sample below illustrate
    /// good uses of all of these parameters.
    ///
    /// ## DiagnosticsProperty subclasses for primitive types
    ///
    ///  * [StringProperty], which supports automatically enclosing a [String]
    ///    value in quotes.
    ///  * [DoubleProperty], which supports specifying a unit of measurement for
    ///    a [double] value.
    ///  * [PercentProperty], which clamps a [double] to between 0 and 1 and
    ///    formats it as a percentage.
    ///  * [IntProperty], which supports specifying a unit of measurement for an
    ///    [int] value.
    ///  * [FlagProperty], which formats a [bool] value as one or more flags.
    ///    Depending on the use case it is better to format a bool as
    ///    `DiagnosticsProperty<bool>` instead of using [FlagProperty] as the
    ///    output is more verbose but unambiguous.
    ///
    /// ## Other important [DiagnosticsProperty] variants
    ///
    ///  * [EnumProperty], which provides terse descriptions of enum values
    ///    working around limitations of the `toString` implementation for Dart
    ///    enum types.
    ///  * [IterableProperty], which handles iterable values with display
    ///    customizable depending on the [DiagnosticsTreeStyle] used.
    ///  * [ObjectFlagProperty], which provides terse descriptions of whether a
    ///    property value is present or not. For example, whether an `onClick`
    ///    callback is specified or an animation is in progress.
    ///
    /// If none of these subclasses apply, use the [DiagnosticsProperty]
    /// constructor or in rare cases create your own [DiagnosticsProperty]
    /// subclass as in the case for [TransformProperty] which handles [Matrix4]
    /// that represent transforms. Generally any property value with a good
    /// `toString` method implementation works fine using [DiagnosticsProperty]
    /// directly.
    ///
    /// ## Sample code
    ///
    /// This example shows best practices for implementing [debugFillProperties]
    /// illustrating use of all common [DiagnosticsProperty] subclasses and all
    /// common [DiagnosticsProperty] parameters.
    ///
    /// ```dart
    /// class ExampleObject extends ExampleSuperclass {
    ///
    ///   // ...various members and properties...
    ///
    ///   @override
    ///   void debugFillProperties(DiagnosticPropertiesBuilder properties) {
    ///     // Always add properties from the base class first.
    ///     super.debugFillProperties(properties);
    ///
    ///     // Omit the property name 'message' when displaying this String property
    ///     // as it would just add visual noise.
    ///     properties.add(new StringProperty('message', message, showName: false));
    ///
    ///     properties.add(new DoubleProperty('stepWidth', stepWidth));
    ///
    ///     // A scale of 1.0 does nothing so should be hidden.
    ///     properties.add(new DoubleProperty('scale', scale, defaultValue: 1.0));
    ///
    ///     // If the hitTestExtent matches the paintExtent, it is just set to its
    ///     // default value so is not relevant.
    ///     properties.add(new DoubleProperty('hitTestExtent', hitTestExtent, defaultValue: paintExtent));
    ///
    ///     // maxWidth of double.infinity indicates the width is unconstrained and
    ///     // so maxWidth has no impact.,
    ///     properties.add(new DoubleProperty('maxWidth', maxWidth, defaultValue: double.infinity));
    ///
    ///     // Progress is a value between 0 and 1 or null. Showing it as a
    ///     // percentage makes the meaning clear enough that the name can be
    ///     // hidden.
    ///     properties.add(new PercentProperty(
    ///       'progress',
    ///       progress,
    ///       showName: false,
    ///       ifNull: '<indeterminate>',
    ///     ));
    ///
    ///     // Most text fields have maxLines set to 1.
    ///     properties.add(new IntProperty('maxLines', maxLines, defaultValue: 1));
    ///
    ///     // Specify the unit as otherwise it would be unclear that time is in
    ///     // milliseconds.
    ///     properties.add(new IntProperty('duration', duration.inMilliseconds, unit: 'ms'));
    ///
    ///     // Tooltip is used instead of unit for this case as a unit should be a
    ///     // terse description appropriate to display directly after a number
    ///     // without a space.
    ///     properties.add(new DoubleProperty(
    ///       'device pixel ratio',
    ///       ui.window.devicePixelRatio,
    ///       tooltip: 'physical pixels per logical pixel',
    ///     ));
    ///
    ///     // Displaying the depth value would be distracting. Instead only display
    ///     // if the depth value is missing.
    ///     properties.add(new ObjectFlagProperty<int>('depth', depth, ifNull: 'no depth'));
    ///
    ///     // bool flag that is only shown when the value is true.
    ///     properties.add(new FlagProperty('using primary controller', value: primary));
    ///
    ///     properties.add(new FlagProperty(
    ///       'isCurrent',
    ///       value: isCurrent,
    ///       ifTrue: 'active',
    ///       ifFalse: 'inactive',
    ///       showName: false,
    ///     ));
    ///
    ///     properties.add(new DiagnosticsProperty<bool>('keepAlive', keepAlive));
    ///
    ///     // FlagProperty could have also been used in this case.
    ///     // This option results in the text "obscureText: true" instead
    ///     // of "obscureText" which is a bit more verbose but a bit clearer.
    ///     properties.add(new DiagnosticsProperty<bool>('obscureText', obscureText, defaultValue: false));
    ///
    ///     properties.add(new EnumProperty<TextAlign>('textAlign', textAlign, defaultValue: null));
    ///     properties.add(new EnumProperty<ImageRepeat>('repeat', repeat, defaultValue: ImageRepeat.noRepeat));
    ///
    ///     // Warn users when the widget is missing but do not show the value.
    ///     properties.add(new ObjectFlagProperty<Widget>('widget', widget, ifNull: 'no widget'));
    ///
    ///     properties.add(new IterableProperty<BoxShadow>(
    ///       'boxShadow',
    ///       boxShadow,
    ///       defaultValue: null,
    ///       style: style,
    ///     ));
    ///
    ///     // Getting the value of size throws an exception unless hasSize is true.
    ///     properties.add(new DiagnosticsProperty<Size>.lazy(
    ///       'size',
    ///       () => size,
    ///       description: '${ hasSize ? size : "MISSING" }',
    ///     ));
    ///
    ///     // If the `toString` method for the property value does not provide a
    ///     // good terse description, write a DiagnosticsProperty subclass as in
    ///     // the case of TransformProperty which displays a nice debugging view
    ///     // of a Matrix4 that represents a transform.
    ///     properties.add(new TransformProperty('transform', transform));
    ///
    ///     // If the value class has a good `toString` method, use
    ///     // DiagnosticsProperty<YourValueType>. Specifying the value type ensures
    ///     // that debugging tools always know the type of the field and so can
    ///     // provide the right UI affordances. For example, in this case even
    ///     // if color is null, a debugging tool still knows the value is a Color
    ///     // and can display relevant color related UI.
    ///     properties.add(new DiagnosticsProperty<Color>('color', color));
    ///
    ///     // Use a custom description to generate a more terse summary than the
    ///     // `toString` method on the map class.
    ///     properties.add(new DiagnosticsProperty<Map<Listenable, VoidCallback>>(
    ///       'handles',
    ///       handles,
    ///       description: handles != null ?
    ///       '${handles.length} active client${ handles.length == 1 ? "" : "s" }' :
    ///       null,
    ///       ifNull: 'no notifications ever received',
    ///       showName: false,
    ///     ));
    ///   }
    /// }
    /// ```
    ///
    /// Used by [toDiagnosticsNode] and [toString].
    open fun debugFillProperties(properties: DiagnosticPropertiesBuilder) { }
}