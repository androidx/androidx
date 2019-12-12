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

import androidx.animation.TweenBuilder
import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.animation.animatedFloat
import androidx.ui.core.Text
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.material.AppBarIcon
import androidx.ui.material.BottomAppBar
import androidx.ui.material.BottomAppBar.FabConfiguration
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.TopAppBar
import androidx.ui.graphics.Image
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.material.MaterialTheme
import androidx.ui.text.TextStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Sampled
@Composable
fun SimpleTopAppBarNavIcon(getMyNavigationImage: () -> Image) {
    val someNavigationImage: Image = getMyNavigationImage()

    TopAppBar(
        title = { Text("Simple TopAppBar") },
        navigationIcon = { AppBarIcon(someNavigationImage) { /* doSomething() */ } }
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
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    TopAppBar(
        title = { Text("Simple TopAppBar") },
        navigationIcon = navigationIcon,
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarNoFab(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarCenterFab(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        fabConfiguration = FabConfiguration {
            FloatingActionButton(
                color = MaterialTheme.colors().secondary,
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarEndFab(getMyActionImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val actionData = listOf(someActionImage, someActionImage)

    BottomAppBar(
        fabConfiguration = FabConfiguration(fabPosition = BottomAppBar.FabPosition.End) {
            FloatingActionButton(
                color = MaterialTheme.colors().secondary,
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarCutoutFab(
    getMyActionImage: () -> Image,
    getMyNavigationImage: () -> Image
) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        fabConfiguration = FabConfiguration(cutoutShape = CircleShape) {
            FloatingActionButton(
                color = MaterialTheme.colors().secondary,
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarExtendedCutoutFab(
    getMyActionImage: () -> Image,
    getMyNavigationImage: () -> Image
) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        fabConfiguration = FabConfiguration(cutoutShape = CircleShape) {
            FloatingActionButton(
                color = MaterialTheme.colors().secondary,
                text = "Extended FAB",
                textStyle = TextStyle(color = Color.White),
                onClick = { /** doSomething() */ })
        },
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Sampled
@Composable
fun SimpleBottomAppBarFancyAnimatingCutoutFab(
    getMyActionImage: () -> Image,
    getMyNavigationImage: () -> Image
) {
    val someActionImage: Image = getMyActionImage()
    val someNavigationImage: Image = getMyNavigationImage()

    val navigationIcon: @Composable() () -> Unit = {
        AppBarIcon(someNavigationImage) { /* doSomething()*/ }
    }
    val actionData = listOf(someActionImage, someActionImage)

    // Consider negative values to mean 'cut corner' and positive values to mean 'round corner'
    val sharpEdgePercent = -50f
    val roundEdgePercent = 45f

    // Start with sharp edges
    val animatedProgress = animatedFloat(sharpEdgePercent)

    val progress = animatedProgress.value.roundToInt()

    // When progress is 0, there is no modification to the edges so we are just drawing a rectangle.
    // This allows for a smooth transition between cut corners and round corners.
    val fabShape = if (progress < 0) {
        CutCornerShape(abs(progress))
    } else {
        RoundedCornerShape(progress)
    }

    val switchShape = {
        val target = animatedProgress.targetValue
        val nextTarget = if (target == roundEdgePercent) sharpEdgePercent else roundEdgePercent
        animatedProgress.animateTo(
            targetValue = nextTarget,
            anim = TweenBuilder<Float>().apply { duration = 600 }
        )
    }

    BottomAppBar(
        navigationIcon = navigationIcon,
        fabConfiguration = FabConfiguration(cutoutShape = fabShape) {
            FloatingActionButton(
                color = MaterialTheme.colors().secondary,
                icon = someActionImage,
                onClick = switchShape,
                shape = fabShape
            )
        },
        actionData = actionData
    ) { actionImage ->
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}
