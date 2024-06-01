/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseChildren
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Default colors for all the TraversableNode(s):
val ROOT_TRAVERSABLE_DEFAULT_COLOR = Color.Cyan
val COLUMN_TRAVERSABLE_DEFAULT_COLOR = Color.White
val BOX_TRAVERSABLE_DEFAULT_COLOR = Color.Magenta
val BOX_NON_TRAVERSABLE_DEFAULT_COLOR = Color.Yellow

// Labels for both drop-down menus:
const val ROOT_LABEL = "Root"
const val COLUMN_A_LABEL = "Column A"
const val BOX_A_LABEL = "Box A"
const val BOX_C_LABEL = "Box C"
const val COLUMN_B_LABEL = "Column B"
const val BOX_E_LABEL = "Box E"

const val ANCESTORS_LABEL = "Ancestors"
const val DESCENDANTS_LABEL = "Descendants"

/**
 * TraversableNode example that does not actually do anything but shows the most simplified example.
 *
 * The traversable functions are separated below (for example, traverseAncestorsWithKeyDemo), so
 * they can be referenced in sample javadocs.
 *
 * For a full featured sample, look below at [TraverseModifierDemo].
 */
class CustomTraversableModifierNode : Modifier.Node(), TraversableNode {
    override val traverseKey = TRAVERSAL_NODE_KEY

    fun doSomethingWithAncestor() {}

    fun doSomethingWithChild() {}

    fun doSomethingWithDescendant() {}
}

/**
 * Simplified example of traverseAncestors with a key. For a full featured sample, look below at
 * [TraverseModifierDemo].
 */
@Sampled
fun traverseAncestorsWithKeyDemo() {
    val customTraversableModifierNode = CustomTraversableModifierNode()

    with(customTraversableModifierNode) {
        traverseAncestors(traverseKey) {
            if (it is CustomTraversableModifierNode) {
                it.doSomethingWithAncestor()
            }
            // Return true to continue searching the tree after a match. If you were looking to
            // match only some of the nodes, you could return false and stop executing the search.
            true
        }
    }
}

/**
 * Simplified example of traverseAncestors. For a full featured sample, look below at
 * [TraverseModifierDemo].
 */
@Sampled
fun traverseAncestorsDemo() {
    val customTraversableModifierNode = CustomTraversableModifierNode()

    with(customTraversableModifierNode) {
        traverseAncestors {
            // Because I use the existing key of the class, I can guarantee 'it' will be of the same
            // type as the class, so I can call my functions directly.
            it.doSomethingWithAncestor()

            // Return true to continue searching the tree after a match. If you were looking to
            // match only some of the nodes, you could return false and stop executing the search.
            true
        }
    }
}

/**
 * Simplified example of traverseChildren with a key. For a full featured sample, look below at
 * [TraverseModifierDemo].
 */
@Sampled
fun traverseChildrenWithKeyDemo() {
    val customTraversableModifierNode = CustomTraversableModifierNode()

    with(customTraversableModifierNode) {
        traverseChildren(traverseKey) {
            if (it is CustomTraversableModifierNode) {
                it.doSomethingWithChild()
            }
            // Return true to continue searching the tree after a match. If you were looking to
            // match only some of the nodes, you could return false and stop executing the search.
            true
        }
    }
}

/**
 * Simplified example of traverseChildren. For a full featured sample, look below at
 * [TraverseModifierDemo].
 */
@Sampled
fun traverseChildrenDemo() {
    val customTraversableModifierNode = CustomTraversableModifierNode()

    with(customTraversableModifierNode) {
        traverseChildren {
            // Because I use the existing key of the class, I can guarantee 'it' will be of the same
            // type as the class, so I can call my functions directly.
            it.doSomethingWithChild()

            // Return true to continue searching the tree after a match. If you were looking to
            // match only some of the nodes, you could return false and stop executing the search.
            true
        }
    }
}

/**
 * Simplified example of traverseDescendants with a key. For a full featured sample, look below at
 * [TraverseModifierDemo].
 */
@Sampled
fun traverseDescendantsWithKeyDemo() {
    val customTraversableModifierNode = CustomTraversableModifierNode()

    with(customTraversableModifierNode) {
        traverseDescendants(traverseKey) {
            if (it is CustomTraversableModifierNode) {
                it.doSomethingWithDescendant()
            }

            // [traverseDescendants()] actually has three options:
            // - ContinueTraversal
            // - SkipSubtreeAndContinueTraversal - rarely used
            // - CancelTraversal
            // They are pretty self explanatory. Usually, you just want to continue or cancel the
            // search. In some rare cases, you might want to skip the subtree but continue searching
            // the tree.
            TraverseDescendantsAction.ContinueTraversal
        }
    }
}

