package androidx.ui.widgets.basic

import androidx.ui.VoidCallback
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.RenderBlockSemantics
import androidx.ui.rendering.proxybox.RenderExcludeSemantics
import androidx.ui.rendering.proxybox.RenderMergeSemantics
import androidx.ui.rendering.proxybox.RenderSemanticsAnnotations
import androidx.ui.semantics.MoveCursorHandler
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.semantics.SemanticsSortKey
import androidx.ui.semantics.SetSelectionHandler
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

/**
 * A widget that annotates the widget tree with a description of the meaning of
 * the widgets.
 *
 * Used by accessibility tools, search engines, and other semantic analysis
 * software to determine the meaning of the application.
 *
 * See also:
 *
 * * [SemanticsSortKey] for a class that determines accessibility traversal
 *   order.
 * * [MergeSemantics], which marks a subtree as being a single node for
 *   accessibility purposes.
 * * [ExcludeSemantics], which excludes a subtree from the semantics tree
 *   (which might be useful if it is, e.g., totally decorative and not
 *   important to the user).
 * * [RenderObject.semanticsAnnotator], the rendering library API through which
 *   the [Semantics] widget is actually implemented.
 * * [SemanticsNode], the object used by the rendering library to represent
 *   semantics in the semantics tree.
 * * [SemanticsDebugger], an overlay to help visualize the semantics tree. Can
 *   be enabled using [WidgetsApp.showSemanticsDebugger] or
 *   [MaterialApp.showSemanticsDebugger].
 */
