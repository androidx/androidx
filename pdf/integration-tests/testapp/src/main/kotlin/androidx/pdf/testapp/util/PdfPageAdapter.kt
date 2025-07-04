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

package androidx.pdf.testapp.util

/*
 * import android.graphics.pdf.component.PdfPageObject
 */
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRendererPreV
import android.graphics.pdf.RenderParams
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.core.util.Supplier

public class PdfPageAdapter {
    private var pageNum: Int = 0
    private var pdfRendererPage: PdfRenderer.Page? = null
    private var pdfRendererPreVPage: PdfRendererPreV.Page? = null

    constructor(pdfRenderer: PdfRenderer, pageNum: Int) {
        this.pageNum = pageNum
        pdfRendererPage = pdfRenderer.openPage(pageNum)
    }

    constructor(pdfRendererPreV: PdfRendererPreV, pageNum: Int) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            this.pageNum = pageNum
            pdfRendererPreVPage = pdfRendererPreV.openPage(pageNum)
        }
    }

    fun render(bitmap: Bitmap) {
        if (pdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            pdfRendererPage?.render(bitmap, null, null, getRenderParams())
        } else {
            checkAndExecute { pdfRendererPreVPage?.render(bitmap, null, null, getRenderParams()) }
        }
    }

    fun getWidth(): Int {
        return if (pdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            pdfRendererPage?.width ?: 0
        } else {
            checkAndExecute { pdfRendererPreVPage?.width ?: 0 }
        }
    }

    fun getHeight(): Int {
        return if (pdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            pdfRendererPage?.height ?: 0
        } else {
            checkAndExecute { pdfRendererPreVPage?.height ?: 0 }
        }
    }

    private fun getRenderParams(): RenderParams {
        return checkAndExecute {
            RenderParams.Builder(RenderParams.RENDER_MODE_FOR_DISPLAY)
                .setRenderFlags(
                    RenderParams.FLAG_RENDER_HIGHLIGHT_ANNOTATIONS or
                        RenderParams.FLAG_RENDER_TEXT_ANNOTATIONS
                )
                .build()
        }
    }

    /*
     *  fun addPageObject(pdfPageObject: PdfPageObject): Int {
     *      return if (pdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
     *          pdfRendererPage?.addPageObject(pdfPageObject) ?: 0
     *      } else {
     *          checkAndExecute { pdfRendererPreVPage?.addPageObject(pdfPageObject) ?: 0 }
     *      }
     *  }
     */

    fun close() {
        if (pdfRendererPage != null && Build.VERSION.SDK_INT >= 35) {
            pdfRendererPage?.close()
            pdfRendererPage = null
        } else {
            checkAndExecute {
                pdfRendererPreVPage?.close()
                pdfRendererPreVPage = null
            }
        }
    }

    private fun checkAndExecute(block: Runnable) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            block.run()
            return
        }
        throw UnsupportedOperationException("Operation support above S")
    }

    private fun <T> checkAndExecute(block: Supplier<T>): T {
        return if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            block.get()
        } else {
            throw UnsupportedOperationException("Operation support above S")
        }
    }
}