/**
 * Simplified example of traverseDescendants. For a full featured sample, look below at
 * [TraverseModifierDemo].
 */
@Sampled
fun traverseDescendantsDemo() {
    val customTraversableModifierNode = CustomTraversableModifierNode()

    with(customTraversableModifierNode) {
        traverseDescendants {
            // Because I use the existing key of the class, I can guarantee 'it' will be of the same
            // type as the class, so I can call my functions directly.
            it.doSomethingWithDescendant()

            // [traverseDescendants()] actually has three options:
            // - ContinueTraversal
            // - SkipSubtreeAndContinueTraversal - rarely used
            // - CancelTraversal
            // They are pretty self explanatory. Usually, you just want to continue or cancel the
            // search. In some rare cases, you might want to skip the subtree but continue searching
            // the tree.
            TraverseDescendantsAction.ContinueTraversal
        }
    }
}

/**
 * Demonstrates how to use TraversableNode to traverse Modifier.Node ancestors, children, and
 * descendants of the same key. This is done in 5 main steps:
 *
 * Step 1: Create a unique key for your TraversableNode. Step 2: Create a custom Modifier.Node class
 * that implements TraversableNode. Step 3: Create a custom Modifier.Element class that uses that
 * custom Modifier.Node. Step 4: Create an extension function on Modifier that uses the
 * Modifier.Element. Step 5: Traverse the Modifier chain when you need to find your node(s).
 *
 * You can see the impact visually of traversing a particular direction from a TraversableNode by
 * the TraversableNode(s) above or below changing their color to green. You can always reset the
 * colors to try out a different traversal methods or nodes.
 *
 * Important Note: References are maintained to all the TraversableNode(s) in this UI, so we can
 * directly call the traversable functions to see how they impact the tree. (We see this visually by
 * the nodes changing color to green.) However, you most likely won't need a block parameter in your
 * code to assign a reference. You will probably just trigger your traversals based on some other
 * event (like a PointerEvent).
 *
 * See Compose Foundation's PointerHoverIconModifierNode or the DragAndDropNode implementations for
 * real life examples. (PointerHoverIconModifierNode also skips subtrees.)
 *
 * You can also see more examples in TraversableModifierNodeTest (where we use multiple
 * TraversableNodes with different keys).
 *
 * Simplified UI hierarchy for this demo (starts at first TraversableNode and ignores
 * non-traversable Row, Spacers, and Text label composables along the way):
 *
 * Column Root (TraversableBackgroundModifierNode) ⤷ Column A (TraversableBackgroundModifierNode) ⤷
 * Box A (TraversableBackgroundModifierNode) ⤷ Box B (NON-TRAVERSABLE Box) ⤷ Box C
 * (TraversableBackgroundModifierNode)
 *
 * ⤷ Column B (TraversableBackgroundModifierNode) ⤷ Box D (NON-TRAVERSABLE Box) ⤷ Box E
 * (TraversableBackgroundModifierNode) ⤷ Box F (NON-TRAVERSABLE Box)
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TraverseModifierDemo() {

    // Normally, you probably don't want to maintain a reference to your Traversable Node. However,
    // we do here, so you can see what happens when you make calls from different nodes and see
    // the impact they have on the node chain.
    var rootTraversableModifierNode: TraversableBackgroundModifierNode? by remember {
        mutableStateOf(null)
    }

    var columnATraversableModifierNode: TraversableBackgroundModifierNode? by remember {
        mutableStateOf(null)
    }

    var boxATraversableModifierNode: TraversableBackgroundModifierNode? by remember {
        mutableStateOf(null)
    }

    var boxCTraversableModifierNode: TraversableBackgroundModifierNode? by remember {
        mutableStateOf(null)
    }

    var columnBTraversableModifierNode: TraversableBackgroundModifierNode? by remember {
        mutableStateOf(null)
    }

    var boxETraversableModifierNode: TraversableBackgroundModifierNode? by remember {
        mutableStateOf(null)
    }

    // Menu options for picking a traversable node to make the call from
    val nodeMenuOptions =
        listOf(ROOT_LABEL, COLUMN_A_LABEL, BOX_A_LABEL, BOX_C_LABEL, COLUMN_B_LABEL, BOX_E_LABEL)
    var nodeMenuExpanded by remember { mutableStateOf(false) }
    var nodeMenuSelectedOptionText by remember { mutableStateOf(nodeMenuOptions[0]) }

    // Menu options for picking the actual traversal call made on the node
    val traversalMenuOptions = listOf(ANCESTORS_LABEL, DESCENDANTS_LABEL)
    var traversalMenuExpanded by remember { mutableStateOf(false) }
    var traversalMenuSelectedOptionText by remember { mutableStateOf(traversalMenuOptions[1]) }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(10.dp)
                .border(width = 1.dp, color = Color.Black)
                .padding(10.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text =
                "Select the Traversable Node and traversal type you want to run.\n\n" +
                    "You can see the results of the tree by which Node is green.\n\n" +
                    "The UI matches the structure of the Composable UI tree.\n\n" +
                    "To reset the colors, click the \"Reset\" button\n."
        )

        Row(modifier = Modifier.fillMaxWidth()) {

            // Lets users select which TraversableNode they want to call the traversable method on.
            ExposedDropdownMenuBox(
                modifier = Modifier.weight(4f),
                expanded = nodeMenuExpanded,
                onExpandedChange = { nodeMenuExpanded = !nodeMenuExpanded }
            ) {
                TextField(
                    readOnly = true,
                    value = nodeMenuSelectedOptionText,
                    onValueChange = { /* No-op */ },
                    label = { Text("Node") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = nodeMenuExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = nodeMenuExpanded,
                    onDismissRequest = { nodeMenuExpanded = false }
                ) {
                    nodeMenuOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            onClick = {
                                nodeMenuSelectedOptionText = selectionOption
                                nodeMenuExpanded = false
                            }
                        ) {
                            Text(text = selectionOption)
                        }
                    }
                }
            }

            // Lets users select which traversal method they would like to take (going through the
            // ancestors or the descendants.
            ExposedDropdownMenuBox(
                modifier = Modifier.weight(5f),
                expanded = traversalMenuExpanded,
                onExpandedChange = { traversalMenuExpanded = !traversalMenuExpanded }
            ) {
                TextField(
                    readOnly = true,
                    value = traversalMenuSelectedOptionText,
                    onValueChange = { /* No-op */ },
                    label = { Text("Traversal type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = traversalMenuExpanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = traversalMenuExpanded,
                    onDismissRequest = { traversalMenuExpanded = false }
                ) {
                    traversalMenuOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            onClick = {
                                traversalMenuSelectedOptionText = selectionOption
                                traversalMenuExpanded = false
                            }
                        ) {
                            Text(text = selectionOption)
                        }
                    }
                }
            }

            Button(
                modifier = Modifier.weight(2f),
                onClick = {
                    val selectedNode =
                        when (nodeMenuSelectedOptionText) {
                            ROOT_LABEL -> {
                                rootTraversableModifierNode
                            }
                            COLUMN_A_LABEL -> {
                                columnATraversableModifierNode
                            }
                            BOX_A_LABEL -> {
                                boxATraversableModifierNode
                            }
                            BOX_C_LABEL -> {
                                boxCTraversableModifierNode
                            }
                            COLUMN_B_LABEL -> {
                                columnBTraversableModifierNode
                            }
                            BOX_E_LABEL -> {
                                boxETraversableModifierNode
                            }
                            else -> {
                                null
                            }
                        }

                    selectedNode?.let {
                        // Step 5: Traverse the Modifier chain when you need to find your node(s)
                        if (traversalMenuSelectedOptionText == ANCESTORS_LABEL) {
                            it.traverseAncestorsAndChangeColorToGreen()
                        } else if (traversalMenuSelectedOptionText == DESCENDANTS_LABEL) {
                            it.traverseDescendantsAndChangeColorToGreen()
                        }
                    }
                }
            ) {
                Text(text = "Run")
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { rootTraversableModifierNode?.resetColor() }
        ) {
            Text(text = "Reset Colors")
        }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 10.dp)
                    .border(width = 1.dp, color = Color.Black)
                    .traversableBackground(color = ROOT_TRAVERSABLE_DEFAULT_COLOR) {
                        rootTraversableModifierNode = this
                    },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(textAlign = TextAlign.Center, text = "Traversable Root")

            Spacer(Modifier.height(10.dp))

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .border(width = 1.dp, color = Color.Black)
                        .padding(10.dp)
                        .background(Color.Blue)
            ) {

                // Column A
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxSize()
                            .border(width = 1.dp, color = Color.Black)
                            .padding(10.dp)
                            .traversableBackground(color = COLUMN_TRAVERSABLE_DEFAULT_COLOR) {
                                columnATraversableModifierNode = this
                            },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        textAlign = TextAlign.Center,
                        text = "Traversable\nColumn A"
                    )

                    Box(
                        modifier =
                            Modifier.border(width = 1.dp, color = Color.Black)
                                .traversableBackground(color = BOX_TRAVERSABLE_DEFAULT_COLOR) {
                                    boxATraversableModifierNode = this
                                }
                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            textAlign = TextAlign.Center,
                            text = "Traversable\nBox A"
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier =
                            Modifier.border(width = 1.dp, color = Color.Black)
                                .background(BOX_NON_TRAVERSABLE_DEFAULT_COLOR)
                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            textAlign = TextAlign.Center,
                            text = "NON-Traversable\nBox B"
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier =
                            Modifier.border(width = 1.dp, color = Color.Black)
                                .traversableBackground(color = BOX_TRAVERSABLE_DEFAULT_COLOR) {
                                    boxCTraversableModifierNode = this
                                }
                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            textAlign = TextAlign.Center,
                            text = "Traversable\nBox C"
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Column B
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxSize()
                            .border(width = 1.dp, color = Color.Black)
                            .padding(10.dp)
                            .traversableBackground(color = COLUMN_TRAVERSABLE_DEFAULT_COLOR) {
                                columnBTraversableModifierNode = this
                            },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        textAlign = TextAlign.Center,
                        text = "Traversable\nColumn B"
                    )

                    Box(
                        modifier =
                            Modifier.border(width = 1.dp, color = Color.Black)
                                .background(BOX_NON_TRAVERSABLE_DEFAULT_COLOR)
                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            textAlign = TextAlign.Center,
                            text = "NON-Traversable\nBox D"
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier =
                            Modifier.border(width = 1.dp, color = Color.Black)
                                .traversableBackground(color = BOX_TRAVERSABLE_DEFAULT_COLOR) {
                                    boxETraversableModifierNode = this
                                }
                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            textAlign = TextAlign.Center,
                            text = "Traversable\nBox E"
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier =
                            Modifier.border(width = 1.dp, color = Color.Black)
                                .background(BOX_NON_TRAVERSABLE_DEFAULT_COLOR)
                    ) {
                        Text(
                            modifier = Modifier.padding(4.dp),
                            textAlign = TextAlign.Center,
                            text = "NON-Traversable\nBox F"
                        )
                    }
                }
            }
        }
    }
}

