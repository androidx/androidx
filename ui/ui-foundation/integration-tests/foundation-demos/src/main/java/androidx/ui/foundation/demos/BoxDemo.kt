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

package androidx.ui.foundation.demos

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Text
import androidx.ui.foundation.samples.SimpleCircleBox
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp

@Composable
fun BoxDemo() {
    Column(Modifier.padding(10.dp)) {
        SimpleCircleBox()
        Spacer(Modifier.preferredHeight(30.dp))
        Box(
            modifier = Modifier.preferredSize(200.dp, 100.dp),
            shape = RoundedCornerShape(10.dp),
            border = Border(5.dp, Color.Gray),
            paddingStart = 20.dp,
            backgroundColor = Color.White
        ) {
            Box(
                modifier = Modifier.padding(10.dp).fillMaxSize(),
                backgroundColor = Color.DarkGray,
                shape = CutCornerShape(10.dp),
                border = Border(10.dp, Color.LightGray),
                gravity = ContentGravity.Center
            ) {
                Text("Nested boxes")
            }
        }
    }
}
