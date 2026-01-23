/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement

class ATestToCheckWebGLContext {

    @Test
    fun test() {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        document.body!!.appendChild(canvas)
        val webGl2 = canvas.getContext("webgl2")
        println("WebGl2 = " + webGl2 + "\n")

        var hasWeglContext = webGl2 != null
        if (webGl2 == null) {
            println("WebGl2 is not supported")
            val webgl1 = canvas.getContext("webgl")
            println("WebGl1 = " + webgl1 + "\n")
            hasWeglContext = webgl1 != null
        }

        canvas.remove()

        assertTrue(hasWeglContext, "Expected hasWeglContext to be true, but was - $hasWeglContext")
    }
}