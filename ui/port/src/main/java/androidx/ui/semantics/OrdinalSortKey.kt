package androidx.ui.semantics

// / A [SemanticsSortKey] that sorts simply based on the `double` value it is
// / given.
// /
// / The [OrdinalSortKey] compares itself with other [OrdinalSortKey]s
// / to sort based on the order it is given.
// /
// / The ordinal value `order` is typically a whole number, though it can be
// / fractional, e.g. in order to fit between two other consecutive whole
// / numbers. The value must be finite (it cannot be [double.nan],
// / [double.infinity], or [double.negativeInfinity]).
// /
// / See also:
// /
// /  * [SemanticsSortOrder] which manages a list of sort keys.
class OrdinalSortKey(
    // / Determines the placement of this key in a sequence of keys that defines
    // / the order in which this node is traversed by the platform's accessibility
    // / services.
    // /
    // / Lower values will be traversed first.
    val order: Double,
    name: String
) : SemanticsSortKey(name) {
    override fun compareTo(other: SemanticsSortKey): Int {
        TODO("not implemented")
    }
//  /// Creates a semantics sort key that uses a [double] as its key value.
//  ///
//  /// The [order] must be a finite number.
//  const OrdinalSortKey(
//    this.order, {
//    String name,
//  }) : assert(order != null),
//       assert(order != double.nan),
//       assert(order > double.negativeInfinity),
//       assert(order < double.infinity),
//       super(name: name);
//
//
//  @override
//  int doCompare(OrdinalSortKey other) {
//    if (other.order == null || order == null || other.order == order)
//      return 0;
//    return order.compareTo(other.order);
//  }
//
//  @override
//  void debugFillProperties(DiagnosticPropertiesBuilder properties) {
//    super.debugFillProperties(properties);
//    properties.add(new DoubleProperty('order', order, defaultValue: null));
//  }
}