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

import androidx.collection.MutableIntSet
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.platform.accessibility.AccessibilityScrollEventResult
import androidx.compose.ui.platform.accessibility.accessibilityCustomActions
import androidx.compose.ui.platform.accessibility.accessibilityTraits
import androidx.compose.ui.platform.accessibility.accessibilityValue
import androidx.compose.ui.platform.accessibility.allScrollableParentNodeIds
import androidx.compose.ui.platform.accessibility.canBeAccessibilityElement
import androidx.compose.ui.platform.accessibility.canScroll
import androidx.compose.ui.platform.accessibility.contentDescription
import androidx.compose.ui.platform.accessibility.isRTL
import androidx.compose.ui.platform.accessibility.isScreenReaderFocusable
import androidx.compose.ui.platform.accessibility.linkTag
import androidx.compose.ui.platform.accessibility.linkText
import androidx.compose.ui.platform.accessibility.scrollIfPossible
import androidx.compose.ui.platform.accessibility.scrollToCenterRectIfNeeded
import androidx.compose.ui.platform.accessibility.sortFlattenChildren
import androidx.compose.ui.platform.accessibility.unclippedBoundsInWindow
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllUncoveredSemanticsNodesToIntObjectMap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.sortByGeometryGroupings
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.toNanoSeconds
import androidx.compose.ui.uikit.utils.CMPAccessibilityElement
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toCGRect
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.NativeAccessibilityViewSemanticsKey
import androidx.compose.ui.window.DisplayLinkListener
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.native.ref.WeakReference
import kotlin.time.measureTime
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import objcnames.classes.Protocol
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGPointZero
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectEqualToRect
import platform.CoreGraphics.CGRectGetMaxX
import platform.CoreGraphics.CGRectGetMaxY
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectIntersectsRect
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.CGSizeZero
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityAnnouncementNotification
import platform.UIKit.UIAccessibilityContainerType
import platform.UIKit.UIAccessibilityContainerTypeNone
import platform.UIKit.UIAccessibilityContainerTypeSemanticGroup
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityElement
import platform.UIKit.UIAccessibilityElementFocusedNotification
import platform.UIKit.UIAccessibilityFocusedElement
import platform.UIKit.UIAccessibilityFocusedElementKey
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityNotifications
import platform.UIKit.UIAccessibilityPageScrolledNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIEdgeInsetsInsetRect
import platform.UIKit.UIEvent
import platform.UIKit.UIFocusAnimationCoordinator
import platform.UIKit.UIFocusEnvironmentProtocol
import platform.UIKit.UIFocusItemContainerProtocol
import platform.UIKit.UIFocusItemProtocol
import platform.UIKit.UIFocusItemScrollableContainerProtocol
import platform.UIKit.UIFocusSystem
import platform.UIKit.UIFocusUpdateContext
import platform.UIKit.UIView
import platform.UIKit.accessibilityElementAtIndex
import platform.UIKit.accessibilityElementCount
import platform.UIKit.accessibilityElements
import platform.UIKit.accessibilityFrame
import platform.UIKit.accessibilityHitTest
import platform.UIKit.isAccessibilityElement
import platform.UIKit.setAutomationElements
import platform.darwin.NSObject
import platform.objc.objc_getProtocol
import platform.objc.protocol_isEqual

private val DUMMY_UI_ACCESSIBILITY_CONTAINER = NSObject()
private val USE_HIERARCHICAL_COORDINATE_SPACE = available(OS.Ios to OSVersion(major = 18))

internal sealed interface AccessibilityElementKey {
    val id: Int

    data class Semantics(override val id: Int) : AccessibilityElementKey
    data class Container(override val id: Int) : AccessibilityElementKey
}

/**
 * Sealed interface that represents behavior of actual accessibility element.
 */
private sealed interface AccessibilityNode {
    val key: AccessibilityElementKey
    val isAccessibilityElement: Boolean
    val semanticsNode: SemanticsNode

    val contentDescription: String? get() = null
    val shouldMergeDescription: Boolean get() = false
    val accessibilityHint: String? get() = null
    val accessibilityValue: String? get() = null
    val accessibilityTraits: UIAccessibilityTraits get() = UIAccessibilityTraitNone
    val accessibilityContainerType: UIAccessibilityContainerType
        get() = UIAccessibilityContainerTypeNone
    val accessibilityIdentifier: String? get() = null
    val accessibilityInteropView: InteropWrappingView? get() = null
    val accessibilityCustomActions: List<UIAccessibilityCustomAction> get() = emptyList()

    fun accessibilityActivate(): Boolean = false
    fun accessibilityIncrement() {}
    fun accessibilityDecrement() {}
    fun accessibilityElementDidBecomeFocused() {}
    fun accessibilityElementDidLoseFocus() {}
    fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean = false
    fun accessibilityPerformEscape(): Boolean = false

    // Focus API
    val canBecomeFocused: Boolean get() = false
    fun didBecomeFocused() {}
    fun didResignFocused() {}

    val canScroll: Boolean get() = false
    val scrollContentOffset: CValue<CGPoint> get() = CGPointZero.readValue()
    val scrollVisibleSize: CValue<CGSize> get() = CGSizeZero.readValue()
    val scrollContentSize: CValue<CGSize> get() = CGSizeZero.readValue()
    suspend fun scrollBy(delta: CValue<CGPoint>) {}

    /**
     * Represents a projection of the Compose semantics node to the iOS world.
     * The object itself is a node in a generated tree that matches 1-to-1 with the [SemanticsNode].
     * @semanticsNode node associated with current accessibility element
     * @mediator reference to the containing AccessibilityMediator
     */
    class Semantics(
        semanticsNode: SemanticsNode,
        private val mediator: AccessibilityMediator,
        private val isBeyondBounds: Boolean,
    ) : Container(semanticsNode) {
        private val cachedConfig = semanticsNode.config
        private val scrollableParentNodeIds by lazy { semanticsNode.allScrollableParentNodeIds }

        override val key: AccessibilityElementKey get() = semanticsNode.semanticsKey

        override val isAccessibilityElement: Boolean get() {
            return if (semanticsNode.isScreenReaderFocusable()) {
                isBeyondBoundsOrFocusable
            } else {
                false
            }
        }

        override val accessibilityContainerType: UIAccessibilityContainerType
            get() = when {
                semanticsNode.canBeAccessibilityElement() -> UIAccessibilityContainerTypeNone
                semanticsNode.isTraversalGroup -> UIAccessibilityContainerTypeSemanticGroup
                else -> UIAccessibilityContainerTypeNone
            }

        override val accessibilityInteropView: InteropWrappingView?
            get() = cachedConfig.getOrNull(NativeAccessibilityViewSemanticsKey)?.also {
                it.isAccessibilityFocusable = ::isBeyondBoundsOrFocusable
            }

        override val contentDescription: String?
            get() = semanticsNode.contentDescription

        override val shouldMergeDescription: Boolean
            get() = semanticsNode.unmergedConfig.isMergingSemanticsOfDescendants &&
                semanticsNode.canBeAccessibilityElement()

        override val accessibilityIdentifier: String?
            get() = cachedConfig.getOrNull(SemanticsProperties.TestTag)
                ?: semanticsNode.linkTag()

        override val accessibilityHint: String?
            get() = cachedConfig.getOrNull(SemanticsActions.OnClick)?.label

        override val accessibilityCustomActions: List<UIAccessibilityCustomAction>
            get() = cachedConfig.accessibilityCustomActions()

        override val accessibilityTraits: UIAccessibilityTraits
            get() = cachedConfig.accessibilityTraits()

        override val accessibilityValue: String?
            get() = cachedConfig.accessibilityValue()

        override fun accessibilityActivate(): Boolean {
            if (!semanticsNode.isValid) {
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

        override fun accessibilityIncrement() =
            updateProgress(increment = true)

        override fun accessibilityDecrement() =
            updateProgress(increment = false)

        private fun updateProgress(increment: Boolean) {
            val progress =
                cachedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo) ?: return
            val setProgress = cachedConfig.getOrNull(SemanticsActions.SetProgress) ?: return
            val step = (progress.range.endInclusive - progress.range.start) / progress.steps
            val value = progress.current + if (increment) step else -step
            setProgress.action?.invoke(value)
        }

        override fun accessibilityElementDidBecomeFocused() {
            accessibilityDebugLogger?.apply {
                log(null)
                log("Focused on:")
                log(cachedConfig)
            }
            mediator.setFocusTarget(key)
        }

        override fun accessibilityElementDidLoseFocus() {
            mediator.clearFocusTargetIfNeeded(key)
        }

        override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
            if (cachedConfig.contains(SemanticsProperties.Disabled)) {
                return false
            }

            val frame = semanticsNode.boundsInWindow
            val approximateScrollAnimationDuration = 350L

            val result = semanticsNode.scrollIfPossible(direction)
            return if (result != null) {
                mediator.clearFocusTargetIfNeeded(key)
                mediator.notifyScrollCompleted(
                    scrollResult = result,
                    delay = approximateScrollAnimationDuration,
                    focusedNode = semanticsNode,
                    focusedRectInWindow = frame
                )
                true
            } else {
                false
            }
        }

        override fun accessibilityPerformEscape(): Boolean {
            if (mediator.performEscape()) {
                AccessibilityNotification(UIAccessibilityScreenChangedNotification).postNotification()
                return true
            } else {
                return false
            }
        }

        override val canBecomeFocused: Boolean
            get() = semanticsNode.unmergedConfig.contains(SemanticsProperties.Focused)

        override fun didBecomeFocused() {
            mediator.scrollToAccessibilityElement(key)
            mediator.keyboardFocusedElementKey = key

            semanticsNode.config.getOrNull(SemanticsActions.RequestFocus)?.action?.invoke()
        }

        override fun didResignFocused() {
            if (mediator.keyboardFocusedElementKey == key) {
                mediator.keyboardFocusedElementKey = null
            }
        }

        private val isBeyondBoundsOrFocusable: Boolean get() = if (isBeyondBounds) {
            // The accessibility element beyond the bounds should only be focusable
            // when it is located within the same scrollable container as the element
            // that is already focused, in order to prevent an unwanted selection order.
            scrollableParentNodeIds.any {
                mediator.focusedNodesScrollableParentsIds.contains(it)
            }
        } else {
            true
        }
    }

