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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.uikit.utils.CMPAccessibilityContainer
import androidx.compose.ui.uikit.utils.CMPAccessibilityElement
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.NativeAccessibilityViewSemanticsKey
import androidx.compose.ui.viewinterop.InteropWrappingView
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTime
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotFound
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityFocusedElement
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityPageScrolledNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification
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
import platform.UIKit.UIWindow
import platform.UIKit.accessibilityCustomActions
import platform.UIKit.accessibilityElements
import platform.UIKit.isAccessibilityElement
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
 * Enum class representing different kinds of accessibility invalidation.
 */
private enum class SemanticsTreeInvalidationKind {
    /**
     * The tree was changed, need to recompute the whole tree.
     */
    COMPLETE,

    /**
     * Only bounds of the nodes were changed, need to recompute the bounds of the affected subtrees.
     */
    BOUNDS
}

private class CachedAccessibilityPropertyKey<V>

private object CachedAccessibilityPropertyKeys {
    val accessibilityLabel = CachedAccessibilityPropertyKey<String?>()
    val isAccessibilityElement = CachedAccessibilityPropertyKey<Boolean>()
    val accessibilityIdentifier = CachedAccessibilityPropertyKey<String>()
    val accessibilityHint = CachedAccessibilityPropertyKey<String?>()
    val accessibilityCustomActions = CachedAccessibilityPropertyKey<List<UIAccessibilityCustomAction>>()
    val accessibilityTraits = CachedAccessibilityPropertyKey<UIAccessibilityTraits>()
    val accessibilityValue = CachedAccessibilityPropertyKey<String?>()
    val accessibilityFrame = CachedAccessibilityPropertyKey<CValue<CGRect>>()
    val nativeAccessibilityView = CachedAccessibilityPropertyKey<InteropWrappingView?>()
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

    /**
     * The latest configuration after the last sync with the Compose semantics tree.
     * It's used avoid unnecessary recomputation of merged configs when accessing
     * [SemanticsNode.config]
     */
    private val cachedConfig: SemanticsConfiguration
        get() {
            val config = _cachedConfig

            if (config != null) {
                return config
            } else {
                val newConfig = semanticsNode.config
                _cachedConfig = newConfig
                return newConfig
            }
        }
    private var _cachedConfig: SemanticsConfiguration? = null

    /**
     * A cache for the properties that are computed from the [SemanticsNode.config] and are communicated
     * to iOS Accessibility services.
     */
    private val cachedProperties = mutableMapOf<CachedAccessibilityPropertyKey<*>, Any?>()

    private var children = mutableListOf<AccessibilityElement>()

