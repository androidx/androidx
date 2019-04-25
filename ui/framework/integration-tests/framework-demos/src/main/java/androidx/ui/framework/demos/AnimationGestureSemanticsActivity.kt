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

package androidx.ui.framework.demos

import android.app.Activity
import android.os.Bundle
import androidx.animation.ColorPropKey
import androidx.animation.FloatPropKey
import androidx.animation.transitionDefinition
import androidx.ui.animation.Transition
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Layout
import androidx.ui.core.Draw
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.min
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.setContent
import com.google.r4a.state
import com.google.r4a.unaryPlus

/* Demo app created to study the interaction of animations, gestures and semantics. */
class AnimationGestureSemanticsActivity : Activity() {

    private enum class ComponentState { Pressed, Released }

    private val colorKey = ColorPropKey()
    private val sizeKey = FloatPropKey()
    private val transitionDefinition = transitionDefinition {
        state(ComponentState.Pressed) {
            this[colorKey] = Color.fromARGB(255, 200, 0, 0)
            this[sizeKey] = 0.2f
        }
        state(ComponentState.Released) {
            this[colorKey] = Color.fromARGB(255, 0, 200, 0)
            this[sizeKey] = 1.0f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            <CraneWrapper>
                <GestureAnimatedComponent />
            </CraneWrapper>
        }
    }

    @Composable
    fun GestureAnimatedComponent() {
        val animationEndState = +state { ComponentState.Released }
        <PressGestureDetector
            onPress={ animationEndState.value = ComponentState.Pressed }
            onRelease={ animationEndState.value = ComponentState.Released }>
            <Layout children=@Composable {
                <Transition
                    definition=transitionDefinition
                    toState=animationEndState.value> state ->
                    <Circle color=state[colorKey] sizeRatio=state[sizeKey] />
                </Transition>
            }> _, constraints -> layout(constraints.maxWidth, constraints.maxHeight) {}
            </Layout>
        </PressGestureDetector>
    }

    @Composable
    fun Circle(color: Color, sizeRatio: Float) {
        <Draw> canvas, parentSize ->
            canvas.drawCircle(
                c = Offset(parentSize.width.value / 2, parentSize.height.value / 2),
                radius = min(parentSize.height, parentSize.width).value * sizeRatio / 2,
                paint = Paint().apply { this.color = color })
        </Draw>
    }
}