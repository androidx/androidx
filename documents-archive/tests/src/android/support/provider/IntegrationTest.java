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

import android.content.ContentProviderClient;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.IllegalArgumentException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * Integration tests for DocumentsProvider and DocumentArchiveHelper.
 *
 * <p>Only checks if the provider, then helper are forwarding the calls to the
 * underlying {@code ArchiveDocument} correctly. More detailed output testing is
 * done in {@code DocumentArchiveTest}.
 */
public class IntegrationTest extends AndroidTestCase {
    private ContentProviderClient mClient;

    @Override
    public void setUp() throws RemoteException {
        mClient = getContext().getContentResolver().acquireContentProviderClient(
                StubProvider.AUTHORITY);
        assertNotNull(mClient);
        mClient.call("reset", null, null);
    }

    @Override
    public void tearDown() {
        if (mClient != null) {
            mClient.release();
            mClient = null;
        }
    }

    public void testQueryForChildren() throws IOException {
        final Cursor cursor = mContext.getContentResolver().query(
                DocumentsContract.buildChildDocumentsUri(
                        StubProvider.AUTHORITY, StubProvider.DOCUMENT_ID),
                        null, null, null, null);
        assertEquals(3, cursor.getCount());
    }

    public void testQueryForDocument_Archive()
            throws IOException, RemoteException, InterruptedException {
        final Cursor cursor = mContext.getContentResolver().query(
                DocumentsContract.buildDocumentUri(
                        StubProvider.AUTHORITY, StubProvider.DOCUMENT_ID),
                        null, null, null, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(Document.FLAG_ARCHIVE,
                cursor.getInt(cursor.getColumnIndexOrThrow(Document.COLUMN_FLAGS)));
    }

    public void testQueryForDocument_ArchiveDescendant()
            throws IOException, RemoteException, InterruptedException {
        final Cursor cursor = mContext.getContentResolver().query(
                DocumentsContract.buildDocumentUri(
                        StubProvider.AUTHORITY, StubProvider.FILE_DOCUMENT_ID),
                        null, null, null, null);
        assertEquals(1, cursor.getCount());
        assertEquals(StubProvider.NOTIFY_URI, cursor.getNotificationUri());

        final CountDownLatch changeSignal = new CountDownLatch(1);
        final ContentObserver observer = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                changeSignal.countDown();
            }
        };

        try {
            getContext().getContentResolver().registerContentObserver(
                    cursor.getNotificationUri(), false /* notifyForDescendants */, observer);

            // Simulate deleting the archive file, then confirm that the notification is
            // propagated and the archive closed.
            mClient.call("delete", null, null);
            changeSignal.await();

            mContext.getContentResolver().query(
                    DocumentsContract.buildChildDocumentsUri(
                            StubProvider.AUTHORITY, StubProvider.FILE_DOCUMENT_ID),
                            null, null, null, null);
            fail("Expected IllegalStateException, but succeeded.");
        } catch (IllegalStateException e) {
            // Expected, as the file is gone.
        } finally {
            getContext().getContentResolver().unregisterContentObserver(observer);
        }
    }

    public void testGetType() throws IOException {
        assertEquals("text/plain", mContext.getContentResolver().getType(
                DocumentsContract.buildDocumentUri(
                        StubProvider.AUTHORITY, StubProvider.FILE_DOCUMENT_ID)));
    }

    public void testOpenFileDescriptor() throws IOException {
        final ParcelFileDescriptor descriptor = mContext.getContentResolver().openFileDescriptor(
                DocumentsContract.buildDocumentUri(
                        StubProvider.AUTHORITY, StubProvider.FILE_DOCUMENT_ID),
                        "r", null);
        assertNotNull(descriptor);
    }
}
