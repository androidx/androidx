package androidx.ui.semantics

import androidx.ui.foundation.AbstractNode
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.foundation.diagnostics.DiagnosticsTreeStyle

//
// /// In tests use this function to reset the counter used to generate
// /// [SemanticsNode.id].
// void debugResetSemanticsIdCounter() {
//  SemanticsNode._lastIdentifier = 0;
// }
//
// / A node that represents some semantic data.
// /
// / The semantics tree is maintained during the semantics phase of the pipeline
// / (i.e., during [PipelineOwner.flushSemantics]), which happens after
// / compositing. The semantics tree is then uploaded into the engine for use
// / by assistive technology.
class SemanticsNode : AbstractNode(),
    DiagnosticableTree {
    //  /// Creates a semantic node.
//  ///
//  /// Each semantic node has a unique identifier that is assigned when the node
//  /// is created.
//  SemanticsNode({
//    this.key,
//    VoidCallback showOnScreen,
//  }) : id = _generateNewId(),
//       _showOnScreen = showOnScreen;
//
//  /// Creates a semantic node to represent the root of the semantics tree.
//  ///
//  /// The root node is assigned an identifier of zero.
//  SemanticsNode.root({
//    this.key,
//    VoidCallback showOnScreen,
//    SemanticsOwner owner
//  }) : id = 0,
//       _showOnScreen = showOnScreen {
//    attach(owner);
//  }
//
//  static int _lastIdentifier = 0;
//  static int _generateNewId() {
//    _lastIdentifier += 1;
//    return _lastIdentifier;
//  }
//
//  /// Uniquely identifies this node in the list of sibling nodes.
//  ///
//  /// Keys are used during the construction of the semantics tree. They are not
//  /// transferred to the engine.
//  final Key key;
//
//  /// The unique identifier for this node.
//  ///
//  /// The root node has an id of zero. Other nodes are given a unique id when
//  /// they are created.
//  final int id;
//
//  final VoidCallback _showOnScreen;
//
//  // GEOMETRY
//
//  /// The transform from this node's coordinate system to its parent's coordinate system.
//  ///
//  /// By default, the transform is null, which represents the identity
//  /// transformation (i.e., that this node has the same coordinate system as its
//  /// parent).
//  Matrix4 get transform => _transform;
//  Matrix4 _transform;
//  set transform(Matrix4 value) {
//    if (!MatrixUtils.matrixEquals(_transform, value)) {
//      _transform = MatrixUtils.isIdentity(value) ? null : value;
//      _markDirty();
//    }
//  }
//
//  /// The bounding box for this node in its coordinate system.
//  Rect get rect => _rect;
//  Rect _rect = Rect.zero;
//  set rect(Rect value) {
//    assert(value != null);
//    if (_rect != value) {
//      _rect = value;
//      _markDirty();
//    }
//  }
//
//  /// The semantic clip from an ancestor that was applied to this node.
//  ///
//  /// Expressed in the coordinate system of the node. May be null if no clip has
//  /// been applied.
//  ///
//  /// Descendant [SemanticsNode]s that are positioned outside of this rect will
//  /// be excluded from the semantics tree. Descendant [SemanticsNode]s that are
//  /// overlapping with this rect, but are outside of [parentPaintClipRect] will
//  /// be included in the tree, but they will be marked as hidden because they
//  /// are assumed to be not visible on screen.
//  ///
//  /// If this rect is null, all descendant [SemanticsNode]s outside of
//  /// [parentPaintClipRect] will be excluded from the tree.
//  ///
//  /// If this rect is non-null it has to completely enclose
//  /// [parentPaintClipRect]. If [parentPaintClipRect] is null this property is
//  /// also null.
//  Rect parentSemanticsClipRect;
//
//  /// The paint clip from an ancestor that was applied to this node.
//  ///
//  /// Expressed in the coordinate system of the node. May be null if no clip has
//  /// been applied.
//  ///
//  /// Descendant [SemanticsNode]s that are positioned outside of this rect will
//  /// either be excluded from the semantics tree (if they have no overlap with
//  /// [parentSemanticsClipRect]) or they will be included and marked as hidden
//  /// (if they are overlapping with [parentSemanticsClipRect]).
//  ///
//  /// This rect is completely enclosed by [parentSemanticsClipRect].
//  ///
//  /// If this rect is null [parentSemanticsClipRect] also has to be null.
//  Rect parentPaintClipRect;
//
//  /// Whether the node is invisible.
//  ///
//  /// A node whose [rect] is outside of the bounds of the screen and hence not
//  /// reachable for users is considered invisible if its semantic information
//  /// is not merged into a (partially) visible parent as indicated by
//  /// [isMergedIntoParent].
//  ///
//  /// An invisible node can be safely dropped from the semantic tree without
//  /// loosing semantic information that is relevant for describing the content
//  /// currently shown on screen.
//  bool get isInvisible => !isMergedIntoParent && rect.isEmpty;
//
//  // MERGING
//
//  /// Whether this node merges its semantic information into an ancestor node.
//  bool get isMergedIntoParent => _isMergedIntoParent;
//  bool _isMergedIntoParent = false;
//  set isMergedIntoParent(bool value) {
//    assert(value != null);
//    if (_isMergedIntoParent == value)
//      return;
//    _isMergedIntoParent = value;
//    _markDirty();
//  }
//
//  /// Whether this node is taking part in a merge of semantic information.
//  ///
//  /// This returns true if the node is either merged into an ancestor node or if
//  /// decedent nodes are merged into this node.
//  ///
//  /// See also:
//  ///
//  ///  * [isMergedIntoParent]
//  ///  * [mergeAllDescendantsIntoThisNode]
//  bool get isPartOfNodeMerging => mergeAllDescendantsIntoThisNode || isMergedIntoParent;
//
//  /// Whether this node and all of its descendants should be treated as one logical entity.
//  bool get mergeAllDescendantsIntoThisNode => _mergeAllDescendantsIntoThisNode;
//  bool _mergeAllDescendantsIntoThisNode = _kEmptyConfig.isMergingSemanticsOfDescendants;
//
//
//  // CHILDREN
//
//  /// Contains the children in inverse hit test order (i.e. paint order).
//  List<SemanticsNode> _children;
//
//  /// A snapshot of `newChildren` passed to [_replaceChildren] that we keep in
//  /// debug mode. It supports the assertion that user does not mutate the list
//  /// of children.
//  List<SemanticsNode> _debugPreviousSnapshot;
//
//  void _replaceChildren(List<SemanticsNode> newChildren) {
//    assert(!newChildren.any((SemanticsNode child) => child == this));
//    assert(() {
//      if (identical(newChildren, _children)) {
//        final StringBuffer mutationErrors = new StringBuffer();
//        if (newChildren.length != _debugPreviousSnapshot.length) {
//          mutationErrors.writeln(
//            'The list\'s length has changed from ${_debugPreviousSnapshot.length} '
//            'to ${newChildren.length}.'
//          );
//        } else {
//          for (int i = 0; i < newChildren.length; i++) {
//            if (!identical(newChildren[i], _debugPreviousSnapshot[i])) {
//              mutationErrors.writeln(
//                'Child node at position $i was replaced:\n'
//                'Previous child: ${newChildren[i]}\n'
//                'New child: ${_debugPreviousSnapshot[i]}\n'
//              );
//            }
//          }
//        }
//        if (mutationErrors.isNotEmpty) {
//          throw new FlutterError(
//            'Failed to replace child semantics nodes because the list of `SemanticsNode`s was mutated.\n'
//            'Instead of mutating the existing list, create a new list containing the desired `SemanticsNode`s.\n'
//            'Error details:\n'
//            '$mutationErrors'
//          );
//        }
//      }
//      assert(!newChildren.any((SemanticsNode node) => node.isMergedIntoParent) || isPartOfNodeMerging);
//
//      _debugPreviousSnapshot = new List<SemanticsNode>.from(newChildren);
//
//      SemanticsNode ancestor = this;
//      while (ancestor.parent is SemanticsNode)
//        ancestor = ancestor.parent;
//      assert(!newChildren.any((SemanticsNode child) => child == ancestor));
//      return true;
//    }());
//    assert(() {
//      final Set<SemanticsNode> seenChildren = new Set<SemanticsNode>();
//      for (SemanticsNode child in newChildren)
//        assert(seenChildren.add(child)); // check for duplicate adds
//      return true;
//    }());
//
//    // The goal of this function is updating sawChange.
//    if (_children != null) {
//      for (SemanticsNode child in _children)
//        child._dead = true;
//    }
//    if (newChildren != null) {
//      for (SemanticsNode child in newChildren) {
//        assert(!child.isInvisible, 'Child $child is invisible and should not be added as a child of $this.');
//        child._dead = false;
//      }
//    }
//    bool sawChange = false;
//    if (_children != null) {
//      for (SemanticsNode child in _children) {
//        if (child._dead) {
//          if (child.parent == this) {
//            // we might have already had our child stolen from us by
//            // another node that is deeper in the tree.
//            dropChild(child);
//          }
//          sawChange = true;
//        }
//      }
//    }
//    if (newChildren != null) {
//      for (SemanticsNode child in newChildren) {
//        if (child.parent != this) {
//          if (child.parent != null) {
//            // we're rebuilding the tree from the bottom up, so it's possible
//            // that our child was, in the last pass, a child of one of our
//            // ancestors. In that case, we drop the child eagerly here.
//            // TODO(ianh): Find a way to assert that the same node didn't
//            // actually appear in the tree in two places.
//            child.parent?.dropChild(child);
//          }
//          assert(!child.attached);
//          adoptChild(child);
//          sawChange = true;
//        }
//      }
//    }
//    if (!sawChange && _children != null) {
//      assert(newChildren != null);
//      assert(newChildren.length == _children.length);
//      // Did the order change?
//      for (int i = 0; i < _children.length; i++) {
//        if (_children[i].id != newChildren[i].id) {
//          sawChange = true;
//          break;
//        }
//      }
//    }
//    _children = newChildren;
//    if (sawChange)
//      _markDirty();
//  }
//
//  /// Whether this node has a non-zero number of children.
//  bool get hasChildren => _children?.isNotEmpty ?? false;
//  bool _dead = false;
//
//  /// The number of children this node has.
//  int get childrenCount => hasChildren ? _children.length : 0;
//
//  /// Visits the immediate children of this node.
//  ///
//  /// This function calls visitor for each immediate child until visitor returns
//  /// false. Returns true if all the visitor calls returned true, otherwise
//  /// returns false.
//  void visitChildren(SemanticsNodeVisitor visitor) {
//    if (_children != null) {
//      for (SemanticsNode child in _children) {
//        if (!visitor(child))
//          return;
//      }
//    }
//  }
//
//  /// Visit all the descendants of this node.
//  ///
//  /// This function calls visitor for each descendant in a pre-order traversal
//  /// until visitor returns false. Returns true if all the visitor calls
//  /// returned true, otherwise returns false.
//  bool _visitDescendants(SemanticsNodeVisitor visitor) {
//    if (_children != null) {
//      for (SemanticsNode child in _children) {
//        if (!visitor(child) || !child._visitDescendants(visitor))
//          return false;
//      }
//    }
//    return true;
//  }
//
//  // AbstractNode OVERRIDES
//
//  @override
//  SemanticsOwner get owner => super.owner;
//
//  @override
//  SemanticsNode get parent => super.parent;
//
//  @override
//  void redepthChildren() {
//    _children?.forEach(redepthChild);
//  }
//
//  @override
//  void attach(SemanticsOwner owner) {
//    super.attach(owner);
//    assert(!owner._nodes.containsKey(id));
//    owner._nodes[id] = this;
//    owner._detachedNodes.remove(this);
//    if (_dirty) {
//      _dirty = false;
//      _markDirty();
//    }
//    if (_children != null) {
//      for (SemanticsNode child in _children)
//        child.attach(owner);
//    }
//  }
//
//  @override
//  void detach() {
//    assert(owner._nodes.containsKey(id));
//    assert(!owner._detachedNodes.contains(this));
//    owner._nodes.remove(id);
//    owner._detachedNodes.add(this);
//    super.detach();
//    assert(owner == null);
//    if (_children != null) {
//      for (SemanticsNode child in _children) {
//        // The list of children may be stale and may contain nodes that have
//        // been assigned to a different parent.
//        if (child.parent == this)
//          child.detach();
//      }
//    }
//    // The other side will have forgotten this node if we ever send
//    // it again, so make sure to mark it dirty so that it'll get
//    // sent if it is resurrected.
//    _markDirty();
//  }
//
//  // DIRTY MANAGEMENT
//
//  bool _dirty = false;
//  void _markDirty() {
//    if (_dirty)
//      return;
//    _dirty = true;
//    if (attached) {
//      assert(!owner._detachedNodes.contains(this));
//      owner._dirtyNodes.add(this);
//    }
//  }
//
//  bool _isDifferentFromCurrentSemanticAnnotation(SemanticsConfiguration config) {
//    return _label != config.label ||
//        _hint != config.hint ||
//        _decreasedValue != config.decreasedValue ||
//        _value != config.value ||
//        _increasedValue != config.increasedValue ||
//        _flags != config._flags ||
//        _textDirection != config.textDirection ||
//        _sortKey != config._sortKey ||
//        _textSelection != config._textSelection ||
//        _scrollPosition != config._scrollPosition ||
//        _scrollExtentMax != config._scrollExtentMax ||
//        _scrollExtentMin != config._scrollExtentMin ||
//        _actionsAsBits != config._actionsAsBits ||
//        _mergeAllDescendantsIntoThisNode != config.isMergingSemanticsOfDescendants;
//  }
//
//  // TAGS, LABELS, ACTIONS
//
//  Map<SemanticsAction, _SemanticsActionHandler> _actions = _kEmptyConfig._actions;
//  Map<CustomSemanticsAction, VoidCallback> _customSemanticsActions = _kEmptyConfig._customSemanticsActions;
//
//  int _actionsAsBits = _kEmptyConfig._actionsAsBits;
//
//  /// The [SemanticsTag]s this node is tagged with.
//  ///
//  /// Tags are used during the construction of the semantics tree. They are not
//  /// transferred to the engine.
//  Set<SemanticsTag> tags;
//
//  /// Whether this node is tagged with `tag`.
//  bool isTagged(SemanticsTag tag) => tags != null && tags.contains(tag);
//
//  int _flags = _kEmptyConfig._flags;
//
//  bool _hasFlag(SemanticsFlag flag) => _flags & flag.index != 0;
//
//  /// A textual description of this node.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get label => _label;
//  String _label = _kEmptyConfig.label;
//
//  /// A textual description for the current value of the node.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get value => _value;
//  String _value = _kEmptyConfig.value;
//
//  /// The value that [value] will have after a [SemanticsAction.decrease] action
//  /// has been performed.
//  ///
//  /// This property is only valid if the [SemanticsAction.decrease] action is
//  /// available on this node.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get decreasedValue => _decreasedValue;
//  String _decreasedValue = _kEmptyConfig.decreasedValue;
//
//  /// The value that [value] will have after a [SemanticsAction.increase] action
//  /// has been performed.
//  ///
//  /// This property is only valid if the [SemanticsAction.increase] action is
//  /// available on this node.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get increasedValue => _increasedValue;
//  String _increasedValue = _kEmptyConfig.increasedValue;
//
//  /// A brief description of the result of performing an action on this node.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get hint => _hint;
//  String _hint = _kEmptyConfig.hint;
//
//  /// Provides hint values which override the default hints on supported
//  /// platforms.
//  SemanticsHintOverrides get hintOverrides => _hintOverrides;
//  SemanticsHintOverrides _hintOverrides;
//
//  /// The reading direction for [label], [value], [hint], [increasedValue], and
//  /// [decreasedValue].
//  TextDirection get textDirection => _textDirection;
//  TextDirection _textDirection = _kEmptyConfig.textDirection;
//
//  /// Determines the position of this node among its siblings in the traversal
//  /// sort order.
//  ///
//  /// This is used to describe the order in which the semantic node should be
//  /// traversed by the accessibility services on the platform (e.g. VoiceOver
//  /// on iOS and TalkBack on Android).
//  SemanticsSortKey get sortKey => _sortKey;
//  SemanticsSortKey _sortKey;
//
//  /// The currently selected text (or the position of the cursor) within [value]
//  /// if this node represents a text field.
//  TextSelection get textSelection => _textSelection;
//  TextSelection _textSelection;
//
//  /// Indicates the current scrolling position in logical pixels if the node is
//  /// scrollable.
//  ///
//  /// The properties [scrollExtentMin] and [scrollExtentMax] indicate the valid
//  /// in-range values for this property. The value for [scrollPosition] may
//  /// (temporarily) be outside that range, e.g. during an overscroll.
//  ///
//  /// See also:
//  ///
//  ///  * [ScrollPosition.pixels], from where this value is usually taken.
//  double get scrollPosition => _scrollPosition;
//  double _scrollPosition;
//
//
//  /// Indicates the maximum in-range value for [scrollPosition] if the node is
//  /// scrollable.
//  ///
//  /// This value may be infinity if the scroll is unbound.
//  ///
//  /// See also:
//  ///
//  ///  * [ScrollPosition.maxScrollExtent], from where this value is usually taken.
//  double get scrollExtentMax => _scrollExtentMax;
//  double _scrollExtentMax;
//
//  /// Indicates the minimum in-range value for [scrollPosition] if the node is
//  /// scrollable.
//  ///
//  /// This value may be infinity if the scroll is unbound.
//  ///
//  /// See also:
//  ///
//  ///  * [ScrollPosition.minScrollExtent] from where this value is usually taken.
//  double get scrollExtentMin => _scrollExtentMin;
//  double _scrollExtentMin;
//
//  bool _canPerformAction(SemanticsAction action) => _actions.containsKey(action);
//
//  static final SemanticsConfiguration _kEmptyConfig = new SemanticsConfiguration();
//
//  /// Reconfigures the properties of this object to describe the configuration
//  /// provided in the `config` argument and the children listed in the
//  /// `childrenInInversePaintOrder` argument.
//  ///
//  /// The arguments may be null; this represents an empty configuration (all
//  /// values at their defaults, no children).
//  ///
//  /// No reference is kept to the [SemanticsConfiguration] object, but the child
//  /// list is used as-is and should therefore not be changed after this call.
//  void updateWith({
//    @required SemanticsConfiguration config,
//    List<SemanticsNode> childrenInInversePaintOrder,
//  }) {
//    config ??= _kEmptyConfig;
//    if (_isDifferentFromCurrentSemanticAnnotation(config))
//      _markDirty();
//
//    _label = config.label;
//    _decreasedValue = config.decreasedValue;
//    _value = config.value;
//    _increasedValue = config.increasedValue;
//    _hint = config.hint;
//    _hintOverrides = config.hintOverrides;
//    _flags = config._flags;
//    _textDirection = config.textDirection;
//    _sortKey = config.sortKey;
//    _actions = new Map<SemanticsAction, _SemanticsActionHandler>.from(config._actions);
//    _customSemanticsActions = new Map<CustomSemanticsAction, VoidCallback>.from(config._customSemanticsActions);
//    _actionsAsBits = config._actionsAsBits;
//    _textSelection = config._textSelection;
//    _scrollPosition = config._scrollPosition;
//    _scrollExtentMax = config._scrollExtentMax;
//    _scrollExtentMin = config._scrollExtentMin;
//    _mergeAllDescendantsIntoThisNode = config.isMergingSemanticsOfDescendants;
//    _replaceChildren(childrenInInversePaintOrder ?? const <SemanticsNode>[]);
//
//    assert(
//      !_canPerformAction(SemanticsAction.increase) || (_value == '') == (_increasedValue == ''),
//      'A SemanticsNode with action "increase" needs to be annotated with either both "value" and "increasedValue" or neither',
//    );
//    assert(
//      !_canPerformAction(SemanticsAction.decrease) || (_value == '') == (_decreasedValue == ''),
//      'A SemanticsNode with action "increase" needs to be annotated with either both "value" and "decreasedValue" or neither',
//    );
//  }
//
//
//  /// Returns a summary of the semantics for this node.
//  ///
//  /// If this node has [mergeAllDescendantsIntoThisNode], then the returned data
//  /// includes the information from this node's descendants. Otherwise, the
//  /// returned data matches the data on this node.
//  SemanticsData getSemanticsData() {
//    int flags = _flags;
//    int actions = _actionsAsBits;
//    String label = _label;
//    String hint = _hint;
//    String value = _value;
//    String increasedValue = _increasedValue;
//    String decreasedValue = _decreasedValue;
//    TextDirection textDirection = _textDirection;
//    Set<SemanticsTag> mergedTags = tags == null ? null : new Set<SemanticsTag>.from(tags);
//    TextSelection textSelection = _textSelection;
//    double scrollPosition = _scrollPosition;
//    double scrollExtentMax = _scrollExtentMax;
//    double scrollExtentMin = _scrollExtentMin;
//    final Set<int> customSemanticsActionIds = new Set<int>();
//    for (CustomSemanticsAction action in _customSemanticsActions.keys)
//      customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action));
//    if (hintOverrides != null) {
//      if (hintOverrides.onTapHint != null) {
//        final CustomSemanticsAction action = new CustomSemanticsAction.overridingAction(
//          hint: hintOverrides.onTapHint,
//          action: SemanticsAction.tap,
//        );
//        customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action));
//      }
//      if (hintOverrides.onLongPressHint != null) {
//        final CustomSemanticsAction action = new CustomSemanticsAction.overridingAction(
//          hint: hintOverrides.onLongPressHint,
//          action: SemanticsAction.longPress,
//        );
//        customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action));
//      }
//    }
//
//    if (mergeAllDescendantsIntoThisNode) {
//      _visitDescendants((SemanticsNode node) {
//        assert(node.isMergedIntoParent);
//        flags |= node._flags;
//        actions |= node._actionsAsBits;
//        textDirection ??= node._textDirection;
//        textSelection ??= node._textSelection;
//        scrollPosition ??= node._scrollPosition;
//        scrollExtentMax ??= node._scrollExtentMax;
//        scrollExtentMin ??= node._scrollExtentMin;
//        if (value == '' || value == null)
//          value = node._value;
//        if (increasedValue == '' || increasedValue == null)
//          increasedValue = node._increasedValue;
//        if (decreasedValue == '' || decreasedValue == null)
//          decreasedValue = node._decreasedValue;
//        if (node.tags != null) {
//          mergedTags ??= new Set<SemanticsTag>();
//          mergedTags.addAll(node.tags);
//        }
//        if (node._customSemanticsActions != null) {
//          for (CustomSemanticsAction action in _customSemanticsActions.keys)
//            customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action));
//        }
//        if (node.hintOverrides != null) {
//          if (node.hintOverrides.onTapHint != null) {
//            final CustomSemanticsAction action = new CustomSemanticsAction.overridingAction(
//              hint: node.hintOverrides.onTapHint,
//              action: SemanticsAction.tap,
//            );
//            customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action));
//          }
//          if (node.hintOverrides.onLongPressHint != null) {
//            final CustomSemanticsAction action = new CustomSemanticsAction.overridingAction(
//              hint: node.hintOverrides.onLongPressHint,
//              action: SemanticsAction.longPress,
//            );
//            customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action));
//          }
//        }
//        label = _concatStrings(
//          thisString: label,
//          thisTextDirection: textDirection,
//          otherString: node._label,
//          otherTextDirection: node._textDirection,
//        );
//        hint = _concatStrings(
//          thisString: hint,
//          thisTextDirection: textDirection,
//          otherString: node._hint,
//          otherTextDirection: node._textDirection,
//        );
//        return true;
//      });
//    }
//
//    return new SemanticsData(
//      flags: flags,
//      actions: actions,
//      label: label,
//      value: value,
//      increasedValue: increasedValue,
//      decreasedValue: decreasedValue,
//      hint: hint,
//      textDirection: textDirection,
//      rect: rect,
//      transform: transform,
//      tags: mergedTags,
//      textSelection: textSelection,
//      scrollPosition: scrollPosition,
//      scrollExtentMax: scrollExtentMax,
//      scrollExtentMin: scrollExtentMin,
//      customSemanticsActionIds: customSemanticsActionIds.toList()..sort(),
//    );
//  }
//
//  static Float64List _initIdentityTransform() {
//    return new Matrix4.identity().storage;
//  }
//
//  static final Int32List _kEmptyChildList = new Int32List(0);
//  static final Int32List _kEmptyCustomSemanticsActionsList = new Int32List(0);
//  static final Float64List _kIdentityTransform = _initIdentityTransform();
//
//  void _addToUpdate(ui.SemanticsUpdateBuilder builder, Set<int> customSemanticsActionIdsUpdate) {
//    assert(_dirty);
//    final SemanticsData data = getSemanticsData();
//    Int32List childrenInTraversalOrder;
//    Int32List childrenInHitTestOrder;
//    if (!hasChildren || mergeAllDescendantsIntoThisNode) {
//      childrenInTraversalOrder = _kEmptyChildList;
//      childrenInHitTestOrder = _kEmptyChildList;
//    } else {
//      final int childCount = _children.length;
//      final List<SemanticsNode> sortedChildren = _childrenInTraversalOrder();
//      childrenInTraversalOrder = new Int32List(childCount);
//      for (int i = 0; i < childCount; i += 1) {
//        childrenInTraversalOrder[i] = sortedChildren[i].id;
//      }
//      // _children is sorted in paint order, so we invert it to get the hit test
//      // order.
//      childrenInHitTestOrder = new Int32List(childCount);
//      for (int i = childCount - 1; i >= 0; i -= 1) {
//        childrenInHitTestOrder[i] = _children[childCount - i - 1].id;
//      }
//    }
//    Int32List customSemanticsActionIds;
//    if (data.customSemanticsActionIds?.isNotEmpty == true) {
//      customSemanticsActionIds = new Int32List(data.customSemanticsActionIds.length);
//      for (int i = 0; i < data.customSemanticsActionIds.length; i++) {
//        customSemanticsActionIds[i] = data.customSemanticsActionIds[i];
//        customSemanticsActionIdsUpdate.add(data.customSemanticsActionIds[i]);
//      }
//    }
//    builder.updateNode(
//      id: id,
//      flags: data.flags,
//      actions: data.actions,
//      rect: data.rect,
//      label: data.label,
//      value: data.value,
//      decreasedValue: data.decreasedValue,
//      increasedValue: data.increasedValue,
//      hint: data.hint,
//      textDirection: data.textDirection,
//      textSelectionBase: data.textSelection != null ? data.textSelection.baseOffset : -1,
//      textSelectionExtent: data.textSelection != null ? data.textSelection.extentOffset : -1,
//      scrollPosition: data.scrollPosition != null ? data.scrollPosition : double.nan,
//      scrollExtentMax: data.scrollExtentMax != null ? data.scrollExtentMax : double.nan,
//      scrollExtentMin: data.scrollExtentMin != null ? data.scrollExtentMin : double.nan,
//      transform: data.transform?.storage ?? _kIdentityTransform,
//      childrenInTraversalOrder: childrenInTraversalOrder,
//      childrenInHitTestOrder: childrenInHitTestOrder,
//      additionalActions: customSemanticsActionIds ?? _kEmptyCustomSemanticsActionsList,
//    );
//    _dirty = false;
//  }
//
//  /// Builds a new list made of [_children] sorted in semantic traversal order.
//  List<SemanticsNode> _childrenInTraversalOrder() {
//    TextDirection inheritedTextDirection = textDirection;
//    SemanticsNode ancestor = parent;
//    while (inheritedTextDirection == null && ancestor != null) {
//      inheritedTextDirection = ancestor.textDirection;
//      ancestor = ancestor.parent;
//    }
//
//    List<SemanticsNode> childrenInDefaultOrder;
//    if (inheritedTextDirection != null) {
//      childrenInDefaultOrder = _childrenInDefaultOrder(_children, inheritedTextDirection);
//    } else {
//      // In the absence of text direction default to paint order.
//      childrenInDefaultOrder = _children;
//    }
//
//    // List.sort does not guarantee stable sort order. Therefore, children are
//    // first partitioned into groups that have compatible sort keys, i.e. keys
//    // in the same group can be compared to each other. These groups stay in
//    // the same place. Only children within the same group are sorted.
//    final List<_TraversalSortNode> everythingSorted = <_TraversalSortNode>[];
//    final List<_TraversalSortNode> sortNodes = <_TraversalSortNode>[];
//    SemanticsSortKey lastSortKey;
//    for (int position = 0; position < childrenInDefaultOrder.length; position += 1) {
//      final SemanticsNode child = childrenInDefaultOrder[position];
//      final SemanticsSortKey sortKey = child.sortKey;
//      lastSortKey = position > 0
//          ? childrenInDefaultOrder[position - 1].sortKey
//          : null;
//      final bool isCompatibleWithPreviousSortKey = position == 0 ||
//          sortKey.runtimeType == lastSortKey.runtimeType &&
//          (sortKey == null || sortKey.name == lastSortKey.name);
//      if (!isCompatibleWithPreviousSortKey && sortNodes.isNotEmpty) {
//        // Do not sort groups with null sort keys. List.sort does not guarantee
//        // a stable sort order.
//        if (lastSortKey != null) {
//          sortNodes.sort();
//        }
//        everythingSorted.addAll(sortNodes);
//        sortNodes.clear();
//      }
//
//      sortNodes.add(new _TraversalSortNode(
//        node: child,
//        sortKey: sortKey,
//        position: position,
//      ));
//    }
//
//    // Do not sort groups with null sort keys. List.sort does not guarantee
//    // a stable sort order.
//    if (lastSortKey != null) {
//      sortNodes.sort();
//    }
//    everythingSorted.addAll(sortNodes);
//
//    return everythingSorted
//      .map<SemanticsNode>((_TraversalSortNode sortNode) => sortNode.node)
//      .toList();
//  }
//
//  /// Sends a [SemanticsEvent] associated with this [SemanticsNode].
//  ///
//  /// Semantics events should be sent to inform interested parties (like
//  /// the accessibility system of the operating system) about changes to the UI.
//  ///
//  /// For example, if this semantics node represents a scrollable list, a
//  /// [ScrollCompletedSemanticsEvent] should be sent after a scroll action is completed.
//  /// That way, the operating system can give additional feedback to the user
//  /// about the state of the UI (e.g. on Android a ping sound is played to
//  /// indicate a successful scroll in accessibility mode).
//  void sendEvent(SemanticsEvent event) {
//    if (!attached)
//      return;
//    SystemChannels.accessibility.send(event.toMap(nodeId: id));
//  }
//
//  @override
//  String toStringShort() => '$runtimeType#$id';
//
    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        TODO()
//    bool hideOwner = true;
//    if (_dirty) {
//      final bool inDirtyNodes = owner != null && owner._dirtyNodes.contains(this);
//      properties.add(new FlagProperty('inDirtyNodes', value: inDirtyNodes, ifTrue: 'dirty', ifFalse: 'STALE'));
//      hideOwner = inDirtyNodes;
//    }
//    properties.add(new DiagnosticsProperty<SemanticsOwner>('owner', owner, level: hideOwner ? DiagnosticLevel.hidden : DiagnosticLevel.info));
//    properties.add(new FlagProperty('isMergedIntoParent', value: isMergedIntoParent, ifTrue: 'merged up ⬆️'));
//    properties.add(new FlagProperty('mergeAllDescendantsIntoThisNode', value: mergeAllDescendantsIntoThisNode, ifTrue: 'merge boundary ⛔️'));
//    final Offset offset = transform != null ? MatrixUtils.getAsTranslation(transform) : null;
//    if (offset != null) {
//      properties.add(new DiagnosticsProperty<Rect>('rect', rect.shift(offset), showName: false));
//    } else {
//      final double scale = transform != null ? MatrixUtils.getAsScale(transform) : null;
//      String description;
//      if (scale != null) {
//        description = '$rect scaled by ${scale.toStringAsFixed(1)}x';
//      } else if (transform != null && !MatrixUtils.isIdentity(transform)) {
//        final String matrix = transform.toString().split('\n').take(4).map((String line) => line.substring(4)).join('; ');
//        description = '$rect with transform [$matrix]';
//      }
//      properties.add(new DiagnosticsProperty<Rect>('rect', rect, description: description, showName: false));
//    }
//    final List<String> actions = _actions.keys.map((SemanticsAction action) => describeEnum(action)).toList()..sort();
//    final List<String> customSemanticsActions = _customSemanticsActions.keys
//      .map<String>((CustomSemanticsAction action) => action.label)
//      .toList();
//    properties.add(new IterableProperty<String>('actions', actions, ifEmpty: null));
//    properties.add(new IterableProperty<String>('customActions', customSemanticsActions, ifEmpty: null));
//    final List<String> flags = SemanticsFlag.values.values.where((SemanticsFlag flag) => _hasFlag(flag)).map((SemanticsFlag flag) => flag.toString().substring('SemanticsFlag.'.length)).toList();
//    properties.add(new IterableProperty<String>('flags', flags, ifEmpty: null));
//    properties.add(new FlagProperty('isInvisible', value: isInvisible, ifTrue: 'invisible'));
//    properties.add(new FlagProperty('isHidden', value: _hasFlag(SemanticsFlag.isHidden), ifTrue: 'HIDDEN'));
//    properties.add(new StringProperty('label', _label, defaultValue: ''));
//    properties.add(new StringProperty('value', _value, defaultValue: ''));
//    properties.add(new StringProperty('increasedValue', _increasedValue, defaultValue: ''));
//    properties.add(new StringProperty('decreasedValue', _decreasedValue, defaultValue: ''));
//    properties.add(new StringProperty('hint', _hint, defaultValue: ''));
//    properties.add(new EnumProperty<TextDirection>('textDirection', _textDirection, defaultValue: null));
//    properties.add(new DiagnosticsProperty<SemanticsSortKey>('sortKey', sortKey, defaultValue: null));
//    if (_textSelection?.isValid == true)
//      properties.add(new MessageProperty('text selection', '[${_textSelection.start}, ${_textSelection.end}]'));
//    properties.add(new DoubleProperty('scrollExtentMin', scrollExtentMin, defaultValue: null));
//    properties.add(new DoubleProperty('scrollPosition', scrollPosition, defaultValue: null));
//    properties.add(new DoubleProperty('scrollExtentMax', scrollExtentMax, defaultValue: null));
    }
