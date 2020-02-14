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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.graphics.painter.ImagePainter
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Image
import androidx.ui.layout.Spacer
import androidx.ui.material.AppBarIcon
import androidx.ui.material.BottomAppBar
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar

@Sampled
@Composable
fun SimpleTopAppBar(
    getMyActionImage: () -> Image,
    getMyNavigationImage: () -> Image
) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(ImagePainter(someNavigationImage)) { /* doSomething()*/ }
    }

    TopAppBar(
        title = { Text("Simple TopAppBar") },
        navigationIcon = navigationIcon,
        actions = { // RowScope here, so these icons will be placed horizontally
            AppBarIcon(ImagePainter(someActionImage)) { /* doSomething()*/ }
            AppBarIcon(ImagePainter(someActionImage)) { /* doSomething()*/ }
        }
    )
}

@Sampled
@Composable
fun SimpleBottomAppBar(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    BottomAppBar {
        AppBarIcon(ImagePainter(someNavigationImage)) { /* doSomething()*/ }
        // The actions should be at the end of the BottomAppBar
        Spacer(LayoutFlexible(1f))
        AppBarIcon(ImagePainter(someActionImage)) { /* doSomething()*/ }
        AppBarIcon(ImagePainter(someActionImage)) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarCutoutWithScaffold(getMyActionImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val fabShape = CircleShape

    Scaffold(
        bottomAppBar = { fabConfiguration ->
            BottomAppBar(fabConfiguration = fabConfiguration, cutoutShape = fabShape) {
                AppBarIcon(ImagePainter(someActionImage)) { /* doSomething()*/ }
                AppBarIcon(ImagePainter(someActionImage)) { /* doSomething()*/ }
            }
        },
        floatingActionButton = {
            FloatingActionButton(icon = someActionImage, shape = fabShape)
        },
        floatingActionButtonPosition = Scaffold.FabPosition.EndDocked
    ) {
        Text("Your app goes here")
    }
}