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

package androidx.documentfile.provider;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

abstract class DocumentsContractApi {

    static boolean isDocumentUri(Context context, @Nullable Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return DocumentsContractApi19Impl.isDocumentUri(context, uri);
        } else {
            return false;
        }
    }

    @Nullable
    static String getDocumentId(Uri documentUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return DocumentsContractApi19Impl.getDocumentId(documentUri);
        } else {
            return null;
        }
    }

    @Nullable
    static String getTreeDocumentId(Uri documentUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.getTreeDocumentId(documentUri);
        } else {
            return null;
        }
    }

    @Nullable
    static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildDocumentUriUsingTree(treeUri, documentId);
        } else {
            return null;
        }
    }

    @RequiresApi(19)
    private static class DocumentsContractApi19Impl {
        static boolean isDocumentUri(Context context, @Nullable Uri uri) {
            return DocumentsContract.isDocumentUri(context, uri);
        }

        static String getDocumentId(Uri documentUri) {
            return DocumentsContract.getDocumentId(documentUri);
        }

        private DocumentsContractApi19Impl() {
        }
    }

    @RequiresApi(21)
    private static class DocumentsContractApi21Impl {
        static String getTreeDocumentId(Uri documentUri) {
            return DocumentsContract.getTreeDocumentId(documentUri);
        }

        static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId) {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        }

        private DocumentsContractApi21Impl() {
        }
    }

    private DocumentsContractApi() {
    }
}
