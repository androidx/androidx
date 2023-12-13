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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import kotlinx.cinterop.CValue
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRect
import platform.Foundation.NSNotFound
import platform.UIKit.accessibilityElements
import platform.UIKit.isAccessibilityElement
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.toCGRect
import androidx.compose.ui.uikit.utils.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityTraitAdjustable
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIAccessibilityTraitUpdatesFrequently
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UIView
import platform.UIKit.accessibilityCustomActions
import platform.darwin.NSInteger
import platform.darwin.NSObject

private val DUMMY_UI_ACCESSIBILITY_CONTAINER = NSObject()

/**
 * Represents a projection of the Compose semantics node to the iOS world. The actual accessibility
 * object communicated to iOS accessibility services is returned by [actualAccessibilityElement]
 * property.
 *
 * For default implementation, it's the same object as the [AccessibilityElement] itself, but it can
 * be overriden by subclasses to return a different object (like a dummy UIScrollView, UITextInput,
 * etc.)
 *
 * The object itself is a node in a generated tree that matches 1-to-1 with the [SemanticsNode]
 * tree. The actual tree that is communicated to iOS accessibility services is synthesized from it
 * lazily in [AccessibilityContainer] class.
 *
 * @param semanticsNode The semantics node with initial data that this element should represent
 *  (can be changed later via [updateWithNewSemanticsNode])
 * @param mediator The mediator that is associated with iOS accessibility tree where this element
 * resides.
 */
