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

package androidx.web

import android.graphics.Canvas
import android.view.MotionEvent
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.web.WebGlueCommunicator.asProxy
import java.util.function.BiConsumer
import org.chromium.support_lib_boundary.web.WebSurfaceBoundaryInterface
import org.chromium.support_lib_boundary.web.WebSurfaceEvent
import org.chromium.support_lib_boundary.web.util.WebSurfaceEventPacker

/** Internal adapter wrapping the Chromium [WebSurfaceBoundaryInterface]. */
@InternalWebApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WebSurfaceChromium
private constructor(private val boundary: WebSurfaceBoundaryInterface) {
    public interface EventListener {
        public fun onInvalidate()

        public fun onOverScroll(deltaX: Float, deltaY: Float, velocityX: Float, velocityY: Float)
    }

    internal class EventListenerAdapter(private val listener: EventListener) :
        BiConsumer<@WebSurfaceEvent Int, Any?> {
        override fun accept(event: @WebSurfaceEvent Int, payload: Any?) {
            when (event) {
                WebSurfaceEvent.INVALIDATE -> listener.onInvalidate()
                WebSurfaceEvent.OVER_SCROLL -> {
                    val (deltaX, deltaY, velocityX, velocityY) =
                        WebSurfaceEventPacker.unpackOverScroll(requireNotNull(payload))
                    listener.onOverScroll(deltaX, deltaY, velocityX, velocityY)
                }
            }
        }
    }

    public fun setWebContent(content: WebContent?) {
        val handler = content?.getInvocationHandler()
        boundary.setWebContent(handler)
    }

    public fun draw(canvas: Canvas) {
        boundary.draw(canvas)
    }

    public fun setSize(width: Int, height: Int) {
        boundary.setSize(width, height)
    }

    public fun onTouchEvent(event: MotionEvent): Boolean {
        return boundary.onTouchEvent(event)
    }

    public fun destroy() {
        boundary.destroy()
    }

    public companion object {
        @UiThread
        @RequiresFeature(
            name = WebFeature.WEB_SURFACE,
            enforcement = "androidx.web.WebFeature#isFeatureSupported",
        )
        public fun create(listener: EventListener): WebSurfaceChromium {
            WebFeature.checkSupported(WebFeature.WEB_SURFACE)
            val boundary =
                WebGlueCommunicator.factory
                    .createWebSurface(EventListenerAdapter(listener))
                    .asProxy<WebSurfaceBoundaryInterface>()
            return WebSurfaceChromium(boundary)
        }
    }
}
