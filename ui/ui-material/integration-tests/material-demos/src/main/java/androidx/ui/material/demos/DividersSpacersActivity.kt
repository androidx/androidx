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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.Spacer
import androidx.ui.layout.Row
import androidx.ui.layout.LayoutWidth
import androidx.ui.material.Divider
import androidx.ui.material.MaterialTheme

class DividersSpacersActivity : MaterialDemoActivity() {

    @Composable
    override fun materialContent() {
        DividersDemo()
    }

    @Composable
    fun DividersDemo() {
        val items = listOf(
            "Lorem ipsum dolor sit amet.",
            "Morbi ac purus eget quam dapibus cursus.",
            "Integer viverra libero eget.",
            "Mauris tristique arcu nec aliquam.",
            "Vivamus euismod augue eget maximus."
        )
        val color = Color(0xFFE91E63)
        val dividerColor = Color(0xFFC6C6C6)
        val blackColor = Color.Black
        Column {
            Column {
                items.forEachIndexed { index, text ->
                    Item(text = text, color = color)
                    if (index != items.lastIndex) {
                        Divider(color = dividerColor, indent = ItemSize)
                    }
                }
            }
            Spacer(LayoutHeight(30.dp))
            Divider(height = 2.dp, color = blackColor)
            Spacer(LayoutHeight(10.dp))
            Column {
                items.forEach { text ->
                    Item(text = text)
                    Divider(color = dividerColor, height = 0.5.dp)
                }
            }
        }
    }

    @Composable
    fun Item(text: String, color: Color? = null) {
        val avatarSize = ItemSize - ItemPadding * 2
        val textStyle = (+MaterialTheme.typography()).body1
        Container(height = ItemSize, padding = EdgeInsets(ItemPadding)) {
            Row {
                if (color != null) {
                    ColoredRect(
                        width = avatarSize,
                        height = avatarSize,
                        color = color,
                        modifier = LayoutGravity.Center
                    )
                    Spacer(LayoutWidth(ItemPadding))
                }
                Text(text = text, style = textStyle, modifier = LayoutGravity.Center)
            }
        }
    }

    private val ItemSize = 55.dp
    private val ItemPadding = 7.5.dp
}