    /**
     * Unlike Android, UIAccessibilityElement can't be a container and an element at the same time.
     * If [isAccessibilityElement] is true, iOS accessibility services won't access the object
     * UIAccessibilityContainer methods. To implement this behavior, flatting the container node
     * with all its children. [Container] is used to indicate element that contains container
     * semantic node with all its children.
     */
    open class Container(
        override val semanticsNode: SemanticsNode
    ) : AccessibilityNode {
        override val key: AccessibilityElementKey = semanticsNode.containerKey

        override val isAccessibilityElement = false

        override val accessibilityContainerType: UIAccessibilityContainerType =
            UIAccessibilityContainerTypeSemanticGroup

        private val horizontalAxis: ScrollAxisRange? = semanticsNode.unmergedConfig
            .getOrNull(SemanticsProperties.HorizontalScrollAxisRange)

        private val verticalAxis: ScrollAxisRange? = semanticsNode.unmergedConfig
            .getOrNull(SemanticsProperties.VerticalScrollAxisRange)

        private val width: Float get() = semanticsNode.size.width.toFloat()

        private val height: Float get() = semanticsNode.size.height.toFloat()

        override val canScroll: Boolean = horizontalAxis != null || verticalAxis != null

        override val scrollContentOffset: CValue<CGPoint>
            get() = with(semanticsNode.layoutNode.density) {
                CGPointMake(
                    x = (horizontalAxis?.value() ?: 0f).toDp().value.toDouble(),
                    y = (verticalAxis?.value() ?: 0f).toDp().value.toDouble(),
                )
            }

        override val scrollContentSize: CValue<CGSize>
            get() {
                return with(semanticsNode.layoutNode.density) {
                    CGSizeMake(
                        width = (width + (horizontalAxis?.maxValue() ?: 0f)).toDp().value.toDouble(),
                        height = (height + (verticalAxis?.maxValue() ?: 0f)).toDp().value.toDouble(),
                    )
                }
            }

        override val scrollVisibleSize: CValue<CGSize>
            get() = with(semanticsNode.layoutNode.density) {
                CGSizeMake(
                    width = width.toDp().value.toDouble(),
                    height = height.toDp().value.toDouble()
                )
            }

        override suspend fun scrollBy(delta: CValue<CGPoint>) {
            val deltaInPx = with(semanticsNode.layoutNode.density) {
                delta.toDpOffset().let {
                    Offset(it.x.toPx(), it.y.toPx())
                }
            }

            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.ScrollByOffset)
                ?.invoke(deltaInPx)
        }
    }
}

private class CachedAccessibilityPropertyKey<V>

private object CachedAccessibilityPropertyKeys {
    val accessibilityLabel = CachedAccessibilityPropertyKey<String?>()
    val accessibilityIdentifier = CachedAccessibilityPropertyKey<String?>()
    val accessibilityHint = CachedAccessibilityPropertyKey<String?>()
    val accessibilityCustomActions = CachedAccessibilityPropertyKey<List<UIAccessibilityCustomAction>>()
    val accessibilityTraits = CachedAccessibilityPropertyKey<UIAccessibilityTraits>()
    val accessibilityValue = CachedAccessibilityPropertyKey<String?>()
    val accessibilityElements = CachedAccessibilityPropertyKey<List<Any>>()
}

@OptIn(BetaInteropApi::class)
@ExportObjCClass
private class AccessibilityRoot(
    val mediator: AccessibilityMediator,
) : CMPAccessibilityElement(DUMMY_UI_ACCESSIBILITY_CONTAINER),
    UIFocusItemContainerProtocol {
    var element: AccessibilityElement? = null
        set(value) {
            if (field?.accessibilityContainer === this) {
                field?.setAccessibilityContainer(null)
            }
            field = value
            field?.setAccessibilityContainer(this)
            mediator.onScreenReaderActive(value != null)

            setAccessibilityElements(value?.let { listOf(it) })
        }

    override fun accessibilityElements(): List<*> {
        if (mediator.isEnabled) {
            mediator.activateAccessibilityIfNeeded()
        }

        return super.accessibilityElements()
    }

    override fun isAccessibilityElement(): Boolean = false

    override fun accessibilityContainer() = mediator.view

    override fun accessibilityFrame(): CValue<CGRect> =
        mediator.view.convertRect(mediator.view.bounds, toView = null)

    // UIFocusItemContainerProtocol

    override fun coordinateSpace(): UICoordinateSpaceProtocol = mediator.view

    override fun focusItemsInRect(rect: CValue<CGRect>): List<*> {
        return if (mediator.isEnabled) {
            mediator.activateAccessibilityIfNeeded()
            listOfNotNull(element)
        } else {
            emptyList<Any>()
        }
    }

    override fun accessibilityHitTest(point: CValue<CGPoint>, withEvent: UIEvent?): Any? {
        if (!mediator.isEnabled) {
            return null
        }

        mediator.activateAccessibilityIfNeeded()

        val hitSemanticsEntities = HitTestResult()
        val pointerPosition = with(mediator.view.density) {
            val point = point.toDpOffset()
            Offset(point.x.toPx(), point.y.toPx())
        }

        mediator.owner.unmergedRootSemanticsNode.layoutNode.hitTestSemantics(
            pointerPosition = pointerPosition,
            hitSemanticsEntities = hitSemanticsEntities,
        )

        for (i in hitSemanticsEntities.lastIndex downTo 0) {
            val layoutNode = hitSemanticsEntities[i].requireLayoutNode()

            val element =
                mediator.getAccessibilityElement(AccessibilityElementKey.Semantics(layoutNode.semanticsId))
                    ?: continue
            val interopView = (element as AccessibilityElement).node.accessibilityInteropView
            if (interopView != null && interopView.isAccessibilityFocusable()) {
                val rect = mediator.view.convertRect(element.accessibilityFrame(), fromView = null)
                val (originX, originY) = rect.useContents { origin.x to origin.y }

                val pointInElement = point.useContents {
                    CGPointMake(x = x - originX, y = y - originY)
                }

                interopView.accessibilityHitTest(pointInElement, withEvent)?.let {
                    return it
                }
            }

            if (!element.isAccessibilityElement()) {
                continue
            }

            return element
        }

        // Used as a backup to iOS-like focus behavior
        return super.accessibilityHitTest(point, withEvent)
    }
}

