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

package androidx.compose.ui.window

import androidx.compose.ui.platform.IOSSkikoInput
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoGestureEvent
import org.jetbrains.skiko.SkikoInputEvent
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoPointerEvent

// TODO: candidate for refactor
internal interface IOSSkikoView {
    // Input
    fun onKeyboardEvent(event: SkikoKeyboardEvent) = Unit
    fun onPointerEvent(event: SkikoPointerEvent) = Unit

    @Deprecated("This method will be removed. Use override val input: SkikoInput")
    fun onInputEvent(event: SkikoInputEvent) = Unit
    val input: IOSSkikoInput get() = IOSSkikoInput.Empty
    fun onGestureEvent(event: SkikoGestureEvent) = Unit

    // Rendering
    fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long)
}