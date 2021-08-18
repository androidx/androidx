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
package androidx.wear.compose.material

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Possible combinations for vignette state.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class VignetteValue constructor(private val key: Int) {
    internal fun drawTop(): Boolean {
        return when (key) {
            1 -> false
            else -> {
                true
            }
        }
    }

    internal fun drawBottom(): Boolean {
        return when (key) {
            0 -> false
            else -> {
                true
            }
        }
    }

    companion object {
        /**
         * Only the top part of the vignette is displayed.
         */
        val Top = VignetteValue(0)

        /**
         * Only the bottom part of the vignette is displayed.
         */
        val Bottom = VignetteValue(1)

        /**
         * Both the top and bottom of the vignette is displayed.
         */
        val Both = VignetteValue(2)
    }

    override fun toString(): String {
        return when (this) {
            Top -> "VignetteValue.Top"
            Bottom -> "VignetteValue.Bottom"
            else -> "VignetteValue.Both"
        }
    }
}

/**
 * Vignette is whole screen decoration used to blur the top and bottom of the edges of a wearable
 * screen when scrolling content is displayed. The vignette is split between a top and bottom image
 * which can be displayed independently depending on the use case.
 *
 * The vignette is designed to be used as an overlay, typically in the [Scaffold].
 *
 * Simple example of a Vignette with a [ScalingLazyColumn] as the main application content where
 * the top/bottom vignette images can be turned on/off can be found at
 *
 * @sample androidx.wear.compose.material.samples.SimpleScaffoldWithScrollIndicator
 *
 * @param vignetteValue whether to draw top and/or bottom images for this [Vignette]
 * @param modifier optional Modifier for the root of the [Vignette]
 */
@Composable
public fun Vignette(
    vignetteValue: VignetteValue,
    modifier: Modifier = Modifier,
) {

    Box(modifier = modifier.fillMaxSize()) {
        if (vignetteValue.drawTop()) {
            Image(
                painter = imageResource(
                    if (isRoundDevice()) "circular_vignette_top"
                    else "rectangular_vignette_top"
                ),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        if (vignetteValue.drawBottom()) {
            Image(
                painter = imageResource(
                    if (isRoundDevice()) "circular_vignette_bottom"
                    else "rectangular_vignette_bottom"
                ),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