private class AccessibilityElement(
    private var semanticsNode: SemanticsNode,
    private val mediator: AccessibilityMediator,

    // The super call below is needed because this constructor is designated in the Obj-C class,
    // the real container will be resolved dynamically by [accessibilityContainer] and
    // [resolveAccessibilityContainer]
) : CMPAccessibilityElement(DUMMY_UI_ACCESSIBILITY_CONTAINER) {
    val semanticsNodeId: Int
        get() = semanticsNode.id

    val hasChildren: Boolean
        get() = children.isNotEmpty()

    val childrenCount: NSInteger
        get() = children.size.toLong()

    val actualAccessibilityElement: Any
        get() = this

    var parent: AccessibilityElement? = null
        private set

    private var children = mutableListOf<AccessibilityElement>()

    /**
     * Constructed lazily if :
     * - The element has children of its own
     * or
     * - The element is representing the root node
     */
    private val synthesizedAccessibilityContainer by lazy {
        AccessibilityContainer(
            wrappedElement = this,
            mediator = mediator
        )
    }

    init {
        update(null, semanticsNode)
    }

    fun childAtIndex(index: NSInteger): AccessibilityElement? {
        if (index < 0L || index >= childrenCount) {
            return null
        }

        return children[index.toInt()]
    }

    /**
     * Tries to match the given [element] with the actual hierarchy resolution callback from
     * iOS Accessibility services. If the element is found, returns its index in the children list.
     * Otherwise, returns null.
     */
    fun indexOfChildAccessibilityElement(element: Any): NSInteger? {
        for (index in 0 until children.size) {
            val child = children[index]

            // There are two exclusive cases here.
            // 1. The element is a container, and it's the same as the container of the child.
            // 2. The element is an actual accessibility element, and it's the same as one of the child.
            // The first case is true if the child has children itself, and hence [AccessibilityContainer] was communicated to iOS.
            // The second case is true if the child doesn't have children, and hence its [actualAccessibilityElement] was communicated to iOS.

            if (child.hasChildren) {
                // accessibilityContainerOfObject retrieves the container of the given element in
                // ObjC via dispatching `accessibilityContainer` to type erased `id` object.
                // In [AccessibilityElement] it will resolve to the synthesized container.
                // Inherited classes with custom [actualAccessibilityElement] can have their own resolution
                // path.
                if (element == accessibilityContainerOfObject(child.actualAccessibilityElement)) {
                    return index.toLong()
                }
            } else {
                if (element == child.actualAccessibilityElement) {
                    return index.toLong()
                }
            }
        }

        return null
    }

    override fun accessibilityActivate(): Boolean {
        if (!mediator.isAlive) {
            return false
        }

        val onClick = semanticsNode.config.getOrNull(SemanticsActions.OnClick) ?: return false
        val action = onClick.action ?: return false

        return action()
    }

    /**
     * This function is the final one called during the accessibility tree resolution for iOS services
     * and is invoked from underlying Obj-C library. If this node has children, then we return its
     * synthesized container, otherwise we look up the parent and return its container.
     */
    override fun resolveAccessibilityContainer(): Any? {
        if (!mediator.isAlive) {
            return null
        }

        return if (hasChildren || semanticsNodeId == mediator.rootSemanticsNodeId) {
            synthesizedAccessibilityContainer
        } else {
            parent?.accessibilityContainer
        }
    }

    /**
     * Compose doesn't communicate fine-grain changes in semantics tree, thus all changes in the particular
     * persistent object to match the latest resolved SemanticsNode should be done via full-scan of all properties
     * of previous and current SemanticsNode.
     *
     * TODO: is it possible to optimize this? Can we calculate the actual `UIAccessibility` properties
     *   lazily? How should we notify iOS about the changes in the properties without direct changes
     *   in `UIAccessibility` properties (are they KVO-observed by iOS? how can we trigger it?)
     */
    fun updateWithNewSemanticsNode(newSemanticsNode: SemanticsNode) {
        check(semanticsNode.id == newSemanticsNode.id)
        update(semanticsNode, newSemanticsNode)
        semanticsNode = newSemanticsNode
    }

    private fun update(oldNode: SemanticsNode? = null, newNode: SemanticsNode) {
        // TODO: check that the field for the properties that were present in the old node but not in
        //  the new one are cleared

        // If the node doesn't have any semantics that can be projected to iOS UIAccessibility entities, it will be invisible to accessibility services
        isAccessibilityElement = false

        var hasAnyMeaningfulSemantics = false

        fun onMeaningfulSemanticAdded() {
            hasAnyMeaningfulSemantics = true
        }

        val accessibilityLabelStrings = mutableListOf<String>()
        val accessibilityValueStrings = mutableListOf<String>()
        var accessibilityTraits = UIAccessibilityTraitNone

        fun addTrait(trait: UIAccessibilityTraits) {
            accessibilityTraits = accessibilityTraits or trait
        }

        fun <T> getValue(key: SemanticsPropertyKey<T>): T = semanticsNode.config[key]

        // Iterate through all semantic properties and map them to values that are expected by iOS Accessibility services for the node with given semantics
        semanticsNode.config.forEach { pair ->
            when (val key = pair.key) {
                // == Properties ==

                SemanticsProperties.InvisibleToUser -> {
                    // Return immediately. Function won't reach a point where [isAccessibilityElement] is set to true
                    return
                }

                SemanticsProperties.LiveRegion -> {
                    onMeaningfulSemanticAdded()
                    addTrait(UIAccessibilityTraitUpdatesFrequently)
                }

                SemanticsProperties.ContentDescription -> {
                    accessibilityLabelStrings.addAll(getValue(key))
                }

                SemanticsProperties.Text -> {
                    accessibilityLabelStrings.addAll(getValue(key).map { it.text })
                }

                SemanticsProperties.PaneTitle -> {
                    accessibilityLabelStrings.add(getValue(key))
                }

                SemanticsProperties.Disabled -> {
                    addTrait(UIAccessibilityTraitNotEnabled)
                }

                SemanticsProperties.Heading -> {
                    onMeaningfulSemanticAdded()
                    addTrait(UIAccessibilityTraitHeader)
                }

                SemanticsProperties.StateDescription -> {
                    val state = getValue(key)
                    accessibilityValueStrings.add(state)
                }

                SemanticsProperties.ToggleableState -> {
                    val state = getValue(key)

                    when (state) {
                        ToggleableState.On -> {
                            addTrait(UIAccessibilityTraitSelected)
                            accessibilityValueStrings.add("On")
                        }

                        ToggleableState.Off -> {
                            accessibilityValueStrings.add("Off")
                        }

                        ToggleableState.Indeterminate -> {
                            accessibilityValueStrings.add("Indeterminate")
                        }
                    }
                }

                SemanticsProperties.Role -> {
                    val role = getValue(key)

                    when (role) {
                        Role.Button, Role.RadioButton, Role.Checkbox, Role.Switch -> {
                            onMeaningfulSemanticAdded()
                            addTrait(UIAccessibilityTraitButton)
                        }

                        Role.DropdownList -> {
                            onMeaningfulSemanticAdded()
                            addTrait(UIAccessibilityTraitAdjustable)
                        }

                        Role.Image -> {
                            onMeaningfulSemanticAdded()
                            addTrait(UIAccessibilityTraitImage)
                        }
                    }
                }

                // == Actions ==

                SemanticsActions.OnClick -> {
                    onMeaningfulSemanticAdded()
                }

                SemanticsActions.CustomActions -> {
                    onMeaningfulSemanticAdded()

                    val actions = getValue(key)
                    accessibilityCustomActions = actions.map {
                        UIAccessibilityCustomAction(
                            name = it.label,
                            actionHandler = { _ ->
                                it.action.invoke()
                            }
                        )
                    }
                }
            }
        }

        if (accessibilityLabelStrings.isNotEmpty()) {
            onMeaningfulSemanticAdded()
            accessibilityLabel = accessibilityLabelStrings.joinToString("\n") { it }
        }

        if (accessibilityValueStrings.isNotEmpty()) {
            onMeaningfulSemanticAdded()
            accessibilityValue = accessibilityLabelStrings.joinToString("\n") { it }
        }

        this.accessibilityTraits = accessibilityTraits
        isAccessibilityElement = hasAnyMeaningfulSemantics

        accessibilityIdentifier = "${semanticsNode.id}"
        accessibilityFrame = mediator.convertRectToWindowSpaceCGRect(semanticsNode.boundsInWindow)
    }

    private fun removeFromParent() {
        val parent = parent ?: return

        val removed = parent.children.remove(this)
        check(removed) {
            "Corrupted tree. Can't remove child from parent, because it's not present in the parent's children list"
        }

        this.parent = null
    }

    fun removeAllChildren() {
        for (child in children) {
            child.parent = null
        }

        children.clear()
    }

    fun addChild(element: AccessibilityElement) {
        // If child was moved from another parent, remove it from there first
        // Perhaps this is excessive, but I can't prove, that situation where an
        // [AccessibilityElement] is contained in multiple parents is impossible, and that it won't
        // lead to issues
        element.removeFromParent()

        children.add(element)
        element.parent = this@AccessibilityElement
    }

    fun debugPrint(depth: Int) {
        val indent = " ".repeat(depth * 2)

        val container = resolveAccessibilityContainer() as AccessibilityContainer
        val indexOfSelf = container.indexOfAccessibilityElement(this)

        check(indexOfSelf != NSNotFound)
        check(container.accessibilityElementAtIndex(indexOfSelf) == this.actualAccessibilityElement)

        println("${indent}AccessibilityElement_$semanticsNodeId")
        println("$indent  containmentChain: ${debugContainmentChain(this)}")
        println("$indent  isAccessibilityElement: $isAccessibilityElement")
        println("$indent  accessibilityLabel: $accessibilityLabel")
        println("$indent  accessibilityValue: $accessibilityValue")
        println("$indent  accessibilityTraits: $accessibilityTraits")
        println("$indent  accessibilityFrame: ${NSStringFromCGRect(accessibilityFrame)}")
        println("$indent  accessibilityIdentifier: $accessibilityIdentifier")
        println("$indent  accessibilityCustomActions: $accessibilityCustomActions")
    }
}

