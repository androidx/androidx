/*
 * Copyright 2019 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.android.captureRegionToImage
import androidx.compose.ui.test.android.forceRedraw
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt

/**
 * Captures the underlying semantics node's surface into an [ImageBitmap].
 *
 * This can be used to capture nodes in a normal composable, as well as across multiple roots.
 * Popups and Dialogs (if API >= 28) are specific cases of this, where they can be captured together
 * with their anchor.
 *
 * For example, selecting the root node (via `onRoot()`) when a popup or dialog is present will
 * detect multiple roots. In this scenario, the resulting image is cropped to the combined bounding
 * box of all nodes across the different roots. If a Dialog is present among the roots, the image is
 * cropped to the window's visible display frame.
 *
 * @throws IllegalArgumentException if an attempt is made to capture a bitmap of a dialog before
 *   API 28.
 */
@OptIn(ExperimentalTestApi::class)
@RequiresApi(Build.VERSION_CODES.O)
fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
    val nodes = fetchSemanticsNodes(atLeastOneRootRequired = true).selectedNodes
    require(nodes.isNotEmpty()) { "Failed to capture a node to bitmap." }

    if (nodes.size > 1) {
        return processMultiWindowScreenshot(nodes, testContext)
    }

    val node = nodes.single()

    // Popups are in a different window; use the multi-window screenshot mechanism
    if (node.isInsidePopup) {
        return processMultiWindowScreenshot(listOf(node), testContext)
    }

    val windowToUse = node.getDialogWindow() ?: node.view.context.getActivityWindow()
    return windowToUse.captureRegionToImage(testContext, node.getBoundsInWindow())
}

/**
 * Handles capturing nodes that exist in separate windows (like Popups or multiple nodes) by taking
 * a full-device screenshot via [android.app.UiAutomation] and cropping it to the nodes' dimensions.
 *
 * @param nodes The list of [SemanticsNode]s to capture.
 * @param testContext The current [TestContext] used to force a UI redraw before capturing.
 * @return An [ImageBitmap] cropped specifically to the bounding box of the provided nodes.
 */
@Suppress("ListIterator")
@ExperimentalTestApi
@RequiresApi(Build.VERSION_CODES.O)
private fun processMultiWindowScreenshot(
    nodes: List<SemanticsNode>,
    testContext: TestContext,
): ImageBitmap {
    val rootViews = nodes.map { it.view }.distinct()
    rootViews.forEach { it.forceRedraw(testContext) }

    val combinedBitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()

    val hasDialog = rootViews.any { findDialogWindowProviderInParent(it) != null }
    if (hasDialog && rootViews.isNotEmpty()) {
        return cropDialogVisibleFrame(rootViews.first(), combinedBitmap)
    }

    val boundingBox = nodes.calculateBoundingBox()

    val cropX = boundingBox.left.coerceAtLeast(0)
    val cropY = boundingBox.top.coerceAtLeast(0)
    val cropWidth = boundingBox.width().coerceAtMost(combinedBitmap.width - cropX)
    val cropHeight = boundingBox.height().coerceAtMost(combinedBitmap.height - cropY)

    val finalBitmap = Bitmap.createBitmap(combinedBitmap, cropX, cropY, cropWidth, cropHeight)
    return finalBitmap.asImageBitmap()
}

/**
 * Extracts the visible frame of a Dialog window from a full-screen screenshot.
 *
 * @param rootView The root [View] of the Dialog.
 * @param combinedBitmap The full-screen [Bitmap] taken via UiAutomation.
 * @return An [ImageBitmap] cropped to the Dialog's visible display frame.
 */
