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

import androidx.compose.runtime.ExperimentalComposeApi
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
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.toCGRect
import androidx.compose.ui.uikit.utils.*
import androidx.compose.ui.unit.toSize
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTime
import kotlinx.cinterop.ExportObjCClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityScrollDirectionDown
import platform.UIKit.UIAccessibilityScrollDirectionLeft
import platform.UIKit.UIAccessibilityScrollDirectionRight
import platform.UIKit.UIAccessibilityScrollDirectionUp
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

// TODO: Impl for UIKit interop views

/**
 * An interface for logging accessibility debug messages.
 */
@ExperimentalComposeApi
interface AccessibilityDebugLogger {
    /**
     * Logs the given [message].
     */
    fun log(message: Any?)
}


/**
 * Represents a projection of the Compose semantics node to the iOS world.
 *
 * The object itself is a node in a generated tree that matches 1-to-1 with the [SemanticsNode]
 * tree. The actual tree that is communicated to iOS accessibility services is synthesized from it
 * lazily in [AccessibilityContainer] class.
 *
 * @param semanticsNode The semantics node with initial data that this element should represent
 *  (can be changed later via [updateWithNewSemanticsNode])
 * @param mediator The mediator that is associated with iOS accessibility tree where this element
 * resides.
 *
 */
