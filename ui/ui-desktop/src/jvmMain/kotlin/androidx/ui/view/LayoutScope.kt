/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.desktop.view

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.Recomposer
import androidx.compose.runtime.dispatch.DesktopUiDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.ui.core.Alignment
import androidx.ui.core.FontLoaderAmbient
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.setContent
import androidx.ui.desktop.DesktopPlatformInput
import androidx.ui.desktop.DesktopSelectionContainer
import androidx.ui.desktop.FontLoader
import androidx.ui.desktop.LayoutScopeGlobal
import androidx.compose.ui.text.input.TextInputService
import androidx.ui.unit.IntOffset
import com.jogamp.opengl.awt.GLCanvas
import org.jetbrains.skija.Canvas

class LayoutScope {
    var name: String = "LayoutScopeRoot"
    val context: Context
    val layout: ViewGroup
    var composeView: View? = null
    val children: MutableList<View> get() = layout.children
    fun getChildAt(i: Int) = children[i]
    val x: Int get() = layout.left
    val y: Int get() = layout.top
    val width: Int get() = layout.right - layout.left
    val height: Int get() = layout.bottom - layout.top

    // Optimization: we don't need more than one redrawing per tick
    var redrawingScheduled = false

    constructor(glCanvas: GLCanvas) {
        context = object : Context() {}
        layout = object : ViewGroup(context) {
            override fun onInvalidate() {
                if (!redrawingScheduled) {
                    DesktopUiDispatcher.Dispatcher.scheduleAfterCallback {
                        redrawingScheduled = false
                        if (Recomposer.current().hasPendingChanges()) {
                            onInvalidate()
                        } else {
                            redraw(glCanvas)
                        }
                    }
                    redrawingScheduled = true
                }
            }
        }
    }

    constructor(composeView: View, context: Context) {
        this.context = context
        this.composeView = composeView
        layout = object : ViewGroup(context) {}
    }

    internal lateinit var platformInputService: DesktopPlatformInput
        private set

    private fun redraw(glCanvas: GLCanvas) {
        glCanvas.display()
    }

    fun setContent(content: @Composable () -> Unit) {
        platformInputService = DesktopPlatformInput()
        ViewTreeLifecycleOwner.set(layout, object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override fun getLifecycle() = lifecycleRegistry
        })
        ViewTreeViewModelStoreOwner.set(layout, ViewModelStoreOwner {
            throw IllegalStateException("ViewModels creation is not supported")
        })
        LayoutScopeGlobal.addLayoutScope(this)
        layout.setContent(Recomposer.current(), null, @Composable {
            Providers(
                TextInputServiceAmbient provides TextInputService(
                    platformInputService),
                FontLoaderAmbient provides FontLoader()
            ) {
                DesktopSelectionContainer(children = content)
            }
        })
        layout.onAttachedToWindow()
    }

    fun dismiss() {
        ViewTreeLifecycleOwner.set(layout, null)
        LayoutScopeGlobal.removeLayoutScope(this)
    }

    fun removeView(view: View) {
        layout.removeView(view)
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        val androidCanvas = android.graphics.Canvas(canvas)
        androidCanvas.skijaCanvas.save()
        androidCanvas.skijaCanvas.translate(x.toFloat(), y.toFloat())
        if (children.isNotEmpty()) {
            val view = getChildAt(0)
            view.onMeasure(width, height)
            view.dispatchDraw(androidCanvas)
        }
        androidCanvas.skijaCanvas.restore()
    }

    fun dispatchTouchEvent(event: MotionEvent) {
        if (children.size > 0) {
            getChildAt(0).dispatchTouchEvent(event)
        }
    }

    var alignment: Alignment = Alignment.TopStart
    var offset: IntOffset = IntOffset(0, 0)

    fun setLayoutParams(alignment: Alignment, offset: IntOffset) {
        this.alignment = alignment
        this.offset = offset
    }
}