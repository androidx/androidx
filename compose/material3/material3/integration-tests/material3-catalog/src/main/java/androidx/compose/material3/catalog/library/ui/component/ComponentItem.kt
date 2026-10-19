/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3.catalog.library.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.material3.catalog.library.ui.common.ItemBanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ComponentItem(
    component: Component,
    markExpressiveComponents: Boolean,
    onClick: (component: Component) -> Unit,
) {
    OutlinedCard(
        onClick = { onClick(component) },
        modifier = Modifier.height(ComponentItemHeight).padding(ComponentItemOuterPadding),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().padding(ComponentItemInnerPadding)) {
                val preview = component.preview
                if (preview != null) {
                    ComponentPreviewBitmap(
                        componentId = component.id,
                        modifier =
                            Modifier.align(Alignment.Center)
                                .padding(bottom = ComponentItemPreviewBottomPadding),
                        content = preview,
                    )
                } else {
                    Image(
                        painter = painterResource(id = component.icon),
                        contentDescription = null,
                        modifier = Modifier.size(ComponentItemIconSize).align(Alignment.Center),
                        colorFilter =
                            if (component.tintIcon) {
                                ColorFilter.tint(LocalContentColor.current)
                            } else {
                                null
                            },
                        contentScale = ContentScale.Inside,
                    )
                }
                Text(
                    text = component.name,
                    modifier = Modifier.align(Alignment.BottomStart),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (markExpressiveComponents && component.hasExpressiveExamples) {
                ItemBanner(
                    text = stringResource(R.string.expressive_banner),
                    bannerSize = ComponentItemBannerSize,
                )
            }
        }
    }
}

private val previewBitmapCache = mutableMapOf<Pair<Int, Color>, ImageBitmap>()

private val GrayscaleColorFilter =
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun ComponentPreviewBitmap(
    componentId: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val cacheKey = componentId to MaterialTheme.colorScheme.surface
    var bitmap by remember(cacheKey) { mutableStateOf(previewBitmapCache[cacheKey]) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.alpha(ComponentItemPreviewAlpha),
            colorFilter = GrayscaleColorFilter,
            contentScale = ContentScale.Fit,
        )
    } else {
        val graphicsLayer = rememberGraphicsLayer()
        val coroutineScope = rememberCoroutineScope()
        var isCapturing by remember(cacheKey) { mutableStateOf(false) }

        Box(
            modifier =
                modifier
                    .alpha(ComponentItemPreviewAlpha)
                    .graphicsLayer { colorFilter = GrayscaleColorFilter }
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                        if (!isCapturing && size.width > 0f && size.height > 0f) {
                            isCapturing = true
                            coroutineScope.launch {
                                val captured = graphicsLayer.toImageBitmap()
                                previewBitmapCache[cacheKey] = captured
                                bitmap = captured
                            }
                        }
                    }
                    .padding(4.dp)
                    .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private val ComponentItemHeight = 180.dp
private val ComponentItemOuterPadding = 4.dp
private val ComponentItemInnerPadding = 16.dp
private val ComponentItemIconSize = 80.dp
private val ComponentItemBannerSize = 80.dp
private val ComponentItemPreviewBottomPadding = 16.dp
private const val ComponentItemPreviewAlpha = 0.8f