@OptIn(ExperimentalComposeApi::class)
@ExportObjCClass
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

    var parent: AccessibilityElement? = null
        private set

    /**
     * Indicates whether this element is still present in the tree.
     */
    var isAlive = true
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

    /**
     * Returns accessibility element communicated to iOS Accessibility services for the given [index].
     * Takes a child at [index].
     * If the child has its own children, then the element at the given index is the synthesized container
     * for the child. Otherwise, the element at the given index is the child itself.
     */
    fun childAccessibilityElementAtIndex(index: NSInteger): Any? {
        val i = index.toInt()

        return if (i in children.indices) {
            val child = children[i]

            if (child.hasChildren) {
                child.accessibilityContainer
            } else {
                child
            }
        } else {
            null
        }
    }

    /**
     * Reverse of [childAccessibilityElementAtIndex]
     * Tries to match the given [element] with the actual hierarchy resolution callback from
     * iOS Accessibility services. If the element is found, returns its index in the children list.
     * Otherwise, returns null.
     */
    fun indexOfChildAccessibilityElement(element: Any): NSInteger? {
        for (index in 0 until children.size) {
            val child = children[index]

            if (child.hasChildren) {
                if (element == child.accessibilityContainer) {
                    return index.toLong()
                }
            } else {
                if (element == child) {
                    return index.toLong()
                }
            }
        }

        return null
    }

    fun dispose() {
        check(isAlive) {
            "AccessibilityElement is already disposed"
        }

        isAlive = false
    }

    override fun accessibilityLabel(): String? {
        val config = semanticsNode.config

        val contentDescription = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString("\n")
        if (contentDescription != null) {
            return contentDescription
        }

        val editableText = config.getOrNull(SemanticsProperties.EditableText)?.text

        if (editableText != null) {
            return editableText
        }

        val text =
            config.getOrNull(SemanticsProperties.Text)?.joinToString("\n") { it.text }

        return text
    }

    override fun accessibilityActivate(): Boolean {
        if (!isAlive || !semanticsNode.isValid) {
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
        if (!isAlive) {
            mediator.debugLogger?.log("resolveAccessibilityContainer failed because $semanticsNodeId was removed from the tree")
            return null
        }

        return if (hasChildren || semanticsNodeId == mediator.rootSemanticsNodeId) {
            synthesizedAccessibilityContainer
        } else {
            parent?.accessibilityContainer
        }
    }

    override fun accessibilityElementDidBecomeFocused() {
        super.accessibilityElementDidBecomeFocused()

        mediator.debugLogger?.log(
            listOf(
                null,
                "Focused on:",
                semanticsNode.config
            )
        )

        if (!isAlive) {
            return
        }

        scrollToIfPossible()
    }

    /**
     * Try to perform a scroll on any ancestor of this element if the element is not fully visible.
     */
    private fun scrollToIfPossible() {
        // TODO: extremely clunky and unreliable, temporarily disabled
        return

        val scrollableAncestor = semanticsNode.scrollableByAncestor ?: return
        val scrollableAncestorRect = scrollableAncestor.boundsInWindow

        val unclippedRect = semanticsNode.unclippedBoundsInWindow

        mediator.debugLogger?.log(listOf(
            "scrollableAncestorRect: $scrollableAncestorRect",
            "unclippedRect: $unclippedRect"
        ))

        // TODO: consider safe areas?
        // TODO: is RTL working properly?
        if (unclippedRect.top < scrollableAncestorRect.top) {
            // The element is above the screen, scroll up
            parent?.scrollByIfPossible(0f, unclippedRect.top - scrollableAncestorRect.top)
            return
        } else if (unclippedRect.bottom > scrollableAncestorRect.bottom) {
            // The element is below the screen, scroll down
            parent?.scrollByIfPossible(0f, unclippedRect.bottom - scrollableAncestorRect.bottom)
            return
        } else if (unclippedRect.left < scrollableAncestorRect.left) {
            // The element is to the left of the screen, scroll left
            parent?.scrollByIfPossible(unclippedRect.left - scrollableAncestorRect.left, 0f)
            return
        } else if (unclippedRect.right > scrollableAncestorRect.right) {
            // The element is to the right of the screen, scroll right
            parent?.scrollByIfPossible(unclippedRect.right - scrollableAncestorRect.right, 0f)
            return
        }
    }

    private fun scrollByIfPossible(dx: Float, dy: Float) {
        if (!isAlive) {
            return
        }

        // if has scrollBy action, invoke it, otherwise try to scroll the parent
        val action = semanticsNode.config.getOrNull(SemanticsActions.ScrollBy)?.action

        if (action != null) {
            action(dx, dy)
        } else {
            parent?.scrollByIfPossible(dx, dy)
        }
    }

    private fun scrollIfPossible(direction: UIAccessibilityScrollDirection, config: SemanticsConfiguration): Boolean {
        val (width, height) = semanticsNode.size

        // TODO: reverse engineer proper dimension scale
        val dimensionScale = 0.5f

        // TODO: post notification about the scroll
        when (direction) {
            UIAccessibilityScrollDirectionUp -> {
                var result = config.getOrNull(SemanticsActions.PageUp)?.action?.invoke()

                if (result != null) {
                    return result
                }

                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    0F,
                    -height.toFloat() * dimensionScale
                )

                if (result != null) {
                    return result
                }
            }

            UIAccessibilityScrollDirectionDown -> {
                var result = config.getOrNull(SemanticsActions.PageDown)?.action?.invoke()

                if (result != null) {
                    return result
                }

                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    0f,
                    height.toFloat() * dimensionScale
                )

                if (result != null) {
                    return result
                }
            }

            UIAccessibilityScrollDirectionLeft -> {
                var result = config.getOrNull(SemanticsActions.PageLeft)?.action?.invoke()

                if (result != null) {
                    return result
                }

                // TODO: check RTL support
                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    -width.toFloat() * dimensionScale,
                    0f,
                )

                if (result != null) {
                    return result
                }
            }

            UIAccessibilityScrollDirectionRight -> {
                var result = config.getOrNull(SemanticsActions.PageRight)?.action?.invoke()

                if (result != null) {
                    return result
                }

                // TODO: check RTL support
                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    width.toFloat() * dimensionScale,
                    0f,
                )

                if (result != null) {
                    return result
                }
            }

            else -> {
                // TODO: UIAccessibilityScrollDirectionPrevious, UIAccessibilityScrollDirectionNext
            }
        }

        parent?.let {
            return it.scrollIfPossible(direction, it.semanticsNode.config)
        }

        return false
    }

    override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
        if (!isAlive) {
            return false
        }

        return scrollIfPossible(direction, semanticsNode.config)
    }

    override fun isAccessibilityElement(): Boolean {
        val config = semanticsNode.config

        if (config.contains(SemanticsProperties.InvisibleToUser)) {
            return false
        }

        // TODO: investigate if it can it be a traversal group _and_ contain properties that should
        //  be communicated to iOS?
        if (config.getOrNull(SemanticsProperties.IsTraversalGroup) == true) {
            return false
        }

        return config.containsImportantForAccessibility()
    }

    override fun accessibilityIdentifier(): String {
        return semanticsNode.config.getOrNull(SemanticsProperties.TestTag)
            ?: "AccessibilityElement for SemanticsNode(id=$semanticsNodeId)"
    }

    override fun accessibilityHint(): String? {
        return semanticsNode.config.getOrNull(SemanticsActions.OnClick)?.label
    }

    override fun accessibilityCustomActions(): List<UIAccessibilityCustomAction> {
        return semanticsNode.config.getOrNull(SemanticsActions.CustomActions)?.let { actions ->
            actions.map {
                UIAccessibilityCustomAction(
                    name = it.label,
                    actionHandler = { _ ->
                        it.action.invoke()
                    }
                )
            }
        } ?: emptyList()
    }

    override fun accessibilityTraits(): UIAccessibilityTraits {
        var result = UIAccessibilityTraitNone

        val config = semanticsNode.config

        if (config.contains(SemanticsProperties.LiveRegion)) {
            result = result or UIAccessibilityTraitUpdatesFrequently
        }

        if (config.contains(SemanticsProperties.Disabled)) {
            result = result or UIAccessibilityTraitNotEnabled
        }

        if (config.contains(SemanticsProperties.Heading)) {
            result = result or UIAccessibilityTraitHeader
        }

        config.getOrNull(SemanticsProperties.ToggleableState)?.let { state ->
            when (state) {
                ToggleableState.On -> {
                    result = result or UIAccessibilityTraitSelected
                }

                ToggleableState.Off, ToggleableState.Indeterminate -> {
                    // Do nothing
                }
            }
        }

        config.getOrNull(SemanticsActions.OnClick)?.let {
            result = result or UIAccessibilityTraitButton
        }

        config.getOrNull(SemanticsProperties.Role)?.let { role ->
            when (role) {
                Role.Button, Role.RadioButton, Role.Checkbox, Role.Switch -> {
                    result = result or UIAccessibilityTraitButton
                }

                Role.DropdownList -> {
                    result = result or UIAccessibilityTraitAdjustable
                }

                Role.Image -> {
                    result = result or UIAccessibilityTraitImage
                }
            }
        }

        return result
    }


    override fun accessibilityValue(): String? {
        return semanticsNode.config.getOrNull(SemanticsProperties.StateDescription)
    }

    override fun accessibilityFrame(): CValue<CGRect> {
        return mediator.convertRectToWindowSpaceCGRect(semanticsNode.boundsInWindow)
    }

    // TODO: check the reference/value semantics for SemanticsNode, perhaps it doesn't need
    //  recreation at all cache config to avoid traversing subtree for merging semantics and
    //  excessive copy on [semanticsNode.config] call
    fun updateWithNewSemanticsNode(newSemanticsNode: SemanticsNode) {
        check(semanticsNode.id == newSemanticsNode.id)
        // TODO: track that SemanticsProperties.LiveRegion is present and conditionally start
        //  proactively comparing the the accessibilityLabel and accessibilityValue properties
        //  to post notifications about the changes.
        semanticsNode = newSemanticsNode
    }

    private fun removeFromParent() {
        val parent = parent ?: return

        val removed = parent.children.remove(this)
        check(removed) {
            "Corrupted tree. Can't remove the child from the parent, because it's not present in the parent's children list"
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

    fun debugLog(logger: AccessibilityDebugLogger, depth: Int) {
        val indent = " ".repeat(depth * 2)

        val container = resolveAccessibilityContainer() as AccessibilityContainer
        val indexOfSelf = container.indexOfAccessibilityElement(this)

        check(indexOfSelf != NSNotFound)
        check(container.accessibilityElementAtIndex(indexOfSelf) == this)

        logger.log("${indent}AccessibilityElement_$semanticsNodeId")
        logger.log("$indent  containmentChain: ${debugContainmentChain(this)}")
        logger.log("$indent  isAccessibilityElement: $isAccessibilityElement")
        logger.log("$indent  accessibilityLabel: $accessibilityLabel")
        logger.log("$indent  accessibilityValue: $accessibilityValue")
        logger.log("$indent  accessibilityTraits: $accessibilityTraits")
        logger.log("$indent  accessibilityFrame: ${NSStringFromCGRect(accessibilityFrame)}")
        logger.log("$indent  accessibilityIdentifier: $accessibilityIdentifier")
        logger.log("$indent  accessibilityCustomActions: $accessibilityCustomActions")
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
 *     AccessibilityElement_A
 *     AccessibilityContainer_B
 *         AccessibilityElement_B
 *         AccessibilityElement_C
 * ```
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
@OptIn(ExperimentalComposeApi::class)
@ExportObjCClass
private class AccessibilityContainer(
    /**
     * The element wrapped by this container
     */
    private val wrappedElement: AccessibilityElement,
    private val mediator: AccessibilityMediator,

    // The super call below is needed because this constructor is designated in the Obj-C class,
    // the real parent container will be resolved dynamically by [accessibilityContainer]
) : CMPAccessibilityContainer(DUMMY_UI_ACCESSIBILITY_CONTAINER) {
    val semanticsNodeId by wrappedElement::semanticsNodeId
    private val isAlive by wrappedElement::isAlive

    /**
     * This function will be called by iOS Accessibility services to traverse the hierarchy of all
     * accessibility elements starting with the root one.
     *
     * The zero element is always the element wrapped by this container due to the restriction of
     * an object not being able to be a container and an element at the same time.
     */
    override fun accessibilityElementAtIndex(index: NSInteger): Any? {
        if (!isAlive) {
            mediator.debugLogger?.log("accessibilityElementAtIndex(NSInteger) called after $semanticsNodeId was removed from the tree")
            return null
        }

        if (index == 0L) {
            return wrappedElement
        }

        return wrappedElement.childAccessibilityElementAtIndex(index - 1)
    }

    override fun accessibilityFrame(): CValue<CGRect> {
        if (!isAlive) {
            return CGRectMake(0.0, 0.0, 0.0, 0.0)
        }

        // Same as wrapped element
        // iOS makes children of a container unreachable, if their frame is outside of
        // the container's frame
        return wrappedElement.accessibilityFrame
    }

    /**
     * The number of elements in the container:
     * The wrapped element itself + the number of children
     */
    override fun accessibilityElementCount(): NSInteger {
        if (!isAlive) {
            mediator.debugLogger?.log("accessibilityElementCount() called after $semanticsNodeId was removed from the tree")
            return 0
        }

        return wrappedElement.childrenCount + 1
    }

    /**
     * Reverse lookup of [accessibilityElementAtIndex]
     */
    override fun indexOfAccessibilityElement(element: Any): NSInteger {
        if (!isAlive) {
            mediator.debugLogger?.log("indexOfAccessibilityElement(Any) called after $semanticsNodeId was removed from the tree")
            return NSNotFound
        }

        if (element == wrappedElement) {
            return 0
        }

        return wrappedElement.indexOfChildAccessibilityElement(element)?.let { index ->
            index + 1
        } ?: NSNotFound
    }

    override fun accessibilityContainer(): Any? {
        if (!isAlive) {
            mediator.debugLogger?.log("accessibilityContainer() called after $semanticsNodeId was removed from the tree")
            return null
        }

        return if (semanticsNodeId == mediator.rootSemanticsNodeId) {
            mediator.view
        } else {
            wrappedElement.parent?.accessibilityContainer
        }
    }

    fun debugLog(logger: AccessibilityDebugLogger, depth: Int) {
        val indent = " ".repeat(depth * 2)
        logger.log("${indent}AccessibilityContainer_${semanticsNodeId}")
    }
}

private sealed interface NodesSyncResult {
    object NoChanges : NodesSyncResult
    data class Success(val newElementToFocus: Any?) : NodesSyncResult
}

/**
 * A sealed class that represents the options for syncing the Compose SemanticsNode tree with the iOS UIAccessibility tree.
 */
@ExperimentalComposeApi
sealed class AccessibilitySyncOptions(
    internal val debugLogger: AccessibilityDebugLogger?
) {
    /**
     * Never sync the tree.
     */
    data object Never: AccessibilitySyncOptions(debugLogger = null)

    /**
     * Sync the tree only when the accessibility services are running.
     *
     * @param debugLogger Optional [AccessibilityDebugLogger] to log into the info about the
     * accessibility tree syncing and interactions.
     */
    class WhenRequiredByAccessibilityServices(debugLogger: AccessibilityDebugLogger?): AccessibilitySyncOptions(debugLogger)

    /**
     * Always sync the tree, can be quite handy for debugging and testing.
     * Be aware that there is a significant overhead associated with doing it that can degrade
     * the visual performance of the app.
     *
     * @param debugLogger Optional [AccessibilityDebugLogger] to log into the info about the
     * accessibility tree syncing and interactions.
     */
    class Always(debugLogger: AccessibilityDebugLogger?): AccessibilitySyncOptions(debugLogger = debugLogger)
}

/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
@OptIn(ExperimentalComposeApi::class)
internal class AccessibilityMediator constructor(
    val view: UIView,
    private val owner: SemanticsOwner,
    coroutineContext: CoroutineContext,
    private val getAccessibilitySyncOptions: () -> AccessibilitySyncOptions,
) {
    private var isAlive = true

    private var latestSyncOptions: AccessibilitySyncOptions = getAccessibilitySyncOptions()

    val debugLogger: AccessibilityDebugLogger?
        get() = latestSyncOptions.debugLogger

    var rootSemanticsNodeId: Int = -1

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
        val updateIntervalMillis = 50L
        // TODO: this approach was copied from desktop implementation, obviously it has a [updateIntervalMillis] lag
        //  between the actual change in the semantics tree and the change in the accessibility tree.
        //  should we use some other approach?
        coroutineScope.launch {
            while (isAlive) {
                var result: NodesSyncResult

                latestSyncOptions = getAccessibilitySyncOptions()

                val shouldPerformSync = when (latestSyncOptions) {
                    is AccessibilitySyncOptions.Never -> false
                    is AccessibilitySyncOptions.WhenRequiredByAccessibilityServices -> {
                        UIAccessibilityIsVoiceOverRunning()
                    }
                    is AccessibilitySyncOptions.Always -> true
                }

                if (shouldPerformSync) {
                    val time = measureTime {
                        result = sync()
                    }

                    when (val immutableResult = result) {
                        is NodesSyncResult.NoChanges -> {
                            // Do nothing
                        }

                        is NodesSyncResult.Success -> {
                            debugLogger?.log("AccessibilityMediator.sync took $time")
                            UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, immutableResult.newElementToFocus)
                        }
                    }
                }

                delay(updateIntervalMillis)
            }
        }
    }

    fun onSemanticsChange() {
        debugLogger?.log("onSemanticsChange")

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

        for (element in accessibilityElementsMap.values) {
            element.dispose()
        }
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
                .filter {
                    it.isValid
                }
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
            val isPresent = it in presentIds

            if (!isPresent) {
                debugLogger?.log("$it removed")
                checkNotNull(accessibilityElementsMap[it]).dispose()
            }

            isPresent
        }

        return checkNotNull(rootAccessibilityElement.resolveAccessibilityContainer()) {
            "Root element must always have an enclosing container"
        }
    }

    /**
     * Syncs the accessibility tree with the current semantics tree.
     * TODO: Does a full tree traversal on every sync, expect changes from Google, they are also aware
     *  of the issue and associated performance overhead.
     */
    private fun sync(): NodesSyncResult {
        // TODO: investigate what needs to be done to reflect that this hiearchy is probably covered
        //   by sibling overlay

        if (!isCurrentComposeAccessibleTreeDirty) {
            return NodesSyncResult.NoChanges
        }

        val rootSemanticsNode = owner.rootSemanticsNode
        rootSemanticsNodeId = rootSemanticsNode.id

        isCurrentComposeAccessibleTreeDirty = false

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        view.accessibilityElements = listOf(
            traverseSemanticsTree(rootSemanticsNode)
        )

        debugLogger?.let {
            debugTraverse(it, view)
        }

        // TODO: return refocused element if the old focus is not present in the new tree
        //  reverse engineer the logic of iOS Accessibility services that is performed when the
        //  focused object is deallocated
        return NodesSyncResult.Success(null)
    }
}

/**
 * Traverse the accessibility tree starting from [accessibilityObject] using the same(assumed) logic
 * as iOS Accessibility services, and prints its debug data.
 */
@OptIn(ExperimentalComposeApi::class)
private fun debugTraverse(debugLogger: AccessibilityDebugLogger, accessibilityObject: Any, depth: Int = 0) {
    val indent = " ".repeat(depth * 2)

    when (accessibilityObject) {
        is UIView -> {
            debugLogger.log("${indent}View")

            accessibilityObject.accessibilityElements?.let { elements ->
                for (element in elements) {
                    element?.let {
                        debugTraverse(debugLogger, element, depth + 1)
                    }
                }
            }
        }

        is AccessibilityElement -> {
            accessibilityObject.debugLog(debugLogger, depth)
        }

        is AccessibilityContainer -> {
            accessibilityObject.debugLog(debugLogger, depth)

            val count = accessibilityObject.accessibilityElementCount()
            for (index in 0 until count) {
                val element = accessibilityObject.accessibilityElementAtIndex(index)
                element?.let {
                    debugTraverse(debugLogger, element, depth + 1)
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

private val SemanticsNode.unclippedBoundsInWindow: Rect
    get() = Rect(positionInWindow, size.toSize())

/**
 * Returns true if corresponding [LayoutNode] is placed and attached, false otherwise.
 */
private val SemanticsNode.isValid: Boolean
    get() = layoutNode.isPlaced && layoutNode.isAttached

/**
 * Closest ancestor that has [SemanticsActions.ScrollBy] action
 */
private val SemanticsNode.scrollableByAncestor: SemanticsNode?
    get() {
        var current = parent

        while (current != null) {
            if (current.config.getOrNull(SemanticsActions.ScrollBy) != null) {
                return current
            }

            current = current.parent
        }

        return null
    }