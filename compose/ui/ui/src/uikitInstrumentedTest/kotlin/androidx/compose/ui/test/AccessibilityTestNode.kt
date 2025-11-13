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

package androidx.compose.ui.test

import androidx.compose.ui.platform.accessibility.CMPAccessibilityTraitIsEditing
import androidx.compose.ui.platform.accessibility.CMPAccessibilityTraitTextView
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.test.utils.intersect
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIAccessibilityElement
import platform.UIKit.UIAccessibilityTraitAdjustable
import platform.UIKit.UIAccessibilityTraitAllowsDirectInteraction
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitCausesPageTurn
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitKeyboardKey
import platform.UIKit.UIAccessibilityTraitLink
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitPlaysSound
import platform.UIKit.UIAccessibilityTraitSearchField
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIAccessibilityTraitStartsMediaSession
import platform.UIKit.UIAccessibilityTraitStaticText
import platform.UIKit.UIAccessibilityTraitSummaryElement
import platform.UIKit.UIAccessibilityTraitSupportsZoom
import platform.UIKit.UIAccessibilityTraitTabBar
import platform.UIKit.UIAccessibilityTraitToggleButton
import platform.UIKit.UIAccessibilityTraitUpdatesFrequently
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.accessibilityCustomActions
import platform.UIKit.accessibilityElementAtIndex
import platform.UIKit.accessibilityElementCount
import platform.UIKit.accessibilityElements
import platform.UIKit.accessibilityFrame
import platform.UIKit.accessibilityLabel
import platform.UIKit.accessibilityTraits
import platform.UIKit.accessibilityValue
import platform.UIKit.automationElements
import platform.UIKit.isAccessibilityElement
import platform.darwin.NSIntegerMax
import platform.darwin.NSObject

/**
 * Constructs an accessibility tree representation of the UI hierarchy starting from the window.
 *
 * This function traverses the accessibility elements and their children to build a structured
 * node tree with information about accessibility properties, allowing for analysis and testing
 * of the accessibility features of the UI.
 *
 * @return The root node of the accessibility tree representing the current UI hierarchy,
 * or null if the tree cannot be constructed.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun UIKitInstrumentedTest.getAccessibilityTree(): AccessibilityTestNode {
    fun buildNode(element: NSObject, isAccessibilityElementContent: Boolean): AccessibilityTestNode {
        val children = mutableListOf<AccessibilityTestNode>()
        val accessibilityElementContent = isAccessibilityElementContent || element.isAccessibilityElement

        if (element.accessibilityElements() != null) {
            element.accessibilityElements()?.forEach {
                children.add(buildNode(it as NSObject, accessibilityElementContent))
            }
        } else if (element is UIView) {
            element.subviews.forEach {
                children.add(buildNode(it as UIView, accessibilityElementContent))
            }
        } else if (available(OS.Ios to OSVersion(major = 17)) &&
            (element.automationElements?.isNotEmpty() ?: false)
        ) {
            // iOS Automation uses `automationElements` to build the semantics tree inside the
            // accessibility element.
            // Exceptions are UIKit elements that use private logic to build their semantics tree
            // for automation.
            element.automationElements?.forEach {
                children.add(buildNode(it as NSObject, accessibilityElementContent))
            }
        } else {
            val count = element.accessibilityElementCount()
            if (count == NSIntegerMax) {
                when (element) {
                    is UIView -> {
                        element.subviews.forEach {
                            children.add(buildNode(it as UIView, accessibilityElementContent))
                        }
                    }

                    is UIWindowScene -> {
                        element.windows.filter { !(it as UIWindow).isHidden() }.forEach {
                            children.add(buildNode(it as UIWindow, accessibilityElementContent))
                        }
                    }
                }
            } else if (count > 0) {
                (0 until count).forEach {
                    val child = element.accessibilityElementAtIndex(it) as NSObject
                    children.add(buildNode(child, accessibilityElementContent))
                }
            } else if (element is UIView) {
                element.subviews.forEach {
                    children.add(buildNode(it as UIView, accessibilityElementContent))
                }
            } else if (element is UIWindowScene) {
                element.windows.filter { !(it as UIWindow).isHidden() }.forEach {
                    children.add(buildNode(it as UIWindow, accessibilityElementContent))
                }
            }
        }

        return AccessibilityTestNode(
            isAccessibilityElement = element.isAccessibilityElement,
            identifier = (element as? UIAccessibilityElement)?.accessibilityIdentifier,
            label = element.accessibilityLabel,
            value = element.accessibilityValue,
            frame = element.accessibilityFrame.asDpRect(),
            children = children,
            traits = allAccessibilityTraits.keys.filter {
                element.accessibilityTraits and it != 0.toULong()
            },
            element = element
        ).also { node ->
            children.forEach { it.parent = node }
        }
    }

    return buildNode(appDelegate.window!!.windowScene!!, isAccessibilityElementContent = false)
}

private val allAccessibilityTraits = mutableMapOf(
    UIAccessibilityTraitNone to "UIAccessibilityTraitNone",
    UIAccessibilityTraitButton to "UIAccessibilityTraitButton",
    UIAccessibilityTraitLink to "UIAccessibilityTraitLink",
    UIAccessibilityTraitHeader to "UIAccessibilityTraitHeader",
    UIAccessibilityTraitSearchField to "UIAccessibilityTraitSearchField",
    UIAccessibilityTraitImage to "UIAccessibilityTraitImage",
    UIAccessibilityTraitSelected to "UIAccessibilityTraitSelected",
    UIAccessibilityTraitPlaysSound to "UIAccessibilityTraitPlaysSound",
    UIAccessibilityTraitKeyboardKey to "UIAccessibilityTraitKeyboardKey",
    UIAccessibilityTraitStaticText to "UIAccessibilityTraitStaticText",
    UIAccessibilityTraitSummaryElement to "UIAccessibilityTraitSummaryElement",
    UIAccessibilityTraitNotEnabled to "UIAccessibilityTraitNotEnabled",
    UIAccessibilityTraitUpdatesFrequently to "UIAccessibilityTraitUpdatesFrequently",
    UIAccessibilityTraitStartsMediaSession to "UIAccessibilityTraitStartsMediaSession",
    UIAccessibilityTraitAdjustable to "UIAccessibilityTraitAdjustable",
    UIAccessibilityTraitAllowsDirectInteraction to "UIAccessibilityTraitAllowsDirectInteraction",
    UIAccessibilityTraitCausesPageTurn to "UIAccessibilityTraitCausesPageTurn",
    UIAccessibilityTraitTabBar to "UIAccessibilityTraitTabBar",
    CMPAccessibilityTraitTextView to "CMPAccessibilityTraitTextView",
    CMPAccessibilityTraitIsEditing to "CMPAccessibilityTraitIsEditing",
).let {
    if (available(OS.Ios to OSVersion(major = 17))) {
        it[UIAccessibilityTraitToggleButton] = "UIAccessibilityTraitToggleButton"
        it[UIAccessibilityTraitSupportsZoom] = "UIAccessibilityTraitSupportsZoom"
    }
    it as Map<UIAccessibilityTraits, String>
}

/**
 * Represents a node in an accessibility tree, which is used for testing accessibility features
 * within a UI hierarchy. This class captures various accessibility properties of UI components
 * and structures them into a tree.
 */