@OptIn(BetaInteropApi::class)
@ExportObjCClass
private class AccessibilityElement(
    var node: AccessibilityNode,
    val mediator: AccessibilityMediator,
    children: List<AccessibilityElement>
) : CMPAccessibilityElement(DUMMY_UI_ACCESSIBILITY_CONTAINER),
    UIFocusItemProtocol,
    UIFocusItemContainerProtocol,
    UIFocusItemScrollableContainerProtocol,
    UIFocusEnvironmentProtocol,
    UICoordinateSpaceProtocol {

    /**
     * A cache for the properties that are computed from the [SemanticsNode.config] and are communicated
     * to iOS Accessibility services.
     */
    private val cachedProperties = mutableMapOf<CachedAccessibilityPropertyKey<*>, Any?>()

    private val scrollableProtocol = objc_getProtocol("UIFocusItemScrollableContainer")!!
    override fun conformsToProtocol(aProtocol: Protocol?): Boolean {
        if (protocol_isEqual(proto = aProtocol, other = scrollableProtocol)) {
            return node.canScroll
        }
        return super.conformsToProtocol(aProtocol)
    }

    val key: AccessibilityElementKey get() = node.key

    private var disposed = false

    /**
     * Indicates whether this element is still present in the tree.
     */
    private val isAlive get() = !disposed && node.semanticsNode.isValid

    init {
        setAccessibilityElements(children + nodeSemanticsElements())
        children.forEach { it.setAccessibilityContainer(this) }
        if (available(OS.Ios to OSVersion(major = 17))) {
            setAutomationElements(children + nodeSemanticsElements())
        }
    }

    private fun nodeSemanticsElements(): List<Any> =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityElements) {
            listOfNotNull(node.accessibilityInteropView?.also {
                it.actualAccessibilityContainer = this
            })
        }

    fun update(node: AccessibilityNode, children: List<AccessibilityElement>) {
        assert(key == node.key) {
            "Element should be updated with a node that has the same key as the initial node"
        }
        this.node = node

        accessibilityElements?.forEach {
            (it as? CMPAccessibilityElement)?.setAccessibilityContainer(null)
        }
        setAccessibilityElements(children + nodeSemanticsElements())
        if (available(OS.Ios to OSVersion(major = 17))) {
            setAutomationElements(children + nodeSemanticsElements())
        }
        children.forEach { it.setAccessibilityContainer(this) }
        this.cachedProperties.clear()
    }

    fun dispose() {
        check(!disposed) {
            "AccessibilityElement is already disposed"
        }

        disposed = true
        setAccessibilityContainer(null)
        setAccessibilityElements(emptyList<Any>())
        if (available(OS.Ios to OSVersion(major = 17))) {
            setAutomationElements(null)
        }
        cachedProperties.clear()
    }

    /**
     * Returns the value for the given [key] from the cache if it's present, otherwise computes the
     * value using the given [block] and caches it.
     */
    @Suppress("UNCHECKED_CAST") // cast is safe because the set value is constrained by the key T
    private inline fun <T> getOrElse(
        key: CachedAccessibilityPropertyKey<T>,
        crossinline block: () -> T
    ): T {
        val value = cachedProperties.getOrElse(key) {
            val newValue = block()
            cachedProperties[key] = newValue
            newValue
        }

        return value as T
    }

    override fun accessibilityLabel(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityLabel) {
            makeAccessibilityLabel()
        }

    override fun accessibilityElementDidBecomeFocused() {
        if (!isAlive) {
            return
        }

        node.accessibilityElementDidBecomeFocused()
    }

    override fun accessibilityElementDidLoseFocus() {
        node.accessibilityElementDidLoseFocus()
    }

    override fun accessibilityActivate(): Boolean {
        if (!isAlive) {
            return false
        }

        return node.accessibilityActivate()
    }

    override fun accessibilityIncrement() {
        if (!isAlive) {
            return
        }

        node.accessibilityIncrement()
    }

    override fun accessibilityDecrement() {
        if (!isAlive) {
            return
        }

        node.accessibilityDecrement()
    }

    override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
        if (!isAlive) {
            return false
        }

        return node.accessibilityScroll(direction)
    }

    override fun isAccessibilityElement(): Boolean {
        // Node visibility changes don't trigger accessibility semantic recalculation.
        // This value should not be cached. See [SemanticsNode.isScreenReaderFocusable()]
        return isAlive && node.isAccessibilityElement
    }

    override fun accessibilityIdentifier(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityIdentifier) {
            node.accessibilityIdentifier
        }

    override fun accessibilityHint(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityHint) {
            node.accessibilityHint
        }

    override fun accessibilityCustomActions(): List<UIAccessibilityCustomAction> =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityCustomActions) {
            node.accessibilityCustomActions
        }

    override fun accessibilityTraits(): UIAccessibilityTraits =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityTraits) {
            node.accessibilityTraits
        }

    override fun accessibilityValue(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityValue) {
            node.accessibilityValue
        }

    override fun accessibilityPerformEscape(): Boolean {
        if (!isAlive) {
            return false
        }

        return if (node.accessibilityPerformEscape()) {
            true
        } else {
            super.accessibilityPerformEscape()
        }
    }

    override fun accessibilityContainerType(): UIAccessibilityContainerType =
        node.accessibilityContainerType

    private fun debugContainmentChain() = debugContainmentChain(this)

    fun debugLog(logger: AccessibilityDebugLogger, depth: Int) {
        val indent = " ".repeat(depth * 2)
        logger.apply {
            log("${indent}${key}")
            log("$indent  isAccessibilityElement: ${isAccessibilityElement()}")
            log("$indent  containmentChain: ${debugContainmentChain()}")
            log("$indent  accessibilityLabel: ${accessibilityLabel()}")
            log("$indent  accessibilityValue: ${accessibilityValue()}")
            log("$indent  accessibilityTraits: ${accessibilityTraits()}")
            log("$indent  accessibilityFrame: ${NSStringFromCGRect(accessibilityFrame())}")
            log("$indent  accessibilityIdentifier: ${accessibilityIdentifier()}")
            log("$indent  accessibilityCustomActions: ${accessibilityCustomActions()}")
        }
    }

    // UIFocusItemProtocol & UIFocusItemContainerProtocol

    override fun canBecomeFocused(): Boolean = isAlive && node.canBecomeFocused

    override fun didUpdateFocusInContext(
        context: UIFocusUpdateContext,
        withAnimationCoordinator: UIFocusAnimationCoordinator
    ) {
        if (!isAlive) {
            return
        }

        if (context.previouslyFocusedItem === this) {
            node.didResignFocused()
        }
        if (context.nextFocusedItem === this) {
            node.didBecomeFocused()
        }
    }

    override fun focusItemContainer(): UIFocusItemContainerProtocol = this

    var focusFrame: CValue<CGRect> = CGRectZero.readValue()
    override fun frame(): CValue<CGRect> = if (USE_HIERARCHICAL_COORDINATE_SPACE) {
        focusFrame
    } else {
        convertRect(rect = bounds(), toCoordinateSpace = mediator.view)
    }

    override fun focusEffectRect(): CValue<CGRect> = convertRect(rect = bounds, toCoordinateSpace = mediator.view)

    override fun bounds(): CValue<CGRect> {
        val offset = contentOffset()
        return CGRectMake(
            x = offset.useContents { x },
            y = offset.useContents { y },
            width = focusFrame.useContents { size.width },
            height = focusFrame.useContents { size.height }
        )
    }

    override fun parentFocusEnvironment(): UIFocusEnvironmentProtocol? =
        accessibilityContainer as? UIFocusEnvironmentProtocol

    override fun preferredFocusEnvironments(): List<*> =
        accessibilityElements?.mapNotNull { it as? UIFocusEnvironmentProtocol } ?: emptyList<Any>()

    private var updateFocusScheduled = false
    override fun setNeedsFocusUpdate() {
        if (updateFocusScheduled) {
            return
        }
        updateFocusScheduled = true
        CoroutineScope(Dispatchers.Main).launch {
            updateFocusIfNeeded()
            updateFocusScheduled = false
        }
    }

    override fun updateFocusIfNeeded() {
        UIFocusSystem.focusSystemForEnvironment(environment = this)?.updateFocusIfNeeded()
    }

    override fun shouldUpdateFocusInContext(context: UIFocusUpdateContext): Boolean = true

    override fun coordinateSpace(): UICoordinateSpaceProtocol =
        if (USE_HIERARCHICAL_COORDINATE_SPACE) {
            this
        } else {
            mediator.view
        }

    override fun focusItemsInRect(rect: CValue<CGRect>): List<*> = accessibilityElements?.filter {
        it is UIFocusItemProtocol && CGRectIntersectsRect(it.frame, rect)
    } ?: emptyList<Any>()

    override fun isTransparentFocusItem(): Boolean = true

    override fun drawsFocusRingWhenChildrenFocused(): Boolean = node.canScroll

    // Scrolling

    override fun visibleSize(): CValue<CGSize> = node.scrollVisibleSize

    override fun contentSize(): CValue<CGSize> = node.scrollContentSize

    override fun contentOffset(): CValue<CGPoint> = node.scrollContentOffset

    override fun setContentOffset(contentOffset: CValue<CGPoint>) {
        val currentContentOffset = contentOffset()
        val delta = CGPointMake(
            x = contentOffset.useContents { x } - currentContentOffset.useContents { x },
            y = contentOffset.useContents { y } - currentContentOffset.useContents { y },
        )

        val motionDurationScale = MotionDurationScaleImpl()
        motionDurationScale.scaleFactor = 0f
        val frameClock = BroadcastFrameClock()

        CoroutineScope(
            context = mediator.coroutineContext + motionDurationScale + frameClock
        ).launch {
            val timerJob = launch {
                while (true) {
                    frameClock.sendFrame(CACurrentMediaTime().toNanoSeconds())
                    delay(1)
                }
            }
            node.scrollBy(delta)
            timerJob.cancel()
        }
    }

    // UICoordinateSpaceProtocol

    @ObjCSignatureOverride
    override fun convertPoint(
        point: CValue<CGPoint>,
        toCoordinateSpace: UICoordinateSpaceProtocol
    ): CValue<CGPoint> {
        val globalPoint = convertPointToGlobal(point)
        return when (toCoordinateSpace) {
            is AccessibilityElement -> toCoordinateSpace.convertPointFromGlobal(globalPoint)
            is UIView -> toCoordinateSpace.convertPoint(globalPoint, fromView = null)
            else -> mediator.view.window!!.convertPoint(globalPoint, toCoordinateSpace = toCoordinateSpace)
        }
    }

    @ObjCSignatureOverride
    override fun convertPoint(
        point: CValue<CGPoint>,
        fromCoordinateSpace: UICoordinateSpaceProtocol
    ): CValue<CGPoint> {
        val globalPoint = when (fromCoordinateSpace) {
            is AccessibilityElement -> fromCoordinateSpace.convertPointToGlobal(point)
            is UIView -> fromCoordinateSpace.convertPoint(point, toView = null)
            else -> mediator.view.window!!.convertPoint(point, fromCoordinateSpace = fromCoordinateSpace)
        }
        return convertPointFromGlobal(globalPoint)
    }

    @ObjCSignatureOverride
    override fun convertRect(
        rect: CValue<CGRect>,
        toCoordinateSpace: UICoordinateSpaceProtocol
    ): CValue<CGRect> {
        val globalRect = convertRectToGlobal(rect)
        return when (toCoordinateSpace) {
            is AccessibilityElement -> toCoordinateSpace.convertRectFromGlobal(globalRect)
            is UIView -> toCoordinateSpace.convertRect(globalRect, fromView = null)
            else -> mediator.view.window!!.convertRect(globalRect, toCoordinateSpace = toCoordinateSpace)
        }
    }

    @ObjCSignatureOverride
    override fun convertRect(
        rect: CValue<CGRect>,
        fromCoordinateSpace: UICoordinateSpaceProtocol
    ): CValue<CGRect> {
        val globalRect = when (fromCoordinateSpace) {
            is AccessibilityElement -> fromCoordinateSpace.convertRectToGlobal(rect)
            is UIView -> fromCoordinateSpace.convertRect(rect, toView = null)
            else -> mediator.view.window!!.convertRect(rect, fromCoordinateSpace = fromCoordinateSpace)
        }
        return convertRectFromGlobal(globalRect)
    }

    private fun convertPointToGlobal(point: CValue<CGPoint>): CValue<CGPoint> {
        var globalPoint = point
        var current: AccessibilityElement? = this
        while (current != null) {
            globalPoint = globalPoint.useContents {
                CGPointMake(
                    x = x + CGRectGetMinX(current.focusFrame) - current.contentOffset().useContents { x },
                    y = y + CGRectGetMinY(current.focusFrame) - current.contentOffset().useContents { y }
                )
            }
            when (val container = current.accessibilityContainer) {
                is AccessibilityElement -> current = container
                is AccessibilityRoot -> return container.mediator.view.convertPoint(globalPoint, toView = null)
                else -> return globalPoint
            }
        }
        return globalPoint
    }

    private fun convertPointFromGlobal(point: CValue<CGPoint>): CValue<CGPoint> {
        fun convertPoint(point: CValue<CGPoint>, element: AccessibilityElement): CValue<CGPoint> {
            val parentPoint = when (val container = element.accessibilityContainer) {
                is AccessibilityElement -> convertPoint(point, container)
                is AccessibilityRoot -> container.mediator.view.convertPoint(point, fromView = null)
                else -> point
            }
            return parentPoint.useContents {
                CGPointMake(
                    y = y - CGRectGetMinY(element.focusFrame) + element.contentOffset().useContents { y },
                    x = x - CGRectGetMinX(element.focusFrame) + element.contentOffset().useContents { x }
                )
            }
        }
        return convertPoint(point, element = this)
    }

    private fun convertRectToGlobal(rect: CValue<CGRect>): CValue<CGRect> {
        var globalRect = rect
        var current: AccessibilityElement? = this
        while (current != null) {
            globalRect = globalRect.useContents {
                CGRectMake(
                    x = origin.x + CGRectGetMinX(current.focusFrame) - current.contentOffset().useContents { x },
                    y = origin.y + CGRectGetMinY(current.focusFrame) - current.contentOffset().useContents { y },
                    width = size.width,
                    height = size.height
                )
            }
            when (val container = current.accessibilityContainer) {
                is AccessibilityElement -> current = container
                is AccessibilityRoot -> return container.mediator.view.convertRect(globalRect, toView = null)
                else -> return globalRect
            }
        }
        return globalRect
    }

    private fun convertRectFromGlobal(rect: CValue<CGRect>): CValue<CGRect> {
        fun convertPoint(rect: CValue<CGRect>, element: AccessibilityElement): CValue<CGRect> {
            val parentPoint = when (val container = element.accessibilityContainer) {
                is AccessibilityElement -> convertPoint(rect, container)
                is AccessibilityRoot -> container.mediator.view.convertRect(rect, fromView = null)
                else -> rect
            }
            return parentPoint.useContents {
                CGRectMake(
                    x = origin.x - CGRectGetMinX(element.focusFrame) + element.contentOffset().useContents { x },
                    y = origin.y - CGRectGetMinY(element.focusFrame) + element.contentOffset().useContents { y },
                    width = size.width,
                    height = size.height
                )
            }
        }
        return convertPoint(rect, element = this)
    }
}

