/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("ExposedDropdownMenu_android")

package androidx.compose.material

import android.graphics.Rect as ViewRect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeIntRect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntRect

internal actual class WindowBoundsCalculator(private val view: View) {
    actual fun getVisibleWindowBounds(): IntRect = view.getWindowBounds()
}

@Composable
internal actual fun platformWindowBoundsCalculator(): WindowBoundsCalculator {
    val view = LocalView.current
    return remember(view) { WindowBoundsCalculator(view) }
}

@Composable
internal actual fun OnPlatformWindowBoundsChange(block: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val listener = OnGlobalLayoutListener(view, block)
        onDispose { listener.dispose() }
    }
}

/**
 * Subscribes to onGlobalLayout and correctly removes the callback when the View is detached. Logic
 * copied from AndroidPopup.android.kt.
 */
private class OnGlobalLayoutListener(
    private val view: View,
    private val onGlobalLayoutCallback: () -> Unit
) : View.OnAttachStateChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
    private var isListeningToGlobalLayout = false

    init {
        view.addOnAttachStateChangeListener(this)
        registerOnGlobalLayoutListener()
    }

    override fun onViewAttachedToWindow(p0: View) = registerOnGlobalLayoutListener()

    override fun onViewDetachedFromWindow(p0: View) = unregisterOnGlobalLayoutListener()

    override fun onGlobalLayout() = onGlobalLayoutCallback()

    private fun registerOnGlobalLayoutListener() {
        if (isListeningToGlobalLayout || !view.isAttachedToWindow) return
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
        isListeningToGlobalLayout = true
    }

    private fun unregisterOnGlobalLayoutListener() {
        if (!isListeningToGlobalLayout) return
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        isListeningToGlobalLayout = false
    }

    fun dispose() {
        unregisterOnGlobalLayoutListener()
        view.removeOnAttachStateChangeListener(this)
    }
}

private fun View.getWindowBounds(): IntRect =
    ViewRect().let {
        this.getWindowVisibleDisplayFrame(it)
        it.toComposeIntRect()
    }