/*
 * Review of steps:
 * Step 1: Create a unique key for your TraversableNode.
 * Step 2: Create a custom Modifier.Node class that implements TraversableNode.
 * Step 3: Create a custom Modifier.Element class that uses the custom Modifier.Node.
 * Step 4: Create an extension function on Modifier that uses the Modifier.Element.
 * Step 5: Traverse the Modifier chain when you need to find your node(s)
 */

/*
 * Step 1: Create a unique key for your TraversableNode.
 */
// Key used to match my custom Modifier.Node that implements TraversableNode.
private const val TRAVERSAL_NODE_KEY =
    "androidx.compose.ui.demos.modifier.MY_UNIQUE_TRAVERSAL_NODE_KEY"

/**
 * Step 2: Create a custom Modifier.Node class that implements TraversableNode. TraversableNode is
 * the interface that lets the system know you want to be traversed. In this sample's case, we are
 * using the Modifier.background() Modifier.Node code but making it Traversable.
 *
 * We also pass a block as the last parameter, so we can get a reference to the Node. That enables,
 * us to make calls directly from it to search the Modifier chain from that Node. You probably won't
 * do this in practice, but instead get some triggers (like a PointerEvent or something) to execute
 * your traversable calls. To see real examples, look at the implementations of
 * PointerHoverIconModifierNode or DragAndDropNode.
 */