//
//  /// Returns a string representation of this node and its descendants.
//  ///
//  /// The order in which the children of the [SemanticsNode] will be printed is
//  /// controlled by the [childOrder] parameter.
//  @override

    override fun toStringDeep(
        prefixLineOne: String,
        prefixOtherLines: String,
        minLevel: DiagnosticLevel
    ): String {
        return toStringDeep(prefixLineOne,
            prefixOtherLines,
            minLevel,
            DebugSemanticsDumpOrder.traversalOrder)
    }

    fun toStringDeep(
        prefixLineOne: String,
        prefixOtherLines: String,
        minLevel: DiagnosticLevel,
        childOrder: DebugSemanticsDumpOrder = DebugSemanticsDumpOrder.traversalOrder
    ): String {
        TODO("Not implemented")
        assert(childOrder != null)
//    return toDiagnosticsNode(childOrder: childOrder).toStringDeep(prefixLineOne: prefixLineOne, prefixOtherLines: prefixOtherLines, minLevel: minLevel);
    }

    override fun toDiagnosticsNode(name: String?, style: DiagnosticsTreeStyle?): DiagnosticsNode {
        // NOTE(ryanmentley): Migrated from overridden default param
        val defaultedStyle = style ?: DiagnosticsTreeStyle.sparse
        return toDiagnosticsNode(name, defaultedStyle, DebugSemanticsDumpOrder.traversalOrder)
    }

    fun toDiagnosticsNode(
        name: String?,
        style: DiagnosticsTreeStyle,
        childOrder: DebugSemanticsDumpOrder
    ): DiagnosticsNode {
        TODO("Not implemented")
//    return new _SemanticsDiagnosticableNode(
//      name: name,
//      value: this,
//      style: style,
//      childOrder: childOrder,
//    );
    }

    override fun toString() = toStringDiagnostic()

    override fun debugDescribeChildren(): List<DiagnosticsNode> {
        return debugDescribeChildren(DebugSemanticsDumpOrder.inverseHitTest)
    }

    fun debugDescribeChildren(
        childOrder: DebugSemanticsDumpOrder
    ): List<DiagnosticsNode> {
        TODO("Not implemented")
//    return debugListChildrenInOrder(childOrder)
//      .map<DiagnosticsNode>((SemanticsNode node) => node.toDiagnosticsNode(childOrder: childOrder))
//      .toList();
    }

//  /// Returns the list of direct children of this node in the specified order.
//  List<SemanticsNode> debugListChildrenInOrder(DebugSemanticsDumpOrder childOrder) {
//    assert(childOrder != null);
//    if (_children == null)
//      return const <SemanticsNode>[];
//
//    switch (childOrder) {
//      case DebugSemanticsDumpOrder.inverseHitTest:
//        return _children;
//      case DebugSemanticsDumpOrder.traversalOrder:
//        return _childrenInTraversalOrder();
//    }
//    assert(false);
//    return null;
//  }
}