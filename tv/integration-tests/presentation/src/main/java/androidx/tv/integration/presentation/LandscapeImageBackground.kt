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

package androidx.tv.integration.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun LandscapeImageBackground(movie: Movie, aspect: String = "orientation/iconic_16x9") {
    val navigationGradient = Brush.verticalGradient(
        colors = listOf(pageColor, Color.Transparent),
        startY = 0f,
        endY = 200f
    )
    var height by remember {
        mutableStateOf(0f)
    }

    val navigationGradientBottom = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            pageColor
        ),
        startY = 50f,
        endY = height,
    )

    val horizontalGradient = Brush.horizontalGradient(
        colors = listOf(pageColor, Color.Transparent),
        startX = 1400f,
        endX = 900f,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { height = it.size.height.toFloat() }
    ) {
        AppAsyncImage(
            imageUrl = getMovieImageUrl(
                movie = movie,
                aspect = aspect
            ),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = null
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(navigationGradientBottom)
                .background(navigationGradient)
                .background(horizontalGradient)
        )
    }
}