internal class TraversableBackgroundModifierNode(
    var color: Color,
    var brush: Brush?,
    var alpha: Float,
    var shape: Shape,
    // Only needed in this sample, so we can grab a reference to call traversable functions
    // directly, that is, you probably won't do this in your own code, it's only to demo.
    var block: (TraversableBackgroundModifierNode.() -> Unit)?
) : Modifier.Node(), TraversableNode, DrawModifierNode {

    override val traverseKey = TRAVERSAL_NODE_KEY

    private val originalColor: Color

    init {
        block?.let { it() }
        originalColor = color
    }

    // I thought it might be helpful to also show how you could use traverseChildren(). Remember,
    // it only goes to the children, but I'm calling it recursively here.
    fun resetColor() {
        // Reset color of calling node.
        color = originalColor
        invalidateDraw()

        // Reset color of children.
        traverseChildren(traverseKey) {
            if (it is TraversableBackgroundModifierNode) {
                it.resetColor()
            }
            true
        }
    }

    // Below are the two functions you want to look at for traversing the Modifier.Node tree for
    // your own nodes. In both, you will notice I don't change the color on the calling node, just
    // all matches after that. Also, I need to case the
    fun traverseAncestorsAndChangeColorToGreen() {
        traverseAncestors(traverseKey) {
            if (it is TraversableBackgroundModifierNode) {
                it.color = Color.Green
                it.invalidateDraw()
            }
            // Return true to continue searching the tree after a match. If you were looking to
            // match only some of the nodes, you could return false here and it would stop executing
            // the search.
            true
        }
    }

    fun traverseDescendantsAndChangeColorToGreen() {
        traverseDescendants(traverseKey) {
            if (it is TraversableBackgroundModifierNode) {
                it.color = Color.Green
                it.invalidateDraw()
            }

            // [traverseDescendants()] actually has three options:
            // - ContinueTraversal
            // - SkipSubtreeAndContinueTraversal - rarely used
            // - CancelTraversal
            // They are pretty self explanatory. Usually, you just want to continue or cancel the
            // search. In some rare cases, you might want to skip the subtree but continue searching
            // the tree. If that is something you are interested in, check out the Compose
            // Foundation's PointerHoverIconModifierNode implementation for an example.
            TraverseDescendantsAction.ContinueTraversal
        }
    }

    // All the rest of the code in this node is to enable the background color changing, you
    // can ignore it if you are only wanting to learn the traversable APIs.

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect()
        } else {
            drawOutline()
        }
        drawContent()
    }

    private fun ContentDrawScope.drawRect() {
        if (color != Color.Unspecified) drawRect(color = color)
        brush?.let { drawRect(brush = it, alpha = alpha) }
    }

    private fun ContentDrawScope.drawOutline() {
        val outline = shape.createOutline(size, layoutDirection, this)
        if (color != Color.Unspecified) drawOutline(outline, color = color)
        brush?.let { drawOutline(outline, brush = it, alpha = alpha) }
    }

    override fun toString() = "CustomTraversableModifierNode of key: $TRAVERSAL_NODE_KEY"
}