internal class AccessibilityNotification private constructor(
    val notification: UIAccessibilityNotifications,
    val elementToFocus: WeakReference<Any>?,
    val message: String?
) {
    companion object {
        // For testing purposes only
        var lastPostedNotificationForTests: AccessibilityNotification? = null
            private set

        private val notificationsWithFocusedElement = setOf(
            UIAccessibilityScreenChangedNotification,
            UIAccessibilityLayoutChangedNotification
        )
    }

    constructor(
        notification: UIAccessibilityNotifications,
        elementToFocus: Any? = null,
        message: String? = null
    ) : this(notification, elementToFocus?.let { WeakReference(it) }, message)

    fun postNotification() {
        val focusNotification = notification in notificationsWithFocusedElement
        lastPostedNotificationForTests = this
        UIAccessibilityPostNotification(
            notification,
            argument = if (focusNotification) elementToFocus?.value else message
        )
    }
}

/**
 * An interface for logging accessibility debug messages.
 */
internal interface AccessibilityDebugLogger {
    /**
     * Logs the given [message].
     */
    fun log(message: Any?)
}

private val accessibilityDebugLogger: AccessibilityDebugLogger? = null
// Uncomment for debugging:
// private val accessibilityDebugLogger: AccessibilityDebugLogger? =
//     object : AccessibilityDebugLogger {
//         override fun log(message: Any?) {
//             if (message == null) {
//                 println()
//             } else {
//                 println("[a11y]: $message")
//             }
//         }
//     }

private sealed interface AccessibilityElementFocusMode {
    val targetElementKey: AccessibilityElementKey?

