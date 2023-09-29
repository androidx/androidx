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

package androidx.compose.mpp.demo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.mpp.demo.SliderSetting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun PopupExample() {
    val scrollState = rememberScrollState()
    Column(Modifier
        .padding(5.dp)
        .verticalScroll(scrollState)
    ) {
        val properties = EditPopupProperties()
        val fillMaxSize = EditBooleanSetting("fillMaxSize", false)
        val windowInsets = EditBooleanSetting("windowInsets", false)
        val absolutePosition = EditBooleanSetting("absolutePosition", false)

        var offsetX by remember { mutableStateOf(0f) }
        SliderSetting("OffsetX", offsetX, -500f..500f) { offsetX = it }
        var offsetY by remember { mutableStateOf(0f) }
        SliderSetting("OffsetY", offsetY, -500f..500f) { offsetY = it }
        val offset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt())

        var open by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick = { open = true }) {
                Text("Open Popup")
            }
        }
        if (open) {
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset = if (absolutePosition) {
                        offset
                    } else {
                        anchorBounds.topLeft + offset
                    }
                },
                onDismissRequest = { open = false },
                properties = properties
            ) {
                val modifier = if (fillMaxSize) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.size(400.dp, 300.dp)
                }
                Box(modifier
                    .background(Color.Yellow)
                    .clickable { open = false }
                ) {
                    val contentModifier = if (windowInsets) {
                        Modifier.windowInsetsPadding(WindowInsets.systemBars)
                    } else {
                        Modifier
                    }
                    Box(contentModifier) {
                        Text("Example Popup content. Click to close")
                    }
                }
            }
        }

        // Additional examples
        ClickCounterPopupExample()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditPopupProperties(): PopupProperties {
    var popupProperties by remember { mutableStateOf(PopupProperties()) }
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray, RoundedCornerShape(5.dp))
            .padding(5.dp)
    ) {
        Text("PopupProperties")
        Spacer(Modifier.height(5.dp))

        val focusable = EditBooleanSetting("focusable", false)
        val dismissOnBackPress = EditBooleanSetting("dismissOnBackPress", true)
        val dismissOnClickOutside = EditBooleanSetting("dismissOnClickOutside", true)
        val clippingEnabled = EditBooleanSetting("clippingEnabled", true)
        val usePlatformDefaultWidth = EditBooleanSetting("usePlatformDefaultWidth", false)
        val usePlatformInsets = EditBooleanSetting("usePlatformInsets", true)
        popupProperties = PopupProperties(
            focusable = focusable,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            clippingEnabled = clippingEnabled,
            usePlatformDefaultWidth = usePlatformDefaultWidth,
            usePlatformInsets = usePlatformInsets
        )
    }
    return popupProperties
}

@Composable
private fun EditBooleanSetting(label: String, defaultValue: Boolean): Boolean {
    var value by remember { mutableStateOf(defaultValue) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(value, { value = it })
        Text(label)
    }
    return value
}

@Composable
private fun ClickCounterPopupExample() {
    var popup1 by remember { mutableStateOf(0) }
    var popup2 by remember { mutableStateOf(0) }
    var popup3 by remember { mutableStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Button(onClick = { popup1++ }) {
            Text("Click Counter")
        }
    }
    if (popup1 > 0) {
        ClickCounterPopup(
            text = "Click count = $popup1",
            offset = IntOffset(50, 250),
            properties = PopupProperties(
                focusable = false,
                dismissOnClickOutside = false
            ),
            onClose = { popup1 = 0 },
            onNext = { popup2++ }
        )
    }
    if (popup2 > 0) {
        ClickCounterPopup(
            text = "Click count = $popup2",
            offset = IntOffset(200, 150),
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true
            ),
            onClose = { popup2 = 0 },
            onNext = { popup3++ }
        )
    }
    if (popup3 > 0) {
        ClickCounterPopup(
            text = "Click count = $popup3",
            offset = IntOffset(100, 50),
            properties = PopupProperties(
                focusable = false,
                dismissOnClickOutside = true
            ),
            onClose = { popup3 = 0 }
        )
    }
}

@Composable
private fun ClickCounterPopup(
    text: String,
    offset: IntOffset,
    properties: PopupProperties,
    onClose: () -> Unit,
    onNext: (() -> Unit)? = null
) {
    Popup(
        offset = offset,
        onDismissRequest = onClose,
        properties = properties
    ) {
        Surface(
            color = MaterialTheme.colors.background,
            modifier = Modifier
                .size(300.dp, 200.dp)
                .border(1.dp, Color.Black)
        ) {
            Column(Modifier.padding(5.dp)) {
                Text(text = text)
                Text(text = "focusable = ${properties.focusable}")
                Text(text = "dismissOnClickOutside = ${properties.dismissOnClickOutside}")
                Spacer(modifier = Modifier.weight(1f))
                Row {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onClose
                    ) {
                        Text(text = "Close")
                    }
                    if (onNext != null) {
                        Spacer(Modifier.size(5.dp))
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onNext
                        ) {
                            Text(text = "Next")
                        }
                    }
                }
            }
        }
    }
}
