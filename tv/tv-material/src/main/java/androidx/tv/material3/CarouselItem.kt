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

package androidx.tv.material3

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.tv.material3.KeyEventPropagation.ContinuePropagation

/**
 * This composable is intended for use in Carousel.
 * A composable that has
 * - a [background] layer that is rendered as soon as the composable is visible.
 * - a [content] layer that is rendered on top of the [background]
 *
 * @param background composable defining the background of the item
 * @param itemIndex current active item index of the carousel
 * @param modifier modifier applied to the CarouselItem
 * @param contentTransform content transform to be applied to the content of the item when
 * scrolling
 * @param content composable defining the content displayed on top of the background
 */
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@ExperimentalTvMaterial3Api
@Composable
internal fun CarouselItem(
    itemIndex: Int,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit = {},
    contentTransform: ContentTransform =
        CarouselItemDefaults.contentTransformStartToEnd,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    var containerBoxFocusState: FocusState? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current
    var exitFocus by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }

    DisposableEffect(itemIndex) {
        isVisible = true
        onDispose { isVisible = false }
    }

    // This box holds the focus until the overlay animation completes
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                isContainer = true
                collectionItemInfo =
                    CollectionItemInfo(
                        rowIndex = 0,
                        rowSpan = 1,
                        columnIndex = itemIndex,
                        columnSpan = 1
                    )
            }
            .onKeyEvent {
                exitFocus = it.isBackPress() && it.isTypeKeyDown()
                ContinuePropagation
            }
            .onFocusChanged {
                containerBoxFocusState = it
                if (it.isFocused && exitFocus) {
                    focusManager.moveFocus(FocusDirection.Exit)
                    exitFocus = false
                }
            }
            .then(
                if (accessibilityManager.isEnabled)
                    Modifier.clickable {
                        focusManager.moveFocus(FocusDirection.Enter)
                    }
                else
                    Modifier.focusable()
            )
    ) {
        background()

        AnimatedVisibility(
            visible = isVisible,
            enter = contentTransform.targetContentEnter,
            exit = contentTransform.initialContentExit,
        ) {
            LaunchedEffect(transition.isRunning, containerBoxFocusState?.isFocused) {
                if (!transition.isRunning &&
                    containerBoxFocusState?.isFocused == true &&
                    !accessibilityManager.isEnabled
                ) {
                    focusManager.moveFocus(FocusDirection.Enter)
                }
            }
            content.invoke()
        }
    }
}

@ExperimentalTvMaterial3Api
object CarouselItemDefaults {
    /**
     * Transform the content from right to left
     */
    // Keeping this as public so that users can access it directly without the isLTR helper
    val contentTransformRightToLeft: ContentTransform
        @Composable get() =
            slideInHorizontally { it * 4 }
                .togetherWith(slideOutHorizontally { it * 4 })

    /**
     * Transform the content from left to right
     */
    // Keeping this as public so that users can access it directly without the isLTR helper
    val contentTransformLeftToRight: ContentTransform
        @Composable get() =
            slideInHorizontally()
                .togetherWith(slideOutHorizontally())

    /**
     * Content transform applied when moving forward taking isLTR into account
     */
    val contentTransformStartToEnd
        @Composable get() =
            if (isLtr())
                contentTransformRightToLeft
            else
                contentTransformLeftToRight

    /**
     * Content transform applied when moving backward taking isLTR into account
     */
    val contentTransformEndToStart
        @Composable get() =
            if (isLtr())
                contentTransformLeftToRight
            else
                contentTransformRightToLeft
}

@Composable
private fun isLtr() = LocalLayoutDirection.current == LayoutDirection.Ltr
