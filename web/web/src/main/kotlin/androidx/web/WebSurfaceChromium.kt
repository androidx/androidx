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
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.web.WebGlueCommunicator.asProxy
import java.util.function.BiConsumer
import org.chromium.support_lib_boundary.web.WebSurfaceBoundaryInterface
import org.chromium.support_lib_boundary.web.WebSurfaceEvent

/** Internal adapter wrapping the Chromium [WebSurfaceBoundaryInterface]. */
@InternalWebApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WebSurfaceChromium
private constructor(private val boundary: WebSurfaceBoundaryInterface) {
    public fun interface InvalidateListener {
        public fun onInvalidate()
    }

    public fun setWebContent(@Nullable content: WebContent?) {
        val handler = content?.getInvocationHandler()
        boundary.setWebContent(handler)
    }

    public fun draw(@NonNull canvas: Canvas) {
        boundary.draw(canvas)
    }

    public fun setSize(width: Int, height: Int) {
        boundary.setSize(width, height)
    }

    public fun onTouchEvent(@NonNull event: MotionEvent): Boolean {
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
        @NonNull
        public fun create(@NonNull listener: InvalidateListener): WebSurfaceChromium {
            WebFeature.checkSupported(WebFeature.WEB_SURFACE)
            val eventListener =
                BiConsumer<@WebSurfaceEvent Int, Any> { event, _ ->
                    when (event) {
                        WebSurfaceEvent.INVALIDATE -> listener.onInvalidate()
                    }
                }
            val boundary =
                WebGlueCommunicator.factory
                    .createWebSurface(eventListener)
                    .asProxy<WebSurfaceBoundaryInterface>()
            return WebSurfaceChromium(boundary)
        }
    }
}