// @immutable
class Semantics(
    key: Key?,
    child: Widget?,
    /**
     * If [container] is true, this widget will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors (if the ancestor allows that).
     *
     * Whether descendants of this widget can add their semantic information to the
     * [SemanticsNode] introduced by this configuration is controlled by
     * [explicitChildNodes].
     */
    val container: Boolean = false,
    /**
     * Whether descendants of this widget are allowed to add semantic information
     * to the [SemanticsNode] annotated by this widget.
     *
     * When set to false descendants are allowed to annotate [SemanticNode]s of
     * their parent with the semantic information they want to contribute to the
     * semantic tree.
     * When set to true the only way for descendants to contribute semantic
     * information to the semantic tree is to introduce new explicit
     * [SemanticNode]s to the tree.
     *
     * If the semantics properties of this node include
     * [SemanticsProperties.scopesRoute] set to true, then [explicitChildNodes]
     * must be true also.
     *
     * This setting is often used in combination with [SemanticsConfiguration.isSemanticBoundary]
     * to create semantic boundaries that are either writable or not for children.
     */
    val explicitChildNodes: Boolean = false,
    /**
     * Contains properties used by assistive technologies to make the application
     * more accessible.
     */
    val properties: SemanticsProperties
) : SingleChildRenderObjectWidget(key, child) {

    constructor(
        key: Key?,
        child: Widget?,
        container: Boolean = false,
        explicitChildNodes: Boolean = false,
        enabled: Boolean = false,
        checked: Boolean = false,
        selected: Boolean = false,
        button: Boolean = false,
        header: Boolean = false,
        textField: Boolean = false,
        focused: Boolean = false,
        inMutuallyExclusiveGroup: Boolean = false,
        obscured: Boolean = false,
        scopesRoute: Boolean = false,
        namesRoute: Boolean = false,
        hidden: Boolean = false,
        label: String? = null,
        value: String? = null,
        increasedValue: String? = null,
        decreasedValue: String? = null,
        hint: String? = null,
        textDirection: TextDirection? = null,
        sortKey: SemanticsSortKey? = null,
        onTap: VoidCallback? = null,
        onLongPress: VoidCallback? = null,
        onScrollLeft: VoidCallback? = null,
        onScrollRight: VoidCallback? = null,
        onScrollUp: VoidCallback? = null,
        onScrollDown: VoidCallback? = null,
        onIncrease: VoidCallback? = null,
        onDecrease: VoidCallback? = null,
        onCopy: VoidCallback? = null,
        onCut: VoidCallback? = null,
        onPaste: VoidCallback? = null,
        onMoveCursorForwardByCharacter: MoveCursorHandler? = null,
        onMoveCursorBackwardByCharacter: MoveCursorHandler? = null,
        onSetSelection: SetSelectionHandler? = null,
        onDidGainAccessibilityFocus: VoidCallback? = null,
        onDidLoseAccessibilityFocus: VoidCallback? = null
    ) : this(
        key = key,
        child = child,
        container = container,
        explicitChildNodes = explicitChildNodes,
        properties = SemanticsProperties(
            enabled = enabled,
            checked = checked,
            selected = selected,
            button = button,
            header = header,
            textField = textField,
            focused = focused,
            inMutuallyExclusiveGroup = inMutuallyExclusiveGroup,
            obscured = obscured,
            scopesRoute = scopesRoute,
            namesRoute = namesRoute,
            hidden = hidden,
            label = label,
            value = value,
            increasedValue = increasedValue,
            decreasedValue = decreasedValue,
            hint = hint,
            textDirection = textDirection,
            sortKey = sortKey,
            onTap = onTap,
            onLongPress = onLongPress,
            onScrollLeft = onScrollLeft,
            onScrollRight = onScrollRight,
            onScrollUp = onScrollUp,
            onScrollDown = onScrollDown,
            onIncrease = onIncrease,
            onDecrease = onDecrease,
            onCopy = onCopy,
            onCut = onCut,
            onPaste = onPaste,
            onMoveCursorForwardByCharacter = onMoveCursorForwardByCharacter,
            onMoveCursorBackwardByCharacter = onMoveCursorBackwardByCharacter,
            onDidGainAccessibilityFocus = onDidGainAccessibilityFocus,
            onDidLoseAccessibilityFocus = onDidLoseAccessibilityFocus,
            onSetSelection = onSetSelection
        )
    )

    init {
        assert(container != null)
        assert(properties != null)
    }

    override fun createRenderObject(context: BuildContext): RenderSemanticsAnnotations {
        return RenderSemanticsAnnotations(
            container = container,
            explicitChildNodes = explicitChildNodes,
            enabled = properties.enabled,
            checked = properties.checked,
            selected = properties.selected,
            button = properties.button,
            header = properties.header,
            textField = properties.textField,
            focused = properties.focused,
            inMutuallyExclusiveGroup = properties.inMutuallyExclusiveGroup,
            obscured = properties.obscured,
            scopesRoute = properties.scopesRoute,
            namesRoute = properties.namesRoute,
            hidden = properties.hidden,
            label = properties.label,
            value = properties.value,
            increasedValue = properties.increasedValue,
            decreasedValue = properties.decreasedValue,
            hint = properties.hint,
            textDirection = _getTextDirection(context),
            sortKey = properties.sortKey,
            onTap = properties.onTap,
            onLongPress = properties.onLongPress,
            onScrollLeft = properties.onScrollLeft,
            onScrollRight = properties.onScrollRight,
            onScrollUp = properties.onScrollUp,
            onScrollDown = properties.onScrollDown,
            onIncrease = properties.onIncrease,
            onDecrease = properties.onDecrease,
            onCopy = properties.onCopy,
            onCut = properties.onCut,
            onPaste = properties.onPaste,
            onMoveCursorForwardByCharacter = properties.onMoveCursorForwardByCharacter,
            onMoveCursorBackwardByCharacter = properties.onMoveCursorBackwardByCharacter,
            onSetSelection = properties.onSetSelection,
            onDidGainAccessibilityFocus = properties.onDidGainAccessibilityFocus,
            onDidLoseAccessibilityFocus = properties.onDidLoseAccessibilityFocus
        )
    }

    fun _getTextDirection(context: BuildContext): TextDirection? {
        if (properties.textDirection != null) {
            return properties.textDirection
        }

        val containsText =
            properties.label != null || properties.value != null || properties.hint != null

        if (!containsText) {
            return null
        }

        return Directionality.of(context)
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        renderObject as RenderSemanticsAnnotations
        renderObject.also {
            it.container = container
            it.scopesRoute = properties.scopesRoute
            it.explicitChildNodes = explicitChildNodes
            it.enabled = properties.enabled
            it.checked = properties.checked
            it.selected = properties.selected
            it.button = properties.button
            it.header = properties.header
            it.textField = properties.textField
            it.focused = properties.focused
            it.inMutuallyExclusiveGroup = properties.inMutuallyExclusiveGroup
            it.obscured = properties.obscured
            it.hidden = properties.hidden
            it.label = properties.label
            it.value = properties.value
            it.increasedValue = properties.increasedValue
            it.decreasedValue = properties.decreasedValue
            it.hint = properties.hint
            it.namesRoute = properties.namesRoute
            it.textDirection = _getTextDirection(context)
            it.sortKey = properties.sortKey
            it.onTap = properties.onTap
            it.onLongPress = properties.onLongPress
            it.onScrollLeft = properties.onScrollLeft
            it.onScrollRight = properties.onScrollRight
            it.onScrollUp = properties.onScrollUp
            it.onScrollDown = properties.onScrollDown
            it.onIncrease = properties.onIncrease
            it.onDecrease = properties.onDecrease
            it.onCopy = properties.onCopy
            it.onCut = properties.onCut
            it.onPaste = properties.onPaste
            it.onMoveCursorForwardByCharacter = properties.onMoveCursorForwardByCharacter
            it.onMoveCursorBackwardByCharacter = properties.onMoveCursorForwardByCharacter
            it.onSetSelection = properties.onSetSelection
            it.onDidGainAccessibilityFocus = properties.onDidGainAccessibilityFocus
            it.onDidLoseAccessibilityFocus = properties.onDidLoseAccessibilityFocus
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("container", container))
        properties.add(DiagnosticsProperty.create("properties", this.properties))
        this.properties.debugFillProperties(properties)
    }
}