    /**
     * Do not change focus. Notifies about content changes.
     */
    data object None : AccessibilityElementFocusMode {
        override val targetElementKey: AccessibilityElementKey? = null
    }

    /**
     * Keeps focus on the element if present, or notify about significant changes on a screen.
     */
    data class KeepFocus(val key: AccessibilityElementKey) : AccessibilityElementFocusMode {
        override val targetElementKey: AccessibilityElementKey = key
    }

    /**
     * Changes focus on the element with the given [key].
     */
    data class Focus(val key: AccessibilityElementKey) : AccessibilityElementFocusMode {
        override val targetElementKey: AccessibilityElementKey = key
    }
}

/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
internal class AccessibilityMediator(
    val view: UIView,
    val owner: SemanticsOwner,
    val coroutineContext: CoroutineContext,
    val performEscape: () -> Boolean,
    val onScreenReaderActive: (Boolean) -> Unit,
) {
    private var focusMode: AccessibilityElementFocusMode = AccessibilityElementFocusMode.None

    var focusedNodesScrollableParentsIds: Set<Int> = setOf()
        private set(value) {
            if (field != value) {
                field = value
                invalidateSemanticsTree()

                if (value.isNotEmpty()) {
                    // Hack to fix an issue where iOS accessibility only reads the items visible
                    // at the moment of the beginning of the "Speak Screen" command.
                    AccessibilityNotification(UIAccessibilityPageScrolledNotification).postNotification()
                }
            }
        }

    var keyboardFocusedElementKey: AccessibilityElementKey? = null
    private var forceFocusedElementKey: AccessibilityElementKey? = null

    /**
     * A set of node ids that had their bounds invalidated after the last sync.
     */
    private val invalidationChannel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    /**
     * Job to cancel tree syncing when the mediator is disposed.
     */
    private val job = Job()

    /**
     * CoroutineScope to launch the tree syncing job on.
     */
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    private val root = AccessibilityRoot(mediator = this)

    /**
     * A map of all [AccessibilityElementKey] currently present in the tree to corresponding
     * [AccessibilityElement].
     */
    private val accessibilityElementsMap =
        mutableMapOf<AccessibilityElementKey, AccessibilityElement>()

    private var updateFocusOnAccessibilityElementsLoaded = false

    var isEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                onSemanticsChange()

                AccessibilityNotification(UIAccessibilityScreenChangedNotification).postNotification()

                updateFocusOnAccessibilityElementsLoaded = value
            }
        }

    val safeAreaRectInWindow: Rect get() {
        val rectInWindow = view.convertRect(
            rect = UIEdgeInsetsInsetRect(view.bounds, view.safeAreaInsets),
            toView = null
        )
        return rectInWindow.toDpRect().toRect(view.density)
    }

    private var displayLinkListener: DisplayLinkListener? = null

    private var scrollJob: Job? = null

    private val focusObserver = AccessibilityFocusedElementObserver { focusedElement ->
        scrollToAccessibilityElement(focusedElement)
        scheduleFocusedScrollableParentsIdsUpdate(focusedElement)
    }

    fun scrollToAccessibilityElement(key: AccessibilityElementKey) {
        val element = accessibilityElementsMap[key] ?: return
        scrollToAccessibilityElement(element)
    }

    private fun scrollToAccessibilityElement(element: NSObject) {
        val listener = displayLinkListener ?: return

        val scrollableContainer = findFirstAccessibilityElementInHierarchy(element) {
            it.node.semanticsNode.canScroll
        }
        if (scrollableContainer != null) {
            scrollJob?.cancel()
            val rect = when {
                element is AccessibilityElement ->
                    element.node.semanticsNode.unclippedBoundsInWindow

                else ->
                    element.accessibilityFrame.toDpRect()
                        .toRect(scrollableContainer.node.semanticsNode.layoutNode.density)
            }
            scrollJob = CoroutineScope(coroutineContext + listener.frameClock).launch {
                scrollableContainer.node.semanticsNode.scrollToCenterRectIfNeeded(
                    targetRect = rect,
                    safeAreaRectInWindow = safeAreaRectInWindow
                )
            }
        }
    }

    fun getAccessibilityElement(key: AccessibilityElementKey): CMPAccessibilityElement? {
        return accessibilityElementsMap[key]
    }

    /**
     * Iterates through the accessibility element hierarchy starting from the top element down to the root.
     * For each element of type `AccessibilityElement` invokes [onElement] callback.
     *
     * @param element The starting point for the iteration in the hierarchy.
     * @param onElement A callback function that is invoked for each `AccessibilityElement` encountered during the iteration.
     * @return Returns `true` if the provided [element] located in the hierarchy of the tree of current [AccessibilityMediator],
     * otherwise returns `false`.
     */
    private fun iterateAccessibilityElementHierarchy(
        element: Any,
        onElement: (AccessibilityElement) -> Unit,
    ): Boolean {
        var iterator: Any? = element
        while (iterator != null) {
            when (iterator) {
                is AccessibilityElement -> {
                    onElement(iterator)
                    iterator = iterator.accessibilityContainer
                }

                is InteropWrappingView -> iterator = iterator.actualAccessibilityContainer
                is AccessibilityRoot -> {
                    // Return true only of the result element has the same root
                    return iterator == root
                }
                is UIView -> iterator = iterator.superview
                is UIAccessibilityElement -> iterator = iterator.accessibilityContainer
                else -> return false
            }
        }
        return false
    }

    private fun findFirstAccessibilityElementInHierarchy(
        element: Any,
        validate: (AccessibilityElement) -> Boolean,
    ): AccessibilityElement? {
        var result: AccessibilityElement? = null
        val isInHierarchy = iterateAccessibilityElementHierarchy(element) {
            if (result == null && validate(it)) {
                result = it
            }
        }
        return result?.takeIf { isInHierarchy }
    }

    init {
        accessibilityDebugLogger?.log("AccessibilityMediator for $view created")

        view.accessibilityElements = listOf(root)
        coroutineScope.launch {
            // The main loop that listens for invalidations and performs the tree syncing
            // Will exit on CancellationException from within await on `invalidationChannel.receive()`
            // when [job] is cancelled
            while (true) {
                hasPendingInvalidations = false
                invalidationChannel.receive()
                hasPendingInvalidations = true

                while (invalidationChannel.tryReceive().isSuccess) {
                    // Do nothing, just consume the channel
                    // Workaround for the channel buffering two invalidations despite the capacity of 1
                }

                if (isEnabled) {
                    if (isAccessibilityActive) {
                        scheduleAccessibilityDisablingAndCleanup()
                        val time = measureTime {
                            sync()
                        }
                        accessibilityDebugLogger?.log("AccessibilityMediator.sync took $time")
                    }
                } else if (root.element != null) {
                    refocusKeyboardElementIfNeeded()
                    root.element = null
                    AccessibilityNotification(UIAccessibilityLayoutChangedNotification).postNotification()
                }

                if (keyboardFocusedElementKey != null) {
                    // Do nothing.
                    // When full keyboard access is enabled, the selection rectangle can be updated
                    // on every frame. To improve the user experience, we should update the
                    // accessibility tree as quickly as possible.
                } else {
                    // Estimated delay between the iOS Accessibility Engine sync intervals.
                    // There is no reason to post change notifications more frequently because the
                    // iOS Accessibility Engine will ignore them.
                    delay(100)
                }
            }
        }
    }

    /**
     * Indicates that accessibility has recently been requested and can be considered active.
     * The flag is set to false if no accessibility tree reads occur for some time.
     */
    private var isAccessibilityActive: Boolean = false

    private var disableAccessibilityJob: Job? = null

    private fun scheduleAccessibilityDisablingAndCleanup() {
        if (disableAccessibilityJob != null ||
            keyboardFocusedElementKey != null ||
            focusMode is AccessibilityElementFocusMode.KeepFocus ||
            hasFocusedElement()) {
            return
        }
        disableAccessibilityJob = coroutineScope.launch {
            // Allow some time for the iOS Accessibility Engine to read the updated accessibility
            // elements tree. If no new reads occur during this time, it is assumed that iOS
            // Accessibility has been disabled and resources can be cleaned up.
            delay(2000)

            cleanUp()
        }
    }

    private fun hasFocusedElement(): Boolean {
        val element = UIAccessibilityFocusedElement(null) ?: return false
        return findFirstAccessibilityElementInHierarchy(element) {
            true
        } != null
    }

    private fun cancelAccessibilityDisabling() {
        disableAccessibilityJob?.cancel()
        disableAccessibilityJob = null
    }

    fun activateAccessibilityIfNeeded() {
        isAccessibilityActive = true
        if (root.element == null) {
            sync()
        }
        cancelAccessibilityDisabling()
    }

    var hasPendingInvalidations: Boolean = false
        private set

    private fun convertToAppWindowCGRect(rect: Rect): CValue<CGRect> {
        return view.convertRect(rect.toDpRect(view.density).toCGRect(), toView = null)
    }

    fun notifyScrollCompleted(
        scrollResult: AccessibilityScrollEventResult,
        delay: Long,
        focusedNode: SemanticsNode,
        focusedRectInWindow: Rect
    ) {
        coroutineScope.launch {
            delay(delay)

            AccessibilityNotification(
                UIAccessibilityPageScrolledNotification,
                message = scrollResult.announceMessage()
            ).postNotification()

            accessibilityDebugLogger?.log("PageScrolled")

            if (accessibilityElementsMap[focusedNode.semanticsKey] == null) {
                val element = findClosestElementToRect(rect = focusedRectInWindow)
                accessibilityDebugLogger?.log("LayoutChanged, result: $element")

                (element as? AccessibilityElement)?.let {
                    focusMode = AccessibilityElementFocusMode.KeepFocus(element.key)
                }

                AccessibilityNotification(UIAccessibilityLayoutChangedNotification, elementToFocus = element).postNotification()
            }
        }
    }

    fun onSemanticsChange() {
        accessibilityDebugLogger?.log("onSemanticsChange")
        invalidateSemanticsTree()
    }

    fun onLayoutChange(nodeId: Int) {
        accessibilityDebugLogger?.log("onLayoutChange (nodeId=$nodeId)")
        invalidateSemanticsTree()
    }

    private fun invalidateSemanticsTree() {
        hasPendingInvalidations = true
        invalidationChannel.trySend(Unit)
    }

    fun dispose() {
        focusObserver.dispose()
        job.cancel()
        disableAccessibilityJob?.cancel()

        refocusKeyboardElementIfNeeded()
        view.accessibilityElements = listOf<NSObject>()

        for (element in accessibilityElementsMap.values) {
            element.dispose()
        }

        cleanUp()
    }

    private fun cleanUp() {
        displayLinkListener?.invalidate()
        displayLinkListener = null

        disableAccessibilityJob = null
        isAccessibilityActive = false

        root.element = null
        accessibilityElementsMap.clear()
    }

    private var focusedScrollableParentsIdsUpdateJob: Job? = null
    private fun scheduleFocusedScrollableParentsIdsUpdate(focusedElement: NSObject) {
        focusedScrollableParentsIdsUpdateJob?.cancel()
        focusedScrollableParentsIdsUpdateJob = coroutineScope.launch {
            // Throttle the recalculation of scrollable parent node IDs to avoid unnecessary
            // reloading of the accessibility tree when the focusMode changes quickly.
            delay(10)
            val scrollableElementsIds = mutableSetOf<Int>()
            val isInHierarchy = iterateAccessibilityElementHierarchy(focusedElement) {
                if (it.node.semanticsNode.canScroll) {
                    scrollableElementsIds.add(it.node.semanticsNode.id)
                }
            }
            if (!isInHierarchy) {
                scrollableElementsIds.clear()
            }
            focusedNodesScrollableParentsIds = scrollableElementsIds
        }
    }

    private fun createOrUpdateAccessibilityElement(
        node: AccessibilityNode,
        container: SemanticsNode,
        children: List<AccessibilityElement> = emptyList(),
        frame: Rect
    ): AccessibilityElement {
        val element = accessibilityElementsMap[node.key]?.also {
            it.update(node = node, children = children)
        } ?: AccessibilityElement(node = node, this, children = children).also {
            accessibilityElementsMap[node.key] = it
        }

        val accessibilityFrame = convertToAppWindowCGRect(frame)
        if (!CGRectEqualToRect(accessibilityFrame, element.accessibilityFrame)) {
            element.setAccessibilityFrame(accessibilityFrame)
        }

        val nodeCoordinator = node.semanticsNode.findCoordinatorToGetBounds()
        val containerCoordinator = container.findCoordinatorToGetBounds()
        var resultFrame = nodeCoordinator?.let {
            containerCoordinator?.localBoundingBoxOf(nodeCoordinator, clipBounds = false)
        } ?: frame

        val dx = container.unmergedConfig
            .getOrNull(SemanticsProperties.HorizontalScrollAxisRange)?.value() ?: 0f
        val dy = container.unmergedConfig
            .getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value() ?: 0f

        resultFrame = resultFrame.translate(dx, dy)

        element.focusFrame = resultFrame.toDpRect(node.semanticsNode.layoutNode.density).toCGRect()

        return element
    }

    /**
     * Traverses semantics tree starting from rootNode and returns an accessibility object which will
     * be put into iOS view's [accessibilityElements] property.
     *
     * Inserts new elements to [accessibilityElementsMap], updates the old ones, and removes the elements
     * that are not present in the tree anymore.
     */
    private fun traverseSemanticsTree(
        rootNode: SemanticsNode
    ): Triple<AccessibilityElement, AccessibilityElementKey?, AccessibilityNotification?> {
        val presentIds = MutableIntSet()
        presentIds.add(rootNode.id)
        val nodes = owner.getAllUncoveredSemanticsNodesToIntObjectMap(
            customRootNodeId = rootNode.id,
            shouldIgnoreNode = { false }
        )
        keyboardFocusedElementKey?.id?.let {
            if (!nodes.contains(it)) {
                // The keyboard-focused node is removed. It's important to trigger focus reload
                // before the node is actually removed from the accessibility elements tree.
                refocusKeyboardElementIfNeeded()
            }
        }
        var focusedKey: AccessibilityElementKey? = null
        var lastLiveRegionAnnouncement: AccessibilityNotification? = null

        // 1. Get accessibility elements and traversal groups.
        // Flattening of accessibility elements is used to:
        // - have the same traversal order as on Android
        // - allow navigation between semantic containers on iOS
        // 2. Split non-visible children beyond bounds to be located go before and after the group
        // of visible semantic children in the accessibility elements tree.
        // See [isBeforeBeyondBoundsItem] for more details.
        fun SemanticsNode.flattenAccessibilityChildren(
            node: SemanticsNode,
            semanticsChildren: ArrayList<SemanticsNode>,
            beforeBeyondBoundsChildren: ArrayList<SemanticsNode>,
            afterBeyondBoundsChildren: ArrayList<SemanticsNode>,
            collectOnlyAccessibilityElements: Boolean = false,
            flatten: Boolean
        ) {
            node.replacedChildren.fastForEach { child ->
                if (presentIds.contains(child.id)) {
                    return@fastForEach
                }

                val canBeAccessibilityElement = child.canBeAccessibilityElement()
                if (child.isValid && (!collectOnlyAccessibilityElements || canBeAccessibilityElement)) {
                    presentIds.add(child.id)
                    if (nodes.contains(child.id)) {
                        semanticsChildren.add(child)
                    } else if (child.size != IntSize.Zero && (child.isScreenReaderFocusable() ||
                            child.unmergedConfig.contains(NativeAccessibilityViewSemanticsKey))
                    ) {
                        if (child.isBeforeBeyondBoundsItem(container = this)) {
                            beforeBeyondBoundsChildren.add(child)
                        } else {
                            afterBeyondBoundsChildren.add(child)
                        }
                    }
                }
                if (!child.isTraversalGroup && flatten) {
                    flattenAccessibilityChildren(
                        child,
                        semanticsChildren,
                        beforeBeyondBoundsChildren,
                        afterBeyondBoundsChildren,
                        collectOnlyAccessibilityElements || child.unmergedConfig.isMergingSemanticsOfDescendants,
                        flatten
                    )
                }
            }
        }

        fun traverseChildren(
            node: SemanticsNode,
            container: SemanticsNode,
            isBeyondBounds: Boolean,
            flatten: Boolean
        ): AccessibilityElement {
            val frame = nodes[node.id]?.adjustedBounds?.toRect() ?: node.unclippedBoundsInWindow

            if (node.unmergedConfig.getOrNull(SemanticsProperties.Focused) == true) {
                focusedKey = node.semanticsKey
            }

            fun makeSemanticsNode(children: List<AccessibilityElement>): AccessibilityElement {
                val isLiveRegion = node.unmergedConfig.contains(SemanticsProperties.LiveRegion)
                val (oldLabel, oldValue) = if (isLiveRegion) {
                    val element = accessibilityElementsMap[node.semanticsKey]
                    element?.accessibilityLabel() to element?.accessibilityValue()
                } else {
                    Pair(null, null)
                }

                val element = createOrUpdateAccessibilityElement(
                    node = AccessibilityNode.Semantics(
                        semanticsNode = node,
                        mediator = this,
                        isBeyondBounds = isBeyondBounds,
                    ),
                    container = container,
                    children = children,
                    frame = frame
                )

                if (isLiveRegion) {
                    val newLabel = element.accessibilityLabel()
                    val newValue = element.accessibilityValue()

                    if ((newLabel != null || newValue != null) &&
                        (oldLabel != newLabel || oldValue != newValue)
                    ) {
                        val announcement = listOfNotNull(newLabel, newValue).joinToString(", ")
                        lastLiveRegionAnnouncement = AccessibilityNotification(
                            UIAccessibilityAnnouncementNotification,
                            message = announcement
                        )
                    }
                }
                return element
            }

            val flattenChildren = flatten && !node.canBeAccessibilityElement()
            return if (node.isTraversalGroup || node.id == rootNode.id || !flattenChildren) {
                val visibleChildren = ArrayList<SemanticsNode>()
                val beforeChildren = ArrayList<SemanticsNode>()
                val afterChildren = ArrayList<SemanticsNode>()

                node.flattenAccessibilityChildren(
                    node = node,
                    semanticsChildren = visibleChildren,
                    beforeBeyondBoundsChildren = beforeChildren,
                    afterBeyondBoundsChildren = afterChildren,
                    flatten = flattenChildren
                )

                val sortedChildren = node.sortFlattenChildren(visibleChildren)
                beforeChildren.sortWith(BeyondBoundsComparator(node.isRTL))
                afterChildren.sortWith(BeyondBoundsComparator(node.isRTL))

                val visibleElements = sortedChildren.map {
                    traverseChildren(it, isBeyondBounds = isBeyondBounds, flatten = flattenChildren, container = node)
                }
                val beforeElements = beforeChildren.map {
                    traverseChildren(it, isBeyondBounds = true, flatten = flattenChildren, container = node)
                }
                val afterElements = afterChildren.map {
                    traverseChildren(it, isBeyondBounds = true, flatten = flattenChildren, container = node)
                }

                if (node.isTraversalGroup || node.id == rootNode.id) {
                    if (node.canBeAccessibilityElement()) {
                        val containerElement = listOf(makeSemanticsNode(emptyList()))
                        createOrUpdateAccessibilityElement(
                            node = AccessibilityNode.Container(semanticsNode = node),
                            container = container,
                            children = beforeElements + visibleElements + containerElement + afterElements,
                            frame = frame
                        )
                    } else {
                        createOrUpdateAccessibilityElement(
                            node = AccessibilityNode.Semantics(
                                semanticsNode = node,
                                mediator = this,
                                isBeyondBounds = isBeyondBounds
                            ),
                            container = container,
                            children = beforeElements + visibleElements + afterElements,
                            frame = frame
                        )
                    }
                } else {
                    makeSemanticsNode(beforeElements + visibleElements + afterElements)
                }
            } else {
                makeSemanticsNode(emptyList())
            }
        }

        val rootAccessibilityElement = traverseChildren(
            node = rootNode,
            container = rootNode,
            isBeyondBounds = false,
            flatten = true
        )

        // Filter out [AccessibilityElement] in [accessibilityElementsMap] that are not present in the tree anymore
        accessibilityElementsMap.keys.retainAll {
            val isPresent = it.id in presentIds

            if (!isPresent) {
                accessibilityDebugLogger?.log("$it removed")
                checkNotNull(accessibilityElementsMap[it]).dispose()
            }

            isPresent
        }

        return Triple(rootAccessibilityElement, focusedKey, lastLiveRegionAnnouncement)
    }

    /**
     * Performs a complete sync of the accessibility tree with the current semantics tree.
     */
    private fun sync() {
        val rootSemanticsNode = owner.unmergedRootSemanticsNode

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        val (element, focusedElementKey, liveRegionAnnouncement) = traverseSemanticsTree(rootSemanticsNode)
        root.element = element

        if (displayLinkListener == null) {
            displayLinkListener = DisplayLinkListener()
            displayLinkListener?.start()
        }

        accessibilityDebugLogger?.let {
            debugTraverse(it, view)
        }

        if (forceFocusedElementKey != focusedElementKey) {
            forceFocusedElementKey = focusedElementKey
            focusedElementKey?.let {
                focusMode = AccessibilityElementFocusMode.Focus(it)
            }
        }

        val isSemanticsTreeLoaded = accessibilityElementsMap.size > 1
        if (isSemanticsTreeLoaded && updateFocusOnAccessibilityElementsLoaded) {
            view.window?.setNeedsFocusUpdate()
            updateFocusOnAccessibilityElementsLoaded = false
        }

        // Post layout notification first, then the live region announcement (if any) so that
        // the announcement is the last posted notification, as iOS VoiceOver expects.
        updateFocusedElement().postNotification()
        liveRegionAnnouncement?.postNotification()
    }

    private fun updateFocusedElement(): AccessibilityNotification {
        return when (val mode = focusMode) {
            AccessibilityElementFocusMode.None -> {
                AccessibilityNotification(UIAccessibilityLayoutChangedNotification)
            }

            is AccessibilityElementFocusMode.KeepFocus -> {
                val focusedElement = UIAccessibilityFocusedElement(null)
                val element = accessibilityElementsMap[mode.key]?.let {
                    findAccessibilityElementInSemanticsHierarchy(it.node.semanticsNode)
                }
                if (element != null && !CGRectIsEmpty(element.accessibilityFrame())) {
                    AccessibilityNotification(
                        UIAccessibilityLayoutChangedNotification,
                        elementToFocus = element.takeIf { it !== focusedElement }
                    )
                } else if (focusedElement is AccessibilityElement) {
                    val newFocusedElement = root.element?.let { findChildAccessibilityElement(it) }

                    focusMode = if (newFocusedElement is AccessibilityElement) {
                        AccessibilityElementFocusMode.KeepFocus(newFocusedElement.key)
                    } else {
                        AccessibilityElementFocusMode.None
                    }

                    AccessibilityNotification(
                        UIAccessibilityScreenChangedNotification,
                        elementToFocus = newFocusedElement
                    )
                } else {
                    AccessibilityNotification(UIAccessibilityLayoutChangedNotification)
                }
            }

            is AccessibilityElementFocusMode.Focus -> {
                val element = accessibilityElementsMap[mode.key]?.let {
                    findAccessibilityElementInSemanticsHierarchy(it.node.semanticsNode)
                }
                if (element != null && !CGRectIsEmpty(element.accessibilityFrame())) {
                    focusMode = AccessibilityElementFocusMode.KeepFocus(mode.key)
                    AccessibilityNotification(
                        UIAccessibilityLayoutChangedNotification,
                        elementToFocus = element
                    )
                } else {
                    focusMode = AccessibilityElementFocusMode.None
                    AccessibilityNotification(UIAccessibilityLayoutChangedNotification)
                }
            }
        }
    }

    private fun findClosestElementToRect(rect: Rect): Any? {
        val windowRect = convertToAppWindowCGRect(rect)
        val centerPoint = CGPointMake(
            x = CGRectGetMidX(windowRect),
            y = CGRectGetMidY(windowRect)
        )

        var closestElement: Pair<Double, NSObject>? = null

        fun findElement(element: NSObject, point: CValue<CGPoint>): Any? {
            if (element.isAccessibilityElement) {
                val distanceSQ = minimalDistanceSQ(point, element.accessibilityFrame)
                if (distanceSQ == 0.0) {
                    return element
                } else if (closestElement == null || distanceSQ < closestElement!!.first) {
                    closestElement = distanceSQ to element
                }
            }

            element.accessibilityElements?.takeIf { it.isNotEmpty() }?.forEach { element ->
                findElement(element as NSObject, point)?.let {
                    return it
                }
            } ?: repeat(element.accessibilityElementCount().toInt()) { index ->
                element.accessibilityElementAtIndex(index.toLong())?.let { element ->
                    findElement(element as NSObject, point)?.let {
                        return it
                    }
                }
            }

            return null
        }

        findElement(root as NSObject, centerPoint)

        return closestElement?.second
    }

    /**
     * Calculates the squared minimal Euclidean distance between a point and the nearest point on
     * the boundary of a rectangle.
     */
    private fun minimalDistanceSQ(point: CValue<CGPoint>, rect: CValue<CGRect>): Double {
        // Clamp the point to the nearest point on the rectangle
        val clampedX = min(max(point.useContents { x }, CGRectGetMinX(rect)), CGRectGetMaxX(rect))
        val clampedY = min(max(point.useContents { y }, CGRectGetMinY(rect)), CGRectGetMaxY(rect))

        // Return the Euclidean distance between the `point` and the nearest point on the edge
        val dx = clampedX - point.useContents { x }
        val dy = clampedY - point.useContents { y }
        return dx * dx + dy * dy
    }

    fun setFocusTarget(key: AccessibilityElementKey) {
        focusMode = AccessibilityElementFocusMode.KeepFocus(key)
    }

    fun clearFocusTargetIfNeeded(key: AccessibilityElementKey) {
        if (focusMode.targetElementKey == key) {
            focusMode = AccessibilityElementFocusMode.None
        }
    }

    /**
     * Because the AccessibilityElement tree is mostly flattened, we need to traverse the original
     * semantics nodes hierarchy to find the corresponding element to focus inside the focused
     * semantics node.
     */
    private fun findAccessibilityElementInSemanticsHierarchy(semanticsNode: SemanticsNode): NSObject? {
        accessibilityElementsMap[semanticsNode.semanticsKey]
            ?.let { findChildAccessibilityElement(it) }
            ?.let { return it }

        semanticsNode.children.forEach { child ->
            findAccessibilityElementInSemanticsHierarchy(semanticsNode = child)?.let { return it }
        }

        return null
    }

    private fun findChildAccessibilityElement(node: Any): NSObject? {
        val nsNode = node as NSObject
        if (nsNode.isAccessibilityElement) {
            return nsNode
        }
        nsNode.accessibilityElements?.takeIf { it.isNotEmpty() }?.firstNotNullOfOrNull {
            findChildAccessibilityElement(it as Any)
        }?.let {
            return it
        }

        repeat(node.accessibilityElementCount().toInt()) { index ->
            node.accessibilityElementAtIndex(index.toLong())?.let {
                findChildAccessibilityElement(it)
            }?.let {
                return it
            }
        }
        return null
    }

    private fun refocusKeyboardElementIfNeeded() {
        if (keyboardFocusedElementKey != null) {
            view.window?.let {
                UIFocusSystem.focusSystemForEnvironment(it)?.requestFocusUpdateToEnvironment(it)
            }
            keyboardFocusedElementKey = null
        }
    }
}