internal data class AccessibilityTestNode(
    var isAccessibilityElement: Boolean? = null,
    var identifier: String? = null,
    var label: String? = null,
    var value: String? = null,
    var frame: DpRect? = null,
    var children: List<AccessibilityTestNode>? = null,
    var traits: List<UIAccessibilityTraits>? = null,
    var element: NSObject? = null,
    var parent: AccessibilityTestNode? = null,
) {
    fun node(builder: AccessibilityTestNode.() -> Unit) {
        children = (children ?: emptyList()) + AccessibilityTestNode().apply(builder)
    }

    fun traits(vararg trait: UIAccessibilityTraits) {
        traits = (traits ?: emptyList()) + trait
    }

    fun validate(actualNode: AccessibilityTestNode?) {
        isAccessibilityElement?.let {
            assertEquals(it, actualNode?.isAccessibilityElement)
        }
        identifier?.let {
            assertEquals(it, actualNode?.identifier)
        }
        label?.let {
            assertEquals(it, actualNode?.label)
        }
        value?.let {
            assertEquals(it, actualNode?.value)
        }
        frame?.let {
            assertEquals(it, actualNode?.frame)
        }
        traits?.let {
            assertEquals(it.toSet(), actualNode?.traits?.toSet())
        }
        children?.let {
            assertEquals(it.count(), actualNode?.children?.count())
            it.zip(actualNode?.children ?: emptyList()) { validator, child ->
                validator.validate(child)
            }
        }
    }

    val hasAccessibilityComponents: Boolean = identifier != null ||
        isAccessibilityElement == true ||
        label != null ||
        value != null ||
        traits?.isNotEmpty() == true

    fun printTree(): String {
        val builder = StringBuilder()

        fun print(node: AccessibilityTestNode, level: Int) {
            val indent = "    ".repeat(level)
            builder.append(indent)
            builder.append(node.label ?: node.identifier ?: "other")
            builder.append(" - ${node.frame}")
            node.element?.let {
                builder.append(" - <${it::class}>")
            }
            builder.appendLine()

            val fieldIndent = "$indent |"
            if (node.isAccessibilityElement == true) {
                builder.appendLine("$fieldIndent isAccessibilityElement: true")
            }
            node.identifier?.let {
                builder.appendLine("$fieldIndent accessibilityIdentifier: $it")
            }
            node.label?.let { builder.appendLine("$fieldIndent accessibilityLabel: $it") }
            if (node.traits?.isNotEmpty() == true) {
                builder.appendLine("$fieldIndent accessibilityTraits:")
                node.traits?.forEach {
                    builder.appendLine("$fieldIndent  - ${allAccessibilityTraits.getValue(it)}")
                }
            }
            node.value?.let { builder.appendLine("$fieldIndent accessibilityValue: $it") }
            node.element?.accessibilityCustomActions?.takeIf { it.isNotEmpty() }?.let {
                builder.appendLine("$fieldIndent accessibilityCustomActions: $it")
            }

            node.children?.forEach { print(it, level + 1) }
        }
        print(this, level = 0)

        return builder.toString()
    }
}

