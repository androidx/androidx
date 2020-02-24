/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.animation.demos

import android.app.Activity
import android.os.Bundle
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.RectPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.setContent
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Clickable
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.LayoutSize

class MultiDimensionalAnimation : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MultiDimens() }
    }

    @Composable
    fun MultiDimens() {
        val currentState = state { AnimState.Collapsed }
        val onClick = {
            // Cycle through states when clicked.
            currentState.value = when (currentState.value) {
                AnimState.Collapsed -> AnimState.Expanded
                AnimState.Expanded -> AnimState.PutAway
                AnimState.PutAway -> AnimState.Collapsed
            }
        }
        Clickable(onClick) {
            val width = state { 0f }
            val height = state { 0f }
            Transition(
                definition = remember(width.value, height.value) {
                    createTransDef(width.value, height.value)
                },
                toState = currentState.value
            ) { state ->
                val paint = remember { Paint() }
                Canvas(modifier = LayoutSize.Fill) {
                    width.value = size.width.value
                    height.value = size.height.value

                    paint.color = state[background]
                    drawRect(state[bounds], paint)
                }
            }
        }
    }

    enum class AnimState {
        Collapsed,
        Expanded,
        PutAway
    }

    // Both PropKeys below are multi-dimensional property keys. That means each dimension's
    // value and velocity will be tracked independently. In the case of a color, each color
    // channel is a separate dimension. For rectangles, the dimensions are: top, left,
    // right and bottom.
    val background = ColorPropKey()
    val bounds = RectPropKey()

    fun createTransDef(width: Float, height: Float) =
        transitionDefinition {
            state(AnimState.Collapsed) {
                this[background] = Color.LightGray
                this[bounds] = Rect(600f, 600f, 900f, 900f)
            }
            state(AnimState.Expanded) {
                this[background] = Color(0xFFd0fff8)
                this[bounds] = Rect(0f, 400f, width, height - 400f)
            }
            state(AnimState.PutAway) {
                this[background] = Color(0xFFe3ffd9)
                this[bounds] = Rect(width - 300f, height - 300f, width, height)
            }

            transition {
                bounds using physics {
                    stiffness = 100f
                }
                background using tween {
                    duration = 500
                }
            }
        }
}