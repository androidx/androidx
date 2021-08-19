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

package androidx.core.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Helper for accessing features in {@link DocumentsContract}.
 */
@SuppressWarnings("unused")
public final class DocumentsContractCompat {

    /**
     * Helper for accessing features in {@link DocumentsContract.Document}.
     */
    public static final class DocumentCompat {
        /**
         * Flag indicating that a document is virtual, and doesn't have byte
         * representation in the MIME type specified as {@link Document#COLUMN_MIME_TYPE}.
         *
         * <p><em>Virtual documents must have at least one alternative streamable
         * format via {@link DocumentsProvider#openTypedDocument}</em>
         *
         * @see Document#FLAG_VIRTUAL_DOCUMENT
         */
        public static final int FLAG_VIRTUAL_DOCUMENT = 1 << 9;

        private DocumentCompat() {
        }
    }

    private static final String PATH_TREE = "tree";

    /**
     * Checks if the given URI represents a {@link Document} backed by a
     * {@link DocumentsProvider}.
     *
     * @see DocumentsContract#isDocumentUri(Context, Uri)
     */
    public static boolean isDocumentUri(@NonNull Context context, @Nullable Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return DocumentsContractApi19Impl.isDocumentUri(context, uri);
        }
        return false;
    }

    /**
     * Checks if the given URI represents a {@link Document} tree.
     *
     * @see DocumentsContract#isTreeUri(Uri)
     */
    public static boolean isTreeUri(@NonNull Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // While "tree" Uris were added in 21, the check was only (publicly) added in 24
            final List<String> paths = uri.getPathSegments();
            return (paths.size() >= 2 && PATH_TREE.equals(paths.get(0)));
        } else {
            return DocumentsContractApi24Impl.isTreeUri(uri);
        }
    }

    /**
     * Extract the {@link Document#COLUMN_DOCUMENT_ID} from the given URI.
     *
     * @see DocumentsContract#getDocumentId(Uri)
     */
    @Nullable
    public static String getDocumentId(@NonNull Uri documentUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return DocumentsContractApi19Impl.getDocumentId(documentUri);
        }
        return null;
    }

    /**
     * Extract the via {@link Document#COLUMN_DOCUMENT_ID} from the given URI.
     *
     * @see DocumentsContract#getTreeDocumentId(Uri)
     */
    @Nullable
    public static String getTreeDocumentId(@NonNull Uri documentUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.getTreeDocumentId(documentUri);
        }
        return null;
    }

    /**
     * Build URI representing the target {@link Document#COLUMN_DOCUMENT_ID} in
     * a document provider. When queried, a provider will return a single row
     * with columns defined by {@link Document}.
     *
     * @see DocumentsContract#buildDocumentUri(String, String)
     */
    @Nullable
    public static Uri buildDocumentUri(@NonNull String authority, @NonNull String documentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return DocumentsContractApi19Impl.buildDocumentUri(authority, documentId);
        }
        return null;
    }

    /**
     * Build URI representing the target {@link Document#COLUMN_DOCUMENT_ID} in
     * a document provider. When queried, a provider will return a single row
     * with columns defined by {@link Document}.
     */
    @Nullable
    public static Uri buildDocumentUriUsingTree(@NonNull Uri treeUri, @NonNull String documentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildDocumentUriUsingTree(treeUri, documentId);
        }
        return null;
    }

    /**
     * Build URI representing access to descendant documents of the given
     * {@link Document#COLUMN_DOCUMENT_ID}.
     *
     * @see DocumentsContract#buildTreeDocumentUri(String, String)
     */
    @Nullable
    public static Uri buildTreeDocumentUri(@NonNull String authority, @NonNull String documentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildTreeDocumentUri(authority, documentId);
        }
        return null;
    }

    /**
     * Build URI representing the children of the target directory in a document
     * provider. When queried, a provider will return zero or more rows with
     * columns defined by {@link Document}.
     *
     * @see DocumentsContract#buildChildDocumentsUri(String, String)
     */
    @Nullable
    public static Uri buildChildDocumentsUri(@NonNull String authority,
            @Nullable String parentDocumentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildChildDocumentsUri(authority, parentDocumentId);
        }
        return null;
    }

    /**
     * Build URI representing the children of the target directory in a document
     * provider. When queried, a provider will return zero or more rows with
     * columns defined by {@link Document}.
     *
     * @see DocumentsContract#buildChildDocumentsUriUsingTree(Uri, String)
     */
    @Nullable
    public static Uri buildChildDocumentsUriUsingTree(@NonNull Uri treeUri,
            @NonNull String parentDocumentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildChildDocumentsUriUsingTree(treeUri,
                    parentDocumentId);
        }
        return null;
    }

    /**
     * Create a new document with given MIME type and display name.
     *
     * @param parentDocumentUri directory with {@link Document#FLAG_DIR_SUPPORTS_CREATE}
     * @param mimeType          MIME type of new document
     * @param displayName       name of new document
     * @return newly created document, or {@code null} if failed
     */
    @Nullable
    public static Uri createDocument(@NonNull ContentResolver content,
            @NonNull Uri parentDocumentUri, @NonNull String mimeType, @NonNull String displayName)
            throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.createDocument(content, parentDocumentUri, mimeType,
                    displayName);
        }
        return null;
    }

    /**
     * Change the display name of an existing document.
     *
     * @see DocumentsContract#renameDocument(ContentResolver, Uri, String)
     */
    @Nullable
    public static Uri renameDocument(@NonNull ContentResolver content,
            @NonNull Uri documentUri, @NonNull String displayName) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.renameDocument(content, documentUri, displayName);
        }
        return null;
    }

    /**
     * Removes the given document from a parent directory.
     *
     * In contrast to {@link DocumentsContract#deleteDocument} it requires specifying the parent.
     * This method is especially useful if the document can be in multiple parents.
     *
     * This method was only added in {@link Build.VERSION_CODES#N}. On versions prior to this,
     * this method calls through to {@link DocumentsContract#deleteDocument(ContentResolver, Uri)}.
     *
     * @param documentUri       document with {@link Document#FLAG_SUPPORTS_REMOVE}
     * @param parentDocumentUri parent document of the document to remove.
     * @return true if the document was removed successfully.
     */
    public static boolean removeDocument(@NonNull ContentResolver content, @NonNull Uri documentUri,
            @NonNull Uri parentDocumentUri) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return DocumentsContractApi24Impl.removeDocument(content, documentUri,
                    parentDocumentUri);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return DocumentsContractApi19Impl.deleteDocument(content, documentUri);
        } else {
            return false;
        }
    }

    @RequiresApi(19)
    private static class DocumentsContractApi19Impl {

        @DoNotInline
        public static Uri buildDocumentUri(String authority, String documentId) {
            return DocumentsContract.buildDocumentUri(authority, documentId);
        }

        @DoNotInline
        static boolean isDocumentUri(Context context, @Nullable Uri uri) {
            return DocumentsContract.isDocumentUri(context, uri);
        }

        @DoNotInline
        static String getDocumentId(Uri documentUri) {
            return DocumentsContract.getDocumentId(documentUri);
        }

        @DoNotInline
        static boolean deleteDocument(ContentResolver content, Uri documentUri)
                throws FileNotFoundException {
            return DocumentsContract.deleteDocument(content, documentUri);
        }

        private DocumentsContractApi19Impl() {
        }
    }

    @RequiresApi(21)
    private static class DocumentsContractApi21Impl {
        @DoNotInline
        static String getTreeDocumentId(Uri documentUri) {
            return DocumentsContract.getTreeDocumentId(documentUri);
        }

        @DoNotInline
        public static Uri buildTreeDocumentUri(String authority, String documentId) {
            return DocumentsContract.buildTreeDocumentUri(authority, documentId);
        }

        @DoNotInline
        static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId) {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        }

        @DoNotInline
        static Uri buildChildDocumentsUri(String authority, String parentDocumentId) {
            return DocumentsContract.buildChildDocumentsUri(authority, parentDocumentId);
        }

        @DoNotInline
        static Uri buildChildDocumentsUriUsingTree(Uri treeUri, String parentDocumentId) {
            return DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
        }

        @DoNotInline
        static Uri createDocument(ContentResolver content, Uri parentDocumentUri,
                String mimeType, String displayName) throws FileNotFoundException {
            return DocumentsContract.createDocument(content, parentDocumentUri, mimeType,
                    displayName);
        }

        @DoNotInline
        static Uri renameDocument(@NonNull ContentResolver content,
                @NonNull Uri documentUri, @NonNull String displayName)
                throws FileNotFoundException {
            return DocumentsContract.renameDocument(content, documentUri, displayName);
        }

        private DocumentsContractApi21Impl() {
        }
    }

    @RequiresApi(24)
    private static class DocumentsContractApi24Impl {
        @DoNotInline
        static boolean isTreeUri(@NonNull Uri uri) {
            return DocumentsContract.isTreeUri(uri);
        }

        @DoNotInline
        static boolean removeDocument(ContentResolver content, Uri documentUri,
                Uri parentDocumentUri) throws FileNotFoundException {
            return DocumentsContract.removeDocument(content, documentUri, parentDocumentUri);
        }

        private DocumentsContractApi24Impl() {
        }
    }

    private DocumentsContractCompat() {
    }
}
