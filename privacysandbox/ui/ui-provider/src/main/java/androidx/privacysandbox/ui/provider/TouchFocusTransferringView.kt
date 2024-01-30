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

package androidx.privacysandbox.ui.provider

import android.content.Context
import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceControlViewHost
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

/**
 * A container [ViewGroup] that delegates touch events to the host or the UI provider.
 *
 * Touch events will first be passed to a scroll detector. If a vertical scroll or fling
 * is detected, the gesture will be transferred to the host. Otherwise, the touch event will pass
 * through and be handled by the provider of UI.
 *
 * TODO(b/286829818): Pass scroll events to the UI provider if it can handle scrolls.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class TouchFocusTransferringView(
    context: Context,
    surfaceControlViewHost: SurfaceControlViewHost
) : FrameLayout(context) {

    private val scvh: SurfaceControlViewHost = surfaceControlViewHost
    private val detector = ScrollDetector(context)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        detector.onTouchEvent(ev)
        if (!detector.isScrolling) {
            return false
        }
        detector.reset()
        return scvh.transferTouchGestureToHost()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // This will only be called if the dispatch of the touch event is intercepted.
        return true
    }

    /**
     * Handles intercepted touch events before they reach the UI provider.
     *
     * If a vertical scroll or fling event is caught, this is indicated by the [isScrolling] var.
     */
    private class ScrollDetector(context: Context) : GestureDetector.SimpleOnGestureListener() {

        var isScrolling = false
          private set

        private val gestureDetector: GestureDetectorCompat = GestureDetectorCompat(context, this)

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dX: Float, dY: Float): Boolean {
            // A scroll is vertical if its y displacement is greater than its x displacement.
            if (abs(dY) > abs(dX)) {
                isScrolling = true
                return false
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // A fling is vertical if its y velocity is greater than its x velocity.
            if (abs(velocityY) > abs(velocityX)) {
                isScrolling = true
                return false
            }
            return true
        }

        fun onTouchEvent(ev: MotionEvent) {
            gestureDetector.onTouchEvent(ev)
        }

        fun reset() {
            isScrolling = false
        }
    }
}
