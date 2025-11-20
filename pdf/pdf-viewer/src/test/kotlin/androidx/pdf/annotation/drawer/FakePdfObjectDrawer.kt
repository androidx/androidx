/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.annotation.drawer

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.pdf.annotation.models.PdfObject

/**
 * Fake implementation of [PdfObjectDrawer] for testing purposes. It records the arguments of the
 * draw method invocations.
 */
internal class FakePdfObjectDrawer<T : PdfObject> : PdfObjectDrawer<T> {
    data class DrawInvocation<T : PdfObject>(
        val pdfObject: T,
        val canvas: Canvas,
        val transform: Matrix,
    )

    val drawInvocations = mutableListOf<DrawInvocation<T>>()

    override fun draw(pdfObject: T, canvas: Canvas, transform: Matrix) {
        drawInvocations.add(DrawInvocation(pdfObject, canvas, transform))
    }
}
