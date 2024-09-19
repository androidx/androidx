/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.graphics.Color
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.graphics.CanvasBufferedRenderer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.ink.authoring.InProgressStrokeId
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A version of [FrontBufferToHwuiHandoff] that relies on temporarily translating the [SurfaceView]
 * off screen while the front buffer is cleared, with a "companion" [HardwareBuffer] whose callbacks
 * are able to replace the timer used in [FrontBufferToHwuiHandoffV29] for a more reliable result
 * (fewer flickers in stress test scenarios) with tighter timing (shorter handoff duration in ideal
 * scenarios).
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class FrontBufferToHwuiHandoffV34(
    private val mainView: ViewGroup,
    private val surfaceView: SurfaceView,
    /**
     * Called to hand off a group of finished strokes from being rendered internally to being
     * rendered by a higher level in HWUI.
     *
     * @see InProgressStrokesRenderHelper.Callback.onStrokeCohortHandoffToHwui
     */
    @UiThread private val onCohortHandoff: (Map<InProgressStrokeId, FinishedStroke>) -> Unit,

    /**
     * Called after [onCohortHandoff] when it is safe for higher level code to start drawing again.
     *
     * @see InProgressStrokesRenderHelper.Callback.onStrokeCohortHandoffToHwuiComplete
     */
    @UiThread private val onCohortHandoffComplete: () -> Unit,
    private val uiThreadExecutor: Executor =
        object : Executor {
            private val handler = Handler(Looper.getMainLooper())

            override fun execute(command: Runnable) {
                if (!handler.post(command)) {
                    throw RejectedExecutionException("$handler is shutting down")
                }
            }
        },
) : FrontBufferToHwuiHandoff {

    private val isInitialized = AtomicBoolean()

    /**
     * This surface layer will be shown and hidden in sync with [surfaceView] being translated on
     * and off screen. [SurfaceView] doesn't directly give us callbacks to indicate whether it is
     * fully off screen, but a [SurfaceControlCompat] is able to via a buffer release callback after
     * that buffer's contents are copied out of it for display.
     */
    private val companionSurfaceControl =
        SurfaceControlCompat.Builder().setName(this.javaClass.simpleName + "_Companion").build()

    /**
     * In order to take advantage of the buffer release callback of [companionSurfaceControl], a
     * buffer must be set on it to later be unset and released. For our purposes, this buffer
     * doesn't need to display anything useful, we just care about it for its release callback, so
     * create it as a 1x1 buffer to minimize the amount of graphics memory it occupies.
     */
    private val companionBufferRenderer =
        CanvasBufferedRenderer.Builder(
                width = COMPANION_BUFFER_SIZE_PX,
                height = COMPANION_BUFFER_SIZE_PX,
            )
            .setMaxBuffers(1)
            .setBufferFormat(HardwareBuffer.RGBA_8888)
            .build()

    override fun setup() {
        isInitialized.set(true)
        companionBufferRenderer.setContentRoot(
            RenderNode(this.javaClass.simpleName + "_Companion").apply {
                // The position within the buffer to draw. Since we're drawing a transparent pixel
                // it
                // doesn't really matter, but when debugging, it's useful to be able to change
                // COMPANION_BUFFER_SIZE_PX to something bigger (like 100) and the color to
                // something
                // visible (like Color.RED).
                setPosition(
                    /* left = */ 0,
                    /* top = */ 0,
                    /* right = */ COMPANION_BUFFER_SIZE_PX,
                    /* bottom = */ COMPANION_BUFFER_SIZE_PX,
                )
                // Apply some drawing instructions to the buffer to ensure that it's not released
                // too
                // early, which would affect our handoff timing and result in a flicker.
                beginRecording().drawColor(Color.TRANSPARENT)
                endRecording()
            }
        )
        setUpCompanionBuffer()
    }

    @UiThread
    override fun cleanup() {
        isInitialized.set(false)
        companionBufferRenderer.close()
        val transaction =
            SurfaceControlCompat.Transaction()
                .reparent(companionSurfaceControl, null)
                .setVisibility(companionSurfaceControl, false)
                // If there is a HardwareBuffer currently in use, this will trigger the release
                // callback
                // which will close the buffer after release.
                .setBuffer(companionSurfaceControl, null)
        transaction.commit()
        companionSurfaceControl.release()
    }

    @UiThread
    override fun requestCohortHandoff(handingOff: Map<InProgressStrokeId, FinishedStroke>) {
        if (!isInitialized.get()) {
            return
        }
        val renderRequest = companionBufferRenderer.obtainRenderRequest().preserveContents(true)

        // Use uiThreadExecutor to ensure the below code runs on the main thread. Otherwise, the
        // three
        // critical actions - the handoff callback, the translation, and the companion buffer being
        // released - are not guaranteed to happen in the same HWUI frame, which would result in a
        // flicker.
        renderRequest.drawAsync(uiThreadExecutor) {
            if (!isInitialized.get()) {
                return@drawAsync
            }
            onCohortHandoff(handingOff)

            // Translate the SurfaceView (and therefore the front buffer) off screen so that it is
            // no
            // longer visible, synchronized with the next HWUI draw. This is done as a translation
            // rather
            // than changing the visibility to avoid SurfaceView destroying the underlying surface,
            // which
            // results in a performance penalty to recreate.
            surfaceView.translationX = mainView.resources.displayMetrics.widthPixels * 2F
            surfaceView.translationY = mainView.resources.displayMetrics.heightPixels * 2F

            val rootSurfaceControl = checkNotNull(mainView.rootSurfaceControl)
            val transaction =
                SurfaceControlCompat.Transaction()
                    .reparent(companionSurfaceControl, rootSurfaceControl)
                    .setLayer(companionSurfaceControl, Int.MAX_VALUE)
                    .setVisibility(companionSurfaceControl, false)
                    // setBuffer(null) leads to the buffer's release callback being executed, which
                    // leads to
                    // onHandoffComplete.
                    .setBuffer(companionSurfaceControl, null)
            // The companion buffer will be on screen at the same time as the SurfaceView.
            transaction.commitTransactionOnDraw(rootSurfaceControl)

            // Ensure that the translation and the show transaction will actually take effect.
            mainView.invalidate()
        }
    }

    @UiThread
    private fun onHandoffComplete() {
        show()
        onCohortHandoffComplete()
    }

    /**
     * Move the front buffer layer back on screen with the next HWUI draw. The front buffer is being
     * cleared simultaneously with this via [onCohortHandoffComplete], but clearing the front buffer
     * is much faster than a HWUI operation so the front buffer will be clear by the time it is back
     * on screen.
     */
    @UiThread
    private fun show() {
        setUpCompanionBuffer()
    }

    /**
     * The companion buffer's visibility is kept in sync with [surfaceView] being on screen. Most
     * importantly, it is hidden at the exact same time as [surfaceView], and the companion buffer's
     * release callback is triggered immediately afterwards. That release callback is when we know
     * it is safe to clear the front buffer and show it again.
     */
    private fun setUpCompanionBuffer() {
        val renderRequest = companionBufferRenderer.obtainRenderRequest().preserveContents(true)

        // Use uiThreadExecutor to ensure the below code runs on the main thread. Unlike
        // requestCohortHandoff above, there aren't dire consequences (flickers) if the two main
        // actions
        // here - the translation and the companion buffer being shown - aren't in the same HWUI
        // frame,
        // but it makes the rest of the code easier to reason about if they are.
        renderRequest.drawAsync(uiThreadExecutor) { renderResult ->
            if (renderResult.status != CanvasBufferedRenderer.RenderResult.SUCCESS) {
                return@drawAsync
            }
            val buffer = renderResult.hardwareBuffer
            // Should never be null on U+. If it is null, then the release callback on setBuffer
            // will
            // never be called. That is why this class is limited to U+ even though
            // commitTransactionOnDraw is available on T+.
            val renderFence = checkNotNull(renderResult.fence)

            // Undo the translation to bring the SurfaceView (and therefore the front buffer) back
            // on
            // screen.
            surfaceView.translationX = 0F
            surfaceView.translationY = 0F

            val rootSurfaceControl = checkNotNull(mainView.rootSurfaceControl)
            val transaction =
                SurfaceControlCompat.Transaction()
                    .reparent(companionSurfaceControl, rootSurfaceControl)
                    .setLayer(companionSurfaceControl, Int.MAX_VALUE)
                    // The buffer must be visible, otherwise the below release fence will run too
                    // early.
                    .setVisibility(companionSurfaceControl, true)
                    // This release callback will be run later, when setBuffer(null) is called.
                    // TODO: b/328087803 - Samsung API 34 devices don't seem to execute this release
                    // callback,
                    //   so those devices use the V29 version of this class.
                    .setBuffer(companionSurfaceControl, buffer, renderFence) { releaseFence ->
                        releaseFence.awaitForever()
                        releaseFence.close()
                        if (!isInitialized.get()) {
                            buffer.close()
                            return@setBuffer
                        }
                        uiThreadExecutor.execute(::onHandoffComplete)
                        companionBufferRenderer.releaseBuffer(buffer, releaseFence)
                    }
            // The companion buffer will be on screen at the same time as the SurfaceView.
            transaction.commitTransactionOnDraw(rootSurfaceControl)

            // Ensure that the translation and the show transaction will actually take effect.
            mainView.invalidate()
        }
    }

    companion object {
        @Px const val COMPANION_BUFFER_SIZE_PX = 1
    }
}