/**
 * Traverse the accessibility tree starting from [accessibilityObject] using the same(assumed) logic
 * as iOS Accessibility services, and prints its debug data.
 */
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

            accessibilityObject.accessibilityElements?.takeIf { it.isNotEmpty() }?.forEach {
                debugTraverse(debugLogger, it as Any, depth + 1)
            } ?: repeat(accessibilityObject.accessibilityElementCount().toInt()) { index ->
                accessibilityObject.accessibilityElementAtIndex(index.toLong())?.let { element ->
                    debugTraverse(debugLogger, element, depth + 1)
                }
            }
        }

        is AccessibilityRoot -> {
            debugLogger.log("${indent}Root")
            accessibilityObject.element?.let {
                debugTraverse(debugLogger, it, depth + 1)
            }
        }

        else -> {
            throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
        }
    }
}

private fun debugContainmentChain(accessibilityObject: Any): String {
    val strings = mutableListOf<String>()

    var currentObject = accessibilityObject as Any?

    while (currentObject != null) {
        when (val constCurrentObject = currentObject) {
            is AccessibilityElement -> {
                strings.add(constCurrentObject.key.toString())
                currentObject = constCurrentObject.accessibilityContainer
            }

            is AccessibilityRoot -> {
                strings.add("Root")
                currentObject = constCurrentObject.accessibilityContainer
            }

            is UIView -> {
                strings.add("View")
                currentObject = null
            }

            else -> {
                throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
            }
        }
    }

    return strings.joinToString(" -> ")
}