/**
 * UIAccessibilityElement can't be a container and an element at the same time.
 * If [isAccessibilityElement] is true, iOS accessibility services won't access the object
 * UIAccessibilityContainer methods.
 * Thus, semantics tree like
 * ```
 * SemanticsNode_A
 *     SemanticsNode_B
 *         SemanticsNode_C
 * ```
 * Is expected by iOS Accessibility services to be represented as:
 * ```
 * AccessibilityContainer_A
 *     AccessibilityElement_A -> AccessibilityElement
 *     AccessibilityContainer_B
 *         AccessibilityElement_B -> AccessibilityElement
 *         AccessibilityElement_C -> AccessibilityElement
 * ```
 *
 * ` -> ` above designates [AccessibilityElement.actualAccessibilityElement] property,
 * [AccessibilityElement] default implementation returns itself. However, it can be overriden
 * in subclasses to return a different object (like a dummy UIScrollView, UITextInput, etc.)
 *
 * The actual internal representation of the tree is:
 * ```
 * AccessibilityElement_A
 *   AccessibilityElement_B
 *      AccessibilityElement_C
 * ```
 * But the object we put into the accessibility root set is the synthesized [AccessibilityContainer]
 * for AccessibilityElement_A. The methods that are be called from iOS Accessibility services will
 * lazily resolve the hierarchy from the internal one to expected.
 *
 * This is needed, because the actual [SemanticsNode]s can be inserted and removed dynamically, so building
 * the whole container hierarchy in advance and maintaining it proactively will make the code even more
 * hard to follow than it is now.
 *
 * This implementation is inspired by Flutter's
 * https://github.com/flutter/engine/blob/main/shell/platform/darwin/ios/framework/Source/SemanticsObject.h
 *
 */
