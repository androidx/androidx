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

package androidx.tv.material.pager

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material.ExperimentalTvMaterialApi

/**
 * Composable that accepts a lambda that generates the slides based on the index provided and
 * displays the slide associated with the index [currentSlide].
 *
 * @param modifier the modifier to apply to this component.
 * @param enterTransition defines how the slide is animated into view.
 * @param exitTransition defines how the slide is animated out of view.
 * @param currentSlide the slide that is currently displayed by the pager.
 * @param slideCount the total number of slides.
 * @param content defines the slide composable for a given index.
 */
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalAnimationApi::class)
@ExperimentalTvMaterialApi
@Composable
internal fun Pager(
    slideCount: Int,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = PagerDefaults.EnterTransition,
    exitTransition: ExitTransition = PagerDefaults.ExitTransition,
    currentSlide: Int = 0,
    content: @Composable (index: Int) -> Unit
) {
    if (slideCount <= 0) {
        Box(modifier)
    } else {
        AnimatedContent(
            modifier = modifier,
            targetState = currentSlide.coerceIn(0, slideCount - 1),
            transitionSpec = { enterTransition.with(exitTransition) }
        ) {
            content(it)
        }
    }
}

@ExperimentalTvMaterialApi
private object PagerDefaults {
    /**
     * Default transition used to bring a slide into view
     */
    val EnterTransition: EnterTransition = fadeIn(animationSpec = tween(900))

    /**
     * Default transition used to remove a slide from view
     */
    val ExitTransition: ExitTransition = fadeOut(animationSpec = tween(900))
}
