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
package androidx.ui.desktop.test

import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.animation.core.rootAnimationClockFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.FontLoaderAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.platform.FontLoader
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.ui.desktop.initCompose
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Surface

class TestSkiaWindow(
    val width: Int,
    val height: Int
) {
    val surface: Surface
    val canvas: Canvas
    var androidCanvas: android.graphics.Canvas
    init {
        surface = Surface.makeRasterN32Premul(width, height)
        canvas = surface.canvas
        androidCanvas = android.graphics.Canvas(canvas)
    }

    @OptIn(androidx.compose.ui.text.android.InternalPlatformTextApi::class)
    companion object {
        init {
            initCompose()
        }
    }

    @Suppress("WrongCall")
    @OptIn(androidx.compose.animation.core.InternalAnimationApi::class)
    fun setContent(content: @Composable () -> Unit) {
        val clocks = mutableListOf<ManualAnimationClock>()
        rootAnimationClockFactory = {
            ManualAnimationClock(0L).also {
                clocks.add(it)
            }
        }

        val context = object : Context() {}
        val viewGroup = object : ViewGroup(context) {}
        ViewTreeLifecycleOwner.set(viewGroup, object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override fun getLifecycle() = lifecycleRegistry
        })
        ViewTreeViewModelStoreOwner.set(viewGroup, ViewModelStoreOwner {
            throw IllegalStateException("ViewModels creation is not supported")
        })
        viewGroup.setContent(Recomposer.current(), null, @Composable {
            Providers(
                FontLoaderAmbient provides FontLoader(),
                children = content
            )
        })
        viewGroup.onAttachedToWindow()
        val view = viewGroup.getChildAt(0)

        view.onMeasure(width, height)
        view.onLayout(true, 0, 0, width, height)
        view.dispatchDraw(androidCanvas)
    }
}