private class AccessibilityContainer(
    /**
     * The element wrapped by this container
     */
    private val wrappedElement: AccessibilityElement,
    private val mediator: AccessibilityMediator,

    // The super call below is needed because this constructor is designated in the Obj-C class,
    // the real parent container will be resolved dynamically by [accessibilityContainer]
) : CMPAccessibilityContainer(DUMMY_UI_ACCESSIBILITY_CONTAINER) {
    val semanticsNodeId: Int
        get() = wrappedElement.semanticsNodeId

    /**
     * This function will be called by iOS Accessibility services to traverse the hierarchy of all
     * accessibility elements starting with the root one.
     *
     * The zero element is always the element wrapped by this container due to the restriction of
     * an object not being able to be a container and an element at the same time.
     *
     * Consequent elements correspond to the children of the [wrappedElement]:
     * - The [actualAccessibilityElement] if the child is a leaf node.
     * - The synthesized [CMPAccessibilityContainer] otherwise
     */
    override fun accessibilityElementAtIndex(index: NSInteger): Any? {
        if (index == 0L) {
            return wrappedElement.actualAccessibilityElement
        }

        val child = wrappedElement.childAtIndex(index - 1) ?: return null

        if (child.hasChildren) {
            return child.accessibilityContainer
        }

        return child.actualAccessibilityElement
    }

    override fun accessibilityFrame(): CValue<CGRect> {
        // Same as wrapped element
        //
        // Accessibility container clips reacheability of its children, if their frame doesn't interesect
        // with their container's frame
        return wrappedElement.accessibilityFrame
    }

    /**
     * The number of elements in the container:
     * The wrapped element itself + the number of children
     */
    override fun accessibilityElementCount(): NSInteger = (wrappedElement.childrenCount + 1)

    /**
     * Reverse lookup of [accessibilityElementAtIndex]
     */
    override fun indexOfAccessibilityElement(element: Any): NSInteger {
        if (element == wrappedElement.actualAccessibilityElement) {
            return 0
        }

        return wrappedElement.indexOfChildAccessibilityElement(element)?.let { index ->
            index + 1
        } ?: NSNotFound
    }

    override fun accessibilityContainer(): Any? {
        if (!mediator.isAlive) {
            return null
        }

        return if (semanticsNodeId == mediator.rootSemanticsNodeId) {
            mediator.view
        } else {
            wrappedElement.parent?.accessibilityContainer
        }
    }

    fun debugPrint(depth: Int) {
        val indent = " ".repeat(depth * 2)
        println("${indent}AccessibilityContainer_${semanticsNodeId}")
    }
}

