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
import androidx.ui.material.AppBarIcon
import androidx.ui.material.BottomAppBar
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar

@Sampled
@Composable
fun SimpleTopAppBarNavIcon(getMyNavigationImage: () -> Image) {
    val someNavigationImage: Image = getMyNavigationImage()

    TopAppBar(
        title = { Text("Simple TopAppBar") },
        navigationIcon = { AppBarIcon(ImagePainter(someNavigationImage)) { /* doSomething() */ } }
    )
}

@Sampled
@Composable
fun SimpleTopAppBarNavIconWithActions(
    getMyActionImage: () -> Image,
    getMyNavigationImage: () -> Image
) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(ImagePainter(someNavigationImage)) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    TopAppBar(
        title = { Text("Simple TopAppBar") },
        navigationIcon = navigationIcon,
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(ImagePainter(actionImage)) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarNoFab(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(ImagePainter(someNavigationImage)) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(ImagePainter(actionImage)) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarCutoutWithScaffold(getMyActionImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val fabShape = CircleShape
    Scaffold(
        bottomAppBar = { fabConfiguration ->
            val actionData = listOf(someActionImage, someActionImage)
            BottomAppBar(
                fabConfiguration = fabConfiguration,
                cutoutShape = fabShape,
                actionData = actionData
            ) { actionImage ->
                AppBarIcon(ImagePainter(actionImage)) { /* doSomething()*/ }
            }
        },
        floatingActionButton = {
            FloatingActionButton(icon = someActionImage, shape = fabShape)
        },
        floatingActionButtonPosition = Scaffold.FabPosition.EndDocked
    ) {
        Text("Your app goes there")
    }
}