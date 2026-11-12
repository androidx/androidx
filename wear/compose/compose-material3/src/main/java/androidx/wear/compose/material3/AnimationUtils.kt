/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
internal fun AnimatedIndicator(
    isVisible: () -> Boolean,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float>? = INDICATOR_FADE_OUT_ANIMATION,
    content: @Composable (BoxScope.() -> Unit)? = null,
) {
    // Skip if no indicator provided
    content?.let { pageIndicator ->
        if (animationSpec == null) {
            // if no animationSpec is provided then indicator will always be visible
            Box(modifier = modifier, content = pageIndicator)
        } else {
            // if animationSpec is provided this will be used to fade out indicator
            val alphaValue = remember { mutableFloatStateOf(0f) }
            LaunchedEffect(isVisible) {
                launch {
                    snapshotFlow { if (isVisible()) 1f else 0f }
                        .distinctUntilChanged()
                        .collectLatest { targetValue ->
                            animate(
                                alphaValue.floatValue,
                                targetValue,
                                animationSpec = animationSpec,
                            ) { value, _ ->
                                alphaValue.floatValue = value
                            }
                        }
                }
            }
            Box(
                modifier = modifier.graphicsLayer { alpha = alphaValue.floatValue },
                content = pageIndicator,
            )
        }
    }
}

internal object AnimationCoordinator {
    fun register() {
        if (registeredCount.incrementAndGet() > 0) running = true
    }

    fun unregister() {
        if (registeredCount.decrementAndGet() <= 0) running = false
    }

    /**
     * The frame time in milliseconds in the calling context of frame dispatch. Used to coordinate
     * animations. If animations are not running this will be Long.MAX_VALUE. Provided by
     * [withInfiniteAnimationFrameMillis].
     */
    val frameMillis = mutableLongStateOf(Long.MAX_VALUE)

    @Composable
    fun Looper() {
        LaunchedEffect(running) {
            if (running) {
                // DO NOT check running in the while, since this may see changes that the
                // LaunchedEffect misses. When running becomes false, this function will recompose
                // and the LaunchedEffect will cancel the running coroutine anyway.
                while (isActive) {
                    withInfiniteAnimationFrameMillis { frameMillis.longValue = it }
                }
            } else {
                // This should make all animations finish :)
                frameMillis.longValue = Long.MAX_VALUE
            }
        }
    }

    private val registeredCount = AtomicInteger(0)
    private var running by mutableStateOf(false)
}

internal val INDICATOR_FADE_OUT_ANIMATION: AnimationSpec<Float> =
    spring(stiffness = Spring.StiffnessMediumLow)
