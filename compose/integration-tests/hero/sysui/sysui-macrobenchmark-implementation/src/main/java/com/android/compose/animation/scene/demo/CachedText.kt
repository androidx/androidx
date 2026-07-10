/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow

/**
 * A text whose measures can be cached outside composition using [textMeasurer].
 *
 * Important: DO NOT USE THIS IN PRODUCTION CODE. You should always use the Text() or BasicText()
 * composables instead. This was introduced only to optimize the SceneTransitionLayout benchmarks
 * and make their frame time metrics more stable.
 */
@Composable
fun CachedText(
    text: String,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    color: ColorProducer? = null,
) {
    val lastLayoutResult = remember {
        object {
            var value: TextLayoutResult? = null
        }
    }

    val localColor = if (color != null) null else LocalContentColor.current
    Layout(
        modifier =
            modifier.drawBehind {
                val textLayoutResult =
                    lastLayoutResult.value ?: error("draw happened before layout")
                val color =
                    color?.let { it() }
                        ?: localColor
                        ?: error("localColor should not be null when color is null")
                drawText(textLayoutResult, color)
            }
    ) { _, constraints ->
        val layoutResult =
            textMeasurer.measure(text, style, overflow, softWrap, maxLines, constraints).also {
                lastLayoutResult.value = it
            }
        layout(layoutResult.size.width, layoutResult.size.height) {}
    }
}
