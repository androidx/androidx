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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation.demos.sharedelement

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.animatedSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.demos.R
import androidx.compose.animation.demos.lookahead.SearchBar
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun ContainerTransformDemo(model: MyModel = remember { MyModel().apply { selected = items[1] } }) {
    BackHandler { model.selected = null }
    SharedTransitionLayout {
        AnimatedContent(
            model.selected,
            transitionSpec = {
                fadeIn(tween(600)) togetherWith
                    fadeOut(tween(600)) using
                    SizeTransform { _, _ -> spring() }
            },
            label = ""
        ) {
            // TODO: Double check on container transform scrolling
            if (it != null) {
                DetailView(
                    this@AnimatedContent,
                    this@SharedTransitionLayout,
                    model = model,
                    selected = it,
                    model.items[6]
                )
            } else {
                GridView(this@AnimatedContent, this@SharedTransitionLayout, model = model)
            }
        }
    }
}

@Composable
fun Details(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    kitty: Kitty
) {
    with(sharedTransitionScope) {
        Column(
            Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)
                .fillMaxHeight()
                .wrapContentHeight(Alignment.Top)
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 10.dp, end = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Spacer(Modifier.size(20.dp))
                    Text(
                        kitty.name,
                        fontSize = 25.sp,
                        modifier =
                            Modifier.padding(start = 10.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = kitty.name + kitty.id),
                                    animatedVisibilityScope
                                )
                    )
                    Text(
                        kitty.breed,
                        fontSize = 22.sp,
                        color = Color.Gray,
                        modifier =
                            Modifier.padding(start = 10.dp)
                                .sharedBounds(
                                    rememberSharedContentState(key = kitty.breed + kitty.id),
                                    animatedVisibilityScope
                                )
                    )
                    Spacer(Modifier.size(10.dp))
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Outlined.Favorite,
                    contentDescription = null,
                    Modifier.background(Color(0xffffddee), CircleShape).padding(10.dp)
                )
                Spacer(Modifier.size(10.dp))
            }
            Box(
                modifier =
                    Modifier.padding(bottom = 10.dp)
                        .height(2.dp)
                        .fillMaxWidth()
                        .background(Color(0xffeeeeee))
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
                        " mauris at urna dictum ornare.\n" +
                        "\n" +
                        "Etiam at facilisis ex. Sed quis arcu diam. Quisque semper pharetra leo eget" +
                        " fermentum. Nulla dapibus eget mi id porta. Nunc quis sodales nulla, eget" +
                        " commodo sem. Donec lacus enim, pharetra non risus nec, eleifend ultrices" +
                        " augue. Donec sit amet orci porttitor, auctor mauris et, facilisis dolor." +
                        " Nullam mattis luctus orci at pulvinar.\n" +
                        "\n" +
                        "Sed accumsan est massa, ut aliquam nulla dignissim id. Suspendisse in urna" +
                        " condimentum, convallis purus at, molestie nisi. In hac habitasse platea" +
                        " dictumst. Pellentesque id justo quam. Cras iaculis tellus libero, eu" +
                        " feugiat ex pharetra eget. Nunc ultrices, magna ut gravida egestas, mauris" +
                        " justo blandit sapien, eget congue nisi felis congue diam. Mauris at felis" +
                        " vitae erat porta auctor. Pellentesque iaculis sem metus. Phasellus quam" +
                        " neque, congue at est eget, sodales interdum justo. Aenean a pharetra dui." +
                        " Morbi odio nibh, hendrerit vulputate odio eget, sollicitudin egestas ex." +
                        " Fusce nisl ex, fermentum a ultrices id, rhoncus vitae urna. Aliquam quis" +
                        " lobortis turpis.\n" +
                        "\n",
                color = Color.Gray,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun DetailView(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    model: MyModel,
    selected: Kitty,
    next: Kitty?
) {
    with(sharedTransitionScope) {
        Column(
            Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    model.selected = null
                }
                .sharedBounds(
                    rememberSharedContentState(key = "container + ${selected.id}"),
                    animatedVisibilityScope,
                    fadeIn(),
                    fadeOut(),
                    resizeMode = ScaleToBounds(ContentScale.Crop),
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp)),
                )
        ) {
            Row(Modifier.fillMaxHeight(0.5f)) {
                Image(
                    painter = painterResource(selected.photoResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.padding(10.dp)
                            .sharedElement(
                                rememberSharedContentState(key = selected.id),
                                animatedVisibilityScope,
                                placeHolderSize = animatedSize
                            )
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                )
                if (next != null) {
                    Image(
                        painter = painterResource(next.photoResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier.padding(top = 10.dp, bottom = 10.dp, end = 10.dp)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .blur(10.dp)
                    )
                }
            }
            Details(sharedTransitionScope, animatedVisibilityScope, kitty = selected)
        }
    }
}

@Composable
fun GridView(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    model: MyModel
) {
    with(animatedVisibilityScope) {
        with(sharedTransitionScope) {
            Box(Modifier.background(lessVibrantPurple)) {
                Box(
                    Modifier.padding(20.dp)
                        .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 2f)
                        .animateEnterExit(fadeIn(), fadeOut())
                ) {
                    SearchBar()
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(top = 90.dp)
                ) {
                    items(6) {
                        Box(modifier = Modifier.clickable { model.selected = model.items[it] }) {
                            KittyItem(
                                animatedVisibilityScope,
                                sharedTransitionScope,
                                model.items[it]
                            )
                        }
                    }
                }
            }
        }
    }
}

class MyModel {
    val items =
        mutableListOf(
            Kitty("Waffle", R.drawable.waffle, "American Short Hair", 0),
            Kitty("油条", R.drawable.yt_profile, "Tabby", 1),
            Kitty("Cowboy", R.drawable.cowboy, "American Short Hair", 2),
            Kitty("Pepper", R.drawable.pepper, "Tabby", 3),
            Kitty("Unknown", R.drawable.question_mark, "Unknown Breed", 4),
            Kitty("Unknown", R.drawable.question_mark, "Unknown Breed", 5),
            Kitty("YT", R.drawable.yt_profile2, "Tabby", 6),
        )
    var selected: Kitty? by mutableStateOf(null)
}

@Composable
fun KittyItem(
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    kitty: Kitty
) {
    with(sharedTransitionScope) {
        Column(
            Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                .sharedBounds(
                    rememberSharedContentState(key = "container + ${kitty.id}"),
                    animatedVisibilityScope,
                )
                .background(Color.White, RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(kitty.photoResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier.sharedElement(
                            rememberSharedContentState(key = kitty.id),
                            animatedVisibilityScope,
                            placeHolderSize = animatedSize
                        )
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.size(10.dp))
            Text(
                kitty.name,
                fontSize = 18.sp,
                modifier =
                    Modifier.padding(start = 10.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = kitty.name + kitty.id),
                            animatedVisibilityScope
                        )
            )
            Spacer(Modifier.size(5.dp))
            Text(
                kitty.breed,
                fontSize = 15.sp,
                color = Color.Gray,
                modifier =
                    Modifier.padding(start = 10.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = kitty.breed + kitty.id),
                            animatedVisibilityScope
                        )
            )
            Spacer(Modifier.size(10.dp))
        }
    }
}

data class Kitty(val name: String, val photoResId: Int, val breed: String, val id: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Kitty && other.id == id
    }
}

private val lessVibrantPurple = Color(0xfff3edf7)
