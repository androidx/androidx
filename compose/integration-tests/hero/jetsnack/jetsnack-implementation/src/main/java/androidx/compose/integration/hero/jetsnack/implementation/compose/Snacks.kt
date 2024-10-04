/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.hero.jetsnack.implementation.compose

import androidx.activity.compose.ReportDrawn
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.integration.hero.jetsnack.implementation.CollectionType
import androidx.compose.integration.hero.jetsnack.implementation.Snack
import androidx.compose.integration.hero.jetsnack.implementation.SnackCollection
import androidx.compose.integration.hero.jetsnack.implementation.compose.theme.JetsnackTheme
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.trace

@Composable
fun SnackCollection(
    snackCollection: SnackCollection,
    onSnackClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    highlight: Boolean = true
) =
    trace("SnackCollection") {
        Column(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 56.dp).padding(start = 24.dp)
            ) {
                Text(
                    text = snackCollection.name,
                    style = MaterialTheme.typography.h6,
                    color = JetsnackTheme.colors.brand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).wrapContentWidth(Alignment.Start)
                )
                IconButton(
                    onClick = { /* todo */ },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        tint = JetsnackTheme.colors.brand,
                        contentDescription = null
                    )
                }
            }
            if (highlight && snackCollection.type == CollectionType.Highlight) {
                HighlightedSnacks(index, snackCollection.snacks, onSnackClick)
            } else {
                Snacks(snackCollection.snacks, onSnackClick)
            }
        }
    }

@Suppress("UNUSED_PARAMETER")
@Composable
private fun HighlightedSnacks(
    index: Int,
    snacks: List<Snack>,
    onSnackClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp)
    ) {
        itemsIndexed(snacks) { _, snack ->
            HighlightSnackItem(
                snack,
                onSnackClick,
                JetsnackTheme.colors.gradient6_1,
            )
        }
    }
}

@Composable
private fun Snacks(
    snacks: List<Snack>,
    onSnackClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, contentPadding = PaddingValues(start = 12.dp, end = 12.dp)) {
        items(snacks) { snack -> SnackItem(snack, onSnackClick) }
    }
}

@Composable
fun SnackItem(snack: Snack, onSnackClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    JetsnackSurface(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.clickable(
                        interactionSource = null,
                        indication = ripple(),
                        onClick = { onSnackClick(snack.id) }
                    )
                    .padding(8.dp)
        ) {
            SnackImage(
                imageDrawable = snack.imageDrawable,
                elevation = 4.dp,
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = snack.name,
                style = MaterialTheme.typography.subtitle1,
                color = JetsnackTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun HighlightSnackItem(
    snack: Snack,
    onSnackClick: (Long) -> Unit,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    JetsnackCard(
        modifier = modifier.size(width = 170.dp, height = 250.dp).padding(bottom = 16.dp)
    ) {
        Column(
            modifier =
                Modifier.clickable(
                        interactionSource = null,
                        indication = ripple(),
                        onClick = { onSnackClick(snack.id) }
                    )
                    .fillMaxSize()
        ) {
            Box(modifier = Modifier.height(160.dp).fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier.height(100.dp).fillMaxWidth().offsetGradientBackground(gradient)
                )
                SnackImage(
                    imageDrawable = snack.imageDrawable,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).align(Alignment.BottomCenter)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = snack.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.h6,
                color = JetsnackTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = snack.tagline,
                style = MaterialTheme.typography.body1,
                color = JetsnackTheme.colors.textHelp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun SnackImage(
    @DrawableRes imageDrawable: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp
) {
    JetsnackSurface(
        color = Color.LightGray,
        elevation = elevation,
        shape = CircleShape,
        modifier = modifier
    ) {
        ReportDrawn()

        Image(
            painter = painterResource(imageDrawable),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

fun Modifier.offsetGradientBackground(colors: List<Color>) =
    background(Brush.horizontalGradient(colors))
