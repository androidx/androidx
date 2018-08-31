package androidx.ui.semantics

// /// Converts `point` to the `node`'s parent's coordinate system.
// Offset _pointInParentCoordinates(SemanticsNode node, Offset point) {
//  if (node.transform == null) {
//    return point;
//  }
//  final Vector3 vector = new Vector3(point.dx, point.dy, 0.0);
//  node.transform.transform3(vector);
//  return new Offset(vector.x, vector.y);
// }
//
// /// Sorts `children` using the default sorting algorithm, and returns them as a
// /// new list.
// ///
// /// The algorithm first breaks up children into groups such that no two nodes
// /// from different groups overlap vertically. These groups are sorted vertically
// /// according to their [_SemanticsSortGroup.startOffset].
// ///
// /// Within each group, the nodes are sorted using
// /// [_SemanticsSortGroup.sortedWithinVerticalGroup].
// ///
// /// For an illustration of the algorithm see http://bit.ly/flutter-default-traversal.
// List<SemanticsNode> _childrenInDefaultOrder(List<SemanticsNode> children, TextDirection textDirection) {
//  final List<_BoxEdge> edges = <_BoxEdge>[];
//  for (SemanticsNode child in children) {
//    edges.add(new _BoxEdge(
//      isLeadingEdge: true,
//      offset: _pointInParentCoordinates(child, child.rect.topLeft).dy,
//      node: child,
//    ));
//    edges.add(new _BoxEdge(
//      isLeadingEdge: false,
//      offset: _pointInParentCoordinates(child, child.rect.bottomRight).dy,
//      node: child,
//    ));
//  }
//  edges.sort();
//
//  final List<_SemanticsSortGroup> verticalGroups = <_SemanticsSortGroup>[];
//  _SemanticsSortGroup group;
//  int depth = 0;
//  for (_BoxEdge edge in edges) {
//    if (edge.isLeadingEdge) {
//      depth += 1;
//      group ??= new _SemanticsSortGroup(
//        startOffset: edge.offset,
//        textDirection: textDirection,
//      );
//      group.nodes.add(edge.node);
//    } else {
//      depth -= 1;
//    }
//    if (depth == 0) {
//      verticalGroups.add(group);
//      group = null;
//    }
//  }
//  verticalGroups.sort();
//
//  final List<SemanticsNode> result = <SemanticsNode>[];
//  for (_SemanticsSortGroup group in verticalGroups) {
//    final List<SemanticsNode> sortedGroupNodes = group.sortedWithinVerticalGroup();
//    result.addAll(sortedGroupNodes);
//  }
//  return result;
// }
//
// /// The implementation of [Comparable] that implements the ordering of
// /// [SemanticsNode]s in the accessibility traversal.
// ///
// /// [SemanticsNode]s are sorted prior to sending them to the engine side.
// ///
// /// This implementation considers a [node]'s [sortKey] and its position within
// /// the list of its siblings. [sortKey] takes precedence over position.
private class _TraversalSortNode : Comparable<_TraversalSortNode> {
    override fun compareTo(other: _TraversalSortNode): Int {
        TODO("not implemented")
    }
//  _TraversalSortNode({
//    @required this.node,
//    this.sortKey,
//    @required this.position,
//  })
//    : assert(node != null),
//      assert(position != null);
//
//  /// The node whose position this sort node determines.
//  final SemanticsNode node;
//
//  /// Determines the position of this node among its siblings.
//  ///
//  /// Sort keys take precedence over other attributes, such as
//  /// [position].
//  final SemanticsSortKey sortKey;
//
//  /// Position within the list of siblings as determined by the default sort
//  /// order.
//  final int position;
//
//  @override
//  int compareTo(_TraversalSortNode other) {
//    if (sortKey == null || other?.sortKey == null) {
//      return position - other.position;
//    }
//    return sortKey.compareTo(other.sortKey);
//  }
}