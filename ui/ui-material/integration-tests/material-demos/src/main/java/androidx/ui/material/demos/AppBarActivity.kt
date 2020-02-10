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
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Text
import androidx.ui.graphics.imageFromResource
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.Spacer
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioGroup
import androidx.ui.material.demos.AppBarActivity.TopAppBarOption.Actions
import androidx.ui.material.demos.AppBarActivity.TopAppBarOption.Simple
import androidx.ui.material.samples.SimpleBottomAppBarNoFab
import androidx.ui.material.samples.SimpleTopAppBarNavIcon
import androidx.ui.material.samples.SimpleTopAppBarNavIconWithActions
import androidx.ui.unit.dp

class AppBarActivity : MaterialDemoActivity() {

    private val favouriteImage by lazy { { imageFromResource(resources, R.drawable.ic_favorite) } }
    private val navigationImage by lazy { { imageFromResource(resources, R.drawable.ic_menu) } }

    private enum class TopAppBarOption(val description: String) {
        Simple("Simple"),
        Actions("With Actions")
    }

    @Composable
    override fun materialContent() {
        var selectedTopAppBar by state { Simple }

        Column {
            Container(height = 120.dp, alignment = Alignment.TopCenter) {
                when (selectedTopAppBar) {
                    Simple -> SimpleTopAppBarNavIcon(navigationImage)
                    Actions -> SimpleTopAppBarNavIconWithActions(
                        favouriteImage,
                        navigationImage
                    )
                }
            }
            DemoText("TopAppBar options")
            RadioGroup {
                TopAppBarOption.values().forEach { topAppBar ->
                    RadioGroupTextItem(
                        selected = (topAppBar == selectedTopAppBar),
                        onSelect = { selectedTopAppBar = topAppBar },
                        text = topAppBar.description
                    )
                }
            }
            Spacer(LayoutFlexible(1f))
            DemoText("BottomAppBar")
            Spacer(LayoutHeight(10.dp))
            SimpleBottomAppBarNoFab(
                favouriteImage,
                navigationImage
            )
        }
    }

    @Composable
    private fun DemoText(text: String) {
        Text(text, style = MaterialTheme.typography().h6)
    }
}
