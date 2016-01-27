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

package android.support.provider;

import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides basic implementation for creating, extracting and accessing
 * files within archives exposed by a document provider.
 *
 * <p>This class is thread safe. All methods can be called on any thread without
 * synchronization.
 *
 * TODO: Update the documentation. b/26047732
 * @hide
 */
public class DocumentArchiveHelper implements Closeable {
    /**
     * Cursor column to be used for passing the local file path for documents.
     * If it's not specified, then a snapshot will be created, which is slower
     * and consumes more resources.
     *
     * <p>Type: STRING
     */
    public static final String COLUMN_LOCAL_FILE_PATH = "local_file_path";

    private static final String TAG = "DocumentArchiveHelper";
    private static final int OPENED_ARCHIVES_CACHE_SIZE = 4;
    private static final String[] ZIP_MIME_TYPES = {
            "application/zip", "application/x-zip", "application/x-zip-compressed"
    };

    private final DocumentsProvider mProvider;
    private final char mIdDelimiter;

    // @GuardedBy("mArchives")
    private final LruCache<String, Loader> mArchives =
            new LruCache<String, Loader>(OPENED_ARCHIVES_CACHE_SIZE) {
                @Override
                public void entryRemoved(boolean evicted, String key,
                        Loader oldValue, Loader newValue) {
                    oldValue.getWriteLock().lock();
                    try {
                        oldValue.get().close();
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Failed to close an archive as it no longer exists.");
                    } finally {
                        oldValue.getWriteLock().unlock();
                    }
                }
            };

    /**
     * Creates a helper for handling archived documents.
     *
     * @param provider Instance of a documents provider which provides archived documents.
     * @param idDelimiter A character used to create document IDs within archives. Can be any
     *            character which is not used in any other document ID. If your provider uses
     *            numbers as document IDs, the delimiter can be eg. a colon. However if your
     *            provider uses paths, then a delimiter can be any character not allowed in the
     *            path, which is often \0.
     */
    public DocumentArchiveHelper(DocumentsProvider provider, char idDelimiter) {
        mProvider = provider;
        mIdDelimiter = idDelimiter;
    }

    /**
     * Lists child documents of an archive or a directory within an
     * archive. Must be called only for archives with supported mime type,
     * or for documents within archives.
     *
     * @see DocumentsProvider.queryChildDocuments(String, String[], String)
     */
    public Cursor queryChildDocuments(String documentId, @Nullable String[] projection,
            @Nullable String sortOrder)
            throws FileNotFoundException {
        Loader loader = null;
        try {
            loader = obtainInstance(documentId);
            return loader.get().queryChildDocuments(documentId, projection, sortOrder);
        } finally {
            releaseInstance(loader);
        }
    }

    /**
     * Returns a MIME type of a document within an archive.
     *
     * @see DocumentsProvider.getDocumentType(String)
     */
    public String getDocumentType(String documentId) throws FileNotFoundException {
        Loader loader = null;
        try {
            loader = obtainInstance(documentId);
            return loader.get().getDocumentType(documentId);
        } finally {
            releaseInstance(loader);
        }
    }

