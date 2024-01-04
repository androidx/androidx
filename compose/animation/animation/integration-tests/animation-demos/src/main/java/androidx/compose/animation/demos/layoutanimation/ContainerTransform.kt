/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.demos.layoutanimation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Preview
@Composable
fun LocalContainerTransformDemo() {
    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            items(20) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(15.dp)
                        .background(MaterialTheme.colors.primary)
                )
            }
        }
    }
    var selectedAlignment by remember { mutableStateOf(Alignment.Center) }
    var contentScale by remember { mutableStateOf(ContentScale.FillWidth) }
    Column(
        Modifier.padding(top = 100.dp)
    ) {
        Column(
            Modifier
                .background(Color.LightGray, RoundedCornerShape(10.dp)),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedAlignment == Alignment.TopStart,
                    onClick = { selectedAlignment = Alignment.TopStart }
                )
                Text("TopStart", Modifier.padding(5.dp))
                RadioButton(
                    selected = selectedAlignment == Alignment.TopCenter,
                    onClick = { selectedAlignment = Alignment.TopCenter }
                )
                Text("TopCenter", Modifier.padding(5.dp))
                RadioButton(
                    selected = selectedAlignment == Alignment.TopEnd,
                    onClick = { selectedAlignment = Alignment.TopEnd }
                )
                Text("TopEnd", Modifier.padding(5.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedAlignment == Alignment.CenterStart,
                    onClick = { selectedAlignment = Alignment.CenterStart }
                )
                Text("CenterStart", Modifier.padding(5.dp))
                RadioButton(
                    selected = selectedAlignment == Alignment.Center,
                    onClick = { selectedAlignment = Alignment.Center }
                )
                Text("Center", Modifier.padding(5.dp))
                RadioButton(
                    selected = selectedAlignment == Alignment.CenterEnd,
                    onClick = { selectedAlignment = Alignment.CenterEnd }
                )
                Text("CenterEnd", Modifier.padding(5.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedAlignment == Alignment.BottomStart,
                    onClick = { selectedAlignment = Alignment.BottomStart }
                )
                Text("BottomStart", Modifier.padding(5.dp))
                RadioButton(
                    selected = selectedAlignment == Alignment.BottomCenter,
                    onClick = { selectedAlignment = Alignment.BottomCenter }
                )
                Text("BottomCenter", Modifier.padding(5.dp))
                RadioButton(
                    selected = selectedAlignment == Alignment.BottomEnd,
                    onClick = { selectedAlignment = Alignment.BottomEnd }
                )
                Text("BottomEnd", Modifier.padding(5.dp))
            }
        }
        Column(
            Modifier
                .background(Color.Gray, RoundedCornerShape(10.dp)),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = contentScale == ContentScale.FillWidth,
                    onClick = { contentScale = ContentScale.FillWidth }
                )
                Text("FillWidth", Modifier.padding(5.dp))
                RadioButton(
                    selected = contentScale == ContentScale.FillHeight,
                    onClick = { contentScale = ContentScale.FillHeight }
                )
                Text("FillHeight", Modifier.padding(5.dp))
                RadioButton(
                    selected = contentScale == ContentScale.FillBounds,
                    onClick = { contentScale = ContentScale.FillBounds }
                )
                Text("FillBounds", Modifier.padding(5.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = contentScale == ContentScale.Crop,
                    onClick = { contentScale = ContentScale.Crop }
                )
                Text("Crop", Modifier.padding(5.dp))
                RadioButton(
                    selected = contentScale == ContentScale.Fit,
                    onClick = { contentScale = ContentScale.Fit }
                )
                Text("Fit", Modifier.padding(5.dp))
                RadioButton(
                    selected = contentScale == ContentScale.Inside,
                    onClick = { contentScale = ContentScale.Inside }
                )
                Text("Inside", Modifier.padding(5.dp))
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        var target by remember { mutableStateOf(ContainerState.FAB) }
        // Corner radius
        val cr by animateIntAsState(if (target == ContainerState.FAB) 50 else 0)
        val padding by animateDpAsState(if (target == ContainerState.FAB) 10.dp else 0.dp)
        AnimatedContent(
            target,
            label = "",
            transitionSpec = {
                fadeIn(tween(200, delayMillis = 100)) +
                    scaleInToFitContainer(selectedAlignment, contentScale) togetherWith
                    fadeOut(tween(100)) + scaleOutToFitContainer(selectedAlignment, contentScale)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(padding)
                .clip(RoundedCornerShape(cr))
                .background(Color.White)
        ) {
            if (it == ContainerState.FAB) {
                Icon(
                    rememberVectorPainter(image = Icons.Default.Add),
                    null,
                    modifier = Modifier
                        .clickable {
                            target = ContainerState.FullScreen
                        }
                        .padding(20.dp))
            } else {
                Column(Modifier.fillMaxSize()) {
                    Icon(
                        rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack),
                        null,
                        modifier = Modifier
                            .clickable {
                                target = ContainerState.FAB
                            }
                            .padding(20.dp))
                    Spacer(Modifier.height(60.dp))
                    Text("Page Title", fontSize = 20.sp, modifier = Modifier.padding(20.dp))
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.LightGray)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Icon(rememberVectorPainter(image = Icons.Default.AccountCircle), null)
                        Spacer(Modifier.width(20.dp))
                        TextField(value = "Account Name", onValueChange = {})
                    }
                }
            }
        }
    }
}

private enum class ContainerState {
    FAB,
    FullScreen
}