private val SemanticsNode.semanticsKey get() = AccessibilityElementKey.Semantics(id)
private val SemanticsNode.containerKey get() = AccessibilityElementKey.Container(id)

/**
 * Returns true if corresponding [LayoutNode] is placed and attached, false otherwise.
 */
private val SemanticsNode.isValid: Boolean
    get() = layoutNode.isPlaced && layoutNode.isAttached

private val SemanticsNode.isTraversalGroup: Boolean
    get() = unmergedConfig.getOrElse(SemanticsProperties.IsTraversalGroup) { false }

/**
 * Divides semantics beyond bounds children to be located before and after the block of visible
 * semantics children, based on the assumption that `before children` are located above and to the
 * left (to the right for RTL layout) of the centre of the parent node.
 * This rule corresponds to the way the iOS accessibility engine traverses elements on the screen.
 */
private fun SemanticsNode.isBeforeBeyondBoundsItem(container: SemanticsNode): Boolean {
    var centerOffset = container.unclippedBoundsInWindow.center - unclippedBoundsInWindow.center
    if (!container.isRTL) {
        centerOffset = centerOffset.copy(x = -centerOffset.x)
    }

    return centerOffset.x < centerOffset.y
}

/**
 * Simplified version of [SemanticsNode.sortByGeometryGroupings] based on the
 * [SemanticsNode.unclippedBoundsInWindow] because [SemanticsNode.boundsInWindow] is empty for
 * nodes beyond visible bounds.
 */
