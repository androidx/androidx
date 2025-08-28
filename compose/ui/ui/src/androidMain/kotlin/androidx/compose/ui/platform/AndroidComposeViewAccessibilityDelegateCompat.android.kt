/*
 * Copyright 2020 The Android Open Source Project
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

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import android.graphics.Region
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.SystemClock
import android.text.SpannableString
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.collection.ArraySet
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.MutableObjectIntMap
import androidx.collection.SparseArrayCompat
import androidx.collection.intListOf
import androidx.collection.intObjectMapOf
import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableIntSetOf
import androidx.collection.mutableObjectIntMapOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.R
import androidx.compose.ui.contentcapture.ContentCaptureManager
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.platform.accessibility.hasCollectionInfo
import androidx.compose.ui.platform.accessibility.setCollectionInfo
import androidx.compose.ui.platform.accessibility.setCollectionItemInfo
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.Role.Companion.Carousel
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.CustomActions
import androidx.compose.ui.semantics.SemanticsActions.PageDown
import androidx.compose.ui.semantics.SemanticsActions.PageLeft
import androidx.compose.ui.semantics.SemanticsActions.PageRight
import androidx.compose.ui.semantics.SemanticsActions.PageUp
import androidx.compose.ui.semantics.SemanticsActions.RequestFocus
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsNodeWithAdjustedBounds
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.IsSensitiveData
import androidx.compose.ui.semantics.SemanticsPropertiesAndroid
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.getAllUncoveredSemanticsNodesToIntObjectMap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.isHidden
import androidx.compose.ui.semantics.isImportantForAccessibility
import androidx.compose.ui.semantics.subtreeSortedByGeometryGrouping
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.URLSpanCache
import androidx.compose.ui.text.platform.toAccessibilitySpannableString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
import androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityManagerCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.FOCUS_ACCESSIBILITY
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.FOCUS_INPUT
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.lifecycle.Lifecycle
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

private fun LayoutNode.findClosestParentNode(selector: (LayoutNode) -> Boolean): LayoutNode? {
    var currentParent = this.parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

@Suppress("NullAnnotationGroup")
@OptIn(InternalTextApi::class)
internal class AndroidComposeViewAccessibilityDelegateCompat(val view: AndroidComposeView) :
    AccessibilityDelegateCompat(),
    OnAttachStateChangeListener,
    AccessibilityStateChangeListener,
    TouchExplorationStateChangeListener {
    @Suppress("ConstPropertyName")
    companion object {
        /** Virtual node identifier value for invalid nodes. */
        const val InvalidId = Integer.MIN_VALUE
        const val ClassName = "android.view.View"
        const val TextFieldClassName = "android.widget.EditText"
        const val TextClassName = "android.widget.TextView"
        const val LogTag = "AccessibilityDelegate"
        const val ExtraDataTestTagKey = "androidx.compose.ui.semantics.testTag"
        const val ExtraDataIdKey = "androidx.compose.ui.semantics.id"
        const val ExtraDataShapeTypeKey = "androidx.compose.ui.semantics.shapeType"
        const val ExtraDataShapeTypeRectangle = 0
        const val ExtraDataShapeTypeRounded = 1
        const val ExtraDataShapeTypeGeneric = 2
        const val ExtraDataShapeRectKey = "androidx.compose.ui.semantics.shapeRect"
        const val ExtraDataShapeRectCornersKey = "androidx.compose.ui.semantics.shapeCorners"
        const val ExtraDataShapeRegionKey = "androidx.compose.ui.semantics.shapeRegion"

        /**
         * Intent size limitations prevent sending over a megabyte of data. Limit text length to
         * 100K characters - 200KB.
         */
        const val ParcelSafeTextLength = 100000

        /** The undefined cursor position. */
        const val AccessibilityCursorPositionUndefined = -1

        // 20 is taken from AbsSeekbar.java.
        const val AccessibilitySliderStepsCount = 20

        /**
         * Timeout to determine whether a text selection changed event and the pending text
         * traversed event could be resulted from the same traverse action.
         */
        const val TextTraversedEventTimeoutMillis: Long = 1000
        private val AccessibilityActionsResourceIds =
            intListOf(
                R.id.accessibility_custom_action_0,
                R.id.accessibility_custom_action_1,
                R.id.accessibility_custom_action_2,
                R.id.accessibility_custom_action_3,
                R.id.accessibility_custom_action_4,
                R.id.accessibility_custom_action_5,
                R.id.accessibility_custom_action_6,
                R.id.accessibility_custom_action_7,
                R.id.accessibility_custom_action_8,
                R.id.accessibility_custom_action_9,
                R.id.accessibility_custom_action_10,
                R.id.accessibility_custom_action_11,
                R.id.accessibility_custom_action_12,
                R.id.accessibility_custom_action_13,
                R.id.accessibility_custom_action_14,
                R.id.accessibility_custom_action_15,
                R.id.accessibility_custom_action_16,
                R.id.accessibility_custom_action_17,
                R.id.accessibility_custom_action_18,
                R.id.accessibility_custom_action_19,
                R.id.accessibility_custom_action_20,
                R.id.accessibility_custom_action_21,
                R.id.accessibility_custom_action_22,
                R.id.accessibility_custom_action_23,
                R.id.accessibility_custom_action_24,
                R.id.accessibility_custom_action_25,
                R.id.accessibility_custom_action_26,
                R.id.accessibility_custom_action_27,
                R.id.accessibility_custom_action_28,
                R.id.accessibility_custom_action_29,
                R.id.accessibility_custom_action_30,
                R.id.accessibility_custom_action_31,
            )
    }

    // TODO(b/272068594): The current tests assert whether this variable was set. We should instead
    //  assert the behavior that is affected based on the value set here. (Eg, If we have a
    //  previously hovered item, we send a hover exit event when a new item is hovered).
    /** Virtual view id for the currently hovered logical item. */
    @VisibleForTesting internal var hoveredVirtualViewId = InvalidId

    // We could use UiAutomation.OnAccessibilityEventListener, but the tests were
    // flaky, so we use this callback to test accessibility events.
    @VisibleForTesting
    internal var onSendAccessibilityEvent: (AccessibilityEvent) -> Boolean = {
        view.parent.requestSendAccessibilityEvent(view, it)
    }

    private val accessibilityManager: AccessibilityManager =
        view.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    internal var accessibilityForceEnabledForTesting = false
        set(value) {
            field = value
            currentSemanticsNodesInvalidated = true
        }

    /**
     * Delay before dispatching a recurring accessibility event in milliseconds. This delay
     * guarantees that a recurring event will be send at most once during the
     * [SendRecurringAccessibilityEventsIntervalMillis] time frame.
     */
    internal var SendRecurringAccessibilityEventsIntervalMillis = 100L

    private var _enabledServices: List<AccessibilityServiceInfo>? = null

    private fun resetEnabledAccessibilityServiceList() {
        _enabledServices = null
    }

    private val enabledServices: List<AccessibilityServiceInfo>
        get() =
            _enabledServices
                ?: accessibilityManager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK).also {
                    _enabledServices = it
                }

    /**
     * True if any accessibility service enabled in the system, except the UIAutomator (as it
     * doesn't appear in the list of enabled services)
     */
    internal val isEnabled: Boolean
        get() =
            accessibilityForceEnabledForTesting ||
                // checking the list allows us to filter out the UIAutomator which doesn't appear in
                // it
                (accessibilityManager.isEnabled && enabledServices.isNotEmpty())

    /**
     * True if accessibility service with the touch exploration (e.g. Talkback) is enabled in the
     * system. Note that UIAutomator doesn't request touch exploration therefore returns false
     */
    private val isTouchExplorationEnabled
        get() =
            accessibilityForceEnabledForTesting ||
                (accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled)

    internal var requestFromAccessibilityToolForTesting: Boolean? = null
    private val handler = Handler(Looper.getMainLooper())
    private var nodeProvider = ComposeAccessibilityNodeProvider()

    private var accessibilityFocusedVirtualViewId = InvalidId
    private var focusedVirtualViewId = InvalidId
    private var currentlyAccessibilityFocusedANI: AccessibilityNodeInfoCompat? = null
    private var currentlyFocusedANI: AccessibilityNodeInfoCompat? = null
    private var sendingFocusAffectingEvent = false
    private val pendingHorizontalScrollEvents = MutableIntObjectMap<ScrollAxisRange>()
    private val pendingVerticalScrollEvents = MutableIntObjectMap<ScrollAxisRange>()

    // For actionIdToId and labelToActionId, the keys are the virtualViewIds. The value of
    // actionIdToLabel holds assigned custom action id to custom action label mapping. The
    // value of labelToActionId holds custom action label to assigned custom action id mapping.
    private var actionIdToLabel = SparseArrayCompat<SparseArrayCompat<CharSequence>>()
    private var labelToActionId = SparseArrayCompat<MutableObjectIntMap<CharSequence>>()
    private var accessibilityCursorPosition = AccessibilityCursorPositionUndefined

    // We hold this node id to reset the [accessibilityCursorPosition] to undefined when
    // traversal with granularity switches to the next node
    private var previousTraversedNode: Int? = null
    private val subtreeChangedLayoutNodes = ArraySet<LayoutNode>()
    private val boundsUpdateChannel = Channel<Unit>(1)
    private var currentSemanticsNodesInvalidated = true

    private class PendingTextTraversedEvent(
        val node: SemanticsNode,
        val action: Int,
        val granularity: Int,
        val fromIndex: Int,
        val toIndex: Int,
        val traverseTime: Long,
    )

    private var pendingTextTraversedEvent: PendingTextTraversedEvent? = null

    /**
     * Up to date semantics nodes in pruned semantics tree. It always reflects the current semantics
     * tree. They key is the virtual view id(the root node has a key of
     * AccessibilityNodeProviderCompat.HOST_VIEW_ID and other node has a key of its id).
     */
    private var currentSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds> =
        intObjectMapOf()
        get() {
            if (currentSemanticsNodesInvalidated) { // first instance of retrieving all nodes
                currentSemanticsNodesInvalidated = false
                field =
                    view.semanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(
                        customRootNodeId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                    )
                if (isEnabled) {
                    setTraversalValues(field, idToBeforeMap, idToAfterMap, view.context.resources)
                }
            }
            return field
        }

    private var paneDisplayed = MutableIntSet()

    internal var idToBeforeMap = MutableIntIntMap()
    internal var idToAfterMap = MutableIntIntMap()
    internal val ExtraDataTestTraversalBeforeVal =
        @Suppress("SpellCheckingInspection")
        "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"
    internal val ExtraDataTestTraversalAfterVal =
        @Suppress("SpellCheckingInspection")
        "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALAFTER_VAL"

    private val urlSpanCache = URLSpanCache()

    // previousSemanticsNodes holds the previous pruned semantics tree so that we can compare the
    // current and previous trees in onSemanticsChange(). We use SemanticsNodeCopy here because
    // SemanticsNode's children are dynamically generated and always reflect the current children.
    // We need to keep a copy of its old structure for comparison.
    private var previousSemanticsNodes: MutableIntObjectMap<SemanticsNodeCopy> =
        mutableIntObjectMapOf()
    private var previousSemanticsRoot =
        SemanticsNodeCopy(view.semanticsOwner.unmergedRootSemanticsNode, intObjectMapOf())
    private var checkingForSemanticsChanges = false

    // A mapping from semantics node IDs to the local drawing order (among the children of a single
    // parent) of the corresponding layout nodes.
    private val drawingOrder = mutableIntIntMapOf()

    init {
        // Remove callbacks that rely on view being attached to a window when we become
        // detached.
        view.addOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        // Whenever the window is reattached, update the `enabledServices` value in
        // case
        // there have been changes while the window was detached that the listeners
        // might not catch.
        if (accessibilityManager.isEnabled) resetEnabledAccessibilityServiceList()
        accessibilityManager.addAccessibilityStateChangeListener(this)
        accessibilityManager.addTouchExplorationStateChangeListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        handler.removeCallbacks(semanticsChangeChecker)
        accessibilityManager.removeAccessibilityStateChangeListener(this)
        accessibilityManager.removeTouchExplorationStateChangeListener(this)
    }

    override fun onAccessibilityStateChanged(enabled: Boolean) {
        resetEnabledAccessibilityServiceList()
    }

    override fun onTouchExplorationStateChanged(enabled: Boolean) {
        resetEnabledAccessibilityServiceList()
    }

    /**
     * Returns true if there is any semantics node in the tree that can scroll in the given
     * [orientation][vertical] and [direction] at the given [position] in the view associated with
     * this delegate.
     *
     * @param direction The direction to check for scrolling: <0 means scrolling left or up, >0
     *   means scrolling right or down.
     * @param position The position in the view to check in view-local coordinates.
     */
    internal fun canScroll(vertical: Boolean, direction: Int, position: Offset): Boolean {
        // Workaround for access from bg thread, it is not supported by semantics (b/298159434)
        if (Looper.getMainLooper().thread != Thread.currentThread()) {
            return false
        }

        return canScroll(currentSemanticsNodes, vertical, direction, position)
    }

    private fun canScroll(
        currentSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds>,
        vertical: Boolean,
        direction: Int,
        position: Offset,
    ): Boolean {
        // No down event has occurred yet which gives us a location to hit test.
        if (position == Offset.Unspecified || !position.isValid()) return false

        val scrollRangeProperty =
            when (vertical) {
                true -> SemanticsProperties.VerticalScrollAxisRange
                false -> SemanticsProperties.HorizontalScrollAxisRange
            }

        var foundNode = false
        currentSemanticsNodes.forEachValue { node ->
            // Only consider nodes that are under the touch event. Checks the adjusted bounds to
            // avoid overlapping siblings. Because position is a float (touch event can happen in-
            // between pixels), convert the int-based Android Rect to a float-based Compose Rect
            // before doing the comparison.
            if (!node.adjustedBounds.toRect().contains(position)) {
                return@forEachValue
            }

            // Using `unmergedConfig` here is okay since we iterate through all nodes anyway
            val scrollRange =
                node.semanticsNode.unmergedConfig.getOrNull(scrollRangeProperty)
                    ?: return@forEachValue

            // A node simply having scrollable semantics doesn't mean it's necessarily scrollable
            // in the given direction – it must also not be scrolled to its limit in that direction.
            var actualDirection = if (scrollRange.reverseScrolling) -direction else direction
            if (direction == 0 && scrollRange.reverseScrolling) {
                // The View implementation of canScroll* treat zero as a positive direction, so
                // this code should do the same. That means if scrolling is reversed, zero should be
                // a negative direction. The actual number doesn't matter, just its sign.
                actualDirection = -1
            }

            if (actualDirection < 0) {
                if (scrollRange.value() > 0) {
                    foundNode = true
                    return@forEachValue
                }
            } else {
                if (scrollRange.value() < scrollRange.maxValue()) {
                    foundNode = true
                    return@forEachValue
                }
            }
        }
        return foundNode
    }

    private fun isRequestFromAccessibilityTool(): Boolean {
        when (requestFromAccessibilityToolForTesting) {
            true -> return true
            false -> return false
            else ->
                return AccessibilityManagerCompat.isRequestFromAccessibilityTool(
                    accessibilityManager
                )
        }
    }

    private fun createNodeInfo(virtualViewId: Int): AccessibilityNodeInfoCompat? {
        if (
            view.viewTreeOwners?.lifecycleOwner?.lifecycle?.currentState ==
                Lifecycle.State.DESTROYED
        ) {
            return emptyNodeInfoOrNull()
        }
        val semanticsNodeWithAdjustedBounds =
            currentSemanticsNodes[virtualViewId] ?: return emptyNodeInfoOrNull()
        val semanticsNode: SemanticsNode = semanticsNodeWithAdjustedBounds.semanticsNode
        val isSensitiveData = semanticsNode.config.getOrNull(IsSensitiveData) == true
        if (isSensitiveData && !isRequestFromAccessibilityTool()) {
            return null
        }
        val info: AccessibilityNodeInfoCompat = AccessibilityNodeInfoCompat.obtain()
        info.setAccessibilityDataSensitive(isSensitiveData)
        if (virtualViewId == AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
            info.setParent(view.getParentForAccessibility() as? View)
        } else {
            var parentId =
                checkPreconditionNotNull(semanticsNode.parent?.id) {
                    "semanticsNode $virtualViewId has null parent"
                }
            if (parentId == view.semanticsOwner.unmergedRootSemanticsNode.id) {
                parentId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
            }
            info.setParent(view, parentId)
        }
        info.setSource(view, virtualViewId)

        info.setBoundsInScreen(boundsInScreen(semanticsNodeWithAdjustedBounds))

        populateAccessibilityNodeInfoProperties(virtualViewId, info, semanticsNode)

        return info
    }

    /**
     * There are cases when [createNodeInfo] is called when the view is already destroyed or if the
     * semantics node is removed from composition. In this case we return null.
     *
     * But looks like this is causing crash in Assistant. This happens because 1) Assistant falls
     * back to using ANIs until we implement b/393515913 and 2) there's no null check in platform's
     * code until API 35. As a workaround we will return non-null empty ANI for such use cases to
     * avoid the crash.
     *
     * This change should be reverted when b/393515913 is fixed.
     */
    private fun emptyNodeInfoOrNull(): AccessibilityNodeInfoCompat? {
        // Accessibility Manager is not enabled if this code is used by Assistant
        return if (!accessibilityManager.isEnabled) {
            AccessibilityNodeInfoCompat.obtain()
        } else null
    }

    private fun boundsInScreen(node: SemanticsNodeWithAdjustedBounds): AndroidRect {
        val boundsInRoot = node.adjustedBounds
        return toBoundsInScreen(
            left = boundsInRoot.left.toFloat(),
            top = boundsInRoot.top.toFloat(),
            right = boundsInRoot.right.toFloat(),
            bottom = boundsInRoot.bottom.toFloat(),
        )
    }

    private fun toBoundsInScreen(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): AndroidRect {
        val topLeftInScreen: Offset = view.localToScreen(Offset(left, top))
        val bottomRightInScreen = view.localToScreen(Offset(right, bottom))

        // Due to rotation, the top left corner of the local bounds may not be the top left corner
        // of the screen bounds.
        return android.graphics.Rect(
            floor(min(topLeftInScreen.x, bottomRightInScreen.x)).toInt(),
            floor(min(topLeftInScreen.y, bottomRightInScreen.y)).toInt(),
            ceil(max(topLeftInScreen.x, bottomRightInScreen.x)).toInt(),
            ceil(max(topLeftInScreen.y, bottomRightInScreen.y)).toInt(),
        )
    }

    private fun populateAccessibilityNodeInfoProperties(
        virtualViewId: Int,
        info: AccessibilityNodeInfoCompat,
        semanticsNode: SemanticsNode,
    ) {
        val resources = view.context.resources

        // set classname
        info.className = ClassName

        // Set a classname for text nodes before setting a classname based on a role ensuring that
        // the latter if present wins (see b/343392125)
        if (semanticsNode.unmergedConfig.contains(SemanticsProperties.EditableText)) {
            info.className = TextFieldClassName
        }
        if (semanticsNode.unmergedConfig.contains(SemanticsProperties.Text)) {
            info.className = TextClassName
        }
        val role = semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Role)
        role?.let {
            if (semanticsNode.isFake || semanticsNode.replacedChildren.isEmpty()) {
                if (role == Role.Tab) {
                    info.roleDescription = resources.getString(R.string.tab)
                } else if (role == Role.Switch) {
                    info.roleDescription = resources.getString(R.string.switch_role)
                } else {
                    val className = role.toLegacyClassName()
                    // Images are often minor children of larger widgets, so we only want to
                    // announce the Image role when the image itself is focusable.
                    if (
                        role != Role.Image ||
                            semanticsNode.isUnmergedLeafNode ||
                            semanticsNode.unmergedConfig.isMergingSemanticsOfDescendants
                    ) {
                        info.className = className
                    }
                }
            }
        }

        info.packageName = view.context.packageName

        // This property exists to distinguish semantically meaningful nodes from purely structural
        // or decorative UI elements.  Most nodes are considered important, except:
        // * Invisible nodes.
        // * Non-merging nodes with only non-accessibility-speakable properties.
        //     * Of the built-in ones, the key example is testTag.
        //     * Custom SemanticsPropertyKeys defined outside the UI package
        //       are also non-speakable.
        // * Non-merging nodes that are empty: notably, clearAndSetSemantics {}
        //   and the root of the SemanticsNode tree.
        info.isImportantForAccessibility = semanticsNode.isImportantForAccessibility()

        val isRequestFromAccessibilityTool = isRequestFromAccessibilityTool()
        var childDrawingOrder = 0
        semanticsNode.replacedChildren.fastForEach { child ->
            if (currentSemanticsNodes.contains(child.id)) {
                val holder = view.androidViewsHandler.layoutNodeToHolder[child.layoutNode]
                // Do not add children if the ID is not valid.
                if (child.id == View.NO_ID) {
                    return@fastForEach
                }
                if (holder != null) {
                    info.addChild(holder)
                } else {
                    val childHasSensitiveData =
                        currentSemanticsNodes[child.id]
                            ?.semanticsNode
                            ?.config
                            ?.getOrNull(IsSensitiveData) == true
                    // If the child has isSensitiveData=true then the node request must come
                    // from an accessibility tool in order for the child to be included.
                    if (isRequestFromAccessibilityTool || !childHasSensitiveData) {
                        info.addChild(view, child.id)
                    }
                }
                // The children are already ordered by the drawing order at this point.
                drawingOrder.put(child.id, childDrawingOrder)
                childDrawingOrder++
            }
        }

        // Manage internal accessibility focus state.
        if (virtualViewId == accessibilityFocusedVirtualViewId) {
            info.isAccessibilityFocused = true
            info.addAction(AccessibilityActionCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        } else {
            info.isAccessibilityFocused = false
            info.addAction(AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS)
        }

        setText(semanticsNode, info)
        setContentInvalid(semanticsNode, info)
        info.stateDescription = getInfoStateDescriptionOrNull(semanticsNode, resources)
        info.isCheckable = getInfoIsCheckable(semanticsNode)

        val toggleState =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.ToggleableState)
        // TODO(b/406574577): Remove suppression once 1.17.0 stable is released.
        @Suppress("DEPRECATION")
        toggleState?.let {
            if (toggleState == ToggleableState.On) {
                info.isChecked = true
            } else if (toggleState == ToggleableState.Off) {
                info.isChecked = false
            }
        }
        semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Selected)?.let {
            if (role == Role.Tab) {
                // Tab in native android uses selected property
                info.isSelected = it
            } else {
                // TODO(b/406574577): Remove suppression once 1.17.0 stable is released.
                @Suppress("DEPRECATION")
                info.isChecked = it
            }
        }

        if (
            !semanticsNode.unmergedConfig.isMergingSemanticsOfDescendants ||
                // we don't emit fake nodes for nodes without children, therefore we should assign
                // content description for such nodes
                semanticsNode.replacedChildren.isEmpty()
        ) {
            info.contentDescription =
                semanticsNode.unmergedConfig
                    .getOrNull(SemanticsProperties.ContentDescription)
                    ?.firstOrNull()
        }

        // Map testTag to resourceName if testTagsAsResourceId == true (which can be set by an
        // ancestor)
        val testTag = semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.TestTag)
        if (testTag != null) {
            var testTagsAsResourceId = false
            var current: SemanticsNode? = semanticsNode
            while (current != null) {
                if (
                    current.unmergedConfig.contains(SemanticsPropertiesAndroid.TestTagsAsResourceId)
                ) {
                    testTagsAsResourceId =
                        current.unmergedConfig[SemanticsPropertiesAndroid.TestTagsAsResourceId]
                    break
                } else {
                    current = current.parent
                }
            }

            if (testTagsAsResourceId) {
                info.viewIdResourceName = testTag
            }
        }

        semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Heading)?.let {
            info.isHeading = true
        }

        // Drawing order is not applicable for the root node.
        if (virtualViewId != AccessibilityNodeProviderCompat.HOST_VIEW_ID) {
            val drawingOrderForNode = drawingOrder.getOrDefault(semanticsNode.id, -1)
            if (drawingOrderForNode != -1) {
                info.drawingOrder = drawingOrderForNode
            } else {
                Log.w(
                    LogTag,
                    "Drawing order is not available, was AccessibilityNodeInfo requested " +
                        "for a child node before its parent?",
                )
            }
        }

        info.isPassword = semanticsNode.unmergedConfig.contains(SemanticsProperties.Password)
        info.isEditable = semanticsNode.unmergedConfig.contains(SemanticsProperties.IsEditable)
        info.maxTextLength =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.MaxTextLength) ?: -1
        info.isEnabled = semanticsNode.enabled()
        info.isFocusable = semanticsNode.unmergedConfig.contains(SemanticsProperties.Focused)
        if (info.isFocusable) {
            info.isFocused = semanticsNode.unmergedConfig[SemanticsProperties.Focused]
            if (info.isFocused) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS)
                focusedVirtualViewId = virtualViewId
            } else {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_FOCUS)
            }
        }

        // Mark invisible nodes
        info.isVisibleToUser = !semanticsNode.isHidden

        semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.LiveRegion)?.let {
            info.liveRegion =
                when (it) {
                    LiveRegionMode.Polite -> ACCESSIBILITY_LIVE_REGION_POLITE
                    LiveRegionMode.Assertive -> ACCESSIBILITY_LIVE_REGION_ASSERTIVE
                    else -> ACCESSIBILITY_LIVE_REGION_POLITE
                }
        }
        info.isClickable = false
        semanticsNode.unmergedConfig.getOrNull(SemanticsActions.OnClick)?.let {
            // Selectable tabs and radio buttons that are already selected cannot be selected again
            // so they should not be exposed as clickable.
            val isSelected =
                semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Selected) == true
            val isRadioButtonOrTab = role == Role.Tab || role == Role.RadioButton
            info.isClickable = !isRadioButtonOrTab || (isRadioButtonOrTab && !isSelected)
            if (semanticsNode.enabled() && info.isClickable) {
                info.addAction(
                    AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CLICK, it.label)
                )
            }
        }
        info.isLongClickable = false
        semanticsNode.unmergedConfig.getOrNull(SemanticsActions.OnLongClick)?.let {
            info.isLongClickable = true
            if (semanticsNode.enabled()) {
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                        it.label,
                    )
                )
            }
        }

        // The config will contain this action only if there is a text selection at the moment.
        semanticsNode.unmergedConfig.getOrNull(SemanticsActions.CopyText)?.let {
            info.addAction(
                AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_COPY, it.label)
            )
        }
        if (semanticsNode.enabled()) {
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.SetText)?.let {
                info.addAction(
                    AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, it.label)
                )
            }

            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.OnImeAction)?.let {
                info.addAction(
                    AccessibilityActionCompat(android.R.id.accessibilityActionImeEnter, it.label)
                )
            }

            // The config will contain this action only if there is a text selection at the moment.
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.CutText)?.let {
                info.addAction(
                    AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CUT, it.label)
                )
            }

            // The config will contain the action anyway, therefore we check the clipboard text to
            // decide whether to add the action to the node or not.
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.PasteText)?.let {
                if (info.isFocused && view.clipboardManager.hasText()) {
                    info.addAction(
                        AccessibilityActionCompat(
                            AccessibilityNodeInfoCompat.ACTION_PASTE,
                            it.label,
                        )
                    )
                }
            }
        }

        val text = getIterableTextForAccessibility(semanticsNode)
        if (!text.isNullOrEmpty()) {
            info.setTextSelection(
                getAccessibilitySelectionStart(semanticsNode),
                getAccessibilitySelectionEnd(semanticsNode),
            )
            val setSelectionAction =
                semanticsNode.unmergedConfig.getOrNull(SemanticsActions.SetSelection)
            // ACTION_SET_SELECTION should be provided even when SemanticsActions.SetSelection
            // semantics action is not provided by the component
            info.addAction(
                AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
                    setSelectionAction?.label,
                )
            )
            info.addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
            info.addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY)
            info.movementGranularities =
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
            // We only traverse the text when contentDescription is not set.
            val contentDescription =
                semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.ContentDescription)
            if (
                contentDescription.isNullOrEmpty() &&
                    semanticsNode.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult) &&
                    // Talkback does not handle below granularities for text field (which includes
                    // label/hint) when text field is not in focus
                    !semanticsNode.excludeLineAndPageGranularities()
            ) {
                info.movementGranularities =
                    info.movementGranularities or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val extraDataKeys: MutableList<String> = mutableListOf()
            extraDataKeys.add(ExtraDataIdKey)
            if (
                !info.text.isNullOrEmpty() &&
                    semanticsNode.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult)
            ) {
                extraDataKeys.add(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
            }
            if (semanticsNode.unmergedConfig.contains(SemanticsProperties.TestTag)) {
                extraDataKeys.add(ExtraDataTestTagKey)
            }
            if (semanticsNode.unmergedConfig.contains(SemanticsProperties.Shape)) {
                extraDataKeys.add(ExtraDataShapeTypeKey)
                extraDataKeys.add(ExtraDataShapeRectKey)
                extraDataKeys.add(ExtraDataShapeRectCornersKey)
                extraDataKeys.add(ExtraDataShapeRegionKey)
            }

            semanticsNode.unmergedConfig.accessibilityExtraKeys?.forEach { key ->
                key.accessibilityExtraKey?.let { extraDataKeys.add(it) }
            }

            info.availableExtraData = extraDataKeys
        }

        val rangeInfo =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        if (rangeInfo != null) {
            if (semanticsNode.unmergedConfig.contains(SemanticsActions.SetProgress)) {
                info.className = "android.widget.SeekBar"
            } else {
                info.className = "android.widget.ProgressBar"
            }
            if (rangeInfo !== ProgressBarRangeInfo.Indeterminate) {
                info.rangeInfo =
                    AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT,
                        rangeInfo.range.start,
                        rangeInfo.range.endInclusive,
                        rangeInfo.current,
                    )
            }
            if (
                semanticsNode.unmergedConfig.contains(SemanticsActions.SetProgress) &&
                    semanticsNode.enabled()
            ) {
                if (
                    rangeInfo.current <
                        rangeInfo.range.endInclusive.coerceAtLeast(rangeInfo.range.start)
                ) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                }
                if (
                    rangeInfo.current >
                        rangeInfo.range.start.coerceAtMost(rangeInfo.range.endInclusive)
                ) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Api24Impl.addSetProgressAction(info, semanticsNode)
        }

        setCollectionInfo(semanticsNode, info)
        setCollectionItemInfo(semanticsNode, info)

        // Will the scrollable scroll when ACTION_SCROLL_FORWARD is performed?
        fun ScrollAxisRange.canScrollForward(): Boolean {
            return value() < maxValue() && !reverseScrolling || value() > 0f && reverseScrolling
        }

        // Will the scrollable scroll when ACTION_SCROLL_BACKWARD is performed?
        fun ScrollAxisRange.canScrollBackward(): Boolean {
            return value() > 0f && !reverseScrolling || value() < maxValue() && reverseScrolling
        }

        val xScrollState =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
        val scrollAction = semanticsNode.unmergedConfig.getOrNull(SemanticsActions.ScrollBy)
        if (xScrollState != null && scrollAction != null) {
            // Talkback defines SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION, so we need to
            // assign a role for auto scroll to work. Node with collectionInfo resolved by
            // Talkback to ROLE_LIST and supports autoscroll too
            if (!semanticsNode.hasCollectionInfo()) {
                info.className = "android.widget.HorizontalScrollView"
            }
            if (xScrollState.maxValue() > 0f) {
                info.isScrollable = true
            }
            if (semanticsNode.enabled()) {
                if (xScrollState.canScrollForward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                    info.addAction(
                        if (!semanticsNode.isRtl) {
                            AccessibilityActionCompat.ACTION_SCROLL_RIGHT
                        } else {
                            AccessibilityActionCompat.ACTION_SCROLL_LEFT
                        }
                    )
                }
                if (xScrollState.canScrollBackward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                    info.addAction(
                        if (!semanticsNode.isRtl) {
                            AccessibilityActionCompat.ACTION_SCROLL_LEFT
                        } else {
                            AccessibilityActionCompat.ACTION_SCROLL_RIGHT
                        }
                    )
                }
            }
        }
        val yScrollState =
            semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        if (yScrollState != null && scrollAction != null) {
            // Talkback defines SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION, so we need to
            // assign a role for auto scroll to work. Node with collectionInfo resolved by
            // Talkback to ROLE_LIST and supports autoscroll too
            if (!semanticsNode.hasCollectionInfo()) {
                info.className = "android.widget.ScrollView"
            }
            if (yScrollState.maxValue() > 0f) {
                info.isScrollable = true
            }
            if (semanticsNode.enabled()) {
                if (yScrollState.canScrollForward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_DOWN)
                }
                if (yScrollState.canScrollBackward()) {
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
                    info.addAction(AccessibilityActionCompat.ACTION_SCROLL_UP)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.addPageActions(info, semanticsNode)
        }

        info.paneTitle = semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.PaneTitle)

        if (semanticsNode.enabled()) {
            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.Expand)?.let {
                info.addAction(
                    AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_EXPAND, it.label)
                )
            }

            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.Collapse)?.let {
                info.addAction(
                    AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_COLLAPSE, it.label)
                )
            }

            semanticsNode.unmergedConfig.getOrNull(SemanticsActions.Dismiss)?.let {
                info.addAction(
                    AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_DISMISS, it.label)
                )
            }

            if (semanticsNode.unmergedConfig.contains(CustomActions)) {
                val customActions = semanticsNode.unmergedConfig[CustomActions]
                if (customActions.size >= AccessibilityActionsResourceIds.size) {
                    throw IllegalStateException(
                        "Can't have more than " +
                            "${AccessibilityActionsResourceIds.size} custom actions for one widget"
                    )
                }
                val currentActionIdToLabel = SparseArrayCompat<CharSequence>()
                val currentLabelToActionId = mutableObjectIntMapOf<CharSequence>()
                // If this virtual node had custom action id assignment before, we try to keep the
                // id
                // unchanged for the same action (identified by action label). This way, we can
                // minimize the influence of custom action change between custom actions are
                // presented to the user and actually performed.
                if (labelToActionId.containsKey(virtualViewId)) {
                    val oldLabelToActionId = labelToActionId[virtualViewId]
                    val availableIds =
                        mutableIntListOf().apply {
                            AccessibilityActionsResourceIds.forEach { add(it) }
                        }
                    val unassignedActions = mutableListOf<CustomAccessibilityAction>()
                    customActions.fastForEach { action ->
                        if (oldLabelToActionId!!.contains(action.label)) {
                            val actionId = oldLabelToActionId[action.label]
                            currentActionIdToLabel.put(actionId, action.label)
                            currentLabelToActionId[action.label] = actionId
                            availableIds.remove(actionId)
                            info.addAction(AccessibilityActionCompat(actionId, action.label))
                        } else {
                            unassignedActions.add(action)
                        }
                    }
                    unassignedActions.fastForEachIndexed { index, action ->
                        val actionId = availableIds[index]
                        currentActionIdToLabel.put(actionId, action.label)
                        currentLabelToActionId[action.label] = actionId
                        info.addAction(AccessibilityActionCompat(actionId, action.label))
                    }
                } else {
                    customActions.fastForEachIndexed { index, action ->
                        val actionId = AccessibilityActionsResourceIds[index]
                        currentActionIdToLabel.put(actionId, action.label)
                        currentLabelToActionId[action.label] = actionId
                        info.addAction(AccessibilityActionCompat(actionId, action.label))
                    }
                }
                actionIdToLabel.put(virtualViewId, currentActionIdToLabel)
                labelToActionId.put(virtualViewId, currentLabelToActionId)
            }
        }

        info.isScreenReaderFocusable = isScreenReaderFocusable(semanticsNode, resources)

        // `beforeId` refers to the semanticsId that should be read before this `virtualViewId`.
        val beforeId = idToBeforeMap.getOrDefault(virtualViewId, -1)
        if (beforeId != -1) {
            val beforeView = view.androidViewsHandler.semanticsIdToView(beforeId)
            if (beforeView != null) {
                // If the node that should come before this one is a view, we want to pass in the
                // "before" view itself, which is retrieved from our `idToViewMap`.
                info.setTraversalBefore(beforeView)
            } else {
                // Otherwise, we'll set the "before" value by passing in the semanticsId.
                info.setTraversalBefore(view, beforeId)
            }
            addExtraDataToAccessibilityNodeInfoHelper(
                virtualViewId,
                info,
                ExtraDataTestTraversalBeforeVal,
                null,
            )
        }

        val afterId = idToAfterMap.getOrDefault(virtualViewId, -1)
        if (afterId != -1) {
            val afterView = view.androidViewsHandler.semanticsIdToView(afterId)
            // Specially use `traversalAfter` value if the node after is a View,
            // as expressing the order using traversalBefore in this case would require mutating the
            // View itself, which is not under Compose's full control.
            if (afterView != null) {
                info.setTraversalAfter(afterView)
                addExtraDataToAccessibilityNodeInfoHelper(
                    virtualViewId,
                    info,
                    ExtraDataTestTraversalAfterVal,
                    null,
                )
            }
        }

        // set the className provided through the [SemanticsPropertyReceiver.accessibilityClassName]
        // as a last step to ensure it overrides a classname derived from other semantics properties
        semanticsNode.unmergedConfig
            .getOrNull(SemanticsPropertiesAndroid.AccessibilityClassName)
            ?.let { info.className = it }
    }

    /** Set the error text for this node */
    private fun setContentInvalid(node: SemanticsNode, info: AccessibilityNodeInfoCompat) {
        if (node.unmergedConfig.contains(SemanticsProperties.Error)) {
            info.isContentInvalid = true
            info.error = node.unmergedConfig.getOrNull(SemanticsProperties.Error)
        }
    }

    @OptIn(InternalTextApi::class)
    private fun AnnotatedString.toSpannableString(): SpannableString? {
        val fontFamilyResolver: FontFamily.Resolver = view.fontFamilyResolver

        return trimToSize(
            toAccessibilitySpannableString(
                density = view.density,
                fontFamilyResolver = fontFamilyResolver,
                urlSpanCache = urlSpanCache,
            ),
            ParcelSafeTextLength,
        )
    }

    private fun setText(node: SemanticsNode, info: AccessibilityNodeInfoCompat) {
        info.text = getInfoText(node)?.toSpannableString()
    }

    /**
     * Returns whether this virtual view is accessibility focused.
     *
     * @return True if the view is accessibility focused.
     */
    private fun isAccessibilityFocused(virtualViewId: Int): Boolean {
        return (accessibilityFocusedVirtualViewId == virtualViewId)
    }

    /**
     * Attempts to give accessibility focus to a virtual view.
     *
     * <p>
     * A virtual view will not actually take focus if {@link AccessibilityManager#isEnabled()}
     * returns false, {@link AccessibilityManager#isTouchExplorationEnabled()} returns false, or the
     * view already has accessibility focus.
     *
     * @param virtualViewId The id of the virtual view on which to place accessibility focus.
     * @return Whether this virtual view actually took accessibility focus.
     */
    private fun requestAccessibilityFocus(virtualViewId: Int): Boolean {
        if (!isTouchExplorationEnabled) {
            return false
        }
        // TODO: Check virtual view visibility.
        if (!isAccessibilityFocused(virtualViewId)) {
            // Clear focus from the previously focused view, if applicable.
            if (accessibilityFocusedVirtualViewId != InvalidId) {
                sendEventForVirtualView(
                    accessibilityFocusedVirtualViewId,
                    AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
                )
            }

            // Set focus on the new view.
            accessibilityFocusedVirtualViewId = virtualViewId
            // TODO(b/272068594): Do we have to set currentlyFocusedANI object too?

            view.invalidate()
            sendEventForVirtualView(
                virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
            )
            return true
        }
        return false
    }

    /**
     * Populates an event of the specified type with information about an item and attempts to send
     * it up through the view hierarchy.
     *
     * <p>
     * You should call this method after performing a user action that normally fires an
     * accessibility event, such as clicking on an item.
     * <pre>public performItemClick(T item) {
     *   ...
     *   sendEventForVirtualView(item.id, AccessibilityEvent.TYPE_VIEW_CLICKED)
     * }
     * </pre>
     *
     * @param virtualViewId The virtual view id for which to send an event.
     * @param eventType The type of event to send.
     * @param contentChangeType The contentChangeType of this event.
     * @param contentDescription Content description of this event.
     * @return true if the event was sent successfully.
     */
    private fun sendEventForVirtualView(
        virtualViewId: Int,
        eventType: Int,
        contentChangeType: Int? = null,
        contentDescription: List<String>? = null,
    ): Boolean {
        if (virtualViewId == InvalidId || !isEnabled) {
            return false
        }

        val event: AccessibilityEvent = createEvent(virtualViewId, eventType)
        if (contentChangeType != null) {
            event.contentChangeTypes = contentChangeType
        }
        if (contentDescription != null) {
            event.contentDescription = contentDescription.fastJoinToString(",")
        }

        return sendEvent(event)
    }

    /**
     * Send an accessibility event.
     *
     * @param event The accessibility event to send.
     * @return true if the event was sent successfully.
     */
    private fun sendEvent(event: AccessibilityEvent): Boolean {
        // only send an event if there's an enabled service listening for events of this type
        if (!isEnabled) {
            return false
        }

        if (
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        ) {
            sendingFocusAffectingEvent = true
        }
        try {
            return onSendAccessibilityEvent.invoke(event)
        } finally {
            sendingFocusAffectingEvent = false
        }
    }

    /**
     * Constructs and returns an {@link AccessibilityEvent} populated with information about the
     * specified item.
     *
     * @param virtualViewId The virtual view id for the item for which to construct an event.
     * @param eventType The type of event to construct.
     * @return An {@link AccessibilityEvent} populated with information about the specified item.
     */
    @Suppress("DEPRECATION")
    @VisibleForTesting
    private fun createEvent(virtualViewId: Int, eventType: Int): AccessibilityEvent {
        val event: AccessibilityEvent = AccessibilityEvent.obtain(eventType)
        event.isEnabled = true
        // TODO(b/403526104) this might need to also be a proper class name
        event.className = ClassName

        // Don't allow the client to override these properties.
        event.packageName = view.context.packageName
        event.setSource(view, virtualViewId)

        if (isEnabled) {
            // populate additional information from the node
            currentSemanticsNodes[virtualViewId]?.let {
                event.isPassword =
                    it.semanticsNode.unmergedConfig.contains(SemanticsProperties.Password)
                AccessibilityEventCompat.setAccessibilityDataSensitive(
                    event,
                    it.semanticsNode.unmergedConfig.getOrNull(IsSensitiveData) == true,
                )
            }
        }

        return event
    }

    private fun createTextSelectionChangedEvent(
        virtualViewId: Int,
        fromIndex: Int?,
        toIndex: Int?,
        itemCount: Int?,
        text: CharSequence?,
    ): AccessibilityEvent {
        return createEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED)
            .apply {
                fromIndex?.let { this.fromIndex = it }
                toIndex?.let { this.toIndex = it }
                itemCount?.let { this.itemCount = it }
                text?.let { this.text.add(it) }
            }
    }

    /**
     * Attempts to clear accessibility focus from a virtual view.
     *
     * @param virtualViewId The id of the virtual view from which to clear accessibility focus.
     * @return Whether this virtual view actually cleared accessibility focus.
     */
    private fun clearAccessibilityFocus(virtualViewId: Int): Boolean {
        if (isAccessibilityFocused(virtualViewId)) {
            accessibilityFocusedVirtualViewId = InvalidId
            currentlyAccessibilityFocusedANI = null
            view.invalidate()
            sendEventForVirtualView(
                virtualViewId,
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
            )
            return true
        }
        return false
    }

    private fun performActionHelper(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        val node = currentSemanticsNodes[virtualViewId]?.semanticsNode ?: return false

        if (
            node.unmergedConfig.getOrNull(IsSensitiveData) == true &&
                !isRequestFromAccessibilityTool()
        ) {
            return false
        }

        // Actions can be performed when disabled.
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS ->
                return requestAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS ->
                return clearAccessibilityFocus(virtualViewId)
            AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
            AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY -> {
                if (arguments != null) {
                    val granularity =
                        arguments.getInt(
                            AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
                        )
                    val extendSelection =
                        arguments.getBoolean(
                            AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
                        )
                    return traverseAtGranularity(
                        node,
                        granularity,
                        action == AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                        extendSelection,
                    )
                }
                return false
            }
            AccessibilityNodeInfoCompat.ACTION_SET_SELECTION -> {
                val start =
                    arguments?.getInt(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT,
                        -1,
                    ) ?: -1
                val end =
                    arguments?.getInt(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT,
                        -1,
                    ) ?: -1
                // Note: This is a little different from current android framework implementation.
                val success = setAccessibilitySelection(node, start, end, false)
                // Text selection changed event already updates the cache. so this may not be
                // necessary.
                if (success) {
                    sendEventForVirtualView(
                        semanticsNodeIdToAccessibilityVirtualNodeId(node.id),
                        AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED,
                    )
                }
                return success
            }
            AccessibilityNodeInfoCompat.ACTION_COPY -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.CopyText)?.action?.invoke()
                    ?: false
            }
        }

        if (!node.enabled()) {
            return false
        }

        // Actions can't be performed when disabled.
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                val result =
                    node.unmergedConfig.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
                sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                return result ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.OnLongClick)?.action?.invoke()
                    ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD,
            AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD,
            android.R.id.accessibilityActionScrollDown,
            android.R.id.accessibilityActionScrollUp,
            android.R.id.accessibilityActionScrollRight,
            android.R.id.accessibilityActionScrollLeft -> {
                // Introduce a few shorthands:
                val scrollForward = action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
                val scrollBackward = action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
                val scrollLeft = action == android.R.id.accessibilityActionScrollLeft
                val scrollRight = action == android.R.id.accessibilityActionScrollRight
                val scrollUp = action == android.R.id.accessibilityActionScrollUp
                val scrollDown = action == android.R.id.accessibilityActionScrollDown

                val scrollHorizontal = scrollLeft || scrollRight || scrollForward || scrollBackward
                val scrollVertical = scrollUp || scrollDown || scrollForward || scrollBackward

                if (scrollForward || scrollBackward) {
                    val rangeInfo =
                        node.unmergedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
                    val setProgressAction =
                        node.unmergedConfig.getOrNull(SemanticsActions.SetProgress)
                    if (rangeInfo != null && setProgressAction != null) {
                        val max = rangeInfo.range.endInclusive.coerceAtLeast(rangeInfo.range.start)
                        val min = rangeInfo.range.start.coerceAtMost(rangeInfo.range.endInclusive)
                        var increment =
                            if (rangeInfo.steps > 0) {
                                (max - min) / (rangeInfo.steps + 1)
                            } else {
                                (max - min) / AccessibilitySliderStepsCount
                            }
                        if (scrollBackward) {
                            increment = -increment
                        }
                        return setProgressAction.action?.invoke(rangeInfo.current + increment)
                            ?: false
                    }
                }

                // Will the scrollable scroll when ScrollBy is invoked with the given [amount]?
                fun ScrollAxisRange.canScroll(amount: Float): Boolean {
                    return amount < 0 && value() > 0 || amount > 0 && value() < maxValue()
                }

                val fallbackViewport = node.layoutInfo.coordinates.boundsInParent().size
                val activeViewPortForScroll = getScrollViewportLength(node.unmergedConfig)

                // The lint warning text is unstable because anonymous lambdas have an autogenerated
                // name, so suppress this lint warning with @SuppressLint instead of a baseline.
                val scrollAction =
                    node.unmergedConfig.getOrNull(SemanticsActions.ScrollBy) ?: return false

                val xScrollState =
                    node.unmergedConfig.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)

                if (xScrollState != null && scrollHorizontal) {
                    var amountToScroll = (activeViewPortForScroll ?: fallbackViewport.width)

                    if (scrollLeft || scrollBackward) {
                        amountToScroll = -amountToScroll
                    }
                    if (xScrollState.reverseScrolling) {
                        amountToScroll = -amountToScroll
                    }
                    if (node.isRtl && (scrollLeft || scrollRight)) {
                        amountToScroll = -amountToScroll
                    }

                    // normal scrollable vs pageable scrollable. If a node can handle
                    // page actions it will scroll by a full page.
                    if (xScrollState.canScroll(amountToScroll)) {
                        val canPageHorizontally =
                            node.unmergedConfig.contains(PageLeft) ||
                                node.unmergedConfig.contains(PageRight)
                        return if (canPageHorizontally) {
                            val horizontalPageAction =
                                if (amountToScroll > 0) {
                                    node.unmergedConfig.getOrNull(PageRight)
                                } else {
                                    node.unmergedConfig.getOrNull(PageLeft)
                                }
                            horizontalPageAction?.action?.invoke() ?: false
                        } else {
                            scrollAction.action?.invoke(amountToScroll, 0f) ?: false
                        }
                    }
                }

                val yScrollState =
                    node.unmergedConfig.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                if (yScrollState != null && scrollVertical) {
                    var amountToScroll = (activeViewPortForScroll ?: fallbackViewport.height)

                    if (scrollUp || scrollBackward) {
                        amountToScroll = -amountToScroll
                    }
                    if (yScrollState.reverseScrolling) {
                        amountToScroll = -amountToScroll
                    }

                    // normal scrollable vs pageable scrollable. If a node can handle
                    // page actions it will scroll by a full page.
                    if (yScrollState.canScroll(amountToScroll)) {
                        val canPageVertically =
                            node.unmergedConfig.contains(PageUp) ||
                                node.unmergedConfig.contains(PageDown)
                        return if (canPageVertically) {
                            val verticalPageAction =
                                if (amountToScroll > 0) {
                                    node.unmergedConfig.getOrNull(PageDown)
                                } else {
                                    node.unmergedConfig.getOrNull(PageUp)
                                }
                            verticalPageAction?.action?.invoke() ?: false
                        } else {
                            scrollAction.action?.invoke(0f, amountToScroll) ?: false
                        }
                    }
                }

                return false
            }
            android.R.id.accessibilityActionPageUp -> {
                val pageAction = node.unmergedConfig.getOrNull(PageUp)
                return pageAction?.action?.invoke() ?: false
            }
            android.R.id.accessibilityActionPageDown -> {
                val pageAction = node.unmergedConfig.getOrNull(PageDown)
                return pageAction?.action?.invoke() ?: false
            }
            android.R.id.accessibilityActionPageLeft -> {
                val pageAction = node.unmergedConfig.getOrNull(PageLeft)
                return pageAction?.action?.invoke() ?: false
            }
            android.R.id.accessibilityActionPageRight -> {
                val pageAction = node.unmergedConfig.getOrNull(PageRight)
                return pageAction?.action?.invoke() ?: false
            }
            android.R.id.accessibilityActionSetProgress -> {
                if (
                    arguments == null ||
                        !arguments.containsKey(
                            AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE
                        )
                ) {
                    return false
                }
                return node.unmergedConfig
                    .getOrNull(SemanticsActions.SetProgress)
                    ?.action
                    ?.invoke(
                        arguments.getFloat(
                            AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE
                        )
                    ) ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_FOCUS -> {
                // The item might not be focusable in touch mode, so we use requestFocusFromTouch()
                // to exit touch mode before requesting focus (b/387576999).
                // Note that this causes a temporary focus shift to the view if it was not focused,
                // which is then corrected by immediately focusing the desired Composable node.
                // We considered calling super.performAccessibilityAction() which would put the
                // system in keyboard mode, but it only works when AndroidComposeView did not have
                // focus.
                if (view.isInTouchMode) view.requestFocusFromTouch()

                return node.unmergedConfig.getOrNull(RequestFocus)?.action?.invoke() ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS -> {
                return if (node.unmergedConfig.getOrNull(SemanticsProperties.Focused) == true) {
                    view.focusOwner.clearFocus(
                        force = false,
                        refreshFocusEvents = true,
                        clearOwnerFocus = true,
                        focusDirection = Exit,
                    )
                    true
                } else {
                    false
                }
            }
            AccessibilityNodeInfoCompat.ACTION_SET_TEXT -> {
                val text =
                    arguments?.getString(
                        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE
                    )
                return node.unmergedConfig
                    .getOrNull(SemanticsActions.SetText)
                    ?.action
                    ?.invoke(AnnotatedString(text ?: "")) ?: false
            }
            android.R.id.accessibilityActionImeEnter -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.OnImeAction)?.action?.invoke()
                    ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_PASTE -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.PasteText)?.action?.invoke()
                    ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_CUT -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.CutText)?.action?.invoke()
                    ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_EXPAND -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.Expand)?.action?.invoke()
                    ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_COLLAPSE -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.Collapse)?.action?.invoke()
                    ?: false
            }
            AccessibilityNodeInfoCompat.ACTION_DISMISS -> {
                return node.unmergedConfig.getOrNull(SemanticsActions.Dismiss)?.action?.invoke()
                    ?: false
            }
            android.R.id.accessibilityActionShowOnScreen -> {
                // TODO(b/190865803): Consider scrolling nested containers instead of only the first
                // one.
                var scrollableAncestor: SemanticsNode? = node.parent
                var scrollAction =
                    scrollableAncestor?.unmergedConfig?.getOrNull(SemanticsActions.ScrollBy)
                while (scrollableAncestor != null) {
                    if (scrollAction != null) {
                        break
                    }
                    scrollableAncestor = scrollableAncestor.parent
                    scrollAction =
                        scrollableAncestor?.unmergedConfig?.getOrNull(SemanticsActions.ScrollBy)
                }
                if (scrollableAncestor == null) {
                    // there's no scrollable ancestor in the Compose hierarchy, let
                    // AndroidComposeView handle it
                    val rect =
                        node.boundsInRoot.run {
                            android.graphics.Rect(
                                floor(left).toInt(),
                                floor(top).toInt(),
                                ceil(right).roundToInt(),
                                ceil(bottom).roundToInt(),
                            )
                        }
                    return view.requestRectangleOnScreen(rect)
                }

                // TalkBack expects the minimum amount of movement to fully reveal the node.
                // First, get the viewport and the target bounds in root coordinates
                val viewportInParent = scrollableAncestor.layoutInfo.coordinates.boundsInParent()
                val parentInRoot =
                    scrollableAncestor.layoutInfo.coordinates.parentLayoutCoordinates
                        ?.positionInRoot() ?: Offset.Zero
                val viewport = viewportInParent.translate(parentInRoot)
                val target = Rect(node.positionInRoot, node.size.toSize())

                val xScrollState =
                    scrollableAncestor.unmergedConfig.getOrNull(
                        SemanticsProperties.HorizontalScrollAxisRange
                    )
                val yScrollState =
                    scrollableAncestor.unmergedConfig.getOrNull(
                        SemanticsProperties.VerticalScrollAxisRange
                    )

                // Given the desired scroll value to align either side of the target with the
                // viewport, what delta should we go with?
                // If we need to scroll in opposite directions for both sides, don't scroll at all.
                // Otherwise, take the delta that scrolls the least amount.
                fun scrollDelta(a: Float, b: Float): Float =
                    if (sign(a) == sign(b)) if (abs(a) < abs(b)) a else b else 0f

                // Get the desired delta X
                var dx = scrollDelta(target.left - viewport.left, target.right - viewport.right)
                // And adjust for reversing properties
                if (xScrollState?.reverseScrolling == true) dx = -dx
                if (node.isRtl) dx = -dx

                // Get the desired delta Y
                var dy = scrollDelta(target.top - viewport.top, target.bottom - viewport.bottom)
                // And adjust for reversing properties
                if (yScrollState?.reverseScrolling == true) dy = -dy

                return scrollAction?.action?.invoke(dx, dy) == true
            }
            // TODO: handling for other system actions
            else -> {
                val label = actionIdToLabel[virtualViewId]?.get(action) ?: return false
                val customActions = node.unmergedConfig.getOrNull(CustomActions) ?: return false
                customActions.fastForEach { customAction ->
                    if (customAction.label == label) {
                        return customAction.action()
                    }
                }
                return false
            }
        }
    }

    private fun addExtraDataToAccessibilityNodeInfoHelper(
        virtualViewId: Int,
        info: AccessibilityNodeInfoCompat,
        extraDataKey: String,
        arguments: Bundle?,
    ) {
        val node = currentSemanticsNodes[virtualViewId]?.semanticsNode ?: return
        val text = getIterableTextForAccessibility(node)

        // This extra is just for testing: needed a way to retrieve `traversalBefore` and
        // `traversalAfter` from a non-sealed instance of an ANI
        if (extraDataKey == ExtraDataTestTraversalBeforeVal) {
            idToBeforeMap.getOrDefault(virtualViewId, -1).let {
                if (it != -1) {
                    info.extras.putInt(extraDataKey, it)
                }
            }
        } else if (extraDataKey == ExtraDataTestTraversalAfterVal) {
            idToAfterMap.getOrDefault(virtualViewId, -1).let {
                if (it != -1) {
                    info.extras.putInt(extraDataKey, it)
                }
            }
        } else if (
            node.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult) &&
                arguments != null &&
                extraDataKey == EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
        ) {
            val positionInfoStartIndex =
                arguments.getInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, -1)
            val positionInfoLength =
                arguments.getInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, -1)
            if (
                (positionInfoLength <= 0) ||
                    (positionInfoStartIndex < 0) ||
                    (positionInfoStartIndex >= (text?.length ?: Int.MAX_VALUE))
            ) {
                Log.e(LogTag, "Invalid arguments for accessibility character locations")
                return
            }
            val textLayoutResult = getTextLayoutResult(node.unmergedConfig) ?: return
            val boundingRects = mutableListOf<RectF?>()
            for (i in 0 until positionInfoLength) {
                // This is a workaround until we fix the merging issue in b/157474582.
                if (positionInfoStartIndex + i >= textLayoutResult.layoutInput.text.length) {
                    boundingRects.add(null)
                    continue
                }
                val bounds = textLayoutResult.getBoundingBox(positionInfoStartIndex + i)
                val boundsOnScreen = toScreenCoords(node, bounds)
                boundingRects.add(boundsOnScreen)
            }
            info.extras.putParcelableArray(extraDataKey, boundingRects.toTypedArray())
        } else if (
            node.unmergedConfig.contains(SemanticsProperties.TestTag) &&
                arguments != null &&
                extraDataKey == ExtraDataTestTagKey
        ) {
            val testTag = node.unmergedConfig.getOrNull(SemanticsProperties.TestTag)
            if (testTag != null) {
                info.extras.putCharSequence(extraDataKey, testTag)
            }
        } else if (extraDataKey == ExtraDataIdKey) {
            info.extras.putInt(extraDataKey, node.id)
        } else if (extraDataKey == ExtraDataShapeTypeKey) {
            node.unmergedConfig.getOrNull(SemanticsProperties.Shape)?.let { shape ->
                val shapeBounds = getShapeBounds(node, info.boundsInScreen, shape)
                val outline = shape.createOutline(shapeBounds.size, node.layoutInfo.layoutDirection)
                // We set not only the shape type but also the shape data itself, as an
                // optimization, since we already need to create an Outline to get the shape type
                // and to avoid another request for the shape data.
                when (outline) {
                    is Outline.Rectangle -> {
                        info.extras.putInt(ExtraDataShapeTypeKey, ExtraDataShapeTypeRectangle)
                        info.extras.putParcelable(
                            ExtraDataShapeRectKey,
                            outline.toAndroidRect(shapeBounds.left, shapeBounds.top),
                        )
                    }
                    is Outline.Rounded -> {
                        info.extras.putInt(ExtraDataShapeTypeKey, ExtraDataShapeTypeRounded)
                        info.extras.putParcelable(
                            ExtraDataShapeRectKey,
                            outline.toAndroidRect(shapeBounds.left, shapeBounds.top),
                        )
                        info.extras.putFloatArray(
                            ExtraDataShapeRectCornersKey,
                            outline.toCornerArray(),
                        )
                    }
                    is Outline.Generic -> {
                        info.extras.putInt(ExtraDataShapeTypeKey, ExtraDataShapeTypeGeneric)
                        info.extras.putParcelable(
                            ExtraDataShapeRegionKey,
                            outline.toRegion(shapeBounds.left, shapeBounds.top),
                        )
                    }
                }
            }
        } else if (extraDataKey == ExtraDataShapeRectKey) {
            node.unmergedConfig.getOrNull(SemanticsProperties.Shape)?.let { shape ->
                val shapeBounds = getShapeBounds(node, info.boundsInScreen, shape)
                shape
                    .createOutline(shapeBounds.size, node.layoutInfo.layoutDirection)
                    .toAndroidRect(shapeBounds.left, shapeBounds.top)
                    ?.let { rect -> info.extras.putParcelable(ExtraDataShapeRectKey, rect) }
            }
        } else if (extraDataKey == ExtraDataShapeRectCornersKey) {
            node.unmergedConfig.getOrNull(SemanticsProperties.Shape)?.let { shape ->
                val shapeBounds = getShapeBounds(node, info.boundsInScreen, shape)
                shape
                    .createOutline(shapeBounds.size, node.layoutInfo.layoutDirection)
                    .toCornerArray()
                    ?.let { corners ->
                        info.extras.putFloatArray(ExtraDataShapeRectCornersKey, corners)
                    }
            }
        } else if (extraDataKey == ExtraDataShapeRegionKey) {
            node.unmergedConfig.getOrNull(SemanticsProperties.Shape)?.let { shape ->
                val shapeBounds = getShapeBounds(node, info.boundsInScreen, shape)
                shape
                    .createOutline(shapeBounds.size, node.layoutInfo.layoutDirection)
                    .toRegion(shapeBounds.left, shapeBounds.top)
                    ?.let { region -> info.extras.putParcelable(ExtraDataShapeRegionKey, region) }
            }
        } else {
            node.unmergedConfig.accessibilityExtraKeys?.forEach { key ->
                val extraKey = key.accessibilityExtraKey
                if (extraKey == extraDataKey) {
                    val value = node.unmergedConfig.getOrNull(key)
                    when (value) {
                        is Serializable -> info.extras.putSerializable(extraKey, value)
                        is Parcelable -> info.extras.putParcelable(extraKey, value)
                        else ->
                            throw IllegalStateException(
                                "Accessibility extra values must be " +
                                    "either Serializable or Parcelable."
                            )
                    }
                    return@forEach
                }
            }
        }
    }

    private val AccessibilityNodeInfoCompat.boundsInScreen: AndroidRect
        get() {
            val boundsInScreen = AndroidRect()
            getBoundsInScreen(boundsInScreen)
            return boundsInScreen
        }

    private fun getShapeBounds(
        node: SemanticsNode,
        nodeBoundsInScreen: AndroidRect,
        shape: Shape,
    ): Rect {
        val shapeNodeMatcher =
            object : SemanticsPropertyReceiver {
                var hasMatchedShape: Boolean = false

                override fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
                    if (value === shape) hasMatchedShape = true
                }
            }
        val layoutNode = node.layoutNode
        val shapeSemanticsModifierNode =
            layoutNode.nodes.firstFromHead(Nodes.Semantics) {
                with(it) {
                    shapeNodeMatcher.run {
                        applySemantics()
                        return@firstFromHead hasMatchedShape
                    }
                }
            }
        if (shapeSemanticsModifierNode?.node?.isAttached != true) {
            return layoutNode.outerCoordinator.boundsInWindow(clipBounds = false)
        }
        val shapeBoundsInRoot = shapeSemanticsModifierNode.requireLayoutCoordinates().boundsInRoot()
        val shapeBoundsInScreen =
            toBoundsInScreen(
                left = shapeBoundsInRoot.left,
                top = shapeBoundsInRoot.top,
                right = shapeBoundsInRoot.right,
                bottom = shapeBoundsInRoot.bottom,
            )
        return shapeBoundsInScreen.toBoundsRelativeToNodeBounds(nodeBoundsInScreen)
    }

    private fun AndroidRect.toBoundsRelativeToNodeBounds(nodeBoundsInScreen: AndroidRect): Rect {
        val leftOffset = (this.left - nodeBoundsInScreen.left).toFloat()
        val topOffset = (this.top - nodeBoundsInScreen.top).toFloat()
        return Rect(
            left = leftOffset,
            top = topOffset,
            right = leftOffset + this.width(),
            bottom = topOffset + this.height(),
        )
    }

    private fun toScreenCoords(textNode: SemanticsNode?, bounds: Rect): RectF? {
        if (textNode == null) return null
        val boundsInRoot = bounds.translate(textNode.positionInRoot)
        val textNodeBoundsInRoot = textNode.boundsInRoot

        // Only visible or partially visible locations are used.
        val visibleBounds =
            if (boundsInRoot.overlaps(textNodeBoundsInRoot)) {
                boundsInRoot.intersect(textNodeBoundsInRoot)
            } else {
                null
            }

        return if (visibleBounds != null) {
            val topLeftInScreen = view.localToScreen(Offset(visibleBounds.left, visibleBounds.top))
            val bottomRightInScreen =
                view.localToScreen(Offset(visibleBounds.right, visibleBounds.bottom))
            // Due to rotation, the top left corner of the local bounds may not be the top left
            // corner of the screen bounds.
            RectF(
                min(topLeftInScreen.x, bottomRightInScreen.x),
                min(topLeftInScreen.y, bottomRightInScreen.y),
                max(topLeftInScreen.x, bottomRightInScreen.x),
                max(topLeftInScreen.y, bottomRightInScreen.y),
            )
        } else {
            null
        }
    }

    private fun Shape.createOutline(size: Size, layoutDirection: LayoutDirection) =
        createOutline(size, layoutDirection, view.density)

    private fun Outline.toAndroidRect(leftOffset: Float, topOffset: Float): AndroidRect? =
        if (this is Outline.Rectangle || this is Outline.Rounded)
            bounds.toAndroidRect(leftOffset, topOffset)
        else null

    /**
     * Returns a radii array for each corner (top-left, top-right, bottom-right, bottom-left) if the
     * outline represents a rounded-corner rectangle, otherwise returns null.
     */
    private fun Outline.toCornerArray(): FloatArray? =
        if (this is Outline.Rounded) {
            floatArrayOf(
                roundRect.topLeftCornerRadius.x,
                roundRect.topLeftCornerRadius.y,
                roundRect.topRightCornerRadius.x,
                roundRect.topRightCornerRadius.y,
                roundRect.bottomRightCornerRadius.x,
                roundRect.bottomRightCornerRadius.y,
                roundRect.bottomLeftCornerRadius.x,
                roundRect.bottomLeftCornerRadius.y,
            )
        } else null

    private fun Outline.toRegion(leftOffset: Float, topOffset: Float): Region? =
        if (this is Outline.Generic) {
            val boundingRectangle = Region(bounds.translate(leftOffset, topOffset).toAndroidRect())
            Region().apply {
                setPath(
                    path.asAndroidPath().apply { offset(leftOffset, topOffset) },
                    boundingRectangle,
                )
            }
        } else null

    private fun Rect.toAndroidRect(leftOffset: Float = 0f, topOffset: Float = 0f): AndroidRect =
        AndroidRect(
            (left + leftOffset).toInt(),
            (top + topOffset).toInt(),
            (right + leftOffset).toInt(),
            (bottom + topOffset).toInt(),
        )

    /**
     * Dispatches hover {@link android.view.MotionEvent}s to the virtual view hierarchy when the
     * Explore by Touch feature is enabled.
     *
     * <p>
     * This method should be called by overriding {@link View#dispatchHoverEvent}:
     * <pre>&#64;Override
     * public boolean dispatchHoverEvent(MotionEvent event) {
     *   if (mHelper.dispatchHoverEvent(this, event) {
     *     return true;
     *   }
     *   return super.dispatchHoverEvent(event);
     * }
     * </pre>
     *
     * @param event The hover event to dispatch to the virtual view hierarchy.
     * @return Whether the hover event was handled.
     */
    internal fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (!isTouchExplorationEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_ENTER -> {
                val virtualViewId = hitTestSemanticsAt(event.x, event.y)
                // The android views could be view groups, so the event must be dispatched to the
                // views. Android ViewGroup.java will take care of synthesizing hover enter/exit
                // actions from hover moves.
                // Note that this should be before calling "updateHoveredVirtualView" so that in
                // the corner case of overlapped nodes, the final hover enter event is sent from
                // the node/view that we want to focus.
                val handled = view.androidViewsHandler.dispatchGenericMotionEvent(event)
                updateHoveredVirtualView(virtualViewId)
                return if (virtualViewId == InvalidId) handled else true
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                return when {
                    hoveredVirtualViewId != InvalidId -> {
                        updateHoveredVirtualView(InvalidId)
                        true
                    }
                    else -> {
                        view.androidViewsHandler.dispatchGenericMotionEvent(event)
                    }
                }
            }
            else -> {
                return false
            }
        }
    }

    /**
     * Hit test the layout tree for semantics wrappers. The return value is a virtual view id, or
     * InvalidId if an embedded Android View was hit.
     */
    @VisibleForTesting
    internal fun hitTestSemanticsAt(x: Float, y: Float): Int {
        view.measureAndLayout()

        val hitSemanticsEntities = HitTestResult()
        view.root.hitTestSemantics(
            pointerPosition = Offset(x, y),
            hitSemanticsEntities = hitSemanticsEntities,
        )

        // Iterate front-to-back until we find a node with semantics that are important-for-a11y
        for (i in hitSemanticsEntities.lastIndex downTo 0) {
            val layoutNode = hitSemanticsEntities[i].requireLayoutNode()

            // If this node corresponds to an AndroidView, then we should return InvalidId
            // to let the View System handle it.
            val androidView = view.androidViewsHandler.layoutNodeToHolder[layoutNode]
            if (androidView != null) {
                return InvalidId
            }

            if (!layoutNode.nodes.has(Nodes.Semantics)) {
                continue
            }

            val virtualViewId = semanticsNodeIdToAccessibilityVirtualNodeId(layoutNode.semanticsId)

            // The node below is not added to the tree; it's a wrapper around outer semantics to
            // use the methods available to the SemanticsNode
            val semanticsNode = SemanticsNode(layoutNode, false)

            // Continue to the next items in the hit test if it's not considered important.
            if (!semanticsNode.isImportantForAccessibility()) {
                continue
            }

            // Links in text nodes are semantics children. But for Android accessibility support
            // we don't publish them to the accessibility services because they are exposed
            // as UrlSpan/ClickableSpan spans instead
            if (semanticsNode.config.contains(SemanticsProperties.LinkTestMarker)) {
                continue
            }

            return virtualViewId
        }

        return InvalidId
    }

    /**
     * Sets the currently hovered item, sending hover accessibility events as necessary to maintain
     * the correct state.
     *
     * @param virtualViewId The virtual view id for the item currently being hovered, or
     *   {@link #InvalidId} if no item is hovered within the parent view.
     */
    private fun updateHoveredVirtualView(virtualViewId: Int) {
        if (hoveredVirtualViewId == virtualViewId) {
            return
        }

        val previousVirtualViewId: Int = hoveredVirtualViewId
        hoveredVirtualViewId = virtualViewId

        /*
        Stay consistent with framework behavior by sending ENTER/EXIT pairs
        in reverse order. This is accurate as of API 18.
        */
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
        sendEventForVirtualView(previousVirtualViewId, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)
    }

    override fun getAccessibilityNodeProvider(host: View): AccessibilityNodeProviderCompat {
        return nodeProvider
    }

    /**
     * Trims the text to [size] length. Returns the string as it is if the length is smaller than
     * [size]. If chars at [size] - 1 and [size] is a surrogate pair, returns a CharSequence of
     * length [size] - 1.
     *
     * @param size length of the result, should be greater than 0
     */
    private fun <T : CharSequence> trimToSize(
        text: T?,
        @Suppress("SameParameterValue") @IntRange(from = 1) size: Int,
    ): T? {
        require(size > 0) { "size should be greater than 0" }
        var len = size
        if (text.isNullOrEmpty() || text.length <= size) return text
        if (Character.isHighSurrogate(text[size - 1]) && Character.isLowSurrogate(text[size])) {
            len = size - 1
        }
        @Suppress("UNCHECKED_CAST")
        return text.subSequence(0, len) as T
    }

    // TODO (in a separate cl): Called when the SemanticsNode with id semanticsNodeId disappears.
    // fun clearNode(semanticsNodeId: Int) { // clear the actionIdToId and labelToActionId nodes }

    private val semanticsChangeChecker = Runnable {
        trace("measureAndLayout") { view.measureAndLayout() }
        trace("checkForSemanticsChanges") { checkForSemanticsChanges() }
        checkingForSemanticsChanges = false
    }

    internal fun onSemanticsChange() {
        // When accessibility is turned off, we still want to keep
        // currentSemanticsNodesInvalidated up to date so that when accessibility is turned on
        // later, we can refresh currentSemanticsNodes if currentSemanticsNodes is stale.
        currentSemanticsNodesInvalidated = true

        if (isEnabled && !checkingForSemanticsChanges) {
            checkingForSemanticsChanges = true
            handler.post(semanticsChangeChecker)
        }
    }

    /**
     * This suspend function loops for the entire lifetime of the Compose instance: it consumes
     * recent layout changes and sends events to the accessibility and content capture framework in
     * batches separated by a 100ms delay.
     */
    internal suspend fun boundsUpdatesEventLoop() {
        try {
            val subtreeChangedSemanticsNodesIds = MutableIntSet()
            for (notification in boundsUpdateChannel) {
                if (isEnabled) {
                    for (i in subtreeChangedLayoutNodes.indices) {
                        val layoutNode = subtreeChangedLayoutNodes.valueAt(i)
                        sendSubtreeChangeAccessibilityEvents(
                            layoutNode,
                            subtreeChangedSemanticsNodesIds,
                        )
                        sendTypeViewScrolledAccessibilityEvent(layoutNode)
                    }
                    subtreeChangedSemanticsNodesIds.clear()
                    // When the bounds of layout nodes change, we will not always get semantics
                    // change notifications because bounds is not part of semantics. And bounds
                    // change from a layout node without semantics will affect the global bounds
                    // of it children which has semantics. Bounds change will affect which nodes
                    // are covered and which nodes are not, so the currentSemanticsNodes is not
                    // up to date anymore.
                    // After the subtree events are sent, accessibility services will get the
                    // current visible/invisible state. We also try to do semantics tree diffing
                    // to send out the proper accessibility events and update our copy here so
                    // that
                    // our incremental changes (represented by accessibility events) are
                    // consistent
                    // with accessibility services. That is: change - notify - new change -
                    // notify, if we don't do the tree diffing and update our copy here, we will
                    // combine old change and new change, which is missing finer-grained
                    // notification.
                    if (!checkingForSemanticsChanges) {
                        checkingForSemanticsChanges = true
                        handler.post(semanticsChangeChecker)
                    }
                }
                subtreeChangedLayoutNodes.clear()
                pendingHorizontalScrollEvents.clear()
                pendingVerticalScrollEvents.clear()
                delay(SendRecurringAccessibilityEventsIntervalMillis)
            }
        } finally {
            subtreeChangedLayoutNodes.clear()
        }
    }

    internal fun onLayoutChange(layoutNode: LayoutNode) {
        // When accessibility is turned off, we still want to keep
        // currentSemanticsNodesInvalidated up to date so that when accessibility is turned on
        // later, we can refresh currentSemanticsNodes if currentSemanticsNodes is stale.
        currentSemanticsNodesInvalidated = true

        // Only using a11y here since the CC manager uses its own flag
        if (!isEnabled) {
            return
        }
        // The layout change of a LayoutNode will also affect its children, so even if it doesn't
        // have semantics attached, we should process it.
        notifySubtreeAccessibilityStateChangedIfNeeded(layoutNode)
    }

    private fun notifySubtreeAccessibilityStateChangedIfNeeded(layoutNode: LayoutNode) {
        if (subtreeChangedLayoutNodes.add(layoutNode)) {
            boundsUpdateChannel.trySend(Unit)
        }
    }

    private fun sendTypeViewScrolledAccessibilityEvent(layoutNode: LayoutNode) {
        // The node may be no longer available while we were waiting so check
        // again.
        if (!layoutNode.isAttached) {
            return
        }
        // Android Views will send proper events themselves.
        if (view.androidViewsHandler.layoutNodeToHolder.contains(layoutNode)) {
            return
        }

        val id = layoutNode.semanticsId
        val pendingHorizontalScroll = pendingHorizontalScrollEvents[id]
        val pendingVerticalScroll = pendingVerticalScrollEvents[id]
        if (pendingHorizontalScroll == null && pendingVerticalScroll == null) {
            return
        }

        val event = createEvent(id, AccessibilityEvent.TYPE_VIEW_SCROLLED)
        pendingHorizontalScroll?.let {
            event.scrollX = it.value().toInt()
            event.maxScrollX = it.maxValue().toInt()
        }
        pendingVerticalScroll?.let {
            event.scrollY = it.value().toInt()
            event.maxScrollY = it.maxValue().toInt()
        }
        sendEvent(event)
    }

    private fun sendSubtreeChangeAccessibilityEvents(
        layoutNode: LayoutNode,
        subtreeChangedSemanticsNodesIds: MutableIntSet,
    ) {
        // The node may be no longer available while we were waiting so check
        // again.
        if (!layoutNode.isAttached) {
            return
        }
        // Android Views will send proper events themselves.
        if (view.androidViewsHandler.layoutNodeToHolder.contains(layoutNode)) {
            return
        }

        // When we finally send the event, make sure it is an accessibility-focusable node.
        var semanticsNode =
            if (layoutNode.nodes.has(Nodes.Semantics)) layoutNode
            else layoutNode.findClosestParentNode { it.nodes.has(Nodes.Semantics) }

        val config = semanticsNode?.semanticsConfiguration ?: return
        if (!config.isMergingSemanticsOfDescendants) {
            semanticsNode
                .findClosestParentNode {
                    it.semanticsConfiguration?.isMergingSemanticsOfDescendants == true
                }
                ?.let { semanticsNode = it }
        }
        val id = semanticsNode?.semanticsId ?: return

        if (!subtreeChangedSemanticsNodesIds.add(id)) {
            return
        }

        sendEventForVirtualView(
            semanticsNodeIdToAccessibilityVirtualNodeId(id),
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE,
        )
    }

    private fun checkForSemanticsChanges() {
        // Accessibility structural change
        trace("sendAccessibilitySemanticsStructureChangeEvents") {
            if (isEnabled) {
                sendAccessibilitySemanticsStructureChangeEvents(
                    view.semanticsOwner.unmergedRootSemanticsNode,
                    previousSemanticsRoot,
                )
            }
        }
        // Accessibility property change
        trace("sendSemanticsPropertyChangeEvents") {
            sendSemanticsPropertyChangeEvents(currentSemanticsNodes)
        }
        trace("updateSemanticsNodesCopyAndPanes") { updateSemanticsNodesCopyAndPanes() }
    }

    private fun updateSemanticsNodesCopyAndPanes() {
        // TODO(b/172606324): removed this compose specific fix when talkback has a proper solution.
        val toRemove = MutableIntSet()
        paneDisplayed.forEach { id ->
            val currentNode = currentSemanticsNodes[id]?.semanticsNode
            if (
                currentNode == null ||
                    !currentNode.unmergedConfig.contains(SemanticsProperties.PaneTitle)
            ) {
                toRemove.add(id)
                sendPaneChangeEvents(
                    id,
                    AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED,
                    previousSemanticsNodes[id]
                        ?.unmergedConfig
                        ?.getOrNull(SemanticsProperties.PaneTitle),
                )
            }
        }
        paneDisplayed.removeAll(toRemove)
        previousSemanticsNodes.clear()
        currentSemanticsNodes.forEach { key, value ->
            if (
                value.semanticsNode.unmergedConfig.contains(SemanticsProperties.PaneTitle) &&
                    paneDisplayed.add(key)
            ) {
                sendPaneChangeEvents(
                    key,
                    AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_APPEARED,
                    value.semanticsNode.unmergedConfig[SemanticsProperties.PaneTitle],
                )
            }
            previousSemanticsNodes[key] =
                SemanticsNodeCopy(value.semanticsNode, currentSemanticsNodes)
        }
        previousSemanticsRoot =
            SemanticsNodeCopy(view.semanticsOwner.unmergedRootSemanticsNode, currentSemanticsNodes)
    }

    private fun sendSemanticsPropertyChangeEvents(
        newSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds>
    ) {
        val oldScrollObservationScopes = ArrayList(scrollObservationScopes)
        scrollObservationScopes.clear()
        newSemanticsNodes.forEachKey { id ->
            // We do doing this search because the new configuration is set as a whole, so we
            // can't indicate which property is changed when setting the new configuration.
            val oldNode = previousSemanticsNodes[id] ?: return@forEachKey
            val newNode =
                checkPreconditionNotNull(newSemanticsNodes[id]?.semanticsNode) {
                    "no value for specified key"
                }

            var propertyChanged = false

            newNode.unmergedConfig.props.forEach { key, value ->
                var newlyObservingScroll = false
                if (
                    key == SemanticsProperties.HorizontalScrollAxisRange ||
                        key == SemanticsProperties.VerticalScrollAxisRange
                ) {
                    newlyObservingScroll = registerScrollingId(id, oldScrollObservationScopes)
                }
                if (!newlyObservingScroll && value == oldNode.unmergedConfig.getOrNull(key)) {
                    return@forEach
                }
                @Suppress("UNCHECKED_CAST")
                when (key) {
                    SemanticsProperties.PaneTitle -> {
                        val paneTitle = value as String
                        // If oldNode doesn't have pane title, it will be handled in
                        // updateSemanticsNodesCopyAndPanes().
                        if (oldNode.unmergedConfig.contains(SemanticsProperties.PaneTitle)) {
                            sendPaneChangeEvents(
                                id,
                                AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_TITLE,
                                paneTitle,
                            )
                        }
                    }
                    SemanticsProperties.StateDescription,
                    SemanticsProperties.ToggleableState -> {
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION,
                        )
                        // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
                        // force ViewRootImpl to update its accessibility-focused virtual-node.
                        // If we have an androidx fix, we can remove this event.
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED,
                        )
                    }
                    SemanticsProperties.ProgressBarRangeInfo -> {
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION,
                        )
                        // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
                        // force ViewRootImpl to update its accessibility-focused virtual-node.
                        // If we have an androidx fix, we can remove this event.
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED,
                        )
                    }
                    SemanticsProperties.Selected -> {
                        // The assumption is among widgets using SemanticsProperties.Selected, only
                        // Tab is using AccessibilityNodeInfo#isSelected, and all others are using
                        // AccessibilityNodeInfo#isCheckable and setting
                        // AccessibilityNodeInfo#stateDescription in this delegate.
                        if (
                            newNode.unmergedConfig.getOrNull(SemanticsProperties.Role) == Role.Tab
                        ) {
                            if (
                                newNode.unmergedConfig.getOrNull(SemanticsProperties.Selected) ==
                                    true
                            ) {
                                val event =
                                    createEvent(
                                        semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                        AccessibilityEvent.TYPE_VIEW_SELECTED,
                                    )
                                // Here we use the merged node. Because we specifically are using
                                // the merged node, we must also use the merged version of the
                                // SemanticsConfiguration via `config` instead of `unmergedConfig`
                                // as the rest of the file uses.
                                val mergedNode = newNode.copyWithMergingEnabled()
                                val contentDescription =
                                    mergedNode.config
                                        .getOrNull(SemanticsProperties.ContentDescription)
                                        ?.fastJoinToString(",")
                                val text =
                                    mergedNode.config
                                        .getOrNull(SemanticsProperties.Text)
                                        ?.fastJoinToString(",")
                                contentDescription?.let { event.contentDescription = it }
                                text?.let { event.text.add(it) }
                                sendEvent(event)
                            } else {
                                // Send this event to match View.java.
                                sendEventForVirtualView(
                                    semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                    AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED,
                                )
                            }
                        } else {
                            sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION,
                            )
                            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
                            // force ViewRootImpl to update its accessibility-focused virtual-node.
                            // If we have an androidx fix, we can remove this event.
                            sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED,
                            )
                        }
                    }
                    SemanticsProperties.ContentDescription -> {
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION,
                            value as List<String>,
                        )
                    }
                    SemanticsProperties.EditableText -> {
                        if (newNode.unmergedConfig.contains(SemanticsActions.SetText)) {

                            val oldText = oldNode.unmergedConfig.getTextForTextField() ?: ""
                            val newText = newNode.unmergedConfig.getTextForTextField() ?: ""
                            val trimmedNewText = trimToSize(newText, ParcelSafeTextLength)

                            var startCount = 0
                            // endCount records how many characters are the same from the end.
                            var endCount = 0
                            val oldTextLen = oldText.length
                            val newTextLen = newText.length
                            val minLength = oldTextLen.coerceAtMost(newTextLen)
                            while (startCount < minLength) {
                                if (oldText[startCount] != newText[startCount]) {
                                    break
                                }
                                startCount++
                            }
                            // abcdabcd vs
                            //     abcd
                            while (endCount < minLength - startCount) {
                                if (
                                    oldText[oldTextLen - 1 - endCount] !=
                                        newText[newTextLen - 1 - endCount]
                                ) {
                                    break
                                }
                                endCount++
                            }
                            val removedCount = oldTextLen - endCount - startCount
                            val addedCount = newTextLen - endCount - startCount

                            val oldNodeIsPassword =
                                oldNode.unmergedConfig.contains(SemanticsProperties.Password)
                            val newNodeIsPassword =
                                newNode.unmergedConfig.contains(SemanticsProperties.Password)
                            val oldNodeIsTextfield =
                                oldNode.unmergedConfig.contains(SemanticsProperties.EditableText)

                            // (b/247891690) We won't send a text change event when we only toggle
                            // the password visibility of the node
                            val becamePasswordNode =
                                oldNodeIsTextfield && !oldNodeIsPassword && newNodeIsPassword
                            val becameNotPasswordNode =
                                oldNodeIsTextfield && oldNodeIsPassword && !newNodeIsPassword
                            val event =
                                if (becamePasswordNode || becameNotPasswordNode) {
                                    // (b/247891690) password visibility toggle is handled by a
                                    // selection event. Because internally Talkback already has the
                                    // correct cursor position, there will be no announcement.
                                    // Therefore we first send the "cursor reset" event with the
                                    // selection at (0, 0) and right after that we will send the
                                    // event
                                    // with the correct cursor position. This behaves similarly to
                                    // the
                                    // View-based material EditText which also sends two selection
                                    // events
                                    createTextSelectionChangedEvent(
                                        virtualViewId =
                                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                        fromIndex = 0,
                                        toIndex = 0,
                                        itemCount = newTextLen,
                                        text = trimmedNewText,
                                    )
                                } else {
                                    createEvent(
                                            semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                                        )
                                        .apply {
                                            this.fromIndex = startCount
                                            this.removedCount = removedCount
                                            this.addedCount = addedCount
                                            this.beforeText = oldText
                                            this.text.add(trimmedNewText)
                                        }
                                }
                            event.className = TextFieldClassName
                            sendEvent(event)

                            // (b/247891690) second event with the correct cursor position (see
                            // comment above for more details)
                            if (becamePasswordNode || becameNotPasswordNode) {
                                val textRange =
                                    newNode.unmergedConfig[SemanticsProperties.TextSelectionRange]
                                event.fromIndex = textRange.start
                                event.toIndex = textRange.end
                                sendEvent(event)
                            }
                        } else {
                            sendEventForVirtualView(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                                AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT,
                            )
                        }
                    }
                    // do we need to overwrite TextRange equals?
                    SemanticsProperties.TextSelectionRange -> {
                        val newText = newNode.unmergedConfig.getTextForTextField()?.text ?: ""
                        val textRange =
                            newNode.unmergedConfig[SemanticsProperties.TextSelectionRange]
                        val event =
                            createTextSelectionChangedEvent(
                                semanticsNodeIdToAccessibilityVirtualNodeId(id),
                                textRange.start,
                                textRange.end,
                                newText.length,
                                trimToSize(newText, ParcelSafeTextLength),
                            )
                        sendEvent(event)
                        sendPendingTextTraversedAtGranularityEvent(newNode.id)
                    }
                    SemanticsProperties.HorizontalScrollAxisRange,
                    SemanticsProperties.VerticalScrollAxisRange -> {
                        notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)

                        val scope = scrollObservationScopes.findById(id)!!
                        scope.horizontalScrollAxisRange =
                            newNode.unmergedConfig.getOrNull(
                                SemanticsProperties.HorizontalScrollAxisRange
                            )
                        scope.verticalScrollAxisRange =
                            newNode.unmergedConfig.getOrNull(
                                SemanticsProperties.VerticalScrollAxisRange
                            )
                        scheduleScrollEventIfNeeded(scope)
                    }
                    SemanticsProperties.Focused -> {
                        if (value as Boolean) {
                            sendEvent(
                                createEvent(
                                    semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                                    AccessibilityEvent.TYPE_VIEW_FOCUSED,
                                )
                            )
                        }
                        // In View.java this window event is sent for unfocused view. But we send
                        // it for focused too so that TalkBack invalidates its cache. Otherwise
                        // PasteText edit option is not displayed properly on some OS versions.
                        sendEventForVirtualView(
                            semanticsNodeIdToAccessibilityVirtualNodeId(newNode.id),
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                            AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED,
                        )
                    }
                    CustomActions -> {
                        val actions = newNode.unmergedConfig[CustomActions]
                        val oldActions = oldNode.unmergedConfig.getOrNull(CustomActions)
                        if (oldActions != null) {
                            // Suppose actions with the same label should be deduped.
                            val labels = mutableSetOf<String>()
                            actions.fastForEach { action -> labels.add(action.label) }
                            val oldLabels = mutableSetOf<String>()
                            oldActions.fastForEach { action -> oldLabels.add(action.label) }
                            propertyChanged =
                                !(labels.containsAll(oldLabels) && oldLabels.containsAll(labels))
                        } else if (actions.isNotEmpty()) {
                            propertyChanged = true
                        }
                    }
                    // TODO(b/151840490) send the correct events for certain properties, like view
                    //  selected.
                    else -> {
                        propertyChanged =
                            if (value is AccessibilityAction<*>) {
                                !value.accessibilityEquals(oldNode.unmergedConfig.getOrNull(key))
                            } else {
                                true
                            }
                    }
                }
            }

            if (!propertyChanged) {
                propertyChanged = newNode.propertiesDeleted(oldNode.unmergedConfig)
            }
            if (propertyChanged) {
                // TODO(b/176105563): throttle the window content change events and merge different
                //  sub types. We can use the subtreeChangedLayoutNodes with sub types.
                sendEventForVirtualView(
                    semanticsNodeIdToAccessibilityVirtualNodeId(id),
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED,
                )
            }
        }
    }

    // List of visible scrollable nodes (which are observing scroll state snapshot writes).
    private val scrollObservationScopes = mutableListOf<ScrollObservationScope>()

    /*
     * Lambda to store in scrolling snapshot observer, which must never be recreated because
     * the snapshot system makes use of lambda reference comparisons.
     * (Note that recent versions of the Kotlin compiler do maintain a persistent
     * object for most lambda expressions, so this is just for the purpose of explicitness.)
     */
    private val scheduleScrollEventIfNeededLambda: (ScrollObservationScope) -> Unit = {
        this.scheduleScrollEventIfNeeded(it)
    }

    private fun registerScrollingId(
        id: Int,
        oldScrollObservationScopes: List<ScrollObservationScope>,
    ): Boolean {
        var newlyObservingScroll = false
        val oldScope = oldScrollObservationScopes.findById(id)
        val newScope =
            if (oldScope != null) {
                oldScope
            } else {
                newlyObservingScroll = true
                ScrollObservationScope(
                    semanticsNodeId = id,
                    allScopes = scrollObservationScopes,
                    oldXValue = null,
                    oldYValue = null,
                    horizontalScrollAxisRange = null,
                    verticalScrollAxisRange = null,
                )
            }
        scrollObservationScopes.add(newScope)
        return newlyObservingScroll
    }

    private fun scheduleScrollEventIfNeeded(scrollObservationScope: ScrollObservationScope) {
        if (!scrollObservationScope.isValidOwnerScope) {
            return
        }
        view.snapshotObserver.observeReads(
            scrollObservationScope,
            scheduleScrollEventIfNeededLambda,
        ) {
            val newXState = scrollObservationScope.horizontalScrollAxisRange
            val newYState = scrollObservationScope.verticalScrollAxisRange
            val oldXValue = scrollObservationScope.oldXValue
            val oldYValue = scrollObservationScope.oldYValue

            val deltaX =
                if (newXState != null && oldXValue != null) {
                    newXState.value() - oldXValue
                } else {
                    0f
                }
            val deltaY =
                if (newYState != null && oldYValue != null) {
                    newYState.value() - oldYValue
                } else {
                    0f
                }

            if (deltaX != 0f || deltaY != 0f) {
                val scrollerId =
                    semanticsNodeIdToAccessibilityVirtualNodeId(
                        scrollObservationScope.semanticsNodeId
                    )

                // Refresh the current "green box" bounds and invalidate the View to tell
                // ViewRootImpl to redraw it at its latest position.
                currentSemanticsNodes[accessibilityFocusedVirtualViewId]?.let {
                    try {
                        currentlyAccessibilityFocusedANI?.setBoundsInScreen(boundsInScreen(it))
                    } catch (e: IllegalStateException) {
                        // setBoundsInScreen could in theory throw an IllegalStateException if the
                        // system has previously sealed the AccessibilityNodeInfo.  This cannot
                        // happen on stock AOSP, because ViewRootImpl only uses it for bounds
                        // checking, and never forwards it to an accessibility service. But that is
                        // a non-CTS-enforced implementation detail, so we should avoid crashing if
                        // this happens.
                    }
                }
                // Refresh the current "blue box" bounds and invalidate the View to tell
                // ViewRootImpl to redraw it at its latest position.
                currentSemanticsNodes[focusedVirtualViewId]?.let {
                    try {
                        currentlyFocusedANI?.setBoundsInScreen(boundsInScreen(it))
                    } catch (e: IllegalStateException) {
                        // setBoundsInScreen could in theory throw an IllegalStateException if the
                        // system has previously sealed the AccessibilityNodeInfo.  This cannot
                        // happen on stock AOSP, because ViewRootImpl only uses it for bounds
                        // checking, and never forwards it to an accessibility service. But that is
                        // a non-CTS-enforced implementation detail, so we should avoid crashing if
                        // this happens.
                    }
                }
                view.invalidate()

                currentSemanticsNodes[scrollerId]?.semanticsNode?.layoutNode?.let { layoutNode ->
                    // Store the data needed for TYPE_VIEW_SCROLLED events.
                    if (newXState != null) {
                        pendingHorizontalScrollEvents[scrollerId] = newXState
                    }
                    if (newYState != null) {
                        pendingVerticalScrollEvents[scrollerId] = newYState
                    }

                    // Schedule a content subtree change event for the scroller. As side effects
                    // this will also schedule a TYPE_VIEW_SCROLLED event, and suppress separate
                    // events from being sent for each child whose bounds moved.
                    notifySubtreeAccessibilityStateChangedIfNeeded(layoutNode)
                }
            }

            if (newXState != null) {
                scrollObservationScope.oldXValue = newXState.value()
            }
            if (newYState != null) {
                scrollObservationScope.oldYValue = newYState.value()
            }
        }
    }

    private fun sendPaneChangeEvents(semanticsNodeId: Int, contentChangeType: Int, title: String?) {
        val event =
            createEvent(
                semanticsNodeIdToAccessibilityVirtualNodeId(semanticsNodeId),
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            )
        event.contentChangeTypes = contentChangeType
        if (title != null) {
            event.text.add(title)
        }
        sendEvent(event)
    }

    private fun sendAccessibilitySemanticsStructureChangeEvents(
        newNode: SemanticsNode,
        oldNode: SemanticsNodeCopy,
    ) {
        val newChildren: MutableIntSet = mutableIntSetOf()

        // If any child is added, clear the subtree rooted at this node and return.
        newNode.replacedChildren.fastForEach { child ->
            if (currentSemanticsNodes.contains(child.id)) {
                if (!oldNode.children.contains(child.id)) {
                    notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)
                    return
                }
                newChildren.add(child.id)
            }
        }

        // If any child is deleted, clear the subtree rooted at this node and return.
        oldNode.children.forEach { child ->
            if (!newChildren.contains(child)) {
                notifySubtreeAccessibilityStateChangedIfNeeded(newNode.layoutNode)
                return
            }
        }

        newNode.replacedChildren.fastForEach { child ->
            previousSemanticsNodes[child.id]?.let { previousNode ->
                if (currentSemanticsNodes.contains(child.id)) {
                    sendAccessibilitySemanticsStructureChangeEvents(child, previousNode)
                }
            }
        }
    }

    private fun semanticsNodeIdToAccessibilityVirtualNodeId(id: Int): Int {
        if (id == view.semanticsOwner.unmergedRootSemanticsNode.id) {
            return AccessibilityNodeProviderCompat.HOST_VIEW_ID
        }
        return id
    }

    private fun traverseAtGranularity(
        node: SemanticsNode,
        granularity: Int,
        forward: Boolean,
        extendSelection: Boolean,
    ): Boolean {
        if (node.id != previousTraversedNode) {
            accessibilityCursorPosition = AccessibilityCursorPositionUndefined
            previousTraversedNode = node.id
        }

        val text = getIterableTextForAccessibility(node)
        if (text.isNullOrEmpty()) {
            return false
        }
        val iterator = getIteratorForGranularity(node, granularity) ?: return false
        var current = getAccessibilitySelectionEnd(node)
        if (current == AccessibilityCursorPositionUndefined) {
            current = if (forward) 0 else text.length
        }
        val range =
            (if (forward) iterator.following(current) else iterator.preceding(current))
                ?: return false
        val segmentStart = range[0]
        val segmentEnd = range[1]
        var selectionStart: Int
        val selectionEnd: Int
        if (extendSelection && isAccessibilitySelectionExtendable(node)) {
            selectionStart = getAccessibilitySelectionStart(node)
            if (selectionStart == AccessibilityCursorPositionUndefined) {
                selectionStart = if (forward) segmentStart else segmentEnd
            }
            selectionEnd = if (forward) segmentEnd else segmentStart
        } else {
            selectionStart = if (forward) segmentEnd else segmentStart
            selectionEnd = selectionStart
        }
        val action =
            if (forward) AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            else AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
        pendingTextTraversedEvent =
            PendingTextTraversedEvent(
                node,
                action,
                granularity,
                segmentStart,
                segmentEnd,
                SystemClock.uptimeMillis(),
            )
        setAccessibilitySelection(node, selectionStart, selectionEnd, true)
        return true
    }

    private fun sendPendingTextTraversedAtGranularityEvent(semanticsNodeId: Int) {
        pendingTextTraversedEvent?.let {
            // not the same node, do nothing. Don't set pendingTextTraversedEvent to null either.
            if (semanticsNodeId != it.node.id) {
                return
            }
            if (SystemClock.uptimeMillis() - it.traverseTime <= TextTraversedEventTimeoutMillis) {
                val event =
                    createEvent(
                        semanticsNodeIdToAccessibilityVirtualNodeId(it.node.id),
                        AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY,
                    )
                event.fromIndex = it.fromIndex
                event.toIndex = it.toIndex
                event.action = it.action
                event.movementGranularity = it.granularity
                event.text.add(getIterableTextForAccessibility(it.node))
                sendEvent(event)
            }
        }
        pendingTextTraversedEvent = null
    }

    private fun setAccessibilitySelection(
        node: SemanticsNode,
        start: Int,
        end: Int,
        traversalMode: Boolean,
    ): Boolean {
        // Any widget which has custom action_set_selection needs to provide cursor
        // positions, so events will be sent when cursor position change.
        // When the node is disabled, only the default/virtual set selection can performed.
        if (node.unmergedConfig.contains(SemanticsActions.SetSelection) && node.enabled()) {
            // Hide all selection controllers used for adjusting selection
            // since we are doing so explicitly by other means and these
            // controllers interact with how selection behaves. From TextView.java.
            return node.unmergedConfig[SemanticsActions.SetSelection]
                .action
                ?.invoke(start, end, traversalMode) ?: false
        }
        if (start == end && end == accessibilityCursorPosition) {
            return false
        }
        val text = getIterableTextForAccessibility(node) ?: return false
        accessibilityCursorPosition =
            if (start >= 0 && start == end && end <= text.length) {
                start
            } else {
                AccessibilityCursorPositionUndefined
            }
        val nonEmptyText = text.isNotEmpty()
        val event =
            createTextSelectionChangedEvent(
                semanticsNodeIdToAccessibilityVirtualNodeId(node.id),
                if (nonEmptyText) accessibilityCursorPosition else null,
                if (nonEmptyText) accessibilityCursorPosition else null,
                if (nonEmptyText) text.length else null,
                text,
            )
        sendEvent(event)
        sendPendingTextTraversedAtGranularityEvent(node.id)
        return true
    }

    /** Returns selection start and end indices in original text */
    private fun getAccessibilitySelectionStart(node: SemanticsNode): Int {
        // If there is ContentDescription, it will be used instead of text during traversal.
        if (
            !node.unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
                node.unmergedConfig.contains(SemanticsProperties.TextSelectionRange)
        ) {
            return node.unmergedConfig[SemanticsProperties.TextSelectionRange].start
        }
        return accessibilityCursorPosition
    }

    private fun getAccessibilitySelectionEnd(node: SemanticsNode): Int {
        // If there is ContentDescription, it will be used instead of text during traversal.
        if (
            !node.unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
                node.unmergedConfig.contains(SemanticsProperties.TextSelectionRange)
        ) {
            return node.unmergedConfig[SemanticsProperties.TextSelectionRange].end
        }
        return accessibilityCursorPosition
    }

    private fun isAccessibilitySelectionExtendable(node: SemanticsNode): Boolean {
        // Currently only TextField is extendable. Static text may become extendable later.
        return !node.unmergedConfig.contains(SemanticsProperties.ContentDescription) &&
            node.unmergedConfig.contains(SemanticsProperties.EditableText)
    }

    private fun getIteratorForGranularity(
        node: SemanticsNode?,
        granularity: Int,
    ): AccessibilityIterators.TextSegmentIterator? {
        if (node == null) return null

        val text = getIterableTextForAccessibility(node)
        if (text.isNullOrEmpty()) {
            return null
        }
        // TODO(b/160190186) Make sure locale is right in AccessibilityIterators.
        val iterator: AccessibilityIterators.AbstractTextSegmentIterator
        @Suppress("DEPRECATION")
        when (granularity) {
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER -> {
                iterator =
                    AccessibilityIterators.CharacterTextSegmentIterator.getInstance(
                        view.context.resources.configuration.locale
                    )
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD -> {
                iterator =
                    AccessibilityIterators.WordTextSegmentIterator.getInstance(
                        view.context.resources.configuration.locale
                    )
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH -> {
                iterator = AccessibilityIterators.ParagraphTextSegmentIterator.getInstance()
                iterator.initialize(text)
            }
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE,
            AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE -> {
                // Line and page granularity are only for static text or text field.
                if (!node.unmergedConfig.contains(SemanticsActions.GetTextLayoutResult)) {
                    return null
                }
                val textLayoutResult = getTextLayoutResult(node.unmergedConfig) ?: return null
                if (granularity == AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE) {
                    iterator = AccessibilityIterators.LineTextSegmentIterator.getInstance()
                    iterator.initialize(text, textLayoutResult)
                } else {
                    iterator = AccessibilityIterators.PageTextSegmentIterator.getInstance()
                    // TODO: the node should be text/textfield node instead of the current node.
                    iterator.initialize(text, textLayoutResult, node)
                }
            }
            else -> return null
        }
        return iterator
    }

    /**
     * Gets the text reported for accessibility purposes. If a text node has a content description
     * in the unmerged config, it will be used instead of the text.
     *
     * This function is basically prioritising the content description over the text or editable
     * text of the text and text field nodes.
     */
    private fun getIterableTextForAccessibility(node: SemanticsNode?): String? {
        if (node == null) {
            return null
        }
        // Note in android framework, TextView set this to its text. This is changed to
        // prioritize content description, even for Text.
        if (node.unmergedConfig.contains(SemanticsProperties.ContentDescription)) {
            return node.unmergedConfig[SemanticsProperties.ContentDescription].fastJoinToString(",")
        }

        if (node.unmergedConfig.contains(SemanticsProperties.EditableText)) {
            return node.unmergedConfig.getTextForTextField()?.text
        }

        return node.unmergedConfig.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
    }

    private fun SemanticsConfiguration.getTextForTextField(): AnnotatedString? {
        return getOrNull(SemanticsProperties.EditableText)
    }

    private inner class ComposeAccessibilityNodeProvider : AccessibilityNodeProviderCompat() {
        override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfoCompat? {
            return createNodeInfo(virtualViewId).also {
                if (sendingFocusAffectingEvent) {
                    if (virtualViewId == accessibilityFocusedVirtualViewId) {
                        currentlyAccessibilityFocusedANI = it
                    }
                    if (virtualViewId == focusedVirtualViewId) {
                        currentlyFocusedANI = it
                    }
                }
            }
        }

        override fun performAction(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
            return performActionHelper(virtualViewId, action, arguments)
        }

        override fun addExtraDataToAccessibilityNodeInfo(
            virtualViewId: Int,
            info: AccessibilityNodeInfoCompat,
            extraDataKey: String,
            arguments: Bundle?,
        ) {
            addExtraDataToAccessibilityNodeInfoHelper(virtualViewId, info, extraDataKey, arguments)
        }

        override fun findFocus(focus: Int): AccessibilityNodeInfoCompat? {
            return when (focus) {
                // TODO(b/364744967): add test for  FOCUS_ACCESSIBILITY
                FOCUS_ACCESSIBILITY ->
                    createAccessibilityNodeInfo(accessibilityFocusedVirtualViewId)
                FOCUS_INPUT ->
                    if (focusedVirtualViewId == InvalidId) null
                    else createAccessibilityNodeInfo(focusedVirtualViewId)
                else -> throw IllegalArgumentException("Unknown focus type: $focus")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private object Api24Impl {
        @JvmStatic
        fun addSetProgressAction(info: AccessibilityNodeInfoCompat, semanticsNode: SemanticsNode) {
            if (semanticsNode.enabled()) {
                semanticsNode.unmergedConfig.getOrNull(SemanticsActions.SetProgress)?.let {
                    info.addAction(
                        AccessibilityActionCompat(
                            android.R.id.accessibilityActionSetProgress,
                            it.label,
                        )
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private object Api29Impl {
        @JvmStatic
        fun addPageActions(info: AccessibilityNodeInfoCompat, semanticsNode: SemanticsNode) {
            val role = semanticsNode.unmergedConfig.getOrNull(SemanticsProperties.Role)
            if (semanticsNode.enabled() && role != Carousel) {
                semanticsNode.unmergedConfig.getOrNull(PageUp)?.let {
                    info.addAction(
                        AccessibilityActionCompat(android.R.id.accessibilityActionPageUp, it.label)
                    )
                }
                semanticsNode.unmergedConfig.getOrNull(PageDown)?.let {
                    info.addAction(
                        AccessibilityActionCompat(
                            android.R.id.accessibilityActionPageDown,
                            it.label,
                        )
                    )
                }
                semanticsNode.unmergedConfig.getOrNull(PageLeft)?.let {
                    info.addAction(
                        AccessibilityActionCompat(
                            android.R.id.accessibilityActionPageLeft,
                            it.label,
                        )
                    )
                }
                semanticsNode.unmergedConfig.getOrNull(PageRight)?.let {
                    info.addAction(
                        AccessibilityActionCompat(
                            android.R.id.accessibilityActionPageRight,
                            it.label,
                        )
                    )
                }
            }
        }
    }
}

// Note: This function was separated into a static function due to b/375509809.
/**
 * Calculates custom traversal order for the semantics tree represented by [currentSemanticsNodes]
 * and saves the result in [outputBeforeMap] and [outputAfterMap].
 *
 * @param currentSemanticsNodes: A map of all the nodes in the semantics tree.
 * @param outputBeforeMap: A mutable map that is an output of this function. It stores references to
 *   node that should be traversed before the node specified by the id.
 * @param outputAfterMap: A mutable map that is an output of this function. It stores references to
 *   node that should be traversed after the node specified by the id.
 * @param resources: Application resources.
 */
private fun setTraversalValues(
    currentSemanticsNodes: IntObjectMap<SemanticsNodeWithAdjustedBounds>,
    outputBeforeMap: MutableIntIntMap,
    outputAfterMap: MutableIntIntMap,
    resources: Resources,
) {
    outputBeforeMap.clear()
    outputAfterMap.clear()

    val hostSemanticsNode =
        currentSemanticsNodes[AccessibilityNodeProviderCompat.HOST_VIEW_ID]?.semanticsNode!!

    val semanticsOrderList =
        hostSemanticsNode.subtreeSortedByGeometryGrouping(
            isVisible = { currentSemanticsNodes.containsKey(it.id) },
            isFocusableContainer = { isScreenReaderFocusable(it, resources) },
            listToSort = listOf(hostSemanticsNode),
        )

    // Iterate through our ordered list, and creating a mapping of current node to next node ID
    // We'll later read through this and set traversal order with IdToBeforeMap
    for (i in 1..semanticsOrderList.lastIndex) {
        val prevId = semanticsOrderList[i - 1].id
        val currId = semanticsOrderList[i].id
        outputBeforeMap[prevId] = currId
        outputAfterMap[currId] = prevId
    }
}

private fun isScreenReaderFocusable(node: SemanticsNode, resources: Resources): Boolean {
    val nodeContentDescriptionOrNull =
        node.unmergedConfig.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
    val isSpeakingNode =
        nodeContentDescriptionOrNull != null ||
            getInfoText(node) != null ||
            getInfoStateDescriptionOrNull(node, resources) != null ||
            getInfoIsCheckable(node)

    return !node.isHidden &&
        (node.unmergedConfig.isMergingSemanticsOfDescendants ||
            node.isUnmergedLeafNode && isSpeakingNode)
}

private fun getInfoText(node: SemanticsNode): AnnotatedString? {
    val editableTextToAssign = node.unmergedConfig.getOrNull(SemanticsProperties.EditableText)
    val textToAssign = node.unmergedConfig.getOrNull(SemanticsProperties.Text)?.firstOrNull()
    return editableTextToAssign ?: textToAssign
}

private fun getInfoStateDescriptionOrNull(node: SemanticsNode, resources: Resources): String? {
    var stateDescription = node.unmergedConfig.getOrNull(SemanticsProperties.StateDescription)
    val toggleState = node.unmergedConfig.getOrNull(SemanticsProperties.ToggleableState)
    val role = node.unmergedConfig.getOrNull(SemanticsProperties.Role)

    // Check toggle state and retrieve description accordingly
    toggleState?.let {
        when (it) {
            ToggleableState.On -> {
                // Unfortunately, talkback has a bug of using "checked", so we set state
                // description here
                if (role == Role.Switch && stateDescription == null) {
                    stateDescription = resources.getString(R.string.state_on)
                }
            }
            ToggleableState.Off -> {
                // Unfortunately, talkback has a bug of using "not checked", so we set state
                // description here
                if (role == Role.Switch && stateDescription == null) {
                    stateDescription = resources.getString(R.string.state_off)
                }
            }
            ToggleableState.Indeterminate -> {
                if (stateDescription == null) {
                    stateDescription = resources.getString(R.string.indeterminate)
                }
            }
        }
    }

    // Check Selected property and retrieve description accordingly
    node.unmergedConfig.getOrNull(SemanticsProperties.Selected)?.let {
        if (role != Role.Tab) {
            if (stateDescription == null) {
                // If a radio entry (radio button + text) is selectable, it won't have the role
                // RadioButton, so if we use info.isCheckable/info.isChecked, talkback will say
                // "checked/not checked" instead "selected/note selected".
                stateDescription =
                    if (it) {
                        resources.getString(R.string.selected)
                    } else {
                        resources.getString(R.string.not_selected)
                    }
            }
        }
    }

    // Check if a node has progress bar range info and retrieve description accordingly
    val rangeInfo = node.unmergedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
    rangeInfo?.let {
        // let's set state description here and use state description change events.
        // otherwise, we need to send out type_view_selected event, as the old android
        // versions do. But the support for type_view_selected event for progress bars
        // maybe deprecated in talkback in the future.
        if (rangeInfo !== ProgressBarRangeInfo.Indeterminate) {
            if (stateDescription == null) {
                val valueRange = rangeInfo.range
                val progress =
                    (if (valueRange.endInclusive - valueRange.start == 0f) 0f
                        else
                            (rangeInfo.current - valueRange.start) /
                                (valueRange.endInclusive - valueRange.start))
                        .fastCoerceIn(0f, 1f)

                // We only display 0% or 100% when it is exactly 0% or 100%.
                val percent =
                    when (progress) {
                        0f -> 0
                        1f -> 100
                        else -> (progress * 100).fastRoundToInt().coerceIn(1, 99)
                    }
                stateDescription = resources.getString(R.string.template_percent, percent)
            }
        } else if (stateDescription == null) {
            stateDescription = resources.getString(R.string.in_progress)
        }
    }

    if (node.unmergedConfig.contains(SemanticsProperties.EditableText)) {
        stateDescription = createStateDescriptionForTextField(node, resources)
    }

    return stateDescription
}

/**
 * Empty text field should not be ignored by the TB so we set a state description. When there is a
 * speakable child, like a label or a placeholder text, setting this state description is redundant
 */
private fun createStateDescriptionForTextField(node: SemanticsNode, resources: Resources): String? {
    val mergedConfig = node.copyWithMergingEnabled().config
    val mergedNodeIsUnspeakable =
        mergedConfig.getOrNull(SemanticsProperties.ContentDescription).isNullOrEmpty() &&
            mergedConfig.getOrNull(SemanticsProperties.Text).isNullOrEmpty() &&
            mergedConfig.getOrNull(SemanticsProperties.EditableText).isNullOrEmpty()
    return if (mergedNodeIsUnspeakable) resources.getString(R.string.state_empty) else null
}

private fun getInfoIsCheckable(node: SemanticsNode): Boolean {
    var isCheckable = false
    val toggleState = node.unmergedConfig.getOrNull(SemanticsProperties.ToggleableState)
    val role = node.unmergedConfig.getOrNull(SemanticsProperties.Role)

    toggleState?.let { isCheckable = true }

    node.unmergedConfig.getOrNull(SemanticsProperties.Selected)?.let {
        if (role != Role.Tab) {
            isCheckable = true
        }
    }

    return isCheckable
}

// TODO(mnuzen): Move common semantics logic into `SemanticsUtils` file to make a11y delegate
// shorter and more readable.
private fun SemanticsNode.enabled() = (!config.contains(SemanticsProperties.Disabled))

private fun SemanticsNode.propertiesDeleted(oldConfig: SemanticsConfiguration): Boolean {
    for (entry in oldConfig) {
        if (!config.contains(entry.key)) {
            return true
        }
    }
    return false
}

private val SemanticsNode.isRtl
    get() = layoutInfo.layoutDirection == LayoutDirection.Rtl

private fun SemanticsNode.excludeLineAndPageGranularities(): Boolean {
    // text field that is not in focus
    if (
        unmergedConfig.contains(SemanticsProperties.EditableText) &&
            unmergedConfig.getOrNull(SemanticsProperties.Focused) != true
    )
        return true

    // text nodes that are part of the 'merged' text field, for example hint or label.
    val ancestor =
        layoutNode.findClosestParentNode {
            // looking for text field merging node
            val ancestorSemanticsConfiguration = it.semanticsConfiguration
            ancestorSemanticsConfiguration?.isMergingSemanticsOfDescendants == true &&
                ancestorSemanticsConfiguration.contains(SemanticsProperties.EditableText)
        }
    return ancestor != null &&
        ancestor.semanticsConfiguration?.getOrNull(SemanticsProperties.Focused) != true
}

private fun AccessibilityAction<*>.accessibilityEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AccessibilityAction<*>) return false

    if (label != other.label) return false
    if (action == null && other.action != null) return false
    if (action != null && other.action == null) return false

    return true
}

@Deprecated(
    message = "Use ContentCapture.isEnabled instead",
    replaceWith =
        ReplaceWith(
            expression = "!ContentCaptureManager.isEnabled",
            imports =
                ["androidx.compose.ui.contentcapture.ContentCaptureManager.Companion.isEnabled"],
        ),
    level = DeprecationLevel.WARNING,
)
@Suppress("GetterSetterNames", "NullAnnotationGroup")
@ExperimentalComposeUiApi
var DisableContentCapture: Boolean
    get() = ContentCaptureManager.isEnabled
    set(value) {
        ContentCaptureManager.isEnabled = value
    }
