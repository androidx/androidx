/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.core

import android.content.Context
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.TextSpan
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.paragraph.RenderParagraph
import com.google.r4a.Component
import com.google.r4a.composer

/**
 * Text Widget Crane version.
 */
class Text() : Component() {
    lateinit var text: TextSpan

    override fun compose() {
        assert(text != null)
        <MeasureBox> constraints, measureOperations ->
            val renderParagraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)
            val context = composer.composer.context

            // TODO(Migration/siyamed): This is temporary and should be removed when resource
            // system is resolved.
            attachContextToFont(text, context)

            val boxConstraints = BoxConstraints(
                    constraints.minWidth.toPx(context).toDouble(),
                    constraints.maxWidth.toPx(context).toDouble(),
                    constraints.minHeight.toPx(context).toDouble(),
                    constraints.maxHeight.toPx(context).toDouble())
            renderParagraph.layoutTextWithConstraints(boxConstraints)
            measureOperations.collect {
                <Draw> canvas, parent ->
                    renderParagraph.textPainter.paint(canvas, Offset(0.0, 0.0))
                </Draw>
            }
            measureOperations.layout(
                    renderParagraph.textPainter.width.toDp(context),
                    renderParagraph.textPainter.height.toDp(context)) {}
        </MeasureBox>
    }

    private fun attachContextToFont(
        text: TextSpan,
        context: Context
    ) {
        text.visitTextSpan() {
            it.style?.fontFamily?.let {
                it.context = context
            }
            true
        }
    }
}