/**
 * Step 3: Create a custom Modifier.Element class that uses the custom Modifier.Node. If you ever
 * need to use a custom Modifier.Node, you need to do this. You can think of this like a Virtual
 * DOM.
 */
private data class DemoCustomTraversableModifierElement(
    private val color: Color = Color.Unspecified,
    private val brush: Brush? = null,
    private val alpha: Float,
    private val shape: Shape,
    val block: (TraversableBackgroundModifierNode.() -> Unit)?
) : ModifierNodeElement<TraversableBackgroundModifierNode>() {
    override fun create() = TraversableBackgroundModifierNode(color, brush, alpha, shape, block)

    override fun update(node: TraversableBackgroundModifierNode) {
        node.color = color
        node.brush = brush
        node.alpha = alpha
        node.shape = shape
        node.block = block
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "demoTraversableModifierNode"
        properties["color"] = color
        properties["brush"] = brush
        properties["alpha"] = alpha
        properties["shape"] = shape
        properties["block"] = block
    }
}

/** Step 4: Create an extension function on Modifier that uses the Modifier.Element. */
private fun Modifier.traversableBackground(
    color: Color,
    shape: Shape = RectangleShape,
    block: (TraversableBackgroundModifierNode.() -> Unit)? = null
): Modifier {
    val alpha = 1.0f // for solid colors

    return this then
        DemoCustomTraversableModifierElement(
            color = color,
            shape = shape,
            alpha = alpha,
            block = block
        )
}