/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
internal class AccessibilityMediator(
    val view: UIView,
    private val owner: SemanticsOwner,
    coroutineContext: CoroutineContext
) {
    var isAlive = true

    val rootSemanticsNodeId: Int
        get() = owner.rootSemanticsNode.id

    /**
     * A value of true indicates that the Compose accessible tree is dirty, meaning that compose
     * semantics tree was modified since last sync, false otherwise.
     */
    private var isCurrentComposeAccessibleTreeDirty = false

    /**
     * Job to cancel tree syncing when the mediator is disposed.
     */
    private val job = Job()

    /**
     * CoroutineScope to launch the tree syncing job on.
     */
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    /**
     * A map of all [SemanticsNode.id] currently present in the tree to corresponding
     * [AccessibilityElement].
     */
    private val accessibilityElementsMap = mutableMapOf<Int, AccessibilityElement>()

    init {
        // TODO: this approach was copied from desktop implementation, obviously it has a 100ms lag
        //  between the actual change in the semantics tree and the change in the accessibility tree.
        //  should we use some other approach?
        coroutineScope.launch {
            while (isAlive) {
                syncNodes()
                delay(100)
            }
        }
    }

    fun onSemanticsChange() {
        isCurrentComposeAccessibleTreeDirty = true
    }

    fun convertRectToWindowSpaceCGRect(rect: Rect): CValue<CGRect> {
        val window = view.window ?: return CGRectMake(0.0, 0.0, 0.0, 0.0)

        val localSpaceCGRect = rect.toCGRect(window.screen.scale)
        return window.convertRect(localSpaceCGRect, fromView = view)
    }

    fun dispose() {
        check(isAlive) { "AccessibilityMediator is already disposed" }

        job.cancel()
        isAlive = false
        view.accessibilityElements = null
    }

    private fun createOrUpdateAccessibilityElementForSemanticsNode(node: SemanticsNode): AccessibilityElement {
        val element = accessibilityElementsMap[node.id]

        if (element != null) {
            element.updateWithNewSemanticsNode(node)
            return element
        }

        val newElement = AccessibilityElement(
            semanticsNode = node,
            mediator = this
        )

        accessibilityElementsMap[node.id] = newElement

        return newElement
    }

    /**
     * Traverses semantics tree starting from rootNode and returns an accessibility object which will
     * be put into iOS view's [accessibilityElements] property.
     *
     * Inserts new elements to [accessibilityElementsMap], updates the old ones, and removes the elements
     * that are not present in the tree anymore.
     */
    private fun traverseSemanticsTree(rootNode: SemanticsNode): Any {
        // TODO: should we move [presentIds] to the class scope to avoid reallocation?
        val presentIds = mutableSetOf<Int>()

        fun traverseSemanticsNode(node: SemanticsNode): AccessibilityElement {
            presentIds.add(node.id)
            val element = createOrUpdateAccessibilityElementForSemanticsNode(node)

            element.removeAllChildren()
            val childSemanticsNodesInAccessibilityOrder = node
                .replacedChildren
                .sortedByAccesibilityOrder()

            for (childNode in childSemanticsNodesInAccessibilityOrder) {
                val childElement = traverseSemanticsNode(childNode)
                element.addChild(childElement)
            }

            return element
        }

        val rootAccessibilityElement = traverseSemanticsNode(rootNode)

        // Filter out [AccessibilityElement] in [accessibilityElementsMap] that are not present in the tree anymore
        accessibilityElementsMap.keys.retainAll {
            it in presentIds
        }

        return checkNotNull(rootAccessibilityElement.resolveAccessibilityContainer()) {
            "Root element must always have an enclosing container"
        }
    }

    /**
     * Syncs the accessibility tree with the current semantics tree.
     * TODO: Does a full tree traversal on every sync. Explore new Google solution in 1.6, that should
     *   perform affected subtree traversal instead.
     */
    private fun syncNodes() {
        // TODO: investigate what happens if the user has an accessibility focus on the element that
        //  is removed from the tree:
        //  - Does it use the index path of containers traversal to restore the focus?
        //  - Does it use the accessibility identifier of the element to restore the focus?
        //  - Does it hold the reference to the focused element? Should we
        //      take some action to reset focus on the element, that got deleted in such case?

        // TODO: investigate what needs to be done to reflect that this hiearchy is probably covered
        //   by overlay/popup/dialogue

        val rooSemanticstNode = owner.rootSemanticsNode

        if (!rooSemanticstNode.layoutNode.isPlaced) {
            return
        }

        if (!isCurrentComposeAccessibleTreeDirty) {
            return
        }

        println("syncNodes")
        isCurrentComposeAccessibleTreeDirty = false

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        view.accessibilityElements = listOf(
            traverseSemanticsTree(rooSemanticstNode)
        )

        debugTraverse(view)
    }
}