private class BeyondBoundsComparator(private val isRTL: Boolean) : Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int {
        var result = a.unmergedConfig
            .getOrElse(SemanticsProperties.TraversalIndex) { 0f }
            .compareTo(b.unmergedConfig.getOrElse(SemanticsProperties.TraversalIndex) { 0f })

        if (result != 0) {
            return result
        }

        result = a.unclippedBoundsInWindow.center.y
            .compareTo(b.unclippedBoundsInWindow.center.y)

        if (result != 0) {
            return result
        }

        result = a.unclippedBoundsInWindow.center.x
            .compareTo(b.unclippedBoundsInWindow.center.x)

        if (result != 0) {
            return if (isRTL) -result else result
        }

        return result
    }
}

private class AccessibilityFocusedElementObserver(
    private val onElementFocused: (NSObject) -> Unit
): NSObject() {
    init {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::onFocus.name + ":"),
            name = UIAccessibilityElementFocusedNotification,
            `object` = null
        )
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    private fun onFocus(arg: NSNotification) {
        val accessibilityElement = arg.userInfo?.get(UIAccessibilityFocusedElementKey) as? NSObject
        accessibilityElement?.let { onElementFocused(it) }
    }

    fun dispose() {
        NSNotificationCenter.defaultCenter.removeObserver(this)
    }
}

private fun AccessibilityElement.makeAccessibilityLabel(): String? {
    val contentDescription = if (node.shouldMergeDescription) {
        val collector = NodeDescriptionCollector()
        collectContentDescription(collector)
        collector.getText().takeIf { it.isNotBlank() }
    } else {
        null
    }

    return contentDescription ?: node.contentDescription ?: node.semanticsNode.linkText()
}

/**
 * Mimics the behavior of the 'NodeDescription' and 'LeafTextCollector' of the TalkBack application.
 * Rather than using merged node semantics, TalkBack navigates through the node hierarchy and
 * generates a description of the child nodes independently.
 */
private class NodeDescriptionCollector {
    companion object {
        private const val MAX_TEXT_COLLECT_NODES = 5
    }
    private val text = StringBuilder()
    private var numNodes = 0

    fun collect(node: AccessibilityElement): Boolean {
        if (numNodes >= MAX_TEXT_COLLECT_NODES) {
            return false
        }
        node.node.contentDescription
            ?.takeIf { it.isNotBlank() }
            ?.let {
                numNodes++
                if (text.isNotEmpty()) {
                    text.append(", ")
                }
                text.append(it)
            }

        return true
    }

    fun getText(): String {
        return text.toString()
    }
}

private fun AccessibilityElement.collectContentDescription(collector: NodeDescriptionCollector): Boolean {
    if (!collector.collect(this)) {
        return false
    }
    for (element in accessibilityElements ?: emptyList<AccessibilityElement>()) {
        if (element is AccessibilityElement) {
            if (!element.collectContentDescription(collector)) {
                return false
            }
        }
    }
    return true
}
