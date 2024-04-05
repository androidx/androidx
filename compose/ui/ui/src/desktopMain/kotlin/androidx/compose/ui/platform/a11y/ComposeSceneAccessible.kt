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

import androidx.compose.ui.platform.a11y.ComposeSceneAccessible.ComposeSceneAccessibleContext
import java.awt.*
import java.awt.event.FocusListener
import java.util.*
import javax.accessibility.*

/**
 * This is a root [Accessible] for a [androidx.compose.ui.ComposeScene]
 *
 * It provides [AccessibleContext] to Swing accessibility support.
 * This context has no parents and provides [ComposeAccessible] as its children.
 *
 * The main purpose of this class is to support screen readers that read text under the mouse (not only, but mostly).
 * To support it [_accessibleContext] provides custom [ComposeSceneAccessibleContext.getAccessibleAt] implementation.
 *
 * Note about a11y for focus-based tools (e.g. VoiceOver).
 * Now focus-based tools are supported on [org.jetbrains.skiko.HardwareLayer] side.
 * When Compose's [androidx.compose.ui.semantics.SemanticsNode] is focused
 * [AccessibilityController.onFocusReceived] is called and
 * [org.jetbrains.skiko.HardwareLayer] provides mapped [ComposeAccessible] to accessibility tool.
 *
 * @see AccessibilityController
 * @see ComposeAccessible
 */
internal class ComposeSceneAccessible(
    private val forceEnableA11y: Boolean = false,
    private val accessibilityControllersProvider: () -> List<AccessibilityController>,
) : Accessible {
    private val a11yEnabled by lazy {
        forceEnableA11y || (
            (System.getProperty("compose.accessibility.enable") != "false") &&
            (System.getenv("COMPOSE_DISABLE_ACCESSIBILITY") == null)
        )
    }

    private val _accessibleContext by lazy {
        ComposeSceneAccessibleContext()
    }

    // Declare ComposeSceneAccessibleContext as the return type for the benefit of tests
    override fun getAccessibleContext(): ComposeSceneAccessibleContext? {
        if (!a11yEnabled) {
            return null
        }
        return _accessibleContext
    }

    inner class ComposeSceneAccessibleContext : AccessibleContext(), AccessibleComponent {
        // Internal for testing
        internal val accessibilityControllers: List<AccessibilityController>
            get() = accessibilityControllersProvider()

        private val accessibilityController: AccessibilityController?
            get() = accessibilityControllers.firstOrNull()

        private fun getMainOwnerAccessibleRoot(): ComposeAccessible? {
            return accessibilityController?.rootAccessible
        }

        /**
         * This function is used by Swing accessibility support to get accessible under a [Point]
         * For example, it is used by screen reader to read text under a cursor.
         *
         * To support that [ComposeSceneAccessibleContext] goes through all skia roots in a [androidx.compose.ui.ComposeScene]
         * and finds the best [Accessible] under the pointer.
         */
        override fun getAccessibleAt(p: Point): Accessible? {
            for (controller in accessibilityControllers) {
                val rootAccessible = controller.rootAccessible
                val context = rootAccessible.composeAccessibleContext
                val accessibleOnPoint = context.getAccessibleAt(p) ?: continue
                if (accessibleOnPoint != rootAccessible) {
                    // TODO: ^ this check produce weird behavior
                    //  when there is a component under the popup,
                    //  and this component will be read by screen reader
                    //  but this check is needed since rootAccessible has full width in [getSize]
                    //  when it will be fixed, check can be removed and better results will be produced
                    return accessibleOnPoint
                }
            }

            return null
        }

        override fun contains(p: Point): Boolean = true

        override fun getAccessibleIndexInParent(): Int {
            return -1
        }

        override fun getAccessibleChildrenCount(): Int {
            return accessibilityControllers.size
        }

        override fun getAccessibleChild(i: Int): Accessible {
            return accessibilityControllers[i].rootAccessible
        }

        override fun getSize(): Dimension? {
            return getMainOwnerAccessibleRoot()?.composeAccessibleContext?.size
        }

        override fun getLocationOnScreen(): Point? {
            return getMainOwnerAccessibleRoot()?.composeAccessibleContext?.locationOnScreen
        }

        override fun getLocation(): Point? {
            return getMainOwnerAccessibleRoot()?.composeAccessibleContext?.location
        }

        override fun getBounds(): Rectangle? {
            return getMainOwnerAccessibleRoot()?.composeAccessibleContext?.bounds
        }

        override fun isShowing(): Boolean = true

        override fun isFocusTraversable(): Boolean = true

        override fun getAccessibleParent(): Accessible? {
            return null
        }

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
            return AccessibleRole.UNKNOWN
        }

        override fun getAccessibleStateSet(): AccessibleStateSet {
            return AccessibleStateSet()
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