/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.tvmaterial.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A surface container using the 'background' color from the theme
            Surface(color = MaterialTheme.colorScheme.background) {
                LazyColumn {
                    item { FeaturedCarousel() }
                    item { SampleImmersiveList() }

                    items(7) { SampleLazyRow() }
                }
            }
        }
    }

    @Composable
    fun SampleLazyRow() {
        LazyRow(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)) {
            items((1..10).map { it.toString() }) { SampleCard(it) }
        }
    }

    @Composable
    private fun SampleCard(it: String) {
        var cardScale by remember { mutableStateOf(0.5f) }
        val borderGlowColorTransition = rememberInfiniteTransition()
        var initialValue by remember { mutableStateOf(Color.Transparent) }
        val glowingColor by borderGlowColorTransition.animateColor(
            initialValue = initialValue,
            targetValue = Color.Transparent,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Card(
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
                .scale(cardScale)
                .border(2.dp, glowingColor, RoundedCornerShape(12.dp))
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        cardScale = 1.0f
                        initialValue = Color.White
                    } else {
                        cardScale = 0.5f
                        initialValue = Color.Transparent
                    }
                }
                .focusable()
        ) {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(12.dp),
                color = Color.Red,
                fontWeight = FontWeight.Bold

            )
        }
    }
}