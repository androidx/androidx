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

package androidx.tv.material.carousel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.tv.material.ExperimentalTvMaterialApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * This composable is intended for use in Carousel.
 * A composable that has
 * - a [background] layer that is rendered as soon as the composable is visible.
 * - an [overlay] layer that is rendered after a delay of
 *   [overlayEnterTransitionStartDelayMillis].
 *
 * @param overlayEnterTransitionStartDelayMillis time between the rendering of the
 * background and the overlay.
 * @param overlayEnterTransition animation used to bring the overlay into view.
 * @param overlayExitTransition animation used to remove the overlay from view.
 * @param background composable defining the background of the slide.
 * @param overlay composable defining the content overlaid on the background.
 */
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@ExperimentalTvMaterialApi
@Composable
fun CarouselItem(
    background: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlayEnterTransitionStartDelayMillis: Long =
        CarouselItemDefaults.OverlayEnterTransitionStartDelayMillis,
    overlayEnterTransition: EnterTransition = CarouselItemDefaults.OverlayEnterTransition,
    overlayExitTransition: ExitTransition = CarouselItemDefaults.OverlayExitTransition,
    overlay: @Composable () -> Unit
) {
    val overlayVisible = remember { MutableTransitionState(initialState = false) }
    var focusState: FocusState? by remember { mutableStateOf(null) }
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(overlayVisible) {
        snapshotFlow { overlayVisible.isIdle && overlayVisible.currentState }.first { it }
        // slide has loaded completely.
        if (focusState?.isFocused == true) {
            // Using bringIntoViewRequester here instead of in Carousel.kt as when the focusable
            // item is within an animation, bringIntoView scrolls excessively and loses focus.
            // b/241591211
            // By using bringIntoView inside the snapshotFlow, we ensure that the focusable has
            // completed animating into position.
            bringIntoViewRequester.bringIntoView()
            focusManager.moveFocus(FocusDirection.Enter)
        }
    }

    Box(modifier = modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged {
            focusState = it
            if (it.isFocused && overlayVisible.isIdle && overlayVisible.currentState) {
                coroutineScope.launch {
                    bringIntoViewRequester.bringIntoView()
                    focusManager.moveFocus(FocusDirection.Enter)
                }
            }
        }
        .focusable()) {
        background()

        LaunchedEffect(overlayVisible) {
            // After the delay, set overlay-visibility to true and trigger the animation to show the
            // overlay.
            delay(overlayEnterTransitionStartDelayMillis)
            overlayVisible.targetState = true
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .onFocusChanged {
                    if (it.isFocused) { focusManager.moveFocus(FocusDirection.Enter) }
                }
                .focusable(),
            visibleState = overlayVisible,
            enter = overlayEnterTransition,
            exit = overlayExitTransition
        ) {
            overlay.invoke()
        }
    }
}

@ExperimentalTvMaterialApi
object CarouselItemDefaults {
    /**
     * Default delay between the background being rendered and the overlay being rendered.
     */
    val OverlayEnterTransitionStartDelayMillis: Long = 1500

    /**
     * Default transition to bring the overlay into view.
     */
    val OverlayEnterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { it * 4 })

    /**
     * Default transition to remove overlay from view.
     */
    val OverlayExitTransition: ExitTransition = slideOutHorizontally()
}
