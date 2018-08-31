package androidx.ui.semantics

import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.Diagnosticable

// / Base class for all sort keys for [Semantics] accessibility traversal order
// / sorting.
// /
// / Only keys of the same type and having matching [name]s are compared. If a
// / list of sibling [SemanticsNode]s contains keys that are not comparable with
// / each other the list is first sorted using the default sorting algorithm.
// / Then the nodes are broken down into groups by moving comparable nodes
// / towards the _earliest_ node in the group. Finally each group is sorted by
// / sort key and the resulting list is made by concatenating the sorted groups
// / back.
// /
// / For example, let's take nodes (C, D, B, E, A, F). Let's assign node A key 1,
// / node B key 2, node C key 3. Let's also assume that the default sort order
// / leaves the original list intact. Because nodes A, B, and C, have comparable
// / sort key, they will form a group by pulling all nodes towards the earliest
// / node, which is C. The result is group (C, B, A). The remaining nodes D, E,
// / F, form a second group with sort key being `null`. The first group is sorted
// / using their sort keys becoming (A, B, C). The second group is left as is
// / because it does not specify sort keys. Then we concatenate the two groups -
// / (A, B, C) and (D, E, F) - into the final (A, B, C, D, E, F).
// /
// / Because of the complexity introduced by incomparable sort keys among sibling
// / nodes, it is recommended to either use comparable keys for all nodes, or
// / use null for all of them, leaving the sort order to the default algorithm.
// /
// / See Also:
// /
// /  * [SemanticsSortOrder] which manages a list of sort keys.
// /  * [OrdinalSortKey] for a sort key that sorts using an ordinal.
abstract class SemanticsSortKey(
    // / An optional name that will make this sort key only order itself
    // / with respect to other sort keys of the same [name], as long as
    // / they are of the same [runtimeType].
    val name: String?
) : Diagnosticable, Comparable<SemanticsSortKey> {

//  @override
//  int compareTo(SemanticsSortKey other) {
//    // The sorting algorithm must not compare incomparable keys.
//    assert(runtimeType == other.runtimeType);
//    assert(name == other.name);
//    return doCompare(other);
//  }
//
//  /// The implementation of [compareTo].
//  ///
//  /// The argument is guaranteed to be of the same type as this object and have
//  /// the same [name].
//  ///
//  /// The method should return a negative number if this object comes earlier in
//  /// the sort order than the argument; and a positive number if it comes later
//  /// in the sort order. Returning zero causes the system to use default sort
//  /// order.
//  @protected
//  int doCompare(covariant SemanticsSortKey other);
//

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        TODO("Not implemented")
//    super.debugFillProperties(properties);
//    properties.add(new StringProperty('name', name, defaultValue: null));
    }
}