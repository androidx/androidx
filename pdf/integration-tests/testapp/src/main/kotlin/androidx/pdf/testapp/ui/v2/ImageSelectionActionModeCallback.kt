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

package androidx.pdf.testapp.ui.v2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.FileProvider
import androidx.pdf.PdfPoint
import androidx.pdf.selection.model.ImageSelection
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectionActionModeCallback(
    private val context: Context,
    private val imageSelection: ImageSelection,
    private val coroutineScope: CoroutineScope,
    private val pdfPointToViewPoint: (pdfPoint: PdfPoint) -> PointF?,
) : ActionMode.Callback2() {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        menu.add(Menu.NONE, android.R.id.copy, Menu.NONE, android.R.string.copy)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == android.R.id.copy) {
            handleCopyImage()
            mode.finish()
            return true
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        // No-op
    }

    override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
        val bounds = imageSelection.bounds.firstOrNull() ?: return

        val topPoint = pdfPointToViewPoint(PdfPoint(bounds.pageNum, bounds.left, bounds.top))
        val bottomPoint = pdfPointToViewPoint(PdfPoint(bounds.pageNum, bounds.right, bounds.bottom))

        outRect?.set(
            topPoint?.x?.toInt() ?: bounds.left.toInt(),
            topPoint?.y?.toInt() ?: bounds.top.toInt(),
            bottomPoint?.x?.toInt() ?: bounds.right.toInt(),
            bottomPoint?.y?.toInt() ?: bounds.bottom.toInt(),
        )
    }

    private fun handleCopyImage() {
        coroutineScope.launch {
            val imageUri =
                withContext(Dispatchers.IO) { saveBitmapToCache(context, imageSelection.bitmap) }
            copyToClipboard(context, imageUri)
        }
    }
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    val sharedImagesDir = File(context.cacheDir, SHARED_IMAGE_DIR)

    // Ensure the directory exists
    if (!sharedImagesDir.exists()) {
        sharedImagesDir.mkdirs()
    } else {
        // If it exists, clear the files inside
        sharedImagesDir.listFiles()?.forEach { file -> file.delete() }
    }

    val fileName = "shared_image_${System.currentTimeMillis()}.png"
    val sharedImageFile = File(sharedImagesDir, fileName)

    FileOutputStream(sharedImageFile).use { imageFileOutputStream ->
        // The quality is ignored for PNG, which is lossless.
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageFileOutputStream)
    }

    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, sharedImageFile)
}

private fun copyToClipboard(context: Context, uri: Uri) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    val clipData = ClipData.newUri(context.contentResolver, SELECTED_IMAGE_LABEL, uri)
    clipboardManager?.setPrimaryClip(clipData)
}

private const val SHARED_IMAGE_DIR = "images"
private const val SELECTED_IMAGE_LABEL = "selected image"
