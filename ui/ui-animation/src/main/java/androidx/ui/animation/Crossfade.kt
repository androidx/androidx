/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.animation

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationBuilder
import androidx.animation.AnimationEndReason
import androidx.animation.TweenBuilder
import androidx.compose.Composable
import androidx.compose.invalidate
import androidx.compose.key
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.drawOpacity
import androidx.ui.layout.Stack
import androidx.ui.util.fastForEach

/**
 * [Crossfade] allows to switch between two layouts with a crossfade animation.
 *
 * @sample androidx.ui.animation.samples.CrossfadeSample
 *
 * @param current is a key representing your current layout state. every time you change a key
 * the animation will be triggered. The [children] called with the old key will be faded out while
 * the [children] called with the new key will be faded in.
 * @param animation the [AnimationBuilder] to configure the animation.
 */
@Composable
fun <T> Crossfade(
    current: T,
    animation: AnimationBuilder<Float> = TweenBuilder(),
    children: @Composable (T) -> Unit
) {
    val state = remember { CrossfadeState<T>() }
    if (current != state.current) {
        state.current = current
        val keys = state.items.map { it.key }.toMutableList()
        if (!keys.contains(current)) {
            keys.add(current)
        }
        state.items.clear()
        keys.mapTo(state.items) { key ->
            CrossfadeAnimationItem(key) { children ->
                val opacity = animatedOpacity(
                    animation = animation,
                    visible = key == current,
                    onAnimationFinish = {
                        if (key == state.current) {
                            // leave only the current in the list
                            state.items.removeAll { it.key != state.current }
                            state.invalidate()
                        }
                    }
                )
                Stack(Modifier.drawOpacity(opacity.value)) {
                    children()
                }
            }
        }
    }
    Stack {
        state.invalidate = invalidate
        state.items.fastForEach { (item, opacity) ->
            key(item) {
                opacity {
                    children(item)
                }
            }
        }
    }
}

private class CrossfadeState<T> {
    var current: T? = null
    var items = mutableListOf<CrossfadeAnimationItem<T>>()
    var invalidate: () -> Unit = { }
}

private data class CrossfadeAnimationItem<T>(
    val key: T,
    val transition: CrossfadeTransition
)

private typealias CrossfadeTransition = @Composable (children: @Composable () -> Unit) -> Unit

@Composable
private fun animatedOpacity(
    animation: AnimationBuilder<Float>,
    visible: Boolean,
    onAnimationFinish: () -> Unit = {}
): AnimatedFloat {
    val animatedFloat = animatedFloat(if (!visible) 1f else 0f)
    onCommit(visible) {
        animatedFloat.animateTo(
            if (visible) 1f else 0f,
            anim = animation,
            onEnd = { reason, _ ->
                if (reason == AnimationEndReason.TargetReached) {
                    onAnimationFinish()
                }
            })
    }
    return animatedFloat
}