/**
 * A widget that merges the semantics of its descendants.
 *
 * Causes all the semantics of the subtree rooted at this node to be
 * merged into one node in the semantics tree. For example, if you
 * have a widget with a Text node next to a checkbox widget, this
 * could be used to merge the label from the Text node with the
 * "checked" semantic state of the checkbox into a single node that
 * had both the label and the checked state. Otherwise, the label
 * would be presented as a separate feature than the checkbox, and
 * the user would not be able to be sure that they were related.
 *
 * Be aware that if two nodes in the subtree have conflicting
 * semantics, the result may be nonsensical. For example, a subtree
 * with a checked checkbox and an unchecked checkbox will be
 * presented as checked. All the labels will be merged into a single
 * string (with newlines separating each label from the other). If
 * multiple nodes in the merged subtree can handle semantic gestures,
 * the first one in tree order will be the one to receive the
 * callbacks.
 */
class MergeSemantics(key: Key?, child: Widget?) : SingleChildRenderObjectWidget(key, child) {
    override fun createRenderObject(context: BuildContext): RenderMergeSemantics {
        return RenderMergeSemantics()
    }
}

/**
 * A widget that drops the semantics of all widget that were painted before it
 * in the same semantic container.
 *
 * This is useful to hide widgets from accessibility tools that are painted
 * behind a certain widget, e.g. an alert should usually disallow interaction
 * with any widget located "behind" the alert (even when they are still
 * partially visible). Similarly, an open [Drawer] blocks interactions with
 * any widget outside the drawer.
 *
 * See also:
 *
 * * [ExcludeSemantics] which drops all semantics of its descendants.
 */
class BlockSemantics(
    key: Key?,
    /**
     * Whether this widget is blocking semantics of all widget that were painted
     * before it in the same semantic container.
     */
    val blocking: Boolean = true,
    child: Widget?
) : SingleChildRenderObjectWidget(key, child) {

    override fun createRenderObject(context: BuildContext): RenderBlockSemantics {
        return RenderBlockSemantics(blocking = blocking)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("blocking", blocking))
    }
}

/**
 * A widget that drops all the semantics of its descendants.
 *
 * When [excluding] is true, this widget (and its subtree) is excluded from
 * the semantics tree.
 *
 * This can be used to hide descendant widgets that would otherwise be
 * reported but that would only be confusing. For example, the
 * material library's [Chip] widget hides the avatar since it is
 * redundant with the chip label.
 *
 * See also:
 *
 * * [BlockSemantics] which drops semantics of widgets earlier in the tree.
 */
class ExcludeSemantics(
    key: Key?,
    /** Whether this widget is excluded in the semantics tree. */
    val excluding: Boolean = true,
    child: Widget?
) : SingleChildRenderObjectWidget(key, child) {

    override fun createRenderObject(context: BuildContext): RenderExcludeSemantics {
        return RenderExcludeSemantics(excluding = excluding)
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        renderObject as RenderExcludeSemantics
        renderObject.excluding = excluding
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("excluding", excluding))
    }
}