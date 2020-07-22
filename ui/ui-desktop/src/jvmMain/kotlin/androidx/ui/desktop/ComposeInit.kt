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

@file:Suppress("DEPRECATION_ERROR")

package androidx.ui.desktop

import androidx.compose.animation.core.rootAnimationClockFactory
import androidx.compose.InternalComposeApi
import androidx.compose.runtime.dispatch.DesktopUiDispatcher
import androidx.ui.graphics.DesktopCanvas
import androidx.ui.graphics.DesktopImageShader
import androidx.ui.graphics.DesktopInternalCanvasHolder
import androidx.ui.graphics.DesktopLinearGradientShader
import androidx.ui.graphics.DesktopPaint
import androidx.ui.graphics.DesktopPath
import androidx.ui.graphics.DesktopRadialGradientShader
import androidx.ui.graphics.GraphicsFactory
import androidx.compose.ui.text.platform.paragraphActualFactory
import androidx.compose.ui.text.platform.paragraphIntrinsicsActualFactory
import org.jetbrains.skija.Library

/**
 * Can be called multiple times.
 *
 * Initialization will occur only on the first call. The next calls will do nothing.
 *
 * Should be called in a class that uses Jetpack Compose Api:
 *
 * class SomeClass {
 *     companion object {
 *         init {
 *             initCompose()
 *         }
 *     }
 * }
 */
fun initCompose() {
    // call object initializer only once
    ComposeInit
}

@OptIn(androidx.compose.ui.text.android.InternalPlatformTextApi::class, InternalComposeApi::class)
private object ComposeInit {
    init {
        Library.load("/", "skija")
        // Until https://github.com/Kotlin/kotlinx.coroutines/issues/2039 is resolved
        // we have to set this property manually for coroutines to work.
        System.getProperties().setProperty("kotlinx.coroutines.fast.service.loader", "false")

        GraphicsFactory.nativeCanvas = ::DesktopCanvas
        GraphicsFactory.imageCanvas = ::DesktopCanvas
        GraphicsFactory.canvasHolder = ::DesktopInternalCanvasHolder
        GraphicsFactory.paint = ::DesktopPaint
        GraphicsFactory.path = { DesktopPath() }
        GraphicsFactory.Shader.linear = ::DesktopLinearGradientShader
        GraphicsFactory.Shader.radial = ::DesktopRadialGradientShader
        GraphicsFactory.Shader.image = ::DesktopImageShader
        paragraphIntrinsicsActualFactory = ::DesktopParagraphIntrinsics
        paragraphActualFactory = ::DesktopParagraph
        @OptIn(androidx.compose.animation.core.InternalAnimationApi::class)
        rootAnimationClockFactory = {
            // TODO: detect actual display refresh rate? what to do with displays with
            //  different refresh rates?
            DesktopAnimationClock(60, DesktopUiDispatcher.Dispatcher)
        }
    }
}