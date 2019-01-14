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

package androidx.ui.material

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.ui.core.Position
import androidx.ui.core.dp
import androidx.ui.rendering.proxybox.HitTestBehavior
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component

// TODO("Migration|Andrey: Simple implementation for touch detector to not be blocked by Shep.")
// TODO("Migration|Andrey: Everything will be replaced with R4a's GestureDetector")

class InkTmpGestureHost(@Children var children: () -> kotlin.Unit) : Component() {

    private val router = MotionEventRouter()

    override fun compose() {
        <FrameLayout layoutParams=FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )>
            <TouchView router />
            <MotionEventRouterAmbient.Provider value=router>
                <children />
            </MotionEventRouterAmbient.Provider>
        </FrameLayout>
    }

}

internal class TouchView(context: Context) : View(context) {

    var router: MotionEventRouter? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        router?.handleTouch(event, resources.displayMetrics.density)
        return true
    }
}

internal val MotionEventRouterAmbient = Ambient.of<MotionEventRouter>()

internal class MotionEventRouter {

    var listener: ((MotionEvent, Float) -> Unit)? = null

    fun handleTouch(event: MotionEvent, density: Float) {
        listener?.invoke(event, density)
    }

}

class InkTmpGestureDetector(
    var onTapDown: ((Position) -> Unit)? = null,
    var onTap: (() -> Unit)? = null,
    var onTapCancel: (() -> Unit)? = null,
    var onDoubleTap: (() -> Unit)? = null,
    var onLongPress: (() -> Unit)? = null,
    var behavior: HitTestBehavior? = null,
    val excludeFromSemantics: Boolean = false,
    @Children var children: () -> Unit
) : Component() {

    override fun compose() {
        <MotionEventRouterAmbient.Consumer> router ->
            router.listener = { event, density ->
                val offset = Position(
                    (event.x / density).dp,
                    (event.y / density).dp
                )
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> onTapDown?.invoke(offset)
                    MotionEvent.ACTION_UP -> onTap?.invoke()
                    MotionEvent.ACTION_CANCEL -> onTapCancel?.invoke()
                }
            }
            <children />
        </MotionEventRouterAmbient.Consumer>
    }

}