/**
 * Normalizes the accessibility nodes tree by analyzing its properties and children.
 * Removes all element that are not accessibility elements or does not work as elements containers.
 */
internal fun AccessibilityTestNode.normalized(): AccessibilityTestNode? {
    val normalizedChildren = children?.flatMap { child ->
        child.normalized()?.let {
            if (it.hasAccessibilityComponents || (it.children?.count() ?: 0) > 1) {
                listOf(it)
            } else {
                it.children
            }
        } ?: emptyList()
    } ?: emptyList()

    return if (hasAccessibilityComponents || normalizedChildren.count() > 1) {
        this.copy(children = normalizedChildren)
    } else if (normalizedChildren.count() == 1) {
        normalizedChildren.single()
    } else {
        null
    }
}

internal fun AccessibilityTestNode.assertVisibleInContainer() {
    var frame = this.frame ?: DpRectZero()
    var iterator = parent
    while (iterator != null && iterator.element !is UIWindow) {
        frame = frame.intersect(iterator.frame ?: DpRectZero())
        iterator = iterator.parent
    }

    assertTrue(
        frame.width >= 1.dp && frame.height >= 1.dp,
        "Element with frame ${this.frame} ($frame) is not visible or has very small size"
    )
}

/**
 * Asserts that the current accessibility tree matches the expected structure defined in the
 * provided lambda. The expected structure is defined by configuring an `AccessibilityTestNode`,
 * which is then validated against the actual normalized accessibility tree. This function waits
 * for the UI to be idle before performing the validation.
 *
 * @param expected A lambda that allows the caller to specify the expected structure and properties
 * of the accessibility tree.
 */
internal fun UIKitInstrumentedTest.assertAccessibilityTree(
    expected: AccessibilityTestNode.() -> Unit
) {
    val validator = AccessibilityTestNode()
    with(validator, expected)
    assertAccessibilityTree(validator)
}

internal fun UIKitInstrumentedTest.findNodeWithTag(tag: String) = findNodeWithTagOrNull(tag)
    ?: run {
        println("Actual accessibility tree:")
        println(getAccessibilityTree().printTree())
        fail("Unable to find node with identifier: $tag")
    }

internal fun UIKitInstrumentedTest.findNodeWithTagOrNull(tag: String) = firstNodeOrNull {
    it.identifier == tag
}

internal fun UIKitInstrumentedTest.findNodeWithLabel(label: String) = findNodeWithLabelOrNull(label)
    ?: run {
        println("Actual accessibility tree:")
        println(getAccessibilityTree().printTree())
        fail("Unable to find node with label: $label")
    }

internal fun UIKitInstrumentedTest.findNodeWithLabelOrNull(label: String) = firstNodeOrNull {
    it.label == label
}

internal fun UIKitInstrumentedTest.firstNodeOrNull(
    isValid: (AccessibilityTestNode) -> Boolean
): AccessibilityTestNode? = findAllNodes(isValid).firstOrNull()

internal fun UIKitInstrumentedTest.findAllNodes(
    isValid: (AccessibilityTestNode) -> Boolean
): Sequence<AccessibilityTestNode> {
    waitForIdle()

    val actualTreeRoot = getAccessibilityTree()
    fun getAllNodes(node: AccessibilityTestNode): Sequence<AccessibilityTestNode> = sequence {
        if (isValid(node)) {
            yield(node)
        }
        node.children?.forEach { child ->
            yieldAll(getAllNodes(child))
        }
    }
    return getAllNodes(actualTreeRoot)
}

/**
 * Asserts that the current accessibility tree matches the expected structure defined in the
 * provided lambda. The expected structure is defined by configuring an `AccessibilityTestNode`,
 * which is then validated against the actual normalized accessibility tree. This function waits
 * for the UI to be idle before performing the validation.
 *
 * @param expected The expected accessibility tree structure represented by an instance of
 * `AccessibilityTestNode`.
 */
internal fun UIKitInstrumentedTest.assertAccessibilityTree(expected: AccessibilityTestNode) {
    waitForIdle()

    val actualTreeRoot = getAccessibilityTree()
    val normalizedTree = actualTreeRoot.normalized()

    try {
        expected.validate(normalizedTree)
    } catch (e: Throwable) {
        val message = "Unable to validate accessibility tree. Expected normalized tree:\n\n" +
            "${expected.printTree()}\n" +
            "Normalized tree:\n\n${normalizedTree?.printTree()}\n" +
            "Actual tree:\n\n${actualTreeRoot.printTree()}\n"
        println(message)

        throw e
    }
}
