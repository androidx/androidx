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

@file:Suppress("PLUGIN_WARNING")

package androidx.ui.framework.demos

import androidx.compose.Composable
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutTag
import androidx.ui.core.LayoutTagParentData
import androidx.ui.core.ParentData
import androidx.ui.core.tag
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.ipx

@Composable
fun HeaderFooterLayout(
    header: @Composable() () -> Unit,
    footer: @Composable() () -> Unit,
    content: @Composable() () -> Unit
) {
    Layout({
        Container(LayoutTag("header"), children = header)
        Container(LayoutTag("footer"), children = footer)
        ParentData(
            object : LayoutTagParentData {
                override val tag: Any get() = "content"
            },
            children = content
        )
    }) { measurables, constraints ->
        val headerPlaceable = measurables.first { it.tag == "header" }.measure(
            Constraints.fixed(constraints.maxWidth, 100.ipx)
        )
        val footerPadding = 50.ipx
        val footerPlaceable = measurables.first { it.tag == "footer" }.measure(
            Constraints.fixed(constraints.maxWidth - footerPadding * 2, 100.ipx)
        )
        val itemHeight =
            (constraints.maxHeight - headerPlaceable.height - footerPlaceable.height) /
                    measurables.filter { it.tag == "content" }.size
        val contentPlaceables = measurables.filter { it.tag == "content" }.map { measurable ->
            measurable.measure(Constraints.fixed(constraints.maxWidth, itemHeight))
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.place(0.ipx, 0.ipx)
            footerPlaceable.place(footerPadding, constraints.maxHeight - footerPlaceable.height)
            var top = headerPlaceable.height
            contentPlaceables.forEach { placeable ->
                placeable.place(0.ipx, top)
                top += itemHeight
            }
        }
    }
}

@Composable
fun MultipleCollectTest() {
    val header = @Composable {
        Box(LayoutSize.Fill, backgroundColor = Color(android.graphics.Color.GRAY))
    }
    val footer = @Composable {
        Box(LayoutSize.Fill, backgroundColor = Color(android.graphics.Color.BLUE))
    }
    HeaderFooterLayout(header = header, footer = footer) {
        Box(LayoutSize.Fill, backgroundColor = Color(android.graphics.Color.GREEN))
        Box(LayoutSize.Fill, backgroundColor = Color(android.graphics.Color.YELLOW))
    }
}
