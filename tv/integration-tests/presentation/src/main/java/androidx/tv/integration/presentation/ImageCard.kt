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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImageCard(
    movie: Movie,
    aspectRatio: Float = 16f / 9,
    customCardWidth: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val aspect = if (aspectRatio == 16f / 9)
        "orientation/vod_art_16x9"
    else
        "orientation/vod_art_2x3"
    val scaleMax = if (aspectRatio == 16f / 9) 1.1f else 1.025f
    val cardWidth = customCardWidth ?: if (aspectRatio == 16f / 9) 200.dp else 172.dp

    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.Transparent
    )
    val scale by animateFloatAsState(targetValue = if (isFocused) scaleMax else 1f)

    Column(
        modifier = Modifier
            .width(cardWidth)
            .scale(scale)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .border(2.dp, borderColor, shape)
                .clip(shape)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
        ) {
            AppAsyncImage(imageUrl = getMovieImageUrl(movie, aspect = aspect))
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.name,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
