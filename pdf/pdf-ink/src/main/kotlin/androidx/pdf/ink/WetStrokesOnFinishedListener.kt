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

package androidx.pdf.ink

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.strokes.Stroke
import androidx.pdf.ink.util.toStampAnnotation

/**
 * Listener that processes finished "wet" strokes, converts them to annotations, and adds them to
 * the [EditableDocumentViewModel].
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
internal class WetStrokesOnFinishedListener(
    private val wetStrokesView: InProgressStrokesView,
    private val strokeIdToPageNumMap: MutableMap<InProgressStrokeId, Int>,
    private val annotationsViewModel: EditableDocumentViewModel,
) : InProgressStrokesFinishedListener {

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        super.onStrokesFinished(strokes)
        wetStrokesView.removeFinishedStrokes(strokes.keys)
        strokes.forEach { id, stroke ->
            strokeIdToPageNumMap[id]?.let { pageNum ->
                val annotation = stroke.toStampAnnotation(pageNum)
                annotationsViewModel.addDraftAnnotation(annotation)
            }
        }
    }
}
