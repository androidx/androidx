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

package bugs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UIScrollView
import platform.UIKit.UIView

private val ItemsHeight: Double = 200.0
private val ItemsWidth: Double = 100.0
private val Colors = listOf(
    Color.Blue,
    Color.Red,
    Color.Green,
    Color.Black,
    Color.Cyan,
    Color.Gray,
    Color.Red,
    Color.Yellow,
    Color.LightGray,
    Color.Magenta
)

val ComposeAndNativeScroll = Screen.Example("ScrollDraggingTest") {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Compose:")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemsHeight.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.height(ItemsHeight.dp)) {
                (Colors + Colors).forEach { item ->
                    Box(
                        Modifier
                            .size(width = ItemsWidth.dp, height = ItemsHeight.dp)
                            .background(item)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("UIKit:")
        Box(modifier = Modifier.height(ItemsHeight.dp)) {
            UIKitView(::makeScrollView, Modifier.fillMaxSize())
        }
    }
}

private fun makeScrollView(): UIScrollView {
    val scroll = UIScrollView()
    val uiColors = listOf(
        UIColor.blueColor,
        UIColor.redColor,
        UIColor.greenColor,
        UIColor.blackColor,
        UIColor.cyanColor,
        UIColor.grayColor,
        UIColor.redColor,
        UIColor.yellowColor,
        UIColor.brownColor,
        UIColor.magentaColor
    )
    scroll.setContentSize(
        CGSizeMake(width = uiColors.count() * 2 * ItemsWidth, height = ItemsHeight)
    )

    (uiColors + uiColors).fastForEachIndexed { i, uiColor ->
        scroll.addSubview(
            UIView(
                frame = CGRectMake(
                    i.toDouble() * ItemsWidth,
                    0.0,
                    ItemsWidth,
                    ItemsHeight
                )
            ).also {
                it.setBackgroundColor(uiColor)
            })
    }
    return scroll
}
