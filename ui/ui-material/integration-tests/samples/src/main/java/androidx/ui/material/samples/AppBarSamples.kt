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
import androidx.compose.composer
import androidx.compose.unaryPlus
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
import androidx.ui.material.shape.CutCornerShape
import androidx.ui.material.themeColor
import androidx.ui.text.TextStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress("UNUSED_VARIABLE")
@Composable
fun SimpleTopAppBar(getMyActionImage: () -> Image) {
    val someActionImage: Image = getMyActionImage()
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    TopAppBar(
        title = { Text("Simple TopAppBar") },
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Suppress("UNUSED_VARIABLE")
@Sampled
@Composable
fun SimpleTopAppBarNavIcon(getMyActionImage: () -> Image, getMyNavigationImage: () -> Image) {
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
        fabConfiguration = FabConfiguration {
            FloatingActionButton(
                color = +themeColor { secondary },
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
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
        fabConfiguration = FabConfiguration(fabPosition = BottomAppBar.FabPosition.End) {
            FloatingActionButton(
                color = +themeColor { secondary },
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Suppress("UNUSED_VARIABLE")
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
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        fabConfiguration = FabConfiguration(cutoutShape = CircleShape) {
            FloatingActionButton(
                color = +themeColor { secondary },
                icon = someActionImage,
                onClick = { /** doSomething() */ })
        },
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Suppress("UNUSED_VARIABLE")
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
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    BottomAppBar(
        navigationIcon = navigationIcon,
        fabConfiguration = FabConfiguration(cutoutShape = CircleShape) {
            FloatingActionButton(
                color = +themeColor { secondary },
                text = "Extended FAB",
                textStyle = TextStyle(color = Color.White),
                onClick = { /** doSomething() */ })
        },
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}

@Suppress("UNUSED_VARIABLE")
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
    val contextualActions = listOf("Action 1" to someActionImage, "action 2" to someActionImage)

    // Consider negative values to mean 'cut corner' and positive values to mean 'round corner'
    val sharpEdgePercent = -50f
    val roundEdgePercent = 45f

    // Start with sharp edges
    val animatedProgress = +animatedFloat(sharpEdgePercent)

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
                color = +themeColor { secondary },
                icon = someActionImage,
                onClick = switchShape,
                shape = fabShape
            )
        },
        contextualActions = contextualActions
    ) { actionData ->
        val (actionTitle, actionImage) = actionData
        AppBarIcon(actionImage) { /* doSomething()*/ }
    }
}
