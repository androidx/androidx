/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import kotlinx.cinterop.useContents
import kotlin.system.getTimeNanos
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.GraphicsApi

// TODO: refactor candidate on iOS, proxies everything and doesn't contain any logic
internal class IOSSkiaLayer {
    var needRedrawCallback: () -> Unit = {}

    var renderApi: GraphicsApi
        get() = GraphicsApi.METAL
        set(_) { throw UnsupportedOperationException() }

    val contentScale: Float
        get() = view!!.contentScaleFactor.toFloat()

    var fullscreen: Boolean
        get() = true
        set(_) { throw UnsupportedOperationException() }

    var transparency: Boolean
        get() = false
        set(_) { throw UnsupportedOperationException() }

    fun needRedraw() {
        needRedrawCallback.invoke()
    }

    val component: Any?
        get() = this.view

    val width: Float
        get() = view!!.frame.useContents {
            return@useContents size.width.toFloat()
        }

    val height: Float
        get() = view!!.frame.useContents {
            return@useContents size.height.toFloat()
        }

    internal var view: SkikoUIView? = null

    fun attachTo(container: Any) {
        view = container as SkikoUIView
    }

    fun detach() {
        view?.detach()

        // GC bug? fixes leak on iOS
        view = null
        skikoView = null
    }

    var skikoView: IOSSkikoView? = null

    internal fun draw(surface: Surface) {
        skikoView?.onRender(surface.canvas, surface.width, surface.height, getTimeNanos())
    }

    val pixelGeometry: PixelGeometry
        get() = PixelGeometry.UNKNOWN
}