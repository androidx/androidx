/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.provider.tests;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.provider.DocumentsProvider;
import android.os.ParcelFileDescriptor;
import android.os.CancellationSignal;
import android.provider.DocumentsContract.Document;
import android.support.provider.DocumentArchiveHelper;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;

/**
 * Stub provider for testing support for archives.
 */
public class StubProvider extends DocumentsProvider {
    public static final String AUTHORITY = "android.support.provider.tests.mystubprovider";
    public static final String DOCUMENT_ID = "document-id";
    public static final String FILE_DOCUMENT_ID = "document-id:dir1/cherries.txt";
    public File mFile;
    public DocumentArchiveHelper mArchiveHelper;

    private static final String TAG = "StubProvider";
    private static final String[] DEFAULT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_SIZE,
            Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS,
            DocumentArchiveHelper.COLUMN_LOCAL_FILE_PATH
    };

    @Override
    public boolean onCreate() {
        try {
            mArchiveHelper = new DocumentArchiveHelper(this, ':');
            mFile = TestUtils.createFileFromResource(getContext(), R.raw.archive);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize StubProvider.");
            return false;
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(
            String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.openDocument(documentId, mode, signal);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor queryChildDocuments(
            String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(parentDocumentId) ||
                mArchiveHelper.isSupportedArchiveType(getDocumentType(parentDocumentId))) {
            return mArchiveHelper.queryChildDocuments(parentDocumentId, projection, sortOrder);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.queryDocument(documentId, projection);
        }

        if (DOCUMENT_ID.equals(documentId)) {
            final MatrixCursor result = new MatrixCursor(
                    projection != null ? projection : DEFAULT_PROJECTION);
            final RowBuilder row = result.newRow();
            row.add(Document.COLUMN_DOCUMENT_ID, DOCUMENT_ID);
            row.add(Document.COLUMN_DISPLAY_NAME, mFile.getName());
            row.add(Document.COLUMN_SIZE, mFile.length());
            row.add(Document.COLUMN_MIME_TYPE, DocumentArchiveHelper.ZIP_MIME_TYPE);
            final int flags = mArchiveHelper.isSupportedArchiveType("application/zip")
                    ? Document.FLAG_ARCHIVE : 0;
            row.add(Document.COLUMN_FLAGS, flags);
            row.add(DocumentArchiveHelper.COLUMN_LOCAL_FILE_PATH, mFile.getPath());
            return result;
        }

        throw new FileNotFoundException();
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.getDocumentType(documentId);
        }

        if (DOCUMENT_ID.equals(documentId)) {
            return "application/zip";
        }

        throw new UnsupportedOperationException();
    }
}