private fun cropDialogVisibleFrame(rootView: View, combinedBitmap: Bitmap): ImageBitmap {
    val window = rootView.context.getActivityWindow()
    val visibleFrame = Rect()
    window.decorView.getWindowVisibleDisplayFrame(visibleFrame)

    val cropX = visibleFrame.left.coerceAtLeast(0)
    val cropY = visibleFrame.top.coerceAtLeast(0)
    val cropWidth = visibleFrame.width().coerceAtMost(combinedBitmap.width - cropX)
    val cropHeight = visibleFrame.height().coerceAtMost(combinedBitmap.height - cropY)

    return Bitmap.createBitmap(combinedBitmap, cropX, cropY, cropWidth, cropHeight).asImageBitmap()
}

private fun SemanticsNode.getDialogWindow(): Window? {
    val isDialog =
        findClosestParentNode(includeSelf = true) {
            it.config.contains(SemanticsProperties.IsDialog)
        } != null

    if (!isDialog) return null

    require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        "Cannot currently capture dialogs on API lower than 28!"
    }

    return findDialogWindowProviderInParent(view)?.window
        ?: throw IllegalArgumentException(
            "Could not find a dialog window provider to capture its bitmap"
        )
}

/**
 * Calculates the bounding [Rect] of this [SemanticsNode] relative to its hosting Window.
 *
 * Bounds within a Compose root do not automatically account for the root's offset within the actual
 * Window. This function applies that necessary offset.
 */
private fun SemanticsNode.getBoundsInWindow(): Rect {
    val bounds = boundsInRoot
    val rect =
        Rect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        )

    val locationInWindow = intArrayOf(0, 0)
    view.getLocationInWindow(locationInWindow)
    rect.offset(locationInWindow[0], locationInWindow[1])

    return rect
}

/**
 * Calculates a single bounding [Rect] that completely encloses all [SemanticsNode]s in the list,
 * using their absolute screen positions.
 */
@Suppress("ListIterator")
private fun List<SemanticsNode>.calculateBoundingBox(): Rect {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    forEach { node ->
        val position = node.getPositionOnScreen()
        val bounds = node.boundsInRoot

        val left = position.x + bounds.left
        val top = position.y + bounds.top
        val right = left + bounds.width
        val bottom = top + bounds.height

        if (left < minX) minX = left
        if (top < minY) minY = top
        if (right > maxX) maxX = right
        if (bottom > maxY) maxY = bottom
    }

    return Rect(minX.roundToInt(), minY.roundToInt(), maxX.roundToInt(), maxY.roundToInt())
}

/**
 * Calculates the absolute position of the root view for the given [SemanticsNode] on the physical
 * screen.
 *
 * This is necessary for multi-window screenshots because bounds within a Compose root do not
 * account for where that root is physically located on the device display.
 */
private fun SemanticsNode.getPositionOnScreen(): Offset {
    val locationOnScreen = intArrayOf(0, 0)
    view.getLocationOnScreen(locationOnScreen)
    return Offset(locationOnScreen[0].toFloat(), locationOnScreen[1].toFloat())
}

/**
 * Recursively traverses up the Android [View] hierarchy to locate the [DialogWindowProvider].
 *
 * @param view The starting [View] to begin the upward traversal.
 * @return The [DialogWindowProvider] if found, or `null` if the view is not part of a Dialog.
 */
internal fun findDialogWindowProviderInParent(view: View): DialogWindowProvider? {
    if (view is DialogWindowProvider) return view
    val parent = view.parent as? View ?: return null
    return findDialogWindowProviderInParent(parent)
}

private fun Context.getActivityWindow(): Window {
    fun Context.getActivity(): Activity {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else ->
                throw IllegalStateException(
                    "Context is not an Activity context, but a ${javaClass.simpleName} context. " +
                        "An Activity context is required to get a Window instance"
                )
        }
    }
    return getActivity().window
}

private val SemanticsNode.view: View
    get() = (root as ViewRootForTest).view

private val SemanticsNode.isInsidePopup: Boolean
    get() =
        findClosestParentNode(includeSelf = true) {
            it.config.contains(SemanticsProperties.IsPopup)
        } != null
