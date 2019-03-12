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
import android.os.Handler
import android.os.Looper
import androidx.animation.ColorPropKey
import androidx.animation.FloatPropKey
import androidx.animation.transitionDefinition
import androidx.ui.core.CraneWrapper
import androidx.ui.core.MeasureBox
import androidx.ui.core.adapter.Draw
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Composable
import com.google.r4a.Recompose
import com.google.r4a.composer
import com.google.r4a.setContent

class HelloAnimationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { <HelloAnimation /> }
    }
}

@Composable
fun HelloAnimation() {
    <CraneWrapper>
        <MeasureBox> constraints ->
            collect {
                <ColorRect />
            }
            layout(constraints.maxWidth, constraints.maxHeight) {}
        </MeasureBox>
    </CraneWrapper>
}

private val background = ColorPropKey()
private val y = FloatPropKey()

val trans = transitionDefinition {
    state(OverlayState.Open) {
        this[background] = Color.fromARGB(255, 128, 128, 128)
        this[y] = 1f // percentage
    }
    state(OverlayState.Closed) {
        this[background] = Color.fromARGB(255, 188, 222, 145)
        this[y] = 0f // percentage
    }
}

val handler = Handler(Looper.getMainLooper())

@Composable
fun ColorRect() {
    var toState = OverlayState.Closed
    <Recompose> recompose ->
        handler.postDelayed(object : Runnable {
            override fun run() {
                if ((0..1).random() == 0) {
                    toState = OverlayState.Open
                } else {
                    toState = OverlayState.Closed
                }
                recompose()
            }
        }, (200..800).random().toLong())
        <Transition transitionDef=trans toState> state ->
            <DrawColorRectState state />
        </Transition>
    </Recompose>
}

@Composable
fun DrawColorRectState(state: TransitionModel<OverlayState>) {

    val color = state[background]
    val scaleY = state[y]

    <DrawRectangle color />

    val paint = Paint().apply { this.color = Color.fromARGB(255, 255, 255, 255) }
    <Draw> canvas, pixelSize ->
        canvas.drawRect(
            Rect(
                100f, 0f, pixelSize.width.value - 100f,
                scaleY * pixelSize.height.value
            ), paint
        )
    </Draw>
}

@Composable
fun DrawRectangle(color: Color) {
    val paint = Paint()
    paint.color = color
    <Draw> canvas, parentSize ->
        canvas.drawRect(parentSize.toRect(), paint)
    </Draw>
}
