/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.layout.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.tag
import androidx.ui.foundation.Text
import androidx.ui.layout.ConstraintLayout
import androidx.ui.layout.ConstraintSet2
import androidx.ui.layout.Dimension
import androidx.ui.layout.atMost
import androidx.ui.unit.dp

@Sampled
@Composable
fun DemoInlineDSL() {
    ConstraintLayout {
        val (text1, text2, text3) = createRefs()

        Text("Text1", Modifier.constrainAs(text1) {
            start.linkTo(text2.end, margin = 20.dp)
        })
        Text("Text2", Modifier.constrainAs(text2) {
            centerTo(parent)
        })

        val barrier = createBottomBarrier(text1, text2)
        Text("This is a very long text", Modifier.constrainAs(text3) {
            top.linkTo(barrier, margin = 20.dp)
            centerHorizontallyTo(parent)
            width = Dimension.preferredWrapContent.atMost(40.dp)
        })
    }
}

@Sampled
@Composable
fun DemoConstraintSet() {
    ConstraintLayout(ConstraintSet2 {
        val text1 = createRefFor("text1")
        val text2 = createRefFor("text2")
        val text3 = createRefFor("text3")

        constrain(text1) {
            start.linkTo(text2.end, margin = 20.dp)
        }
        constrain(text2) {
            centerTo(parent)
        }

        val barrier = createBottomBarrier(text1, text2)
        constrain(text3) {
            top.linkTo(barrier, margin = 20.dp)
            centerHorizontallyTo(parent)
            width = Dimension.preferredWrapContent.atMost(40.dp)
        }
    }) {
        Text("Text1", Modifier.tag("text1"))
        Text("Text2", Modifier.tag("text2"))
        Text("This is a very long text", Modifier.tag("text3"))
    }
}