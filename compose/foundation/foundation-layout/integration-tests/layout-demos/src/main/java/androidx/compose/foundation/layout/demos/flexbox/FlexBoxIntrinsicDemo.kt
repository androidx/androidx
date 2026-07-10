/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.layout.demos.flexbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
fun FlexBoxIntrinsicDemo() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Row, NoWrap (Intrinsic Width)")
        Text("Width should match the sum of all items.")
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.width(IntrinsicSize.Min).background(Color.LightGray)) {
            FlexBox(
                config = {
                    direction(FlexDirection.Row)
                    wrap(FlexWrap.NoWrap)
                },
                modifier = Modifier.border(2.dp, Color.Red),
            ) {
                Text("Hello", Modifier.padding(8.dp).background(Color.White))
                Text("World", Modifier.padding(8.dp).background(Color.White))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Row, Wrap (Intrinsic Width)")
        Text("Width should match the widest item (forcing wrap).")
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.width(IntrinsicSize.Min).background(Color.LightGray)) {
            FlexBox(
                config = {
                    direction(FlexDirection.Row)
                    wrap(FlexWrap.Wrap)
                },
                modifier = Modifier.border(2.dp, Color.Red),
            ) {
                Text("Short", Modifier.padding(8.dp).background(Color.White))
                Text("A much longer item", Modifier.padding(8.dp).background(Color.White))
                Text("Med", Modifier.padding(8.dp).background(Color.White))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Column, NoWrap (Intrinsic Height)")
        Text("Height should match the sum of all items.")
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.height(IntrinsicSize.Min).background(Color.LightGray)) {
            FlexBox(
                config = {
                    direction(FlexDirection.Column)
                    wrap(FlexWrap.NoWrap)
                },
                modifier = Modifier.border(2.dp, Color.Red),
            ) {
                Text("Item 1", Modifier.padding(8.dp).background(Color.White))
                Text("Item 2", Modifier.padding(8.dp).background(Color.White))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Column, Wrap (Intrinsic Height)")
        Text("Height should match the tallest item (forcing wrap).")
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.height(IntrinsicSize.Min).background(Color.LightGray)) {
            FlexBox(
                config = {
                    direction(FlexDirection.Column)
                    wrap(FlexWrap.Wrap)
                },
                modifier = Modifier.border(2.dp, Color.Red),
            ) {
                Text("Small", Modifier.padding(8.dp).background(Color.White))
                Text("Tall\nItem", Modifier.padding(8.dp).background(Color.White))
                Text("Med", Modifier.padding(8.dp).background(Color.White))
            }
        }
    }
}
