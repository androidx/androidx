/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ui.client

import android.content.Context
import android.os.Handler
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.animation.AnimationUtils
import androidx.privacysandbox.ui.core.IMotionEventTransferCallback
import androidx.tracing.trace

/**
 * Custom implementation of the [android.view.SurfaceView] that holds the
 * [android.view.SurfaceControlViewHost.SurfacePackage] passed from the provider. It transfers the
 * [android.view.MotionEvent] objects received in [android.view.View.onTouchEvent] to the provider.
 * It also proxy calls to [android.view.ViewParent.requestDisallowInterceptTouchEvent] from provider
 * side to the parent [android.view.View].
 */
internal class ContentView(
    context: Context,
    val remoteSessionController: IRemoteSessionController,
) : SurfaceView(context) {
    private var currentGestureMotionEventTransferCallback: IMotionEventTransferCallback? = null
    private var requestDisallowInterceptHandler: Handler? = null

    override fun onAttachedToWindow() {
        requestDisallowInterceptHandler = Handler(handler.looper)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        // Remove all handler posted messages.
        requestDisallowInterceptHandler?.removeCallbacksAndMessages(/* token */ null)
        requestDisallowInterceptHandler = null
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        // Don't add anything above this trace to allow us to catch regression
        trace("ContentView#onTouchEvent", {})

        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            currentGestureMotionEventTransferCallback = MotionEventTransferCallbackProxy()
        }

        if (
            (motionEvent.action == MotionEvent.ACTION_UP ||
                motionEvent.action == MotionEvent.ACTION_CANCEL)
        ) {
            currentGestureMotionEventTransferCallback = null
        }

        remoteSessionController.notifyMotionEvent(
            motionEvent,
            getFrameTimeForBatchedInputOrCurrentTimeForUnBatchedInput(),
            currentGestureMotionEventTransferCallback,
        )
        return true
    }

    override fun onHoverEvent(hoverEvent: MotionEvent): Boolean {
        remoteSessionController.notifyHoverEvent(
            hoverEvent,
            getFrameTimeForBatchedInputOrCurrentTimeForUnBatchedInput(),
        )
        return true
    }

    private fun getFrameTimeForBatchedInputOrCurrentTimeForUnBatchedInput(): Long {
        return AnimationUtils.currentAnimationTimeMillis()
    }

    inner class MotionEventTransferCallbackProxy() : IMotionEventTransferCallback.Stub() {
        override fun requestDisallowIntercept(disallowIntercept: Boolean) {
            requestDisallowInterceptHandler?.post {
                if (this == currentGestureMotionEventTransferCallback) {
                    parent.requestDisallowInterceptTouchEvent(disallowIntercept)
                }
            }
        }
    }
}
