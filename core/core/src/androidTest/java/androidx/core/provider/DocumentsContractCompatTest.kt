/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.provider

import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents"
private const val DOWNLOAD_DOCID = "primary:Download"

/** Tests for {@link DocumentsContractCompat}. */
@SmallTest
class DocumentsContractCompatTest {

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testVirtualDocConstantCheck() {
        assertEquals(
            DocumentsContractCompat.DocumentCompat.FLAG_VIRTUAL_DOCUMENT,
            DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT,
        )
    }

    @Test
    fun testBuildChildDocumentsUri() {
        assertEquals(
            DocumentsContractCompat.buildChildDocumentsUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            ),
            DocumentsContract.buildChildDocumentsUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            ),
        )
    }

    @Test
    fun testBuildChildDocumentsUriUsingTree() {
        val treeUri =
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            )
        assertEquals(
            DocumentsContractCompat.buildChildDocumentsUriUsingTree(treeUri, DOWNLOAD_DOCID),
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DOWNLOAD_DOCID),
        )
    }

    @Test
    fun testBuildDocumentUri() {
        assertEquals(
            DocumentsContractCompat.buildDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            ),
            DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_PROVIDER_AUTHORITY, DOWNLOAD_DOCID),
        )
    }

    @Test
    fun testBuildDocumentUriUsingTree() {
        val treeUri =
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            )
        assertEquals(
            DocumentsContractCompat.buildDocumentUriUsingTree(treeUri, DOWNLOAD_DOCID),
            DocumentsContract.buildDocumentUriUsingTree(treeUri, DOWNLOAD_DOCID),
        )
    }

    @Test
    fun testBuildTreeDocumentUri() {
        assertEquals(
            DocumentsContractCompat.buildTreeDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            ),
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            ),
        )
    }

    @Test
    fun testGetDocumentId() {
        val documentUri =
            DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_PROVIDER_AUTHORITY, DOWNLOAD_DOCID)
        assertEquals(
            DocumentsContractCompat.getDocumentId(documentUri),
            DocumentsContract.getDocumentId(documentUri),
        )
    }

    @Test
    fun testGetTreeDocumentId() {
        val treeUri =
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            )
        val treeUriWithDocId = DocumentsContract.buildDocumentUriUsingTree(treeUri, DOWNLOAD_DOCID)
        assertEquals(
            DocumentsContractCompat.getTreeDocumentId(treeUri),
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        assertEquals(
            DocumentsContractCompat.getTreeDocumentId(treeUriWithDocId),
            DocumentsContract.getTreeDocumentId(treeUriWithDocId),
        )
    }

    @Test
    fun testIsTreeUri() {
        val documentUri =
            DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_PROVIDER_AUTHORITY, DOWNLOAD_DOCID)
        assertFalse(DocumentsContractCompat.isTreeUri(documentUri))
        val downloadTree =
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            )
        val downloadTreeDocUri =
            DocumentsContract.buildDocumentUriUsingTree(downloadTree, DOWNLOAD_DOCID)
        // A bare "tree" Uri is a tree Uri.
        assertTrue(DocumentsContractCompat.isTreeUri(downloadTree))
        // So is a "tree" Uri that includes a "document" part.
        assertTrue(DocumentsContractCompat.isTreeUri(downloadTreeDocUri))
    }

    /**
     * This is a "medium" test because it performs IPC to lookup if the authority of the Uri passed
     * in is a [android.provider.DocumentsProvider]. To be safe, we use the authority of
     * `com.android.externalstorage.ExternalStorageProvider`.
     */
    @MediumTest
    @Test
    fun testIsDocumentUri() {
        val context = InstrumentationRegistry.getInstrumentation().context

        // MediaStore Uris are not DocumentUris.
        val mediaStoreUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        assertFalse(DocumentsContractCompat.isDocumentUri(context, mediaStoreUri))

        val documentUri =
            DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_PROVIDER_AUTHORITY, DOWNLOAD_DOCID)
        assertTrue(DocumentsContractCompat.isDocumentUri(context, documentUri))

        val downloadTree =
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                DOWNLOAD_DOCID,
            )
        val downloadTreeDocUri =
            DocumentsContract.buildDocumentUriUsingTree(downloadTree, DOWNLOAD_DOCID)
        // A bare "tree" Uri is not a "document" Uri.
        assertFalse(DocumentsContractCompat.isDocumentUri(context, downloadTree))
        // But a "tree" with a "document" part is.
        assertTrue(DocumentsContractCompat.isDocumentUri(context, downloadTreeDocUri))
    }

    /** Helper method that works similar to [DocumentsContract.buildDocumentUri]. */
    @Suppress("SameParameterValue")
    private fun buildDocUri(authority: String, docId: String) =
        Uri.Builder()
            .scheme("content")
            .authority(authority)
            .appendPath("document")
            .appendPath(docId)
            .build()

    /** Helper method that works similar to [DocumentsContract.buildDocumentUriUsingTree]. */
    @Suppress("SameParameterValue")
    private fun buildDocUriWithTree(treeUri: Uri, docId: String) =
        Uri.Builder()
            .scheme("content")
            .authority(treeUri.authority)
            .appendPath("tree")
            .appendPath(treeUri.pathSegments[1])
            .appendPath("document")
            .appendPath(docId)
            .build()

    /** Helper method that works similar to [DocumentsContract.buildTreeDocumentUri]. */
    @Suppress("SameParameterValue")
    private fun buildTreeDocUri(authority: String, docId: String) =
        Uri.Builder()
            .scheme("content")
            .authority(authority)
            .appendPath("tree")
            .appendPath(docId)
            .build()
}
