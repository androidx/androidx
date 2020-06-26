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

package androidx.ui.desktop.emu

import androidx.ui.graphics.Canvas
import org.jetbrains.skija.Library
import org.jetbrains.skija.Surface
import org.junit.After

abstract class AndroidEmuTest {
    private var surface: Surface? = null

    @Suppress("SameParameterValue")
    protected fun initCanvas(width: Int, height: Int): Canvas {
        require(surface == null)
        surface = Surface.makeRasterN32Premul(width, height)
        return Canvas(android.graphics.Canvas(surface!!.canvas))
    }

    @After
    fun teardown() {
        surface?.close()
    }

    private companion object {
        init {
            Library.load("/", "skija")
        }
    }
}