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

package androidx.compose.ui

import kotlin.math.abs
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.asList

/**
 * An interface with helper functions to initialise the tests
 */
internal interface OnCanvasTests {

    val canvasId: String
        get() = "canvas1"

    fun createCanvasAndAttach(id: String = canvasId): HTMLCanvasElement {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.setAttribute("id", canvasId)
        canvas.setAttribute("tabindex", "0")

        document.body!!.appendChild(canvas)
        return canvas
    }

    fun commonAfterTest() {
        document.getElementById(canvasId)?.remove()
    }

    fun assertApproximatelyEqual(expected: Float, actual: Float, tolerance: Float = 1f) {
        if (abs(expected - actual) > tolerance) {
            throw AssertionError("Expected $expected but got $actual. Difference is more than the allowed delta $tolerance")
        }
    }
}