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

package androidx.pdf.signature

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.pdf.annotation.PdfViewportState
import androidx.pdf.models.Signature

/** A custom [View] responsible for drawing a single signature onto a Canvas. */
internal class SignatureView
@JvmOverloads
internal constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private var signatureData: Signature? = null

    internal fun setSignature(value: Signature) {
        if (signatureData != value) {
            signatureData = value
            invalidate()
            requestLayout()
        }
    }

    private var currentZoom: Float = 1f

    internal fun updatePdfViewport(viewport: PdfViewportState) {
        if (this.currentZoom != viewport.zoom) {
            this.currentZoom = viewport.zoom
        }
    }

    internal interface OnSignatureUpdatedListener {
        fun onSignatureUpdated(updatedSignature: Signature)

        fun onSignatureDeleted(id: String)
    }

    private var signatureUpdatedListener: OnSignatureUpdatedListener? = null

    /**
     * Sets the callback triggered for all signature interactions, including selection,
     * moving/resizing, and deletion.
     */
    internal fun setOnSignatureUpdatedListener(listener: OnSignatureUpdatedListener?) {
        this.signatureUpdatedListener = listener
    }
}
