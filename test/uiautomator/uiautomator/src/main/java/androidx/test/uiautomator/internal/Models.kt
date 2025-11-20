/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator.internal

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.boundsInScreen
import androidx.test.uiautomator.children

/** A snapshot of an accessibility node info at a certain moment in time. */
internal data class ElementNode(
    val depth: Int,
    val text: String,
    val viewIdResourceName: String,
    val className: String,
    val packageName: String,
    val contentDescription: String,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val isFocusable: Boolean,
    val isFocused: Boolean,
    val isScrollable: Boolean,
    val isLongClickable: Boolean,
    val isPassword: Boolean,
    val isSelected: Boolean,
    val bounds: Rect,
    val childCount: Int,
    val children: Set<ElementNode>,
    val hintText: String,
    val isLeaf: Boolean,
    val drawingOrderInParent: Int,
    val accessibilityNodeInfo: AccessibilityNodeInfo,
) {
    companion object {

        fun fromAccessibilityNodeInfo(
            depth: Int,
            node: AccessibilityNodeInfo,
            displayRect: Rect,
        ): ElementNode {

            val children = node.children()

            val childrenNodes =
                children
                    .map { child ->
                        val childNode =
                            fromAccessibilityNodeInfo(
                                node = child,
                                displayRect = displayRect,
                                depth = depth + 1,
                            )
                        @Suppress("DEPRECATION") child.recycle()
                        childNode
                    }
                    .sortedBy { it.drawingOrderInParent }

            val hintText =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    node.hintText?.toString() ?: ""
                } else ""
            val drawingOrderInParent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    node.drawingOrder
                } else 0

            fun CharSequence?.orBlank() = this.toString()
            return ElementNode(
                depth = depth,
                text = node.text.orBlank(),
                viewIdResourceName = node.viewIdResourceName.orBlank(),
                className = node.className.orBlank(),
                packageName = node.packageName.orBlank(),
                contentDescription = node.contentDescription.orBlank(),
                hintText = hintText,
                isCheckable = node.isCheckable,
                isChecked = node.isChecked,
                isClickable = node.isClickable,
                isEnabled = node.isEnabled,
                isFocusable = node.isFocusable,
                isFocused = node.isFocused,
                isScrollable = node.isScrollable,
                isLongClickable = node.isLongClickable,
                isPassword = node.isPassword,
                isSelected = node.isSelected,
                childCount = node.childCount,
                bounds = node.boundsInScreen(),
                children = childrenNodes.toSet(),
                isLeaf = children.isEmpty(),
                drawingOrderInParent = drawingOrderInParent,
                accessibilityNodeInfo = node,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ElementNode

        if (depth != other.depth) return false
        if (text != other.text) return false
        if (viewIdResourceName != other.viewIdResourceName) return false
        if (className != other.className) return false
        if (packageName != other.packageName) return false
        if (contentDescription != other.contentDescription) return false
        if (isCheckable != other.isCheckable) return false
        if (isChecked != other.isChecked) return false
        if (isClickable != other.isClickable) return false
        if (isEnabled != other.isEnabled) return false
        if (isFocusable != other.isFocusable) return false
        if (isFocused != other.isFocused) return false
        if (isScrollable != other.isScrollable) return false
        if (isLongClickable != other.isLongClickable) return false
        if (isPassword != other.isPassword) return false
        if (isSelected != other.isSelected) return false
        if (bounds != other.bounds) return false
        if (childCount != other.childCount) return false
        if (children != other.children) return false
        if (hintText != other.hintText) return false
        if (isLeaf != other.isLeaf) return false
        if (drawingOrderInParent != other.drawingOrderInParent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depth
        result = 31 * result + text.hashCode()
        result = 31 * result + viewIdResourceName.hashCode()
        result = 31 * result + className.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + contentDescription.hashCode()
        result = 31 * result + isCheckable.hashCode()
        result = 31 * result + isChecked.hashCode()
        result = 31 * result + isClickable.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isFocusable.hashCode()
        result = 31 * result + isFocused.hashCode()
        result = 31 * result + isScrollable.hashCode()
        result = 31 * result + isLongClickable.hashCode()
        result = 31 * result + isPassword.hashCode()
        result = 31 * result + isSelected.hashCode()
        result = 31 * result + bounds.hashCode()
        result = 31 * result + childCount
        result = 31 * result + children.hashCode()
        result = 31 * result + hintText.hashCode()
        result = 31 * result + isLeaf.hashCode()
        result = 31 * result + drawingOrderInParent
        return result
    }
}
