package androidx.ui.semantics

import SemanticsNodeVisitor
import _SemanticsActionHandler
import _concatStrings
import androidx.ui.VoidCallback
import androidx.ui.describeEnum
import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.AbstractNode
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DiagnosticsTreeStyle
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.foundation.diagnostics.IterableProperty
import androidx.ui.foundation.diagnostics.MessageProperty
import androidx.ui.foundation.diagnostics.StringProperty
import androidx.ui.painting.matrixutils.getAsScale
import androidx.ui.painting.matrixutils.getAsTranslation
import androidx.ui.painting.matrixutils.isIdentity
import androidx.ui.runtimeType
import androidx.ui.text.TextDirection
import androidx.ui.text.TextSelection
import androidx.ui.toStringAsFixed
import androidx.ui.vectormath64.Matrix4

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
// TODO(Migration/ryanmentley): This constructor should be private, but the resulting synthetic
// constructor breaks the Kotlin compiler
class SemanticsNode internal constructor(
    // / Uniquely identifies this node in the list of sibling nodes.
    // /
    // / Keys are used during the construction of the semantics tree. They are not
    // / transferred to the engine.
    val key: Key?,
    private val showOnScreen: VoidCallback?,
    // / The unique identifier for this node.
    // /
    // / The root node has an id of zero. Other nodes are given a unique id when
    // / they are created.
    val id: Int
) : AbstractNode(),
    DiagnosticableTree {

    companion object {
        // TODO(Migration/ryanmentley): Should be private, but is internal to avoid a synthetic accessor
        internal var _lastIdentifier: Int = 0
        // TODO(Migration/ryanmentley): Should be private, but is internal to avoid a synthetic accessor
        internal fun _generateNewId(): Int {
            _lastIdentifier += 1
            return _lastIdentifier
        }

        private val _kEmptyConfig: SemanticsConfiguration = SemanticsConfiguration()

        fun root(
            key: Key? = null,
            showOnScreen: VoidCallback? = null,
            owner: SemanticsOwner
        ): SemanticsNode {
            val node = SemanticsNode(key, showOnScreen, 0)
            node.attach(owner)
            return node
        }
    }

    // / Creates a semantic node.
    // /
    // / Each semantic node has a unique identifier that is assigned when the node
    // / is created.
    constructor(
        key: Key? = null,
        showOnScreen: VoidCallback? = null
    ) : this(key, showOnScreen, _generateNewId())

    // GEOMETRY

    // / The transform from this node's coordinate system to its parent's coordinate system.
    // /
    // / By default, the transform is null, which represents the identity
    // / transformation (i.e., that this node has the same coordinate system as its
    // / parent).
    var transform: Matrix4? = null
        set(value) {
            TODO()
//        if (!MatrixUtils.matrixEquals(field, value)) {
//            _transform = MatrixUtils.isIdentity(value) ? null : value;
//            _markDirty();
//        }
        }

    // / The bounding box for this node in its coordinate system.
    var rect: Rect = Rect.zero
        set(value) {
            assert(value != null)
            if (field != value) {
                field = value
                _markDirty()
            }
        }

    // / The semantic clip from an ancestor that was applied to this node.
    // /
    // / Expressed in the coordinate system of the node. May be null if no clip has
    // / been applied.
    // /
    // / Descendant [SemanticsNode]s that are positioned outside of this rect will
    // / be excluded from the semantics tree. Descendant [SemanticsNode]s that are
    // / overlapping with this rect, but are outside of [parentPaintClipRect] will
    // / be included in the tree, but they will be marked as hidden because they
    // / are assumed to be not visible on screen.
    // /
    // / If this rect is null, all descendant [SemanticsNode]s outside of
    // / [parentPaintClipRect] will be excluded from the tree.
    // /
    // / If this rect is non-null it has to completely enclose
    // / [parentPaintClipRect]. If [parentPaintClipRect] is null this property is
    // / also null.
    var parentSemanticsClipRect: Rect? = null

    // / The paint clip from an ancestor that was applied to this node.
    // /
    // / Expressed in the coordinate system of the node. May be null if no clip has
    // / been applied.
    // /
    // / Descendant [SemanticsNode]s that are positioned outside of this rect will
    // / either be excluded from the semantics tree (if they have no overlap with
    // / [parentSemanticsClipRect]) or they will be included and marked as hidden
    // / (if they are overlapping with [parentSemanticsClipRect]).
    // /
    // / This rect is completely enclosed by [parentSemanticsClipRect].
    // /
    // / If this rect is null [parentSemanticsClipRect] also has to be null.
    var parentPaintClipRect: Rect? = null

    // / Whether the node is invisible.
    // /
    // / A node whose [rect] is outside of the bounds of the screen and hence not
    // / reachable for users is considered invisible if its semantic information
    // / is not merged into a (partially) visible parent as indicated by
    // / [isMergedIntoParent].
    // /
    // / An invisible node can be safely dropped from the semantic tree without
    // / loosing semantic information that is relevant for describing the content
    // / currently shown on screen.
    val isInvisible: Boolean
        get() = !isMergedIntoParent && rect.isEmpty()

    // MERGING

    // / Whether this node merges its semantic information into an ancestor node.
    var isMergedIntoParent: Boolean = false
        set(value) {
            assert(value != null)
            if (field == value)
                return
            field = value
            _markDirty()
        }

    // / Whether this node is taking part in a merge of semantic information.
    // /
    // / This returns true if the node is either merged into an ancestor node or if
    // / decedent nodes are merged into this node.
    // /
    // / See also:
    // /
    // /  * [isMergedIntoParent]
    // /  * [mergeAllDescendantsIntoThisNode]
    val isPartOfNodeMerging
        get() = mergeAllDescendantsIntoThisNode || isMergedIntoParent

    // / Whether this node and all of its descendants should be treated as one logical entity.
    var mergeAllDescendantsIntoThisNode = _kEmptyConfig.isMergingSemanticsOfDescendants
        private set

    // CHILDREN

    // / Contains the children in inverse hit test order (i.e. paint order).
    var _children: List<SemanticsNode>? = null

    // / A snapshot of `newChildren` passed to [_replaceChildren] that we keep in
    // / debug mode. It supports the assertion that user does not mutate the list
    // / of children.
    var _debugPreviousSnapshot: List<SemanticsNode>? = null

    fun _replaceChildren(newChildren: List<SemanticsNode>) {
        TODO()
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
    }

    // / Whether this node has a non-zero number of children.
    val hasChildren
        get() = _children?.isNotEmpty() ?: false

    private var _dead = false

    // / The number of children this node has.
    val childrenCount
        get() = if (hasChildren) _children!!.size else 0

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
    // / Visit all the descendants of this node.
    // /
    // / This function calls visitor for each descendant in a pre-order traversal
    // / until visitor returns false. Returns true if all the visitor calls
    // / returned true, otherwise returns false.
    fun _visitDescendants(visitor: SemanticsNodeVisitor): Boolean {
        _children?.forEach {
            if (!visitor(it) || !it._visitDescendants(visitor))
                return false
        }
        return true
    }

    // AbstractNode OVERRIDES

    override val owner: SemanticsOwner
        get() = super.owner as SemanticsOwner

    // TODO(Migration/ryanmentley): The use of types and overriding here is kind of messy.
    // It requires private backing properties to get around type requirements, which feels like
    // a hack.
    override val parent: SemanticsNode?
        get() = super.parent as SemanticsNode?

    override fun redepthChildren() {
        _children?.forEach(::redepthChild)
    }

    // TODO(Migration/ryanmentley): Removed covariant
    override fun attach(owner: Any) {
        owner as SemanticsOwner

        super.attach(owner)
        assert(!owner._nodes.containsKey(id))
        owner._nodes[id] = this
        owner._detachedNodes.remove(this)
        if (_dirty) {
            _dirty = false
            _markDirty()
        }
        _children?.let {
            for (child in it) {
                child.attach(owner)
            }
        }
    }

    override fun detach() {
        assert(owner._nodes.containsKey(id))
        assert(!owner._detachedNodes.contains(this))
        owner._nodes.remove(id)
        owner._detachedNodes.add(this)
        super.detach()
        assert(owner == null)
        _children?.let {
            for (child in it) {
                // The list of children may be stale and may contain nodes that have
                // been assigned to a different parent.
                if (child.parent == this)
                    child.detach()
            }
        }
        // The other side will have forgotten this node if we ever send
        // it again, so make sure to mark it dirty so that it'll get
        // sent if it is resurrected.
        _markDirty()
    }

// DIRTY MANAGEMENT

    private var _dirty: Boolean = false

    fun _markDirty() {
        if (_dirty)
            return
        _dirty = true
        if (attached) {
            assert(!owner._detachedNodes.contains(this))
            owner._dirtyNodes.add(this)
        }
    }

    fun _isDifferentFromCurrentSemanticAnnotation(config: SemanticsConfiguration): Boolean {
        return label != config.label ||
                hint != config.hint ||
                decreasedValue != config.decreasedValue ||
                value != config.value ||
                increasedValue != config.increasedValue ||
                _flags != config._flags ||
                textDirection != config.textDirection ||
                sortKey != config.sortKey ||
                textSelection != config.textSelection ||
                scrollPosition != config.scrollPosition ||
                scrollExtentMax != config.scrollExtentMax ||
                scrollExtentMin != config.scrollExtentMin ||
                _actionsAsBits != config._actionsAsBits ||
                mergeAllDescendantsIntoThisNode != config.isMergingSemanticsOfDescendants
    }

    // TAGS, LABELS, ACTIONS

    var _actions: Map<SemanticsAction, _SemanticsActionHandler> = _kEmptyConfig._actions
    var _customSemanticsActions: Map<CustomSemanticsAction, VoidCallback> =
        _kEmptyConfig.customSemanticsActions

    var _actionsAsBits = _kEmptyConfig._actionsAsBits

    // / The [SemanticsTag]s this node is tagged with.
    // /
    // / Tags are used during the construction of the semantics tree. They are not
    // / transferred to the engine.
    var tags: Set<SemanticsTag>? = null

    // / Whether this node is tagged with `tag`.
    fun isTagged(tag: SemanticsTag) = tags?.contains(tag) == true

    private var _flags: Int = _kEmptyConfig._flags

    private fun _hasFlag(flag: SemanticsFlag) = _flags and flag.index != 0

    // / A textual description of this node.
    // /
    // / The reading direction is given by [textDirection].
    var label: String = _kEmptyConfig.label
        private set

    // / A textual description for the current value of the node.
    // /
    // / The reading direction is given by [textDirection].
    var value: String = _kEmptyConfig.value
        private set

    // / The value that [value] will have after a [SemanticsAction.decrease] action
    // / has been performed.
    // /
    // / This property is only valid if the [SemanticsAction.decrease] action is
    // / available on this node.
    // /
    // / The reading direction is given by [textDirection].
    var decreasedValue: String = _kEmptyConfig.decreasedValue
        private set

    // / The value that [value] will have after a [SemanticsAction.increase] action
    // / has been performed.
    // /
    // / This property is only valid if the [SemanticsAction.increase] action is
    // / available on this node.
    // /
    // / The reading direction is given by [textDirection].
    var increasedValue: String = _kEmptyConfig.increasedValue
        private set

    // / A brief description of the result of performing an action on this node.
    // /
    // / The reading direction is given by [textDirection].
    var hint: String = _kEmptyConfig.hint
        private set

    // / Provides hint values which override the default hints on supported
    // / platforms.
    var hintOverrides: SemanticsHintOverrides? = _kEmptyConfig.hintOverrides
        private set

    // / The reading direction for [label], [value], [hint], [increasedValue], and
    // / [decreasedValue].
    var textDirection: TextDirection? = _kEmptyConfig.textDirection
        private set

    // / Determines the position of this node among its siblings in the traversal
    // / sort order.
    // /
    // / This is used to describe the order in which the semantic node should be
    // / traversed by the accessibility services on the platform (e.g. VoiceOver
    // / on iOS and TalkBack on Android).
    var sortKey: SemanticsSortKey? = null
        private set

    // / The currently selected text (or the position of the cursor) within [value]
    // / if this node represents a text field.
    var textSelection: TextSelection? = null
        private set

    // / Indicates the current scrolling position in logical pixels if the node is
    // / scrollable.
    // /
    // / The properties [scrollExtentMin] and [scrollExtentMax] indicate the valid
    // / in-range values for this property. The value for [scrollPosition] may
    // / (temporarily) be outside that range, e.g. during an overscroll.
    // /
    // / See also:
    // /
    // /  * [ScrollPosition.pixels], from where this value is usually taken.
    var scrollPosition: Double? = null
        private set

    // / Indicates the maximum in-range value for [scrollPosition] if the node is
    // / scrollable.
    // /
    // / This value may be infinity if the scroll is unbound.
    // /
    // / See also:
    // /
    // /  * [ScrollPosition.maxScrollExtent], from where this value is usually taken.
    var scrollExtentMax: Double? = null
        private set

    // / Indicates the minimum in-range value for [scrollPosition] if the node is
    // / scrollable.
    // /
    // / This value may be infinity if the scroll is unbound.
    // /
    // / See also:
    // /
    // /  * [ScrollPosition.minScrollExtent] from where this value is usually taken.
    var scrollExtentMin: Double? = null
        private set

    private fun _canPerformAction(action: SemanticsAction) = _actions.containsKey(action)

    // / Reconfigures the properties of this object to describe the configuration
    // / provided in the `config` argument and the children listed in the
    // / `childrenInInversePaintOrder` argument.
    // /
    // / The arguments may be null; this represents an empty configuration (all
    // / values at their defaults, no children).
    // /
    // / No reference is kept to the [SemanticsConfiguration] object, but the child
    // / list is used as-is and should therefore not be changed after this call.
    fun updateWith(
        config: SemanticsConfiguration?,
        childrenInInversePaintOrder: List<SemanticsNode>
    ) {
        val sourceConfig = config ?: _kEmptyConfig
        if (_isDifferentFromCurrentSemanticAnnotation(sourceConfig))
            _markDirty()

        label = sourceConfig.label
        decreasedValue = sourceConfig.decreasedValue
        value = sourceConfig.value
        increasedValue = sourceConfig.increasedValue
        hint = sourceConfig.hint
        hintOverrides = sourceConfig.hintOverrides
        _flags = sourceConfig._flags
        textDirection = sourceConfig.textDirection
        sortKey = sourceConfig.sortKey
        _actions = sourceConfig._actions.toMap()
        _customSemanticsActions = sourceConfig.customSemanticsActions.toMap()
        _actionsAsBits = sourceConfig._actionsAsBits
        textSelection = sourceConfig.textSelection
        scrollPosition = sourceConfig.scrollPosition
        scrollExtentMax = sourceConfig.scrollExtentMax
        scrollExtentMin = sourceConfig.scrollExtentMin
        mergeAllDescendantsIntoThisNode = sourceConfig.isMergingSemanticsOfDescendants
        _replaceChildren(childrenInInversePaintOrder ?: listOf<SemanticsNode>())

        assert(
            !_canPerformAction(
                SemanticsAction.increase
            ) || (value == "") == (increasedValue == "")
        ) {
            "A SemanticsNode with action \"increase\" needs to be annotated with either both " +
                    "\"value\" and \"increasedValue\" or neither"
        }
        assert(
            !_canPerformAction(
                SemanticsAction.decrease
            ) || (value == "") == (decreasedValue == "")
        ) {
            "A SemanticsNode with action \"increase\" needs to be annotated with either both " +
                    "\"value\" and \"decreasedValue\" or neither"
        }
    }

    // / Returns a summary of the semantics for this node.
    // /
    // / If this node has [mergeAllDescendantsIntoThisNode], then the returned data
    // / includes the information from this node's descendants. Otherwise, the
    // / returned data matches the data on this node.
    fun getSemanticsData(): SemanticsData {
        var flags = _flags
        var actions = _actionsAsBits
        var label = label
        var hint = hint
        var value = value
        var increasedValue = increasedValue
        var decreasedValue = decreasedValue
        var textDirection = textDirection
        var mergedTags: MutableSet<SemanticsTag> = tags?.toMutableSet() ?: mutableSetOf()
        var textSelection = textSelection
        var scrollPosition = scrollPosition
        var scrollExtentMax = scrollExtentMax
        var scrollExtentMin = scrollExtentMin
        val customSemanticsActionIds: MutableSet<Int> = _customSemanticsActions.keys
            .map { CustomSemanticsAction.getIdentifier(it) }
            .toMutableSet()
        hintOverrides?.let {
            it.onTapHint?.let {
                val action: CustomSemanticsAction = CustomSemanticsAction.overridingAction(
                    hint = it,
                    action = SemanticsAction.tap
                )
                customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action))
            }
            it.onLongPressHint?.let {
                val action: CustomSemanticsAction = CustomSemanticsAction.overridingAction(
                    hint = it,
                    action = SemanticsAction.longPress
                )
                customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action))
            }
        }

        if (mergeAllDescendantsIntoThisNode) {
            _visitDescendants { node: SemanticsNode ->
                assert(node.isMergedIntoParent)
                flags = flags or node._flags
                actions = actions or node._actionsAsBits
                textDirection = textDirection ?: node.textDirection
                textSelection = textSelection ?: node.textSelection
                scrollPosition = scrollPosition ?: node.scrollPosition
                scrollExtentMax = scrollExtentMax ?: node.scrollExtentMax
                scrollExtentMin = scrollExtentMin ?: node.scrollExtentMin
                if (value == "" || value == null) {
                    value = node.value
                }
                if (increasedValue == "" || increasedValue == null) {
                    increasedValue = node.increasedValue
                }
                if (decreasedValue == "" || decreasedValue == null) {
                    decreasedValue = node.decreasedValue
                }
                node.tags?.let {
                    var localMergedTags = mergedTags
                    if (localMergedTags == null) {
                        localMergedTags = mutableSetOf()
                        mergedTags = localMergedTags
                    }
                    localMergedTags.addAll(it)
                }
                if (node._customSemanticsActions != null) {
                    for (action in _customSemanticsActions.keys)
                        customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action))
                }
                node.hintOverrides?.onTapHint?.let {
                    val action = CustomSemanticsAction.overridingAction(
                        hint = it,
                        action = SemanticsAction.tap
                    )
                    customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action))
                }
                node.hintOverrides?.onLongPressHint?.let {
                    val action = CustomSemanticsAction.overridingAction(
                        hint = it,
                        action = SemanticsAction.longPress
                    )
                    customSemanticsActionIds.add(CustomSemanticsAction.getIdentifier(action))
                }

                label = _concatStrings(
                    thisString = label,
                    thisTextDirection = textDirection,
                    otherString = node.label,
                    otherTextDirection = node.textDirection
                )
                hint = _concatStrings(
                    thisString = hint,
                    thisTextDirection = textDirection,
                    otherString = node.hint,
                    otherTextDirection = node.textDirection
                )
                return@_visitDescendants true
            }
        }

        return SemanticsData(
            flags = flags,
            actions = actions,
            label = label,
            value = value,
            increasedValue = increasedValue,
            decreasedValue = decreasedValue,
            hint = hint,
            textDirection = textDirection,
            rect = rect,
            transform = transform,
            tags = mergedTags,
            textSelection = textSelection,
            scrollPosition = scrollPosition,
            scrollExtentMax = scrollExtentMax,
            scrollExtentMin = scrollExtentMin,
            customSemanticsActionIds = customSemanticsActionIds.sorted()
        )
    }

    //
