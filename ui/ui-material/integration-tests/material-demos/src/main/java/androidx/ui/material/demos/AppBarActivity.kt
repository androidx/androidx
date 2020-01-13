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
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioGroup
import androidx.ui.material.demos.AppBarActivity.BottomAppBarOption.CenterFab
import androidx.ui.material.demos.AppBarActivity.BottomAppBarOption.CutoutFab
import androidx.ui.material.demos.AppBarActivity.BottomAppBarOption.EndFab
import androidx.ui.material.demos.AppBarActivity.BottomAppBarOption.ExtendedCutoutFab
import androidx.ui.material.demos.AppBarActivity.BottomAppBarOption.FancyAnimatingCutoutFab
import androidx.ui.material.demos.AppBarActivity.BottomAppBarOption.NoFab
import androidx.ui.material.demos.AppBarActivity.TopAppBarOption.Actions
import androidx.ui.material.demos.AppBarActivity.TopAppBarOption.Simple
import androidx.ui.material.samples.SimpleBottomAppBarCenterFab
import androidx.ui.material.samples.SimpleBottomAppBarCutoutFab
import androidx.ui.material.samples.SimpleBottomAppBarEndFab
import androidx.ui.material.samples.SimpleBottomAppBarExtendedCutoutFab
import androidx.ui.material.samples.SimpleBottomAppBarFancyAnimatingCutoutFab
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

    private enum class BottomAppBarOption(val description: String) {
        NoFab("No FAB"),
        CenterFab("Center FAB"),
        EndFab("End FAB"),
        CutoutFab("Cutout FAB"),
        ExtendedCutoutFab("Extended Cutout FAB"),
        FancyAnimatingCutoutFab("Fancy Animating Cutout FAB")
    }

    private val topAppBarOptions = listOf(Simple, Actions)
    private val bottomAppBarOptions = listOf(
        NoFab, CenterFab, EndFab, CutoutFab, ExtendedCutoutFab, FancyAnimatingCutoutFab
    )

    @Composable
    override fun materialContent() {
        var selectedTopAppBar by state { Simple }
        var selectedBottomAppBar by state { NoFab }

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
            Column(LayoutFlexible(1f), arrangement = Arrangement.SpaceBetween) {
                DemoText("TopAppBar options")
                RadioGroup {
                    topAppBarOptions.forEach { topAppBar ->
                        RadioGroupTextItem(
                            selected = (topAppBar == selectedTopAppBar),
                            onSelect = { selectedTopAppBar = topAppBar },
                            text = topAppBar.description
                        )
                    }
                }
                DemoText("BottomAppBar options")
                RadioGroup {
                    bottomAppBarOptions.forEach { bottomAppBar ->
                        RadioGroupTextItem(
                            selected = (bottomAppBar == selectedBottomAppBar),
                            onSelect = { selectedBottomAppBar = bottomAppBar },
                            text = bottomAppBar.description
                        )
                    }
                }
            }
            Container(height = 120.dp, alignment = Alignment.BottomCenter) {
                when (selectedBottomAppBar) {
                    NoFab -> SimpleBottomAppBarNoFab(favouriteImage, navigationImage)
                    CenterFab -> SimpleBottomAppBarCenterFab(
                        favouriteImage,
                        navigationImage
                    )
                    EndFab -> SimpleBottomAppBarEndFab(favouriteImage)
                    CutoutFab -> SimpleBottomAppBarCutoutFab(
                        favouriteImage,
                        navigationImage
                    )
                    ExtendedCutoutFab -> SimpleBottomAppBarExtendedCutoutFab(
                        favouriteImage,
                        navigationImage
                    )
                    FancyAnimatingCutoutFab -> SimpleBottomAppBarFancyAnimatingCutoutFab(
                        favouriteImage,
                        navigationImage
                    )
                }
            }
        }
    }

    @Composable
    private fun DemoText(text: String) {
        Text(text, style = MaterialTheme.typography().h6)
    }
}
