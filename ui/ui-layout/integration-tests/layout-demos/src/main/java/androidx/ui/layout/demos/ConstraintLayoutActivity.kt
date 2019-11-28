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

package androidx.ui.layout.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.ui.core.Draw
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.setContent
import androidx.ui.core.sp
import androidx.ui.core.toRect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.constraintlayout.ConstraintLayout
import androidx.ui.layout.constraintlayout.ConstraintSet
import androidx.ui.layout.constraintlayout.ConstraintSetBuilderScope
import androidx.ui.layout.constraintlayout.Tag
import androidx.ui.text.TextStyle

/**
 * Simple ConstraintLayout demo
 */
class ConstraintLayoutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CLDemo()
        }
    }
}

@Composable
fun CLDemo() {
    ConstraintLayout(ConstraintSet {
        val text1 = tag("text1")
        val text2 = tag("text2")
        val text3 = tag("text3")
        val text4 = tag("text4")
        val text5 = tag("text5")

        text2.center()

        val half = createGuidelineFromLeft(percent = 0.5f)
        text1.apply {
            left constrainTo half
            left.margin = 50.dp
            bottom constrainTo text2.top
        }

        text3 constrainHorizontallyTo parent
        text3.horizontalBias = 0.2f
        text4 constrainHorizontallyTo parent
        text4.horizontalBias = 0.8f
        val chain = createVerticalChain(
            text3,
            text4,
            chainStyle = ConstraintSetBuilderScope.ChainStyle.Spread
        )
        chain.top.margin = 100.dp
        chain.bottom.margin = 100.dp

        val barrier = createBottomBarrier(text2, text3)
        barrier.margin = 50.dp
        text5.top constrainTo barrier
        text5.centerHorizontally()
    }) {
        Draw { canvas, parentSize ->
            canvas.drawRect(parentSize.toRect(), Paint().apply { color = Color.Blue })
        }
        Text(modifier = Tag("text1"), text = "Text1", style = TextStyle(fontSize = 10.sp))
        Text(modifier = Tag("text2"), text = "Text2", style = TextStyle(fontSize = 12.sp))
        Text(modifier = Tag("text3"), text = "Text3", style = TextStyle(fontSize = 14.sp))
        Text(modifier = Tag("text4"), text = "Text4", style = TextStyle(fontSize = 16.sp))
        Text(modifier = Tag("text5"), text = "Text5", style = TextStyle(fontSize = 18.sp))
    }
}
