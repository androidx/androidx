package androidx.ui.semantics

import androidx.ui.engine.text.TextDirection
import kotlin.math.sign

// / A group of [nodes] that are disjoint vertically or horizontally from other
// / nodes that share the same [SemanticsNode] parent.
// /
// / The [nodes] are sorted among each other separately from other nodes.
internal data class _SemanticsSortGroup(
    // / The offset from the start edge of the parent [SemanticsNode] in the
    // / direction of the traversal.
    // /
    // / This value is equal to the [_BoxEdge.offset] of the first node in the
    // / [nodes] list being considered.
    val startOffset: Double,

    val textDirection: TextDirection?

) : Comparable<_SemanticsSortGroup> {
    init {
        assert(startOffset != null)
    }

    // / The nodes that are sorted among each other.
    val nodes: MutableList<SemanticsNode> = mutableListOf()

    override fun compareTo(other: _SemanticsSortGroup): Int {
        return (startOffset - other.startOffset).sign.toInt()
    }

    //
//  /// Sorts this group assuming that [nodes] belong to the same vertical group.
//  ///
//  /// This method breaks up this group into horizontal [_SemanticsSortGroup]s
//  /// then sorts them using [sortedWithinKnot].
    fun sortedWithinVerticalGroup(): List<SemanticsNode> {
        TODO()
//    final List<_BoxEdge> edges = <_BoxEdge>[];
//    for (SemanticsNode child in nodes) {
//      edges.add(new _BoxEdge(
//        isLeadingEdge: true,
//        offset: _pointInParentCoordinates(child, child.rect.topLeft).dx,
//        node: child,
//      ));
//      edges.add(new _BoxEdge(
//        isLeadingEdge: false,
//        offset: _pointInParentCoordinates(child, child.rect.bottomRight).dx,
//        node: child,
//      ));
//    }
//    edges.sort();
//
//    List<_SemanticsSortGroup> horizontalGroups = <_SemanticsSortGroup>[];
//    _SemanticsSortGroup group;
//    int depth = 0;
//    for (_BoxEdge edge in edges) {
//      if (edge.isLeadingEdge) {
//        depth += 1;
//        group ??= new _SemanticsSortGroup(
//          startOffset: edge.offset,
//          textDirection: textDirection,
//        );
//        group.nodes.add(edge.node);
//      } else {
//        depth -= 1;
//      }
//      if (depth == 0) {
//        horizontalGroups.add(group);
//        group = null;
//      }
//    }
//    horizontalGroups.sort();
//
//    if (textDirection == TextDirection.rtl) {
//      horizontalGroups = horizontalGroups.reversed.toList();
//    }
//
//    final List<SemanticsNode> result = <SemanticsNode>[];
//    for (_SemanticsSortGroup group in horizontalGroups) {
//      final List<SemanticsNode> sortedKnotNodes = group.sortedWithinKnot();
//      result.addAll(sortedKnotNodes);
//    }
//    return result;
    }
//
//  /// Sorts [nodes] where nodes intersect both vertically and horizontally.
//  ///
//  /// In the special case when [nodes] contains one or less nodes, this method
//  /// returns [nodes] unchanged.
//  ///
//  /// This method constructs a graph, where vertices are [SemanticsNode]s and
//  /// edges are "traversed before" relation between pairs of nodes. The sort
//  /// order is the topological sorting of the graph, with the original order of
//  /// [nodes] used as the tie breaker.
//  ///
//  /// Whether a node is traversed before another node is determined by the
//  /// vector that connects the two nodes' centers. If the vector "points to the
//  /// right or down", defined as the [Offset.direction] being between `-pi/4`
//  /// and `3*pi/4`), then the semantics node whose center is at the end of the
//  /// vector is said to be traversed after.
//  List<SemanticsNode> sortedWithinKnot() {
//    if (nodes.length <= 1) {
//      // Trivial knot. Nothing to do.
//      return nodes;
//    }
//    final Map<int, SemanticsNode> nodeMap = <int, SemanticsNode>{};
//    final Map<int, int> edges = <int, int>{};
//    for (SemanticsNode node in nodes) {
//      nodeMap[node.id] = node;
//      val center: Offset = _pointInParentCoordinates(node, node.rect.center);
//      for (SemanticsNode nextNode in nodes) {
//        if (identical(node, nextNode) || edges[nextNode.id] == node.id) {
//          // Skip self or when we've already established that the next node
//          // points to current node.
//          continue;
//        }
//
//        val nextCenter: Offset = _pointInParentCoordinates(nextNode, nextNode.rect.center);
//        val centerDelta: Offset = nextCenter - center;
//        // When centers coincide, direction is 0.0.
//        val direction: Double = centerDelta.direction;
//        val isLtrAndForward: Boolean = textDirection == TextDirection.ltr &&
//            -math.pi / 4 < direction && direction < 3 * math.pi / 4;
//        val isRtlAndForward: Boolean = textDirection == TextDirection.rtl &&
//            (direction < -3 * math.pi / 4 || direction > 3 * math.pi / 4);
//        if (isLtrAndForward || isRtlAndForward) {
//          edges[node.id] = nextNode.id;
//        }
//      }
//    }
//
//    final List<int> sortedIds = <int>[];
//    final Set<int> visitedIds = new Set<int>();
//    final List<SemanticsNode> startNodes = nodes.toList()..sort((SemanticsNode a, SemanticsNode b) {
//      val aTopLeft: Offset = _pointInParentCoordinates(a, a.rect.topLeft);
//      val bTopLeft: Offset = _pointInParentCoordinates(b, b.rect.topLeft);
//      val verticalDiff: int = aTopLeft.dy.compareTo(bTopLeft.dy);
//      if (verticalDiff != 0) {
//        return -verticalDiff;
//      }
//      return -aTopLeft.dx.compareTo(bTopLeft.dx);
//    });
//
//    void search(int id) {
//      if (visitedIds.contains(id)) {
//        return;
//      }
//      visitedIds.add(id);
//      if (edges.containsKey(id)) {
//        search(edges[id]);
//      }
//      sortedIds.add(id);
//    }
//
//    startNodes.map((SemanticsNode node) => node.id).forEach(search);
//    return sortedIds.map<SemanticsNode>((int id) => nodeMap[id]).toList().reversed.toList();
//  }

    // TODO(Migration/ryanmentley): Should these compare by values?
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}