/**
 * Traverse the accessibility tree starting from [accessibilityObject] using the same(assumed) logic
 * as iOS Accessibility services, and prints it debug data.
 */
private fun debugTraverse(accessibilityObject: Any, depth: Int = 0) {
    val indent = " ".repeat(depth * 2)

    when (accessibilityObject) {
        is UIView -> {
            println("${indent}View")

            accessibilityObject.accessibilityElements?.let { elements ->
                for (element in elements) {
                    element?.let {
                        debugTraverse(element, depth + 1)
                    }
                }
            }
        }

        is AccessibilityElement -> {
            accessibilityObject.debugPrint(depth)
        }

        is AccessibilityContainer -> {
            accessibilityObject.debugPrint(depth)

            val count = accessibilityObject.accessibilityElementCount()
            for (index in 0 until count) {
                val element = accessibilityObject.accessibilityElementAtIndex(index)
                element?.let {
                    debugTraverse(element, depth + 1)
                }
            }
        }

        else -> {
            throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
        }
    }
}

private fun debugContainmentChain(accessibilityObject: Any): String {
    val strings = mutableListOf<String>()

    var currentObject = accessibilityObject as? Any

    while (currentObject != null) {
        when (val constCurrentObject = currentObject) {
            is AccessibilityElement -> {
                currentObject = constCurrentObject.resolveAccessibilityContainer()
            }

            is UIView -> {
                strings.add("View")
                currentObject = null
            }

            is AccessibilityContainer -> {
                strings.add("AccessibilityContainer_${constCurrentObject.semanticsNodeId}")
                currentObject = constCurrentObject.accessibilityContainer()
            }

            else -> {
                throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
            }
        }
    }

    return strings.joinToString(" -> ")
}

/**
 * Sort the elements in their visual order using their bounds:
 * - from top to bottom,
 * - from left to right // TODO: consider RTL layout
 *
 * The sort is needed because [SemanticsNode.replacedChildren] order doesn't match the
 * expected order of the children in the accessibility tree.
 *
 * TODO: investigate if it's a bug, or some assumptions about the order are wrong.
 */
private fun List<SemanticsNode>.sortedByAccesibilityOrder(): List<SemanticsNode> {
    return sortedWith { lhs, rhs ->
        val result = lhs.boundsInWindow.topLeft.y.compareTo(rhs.boundsInWindow.topLeft.y)

        if (result == 0) {
            lhs.boundsInWindow.topLeft.x.compareTo(rhs.boundsInWindow.topLeft.x)
        } else {
            result
        }
    }
}