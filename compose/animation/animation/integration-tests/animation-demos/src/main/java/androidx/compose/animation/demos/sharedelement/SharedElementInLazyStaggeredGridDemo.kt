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

package androidx.compose.animation.demos.sharedelement

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.demos.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal val listCats =
    listOf(
        Cat("YT", "", R.drawable.yt_profile),
        Cat("Waffle", "", R.drawable.waffle),
        Cat("YT Also", "", R.drawable.yt_profile2),
        Cat("Pepper", "", R.drawable.pepper),
        Cat("YT Yet Again", "", R.drawable.yt_profile),
        Cat("Still Waffle", "", R.drawable.waffle),
        Cat("Pepper Take 2", "", R.drawable.pepper),
    )

@OptIn(ExperimentalSharedTransitionApi::class)
private val boundsTransition = BoundsTransform { _, _ -> tween(500) }
private val shapeForSharedElement = RoundedCornerShape(16.dp)

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun SharedElementInLazyStaggeredGridDemo() {
    var selectedCat by remember { mutableStateOf<Cat?>(null) }
    val state = rememberLazyStaggeredGridState(0)
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(selectedCat) {
            if (it == null) {
                LazyVerticalStaggeredGrid(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(Color.LightGray.copy(alpha = 0.5f))
                            .drawWithContent { drawContent() },
                    state = state,
                    columns = StaggeredGridCells.Adaptive(150.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.run { spacedBy(8.dp) },
                ) {
                    itemsIndexed(listCats, key = { index, _ -> index }) { _, cat ->
                        CatItem(
                            cat = cat,
                            onClick = { selectedCat = cat },
                            scope = this@AnimatedContent,
                            modifier = Modifier.animateItem(placementSpec = tween(500)),
                        )
                    }
                }
            } else {
                CatDetails(cat = it, this@AnimatedContent, onConfirmClick = { selectedCat = null })
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.CatItem(
    cat: Cat,
    onClick: () -> Unit,
    scope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "${cat.name}-bounds"),
                    boundsTransform = boundsTransition,
                    animatedVisibilityScope = scope,
                    clipInOverlayDuringTransition = OverlayClip(shapeForSharedElement),
                )
                .background(Color.White, shapeForSharedElement)
                .clip(shapeForSharedElement)
    ) {
        CatContent(
            cat = cat,
            modifier =
                Modifier.sharedElement(
                    rememberSharedContentState(key = cat.name),
                    animatedVisibilityScope = scope,
                    boundsTransform = boundsTransition,
                ),
            onClick = onClick,
        )
    }
}

@SuppressLint("UnnecessaryLambdaCreation")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.CatDetails(
    cat: Cat,
    scope: AnimatedVisibilityScope,
    onConfirmClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier.padding(horizontal = 16.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${cat.name}-bounds"),
                        animatedVisibilityScope = scope,
                        boundsTransform = boundsTransition,
                        clipInOverlayDuringTransition = OverlayClip(shapeForSharedElement),
                    )
                    .background(Color.White, shapeForSharedElement)
                    .clip(shapeForSharedElement)
        ) {
            CatContent(
                cat = cat,
                modifier =
                    Modifier.sharedElement(
                        rememberSharedContentState(key = cat.name),
                        animatedVisibilityScope = scope,
                        boundsTransform = boundsTransition,
                    ),
                onClick = { onConfirmClick() },
            )
            Text(
                text =
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent fringilla" +
                        " mollis efficitur. Maecenas sit amet urna eu urna blandit suscipit efficitur" +
                        " eget mauris. Nullam eget aliquet ligula. Nunc id euismod elit. Morbi aliquam" +
                        " enim eros, eget consequat dolor consequat id. Quisque elementum faucibus" +
                        " congue. Curabitur mollis aliquet turpis, ut pellentesque justo eleifend nec.\n" +
                        "\n" +
                        "Suspendisse ac consequat turpis, euismod lacinia quam. Nulla lacinia tellus" +
                        " eu felis tristique ultricies. Vivamus et ultricies dolor. Orci varius" +
                        " natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus." +
                        " Ut gravida porttitor arcu elementum elementum. Phasellus ultrices vel turpis" +
                        " volutpat mollis. Vivamus leo diam, placerat quis leo efficitur, ultrices" +
                        " placerat ex. Nullam mollis et metus ac ultricies. Ut ligula metus, congue" +
                        " gravida metus in, vestibulum posuere velit. Sed et ex nisl. Fusce tempor" +
                        " odio eget sapien pellentesque, sed cursus velit fringilla. Nullam odio" +
                        " ipsum, eleifend non consectetur vitae, congue id libero. Etiam tincidunt" +
                        " mauris at urna dictum ornare.\n"
            )
        }
    }
}

@SuppressLint("UnnecessaryLambdaCreation")
@Composable
fun CatContent(cat: Cat, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }) {
        Image(
            painter = painterResource(id = cat.image),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
            contentDescription = null,
        )
        Text(text = cat.name, modifier = Modifier.wrapContentWidth().padding(8.dp))
    }
}

data class Cat(val name: String, val description: String, @DrawableRes val image: Int) {
    override fun toString(): String {
        return name
    }
}