    /**
     * Cached [InteropWrappingView] for the element if it's present. AX services will be redirected
     * to this view if it's not null, other Compose semantics data for this element will be ignored.
     *
     * The specific type of [InteropWrappingView] is needed to allow to change the
     * [InteropWrappingView.actualAccessibilityContainer], which overrides defaults accessibility
     * containers of view (its superview) to be whatever container is resolved within Compose
     * hierarchy. This is required to allow the synthesized accessibility tree to be properly
     * traversed by AX services.
     */
    private val nativeAccessibilityView: InteropWrappingView?
        get() = getOrElse(CachedAccessibilityPropertyKeys.nativeAccessibilityView) {
            cachedConfig.getOrNull(NativeAccessibilityViewSemanticsKey)?.also {
                it.actualAccessibilityContainer = parent?.accessibilityContainer
            }
        }

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
     * If the child is constructed from a [SemanticsNode] with [NativeAccessibilityViewSemanticsKey],
     * then the element at the given index is a native view.
     * If the child has its own children, then the element at the given index is the synthesized container
     * for the child. Otherwise, the element at the given index is the child itself.
     */
    fun childAccessibilityElementAtIndex(index: NSInteger): Any? {
        val i = index.toInt()

        return if (i in children.indices) {
            val child = children[i]

            val nativeView = child.nativeAccessibilityView

            if (nativeView != null) {
                return nativeView
            } else if (child.hasChildren) {
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

            if (element == child.nativeAccessibilityView) {
                return index.toLong()
            } else if (child.hasChildren && element == child.accessibilityContainer) {
                return index.toLong()
            } else if (element == child) {
                return index.toLong()
            }
        }

        return null
    }

    fun discardCache(invalidationKind: SemanticsTreeInvalidationKind) {
        when (invalidationKind) {
            SemanticsTreeInvalidationKind.COMPLETE -> {
                _cachedConfig = null
                cachedProperties.clear()
            }

            SemanticsTreeInvalidationKind.BOUNDS -> {
                discardCachedAccessibilityFrameRecursively()
            }
        }
    }
    private fun discardCachedAccessibilityFrameRecursively() {
        if (cachedProperties.remove(CachedAccessibilityPropertyKeys.accessibilityFrame) != null) {
            for (child in children) {
                child.discardCachedAccessibilityFrameRecursively()
            }
        } else {
            // Not calculated yet, or the subtree was already discarded. Do nothing.
        }
    }

    fun dispose() {
        check(isAlive) {
            "AccessibilityElement is already disposed"
        }

        isAlive = false
    }

    /**
     * Returns the value for the given [key] from the cache if it's present, otherwise computes the
     * value using the given [block] and caches it.
     */
    @Suppress("UNCHECKED_CAST") // cast is safe because the set value is constrained by the key T
    private inline fun <T>getOrElse(key: CachedAccessibilityPropertyKey<T>, crossinline block: () -> T): T {
        val value = cachedProperties.getOrElse(key) {
            val newValue = block()
            cachedProperties[key] = newValue
            newValue
        }

        return value as T
    }

    override fun accessibilityLabel(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityLabel) {
            val config = cachedConfig

            val contentDescription =
                config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString("\n")

            if (contentDescription != null) {
                contentDescription
            } else {
                val editableText = config.getOrNull(SemanticsProperties.EditableText)?.text

                editableText ?: config.getOrNull(SemanticsProperties.Text)
                    ?.joinToString("\n") { it.text }
            }
        }

    override fun accessibilityActivate(): Boolean {
        if (!isAlive || !semanticsNode.isValid) {
            return false
        }

        val config = cachedConfig

        if (config.contains(SemanticsProperties.Disabled)) {
            return false
        }

        val onClick = config.getOrNull(SemanticsActions.OnClick) ?: return false
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

        mediator.debugLogger?.apply {
            log(null)
            log("Focused on:")
            log(cachedConfig)
        }
    }

    override fun accessibilityScrollToVisible(): Boolean {
        if (!isAlive) {
            return false
        }

        scrollToIfPossible()

        return true
    }

    override fun accessibilityScrollToVisibleWithChild(child: Any): Boolean {
        if (!isAlive) {
            return false
        }

        if (child is AccessibilityElement && child.isAlive) {
            child.scrollToIfPossible()
            return true
        }

        return false
    }

    /**
     * Try to perform a scroll on any ancestor of this element if the element is not fully visible.
     */
    private fun scrollToIfPossible() {
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
            parent?.scrollByIfPossible(
                0f,
                unclippedRect.top - scrollableAncestorRect.top - scrollableAncestor.size.height / 2
            )
        } else if (unclippedRect.bottom > scrollableAncestorRect.bottom) {
            // The element is below the screen, scroll down
            parent?.scrollByIfPossible(
                0f,
                unclippedRect.bottom - scrollableAncestorRect.bottom + scrollableAncestor.size.height / 2
            )
        } else if (unclippedRect.left < scrollableAncestorRect.left) {
            // The element is to the left of the screen, scroll left
            parent?.scrollByIfPossible(
                unclippedRect.left - scrollableAncestorRect.left - scrollableAncestor.size.width / 2,
                0f
            )
        } else if (unclippedRect.right > scrollableAncestorRect.right) {
            // The element is to the right of the screen, scroll right
            parent?.scrollByIfPossible(
                unclippedRect.right - scrollableAncestorRect.right + scrollableAncestor.size.width / 2,
                0f
            )
        }
    }

    private fun scrollByIfPossible(dx: Float, dy: Float) {
        if (!isAlive) {
            return
        }

        // if has scrollBy action, invoke it, otherwise try to scroll the parent
        val action = cachedConfig.getOrNull(SemanticsActions.ScrollBy)?.action

        if (action != null) {
            action(dx, dy)
        } else {
            parent?.scrollByIfPossible(dx, dy)
        }
    }

