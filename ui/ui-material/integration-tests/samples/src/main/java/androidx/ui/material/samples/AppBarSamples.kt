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
import androidx.compose.composer
import androidx.ui.core.Text
import androidx.ui.graphics.Color
import androidx.ui.material.AppBarIcon
import androidx.ui.material.BottomAppBar
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.TopAppBar
import androidx.ui.painting.Image

@Suppress("UNUSED_VARIABLE")
@Sampled
@Composable
fun SimpleTopAppBar(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    TopAppBar(
        title = { Text("Simple TopAppBar") },
        navigationIcon = navigationIcon,
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Suppress("UNUSED_VARIABLE")
@Sampled
@Composable
fun SimpleBottomAppBarNoFab(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Suppress("UNUSED_VARIABLE")
@Sampled
@Composable
fun SimpleBottomAppBarCenterFab(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        floatingActionButton = {
            FloatingActionButton(
                color = Color.Black,
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
        fabPosition = BottomAppBar.FabPosition.Center,
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Suppress("UNUSED_VARIABLE")
@Sampled
@Composable
fun SimpleBottomAppBarEndFab(getMyActionImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    BottomAppBar(
        floatingActionButton = {
            FloatingActionButton(
                color = Color.Black,
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
        fabPosition = BottomAppBar.FabPosition.End,
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}