    /**
     * Returns true if a document within an archive is a child or any descendant of the archive
     * document or another document within the archive.
     *
     * @see DocumentsProvider.isChildDocument(String, String)
     */
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        Loader loader = null;
        try {
            loader = obtainInstance(documentId);
            return loader.get().isChildDocument(parentDocumentId, documentId);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            releaseInstance(loader);
        }
    }

    /**
     * Returns metadata of a document within an archive.
     *
     * @see DocumentsProvider.queryDocument(String, String[])
     */
    public Cursor queryDocument(String documentId, @Nullable String[] projection)
            throws FileNotFoundException {
        Loader loader = null;
        try {
            loader = obtainInstance(documentId);
            return loader.get().queryDocument(documentId, projection);
        } finally {
            releaseInstance(loader);
        }
    }

    /**
     * Opens a file within an archive.
     *
     * @see DocumentsProvider.openDocument(String, String, CancellationSignal))
     */
    public ParcelFileDescriptor openDocument(
            String documentId, String mode, final CancellationSignal signal)
            throws FileNotFoundException {
        Loader loader = null;
        try {
            loader = obtainInstance(documentId);
            return loader.get().openDocument(documentId, mode, signal);
        } finally {
            releaseInstance(loader);
        }
    }

    /**
     * Opens a thumbnail of a file within an archive.
     *
     * @see DocumentsProvider.openDocumentThumbnail(String, Point, CancellationSignal))
     */
    public AssetFileDescriptor openDocumentThumbnail(
            String documentId, Point sizeHint, final CancellationSignal signal)
            throws FileNotFoundException {
        Loader loader = null;
        try {
            loader = obtainInstance(documentId);
            return loader.get().openDocumentThumbnail(documentId, sizeHint, signal);
        } finally {
            releaseInstance(loader);
        }
    }

    /**
     * Returns true if the passed document ID is for a document within an archive.
     */
    public boolean isArchivedDocument(String documentId) {
        return ParsedDocumentId.hasPath(documentId, mIdDelimiter);
    }

    /**
     * Returns true if the passed mime type is supported by the helper.
     */
    public boolean isSupportedArchiveType(String mimeType) {
        for (final String zipMimeType : ZIP_MIME_TYPES) {
            if (zipMimeType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closes the helper and disposes all existing archives. It will block until all ongoing
     * operations on each opened archive are finished.
     */
    @Override
    public void close() {
        synchronized (mArchives) {
            mArchives.evictAll();
        }
    }

    /**
     * Releases resources for an archive with the specified document ID. It will block until all
     * operations on the archive are finished. If not opened, the method does nothing.
     *
     * <p>Calling this method is optional. The helper automatically closes the least recently used
     * archives if too many archives are opened.
     *
     * @param archiveDocumentId ID of the archive file.
     */
    public void closeArchive(String documentId) {
        synchronized (mArchives) {
            mArchives.remove(documentId);
        }
    }

    private Loader obtainInstance(String documentId) throws FileNotFoundException {
        Loader loader;
        synchronized (mArchives) {
            loader = getInstanceUncheckedLocked(documentId);
            loader.getReadLock().lock();
        }
        return loader;
    }

    private void releaseInstance(@Nullable Loader loader) {
        if (loader != null) {
            loader.getReadLock().unlock();
        }
    }

    private Loader getInstanceUncheckedLocked(String documentId)
            throws FileNotFoundException {
        try {
            final ParsedDocumentId id = ParsedDocumentId.fromDocumentId(documentId, mIdDelimiter);
            if (mArchives.get(id.mArchiveId) != null) {
                return mArchives.get(id.mArchiveId);
            }

            final Cursor cursor = mProvider.queryDocument(id.mArchiveId, new String[]
                    { Document.COLUMN_MIME_TYPE, COLUMN_LOCAL_FILE_PATH });
            cursor.moveToFirst();
            final String mimeType = cursor.getString(cursor.getColumnIndex(
                    Document.COLUMN_MIME_TYPE));
            Preconditions.checkArgument(isSupportedArchiveType(mimeType),
                    "Unsupported archive type.");
            final int columnIndex = cursor.getColumnIndex(COLUMN_LOCAL_FILE_PATH);
            final String localFilePath = columnIndex != -1 ? cursor.getString(columnIndex) : null;
            final File localFile = localFilePath != null ? new File(localFilePath) : null;
            final Uri notificationUri = cursor.getNotificationUri();
            final Loader loader = new Loader(mProvider, localFile, id, mIdDelimiter,
                    notificationUri);

            // Remove the instance from mArchives collection once the archive file changes.
            if (notificationUri != null) {
                final LruCache<String, Loader> finalArchives = mArchives;
                mProvider.getContext().getContentResolver().registerContentObserver(notificationUri,
                        false,
                        new ContentObserver(null) {
                            @Override
                            public void onChange(boolean selfChange, Uri uri) {
                                synchronized (mArchives) {
                                    final Loader currentLoader = mArchives.get(id.mArchiveId);
                                    if (currentLoader == loader) {
                                        mArchives.remove(id.mArchiveId);
                                    }
                                }
                            }
                        });
            }

            mArchives.put(id.mArchiveId, loader);
            return loader;
        } catch (IOException e) {
            // DocumentsProvider doesn't use IOException. For consistency convert it to
            // IllegalStateException.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Loads an instance of DocumentArchive lazily.
     */
    private static final class Loader {
        private final DocumentsProvider mProvider;
        private final File mLocalFile;
        private final ParsedDocumentId mId;
        private final char mIdDelimiter;
        private final Uri mNotificationUri;
        private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();
        private DocumentArchive mArchive = null;

        Loader(DocumentsProvider provider, @Nullable File localFile, ParsedDocumentId id,
                char idDelimiter, Uri notificationUri) {
            this.mProvider = provider;
            this.mLocalFile = localFile;
            this.mId = id;
            this.mIdDelimiter = idDelimiter;
            this.mNotificationUri = notificationUri;
        }

        synchronized DocumentArchive get() throws FileNotFoundException {
            if (mArchive != null) {
                return mArchive;
            }

            try {
                if (mLocalFile != null) {
                    mArchive = DocumentArchive.createForLocalFile(
                            mProvider.getContext(), mLocalFile, mId.mArchiveId, mIdDelimiter,
                            mNotificationUri);
                } else {
                    mArchive = DocumentArchive.createForParcelFileDescriptor(
                            mProvider.getContext(),
                            mProvider.openDocument(mId.mArchiveId, "r", null /* signal */),
                            mId.mArchiveId, mIdDelimiter, mNotificationUri);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            return mArchive;
        }

        Lock getReadLock() {
            return mLock.readLock();
        }

        Lock getWriteLock() {
            return mLock.writeLock();
        }
    }
}