    private fun scrollIfPossible(direction: UIAccessibilityScrollDirection): AccessibilityElement? {
        val config = cachedConfig

        when (direction) {
            UIAccessibilityScrollDirectionUp -> {
                var result = config.getOrNull(SemanticsActions.PageUp)?.action?.invoke()

                if (result != null) {
                    return if (result) this else null
                }

                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    0f,
                    -semanticsNode.size.height.toFloat()
                )

                if (result != null) {
                    return if (result) this else null
                }
            }

            UIAccessibilityScrollDirectionDown -> {
                var result = config.getOrNull(SemanticsActions.PageDown)?.action?.invoke()

                if (result != null) {
                    return if (result) this else null
                }

                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    0f,
                    semanticsNode.size.height.toFloat()
                )

                if (result != null) {
                    return if (result) this else null
                }
            }

            UIAccessibilityScrollDirectionLeft -> {
                var result = config.getOrNull(SemanticsActions.PageLeft)?.action?.invoke()

                if (result != null) {
                    return if (result) this else null
                }

                // TODO: check RTL support
                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    -semanticsNode.size.width.toFloat(),
                    0f,
                )

                if (result != null) {
                    return if (result) this else null
                }
            }

            UIAccessibilityScrollDirectionRight -> {
                var result = config.getOrNull(SemanticsActions.PageRight)?.action?.invoke()

                if (result != null) {
                    return if (result) this else null
                }

                // TODO: check RTL support
                result = config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    semanticsNode.size.width.toFloat(),
                    0f,
                )

                if (result != null) {
                    return if (result) this else null
                }
            }

            else -> {
                // TODO: UIAccessibilityScrollDirectionPrevious, UIAccessibilityScrollDirectionNext
            }
        }

        parent?.let {
            return it.scrollIfPossible(direction)
        }

        return null
    }

    override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
        if (!isAlive) {
            return false
        }

        if (cachedConfig.contains(SemanticsProperties.Disabled)) {
            return false
        }

        val frame = semanticsNode.boundsInWindow
        val approximateScrollAnimationDuration = 350L

        val scrollableElement = scrollIfPossible(direction)
        return if (scrollableElement != null) {
            mediator.notifyScrollCompleted(
                delay = approximateScrollAnimationDuration,
                focusedNode = semanticsNode,
                focusedRectInWindow = frame
            )
            true
        } else {
            false
        }
    }

    override fun isAccessibilityElement(): Boolean =
        getOrElse(CachedAccessibilityPropertyKeys.isAccessibilityElement) {
            val config = cachedConfig

            if (config.contains(SemanticsProperties.InvisibleToUser)) {
                false
            } else {
                // TODO: investigate if it can it be one of those _and_ contain properties that should
                //  be communicated to iOS?
                if (config.getOrNull(SemanticsProperties.IsTraversalGroup) == true
                    || config.contains(SemanticsProperties.IsPopup)
                    || config.contains(SemanticsProperties.IsDialog)
                ) {
                    false
                } else {
                    config.containsImportantForAccessibility()
                }
            }
        }

    override fun accessibilityIdentifier(): String =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityIdentifier) {
            cachedConfig.getOrNull(SemanticsProperties.TestTag)
                ?: "AccessibilityElement for SemanticsNode(id=$semanticsNodeId)"
        }

    override fun accessibilityHint(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityHint) {
            cachedConfig.getOrNull(SemanticsActions.OnClick)?.label
        }

    override fun accessibilityCustomActions(): List<UIAccessibilityCustomAction> {
        val config = cachedConfig

        return getOrElse(CachedAccessibilityPropertyKeys.accessibilityCustomActions) {
            config.getOrNull(SemanticsActions.CustomActions)?.let { actions ->
                actions.map {
                    UIAccessibilityCustomAction(
                        name = it.label,
                        actionHandler = { _ ->
                            if (config.contains(SemanticsProperties.Disabled)) {
                                false
                            } else {
                                it.action.invoke()
                            }
                        }
                    )
                }
            } ?: emptyList()
        }
    }

    override fun accessibilityTraits(): UIAccessibilityTraits =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityTraits) {
            var result = UIAccessibilityTraitNone

            val config = cachedConfig

            if (config.contains(SemanticsProperties.LiveRegion)) {
                // TODO: LiveRegionMode in the config is currently ignored.
                //  the default behavior due this flag set will actually do `Polite` announcements
                //  to do `Assertive` announcements, we need to post a notification explicitly on each change
                //  which we need to track manually
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

            config.getOrNull(SemanticsProperties.LiveRegion)?.let {
                result = result or UIAccessibilityTraitUpdatesFrequently
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

            result
        }


    override fun accessibilityValue(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityValue) {
            cachedConfig.getOrNull(SemanticsProperties.StateDescription)
        }

    override fun accessibilityFrame(): CValue<CGRect> =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityFrame) {
            // AX services expect the frame to be in the coordinate space of the root UIWindow
            // [semanticsNode.boundsInWindow] provide it in so called `window container` space,
            // which can be different from the root UIWindow space.
            mediator.convertToAppWindowCGRect(semanticsNode.boundsInWindow)
        }


    override fun accessibilityPerformEscape(): Boolean {
        if (!isAlive) {
            mediator.debugLogger?.log("accessibilityPerformEscape() called after $semanticsNodeId was removed from the tree")
            return false
        }

        if (mediator.performEscape()) {
            UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, null)
            return true
        } else {
            return super.accessibilityPerformEscape()
        }
    }

    // TODO: check the reference/value semantics for SemanticsNode, perhaps it doesn't need
    //  recreation at all
    fun updateWithNewSemanticsNode(newSemanticsNode: SemanticsNode) {
        check(semanticsNode.id == newSemanticsNode.id)
        // TODO: track that SemanticsProperties.LiveRegion is present and conditionally start
        //  proactively comparing the the accessibilityLabel and accessibilityValue properties
        //  to post notifications about the changes.
        semanticsNode = newSemanticsNode
    }

    /**
     * Find the element that is eligible for focusing.
     */
    fun findFocusableElement(): AccessibilityElement? {
        // TODO: follow convention on refocusing on the first eligible element
        //  following the element with `Heading` trait
        check(isAlive)

        if (isAccessibilityElement) {
            return this
        }

        for (child in children) {
            val focusableElement = child.findFocusableElement()

            if (focusableElement != null) {
                return focusableElement
            }
        }

        return null
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

    private fun debugContainmentChain() = debugContainmentChain(this)

    fun debugLog(logger: AccessibilityDebugLogger, depth: Int) {
        val indent = " ".repeat(depth * 2)

        val container = resolveAccessibilityContainer() as AccessibilityContainer
        val indexOfSelf = container.indexOfAccessibilityElement(this)

        check(indexOfSelf != NSNotFound)
        check(container.accessibilityElementAtIndex(indexOfSelf) == this)

        logger.apply {
            log("${indent}AccessibilityElement_$semanticsNodeId")
            log("$indent  containmentChain: ${debugContainmentChain()}")
            log("$indent  isAccessibilityElement: $isAccessibilityElement")
            log("$indent  accessibilityLabel: $accessibilityLabel")
            log("$indent  accessibilityValue: $accessibilityValue")
            log("$indent  accessibilityTraits: $accessibilityTraits")
            log("$indent  accessibilityFrame: ${NSStringFromCGRect(accessibilityFrame)}")
            log("$indent  accessibilityIdentifier: $accessibilityIdentifier")
            log("$indent  accessibilityCustomActions: $accessibilityCustomActions")
        }
    }

    fun hitTest(offsetInWindow: Offset): AccessibilityElement? {
        if (!isAlive) {
            return null
        }

        val containsPoint = semanticsNode.boundsInWindow.contains(offsetInWindow)
        if (containsPoint && isAccessibilityElement) {
            return this
        }

        children.forEach { child ->
            child.hitTest(offsetInWindow)?.let {
                return it
            }
        }
        return this.takeIf { containsPoint }
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

private class NodesSyncResult(
    val newElementToFocus: Any?
)

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

@OptIn(ExperimentalComposeApi::class)
private val AccessibilitySyncOptions.shouldPerformSync
    get() =
        when (this) {
            is AccessibilitySyncOptions.Never -> false
            is AccessibilitySyncOptions.WhenRequiredByAccessibilityServices -> UIAccessibilityIsVoiceOverRunning()
            is AccessibilitySyncOptions.Always -> true
        }

@OptIn(ExperimentalComposeApi::class)
private val AccessibilitySyncOptions.debugLoggerIfEnabled: AccessibilityDebugLogger?
    get() =
        if (shouldPerformSync) {
            debugLogger
        } else {
            null
        }


/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
@OptIn(ExperimentalComposeApi::class)
internal class AccessibilityMediator(
    val view: UIView,
    private val owner: SemanticsOwner,
    coroutineContext: CoroutineContext,
    private val getAccessibilitySyncOptions: () -> AccessibilitySyncOptions,

    /**
     * A function that converts the given [Rect] from the semantics tree coordinate space (window container for layers)
     * to the [CGRect] in coordinate space of the app window.
     */
    val convertToAppWindowCGRect: (Rect, UIWindow) -> CValue<CGRect>,
    val performEscape: () -> Boolean
) {
    /**
     * Indicates that this mediator was just created and the accessibility focus should be set on the
     * first eligible element.
     */
    private var needsInitialRefocusing = true
    private var isAlive = true

    private var inflightScrollsCount = 0
    private val needsRedundantRefocusingOnSameElement: Boolean
        get() = inflightScrollsCount > 0

    /**
     * The kind of invalidation that determines what kind of logic will be executed in the next sync.
     * `COMPLETE` invalidation means that the whole tree should be recomputed, `BOUNDS` means that only
     * the bounds of the nodes should be recomputed. A list of changed performed by `BOUNDS` path
     * is a strict subset of `COMPLETE`, so in the end of sync it will be reset to `BOUNDS`.
     * Executing sync assumes that at least one kind of invalidation happened, if it was triggered
     * by [onSemanticsChange] it will be automatically promoted to `COMPLETE`.
     */
    private var invalidationKind = SemanticsTreeInvalidationKind.COMPLETE

    /**
     * A set of node ids that had their bounds invalidated after the last sync.
     */
    private var invalidatedBoundsNodeIds = mutableSetOf<Int>()
    private val invalidationChannel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    /**
     * Remembered [AccessibilityDebugLogger] after last sync, if logging is enabled according to
     * [AccessibilitySyncOptions].
     */
    var debugLogger: AccessibilityDebugLogger? = null
        private set

    var rootSemanticsNodeId: Int = -1

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
        getAccessibilitySyncOptions().debugLoggerIfEnabled?.log("AccessibilityMediator for ${view} created")

        coroutineScope.launch {
            // The main loop that listens for invalidations and performs the tree syncing
            // Will exit on CancellationException from within await on `invalidationChannel.receive()`
            // when [job] is cancelled
            while (true) {
                invalidationChannel.receive()

                while (invalidationChannel.tryReceive().isSuccess) {
                    // Do nothing, just consume the channel
                    // Workaround for the channel buffering two invalidations despite the capacity of 1
                }

                val syncOptions = getAccessibilitySyncOptions()

                val shouldPerformSync = syncOptions.shouldPerformSync

                debugLogger = if (shouldPerformSync) {
                    syncOptions.debugLogger
                } else {
                    null
                }

                if (shouldPerformSync) {
                    var result: NodesSyncResult

                    val time = measureTime {
                        result = sync(invalidationKind)
                    }

                    debugLogger?.log("AccessibilityMediator.sync took $time")
                    debugLogger?.log("LayoutChanged, newElementToFocus: ${result.newElementToFocus}")

                    UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, result.newElementToFocus)
                }

                invalidationKind = SemanticsTreeInvalidationKind.BOUNDS
                invalidatedBoundsNodeIds.clear()
            }
        }
    }

    fun convertToAppWindowCGRect(rect: Rect): CValue<CGRect> {
        val window = view.window ?: return CGRectZero.readValue()

        return convertToAppWindowCGRect(rect, window)
    }

    fun notifyScrollCompleted(
        delay: Long,
        focusedNode: SemanticsNode,
        focusedRectInWindow: Rect
    ) {
        inflightScrollsCount++

        coroutineScope.launch {
            delay(delay)

            inflightScrollsCount--

            UIAccessibilityPostNotification(
                UIAccessibilityPageScrolledNotification,
                null
            )

            debugLogger?.log("PageScrolled")

            if (accessibilityElementsMap[focusedNode.id] == null) {
                findElementInRect(rect = focusedRectInWindow)?.let {
                    debugLogger?.log("LayoutChanged, result: $it")

                    UIAccessibilityPostNotification(
                        UIAccessibilityLayoutChangedNotification,
                        it
                    )
                }
            }
        }
    }

    fun onSemanticsChange() {
        debugLogger?.log("onSemanticsChange")

        invalidationKind = SemanticsTreeInvalidationKind.COMPLETE
        invalidationChannel.trySend(Unit)
    }

    fun onLayoutChange(nodeId: Int) {
        debugLogger?.log("onLayoutChange (nodeId=$nodeId)")

        invalidatedBoundsNodeIds.add(nodeId)

        // unprocessedInvalidationKind will be set to BOUNDS in sync(), it's a strict subset of COMPLETE
        invalidationChannel.trySend(Unit)
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

        for (element in accessibilityElementsMap.values) {
            element.discardCache(SemanticsTreeInvalidationKind.COMPLETE)
        }

        return checkNotNull(rootAccessibilityElement.resolveAccessibilityContainer()) {
            "Root element must always have an enclosing container"
        }
    }

    /**
     * Syncs the accessibility tree with the current semantics tree.
     */
    private fun sync(invalidationKind: SemanticsTreeInvalidationKind): NodesSyncResult {
        when (invalidationKind) {
            SemanticsTreeInvalidationKind.COMPLETE -> {
                return completeSync()
            }

            SemanticsTreeInvalidationKind.BOUNDS -> {
                for (id in invalidatedBoundsNodeIds) {
                    val element = accessibilityElementsMap[id]
                    element?.discardCache(SemanticsTreeInvalidationKind.BOUNDS)
                }

                return NodesSyncResult(null)
            }
        }
    }

    /**
     * Performs a complete sync of the accessibility tree with the current semantics tree.
     *
     * TODO: Does a full tree traversal on every sync, expect changes from Google, they are also aware
     *  of the issue and associated performance overhead.
     */

    private fun completeSync(): NodesSyncResult {
        // TODO: investigate what needs to be done to reflect that this hierarchy is probably covered
        //   by sibling overlay or another UIView hierarchy represented by other mediator
        val rootSemanticsNode = owner.rootSemanticsNode
        rootSemanticsNodeId = rootSemanticsNode.id

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        view.accessibilityElements = listOf(
            traverseSemanticsTree(rootSemanticsNode)
        )

        debugLogger?.let {
            debugTraverse(it, view)
        }

        val focusedElement = UIAccessibilityFocusedElement(null) as? AccessibilityElement

        // TODO: in future the focused element could be the interop UIView that is detached from the
        //  hierarchy, but still maintains the focus until the GC collects it, or AX services detect
        //  that it's not reachable anymore through containment chain
        val isFocusedElementAlive = focusedElement?.isAlive ?: false

        val isFocusedElementDead = !isFocusedElementAlive

        val needsRefocusing = needsInitialRefocusing || isFocusedElementDead

        val newElementToFocus = if (needsRefocusing) {
            debugLogger?.log("Needs refocusing")
            val refocusedElement = checkNotNull(accessibilityElementsMap[rootSemanticsNodeId])
                .findFocusableElement()

            if (refocusedElement != null) {
                needsInitialRefocusing = false
                debugLogger?.log("Refocusing on $refocusedElement")
            } else {
                debugLogger?.log("No focusable element found")
            }

            refocusedElement
        } else {
            if (needsRedundantRefocusingOnSameElement) {
                focusedElement?.semanticsNodeId?.let {
                    accessibilityElementsMap[it]
                }
            } else {
                null // No need to refocus to anything
            }
        }

        return NodesSyncResult(newElementToFocus)
    }

    private fun findElementInRect(rect: Rect): AccessibilityElement? {
        val offsetInWindow = Offset(
            x = (rect.right + rect.left) / 2,
            y = (rect.bottom + rect.top) / 2
        )
        return accessibilityElementsMap[rootSemanticsNodeId]?.hitTest(offsetInWindow)
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