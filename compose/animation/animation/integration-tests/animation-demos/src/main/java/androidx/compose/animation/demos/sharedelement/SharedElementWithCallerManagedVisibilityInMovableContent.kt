/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun SharedElementWithCallerManagedVisibilityInMovableContent() {
    MultiDisplay.Show()
}

@OptIn(ExperimentalSharedTransitionApi::class)
object MultiDisplay {
    @Composable
    fun Show() {
        SharedTransitionLayout {
            var navigation by remember { mutableStateOf<Navigation>(Navigation.Start) }
            val saveableStateHolder = rememberSaveableStateHolder()

            val movableStart = remember {
                movableContentOf {
                    modifier: Modifier,
                    sharedTransitionScope: SharedTransitionScope,
                    visible: Boolean ->
                    Start(
                        modifier = modifier.animateBounds(sharedTransitionScope),
                        sharedTransitionScope = sharedTransitionScope,
                        visible = visible,
                        onClick = { selectedNavigation -> navigation = selectedNavigation },
                    )
                }
            }

            Scaffold { paddingValues ->
                AnimatedContent(
                    modifier = Modifier.padding(paddingValues),
                    targetState = navigation,
                ) { targetState ->
                    saveableStateHolder.SaveableStateProvider(targetState.id) {
                        when (targetState) {
                            Navigation.Start ->
                                if (transition.targetState == EnterExitState.Visible)
                                    movableStart(Modifier, this@SharedTransitionLayout, true)

                            is Navigation.End ->
                                Row {
                                    if (transition.targetState == EnterExitState.Visible)
                                        movableStart(
                                            Modifier.weight(1f),
                                            this@SharedTransitionLayout,
                                            false,
                                        )

                                    End(
                                        modifier = Modifier.weight(1f),
                                        navigation = targetState,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@AnimatedContent,
                                        onClick = { navigation = Navigation.Start },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }

    private sealed class Navigation {
        data object Start : Navigation()

        data class End(val index: Int, val color: Color) : Navigation()

        val id
            get() =
                when (this) {
                    is End -> index
                    Start -> -1
                }
    }

    @Composable
    private fun Start(
        modifier: Modifier = Modifier,
        visible: Boolean,
        sharedTransitionScope: SharedTransitionScope,
        onClick: (Navigation.End) -> Unit,
    ) =
        with(sharedTransitionScope) {
            val visibleState = rememberUpdatedState(visible)
            val state = rememberLazyListState()
            LazyColumn(modifier = modifier.fillMaxSize(), state = state) {
                items(
                    items = items,
                    key = { it },
                    itemContent = { index ->
                        val color = colors[index % colors.size]
                        Row(
                            modifier =
                                Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    )
                                    .clickable { onClick(Navigation.End(index, color)) },
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.sharedElementWithCallerManagedVisibility(
                                            sharedContentState =
                                                sharedTransitionScope.rememberSharedContentState(
                                                    index
                                                ),
                                            visible = visible,
                                        )
                                        .background(color = color, shape = CircleShape)
                                        .size(56.dp)
                            )
                            Text(text = index.toString())
                        }
                    },
                )
            }
        }

    @Composable
    private fun End(
        modifier: Modifier = Modifier,
        navigation: Navigation.End,
        sharedTransitionScope: SharedTransitionScope,
        animatedVisibilityScope: AnimatedVisibilityScope,
        onClick: () -> Unit,
    ) =
        with(sharedTransitionScope) {
            Box(modifier = modifier.fillMaxSize().clickable(onClick = onClick)) {
                Box(
                    modifier =
                        Modifier.align(Alignment.Center)
                            .sharedElementWithCallerManagedVisibility(
                                sharedContentState =
                                    sharedTransitionScope.rememberSharedContentState(
                                        navigation.index
                                    ),
                                visible =
                                    animatedVisibilityScope.transition.targetState ==
                                        EnterExitState.Visible,
                            )
                            .background(color = navigation.color, shape = CircleShape)
                            .size(140.dp)
                )
            }
        }

    @SuppressLint("PrimitiveInCollection") private val items = (0..100).toList()

    @SuppressLint("PrimitiveInCollection")
    private val colors =
        listOf(
            Color.Blue,
            Color.Yellow,
            Color.Red,
            Color.Gray,
            Color.Cyan,
            Color.Magenta,
            Color.Green,
        )
}
