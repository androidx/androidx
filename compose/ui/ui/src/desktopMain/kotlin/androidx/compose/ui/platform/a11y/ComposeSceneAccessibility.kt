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

package androidx.compose.ui.platform.a11y

import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.util.fastForEachReversed
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.FocusListener
import java.util.*
import javax.accessibility.Accessible
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleState
import javax.accessibility.AccessibleStateSet
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

/**
 * Manages the accessibility aspects of the Compose scene for [ComposeSceneMediator].
 *
 * @see SemanticsOwnerAccessibility
 * @see ComposeAccessible
 */
internal class ComposeSceneAccessibility(
    private val platformComponent: PlatformComponent,
    private val coroutineContext: CoroutineContext,
    private val isWindowLevel: Boolean = false,
    private val sceneRoot: () -> Component,
) : PlatformContext.SemanticsOwnerListener {
    private val enabled by lazy {
        (System.getProperty("compose.accessibility.enable") != "false") &&
        (System.getenv("COMPOSE_DISABLE_ACCESSIBILITY") == null)
    }

    // Exposed for the benefit of tests
    val sceneAccessibleContext by lazy {
        ComposeSceneAccessibleContext()
    }

    private val accessibleFocusHelper by lazy {
        AccessibleFocusHelper(sceneRoot(), sceneAccessibleContext)
    }

    val accessibleContextProvider: ((Component) -> AccessibleContext)?
        get() = if (enabled) { _ -> accessibleFocusHelper.accessibleContext } else null

    private val ownerAccessibilityByOwner = mutableMapOf<SemanticsOwner, SemanticsOwnerAccessibility>()
    // Internal for testing
    internal val ownerAccessibilityList = mutableListOf<SemanticsOwnerAccessibility>()

    private var requestingFocus = false
    private fun onAccessibleReceivedFocus(accessible: ComposeAccessible?) {
        // requestFocusOnAccessible fires focusGained events, which in turn
        // can call this method themselves, so we need to prevent infinite recursion
        if (requestingFocus) return

        requestingFocus = true
        try {
            val target = accessible ?: defaultAccessibilityFocusTarget()
            accessibleFocusHelper.requestFocusOnAccessible(target)
        } finally {
            requestingFocus = false
        }
    }

    override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
        check(semanticsOwner !in ownerAccessibilityByOwner)
        val ownerAccessibility = SemanticsOwnerAccessibility(
            owner = semanticsOwner,
            desktopComponent = platformComponent,
            sceneAccessibility = this,
            onFocusReceived = ::onAccessibleReceivedFocus,
        )
        ownerAccessibility.launchSyncLoop(coroutineContext)
        ownerAccessibilityByOwner[semanticsOwner] = ownerAccessibility
        ownerAccessibilityList.add(ownerAccessibility)
    }

    override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
        val controller = ownerAccessibilityByOwner.remove(semanticsOwner) ?: return
        ownerAccessibilityList.remove(controller)
        controller.dispose()
    }

    override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
        ownerAccessibilityByOwner[semanticsOwner]?.onSemanticsChange()
    }

    override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {
        ownerAccessibilityByOwner[semanticsOwner]?.onLayoutChanged(nodeId = semanticsNodeId)
    }

    fun onContentComponentGainedFocus() {
        ownerAccessibilityList.lastOrNull()?.onFocusGained()
    }

    fun onContentComponentLostFocus() {
        ownerAccessibilityList.lastOrNull()?.onFocusLost()
    }

    fun accessibleParentOverride(accessible: Accessible): Accessible? {
        return accessibleFocusHelper.accessibleParentOverride(accessible)
    }

    fun accessible(): Accessible? {
        return sceneRoot() as? Accessible
    }

    fun indexOfChild(ownerAccessibility: SemanticsOwnerAccessibility): Int {
        return ownerAccessibilityList.indexOf(ownerAccessibility)
    }

    /**
     * Finds and returns a descendant [Accessible] that should receive accessibility focus when
     * no element is actually focused.
     *
     * This is used, for example, to transfer focus when the currently focused [Accessible] is
     * removed from the hierarchy.
     */
    private fun defaultAccessibilityFocusTarget(): Accessible? {
        val ignoredRoles = setOf(
            AccessibleRole.PANEL,
            AccessibleRole.GROUP_BOX,
            AccessibleRole.UNKNOWN
        )

        // DFS over the Accessible hierarchy
        val queue = ArrayDeque<Accessible>()
        ownerAccessibilityList.lastOrNull()?.let {
            queue.add(it.rootAccessible)
        }
        while (queue.isNotEmpty()) {
            val accessible = queue.removeFirst()
            val context = accessible.accessibleContext ?: continue
            if (context.accessibleRole !in ignoredRoles) {
                return accessible
            }

            val childCount = context.accessibleChildrenCount
            for (index in 0 until childCount) {
                val child = context.getAccessibleChild(index)
                if (child != null) {
                    queue.addFirst(child)
                }
            }
        }

        return null
    }

    inner class ComposeSceneAccessibleContext : AccessibleContext(), AccessibleComponent {
        private val mainRootAccessible: ComposeAccessible?
            get() = ownerAccessibilityList.firstOrNull()?.rootAccessible

        /**
         * This function is used by Swing accessibility support to get accessible under a [Point]
         * For example, it is used by screen reader to read text under a cursor.
         *
         * To support that [ComposeSceneAccessibleContext] goes through all skia roots in a
         * [ComposeScene] and finds the best [Accessible] under the pointer.
         */
        override fun getAccessibleAt(p: Point): Accessible {
            ownerAccessibilityList.fastForEachReversed { controller ->
                val rootAccessible = controller.rootAccessible
                val context = rootAccessible.composeAccessibleContext
                val accessibleOnPoint = context.getAccessibleAt(p) ?: return@fastForEachReversed
                if (accessibleOnPoint != rootAccessible) {
                    // TODO: ^ this check produce weird behavior
                    //  when there is a component under the popup,
                    //  and this component will be read by screen reader
                    //  but this check is needed since rootAccessible has full width in [getSize]
                    //  when it will be fixed, check can be removed and better results will be produced
                    return accessibleOnPoint
                }
            }

            return sceneRoot() as Accessible
        }

        override fun contains(p: Point): Boolean = true

        override fun getAccessibleIndexInParent(): Int {
            return 0
        }

        override fun getAccessibleParent(): Accessible? {
            return sceneRoot().parent as? Accessible
        }

        override fun getAccessibleChildrenCount(): Int {
            return ownerAccessibilityList.size
        }

        override fun getAccessibleChild(i: Int): Accessible {
            return ownerAccessibilityList[i].rootAccessible
        }

        override fun getSize(): Dimension? {
            return mainRootAccessible?.composeAccessibleContext?.size
        }

        override fun getLocationOnScreen(): Point? {
            return mainRootAccessible?.composeAccessibleContext?.locationOnScreen
        }

        override fun getLocation(): Point? {
            return mainRootAccessible?.composeAccessibleContext?.location
        }

        override fun getBounds(): Rectangle? {
            return mainRootAccessible?.composeAccessibleContext?.bounds
        }

        override fun isShowing(): Boolean = true

        override fun isFocusTraversable() = false

        override fun getAccessibleComponent(): AccessibleComponent {
            return this
        }

        override fun getLocale(): Locale = Locale.getDefault()

        override fun isVisible(): Boolean = true

        override fun isEnabled(): Boolean = true

        override fun requestFocus() {
            // DO NOTHING
        }

        override fun getAccessibleRole(): AccessibleRole {
            // We want to return a role that makes the ComposeScene container "transparent" to
            // accessibility, as if its contents are inside the parent directly.
            // - On Windows, NVDA ignores UNKNOWN, but on macOS UNKNOWN causes VoiceOver to highlight
            //   the entire component when traversing via VoiceOver shortcuts.
            // - On macOS, PANEL is ignored by Java's a11y (see CAccessibility.ignoredRoles), but on
            //   Windows, it makes NVDA read it as "panel" when clicked. The exception to this is
            //   when the scene is for the entire window (with, e.g., Composable Window), returning
            //   PANEL when nothing else is focused makes VoiceOver highlight it because it is
            //   the focused Swing component. UNKNOWN prevents that, and because it's top-level,
            //   the case with traversing via VoiceOver shortcuts doesn't apply.
            return when (hostOs) {
                OS.MacOS -> if (isWindowLevel) AccessibleRole.UNKNOWN else AccessibleRole.PANEL
                else -> AccessibleRole.UNKNOWN
            }
        }

        private val _accessibleStateSet = AccessibleStateSet().apply {
            add(AccessibleState.ENABLED)
            add(AccessibleState.VISIBLE)
            add(AccessibleState.SHOWING)
        }

        override fun getAccessibleStateSet(): AccessibleStateSet {
            return _accessibleStateSet
        }

        override fun setLocation(p: Point?) {
            // DO NOTHING
        }

        override fun setBounds(r: Rectangle?) {
            // DO NOTHING
        }

        override fun setSize(d: Dimension?) {
            // DO NOTHING
        }

        override fun setVisible(b: Boolean) {
            // DO NOTHING
        }

        override fun getBackground(): Color? {
            return null
        }

        override fun setBackground(c: Color?) {
            // DO NOTHING
        }

        override fun getForeground(): Color? {
            return null
        }

        override fun setForeground(c: Color?) {
            // DO NOTHING
        }

        override fun getCursor(): Cursor? {
            return null
        }

        override fun setCursor(cursor: Cursor?) {
            // DO NOTHING
        }

        override fun getFont(): Font? {
            return null
        }

        override fun setFont(f: Font?) {
            // DO NOTHING
        }

        override fun getFontMetrics(f: Font?): FontMetrics? {
            return null
        }

        override fun setEnabled(b: Boolean) {
            // DO NOTHING
        }

        override fun addFocusListener(l: FocusListener?) {
            // DO NOTHING
        }

        override fun removeFocusListener(l: FocusListener?) {
            // DO NOTHING
        }
    }
}