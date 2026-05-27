/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.demos.accessibility

import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val green = Color.Green.copy(alpha = 0.3f)
private val red = Color.Red.copy(alpha = 0.3f)

/**
 * Demonstrates that placing testTag() before padding() in a Text modifier chain causes
 * accessibility character bounding boxes to be offset from the actual rendered text.
 *
 * Each case draws two sets of filled rectangles over the text:
 * - Green: getBoundingBox() rects (always correct, drawn in local space)
 * - Red: accessibility API bounding box rects (converted from screen to local)
 *
 * When aligned, the colors blend to a brownish/olive tone. When offset, you'll see separate red and
 * green bands.
 */
@Composable
fun TextBoundingBoxDemo() {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(background = green)) { append("Green") }
                append(" = getBoundingBox (local).\n")
                withStyle(SpanStyle(background = red)) { append("Red") }
                append(" = accessibility API bounding boxes.\n")
                withStyle(SpanStyle(background = Color(0x4D996600))) { append("olive/brown") }
                append("-ish = When the above two align.\n")
                append("Separate red and green bands indicate an offset bug.")
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Case 1: semantics BEFORE padding", fontWeight = FontWeight.Bold)
            TextCase(
                label = "Case1",
                modifier =
                    Modifier.semantics {}
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray)
                        .padding(24.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Case 2: semantics AFTER padding", fontWeight = FontWeight.Bold)
            TextCase(
                label = "Case2",
                modifier =
                    Modifier.fillMaxWidth()
                        .border(1.dp, Color.LightGray)
                        .padding(24.dp)
                        .semantics {},
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Case 3: No semantics", fontWeight = FontWeight.Bold)
            TextCase(
                label = "Case3",
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray).padding(24.dp),
            )
        }
    }
}

@Composable
private fun TextCase(label: String, modifier: Modifier) {
    val view = LocalView.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var axBoundingBoxes by remember { mutableStateOf<List<RectF?>>(emptyList()) }
    var positionOnScreen by remember { mutableStateOf(Offset.Zero) }

    val text = "$label: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod"

    Text(
        text = text,
        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        maxLines = Int.MAX_VALUE,
        onTextLayout = { textLayoutResult = it },
        modifier =
            modifier
                .onGloballyPositioned { coords ->
                    positionOnScreen = coords.positionOnScreen()
                    if (Build.VERSION.SDK_INT >= 34) {
                        axBoundingBoxes = queryAxBoundingBoxes(view, text)
                    }
                }
                .drawWithContent {
                    drawContent()

                    val tlr = textLayoutResult ?: return@drawWithContent

                    // GREEN: getBoundingBox in local coords — always correct
                    for (i in 0 until text.length) {
                        val box = tlr.getBoundingBox(i)
                        if (box.width > 0 && box.height > 0) {
                            drawRect(
                                color = green,
                                topLeft = Offset(box.left, box.top),
                                size = Size(box.width, box.height),
                            )
                        }
                    }

                    // RED: accessibility API bounding boxes (screen → local)
                    val screenPos = positionOnScreen
                    for (rect in axBoundingBoxes) {
                        if (rect == null || rect.isEmpty) continue
                        drawRect(
                            color = red,
                            topLeft = Offset(rect.left - screenPos.x, rect.top - screenPos.y),
                            size = Size(rect.width(), rect.height()),
                        )
                    }
                },
    )
}

@RequiresApi(34)
private fun queryAxBoundingBoxes(hostView: View, text: String): List<RectF?> {
    val rootInfo = hostView.createAccessibilityNodeInfo() ?: return emptyList()
    rootInfo.setQueryFromAppProcessEnabled(hostView, true)

    // Walk the tree to find a node whose text starts with ours.
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(rootInfo)
    var textNode: AccessibilityNodeInfo? = null
    while (queue.isNotEmpty() && textNode == null) {
        val node = queue.removeFirst()
        if (node.text?.startsWith(text.take(30)) == true) {
            textNode = node
        } else {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
    }
    textNode ?: return emptyList()

    val args =
        Bundle().apply {
            putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0)
            putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, text.length)
        }
    if (
        !textNode.refreshWithExtraData(
            AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
            args,
        )
    ) {
        return emptyList()
    }

    @Suppress("DEPRECATION")
    return textNode.extras
        .getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
        ?.map { it as? RectF } ?: emptyList()
}
