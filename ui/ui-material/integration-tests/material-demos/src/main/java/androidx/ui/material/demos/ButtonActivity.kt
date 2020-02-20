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
import androidx.ui.core.Text
import androidx.ui.foundation.Border
import androidx.ui.foundation.shape.GenericShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Image
import androidx.ui.graphics.imageFromResource
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.material.Button
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.MaterialTheme
import androidx.ui.material.OutlinedButton
import androidx.ui.material.samples.ButtonSample
import androidx.ui.material.samples.IconButtonSample
import androidx.ui.material.samples.IconToggleButtonSample
import androidx.ui.material.samples.OutlinedButtonSample
import androidx.ui.material.samples.TextButtonSample
import androidx.ui.unit.dp

class ButtonActivity : MaterialDemoActivity() {

    private val DefaultSpace = 20.dp

    @Composable
    override fun materialContent() {
        val icon = imageFromResource(resources, R.drawable.ic_favorite)

        Column(LayoutPadding(10.dp)) {
            Buttons()
            Spacer(LayoutHeight(DefaultSpace))
            Fabs(icon)
            Spacer(LayoutHeight(DefaultSpace))
            IconButtons()
            Spacer(LayoutHeight(DefaultSpace))
            CustomShapeButton()
        }
    }

    @Composable
    private fun Buttons() {
        Text("Buttons")
        Spacer(LayoutHeight(DefaultSpace))
        Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
            ButtonSample()
            OutlinedButtonSample()
            TextButtonSample()
        }

        Spacer(LayoutHeight(DefaultSpace))

        Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
            Button(backgroundColor = MaterialTheme.colors().secondary, onClick = {}) {
                Text("Secondary Color")
            }
            // TODO(Andrey): Disabled button has wrong bg and text color for now.
            // Need to figure out where will we store their styling. Not a part of
            // ColorPalette right now and specs are not clear about this.
            Button {
                Text("DISABLED. TODO")
            }
        }
    }

    @Composable
    private fun Fabs(icon: Image) {
        Text("Floating action buttons")
        Spacer(LayoutHeight(DefaultSpace))

        Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
            FloatingActionButton(
                icon = icon,
                onClick = {}
            )
            FloatingActionButton(
                text = "EXTENDED",
                onClick = {}
            )
            FloatingActionButton(
                icon = icon,
                text = "ADD TO FAVS",
                color = MaterialTheme.colors().error,
                onClick = {}
            )
        }
    }

    @Composable
    private fun IconButtons() {
        Text("Icon buttons")
        Spacer(LayoutHeight(DefaultSpace))

        Row {
            IconButtonSample()
            IconToggleButtonSample()
        }
    }

    @Composable
    private fun CustomShapeButton() {
        Text("Custom shape button")
        Spacer(LayoutHeight(DefaultSpace))
        OutlinedButton(
            modifier = LayoutSize(100.dp),
            onClick = {},
            shape = TriangleShape,
            backgroundColor = Color.Yellow,
            border = Border(size = 2.dp, color = Color.Black)
        ) {
            Text("Ok")
        }
    }

    private val TriangleShape = GenericShape { size ->
        moveTo(size.width.value / 2f, 0f)
        lineTo(size.width.value, size.height.value)
        lineTo(0f, size.height.value)
    }
}
