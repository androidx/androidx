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
import androidx.animation.AnimationEndReason
import androidx.animation.TweenBuilder
import androidx.compose.Composable
import androidx.compose.key
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.drawOpacity
import androidx.ui.layout.Stack

/**
 * [Crossfade] allows to switch between two layouts with a crossfade animation.
 *
 * @sample androidx.ui.animation.samples.CrossfadeSample
 *
 * @param current is a key representing your current layout state. every time you change a key
 * the animation will be triggered. The [children] called with the old key will be faded out while
 * the [children] called with the new key will be faded in.
 */
@Composable
fun <T> Crossfade(current: T, children: @Composable() (T) -> Unit) {
    val state = remember { CrossfadeState<T>() }
    if (current != state.current) {
        state.current = current
        val keys = state.items.map { it.first }.toMutableList()
        if (!keys.contains(current)) {
            keys.add(current)
        }
        state.items.clear()
        keys.forEach { key ->
            state.items.add(key to @Composable() { children ->
                val opacity = animatedOpacity(
                    visible = (key == current),
                    onAnimationFinish = {
                        if (key == state.current) {
                            // leave only the current in the list
                            state.items.removeAll { it.first != state.current }
                        }
                    })
                Stack(modifier = drawOpacity(opacity.value)) {
                    children()
                }
            })
        }
    }
    Stack {
        state.items.forEach { (item, opacity) ->
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
    val items = mutableListOf<Pair<T, @Composable() (@Composable() () -> Unit) -> Unit>>()
}

@Composable
private fun animatedOpacity(
    visible: Boolean,
    onAnimationFinish: () -> Unit = {}
): AnimatedFloat {
    val animatedFloat = animatedFloat(if (!visible) 1f else 0f)
    onCommit(visible) {
        animatedFloat.animateTo(
            if (visible) 1f else 0f,
            anim = TweenBuilder<Float>().apply { duration = 300 },
            onEnd = { reason, _ ->
                if (reason == AnimationEndReason.TargetReached) {
                    onAnimationFinish()
                }
            })
    }
    return animatedFloat
}
