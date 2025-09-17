/*
 * Copyright (C) 2023 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.capture

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import androidx.core.graphics.createBitmap

/**
 * Implements an AbstractComposeView to run Compose functions on a 1x1 backing surface, capturing
 * the canvas commands via RecordingCanvas.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CaptureComposeView
@JvmOverloads
public constructor(
    context: Context,
    public var immediateCapture: Boolean,
    public var onPaint: (View, RemoteComposeWriter) -> Boolean,
    public val onCaptureReady: @Composable () -> Unit,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {
    public val initialImmediateCapture: Boolean = immediateCapture
    public val recordingCanvas: RecordingCanvas = RecordingCanvas(createBitmap(1, 1))
    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke()
    }

    public override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    /**
     * Set the Jetpack Compose UI content for this view. Initial composition will occur when the
     * view becomes attached to a window or when [createComposition] is called, whichever comes
     * first.
     */
    public fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = content
        if (isAttachedToWindow) {
            createComposition()
        }
        contentDone = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        recordContent()
        recordingCanvas.paint(canvas)
        if (!immediateCapture) {
            invalidate()
        }
    }

    public var contentDone: Boolean = false

    public fun recordContent() {
        if (contentDone) {
            recordingCanvas.document.reset()
        } else {
            contentDone = true
        }
        recordingCanvas.document.root { super.dispatchDraw(recordingCanvas) }
        val captured = onPaint(this, recordingCanvas.document)
        if (!initialImmediateCapture && captured) {
            immediateCapture = true
        }
    }

    public fun setRemoteComposeState(remoteComposeCreationState: RemoteComposeCreationState) {
        recordingCanvas.setRemoteComposeCreationState(remoteComposeCreationState)
    }
}
