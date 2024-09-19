/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.integration.demos

import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.lightColors
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeIntRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.children
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val InspectorButtonTestTag =
    "androidx.compose.foundation.demos.AccessibilityNodeInspectorButton"

/** The key used to read Compose testTag semantics properties from accessibility nodes' extras. */
private const val TestTagExtrasKey = "androidx.compose.ui.semantics.testTag"

private const val LogTag = "A11yNodeInspector"

private val UnsupportedMessage =
    "This tool is not supported on this device. AccessibilityNodeInfo objects are not readable " +
        "by code in the same process without an accessibility service before API 34.\n\n" +
        "This device is running API ${Build.VERSION.SDK_INT}."

private const val UsageMessage =
    "Drag anywhere to explore accessibility nodes.\n\n" +
        "Release to view the node's properties and print the information to logcat " +
        "(tagged \"$LogTag\").\n\n" +
        "Go back to close inspector."

/**
 * A composable that, when touched or dragged, will immediately show an overlay on the current
 * window that allows the user to interactively explore accessibility nodes and view their
 * properties.
 */
@Composable
fun AccessibilityNodeInspectorButton(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var active by remember { mutableStateOf(false) }
    val state = rememberAccessibilityNodeInspectorState()
    Box(
        propagateMinConstraints = true,
        modifier =
            modifier
                // This node needs to have the same gesture modifier as the dedicated inspector
                // overlay
                // since when the button is dragged initially, the pointer events will all still be
                // sent
                // to the button, and not the overlay, even though the overlay will immediately be
                // shown. Because node coordinates are all communicated in screen space, it doesn't
                // actually matter which window accepts the pointer events.
                .then(NodeSelectionGestureModifier(state, onDragStarted = { active = true }))
                // Tag the button so the inspector can detect when the button itself is selected and
                // show a help message.
                .semantics(mergeDescendants = true) { testTag = InspectorButtonTestTag }
    ) {
        content()

        if (active) {
            if (Build.VERSION.SDK_INT >= 34) {
                AccessibilityNodeInspector(state = state, onDismissRequest = { active = false })
            } else {
                AlertDialog(
                    onDismissRequest = { active = false },
                    title = { Text("Accessibility Node Inspector") },
                    text = { Text(UnsupportedMessage) },
                    buttons = {
                        Button(
                            onClick = { active = false },
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            Text("DISMISS")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Returns true if this [NodeInfo] or any of its ancestors represents an
 * [AccessibilityNodeInspectorButton].
 */
private val NodeInfo.isInspectorButton: Boolean
    get() {
        if (Build.VERSION.SDK_INT >= 26) {
            visitSelfAndAncestors {
                val testTag =
                    AccessibilityNodeInfoHelper.readExtraData(
                        it.nodeInfo.unwrap(),
                        TestTagExtrasKey
                    )
                if (testTag == InspectorButtonTestTag) {
                    return true
                }
            }
        }
        return false
    }

// region Selection UI

/** A popup that overlays another window and allows exploring its accessibility nodes by touch. */
@Composable
private fun AccessibilityNodeInspector(
    state: AccessibilityNodeInspectorState,
    onDismissRequest: () -> Unit,
) {
    if (state.isReady) {
        Popup(
            popupPositionProvider = state,
            properties =
                PopupProperties(
                    focusable = true,
                    excludeFromSystemGesture = false,
                ),
            onDismissRequest = onDismissRequest
        ) {
            Box(
                propagateMinConstraints = true,
                modifier =
                    Modifier.width { state.inspectorWindowSize.width }
                        .height { state.inspectorWindowSize.height }
            ) {
                // Selection UI and input handling.
                Box(
                    Modifier.then(NodeSelectionGestureModifier(state))
                        .then(DrawSelectionOverlayModifier(state))
                )

                state.nodeUnderInspection?.let {
                    if (it.isInspectorButton) {
                        // Don't use Surface here, it breaks touch input.
                        Text(
                            UsageMessage,
                            modifier =
                                Modifier.wrapContentSize()
                                    .padding(16.dp)
                                    .background(MaterialTheme.colors.surface)
                                    .padding(16.dp)
                        )
                    } else {
                        InspectorNodeDetailsDialog(
                            leafNode = it,
                            onNodeClick = state::inspectNode,
                            onBack = onDismissRequest,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A modifier that draws the current selection of an [AccessibilityNodeInspectorState] in an
 * [AccessibilityNodeInspector].
 */
private data class DrawSelectionOverlayModifier(val state: AccessibilityNodeInspectorState) :
    ModifierNodeElement<DrawSelectionOverlayModifierNode>() {
    override fun create(): DrawSelectionOverlayModifierNode =
        DrawSelectionOverlayModifierNode(state)

    override fun update(node: DrawSelectionOverlayModifierNode) {
        check(node.state === state) { "Cannot change state" }
    }

    override fun InspectorInfo.inspectableProperties() {}
}

private class DrawSelectionOverlayModifierNode(val state: AccessibilityNodeInspectorState) :
    Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        val coords = requireLayoutCoordinates()
        state.nodesUnderCursor.let { nodes ->
            if (nodes.isNotEmpty()) {
                val layerAlpha = 0.8f / nodes.size
                nodes.fastForEach { node ->
                    val bounds = coords.screenToLocal(node.boundsInScreen)
                    clipRect(
                        left = bounds.left.toFloat(),
                        top = bounds.top.toFloat(),
                        right = bounds.right.toFloat(),
                        bottom = bounds.bottom.toFloat(),
                        clipOp = ClipOp.Difference
                    ) {
                        drawRect(Color.Black.copy(alpha = layerAlpha))
                    }
                }
            }
        }

        state.highlightedNode?.let { node ->
            val lastBounds = coords.screenToLocal(node.boundsInScreen)
            drawRect(
                Color.Green,
                style = Stroke(1.dp.toPx()),
                topLeft = lastBounds.topLeft.toOffset(),
                size = lastBounds.size.toSize()
            )
        }

        state.selectionOffset
            .takeIf { it.isSpecified }
            ?.let { screenOffset ->
                val localOffset = coords.screenToLocal(screenOffset)
                drawLine(
                    Color.Red,
                    start = Offset(0f, localOffset.y),
                    end = Offset(size.width, localOffset.y)
                )
                drawLine(
                    Color.Red,
                    start = Offset(localOffset.x, 0f),
                    end = Offset(localOffset.x, size.height)
                )
            }
    }

    private fun LayoutCoordinates.screenToLocal(rect: IntRect): IntRect {
        return IntRect(
            topLeft = screenToLocal(rect.topLeft.toOffset()).round(),
            bottomRight = screenToLocal(rect.bottomRight.toOffset()).round(),
        )
    }
}

/**
 * A modifier that accepts pointer input to select accessibility nodes in an
 * [AccessibilityNodeInspectorState].
 */
private data class NodeSelectionGestureModifier(
    val state: AccessibilityNodeInspectorState,
    val onDragStarted: (() -> Unit)? = null,
) : ModifierNodeElement<NodeSelectionGestureModifierNode>() {
    override fun create(): NodeSelectionGestureModifierNode =
        NodeSelectionGestureModifierNode(state, onDragStarted)

    override fun update(node: NodeSelectionGestureModifierNode) {
        check(node.state === state) { "Cannot change state" }
        node.onDragStarted = onDragStarted
    }

    override fun InspectorInfo.inspectableProperties() {}
}

private class NodeSelectionGestureModifierNode(
    val state: AccessibilityNodeInspectorState,
    var onDragStarted: (() -> Unit)?,
) : DelegatingNode() {

    private val pass = PointerEventPass.Initial

    @Suppress("unused")
    private val inputNode =
        delegate(
            SuspendingPointerInputModifierNode {
                // Detect drag gestures but without slop.
                val layoutCoords = requireLayoutCoordinates()
                awaitEachGesture {
                    try {
                        val firstChange = awaitFirstDown(pass = pass)
                        state.setNodeCursor(firstChange.position, layoutCoords)
                        onDragStarted?.invoke()
                        firstChange.consume()

                        while (true) {
                            val event = awaitPointerEvent(pass = pass)
                            event.changes
                                .fastFirstOrNull { it.id == firstChange.id }
                                ?.let { change ->
                                    if (change.changedToUp()) {
                                        return@awaitEachGesture
                                    } else {
                                        state.setNodeCursor(change.position, layoutCoords)
                                    }
                                }
                        }
                    } finally {
                        state.inspectNodeUnderCursor()
                    }
                }
            }
        )
}

// endregion

// region Details UI

/**
 * A dialog that shows all the properties of [leafNode] and all its ancestors and allows exploring
 * them interactively.
 */
@Composable
private fun InspectorNodeDetailsDialog(
    leafNode: NodeInfo,
    onNodeClick: (NodeInfo) -> Unit,
    onBack: () -> Unit,
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onBack
    ) {
        InspectorNodeDetails(leafNode = leafNode, onNodeClick = onNodeClick, onBack = onBack)
    }
}

@Composable
private fun InspectorNodeDetails(
    leafNode: NodeInfo,
    onNodeClick: (NodeInfo) -> Unit,
    onBack: () -> Unit
) {
    MaterialTheme(colors = if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        val peekInteractionSource = remember { MutableInteractionSource() }
        val peeking by peekInteractionSource.collectIsPressedAsState()
        Surface(
            modifier = Modifier.padding(16.dp).alpha(if (peeking) 0f else 1f),
            elevation = 4.dp
        ) {
            Column {
                TopAppBar(
                    title = { NodeHeader(leafNode) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}, interactionSource = peekInteractionSource) {
                            Icon(Icons.Filled.Info, contentDescription = null)
                        }
                    }
                )

                NodeProperties(
                    node = leafNode,
                    onNodeClick = onNodeClick,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

private fun NodeInfo.selfAndAncestorsToList() =
    buildList { visitSelfAndAncestors(::add) }.asReversed()

@Composable
private fun NodeHeader(node: NodeInfo) {
    Column {
        val (nodeClassPackage, nodeClassName) = node.nodeInfo.parseClassPackageAndName()
        Text(nodeClassName, fontWeight = FontWeight.Medium)
        Text(
            nodeClassPackage,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.alpha(0.5f),
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}

@Composable
private fun NodeProperties(node: NodeInfo, onNodeClick: (NodeInfo) -> Unit, modifier: Modifier) {
    SelectionContainer {
        Column(modifier = modifier) {
            NodeAncestorLinks(node, onNodeClick)

            val properties =
                node
                    .getProperties()
                    .mapValues { (_, v) ->
                        // Turn references to other nodes into links that actually open those nodes
                        // in the inspector.
                        if (v is AccessibilityNodeInfoCompat) {
                            nodeLinkRepresentation(
                                node = v,
                                onClick = { onNodeClick(v.toNodeInfo()) }
                            )
                        } else {
                            PropertyValueRepresentation(v)
                        }
                    }
                    .toList()
            KeyValueView(
                elements = properties,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun NodeAncestorLinks(node: NodeInfo, onNodeClick: (NodeInfo) -> Unit) {
    val ancestors = remember(node) { node.selfAndAncestorsToList().dropLast(1) }
    if (ancestors.isNotEmpty()) {
        val ancestorLinks =
            remember(ancestors) {
                buildAnnotatedString {
                    ancestors.fastForEachIndexed { index, ancestorNode ->
                        withLink(
                            LinkAnnotation.Clickable("ancestor") { onNodeClick(ancestorNode) }
                        ) {
                            append(ancestorNode.nodeInfo.parseClassPackageAndName().second)
                        }

                        if (index < ancestors.size - 1) {
                            append(" > ")
                        }
                    }
                }
            }

        Surface(
            color = MaterialTheme.colors.primarySurface.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(ancestorLinks, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

private fun nodeLinkRepresentation(node: AccessibilityNodeInfoCompat, onClick: () -> Unit) =
    PropertyValueRepresentation(
        buildAnnotatedString {
            withLink(LinkAnnotation.Clickable("node") { onClick() }) { append(node.className) }
        }
    )

/**
 * Shows a table of keys and their values. Values are rendered using [PropertyValueRepresentation].
 */
@Composable
private fun KeyValueView(
    elements: List<Pair<String, PropertyValueRepresentation>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = spacedBy(8.dp)) {
        elements.forEach { (name, valueRepresentation) -> KeyValueRow(name, valueRepresentation) }
    }
}

/**
 * A row inside a [KeyValueView] that shows a single key and its value. The value will be shown
 * beside the row, if there's space, otherwise it will be placed below it.
 */
@Composable
private fun KeyValueRow(name: String, valueRepresentation: PropertyValueRepresentation) {
    KeyValueRowLayout(
        contentPadding = 8.dp,
        keyContent = {
            Text(
                name,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.alpha(0.5f)
            )
        },
        valueContent = {
            if (valueRepresentation.customRenderer != null) {
                valueRepresentation.customRenderer.invoke()
            } else {
                Text(
                    valueRepresentation.text,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    )
}

/**
 * Places [keyContent] and [valueContent] on the same line if they both fit with [contentPadding]
 * spacing, otherwise places [valueContent] below [keyContent] and indents it by [contentPadding].
 * If [valueContent] wraps and fills all available space, a thin line is drawn in the margin to help
 * visually track the nesting level.
 */
@Composable
private inline fun KeyValueRowLayout(
    contentPadding: Dp,
    keyContent: @Composable RowScope.() -> Unit,
    valueContent: @Composable RowScope.() -> Unit,
) {
    var nestingIndicator: Pair<Offset, Offset>? by remember { mutableStateOf(null) }

    Layout(
        modifier =
            Modifier.drawBehind {
                nestingIndicator?.let { (start, end) ->
                    drawLine(
                        start = start,
                        end = end,
                        color = Color.Gray,
                        alpha = 0.3f,
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
        content = {
            Row(content = keyContent)
            Row(content = valueContent)
        },
        measurePolicy = { measurables, constraints ->
            val contentPaddingPx = contentPadding.roundToPx()
            val (keyMeasurable, valueMeasurable) = measurables
            val keyConstraints = constraints.copyMaxDimensions()
            // contentPadding will either act as the spacing between items if they fit on the same
            // line, or indent if content wraps, so inset the constraints either way.
            val valueConstraints =
                constraints.copyMaxDimensions().offset(horizontal = -contentPaddingPx)
            val keyPlaceable = keyMeasurable.measure(keyConstraints)
            val valuePlaceable = valueMeasurable.measure(valueConstraints)
            val wrap =
                keyPlaceable.width + contentPaddingPx + valuePlaceable.width > constraints.maxWidth

            val totalWidth = constraints.maxWidth
            val totalHeight =
                if (wrap) {
                    keyPlaceable.height + valuePlaceable.height
                } else {
                    maxOf(keyPlaceable.height, valuePlaceable.height)
                }

            // Only draw the nesting indicator if the value filled its max width, which indicates it
            // will probably be taller, and harder to track the start edge visually.
            nestingIndicator =
                if (wrap && valuePlaceable.width == valueConstraints.maxWidth) {
                    Pair(
                        Offset(contentPaddingPx / 2f, keyPlaceable.height.toFloat()),
                        Offset(contentPaddingPx / 2f, totalHeight.toFloat())
                    )
                } else {
                    null
                }

            layout(totalWidth, totalHeight) {
                val valueX = totalWidth - valuePlaceable.width
                if (wrap) {
                    // Arrange vertically.
                    keyPlaceable.placeRelative(0, 0)
                    valuePlaceable.placeRelative(valueX, keyPlaceable.height)
                } else {
                    // Arrange horizontally.
                    val keyY =
                        Alignment.CenterVertically.align(
                            size = keyPlaceable.height,
                            space = totalHeight
                        )
                    keyPlaceable.placeRelative(0, keyY)

                    val valueY =
                        Alignment.CenterVertically.align(
                            size = valuePlaceable.height,
                            space = totalHeight
                        )
                    valuePlaceable.placeRelative(valueX, valueY)
                }
            }
        }
    )
}

/**
 * A representation of an arbitrary value as a potentially-styled [AnnotatedString], and optionally
 * also as a completely custom composable. To create an instance for standard types, call the
 * [PropertyValueRepresentation] function.
 */
private data class PropertyValueRepresentation(
    val text: AnnotatedString,
    val customRenderer: (@Composable () -> Unit)? = null
)

private val ValueTypeTextStyle = TextStyle(fontFamily = FontFamily.Monospace)

/**
 * Creates a [PropertyValueRepresentation] appropriate for certain well-known types. For other types
 * returns a representation that is just the result of the value's [toString].
 */
private fun PropertyValueRepresentation(value: Any?): PropertyValueRepresentation =
    when (value) {
        is CharSequence -> PropertyValueRepresentation(value.toFormattedDebugString())
        is Iterable<*> -> {
            val valueType = value.javaClass.canonicalName ?: value.javaClass.name
            // No isEmpty on iterable.
            if (!value.iterator().hasNext()) {
                PropertyValueRepresentation(AnnotatedString("$valueType()"))
            } else {
                PropertyValueRepresentation(AnnotatedString(value.toString())) {
                    Column {
                        Text(valueType, style = ValueTypeTextStyle)
                        KeyValueView(
                            value.mapIndexed { index, element ->
                                Pair("[$index]", PropertyValueRepresentation(element))
                            }
                        )
                    }
                }
            }
        }
        is Map<*, *> -> {
            val valueType = value.javaClass.canonicalName ?: value.javaClass.name
            if (value.isEmpty()) {
                PropertyValueRepresentation(AnnotatedString("$valueType()"))
            } else {
                PropertyValueRepresentation(AnnotatedString(value.toString())) {
                    Column {
                        Text(valueType, style = ValueTypeTextStyle)
                        KeyValueView(
                            value.entries.map { (key, value) ->
                                Pair(key.toString(), PropertyValueRepresentation(value))
                            }
                        )
                    }
                }
            }
        }
        is Bundle -> {
            if (value.isEmpty) {
                PropertyValueRepresentation(
                    AnnotatedString("empty Bundle", SpanStyle(fontStyle = FontStyle.Italic))
                )
            } else {
                PropertyValueRepresentation(AnnotatedString(value.toString())) {
                    KeyValueView(
                        value.keySet().map { key ->
                            @Suppress("DEPRECATION") val rawValue = value.get(key)
                            Pair(key, PropertyValueRepresentation(rawValue))
                        }
                    )
                }
            }
        }
        else -> PropertyValueRepresentation(AnnotatedString(value.toString()))
    }

/** Returns the package and simple name parts of a FQCN by splitting at the last '.' character. */
private fun AccessibilityNodeInfoCompat.parseClassPackageAndName(): Pair<String, String> {
    val separatorIndex = className.indexOfLast { it == '.' }
    return Pair(className.substring(0, separatorIndex), className.substring(separatorIndex + 1))
}

/**
 * A column of expandable headers. Only one header can be expanded at a time. To create an item call
 * [AccordionScope.item] in [content].
 */
@Composable
private fun Accordion(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: AccordionScope.() -> Unit
) {
    Column(modifier) {
        // Don't rebuild the items every time the selection changes.
        val items by remember(content) { derivedStateOf { buildAccordionItems(content) } }
        val isSelectedIndexValid = selectedIndex in items.indices
        items.fastForEachIndexed { index, item ->
            val isItemSelected = index == selectedIndex
            AccordionItemView(
                item = item,
                headerHeight = 40.dp,
                isExpanded = isItemSelected,
                shrinkHeader = !isItemSelected && isSelectedIndexValid,
                onHeaderClick = { onSelectIndex(if (selectedIndex == index) -1 else index) },
            )
            if (index < items.size - 1) {
                Divider()
            }
        }
    }
}

/**
 * An item header and optionally-visible content inside an [Accordion]. Only intended to be called
 * by [Accordion] itself.
 */
@Composable
private fun AccordionItemView(
    item: AccordionItem,
    headerHeight: Dp,
    isExpanded: Boolean,
    shrinkHeader: Boolean,
    onHeaderClick: () -> Unit
) {
    // Shrink collapsed headers to give more space to the expanded body.
    val headerScale by animateFloatAsState(if (shrinkHeader) 0.8f else 1f, label = "headerScale")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.height { (headerHeight * headerScale).roundToPx() }
                .fillMaxWidth()
                .selectable(selected = isExpanded, onClick = onHeaderClick)
                .graphicsLayer {
                    scaleX = headerScale
                    scaleY = headerScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
    ) {
        val iconRotation by
            animateFloatAsState(if (isExpanded) 0f else -90f, label = "iconRotation")
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.graphicsLayer { rotationZ = iconRotation }
        )
        item.header()
    }
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        item.content()
    }
}

private interface AccordionScope {
    /**
     * Creates an accordion item with a [header] that is always visible, and a [body] that is only
     * visible when the item is expanded.
     */
    fun item(header: @Composable () -> Unit, body: @Composable () -> Unit)
}

private data class AccordionItem(
    val header: @Composable () -> Unit,
    val content: @Composable () -> Unit
)

private fun buildAccordionItems(content: AccordionScope.() -> Unit): List<AccordionItem> {
    return buildList {
        content(
            object : AccordionScope {
                override fun item(header: @Composable () -> Unit, body: @Composable () -> Unit) {
                    add(AccordionItem(header, body))
                }
            }
        )
    }
}

/** Sets [key] to [value] in this map if [value] is not [unspecifiedValue] (null by default). */
private fun MutableMap<String, Any?>.setIfSpecified(
    key: String,
    value: Any?,
    unspecifiedValue: Any? = null
) {
    if (value != unspecifiedValue) {
        set(key, value)
    }
}

/** Sets [key] to [value] in this map if [value] is not [unspecifiedValue] (false by default). */
private fun MutableMap<String, Any?>.setIfSpecified(
    key: String,
    value: Boolean,
    unspecifiedValue: Boolean = false
) {
    if (value != unspecifiedValue) {
        set(key, value)
    }
}

/** Sets [key] to [value] in this map if [value] is not [unspecifiedValue] (0 by default). */
private fun MutableMap<String, Any?>.setIfSpecified(
    key: String,
    value: Int,
    unspecifiedValue: Int = 0
) {
    if (value != unspecifiedValue) {
        set(key, value)
    }
}

/**
 * Returns an [AnnotatedString] that makes this [CharSequence] value easier to read for debugging.
 * Wraps the value in stylized quote marks so empty strings are more clear, and replaces invisible
 * control characters (e.g. `'\n'`) with their stylized literal escape sequences.
 */
private fun CharSequence.toFormattedDebugString(): AnnotatedString = buildAnnotatedString {
    val quoteStyle = SpanStyle(color = Color.Gray, fontWeight = FontWeight.Bold)
    val specialStyle =
        SpanStyle(
            color = Color.Red,
            fontWeight = FontWeight.Bold,
        )

    withStyle(quoteStyle) { append('"') }

    this@toFormattedDebugString.forEach { c ->
        var formattedChar: String? = null
        when (c) {
            '\n' -> formattedChar = "\\n"
            '\r' -> formattedChar = "\\r"
            '\t' -> formattedChar = "\\t"
            '\b' -> formattedChar = "\\b"
        }
        if (formattedChar != null) {
            withStyle(specialStyle) { append(formattedChar) }
        } else {
            append(c)
        }
    }

    withStyle(quoteStyle) { append('"') }
}

// endregion

/** Like the standard [Modifier.width] modifier but the width is only calculated at measure time. */
private fun Modifier.width(calculateWidth: Density.() -> Int): Modifier =
    layout { measurable, constraints ->
        val calculatedWidth = calculateWidth()
        val childConstraints =
            constraints.copy(minWidth = calculatedWidth, maxWidth = calculatedWidth)
        val placeable = measurable.measure(childConstraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/**
 * Like the standard [Modifier.height] modifier but the height is only calculated at measure time.
 */
private fun Modifier.height(calculateHeight: Density.() -> Int): Modifier =
    layout { measurable, constraints ->
        val calculatedHeight = calculateHeight()
        val childConstraints =
            constraints.copy(minHeight = calculatedHeight, maxHeight = calculatedHeight)
        val placeable = measurable.measure(childConstraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

// region Accessibility node access

/**
 * Creates and remembers an [AccessibilityNodeInspectorState] for inspecting the nodes in the window
 * hosting this composition.
 */
@Composable
private fun rememberAccessibilityNodeInspectorState(): AccessibilityNodeInspectorState {
    val hostView = LocalView.current
    val state = remember(hostView) { AccessibilityNodeInspectorState(hostView = hostView) }
    LaunchedEffect(state) { state.runWhileDisplayed() }

    DisposableEffect(hostView) {
        val testRoot = hostView as RootForTest
        onDispose { testRoot.forceAccessibilityForTesting(false) }
    }
    return state
}

/** State holder for an [AccessibilityNodeInspectorButton]. */
private class AccessibilityNodeInspectorState(private val hostView: View) :
    PopupPositionProvider, View.OnLayoutChangeListener {

    var inspectorWindowSize: IntSize by mutableStateOf(calculateInspectorWindowSize())
        private set

    private val service: InspectableTreeProvider =
        if (Build.VERSION.SDK_INT >= 34) {
            AccessibilityTreeInspectorApi34(hostView.rootView)
        } else {
            NoopTreeProvider
        }

    val isReady: Boolean by derivedStateOf {
        inspectorWindowSize.width > 0 && inspectorWindowSize.height > 0
    }

    var selectionOffset: Offset by mutableStateOf(Offset.Unspecified)
        private set

    /**
     * All the nodes that pass the hit test after a call to [setNodeCursor], or if a node is
     * programmatically selected via [inspectNode] then that node and all its ancestors.
     */
    var nodesUnderCursor: List<NodeInfo> by mutableStateOf(emptyList())
        private set

    /**
     * The node to highlight – during selection, this will be the node that will be opened in the
     * inspector when the gesture is finished.
     */
    var highlightedNode: NodeInfo? by mutableStateOf(null)
        private set

    /** If non-null, the node being shown in the inspector. */
    var nodeUnderInspection: NodeInfo? by mutableStateOf(null)
        private set

    /**
     * Temporarily select the node at [localOffset] in the window being inspected. This should be
     * called while the user is dragging.
     */
    fun setNodeCursor(localOffset: Offset, layoutCoordinates: LayoutCoordinates) {
        hideInspector()
        val screenOffset = layoutCoordinates.localToScreen(localOffset)
        selectionOffset = screenOffset
        nodesUnderCursor = service.findNodesAt(screenOffset)
        highlightedNode = nodesUnderCursor.lastOrNull()
    }

    /** Opens the node under the selection cursor in the inspector and dumps it to logcat. */
    fun inspectNodeUnderCursor() {
        selectionOffset = Offset.Unspecified
        nodeUnderInspection = highlightedNode?.also { it.dumpToLog(tag = LogTag) }
    }

    /**
     * Highlights the given node in the selection popup, dumps it to logcat, and opens it in the
     * inspector.
     */
    fun inspectNode(node: NodeInfo?) {
        highlightedNode = node
        nodesUnderCursor = node?.selfAndAncestorsToList() ?: emptyList()
        nodeUnderInspection = node
        node?.also { it.dumpToLog(tag = LogTag) }
    }

    /** Hides the inspector dialog to allow the user to select a different node. */
    fun hideInspector() {
        nodeUnderInspection = null
    }

    /** Runs any coroutine effects the state holder requires while it's connected to some UI. */
    suspend fun runWhileDisplayed() {
        service.initialize()

        coroutineScope {
            // Update the overlay window size when the target window is resized.
            launch {
                hostView.addOnLayoutChangeListener(this@AccessibilityNodeInspectorState)
                try {
                    awaitCancellation()
                } finally {
                    hostView.removeOnLayoutChangeListener(this@AccessibilityNodeInspectorState)
                }
            }
        }
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        inspectorWindowSize = calculateInspectorWindowSize()
    }

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset.Zero

    private fun calculateInspectorWindowSize(): IntSize {
        return Rect()
            .also { hostView.getWindowVisibleDisplayFrame(it) }
            .let { IntSize(it.width(), it.height()) }
    }
}

private data class NodeInfo(
    val nodeInfo: AccessibilityNodeInfoCompat,
    val boundsInScreen: IntRect,
)

/** Returns a map with all the inspectable properties of this [NodeInfo]. */
private fun NodeInfo.getProperties(): Map<String, Any?> = buildMap {
    val node = nodeInfo
    // Don't render className, it's in the title.
    setIfSpecified("packageName", node.packageName)
    setIfSpecified("boundsInScreen", Rect().also(node::getBoundsInScreen))
    setIfSpecified("boundsInWindow", Rect().also(node::getBoundsInWindow))
    setIfSpecified("viewIdResourceName", node.viewIdResourceName)
    setIfSpecified("uniqueId", node.uniqueId)
    setIfSpecified("text", node.text)
    setIfSpecified("textSelectionStart", node.textSelectionStart, unspecifiedValue = -1)
    setIfSpecified("textSelectionEnd", node.textSelectionEnd, unspecifiedValue = -1)
    setIfSpecified("contentDescription", node.contentDescription)
    setIfSpecified("collectionInfo", node.collectionInfo)
    setIfSpecified("collectionItemInfo", node.collectionItemInfo)
    setIfSpecified("containerTitle", node.containerTitle)
    setIfSpecified("childCount", node.childCount)
    setIfSpecified("drawingOrder", node.drawingOrder)
    setIfSpecified("error", node.error)
    setIfSpecified("hintText", node.hintText)
    setIfSpecified("inputType", node.inputType)
    setIfSpecified("isAccessibilityDataSensitive", node.isAccessibilityDataSensitive)
    setIfSpecified("isAccessibilityFocused", node.isAccessibilityFocused)
    setIfSpecified("isCheckable", node.isCheckable)
    setIfSpecified("isChecked", node.isChecked)
    setIfSpecified("isClickable", node.isClickable)
    setIfSpecified("isLongClickable", node.isLongClickable)
    setIfSpecified("isContextClickable", node.isContextClickable)
    setIfSpecified("isContentInvalid", node.isContentInvalid)
    setIfSpecified("isDismissable", node.isDismissable)
    setIfSpecified("isEditable", node.isEditable)
    setIfSpecified("isEnabled", node.isEnabled, unspecifiedValue = true)
    setIfSpecified("isFocusable", node.isFocusable)
    setIfSpecified("isFocused", node.isFocused)
    setIfSpecified("isGranularScrollingSupported", node.isGranularScrollingSupported)
    setIfSpecified("isHeading", node.isHeading)
    set("isImportantForAccessibility", node.isImportantForAccessibility)
    setIfSpecified("isMultiLine", node.isMultiLine)
    setIfSpecified("isPassword", node.isPassword)
    setIfSpecified("isScreenReaderFocusable", node.isScreenReaderFocusable)
    setIfSpecified("isScrollable", node.isScrollable)
    setIfSpecified("isSelected", node.isSelected)
    setIfSpecified("isShowingHintText", node.isShowingHintText)
    setIfSpecified("isTextEntryKey", node.isTextEntryKey)
    setIfSpecified("isTextSelectable", node.isTextSelectable)
    setIfSpecified("isVisibleToUser", node.isVisibleToUser, unspecifiedValue = true)
    setIfSpecified("labelFor", node.labelFor)
    setIfSpecified("labeledBy", node.labeledBy)
    setIfSpecified("liveRegion", node.liveRegion)
    setIfSpecified("maxTextLength", node.maxTextLength, unspecifiedValue = -1)
    setIfSpecified("movementGranularities", node.movementGranularities)
    setIfSpecified("paneTitle", node.paneTitle)
    setIfSpecified("rangeInfo", node.rangeInfo)
    setIfSpecified("roleDescription", node.roleDescription)
    setIfSpecified("stateDescription", node.stateDescription)
    setIfSpecified("tooltipText", node.tooltipText)
    setIfSpecified("touchDelegateInfo", node.touchDelegateInfo)
    setIfSpecified("windowId", node.windowId, unspecifiedValue = -1)
    setIfSpecified("canOpenPopup", node.canOpenPopup())
    setIfSpecified(
        "hasRequestInitialAccessibilityFocus",
        node.hasRequestInitialAccessibilityFocus()
    )
    setIfSpecified("extras", node.extrasWithoutExtraData)
    setIfSpecified("extraRenderingInfo", node.extraRenderingInfo)

    if (Build.VERSION.SDK_INT >= 26 && node.availableExtraData.isNotEmpty()) {
        val extraData = mutableMapOf<String, Any?>()
        node.availableExtraData.forEach { key ->
            extraData[key] = AccessibilityNodeInfoHelper.readExtraData(node.unwrap(), key)
        }
        setIfSpecified("extraData (from availableExtraData)", extraData)
    }

    setIfSpecified("traversalBefore", node.traversalBefore)
    setIfSpecified("traversalAfter", node.traversalAfter)
}

/**
 * Returns the extras bundle, but without any keys from
 * [AccessibilityNodeInfoCompat.getAvailableExtraData], since those are reported separately.
 */
private val AccessibilityNodeInfoCompat.extrasWithoutExtraData: Bundle
    get() {
        val extras = Bundle(extras)
        availableExtraData.forEach { extras.remove(it) }
        return extras
    }

/** Class verification helper for reading extras data from an [AccessibilityNodeInfo]. */
@RequiresApi(26)
private object AccessibilityNodeInfoHelper {
    fun readExtraData(node: AccessibilityNodeInfo, key: String): Any? {
        if (key in node.availableExtraData && node.refreshWithExtraData(key, Bundle())) {
            @Suppress("DEPRECATION") return node.extras.get(key)
        } else {
            return null
        }
    }
}

private interface InspectableTreeProvider {
    fun initialize() {}

    fun findNodesAt(screenOffset: Offset): List<NodeInfo>
}

private object NoopTreeProvider : InspectableTreeProvider {
    override fun findNodesAt(screenOffset: Offset): List<NodeInfo> = emptyList()
}

@RequiresApi(34)
private class AccessibilityTreeInspectorApi34(private val rootView: View) :
    InspectableTreeProvider {

    private val matrixCache = Matrix()

    override fun initialize() {
        // This will call setQueryableFromApp process, which enables accessibility on the platform,
        // which allows us to tell compose views to force accessibility support. This is required
        // for certain fields, such as traversal before/after, to be populated.
        rootView.createNodeInfo()
        rootView.visitViewAndChildren { view ->
            (view as? RootForTest)?.forceAccessibilityForTesting(true)
            true
        }
    }

    override fun findNodesAt(screenOffset: Offset): List<NodeInfo> {
        rootView.transformMatrixToLocal(matrixCache)

        val nodes = mutableListOf<NodeInfo>()
        val rootInfo = rootView.createNodeInfo()
        rootInfo.visitNodeAndChildren { node ->
            if (node.hitTest(screenOffset)) {
                nodes += node
                true
            } else {
                false
            }
        }
        return nodes
    }

    private fun NodeInfo.hitTest(screenOffset: Offset): Boolean {
        return boundsInScreen.contains(screenOffset.round())
    }

    private inline fun View.visitViewAndChildren(visitor: (View) -> Boolean) {
        val queue = mutableVectorOf(this)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(queue.lastIndex)
            val visitChildren = visitor(current)
            if (visitChildren && current is ViewGroup) {
                for (child in current.children) {
                    queue += child
                }
            }
        }
    }

    private inline fun NodeInfo.visitNodeAndChildren(visitor: (NodeInfo) -> Boolean) {
        val queue = mutableVectorOf(this)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(queue.lastIndex)
            val visitChildren = visitor(current)
            if (visitChildren) {
                for (i in 0 until current.nodeInfo.childCount) {
                    queue += current.nodeInfo.getChild(i).toNodeInfo()
                }
            }
        }
    }

    private fun View.createNodeInfo(): NodeInfo {
        val rawNodeInfo = createAccessibilityNodeInfo()
        val nodeInfoCompat = AccessibilityNodeInfoCompat.wrap(rawNodeInfo)
        rawNodeInfo.setQueryFromAppProcessEnabled(this, true)
        return nodeInfoCompat.toNodeInfo()
    }
}

private fun AccessibilityNodeInfoCompat.toNodeInfo(): NodeInfo =
    NodeInfo(
        nodeInfo = this,
        boundsInScreen = Rect().also(::getBoundsInScreen).toComposeIntRect(),
    )

private fun NodeInfo.dumpToLog(tag: String) {
    val indent = "  "
    var depth = 0
    visitSelfAndAncestors { node ->
        Log.d(tag, indent.repeat(depth) + node.nodeInfo.unwrap().toString())
        depth++
    }
}

private inline fun NodeInfo.visitSelfAndAncestors(block: (NodeInfo) -> Unit) {
    var node: NodeInfo? = this
    while (node != null) {
        block(node)
        node = node.parent
    }
}

private val NodeInfo.parent: NodeInfo?
    get() = nodeInfo.parent?.toNodeInfo()

// endregion