//  static Float64List _initIdentityTransform() {
//    return new Matrix4.identity().storage;
//  }
//
//  static final Int32List _kEmptyChildList = new Int32List(0);
//  static final Int32List _kEmptyCustomSemanticsActionsList = new Int32List(0);
//  static final Float64List _kIdentityTransform = _initIdentityTransform();
//
    fun _addToUpdate(builder: SemanticsUpdateBuilder, customSemanticsActionIdsUpdate: Set<Int>) {
        TODO()
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
//      id = id,
//      flags = data.flags,
//      actions = data.actions,
//      rect = data.rect,
//      label = data.label,
//      value = data.value,
//      decreasedValue = data.decreasedValue,
//      increasedValue = data.increasedValue,
//      hint = data.hint,
//      textDirection = data.textDirection,
//      textSelectionBase = data.textSelection != null ? data.textSelection.baseOffset : -1,
//      textSelectionExtent = data.textSelection != null ? data.textSelection.extentOffset : -1,
//      scrollPosition = data.scrollPosition != null ? data.scrollPosition : Double.nan,
//      scrollExtentMax = data.scrollExtentMax != null ? data.scrollExtentMax : Double.nan,
//      scrollExtentMin = data.scrollExtentMin != null ? data.scrollExtentMin : Double.nan,
//      transform = data.transform?.storage ?? _kIdentityTransform,
//      childrenInTraversalOrder = childrenInTraversalOrder,
//      childrenInHitTestOrder = childrenInHitTestOrder,
//      additionalActions = customSemanticsActionIds ?? _kEmptyCustomSemanticsActionsList,
//    );
//    _dirty = false;
    }

    //
