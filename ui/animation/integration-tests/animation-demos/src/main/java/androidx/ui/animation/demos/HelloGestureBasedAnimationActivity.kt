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

package androidx.ui.animation.demos

import android.app.Activity
import android.os.Bundle
import androidx.animation.ColorPropKey
import androidx.animation.FloatPropKey
import androidx.animation.transitionDefinition
import androidx.ui.animation.Transition
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Layout
import androidx.ui.core.PxPosition
import androidx.ui.core.Draw
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Composable
import com.google.r4a.Recompose
import com.google.r4a.composer
import com.google.r4a.setContent

class HelloGestureBasedAnimationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { <HelloGesture /> }
    }
}

@Suppress("FunctionName")
@Composable
fun HelloGesture() {
    <CraneWrapper>
        <TransitionExample />
    </CraneWrapper>
}

private val scale = FloatPropKey()
private val color = ColorPropKey()

private val definition = transitionDefinition {
    state("released") {
        this[scale] = 1f
        this[color] = Color.fromARGB(255, 0, 200, 0)
    }
    state("pressed") {
        this[scale] = 3f
        this[color] = Color.fromARGB(255, 0, 100, 0)
    }
}

@Suppress("FunctionName")
@Composable
fun TransitionExample() {
    var toState = "released"

    <Recompose> recompose ->
        val onPress: (PxPosition) -> Unit = {
            toState = "pressed"
            recompose()
        }
        val onRelease = {
            toState = "released"
            recompose()
        }
        <PressGestureDetector onPress onRelease onCancel=onRelease>
            val children = @Composable {
                <Transition definition toState> state ->
                    <DrawScaledRect scale=state[scale] color=state[color] />
                </Transition>
            }
            <Layout children> _, constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) {}
            </Layout>
        </PressGestureDetector>
    </Recompose>
}

val paint: Paint = Paint()
const val halfSize = 200f

@Suppress("FunctionName")
@Composable
fun DrawScaledRect(scale: Float, color: Color) {
    <Draw> canvas, parentSize ->
        val centerX = parentSize.width.value / 2
        val centerY = parentSize.height.value / 2
        paint.color = color
        canvas.drawRect(
            Rect(
                centerX - halfSize * scale, centerY - halfSize * scale,
                centerX + halfSize * scale, centerY + halfSize * scale
            ), paint
        )
    </Draw>
}
