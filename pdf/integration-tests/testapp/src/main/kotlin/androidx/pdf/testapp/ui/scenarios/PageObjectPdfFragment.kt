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

package androidx.pdf.testapp.ui.scenarios

/*
 * import android.graphics.pdf.component.PdfPagePathObject
 */
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.pdf.testapp.R
import androidx.pdf.testapp.ui.view.PdfPageObjectView
import androidx.pdf.testapp.util.PdfPageAdapter
import androidx.pdf.testapp.util.PdfRendererAdapter
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

class PageObjectPdfFragment : Fragment() {
    private lateinit var openPdfButton: MaterialButton
    private lateinit var savePdfButton: MaterialButton
    private lateinit var pageObjectPdfView: PdfPageObjectView
    private lateinit var mPdfRenderer: PdfRendererAdapter
    private var mFileName: String = ""

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // Get the PDF fileName
                    mFileName = getFileName(uri)
                    // Open the PDF file
                    openPdf(uri)
                }
            }
        }

    // Register for activity result
    private val manageExternalStorageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (Environment.isExternalStorageManager()) {
                // Permission granted
                saveAsNewPdf()
            }
        }

    private fun getFileName(uri: Uri): String {
        var name: String = ""
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name.removeSuffix(PDF)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val pageObjectPdf = inflater.inflate(R.layout.page_object_pdf_fragment, container, false)
        return pageObjectPdf
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        openPdfButton = view.findViewById(R.id.open_pdf_button)
        savePdfButton = view.findViewById(R.id.save_pdf_button)
        pageObjectPdfView = view.findViewById(R.id.page_object_view)

        savePdfButton.setOnClickListener { savePdf() }
        openPdfButton.setOnClickListener { pickPdfFile() }
    }

    private fun pickPdfFile() {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = MIME_TYPE_PDF
            }
        filePicker.launch(intent)
    }

    private fun openPdf(uri: Uri) {
        val pfd = requireContext().contentResolver.openFileDescriptor(uri, "r")
        mPdfRenderer = PdfRendererAdapter(pfd!!)
        loadPageView()
    }

    private fun loadPageView() {
        try {
            val page: PdfPageAdapter = mPdfRenderer.openPage(0)
            val bitmap =
                Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888)
            page.render(bitmap)
            pageObjectPdfView.clear()
            pageObjectPdfView.setPageBitmap(bitmap)
            page.close()
        } catch (e: Exception) {
            Log.e(TAG, "Load PDF Failed", e)
        }
    }

    private fun savePdf() {
        try {
            /*
             * val page: PdfPageAdapter = mPdfRenderer.openPage(0)
             *
             * for (path in pageObjectPdfView.pdfPathList) {
             *    val pdfPathObject = PdfPagePathObject(path)
             *    pdfPathObject.setStrokeColor(Color.valueOf(0.0f, 0.0f, 0.0f, 1.0f))
             *    pdfPathObject.setStrokeWidth(pageObjectPdfView.mainPaint.strokeWidth)
             *
             *    page.addPageObject(pdfPathObject)
             * }
             * page.close()
             * if(checkFileCreatePermission()) {
             *    saveAsNewPdf()
             * }
             */
        } catch (e: Exception) {
            Log.e(TAG, "Save PDF Failed", e)
        }
    }

    private fun checkFileCreatePermission(): Boolean {
        if (!Environment.isExternalStorageManager()) {
            // Request MANAGE_EXTERNAL_STORAGE permission
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${requireContext().packageName}")
            manageExternalStorageLauncher.launch(intent)
        }
        return Environment.isExternalStorageManager()
    }

    private fun saveAsNewPdf() {
        val pdfDocument = PdfDocument()
        try {
            // Open the source page
            val sourcePage: PdfPageAdapter = mPdfRenderer.openPage(0)

            val bitmap =
                Bitmap.createBitmap(
                    sourcePage.getWidth(),
                    sourcePage.getHeight(),
                    Bitmap.Config.ARGB_8888,
                )

            sourcePage.render(bitmap)

            val newPageInfo =
                PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, FIRST_PAGE_NUM).create()

            val newPage = pdfDocument.startPage(newPageInfo)
            val canvas = newPage.canvas

            canvas.drawBitmap(bitmap, 0f, 0f, null)

            pdfDocument.finishPage(newPage)

            sourcePage.close()

            val outputFile =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    mFileName + EDIT_PDF,
                )

            // Write the PDF to the file
            FileOutputStream(outputFile).use { stream -> pdfDocument.writeTo(stream) }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing PDF: ", e)
        } finally {
            // Release resources
            pdfDocument.close()
        }
    }

    companion object {
        private const val EDIT_PDF = "_edit.pdf"
        private const val FIRST_PAGE_NUM = 0
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val PDF = ".pdf"
        private const val TAG = "PageObjectPdfFragment"
    }
}