//  /// Builds a new list made of [_children] sorted in semantic traversal order.
    fun _childrenInTraversalOrder(): List<SemanticsNode> {
        TODO()
//    TextDirection inheritedTextDirection = textDirection;
//    SemanticsNode ancestor = parent;
//    while (inheritedTextDirection == null && ancestor != null) {
//      inheritedTextDirection = ancestor.textDirection;
//      ancestor = ancestor.parent;
//    }textDirection
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
//        node = child,
//        sortKey = sortKey,
//        position = position,
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
    }

    // / Sends a [SemanticsEvent] associated with this [SemanticsNode].
    // /
    // / Semantics events should be sent to inform interested parties (like
    // / the accessibility system of the operating system) about changes to the UI.
    // /
    // / For example, if this semantics node represents a scrollable list, a
    // / [ScrollCompletedSemanticsEvent] should be sent after a scroll action is completed.
    // / That way, the operating system can give additional feedback to the user
    // / about the state of the UI (e.g. on Android a ping sound is played to
    // / indicate a successful scroll in accessibility mode).
    fun sendEvent(event: SemanticsEvent) {
        TODO()
//    if (!attached)
//      return;
//    SystemChannels.accessibility.send(event.toMap(nodeId = id));
    }

    override fun toStringShort() = "${runtimeType()}#$id"

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
//        TODO()
        var hideOwner = true
        if (_dirty) {
            val inDirtyNodes = owner != null && owner._dirtyNodes.contains(this)
            properties.add(
                FlagProperty(
                    "inDirtyNodes",
                    value = inDirtyNodes,
                    ifTrue = "dirty",
                    ifFalse = "STALE"
                )
            )
            hideOwner = inDirtyNodes
        }
        properties.add(
            DiagnosticsProperty.create(
                "owner",
                owner,
                level = if (hideOwner) DiagnosticLevel.hidden else DiagnosticLevel.info
            )
        )
        properties.add(
            FlagProperty(
                "isMergedIntoParent",
                value = isMergedIntoParent,
                ifTrue = "merged up ⬆️"
            )
        )
        properties.add(
            FlagProperty(
                "mergeAllDescendantsIntoThisNode",
                value = mergeAllDescendantsIntoThisNode,
                ifTrue = "merge boundary ⛔️"
            )
        )
        val offset = transform?.getAsTranslation()
        if (offset != null) {
            properties.add(
                DiagnosticsProperty.create(
                    "rect",
                    rect.shift(offset),
                    showName = false
                )
            )
        } else {
            val scale = transform?.getAsScale()
            val description = when {
                scale != null -> "$rect scaled by ${scale.toStringAsFixed(1)}x"

                transform?.isIdentity() == false -> {
                    val matrix: String = transform
                        .toString()
                        .split("\n")
                        .take(4)
                        .map { line: String -> line.substring(4) }
                        .joinToString("; ")
                    "$rect with transform [$matrix]"
                }
                else -> null
            }
            properties.add(
                DiagnosticsProperty.create(
                    "rect",
                    rect,
                    description = description,
                    showName = false
                )
            )
        }
        val actions: List<String> = _actions.keys.map { action: SemanticsAction ->
            describeEnum(action)
        }.sorted()
        val customSemanticsActions: List<String> = _customSemanticsActions.keys
            .map({ action: CustomSemanticsAction -> action.label })
            .toList()
        properties.add(IterableProperty<String>("actions", actions, ifEmpty = null))
        properties.add(
            IterableProperty<String>(
                "customActions", customSemanticsActions,
                ifEmpty = null
            )
        )
        val flags: List<String> = SemanticsFlag.values.values
            .filter { flag: SemanticsFlag -> _hasFlag(flag) }
            .map { flag: SemanticsFlag -> flag.toString().substring("SemanticsFlag.".length) }
            .toList()
        properties.add(IterableProperty<String>("flags", flags, ifEmpty = null))
        properties.add(
            FlagProperty("isInvisible", value = isInvisible, ifTrue = "invisible")
        )
        properties.add(
            FlagProperty(
                "isHidden", value = _hasFlag(SemanticsFlag.isHidden),
                ifTrue = "HIDDEN"
            )
        )
        properties.add(StringProperty("label", label, defaultValue = ""))
        properties.add(StringProperty("value", value, defaultValue = ""))
        properties.add(
            StringProperty("increasedValue", increasedValue, defaultValue = "")
        )
        properties.add(
            StringProperty("decreasedValue", decreasedValue, defaultValue = "")
        )
        properties.add(StringProperty("hint", hint, defaultValue = ""))
        properties.add(
            EnumProperty<TextDirection>(
                "textDirection", textDirection,
                defaultValue = null
            )
        )
        properties.add(
            DiagnosticsProperty.create(
                "sortKey", sortKey,
                defaultValue = null
            )
        )
        textSelection?.let {
            if (it.isValid) {
                properties.add(
                    MessageProperty(
                        "text selection",
                        "[${it.start}, ${it.end}]"
                    )
                )
            }
        }
        properties.add(
            DoubleProperty.create("scrollExtentMin", scrollExtentMin, defaultValue = null)
        )
        properties.add(
            DoubleProperty.create("scrollPosition", scrollPosition, defaultValue = null)
        )
        properties.add(
            DoubleProperty.create("scrollExtentMax", scrollExtentMax, defaultValue = null)
        )
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
        return toStringDeep(
            prefixLineOne,
            prefixOtherLines,
            minLevel,
            DebugSemanticsDumpOrder.traversalOrder
        )
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

    override fun toDiagnosticsNode(
        name: String?,
        style: DiagnosticsTreeStyle?
    ): DiagnosticsNode {
        // NOTE(ryanmentley): Migrated from overridden default param
        val defaultedStyle = style ?: DiagnosticsTreeStyle.sparse
        return toDiagnosticsNode(
            name, defaultedStyle,
            DebugSemanticsDumpOrder.traversalOrder
        )
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